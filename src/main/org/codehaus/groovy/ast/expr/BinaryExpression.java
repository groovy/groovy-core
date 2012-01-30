/*
 * Copyright 2003-2007 the original author or authors.
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
package org.codehaus.groovy.ast.expr;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

/**
 * Represents two expressions and an operation
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class BinaryExpression extends Expression {

    private Expression leftExpression;
    private Expression rightExpression;
    private final Token operation;

    public BinaryExpression(Expression leftExpression,
                            Token operation,
                            Expression rightExpression) {
        this.leftExpression = leftExpression;
        this.operation = operation;
        this.rightExpression = rightExpression;
    }

    public String toString() {
        return super.toString() + "[" + leftExpression + operation + rightExpression + "]";
    }

    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitBinaryExpression(this);
    }

    public Expression transformExpression(ExpressionTransformer transformer) {
        Expression ret = new BinaryExpression(transformer.transform(leftExpression), operation, transformer.transform(rightExpression));
        ret.setSourcePosition(this);
        ret.copyNodeMetaData(this);
        return ret;
    }

    public Expression getLeftExpression() {
        return leftExpression;
    }

    public void setLeftExpression(Expression leftExpression) {
        this.leftExpression = leftExpression;
    }

    public void setRightExpression(Expression rightExpression) {
        this.rightExpression = rightExpression;
    }

    public Token getOperation() {
        return operation;
    }

    public Expression getRightExpression() {
        return rightExpression;
    }

    public String getText() {
        if (operation.getType() == Types.LEFT_SQUARE_BRACKET) {
            return leftExpression.getText() + "[" + rightExpression.getText() + "]";
        }
        return "(" + leftExpression.getText() + " " + operation.getText() + " " + rightExpression.getText() + ")";
    }


    /**
     * Creates an assignment expression in which the specified expression
     * is written into the specified variable name.
     */

    public static BinaryExpression newAssignmentExpression(Variable variable, Expression rhs) {
        VariableExpression lhs = new VariableExpression(variable);
        Token operator = Token.newPlaceholder(Types.ASSIGN);

        return new BinaryExpression(lhs, operator, rhs);
    }


    /**
     * Creates variable initialization expression in which the specified expression
     * is written into the specified variable name.
     */

    public static BinaryExpression newInitializationExpression(String variable, ClassNode type, Expression rhs) {
        VariableExpression lhs = new VariableExpression(variable);

        if (type != null) {
            lhs.setType(type);
        }

        Token operator = Token.newPlaceholder(Types.ASSIGN);

        return new BinaryExpression(lhs, operator, rhs);
    }

}
