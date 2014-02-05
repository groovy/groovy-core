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

import groovy.transform.Memoized
import groovy.transform.TailRecursive
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.ReturnAdder
import org.codehaus.groovy.classgen.VariableScopeVisitor
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Handles generation of code for the @TailRecursive annotation.
 *
 * It's doing its work in the earliest possible compile phase
 *
 * @author Johannes Link
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class TailRecursiveASTTransformation extends AbstractASTTransformation {

    private static final Class MY_CLASS = TailRecursive.class;
    private static final ClassNode MY_TYPE = new ClassNode(MY_CLASS);
    static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage()
    private HasRecursiveCalls hasRecursiveCalls = new HasRecursiveCalls()
    private TernaryToIfStatementConverter ternaryToIfStatement = new TernaryToIfStatementConverter()


    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);

        MethodNode method = nodes[1]
        if (hasAnnotation(method, ClassHelper.make(Memoized)))
            addError("@TailRecursive is incompatible with @Memoized", method)

        if (!hasRecursiveMethodCalls(method)) {
            //Todo: Emit a compiler warning. How to do that?
            //System.err.println(transformationDescription(method) + " skipped: No recursive calls detected.")
            return;
        }
        transformToIteration(method, source)
        ensureAllRecursiveCallsHaveBeenTransformed(method)
    }

    private boolean hasAnnotation(MethodNode methodNode, ClassNode annotation) {
        List annots = methodNode.getAnnotations(annotation);
        return (annots != null && annots.size() > 0);
    }


    private void transformToIteration(MethodNode method, SourceUnit source) {
        if (method.isVoidMethod()) {
            transformVoidMethodToIteration(method, source)
        } else {
            transformNonVoidMethodToIteration(method, source)
        }
    }

    private void transformVoidMethodToIteration(MethodNode method, SourceUnit source) {
        addError("Void methods are not supported yet", method)
    }

    private void transformNonVoidMethodToIteration(MethodNode method, SourceUnit source) {
        addMissingDefaultReturnStatement(method)
        replaceReturnsWithTernariesToIfStatements(method)
        wrapMethodBodyWithWhileLoop(method)
        def (nameAndTypeMapping, positionMapping) = parameterMappingFor(method)
        replaceAllAccessToParams(method, nameAndTypeMapping)
        addLocalVariablesForAllParameters(method, nameAndTypeMapping) //must happen after replacing access to params
        replaceAllRecursiveReturnsWithIteration(method, positionMapping)
        repairVariableScopes(source, method)
    }

    private void repairVariableScopes(SourceUnit source, MethodNode method) {
        new VariableScopeVisitor(source).visitClass(method.declaringClass)
    }

    private void replaceReturnsWithTernariesToIfStatements(MethodNode method) {
        def whenReturnWithTernary = { expression ->
            if (!(expression instanceof ReturnStatement)) {
                return false
            }
            return (expression.expression instanceof TernaryExpression)
        }
        def replaceWithIfStatement = { expression ->
            ternaryToIfStatement.convert(expression)
        }
        def replacer = new StatementReplacer(when: whenReturnWithTernary, replaceWith: replaceWithIfStatement)
        replacer.replaceIn(method.code)

    }

    private void addLocalVariablesForAllParameters(MethodNode method, Map nameAndTypeMapping) {
        BlockStatement code = method.code
        nameAndTypeMapping.each { paramName, localNameAndType ->
            code.statements.add(0, AstHelper.createVariableDefinition(localNameAndType.name, localNameAndType.type, new VariableExpression(paramName, localNameAndType.type)))
        }
    }

    private void replaceAllAccessToParams(MethodNode method, Map nameAndTypeMapping) {
        new VariableAccessReplacer(nameAndTypeMapping: nameAndTypeMapping).replaceIn(method.code)
    }

    private parameterMappingFor(MethodNode method) {
        def nameAndTypeMapping = [:]
        def positionMapping = [:]
        BlockStatement code = method.code
        method.parameters.eachWithIndex { Parameter param, index ->
            def paramName = param.name
            def paramType = param.type
            def localName = '_' + paramName + '_'
            nameAndTypeMapping[paramName] = [name: localName, type: paramType]
            positionMapping[index] = [name: localName, type: paramType]
        }
        return [nameAndTypeMapping, positionMapping]
    }

    private void replaceAllRecursiveReturnsWithIteration(MethodNode method, Map positionMapping) {
        replaceRecursiveReturnsOutsideClosures(method, positionMapping)
        replaceRecursiveReturnsInsideClosures(method, positionMapping)
    }

    private void replaceRecursiveReturnsOutsideClosures(MethodNode method, Map positionMapping) {
        def whenRecursiveReturn = { Statement statement, boolean inClosure ->
            if (inClosure)
                return false
            if (!(statement instanceof ReturnStatement)) {
                return false
            }
            Expression inner = statement.expression
            if (!(inner instanceof MethodCallExpression) && !(inner instanceof StaticMethodCallExpression)) {
                return false
            }
            return isRecursiveIn(inner, method)
        }
        def replaceWithContinueBlock = { statement ->
            new ReturnStatementToIterationConverter().convert(statement, positionMapping)
        }
        def replacer = new StatementReplacer(when: whenRecursiveReturn, replaceWith: replaceWithContinueBlock)
        replacer.replaceIn(method.code)
    }

    private void replaceRecursiveReturnsInsideClosures(MethodNode method, Map positionMapping) {
        def whenRecursiveReturn = { Statement statement, boolean inClosure ->
            if (!inClosure)
                return false
            if (!(statement instanceof ReturnStatement)) {
                return false
            }
            Expression inner = statement.expression
            if (!(inner instanceof MethodCallExpression) && !(inner instanceof StaticMethodCallExpression)) {
                return false
            }
            return isRecursiveIn(inner, method)
        }
        def replaceWithContinueBlock = { statement ->
            new ReturnStatementToIterationConverter(recurStatement: AstHelper.recurByThrowStatement()).convert(statement, positionMapping)
        }
        def replacer = new StatementReplacer(when: whenRecursiveReturn, replaceWith: replaceWithContinueBlock)
        replacer.replaceIn(method.code)
    }

    private void wrapMethodBodyWithWhileLoop(MethodNode method) {
        new InWhileLoopWrapper().wrap(method)
    }

    private void addMissingDefaultReturnStatement(MethodNode method) {
        new ReturnAdder().visitMethod(method)
        new ReturnAdderForClosures().addReturnsIfNeeded(method)
    }

    private void ensureAllRecursiveCallsHaveBeenTransformed(MethodNode method) {
        def remainingRecursiveCalls = new CollectRecursiveCalls().collect(method)
        remainingRecursiveCalls.each {
            addError("Recursive call could not be transformed.", it)
        }
    }

    private transformationDescription(MethodNode method) {
        "$MY_TYPE_NAME transformation on '${method.declaringClass}.${method.name}(${method.parameters.size()} params)'"
    }

    private boolean hasRecursiveMethodCalls(MethodNode method) {
        hasRecursiveCalls.test(method)
    }

    private boolean isRecursiveIn(methodCall, MethodNode method) {
        new RecursivenessTester().isRecursive(method, methodCall)
    }
}