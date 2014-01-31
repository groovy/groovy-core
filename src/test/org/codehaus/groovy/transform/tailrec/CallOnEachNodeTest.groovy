package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.builder.AstBuilder
import org.junit.Test

import static org.objectweb.asm.Opcodes.ACC_PUBLIC

/**
 * @author Johannes Link
 */
class CallOnEachNodeTest {

	def visitor = new CallOnEachNode()

	@Test
	public void rootNode() {
		def myMethod = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]

		assertOnEachNode(callOn: myMethod.code, wasCalled: myMethod.code)
		assertOnEachNode(callOn: myMethod.code, wasCalledWithParent: [(myMethod.code): null])
	}

	@Test
	public void statementsInBlock() {
		def myMethod = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {}
				exceptions {}
				block {
					expression { constant 1  }
					returnStatement {constant null}
				}
			}
		}[0]

		assertOnEachNode(callOn: myMethod.code, wasCalled: myMethod.code.statements)
		assertOnEachNode(callOn: myMethod.code, wasCalledWithParent: [(myMethod.code.statements[0]): myMethod.code])
	}

	@Test
	public void expressions() {
		def myExpression = new AstBuilder().buildFromSpec { expression { constant 1 } }[0]

		assertOnEachNode(callOn: myExpression, wasCalled: myExpression.expression)
		assertOnEachNode(callOn: myExpression, wasCalledWithParent: [(myExpression.expression): myExpression])
	}

	@Test
	public void returns() {
		def myReturn = new AstBuilder().buildFromSpec { returnStatement { constant 1 } }[0]

		assertOnEachNode(callOn: myReturn, wasCalled: myReturn.expression)
		assertOnEachNode(callOn: myReturn, wasCalledWithParent: [(myReturn.expression): myReturn])
	}

	@Test
	public void methodCall() {
		def myMethodCall = new AstBuilder().buildFromSpec {
			methodCall {
				variable "this"
				constant "println"
				argumentList { constant "Hello" }
			}
		}[0]

		assertOnEachNode(callOn: myMethodCall, wasCalled: [
			myMethodCall.objectExpression,
			myMethodCall.method,
			myMethodCall.arguments
		])
		assertOnEachNode(callOn: myMethodCall, wasCalledWithParent: [(myMethodCall.objectExpression): myMethodCall])
	}

	@Test
	public void staticMethodCall() {
		def myMethodCall = new AstBuilder().buildFromSpec {
			staticMethodCall(Math, "min") {
				argumentList {
					constant 1
					constant 2
				}
			}
		}[0]

		assertOnEachNode(callOn: myMethodCall, wasCalled: [myMethodCall.arguments])
		assertOnEachNode(callOn: myMethodCall, wasCalledWithParent: [(myMethodCall.arguments): myMethodCall])
	}

	private void assertOnEachNode(params) {
		if (params.wasCalled) {
			def calledOn = []
			visitor.onEachNode(params.callOn) {calledOn << it}
			params.wasCalled.each({
				assert calledOn.contains(it), "$it should have been called"
			})
		}
		if (params.wasCalledWithParent) {
			def calledWithParent = [:]
			visitor.onEachNode(params.callOn) {node, parent ->
				calledWithParent[node] = parent
			}
			params.wasCalledWithParent.each({key, value ->
				assert calledWithParent[key] == value, "$key should have been called with parent $value"
			})
		}
	}
}
