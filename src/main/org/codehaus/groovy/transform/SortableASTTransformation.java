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
import org.codehaus.groovy.runtime.AbstractComparator;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE;
import static org.codehaus.groovy.ast.ClassHelper.isPrimitiveType;
import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe;
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
    private static final ClassNode ABSTRACT_COMPARATOR_TYPE = makeClassSafe(AbstractComparator.class);
    private static final ClassNode INVOKER_HELPER_TYPE = makeClassSafe(InvokerHelper.class);

    private static final String VALUE = "value";
    private static final String OBJ = "obj";
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
                params(param(OBJECT_TYPE, OBJ)),
                ClassNode.EMPTY_ARRAY,
                createCompareToMethodBody(classNode, properties)
        ));

        for (PropertyNode property : properties) {
            createComparatorFor(classNode, property);
        }
    }

    private void implementComparable(ClassNode classNode) {
        if (!classNode.implementsInterface(COMPARABLE_TYPE)) {
            classNode.addInterface(COMPARABLE_TYPE);
        }
    }

    private static Statement createCompareToMethodBody(ClassNode classNode, List<PropertyNode> properties) {
        List<Statement> statements = new ArrayList<Statement>();

        // if(this.is(obj)) return 0;
        statements.add(ifS(callThisX("is", args(OBJ)), returnS(constX(0))));
        // if(!(obj instanceof <type>)) return -1;
        statements.add(ifS(notX(isInstanceOfX(varX(OBJ), newClass(classNode))), returnS(constX(-1))));
        // int value = 0;
        statements.add(declS(varX(VALUE, ClassHelper.int_TYPE), constX(0)));
        for (PropertyNode property : properties) {
            String name = property.getName();
            // value = this.prop <=> ((<type>)obj).prop;
            statements.add(assignS(varX(VALUE), cmpX(propX(varX("this"), name), propX(castX(newClass(classNode), varX(OBJ)), name))));
            // if(value != 0) return value;
            statements.add(ifS(neX(varX(VALUE), constX(0)), returnS(varX(VALUE))));
        }

        if (properties.isEmpty()) {
            // let this object be less than obj
            statements.add(returnS(constX(-1)));
        } else {
            // objects are equal
            statements.add(returnS(constX(0)));
        }

        final BlockStatement body = new BlockStatement();
        body.addStatements(statements);
        return body;
    }

    private static Statement createCompareToMethodBody(PropertyNode property) {
        String propertyName = property.getName();
        return block(
                // if(arg0 == arg1) return 0;
                ifS(eqX(varX(ARG0), varX(ARG1)), returnS(constX(0))),
                // if(arg0 != null && arg1 == null) return -1;
                ifS(andX(notNullX(varX(ARG0)), equalsNullX(varX(ARG1))), returnS(constX(-1))),
                // if(arg0 == null && arg1 != null) return 1;
                ifS(andX(equalsNullX(varX(ARG0)), notNullX(varX(ARG1))), returnS(constX(1))),
                // return arg0.prop <=> arg1.prop;
                returnS(cmpX(callX(INVOKER_HELPER_TYPE, "getProperty", args(varX(ARG0), constX(propertyName))), callX(INVOKER_HELPER_TYPE, "getProperty", args(varX(ARG1), constX(propertyName)))))
        );
    }

    private static void createComparatorFor(ClassNode classNode, PropertyNode property) {
        String propertyName = property.getName();
        String className = classNode.getName() + "$" + StringGroovyMethods.capitalize(propertyName) + "Comparator";
        InnerClassNode cmpClass = new InnerClassNode(classNode, className, ACC_PRIVATE | ACC_STATIC, ABSTRACT_COMPARATOR_TYPE);
        classNode.getModule().addClass(cmpClass);

        cmpClass.addMethod(new MethodNode(
                "compare",
                ACC_PUBLIC,
                ClassHelper.int_TYPE,
                params(param(OBJECT_TYPE, ARG0), param(OBJECT_TYPE, ARG1)),
                ClassNode.EMPTY_ARRAY,
                createCompareToMethodBody(property)
        ));

        String fieldName = "this$" + StringGroovyMethods.capitalize(propertyName) + "Comparator";
        // private final Comparator this$<property>Comparator = new <type>$<property>Comparator();
        FieldNode cmpField = classNode.addField(
                fieldName,
                ACC_STATIC | ACC_FINAL | ACC_PRIVATE | ACC_SYNTHETIC,
                COMPARATOR_TYPE,
                ctorX(cmpClass));

        classNode.addMethod(new MethodNode(
                "comparatorBy" + StringGroovyMethods.capitalize(propertyName),
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
