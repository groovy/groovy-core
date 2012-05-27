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
package groovy.transform;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class annotation used to assist in creating appropriate {@code equals()} and {@code hashCode()} methods.
 * <p/>
 * It allows you to write classes in this shortened form:
 * <pre>
 * import groovy.transform.EqualsAndHashCode
 * {@code @EqualsAndHashCode}
 * class Person {
 *     String first, last
 *     int age
 * }
 * def p1 = new Person(first:'John', last:'Smith', age:21)
 * def p2 = new Person(first:'John', last:'Smith', age:21)
 * assert p1 == p2
 * def map = [:]
 * map[p1] = 45
 * assert map[p2] == 45
 * </pre>
 * The {@code @EqualsAndHashCode} annotation instructs the compiler to execute an
 * AST transformation which adds the necessary equals and hashCode methods to the class.
 * <p/>
 * The {@code hashCode()} method is calculated using Groovy's {@code HashCodeHelper} class
 * which implements an algorithm similar to the one outlined in the book <em>Effective Java</em>.
 * <p/>
 * The {@code equals()} method compares the values of the individual properties (and optionally fields)
 * of the class.  It can also optionally call equals on the super class. Two different equals method
 * implementations are supported both of which support the equals contract outlined in the javadoc
 * for <code>java.lang.Object</code>
 * <p/>
 * To illustrate the 'canEqual' implementation style (see http://www.artima.com/lejava/articles/equality.html
 * for further details), consider this class:
 * <pre>
 * {@code @EqualsAndHashCode}
 * class IntPair {
 *     int x, y
 * }
 * </pre>
 * The generated <code>equals</code> and <code>canEqual</code> methods will be something like below:
 * <pre>
 * public boolean equals(java.lang.Object other)
 *     if (other == null) return false
 *     if (this.is(other)) return true
 *     if (!(other instanceof IntPair)) return false
 *     if (!other.canEqual(this)) return false
 *     if (x != other.x) return false
 *     if (y != other.y) return false
 *     return true
 * }
 *
 * public boolean canEqual(java.lang.Object other) {
 *     return other instanceof IntPair
 * }
 * </pre>
 * If no further options are specified, this is the default style for {@code @Canonical} and
 * {@code @EqualsAndHashCode} annotated classes. The advantage of this style is that it allows inheritance
 * to be used in limited cases where its purpose is for overriding implementation details rather than
 * creating a derived type with different behavior. This is useful when using JPA Proxies for example or
 * as shown in the following examples:
 * <pre>
 * {@code @Canonical} class IntPair { int x, y }
 * def p1 = new IntPair(1, 2)
 *
 * // overriden getter but deemed an IntPair as far as domain is concerned
 * def p2 = new IntPair(1, 1) { int getY() { 2 } }
 *
 * // additional helper method added through inheritance but
 * // deemed an IntPair as far as our domain is concerned
 * {@code @InheritConstructors} class IntPairWithSum extends IntPair {
 *     def sum() { x + y }
 * }
 *
 * def p3 = new IntPairWithSum(1, 2)
 *
 * assert p1 == p2 && p2 == p1
 * assert p1 == p3 && p3 == p1
 * assert p3 == p2 && p2 == p3
 * </pre>
 * Note that if you create any domain classes which don't have exactly the
 * same contract as <code>IntPair</code> then you should provide an appropriate
 * <code>equals</code> and <code>canEqual</code> method. The easiest way to
 * achieve this would be to use the {@code @Canonical} or
 * {@code @EqualsAndHashCode} annotations as shown below:
 * <pre>
 * {@code @EqualsAndHashCode}
 * {@code @TupleConstructor(includeSuperProperties=true)}
 * class IntTriple extends IntPair { int z }
 * def t1 = new IntTriple(1, 2, 3)
 * assert p1 != t1 && p2 != t1 && t1 != p3
 * </pre>
 *
 * The alternative supported style regards any kind of inheritance as creation of
 * a new type and is illustrated in the following example:
 * <pre>
 * {@code @EqualsAndHashCode(useCanEqual=false)}
 * class IntPair {
 *     int x, y
 * }
 * </pre>
 * The generated equals method will be something like below:
 * <pre>
 * public boolean equals(java.lang.Object other)
 *     if (other == null) return false
 *     if (this.is(other)) return true
 *     if (IntPair != other.getClass()) return false
 *     if (x != other.x) return false
 *     if (y != other.y) return false
 *     return true
 * }
 * </pre>
 * This style is appropriate for final classes (where inheritance is not
 * allowed) which have only <code>java.lang.Object</code> as a super class.
 * Most {@code @Immutable} classes fall in to this category. For such classes,
 * there is no need to introduce the <code>canEqual()</code> method.
 * <p/>
 * Note that if you explicitly set <code>useCanEqual=false</code> for child nodes
 * in a class hierarchy but have it <code>true</code> for parent nodes and you
 * also have <code>callSuper=true</code> in the child, then your generated
 * equals methods will not strictly follow the equals contract.
 * <p/>
 * Note that when used in the recommended fashion, the two implementations supported adhere
 * to the equals contract. You can provide your own equivalence relationships if you need,
 * e.g. for comparing instances of the <code>IntPair</code> and <code>IntTriple</code> classes
 * discussed earlier, you could provide the following method in <code>IntPair</code>:
 * <pre>
 * boolean hasEqualXY(other) { other.x == getX() && other.y == getY() }
 * </pre>
 * Then for the objects defined earlier, the following would be true:
 * <pre>
 * assert p1.hasEqualXY(t1) && t1.hasEqualXY(p1)
 * assert p2.hasEqualXY(t1) && t1.hasEqualXY(p2)
 * assert p3.hasEqualXY(t1) && t1.hasEqualXY(p3)
 * </pre>
 * There is also support for including or excluding fields/properties by name when constructing
 * the equals and hashCode methods as shown here:
 * <pre>
 * import groovy.transform.*
 * {@code @EqualsAndHashCode}(excludes="z")
 * {@code @TupleConstructor}
 * public class Point2D {
 *     int x, y, z
 * }
 *
 * assert  new Point2D(1, 1, 1).equals(new Point2D(1, 1, 2))
 * assert !new Point2D(1, 1, 1).equals(new Point2D(2, 1, 1))
 *
 * {@code @EqualsAndHashCode}(excludes=["y", "z"])
 * {@code @TupleConstructor}
 * public class Point2D {
 *     int x, y, z
 * }
 *
 * assert  new Point1D(1, 1, 1).equals(new Point1D(1, 1, 2))
 * assert  new Point1D(1, 1, 1).equals(new Point1D(1, 2, 1))
 * assert !new Point1D(1, 1, 1).equals(new Point1D(2, 1, 1))
 * </pre>
 *
 * @see org.codehaus.groovy.util.HashCodeHelper
 * @author Paul King
 * @since 1.8.0
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.codehaus.groovy.transform.EqualsAndHashCodeASTTransformation")
public @interface EqualsAndHashCode {
    /**
     * List of field and/or property names to exclude from the equals and hashCode calculations.
     * Must not be used if 'includes' is used. For convenience, a String with comma separated names
     * can be used in addition to an array (using Groovy's literal list notation) of String values.
     */
    String[] excludes() default {};

    /**
     * List of field and/or property names to include within the equals and hashCode calculations.
     * Must not be used if 'excludes' is used. For convenience, a String with comma separated names
     * can be used in addition to an array (using Groovy's literal list notation) of String values.
     */
    String[] includes() default {};

    /**
     * Whether to include super in equals and hashCode calculations.
     */
    boolean callSuper() default false;

    /**
     * Include fields as well as properties in equals and hashCode calculations.
     */
    boolean includeFields() default false;

    /**
     * Generate a canEqual method to be used by equals.
     */
    boolean useCanEqual() default true;
}
