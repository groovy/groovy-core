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

import groovy.transform.AutoClone;
import groovy.transform.AutoCloneStyle;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.isPrimitiveType;
import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

/**
 * Handles generation of code for the @AutoClone annotation.
 *
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class AutoCloneASTTransformation extends AbstractASTTransformation {
    static final Class MY_CLASS = AutoClone.class;
    static final ClassNode MY_TYPE = make(MY_CLASS);
    static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode CLONEABLE_TYPE = make(Cloneable.class);
    private static final ClassNode BAOS_TYPE = make(ByteArrayOutputStream.class);
    private static final ClassNode BAIS_TYPE = make(ByteArrayInputStream.class);
    private static final ClassNode OOS_TYPE = make(ObjectOutputStream.class);
    private static final ClassNode OIS_TYPE = make(ObjectInputStream.class);
    private static final ClassNode INVOKER_TYPE = make(InvokerHelper.class);

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode anno = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(anno.getClassNode())) return;

        if (parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            if (!checkNotInterface(cNode, MY_TYPE_NAME)) return;
            cNode.addInterface(CLONEABLE_TYPE);
            boolean includeFields = memberHasValue(anno, "includeFields", true);
            AutoCloneStyle style = getStyle(anno, "style");
            List<String> excludes = getMemberList(anno, "excludes");
            List<FieldNode> list = getInstancePropertyFields(cNode);
            if (includeFields) {
                list.addAll(getInstanceNonPropertyFields(cNode));
            }
            if (style == null) style = AutoCloneStyle.CLONE;
            switch (style) {
                case COPY_CONSTRUCTOR:
                    createCloneCopyConstructor(cNode, list, excludes);
                    break;
                case SERIALIZATION:
                    createCloneSerialization(cNode);
                    break;
                case CLONE:
                    createClone(cNode, list, excludes);
                    break;
                case SIMPLE:
                    createSimpleClone(cNode, list, excludes);
                    break;
            }
        }
    }

    private void createCloneSerialization(ClassNode cNode) {
        final BlockStatement body = new BlockStatement();
        // def baos = new ByteArrayOutputStream()
        final Expression baos = varX("baos");
        body.addStatement(declS(baos, ctorX(BAOS_TYPE)));

        // baos.withObjectOutputStream{ it.writeObject(this) }
        MethodCallExpression writeObject = callX(castX(OOS_TYPE, varX("it")), "writeObject", varX("this"));
        writeObject.setImplicitThis(false);
        ClosureExpression writeClos = closureX(block(stmt(writeObject)));
        writeClos.setVariableScope(new VariableScope());
        body.addStatement(stmt(callX(baos, "withObjectOutputStream", args(writeClos))));

        // def bais = new ByteArrayInputStream(baos.toByteArray())
        final Expression bais = varX("bais");
        body.addStatement(declS(bais, ctorX(BAIS_TYPE, args(callX(baos, "toByteArray")))));

        // return bais.withObjectInputStream(getClass().classLoader){ (<type>) it.readObject() }
        MethodCallExpression readObject = callX(castX(OIS_TYPE, varX("it")), "readObject");
        readObject.setImplicitThis(false);
        ClosureExpression readClos = closureX(block(stmt(castX(GenericsUtils.nonGeneric(cNode), readObject))));
        readClos.setVariableScope(new VariableScope());
        Expression classLoader = callX(callThisX("getClass"), "getClassLoader");
        body.addStatement(returnS(callX(bais, "withObjectInputStream", args(classLoader, readClos))));

        new VariableScopeVisitor(sourceUnit, true).visitClass(cNode);
        ClassNode[] exceptions = {make(CloneNotSupportedException.class)};
        cNode.addMethod("clone", ACC_PUBLIC, GenericsUtils.nonGeneric(cNode), Parameter.EMPTY_ARRAY, exceptions, body);
    }

    private void createCloneCopyConstructor(ClassNode cNode, List<FieldNode> list, List<String> excludes) {
        if (cNode.getDeclaredConstructors().size() == 0) {
            // add no-arg constructor
            BlockStatement noArgBody = new BlockStatement();
            noArgBody.addStatement(EmptyStatement.INSTANCE);
            cNode.addConstructor(ACC_PUBLIC, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, noArgBody);
        }
        boolean hasThisCons = false;
        for (ConstructorNode consNode : cNode.getDeclaredConstructors()) {
            Parameter[] parameters = consNode.getParameters();
            if (parameters.length == 1 && parameters[0].getType().equals(cNode)) {
                hasThisCons = true;
            }
        }
        if (!hasThisCons) {
            BlockStatement initBody = new BlockStatement();
            Parameter initParam = param(GenericsUtils.nonGeneric(cNode), "other");
            final Expression other = varX(initParam);
            boolean hasParent = cNode.getSuperClass() != ClassHelper.OBJECT_TYPE;
            if (hasParent) {
                initBody.addStatement(stmt(ctorX(ClassNode.SUPER, other)));
            }
            for (FieldNode fieldNode : list) {
                String name = fieldNode.getName();
                if (excludes.contains(name)) continue;
                ClassNode fieldType = fieldNode.getType();
                Expression direct = propX(other, name);
                Expression to = propX(varX("this"), name);
                Statement assignDirect = assignS(to, direct);
                Statement assignCloned = assignS(to, castX(fieldType, callCloneDirectX(direct)));
                Statement assignClonedDynamic = assignS(to, castX(fieldType, callCloneDynamicX(direct)));
                if (isCloneableType(fieldType)) {
                    initBody.addStatement(assignCloned);
                } else if (!possiblyCloneable(fieldType)) {
                    initBody.addStatement(assignDirect);
                } else {
                    initBody.addStatement(ifElseS(isInstanceOfX(direct, CLONEABLE_TYPE), assignClonedDynamic, assignDirect));
                }
            }
            cNode.addConstructor(ACC_PROTECTED, params(initParam), ClassNode.EMPTY_ARRAY, initBody);
        }
        ClassNode[] exceptions = {make(CloneNotSupportedException.class)};
        cNode.addMethod("clone", ACC_PUBLIC, GenericsUtils.nonGeneric(cNode), Parameter.EMPTY_ARRAY, exceptions, block(stmt(ctorX(cNode, args(varX("this"))))));
    }

    private boolean isCloneableType(ClassNode fieldType) {
        return isOrImplements(fieldType, CLONEABLE_TYPE) || !fieldType.getAnnotations(MY_TYPE).isEmpty();
    }

    private boolean possiblyCloneable(ClassNode type) {
        return !isPrimitiveType(type) && ((isCloneableType(type) || (type.getModifiers() & ACC_FINAL) == 0));
    }

    private Expression callCloneDynamicX(Expression target) {
        return callX(INVOKER_TYPE, "invokeMethod", args(target, constX("clone"), ConstantExpression.NULL));
    }

    private Expression callCloneDirectX(Expression direct) {
        return ternaryX(equalsNullX(direct), ConstantExpression.NULL, callX(direct, "clone"));
    }

    private void createSimpleClone(ClassNode cNode, List<FieldNode> fieldNodes, List<String> excludes) {
        if (cNode.getDeclaredConstructors().size() == 0) {
            // add no-arg constructor
            cNode.addConstructor(ACC_PUBLIC, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, block(EmptyStatement.INSTANCE));
        }
        addSimpleCloneHelperMethod(cNode, fieldNodes, excludes);
        final Expression result = varX("_result", cNode);
        ClassNode[] exceptions = {make(CloneNotSupportedException.class)};
        cNode.addMethod("clone", ACC_PUBLIC, GenericsUtils.nonGeneric(cNode), Parameter.EMPTY_ARRAY, exceptions, block(
            declS(result, ctorX(cNode)),
            stmt(callThisX("cloneOrCopyMembers", args(result))),
            returnS(result)));
    }

    private void addSimpleCloneHelperMethod(ClassNode cNode, List<FieldNode> fieldNodes, List<String> excludes) {
        Parameter methodParam = new Parameter(GenericsUtils.nonGeneric(cNode), "other");
        final Expression other = varX(methodParam);
        boolean hasParent = cNode.getSuperClass() != ClassHelper.OBJECT_TYPE;
        BlockStatement methodBody = new BlockStatement();
        if (hasParent) {
            methodBody.addStatement(stmt(callSuperX("cloneOrCopyMembers", args(other))));
        }
        for (FieldNode fieldNode : fieldNodes) {
            String name = fieldNode.getName();
            if (excludes.contains(name)) continue;
            ClassNode fieldType = fieldNode.getType();
            Expression direct = propX(varX("this"), name);
            Expression to = propX(other, name);
            Statement assignDirect = assignS(to, direct);
            Statement assignCloned = assignS(to, castX(fieldType, callCloneDirectX(direct)));
            Statement assignClonedDynamic = assignS(to, castX(fieldType, callCloneDynamicX(direct)));
            if (isCloneableType(fieldType)) {
                methodBody.addStatement(assignCloned);
            } else if (!possiblyCloneable(fieldType)) {
                methodBody.addStatement(assignDirect);
            } else {
                methodBody.addStatement(ifElseS(isInstanceOfX(direct, CLONEABLE_TYPE), assignClonedDynamic, assignDirect));
            }
        }
        ClassNode[] exceptions = {make(CloneNotSupportedException.class)};
        cNode.addMethod("cloneOrCopyMembers", ACC_PROTECTED, ClassHelper.VOID_TYPE, params(methodParam), exceptions, methodBody);
    }

    private void createClone(ClassNode cNode, List<FieldNode> fieldNodes, List<String> excludes) {
        final BlockStatement body = new BlockStatement();
        final Expression result = varX("_result", cNode);
        body.addStatement(declS(result, castX(cNode, callSuperX("clone"))));
        for (FieldNode fieldNode : fieldNodes) {
            if (excludes.contains(fieldNode.getName())) continue;
            ClassNode fieldType = fieldNode.getType();
            Expression fieldExpr = varX(fieldNode);
            Expression to = propX(result, fieldNode.getName());
            Statement doClone = assignS(to, castX(fieldType, callCloneDirectX(fieldExpr)));
            Statement doCloneDynamic = assignS(to, castX(fieldType, callCloneDynamicX(fieldExpr)));
            if (isCloneableType(fieldType)) {
                body.addStatement(doClone);
            } else if (possiblyCloneable(fieldType)) {
                body.addStatement(ifS(isInstanceOfX(fieldExpr, CLONEABLE_TYPE), doCloneDynamic));
            }
        }
        body.addStatement(returnS(result));
        ClassNode[] exceptions = {make(CloneNotSupportedException.class)};
        cNode.addMethod("clone", ACC_PUBLIC, GenericsUtils.nonGeneric(cNode), Parameter.EMPTY_ARRAY, exceptions, body);
    }

    private AutoCloneStyle getStyle(AnnotationNode node, String name) {
        final Expression member = node.getMember(name);
        if (member != null && member instanceof PropertyExpression) {
            PropertyExpression prop = (PropertyExpression) member;
            Expression oe = prop.getObjectExpression();
            if (oe instanceof ClassExpression) {
                ClassExpression ce = (ClassExpression) oe;
                if (ce.getType().getName().equals("groovy.transform.AutoCloneStyle")) {
                    return AutoCloneStyle.valueOf(prop.getPropertyAsString());
                }
            }
        }
        return null;
    }

}
