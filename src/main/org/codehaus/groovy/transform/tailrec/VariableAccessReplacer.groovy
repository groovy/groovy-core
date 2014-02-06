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

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.expr.VariableExpression

/**
 * Replace all access to variables and args by new variables.
 * The variable names to replace as well as their replacement name and type have to be configured
 * in nameAndTypeMapping before calling replaceIn().
 *
 * The closure replaceBy can be changed if clients want to do more in case of replacement.
 *
 * @author Johannes Link
 */
class VariableAccessReplacer {

    Map<String, Map> nameAndTypeMapping = [:] //e.g.: ['varToReplace': [name: 'newVar', type: TypeOfVar]]
    Closure<VariableExpression> replaceBy = { Map nameAndType -> AstHelper.createVariableReference(nameAndType) }

    void replaceIn(ASTNode root) {
        Closure<Boolean> whenParam = { VariableExpression expr ->
            return nameAndTypeMapping.containsKey(expr.name)
        }
        Closure<VariableExpression> replaceWithLocalVariable = { VariableExpression expr ->
            Map nameAndType = nameAndTypeMapping[expr.name]
            return replaceBy(nameAndType)
        }
        new VariableExpressionReplacer(when: whenParam, replaceWith: replaceWithLocalVariable).replaceIn(root)
    }

}

