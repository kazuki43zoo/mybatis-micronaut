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
package org.mybatis.micronaut.service;

import javax.inject.Singleton;

import io.micronaut.spring.tx.annotation.Transactional;
import org.mybatis.micronaut.mapper.city.CityMapper;
import org.mybatis.micronaut.mapper.country.CountryMapper;
import org.mybatis.micronaut.mapper.region.RegionMapper;
import org.springframework.transaction.annotation.Propagation;

@Singleton
@Transactional
public class CityService {

  private final RegionMapper regionMapper;
  private final CountryMapper countryMapper;
  private final CityMapper cityMapper;

  CityService(RegionMapper regionMapper, CountryMapper countryMapper, CityMapper cityMapper) {
    this.regionMapper = regionMapper;
    this.countryMapper = countryMapper;
    this.cityMapper = cityMapper;
  }

  public String getDatabaseName() {
    return regionMapper.selectDatabaseName();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void create(String region, String country, String city, Runnable anyProcessing) {
    regionMapper.insert(region);
    countryMapper.insert(country);
    cityMapper.insert(city);
    anyProcessing.run();
  }

}
