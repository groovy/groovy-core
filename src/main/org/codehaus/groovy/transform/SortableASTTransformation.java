/*
 * Copyright 2009-2014 the original author or authors.
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
import org.codehaus.groovy.runtime.AbstractComparator;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.codehaus.groovy.ast.tools.GeneralUtils.*;
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeSafe;
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass;

/**
 * Injects a set of Comparators and sort methods.
 *
 * @author Andres Almiray
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class SortableASTTransformation extends AbstractASTTransformation {
    private static final ClassNode MY_TYPE = newClass(Sortable.class);
    private static final ClassNode COMPARABLE_TYPE = newClass(Comparable.class);
    private static final ClassNode COMPARATOR_TYPE = newClass(Comparator.class);
    private static final ClassNode ABSTRACT_COMPARATOR_TYPE = newClass(AbstractComparator.class);
    private static final Expression NIL = ConstantExpression.NULL;

    private static final String VALUE = "value";
    private static final String OBJ = "obj";
    private static final String ARG0 = "arg0";
    private static final String ARG1 = "arg1";

    /**
     * Convenience method to see if an annotated node is {@code @Sortable}.
     *
     * @param node the node to check
     * @return true if the node is annotated with @Sortable
     */
    public static boolean hasSortableAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            if (MY_TYPE.equals(annotation.getClassNode())) {
                return true;
            }
        }
        return false;
    }

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        if (parent instanceof ClassNode) {
            createSortable(source, annotation, (ClassNode) parent);
        }
    }

    private void createSortable(SourceUnit sourceUnit, AnnotationNode annotation, ClassNode classNode) {
        List<String> includes = getMemberList(annotation, "includes");
        List<String> excludes = getMemberList(annotation, "excludes");
        List<PropertyNode> properties = findProperties(annotation, classNode, includes, excludes);
        if (!classNode.implementsInterface(COMPARABLE_TYPE)) {
            classNode.addInterface(COMPARABLE_TYPE);
        }

        classNode.addMethod(new MethodNode(
                "compareTo",
                ACC_PUBLIC,
                ClassHelper.int_TYPE,
                params(param(ClassHelper.OBJECT_TYPE, OBJ)),
                ClassNode.EMPTY_ARRAY,
                createCompareToMethodBody(classNode, properties)
        ));

        for (PropertyNode property : properties) {
            createComparatorFor(classNode, property);
        }
    }

    private static Statement createCompareToMethodBody(ClassNode classNode, List<PropertyNode> properties) {
        List<Statement> statements = new ArrayList<Statement>();

        // if(this.is(obj)) return 0;
        statements.add(ifs(new MethodCallExpression(THIS, "is", vars(OBJ)), constx(0)));
        // if(!(obj instanceof <type>)) return -1;
        statements.add(ifs(not(iof(var(OBJ), makeSafe(classNode))), constx(-1)));
        // int value = 0;
        statements.add(decls(var(VALUE, ClassHelper.int_TYPE), constx(0)));
        for (PropertyNode property : properties) {
            String name = property.getName();
            // TODO: check that this.prop is Comparable otherwise KABOOM!
            // value = this.prop <=> obj.prop;
            statements.add(
                    assigns(var(VALUE), cmp(prop(THIS, name), prop(var(OBJ), name)))
            );
            // if(value != 0) return value;
            statements.add(
                    ifs(ne(var(VALUE), constx(0)), var(VALUE))
            );
        }

        if (properties.isEmpty()) {
            // let this object be less than obj
            statements.add(returns(constx(-1)));
        } else {
            // objects are equal
            statements.add(returns(constx(0)));
        }

        final BlockStatement body = new BlockStatement();
        body.addStatements(statements);
        return body;
    }

    private static Statement createCompareToMethodBody(ClassNode classNode, PropertyNode property) {
        String propertyName = property.getName();
        return block(
                // if(arg0 == arg1) return 0;
                ifs(eq(var(ARG0), var(ARG1)), constx(0)),
                // if(arg0 != null && arg1 == null) return -1;
                ifs(and(ne(var(ARG0), NIL), eq(var(ARG1), NIL)), constx(-1)),
                // if(arg0 == null && arg1 != null) return 1;
                ifs(and(eq(var(ARG0), NIL), ne(var(ARG1), NIL)), constx(1)),
                // return arg0.prop <=> arg1.prop;
                returns(cmp(prop(var(ARG0), propertyName), prop(var(ARG1), propertyName)))
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
                params(
                        param(ClassHelper.OBJECT_TYPE, ARG0),
                        param(ClassHelper.OBJECT_TYPE, ARG1)),
                ClassNode.EMPTY_ARRAY,
                createCompareToMethodBody(classNode, property)
        ));

        String fieldName = "this$" + StringGroovyMethods.capitalize(propertyName) + "Comparator";
        // private final Comparator this$<property>Comparator = new <type>$<property>Comparator();
        FieldNode cmpField = classNode.addField(
                fieldName,
                ACC_STATIC | ACC_FINAL | ACC_PRIVATE | ACC_SYNTHETIC,
                COMPARATOR_TYPE,
                new ConstructorCallExpression(cmpClass, NO_ARGS));

        classNode.addMethod(new MethodNode(
                "comparatorBy" + StringGroovyMethods.capitalize(propertyName),
                ACC_PUBLIC | ACC_STATIC,
                COMPARATOR_TYPE,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                returns(field(cmpField))
        ));
    }

    private static List<PropertyNode> findProperties(AnnotationNode annotation, ClassNode classNode, List<String> includes, List<String> excludes) {
        List<PropertyNode> properties = new ArrayList<PropertyNode>();
        for (PropertyNode property : classNode.getProperties()) {
            String propertyName = property.getName();
            if (property.isStatic() ||
                    excludes.contains(propertyName) ||
                    !includes.isEmpty() && !includes.contains(propertyName)) continue;
            properties.add(property);
        }
        return properties;
    }
}