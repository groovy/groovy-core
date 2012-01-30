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
 * Unit tests for static type checking : arrays and collections.
 *
 * @author Cedric Champeau
 */
class ArraysAndCollectionsSTCTest extends StaticTypeCheckingTestCase {

    void testArrayAccess() {
        assertScript '''
            String[] strings = ['a','b','c']
            String str = strings[0]
        '''
    }

    void testArrayElementReturnType() {
        shouldFailWithMessages '''
            String[] strings = ['a','b','c']
            int str = strings[0]
        ''', 'Cannot assign value of type java.lang.String to variable of type int'
    }

    void testWrongComponentTypeInArray() {
        shouldFailWithMessages '''
            int[] intArray = ['a']
        ''', 'Cannot assign value of type java.lang.String into array of type [I'
    }

    void testAssignValueInArrayWithCorrectType() {
        assertScript '''
            int[] arr2 = [1, 2, 3]
            arr2[1] = 4
        '''
    }

    void testAssignValueInArrayWithWrongType() {
        shouldFailWithMessages '''
            int[] arr2 = [1, 2, 3]
            arr2[1] = "One"
        ''', 'Cannot assign value of type java.lang.String to variable of type int'
    }

    void testBidimensionalArray() {
        assertScript '''
            int[][] arr2 = new int[1][]
            arr2[0] = [1,2]
        '''
    }

    void testBidimensionalArrayWithInitializer() {
        shouldFailWithMessages '''
            int[][] arr2 = new Object[1][]
        ''', 'Cannot assign value of type [[Ljava.lang.Object; to variable of type [[I'
    }

    void testBidimensionalArrayWithWrongSubArrayType() {
        shouldFailWithMessages '''
            int[][] arr2 = new int[1][]
            arr2[0] = ['1']
        ''', 'Cannot assign value of type java.lang.String into array of type [I'
    }

    void testForLoopWithArrayAndUntypedVariable() {
        assertScript '''
            String[] arr = ['1','2','3']
            for (i in arr) { }
        '''
    }

    void testForLoopWithArrayAndWrongVariableType() {
        shouldFailWithMessages '''
            String[] arr = ['1','2','3']
            for (int i in arr) { }
        ''', 'Cannot loop with element of type int with collection of type [Ljava.lang.String;'
    }
    void testJava5StyleForLoopWithArray() {
        assertScript '''
            String[] arr = ['1','2','3']
            for (String i : arr) { }
        '''
    }

    void testJava5StyleForLoopWithArrayAndIncompatibleType() {
        shouldFailWithMessages '''
            String[] arr = ['1','2','3']
            for (int i : arr) { }
        ''', 'Cannot loop with element of type int with collection of type [Ljava.lang.String;'
    }

    void testForEachLoopOnString() {
        assertScript '''
            String name = 'Guillaume'
            for (String s in name) {
                println s
            }
        '''
    }

    void testSliceInference() {
        assertScript '''
        List<String> foos = ['aa','bb','cc']
        foos[0].substring(1)
        def bars = foos[0..1]
        println bars[0].substring(1)
        '''

        assertScript '''
        def foos = ['aa','bb','cc']
        foos[0].substring(1)
        def bars = foos[0..1]
        println bars[0].substring(1)
        '''
    }

    void testListStarProperty() {
        assertScript '''
            List list = ['a','b','c']
            List classes = list*.class
        '''
    }

    void testListStarMethod() {
        assertScript '''
            List list = ['a','b','c']
            List classes = list*.toUpperCase()
        '''
    }

    void testInferredMapDotProperty() {
        assertScript '''
            def map = [:]
            map['a'] = 1
            map.b = 2
        '''
    }

    void testInlineMap() {

        assertScript '''
            Map map = [a:1, b:2]
        '''

        assertScript '''
            def map = [a:1, b:2]
            map = [b:2, c:3]
        '''

        assertScript '''
            Map map = ['a':1, 'b':2]
        '''
    }

