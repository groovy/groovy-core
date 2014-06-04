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

import groovy.lang.Lazy;
import groovy.transform.Field;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.block;
import static org.codehaus.groovy.ast.tools.GeneralUtils.param;
import static org.codehaus.groovy.ast.tools.GeneralUtils.params;
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;

/**
 * Handles transformation for the @Field annotation.
 *
 * @author Paul King
 * @author Cedric Champeau
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class FieldASTTransformation extends ClassCodeExpressionTransformer implements ASTTransformation, Opcodes {

    private static final Class MY_CLASS = Field.class;
    private static final ClassNode MY_TYPE = make(MY_CLASS);
    private static final ClassNode LAZY_TYPE = make(Lazy.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode ASTTRANSFORMCLASS_TYPE = make(GroovyASTTransformationClass.class);
    static final String MEMBER_ADD_SETTER = "addSetter";
    private SourceUnit sourceUnit;
    private DeclarationExpression candidate;
    private boolean insideScriptBody;
    private String variableName;
    private FieldNode fieldNode;
    private ClosureExpression currentClosure;

    public void visit(ASTNode[] nodes, SourceUnit source) {
        sourceUnit = source;
        if (nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new GroovyBugError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + Arrays.asList(nodes));
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode node = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(node.getClassNode())) return;

        if (parent instanceof DeclarationExpression) {
            DeclarationExpression de = (DeclarationExpression) parent;
            ClassNode cNode = de.getDeclaringClass();
            if (!cNode.isScript()) {
                addError("Annotation " + MY_TYPE_NAME + " can only be used within a Script.", parent);
                return;
            }
            boolean addSetter = memberHasValue(node, MEMBER_ADD_SETTER, true);
            candidate = de;
            // GROOVY-4548: temp fix to stop CCE until proper support is added
            if (de.isMultipleAssignmentDeclaration()) {
                addError("Annotation " + MY_TYPE_NAME + " not supported with multiple assignment notation.", parent);
                return;
            }
            VariableExpression ve = de.getVariableExpression();
            variableName = ve.getName();
            // set owner null here, it will be updated by addField
            fieldNode = new FieldNode(variableName, ve.getModifiers(), ve.getType(), null, de.getRightExpression());
            fieldNode.setSourcePosition(de);
            cNode.addField(fieldNode);
            if (addSetter) {
                String setterName = "set" + MetaClassHelper.capitalize(variableName);
                cNode.addMethod(setterName, ACC_PUBLIC | ACC_SYNTHETIC, ClassHelper.VOID_TYPE, params(param(ve.getType(), variableName)), ClassNode.EMPTY_ARRAY, block(
                        stmt(assignX(propX(varX("this"), variableName), varX(variableName)))
                ));
            }

            // GROOVY-4833 : annotations that are not Groovy transforms should be transferred to the generated field
            // GROOVY-6112 : also copy acceptable Groovy transforms
            final List<AnnotationNode> annotations = de.getAnnotations();
            for (AnnotationNode annotation : annotations) {
                // GROOVY-6337 HACK: in case newly created field is @Lazy
                if (annotation.getClassNode().equals(LAZY_TYPE)) {
                    LazyASTTransformation.visitField(annotation, fieldNode);
                }
                final ClassNode annotationClassNode = annotation.getClassNode();
                if (notTransform(annotationClassNode) || acceptableTransform(annotation)) {
                    fieldNode.addAnnotation(annotation);
                }
            }

            super.visitClass(cNode);
            // GROOVY-5207 So that Closures can see newly added fields
            // (not super efficient for a very large class with many @Fields but we chose simplicity
            // and understandability of this solution over more complex but efficient alternatives)
            VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(source);
            scopeVisitor.visitClass(cNode);
        }
    }

    private boolean acceptableTransform(AnnotationNode annotation) {
        // TODO also check for phase after sourceUnit.getPhase()? but will be ignored anyway?
        // TODO we should only copy those annotations with FIELD_TARGET but haven't visited annotations
        // and gathered target info at this phase, so we can't do this:
        // return annotation.isTargetAllowed(AnnotationNode.FIELD_TARGET);
        // instead just don't copy ourselves for now
        return !annotation.getClassNode().equals(MY_TYPE);
    }

    public boolean memberHasValue(AnnotationNode node, String name, Object value) {
        final Expression member = node.getMember(name);
        return member != null && member instanceof ConstantExpression && value.equals(((ConstantExpression) member).getValue());
    }

    private boolean notTransform(ClassNode annotationClassNode) {
        return annotationClassNode.getAnnotations(ASTTRANSFORMCLASS_TYPE).isEmpty();
    }

    @Override
    public Expression transform(Expression expr) {
        if (expr == null) return null;
        if (expr instanceof DeclarationExpression) {
            DeclarationExpression de = (DeclarationExpression) expr;
            if (de.getLeftExpression() == candidate.getLeftExpression()) {
                if (insideScriptBody) {
                    // TODO make EmptyExpression work
                    // partially works but not if only thing in script
                    // return EmptyExpression.INSTANCE;
                    return new ConstantExpression(null);
                }
                addError("Annotation " + MY_TYPE_NAME + " can only be used within a Script body.", expr);
                return expr;
            }
        } else if (insideScriptBody && expr instanceof VariableExpression && currentClosure != null) {
            VariableExpression ve = (VariableExpression) expr;
            if (ve.getName().equals(variableName)) {
                // we may only check the variable name because the Groovy compiler
                // already fails if a variable with the same name already exists in the scope.
                // this means that a closure cannot shadow a class variable
                ve.setAccessedVariable(fieldNode);
                final VariableScope variableScope = currentClosure.getVariableScope();
                final Iterator<Variable> iterator = variableScope.getReferencedLocalVariablesIterator();
                while (iterator.hasNext()) {
                    Variable next = iterator.next();
                    if (next.getName().equals(variableName)) iterator.remove();
                }
                variableScope.putReferencedClassVariable(fieldNode);
                return ve;
            }
        }
        return expr.transformExpression(this);
    }

    @Override
    public void visitClosureExpression(final ClosureExpression expression) {
        ClosureExpression old = currentClosure;
        currentClosure = expression;
        super.visitClosureExpression(expression);
        currentClosure = old;
    }

    @Override
    public void visitMethod(MethodNode node) {
        Boolean oldInsideScriptBody = insideScriptBody;
        if (node.isScriptBody()) insideScriptBody = true;
        super.visitMethod(node);
        insideScriptBody = oldInsideScriptBody;
    }

    @Override
    public void visitExpressionStatement(ExpressionStatement es) {
        Expression exp = es.getExpression();
        if (exp instanceof BinaryExpression) {
            exp.visit(this);
        }
        super.visitExpressionStatement(es);
    }

    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }
}
