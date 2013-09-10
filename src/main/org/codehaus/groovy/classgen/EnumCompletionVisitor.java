/*
 * Copyright 2003-2013 the original author or authors.
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
package org.codehaus.groovy.classgen;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.TupleConstructorASTTransformation;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Enums have a parent constructor with two arguments from java.lang.Enum.
 * This visitor adds those two arguments into manually created constructors
 * and performs the necessary super call.
 */
public class EnumCompletionVisitor extends ClassCodeVisitorSupport {
    private final SourceUnit sourceUnit;

    public EnumCompletionVisitor(CompilationUnit cu, SourceUnit su) {
        sourceUnit = su;
    }

    public void visitClass(ClassNode node) {
        if (!node.isEnum()) return;
        completeEnum(node);
    }

    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }

    private void completeEnum(ClassNode enumClass) {
        boolean isAic = isAnonymousInnerClass(enumClass);
        if (enumClass.getDeclaredConstructors().size() == 0) {
            addImplicitConstructors(enumClass, isAic);
        }

        for (ConstructorNode ctor : enumClass.getDeclaredConstructors()) {
            transformConstructor(ctor, isAic);
        }
    }

    /**
     * Add map and no-arg constructor or mirror those of the superclass (i.e. base enum).
     */
    private void addImplicitConstructors(ClassNode enumClass, boolean aic) {
        if (aic) {
            ClassNode sn = enumClass.getSuperClass();
            List<ConstructorNode> sctors = new ArrayList<ConstructorNode>(sn.getDeclaredConstructors());
            if (sctors.size() == 0) {
                addMapConstructors(enumClass, false);
            } else {
                for (ConstructorNode constructorNode : sctors) {
                    ConstructorNode init = new ConstructorNode(Opcodes.ACC_PUBLIC, constructorNode.getParameters(), ClassNode.EMPTY_ARRAY, new BlockStatement());
                    enumClass.addConstructor(init);
                }
            }
        } else {
            addMapConstructors(enumClass, false);
        }
    }

    /**
     * If constructor does not define a call to super, then transform constructor
     * to get String,int parameters at beginning and add call super(String,int).
     */
    private void transformConstructor(ConstructorNode ctor, boolean isAic) {
        boolean chainedThisConstructorCall = false;
        ConstructorCallExpression cce = null;
        if (ctor.firstStatementIsSpecialConstructorCall()) {
            Statement code = ctor.getFirstStatement();
            cce = (ConstructorCallExpression) ((ExpressionStatement) code).getExpression();
            if (cce.isSuperCall()) return;
            // must be call to this(...)
            chainedThisConstructorCall = true;
        }
        // we need to add parameters
        Parameter[] oldP = ctor.getParameters();
        Parameter[] newP = new Parameter[oldP.length + 2];
        String stringParameterName = getUniqueVariableName("__str", ctor.getCode());
        newP[0] = new Parameter(ClassHelper.STRING_TYPE, stringParameterName);
        String intParameterName = getUniqueVariableName("__int", ctor.getCode());
        newP[1] = new Parameter(ClassHelper.int_TYPE, intParameterName);
        System.arraycopy(oldP, 0, newP, 2, oldP.length);
        ctor.setParameters(newP);
        VariableExpression stringVariable = new VariableExpression(newP[0]);
        VariableExpression intVariable = new VariableExpression(newP[1]);
        if (chainedThisConstructorCall) {
            TupleExpression args = (TupleExpression) cce.getArguments();
            List<Expression> argsExprs = args.getExpressions();
            argsExprs.add(0, stringVariable);
            argsExprs.add(1, intVariable);
        } else {
            // add a super call
            List<Expression> args = new ArrayList<Expression>();
            args.add(stringVariable);
            args.add(intVariable);
            if (isAic) {
                for (Parameter parameter : oldP) {
                    args.add(new VariableExpression(parameter.getName()));
                }
            }
            cce = new ConstructorCallExpression(ClassNode.SUPER, new ArgumentListExpression(args));
            BlockStatement code = new BlockStatement();
            code.addStatement(new ExpressionStatement(cce));
            Statement oldCode = ctor.getCode();
            if (oldCode != null) code.addStatement(oldCode);
            ctor.setCode(code);
        }
    }

    public static void addMapConstructors(ClassNode enumClass, boolean hasNoArg) {
        TupleConstructorASTTransformation.addMapConstructors(enumClass, hasNoArg, "One of the enum constants for enum " + enumClass.getName() +
                " was initialized with null. Please use a non-null value or define your own constructor.");
    }

    private String getUniqueVariableName(final String name, Statement code) {
        if (code == null) return name;
        final Object[] found = new Object[1];
        CodeVisitorSupport cv = new CodeVisitorSupport() {
            public void visitVariableExpression(VariableExpression expression) {
                if (expression.getName().equals(name)) found[0] = Boolean.TRUE;
            }
        };
        code.visit(cv);
        if (found[0] != null) return getUniqueVariableName("_" + name, code);
        return name;
    }

    private boolean isAnonymousInnerClass(ClassNode enumClass) {
        if (!(enumClass instanceof EnumConstantClassNode)) return false;
        InnerClassNode ic = (InnerClassNode) enumClass;
        return ic.getVariableScope() == null;
    }
}
