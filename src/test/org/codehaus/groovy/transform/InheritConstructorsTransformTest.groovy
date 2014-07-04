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
 * @author Paul King
 */
class InheritConstructorsTransformTest extends GroovyShellTestCase {

    void testStandardCase() {
        assertScript """
            import groovy.transform.InheritConstructors
            @InheritConstructors class CustomException extends RuntimeException { }
            def ce = new CustomException('foo')
            assert ce.message == 'foo'
        """
    }

    void testOverrideCase() {
        assertScript """
            import groovy.transform.InheritConstructors
            @InheritConstructors
            class CustomException2 extends RuntimeException {
                CustomException2() { super('bar') }
            }
            def ce = new CustomException2()
            assert ce.message == 'bar'
            ce = new CustomException2('foo')
            assert ce.message == 'foo'
        """
    }

    void testChainedCase() {
        assertScript """
            import groovy.transform.InheritConstructors
            @InheritConstructors
            class CustomException5 extends CustomException4 {}
            @InheritConstructors
            class CustomException3 extends RuntimeException {}
            @InheritConstructors
            class CustomException4 extends CustomException3 {}
            def ce = new CustomException5('baz')
            assert ce.message == 'baz'
        """
    }

    void testInnerClassUsage() {
        assertScript """
            import groovy.transform.InheritConstructors
            @InheritConstructors
            class Outer extends RuntimeException {
                @InheritConstructors
                class Inner extends RuntimeException {}
                @InheritConstructors
                static class StaticInner extends RuntimeException {}
                void test() {
                    assert new StaticInner('bar').message == 'bar'
                    assert new Inner('foo').message == 'foo'
                }
            }
            class Outer2 extends Outer {
                @InheritConstructors
                class Inner2 extends Outer.Inner {}
                void test() {
                    assert new Inner2('foobar').message == 'foobar'
                }
            }

            def o = new Outer('baz')
            assert o.message == 'baz'
            o.test()
            new Outer2().test()
        """
    }

    void testParametersWithGenericsAndCompileStatic_GROOVY6874() {
        assertScript """
            import groovy.transform.*
            import java.math.RoundingMode

            @CompileStatic
            abstract class BasePublisher<T, U> {
               final Deque<T> items
               private U mode
               BasePublisher(Deque<T> items) { this.items = items }
               BasePublisher(U mode) {
                  this.mode = mode
                  this.items = []
               }
               BasePublisher(Set<U> modes) { this(modes[0]) }
               void publish(T item) { items.addFirst(item) }
               void init(U mode) { this.mode = mode }
               String toString() { items.join('|') + "|" + mode.toString() }
            }

            @CompileStatic @InheritConstructors
            class OrderPublisher<V> extends BasePublisher<Integer, V> {
              static OrderPublisher make() {
                new OrderPublisher<RoundingMode>(new LinkedList<Integer>())
              }
              void foo() { publish(3) }
              void bar(V mode) { init(mode) }
              void baz() {
                new OrderPublisher<RoundingMode>(RoundingMode.UP)
                new OrderPublisher<RoundingMode>(new HashSet<RoundingMode>())
              }
            }

            def op = OrderPublisher.make()
            op.foo()
            op.bar(RoundingMode.DOWN)
            assert op.toString() == '3|DOWN'
        """
    }

    void testParametersWithGenericsAndCompileStatic_errors_GROOVY6874() {
        def message = shouldFail """
            import groovy.transform.*
            import java.math.RoundingMode

            @CompileStatic
            abstract class BasePublisher<T, U> {
               final Deque<T> items
               private U mode
               BasePublisher(Deque<T> items) { this.items = items }
               BasePublisher(U mode) {
                  this.mode = mode
                  this.items = []
               }
               BasePublisher(Set<U> modes) { this(modes[0]) }
               void publish(T item) { items.addFirst(item) }
               void init(U mode) { this.mode = mode }
               String toString() { items.join('|') + "|" + mode.toString() }
            }

            @CompileStatic @InheritConstructors
            class OrderPublisher<V> extends BasePublisher<Integer, V> {
              static OrderPublisher make() {
                new OrderPublisher<RoundingMode>(new LinkedList<String>())
              }
              void foo() { publish(3) }
              void bar(V mode) { init(mode) }
              void baz() {
                new OrderPublisher<RoundingMode>(new Date())
                new OrderPublisher<RoundingMode>(new HashSet<Date>())
              }
            }

            def op = OrderPublisher.make()
            op.foo()
            op.bar(RoundingMode.DOWN)
            assert op.toString() == '3|DOWN'
        """
        assert message.contains('Cannot call OrderPublisher <RoundingMode>#<init>(java.util.Deque <java.lang.Integer>) with arguments [java.util.LinkedList <String>]')
        assert message.contains('Cannot call OrderPublisher <RoundingMode>#<init>(java.math.RoundingMode) with arguments [java.util.Date]')
        assert message.contains('Cannot call OrderPublisher <RoundingMode>#<init>(java.util.Set <RoundingMode>) with arguments [java.util.HashSet <Date>]')
    }

}