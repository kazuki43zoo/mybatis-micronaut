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

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

import com.example.mapper.mail.MailMapper;
import com.example.mapper.phone.PhoneMapper;
import com.example.mapper.user.UserMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.BeanInstantiationException;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.ResultLoaderMap;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.micronaut.domain.City;
import org.mybatis.micronaut.domain.Country;
import org.mybatis.micronaut.domain.Entity;
import org.mybatis.micronaut.domain.Region;
import org.mybatis.micronaut.mapper.city.CityMapper;
import org.mybatis.micronaut.mapper.country.CountryMapper;
import org.mybatis.micronaut.mapper.region.RegionMapper;
import org.mybatis.micronaut.scripting.CustomLanguageDriver;
import org.mybatis.micronaut.scripting.MyLanguageDriver;
import org.mybatis.micronaut.service.CityService;
import org.mybatis.micronaut.typehandler.UUIDTypeHandler;
import org.mybatis.micronaut.typehandler.city.CityTypeHandler;
import org.mybatis.micronaut.typehandler.country.CountryTypeHandler;
import org.mybatis.micronaut.typehandler.region.RegionTypeHandler;

class MyBatisFactoryBeanTest {

  @Test
  void whenMapperPackageIsEmptyShouldScanFromDefaultPackage() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      MyBatisConfiguration configuration = context.getBean(MyBatisConfiguration.class);
      Assertions.assertEquals(0, configuration.getMapperPackages().length);
      Assertions.assertNull(configuration.getDataSourceName());
      SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
      Assertions.assertEquals(3, factory.getConfiguration().getMapperRegistry().getMappers().size());
      {
        CityMapper mapper = context.getBean(CityMapper.class);
        Assertions.assertEquals(1, mapper.select());
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
      {
        CountryMapper mapper = context.getBean(CountryMapper.class);
        Assertions.assertEquals(2, mapper.select());
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
      {
        RegionMapper mapper = context.getBean(RegionMapper.class);
        Assertions.assertEquals(3, mapper.select());
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
    }
  }

  @Test
  void whenMapperPackageIsSpecifyShouldScanOnlyFromSpecifiedPackage() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages",
        new String[] { "org.mybatis.micronaut.mapper.city", "org.mybatis.micronaut.mapper.country" });
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      MyBatisConfiguration configuration = context.getBean(MyBatisConfiguration.class);
      Assertions.assertArrayEquals(
          new String[] { "org.mybatis.micronaut.mapper.city", "org.mybatis.micronaut.mapper.country" },
          configuration.getMapperPackages());
      Assertions.assertNull(configuration.getDataSourceName());
      SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
      Assertions.assertEquals(2, factory.getConfiguration().getMapperRegistry().getMappers().size());
      Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(CityMapper.class));
      Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(CountryMapper.class));
    }
  }

  @Test
  void whenMappersIsSpecifyShouldApplyToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mappers", new Class[] { UserMapper.class });
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
      Assertions.assertEquals(4, factory.getConfiguration().getMapperRegistry().getMappers().size());
      Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(UserMapper.class));
    }
  }

  @Test
  void whenMapperXmlFilesIsSpecifyShouldApplyToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-xml-base-paths",
        new String[] { "META-INF/mappers/mail", "META-INF/mappers/phone" });
    properties.put("mybatis.default.mapper-xml-files", new String[] { "MailMapper.xml", "PhoneMapper.xml" });
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
      Assertions.assertEquals(5, factory.getConfiguration().getMapperRegistry().getMappers().size());
      Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(MailMapper.class));
      Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(PhoneMapper.class));
    }
  }

  @Test
  void whenMapperXmlFilesNotFoundShouldThrowIllegalArgumentException() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-xml-base-paths",
        new String[] { "META-INF/mappers/mail", "META-INF/mappers/phone" });
    properties.put("mybatis.default.mapper-xml-files", new String[] { "MailMapper.xml", "UserMapper.xml" });
    BeanInstantiationException exception = Assertions.assertThrows(BeanInstantiationException.class,
        () -> ApplicationContext.build("default").properties(properties).start());
    Assertions.assertTrue(exception.getMessage().contains(
        "Does not exists [UserMapper.xml] in either [META-INF/mappers/mail, META-INF/mappers/phone] on your classpath."));
  }

  @Test
  void whenExistsMultipleDataSourceShouldConfigureMultipleMyBatisBeans() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages",
        new String[] { "org.mybatis.micronaut.mapper.city", "org.mybatis.micronaut.mapper.country" });
    properties.put("mybatis.2nd.mapper-packages",
        new String[] { "org.mybatis.micronaut.mapper.city", "org.mybatis.micronaut.mapper.region" });
    try (ApplicationContext context = ApplicationContext.build("default", "2nd").properties(properties).start()) {
      {
        MyBatisConfiguration configuration = context.getBean(MyBatisConfiguration.class, Qualifiers.byName("default"));
        Assertions.assertArrayEquals(
            new String[] { "org.mybatis.micronaut.mapper.city", "org.mybatis.micronaut.mapper.country" },
            configuration.getMapperPackages());
        Assertions.assertNull(configuration.getDataSourceName());
        SqlSessionFactory factory = context.getBean(SqlSessionFactory.class, Qualifiers.byName("default"));
        Assertions.assertEquals(2, factory.getConfiguration().getMapperRegistry().getMappers().size());
        Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(CityMapper.class));
        Assertions
            .assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(CountryMapper.class));
        Assertions.assertEquals("default", factory.getConfiguration().getEnvironment().getId());
      }
      {
        CityMapper mapper = context.getBean(CityMapper.class, Qualifiers.byName("default"));
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
      {
        CountryMapper mapper = context.getBean(CountryMapper.class);
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
      {
        MyBatisConfiguration configuration = context.getBean(MyBatisConfiguration.class, Qualifiers.byName("2nd"));
        Assertions.assertArrayEquals(
            new String[] { "org.mybatis.micronaut.mapper.city", "org.mybatis.micronaut.mapper.region" },
            configuration.getMapperPackages());
        Assertions.assertNull(configuration.getDataSourceName());
        SqlSessionFactory factory = context.getBean(SqlSessionFactory.class, Qualifiers.byName("2nd"));
        Assertions.assertEquals(2, factory.getConfiguration().getMapperRegistry().getMappers().size());
        Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(CityMapper.class));
        Assertions.assertTrue(factory.getConfiguration().getMapperRegistry().getMappers().contains(RegionMapper.class));
        Assertions.assertEquals("2nd", factory.getConfiguration().getEnvironment().getId());
      }
      {
        CityMapper mapper = context.getBean(CityMapper.class, Qualifiers.byName("2nd"));
        Assertions.assertEquals("2ND", mapper.selectDatabaseName());
      }
      {
        RegionMapper mapper = context.getBean(RegionMapper.class);
        Assertions.assertEquals(3, mapper.select());
        Assertions.assertEquals("2ND", mapper.selectDatabaseName());
      }
    }
  }

  @Test
  void whenSpecifyDataSourceNameShouldLookupSpecifiedDataSource() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    properties.put("mybatis.2nd.mapper-packages", new String[] {});
    properties.put("mybatis.2nd.data-source-name", "default");
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      {
        CityMapper mapper = context.getBean(CityMapper.class, Qualifiers.byName("default"));
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
      {
        CityMapper mapper = context.getBean(CityMapper.class, Qualifiers.byName("2nd"));
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
    }
  }

  @Test
  void whenDoesNotExistDataSourceWithSameQualifyShouldThrowException() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    BeanInstantiationException exception = Assertions.assertThrows(BeanInstantiationException.class, () -> {
      try (ApplicationContext context = ApplicationContext.build().properties(properties).start()) {
        context.getBean(SqlSessionFactory.class);
      }
    });
    Assertions.assertTrue(exception.getMessage()
        .contains("No bean of type [javax.sql.DataSource] exists for the given qualifier: @Named('default')."));
  }

  @Test
  void whenDoesNotExistSpecifiedDataSourceShouldThrowException() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    properties.put("mybatis.default.data-source-name", "2nd");
    BeanInstantiationException exception = Assertions.assertThrows(BeanInstantiationException.class, () -> {
      try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
        context.getBean(SqlSessionFactory.class);
      }
    });
    Assertions.assertTrue(exception.getMessage()
        .contains("No bean of type [javax.sql.DataSource] exists for the given qualifier: @Named('2nd')."));
  }

  @Test
  void whenSpecifyMyBatisCoreConfigurationPropertyShouldApplyToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    properties.put("mybatis.default.configuration.default-executor-type", ExecutorType.REUSE);
    properties.put("mybatis.default.configuration.variables.key1", "value1");
    properties.put("mybatis.default.configuration.variables.key2", "value2");
    properties.put("mybatis.default.configuration.lazy-load-trigger-methods",
        "equals,clone,hashCode,toString,getClass");
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
      Assertions.assertEquals(ExecutorType.REUSE, factory.getConfiguration().getDefaultExecutorType());
      Assertions.assertEquals(2, factory.getConfiguration().getVariables().size());
      Assertions.assertEquals("value1", factory.getConfiguration().getVariables().getProperty("key1"));
      Assertions.assertEquals("value2", factory.getConfiguration().getVariables().getProperty("key2"));
      Assertions.assertEquals(5, factory.getConfiguration().getLazyLoadTriggerMethods().size());
      Assertions.assertTrue(factory.getConfiguration().getLazyLoadTriggerMethods()
          .containsAll(Arrays.asList("equals", "clone", "hashCode", "toString", "getClass")));
    }
  }

  @Test
  void whenSpecifyExcludeMyBatisCoreConfigurationPropertyShouldIgnoreBindingToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    properties.put("mybatis.default.configuration.environment",
        new Environment("test", new JdbcTransactionFactory(), new HikariDataSource()));
    properties.put("mybatis.default.configuration.proxy-factory", new MyProxyFactory());
    properties.put("mybatis.default.configuration.reflector-factory", new MyReflectorFactory());
    properties.put("mybatis.default.configuration.object-factory", new MyObjectFactory());
    properties.put("mybatis.default.configuration.object-wrapper-factory", new MyObjectWrapperFactory());
    properties.put("mybatis.default.configuration.default-scripting-language", MyLanguageDriver.class);
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
      Assertions.assertEquals("default", factory.getConfiguration().getEnvironment().getId());
      Assertions.assertEquals(JavassistProxyFactory.class, factory.getConfiguration().getProxyFactory().getClass());
      Assertions.assertEquals(DefaultReflectorFactory.class,
          factory.getConfiguration().getReflectorFactory().getClass());
      Assertions.assertEquals(DefaultObjectFactory.class, factory.getConfiguration().getObjectFactory().getClass());
      Assertions.assertEquals(DefaultObjectWrapperFactory.class,
          factory.getConfiguration().getObjectWrapperFactory().getClass());
      Assertions.assertEquals(XMLLanguageDriver.class,
          factory.getConfiguration().getDefaultScriptingLanguageInstance().getClass());
    }
  }

  @Test
  void whenSpecifyTypeAliasConfigurationPropertiesShouldApplyToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.type-alias-packages",
        new String[] { "org.mybatis.micronaut.domain", "org.mybatis.micronaut.mapper.city" });
    properties.put("mybatis.default.type-alias-super-type", Entity.class);
    properties.put("mybatis.default.type-aliases", new Class[] { CityService.class });
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      Configuration configuration = context.getBean(SqlSessionFactory.class).getConfiguration();
      Assertions.assertEquals(City.class, configuration.getTypeAliasRegistry().resolveAlias("city"));
      Assertions.assertEquals(Country.class, configuration.getTypeAliasRegistry().resolveAlias("country"));
      Assertions.assertEquals(Region.class, configuration.getTypeAliasRegistry().resolveAlias("region"));
      Assertions.assertEquals(CityService.class, configuration.getTypeAliasRegistry().resolveAlias("cityService"));
      Assertions.assertThrows(TypeException.class, () -> configuration.getTypeAliasRegistry().resolveAlias("status"));
      Assertions.assertThrows(TypeException.class, () -> configuration.getTypeAliasRegistry().resolveAlias("entity"));
    }
  }

  @Test
  void whenSpecifyTypeHandlerConfigurationPropertiesShouldApplyToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.type-handler-packages",
        new String[] { "org.mybatis.micronaut.typehandler.city", "org.mybatis.micronaut.typehandler.country" });
    properties.put("mybatis.default.type-handlers", new Class[] { UUIDTypeHandler.class });
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      Configuration configuration = context.getBean(SqlSessionFactory.class).getConfiguration();
      Assertions.assertEquals(CityTypeHandler.class,
          configuration.getTypeHandlerRegistry().getTypeHandler(City.class).getClass());
      Assertions.assertEquals(CountryTypeHandler.class,
          configuration.getTypeHandlerRegistry().getTypeHandler(Country.class).getClass());
      Assertions.assertEquals(UUIDTypeHandler.class,
          configuration.getTypeHandlerRegistry().getTypeHandler(UUID.class).getClass());
      Assertions.assertNull(configuration.getTypeHandlerRegistry().getTypeHandler(Path.class));
      Assertions.assertNull(configuration.getTypeHandlerRegistry().getTypeHandler(Region.class));
    }
  }

  @Test
  void whenSpecifyScriptingLanguageDriverConfigurationPropertiesShouldApplyToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.scripting-language-drivers", new Class[] { CustomLanguageDriver.class });
    properties.put("mybatis.default.default-scripting-language-driver", MyLanguageDriver.class);
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      Configuration configuration = context.getBean(SqlSessionFactory.class).getConfiguration();
      Assertions.assertEquals(MyLanguageDriver.class, configuration.getDefaultScriptingLanguageInstance().getClass());
      Assertions.assertNotNull(configuration.getLanguageRegistry().getDriver(MyLanguageDriver.class));
      Assertions.assertNotNull(configuration.getLanguageRegistry().getDriver(CustomLanguageDriver.class));
    }
  }

  @Test
  void whenMyBatisComponentsExistsInApplicationContextShouldApplyToSqlSessionFactory() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    MyPlugin1 plugin1 = new MyPlugin1();
    MyPlugin2 plugin2 = new MyPlugin2();
    ConfigurationCustomizer customizer1 = c -> c.setDefaultExecutorType(ExecutorType.REUSE);
    ConfigurationCustomizer customizer2 = c -> c.setDefaultScriptingLanguage(MyLanguageDriver.class);

    try (ApplicationContext context = ApplicationContext.build("default")
        .singletons(new RegionTypeHandler(), new MyObjectFactory(), new MyObjectWrapperFactory(),
            new MyReflectorFactory(), new MyProxyFactory(), plugin1, plugin2, new CustomLanguageDriver(),
            new MyLanguageDriver(), new PerpetualCache("test1"), new MyCache("test2"), new MyDatabaseIdProvider(),
            customizer1, customizer2)
        .properties(properties).start()) {
      Configuration configuration = context.getBean(SqlSessionFactory.class).getConfiguration();
      Assertions.assertEquals(RegionTypeHandler.class,
          configuration.getTypeHandlerRegistry().getTypeHandler(Region.class).getClass());
      Assertions.assertEquals(MyObjectFactory.class, configuration.getObjectFactory().getClass());
      Assertions.assertEquals(MyObjectWrapperFactory.class, configuration.getObjectWrapperFactory().getClass());
      Assertions.assertEquals(MyReflectorFactory.class, configuration.getReflectorFactory().getClass());
      Assertions.assertEquals(MyProxyFactory.class, configuration.getProxyFactory().getClass());
      Assertions.assertEquals(2, configuration.getInterceptors().size());
      Assertions.assertTrue(configuration.getInterceptors().containsAll(Arrays.asList(plugin1, plugin2)));
      Assertions.assertNotNull(configuration.getLanguageRegistry().getDriver(MyLanguageDriver.class));
      Assertions.assertNotNull(configuration.getLanguageRegistry().getDriver(CustomLanguageDriver.class));
      Assertions.assertNotNull(configuration.getCache("test1"));
      Assertions.assertNotNull(configuration.getCache("test2"));
      Assertions.assertEquals("test", configuration.getDatabaseId());
      Assertions.assertEquals(ExecutorType.REUSE, configuration.getDefaultExecutorType());
      Assertions.assertEquals(MyLanguageDriver.class, configuration.getDefaultScriptingLanguageInstance().getClass());
    }
  }

  @Test
  void normalForSqlSessionFactoryWithJdbcBasedTx() throws IOException, SQLException {
    Map<String, Object> properties = new HashMap<>();
    properties.put("mybatis.default.mapper-packages", new String[] {});
    try (ApplicationContext context = ApplicationContext.build("default").properties(properties).start()) {
      MyBatisFactoryBean factoryBean = context.getBean(MyBatisFactoryBean.class);
      SqlSessionFactory factory = factoryBean.sqlSessionFactoryWithJdbcBasedTx("default",
          new MyBatisConfiguration(context.getEnvironment()));
      try (SqlSession sqlSession = factory.openSession()) {
        CityMapper mapper = sqlSession.getMapper(CityMapper.class);
        Assertions.assertEquals("DEFAULT", mapper.selectDatabaseName());
      }
    }
  }

  private static class MyObjectFactory extends DefaultObjectFactory {
  }

  private static class MyObjectWrapperFactory extends DefaultObjectWrapperFactory {
  }

  private static class MyReflectorFactory extends DefaultReflectorFactory {
  }

  private static class MyCache implements Cache {
    private final String id;

    private MyCache(String id) {
      this.id = id;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void putObject(Object key, Object value) {

    }

    @Override
    public Object getObject(Object key) {
      return null;
    }

    @Override
    public Object removeObject(Object key) {
      return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public int getSize() {
      return 0;
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
      return null;
    }
  }

  @Intercepts({ @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
      RowBounds.class, ResultHandler.class }) })
  private static class MyPlugin1 implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
      return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
      return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
  }

  @Intercepts({ @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class,
      RowBounds.class, ResultHandler.class }) })
  private static class MyPlugin2 implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
      return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
      return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
  }

  private static class MyDatabaseIdProvider implements DatabaseIdProvider {

    @Override
    public void setProperties(Properties p) {

    }

    @Override
    public String getDatabaseId(DataSource dataSource) {
      return "test";
    }
  }

  private static class MyProxyFactory implements ProxyFactory {

    @Override
    public void setProperties(Properties properties) {

    }

    @Override
    public Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration,
        ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
      return null;
    }
  }

}
