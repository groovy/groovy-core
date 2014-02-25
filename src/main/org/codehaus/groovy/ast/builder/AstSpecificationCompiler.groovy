/*
 * Copyright 2003-2010 the original author or authors.
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
package org.codehaus.groovy.ast.builder

import groovy.transform.PackageScope
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types

/**
 * Handles parsing the properties from the closure into values that can be referenced.
 * 
 * This object is very stateful and not threadsafe. It accumulates expressions in the 
 * 'expression' field as they are found and executed within the DSL. 
 * 
 * Note: this class consists of many one-line method calls. A better implementation
 * might be to take a declarative approach and replace the one-liners with map entries. 
 * 
 * @author Hamlet D'Arcy
 */
@PackageScope class AstSpecificationCompiler implements GroovyInterceptable {

    private final List<ASTNode> expression = []

    /**
     * Creates the DSL compiler.
     */

    AstSpecificationCompiler(@DelegatesTo(AstSpecificationCompiler) Closure spec) {
        spec.delegate = this
        spec()
    }


    /**
     * Gets the current generated expression.
     */

    List<ASTNode> getExpression() {
        return expression
    }

    /**
    * This method takes a List of Classes (a "spec"), and makes sure that the expression field 
    * contains those classes. It is a safety mechanism to enforce that the DSL is being called
    * properly. 
    * 
    * @param methodName
    *   the name of the method within the DSL that is being invoked. Used in creating error messages. 
    * @param spec
    *   the list of Class objects that the method expects to have in the expression field when invoked.
    * @return 
    *   the portions of the expression field that adhere to the spec. 
    */ 
    private List<ASTNode> enforceConstraints(String methodName, List<Class> spec) {

        // enforce that the correct # arguments was passed
        if (spec.size() != expression.size()) {
            throw new IllegalArgumentException("$methodName could not be invoked. Expected to receive parameters $spec but found ${expression?.collect { it.class }}")
        }

        // enforce types and collect result
        (0..(spec.size() - 1)).collect { int it ->
            def actualClass = expression[it].class
            def expectedClass = spec[it]
            if (!expectedClass.isAssignableFrom(actualClass)) {
                throw new IllegalArgumentException("$methodName could not be invoked. Expected to receive parameters $spec but found ${expression?.collect { it.class }}")
            }
            expression[it]
        }
    }

    /**
    * This method helps you take Closure parameters to a method and bundle them into 
    * constructor calls to a specific ASTNode subtype. 
    * @param name 
    *       name of object being constructed, used to create helpful error message. 
    * @param argBlock
    *       the actual parameters being specified for the node
    * @param constructorStatement
    *       the type specific construction code that will be run
    */ 
    private void captureAndCreateNode(String name, @DelegatesTo(AstSpecificationCompiler) Closure argBlock, Closure constructorStatement) {
        if (!argBlock) throw new IllegalArgumentException("nodes of type $name require arguments to be specified")

        def oldProps = new ArrayList(expression)
        expression.clear()
        new AstSpecificationCompiler(argBlock)
        def result = constructorStatement(expression) // invoke custom constructor for node
        expression.clear()
        expression.addAll(oldProps)
        expression.add(result)
    }

    /**
    * Helper method to convert a DSL invocation into an ASTNode instance. 
    * 
    * @param target     
    *       the class you are going to create
    * @param typeAlias  
    *       the DSL keyword that was used to invoke this type
    * @param ctorArgs   
    *       a specification of what arguments the constructor expects
    * @param argBlock   
    *       the single closure argument used during invocation
    */ 
    private void makeNode(Class target, String typeAlias, List<Class<? super ASTNode>> ctorArgs, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode(target.class.simpleName, argBlock) {
            target.newInstance(
                    * enforceConstraints(typeAlias, ctorArgs)
            )
        }
    }


    /**
    * Helper method to convert a DSL invocation with a list of parameters specified 
    * in a Closure into an ASTNode instance. 
    * 
    * @param target     
    *       the class you are going to create
    * @param argBlock   
    *       the single closure argument used during invocation
    */ 
    private void makeNodeFromList(Class target, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        //todo: add better error handling?
        captureAndCreateNode(target.simpleName, argBlock) {
            target.newInstance(new ArrayList(expression))
        }
    }


