package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*

/**
 * Generic tool for replacing parts of an AST
 *
 * It takes care to handle expressions as well since those are needed a few time for @TailRecursive
 *
 * @author Johannes Link
 */
class ASTNodesReplacer extends CodeVisitorSupport {

	Map<ASTNode, ASTNode> replace = [:]
	Closure when = { replace.containsKey it}
	Closure replaceWith = { replace[it] }
    int closureLevel = 0

    void replaceIn(ASTNode root) {
		root.visit(this)
	}

    public void visitClosureExpression(ClosureExpression expression) {
        closureLevel++
        super.visitClosureExpression(expression)
        closureLevel--
    }

    public void visitBlockStatement(BlockStatement block) {
		block.statements.clone().eachWithIndex { Statement statement, index ->
			replaceIfNecessary(statement) {block.statements[index] = it}
		}
		super.visitBlockStatement(block);
	}

	public void visitIfElse(IfStatement ifElse) {
		replaceIfNecessary(ifElse.booleanExpression) { ifElse.booleanExpression = it }
		replaceIfNecessary(ifElse.ifBlock) { ifElse.ifBlock = it }
		replaceIfNecessary(ifElse.elseBlock) { ifElse.elseBlock = it }
		super.visitIfElse(ifElse);
	}

	public void visitBinaryExpression(BinaryExpression expression) {
		replaceIfNecessary(expression.leftExpression) {expression.leftExpression = it}
		replaceIfNecessary(expression.rightExpression) {expression.rightExpression = it}
		super.visitBinaryExpression(expression);
	}

	public void visitReturnStatement(ReturnStatement statement) {
		replaceInnerExpressionIfNecessary(statement)
		super.visitReturnStatement(statement);
	}

	public void visitMethodCallExpression(MethodCallExpression call) {
		replaceIfNecessary(call.objectExpression) {call.objectExpression = it}
		super.visitMethodCallExpression(call);
	}

    public void visitSwitch(SwitchStatement statement) {
        replaceInnerExpressionIfNecessary(statement)
        super.visitSwitch(statement)
    }

    public void visitCaseStatement(CaseStatement statement) {
        replaceInnerExpressionIfNecessary(statement)
        super.visitCaseStatement(statement)
    }


    protected void visitListOfExpressions(List<? extends Expression> list) {
		list.clone().eachWithIndex { Expression expression, index ->
			replaceIfNecessary(expression) {list[index] = it}
		}
		super.visitListOfExpressions(list)
	}

	private replaceIfNecessary(ASTNode nodeToCheck, Closure replacementCode) {
		if (conditionFulfilled(nodeToCheck)) {
			def replacement = replaceWith(nodeToCheck)
			replacementCode(replacement)
		}
	}

    private boolean conditionFulfilled(ASTNode nodeToCheck) {
        if (when.maximumNumberOfParameters < 2)
            return when(nodeToCheck)
        else
            return when(nodeToCheck, isInClosure())
    }

    private boolean isInClosure() {
        closureLevel > 0
    }

    private void replaceInnerExpressionIfNecessary(statement) {
		replaceIfNecessary(statement.expression) {statement.expression = it}
	}

    //todo: test
	public void visitExpressionStatement(ExpressionStatement statement) {
		replaceIfNecessary(statement.expression) {statement.expression = it}
		super.visitExpressionStatement(statement);
	}

	//todo: test
	public void visitForLoop(ForStatement forLoop) {
		replaceIfNecessary(forLoop.collectionExpression) {forLoop.collectionExpression = it}
		replaceIfNecessary(forLoop.loopBlock) {forLoop.loopBlock = it}
		super.visitForLoop(forLoop);
	}

	//todo: test
	public void visitWhileLoop(WhileStatement loop) {
		replaceIfNecessary(loop.booleanExpression) {loop.booleanExpression = it}
		replaceIfNecessary(loop.loopBlock) {loop.loopBlock = it}
		super.visitWhileLoop(loop);
	}

	//todo: test
	public void visitDoWhileLoop(DoWhileStatement loop) {
		replaceIfNecessary(loop.booleanExpression) {loop.booleanExpression = it}
		replaceIfNecessary(loop.loopBlock) {loop.loopBlock = it}
		super.visitDoWhileLoop(loop);
	}

	//todo: test
	public void visitThrowStatement(ThrowStatement statement) {
		replaceInnerExpressionIfNecessary(statement)
		super.visitThrowStatement(statement)
	}

