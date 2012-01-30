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

/**
 * Represents a spread map expression *:m
 * in the map expression [1, *:m, 2, "c":100]
 * or in the method invoke expression func(1, *:m, 2, "c":100).
 *
 * @version $Revision$
 */
public class SpreadMapExpression extends Expression {

    private Expression expression;

    public SpreadMapExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitSpreadMapExpression(this);
    }

    public Expression transformExpression(ExpressionTransformer transformer) {
        Expression ret = new SpreadMapExpression(transformer.transform(expression));
        ret.setSourcePosition(this);
        ret.copyNodeMetaData(this);
        return ret;
    }

    public String getText() {
        return "*:" + expression.getText();
    }

    public ClassNode getType() {
        return expression.getType();
    }
}