    void testAmbiguousCallWithVargs() {
        assertScript '''
            int sum(int x) { 1 }
            int sum(int... args) {
                0
            }
            assert sum(1) == 1
        '''
    }

    void testAmbiguousCallWithVargs2() {
        assertScript '''
            int sum(int x) { 1 }
            int sum(int y, int... args) {
                0
            }
            assert sum(1) == 1
        '''
    }

    void testCollectMethodCallOnList() {
        assertScript '''
            [1,2,3].collect { it.toString() }
        '''
    }

    void testForInLoop() {
        assertScript '''
            class A {
                String name
            }
            List<A> myList = [new A(name:'Cedric'), new A(name:'Yakari')] as LinkedList<A>
            for (element in myList) {
                element.name.toUpperCase()
            }
        '''
    }

    void testForInLoopWithDefaultListType() {
        assertScript '''
            class A {
                String name
            }
            List<A> myList = [new A(name:'Cedric'), new A(name:'Yakari')]
            for (element in myList) {
                element.name.toUpperCase()
            }
        '''
    }

    void testForInLoopWithRange() {
        assertScript '''
            for (int i in 1..10) { i*2 }
        '''
    }

    // GROOVY-5177
    void testShouldNotAllowArrayAssignment() {
        shouldFailWithMessages '''
            class Foo {
                def say() {
                    FooAnother foo1 = new Foo[13] // but FooAnother foo1 = new Foo() reports a STC                        Error
                }
            }
            class FooAnother {

            }
        ''', 'Cannot assign value of type Foo[] to variable of type FooAnother'
    }

    void testListPlusEquals() {
        assertScript '''
            List<String> list = ['a','b']
            list += ['c']
            assert list == ['a','b','c']
        '''

        assertScript '''
            Collection<String> list = ['a','b']
            list += 'c'
            assert list == ['a','b','c']
        '''
    }
    
    void testObjectArrayGet() {
        assertScript '''
            Object[] arr = [new Object()]
            arr.getAt(0)
        '''
    }

    void testStringArrayGet() {
        assertScript '''
            String[] arr = ['abc']
            arr.getAt(0)
        '''
    }

    void testObjectArrayPut() {
        assertScript '''
            Object[] arr = [new Object()]
            arr.putAt(0, new Object())
        '''
    }

    void testObjectArrayPutWithNull() {
        assertScript '''
            Object[] arr = [new Object()]
            arr.putAt(0, null)
        '''
    }

    void testStringArrayPut() {
        assertScript '''
            String[] arr = ['abc']
            arr.putAt(0, 'def')
        '''
    }

    void testStringArrayPutWithNull() {
        assertScript '''
            String[] arr = ['abc']
            arr.putAt(0, null)
        '''
    }

    void testStringArrayPutWithWrongType() {
        shouldFailWithMessages '''
            String[] arr = ['abc']
            arr.putAt(0, new Object())
        ''', 'Cannot find matching method [Ljava.lang.String;#putAt(int, java.lang.Object)'
    }

    void testStringArrayPutWithSubType() {
        assertScript '''
            Serializable[] arr = ['abc']
            arr.putAt(0, new Integer(1))
        '''
    }

    void testStringArrayPutWithSubTypeAsPrimitive() {
        assertScript '''
            Serializable[] arr = ['abc']
            arr.putAt(0, 1)
        '''
    }

    void testStringArrayPutWithIncorrectSubType() {
        shouldFailWithMessages '''
            Serializable[] arr = ['abc']
            arr.putAt(0, new XmlSlurper())
        ''', 'Cannot find matching method [Ljava.io.Serializable;#putAt(int, groovy.util.XmlSlurper)'
    }

    void testArrayGetOnPrimitiveArray() {
        assertScript '''
            int m() {
                int[] arr = [1,2,3]
                arr.getAt(1)
            }
            assert m()==2
        '''
    }

    void testReturnTypeOfArrayGet() {
        assertScript '''
            Serializable m() {
                String[] arr = ['1','2','3']
                arr.getAt(1)
            }
            assert m()=='2'
        '''
    }
}

