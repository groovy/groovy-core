/*
 * Copyright 2003-2015 the original author or authors.
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





package org.codehaus.groovy.classgen.asm.sc.bugs

import groovy.transform.NotYetImplemented
import groovy.transform.stc.StaticTypeCheckingTestCase
import org.codehaus.groovy.classgen.asm.sc.StaticCompilationTestSupport

class Groovy7276Bug extends StaticTypeCheckingTestCase implements StaticCompilationTestSupport {
    void testShouldGoThroughPrivateBridgeAccessor() {
            assertScript '''
                class Foo {
                    private i = 1
                    def m() { new String().with {i}}
                }
                assert new Foo().m() == 1
                class Bar extends Foo {}
                assert new Bar().m() == 1
        '''
    }

    void testShouldGoThroughPrivateBridgeMethod() {
            assertScript '''
                class Foo {
                    private i = 1
                    private def pvI() { i }
                    def m() { new String().with {pvI()}}
                }
                assert new Foo().m() == 1
                class Bar extends Foo {}
                assert new Bar().m() == 1
        '''
    }

    void testPrivateAccessInInnerClass() {
        assertScript '''import groovy.transform.CompileStatic

class Outer {
    private static class Inner {

        private Set<String> variablesToCheck = []

        private void checkAssertions(String name) {
            Runnable r = {
                def candidates = variablesToCheck.findAll { it == name }
            }
            r.run()
        }
    }

    static void test() {
        new Inner().checkAssertions('name')
    }
}

Outer.test()'''
    }

    @NotYetImplemented
    // GROOVY-7304
    void testShouldGoThroughPrivateBridgeAccessorWithWriteAccess() {
            assertScript '''
                class Foo {
                    private int i = 1
                    def m() { new String().with {++i}}
                }
                assert new Foo().m() == 2
                class Bar extends Foo {}
                assert new Bar().m() == 2
        '''
    }


}
