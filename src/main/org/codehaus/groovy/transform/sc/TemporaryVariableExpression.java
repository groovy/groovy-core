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
package org.codehaus.groovy.transform.sc;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ExpressionTransformer;
import org.codehaus.groovy.classgen.AsmClassGenerator;
import org.codehaus.groovy.classgen.asm.ExpressionAsVariableSlot;
import org.codehaus.groovy.classgen.asm.WriterController;

/**
 * A front-end class for {@link org.codehaus.groovy.classgen.asm.ExpressionAsVariableSlot} which
 * allows defining temporary variables loaded from variable slots directly at the AST level,
 * without any knowledge of {@link org.codehaus.groovy.classgen.AsmClassGenerator}.
 *
 * @author Cédric Champeau
 * @since 2.4.0
 */
public class TemporaryVariableExpression extends Expression {

    private Expression expression;

    private ExpressionAsVariableSlot variable;

    public TemporaryVariableExpression(final Expression expression) {
        this.expression = expression;
    }

    @Override
    public Expression transformExpression(final ExpressionTransformer transformer) {
        expression = expression.transformExpression(transformer);
        return this;
    }

    @Override
    public void visit(final GroovyCodeVisitor visitor) {
        if (visitor instanceof AsmClassGenerator) {
            if (variable==null) {
                AsmClassGenerator acg = (AsmClassGenerator) visitor;
                WriterController controller = acg.getController();
                variable = new ExpressionAsVariableSlot(controller, expression);
            }
            variable.visit(visitor);
        } else {
            expression.visit(visitor);
        }
    }

    public void remove(WriterController controller) {
        controller.getCompileStack().removeVar(variable.getIndex());
    }

    @Override
    public ClassNode getType() {
        return expression.getType();
    }
}
