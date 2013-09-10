/*
 * Copyright 2003-2011 the original author or authors.
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
public class IntRangeTest extends GroovyTestCase {

    public void testCreateTooBigRange() {
        try {
            new IntRange(0, Integer.MAX_VALUE);
            fail("too large range accepted");
        }
        catch (IllegalArgumentException e) {
            assertTrue("expected exception thrown", true);
        }
    }

    /**
     * Tests providing invalid arguments to the protected constructor.
     */
    public void testInvalidArgumentsToConstructor() {
        try {
            new IntRange(2, 1, true);
            fail("invalid range created");
        }
        catch (IllegalArgumentException e) {
            assertTrue("expected exception thrown", true);
        }
    }

    /**
     * Tests getting the to and from values as <code>int</code>s.
     */
    public void testGetToFromInt() {
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
}
