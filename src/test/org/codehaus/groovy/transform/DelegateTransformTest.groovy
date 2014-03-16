/*
 * Copyright 2003-2014 the original author or authors.
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

import gls.CompilableTestSupport

/**
 * @author Alex Tkachman
 * @author Guillaume Laforge
 * @author Paul King
 * @author Andre Steingress
 */
class DelegateTransformTest extends CompilableTestSupport {

    /** fix for GROOVY-3380   */
    void testDelegateImplementingANonPublicInterface() {
        assertScript """
            import org.codehaus.groovy.transform.ClassImplementingANonPublicInterface

            class DelegatingToClassImplementingANonPublicInterface {
                @Delegate ClassImplementingANonPublicInterface delegate = new ClassImplementingANonPublicInterface()
            }

            def constant = new DelegatingToClassImplementingANonPublicInterface().returnConstant()
            assert constant == "constant"
        """
    }

    /** fix for GROOVY-3380   */
    void testDelegateImplementingANonPublicInterfaceWithZipFileConcreteCase() {
        assertScript """
            import java.util.zip.*

            class ZipWrapper{
               @Delegate ZipFile zipFile
            }

            new ZipWrapper()
        """
    }

    /** test for GROOVY-GROOVY-5974 */
    void testDelegateExcludes() {
        assertScript """
          class MapSet {
            @Delegate(interfaces=false, excludes=['remove','clear']) Map m = [a: 1]
            @Delegate Set s = new LinkedHashSet([2, 3, 4] as Set) // HashSet not good enough in JDK 1.5
            String toString() { m.toString() + ' ' + s }
          }

          def ms = new MapSet()
          assert ms.size() == 1
          assert ms.toString() == '[a:1] [2, 3, 4]'
          ms.remove(3)
          assert ms.size() == 1
          assert ms.toString() == '[a:1] [2, 4]'
          ms.clear()
          assert ms.toString() == '[a:1] []'
        """
    }

    void testLock() {
        def res = new GroovyShell().evaluate("""
              import java.util.concurrent.locks.*

              class LockableMap {
                 @Delegate private Map map = [:]

                 @Delegate private Lock lock = new ReentrantLock ()

                 @Delegate(interfaces=false) private List list = new ArrayList ()
              }

              new LockableMap ()
        """)

        res.lock()
        try {
            res[0] = 0
            res[1] = 1
            res[2] = 2

            res.add("in list")
        }
        finally {
            res.unlock()
        }

        assertEquals([0: 0, 1: 1, 2: 2], res.@map)
        assertEquals("in list", res.@list[0])

        assertTrue res instanceof Map
        assertTrue res instanceof java.util.concurrent.locks.Lock
        assertFalse res instanceof List
    }

    void testMultiple() {
        def res = new GroovyShell().evaluate("""
        class X {
          def value = 10
        }

        class Y {
          @Delegate X  x  = new X ()
          @Delegate XX xx = new XX ()

          void setValue (v) {
            this.@x.@value = 12
          }
        }

        class XX {
          def value2 = 11
        }

        new Y ()
        """)

        assertEquals 10, res.value
        assertEquals 11, res.value2
        res.value = 123
        assertEquals 12, res.value
    }

    void testUsingDateCompiles() {
        assertScript """
        class Foo { 
          @Delegate Date d = new Date(); 
        } 
        Foo
      """
    }

    /** fix for GROOVY-3471   */
    void testDelegateOnAMapTypeFieldWithInitializationUsingConstructorProperties() {
        assertScript """
            class Test3471 { @Delegate Map mp }
            def t = new Test3471(mp: new HashMap()) // this was resulting in a NPE due to MetaClassImpl's special handling of Map
            assert t.keySet().size() == 0
        """
    }

    /** GROOVY-3323   */
    void testDelegateTransformCorrectlyDelegatesMethodsFromSuperInterfaces() {
        assert new DelegateBarImpl(new DelegateFooImpl()).bar() == 'bar impl'
        assert new DelegateBarImpl(new DelegateFooImpl()).foo() == 'foo impl'
    }

