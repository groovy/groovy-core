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

import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement

/**
 * Translates all return statements into an invocation of the next iteration. This can be either
 * - "continue LOOP_LABEL": Outside closures
 * - "throw LOOP_EXCEPTION": Inside closures
 *
 * Moreover, before adding the recur statement the iteration parameters (originally the method args)
 * are set to their new value. To prevent variable aliasing parameters will be copied into temp vars
 * before they are changes so that their current iteration value can be used when setting other params.
 *
 * There's probably place for optimizing the amount of variable copying being done, e.g.
 * parameters that are only handed through must not be copied at all.
 *
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

    private replaceAllArgUsages(List<ExpressionStatement> nodes, Map tempMapping) {
        Set<String> unusedTempNames = new HashSet(tempMapping.values()*.name)
        VariableReplacedListener tracker = new UsedVariableTracker()
        for (ExpressionStatement statement : nodes) {
            replaceArgUsageByTempUsage(statement.expression, tempMapping, tracker)
        }
        unusedTempNames = unusedTempNames - tracker.usedVariableNames
        return unusedTempNames
    }

    private void replaceArgUsageByTempUsage(BinaryExpression binary, Map tempMapping, UsedVariableTracker tracker) {
        VariableAccessReplacer replacer = new VariableAccessReplacer(nameAndTypeMapping: tempMapping, listener: tracker)
        // Replacement must only happen in binary.rightExpression. It's a hack in VariableExpressionReplacer which takes care of that.
        replacer.replaceIn(binary)
    }
}

class UsedVariableTracker implements VariableReplacedListener {

    final Set<String> usedVariableNames = [] as Set

    @Override
    void variableReplaced(VariableExpression oldVar, VariableExpression newVar) {
        usedVariableNames.add(newVar.name)
    }
}
