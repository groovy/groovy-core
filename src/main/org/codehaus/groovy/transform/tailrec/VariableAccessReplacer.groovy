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
import org.codehaus.groovy.ast.expr.*

/**
 * @author Johannes Link
 */
class VariableAccessReplacer {

    Map nameAndTypeMapping = [:] //e.g.: ['myVar': [name: '_myVar', type: MyType]]
    Closure replaceBy = { nameAndType -> AstHelper.createVariableReference(nameAndType) }

    void replaceIn(ASTNode root) {
        replaceAccessToParams(root)
    }

    private void replaceAccessToParams(ASTNode root) {
        replaceAllAccessToParamsInVariableExpressions(root)
        replaceAllAccessToParamsInNotExpression(root)
        replaceAllAccessToParamsInInnerExpression(root, BooleanExpression)
        replaceAllAccessToParamsInInnerExpression(root, UnaryMinusExpression)
        replaceAllAccessToParamsInInnerExpression(root, UnaryPlusExpression)
        replaceAllAccessToParamsInInnerExpression(root, SpreadExpression)
    }

    private void replaceAllAccessToParamsInVariableExpressions(ASTNode root) {
        def whenParam = { expression ->
            if (!(expression instanceof VariableExpression)) {
                return false
            }
            return nameAndTypeMapping.containsKey(expression.name)
        }
        def replaceWithLocalVariable = { expression ->
            def nameAndType = nameAndTypeMapping[expression.name]
            replaceBy(nameAndType)
        }
        def replacer = new ASTNodesReplacer(when: whenParam, replaceWith: replaceWithLocalVariable)
        replacer.replaceIn(root)
    }

    /**
     * BooleanExpressions need special handling since inner field expression is readonly
     */
    private void replaceAllAccessToParamsInInnerExpression(ASTNode root, Class expressionType) {
        def whenParamInInnerVariableExpression = { expression ->
            if (!(expressionType.isInstance(expression))) {
                return false
            }
            Expression inner = expression.expression
            if (!(inner instanceof VariableExpression)) {
                return false
            }
            return nameAndTypeMapping.containsKey(inner.name)
        }
        def replaceWithNewExpression = { expression ->
            def nameAndType = nameAndTypeMapping[expression.expression.name]
            expressionType.newInstance([replaceBy(nameAndType)].toArray())
        }
        def replacer = new ASTNodesReplacer(when: whenParamInInnerVariableExpression, replaceWith: replaceWithNewExpression)
        replacer.replaceIn(root)
    }

    /**
     * NotExpressions (within BooleanExpressions) need special handling since inner field expression is readonly
     */
    private void replaceAllAccessToParamsInNotExpression(ASTNode root) {
        def whenParamInNotExpression = { expression ->
            if (!(expression instanceof BooleanExpression)) {
                return false
            }
            Expression inner = expression.expression
            if (!(inner instanceof NotExpression)) {
                return false
            }
            if (!(inner.expression instanceof VariableExpression)) {
                return false
            }
            return nameAndTypeMapping.containsKey(inner.expression.name)
        }
        def replaceWithLocalVariableInNotExpression = { expression ->
            def nameAndType = nameAndTypeMapping[expression.expression.expression.name]
            new BooleanExpression(new NotExpression(replaceBy(nameAndType)))
        }
        def replacer = new ASTNodesReplacer(when: whenParamInNotExpression, replaceWith: replaceWithLocalVariableInNotExpression)
        replacer.replaceIn(root)
    }


}
