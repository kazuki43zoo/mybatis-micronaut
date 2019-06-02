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
package org.mybatis.micronaut.sample;

import javax.inject.Inject;

import io.micronaut.configuration.picocli.PicocliRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.micronaut.sample.domain.City;
import org.mybatis.micronaut.sample.mapper.CityMapper;
import picocli.CommandLine;

@CommandLine.Command(name = "mybatis-micronaut-sample", mixinStandardHelpOptions = true)
public class SampleApplication implements Runnable {

  @Inject
  private SqlSessionFactory sqlSessionFactory;

  public static void main(String[] args) throws Exception {
    PicocliRunner.run(SampleApplication.class, args);
  }

  @Override
  public void run() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      CityMapper mapper = sqlSession.getMapper(CityMapper.class);
      City city = mapper.findByState("CA");
      System.out.println(city);
    }
  }

}
