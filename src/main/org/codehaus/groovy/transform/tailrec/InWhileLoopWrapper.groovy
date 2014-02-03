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

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement

/**
 * @author Johannes Link
 */
class InWhileLoopWrapper {
	
	final static String LOOP_LABEL = '_RECUR_HERE_'
    final static GotoRecurHereException  LOOP_EXCEPTION = new GotoRecurHereException()

	void wrap(MethodNode method) {
		BlockStatement oldBody = method.code
        TryCatchStatement tryCatchStatement = new TryCatchStatement(
                oldBody,
                new EmptyStatement()
        )
        tryCatchStatement.addCatch(new CatchStatement(
                new Parameter(ClassHelper.make(GotoRecurHereException), 'ignore'),
                new ContinueStatement(InWhileLoopWrapper.LOOP_LABEL)
        ))

        WhileStatement whileLoop = new WhileStatement(
                new BooleanExpression(new ConstantExpression(true)),
                new BlockStatement([tryCatchStatement], new VariableScope(method.variableScope))
        )
		if (whileLoop.loopBlock.statements.size() > 0)
			whileLoop.loopBlock.statements[0].statementLabel = LOOP_LABEL
		BlockStatement newBody = new BlockStatement([], new VariableScope(method.variableScope))
		newBody.addStatement(whileLoop)
		method.code = newBody
	}
}

class GotoRecurHereException extends Throwable {

}