    /**
    * Helper method to convert a DSL invocation with a String parameter into a List of ASTNode instances. 
    * 
    * @param argBlock   
    *       the single closure argument used during invocation
    * @param input   
    *       the single String argument used during invocation
    */ 
    private void makeListOfNodes(@DelegatesTo(AstSpecificationCompiler) Closure argBlock, String input) {
        captureAndCreateNode(input, argBlock) {
            new ArrayList(expression)
        }
    }


    /**
    * Helper method to convert a DSL invocation with a String parameter into an Array of ASTNode instances. 
    * 
    * @param argBlock   
    *       the single closure argument used during invocation
    * @param target   
    *       the target type
    */ 
    private void makeArrayOfNodes(Object target, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode(target.class.simpleName, argBlock) {
            expression.toArray(target)
        }
    }

    
    /**
    * Helper method to convert a DSL invocation into an ASTNode instance when a Class parameter is specified. 
    * 
    * @param target     
    *       the class you are going to create
    * @param alias  
    *       the DSL keyword that was used to invoke this type
    * @param spec
    *       the list of Classes that you expect to be present as parameters
    * @param argBlock   
    *       the single closure argument used during invocation
    * @param type 
    *       a type parameter
    */ 
    private void makeNodeWithClassParameter(Class target, String alias, List<Class> spec, @DelegatesTo(AstSpecificationCompiler) Closure argBlock, Class type) {
        captureAndCreateNode(target.class.simpleName, argBlock) {
            expression.add(0, ClassHelper.make(type))
            target.newInstance(
                    * enforceConstraints(alias, spec)
            )
        }
    }

    private void makeNodeWithStringParameter(Class target, String alias, List<Class> spec, @DelegatesTo(AstSpecificationCompiler) Closure argBlock, String text) {
        captureAndCreateNode(target.class.simpleName, argBlock) {
            expression.add(0, text)
            target.newInstance(
                    * enforceConstraints(alias, spec)
            )
        }
    }


    /**
     * Creates a CastExpression.
     */

    private void cast(Class type, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeWithClassParameter(CastExpression, 'cast', [ClassNode, Expression], argBlock, type)
    }


    /**
     * Creates an ConstructorCallExpression.
     */

    private void constructorCall(Class type, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeWithClassParameter(ConstructorCallExpression, 'constructorCall', [ClassNode, Expression], argBlock, type)
    }


    /**
     * Creates a MethodCallExpression.
     */

    private void methodCall(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(MethodCallExpression, 'methodCall', [Expression, Expression, Expression], argBlock)
    }


    /**
     * Creates an AnnotationConstantExpression.
     */

    private void annotationConstant(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(AnnotationConstantExpression, 'annotationConstant', [AnnotationNode], argBlock)
    }


    /**
     * Creates a PostfixExpression.
     */

    private void postfix(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(PostfixExpression, 'postfix', [Expression, Token], argBlock)
    }


    /**
     * Creates a FieldExpression.
     */

    private void field(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(FieldExpression, 'field', [FieldNode], argBlock)
    }


    /**
     * Creates a MapExpression.
     */

    private void map(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeFromList(MapExpression, argBlock)
    }


    /**
     * Creates a TupleExpression.
     */

    private void tuple(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeFromList(TupleExpression, argBlock)
    }


    /**
     * Creates a MapEntryExpression.
     */

    private void mapEntry(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(MapEntryExpression, 'mapEntry', [Expression, Expression], argBlock)
    }


    /**
     * Creates a gString.
     */

    private void gString(String verbatimText, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeWithStringParameter(GStringExpression, 'gString', [String, List, List], argBlock, verbatimText)
    }


    /**
     * Creates a methodPointer.
     */

    private void methodPointer(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(MethodPointerExpression, 'methodPointer', [Expression, Expression], argBlock)
    }