	//todo: test
	public void visitPostfixExpression(PostfixExpression expression) {
		replaceInnerExpressionIfNecessary(expression)
		super.visitPostfixExpression(expression);
	}

	//todo: test
	public void visitPrefixExpression(PrefixExpression expression) {
		replaceInnerExpressionIfNecessary(expression)
		super.visitPrefixExpression(expression);
	}

	//todo: test
	public void visitBooleanExpression(BooleanExpression expression) {
        //BooleanExpression.expression is readonly
        //replaceInnerExpressionIfNecessary(expression)
		super.visitBooleanExpression(expression);
	}

	//todo: test
	public void visitNotExpression(NotExpression expression) {
        //NotExpression.expression is readonly
		//replaceInnerExpressionIfNecessary(expression)
		super.visitNotExpression(expression);
	}

	//todo: test
	public void visitSpreadExpression(SpreadExpression expression) {
        //SpreadExpression.expression is readonly
		//replaceInnerExpressionIfNecessary(expression)
		super.visitSpreadExpression(expression);
	}

	//todo: test
	public void visitSpreadMapExpression(SpreadMapExpression expression) {
		replaceInnerExpressionIfNecessary(expression)
		super.visitSpreadMapExpression(expression);
	}

	//todo: test
	public void visitUnaryMinusExpression(UnaryMinusExpression expression) {
        //UnaryMinusExpression.expression is readonly
		//replaceInnerExpressionIfNecessary(expression)
		super.visitUnaryMinusExpression(expression);
	}

	//todo: test
	public void visitUnaryPlusExpression(UnaryPlusExpression expression) {
        //UnaryPlusExpression.expression is readonly
		//replaceInnerExpressionIfNecessary(expression)
		super.visitUnaryPlusExpression(expression);
	}

	//todo: test
	public void visitBitwiseNegationExpression(BitwiseNegationExpression expression) {
		replaceInnerExpressionIfNecessary(expression)
		super.visitBitwiseNegationExpression(expression);
	}

	//todo: test
	public void visitCastExpression(CastExpression expression) {
		replaceInnerExpressionIfNecessary(expression)
		super.visitCastExpression(expression);
	}

	//todo: test
	public void visitMapEntryExpression(MapEntryExpression expression) {
		replaceIfNecessary(expression.keyExpression) {expression.keyExpression = it}
		replaceIfNecessary(expression.valueExpression) {expression.valueExpression = it}
		super.visitMapEntryExpression(expression);
	}

	//todo: test
	public void visitTernaryExpression(TernaryExpression expression) {
		replaceIfNecessary(expression.booleanExpression) {expression.booleanExpression = it}
		replaceIfNecessary(expression.trueExpression) {expression.trueExpression = it}
		replaceIfNecessary(expression.falseExpression) {expression.falseExpression = it}
		super.visitTernaryExpression(expression);
	}

	//todo: test
	public void visitAssertStatement(AssertStatement statement) {
        replaceIfNecessary(statement.booleanExpression) {statement.booleanExpression = it}
        replaceIfNecessary(statement.messageExpression) {statement.messageExpression= it}
        super.visitAssertStatement(statement)
	}

	//todo: test
	public void visitSynchronizedStatement(SynchronizedStatement statement) {
        replaceInnerExpressionIfNecessary(statement)
        super.visitSynchronizedStatement(statement)
	}

	//todo: test
	public void visitRangeExpression(RangeExpression expression) {
        replaceIfNecessary(expression.from) {expression.from = it}
        replaceIfNecessary(expression.to) {expression.to = it}
        super.visitRangeExpression(expression)
	}

	//todo: test
	public void visitMethodPointerExpression(MethodPointerExpression expression) {
        replaceInnerExpressionIfNecessary(expression)
        replaceIfNecessary(expression.methodName) {expression.methodName = it}
        super.visitMethodPointerExpression(expression)
	}

	//todo: test
	public void visitPropertyExpression(PropertyExpression expression) {
        replaceIfNecessary(expression.objectExpression) {expression.objectExpression = it}
        replaceIfNecessary(expression.property) {expression.property = it}
        super.visitPropertyExpression(expression)
	}

	//todo: test
	public void visitAttributeExpression(AttributeExpression expression) {
        replaceIfNecessary(expression.objectExpression) {expression.objectExpression = it}
        replaceIfNecessary(expression.property) {expression.property = it}
        super.visitAttributeExpression(expression)
	}

	//todo: test
	public void visitCatchStatement(CatchStatement statement) {
        replaceIfNecessary(statement.variable) {statement.variable = it}
        super.visitCatchStatement(statement)
	}
}
