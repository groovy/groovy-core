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

import static org.codehaus.groovy.transform.AbstractASTTransformUtil.*;
import static org.codehaus.groovy.transform.AbstractASTTransformUtil.assignStatement;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;

import groovy.transform.PojoBuilder;

/**
 * Handles generation of code for the @Builder annotation.
 *
 * @author Marcin Grzejszczak
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class PojoBuilderASTTransformation extends AbstractASTTransformation {

    private static final Class MY_CLASS = PojoBuilder.class;
    private static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final Expression DEFAULT_INITIAL_VALUE_EXPRESSION = null;

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(annotation.getClassNode())) return;

        if (parent instanceof ClassNode) {
            ClassNode annotatedClassNode = (ClassNode) parent;
            checkNotInterface(annotatedClassNode, MY_TYPE_NAME);
            ClassNode pojoClassNode = getPojoClassNodeToCreateBuilderFor(annotation);
            if (pojoClassNode == null) {
                addError("Error during " + MY_TYPE_NAME + " processing: You have to provide the class for which you want to create the builder", annotation);
                return;
            }
            List<String> excludes = getMemberList(annotation, "excludes");
            List<String> includes = getMemberList(annotation, "includes");
            if (hasAnnotation(annotatedClassNode, CanonicalASTTransformation.MY_TYPE)) {
                AnnotationNode canonical = annotatedClassNode.getAnnotations(CanonicalASTTransformation.MY_TYPE).get(0);
                if (excludes == null || excludes.isEmpty()) excludes = getMemberList(canonical, "excludes");
                if (includes == null || includes.isEmpty()) includes = getMemberList(canonical, "includes");
            }
            if (includes != null && !includes.isEmpty() && excludes != null && !excludes.isEmpty()) {
                addError("Error during " + MY_TYPE_NAME + " processing: Only one of 'includes' and 'excludes' should be supplied not both.", annotation);
                return;
            }
            List<FieldNode> classToCreateBuilderForFields = getAllFieldsOfClassWithIncludesExcludes(pojoClassNode, includes, excludes);
            List<FieldNode> createdPrivateFields = createPrivateFieldsForClass(annotatedClassNode, classToCreateBuilderForFields);
            appendWithBuilderMethodsForPrivateFields(annotatedClassNode, createdPrivateFields);
            appendBuildMethod(annotatedClassNode, pojoClassNode, createdPrivateFields);
            appendBuildMethodWithValidationClosure(annotatedClassNode, pojoClassNode, createdPrivateFields);
        }
    }

    private static ClassNode getPojoClassNodeToCreateBuilderFor(AnnotationNode anno) {
        final Expression forClass = anno.getMember("forClass");
        if (forClass != null) {
            return forClass.getType();
        }
        return null;
    }

    private static List<FieldNode> createPrivateFieldsForClass(ClassNode cNode, List<FieldNode> classToCreateBuilderForFields) {
        List<FieldNode> createdPrivateFields = new ArrayList<FieldNode>();
        for (FieldNode fNode : classToCreateBuilderForFields) {
            FieldNode fNodeCopy = createFieldCopyInGivenClass(fNode, cNode);
            createdPrivateFields.add(fNodeCopy);
            cNode.addField(fNodeCopy);
        }
        return createdPrivateFields;
    }

    private static FieldNode createFieldCopyInGivenClass(FieldNode fNode, ClassNode cNode) {
        return new FieldNode(fNode.getName(), fNode.getModifiers(), ClassHelper.make(fNode.getType().getName()), cNode,
                DEFAULT_INITIAL_VALUE_EXPRESSION);
    }

    private static List<FieldNode> getAllFieldsOfClassWithIncludesExcludes(ClassNode classToCreateBuilderFor, List<String> includes, List<String> excludes) {
        List<FieldNode> declaredFields = classToCreateBuilderFor.getFields();
        List<FieldNode> fieldsToInclude = new ArrayList<FieldNode>();
        if (!includes.isEmpty()) {
            for (FieldNode field : declaredFields) {
                if (includes.contains(field.getName())) {
                    fieldsToInclude.add(field);
                }
            }
        } else if (!excludes.isEmpty()) {
            for (FieldNode field : declaredFields) {
                if (!excludes.contains(field.getName())) {
                    fieldsToInclude.add(field);
                }
            }
        } else {
            fieldsToInclude = declaredFields;
        }
        return fieldsToInclude;
    }

    private static void appendWithBuilderMethodsForPrivateFields(ClassNode annotatedClassNode, List<FieldNode> createdPrivateFields) {
        for (FieldNode fieldNode : createdPrivateFields) {
            MethodNode withBuilderMethodForField = createWithBuilderMethodForField(annotatedClassNode, fieldNode);
            annotatedClassNode.addMethod(withBuilderMethodForField);
        } 
    }

    private static MethodNode createWithBuilderMethodForField(ClassNode annotatedClassNode, FieldNode fieldNode) {
        final BlockStatement body = new BlockStatement();
        String fieldName = fieldNode.getName();
        Parameter parameter = new Parameter(fieldNode.getType(), fieldName);
        // this.field = parameter
        body.addStatement(assignStatement(new FieldExpression(fieldNode), new VariableExpression(parameter)));
        // return this
        body.addStatement(new ReturnStatement(VariableExpression.THIS_EXPRESSION));
        String firstLetterUppercasedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        // Builder withField(type parameter) { this.field = parameter; return this; }
        return new MethodNode("with" + firstLetterUppercasedFieldName, ACC_PUBLIC, annotatedClassNode.getPlainNodeReference(),
                new Parameter[] {parameter}, ClassNode.EMPTY_ARRAY, body);
    }

    private static void appendBuildMethod(ClassNode annotatedClassNode, ClassNode classToCreateBuilderFor, List<FieldNode> createdPrivateFields) {
        MethodNode buildMethodForField = createBuildMethodForField(classToCreateBuilderFor, createdPrivateFields);
        annotatedClassNode.addMethod(buildMethodForField);
    }

    private static MethodNode createBuildMethodForField(ClassNode classToCreateBuilderFor, List<FieldNode> createdPrivateFields) {
        final BlockStatement body = new BlockStatement();
        VariableExpression classToCreateBuilderForVariable = initializeObjectAndSetItsValues(classToCreateBuilderFor, createdPrivateFields, body);
        // return objectToBuild
        body.addStatement(new ReturnStatement(classToCreateBuilderForVariable));
        // ObjectToBuild build() { ObjectToBuild objectToBuild = new ObjectToBuild(); SET_VALUES; return objectToBuild;}
        return new MethodNode("build", ACC_PUBLIC, classToCreateBuilderFor, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, body);
    }

    private static VariableExpression initializeObjectAndSetItsValues(ClassNode classToCreateBuilderFor, List<FieldNode> createdPrivateFields,
                                                                      BlockStatement body) {
        final Expression callClassToCreateBuilderForConstructor = new ConstructorCallExpression(classToCreateBuilderFor, ArgumentListExpression.EMPTY_ARGUMENTS);
        VariableExpression classToCreateBuilderForVariable = new VariableExpression("objectToBuild", classToCreateBuilderFor);
        // ObjectToBuild objectToBuild = new ObjectToBuild();
        Statement constructorCallToVariableAssignment = declStatement(classToCreateBuilderForVariable, callClassToCreateBuilderForConstructor);
        body.addStatement(constructorCallToVariableAssignment);
        // objectToBuild.property = field; 
        for (FieldNode createdPrivateField : createdPrivateFields) {
            PropertyExpression propertyExpressionForClass = new PropertyExpression(classToCreateBuilderForVariable, createdPrivateField.getName());
            Statement setPropertyOnObject = assignStatement(propertyExpressionForClass, new VariableExpression(createdPrivateField));
            body.addStatement(setPropertyOnObject);
        }
        return classToCreateBuilderForVariable;
    }

    private void appendBuildMethodWithValidationClosure(ClassNode annotatedClassNode, ClassNode classToCreateBuilderFor, List<FieldNode> createdPrivateFields) {        
        MethodNode methodNode = createBuildMethodWithValidationClosure(classToCreateBuilderFor, createdPrivateFields);
        annotatedClassNode.addMethod(methodNode);
    }

    private MethodNode createBuildMethodWithValidationClosure(ClassNode classToCreateBuilderFor, List<FieldNode> createdPrivateFields) {
        final BlockStatement body = new BlockStatement();
        // ObjectToBuild objectToBuild = new ObjectToBuild();
        // set values
        VariableExpression classToCreateBuilderForVariable = initializeObjectAndSetItsValues(classToCreateBuilderFor, createdPrivateFields, body);
        Parameter parameter = new Parameter(ClassHelper.CLOSURE_TYPE.getPlainNodeReference(), "validationClosure");
        // call validation closure with created object as parameter
        MethodCallExpression closureCall = new MethodCallExpression(new VariableExpression(parameter), "call", classToCreateBuilderForVariable);
        body.addStatement(new ExpressionStatement(closureCall));
        // return objectToBuild
        body.addStatement(new ReturnStatement(classToCreateBuilderForVariable));
        // ObjectToBuild build(Closure validationClosure) { ObjectToBuild objectToBuild = new ObjectToBuild(); SET_VALUES; validationClosure.call(objectToBuild); return objectToBuild;}
        return new MethodNode("build", ACC_PUBLIC, classToCreateBuilderFor, new Parameter[] {parameter}, ClassNode.EMPTY_ARRAY, body);
    }

}
