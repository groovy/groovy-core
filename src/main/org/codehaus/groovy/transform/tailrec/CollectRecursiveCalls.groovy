package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;

/**
 * @author Johannes Link
 */
class CollectRecursiveCalls extends CodeVisitorSupport {
	MethodNode method
	List recursiveCalls = []

	public void visitMethodCallExpression(MethodCallExpression call) {
		if (isRecursive(call)) {
			recursiveCalls << call
		}
        super.visitMethodCallExpression(call)
    }

	public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
		if (isRecursive(call)) {
            recursiveCalls << call
        }
		super.visitStaticMethodCallExpression(call)
	}
	
	private boolean isRecursive(call) {
		new RecursivenessTester().isRecursive(method: method, call: call)
	}
	
	synchronized List collect(method) {
		recursiveCalls.clear()
		this.method = method
		this.method.code.visit(this)
		recursiveCalls
	}
}
