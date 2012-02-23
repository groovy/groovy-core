/*
 * Copyright 2008 the original author or authors.
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

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;

import groovy.lang.GroovyClassLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * This visitor walks the AST tree and collects references to Annotations that
 * are annotated themselves by {@link GroovyASTTransformation}. Each such
 * annotation is added.
 * <p/>
 * This visitor is only intended to be executed once, during the
 * SEMANTIC_ANALYSIS phase of compilation.
 *
 * @author Danno Ferrin (shemnon)
 * @author Roshan Dawrani (roshandawrani)
 */
public class ASTTransformationCollectorCodeVisitor extends ClassCodeVisitorSupport {
    private SourceUnit source;
    private ClassNode classNode;
    private GroovyClassLoader transformLoader;

    public ASTTransformationCollectorCodeVisitor(SourceUnit source, GroovyClassLoader transformLoader) {
        this.source = source;
        this.transformLoader = transformLoader;
    }

    protected SourceUnit getSourceUnit() {
        return source;
    }

    public void visitClass(ClassNode klassNode) {
        ClassNode oldClass = classNode;
        classNode = klassNode;
        super.visitClass(classNode);
        classNode = oldClass;
    }

    /**
     * If the annotation is annotated with {@link GroovyASTTransformation}
     * the annotation is added to <code>stageVisitors</code> at the appropriate processor visitor.
     *
     * @param node the node to process
     */
    public void visitAnnotations(AnnotatedNode node) {
        super.visitAnnotations(node);
        for (AnnotationNode annotation : node.getAnnotations()) {
            Annotation transformClassAnnotation = getTransformClassAnnotation(annotation.getClassNode());
            if (transformClassAnnotation == null) {
                // skip if there is no such annotation
                continue;
            }
            addTransformsToClassNode(annotation, transformClassAnnotation);
        }
    }
    
    private void addTransformsToClassNode(AnnotationNode annotation, Annotation transformClassAnnotation) {
        String[] transformClassNames = getTransformClassNames(transformClassAnnotation);
        Class[] transformClasses = getTransformClasses(transformClassAnnotation);

        if(transformClassNames.length == 0 && transformClasses.length == 0) {
            source.getErrorCollector().addError(new SimpleMessage("@GroovyASTTransformationClass in " + 
                    annotation.getClassNode().getName() + " does not specify any transform class names/classes", source));
        }

        if(transformClassNames.length > 0 && transformClasses.length > 0) {
            source.getErrorCollector().addError(new SimpleMessage("@GroovyASTTransformationClass in " + 
                    annotation.getClassNode().getName() +  " should specify transforms only by class names or by classes and not by both", source));
        }

        for (String transformClass : transformClassNames) {
            try {
                Class klass = transformLoader.loadClass(transformClass, false, true, false);
                verifyAndAddTransform(annotation, klass);

            } catch (ClassNotFoundException e) {
                source.getErrorCollector().addErrorAndContinue(
                        new SimpleMessage(
                                "Could not find class for Transformation Processor " + transformClass
                                + " declared by " + annotation.getClassNode().getName(),
                                source));
            }
        }
        for (Class klass : transformClasses) {
            verifyAndAddTransform(annotation, klass);
        }
    }

    private void verifyAndAddTransform(AnnotationNode annotation, Class klass) {
        verifyClass(annotation, klass);
        verifyCompilePhase(annotation, klass);
        addTransform(annotation, klass);
    }

    private void verifyCompilePhase(AnnotationNode annotation, Class klass) {
        GroovyASTTransformation transformationClass = (GroovyASTTransformation) klass.getAnnotation(GroovyASTTransformation.class);
        if (transformationClass != null)  {
            CompilePhase specifiedCompilePhase = transformationClass.phase();
            if (specifiedCompilePhase.getPhaseNumber() < CompilePhase.SEMANTIC_ANALYSIS.getPhaseNumber())  {
                source.getErrorCollector().addError(
                        new SimpleMessage(
                                annotation.getClassNode().getName() + " is defined to be run in compile phase " + specifiedCompilePhase + ". Local AST transformations must run in " + CompilePhase.SEMANTIC_ANALYSIS + " or later!",
                                source));
            }
        }
    }

    private void verifyClass(AnnotationNode annotation, Class klass) {
        if (!ASTTransformation.class.isAssignableFrom(klass)) {
            source.getErrorCollector().addError(new SimpleMessage("Not an ASTTransformation: " +
                    klass.getName() + " declared by " + annotation.getClassNode().getName(), source));
        }
    }

    private void addTransform(AnnotationNode annotation, Class klass)  {
        classNode.addTransform(klass, annotation);
    }

    private static Annotation getTransformClassAnnotation(ClassNode annotatedType) {
        if (!annotatedType.isResolved()) return null;

        for (Annotation ann : annotatedType.getTypeClass().getAnnotations()) {
            // because compiler clients are free to choose any GroovyClassLoader for
            // resolving ClassNodeS such as annotatedType, we have to compare by name,
            // and cannot cast the return value to GroovyASTTransformationClass
            if (ann.annotationType().getName().equals(GroovyASTTransformationClass.class.getName())){
                return ann;
            }
        }  

        return null;
    }

    private String[] getTransformClassNames(Annotation transformClassAnnotation) {
        try {
            Method valueMethod = transformClassAnnotation.getClass().getMethod("value");
            return (String[]) valueMethod.invoke(transformClassAnnotation);
        } catch (Exception e) {
            source.addException(e);
            return new String[0];
        }
    }

    private Class[] getTransformClasses(Annotation transformClassAnnotation) {
        try {
            Method classesMethod = transformClassAnnotation.getClass().getMethod("classes");
            return (Class[]) classesMethod.invoke(transformClassAnnotation);
        } catch (Exception e) {
            source.addException(e);
            return new Class[0];
        }
    }
}
