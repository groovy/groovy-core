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
package org.codehaus.groovy.classgen.asm;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;

import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE;

/**
 * A {@link TypeChooser} which is aware of statement metadata.
 *
 * @author Jochen Theodorou
 * @author Cedric Champeau
 */
public class StatementMetaTypeChooser implements TypeChooser {
    public ClassNode resolveType(final Expression exp, final ClassNode current) {
        if (exp instanceof ClassExpression) return ClassHelper.CLASS_Type;
        OptimizingStatementWriter.StatementMeta meta = (OptimizingStatementWriter.StatementMeta) exp.getNodeMetaData(OptimizingStatementWriter.StatementMeta.class);
        ClassNode type = null;
        if (meta!=null) type = meta.type;
        if (type!=null) return type;
        if (exp instanceof VariableExpression) {
            VariableExpression ve = (VariableExpression) exp;
            if (ve.isClosureSharedVariable()) return ve.getType();
            type = ve.getOriginType();
            if (ve.getAccessedVariable() instanceof FieldNode) {
                FieldNode fn = (FieldNode) ve.getAccessedVariable();
                if (!fn.getDeclaringClass().equals(current)) return fn.getOriginType();
            }
        } else if (exp instanceof Variable) {
            Variable v = (Variable) exp;
            type = v.getOriginType();
        } else {
            type = exp.getType();
        }
        return type.redirect();
    }
}
