/*
 * Copyright 2004-2014 the original author or authors.
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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper methods for working with Groovy AST trees.
 *
 * @author Graeme Rocher (Grails 0.3)
 */
public class GroovyASTUtils {
    /**
     * Returns whether a classNode has the specified property or not
     *
     * @param classNode    The ClassNode
     * @param propertyName The name of the property
     * @return True if the property exists in the ClassNode
     */
    public static boolean hasProperty(ClassNode classNode, String propertyName) {
// TODO reinstate
//        if (classNode == null || StringUtils.isBlank(propertyName)) {
//            return false;
//        }

// TODO reinstate
//        final MethodNode method = classNode.getMethod(GriffonUtil.getGetterName(propertyName), new Parameter[0]);
        final MethodNode method = null;
        if (method != null) return true;

        for (PropertyNode pn : classNode.getProperties()) {
            if (pn.getName().equals(propertyName) && !pn.isPrivate()) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasOrInheritsProperty(ClassNode classNode, String propertyName) {
        if (hasProperty(classNode, propertyName)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (hasProperty(parent, propertyName)) {
                return true;
            }
            parent = parent.getSuperClass();
        }

        return false;
    }

    /**
     * Tests whether the ClasNode implements the specified method name.
     *
     * @param classNode  The ClassNode
     * @param methodName The method name
     * @return True if it does implement the method
     */
    public static boolean implementsZeroArgMethod(ClassNode classNode, String methodName) {
        MethodNode method = classNode.getDeclaredMethod(methodName, new Parameter[]{});
        return method != null && (method.isPublic() || method.isProtected()) && !method.isAbstract();
    }

    @SuppressWarnings("unchecked")
    public static boolean implementsOrInheritsZeroArgMethod(ClassNode classNode, String methodName, List ignoreClasses) {
        if (implementsZeroArgMethod(classNode, methodName)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (!ignoreClasses.contains(parent) && implementsZeroArgMethod(parent, methodName)) {
                return true;
            }
            parent = parent.getSuperClass();
        }
        return false;
    }

    public static boolean implementsMethod(ClassNode classNode, MethodNode methodNode) {
        MethodNode method = classNode.getDeclaredMethod(methodNode.getName(), methodNode.getParameters());
        return method != null && method.getModifiers() == methodNode.getModifiers();
    }

    public static boolean implementsOrInheritsMethod(ClassNode classNode, MethodNode methodNode) {
        if (implementsMethod(classNode, methodNode)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (implementsMethod(parent, methodNode)) {
                return true;
            }
            parent = parent.getSuperClass();
        }
        return false;
    }

    public static void injectMethod(ClassNode classNode, MethodNode methodNode) {
        injectMethod(classNode, methodNode, true);
    }

    public static void injectMethod(ClassNode classNode, MethodNode methodNode, boolean deep) {
        if (deep) {
            if (!implementsOrInheritsMethod(classNode, methodNode)) {
                getFurthestParent(classNode).addMethod(methodNode);
            }
        } else {
            if (!implementsMethod(classNode, methodNode)) {
                classNode.addMethod(methodNode);
            }
        }
    }

    public static boolean hasField(ClassNode classNode, String name, int modifiers, ClassNode type) {
        FieldNode fieldNode = classNode.getDeclaredField(name);
        return fieldNode != null && fieldNode.getModifiers() == modifiers &&
                fieldNode.getType().equals(type);
    }

    public static boolean hasOrInheritsField(ClassNode classNode, String name, int modifiers, ClassNode type) {
        if (hasField(classNode, name, modifiers, type)) {
            return true;
        }

        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
            if (hasField(parent, name, modifiers, type)) {
                return true;
            }
            parent = parent.getSuperClass();
        }
        return false;
    }

    public static FieldNode injectField(ClassNode classNode, String name, int modifiers, ClassNode type, Object value) {
        return injectField(classNode, name, modifiers, type, value, true);
    }

    public static FieldNode injectField(ClassNode classNode, String name, int modifiers, ClassNode type, Object value, boolean deep) {
        Expression initialExpression = null;
        if (value != null) initialExpression = new ConstantExpression(value);
        return injectField(classNode, name, modifiers, type, initialExpression, deep);
    }

    public static FieldNode injectField(ClassNode classNode, String name, int modifiers, ClassNode type, Expression initialExpression) {
        return injectField(classNode, name, modifiers, type, initialExpression, true);
    }

    public static FieldNode injectField(ClassNode classNode, String name, int modifiers, ClassNode type, Expression initialExpression, boolean deep) {
        if (deep) {
            if (!hasOrInheritsField(classNode, name, modifiers, type)) {
                return getFurthestParent(classNode).addField(name, modifiers, type, initialExpression);
            }
        } else {
            if (!hasField(classNode, name, modifiers, type)) {
                return classNode.addField(name, modifiers, type, initialExpression);
            }
        }
        return null;
    }

    public static void injectInterface(ClassNode classNode, ClassNode type) {
        injectInterface(classNode, type, true);
    }

    public static void injectInterface(ClassNode classNode, ClassNode type, boolean deep) {
        if (classNode.implementsInterface(type)) return;
        if (deep) {
            getFurthestParent(classNode).addInterface(type);
        } else {
            classNode.addInterface(type);
        }
    }

    /**
     * Gets the full name of a ClassNode.
     *
     * @param classNode The class node
     * @return The full name
     */
    public static String getFullName(ClassNode classNode) {
        return classNode.getName();
    }

    public static ClassNode getFurthestParent(ClassNode classNode) {
        ClassNode parent = classNode.getSuperClass();
        while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
// TODO reinstate
//            if (SourceUnitCollector.getInstance().getSourceUnit(parent) == null)
//                break;
            classNode = parent;
            parent = parent.getSuperClass();
        }
        return classNode;
    }

