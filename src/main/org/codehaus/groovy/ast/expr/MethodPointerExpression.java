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

import groovy.lang.Closure;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;

/**
 * Represents a method pointer on an object such as
 * foo.&bar which means find the method pointer on foo for the method called "bar"
 * which is equivalent to
 * <code>
 * foo.metaClass.getMethodPointer(foo, "bar")
 *
 * @version $Revision$
 */
public class MethodPointerExpression extends Expression {

    private Expression expression;
    private Expression methodName;

    public MethodPointerExpression(Expression expression, Expression methodName) {
        this.expression = expression;
        this.methodName = methodName;
    }

    public Expression getExpression() {
        if (expression == null)
            return VariableExpression.THIS_EXPRESSION;
        else
            return expression;
    }

    public Expression getMethodName() {
        return methodName;
    }

    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitMethodPointerExpression(this);
    }

    public Expression transformExpression(ExpressionTransformer transformer) {
        Expression ret;
        Expression mname = transformer.transform(methodName);
        if (expression == null) {
            ret = new MethodPointerExpression(VariableExpression.THIS_EXPRESSION, mname);
        } else {
            ret = new MethodPointerExpression(transformer.transform(expression), mname);
        }
        ret.setSourcePosition(this);
        ret.copyNodeMetaData(this);
        return ret;
    }

    public String getText() {
        if (expression == null) {
            return "&" + methodName;
        } else {
            return expression.getText() + ".&" + methodName.getText();
        }
    }

    public ClassNode getType() {
        return ClassHelper.CLOSURE_TYPE.getPlainNodeReference();
    }

    public boolean isDynamic() {
        return false;
    }

    public Class getTypeClass() {
        return Closure.class;
    }
}
