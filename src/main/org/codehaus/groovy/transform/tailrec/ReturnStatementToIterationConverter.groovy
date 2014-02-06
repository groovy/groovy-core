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
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement

/**
 * @author Johannes Link
 */
class ReturnStatementToIterationConverter {

    Statement recurStatement = AstHelper.recurStatement()

    Statement convert(ReturnStatement statement, Map positionMapping) {
        def recursiveCall = statement.expression
        if (!recursiveCall.class in [
                MethodCallExpression,
                StaticMethodCallExpression
        ])
            return statement

        Map tempMapping = [:]
        Map tempDeclarations = [:]
        List<ExpressionStatement> argAssignments = []

        BlockStatement result = new BlockStatement()
        result.statementLabel = statement.statementLabel
        recursiveCall.arguments.expressions.eachWithIndex { Expression expression, index ->
            def argName = positionMapping[index].name
            def tempName = "_${argName}_"
            def argAndTempType = positionMapping[index].type
            tempMapping[argName] = [name: tempName, type: argAndTempType]
            def tempDeclaration = AstHelper.createVariableAlias(tempName, argAndTempType, argName)
            tempDeclarations[tempName] = tempDeclaration
            result.addStatement(tempDeclaration)
            def argAssignment = AstHelper.createAssignment(argName, argAndTempType, expression)
            argAssignments << argAssignment
            result.addStatement(argAssignment)
        }
        def unusedTemps = replaceAllArgUsages(argAssignments, tempMapping)
        for (String temp : unusedTemps) {
            result.statements.remove(tempDeclarations[temp])
        }
        result.addStatement(recurStatement)

        return result
    }

    private replaceAllArgUsages(List<ExpressionStatement> nodes, tempMapping) {
        def unusedTempNames = new HashSet(tempMapping.values()*.name)
        for (ExpressionStatement statement : nodes) {
            unusedTempNames.removeAll(replaceArgUsageByTempUsage(statement.expression, tempMapping))
        }
        return unusedTempNames
    }

    private replaceArgUsageByTempUsage(BinaryExpression binary, Map tempMapping) {
        Set usedTempNames = [] as Set
        def useTempInstead = { Map tempNameAndType ->
            usedTempNames << tempNameAndType.name
            AstHelper.createVariableReference(tempNameAndType)
        }
        VariableAccessReplacer replacer = new VariableAccessReplacer(nameAndTypeMapping: tempMapping, replaceBy: useTempInstead)
        // Replacement must only happen in binary.rightExpression. It's a hack in VariableExpressionReplacer which takes care of that.
        replacer.replaceIn(binary)
        return usedTempNames
    }
}
