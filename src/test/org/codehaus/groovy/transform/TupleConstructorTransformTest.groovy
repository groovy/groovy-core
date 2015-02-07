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
class TupleConstructorTransformTest extends GroovyShellTestCase {

    void testOk() {
        assertScript """
                import groovy.transform.TupleConstructor

                @TupleConstructor
                class Person {
                    String firstName
                    String lastName
                }

                def p = new Person("John", "Doe")
                assert p.firstName == "John"
                assert p.lastName == "Doe"
            """
    }

    void testIncludesAndExcludesTogetherResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.TupleConstructor

                    @TupleConstructor(includes='surName', excludes='surName')
                    class Person {
                        String surName
                    }

                    new Person("Doe")
                """)
        }
        assert message.contains("Error during @TupleConstructor processing: Only one of 'includes' and 'excludes' should be supplied not both.")
    }

    // Original behavior: If property names are not checked, and an invalid property name is given in 'includes',
    // the property is not included.
    void testIncludesWithInvalidPropertyWithoutCheckIgnoresProperty() {
        assertScript """
                import groovy.transform.TupleConstructor

                @TupleConstructor(includes='sirName', checkPropertyNames=false)
                class Person {
                    String firstName
                    String surName
                }

                new Person()
            """
    }

    // Original behavior: If property names are not checked, and an invalid property is given in 'excludes',
    // the property is not excluded.
    void testExcludesWithInvalidPropertyWithoutCheckIgnoresProperty() {
        assertScript """
                import groovy.transform.TupleConstructor

                @TupleConstructor(excludes='sirName', checkPropertyNames=false)
                class Person {
                    String firstName
                    String surName
                }

                def p = new Person("John", "Doe")
                assert p.firstName == "John"
                assert p.surName == "Doe"
            """
    }

    void testIncludesWithInvalidPropertyNameResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.TupleConstructor

                    @TupleConstructor(includes='sirName')
                    class Person {
                        String firstName
                        String surName
                    }

                    def p = new Person("John", "Doe")
                """)
        }
        assert message.contains("Error during @TupleConstructor processing: 'includes' property 'sirName' does not exist.")
    }

    void testExcludesWithInvalidPropertyNameResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.TupleConstructor

                    @TupleConstructor(excludes='sirName')
                    class Person {
                        String firstName
                        String surName
                    }

                    def p = new Person("John", "Doe")
                """)
        }
        assert message.contains("Error during @TupleConstructor processing: 'excludes' property 'sirName' does not exist.")
    }

}