    /** GROOVY-3555   */
    void testDelegateTransformIgnoresDeprecatedMethodsByDefault() {
        def b1 = new DelegateBarForcingDeprecated(baz: new BazWithDeprecatedFoo())
        def b2 = new DelegateBarWithoutDeprecated(baz: new BazWithDeprecatedFoo())
        assert b1.bar() == 'bar'
        assert b2.bar() == 'bar'
        assert b1.foo() == 'foo'
        shouldFail(MissingMethodException) {
            assert b2.foo() == 'foo'
        }
    }

    /** GROOVY-4163   */
    void testDelegateTransformAllowsInterfacesAndDelegation() {
        assertScript """
            class Temp implements Runnable {
                @Delegate
                private Thread runnable

                static main(args) {
                    def thread = Thread.currentThread()
                    def temp = new Temp(runnable: thread)
                }
            }
        """
    }

    void testDelegateToSelfTypeShouldFail() {
        shouldNotCompile """
            class B {
                @Delegate B b = new B()
                static main(args){
                    new B()
                }
            }
        """
    }

    // GROOVY-4265
    void testShouldPreferDelegatedOverStaticSuperMethod() {
        assertScript """
            class A {
                static foo(){"A->foo()"}
            }
            class B extends A {
                @Delegate C c = new C()
            }
            class C {
                def foo(){"C->foo()"}
            }
            assert new B().foo() == 'C->foo()'
        """
    }

    void testDelegateToObjectShouldFail() {
        shouldNotCompile """
            class B {
                @Delegate b = new Object()
            }
        """
    }

    /** GROOVY-4244 */
    void testSetPropertiesThroughDelegate() {
        def foo = new Foo4244()

        assert foo.nonFinalBaz == 'Initial value - nonFinalBaz'
        foo.nonFinalBaz = 'New value - nonFinalBaz'
        assert foo.nonFinalBaz == 'New value - nonFinalBaz'

        assert foo.finalBaz == 'Initial value - finalBaz'
        shouldFail(ReadOnlyPropertyException) {
            foo.finalBaz = 'New value - finalBaz'
        }
    }

    void testDelegateSuperInterfaces_Groovy4619() {
        assert 'doSomething' in SomeClass4619.class.methods*.name
    }

    // GROOVY-5112
    void testGenericsOnArray() {
        assertScript '''
            class ListWrapper {
              @Delegate
              List myList

              @Delegate
              URL homepage
            }
            new ListWrapper()
        '''
    }

    // GROOVY-5732
    void testInterfacesFromSuperClasses() {
        assertScript '''
            interface I5732 {
                void aMethod()
            }

            abstract class AbstractBaseClass implements I5732 { }

            abstract class DelegatedClass extends AbstractBaseClass {
                void aMethod() {}
            }

            class Delegator {
                @Delegate private DelegatedClass delegate
            }

            assert I5732.isAssignableFrom(Delegator)
        '''
    }

    // GROOVY-5729
    void testDeprecationWithInterfaces() {
        assertScript '''
            interface I5729 {
                @Deprecated
                void aMethod()
            }

            class Delegator1 {
                @Delegate private I5729 delegate
            }
            assert I5729.isAssignableFrom(Delegator1)
            assert Delegator1.methods*.name.contains('aMethod')

            class Delegator2 {
                @Delegate(interfaces=false) private I5729 delegate
            }
            assert !I5729.isAssignableFrom(Delegator2)
            assert !Delegator2.methods*.name.contains('aMethod')

            class Delegator3 {
                @Delegate(interfaces=false, deprecated=true) private I5729 delegate
            }
            assert !I5729.isAssignableFrom(Delegator3)
            assert Delegator3.methods*.name.contains('aMethod')
        '''
    }

