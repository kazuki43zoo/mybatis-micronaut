/**
 *    Copyright 2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.micronaut;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.inject.qualifiers.Qualifiers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The factory bean for MyBatis integration.
 *
 * @author Kazuki Shimizu
 * @since 1.0.0
 */
@Factory
class MyBatisFactoryBean {

  private static final Logger logger = LoggerFactory.getLogger(MyBatisFactoryBean.class);

  private final ApplicationContext applicationContext;

  /**
   * Constructor.
   *
   * @param applicationContext
   *          The application context of Micronaut
   */
  @SuppressWarnings("unused")
  MyBatisFactoryBean(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Configure an {@link SqlSessionFactory} with JDBC based transaction.
   *
   * @param name
   *          The qualify of bean
   * @param configuration
   *          configuration bean for MyBatis
   * @return The {@link SqlSessionFactory} bean
   * @throws SQLException
   *           If fail to get the database id via {@link DatabaseIdProvider}
   * @throws IOException
   *           If fail to open mapper xml file
   */
  @Singleton
  @Requires(missingBeans = DataSourceTransactionManager.class)
  @EachBean(MyBatisConfiguration.class)
  @SuppressWarnings("unused")
  SqlSessionFactory sqlSessionFactoryWithJdbcBasedTx(@Parameter String name, MyBatisConfiguration configuration)
      throws SQLException, IOException {
    logger.info("Configure an SqlSessionFactory with JDBC based transaction for '{}'.", name);
    DataSource dataSource = decideDataSource(name, configuration);
    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    Configuration coreConfiguration = newCoreConfiguration(name, configuration, transactionFactory, dataSource);
    return new SqlSessionFactoryBuilder().build(coreConfiguration);
  }

  /**
   * Configure an {@link SqlSessionFactory} with Spring based transaction.
   *
   * @param name
   *          The qualify of bean
   * @param configuration
   *          The configuration bean for MyBatis
   * @return The {@link SqlSessionFactory} bean
   * @throws SQLException
   *           If fail to get the database id via {@link DatabaseIdProvider}
   * @throws IOException
   *           If fail to open mapper xml file
   */
  @Singleton
  @Requires(classes = { SpringManagedTransaction.class, DataSourceUtils.class,
      TransactionSynchronizationManager.class }, beans = DataSourceTransactionManager.class)
  @Requires(beans = DataSourceTransactionManager.class)
  @EachBean(MyBatisConfiguration.class)
  @SuppressWarnings("unused")
  SqlSessionFactory sqlSessionFactoryWithSpringManagedTx(@Parameter String name, MyBatisConfiguration configuration)
      throws SQLException, IOException {
    logger.info("Configure an SqlSessionFactory with Spring based transaction for '{}'.", name);
    DataSource dataSource = decideDataSource(name, configuration);
    if (dataSource instanceof TransactionAwareDataSourceProxy) {
      dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
    }
    TransactionFactory transactionFactory = new SpringManagedTransactionFactory();
    Configuration coreConfiguration = newCoreConfiguration(name, configuration, transactionFactory, dataSource);
    return new SqlSessionFactoryBuilder().build(coreConfiguration);
  }

  /**
   * Configure an {@link SqlSessionTemplate}.
   *
   * @param name
   *          The qualify of bean
   * @param sqlSessionFactory
   *          The {@link SqlSessionFactory} bean
   * @return The {@link SqlSessionTemplate} bean
   */
  @Context
  @Requires(classes = { SpringManagedTransaction.class, DataSourceUtils.class,
      TransactionSynchronizationManager.class }, beans = DataSourceTransactionManager.class)
  @EachBean(SqlSessionFactory.class)
  @Bean(preDestroy = "destroy")
  @SuppressWarnings({ "unchecked", "unused" })
  SqlSessionTemplate sqlSessionTemplate(@Parameter String name, SqlSessionFactory sqlSessionFactory) {
    logger.info("Configure an SqlSessionTemplate for '{}'.", name);
    Configuration configuration = sqlSessionFactory.getConfiguration();
    SqlSessionTemplate sqlSession = new SqlSessionTemplate(sqlSessionFactory, configuration.getDefaultExecutorType(),
        null);
    sqlSession.getConfiguration().getMapperRegistry().getMappers()
        .forEach(mapperType -> applicationContext.registerSingleton((Class<Object>) mapperType,
            configuration.getMapper(mapperType, sqlSession),
            Qualifiers.byName(configuration.getEnvironment().getId())));
    return sqlSession;
  }

  private DataSource decideDataSource(String name, MyBatisConfiguration configuration) {
    return applicationContext.getBean(DataSource.class,
        Qualifiers.byName(Optional.ofNullable(configuration.getDataSourceName()).orElse(name)));
  }

  private Configuration newCoreConfiguration(String name, MyBatisConfiguration configuration,
      TransactionFactory transactionFactory, DataSource dataSource) throws SQLException, IOException {
    Configuration coreConfiguration = configuration.getConfiguration();
    Environment mybatisEnvironment = new Environment(name, transactionFactory, dataSource);
    coreConfiguration.setEnvironment(mybatisEnvironment);
    configureTypeAliases(configuration, coreConfiguration);
    configureTypeHandlers(configuration, coreConfiguration);
    configureFactories(coreConfiguration);
    configurePluginInterceptors(coreConfiguration);
    configureScriptingLanguageDrivers(configuration, coreConfiguration);
    configureCaches(coreConfiguration);
    configureDatabaseIdProvider(dataSource, coreConfiguration);
    applyConfigurationCustomizers(coreConfiguration);
    configureMappers(configuration, coreConfiguration);
    return coreConfiguration;

  }

  private void configureTypeAliases(MyBatisConfiguration configuration, Configuration coreConfiguration) {
    if (ArrayUtils.isNotEmpty(configuration.getTypeAliasPackages())) {
      Arrays.stream(configuration.getTypeAliasPackages()).forEach(packageName -> coreConfiguration
          .getTypeAliasRegistry().registerAliases(packageName, configuration.getTypeAliasSuperType()));
    }
    if (ArrayUtils.isNotEmpty(configuration.getTypeAliases())) {
      Arrays.stream(configuration.getTypeAliases()).forEach(coreConfiguration.getTypeAliasRegistry()::registerAlias);
    }
  }

  private void configureTypeHandlers(MyBatisConfiguration configuration, Configuration coreConfiguration) {
    if (ArrayUtils.isNotEmpty(configuration.getTypeHandlerPackages())) {
      Arrays.stream(configuration.getTypeHandlerPackages())
          .forEach(coreConfiguration.getTypeHandlerRegistry()::register);
    }
    if (ArrayUtils.isNotEmpty(configuration.getTypeHandlers())) {
      Arrays.stream(configuration.getTypeHandlers()).forEach(coreConfiguration.getTypeHandlerRegistry()::register);
    }
    applicationContext.getBeansOfType(TypeHandler.class).forEach(coreConfiguration.getTypeHandlerRegistry()::register);
  }

  private void configureFactories(Configuration coreConfiguration) {
    if (applicationContext.containsBean(ObjectFactory.class)) {
      coreConfiguration.setObjectFactory(applicationContext.getBean(ObjectFactory.class));
    }
    if (applicationContext.containsBean(ObjectWrapperFactory.class)) {
      coreConfiguration.setObjectWrapperFactory(applicationContext.getBean(ObjectWrapperFactory.class));
    }
    if (applicationContext.containsBean(ReflectorFactory.class)) {
      coreConfiguration.setReflectorFactory(applicationContext.getBean(ReflectorFactory.class));
    }
    if (applicationContext.containsBean(ProxyFactory.class)) {
      coreConfiguration.setProxyFactory(applicationContext.getBean(ProxyFactory.class));
    }
  }

  private void configurePluginInterceptors(Configuration coreConfiguration) {
    applicationContext.getBeansOfType(Interceptor.class).forEach(coreConfiguration::addInterceptor);
  }

  private void configureScriptingLanguageDrivers(MyBatisConfiguration configuration, Configuration coreConfiguration) {
    if (ArrayUtils.isNotEmpty(configuration.getScriptingLanguageDrivers())) {
      Arrays.stream(configuration.getScriptingLanguageDrivers())
          .forEach(coreConfiguration.getLanguageRegistry()::register);
    }
    applicationContext.getBeansOfType(LanguageDriver.class).forEach(coreConfiguration.getLanguageRegistry()::register);
    Optional.ofNullable(configuration.getDefaultScriptingLanguageDriver())
        .ifPresent(coreConfiguration::setDefaultScriptingLanguage);
  }

  private void configureCaches(Configuration coreConfiguration) {
    applicationContext.getBeansOfType(Cache.class).forEach(coreConfiguration::addCache);
  }

  private void configureDatabaseIdProvider(DataSource dataSource, Configuration mybatisConfiguration)
      throws SQLException {
    if (applicationContext.containsBean(DatabaseIdProvider.class)) {
      mybatisConfiguration
          .setDatabaseId(applicationContext.getBean(DatabaseIdProvider.class).getDatabaseId(dataSource));
    }
  }

  private void applyConfigurationCustomizers(Configuration coreConfiguration) {
    applicationContext.getBeansOfType(ConfigurationCustomizer.class).forEach(c -> c.customize(coreConfiguration));
  }

  private void configureMappers(MyBatisConfiguration configuration, Configuration coreConfiguration)
      throws IOException {
    configuration.findMappers().stream().filter(Class::isInterface).forEach(coreConfiguration::addMapper);
    for (URL mapperXmlFile : configuration.findMapperXmlFiles()) {
      XMLMapperBuilder builder = new XMLMapperBuilder(mapperXmlFile.openStream(), coreConfiguration,
          mapperXmlFile.toString(), coreConfiguration.getSqlFragments());
      builder.parse();
    }
  }

}
