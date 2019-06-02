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

import javax.inject.Inject;
import java.util.Arrays;

import com.example.mapper.user.UserMapper;
import io.micronaut.test.annotation.MicronautTest;
import org.apache.ibatis.session.ExecutorType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.micronaut.mapper.region.RegionMapper;
import org.mybatis.micronaut.scripting.CustomLanguageDriver;
import org.mybatis.micronaut.scripting.MyLanguageDriver;
import org.mybatis.micronaut.typehandler.country.CountryTypeHandler;
import org.mybatis.micronaut.typehandler.region.RegionTypeHandler;

@MicronautTest(environments = "binding")
class MyBatisConfigurationTest {

  @Inject
  MyBatisConfiguration configuration;

  @Test
  void binding() {
    Assertions.assertEquals(2, configuration.getMapperPackages().length);
    Assertions.assertEquals(Arrays.asList("org.mybatis.micronaut.mapper.city", "org.mybatis.micronaut.mapper.country"),
        Arrays.asList(configuration.getMapperPackages()));
    Assertions.assertEquals(Arrays.asList(RegionMapper.class, UserMapper.class),
        Arrays.asList(configuration.getMappers()));
    Assertions.assertEquals(Arrays.asList("/META-INF/mappers/mail", "/META-INF/mappers/phone"),
        Arrays.asList(configuration.getMapperXmlBasePaths()));
    Assertions.assertEquals(Arrays.asList("MailMapper.xml", "PhoneMapper.xml"),
        Arrays.asList(configuration.getMapperXmlFiles()));

    Assertions.assertEquals(Arrays.asList("org.mybatis.micronaut.domain", "org.mybatis.micronaut.typehandler.city"),
        Arrays.asList(configuration.getTypeAliasPackages()));
    Assertions.assertEquals(Arrays.asList(CountryTypeHandler.class, RegionTypeHandler.class),
        Arrays.asList(configuration.getTypeAliases()));

    Assertions.assertEquals(
        Arrays.asList("org.mybatis.micronaut.typehandler.city", "org.mybatis.micronaut.typehandler.country"),
        Arrays.asList(configuration.getTypeHandlerPackages()));
    Assertions.assertEquals(Arrays.asList(RegionTypeHandler.class), Arrays.asList(configuration.getTypeHandlers()));

    Assertions.assertEquals(Arrays.asList(MyLanguageDriver.class, CustomLanguageDriver.class),
        Arrays.asList(configuration.getScriptingLanguageDrivers()));
    Assertions.assertEquals(MyLanguageDriver.class, configuration.getDefaultScriptingLanguageDriver());

    Assertions.assertEquals("itest", configuration.getDataSourceName());

    Assertions.assertEquals(ExecutorType.REUSE, configuration.getConfiguration().getDefaultExecutorType());
    Assertions.assertTrue(configuration.getConfiguration().isMapUnderscoreToCamelCase());
    Assertions.assertEquals("value1", configuration.getConfiguration().getVariables().getProperty("key1"));
    Assertions.assertEquals("value2", configuration.getConfiguration().getVariables().getProperty("key2"));
    Assertions.assertEquals(5, configuration.getConfiguration().getLazyLoadTriggerMethods().size());
    Assertions.assertTrue(configuration.getConfiguration().getLazyLoadTriggerMethods()
        .containsAll(Arrays.asList("equals", "clone", "hashCode", "toString", "getClass")));
  }

}
