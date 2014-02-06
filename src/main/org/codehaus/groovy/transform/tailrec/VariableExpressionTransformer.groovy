package org.codehaus.groovy.transform.tailrec

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ExpressionTransformer
import org.codehaus.groovy.ast.expr.VariableExpression

@CompileStatic
class VariableExpressionTransformer implements ExpressionTransformer {

    Closure<Boolean> when
    Closure<VariableExpression> replaceWith

    @Override
    Expression transform(Expression expr) {
        if ((expr instanceof VariableExpression) && when(expr)) {
            VariableExpression newExpr = replaceWith(expr)
            newExpr.setSourcePosition(expr);
            newExpr.copyNodeMetaData(expr);
            return newExpr
        }
        return expr.transformExpression(this)
    }
}
