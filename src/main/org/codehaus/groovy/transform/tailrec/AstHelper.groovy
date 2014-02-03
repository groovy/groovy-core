/*
 * Copyright 2008-2012 the original author or authors.
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
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.syntax.Token

import java.lang.reflect.Modifier

/**
 * Helping to create a few standard AST constructs
 *
 * @author Johannes Link
 */
@CompileStatic
class AstHelper {

	static final Token ASSIGN = Token.newSymbol("=", -1, -1)
	static final Token PLUS = Token.newSymbol("+", -1, -1)

	static ExpressionStatement createVariableDefinition(String variableName, ClassNode variableType, Expression value, boolean variableShouldBeFinal = false ) {
        def newVariable = new VariableExpression(variableName, variableType)
        if (variableShouldBeFinal)
            newVariable.setModifiers(Modifier.FINAL)
        new ExpressionStatement(new DeclarationExpression(newVariable, AstHelper.ASSIGN, value))
	}

	static ExpressionStatement createVariableAlias(String aliasName, ClassNode variableType, String variableName ) {
		createVariableDefinition(aliasName, variableType, new VariableExpression(variableName, variableType), true)
	}

	static ExpressionStatement createAssignment(String variableName, ClassNode variableType, Expression value ) {
		new ExpressionStatement(new BinaryExpression(new VariableExpression(variableName, variableType), AstHelper.ASSIGN, value))
	}

    static VariableExpression createVariableReference(Map variableSpec) {
        new VariableExpression((String) variableSpec.name, (ClassNode) variableSpec.type)
    }

    /**
     * This statement should make the code jump to surrounding while loop's start label
     * Does not work from within Closures
     */
    static Statement recurStatement() {
        //continue _RECUR_HERE_
        new ContinueStatement(InWhileLoopWrapper.LOOP_LABEL)
    }

    /**
     * This statement will throw exception which will be caught and redirected to jump to surrounding while loop's start label
     * Also works from within Closures but is a tiny bit slower
     */
    static Statement recurByThrowStatement() {
        // throw InWhileLoopWrapper.LOOP_EXCEPTION
        new ThrowStatement(new PropertyExpression(new ClassExpression(ClassHelper.make(InWhileLoopWrapper)), 'LOOP_EXCEPTION'))
    }
}
