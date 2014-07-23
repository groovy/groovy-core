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
package org.codehaus.groovy.transform.sc.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;

import java.util.List;

public class MethodCallExpressionTransformer {
    private final StaticCompilationTransformer staticCompilationTransformer;

    public MethodCallExpressionTransformer(StaticCompilationTransformer staticCompilationTransformer) {
        this.staticCompilationTransformer = staticCompilationTransformer;
    }

    Expression transformMethodCallExpression(final MethodCallExpression expr) {
        Expression objectExpression = expr.getObjectExpression();
        ClassNode type = staticCompilationTransformer.getTypeChooser().resolveType(objectExpression, staticCompilationTransformer.getClassNode());
        if (isCallOnClosure(expr)) {
            FieldNode field = staticCompilationTransformer.getClassNode().getField(expr.getMethodAsString());
            if (field != null) {
                VariableExpression vexp = new VariableExpression(field);
                MethodCallExpression result = new MethodCallExpression(
                        vexp,
                        "call",
                        staticCompilationTransformer.transform(expr.getArguments())
                );
                result.setImplicitThis(false);
                result.setSourcePosition(expr);
                result.setSafe(expr.isSafe());
                result.setSpreadSafe(expr.isSpreadSafe());
                result.setMethodTarget(StaticTypeCheckingVisitor.CLOSURE_CALL_VARGS);
                return result;
            }
        }
        if (type != null && type.isArray()) {
            String method = expr.getMethodAsString();
            ClassNode componentType = type.getComponentType();
            if ("getAt".equals(method)) {
                Expression arguments = expr.getArguments();
                if (arguments instanceof TupleExpression) {
                    List<Expression> argList = ((TupleExpression) arguments).getExpressions();
                    if (argList.size() == 1) {
                        Expression indexExpr = argList.get(0);
                        ClassNode argType = staticCompilationTransformer.getTypeChooser().resolveType(indexExpr, staticCompilationTransformer.getClassNode());
                        ClassNode indexType = ClassHelper.getWrapper(argType);
                        if (componentType.isEnum() && ClassHelper.Number_TYPE == indexType) {
                            // workaround for generated code in enums which use .next() returning a Number
                            indexType = ClassHelper.Integer_TYPE;
                        }
                        if (argType != null && ClassHelper.Integer_TYPE == indexType) {
                            BinaryExpression binaryExpression = new BinaryExpression(
                                    objectExpression,
                                    Token.newSymbol("[", indexExpr.getLineNumber(), indexExpr.getColumnNumber()),
                                    indexExpr
                            );
                            binaryExpression.putNodeMetaData(StaticTypesMarker.INFERRED_TYPE, componentType);
                            return staticCompilationTransformer.transform(binaryExpression);
                        }
                    }
                }
            } else if ("putAt".equals(method)) {
                Expression arguments = expr.getArguments();
                if (arguments instanceof TupleExpression) {
                    List<Expression> argList = ((TupleExpression) arguments).getExpressions();
                    if (argList.size() == 2) {
                        Expression indexExpr = argList.get(0);
                        Expression objExpr = argList.get(1);
                        ClassNode argType = staticCompilationTransformer.getTypeChooser().resolveType(indexExpr, staticCompilationTransformer.getClassNode());
                        if (argType != null && ClassHelper.Integer_TYPE == ClassHelper.getWrapper(argType)) {
                            BinaryExpression arrayGet = new BinaryExpression(
                                    objectExpression,
                                    Token.newSymbol("[", indexExpr.getLineNumber(), indexExpr.getColumnNumber()),
                                    indexExpr
                            );
                            arrayGet.putNodeMetaData(StaticTypesMarker.INFERRED_TYPE, componentType);
                            BinaryExpression assignment = new BinaryExpression(
                                    arrayGet,
                                    Token.newSymbol("=", objExpr.getLineNumber(), objExpr.getColumnNumber()),
                                    objExpr
                            );
                            return staticCompilationTransformer.transform(assignment);
                        }
                    }
                }
            }
        }
        return staticCompilationTransformer.superTransform(expr);
    }

    private boolean isCallOnClosure(final MethodCallExpression expr) {
        return expr.isImplicitThis()
                && expr.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET) == StaticTypeCheckingVisitor.CLOSURE_CALL_VARGS
                && !"call".equals(expr.getMethodAsString());
    }

}