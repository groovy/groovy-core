package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement

/**
 * @author Johannes Link
 */
class CallOnEachNode extends CodeVisitorSupport {

	private Closure closure
	
	def synchronized onEachNode(node, closure) {
		this.closure = closure
		callOn(node, null)
		node.visit(this)
	}
	
	private callOn(node, parent) {
		if (closure.getParameterTypes().size() < 2) {
			closure(node)
		} else {
			closure(node, parent)
		}
	}
	
	@Override
	void visitBlockStatement(BlockStatement block) {
        for (Statement statement : block.getStatements()) {
            callOn(statement, block)
        }
		super.visitBlockStatement(block)
	}
	
	@Override
    void visitExpressionStatement(ExpressionStatement statement) {
        callOn(statement.getExpression(), statement)
		super.visitExpressionStatement(statement)
    }
	
	@Override
	public void visitReturnStatement(ReturnStatement statement) {
        callOn(statement.getExpression(), statement)
		super.visitReturnStatement(statement)
	}

	@Override
	public void visitMethodCallExpression(MethodCallExpression call) {
		callOn(call.getObjectExpression(), call);
		callOn(call.getMethod(), call);
		callOn(call.getArguments(), call);
		super.visitMethodCallExpression(call)
	}

	@Override
	public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
		callOn(call.getArguments(), call);
		super.visitStaticMethodCallExpression(call);
	}
}
