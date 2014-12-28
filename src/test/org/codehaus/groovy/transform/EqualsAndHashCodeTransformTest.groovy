/*
 * Copyright 2008-2014 the original author or authors.
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
package org.codehaus.groovy.transform

/**
 * @author John Hurst
 */
class EqualsAndHashCodeTransformTest extends GroovyShellTestCase {

    void testOk() {
        assertScript """
                import groovy.transform.EqualsAndHashCode
                @EqualsAndHashCode
                class Person {
                    String first, last
                    int age
                }

                def p1 = new Person(first:'John', last:'Smith', age:21)
                def p2 = new Person(first:'John', last:'Smith', age:21)
                assert p1 == p2
                def map = [:]
                map[p1] = 45
                assert map[p2] == 45
            """
    }

    void testIncludesAndExcludesTogetherResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.EqualsAndHashCode

                    @EqualsAndHashCode(includes='surName', excludes='surName')
                    class Person {
                        String surName
                    }

                    new Person(surName: "Doe")
                """)
        }
        assert message.contains("Error during @EqualsAndHashCode processing: Only one of 'includes' and 'excludes' should be supplied not both.")
    }

    // Original behavior: If property names are not checked, and an invalid property is given in 'includes',
    // the property is not included.
    void testIncludesWithInvalidPropertyWithoutCheckIgnoresProperty() {
        assertScript """
                import groovy.transform.EqualsAndHashCode

                @EqualsAndHashCode(includes='sirName', checkPropertyNames=false)
                class Person {
                    String firstName
                    String surName
                }

                assert new Person(firstName: "John", surName: "Doe") == new Person(firstName: "Jack", surName: "Smith")
            """
    }

    // Original behavior: If property names are not checked, and an invalid property is given in 'excludes',
    // the property is not excluded.
    void testExcludesWithInvalidPropertyWithoutCheckIgnoresProperty() {
        assertScript """
                import groovy.transform.EqualsAndHashCode

                @EqualsAndHashCode(excludes='sirName', checkPropertyNames=false)
                class Person {
                    String firstName
                    String surName
                }

                assert new Person(firstName: "John", surName: "Doe") != new Person(firstName: "John", surName: "Smith")
            """
    }

    void testIncludesWithInvalidPropertyNameResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.EqualsAndHashCode

                    @EqualsAndHashCode(includes='sirName')
                    class Person {
                        String surName
                    }

                    new Person(surName: "Doe")
                """)
        }
        assert message.contains("Error during @EqualsAndHashCode processing: 'includes' property 'sirName' does not exist.")
    }

    void testExcludesWithInvalidPropertyNameResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.EqualsAndHashCode

                    @EqualsAndHashCode(excludes='sirName')
                    class Person {
                        String firstName
                        String surName
                    }

                    new Person(firstName: "John", surName: "Doe")
                """)
        }
        assert message.contains("Error during @EqualsAndHashCode processing: 'excludes' property 'sirName' does not exist.")
    }


}
