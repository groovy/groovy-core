package org.codehaus.groovy.transform.tailrec

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
        new VariableExpression(variableSpec.name, variableSpec.type)
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