    public static boolean isEnum(ClassNode classNode) {
        ClassNode parent = classNode.getSuperClass();
        while (parent != null) {
            if (parent.getName().equals("java.lang.Enum")) return true;
            parent = parent.getSuperClass();
        }
        return false;
    }

    public static boolean addMethod(ClassNode classNode, MethodNode methodNode) {
        return addMethod(classNode, methodNode, false);
    }

    public static boolean addMethod(ClassNode classNode, MethodNode methodNode, boolean replace) {
        MethodNode oldMethod = classNode.getMethod(methodNode.getName(), methodNode.getParameters());
        if (oldMethod == null) {
            classNode.addMethod(methodNode);
            return true;
        } else if (replace) {
            classNode.getMethods().remove(oldMethod);
            classNode.addMethod(methodNode);
            return true;
        }
        return false;
    }

    /**
     * @return true if the two arrays are of the same size and have the same contents
     */
    public static boolean parametersEqual(Parameter[] a, Parameter[] b) {
        if (a.length == b.length) {
            boolean answer = true;
            for (int i = 0; i < a.length; i++) {
                if (!a[i].getType().equals(b[i].getType())) {
                    answer = false;
                    break;
                }
            }
            return answer;
        }
        return false;
    }

    public static void injectProperty(ClassNode classNode, String propertyName, Class propertyClass) {
        injectProperty(classNode, propertyName, Modifier.PUBLIC, new ClassNode(propertyClass), null);
    }

    public static void injectProperty(ClassNode classNode, String propertyName, Class propertyClass, Object value) {
        injectProperty(classNode, propertyName, Modifier.PUBLIC, new ClassNode(propertyClass), value);
    }

    public static void injectProperty(ClassNode classNode, String propertyName, int modifiers, Class propertyClass) {
        injectProperty(classNode, propertyName, modifiers, new ClassNode(propertyClass), null);
    }

    public static void injectProperty(ClassNode classNode, String propertyName, int modifiers, Class propertyClass, Object value) {
        injectProperty(classNode, propertyName, modifiers, new ClassNode(propertyClass), value);
    }

    public static void injectProperty(ClassNode classNode, String propertyName, ClassNode propertyClass) {
        injectProperty(classNode, propertyName, Modifier.PUBLIC, propertyClass, null);
    }

    public static void injectProperty(ClassNode classNode, String propertyName, int modifiers, ClassNode propertyClass) {
        injectProperty(classNode, propertyName, modifiers, propertyClass, null);
    }

    public static void injectProperty(ClassNode classNode, String propertyName, int modifiers, ClassNode propertyClass, Object value) {
        if (!hasOrInheritsProperty(classNode, propertyName)) {
            // inject into furthest relative
            ClassNode parent = getFurthestParent(classNode);
            Expression initialExpression = value instanceof Expression ? (Expression) value : null;
            if (value != null && initialExpression == null)
                initialExpression = new ConstantExpression(value);
            parent.addProperty(propertyName, modifiers, propertyClass, initialExpression, null, null);
        }
    }

