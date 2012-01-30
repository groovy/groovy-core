/*
 * Copyright 2003-2011 the original author or authors.
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
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
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
        addConstructor(enumClass);
    }

    private void addConstructor(ClassNode enumClass) {
        // first look if there are declared constructors
        List<ConstructorNode> ctors = new ArrayList<ConstructorNode>(enumClass.getDeclaredConstructors());
        if (ctors.size() == 0) {
            // add default constructor
            ConstructorNode init = new ConstructorNode(Opcodes.ACC_PUBLIC, new Parameter[0], ClassNode.EMPTY_ARRAY, new BlockStatement());
            enumClass.addConstructor(init);
            ctors.add(init);
        }

        // for each constructor:
        // if constructor does not define a call to super, then transform constructor
        // to get String,int parameters at beginning and add call super(String,int)  

        for (ConstructorNode ctor : ctors) {
            transformConstructor(ctor);
        }
    }

    private void transformConstructor(ConstructorNode ctor) {
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
        if (chainedThisConstructorCall) {
            TupleExpression args = (TupleExpression) cce.getArguments();
            List<Expression> argsExprs = args.getExpressions();
            argsExprs.add(0, new VariableExpression(stringParameterName));
            argsExprs.add(1, new VariableExpression(intParameterName));
        } else {
            // and a super call
            cce = new ConstructorCallExpression(
                    ClassNode.SUPER,
                    new ArgumentListExpression(
                            new VariableExpression(stringParameterName),
                            new VariableExpression(intParameterName)
                    )
            );
            BlockStatement code = new BlockStatement();
            code.addStatement(new ExpressionStatement(cce));
            Statement oldCode = ctor.getCode();
            if (oldCode != null) code.addStatement(oldCode);
            ctor.setCode(code);
        }
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

}