    // GROOVY-5446
    void testDelegateWithParameterAnnotations() {
        assertScript """
            import java.lang.annotation.*

            @Retention(RetentionPolicy.RUNTIME)
            @Target([ElementType.PARAMETER])
            public @interface SomeAnnotation {
            }

            class A {
                def method(@SomeAnnotation def param) { "Test" }
            }

            class A_Delegate {
                @Delegate(parameterAnnotations = true)
                A a = new A()
            }

            def originalMethod = A.getMethod('method', [Object.class] as Class[])
            def originalAnno = originalMethod.parameterAnnotations[0][0]

            def delegateMethod = A_Delegate.getMethod('method', [Object.class] as Class[])
            def delegateAnno = delegateMethod.parameterAnnotations[0][0]

            assert delegateAnno == originalAnno
        """
    }

    void testDelegateWithMethodAnnotations() {
        assertScript """
            import java.lang.annotation.*

            @Retention(RetentionPolicy.RUNTIME)
            @Target([ElementType.METHOD])
            public @interface SomeAnnotation {
                int value()
            }

            class A {
                @SomeAnnotation(42)
                def method( def param) { "Test" }
            }

            class A_Delegate {
                @Delegate(methodAnnotations = true)
                A a = new A()
            }

            def originalMethod = A.getMethod('method', [Object.class] as Class[])
            def originalAnno = originalMethod.declaredAnnotations[0]

            def delegateMethod = A_Delegate.getMethod('method', [Object.class] as Class[])
            def delegateAnno = delegateMethod.declaredAnnotations[0]

            assert delegateAnno == originalAnno

            assert delegateAnno.value() == 42
            assert delegateAnno.value() == originalAnno.value()
        """
    }

    void testParameterAnnotationsShouldNotBeCarriedOverByDefault() {
        assertScript """
            import java.lang.annotation.*

            @Retention(RetentionPolicy.RUNTIME)
            @Target([ElementType.PARAMETER])
            public @interface SomeAnnotation {
            }

            class A {
                def method(@SomeAnnotation def param) { "Test" }
            }

            class A_Delegate {
                @Delegate
                A a = new A()
            }

            def originalMethod = A.getMethod('method', [Object.class] as Class[])
            def originalAnno = originalMethod.parameterAnnotations[0][0]

            def delegateMethod = A_Delegate.getMethod('method', [Object.class] as Class[])
            assert delegateMethod.parameterAnnotations[0].length == 0
        """
    }

    // this test reflects that we currently don't support carrying over
    // Closure Annotations rather than a desired design goal
    // TODO: support Closure Annotations and then remove/change this test
    void testAnnotationWithClosureMemberIsNotSupported() {
        def message = shouldFail {
            assertScript """
                import java.lang.annotation.*

                @Retention(RetentionPolicy.RUNTIME)
                @Target([ElementType.METHOD])
                public @interface SomeAnnotation {
                    Class value()
                }

                class A {
                    @SomeAnnotation({ param != null })
                    def method(def param) { "Test" }
                }

                class A_Delegate {
                    @Delegate(methodAnnotations = true)
                    A a = new A()
                }
            """
        }

        assert message.contains('@Delegate does not support keeping Closure annotation members.')
    }

    // this test reflects that we currently don't support carrying over
    // Closure Annotations rather than a desired design goal
    // TODO: support Closure Annotations and then remove/change this test
    void testAnnotationWithClosureClassDescendantIsNotSupported() {
        def message = shouldFail {
            assertScript """
                import java.lang.annotation.*

                @Retention(RetentionPolicy.RUNTIME)
                @Target([ElementType.METHOD])
                public @interface SomeAnnotation {
                    Class value()
                }

                class A {
                    @SomeAnnotation(org.codehaus.groovy.runtime.GeneratedClosure.class)
                    def method(def param) { "Test" }
                }

                class A_Delegate {
                    @Delegate(methodAnnotations = true)
                    A a = new A()
                }
            """
        }
        assert message.contains('@Delegate does not support keeping Closure annotation members.')
    }

