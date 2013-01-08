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
package gls.invocation

import gls.CompilableTestSupport

class DefaultParamTest extends CompilableTestSupport {

    void testDefaultParameterCausingDoubledMethod() {
        //GROOVY-2191
        shouldNotCompile '''
            def foo(String one, String two = "two") {"$one $two"}
            def foo(String one, String two = "two", String three = "three") {"$one $two $three"}
        '''

        shouldNotCompile '''
            def foo(String one, String two = "two", String three = "three") {"$one $two $three"}
            def foo(String one, String two = "two") {"$one $two"}
        '''
            
        shouldNotCompile '''
            def meth(Closure cl = null) {meth([:], cl)}
            def meth(Map args = [:], Closure cl = null) {}
        '''
    } 

    void testDefaultParameters() {
        assertScript '''
            def doSomething(a, b = 'defB', c = 'defC') {
                return a + "-" + b + "-" + c
            }
            assert "X-Y-Z" == doSomething("X", "Y", "Z")
            assert "X-Y-defC" == doSomething("X", "Y")
            assert "X-defB-defC" == doSomething("X")
        '''
        shouldFail {
            assertScript '''
                def doSomething(a, b = 'defB', c = 'defC') {
                    return a + "-" + b + "-" + c
                }
                doSomething()        
            '''
        }
    }

    void testDefaultTypedParameters() {
        assertScript """
            String doTypedSomething(String a = 'defA', String b = 'defB', String c = 'defC') {
                a + "-" + b + "-" + c
            }
            assert "X-Y-Z" == doTypedSomething("X", "Y", "Z")
            assert "X-Y-defC" == doTypedSomething("X", "Y")
            assert "X-defB-defC" == doTypedSomething("X")
            assert "defA-defB-defC" == doTypedSomething()
        """
    }

    void testDefaultTypedParametersAnother() {
        assertScript """
            String doTypedSomethingAnother(String a = 'defA', String b = 'defB', String c) {
                return a + "-" + b + "-" + c
            }
            assert "X-Y-Z" == doTypedSomethingAnother("X", "Y", "Z")
            assert "X-defB-Z" == doTypedSomethingAnother("X", "Z")
            assert "defA-defB-Z" == doTypedSomethingAnother("Z")            
        """
        shouldFail {
            assertScript """
                String doTypedSomethingAnother(String a = 'defA', String b = 'defB', String c) {
                    return a + "-" + b + "-" + c
                }
                doTypedSomethingAnother()
            """
        }
    }
    
    void testConstructor() {
        assertScript """
            class DefaultParamTestTestClass {
                def j
                DefaultParamTestTestClass(int i = 1){j=i}
            }
            assert DefaultParamTestTestClass.declaredConstructors.size() == 2
            def foo = new DefaultParamTestTestClass()
            assert foo.j == 1
            foo = new DefaultParamTestTestClass(2)
            assert foo.j == 2        
        """
    }
    
    void testPrecendence() {
        // def meth(Closure cl = null) will produce a call meth(null)
        // since interfaces are prefered over normal classes and since
        // def meth(Map args, Closure cl = null) will produce a method
        // meth(Map) a simple call with meth(null) would normally call
        // meth(Map). To ensure this will not happen the call has to 
        // use a cast in the automatically created method. 
        assertScript """
            def meth(Closure cl = null) {
              return '1' +meth([:], cl)
            }

            def meth(Map args, Closure cl = null) {
                if(args==null) return "2" 
                return '2'+args.size()
            }

            assert meth() == "120"
            assert meth(null) == "2"
            assert meth {} == "120"
            assert meth(a:1) == "21"
            assert meth(a:1) {} == "21"
        """
    }

    void testClosureSharedVariableRefersToDefaultParameter() {
        assertScript """
                def f1( int x = 3, fn={ -> x } ) {
                    return fn()
                }

                assert 3 == f1()
                assert 42 == f1(42)
            """

        assertScript """
               def f2( int x = 3, fn={ -> def c2 = { -> x }; c2.call() } ) {
                   return fn()
               }

               assert 42 == f2(42)
               assert 42 == f2(42)
               assert 84 == f2(42) { 84 }
            """

        assertScript """
               def f3(fn={ -> 42 }, fn2={ -> def c2 = { -> fn() }; c2.call() } ) {
                   return fn2()
               }

               assert 42 == f3()
               assert 84 == f3({ -> 84 })
            """

        assertScript """
               def f4(def s = [1,2,3], fn = { -> s.size() }) {
                   fn()
               }

               assert 3 == f4()
            """

        assertScript """
           static <T extends Number> Integer f5(List<T> s = [1,2,3], fn = { -> (s*.intValue()).sum() }) {
               fn()
           }

           assert 6 == f5()
           assert 6 == f5([1.1, 2.1, 3.1])
        """

        assertScript """
           def f6(def s = [1,2,3], fn = { -> s.size() }, fn2 = { fn() + s.size() }) {
               fn2()
           }

           assert 6 == f6()
        """

        assertScript """
            def f7( int x = 3, int y = 39, fn={ -> x + y } ) {
                return fn()
            }

            assert 42 == f7()
        """
    }
}

