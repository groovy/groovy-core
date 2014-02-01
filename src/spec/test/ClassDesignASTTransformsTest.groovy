/*
 * Copyright 2003-2013 the original author or authors.
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



class ClassDesignASTTransformsTest extends GroovyTestCase {

    void testDelegateTransformation() {
        assertScript '''
// tag::delegating_class[]
class Event {
    @Delegate Date when
    String title
}
// end::delegating_class[]

/*
// tag::delegating_class_generated[]
class Event {
    Date when
    String title
    boolean before(Date other) {
        when.before(other)
    }
    // ...
}
// end::delegating_class_generated[]
*/

// tag::delegation_assert[]
def ev = new Event(title:'Groovy keynote', when: Date.parse('yyyy/MM/dd', '2013/09/10'))
def now = new Date()
assert ev.before(now)
// end::delegation_assert[]
'''
        assertScript '''
// tag::delegate_example_interfaces[]
    interface Greeter { void sayHello() }
    class MyGreeter implements Greeter { void sayHello() { println 'Hello!'} }

    class DelegatingGreeter { // no explicit interface
        @Delegate MyGreeter greeter = new MyGreeter()
    }
    def greeter = new DelegatingGreeter()
    assert greeter instanceof Greeter // interface was added transparently
// end::delegate_example_interfaces[]
'''
        assertScript '''
// tag::delegate_deprecated_header[]
class WithDeprecation {
    @Deprecated
    void foo() {}
}
class WithoutDeprecation {
    @Deprecated
    void bar() {}
}
class Delegating {
    @Delegate(deprecated=true) WithDeprecation with = new WithDeprecation()
    @Delegate WithoutDeprecation without = new WithoutDeprecation()
}
def d = new Delegating()
d.foo() // passes thanks to deprecated=true
// end::delegate_deprecated_header[]
try {
// tag::delegate_deprecated_footer[]
d.bar() // fails because of @Deprecated
// end::delegate_deprecated_footer[]
} catch (e ) {}
'''

        assertScript '''
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD])
@interface Transactional {}
// tag::delegate_example_annotations[]
class WithAnnotations {
    @Transactional
    void method() {
    }
}
class DelegatingWithoutAnnotations {
    @Delegate WithAnnotations delegate
}
class DelegatingWithAnnotations {
    @Delegate(methodAnnotations = true) WithAnnotations delegate
}
def d1 = new DelegatingWithoutAnnotations()
def d2 = new DelegatingWithAnnotations()
assert d1.class.getDeclaredMethod('method').annotations.length==0
assert d2.class.getDeclaredMethod('method').annotations.length==1
// end::delegate_example_annotations[]
        '''

        assertScript '''
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.PARAMETER])
@interface NotNull {}
// tag::delegate_example_parameter_annotations[]
class WithAnnotations {
    void method(@NotNull String str) {
    }
}
class DelegatingWithoutAnnotations {
    @Delegate WithAnnotations delegate
}
class DelegatingWithAnnotations {
    @Delegate(parameterAnnotations = true) WithAnnotations delegate
}
def d1 = new DelegatingWithoutAnnotations()
def d2 = new DelegatingWithAnnotations()
assert d1.class.getDeclaredMethod('method',String).parameterAnnotations[0].length==0
assert d2.class.getDeclaredMethod('method',String).parameterAnnotations[0].length==1
// end::delegate_example_parameter_annotations[]
        '''

        assertScript '''
// tag::delegate_example_excludes_header[]
class Worker {
    void task1() {}
    void task2() {}
}
class Delegating {
    @Delegate(excludes=['task2']) Worker worker = new Worker()
}
def d = new Delegating()
d.task1() // passes
// end::delegate_example_excludes_header[]
try {
// tag::delegate_example_excludes_footer[]
d.bar() // fails because method is excluded
// end::delegate_example_excludes_footer[]
} catch (e ) {}

'''

        assertScript '''
// tag::delegate_example_includes_header[]
class Worker {
    void task1() {}
    void task2() {}
}
class Delegating {
    @Delegate(includes=['task1']) Worker worker = new Worker()
}
def d = new Delegating()
d.task1() // passes
// end::delegate_example_includes_header[]
try {
// tag::delegate_example_includes_footer[]
d.bar() // fails because method is not included
// end::delegate_example_includes_footer[]
} catch (e ) {}

'''
    }

    void testImmutable() {
        assertScript '''