    // GROOVY-5445
    void testDelegateToSuperProperties() {
        assertScript """
            class Foo {
                @Delegate Bar delegate = new Bar()
                def foo() {
                    bar = "bar"
                    baz = "baz"
                }
            }

            class Bar extends Baz { String bar }
            class Baz { String baz }

            def f = new Foo()
            f.foo()
            assert f.bar + f.baz == 'barbaz'
        """
    }

    // GROOVY-6330
    void testIncludeAndExcludeByType() {
        assertScript """
            interface AddAllCollectionSelector {
                boolean addAll(Collection<? extends Integer> c)
                Integer remove(int index)
            }

            class SplitNumberList {
                // collection variant of addAll and remove will work on odd list, all other methods on even list
                @Delegate(excludeTypes=AddAllCollectionSelector) List<Integer> evens = [2, 4, 6]
                @Delegate(includeTypes=AddAllCollectionSelector) List<Integer> odds = [1, 3, 5]
                def getEvensThenOdds() { evens + odds }
            }

            def list = new SplitNumberList()
            assert list.evensThenOdds == [2, 4, 6, 1, 3, 5]
            list.addAll([7, 9])
            list.addAll(1, [8])
            list.remove(0)
            assert list.indexOf(8) == 1
            assert list.evensThenOdds == [2, 8, 4, 6, 3, 5, 7, 9]
        """
    }

    // GROOVY-5211
    void testAvoidFieldNameClashWithParameterName() {
        assertScript """
            class A {
                def foo(a) { a * 2 }
            }

            class B {
                @Delegate A a = new A()
            }

            assert new B().foo(10) == 20
        """
    }

    // GROOVY-6542
    void testLineNumberInStackTrace() {
        try {
            assertScript '''import groovy.transform.ASTTest
    import org.codehaus.groovy.control.CompilePhase

    @ASTTest(phase=CompilePhase.CANONICALIZATION, value={
        def fieldNode = node.getDeclaredField('thingie')
        def blowupMethod = node.getDeclaredMethod('blowup')
        def mce = blowupMethod.code.expression
        assert mce.lineNumber==fieldNode.lineNumber
        assert mce.lineNumber>0
    })
    class Upper {
      @Delegate Lower thingie

      Upper() {
        thingie = new Lower()
      }
    }

    class Lower {
      def foo() {
        println("Foo!")
      }

      def blowup(String a) {
        throw new Exception("blow up with ${a}")
      }

      def blowup() {
        throw new Exception("blow up")
      }
    }

    def up = new Upper()
    up.foo()
    up.blowup("bar")
    '''
        } catch (e) {
            // ok
        }
    }
}

interface DelegateFoo {
    def foo()
}

class DelegateFooImpl implements DelegateFoo {
    def foo() { 'foo impl' }
}

interface DelegateBar extends DelegateFoo {
    def bar()
}

class DelegateBarImpl implements DelegateBar {
    @Delegate DelegateFoo foo;

    DelegateBarImpl(DelegateFoo f) { this.foo = f}

    def bar() { 'bar impl'}
}

class BazWithDeprecatedFoo {
    @Deprecated foo() { 'foo' }
    def bar() { 'bar' }
}

class DelegateBarWithoutDeprecated {
    @Delegate BazWithDeprecatedFoo baz
}

class DelegateBarForcingDeprecated {
    @Delegate(deprecated=true) BazWithDeprecatedFoo baz
}

class Foo4244 {
    @Delegate Bar4244 bar = new Bar4244()
}

class Bar4244 {
    String nonFinalBaz = "Initial value - nonFinalBaz"
    final String finalBaz = "Initial value - finalBaz"
}

interface SomeInterface4619 {
    void doSomething()
}

interface SomeOtherInterface4619 extends SomeInterface4619 {}

class SomeClass4619 {
    @Delegate
    SomeOtherInterface4619 delegate
}
