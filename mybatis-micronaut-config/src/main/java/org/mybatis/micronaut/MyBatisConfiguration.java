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

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.env.Environment;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.util.ArrayUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;

/**
 * The configuration properties for MyBatis integration.
 *
 * @author Kazuki Shimizu
 * @since 1.0.0
 */
@EachProperty(value = MyBatisConfiguration.PREFIX, primary = "default")
public class MyBatisConfiguration {

  static final String PREFIX = "mybatis";

  private final Environment environment;

  private Class<?>[] mappers;
  private String[] mapperPackages;
  private String[] mapperXmlBasePaths;
  private String[] mapperXmlFiles;
  private String[] typeAliasPackages;
  private Class<?> typeAliasSuperType = Object.class;
  private Class<?>[] typeAliases;
  private String[] typeHandlerPackages;
  private Class<? extends TypeHandler>[] typeHandlers;
  private Class<? extends LanguageDriver>[] scriptingLanguageDrivers;
  private Class<? extends LanguageDriver> defaultScriptingLanguageDriver;
  private String dataSourceName;

  @ConfigurationBuilder(configurationPrefix = "configuration", excludes = { "environment", "proxyFactory",
      "reflectorFactory", "objectFactory", "objectWrapperFactory", "defaultScriptingLanguage" })
  private Configuration configuration = new Configuration();

  /**
   * Constructor.
   *
   * @param environment
   *          The environment of the Micronaut
   */
  public MyBatisConfiguration(Environment environment) {
    this.environment = environment;
  }

  /**
   * Sets the target classes to register as mapper.
   *
   * @param mappers
   *          The target classes to register as mapper
   */
  public void setMappers(Class<?>... mappers) {
    this.mappers = mappers;
  }

  /**
   * Return the target classes to register as mapper.
   *
   * @return The target classes to register as mapper
   */
  public Class<?>[] getMappers() {
    return mappers;
  }

  /**
   * Sets the base packages to scan mapper.
   *
   * @param mapperPackages
   *          The base packages to scan mapper
   */
  public void setMapperPackages(String... mapperPackages) {
    this.mapperPackages = mapperPackages;
  }

  /**
   * Returns the base packages to scan mapper.
   *
   * @return The base packages to scan mapper
   */
  public String[] getMapperPackages() {
    return mapperPackages;
  }

  /**
   * Sets the base paths to load mapper xml file.
   *
   * @param mapperXmlBasePaths
   *          The base paths to scan mapper xml files.
   */
  public void setMapperXmlBasePaths(String... mapperXmlBasePaths) {
    this.mapperXmlBasePaths = mapperXmlBasePaths;
  }

  /**
   * Returns the base paths to scan mapper xml files.
   *
   * @return The base paths to scan mapper xml files
   */
  public String[] getMapperXmlBasePaths() {
    return mapperXmlBasePaths;
  }

  /**
   * Sets the mapper xml files.
   *
   * @param mapperXmlFiles
   *          The mapper xml files
   */
  public void setMapperXmlFiles(String... mapperXmlFiles) {
    this.mapperXmlFiles = mapperXmlFiles;
  }

  /**
   * Return the mapper xml files.
   *
   * @return The mapper xml files
   */
  public String[] getMapperXmlFiles() {
    return mapperXmlFiles;
  }

  /**
   * Sets the base packages to scan type alias.
   *
   * @param typeAliasPackages
   *          The base packages to scan type alias
   */
  public void setTypeAliasPackages(String... typeAliasPackages) {
    this.typeAliasPackages = typeAliasPackages;
  }

  /**
   * Return base packages to scan type alias.
   *
   * @return The base packages to scan type alias
   */
  public String[] getTypeAliasPackages() {
    return typeAliasPackages;
  }

  /**
   * Sets the super type to scan the type aliases.
   *
   * @param typeAliasSuperType
   *          The super type to scan the type aliases
   */
  public void setTypeAliasSuperType(Class<?> typeAliasSuperType) {
    this.typeAliasSuperType = typeAliasSuperType;
  }

  /**
   * Return the super type to scan the type aliases.
   *
   * @return The super type to scan the type aliases
   */
  public Class<?> getTypeAliasSuperType() {
    return typeAliasSuperType;
  }

  /**
   * Sets the target classes to register type aliases.
   *
   * @param typeAliases
   *          The target classes to register type aliases
   */
  public void setTypeAliases(Class<?>... typeAliases) {
    this.typeAliases = typeAliases;
  }

  /**
   * Return the target classes to register type aliases.
   *
   * @return the Target classes to register type aliases
   */
  public Class<?>[] getTypeAliases() {
    return typeAliases;
  }

  /**
   * Sets the base packages to scan the type handlers.
   *
   * @param typeHandlerPackages
   *          The base packages to scan the type handlers
   */
  public void setTypeHandlerPackages(String... typeHandlerPackages) {
    this.typeHandlerPackages = typeHandlerPackages;
  }

