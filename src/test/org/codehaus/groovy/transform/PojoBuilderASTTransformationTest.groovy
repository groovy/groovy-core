/*
 * Copyright 2008-2012 the original author or authors.
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
 * @author Marcin Grzejszczak
 */
class PojoBuilderASTTransformationTest extends GroovyShellTestCase {

    void testBuilder() {
        def personBuilder = evaluate("""
            import groovy.transform.PojoBuilder

            class Person {
                String firstName
                String surName
            }            

            @PojoBuilder(forClass = Person)
            class PersonBuilder {
                
            }
            
            return new PersonBuilder()
        """)
        def person = personBuilder.withFirstName("Robert").withSurName("Lewandowski").build()
        assert person.firstName == "Robert"
        assert person.surName == "Lewandowski"
    }
    
    void testBuilderSettingValuesThroughFields() {
        def personBuilder = evaluate("""
            import groovy.transform.PojoBuilder

            class Person {
                private String firstName
                private String surName
            }            

            @PojoBuilder(forClass = Person)
            class PersonBuilder {
                
            }
            
            return new PersonBuilder()
        """)
        def person = personBuilder.withFirstName("Robert").withSurName("Lewandowski").build()
        assert person.firstName == "Robert"
        assert person.surName == "Lewandowski"
    }
    
    void testBuilderWithInclude() {
        def personBuilder = evaluate("""
            import groovy.transform.PojoBuilder

            class Person {
                String firstName
                String surName
            }            

            @PojoBuilder(forClass = Person, includes = ['firstName'])
            class PersonBuilder {
                
            }
            
            return new PersonBuilder()
        """)
        def person = personBuilder.withFirstName("Robert").build()
        assert person.firstName == "Robert"
        assert personBuilder.metaClass.methods.find { it.name == "withSurName"} == null 
        assert personBuilder.metaClass.methods.find { it.name == "withFirstName"} != null 
    }
    
    void testBuilderWithExclude() {
        def personBuilder = evaluate("""
            import groovy.transform.PojoBuilder

            class Person {
                String firstName
                String surName
            }            

            @PojoBuilder(forClass = Person, excludes = ['surName'])
            class PersonBuilder {
                
            }
            
            return new PersonBuilder()
        """)
        def person = personBuilder.withFirstName("Robert").build()
        assert person.firstName == "Robert"
        assert personBuilder.metaClass.methods.find { it.name == "withSurName"} == null 
        assert personBuilder.metaClass.methods.find { it.name == "withFirstName"} != null 
    }


    void testBuilderWithValidation() {
        def personBuilder = evaluate("""
            import groovy.transform.PojoBuilder

            class Person {
                String firstName
                String surName
            }            

            @PojoBuilder(forClass = Person)
            class PersonBuilder {
                
            }
            
            return new PersonBuilder()
        """)
        try {
            def person = personBuilder.withFirstName("Robert").build {
                if (it.surName == null ){
                    throw new IllegalStateException()
                }
            }
            fail("should fail due to validation closure")
        } catch(Exception exception ) {
            
        }
    }

    void testBuilderWithPrimitiveFields() {
        def primitivePojoBuilder = evaluate("""
            import groovy.transform.PojoBuilder

            class PrimitivePojo {
                int intField
                boolean booleanField
            }

            @PojoBuilder(forClass = PrimitivePojo)
            class PrimitivePojoBuilder {

            }

            return new PrimitivePojoBuilder()
        """)
        def person = primitivePojoBuilder.withIntField(5).build()
        assert person.intField == 5
        assert person.booleanField == false
    }
}