    public static void injectConstant(ClassNode classNode, String propertyName, Class propertyClass, Object value) {
        final boolean hasProperty = hasOrInheritsProperty(classNode, propertyName);

        if (!hasProperty) {
            // inject into furthest relative
            // ClassNode parent = getFurthestParent(classNode);
            Expression initialExpression = new ConstantExpression(value);
            classNode.addProperty(propertyName, Modifier.PUBLIC | Modifier.FINAL, new ClassNode(propertyClass), initialExpression, null, null);
        }
    }

    public static void addReadOnlyProperty(ClassNode classNode, String propertyName, ClassNode propertyClass, Object value) {
        final boolean hasProperty = hasOrInheritsProperty(classNode, propertyName);

        if (!hasProperty) {
            int visibility = Modifier.PRIVATE | Modifier.FINAL;
            Expression initialValue = value != null && !(value instanceof Expression) ? initialValue = new ConstantExpression(value) : (Expression) value;
            classNode.addField(propertyName, visibility, propertyClass, initialValue);
            addMethod(classNode, new MethodNode(
                    "get" + MetaClassHelper.capitalize(propertyName),
                    Modifier.PUBLIC,
                    propertyClass,
                    Parameter.EMPTY_ARRAY,
                    ClassNode.EMPTY_ARRAY,
                    new ReturnStatement(
                            new ExpressionStatement(
                                    new FieldExpression(classNode.getField(propertyName))))));
        }
    }

