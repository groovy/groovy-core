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

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * @deprecated use org.codehaus.groovy.ast.tools.GeneralUtils
 */
@Deprecated
public abstract class AbstractASTTransformUtil implements Opcodes {
    @Deprecated
    public static Statement assignStatement(Expression target, Expression value) {
        return GeneralUtils.assignS(target, value);
    }

    @Deprecated
    public static Statement createConstructorStatementDefault(FieldNode fNode) {
        return GeneralUtils.createConstructorStatementDefault(fNode);
    }

    @Deprecated
    public static ExpressionStatement declStatement(Expression result, Expression init) {
        return (ExpressionStatement) GeneralUtils.declS(result, init);
    }

    @Deprecated
    public static BooleanExpression differentExpr(Expression self, Expression other) {
        return GeneralUtils.differentX(self, other);
    }

    @Deprecated
    public static BooleanExpression differentFieldExpr(FieldNode fNode, Expression other) {
        return GeneralUtils.differentFieldX(fNode, other);
    }

    @Deprecated
    public static BooleanExpression differentPropertyExpr(PropertyNode pNode, Expression other) {
        return GeneralUtils.differentPropertyX(pNode, other);
    }

    @Deprecated
    public static BooleanExpression equalsNullExpr(Expression argExpr) {
        return GeneralUtils.equalsNullX(argExpr);
    }

    @Deprecated
    public static Expression findArg(String argName) {
        return GeneralUtils.findArg(argName);
    }

    @Deprecated
    public static List<FieldNode> getInstanceNonPropertyFields(ClassNode cNode) {
        return GeneralUtils.getInstanceNonPropertyFields(cNode);
    }

    @Deprecated
    public static List<PropertyNode> getInstanceProperties(ClassNode cNode) {
        return GeneralUtils.getInstanceProperties(cNode);
    }

    @Deprecated
    public static List<FieldNode> getInstancePropertyFields(ClassNode cNode) {
        return GeneralUtils.getInstancePropertyFields(cNode);
    }

    @Deprecated
    public static List<FieldNode> getSuperNonPropertyFields(ClassNode cNode) {
        return GeneralUtils.getSuperNonPropertyFields(cNode);
    }

    @Deprecated
    public static List<FieldNode> getSuperPropertyFields(ClassNode cNode) {
        return GeneralUtils.getSuperPropertyFields(cNode);
    }

    @Deprecated
    public static boolean hasDeclaredMethod(ClassNode cNode, String name, int argsCount) {
        return GeneralUtils.hasDeclaredMethod(cNode, name, argsCount);
    }

    @Deprecated
    public static BooleanExpression identicalExpr(Expression self, Expression other) {
        return GeneralUtils.identicalX(self, other);
    }

    @Deprecated
    public static BooleanExpression isInstanceOf(Expression objectExpression, ClassNode cNode) {
        return GeneralUtils.isInstanceOf(objectExpression, cNode);
    }

    @Deprecated
    public static BooleanExpression isInstanceof(ClassNode cNode, Expression other) {
        return GeneralUtils.isInstanceOf(other, cNode);
    }

    @Deprecated
    public static BooleanExpression isOneExpr(Expression expr) {
        return GeneralUtils.isOneX(expr);
    }

    @Deprecated
    public static boolean isOrImplements(ClassNode fieldType, ClassNode interfaceType) {
        return GeneralUtils.isOrImplements(fieldType, interfaceType);
    }

    @Deprecated
    public static BooleanExpression isTrueExpr(Expression argExpr) {
        return GeneralUtils.isTrueX(argExpr);
    }

    @Deprecated
    public static BooleanExpression isZeroExpr(Expression expr) {
        return GeneralUtils.isZeroX(expr);
    }

    @Deprecated
    public static BooleanExpression notNullExpr(Expression argExpr) {
        return GeneralUtils.notNullExpr(argExpr);
    }

    @Deprecated
    public static Statement returnFalseIfFieldNotEqual(FieldNode fNode, Expression other) {
        return GeneralUtils.returnFalseIfFieldNotEqual(fNode, other);
    }

    @Deprecated
    public static Statement returnFalseIfNotInstanceof(ClassNode cNode, Expression other) {
        return GeneralUtils.returnFalseIfNotInstanceof(cNode, other);
    }

    @Deprecated
    public static IfStatement returnFalseIfNull(Expression other) {
        return GeneralUtils.returnFalseIfNull(other);
    }

    @Deprecated
    public static Statement returnFalseIfPropertyNotEqual(PropertyNode pNode, Expression other) {
        return GeneralUtils.returnFalseIfPropertyNotEqual(pNode, other);
    }

    @Deprecated
    public static Statement returnFalseIfWrongType(ClassNode cNode, Expression other) {
        return GeneralUtils.returnFalseIfWrongType(cNode, other);
    }

    @Deprecated
    public static IfStatement returnTrueIfIdentical(Expression self, Expression other) {
        return GeneralUtils.returnTrueIfIdentical(self, other);
    }

    @Deprecated
    public static Statement safeExpression(Expression fieldExpr, Expression expression) {
        return GeneralUtils.safeExpression(fieldExpr, expression);
    }

}
