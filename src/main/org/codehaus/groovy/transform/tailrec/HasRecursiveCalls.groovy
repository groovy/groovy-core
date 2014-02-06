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
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression

/**
 *
 * Check if there are any recursive calls in a method
 *
 * @author Johannes Link
 */
@CompileStatic
class HasRecursiveCalls extends CodeVisitorSupport {
    MethodNode method
    boolean hasRecursiveCalls = false

    public void visitMethodCallExpression(MethodCallExpression call) {
        if (isRecursive(call)) {
            hasRecursiveCalls = true
        } else {
            super.visitMethodCallExpression(call)
        }
    }

    public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
        if (isRecursive(call)) {
            hasRecursiveCalls = true
        } else {
            super.visitStaticMethodCallExpression(call)
        }
    }

    private boolean isRecursive(call) {
        new RecursivenessTester().isRecursive(method: method, call: call)
    }

    synchronized boolean test(MethodNode method) {
        hasRecursiveCalls = false
        this.method = method
        this.method.code.visit(this)
        hasRecursiveCalls
    }
}