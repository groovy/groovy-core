/*
 * Copyright 2008-2014 the original author or authors.
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

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.objectweb.asm.Opcodes;

import java.lang.ref.SoftReference;
import java.util.Arrays;

import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.block;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifElseS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.notNullExpr;
import static org.codehaus.groovy.ast.tools.GeneralUtils.param;
import static org.codehaus.groovy.ast.tools.GeneralUtils.params;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt;
import static org.codehaus.groovy.ast.tools.GeneralUtils.var;

/**
 * Handles generation of code for the @Lazy annotation
 *
 * @author Alex Tkachman
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class LazyASTTransformation implements ASTTransformation, Opcodes {

    private static final ClassNode SOFT_REF = ClassHelper.makeWithoutCaching(SoftReference.class, false);
    private static final Expression NULL_EXPR = ConstantExpression.NULL;

    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new GroovyBugError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + Arrays.asList(nodes));
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode node = (AnnotationNode) nodes[0];

        if (parent instanceof FieldNode) {
            final FieldNode fieldNode = (FieldNode) parent;
            visitField(node, fieldNode);
        }
    }

    static void visitField(AnnotationNode node, FieldNode fieldNode) {
        final Expression soft = node.getMember("soft");
        final Expression init = getInitExpr(fieldNode);

        fieldNode.rename("$" + fieldNode.getName());
        fieldNode.setModifiers(ACC_PRIVATE | (fieldNode.getModifiers() & (~(ACC_PUBLIC | ACC_PROTECTED))));

        if (soft instanceof ConstantExpression && ((ConstantExpression) soft).getValue().equals(true)) {
            createSoft(fieldNode, init);
        } else {
            create(fieldNode, init);
            // @Lazy not meaningful with primitive so convert to wrapper if needed
            if (ClassHelper.isPrimitiveType(fieldNode.getType())) {
                fieldNode.setType(ClassHelper.getWrapper(fieldNode.getType()));
            }
        }
    }

    private static void create(FieldNode fieldNode, final Expression initExpr) {
        final BlockStatement body = new BlockStatement();
        if (fieldNode.isStatic()) {
            addHolderClassIdiomBody(body, fieldNode, initExpr);
        } else if (fieldNode.isVolatile()) {
            addDoubleCheckedLockingBody(body, fieldNode, initExpr);
        } else {
            addNonThreadSafeBody(body, fieldNode, initExpr);
        }
        addMethod(fieldNode, body, fieldNode.getType());
    }

    private static void addHolderClassIdiomBody(BlockStatement body, FieldNode fieldNode, Expression initExpr) {
        final ClassNode declaringClass = fieldNode.getDeclaringClass();
        final ClassNode fieldType = fieldNode.getType();
        final int visibility = ACC_PRIVATE | ACC_STATIC;
        final String fullName = declaringClass.getName() + "$" + fieldType.getNameWithoutPackage() + "Holder_" + fieldNode.getName().substring(1);
        final InnerClassNode holderClass = new InnerClassNode(declaringClass, fullName, visibility, ClassHelper.OBJECT_TYPE);
        final String innerFieldName = "INSTANCE";
        holderClass.addField(innerFieldName, ACC_PRIVATE | ACC_STATIC | ACC_FINAL, fieldType, initExpr);
        final Expression innerField = new PropertyExpression(new ClassExpression(holderClass), innerFieldName);
        declaringClass.getModule().addClass(holderClass);
        body.addStatement(returnS(innerField));
    }

    private static void addDoubleCheckedLockingBody(BlockStatement body, FieldNode fieldNode, Expression initExpr) {
        final Expression fieldExpr = new VariableExpression(fieldNode);
        final VariableExpression localVar = var(fieldNode.getName() + "_local");
        body.addStatement(declS(localVar, fieldExpr));
        body.addStatement(ifElseS(
                notNullExpr(localVar),
                returnS(localVar),
                new SynchronizedStatement(
                        syncTarget(fieldNode),
                        ifElseS(
                                notNullExpr(fieldExpr),
                                returnS(fieldExpr),
                                returnS(assignX(fieldExpr, initExpr))
                        )
                )
        ));
    }

    private static void addNonThreadSafeBody(BlockStatement body, FieldNode fieldNode, Expression initExpr) {
        final Expression fieldExpr = new VariableExpression(fieldNode);
        body.addStatement(ifElseS(notNullExpr(fieldExpr), stmt(fieldExpr), assignS(fieldExpr, initExpr)));
    }

    private static void addMethod(FieldNode fieldNode, BlockStatement body, ClassNode type) {
        int visibility = ACC_PUBLIC;
        if (fieldNode.isStatic()) visibility |= ACC_STATIC;
        final String name = "get" + MetaClassHelper.capitalize(fieldNode.getName().substring(1));
        fieldNode.getDeclaringClass().addMethod(name, visibility, type, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, body);
    }

    private static void createSoft(FieldNode fieldNode, Expression initExpr) {
        final ClassNode type = fieldNode.getType();
        fieldNode.setType(SOFT_REF);
        createSoftGetter(fieldNode, initExpr, type);
        createSoftSetter(fieldNode, type);
    }

    private static void createSoftGetter(FieldNode fieldNode, Expression initExpr, ClassNode type) {
        final BlockStatement body = new BlockStatement();
        final Expression fieldExpr = new VariableExpression(fieldNode);
        final Expression resExpr = var("res", type);
        final MethodCallExpression callExpression = callX(fieldExpr, "get");
        callExpression.setSafe(true);
        body.addStatement(declS(resExpr, callExpression));

        final Statement mainIf = ifElseS(notNullExpr(resExpr), stmt(resExpr), block(
                assignS(resExpr, initExpr),
                assignS(fieldExpr, ctorX(SOFT_REF, resExpr)),
                stmt(resExpr)));

        if (fieldNode.isVolatile()) {
            body.addStatement(ifElseS(
                    notNullExpr(resExpr),
                    stmt(resExpr),
                    new SynchronizedStatement(syncTarget(fieldNode), block(
                            assignS(resExpr, callExpression),
                            mainIf)
                    )
            ));
        } else {
            body.addStatement(mainIf);
        }
        addMethod(fieldNode, body, type);
    }

    private static void createSoftSetter(FieldNode fieldNode, ClassNode type) {
        final BlockStatement body = new BlockStatement();
        final Expression fieldExpr = new VariableExpression(fieldNode);
        final String name = "set" + MetaClassHelper.capitalize(fieldNode.getName().substring(1));
        final Parameter parameter = param(type, "value");
        final Expression paramExpr = new VariableExpression(parameter);
        body.addStatement(ifElseS(
                notNullExpr(paramExpr),
                assignS(fieldExpr, ctorX(SOFT_REF, paramExpr)),
                assignS(fieldExpr, NULL_EXPR)
        ));
        int visibility = ACC_PUBLIC;
        if (fieldNode.isStatic()) visibility |= ACC_STATIC;
        fieldNode.getDeclaringClass().addMethod(name, visibility, ClassHelper.VOID_TYPE, params(parameter), ClassNode.EMPTY_ARRAY, body);
    }

    private static Expression syncTarget(FieldNode fieldNode) {
        return fieldNode.isStatic() ? new ClassExpression(fieldNode.getDeclaringClass()) : VariableExpression.THIS_EXPRESSION;
    }

    private static Expression getInitExpr(FieldNode fieldNode) {
        Expression initExpr = fieldNode.getInitialValueExpression();
        fieldNode.setInitialValueExpression(null);

        if (initExpr == null) {
            initExpr = ctorX(fieldNode.getType());
        }

        return initExpr;
    }
}
