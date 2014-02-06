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

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression

/**
 *
 * Test if a method call is recursive if called within a given method node.
 * Handles static calls as well.
 * 
 * Currently known simplifications:
 * - Does not check for method overloading or overridden methods.
 * - Does not check for matching return types; even void and any object type are considered to be compatible.
 * - Argument type matching could be more specific in case of static compilation.
 * - Method names via a GString are never considered to be recursive
 * 
 * @author Johannes Link
 */
class RecursivenessTester {
	public boolean isRecursive(params) {
		assert params.method.class == MethodNode
		assert params.call.class == MethodCallExpression || StaticMethodCallExpression

		isRecursive(params.method, params.call)
	}

	public boolean isRecursive(MethodNode method, MethodCallExpression call) {
		if (!isCallToThis(call))
			return false
        // Could be a GStringExpression
        if (! (call.method instanceof ConstantExpression))
            return false
		if (call.method.value != method.name)
			return false
		methodParamsMatchCallArgs(method, call)
	}

    public boolean isRecursive(MethodNode method, StaticMethodCallExpression call) {
        if (!method.isStatic())
            return false
        if (method.declaringClass != call.ownerType)
            return false
        if (call.method != method.name)
            return false
        methodParamsMatchCallArgs(method, call)
    }

	private boolean isCallToThis(MethodCallExpression call) {
		if (call.objectExpression == null)
			return call.isImplicitThis()
        if (! (call.objectExpression instanceof VariableExpression)) {
            return false
        }
		return call.objectExpression.isThisExpression()
	}
	
	private boolean methodParamsMatchCallArgs(method, call) {
        if (method.parameters.size() != call.arguments.expressions.size())
            return false
        def classNodePairs = [method.parameters*.type, call.arguments*.type].transpose()
        return classNodePairs.every { ClassNode paramType, ClassNode argType  ->
            return areTypesCallCompatible(argType, paramType)
        }
	}

    /**
     * Parameter type and calling argument type can both be derived from the other since typing information is
     * optional in Groovy.
     * Since int is not derived from Integer (nor the other way around) we compare the boxed types
     */
    private areTypesCallCompatible(ClassNode argType, ClassNode paramType) {
        ClassNode boxedArg = ClassHelper.getWrapper(argType)
        ClassNode boxedParam = ClassHelper.getWrapper(paramType)
        return boxedArg.isDerivedFrom(boxedParam) || boxedParam.isDerivedFrom(boxedArg)
    }

}
