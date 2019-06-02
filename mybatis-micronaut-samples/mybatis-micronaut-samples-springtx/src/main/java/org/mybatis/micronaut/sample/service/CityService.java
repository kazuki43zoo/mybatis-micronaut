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
package org.mybatis.micronaut.sample.service;

import javax.inject.Singleton;

import java.util.Arrays;

import io.micronaut.spring.tx.annotation.Transactional;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.micronaut.sample.domain.City;
import org.mybatis.micronaut.sample.mapper.CityMapper;

@Singleton
@Transactional
public class CityService {

  private final CityMapper cityMapper;
  private final SqlSession sqlSession;

  public CityService(CityMapper cityMapper, SqlSession sqlSession) {
    this.cityMapper = cityMapper;
    this.sqlSession = sqlSession;
  }

  public City getCityByState(String state) {
    return sqlSession.selectOne("org.mybatis.micronaut.sample.mapper.CityMapper.findByState", state);
  }

  public City getCityById(Long id) {
    return cityMapper.findById(id);
  }

  public void createCities(City... cities) {
    Arrays.stream(cities).forEach(cityMapper::insert);
  }

}
