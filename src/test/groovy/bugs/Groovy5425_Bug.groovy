/*
 * Copyright 2003-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.bugs

/**
 * Check Range.size finishes in a timely fashion. If we get a regression on this bug,
 * the build will take 10 - 20 minutes longer than normal
 *
 * @author Tim Yates
 */
class Groovy5425_Bug extends GroovyTestCase {

  void testBigDecimalRangeSize() {
    assert (1.0G..2147483647.0G).size() == 2147483647
  }

  void testBigIntegerRangeSize() {
    assert (1G..2147483647G).size() == 2147483647
  }
}
