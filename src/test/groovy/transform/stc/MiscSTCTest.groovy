/*
 * Copyright 2003-2010 the original author or authors.
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
package groovy.transform.stc

/**
 * Unit tests for static type checking : miscellaneous tests.
 *
 * @author Cedric Champeau
 */
class MiscSTCTest extends StaticTypeCheckingTestCase {

     void testFibonacci() {
         assertScript '''
            long sd = System.currentTimeMillis()
            int fib(int i) {
                i < 2 ? 1 : fib(i - 2) + fib(i - 1);
            }
            println fib(40)
            long dur = System.currentTimeMillis()-sd
            println "${dur}ms"
         '''
     }

    void testGreeter() {
        assertScript '''
            class Greet {
                def name

                Greet(String who) {
                    name = who[0].toUpperCase() +
                            who[1..-1]
                }

                def salute() { println "Hello $name!" }
            }

            def g = new Greet('world')  // create object
            g.salute()               // output "Hello World!"
        '''
    }

    void testClosureReturnTypeShouldNotBeTestedAgainstMethodReturnType() {
        assertScript '''
        void method() {
            def cl = { String it -> it.toUpperCase() }
            assert cl('test')=='TEST'
        }
        method()
        '''
    }

    void testPropertyOnClass() {
        assertScript '''
            Class clazz = String
            assert clazz.name=='java.lang.String'
        '''
    }

    void testDirectPropertyOnClass() {
        assertScript '''
            assert String.name=='java.lang.String'
        '''
    }

    void testMethodOnClass() {
        assertScript '''
            Class clazz = String
            clazz.getDeclaredFields()
        '''
    }

    void testDirectMethodOnClass() {
        assertScript '''
            String.getDeclaredFields()
        '''
    }

    void testFieldsFromClass() {
        assertScript '''
            String.class.fields
        '''
    }

    void testDirectFieldsFromClass() {
        assertScript '''
            String.fields
        '''
    }

    void testMissingSetter() {
        shouldFailWithMessages '''
            class Foo {
                String getName() { 'Hello' }
            }
            Foo foo = new Foo()
            foo.name = 'Error'
        ''', 'Cannot set read-only property: name'
    }

    void testMissingSetterThroughPath() {
        shouldFailWithMessages '''
            class Foo {
                String getName() { 'Hello' }
            }
            class Bar {
                Foo foo = new Foo()
            }
            Bar bar = new Bar()
            bar.foo.name = 'Error'
        ''', 'Cannot set read-only property: name'
    }

    void testMissingSetterAndWith() {
        shouldFailWithMessages '''
            class Foo {
                String getName() { 'Hello' }
            }
            Foo foo = new Foo()
            foo.with {
                name = 'Error'
            }
        ''', 'The variable [name] is undeclared.' // todo: can we provide a better error message ?
    }

    void testFindMethodFromSameClass() {
        assertScript '''
        class Foo {
            int foo() {
                1
            }
            int bar(int x) {
                foo()
            }
        }
        new Foo().bar(2)
        '''
    }

    void testCallToSuperConstructor() {
        assertScript '''
            class MyException extends Exception {
                MyException(String message) { super(message) }
            }
            1
        '''
    }

    void testCallToThisConstructor() {
        assertScript '''
            class MyException extends Exception {
                MyException(int errNo, String message) {
                    this(message)
                }
                MyException(String message) { super(message) }
            }
            1
        '''
    }
    
    void testCompareEnumToNull() {
        assertScript '''
            enum MyEnum { a,b }
            MyEnum val = null
            if (val == null) {
                val = MyEnum.a
            }
        '''
    }

    void testMethodReturnTypeInferenceShouldWorkBecauseInSameSourceUnit() {
        assertScript '''
            class A {
                static def foo() { '123' }
            }
            A.foo().toInteger()
        '''
    }

    void testMethodReturnTypeInferenceShouldNotWorkBecauseNotSameSourceUnit() {
        shouldFailWithMessages '''
            import groovy.transform.stc.MiscSTCTest.MiscSTCTestSupport as A
            A.foo().toInteger()
        ''', 'Cannot find matching method java.lang.Object#toInteger()'
    }

    void testClassLiteralAsArgument() {
        assertScript '''
            void lookup(Class clazz) { }
            lookup(Date)
        '''
    }

    // GROOVY-5699
    void testIntRangeInference() {
        assertScript '''
            @ASTTest(phase=INSTRUCTION_SELECTION, value={
                assert node.getNodeMetaData(INFERRED_TYPE) == make(IntRange)
            })
            def range = 1..10

            @ASTTest(phase=INSTRUCTION_SELECTION, value={
                assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
            })
            def from = range.fromInt
        '''
    }

    public static class MiscSTCTestSupport {
        static def foo() { '123' }
    }
}