    /**
     * Creates a property.
     */

    private void property(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(PropertyExpression, 'property', [Expression, Expression], argBlock)
    }


    /**
     * Creates a RangeExpression.
     */

    private void range(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(RangeExpression, 'range', [Expression, Expression, Boolean], argBlock)
    }


    /**
     * Creates EmptyStatement.
     */

    private void empty() {
        expression << EmptyStatement.INSTANCE
    }


    /**
     * Creates a label.
     */

    private void label(String label) {
        expression << label
    }


    /**
     * Creates an ImportNode.
     */

    private void importNode(Class target, String alias = null) {
        expression << new ImportNode(ClassHelper.make(target), alias)
    }


    /**
     * Creates a CatchStatement.
     */

    private void catchStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(CatchStatement, 'catchStatement', [Parameter, Statement], argBlock)
    }


    /**
     * Creates a ThrowStatement.
     */

    private void throwStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(ThrowStatement, 'throwStatement', [Expression], argBlock)
    }


    /**
     * Creates a SynchronizedStatement.
     */

    private void synchronizedStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(SynchronizedStatement, 'synchronizedStatement', [Expression, Statement], argBlock)
    }


    /**
     * Creates a ReturnStatement.
     */

    private void returnStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(ReturnStatement, 'returnStatement', [Expression], argBlock)
    }


    /**
     * Creates a TernaryExpression.
     */

    private void ternary(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(TernaryExpression, 'ternary', [BooleanExpression, Expression, Expression], argBlock)
    }


    /**
     * Creates an ElvisOperatorExpression.
     */

    private void elvisOperator(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(ElvisOperatorExpression, 'elvisOperator', [Expression, Expression], argBlock)
    }


    /**
     * Creates a BreakStatement.
     */

    private void breakStatement(String label = null) {
        if (label) {
            expression << new BreakStatement(label)
        } else {
            expression << new BreakStatement()
        }
    }


    /**
     * Creates a ContinueStatement.
     */

    private void continueStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock = null) {
        if (!argBlock) {
            expression << new ContinueStatement()
        } else {
            makeNode(ContinueStatement, 'continueStatement', [String], argBlock)
        }
    }


    /**
     * Create a CaseStatement.
     */

    private void caseStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(CaseStatement, 'caseStatement', [Expression, Statement], argBlock)
    }


    /**
     * Creates a BlockStatement.
     */

    private void defaultCase(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        block(argBlock) // same as arg block
    }


    /**
     * Creates a PrefixExpression.
     */

    private void prefix(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(PrefixExpression, 'prefix', [Token, Expression], argBlock)
    }


    /**
     * Creates a NotExpression.
     */

    private void not(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(NotExpression, 'not', [Expression], argBlock)
    }


    /**
     * Creates a DynamicVariable.
     */

    private void dynamicVariable(String variable, boolean isStatic = false) {
        expression << new DynamicVariable(variable, isStatic)
    }


    /**
     * Creates a ClassNode[].
     */

    private void exceptions(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeArrayOfNodes([] as ClassNode[], argBlock)
    }


    /**
     * Designates a list of AnnotationNodes.
     */

    private void annotations(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeListOfNodes(argBlock, "List<AnnotationNode>")
    }

    /**
     * Designates a list of ConstantExpressions.
     */

    private void strings(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeListOfNodes(argBlock, "List<ConstantExpression>")
    }

    /**
     * Designates a list of Expressions.
     */

    private void values(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeListOfNodes(argBlock, "List<Expression>")
    }


    /**
     * Creates a boolean value.
     */

    private void inclusive(boolean value) {
        expression << value
    }


    /**
     * Creates a ConstantExpression.
     */

    private void constant(Object value) {
        expression << new ConstantExpression(value)
    }


    /**
     * Creates an IfStatement.
     */

    private void ifStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(IfStatement, 'ifStatement', [BooleanExpression, Statement, Statement], argBlock)
    }


    /**
     * Creates a SpreadExpression.
     */

    private void spread(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(SpreadExpression, 'spread', [Expression], argBlock)
    }


    /**
     * Creates a SpreadMapExpression.
     */

    private void spreadMap(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(SpreadMapExpression, 'spreadMap', [Expression], argBlock)
    }


    /**
     * Creates a WhileStatement.
     */

    private void whileStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(WhileStatement, 'whileStatement', [BooleanExpression, Statement], argBlock)
    }


    /**
     * Create a ForStatement.
     */

    private void forStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(ForStatement, 'forStatement', [Parameter, Expression, Statement], argBlock)
    }


    /**
     * Creates a ClosureListExpression.
     */

    private void closureList(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeFromList(ClosureListExpression, argBlock)
    }


    /**
     * Creates a DeclarationExpression.
     */

    private void declaration(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(DeclarationExpression, 'declaration', [Expression, Token, Expression], argBlock)
    }


    /**
     * Creates a ListExpression.
     */

    private void list(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeFromList(ListExpression, argBlock)
    }


    /**
     * Creates a BitwiseNegationExpression.
     */

    private void bitwiseNegation(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(BitwiseNegationExpression, 'bitwiseNegation', [Expression], argBlock)
    }


    /**
     * Creates a ClosureExpression.
     */

    private void closure(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(ClosureExpression, 'closure', [Parameter[], Statement], argBlock)
    }


    /**
     * Creates a BooleanExpression.
     */

    private void booleanExpression(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(BooleanExpression, 'booleanExpression', [Expression], argBlock)
    }


    /**
     * Creates a BinaryExpression.
     */

    private void binary(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(BinaryExpression, 'binary', [Expression, Token, Expression], argBlock)
    }


    /**
     * Creates a UnaryPlusExpression.
     */

    private void unaryPlus(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(UnaryPlusExpression, 'unaryPlus', [Expression], argBlock)
    }


    /**
     * Creates a ClassExpression.
     */

    private void classExpression(Class type) {
        expression << new ClassExpression(ClassHelper.make(type))
    }


    /**
     * Creates a UnaryMinusExpression
     */

    private void unaryMinus(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(UnaryMinusExpression, 'unaryMinus', [Expression], argBlock)
    }


    /**
     * Creates an AttributeExpression.
     */

    private void attribute(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(AttributeExpression, 'attribute', [Expression, Expression], argBlock)
    }


    /**
     * Creates an ExpressionStatement.
     */

    private void expression(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNode(ExpressionStatement, 'expression', [Expression], argBlock)
    }


    /**
     * Creates a NamedArgumentListExpression.
     */

    private void namedArgumentList(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeNodeFromList(NamedArgumentListExpression, argBlock)
    }


    /**
     * Creates a ClassNode[].
     */

    private void interfaces(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeListOfNodes(argBlock, "List<ClassNode>")
    }


    /**
     * Creates a MixinNode[].
     */

    private void mixins(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeListOfNodes(argBlock, "List<MixinNode>")
    }


    /**
     * Creates a GenericsTypes[].
     */

    private void genericsTypes(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeListOfNodes(argBlock, "List<GenericsTypes>")
    }


    /**
     * Creates a ClassNode.
     */

    private void classNode(Class target) {
        expression << ClassHelper.make(target, false)
    }


    /**
     * Creates a Parameter[].
     */

    private void parameters(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeArrayOfNodes([] as Parameter[], argBlock)
    }


    /**
     * Creates a BlockStatement.
     */

    private void block(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("BlockStatement", argBlock) {
            return new BlockStatement(new ArrayList(expression), new VariableScope())
        }
    }


    /**
     * Creates a Parameter.
     */

    private void parameter(Map<String, Class> args, @DelegatesTo(AstSpecificationCompiler) Closure argBlock = null) {
        if (!args) throw new IllegalArgumentException()
        if (args.size() > 1) throw new IllegalArgumentException()

        //todo: add better error handling?
        if (argBlock) {
            args.each {name, type ->
                captureAndCreateNode("Parameter", argBlock) {
                    new Parameter(ClassHelper.make(type), name, expression[0])
                }
            }
        } else {
            args.each {name, type ->
                expression << (new Parameter(ClassHelper.make(type), name))
            }
        }
    }


    /**
     * Creates an ArrayExpression.
     */

    private void array(Class type, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("ArrayExpression", argBlock) {
            new ArrayExpression(ClassHelper.make(type), new ArrayList(expression))
        }
    }



    /**
     * Creates a GenericsType.
     */

    private void genericsType(Class type, @DelegatesTo(AstSpecificationCompiler) Closure argBlock = null) {
        if (argBlock) {
            captureAndCreateNode("GenericsType", argBlock) {
                new GenericsType(ClassHelper.make(type), expression[0] as ClassNode[], expression[1])
            }
        } else {
            expression << new GenericsType(ClassHelper.make(type))
        }
    }

    /**
     * Creates a list of ClassNodes.
     */
    private void upperBound(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        makeListOfNodes(argBlock, 'List<ClassNode>')
    }

    /**
     * Creates a list of ClassNodes. 
     */
    private void lowerBound(Class target) {
        expression << ClassHelper.make(target)
    }

    /**
     * Creates a 2 element list of name and Annotation. Used with Annotation Members.
     */

    private void member(String name, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("Annotation Member", argBlock) {
            [name, expression[0]]
        }
    }


    /**
     * Creates an ArgumentListExpression.
     */

    private void argumentList(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        if (!argBlock) {
            expression << new ArgumentListExpression()
        } else {
            makeNodeFromList(ArgumentListExpression, argBlock)
        }
    }


    /**
     * Creates an AnnotationNode.
     */

    private void annotation(Class target, @DelegatesTo(AstSpecificationCompiler) Closure argBlock = null) {
        if (argBlock) {
            //todo: add better error handling
            captureAndCreateNode("ArgumentListExpression", argBlock) {
                def node = new AnnotationNode(ClassHelper.make(target))
                expression?.each {
                    node.addMember(it[0], it[1])
                }
                node
            }
        } else {
            expression << new AnnotationNode(ClassHelper.make(target))
        }
    }


    /**
     * Creates a MixinNode.
     */

    private void mixin(String name, int modifiers, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("AttributeExpression", argBlock) {
            if (expression.size() > 1) {
                new MixinNode(name, modifiers, expression[0], new ArrayList(expression[1]) as ClassNode[])
            } else {
                new MixinNode(name, modifiers, expression[0])
            }
        }
    }



    /**
     * Creates a ClassNode
     */

    private void classNode(String name, int modifiers, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("ClassNode", argBlock) {
            def result = new ClassNode(name, modifiers,
                    expression[0],
                    new ArrayList(expression[1]) as ClassNode[],
                    new ArrayList(expression[2]) as MixinNode[]
            )
            if (expression[3]) {
                result.setGenericsTypes(new ArrayList(expression[3]) as GenericsType[])
            }
            result
        }
    }


    /**
     * Creates an AssertStatement.
     */

    private void assertStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("AssertStatement", argBlock) {
            if (expression.size() < 2) {
                new AssertStatement(
                        * enforceConstraints('assertStatement', [BooleanExpression])
                )
            } else {
                new AssertStatement(
                        * enforceConstraints('assertStatement', [BooleanExpression, Expression])
                )
            }
        }
    }


    /**
     * Creates a TryCatchStatement.
     */

    private void tryCatch(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("TryCatchStatement", argBlock) {
            def result = new TryCatchStatement(expression[0], expression[1])
            def catchStatements = expression.tail().tail()
            catchStatements.each {statement -> result.addCatch(statement) }
            return result
        }
    }


    /**
     * Creates a VariableExpression.
     */

    private void variable(String variable) {
        expression << new VariableExpression(variable)
    }


    /**
     * Creates a MethodNode.
     */

    private void method(String name, int modifiers, Class returnType, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("MethodNode", argBlock) {
            //todo: enforce contract
            def result = new MethodNode(
                    name, modifiers, ClassHelper.make(returnType), expression[0], expression[1], expression[2]
            )
            if (expression[3]) {
                def annotations = expression[3]
                result.addAnnotations(new ArrayList(annotations))
            }
            result
        }
    }


    /**
     * Creates a token.
     */

    private void token(String value) {
        if (value == null) throw new IllegalArgumentException("Null: value")

        def tokenID = Types.lookupKeyword(value)
        if (tokenID == Types.UNKNOWN) {
            tokenID = Types.lookupSymbol(value)
        }
        if (tokenID == Types.UNKNOWN) throw new IllegalArgumentException("could not find token for $value")

        expression << new Token(tokenID, value, -1, -1)
    }


    /**
     * Creates a RangeExpression.
     */

    private void range(Range range) {
        if (range == null) throw new IllegalArgumentException('Null: range')
        expression << new RangeExpression(
                new ConstantExpression(range.getFrom()),
                new ConstantExpression(range.getTo()),
                true)   //default is inclusive
    }


    /**
     * Creates a SwitchStatement.
     */

    private void switchStatement(@DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("SwitchStatement", argBlock) {
            def switchExpression = expression.head()
            def caseStatements = expression.tail().tail()
            def defaultExpression = expression.tail().head()
            new SwitchStatement(switchExpression, caseStatements, defaultExpression)
        }
    }


    /**
     * Creates a mapEntry.
     */

    private void mapEntry(Map map) {
        map.entrySet().each {
            expression << new MapEntryExpression(
                    new ConstantExpression(it.key),
                    new ConstantExpression(it.value))
        }
    }

    //
    // todo: these methods can still be reduced smaller
    //


    /**
     * Creates a FieldNode.
     */

    private void fieldNode(String name, int modifiers, Class type, Class owner, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("FieldNode", argBlock) {
            expression.add(0, ClassHelper.make(owner))
            expression.add(0, ClassHelper.make(type))
            expression.add(0, modifiers)
            expression.add(0, name)
            new FieldNode(
                    * enforceConstraints('fieldNode', [String, Integer, ClassNode, ClassNode, Expression]))
        }
    }



    /**
     * Creates a property.
     */

    private void innerClass(String name, int modifiers, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("InnerClassNode", argBlock) {
            //todo: enforce contract
            new InnerClassNode(
                    expression[0],
                    name,
                    modifiers,
                    expression[1],
                    new ArrayList(expression[2]) as ClassNode[],
                    new ArrayList(expression[3]) as MixinNode[])
        }
    }


    /**
     * Creates a PropertyNode.
     */

    private void propertyNode(String name, int modifiers, Class type, Class owner, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        //todo: improve error handling?
        captureAndCreateNode("PropertyNode", argBlock) {
            new PropertyNode(name, modifiers, ClassHelper.make(type), ClassHelper.make(owner),
                    expression[0],  // initial value
                    expression[1],  // getter block
                    expression[2])  // setter block
        }
    }



    /**
     * Creates a StaticMethodCallExpression.
     */

    private void staticMethodCall(Class target, String name, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("StaticMethodCallExpression", argBlock) {
            expression.add(0, name)
            expression.add(0, ClassHelper.make(target))
            new StaticMethodCallExpression(
                    * enforceConstraints('staticMethodCall', [ClassNode, String, Expression])
            )
        }
    }


    /**
     * Creates a StaticMethodCallExpression.
     */

    private void staticMethodCall(MethodClosure target, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("StaticMethodCallExpression", argBlock) {
            expression.add(0, target.method)
            expression.add(0, ClassHelper.makeWithoutCaching(target.owner.class, false))
            new StaticMethodCallExpression(
                    * enforceConstraints('staticMethodCall', [ClassNode, String, Expression])
            )
        }
    }


    /**
     * Creates a ConstructorNode.
     */

    private void constructor(int modifiers, @DelegatesTo(AstSpecificationCompiler) Closure argBlock) {
        captureAndCreateNode("ConstructorNode", argBlock) {
            expression.add(0, modifiers)
            new ConstructorNode(
                    * enforceConstraints('constructor', [Integer, Parameter[], ClassNode[], Statement])
            )
        }
    }
}
