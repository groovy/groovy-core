/*
 * Copyright 2003-2012 the original author or authors.
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

package org.codehaus.groovy.classgen.asm.sc

import groovy.transform.stc.DelegatesToSTCTest

/**
 * Units tests aimed at testing the behaviour of {@link DelegatesTo} in combination
 * with static compilation.
 *
 * @author Cedric Champeau
 */
class DelegatesToStaticCompileTest extends DelegatesToSTCTest implements StaticCompilationTestSupport {

    // GROOVY-6953
    void testRatpackRegression1() {
        assertScript '''
            class MyHandlers  {
              Map execute() {
                againstList {
                  foo = "bar"
                }
              }

              Map againstList(@DelegatesTo(Map) Closure<?> c) {
                def l = [:]
                c.delegate = l
                c.call()
                l
              }
            }


            def map = new MyHandlers().execute()
            assert map.foo == 'bar'
        '''
    }

    // GROOVY-6955
    void testRatpackRegressionIfDelegateToJavaClass() {
        try {
            assertScript '''import org.codehaus.groovy.classgen.asm.sc.Groovy6955Support as GroovyContext
                class MyHandlers {
                  def handler(@DelegatesTo(GroovyContext) Closure<?> c) {
                    def l = new GroovyContext()
                    c.delegate = l
                    c.call()
                  }
                  def execute() {
                    handler {
                      request.headers.someKey
                    }
                  }

                }


                def result = new MyHandlers().execute()
                assert result == 'someValue'
            '''
        } finally {
            def bytecode = astTrees['MyHandlers$_execute_closure1'][1]
            assert bytecode.contains('INVOKEVIRTUAL org/codehaus/groovy/classgen/asm/sc/Groovy6955Support.getRequest')
            assert bytecode.contains('INVOKEVIRTUAL org/codehaus/groovy/classgen/asm/sc/Groovy6955Support$Request.getHeaders')
        }
    }
}