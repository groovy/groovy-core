/*
 * Copyright 2003-2013 the original author or authors.
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
package org.codehaus.groovy.control;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.Verifier;

/**
 * Visitor to resolve constants in annotation definitions.
 *
 * @author Paul King
 */
public class AnnotationConstantsVisitor extends ClassCodeVisitorSupport {
    private SourceUnit source;
    private boolean inAnnotationDef;

    public void visitClass(ClassNode node, SourceUnit source) {
        this.source = source;
        this.inAnnotationDef = node.isAnnotationDefinition();
        super.visitClass(node);
        this.inAnnotationDef = false;
    }

    @Override
    protected void visitConstructorOrMethod(MethodNode node, boolean isConstructor) {
        if (!inAnnotationDef) return;
        visitStatement(node.getFirstStatement(), node.getReturnType());
    }

    private void visitStatement(Statement statement, ClassNode returnType) {
        if (statement instanceof ReturnStatement) {
            ReturnStatement rs = (ReturnStatement) statement;
            rs.setExpression(transformConstantExpression(rs.getExpression(), returnType));
        }
    }

    private Expression transformConstantExpression(Expression val, ClassNode returnType) {
        ClassNode returnWrapperType = ClassHelper.getWrapper(returnType);
        if (val instanceof ConstantExpression) {
            Expression result = revertType(val, returnWrapperType);
            if (result != null) {
                return result;
            }
            return val;
        }
        if (val instanceof CastExpression) {
            CastExpression castExp = (CastExpression) val;
            Expression castee = castExp.getExpression();
            if (castee instanceof ConstantExpression) {
                if (ClassHelper.getWrapper(castee.getType()).isDerivedFrom(returnWrapperType)) {
                    return castee;
                }
                Expression result = revertType(castee, returnWrapperType);
                if (result != null) {
                    return result;
                }
                return castee;
            }
        }
        return val;
    }

    private Expression revertType(Expression val, ClassNode returnWrapperType) {
        ClassNode valWrapperType = ClassHelper.getWrapper(val.getType());
        ConstantExpression ce = (ConstantExpression) val;
        if (ClassHelper.Character_TYPE.equals(returnWrapperType) && ClassHelper.STRING_TYPE.equals(val.getType())) {
            return configure(val, Verifier.transformToPrimitiveConstantIfPossible((ConstantExpression) val));
        }
        if (ClassHelper.Integer_TYPE.equals(valWrapperType)) {
            Integer i = (Integer) ce.getValue();
            if (ClassHelper.Character_TYPE.equals(returnWrapperType)) {
                return configure(val, new ConstantExpression((char) i.intValue()));
            }
            if (ClassHelper.Short_TYPE.equals(returnWrapperType)) {
                return configure(val, new ConstantExpression(i.shortValue()));
            }
            if (ClassHelper.Byte_TYPE.equals(returnWrapperType)) {
                return configure(val, new ConstantExpression(i.byteValue()));
            }
        }
        return null;
    }

    private Expression configure(Expression orig, Expression result) {
        result.setSourcePosition(orig);
        return result;
    }

    protected SourceUnit getSourceUnit() {
        return source;
    }
}
