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

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.classgen.ReturnAdder

/**
 * Adds explicit return statements to implicit return points in a closure. This is necessary since
 * tail-recursion is detected by having the recursive call within the return statement.
 *
 * @author Johannes Link
 */
class ReturnAdderForClosures extends CodeVisitorSupport {

    synchronized void visitMethod(MethodNode method) {
        method.code.visit(this)
    }

    public void visitClosureExpression(ClosureExpression expression) {
        //Create a dummy method with the closure's code as the method's code. Then user ReturnAdder, which only works for methods.
        MethodNode node = new MethodNode("dummy", 0, ClassHelper.OBJECT_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, expression.code);
        new ReturnAdder().visitMethod(node);
        super.visitClosureExpression(expression)
    }

}
