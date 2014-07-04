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
package org.codehaus.groovy.transform;

import groovy.transform.Sortable;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.runtime.AbstractComparator;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.isPrimitiveType;
import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe;
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics;
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass;

/**
 * Injects a set of Comparators and sort methods.
 *
 * @author Andres Almiray
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)

public class SortableASTTransformation extends AbstractASTTransformation {
    private static final ClassNode MY_TYPE = make(Sortable.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode COMPARABLE_TYPE = makeClassSafe(Comparable.class);
    private static final ClassNode COMPARATOR_TYPE = makeClassSafe(Comparator.class);

    private static final String VALUE = "value";
    private static final String OTHER = "other";
    private static final String THIS_HASH = "thisHash";
    private static final String OTHER_HASH = "otherHash";
    private static final String ARG0 = "arg0";
    private static final String ARG1 = "arg1";

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        if (parent instanceof ClassNode) {
            createSortable(annotation, (ClassNode) parent);
        }
    }

    private void createSortable(AnnotationNode annotation, ClassNode classNode) {
        List<String> includes = getMemberList(annotation, "includes");
        List<String> excludes = getMemberList(annotation, "excludes");
        if (!checkIncludeExclude(annotation, excludes, includes, MY_TYPE_NAME)) return;
        if (classNode.isInterface()) {
            addError(MY_TYPE_NAME + " cannot be applied to interface " + classNode.getName(), annotation);
        }
        List<PropertyNode> properties = findProperties(annotation, classNode, includes, excludes);
        implementComparable(classNode);

        classNode.addMethod(new MethodNode(
                "compareTo",
                ACC_PUBLIC,
                ClassHelper.int_TYPE,
                params(param(newClass(classNode), OTHER)),
                ClassNode.EMPTY_ARRAY,
                createCompareToMethodBody(properties)
        ));

        for (PropertyNode property : properties) {
            createComparatorFor(classNode, property);
        }
        new VariableScopeVisitor(sourceUnit, true).visitClass(classNode);
    }

    private void implementComparable(ClassNode classNode) {
        if (!classNode.implementsInterface(COMPARABLE_TYPE)) {
            classNode.addInterface(makeClassSafeWithGenerics(Comparable.class, classNode));
        }
    }

    private static Statement createCompareToMethodBody(List<PropertyNode> properties) {
        List<Statement> statements = new ArrayList<Statement>();

        // if (this.is(other)) return 0;
        statements.add(ifS(callThisX("is", args(OTHER)), returnS(constX(0))));

        if (properties.isEmpty()) {
            // perhaps overkill but let compareTo be based on hashes for commutativity
            // return this.hashCode() <=> other.hashCode()
            statements.add(declS(varX(THIS_HASH, ClassHelper.Integer_TYPE), callX(varX("this"), "hashCode")));
            statements.add(declS(varX(OTHER_HASH, ClassHelper.Integer_TYPE), callX(varX(OTHER), "hashCode")));
            statements.add(returnS(cmpX(varX(THIS_HASH), varX(OTHER_HASH))));
        } else {
            // int value = 0;
            statements.add(declS(varX(VALUE, ClassHelper.int_TYPE), constX(0)));
            for (PropertyNode property : properties) {
                String propName = property.getName();
                // value = this.prop <=> other.prop;
                statements.add(assignS(varX(VALUE), cmpX(propX(varX("this"), propName), propX(varX(OTHER), propName))));
                // if (value != 0) return value;
                statements.add(ifS(neX(varX(VALUE), constX(0)), returnS(varX(VALUE))));
            }
            // objects are equal
            statements.add(returnS(constX(0)));
        }

        final BlockStatement body = new BlockStatement();
        body.addStatements(statements);
        return body;
    }

    private static Statement createCompareMethodBody(PropertyNode property) {
        String propName = property.getName();
        return block(
                // if (arg0 == arg1) return 0;
                ifS(eqX(varX(ARG0), varX(ARG1)), returnS(constX(0))),
                // if (arg0 != null && arg1 == null) return -1;
                ifS(andX(notNullX(varX(ARG0)), equalsNullX(varX(ARG1))), returnS(constX(-1))),
                // if (arg0 == null && arg1 != null) return 1;
                ifS(andX(equalsNullX(varX(ARG0)), notNullX(varX(ARG1))), returnS(constX(1))),
                // return arg0.prop <=> arg1.prop;
                returnS(cmpX(propX(varX(ARG0), propName), propX(varX(ARG1), propName)))
        );
    }

    private static void createComparatorFor(ClassNode classNode, PropertyNode property) {
        String propName = property.getName();
        String className = classNode.getName() + "$" + StringGroovyMethods.capitalize(propName) + "Comparator";
        ClassNode superClass = makeClassSafeWithGenerics(AbstractComparator.class, classNode);
        InnerClassNode cmpClass = new InnerClassNode(classNode, className, ACC_PRIVATE | ACC_STATIC, superClass);
        classNode.getModule().addClass(cmpClass);

        cmpClass.addMethod(new MethodNode(
                "compare",
                ACC_PUBLIC,
                ClassHelper.int_TYPE,
                params(param(newClass(classNode), ARG0), param(newClass(classNode), ARG1)),
                ClassNode.EMPTY_ARRAY,
                createCompareMethodBody(property)
        ));

        String fieldName = "this$" + StringGroovyMethods.capitalize(propName) + "Comparator";
        // private final Comparator this$<property>Comparator = new <type>$<property>Comparator();
        FieldNode cmpField = classNode.addField(
                fieldName,
                ACC_STATIC | ACC_FINAL | ACC_PRIVATE | ACC_SYNTHETIC,
                COMPARATOR_TYPE,
                ctorX(cmpClass));

        classNode.addMethod(new MethodNode(
                "comparatorBy" + StringGroovyMethods.capitalize(propName),
                ACC_PUBLIC | ACC_STATIC,
                COMPARATOR_TYPE,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                returnS(fieldX(cmpField))
        ));
    }

    private List<PropertyNode> findProperties(AnnotationNode annotation, ClassNode classNode, final List<String> includes, final List<String> excludes) {
        List<PropertyNode> properties = new ArrayList<PropertyNode>();
        for (PropertyNode property : classNode.getProperties()) {
            String propertyName = property.getName();
            if (property.isStatic() ||
                    excludes.contains(propertyName) ||
                    !includes.isEmpty() && !includes.contains(propertyName)) continue;
            properties.add(property);
        }
        for (String name : includes) {
            checkKnownProperty(annotation, name, properties);
        }
        for (PropertyNode pNode : properties) {
            checkComparable(pNode);
        }
        if (!includes.isEmpty()) {
            Comparator<PropertyNode> includeComparator = new Comparator<PropertyNode>() {
                public int compare(PropertyNode o1, PropertyNode o2) {
                    return new Integer(includes.indexOf(o1.getName())).compareTo(includes.indexOf(o2.getName()));
                }
            };
            Collections.sort(properties, includeComparator);
        }
        return properties;
    }

    private void checkComparable(PropertyNode pNode) {
        if (pNode.getType().implementsInterface(COMPARABLE_TYPE) || isPrimitiveType(pNode.getType()) || hasAnnotation(pNode.getType(), MY_TYPE)) {
            return;
        }
        addError("Error during " + MY_TYPE_NAME + " processing: property '" +
                pNode.getName() + "' must be Comparable", pNode);
    }

    private void checkKnownProperty(AnnotationNode annotation, String name, List<PropertyNode> properties) {
        for (PropertyNode pNode: properties) {
            if (name.equals(pNode.getName())) {
                return;
            }
        }
        addError("Error during " + MY_TYPE_NAME + " processing: tried to include unknown property '" +
                name + "'", annotation);
    }
}
