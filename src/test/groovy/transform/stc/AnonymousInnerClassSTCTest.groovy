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

package groovy.transform.stc

import groovy.transform.NotYetImplemented

/**
 * Test cases specifically aimed at testing the behaviour of the type checker
 * with regards to anonymous inner classes.
 *
 * @author Cedric Champeau
 */
class AnonymousInnerClassSTCTest extends StaticTypeCheckingTestCase {

    // GROOVY-5565
    void testShouldNotThrowNPE() {
        assertScript '''
            Serializable s = new Serializable() { List things = [] }
            assert s.things.size() == 0
        '''
        assertScript '''
            Serializable s = new Serializable() { List things = [1] }
            assert s.things.size() == 1
        '''
    }

    void testAssignmentOfAICFromInterface() {
        assertScript '''
            Runnable r = new Runnable() {
                public void run() { println 'ok' }
            }
            r.run()
        '''
    }

    void testAssignmentOfAICFromAbstractClass() {
        assertScript '''
            abstract class Foo { abstract String item() }
            Foo f = new Foo() {
                String item() { 'ok' }
            }
            assert f.item() == 'ok'
        '''
    }

    void testAssignmentOfAICFromAbstractClassAndInterface() {
        assertScript '''
            abstract class Foo  implements Runnable { abstract String item() }
            Foo f = new Foo() {
                String item() { 'ok' }
                void run() {}
            }
            assert f.item() == 'ok'
            f.run()
        '''
    }

    void testCallMethodUsingAIC() {
        assertScript '''abstract class Foo { abstract String item() }
            boolean valid(Foo foo) {
                foo.item() == 'ok'
            }
            def f = new Foo() {
                String item() { 'ok' }
            }
            assert valid(f)
        '''
    }

    void testCallMethodUsingAICImplementingInterface() {
        assertScript '''abstract class Foo implements Runnable { abstract String item() }
            boolean valid(Foo foo) {
                foo.item() == 'ok'
            }
            def f = new Foo() {
                String item() { 'ok' }
                void run() {}
            }
            assert valid(f)
            f.run()
        '''
    }

    @NotYetImplemented
    void testAICIntoClass() {
        assertScript '''
            class Outer {
                int outer() { 1 }
                abstract class Inner {
                    abstract int inner()
                }
                int test() {
                    Inner inner = new Inner() {
                        int inner() { outer() }
                    }
                    inner.inner()
                }
            }
            assert new Outer().test() == 1
        '''
    }

    // GROOVY-5566
    void testAICReferencingOuterLocalVariable() {
        assertScript '''
            def foo() {
                List things = []
                Serializable s = new Serializable() {
                  def size() {
                    things.size()
                  }
                }
                s.size()
            }'''
    }
}
