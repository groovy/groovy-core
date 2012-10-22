/*
 * Copyright 2003-2009 the original author or authors.
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
package org.codehaus.groovy.transform.sc.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ExpressionTransformer;
import org.codehaus.groovy.classgen.AsmClassGenerator;
import org.codehaus.groovy.classgen.asm.WriterController;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compares two objects using identity comparison.
 * This expression will generate bytecode using the IF_ACMPNE instruction, instead of
 * using the "equals" method that is currently mapped to "==" in Groovy.
 *
 * This expression should only be used to compare to objects, not primitives, and only
 * in the context of reference equality check.
 *
 * @author Cedric Champeau
 */
public class CompareIdentityExpression extends BinaryExpression implements Opcodes {
    private final Expression leftExpression;
    private final Expression rightExpression;

    public CompareIdentityExpression(final Expression leftExpression, final Expression rightExpression) {
        super(leftExpression, new Token(Types.COMPARE_TO, "==", -1, -1), rightExpression);
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    @Override
    public Expression transformExpression(final ExpressionTransformer transformer) {
        return this;
    }

    @Override
    public void visit(final GroovyCodeVisitor visitor) {
        if (visitor instanceof AsmClassGenerator) {
            AsmClassGenerator acg = (AsmClassGenerator) visitor;
            WriterController controller = acg.getController();
            ClassNode leftType = controller.getTypeChooser().resolveType(leftExpression, controller.getClassNode());
            ClassNode rightType = controller.getTypeChooser().resolveType(rightExpression, controller.getClassNode());
            if (ClassHelper.isPrimitiveType(leftType) || ClassHelper.isPrimitiveType(rightType)) {
                throw new IllegalArgumentException("Both operands of a CompareIdentityExpression must be objects");
            }
            MethodVisitor mv = controller.getMethodVisitor();
            leftExpression.visit(acg);
            rightExpression.visit(acg);
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l1);
            mv.visitInsn(ICONST_1);
            Label l2 = new Label();
            mv.visitJumpInsn(GOTO, l2);
            mv.visitLabel(l1);
            mv.visitInsn(ICONST_0);
            mv.visitLabel(l2);
            controller.getOperandStack().replace(ClassHelper.boolean_TYPE, 2);
        } else {
            super.visit(visitor);
        }
    }
}
