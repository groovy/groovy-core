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

import groovy.transform.InheritConstructors;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.block;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorSuperS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.param;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;
import static org.codehaus.groovy.ast.tools.GenericsUtils.correctToGenericsSpecRecurse;
import static org.codehaus.groovy.ast.tools.GenericsUtils.createGenericsSpec;
import static org.codehaus.groovy.ast.tools.GenericsUtils.extractSuperClassGenerics;

/**
 * Handles generation of code for the {@code @}InheritConstructors annotation.
 *
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class InheritConstructorsASTTransformation extends AbstractASTTransformation {

    private static final Class MY_CLASS = InheritConstructors.class;
    private static final ClassNode MY_TYPE = make(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode node = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(node.getClassNode())) return;

        if (parent instanceof ClassNode) {
            processClass((ClassNode) parent);
        }
    }

    private void processClass(ClassNode cNode) {
        if (cNode.isInterface()) {
            addError("Error processing interface '" + cNode.getName() +
                    "'. " + MY_TYPE_NAME + " only allowed for classes.", cNode);
            return;
        }
        ClassNode sNode = cNode.getSuperClass();
        List<AnnotationNode> superAnnotations = sNode.getAnnotations(MY_TYPE);
        if (superAnnotations.size() == 1) {
            // We need @InheritConstructors from parent classes processed first
            // so force that order here. The transformation is benign on an already
            // processed node so processing twice in any order won't matter bar
            // a very small time penalty.
            processClass(sNode);
        }
        for (ConstructorNode cn : sNode.getDeclaredConstructors()) {
            addConstructorUnlessAlreadyExisting(cNode, cn);
        }
    }

    private void addConstructorUnlessAlreadyExisting(ClassNode classNode, ConstructorNode consNode) {
        Parameter[] origParams = consNode.getParameters();
        if (consNode.isPrivate()) return;
        Parameter[] params = new Parameter[origParams.length];
        List<Expression> theArgs = new ArrayList<Expression>();
        Map<String, ClassNode> genericsSpec = createGenericsSpec(classNode);
        extractSuperClassGenerics(classNode, classNode.getSuperClass(), genericsSpec);
        for (int i = 0; i < origParams.length; i++) {
            Parameter p = origParams[i];
            ClassNode newType = correctToGenericsSpecRecurse(genericsSpec, p.getType());
            params[i] = p.hasInitialExpression() ? param(newType, p.getName(), p.getInitialExpression()) : param(newType, p.getName());
            theArgs.add(varX(p.getName(), newType));
        }
        if (isExisting(classNode, params)) return;
        classNode.addConstructor(consNode.getModifiers(), params, consNode.getExceptions(), block(ctorSuperS(args(theArgs))));
    }

    private boolean isExisting(ClassNode classNode, Parameter[] params) {
        for (ConstructorNode consNode : classNode.getDeclaredConstructors()) {
            if (matchingTypes(params, consNode.getParameters())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchingTypes(Parameter[] params, Parameter[] existingParams) {
        if (params.length != existingParams.length) return false;
        for (int i = 0; i < params.length; i++) {
            if (!params[i].getType().equals(existingParams[i].getType())) {
                return false;
            }
        }
        return true;
    }
}
