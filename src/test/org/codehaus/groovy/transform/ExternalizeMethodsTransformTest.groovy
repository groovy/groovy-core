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
class ExternalizeMethodsTransformTest extends GroovyShellTestCase {


    void testOk() {
        assertScript """
                import groovy.transform.ExternalizeMethods
                @ExternalizeMethods
                class Person {
                  String first, last
                  List favItems
                  Date since
                }

                def p = new Person(first: "John", last: "Doe", favItems: ["one", "two"], since: Date.parse("yyyy-MM-dd", "2014-12-28"))

                def baos = new ByteArrayOutputStream()
                p.writeExternal(new ObjectOutputStream(baos))

                def p2 = new Person()
                p2.readExternal(new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())))

                assert p2.first == "John"
                assert p2.last == "Doe"
                assert p2.favItems == ["one", "two"]
                assert p2.since == Date.parse("yyyy-MM-dd", "2014-12-28")
            """
    }

    // Original behavior: If property names are not checked, and an invalid property is given in 'excludes',
    // the property is not excluded.
    void testExcludesWithInvalidPropertyWithoutCheckIgnoresProperty() {
        assertScript """
                import groovy.transform.ExternalizeMethods

                @ExternalizeMethods(excludes='sirName', checkPropertyNames=false)
                class Person {
                    String firstName
                    String surName
                }

                def p = new Person(firstName: "John", surName: "Doe")

                def baos = new ByteArrayOutputStream()
                p.writeExternal(new ObjectOutputStream(baos))

                def p2 = new Person()
                p2.readExternal(new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())))

                assert p2.firstName == "John"
                assert p2.surName == "Doe"
            """
    }

    void testExcludesWithInvalidPropertyNameResultsInError() {
        def message = shouldFail {
            evaluate("""
                    import groovy.transform.ExternalizeMethods

                    @ExternalizeMethods(excludes='sirName')
                    class Person {
                        String firstName
                        String surName
                    }

                    new Person(firstName: "John", surName: "Doe")
                """)
        }
        assert message.contains("Error during @ExternalizeMethods processing: 'excludes' property 'sirName' does not exist.")
    }

}
