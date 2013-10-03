/*
 * Copyright 2003-2013 the original author or authors.
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
package groovy.lang;

/**
 * Provides unit tests for the <code>IntRange</code> class.
 *
 * @author James Strachan
 */
class IntRangeTest extends GroovyTestCase {

    void testCreateTooBigRange() {
        try {
            new IntRange(0, Integer.MAX_VALUE);
            fail("too large range accepted");
        }
        catch (IllegalArgumentException ignore) {
            assertTrue("expected exception thrown", true);
        }
    }

    /**
     * Tests providing invalid arguments to the protected constructor.
     */
    void testInvalidArgumentsToConstructor() {
        try {
            new IntRange(2, 1, true);
            fail("invalid range created");
        }
        catch (IllegalArgumentException ignore) {
            assertTrue("expected exception thrown", true);
        }
    }

    /**
     * Tests getting the to and from values as <code>int</code>s.
     */
    void testGetToFromInt() {
        final int from = 3, to = 7;
        final IntRange range = new IntRange(from, to);
        assertEquals("wrong 'from'", from, range.getFromInt());
        assertEquals("wrong 'to'", to, range.getToInt());
    }

    void test_Step_ShouldNotOverflowForIntegerMaxValue() {
        (Integer.MAX_VALUE..Integer.MAX_VALUE).step(1) {
            assert it == Integer.MAX_VALUE
        }
    }

    void test_Step_ShouldNotOverflowForIntegerMinValue() {
        (Integer.MIN_VALUE..Integer.MIN_VALUE).step(-1) {
            assert it == Integer.MIN_VALUE
        }
    }

    void test_Step_ShouldNotOverflowForBigSteps(){
        (0..2000000000).step(1000000000) {
            assert it >= 0
        }

        (0..-2000000000).step(-1000000000) {
            assert it <= 0
        }
    }

    void testInclusiveRangesWithNegativesAndPositives() {
        final a = [1, 2, 3, 4]
        assert a[-3..-2] == [2, 3]
        assert a[-3..<-2] == [2]
        assert a[2..-3] == [3, 2]
        assert a[1..-1] == [2, 3, 4]
        assert a[1..<-1] == [2, 3]
        assert a[-2..<1] == [3]
        assert a[-2..<-3] == [3]
    }

    void testInclusiveRangesWithNegativesAndPositivesStrings() {
        def items = 'abcde'
        assert items[1..-2]   == 'bcd'
        assert items[1..<-2]  == 'bc'
        assert items[-3..<-2] == 'c'
        assert items[-2..-4]  == 'dcb'
        assert items[-2..<-4] == 'dc'
    }

    void testInclusiveRangesWithNegativesAndPositivesPrimBoolArray() {
        boolean[] bs = [true, false, true, true]
        assert bs[-3..-2]  == [false, true]
        assert bs[-3..<-2] == [false]
        assert bs[2..-3]   == [true, false]
        assert bs[1..-1]   == [false, true, true]
        assert bs[1..<-1]  == [false, true]
        assert bs[-2..<1]  == [true]
        assert bs[-2..<-3] == [true]
    }

    void testInclusiveRangesWithNegativesAndPositivesBitset() {
        int bits = 0b100001110100010001111110
        int numBits = 24
        def bs = new BitSet()
        numBits.times{ index -> bs[index] = (bits & 0x1).asBoolean(); bits >>= 1 }
        bs[3..5] = false
        assert bs.toString() == '{1, 2, 6, 10, 14, 16, 17, 18, 23}'
        assert bs[bs.length()-1] == true
        assert bs[-1] == true
        assert bs[6..17].toString() == '{0, 4, 8, 10, 11}'
        assert bs[6..<17].toString() == '{0, 4, 8, 10}'
        assert bs[17..6].toString() == '{0, 1, 3, 7, 11}'
        assert bs[17..<6].toString() == '{0, 1, 3, 7}'
        assert bs[-1..-7].toString() == '{0, 5, 6}'
        assert bs[-1..<-7].toString() == '{0, 5}'
        assert bs[20..<-8].toString() == '{2, 3}'
    }
}
