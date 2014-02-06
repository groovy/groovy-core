/*
 * Copyright 2013-2014 the original author or authors.
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
package org.codehaus.groovy.transform.tailrec

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement

/**
 * Since a ternary statement has more than one exit point tail-recursiveness testing cannot be easily done.
 * Therefore this class translates a ternary statement (or Elvis operator) into the equivalent if-else statement.
 *
 * @author Johannes Link
 */
@CompileStatic
class TernaryToIfStatementConverter {

    Statement convert(ReturnStatement statementWithInnerTernaryExpression) {
        if (!(statementWithInnerTernaryExpression.expression instanceof TernaryExpression))
            return statementWithInnerTernaryExpression
        TernaryExpression ternary = statementWithInnerTernaryExpression.expression as TernaryExpression
        return new IfStatement(ternary.booleanExpression, new ReturnStatement(ternary.trueExpression), new ReturnStatement(ternary.falseExpression))
    }
}