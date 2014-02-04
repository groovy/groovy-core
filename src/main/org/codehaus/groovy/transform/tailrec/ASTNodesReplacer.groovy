/*
 * Copyright 2013-2014 the original author or authors.
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
package org.codehaus.groovy.transform.tailrec

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*

import java.lang.reflect.Method

/**
 * Generic tool for replacing parts of an AST.
 *
 * It takes care to handle expressions as well since those are needed a few times for @TailRecursive
 *
 * @author Johannes Link
 */
@CompileStatic
class ASTNodesReplacer extends CodeVisitorSupport {

    Map<ASTNode, ASTNode> replace = [:]
    Closure<Boolean> when = { ASTNode node -> replace.containsKey node }
    Closure<ASTNode> replaceWith = { ASTNode node -> replace[node] }
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
        List<Statement> copyOfStatements = new ArrayList<Statement>(block.statements)
        copyOfStatements.eachWithIndex { Statement statement, int index ->
            replaceIfNecessary(statement) { Statement node -> block.statements[index] = node }
        }
        super.visitBlockStatement(block);
    }

    public void visitIfElse(IfStatement ifElse) {
        replaceIfNecessary(ifElse.booleanExpression) { BooleanExpression ex -> ifElse.booleanExpression = ex }
        replaceIfNecessary(ifElse.ifBlock) { Statement s -> ifElse.ifBlock = s }
        replaceIfNecessary(ifElse.elseBlock) { Statement s -> ifElse.elseBlock = s }
        super.visitIfElse(ifElse);
    }

    public void visitBinaryExpression(BinaryExpression expression) {
        replaceIfNecessary(expression.leftExpression) { Expression ex -> expression.leftExpression = ex }
        replaceIfNecessary(expression.rightExpression) { Expression ex -> expression.rightExpression = ex }
        super.visitBinaryExpression(expression);
    }

    public void visitReturnStatement(ReturnStatement statement) {
        replaceInnerExpressionIfNecessary(statement)
        super.visitReturnStatement(statement);
    }

    public void visitMethodCallExpression(MethodCallExpression call) {
        replaceIfNecessary(call.objectExpression) { Expression ex -> call.objectExpression = ex }
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
        new ArrayList<? extends Expression>(list).eachWithIndex { Expression expression, int index ->
            replaceIfNecessary(expression) { Expression exp -> list[index] = exp }
        }
        super.visitListOfExpressions(list)
    }

    private void replaceIfNecessary(ASTNode nodeToCheck, Closure replacementCode) {
        if (conditionFulfilled(nodeToCheck)) {
            ASTNode replacement = replaceWith(nodeToCheck)
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

    private void replaceInnerExpressionIfNecessary(ASTNode node) {
        //Simulate Groovy's property access
        Method getExpressionMethod = node.class.getMethod('getExpression', new Class[0])
        Method setExpressionMethod = node.class.getMethod('setExpression', [Expression].toArray(new Class[1]))
        Expression expr = getExpressionMethod.invoke(node, new Object[0]) as Expression
        replaceIfNecessary(expr) { Expression ex ->
            setExpressionMethod.invoke(node, [ex].toArray())
        }
    }

    //todo: test
    public void visitExpressionStatement(ExpressionStatement statement) {
        replaceIfNecessary(statement.expression) { Expression ex -> statement.expression = ex }
        super.visitExpressionStatement(statement);
    }

    //todo: test
    public void visitForLoop(ForStatement forLoop) {
        replaceIfNecessary(forLoop.collectionExpression) { Expression ex -> forLoop.collectionExpression = ex }
        replaceIfNecessary(forLoop.loopBlock) { Statement s -> forLoop.loopBlock = s }
        super.visitForLoop(forLoop);
    }

    //todo: test
    public void visitWhileLoop(WhileStatement loop) {
        replaceIfNecessary(loop.booleanExpression) { BooleanExpression ex -> loop.booleanExpression = ex }
        replaceIfNecessary(loop.loopBlock) { Statement s -> loop.loopBlock = s }
        super.visitWhileLoop(loop);
    }

    //todo: test
    public void visitDoWhileLoop(DoWhileStatement loop) {
        replaceIfNecessary(loop.booleanExpression) { BooleanExpression ex -> loop.booleanExpression = ex }
        replaceIfNecessary(loop.loopBlock) { Statement s -> loop.loopBlock = s }
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
        //CastExpression.expression is readonly todo: Handle in VariableAccessReplacer or w/ reflection
        //replaceInnerExpressionIfNecessary(expression)
        super.visitCastExpression(expression);
    }

    //todo: test
    public void visitMapEntryExpression(MapEntryExpression expression) {
        replaceIfNecessary(expression.keyExpression) { Expression ex -> expression.keyExpression = ex }
        replaceIfNecessary(expression.valueExpression) { Expression ex -> expression.valueExpression = ex }
        super.visitMapEntryExpression(expression);
    }

    //todo: test
    public void visitTernaryExpression(TernaryExpression expression) {
        //TernaryExpression.*Expression are readonly todo: Handle in VariableAccessReplacer or w/ reflection
        //replaceIfNecessary(expression.booleanExpression) { BooleanExpression ex -> expression.booleanExpression = ex }
        //replaceIfNecessary(expression.trueExpression) { Expression ex -> expression.trueExpression = ex }
        //replaceIfNecessary(expression.falseExpression) { Expression ex -> expression.falseExpression = ex }
        super.visitTernaryExpression(expression);
    }

    //todo: test
    public void visitAssertStatement(AssertStatement statement) {
        replaceIfNecessary(statement.booleanExpression) { BooleanExpression ex -> statement.booleanExpression = ex }
        replaceIfNecessary(statement.messageExpression) { Expression ex -> statement.messageExpression = ex }
        super.visitAssertStatement(statement)
    }

    //todo: test
    public void visitSynchronizedStatement(SynchronizedStatement statement) {
        replaceInnerExpressionIfNecessary(statement)
        super.visitSynchronizedStatement(statement)
    }

    //todo: test
    public void visitRangeExpression(RangeExpression expression) {
        //RangeExpression.from/to are readonly todo: Handle in VariableAccessReplacer or w/ reflection
        //replaceIfNecessary(expression.from) { Expression ex -> expression.from = ex }
        //replaceIfNecessary(expression.to) { Expression ex -> expression.to = ex }
        super.visitRangeExpression(expression)
    }

    //todo: test
    public void visitMethodPointerExpression(MethodPointerExpression expression) {
        replaceInnerExpressionIfNecessary(expression)
        //MethodPointerExpression.methodName is readonly todo: Handle in VariableAccessReplacer or w/ reflection
        //replaceIfNecessary(expression.methodName) { Expression ex -> expression.methodName = ex }
        super.visitMethodPointerExpression(expression)
    }

    //todo: test
    public void visitPropertyExpression(PropertyExpression expression) {
        replaceIfNecessary(expression.objectExpression) { Expression ex -> expression.objectExpression = ex }
        //PropertyExpression.property is readonly todo: Handle in VariableAccessReplacer or w/ reflection
        //replaceIfNecessary(expression.property) { Expression ex -> expression.property = ex }
        super.visitPropertyExpression(expression)
    }

    //todo: test
    public void visitAttributeExpression(AttributeExpression expression) {
        replaceIfNecessary(expression.objectExpression) { Expression ex -> expression.objectExpression = ex }
        //AttributeExpression.property is readonly todo: Handle in VariableAccessReplacer or w/ reflection
        //replaceIfNecessary(expression.property) { Expression ex -> expression.property = ex }
        super.visitAttributeExpression(expression)
    }

    //todo: test
    public void visitCatchStatement(CatchStatement statement) {
        //CatchStatement.variable is readonly todo: Handle in VariableAccessReplacer or w/ reflection
        //replaceIfNecessary(statement.variable) { Parameter p -> statement.variable = p }
        super.visitCatchStatement(statement)
    }
}
