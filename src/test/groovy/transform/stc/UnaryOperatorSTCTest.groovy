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
 * Unit tests for static type checking : unary operator tests.
 *
 * @author Cedric Champeau
 */
class UnaryOperatorSTCTest extends StaticTypeCheckingTestCase {

     void testUnaryPlusOnInt() {
         assertScript '''
            int x = 1
            assert +x == 1
         '''
     }

     void testUnaryPlusOnInteger() {
         assertScript '''
            Integer x = new Integer(1)
            assert +x == 1
         '''
     }

     void testUnaryMinusOnInt() {
         assertScript '''
            int x = 1
            assert -x == -1
         '''
     }

     void testUnaryMinusOnInteger() {
         assertScript '''
            Integer x = new Integer(1)
            assert -x == -1
         '''
     }

     void testUnaryPlusOnShort() {
         assertScript '''
            short x = 1
            assert +x == 1
         '''
     }

     void testUnaryPlusOnBoxedShort() {
         assertScript '''
            Short x = new Short((short)1)
            assert +x == 1
         '''
     }

     void testUnaryMinusOnShort() {
         assertScript '''
            short x = 1
            assert -x == -1
         '''
     }

     void testUnaryMinusOnBoxedShort() {
         assertScript '''
            Short x = new Short((short)1)
            assert -x == -1
         '''
     }

     void testUnaryPlusOnFloat() {
         assertScript '''
            float x = 1f
            assert +x == 1f
         '''
     }

     void testUnaryPlusOnBoxedFloat() {
         assertScript '''
            Float x = new Float(1f)
            assert +x == 1f
         '''
     }

     void testUnaryMinusOnFloat() {
         assertScript '''
            float x = 1f
            assert -x == -1f
         '''
     }

     void testUnaryMinusOnBoxedFloat() {
         assertScript '''
            Float x = new Float(1f)
            assert -x == -1f
         '''
     }

     void testUnaryPlusOnDouble() {
         assertScript '''
            double x = 1d
            assert +x == 1d
         '''
     }

     void testUnaryPlusOnBoxedDouble() {
         assertScript '''
            Double x = new Double(1d)
            assert +x == 1d
         '''
     }

     void testUnaryMinusOnDouble() {
         assertScript '''
            double x = 1d
            assert -x == -1d
         '''
     }

     void testUnaryMinusOnBoxedDouble() {
         assertScript '''
            Double x = new Double(1d)
            assert -x == -1d
         '''
     }

    void testIntXIntInferredType() {
        assertScript '''
            int x = 1
            int y = 2
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == int_TYPE
            })
            def zp = x+y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == int_TYPE
            })
            def zm = x*y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == int_TYPE
            })
            def zmi = x-y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == BigDecimal_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == BigDecimal_TYPE
            })
            def zd = x/y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == int_TYPE
            })
            def zmod = x%y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == int_TYPE
            })
            def zpeq = (x+=y)
        '''
    }

    void testDoubleXDoubleInferredType() {
        assertScript '''
            double x = 1
            double y = 2
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == double_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == double_TYPE
            })
            def zp = x+y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == double_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == double_TYPE
            })
            def zm = x*y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == double_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == double_TYPE
            })
            def zmi = x-y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == double_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == double_TYPE
            })
            def zd = x/y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == double_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == double_TYPE
            })
            def zmod = x%y

            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == double_TYPE
                def right = node.rightExpression
                assert right.getNodeMetaData(INFERRED_TYPE) == double_TYPE
            })
            def zpeq = (x+=y)
        '''
    }

    void testIntUnaryMinusInferredType() {
        assertScript '''
            int x = 1
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
            })
            def y = -x
        '''
    }

    void testShortUnaryMinusInferredType() {
        assertScript '''
            short x = 1
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == short_TYPE
            })
            def y = -x
        '''
    }

    void testByteUnaryMinusInferredType() {
        assertScript '''
            byte x = 1
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == byte_TYPE
            })
            def y = -x
        '''
    }

    void testLongUnaryMinusInferredType() {
        assertScript '''
            long x = 1
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == long_TYPE
            })
            def y = -x
        '''
    }

    void testFloatUnaryMinusInferredType() {
        assertScript '''
            float x = 1
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == float_TYPE
            })
            def y = -x
        '''
    }

    void testDoubleUnaryMinusInferredType() {
        assertScript '''
            double x = 1
            @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                assert node.getNodeMetaData(INFERRED_TYPE) == double_TYPE
            })
            def y = -x
        '''
    }

    // GROOVY-5834
    void testCreatePatternInField() {
        assertScript '''
            class Sample {
                def pattern = ~'foo|bar'
                void test() {
                    assert pattern instanceof java.util.regex.Pattern
                }
            }
            new Sample().test()
        '''
    }

    // GROOVY-6223
    void testShouldNotRequireExplicitTypeDefinition() {
        assertScript '''
        def i = 0
        def j = 0

        int x = i++
        int y = ++j
        assert x == 0
        assert y == 1
        '''
    }
}

