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
class AutoCloneTransformTest extends GroovyShellTestCase {

    void testOk() {
        assertScript """
                import groovy.transform.AutoClone

                @AutoClone
                class Person {
                    String first, last
                    List favItems
                    Date since
                }

                def p = new Person(first:'John', last:'Smith', favItems:['ipod', 'shiraz'], since:new Date())
                def p2 = p.clone()

                assert p instanceof Cloneable
                assert p.favItems instanceof Cloneable
                assert p.since instanceof Cloneable
                assert !(p.first instanceof Cloneable)

                assert !p.is(p2)
                assert !p.favItems.is(p2.favItems)
                assert !p.since.is(p2.since)
                assert p.first.is(p2.first)
            """
    }

    // Original behavior: If property names are not checked, and an invalid property name is given in 'excludes',
    // the property is not excluded.
    void testExcludesWithInvalidPropertyWithoutCheckIgnoresProperty() {
        assertScript """
                import groovy.transform.AutoClone

                @AutoClone(excludes='sirName', checkPropertyNames=false)
                class Person {
                    String firstName
                    String surName
                }

                def p = new Person(firstName: "John", surName: "Doe")
                def p2 = p.clone()

                assert p2.firstName == "John"
                assert p2.surName == "Doe"
            """
    }

    void testExcludesWithInvalidPropertyNameResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.AutoClone

                    @AutoClone(excludes='sirName')
                    class Person {
                        String firstName
                        String surName
                    }

                    new Person(firstName: "John", surName: "Doe").clone()
                """)
        }
        assert message.contains("Error during @AutoClone processing: 'excludes' property 'sirName' does not exist.")
    }

}
