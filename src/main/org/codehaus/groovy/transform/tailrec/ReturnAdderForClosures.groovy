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

import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.classgen.BytecodeSequence

/**
 * Adds explicit return statements to implicit return points in a closure. This is necessary since
 * tail-recursion is detected by having the recursive call within the return statement.
 *
 * Copied a lot of code from package org.codehaus.groovy.classgen.ReturnAdder which can only be used for Methods
 *
 * @author Johannes Link
 */
class ReturnAdderForClosures extends CodeVisitorSupport {

    /**
     * If set to 'true', then returns are effectively added. This is useful whenever you just want
     * to check what returns are produced without eventually adding them.
     */
    boolean doAdd = true;


    synchronized void addReturnsIfNeeded(MethodNode method) {
        method.code.visit(this)
    }

    public void visitClosureExpression(ClosureExpression expression) {
        expression.code = getStatementWithAddedReturns(expression.code, expression.variableScope)
        super.visitClosureExpression(expression)
    }

    Statement getStatementWithAddedReturns(Statement statement, VariableScope scope) {
        if (statement instanceof ReturnStatement
                || statement instanceof BytecodeSequence
                || statement instanceof ThrowStatement) {
            return statement;
        }

        if (statement instanceof EmptyStatement) {
            final ReturnStatement returnStatement = new ReturnStatement(ConstantExpression.NULL);
            return returnStatement;
        }

        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expStmt = (ExpressionStatement) statement;
            Expression expr = expStmt.getExpression();
            ReturnStatement ret = new ReturnStatement(expr);
            ret.setSourcePosition(expr);
            ret.setStatementLabel(statement.getStatementLabel());
            return ret;
        }

        if (statement instanceof SynchronizedStatement) {
            SynchronizedStatement sync = (SynchronizedStatement) statement;
            final org.codehaus.groovy.ast.stmt.Statement code = getStatementWithAddedReturns(sync.getCode(), scope);
            if (doAdd) sync.setCode(code);
            return sync;
        }

        if (statement instanceof IfStatement) {
            IfStatement ifs = (IfStatement) statement;
            final org.codehaus.groovy.ast.stmt.Statement ifBlock = getStatementWithAddedReturns(ifs.getIfBlock(), scope);
            final org.codehaus.groovy.ast.stmt.Statement elseBlock = getStatementWithAddedReturns(ifs.getElseBlock(), scope);
            if (doAdd) {
                ifs.setIfBlock(ifBlock);
                ifs.setElseBlock(elseBlock);
            }
            return ifs;
        }

        if (statement instanceof SwitchStatement) {
            SwitchStatement swi = (SwitchStatement) statement;
            for (CaseStatement caseStatement : swi.getCaseStatements()) {
                final org.codehaus.groovy.ast.stmt.Statement code = adjustSwitchCaseCode(caseStatement.getCode(), scope, false);
                if (doAdd) caseStatement.setCode(code);
            }
            final org.codehaus.groovy.ast.stmt.Statement defaultStatement = adjustSwitchCaseCode(swi.getDefaultStatement(), scope, true);
            if (doAdd) swi.setDefaultStatement(defaultStatement);
            return swi;
        }

        if (statement instanceof TryCatchStatement) {
            TryCatchStatement trys = (TryCatchStatement) statement;
            final org.codehaus.groovy.ast.stmt.Statement tryStatement = getStatementWithAddedReturns(trys.getTryStatement(), scope);
            if (doAdd) trys.setTryStatement(tryStatement);
            final int len = trys.getCatchStatements().size();
            for (int i = 0; i != len; ++i) {
                final CatchStatement catchStatement = trys.getCatchStatement(i);
                final org.codehaus.groovy.ast.stmt.Statement code = getStatementWithAddedReturns(catchStatement.getCode(), scope);
                if (doAdd) catchStatement.setCode(code);
            }
            return trys;
        }

        if (statement instanceof BlockStatement) {
            BlockStatement block = (BlockStatement) statement;

            final List list = block.getStatements();
            if (!list.isEmpty()) {
                int idx = list.size() - 1;
                org.codehaus.groovy.ast.stmt.Statement last = getStatementWithAddedReturns((org.codehaus.groovy.ast.stmt.Statement) list.get(idx), block.getVariableScope());
                if (doAdd) list.set(idx, last);
                if (!statementReturns(last)) {
                    final ReturnStatement returnStatement = new ReturnStatement(ConstantExpression.NULL);
                    if (doAdd) list.add(returnStatement);
                }
            } else {
                ReturnStatement ret = new ReturnStatement(ConstantExpression.NULL);
                ret.setSourcePosition(block);
                return ret;
            }

            BlockStatement newBlock = new BlockStatement(list, block.getVariableScope());
            newBlock.setSourcePosition(block);
            return newBlock;
        }

        if (statement == null) {
            final ReturnStatement returnStatement = new ReturnStatement(ConstantExpression.NULL);
            return returnStatement;
        } else {
            final List list = new ArrayList();
            list.add(statement);
            final ReturnStatement returnStatement = new ReturnStatement(ConstantExpression.NULL);
            list.add(returnStatement);

            BlockStatement newBlock = new BlockStatement(list, new VariableScope(scope));
            newBlock.setSourcePosition(statement);
            return newBlock;
        }
    }

    private Statement adjustSwitchCaseCode(Statement statement, VariableScope scope, boolean defaultCase) {
        if (statement instanceof BlockStatement) {
            final List list = ((BlockStatement) statement).getStatements();
            if (!list.isEmpty()) {
                int idx = list.size() - 1;
                org.codehaus.groovy.ast.stmt.Statement last = (org.codehaus.groovy.ast.stmt.Statement) list.get(idx);
                if (last instanceof BreakStatement) {
                    if (doAdd) {
                        list.remove(idx);
                        return getStatementWithAddedReturns(statement, scope);
                    } else {
                        BlockStatement newStmt = new BlockStatement();
                        for (int i = 0; i < idx; i++) {
                            newStmt.addStatement((org.codehaus.groovy.ast.stmt.Statement) list.get(i));
                        }
                        return getStatementWithAddedReturns(newStmt, scope);
                    }
                } else if (defaultCase) {
                    return getStatementWithAddedReturns(statement, scope);
                }
            }
        }
        return statement;
    }

    private static boolean statementReturns(Statement last) {
        return (
                last instanceof ReturnStatement ||
                        last instanceof BlockStatement ||
                        last instanceof IfStatement ||
                        last instanceof ExpressionStatement ||
                        last instanceof EmptyStatement ||
                        last instanceof TryCatchStatement ||
                        last instanceof BytecodeSequence ||
                        last instanceof ThrowStatement ||
                        last instanceof SynchronizedStatement
        );
    }

}
