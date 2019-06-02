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

import extensions.CaptureSystemOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@extensions.CaptureSystemOutput
class SampleApplicationTest {

  @Test
  void test(CaptureSystemOutput.OutputCapture outputCapture) throws Exception {
    SampleApplication.main(new String[] {});
    String output = outputCapture.toString();
    Assertions.assertTrue(output.contains("1,San Francisco,CA,US"));
    Assertions.assertTrue(output.contains("2,Tokyo,13,JP"));
    Assertions.assertTrue(output.contains("3,Chiba,12,JP"));
    Assertions.assertTrue(output.contains("Catch the org.apache.ibatis.exceptions.PersistenceException."));
    Assertions.assertTrue(output.contains("Yokohama = null"));
    Assertions.assertTrue(output.contains("Saitama = null"));
  }

}
