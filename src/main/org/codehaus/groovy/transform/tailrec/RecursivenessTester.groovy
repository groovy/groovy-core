package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression

/**
 *
 * Test if a method call is recursive if called within a given method node.
 * Tries to handle static calls as well.
 * 
 * Currently known simplifications:
 * Does not check for matching argument types but only considers the number of arguments.
 * Does not check for matching return types; even void and any object type are considered to be compatible.
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
		call.objectExpression.isThisExpression()
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
     * optional in Groovy
     */
    private areTypesCallCompatible(ClassNode argType, ClassNode paramType) {
        ClassNode boxedArg = boxIfPossible(argType)
        ClassNode boxedParam = boxIfPossible(paramType)
        return boxedArg.isDerivedFrom(boxedParam) || boxedParam.isDerivedFrom(boxedArg)
    }

    private ClassNode boxIfPossible(ClassNode classNode) {
        switch(classNode) {
            case ClassHelper.int_TYPE:
                return ClassHelper.Integer_TYPE
            case ClassHelper.double_TYPE:
                return ClassHelper.Double_TYPE
            case ClassHelper.float_TYPE:
                return ClassHelper.Float_TYPE
            case ClassHelper.char_TYPE:
                return ClassHelper.Character_TYPE
            case ClassHelper.byte_TYPE:
                return ClassHelper.Byte_TYPE
            case ClassHelper.short_TYPE:
                return ClassHelper.Short_TYPE
            case ClassHelper.long_TYPE:
                return ClassHelper.Long_TYPE
            default:
                return classNode
        }
    }
}
