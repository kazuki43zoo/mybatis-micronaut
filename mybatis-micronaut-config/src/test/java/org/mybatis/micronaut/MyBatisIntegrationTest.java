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
import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.test.annotation.MicronautTest;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.micronaut.service.CityService;

@MicronautTest(environments = "itest")
class MyBatisIntegrationTest {

  @Inject
  CityService cityService;

  @Inject
  DataSource dataSource;

  @BeforeEach
  void setupDatabase() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      ScriptRunner scriptRunner = new ScriptRunner(connection);
      scriptRunner.runScript(new InputStreamReader(ClassPathResourceLoader.defaultLoader(getClass().getClassLoader())
          .getResourceAsStream("create-db.sql").get()));
    }
  }

  @Test
  void selectDatabaseName() {
    String databaseName = cityService.getDatabaseName();
    Assertions.assertEquals("ITEST", databaseName);
  }

  @Test
  void create() throws SQLException {
    cityService.create("ASIA", "JAPAN", "TOKYO", () -> {
    });

    try (Connection connection = dataSource.getConnection();
        ResultSet regionResultSet = connection.createStatement().executeQuery("SELECT * FROM region WHERE id = 1");
        ResultSet countryResultSet = connection.createStatement().executeQuery("SELECT * FROM country WHERE id = 1");
        ResultSet cityResultSet = connection.createStatement().executeQuery("SELECT * FROM city WHERE id = 1")) {
      Assertions.assertTrue(regionResultSet.next());
      Assertions.assertEquals("ASIA", regionResultSet.getString("NAME"));
      Assertions.assertTrue(countryResultSet.next());
      Assertions.assertEquals("JAPAN", countryResultSet.getString("NAME"));
      Assertions.assertTrue(cityResultSet.next());
      Assertions.assertEquals("TOKYO", cityResultSet.getString("NAME"));
    }
  }

  @Test
  void createOnError() throws SQLException {
    Assertions.assertThrows(IllegalStateException.class, () -> cityService.create("ASIA", "JAPAN", "TOKYO", () -> {
      throw new IllegalStateException("test!");
    }));
    try (Connection connection = dataSource.getConnection();
        ResultSet regionResultSet = connection.createStatement().executeQuery("SELECT * FROM region");
        ResultSet countryResultSet = connection.createStatement().executeQuery("SELECT * FROM country");
        ResultSet cityResultSet = connection.createStatement().executeQuery("SELECT * FROM city")) {
      Assertions.assertFalse(regionResultSet.next());
      Assertions.assertFalse(countryResultSet.next());
      Assertions.assertFalse(cityResultSet.next());
    }
  }

}
