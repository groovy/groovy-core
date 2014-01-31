package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstAssert
import org.codehaus.groovy.ast.builder.AstBuilder
import org.junit.Test

import static org.objectweb.asm.Opcodes.ACC_PUBLIC

/**
 * @author Johannes Link
 */
class InWhileLoopWrapperTest {

	InWhileLoopWrapper wrapper = new InWhileLoopWrapper()

	@Test
	public void wrapWholeMethodBody() throws Exception {
		MethodNode methodToWrap = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, int.class) {
				parameters {}
				exceptions {}
				block { returnStatement{ constant 2 } }
			}
		}[0]
		
		MethodNode expectedWrap = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, int.class) {
				parameters {}
				exceptions {}
				block {
					whileStatement {
						booleanExpression { constant true }
                        block {
                            tryCatch {
                                block {
                                    returnStatement{  constant 2 }
                                }
                                empty() //finally block
                                catchStatement {
                                    parameter 'ignore': GotoRecurHereException.class
                                    //block {
                                        continueStatement {
                                            label InWhileLoopWrapper.LOOP_LABEL
                                        }
                                    //}
                                }
                            }
                        }
					}
				}
			}
		}[0]
		
		wrapper.wrap(methodToWrap)
		AstAssert.assertSyntaxTree([expectedWrap], [methodToWrap])
		assert methodToWrap.code.statements[0].loopBlock.statements[0].statementLabel == InWhileLoopWrapper.LOOP_LABEL
	}
}
