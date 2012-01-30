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
 * Unit tests for static type checking : default groovy methods.
 *
 * @author Cedric Champeau
 */
class DefaultGroovyMethodsSTCTest extends StaticTypeCheckingTestCase {

    void testEach() {
        assertScript """
            ['a','b'].each { // DGM#each(Object, Closure)
                println it // DGM#println(Object,Object)
            }
        """

        assertScript """
            ['a','b'].eachWithIndex { it, i ->// DGM#eachWithIndex(Object, Closure)
                println it // DGM#println(Object,Object)
            }
        """
    }

    void testStringToInteger() {
        assertScript """
        String name = "123"
        name.toInteger() // toInteger() is defined by DGM
        """
    }

    void testVariousAssignmentsThenToInteger() {
        assertScript """
         class A {
          void foo() {}
         }
        def name = new A()
        name.foo()
        name = 1
        name = '123'
        name.toInteger() // toInteger() is defined by DGM
        """
    }

    void testMethodsOnPrimitiveTypes() {
        assertScript '''
            1.times { it }
        '''

        assertScript '''
            true.equals { it }
        '''
    }
  
    void testShouldAcceptMethodFromDefaultDateMethods() {
      assertScript '''
        def s = new Date()
        println s.year
        println s.format("yyyyMMdd")
      '''
    }
}