  /**
   * Return the base packages to scan the type handlers.
   *
   * @return The base packages to scan the type handlers
   */
  public String[] getTypeHandlerPackages() {
    return typeHandlerPackages;
  }

  /**
   * Sets target classes to register as type handler.
   *
   * @param typeHandlers
   *          target classes to register type handler
   */
  public void setTypeHandlers(Class<? extends TypeHandler>... typeHandlers) {
    this.typeHandlers = typeHandlers;
  }

  /**
   * Return the target classes to register as type handler.
   *
   * @return the target classes to register as type handler
   */
  public Class<? extends TypeHandler>[] getTypeHandlers() {
    return typeHandlers;
  }

  /**
   * Sets the target classes to register as scripting language driver.
   *
   * @param scriptingLanguageDrivers
   *          The target classes to register as scripting language driver
   */
  public void setScriptingLanguageDrivers(Class<? extends LanguageDriver>... scriptingLanguageDrivers) {
    this.scriptingLanguageDrivers = scriptingLanguageDrivers;
  }

  /**
   * Return the target classes to register as scripting language driver.
   *
   * @return The target classes to register as scripting language driver.
   */
  public Class<? extends LanguageDriver>[] getScriptingLanguageDrivers() {
    return scriptingLanguageDrivers;
  }

  /**
   * Sets the default scripting language driver class.
   *
   * @param defaultScriptingLanguageDriver
   *          The default scripting language driver class.
   */
  public void setDefaultScriptingLanguageDriver(Class<? extends LanguageDriver> defaultScriptingLanguageDriver) {
    this.defaultScriptingLanguageDriver = defaultScriptingLanguageDriver;
  }

  /**
   * Return the default scripting language driver class.
   *
   * @return The default scripting language driver class
   */
  public Class<? extends LanguageDriver> getDefaultScriptingLanguageDriver() {
    return defaultScriptingLanguageDriver;
  }

  /**
   * Sets the data source name.
   *
   * @param dataSourceName
   *          The data source name
   */
  public void setDataSourceName(String dataSourceName) {
    this.dataSourceName = dataSourceName;
  }

  /**
   * Return the data source name.
   *
   * @return The data source name
   */
  public String getDataSourceName() {
    return dataSourceName;
  }

  /**
   * Sets the MyBatis's core component configuration.
   *
   * For details, see the link:http://www.mybatis.org/mybatis-3/configuration.html#settings[MyBatis core module
   * reference document^].
   *
   * @param configuration
   *          The MyBatis's core component configuration
   */
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Return the MyBatis's core component configuration.
   *
   * @return The MyBatis's core component configuration
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * Find mapper interface classes for the current configuration.
   *
   * @return The mapper interface classes
   */
  Collection<Class<?>> findMappers() {
    Collection<Class<?>> mappers = new HashSet<>(
        ArrayUtils.isEmpty(getMappers()) ? Collections.emptySet() : Arrays.asList(getMappers()));
    Collection<String> packageNamesToScan = ArrayUtils.isEmpty(getMapperPackages()) ? environment.getPackages()
        : Arrays.asList(getMapperPackages());
    packageNamesToScan.forEach(mapperPackage -> environment.scan(Mapper.class, mapperPackage).forEach(mappers::add));
    return Collections.unmodifiableCollection(mappers);
  }

  /**
   * Find mapper xml URLs for the current configuration.
   *
   * @return The mapper xml URLs
   */
  Collection<URL> findMapperXmlFiles() {
    Collection<ResourceLoader> resourceLoaders = ArrayUtils.isEmpty(getMapperXmlBasePaths())
        ? Collections.singletonList(environment)
        : Arrays.stream(getMapperXmlBasePaths()).map(environment::forBase).collect(Collectors.toList());
    Set<String> resolvedXmlFiles = new HashSet<>();
    if (ArrayUtils.isNotEmpty(getMapperXmlFiles())) {
      Collection<URL> files = resourceLoaders.stream()
          .flatMap(resourceLoader -> Arrays.stream(getMapperXmlFiles()).map(xmlFile -> {
            Optional<URL> url = resourceLoader.getResource(xmlFile);
            if (url.isPresent()) {
              resolvedXmlFiles.add(xmlFile);
            }
            return url.orElse(null);
          }).filter(Objects::nonNull)).collect(Collectors.toSet());
      if (getMapperXmlFiles().length != resolvedXmlFiles.size()) {
        throw new IllegalArgumentException(
            "Does not exists "
                + Arrays.stream(getMapperXmlFiles()).filter(xmlFile -> !resolvedXmlFiles.contains(xmlFile))
                    .collect(Collectors.toSet())
                + " in either " + Arrays.asList(getMapperXmlBasePaths()) + " on your classpath.");
      }
      return Collections.unmodifiableCollection(files);
    }
    return Collections.emptySet();
  }

}
