/*
 * Copyright 2003-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.transform

import gls.CompilableTestSupport

class SortableTransformTest extends CompilableTestSupport {
    void testSortable() {
        assertScript '''
            import groovy.transform.*

            @Sortable @ToString class Person {
              String first
              String last
              Integer born
            }

            def people = [
              new Person(first: 'Johnny', last: 'Depp', born: 1963),
              new Person(first: 'Keira', last: 'Knightley', born: 1985),
              new Person(first: 'Geoffrey', last: 'Rush', born: 1951),
              new Person(first: 'Orlando', last: 'Bloom', born: 1977)
            ]

            assert people.sort()*.last == ['Rush', 'Depp', 'Knightley', 'Bloom']
            assert people.sort(false, Person.comparatorByFirst())*.first == ['Geoffrey', 'Johnny', 'Keira', 'Orlando']
            assert people.sort(false, Person.comparatorByLast())*.last == ['Bloom', 'Depp', 'Knightley', 'Rush']
            assert people.sort(false, Person.comparatorByBorn())*.last == ['Rush', 'Depp', 'Bloom', 'Knightley']
        '''
    }
}
