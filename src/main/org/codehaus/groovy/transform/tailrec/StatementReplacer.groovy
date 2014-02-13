/*
 * Copyright 2003-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.stmt.*

/**
 * Tool for replacing Statement objects in an AST by other Statement instances.
 *
 * Within @TailRecursive it is used to swap ReturnStatements with looping back to RECUR label
 *
 * @author Johannes Link
 */
@CompileStatic
class StatementReplacer extends CodeVisitorSupport {

    Closure<Boolean> when = { Statement node -> false }
    Closure<Statement> replaceWith = { Statement statement -> statement }
    int closureLevel = 0

    void replaceIn(ASTNode root) {
        root.visit(this)
    }

    public void visitClosureExpression(ClosureExpression expression) {
        closureLevel++
        try {
            super.visitClosureExpression(expression)
        } finally {
            closureLevel--
        }
    }

    public void visitBlockStatement(BlockStatement block) {
        List<Statement> copyOfStatements = new ArrayList<Statement>(block.statements)
        copyOfStatements.eachWithIndex { Statement statement, int index ->
            replaceIfNecessary(statement) { Statement node -> block.statements[index] = node }
        }
        super.visitBlockStatement(block);
    }

    public void visitIfElse(IfStatement ifElse) {
        replaceIfNecessary(ifElse.ifBlock) { Statement s -> ifElse.ifBlock = s }
        replaceIfNecessary(ifElse.elseBlock) { Statement s -> ifElse.elseBlock = s }
        super.visitIfElse(ifElse);
    }

    public void visitForLoop(ForStatement forLoop) {
        replaceIfNecessary(forLoop.loopBlock) { Statement s -> forLoop.loopBlock = s }
        super.visitForLoop(forLoop);
    }

    public void visitWhileLoop(WhileStatement loop) {
        replaceIfNecessary(loop.loopBlock) { Statement s -> loop.loopBlock = s }
        super.visitWhileLoop(loop);
    }

    public void visitDoWhileLoop(DoWhileStatement loop) {
        replaceIfNecessary(loop.loopBlock) { Statement s -> loop.loopBlock = s }
        super.visitDoWhileLoop(loop);
    }


    private void replaceIfNecessary(Statement nodeToCheck, Closure replacementCode) {
        if (conditionFulfilled(nodeToCheck)) {
            ASTNode replacement = replaceWith(nodeToCheck)
            replacement.setSourcePosition(nodeToCheck);
            replacement.copyNodeMetaData(nodeToCheck);
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

}
