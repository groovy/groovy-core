package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.builder.AstAssert
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.junit.Test

/**
 * @author Johannes Link
 */
class ReturnStatementToIterationConverterTest {

    @Test
    public void oneConstantParameter() {
        ReturnStatement statement = new AstBuilder().buildFromSpec {
            returnStatement {
                methodCall {
                    variable "this"
                    constant "myMethod"
                    argumentList { constant 1 }
                }
            }
        }[0]
        statement.statementLabel = "aLabel"

        BlockStatement expected = new AstBuilder().buildFromSpec {
            block {
                expression {
                    binary {
                        variable '_a_'
                        token '='
                        constant 1
                    }
                }
                continueStatement {
                    label InWhileLoopWrapper.LOOP_LABEL
                }
            }
        }[0]

        Map positionMapping = [0: [name: '_a_', type: ClassHelper.DYNAMIC_TYPE]]
        def block = new ReturnStatementToIterationConverter().convert(statement, positionMapping)

        AstAssert.assertSyntaxTree([expected], [block])
        assert block.statementLabel == "aLabel"
    }

    @Test
    public void twoParametersOnlyOneUsedInRecursiveCall() {

        BlockStatement expected = new AstBuilder().buildFromSpec {
            block {
                expression {
                    declaration {
                        variable '__a__'
                        token '='
                        variable '_a_'
                    }
                }
                expression {
                    binary {
                        variable '_a_'
                        token '='
                        constant 1
                    }
                }
                expression {
                    binary {
                        variable '_b_'
                        token '='
                        binary {
                            variable '__a__'
                            token '+'
                            constant 1
                        }
                    }
                }
                continueStatement {
                    label InWhileLoopWrapper.LOOP_LABEL
                }
            }
        }[0]

        ReturnStatement statement = new AstBuilder().buildFromString("""
				return(myMethod(1, _a_ + 1))
		""")[0].statements[0]

        Map positionMapping = [0: [name: '_a_', type: ClassHelper.DYNAMIC_TYPE], 1: [name: '_b_', type: ClassHelper.DYNAMIC_TYPE]]
        def block = new ReturnStatementToIterationConverter().convert(statement, positionMapping)

        AstAssert.assertSyntaxTree([expected], [block])
    }

    @Test
    public void twoParametersBothUsedInRecursiveCall() {
        BlockStatement expected = new AstBuilder().buildFromSpec {
            block {
                expression {
                    declaration {
                        variable '__a__'
                        token '='
                        variable '_a_'
                    }
                }
                expression {
                    binary {
                        variable '_a_'
                        token '='
                        binary {
                            variable '__a__'
                            token '+'
                            constant 1
                        }
                    }
                }
                expression {
                    declaration {
                        variable '__b__'
                        token '='
                        variable '_b_'
                    }
                }
                expression {
                    binary {
                        variable '_b_'
                        token '='
                        binary {
                            variable '__b__'
                            token '+'
                            variable '__a__'
                        }
                    }
                }
                continueStatement {
                    label InWhileLoopWrapper.LOOP_LABEL
                }
            }
        }[0]

        ReturnStatement statement = new AstBuilder().buildFromString("""
		return(myMethod(_a_ + 1, _b_ + _a_))
				""")[0].statements[0]


        Map positionMapping = [0: [name: '_a_', type: ClassHelper.DYNAMIC_TYPE], 1: [name: '_b_', type: ClassHelper.DYNAMIC_TYPE]]
        def block = new ReturnStatementToIterationConverter().convert(statement, positionMapping)

        AstAssert.assertSyntaxTree([expected], [block])
    }

    @Test
    public void worksWithStaticMethods() {
        ReturnStatement statement = new ReturnStatement(new StaticMethodCallExpression(
                ClassHelper.make(Math, false), "min",
                new ArgumentListExpression(new ConstantExpression(1))))

        BlockStatement expected = new AstBuilder().buildFromSpec {
            block {
                expression {
                    binary {
                        variable '_a_'
                        token '='
                        constant 1
                    }
                }
                continueStatement {
                    label InWhileLoopWrapper.LOOP_LABEL
                }
            }
        }[0]

        Map positionMapping = [0: [name: '_a_', type: ClassHelper.DYNAMIC_TYPE]]
        def block = new ReturnStatementToIterationConverter().convert(statement, positionMapping)

        AstAssert.assertSyntaxTree([expected], [block])
    }
}
