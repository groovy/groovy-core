/*
 * Copyright 2008-2013 the original author or authors.
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
package org.codehaus.groovy.transform

import groovy.transform.TimedInterrupt
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.objectweb.asm.Opcodes

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Allows "interrupt-safe" executions of scripts by adding timer expiration
 * checks on loops (for, while, do) and first statement of closures. By default,
 * also adds an interrupt check statement on the beginning of method calls.
 *
 * @author Cedric Champeau
 * @author Hamlet D'Arcy
 * @author Paul King
 * @see groovy.transform.ThreadInterrupt
 * @since 1.8.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class TimedInterruptibleASTTransformation implements ASTTransformation {

  private static final ClassNode MY_TYPE = ClassHelper.make(TimedInterrupt.class)
  private static final String CHECK_METHOD_START_MEMBER = 'checkOnMethodStart'
  private static final String APPLY_TO_ALL_CLASSES = 'applyToAllClasses'
  private static final String APPLY_TO_ALL_MEMBERS = 'applyToAllMembers'
  private static final String THROWN_EXCEPTION_TYPE = "thrown"

  public void visit(ASTNode[] nodes, SourceUnit source) {
    if (nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
      internalError("Expecting [AnnotationNode, AnnotatedNode] but got: ${Arrays.asList(nodes)}")
    }

    AnnotationNode node = nodes[0]
    AnnotatedNode annotatedNode = nodes[1]

    if (!MY_TYPE.equals(node.getClassNode())) {
      internalError("Transformation called from wrong annotation: $node.classNode.name")
    }

    def checkOnMethodStart = getConstantAnnotationParameter(node, CHECK_METHOD_START_MEMBER, Boolean.TYPE, true)
    def applyToAllMembers = getConstantAnnotationParameter(node, APPLY_TO_ALL_MEMBERS, Boolean.TYPE, true)
    def applyToAllClasses = applyToAllMembers ? getConstantAnnotationParameter(node, APPLY_TO_ALL_CLASSES, Boolean.TYPE, true) : false
    def maximum = getConstantAnnotationParameter(node, 'value', Long.TYPE, Long.MAX_VALUE)
    def thrown = AbstractInterruptibleASTTransformation.getClassAnnotationParameter(node, THROWN_EXCEPTION_TYPE, ClassHelper.make(TimeoutException))

    Expression unit = node.getMember('unit') ?: new PropertyExpression(new ClassExpression(ClassHelper.make(TimeUnit)), "SECONDS")

    // should be limited to the current SourceUnit or propagated to the whole CompilationUnit
    // DO NOT inline visitor creation in code below. It has state that must not persist between calls
    if (applyToAllClasses) {
      // guard every class and method defined in this script
      source.getAST()?.classes?.each { ClassNode it ->
        def visitor = new TimedInterruptionVisitor(source, checkOnMethodStart, applyToAllClasses, applyToAllMembers, maximum, unit, thrown, node.hashCode())
        visitor.visitClass(it)
      }
    } else if (annotatedNode instanceof ClassNode) {
      // only guard this particular class
      def visitor = new TimedInterruptionVisitor(source, checkOnMethodStart, applyToAllClasses, applyToAllMembers, maximum, unit, thrown, node.hashCode())
      visitor.visitClass annotatedNode
    } else if (!applyToAllMembers && annotatedNode instanceof MethodNode) {
      // only guard this particular method (plus initCode for class)
      def visitor = new TimedInterruptionVisitor(source, checkOnMethodStart, applyToAllClasses, applyToAllMembers, maximum, unit, thrown, node.hashCode())
      visitor.visitMethod annotatedNode
      visitor.visitClass annotatedNode.declaringClass
    } else if (!applyToAllMembers && annotatedNode instanceof FieldNode) {
      // only guard this particular field (plus initCode for class)
      def visitor = new TimedInterruptionVisitor(source, checkOnMethodStart, applyToAllClasses, applyToAllMembers, maximum, unit, thrown, node.hashCode())
      visitor.visitField annotatedNode
      visitor.visitClass annotatedNode.declaringClass
    } else if (!applyToAllMembers && annotatedNode instanceof DeclarationExpression) {
      // only guard this particular declaration (plus initCode for class)
      def visitor = new TimedInterruptionVisitor(source, checkOnMethodStart, applyToAllClasses, applyToAllMembers, maximum, unit, thrown, node.hashCode())
      visitor.visitDeclarationExpression annotatedNode
      visitor.visitClass annotatedNode.declaringClass
    } else {
      // only guard the script class
      source.getAST()?.classes?.each { ClassNode it ->
        if (it.isScript()) {
          def visitor = new TimedInterruptionVisitor(source, checkOnMethodStart, applyToAllClasses, applyToAllMembers, maximum, unit, thrown, node.hashCode())
          visitor.visitClass(it)
        }
      }
    }
  }

  static def getConstantAnnotationParameter(AnnotationNode node, String parameterName, Class type, defaultValue) {
    def member = node.getMember(parameterName)
    if (member) {
      if (member instanceof ConstantExpression) {
        // TODO not sure this try offers value - testing Groovy annotation type handing - throw GroovyBugError or remove?
        try {
          return member.value.asType(type)
        } catch (ignore) {
          internalError("Expecting boolean value for ${parameterName} annotation parameter. Found $member")
        }
      } else {
        internalError("Expecting boolean value for ${parameterName} annotation parameter. Found $member")
      }
    }
    return defaultValue
  }

  private static void internalError(String message) {
    throw new RuntimeException("Internal error: $message")
  }

  private static class TimedInterruptionVisitor extends ClassCodeVisitorSupport {
    final private SourceUnit source
    final private boolean checkOnMethodStart
    final private boolean applyToAllClasses
    final private boolean applyToAllMembers
    private FieldNode expireTimeField = null
    private FieldNode startTimeField = null
    private final Expression unit
    private final maximum
    private final ClassNode thrown
    private final String basename

    TimedInterruptionVisitor(source, checkOnMethodStart, applyToAllClasses, applyToAllMembers, maximum, unit, thrown, hash) {
      this.source = source
      this.checkOnMethodStart = checkOnMethodStart
      this.applyToAllClasses = applyToAllClasses
      this.applyToAllMembers = applyToAllMembers
      this.unit = unit
      this.maximum = maximum
      this.thrown = thrown
      this.basename = 'timedInterrupt' + hash
    }

    /**
     * @return Returns the interruption check statement.
     */
    final createInterruptStatement() {
      new IfStatement(
          new BooleanExpression(
              new BinaryExpression(
                  new PropertyExpression(new VariableExpression("this"), basename + '$expireTime'),
                  new Token(Types.COMPARE_LESS_THAN, '<', -1, -1),
                  new StaticMethodCallExpression(ClassHelper.make(System), 'nanoTime', ArgumentListExpression.EMPTY_ARGUMENTS)
              )
          ),
          new ThrowStatement(
              new ConstructorCallExpression(thrown,
                  new ArgumentListExpression(
                      new BinaryExpression(
                          new ConstantExpression('Execution timed out after ' + maximum + ' units. Start time: '),
                          new Token(Types.PLUS, '+', -1, -1),
                          new PropertyExpression(new VariableExpression("this"), basename + '$startTime'),
                      )

                  )
              )
          ),
          EmptyStatement.INSTANCE
      )
    }

    /**
     * Takes a statement and wraps it into a block statement which first element is the interruption check statement.
     * @param statement the statement to be wrapped
     * @return a {@link BlockStatement block statement}    which first element is for checking interruption, and the
     * second one the statement to be wrapped.
     */
    private wrapBlock(statement) {
      def stmt = new BlockStatement();
      stmt.addStatement(createInterruptStatement());
      stmt.addStatement(statement);
      stmt
    }

    @Override
    void visitClass(ClassNode node) {
      if (node.getDeclaredField(basename + '$expireTime')) {
        return
      }
      expireTimeField = node.addField(basename + '$expireTime',
          Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE,
          ClassHelper.long_TYPE,
          new BinaryExpression(
              new StaticMethodCallExpression(ClassHelper.make(System), 'nanoTime', ArgumentListExpression.EMPTY_ARGUMENTS),
              new Token(Types.PLUS, '+', -1, -1),
              new MethodCallExpression(
                  new PropertyExpression(new ClassExpression(ClassHelper.make(TimeUnit)), 'NANOSECONDS'),
                  'convert',
                  new ArgumentListExpression(new ConstantExpression(maximum, true), unit)
              )
          )
      );
      expireTimeField.synthetic = true
      startTimeField = node.addField(basename + '$startTime',
          Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE,
          ClassHelper.make(Date),
          new ConstructorCallExpression(ClassHelper.make(Date), ArgumentListExpression.EMPTY_ARGUMENTS)
      )
      startTimeField.synthetic = true

      // force these fields to be initialized first
      node.fields.remove(expireTimeField)
      node.fields.remove(startTimeField)
      node.fields.add(0, startTimeField)
      node.fields.add(0, expireTimeField)
      if (applyToAllMembers) {
        super.visitClass node
      }
    }

    @Override
    void visitClosureExpression(ClosureExpression closureExpr) {
      def code = closureExpr.code
      if (code instanceof BlockStatement) {
        code.statements.add(0, createInterruptStatement())
      } else {
        closureExpr.code = wrapBlock(code)
      }
      super.visitClosureExpression closureExpr
    }

    @Override
    void visitField(FieldNode node) {
      if (!node.isStatic() && !node.isSynthetic()) {
        super.visitField node
      }
    }

    @Override
    void visitProperty(PropertyNode node) {
      if (!node.isStatic() && !node.isSynthetic()) {
        super.visitProperty node
      }
    }

    /**
     * Shortcut method which avoids duplicating code for every type of loop.
     * Actually wraps the loopBlock of different types of loop statements.
     */
    private visitLoop(loopStatement) {
      def statement = loopStatement.loopBlock
      loopStatement.loopBlock = wrapBlock(statement)
    }

    @Override
    void visitForLoop(ForStatement forStatement) {
      visitLoop(forStatement)
      super.visitForLoop(forStatement)
    }

    @Override
    void visitDoWhileLoop(final DoWhileStatement doWhileStatement) {
      visitLoop(doWhileStatement)
      super.visitDoWhileLoop(doWhileStatement)
    }

    @Override
    void visitWhileLoop(final WhileStatement whileStatement) {
      visitLoop(whileStatement)
      super.visitWhileLoop(whileStatement)
    }

    @Override
    void visitMethod(MethodNode node) {
      if (checkOnMethodStart && !node.isSynthetic() && !node.isStatic() && !node.isAbstract()) {
        def code = node.code
        node.code = wrapBlock(code);
      }
      if (!node.isSynthetic() && !node.isStatic()) {
        super.visitMethod(node)
      }
    }

    protected SourceUnit getSourceUnit() {
      return source;
    }
  }
}
