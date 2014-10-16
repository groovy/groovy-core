/*
 * Copyright 2003-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.text.markup

import groovy.transform.CompileStatic
import org.codehaus.groovy.antlr.AntlrParserPlugin
import org.codehaus.groovy.antlr.parser.GroovyLexer
import org.codehaus.groovy.antlr.parser.GroovyRecognizer
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.control.ResolveVisitor
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.ParserException
import org.codehaus.groovy.syntax.Reduction
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport
import org.codehaus.groovy.transform.stc.TypeCheckingContext

import java.util.concurrent.atomic.AtomicReference

import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE
import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.isAssignment

/**
 * <p>A static compilation type checking extension, responsible for transforming unresolved method
 * calls into direct calls to {@link BaseTemplate#methodMissing(java.lang.String, java.lang.Object)}
 * for faster rendering.</p>
 * <p>This extension also supports optional type checking of the model</p>
 *
 * @author Cedric Champeau
 */
class MarkupTemplateTypeCheckingExtension extends GroovyTypeCheckingExtensionSupport.TypeCheckingDSL {

    @Override
    Object run() {
        def modelTypesClassNodes = null

        beforeVisitClass { classNode ->
            def modelTypes = MarkupTemplateEngine.TemplateGroovyClassLoader.modelTypes.get()
            if (modelTypes!=null) {
                modelTypesClassNodes = [:]
                modelTypes.each { k, v ->
                    modelTypesClassNodes[k] = buildNodeFromString(v, context)
                }
            }
            def modelTypesFromTemplate = classNode.getNodeMetaData(MarkupTemplateEngine.MODELTYPES_ASTKEY)
            if (modelTypesFromTemplate) {
                if (modelTypesClassNodes==null) {
                    modelTypesClassNodes = modelTypesFromTemplate
                } else {
                    modelTypesClassNodes.putAll(modelTypesFromTemplate)
                }
            }
            if (modelTypesClassNodes==null) {
                // push a new error collector, we want type checking errors to be silent
                context.pushErrorCollector()
            }
        }

        beforeVisitMethod {
            newScope {
                builderCalls = []
                binaryExpressions = [:]
            }
        }
        methodNotFound { receiver, name, argList, argTypes, call ->
            if ("getAt"==name && OBJECT_TYPE==receiver) {
                // GROOVY-6940
                def enclosingBinaryExpression = context.enclosingBinaryExpression
                if (enclosingBinaryExpression.leftExpression.is(call.objectExpression)) {
                    def stack = context.enclosingBinaryExpressionStack
                    if (stack.size()>1) {
                        def superEnclosing = stack.get(1)
                        def opType = superEnclosing.operation.type
                        if (superEnclosing.leftExpression.is(enclosingBinaryExpression) && isAssignment(opType)) {
                            if (opType== Types.ASSIGN) {
                                // type checker looks for getAt() but we need to replace the super binary expression with a putAt
                                // foo[x] = y --> foo.putAt(x,y)
                                def mce = new MethodCallExpression(
                                        enclosingBinaryExpression.leftExpression,
                                        "putAt",
                                        new ArgumentListExpression(enclosingBinaryExpression.rightExpression, superEnclosing.rightExpression))
                                makeDynamic(mce)
                                currentScope.binaryExpressions.put(superEnclosing, mce)
                                return null
                            } else {
                                throw new UnsupportedOperationException("Operation not supported in templates: ${superEnclosing.text}. Please declare an explicit type for the variable.")
                            }
                        }
                    }
                    currentScope.binaryExpressions.put(enclosingBinaryExpression, call)
                    return makeDynamic(call)
                }
            }
            if (call.lineNumber > 0) {
                if (call.implicitThis) {
                    currentScope.builderCalls << call
                    return makeDynamic(call, OBJECT_TYPE)
                }
                if (modelTypesClassNodes==null) {
                    // unchecked mode
                    return makeDynamic(call, OBJECT_TYPE)
                }
            }
        }

        onMethodSelection { call, node ->
            if (isMethodCallExpression(call) && modelTypesClassNodes!=null) {
                def args = getArguments(call).expressions
                if (args.size()==1) {
                    String varName = isConstantExpression(args[0])?args[0].text:call.getNodeMetaData(MarkupBuilderCodeTransformer.TARGET_VARIABLE)
                    def type = modelTypesClassNodes[varName]
                    if (type) {
                        if (call.objectExpression.text=='this.getModel()') {
                            storeType(call, type)
                        } else if (call.methodAsString=='tryEscape') {
                            storeType(call, type)
                        }
                    }
                }
            }
        }

        unresolvedProperty { pexp ->
            if (pexp.objectExpression.text=='this.getModel()') {
                if (modelTypesClassNodes!=null) {
                    // type checked mode detected!
                    def type = modelTypesClassNodes[pexp.propertyAsString]
                    if (type) {
                        makeDynamic(pexp, type)
                    }
                } else {
                    makeDynamic(pexp)
                }
            } else if (modelTypesClassNodes==null) {
                // dynamic mode
                makeDynamic(pexp)
            }
        }

        afterVisitMethod { mn ->
            scopeExit {
                new BuilderMethodReplacer(context.source, builderCalls, binaryExpressions).visitMethod(mn)
            }
        }
    }

