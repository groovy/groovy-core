/*
 * Copyright 2003-2009 the original author or authors.
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
package org.codehaus.groovy.transform.sc;

import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ExpressionTransformer;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is used internally by the compiler to transform expressions
 * like multiple assignments into a list of assignments.
 *
 * @author Cedric Champeau
 */
public class ListOfExpressionsExpression extends Expression {

    private final List<Expression> expressions;

    public ListOfExpressionsExpression() {
        expressions = new LinkedList<Expression>();
    }

    public ListOfExpressionsExpression(final List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public Expression transformExpression(final ExpressionTransformer transformer) {
        return new ListOfExpressionsExpression(transformExpressions(expressions,transformer));
    }

    @Override
    public void visit(final GroovyCodeVisitor visitor) {
        for (Expression expression : expressions) {
            expression.visit(visitor);
        }
    }

    public void addExpression(Expression expression) {
        expressions.add(expression);
    }
}
