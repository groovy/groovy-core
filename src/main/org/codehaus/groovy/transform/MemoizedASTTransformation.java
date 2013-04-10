/*
 * Copyright 2008-2013 the original author or authors.
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
package org.codehaus.groovy.transform;

import groovy.transform.Memoized;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.control.SourceUnit;

/**
 * Handles generation of code for the {@link Memoized} annotation.
 * 
 * @author Andrey Bloschetsov
 */
@GroovyASTTransformation
public class MemoizedASTTransformation extends AbstractASTTransformation {

    private static final String CLOSURE_CALL_METHOD_NAME = "call";
    private static final Class<Memoized> MY_CLASS = Memoized.class;
    private static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final String PROTECTED_CACHE_SIZE_NAME = "protectedCacheSize";
    private static final String MAX_CACHE_SIZE_NAME = "maxCacheSize";

    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (nodes == null) {
            return;
        }
        init(nodes, source);

        AnnotationNode annotationNode = (AnnotationNode) nodes[0];
        AnnotatedNode annotatedNode = (AnnotatedNode) nodes[1];
        if (MY_TYPE.equals(annotationNode.getClassNode()) && annotatedNode instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) annotatedNode;
            if (methodNode.isAbstract()) {
                addError("Error: annotation " + MY_TYPE_NAME + " can not be used for abstract method.", methodNode);
                return;
            }
            if (methodNode.isVoidMethod()) {
                addError("Error: annotation " + MY_TYPE_NAME + " can not be used for method that return void.",
                        methodNode);
                return;
            }

            ClosureExpression closureExpression = new ClosureExpression(methodNode.getParameters(),
                    methodNode.getCode());
            closureExpression.setVariableScope(methodNode.getVariableScope());

            ClassNode ownerClassNode = methodNode.getDeclaringClass();
            int modifiers = FieldNode.ACC_PRIVATE | FieldNode.ACC_FINAL;
            if (methodNode.isStatic()) {
                modifiers = modifiers | FieldNode.ACC_STATIC;
            }

            int protectedCacheSize = getIntMemberValue(annotationNode, PROTECTED_CACHE_SIZE_NAME);
            int maxCacheSize = getIntMemberValue(annotationNode, MAX_CACHE_SIZE_NAME);
            MethodCallExpression memoizeClosureCallExpression = buildMemoizeClosureCallExpression(closureExpression,
                    protectedCacheSize, maxCacheSize);

            String memoizedClosureFieldName = buildUniqueName(ownerClassNode, methodNode);
            FieldNode memoizedClosureField = new FieldNode(memoizedClosureFieldName, modifiers,
                    ClassHelper.DYNAMIC_TYPE, null, memoizeClosureCallExpression);
            ownerClassNode.addField(memoizedClosureField);

            BlockStatement newCode = new BlockStatement();
            ArgumentListExpression args = new ArgumentListExpression(methodNode.getParameters());
            MethodCallExpression closureCallExpression = new MethodCallExpression(new FieldExpression(
                    memoizedClosureField), CLOSURE_CALL_METHOD_NAME, args);
            newCode.addStatement(new ReturnStatement(closureCallExpression));
            newCode.setVariableScope(methodNode.getVariableScope());
            methodNode.setCode(newCode);
        }
    }

    private int getIntMemberValue(AnnotationNode node, String name) {
        Object value = getMemberValue(node, name);
        if (value != null && value instanceof Integer) {
            return ((Integer) value).intValue();
        }

        return 0;
    }

    private static final String MEMOIZE_METHOD_NAME = "memoize";
    private static final String MEMOIZE_AT_MOST_METHOD_NAME = "memoizeAtMost";
    private static final String MEMOIZE_AT_LEAST_METHOD_NAME = "memoizeAtLeast";
    private static final String MEMOIZE_BETWEEN_METHOD_NAME = "memoizeBetween";

    private MethodCallExpression buildMemoizeClosureCallExpression(ClosureExpression expression,
            int protectedCacheSize, int maxCacheSize) {

        if (protectedCacheSize == 0 && maxCacheSize == 0) {
            return new MethodCallExpression(expression, MEMOIZE_METHOD_NAME, MethodCallExpression.NO_ARGUMENTS);
        } else if (protectedCacheSize == 0) {
            return new MethodCallExpression(expression, MEMOIZE_AT_MOST_METHOD_NAME, new ArgumentListExpression(
                    new ConstantExpression(maxCacheSize)));
        } else if (maxCacheSize == 0) {
            return new MethodCallExpression(expression, MEMOIZE_AT_LEAST_METHOD_NAME, new ArgumentListExpression(
                    new ConstantExpression(protectedCacheSize)));
        } else {
            ArgumentListExpression args = new ArgumentListExpression(new Expression[] {
                    new ConstantExpression(protectedCacheSize), new ConstantExpression(maxCacheSize) });

            return new MethodCallExpression(expression, MEMOIZE_BETWEEN_METHOD_NAME, args);
        }
    }

    /*
     * Build unique name.
     */
    private String buildUniqueName(ClassNode owner, MethodNode methodNode) {
        StringBuilder nameBuilder = new StringBuilder("memoizedMethodClosure$").append(methodNode.getName());
        if (methodNode.getParameters() != null) {
            for (Parameter parameter : methodNode.getParameters()) {
                nameBuilder.append(parameter.getType().getNameWithoutPackage());
            }
        }
        while (owner.getField(nameBuilder.toString()) != null) {
            nameBuilder.insert(0, "_");
        }

        return nameBuilder.toString();
    }

}