    @CompileStatic
    private static ClassNode buildNodeFromString(String option, TypeCheckingContext ctx) {
        GroovyLexer lexer = new GroovyLexer(new StringReader(option))
        final GroovyRecognizer rn = GroovyRecognizer.make(lexer)
        rn.classOrInterfaceType(true);
        final AtomicReference<ClassNode> ref = new AtomicReference<ClassNode>();
        AntlrParserPlugin plugin = new AntlrParserPlugin() {
            @Override
            public ModuleNode buildAST(
                    final SourceUnit sourceUnit,
                    final ClassLoader classLoader, final Reduction cst) throws ParserException {
                ref.set(makeTypeWithArguments(rn.getAST()));
                return null;
            }
        };
        plugin.buildAST(null, null, null);
        ClassNode parsedNode = ref.get();
        ClassNode dummyClass = new ClassNode("dummy", 0, ClassHelper.OBJECT_TYPE);
        dummyClass.setModule(new ModuleNode(ctx.source));
        MethodNode dummyMN = new MethodNode(
                "dummy",
                0,
                parsedNode,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                EmptyStatement.INSTANCE
        )
        dummyClass.addMethod(dummyMN);
        ResolveVisitor visitor = new ResolveVisitor(ctx.compilationUnit) {
            @Override
            protected void addError(final String msg, final ASTNode expr) {
                ctx.errorCollector.addErrorAndContinue(new SyntaxErrorMessage(
                        new SyntaxException(msg + '\n', expr.getLineNumber(), expr.getColumnNumber(), expr.getLastLineNumber(), expr.getLastColumnNumber()),
                        ctx.source)
                );
            }
        };
        visitor.startResolving(dummyClass, ctx.source)
        return dummyMN.getReturnType()
    }

    private static class BuilderMethodReplacer extends ClassCodeExpressionTransformer {

        private static final MethodNode METHOD_MISSING = ClassHelper.make(BaseTemplate).getMethods('methodMissing')[0]

        private final SourceUnit unit
        private final Set<MethodCallExpression> callsToBeReplaced
        private final Map<BinaryExpression, MethodCallExpression> binaryExpressionsToBeReplaced

        BuilderMethodReplacer(SourceUnit unit, Collection<MethodCallExpression> calls, Map<BinaryExpression, MethodCallExpression> binExpressionsWithReplacements) {
            this.unit = unit
            this.callsToBeReplaced = calls as Set
            this.binaryExpressionsToBeReplaced = binExpressionsWithReplacements
        }

        @Override
        protected SourceUnit getSourceUnit() {
            unit
        }

        @Override
        void visitClosureExpression(final ClosureExpression expression) {
            super.visitClosureExpression(expression)
        }

        @Override
        public Expression transform(final Expression exp) {
            if (exp instanceof BinaryExpression && binaryExpressionsToBeReplaced.containsKey(exp)) {
                return binaryExpressionsToBeReplaced.get(exp)
            }
            if (callsToBeReplaced.contains(exp)) {
                def args = exp.arguments instanceof TupleExpression ? exp.arguments.expressions : [exp.arguments]
                args*.visit(this)
                // replace with direct call to methodMissing
                def call = new MethodCallExpression(
                        new VariableExpression("this"),
                        "methodMissing",
                        new ArgumentListExpression(
                                new ConstantExpression(exp.getMethodAsString()),
                                new ArrayExpression(
                                        OBJECT_TYPE,
                                        [* args]
                                )
                        )
                )
                call.implicitThis = true
                call.safe = exp.safe
                call.spreadSafe = exp.spreadSafe
                call.methodTarget = METHOD_MISSING
                call
            } else if (exp instanceof ClosureExpression) {
                exp.code.visit(this)
                super.transform(exp)
            } else {
                super.transform(exp)
            }
        }
    }
}
