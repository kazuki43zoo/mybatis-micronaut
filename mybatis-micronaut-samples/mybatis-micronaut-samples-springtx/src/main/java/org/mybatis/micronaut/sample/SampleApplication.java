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
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.micronaut.sample.domain.City;
import org.mybatis.micronaut.sample.service.CityService;
import picocli.CommandLine;

@CommandLine.Command(name = "mybatis-micronaut-sample", mixinStandardHelpOptions = true)
public class SampleApplication implements Runnable {

  @Inject
  private CityService cityService;

  public static void main(String[] args) throws Exception {
    PicocliRunner.run(SampleApplication.class, args);
  }

  @Override
  public void run() {
    {
      City city = cityService.getCityByState("CA");
      System.out.println(city);
    }

    {
      City tokyo = new City();
      tokyo.setName("Tokyo");
      tokyo.setState("13");
      tokyo.setCountry("JP");

      City chiba = new City();
      chiba.setName("Chiba");
      chiba.setState("12");
      chiba.setCountry("JP");

      cityService.createCities(tokyo, chiba);

      System.out.println(cityService.getCityById(tokyo.getId()));
      System.out.println(cityService.getCityById(chiba.getId()));
    }

    {
      City yokohama = new City();
      yokohama.setName("Yokohama");
      yokohama.setState("14");
      yokohama.setCountry("JP");

      City saitama = new City();
      saitama.setName(null);
      saitama.setState("11");
      saitama.setCountry("JP");
      try {
        cityService.createCities(yokohama, saitama);
      } catch (PersistenceException e) {
        System.out.println("Catch the " + e.getClass().getName() + ". message : [" + e.getMessage() + "]");
        System.out.println("Yokohama = " + cityService.getCityById(yokohama.getId()));
        System.out.println("Saitama = " + cityService.getCityById(saitama.getId()));
      }
    }
  }

}