    public static final ClassNode[] NO_EXCEPTIONS = ClassNode.EMPTY_ARRAY;
    public static final Parameter[] NO_PARAMS = Parameter.EMPTY_ARRAY;
    public static final Expression THIS = VariableExpression.THIS_EXPRESSION;
    public static final Expression SUPER = VariableExpression.SUPER_EXPRESSION;
    public static final ArgumentListExpression NO_ARGS = ArgumentListExpression.EMPTY_ARGUMENTS;
    public static final Token ASSIGN = Token.newSymbol(Types.ASSIGN, -1, -1);
    public static final Token EQ = Token.newSymbol(Types.COMPARE_EQUAL, -1, -1);
    public static final Token NE = Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1);
    public static final Token AND = Token.newSymbol(Types.LOGICAL_AND, -1, -1);
    public static final Token OR = Token.newSymbol(Types.LOGICAL_OR, -1, -1);
    public static final Token CMP = Token.newSymbol(Types.COMPARE_TO, -1, -1);
    public static final Token INSTANCEOF = Token.newSymbol(Types.KEYWORD_INSTANCEOF, -1, -1);

    public static Statement returns(Expression expr) {
        return new ReturnStatement(new ExpressionStatement(expr));
    }

    public static ArgumentListExpression vars(String... names) {
        List<Expression> vars = new ArrayList<Expression>();
        for (String name : names) {
            vars.add(var(name));
        }
        return new ArgumentListExpression(vars);
    }

    public static ArgumentListExpression args(Expression... expressions) {
        List<Expression> args = new ArrayList<Expression>();
        Collections.addAll(args, expressions);
        return new ArgumentListExpression(args);
    }

    public static ArgumentListExpression args(List<Expression> expressions) {
        return new ArgumentListExpression(expressions);
    }

    public static VariableExpression var(String name) {
        return new VariableExpression(name);
    }

    public static VariableExpression var(String name, ClassNode type) {
        return new VariableExpression(name, type);
    }

    public static Parameter param(ClassNode type, String name) {
        return param(type, name, null);
    }

    public static Parameter param(ClassNode type, String name, Expression initialExpression) {
        Parameter param = new Parameter(type, name);
        if (initialExpression != null) {
            param.setInitialExpression(initialExpression);
        }
        return param;
    }

    public static ClassNode[] throwing(ClassNode... exceptions) {
        return exceptions;
    }

    public static Parameter[] params(Parameter... params) {
        return params != null ? params : Parameter.EMPTY_ARRAY;
    }

    public static NotExpression not(Expression expr) {
        return new NotExpression(expr);
    }

    public static ConstantExpression constx(Object val) {
        return new ConstantExpression(val);
    }

    public static ClassExpression classx(ClassNode clazz) {
        return new ClassExpression(clazz);
    }

    public static ClassExpression classx(Class clazz) {
        return classx(ClassHelper.make(clazz).getPlainNodeReference());
    }

    public static BlockStatement block(Statement... stms) {
        BlockStatement block = new BlockStatement();
        for (Statement stm : stms) block.addStatement(stm);
        return block;
    }

    public static Statement ifs(Expression cond, Expression trueExpr) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                new ReturnStatement(trueExpr),
                new EmptyStatement()
        );
    }

    public static Statement ifs(Expression cond, Expression trueExpr, Expression falseExpr) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                new ReturnStatement(trueExpr),
                new ReturnStatement(falseExpr)
        );
    }

    public static Statement ifs_no_return(Expression cond, Expression trueExpr) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                new ExpressionStatement(trueExpr),
                new EmptyStatement()
        );
    }

    public static Statement ifs_no_return(Expression cond, Expression trueExpr, Expression falseExpr) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                new ExpressionStatement(trueExpr),
                new ExpressionStatement(falseExpr)
        );
    }

    public static Statement ifs_no_return(Expression cond, Statement trueStmnt) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                trueStmnt,
                new EmptyStatement()
        );
    }

    public static Statement ifs_no_return(Expression cond, Statement trueStmnt, Statement falseStmnt) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                trueStmnt,
                falseStmnt
        );
    }

    public static Statement decls(Expression lhv, Expression rhv) {
        return new ExpressionStatement(new DeclarationExpression(lhv, ASSIGN, rhv));
    }

    public static Statement assigns(Expression expression, Expression value) {
        return new ExpressionStatement(assign(expression, value));
    }

    public static BinaryExpression assign(Expression lhv, Expression rhv) {
        return new BinaryExpression(lhv, ASSIGN, rhv);
    }

    public static BinaryExpression eq(Expression lhv, Expression rhv) {
        return new BinaryExpression(lhv, EQ, rhv);
    }

    public static BinaryExpression ne(Expression lhv, Expression rhv) {
        return new BinaryExpression(lhv, NE, rhv);
    }

    public static BinaryExpression and(Expression lhv, Expression rhv) {
        return new BinaryExpression(lhv, AND, rhv);
    }

    public static BinaryExpression or(Expression lhv, Expression rhv) {
        return new BinaryExpression(lhv, OR, rhv);
    }

    public static BinaryExpression cmp(Expression lhv, Expression rhv) {
        return new BinaryExpression(lhv, CMP, rhv);
    }

    public static BinaryExpression iof(Expression lhv, Expression rhv) {
        return new BinaryExpression(lhv, INSTANCEOF, rhv);
    }

    public static BinaryExpression iof(Expression lhv, ClassNode rhv) {
        return new BinaryExpression(lhv, INSTANCEOF, new ClassExpression(rhv));
    }

    public static Expression prop(Expression owner, String property) {
        return new PropertyExpression(owner, property);
    }

    public static Expression prop(Expression owner, Expression property) {
        return new PropertyExpression(owner, property);
    }

    public static MethodCallExpression call(Expression receiver, String methodName, ArgumentListExpression args) {
        return new MethodCallExpression(receiver, methodName, args);
    }

    public static StaticMethodCallExpression call(ClassNode receiver, String methodName, ArgumentListExpression args) {
        return new StaticMethodCallExpression(receiver, methodName, args);
    }

    public static ExpressionStatement stmnt(Expression expression) {
        return new ExpressionStatement(expression);
    }

    public static FieldExpression field(FieldNode fieldNode) {
        return new FieldExpression(fieldNode);
    }

    public static FieldExpression field(ClassNode owner, String fieldName) {
        return new FieldExpression(owner.getField(fieldName));
    }

    public static ConstructorCallExpression ctor(ClassNode type, Expression args) {
        return new ConstructorCallExpression(type, args);
    }

    public static ListExpression listx(Expression... expressions) {
        ListExpression list = new ListExpression();
        for (Expression expression : expressions) {
            list.addExpression(expression);
        }
        return list;
    }

    public static MapEntryExpression mapEntryx(Expression key, Expression value) {
        return new MapEntryExpression(key, value);
    }

    public static MapExpression mapx(MapEntryExpression... entries) {
        MapExpression map = new MapExpression();
        for (MapEntryExpression entry : entries) {
            map.addMapEntryExpression(entry);
        }
        return map;
    }
}