// tag::immutable_simple[]
import groovy.transform.Immutable

@Immutable
class Point {
    int x
    int y
}
// end::immutable_simple[]

def p = new Point(x:1,y:2)
assert p.toString() == 'Point(1, 2)' // @ToString equivalent
try {
    p.x = 2
} catch (ReadOnlyPropertyException rope) {
    println 'ReadOnly property'
}
'''

        assertScript '''
// tag::immutable_example_knownimmutableclasses[]
import groovy.transform.Immutable
import groovy.transform.TupleConstructor

@TupleConstructor
final class Point {
    final int x
    final int y
    public String toString() { "($x,$y)" }
}

@Immutable(knownImmutableClasses=[Point])
class Triangle {
    Point a,b,c
}
// end::immutable_example_knownimmutableclasses[]

def p = new Triangle(a:[47,22], b:[12,12],c:[88,17])
assert p.toString() == 'Triangle((47,22), (12,12), (88,17))' // @ToString equivalent
try {
    p.a = [0,0]
} catch (ReadOnlyPropertyException rope) {
    println 'ReadOnly property'
}
'''


        assertScript '''
// tag::immutable_example_knownimmutables[]
import groovy.transform.Immutable
import groovy.transform.TupleConstructor

@TupleConstructor
final class Point {
    final int x
    final int y
    public String toString() { "($x,$y)" }
}

@Immutable(knownImmutables=['a','b','c'])
class Triangle {
    Point a,b,c
}
// end::immutable_example_knownimmutables[]

def p = new Triangle(a:[47,22], b:[12,12],c:[88,17])
assert p.toString() == 'Triangle((47,22), (12,12), (88,17))' // @ToString equivalent
try {
    p.a = [0,0]
} catch (ReadOnlyPropertyException rope) {
    println 'ReadOnly property'
}
'''

        assertScript '''
// tag::immutable_example_copyWith[]
import groovy.transform.Immutable

@Immutable( copyWith=true )
class User {
    String  name
    Integer age
}

def bob   = new User( 'bob', 43 )
def alice = bob.copyWith( name:'alice' )
assert alice.name == 'alice'
assert alice.age  == 43
// end::immutable_example_copyWith[]

try {
    bob.name = 'alice'
} catch (ReadOnlyPropertyException rope) {
    println 'ReadOnly property'
}
'''
    }

    void testMemoized() {
        assertScript '''
import groovy.transform.Memoized

// tag::memoized_long_computation[]
long longComputation(int seed) {
    // slow computation
    Thread.sleep(1000*seed)
    System.nanoTime()
}
// end::memoized_long_computation[]
// tag::memoized_long_computation_asserts[]
def x = longComputation(1)
def y = longComputation(1)
assert x!=y
// end::memoized_long_computation_asserts[]
'''

        assertScript '''
import groovy.transform.Memoized

// tag::memoized_long_computation_cached[]
@Memoized
long longComputation(int seed) {
    // slow computation
    Thread.sleep(1000*seed)
    System.nanoTime()
}

def x = longComputation(1) // returns after 1 second
def y = longComputation(1) // returns immediatly
def z = longComputation(2) // returns after 2 seconds
assert x==y
assert x!=z
// end::memoized_long_computation_cached[]
'''
    }

    void testSingleton() {
        assertScript '''
// tag::singleton_simple[]
@Singleton
class GreetingService {
    String greeting(String name) { "Hello, $name!" }
}
assert GreetingService.instance.greeting('Bob') == 'Hello, Bob!'
// end::singleton_simple[]
'''

        assertScript '''
// tag::singleton_example_property[]
@Singleton(property='theOne')
class GreetingService {
    String greeting(String name) { "Hello, $name!" }
}

assert GreetingService.theOne.greeting('Bob') == 'Hello, Bob!'
// end::singleton_example_property[]
'''

        assertScript '''
// tag::singleton_example_lazy[]
class Collaborator {
    public static boolean init = false
}
@Singleton(lazy=true,strict=false)
class GreetingService {
    static void init() {}
    GreetingService() {
        Collaborator.init = true
    }
    String greeting(String name) { "Hello, $name!" }
}
GreetingService.init() // make sure class is initialized
assert Collaborator.init == false
GreetingService.instance
assert Collaborator.init == true
assert GreetingService.instance.greeting('Bob') == 'Hello, Bob!'
// end::singleton_example_lazy[]
'''
    }
}
