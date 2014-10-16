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
package org.codehaus.groovy.classgen.asm.sc;

import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.classgen.AsmClassGenerator;
import org.codehaus.groovy.transform.sc.ListOfExpressionsExpression;
import org.codehaus.groovy.transform.sc.TemporaryVariableExpression;

import java.util.Arrays;

/**
 * Contains helper methods aimed at facilitating the generation of statically compiled bytecode for property access.
 *
 * @author Cédric Champeau
 * @since 2.4.0
 */
public abstract class StaticPropertyAccessHelper {
    public static Expression transformToSetterCall(
            Expression receiver,
            MethodNode setterMethod,
            final Expression arguments,
            boolean implicitThis,
            boolean safe,
            boolean spreadSafe,
            boolean requiresReturnValue,
            Expression location) {
        if (requiresReturnValue) {
            final TemporaryVariableExpression tmp = new TemporaryVariableExpression(arguments);
            MethodCallExpression call = new MethodCallExpression(
                    receiver,
                    setterMethod.getName(),
                    tmp
            ) {
                @Override
                public void visit(final GroovyCodeVisitor visitor) {
                    super.visit(visitor);
                    if (visitor instanceof AsmClassGenerator) {
                        // ignore the return of the call
                        ((AsmClassGenerator) visitor).getController().getOperandStack().pop();
                    }
                }
            };
            call.setImplicitThis(implicitThis);
            call.setSafe(safe);
            call.setSpreadSafe(spreadSafe);
            call.setMethodTarget(setterMethod);
            call.setSourcePosition(location);
            ListOfExpressionsExpression result = new ListOfExpressionsExpression(
                    Arrays.asList(
                            tmp,
                            call
                    )
            ) {
                @Override
                public void visit(final GroovyCodeVisitor visitor) {
                    super.visit(visitor);
                    if (visitor instanceof AsmClassGenerator) {
                        tmp.remove(((AsmClassGenerator) visitor).getController());
                    }
                }
            };
            result.setSourcePosition(location);
            return result;
        } else {
            MethodCallExpression call = new MethodCallExpression(
                    receiver,
                    setterMethod.getName(),
                    arguments
            );
            call.setImplicitThis(implicitThis);
            call.setSafe(safe);
            call.setSpreadSafe(spreadSafe);
            call.setMethodTarget(setterMethod);
            call.setSourcePosition(location);
            return call;
        }
    }
}
