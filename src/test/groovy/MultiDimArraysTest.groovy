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
package groovy

/**
 * Expose how to deal with multi-dimensional Arrays until this is supported at the language level.
 * @author Dierk Koenig
 * @author Jochen Theodorou
 */
class MultiDimArraysTest extends GroovyTestCase {

    // todo: enable as soon as multi dims are supported
    void testCallTwoDimStringArray() {
        def someArrayOfStringArrays = new SomeClass().anArrayOfStringArrays()
        assert 1 == someArrayOfStringArrays.size()
    }

    void testCallTwoDimStringArrayWorkaround() {
        def someArrayOfStringArrays = new SomeClass().anArrayOfStringArraysWorkaround()
        assert 1 == someArrayOfStringArrays.size()
        assert "whatever" == someArrayOfStringArrays[0][0]
        for (i in 0..<someArrayOfStringArrays.size()) {
            assert someArrayOfStringArrays[i]
        }
    }

    void testCallTwoDimStringArrayWorkaroundWithNull() {
        def someArrayOfStringArrays = new SomeClass().anArrayOfStringArraysWorkaround()
        assert 1 == someArrayOfStringArrays.size()
        assert "whatever" == someArrayOfStringArrays[0][0]
        someArrayOfStringArrays.each() { assert it}
    }

    void testInsideGroovyMultiDimReplacement() {
        Object[] someArrayOfStringArrays = [["a", "a", "a"], ["b", "b", "b", null]]
        assert "a" == someArrayOfStringArrays[0][0]
        someArrayOfStringArrays.each() { assert it}
    }

    void testMultiDimCreationWithSizes() {
        Object[][] objectArray = new Object[2][5]
        assert objectArray.length == 2
        objectArray.each {
            assert it.length == 5
            it.each { assert it == null }
        }
    }

    void testMultiDimCreationWithoutSizeAtEnd() {
        def array = new int[5][6][]
        assert array.class.name == "[[[I"
        assert array[0].class.name == "[[I"
        assert array[0][0] == null
    }

    void testMultiDimArrayForCustomClass() {
        def ff = new MultiDimArraysTest[3][4]
        assert "[[Lgroovy.MultiDimArraysTest;" == ff.class.name;
    }

    void testIntArrayIncrement() {
        int[][] x = new int[10][10]
        x[1][1] += 5
        assert x[1][1] == 5
    }
}

