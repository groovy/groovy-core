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

import java.util.*;

import org.codehaus.groovy.ast.GroovyCodeVisitor;

/**
 * Represents a tuple expression {1, 2, 3} which creates an immutable List
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class TupleExpression extends Expression implements Iterable<Expression> {
    private List<Expression> expressions;

    public TupleExpression() {
        this(0);
    }

    public TupleExpression(Expression expr) {
        this(1);
        addExpression(expr);
    }

    public TupleExpression(Expression expr1, Expression expr2) {
        this(2);
        addExpression(expr1);
        addExpression(expr2);
    }

    public TupleExpression(Expression expr1, Expression expr2, Expression expr3) {
        this(3);
        addExpression(expr1);
        addExpression(expr2);
        addExpression(expr3);
    }
    
    public TupleExpression(int length) {
        this.expressions = new ArrayList<Expression>(length);
    }
    
    public TupleExpression(List<Expression> expressions) {
        this.expressions = expressions;
    }
    
    public TupleExpression(Expression[] expressionArray) {
        this();
        expressions.addAll(Arrays.asList(expressionArray));
    }

    public TupleExpression addExpression(Expression expression) {
        expressions.add(expression);
        return this;
    }
    
    public List<Expression> getExpressions() {
        return expressions;
    }

    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitTupleExpression(this);
    }

    public Expression transformExpression(ExpressionTransformer transformer) {
        Expression ret = new TupleExpression(transformExpressions(getExpressions(), transformer)); 
        ret.setSourcePosition(this);
        ret.copyNodeMetaData(this);
        return ret;
    }

    public Expression getExpression(int i) {
        return expressions.get(i);
    }

    public String getText() {
        StringBuffer buffer = new StringBuffer("(");
        boolean first = true;
        for (Expression expression : expressions) {
            if (first) {
                first = false;
            }
            else {
                buffer.append(", ");
            }
            
            buffer.append(expression.getText());
        }
        buffer.append(")");
        return buffer.toString();
    }

    public String toString() {
        return super.toString() + expressions;
    }

    public Iterator<Expression> iterator() {
        return Collections.unmodifiableList(expressions).iterator();
    }
}
