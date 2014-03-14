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
package groovy.transform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

/**
 * Field annotation to automatically create "with" builder methods for the fields of a given class
 * <p>
 * If not defined otherwise by the includes, excludes property for each property of the picked class
 * a "with" method will be added to owner class at compile time.
 * 
 * The implementation of such automatically added methods is code which for a property of 
 * type T takes as an argument of type T, sets its value on the builder and then returns the builder itself  
 * <p>
 * As an example, consider this code:
 * <pre>
 * class Person {
 *     String firstName
 *     String surName
 * }
 *
 * {@code @PojoBuilder}(forClass = Person)
 * class PersonBuilder {
 *     
 * }
 * 
 * def person = new PersonBuilder().withFirstName("Robert").withSurName("Lewandowski").build()
 * assert person.firstName == "Robert"
 * assert person.surName == "Lewandowski"
 * </pre>
 *
 * In this example, the PersonBuilder class will have properties 
 * <pre>
 *     String firstName
 *     String surnName
 * </pre> 
 * added at compile time. What also will be added would be proper "with" methods:
 * <pre>
 *     PersonBuilder withFirstName(String firstName);
 *     PersonBuilder withSurName(String surName);
 * </pre>
 * 
 * The implementation of the {@code withFirstName(String firstName)} method will look like this:
 * <pre>
 *     public PersonBuilder withFirstName(String firstName) {
 *         this.firstName = firstName;
 *         return this;
 *     }
 * </pre>
 *
 * Additionally you are able to provide which of the fields you would like to include
 * 
 * <pre>
 *     {@code @PojoBuilder}(forClass = Person, includes = ['firstName'])
 *     class PersonBuilder {
 *     
 *     }
 *     def person = new PersonBuilder().withFirstName("Robert").build()
 *     assert person.firstName == "Robert"
 *     assert personBuilder.metaClass.methods.find { it.name == "withSurName"} == null
 *     assert personBuilder.metaClass.methods.find { it.name == "withFirstName"} != null
 * </pre>
 * 
 * you are also able to provide which of the fields you would like to exclude
 * 
 * <pre>
 *      {@code @PojoBuilder}(forClass = Person, excludes = ['surName'])
 *     class PersonBuilder {
 *         
 *     }
 *    def person = new PersonBuilder().withFirstName("Robert").build()
 *    assert person.firstName == "Robert"
 *    assert personBuilder.metaClass.methods.find { it.name == "withSurName"} == null 
 *    assert personBuilder.metaClass.methods.find { it.name == "withFirstName"} != null 
 * </pre>
 * 
 * {@code @PojoBuilder} can also be used in conjunction with {@code @Canonical}
 * 
 * @author Marcin Grzejszczak
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE})
@GroovyASTTransformationClass("org.codehaus.groovy.transform.PojoBuilderASTTransformation")
public @interface PojoBuilder {

	/**
	 * A class for which builder methods should be created
	 */
	Class forClass();

	/**
	 * List of field and/or property names to exclude from generated builder methods.
	 * Must not be used if 'includes' is used. For convenience, a String with comma separated names
	 * can be used in addition to an array (using Groovy's literal list notation) of String values.
	 */
	String[] excludes() default {};

	/**
	 * List of field and/or property names to include within the generated builder methods.
	 * Must not be used if 'excludes' is used. For convenience, a String with comma separated names
	 * can be used in addition to an array (using Groovy's literal list notation) of String values.
	 */
	String[] includes() default {};
}
