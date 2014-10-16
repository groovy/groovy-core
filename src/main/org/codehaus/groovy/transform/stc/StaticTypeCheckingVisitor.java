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
package org.codehaus.groovy.transform.stc;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.IntRange;
import groovy.lang.Range;
import groovy.transform.TypeChecked;
import groovy.transform.TypeCheckingMode;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.ClosureSignatureHint;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.ast.tools.WideningCategories;
import org.codehaus.groovy.classgen.ReturnAdder;
import org.codehaus.groovy.classgen.asm.InvocationWriter;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.StaticTypesTransformation;
import org.codehaus.groovy.util.ListHashMap;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.codehaus.groovy.ast.ClassHelper.*;
import static org.codehaus.groovy.ast.tools.WideningCategories.LowestUpperBoundClassNode;
import static org.codehaus.groovy.ast.tools.WideningCategories.isBigDecCategory;
import static org.codehaus.groovy.ast.tools.WideningCategories.isBigIntCategory;
import static org.codehaus.groovy.ast.tools.WideningCategories.isDouble;
import static org.codehaus.groovy.ast.tools.WideningCategories.isDoubleCategory;
import static org.codehaus.groovy.ast.tools.WideningCategories.isFloat;
import static org.codehaus.groovy.ast.tools.WideningCategories.isFloatingCategory;
import static org.codehaus.groovy.ast.tools.WideningCategories.isIntCategory;
import static org.codehaus.groovy.ast.tools.WideningCategories.isLongCategory;
import static org.codehaus.groovy.ast.tools.WideningCategories.isNumberCategory;
import static org.codehaus.groovy.ast.tools.WideningCategories.lowestUpperBound;
import static org.codehaus.groovy.syntax.Types.ASSIGN;
import static org.codehaus.groovy.syntax.Types.ASSIGNMENT_OPERATOR;
import static org.codehaus.groovy.syntax.Types.COMPARE_EQUAL;
import static org.codehaus.groovy.syntax.Types.COMPARE_NOT_EQUAL;
import static org.codehaus.groovy.syntax.Types.COMPARE_TO;
import static org.codehaus.groovy.syntax.Types.DIVIDE;
import static org.codehaus.groovy.syntax.Types.DIVIDE_EQUAL;
import static org.codehaus.groovy.syntax.Types.EQUAL;
import static org.codehaus.groovy.syntax.Types.FIND_REGEX;
import static org.codehaus.groovy.syntax.Types.KEYWORD_IN;
import static org.codehaus.groovy.syntax.Types.KEYWORD_INSTANCEOF;
import static org.codehaus.groovy.syntax.Types.LEFT_SQUARE_BRACKET;
import static org.codehaus.groovy.syntax.Types.MINUS_MINUS;
import static org.codehaus.groovy.syntax.Types.MOD;
import static org.codehaus.groovy.syntax.Types.MOD_EQUAL;
import static org.codehaus.groovy.syntax.Types.PLUS_PLUS;
import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.*;

/**
 * The main class code visitor responsible for static type checking. It will perform various inspections like checking
 * assignment types, type inference, ... Eventually, class nodes may be annotated with inferred type information.
 *
 * @author Cedric Champeau
 * @author Jochen Theodorou
 */
public class StaticTypeCheckingVisitor extends ClassCodeVisitorSupport {

    private static final boolean DEBUG_GENERATED_CODE = Boolean.valueOf(System.getProperty("groovy.stc.debug", "false"));

    protected static final Object ERROR_COLLECTOR = ErrorCollector.class;
    protected static final ClassNode ITERABLE_TYPE = ClassHelper.make(Iterable.class);
    protected static final List<MethodNode> EMPTY_METHODNODE_LIST = Collections.emptyList();
    protected static final ClassNode TYPECHECKED_CLASSNODE = ClassHelper.make(TypeChecked.class);
    protected static final ClassNode[] TYPECHECKING_ANNOTATIONS = new ClassNode[]{TYPECHECKED_CLASSNODE};
    protected static final ClassNode TYPECHECKING_INFO_NODE = ClassHelper.make(TypeChecked.TypeCheckingInfo.class);
    protected static final ClassNode DGM_CLASSNODE = ClassHelper.make(DefaultGroovyMethods.class);
    protected static final int CURRENT_SIGNATURE_PROTOCOL_VERSION = 1;
    protected static final Expression CURRENT_SIGNATURE_PROTOCOL = new ConstantExpression(CURRENT_SIGNATURE_PROTOCOL_VERSION, true);
    protected static final MethodNode GET_DELEGATE = CLOSURE_TYPE.getGetterMethod("getDelegate");
    protected static final MethodNode GET_OWNER = CLOSURE_TYPE.getGetterMethod("getOwner");
    protected static final MethodNode GET_THISOBJECT = CLOSURE_TYPE.getGetterMethod("getThisObject");
    protected static final ClassNode DELEGATES_TO = ClassHelper.make(DelegatesTo.class);
    protected static final ClassNode DELEGATES_TO_TARGET = ClassHelper.make(DelegatesTo.Target.class);
    protected static final ClassNode LINKEDHASHMAP_CLASSNODE = make(LinkedHashMap.class);
    protected static final ClassNode CLOSUREPARAMS_CLASSNODE = make(ClosureParams.class);



    public static final Statement GENERATED_EMPTY_STATEMENT = new EmptyStatement();

    public static final MethodNode CLOSURE_CALL_NO_ARG;
    public static final MethodNode CLOSURE_CALL_ONE_ARG;
    public static final MethodNode CLOSURE_CALL_VARGS;

    static {
        // Cache closure call methods
        CLOSURE_CALL_NO_ARG = CLOSURE_TYPE.getDeclaredMethod("call", Parameter.EMPTY_ARRAY);
        CLOSURE_CALL_ONE_ARG = CLOSURE_TYPE.getDeclaredMethod("call", new Parameter[]{
                new Parameter(OBJECT_TYPE, "arg")
        });
        CLOSURE_CALL_VARGS = CLOSURE_TYPE.getDeclaredMethod("call", new Parameter[]{
                new Parameter(OBJECT_TYPE.makeArray(), "args")
        });
    }

    protected final ReturnAdder.ReturnStatementListener returnListener = new ReturnAdder.ReturnStatementListener() {
        public void returnStatementAdded(final ReturnStatement returnStatement) {
            ClassNode returnType = checkReturnType(returnStatement);
            if (returnStatement.getExpression().equals(ConstantExpression.NULL)) return;
            if (typeCheckingContext.getEnclosingClosure()!=null) {
                addClosureReturnType(getType(returnStatement.getExpression()));
            } else if (typeCheckingContext.getEnclosingMethod() != null) {
            } else {
                throw new GroovyBugError("Unexpected return statement at "
                        + returnStatement.getLineNumber()+":"+returnStatement.getColumnNumber()
                        + " "+returnStatement.getText());
            }
        }
    };

    protected final ReturnAdder returnAdder = new ReturnAdder(returnListener);

    protected TypeCheckingContext typeCheckingContext;
    protected DefaultTypeCheckingExtension extension;

    public StaticTypeCheckingVisitor(SourceUnit source, ClassNode cn) {
        this.typeCheckingContext = new TypeCheckingContext(this);
        this.extension = createDefaultTypeCheckingExtension();
        this.typeCheckingContext.source = source;
        this.typeCheckingContext.pushEnclosingClassNode(cn);
        this.typeCheckingContext.pushErrorCollector(source.getErrorCollector());
        this.typeCheckingContext.pushTemporaryTypeInfo();
    }

    private DefaultTypeCheckingExtension createDefaultTypeCheckingExtension() {
        DefaultTypeCheckingExtension ext = new DefaultTypeCheckingExtension(this);
        ext.addHandler(new TraitTypeCheckingExtension(this));
        return ext;
    }

    //        @Override
    protected SourceUnit getSourceUnit() {
        return typeCheckingContext.source;
    }

    public void initialize() {
        extension.setup();
    }

    /**
     * Returns the current type checking context. The context is used internally by the type
     * checker during type checking to store various state data.
     *
     * @return the type checking context
     */
    public TypeCheckingContext getTypeCheckingContext() {
        return typeCheckingContext;
    }

    public void addTypeCheckingExtension(TypeCheckingExtension extension) {
        this.extension.addHandler(extension);
    }

    public void setCompilationUnit(CompilationUnit cu) {
        typeCheckingContext.setCompilationUnit(cu);
    }

    @Override
    public void visitClass(final ClassNode node) {
        if (shouldSkipClassNode(node)) return;
        if (extension.beforeVisitClass(node)) {
            extension.afterVisitClass(node);
            return;
        }
        Object type = node.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
        if (type != null) {
            // transformation has already been run on this class node
            // so we'll use a silent collector in order not to duplicate errors
            typeCheckingContext.pushErrorCollector();
        }
        typeCheckingContext.pushEnclosingClassNode(node);
        Set<MethodNode> oldVisitedMethod = typeCheckingContext.alreadyVisitedMethods;
        typeCheckingContext.alreadyVisitedMethods = new LinkedHashSet<MethodNode>();
        super.visitClass(node);
        Iterator<InnerClassNode> innerClasses = node.getInnerClasses();
        while (innerClasses.hasNext()) {
            InnerClassNode innerClassNode = innerClasses.next();
            visitClass(innerClassNode);
        }
        typeCheckingContext.alreadyVisitedMethods = oldVisitedMethod;
        node.putNodeMetaData(StaticTypesMarker.INFERRED_TYPE, node);
        // mark all methods as visited. We can't do this in visitMethod because the type checker
        // works in a two pass sequence and we don't want to skip the second pass
        for (MethodNode methodNode : node.getMethods()) {
            methodNode.putNodeMetaData(StaticTypeCheckingVisitor.class, Boolean.TRUE);
        }
        for (ConstructorNode constructorNode : node.getDeclaredConstructors()) {
            constructorNode.putNodeMetaData(StaticTypeCheckingVisitor.class, Boolean.TRUE);
        }
        extension.afterVisitClass(node);
    }

    protected boolean shouldSkipClassNode(final ClassNode node) {
        if (isSkipMode(node)) return true;
        return false;
    }

    /**
     * Returns the list of type checking annotations class nodes. Subclasses may override this method
     * in order to provide additional classes which must be looked up when checking if a method or
     * a class node should be skipped.
     * <p>
     * The default implementation returns {@link TypeChecked}.
     *
     * @return array of class nodes
     */
    protected ClassNode[] getTypeCheckingAnnotations() {
        return TYPECHECKING_ANNOTATIONS;
    }

    public boolean isSkipMode(final AnnotatedNode node) {
        if (node == null) return false;
        for (ClassNode tca : getTypeCheckingAnnotations()) {
            List<AnnotationNode> annotations = node.getAnnotations(tca);
            if (annotations != null) {
                for (AnnotationNode annotation : annotations) {
                    Expression value = annotation.getMember("value");
                    if (value != null) {
                        if (value instanceof ConstantExpression) {
                            ConstantExpression ce = (ConstantExpression) value;
                            if (TypeCheckingMode.SKIP.toString().equals(ce.getValue().toString())) return true;
                        } else if (value instanceof PropertyExpression) {
                            PropertyExpression pe = (PropertyExpression) value;
                            if (TypeCheckingMode.SKIP.toString().equals(pe.getPropertyAsString())) return true;
                        }
                    }
                }
            }
        }
        if (node instanceof MethodNode) {
            return isSkipMode(node.getDeclaringClass());
        }
        if (isSkippedInnerClass(node)) return true;
        return false;
    }

    /**
     * Test if a node is an inner class node, and if it is, then checks if the enclosing method is skipped.
     * @param node
     * @return true if the inner class node should be skipped
     */
    protected boolean isSkippedInnerClass(AnnotatedNode node) {
        if (!(node instanceof InnerClassNode)) return false;
        MethodNode enclosingMethod = ((InnerClassNode) node).getEnclosingMethod();
        return enclosingMethod != null && isSkipMode(enclosingMethod);
    }

    @Override
    public void visitClassExpression(final ClassExpression expression) {
        super.visitClassExpression(expression);
        ClassNode cn = (ClassNode) expression.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
        if (cn == null) {
            storeType(expression, getType(expression));
        }
    }

    @SuppressWarnings("unchecked")
    private void addPrivateFieldOrMethodAccess(ClassNode cn, StaticTypesMarker type, ASTNode accessedMember) {
        Set<ASTNode> set = (Set<ASTNode>) cn.getNodeMetaData(type);
        if (set==null) {
            set = new LinkedHashSet<ASTNode>();
            cn.putNodeMetaData(type, set);
        }
        set.add(accessedMember);
    }

    /**
     * Given a field node, checks if we are calling a private field from an inner class.
     */
    private void checkOrMarkPrivateAccess(FieldNode fn) {
        if (fn!=null && Modifier.isPrivate(fn.getModifiers()) &&
            (fn.getDeclaringClass() != typeCheckingContext.getEnclosingClassNode() || typeCheckingContext.getEnclosingClosure()!=null) &&
            fn.getDeclaringClass().getModule() == typeCheckingContext.getEnclosingClassNode().getModule()) {
            addPrivateFieldOrMethodAccess(fn.getDeclaringClass(), StaticTypesMarker.PV_FIELDS_ACCESS, fn);
        }
    }

    /**
     * Given a method node, checks if we are calling a private method from an inner class.
     */
    private void checkOrMarkPrivateAccess(MethodNode mn) {
        if (mn==null) {
            return;
        }
        ClassNode declaringClass = mn.getDeclaringClass();
        ClassNode enclosingClassNode = typeCheckingContext.getEnclosingClassNode();
        if (declaringClass != enclosingClassNode || typeCheckingContext.getEnclosingClosure() != null) {
            int mods = mn.getModifiers();
            boolean sameModule = declaringClass.getModule() == enclosingClassNode.getModule();
            String packageName = declaringClass.getPackageName();
            if (packageName==null) {
                packageName = "";
            }
            if ((Modifier.isPrivate(mods) && sameModule)
                    || (Modifier.isProtected(mods) && !packageName.equals(enclosingClassNode.getPackageName()))) {
                addPrivateFieldOrMethodAccess(sameModule? declaringClass : enclosingClassNode, StaticTypesMarker.PV_METHODS_ACCESS, mn);
            }
        }
    }

    /**
     * wrap type in Class<> if usingClass==true
     */
    private ClassNode makeType(ClassNode cn, boolean usingClass) {
        if (usingClass) {
            ClassNode clazzType = CLASS_Type.getPlainNodeReference();
            clazzType.setGenericsTypes(new GenericsType[] {new GenericsType(cn)});
            return clazzType;
        } else {
            return cn;
        }
    }

    private boolean storeTypeForThis(VariableExpression vexp) {
        if (vexp == VariableExpression.THIS_EXPRESSION) return true;
        if (!vexp.isThisExpression()) return false;
        ClassNode enclosingClassNode = typeCheckingContext.getEnclosingClassNode();
        storeType(vexp, makeType(enclosingClassNode, typeCheckingContext.isInStaticContext));
        return true;
    }

    private boolean storeTypeForSuper(VariableExpression vexp) {
        if (vexp == VariableExpression.SUPER_EXPRESSION) return true;
        if (!vexp.isSuperExpression()) return false;
        ClassNode superClassNode = typeCheckingContext.getEnclosingClassNode().getSuperClass();
        storeType(vexp, makeType(superClassNode, typeCheckingContext.isInStaticContext));
        return true;
    }

    @Override
    public void visitVariableExpression(VariableExpression vexp) {
        super.visitVariableExpression(vexp);

        if (storeTypeForThis(vexp)) return;
        if (storeTypeForSuper(vexp)) return;
        if (vexp.getAccessedVariable() instanceof PropertyNode) {
            // we must be careful, because the property node may be of a wrong type:
            // if a class contains a getter and a setter of different types or
            // overloaded setters, the type of the property node is arbitrary!
            if (tryVariableExpressionAsProperty(vexp, vexp.getName())) {
                BinaryExpression enclosingBinaryExpression = typeCheckingContext.getEnclosingBinaryExpression();
                if (enclosingBinaryExpression != null) {
                    Expression leftExpression = enclosingBinaryExpression.getLeftExpression();
                    Expression rightExpression = enclosingBinaryExpression.getRightExpression();
                    SetterInfo setterInfo = removeSetterInfo(leftExpression);
                    if (setterInfo != null) {
                        if (!ensureValidSetter(vexp, leftExpression, rightExpression, setterInfo)) {
                            return;
                        }

                    }
                }
            }
        }

        TypeCheckingContext.EnclosingClosure enclosingClosure = typeCheckingContext.getEnclosingClosure();
        if (enclosingClosure != null) {
            String name = vexp.getName();
            if (name.equals("owner") || name.equals("thisObject")) {
                storeType(vexp, typeCheckingContext.getEnclosingClassNode());
                return;
            } else if ("delegate".equals(name)) {
                DelegationMetadata md = getDelegationMetadata(enclosingClosure.getClosureExpression());
                ClassNode type = typeCheckingContext.getEnclosingClassNode();
                if (md!=null) type = md.getType();
                storeType(vexp, type);
                return;
            }
        }

        if (! (vexp.getAccessedVariable() instanceof DynamicVariable)) return;

        // a dynamic variable is either an undeclared variable
        // or a member of a class used in a 'with'
        DynamicVariable dyn = (DynamicVariable) vexp.getAccessedVariable();
        // first, we must check the 'with' context
        String dynName = dyn.getName();
        if (tryVariableExpressionAsProperty(vexp, dynName)) return;

        if (!extension.handleUnresolvedVariableExpression(vexp)) {
            addStaticTypeError("The variable [" + vexp.getName() + "] is undeclared.", vexp);
        }
    }

    private boolean tryVariableExpressionAsProperty(final VariableExpression vexp, final String dynName) {
        VariableExpression implicitThis = new VariableExpression("this");
        PropertyExpression pe = new PropertyExpression(implicitThis, dynName);
        pe.setImplicitThis(true);
        if (visitPropertyExpressionSilent(pe, vexp)) {
            ClassNode previousIt = vexp.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
            vexp.copyNodeMetaData(implicitThis);
            vexp.putNodeMetaData(StaticTypesMarker.INFERRED_TYPE, previousIt);
            storeType(vexp, getType(pe));
            Object val = pe.getNodeMetaData(StaticTypesMarker.READONLY_PROPERTY);
            if (val!=null) vexp.putNodeMetaData(StaticTypesMarker.READONLY_PROPERTY,val);
            val = pe.getNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER);
            if (val!=null) vexp.putNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER,val);
            return true;
        }
        return false;
    }

    private boolean visitPropertyExpressionSilent(PropertyExpression pe, Expression lhsPart) {
        return (existsProperty(pe, !isLHSOfEnclosingAssignment(lhsPart)));
    }

    @Override
    public void visitPropertyExpression(final PropertyExpression pexp) {
        if (visitPropertyExpressionSilent(pexp,pexp)) return;

        if (!extension.handleUnresolvedProperty(pexp)) {
            Expression objectExpression = pexp.getObjectExpression();
            addStaticTypeError("No such property: " + pexp.getPropertyAsString() +
                    " for class: " + findCurrentInstanceOfClass(objectExpression, getType(objectExpression)).toString(false), pexp);
        }
    }

    private boolean isLHSOfEnclosingAssignment(final Expression expression) {
        final BinaryExpression ec = typeCheckingContext.getEnclosingBinaryExpression();
        return ec != null && ec.getLeftExpression() == expression && isAssignment(ec.getOperation().getType());
    }

    @Override
    public void visitAttributeExpression(final AttributeExpression expression) {
        super.visitAttributeExpression(expression);
        if (!existsProperty(expression, true) && !extension.handleUnresolvedAttribute(expression)) {
            Expression objectExpression = expression.getObjectExpression();
            addStaticTypeError("No such property: " + expression.getPropertyAsString() +
                    " for class: " + findCurrentInstanceOfClass(objectExpression, objectExpression.getType()), expression);
        }
    }

    @Override
    public void visitRangeExpression(final RangeExpression expression) {
        super.visitRangeExpression(expression);
        ClassNode fromType = getWrapper(getType(expression.getFrom()));
        ClassNode toType = getWrapper(getType(expression.getTo()));
        if (Integer_TYPE.equals(fromType) && Integer_TYPE.equals(toType)) {
            storeType(expression, ClassHelper.make(IntRange.class));
        } else {
            ClassNode rangeType = ClassHelper.make(Range.class).getPlainNodeReference();
            rangeType.setGenericsTypes(new GenericsType[] { new GenericsType(WideningCategories.lowestUpperBound(fromType, toType))});
            storeType(expression,  rangeType);
        }
    }

    @Override
    public void visitBinaryExpression(BinaryExpression expression) {
        BinaryExpression enclosingBinaryExpression = typeCheckingContext.getEnclosingBinaryExpression();
        typeCheckingContext.pushEnclosingBinaryExpression(expression);
        try {
            final Expression leftExpression = expression.getLeftExpression();
            final Expression rightExpression = expression.getRightExpression();
            int op = expression.getOperation().getType();
            leftExpression.visit(this);
            SetterInfo setterInfo = removeSetterInfo(leftExpression);
            if (setterInfo != null) {
                if (ensureValidSetter(expression, leftExpression, rightExpression, setterInfo)) {
                    return;
                }

            } else {
                rightExpression.visit(this);
            }
            ClassNode lType = getType(leftExpression);
            ClassNode rType = getType(rightExpression);
            if (isNullConstant(rightExpression)) {
                if (!isPrimitiveType(lType))
                    rType = UNKNOWN_PARAMETER_TYPE; // primitive types should be ignored as they will result in another failure
            }
            BinaryExpression reversedBinaryExpression = new BinaryExpression(rightExpression, expression.getOperation(), leftExpression);
            ClassNode resultType = op==KEYWORD_IN
                    ?getResultType(rType,op,lType,reversedBinaryExpression)
                    :getResultType(lType, op, rType, expression);
            if (op==KEYWORD_IN) {
                // in case of the "in" operator, the receiver and the arguments are reversed
                // so we use the reversedExpression and get the target method from it
                storeTargetMethod(expression, (MethodNode) reversedBinaryExpression.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET));
            } else if (op == LEFT_SQUARE_BRACKET
                    && leftExpression instanceof VariableExpression
                    && leftExpression.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE)==null) {
                storeType(leftExpression, lType);
            }
            if (resultType == null) {
                resultType = lType;
            }

            // if left expression is a closure shared variable, a second pass should be done
            if (leftExpression instanceof VariableExpression) {
                VariableExpression leftVar = (VariableExpression) leftExpression;
                if (leftVar.isClosureSharedVariable()) {
                    // if left expression is a closure shared variable, we should check it twice
                    // see GROOVY-5874
                    typeCheckingContext.secondPassExpressions.add(new SecondPassExpression<Void>(expression));
                }
            }

            if (lType.isUsingGenerics() && missesGenericsTypes(resultType) && isAssignment(op)) {
                // unchecked assignment
                // examples:
                // List<A> list = new LinkedList()
                // List<A> list = []
                // Iterable<A> list = new LinkedList()

                // in that case, the inferred type of the binary expression is the type of the RHS
                // "completed" with generics type information available in the LHS
                ClassNode completedType = GenericsUtils.parameterizeType(lType, resultType.getPlainNodeReference());

                resultType = completedType;

            }
            if (isArrayOp(op) &&
                    enclosingBinaryExpression != null
                    && enclosingBinaryExpression.getLeftExpression() == expression
                    && isAssignment(enclosingBinaryExpression.getOperation().getType())
                    && !lType.isArray()) {
                // left hand side of an assignment : map['foo'] = ...
                Expression enclosingBE_rightExpr = enclosingBinaryExpression.getRightExpression();
                if (!(enclosingBE_rightExpr instanceof ClosureExpression)) {
                    enclosingBE_rightExpr.visit(this);
                }
                ClassNode[] arguments = {rType, getType(enclosingBE_rightExpr)};
                List<MethodNode> nodes = findMethod(lType.redirect(), "putAt", arguments);
                if (nodes.size() == 1) {
                    typeCheckMethodsWithGenericsOrFail(lType, arguments, nodes.get(0), enclosingBE_rightExpr);
                } else if (nodes.isEmpty()) {
                    addNoMatchingMethodError(lType, "putAt", arguments, enclosingBinaryExpression);
                }
            }
            boolean isEmptyDeclaration = expression instanceof DeclarationExpression && rightExpression instanceof EmptyExpression;
            if (!isEmptyDeclaration && isAssignment(op)) {
                if (rightExpression instanceof ConstructorCallExpression) {
                    inferDiamondType((ConstructorCallExpression) rightExpression, lType);
                }

                ClassNode originType = getOriginalDeclarationType(leftExpression);
                typeCheckAssignment(expression, leftExpression, originType, rightExpression, resultType);
                // if assignment succeeds but result type is not a subtype of original type, then we are in a special cast handling
                // and we must update the result type
                if (!implementsInterfaceOrIsSubclassOf(getWrapper(resultType), getWrapper(originType))) {
                    resultType = originType;
                } else if (lType.isUsingGenerics() && !lType.isEnum() && hasRHSIncompleteGenericTypeInfo(resultType)) {
                    // for example, LHS is List<ConcreteClass> and RHS is List<T> where T is a placeholder
                    resultType = lType;
                }

                // make sure we keep primitive types
                if (isPrimitiveType(originType) && resultType.equals(getWrapper(originType))) {
                    resultType = originType;
                }

                // if we are in an if/else branch, keep track of assignment
                if (typeCheckingContext.ifElseForWhileAssignmentTracker != null && leftExpression instanceof VariableExpression
                        && !isNullConstant(rightExpression)) {
                    Variable accessedVariable = ((VariableExpression) leftExpression).getAccessedVariable();
                    if (accessedVariable instanceof VariableExpression) {
                        VariableExpression var = (VariableExpression) accessedVariable;
                        List<ClassNode> types = typeCheckingContext.ifElseForWhileAssignmentTracker.get(var);
                        if (types == null) {
                            types = new LinkedList<ClassNode>();
                            ClassNode type = var.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                            types.add(type);
                            typeCheckingContext.ifElseForWhileAssignmentTracker.put(var, types);
                        }
                        types.add(resultType);
                    }
                }
                storeType(leftExpression, resultType);

                // if right expression is a ClosureExpression, store parameter type information
                if (leftExpression instanceof VariableExpression) {
                    if (rightExpression instanceof ClosureExpression) {
                        Parameter[] parameters = ((ClosureExpression) rightExpression).getParameters();
                        leftExpression.putNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS, parameters);
                    } else if (rightExpression instanceof VariableExpression &&
                            ((VariableExpression)rightExpression).getAccessedVariable() instanceof Expression &&
                            ((Expression)((VariableExpression)rightExpression).getAccessedVariable()).getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS)!=null) {
                        Variable targetVariable = findTargetVariable((VariableExpression)leftExpression);
                        if (targetVariable instanceof ASTNode) {
                            ((ASTNode)targetVariable).putNodeMetaData(
                                StaticTypesMarker.CLOSURE_ARGUMENTS,
                                ((Expression)((VariableExpression)rightExpression).getAccessedVariable()).getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS));
                        }
                    }
                }


            } else if (op == KEYWORD_INSTANCEOF) {
                pushInstanceOfTypeInfo(leftExpression, rightExpression);
            }
            if (!isEmptyDeclaration) {
                storeType(expression, resultType);
            }
        } finally {
            typeCheckingContext.popEnclosingBinaryExpression();
        }
    }

    /**
     * Given a binary expression corresponding to an assignment, will check that the type of the RHS matches one
     * of the possible setters and if not, throw a type checking error.
     * @param expression the assignment expression
     * @param leftExpression left expression of the assignment
     * @param rightExpression right expression of the assignment
     * @param setterInfo possible setters
     * @return true if type checking passed
     */
    private boolean ensureValidSetter(final Expression expression, final Expression leftExpression, final Expression rightExpression, final SetterInfo setterInfo) {
        // for expressions like foo = { ... }
        // we know that the RHS type is a closure
        // but we must check if the binary expression is an assignment
        // because we need to check if a setter uses @DelegatesTo
        VariableExpression ve = new VariableExpression("%", setterInfo.receiverType);
        MethodCallExpression call = new MethodCallExpression(
                ve,
                setterInfo.name,
                rightExpression
        );
        call.setImplicitThis(false);
        visitMethodCallExpression(call);
        MethodNode directSetterCandidate = call.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
        if (directSetterCandidate != null) {
            for (MethodNode setter : setterInfo.setters) {
                if (setter == directSetterCandidate) {
                    leftExpression.putNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, directSetterCandidate);
                    storeType(leftExpression, getType(rightExpression));
                    break;
                }
            }
        } else {
            ClassNode firstSetterType = setterInfo.setters.iterator().next().getParameters()[0].getOriginType();
            addAssignmentError(firstSetterType, getType(rightExpression), expression);
            return true;
        }
        return false;
    }

    protected ClassNode getOriginalDeclarationType(Expression lhs) {
        if (lhs instanceof VariableExpression) {
            Variable var = findTargetVariable((VariableExpression) lhs);
            if (var instanceof PropertyNode) {
                // Do NOT trust the type of the property node!
                return getType(lhs);
            }
            if (var instanceof DynamicVariable) return getType(lhs);
            return var.getOriginType();
        }
        if (lhs instanceof FieldExpression) {
            return ((FieldExpression) lhs).getField().getOriginType();
        }
        return getType(lhs);
    }

    protected void inferDiamondType(final ConstructorCallExpression cce, final ClassNode lType) {
        // check if constructor call expression makes use of the diamond operator
        ClassNode node = cce.getType();
        if (node.isUsingGenerics() && node instanceof InnerClassNode && ((InnerClassNode) node).isAnonymous()) {
            node = node.getUnresolvedSuperClass(false);
            if ((node.getGenericsTypes() == null || node.getGenericsTypes().length == 0) && lType.isUsingGenerics()) {
                // InterfaceA<Foo> obj = new InterfaceA<>() { ... }
                // InterfaceA<Foo> obj = new ClassA<>() { ... }
                // ClassA<Foo> obj = new ClassA<>() { ... }
                addStaticTypeError("Cannot use diamond <> with anonymous inner classes", cce);
            }
        } else if (node.isUsingGenerics() && node.getGenericsTypes() != null && node.getGenericsTypes().length == 0) {
            ArgumentListExpression argumentListExpression = InvocationWriter.makeArgumentList(cce.getArguments());
            if (argumentListExpression.getExpressions().isEmpty()) {
                GenericsType[] genericsTypes = lType.getGenericsTypes();
                if (lType.getGenericsTypes()==null) {
                    // case of def foo = new HashMap<>()
                    genericsTypes = node.redirect().getGenericsTypes();
                }
                GenericsType[] copy = new GenericsType[genericsTypes.length];
                for (int i = 0; i < genericsTypes.length; i++) {
                    GenericsType genericsType = genericsTypes[i];
                    copy[i] = new GenericsType(
                            wrapTypeIfNecessary(genericsType.getType()),
                            genericsType.getUpperBounds(),
                            genericsType.getLowerBound()
                    );
                }
                node.setGenericsTypes(copy);
            } else {
                ClassNode type = getType(argumentListExpression.getExpression(0));
                if (type.isUsingGenerics()) {
                    GenericsType[] genericsTypes = type.getGenericsTypes();
                    GenericsType[] copy = new GenericsType[genericsTypes.length];
                    for (int i = 0; i < genericsTypes.length; i++) {
                        GenericsType genericsType = genericsTypes[i];
                        copy[i] = new GenericsType(
                                wrapTypeIfNecessary(genericsType.getType()),
                                genericsType.getUpperBounds(),
                                genericsType.getLowerBound()
                        );
                    }
                    node.setGenericsTypes(copy);
                }
            }
            // store inferred type on CCE
            storeType(cce, node);
        }
    }

    /**
     * Stores information about types when [objectOfInstanceof instanceof typeExpression] is visited
     *
     * @param objectOfInstanceOf the expression which must be checked against instanceof
     * @param typeExpression     the expression which represents the target type
     */
    protected void pushInstanceOfTypeInfo(final Expression objectOfInstanceOf, final Expression typeExpression) {
        final Map<Object, List<ClassNode>> tempo = typeCheckingContext.temporaryIfBranchTypeInformation.peek();
        Object key = extractTemporaryTypeInfoKey(objectOfInstanceOf);
        List<ClassNode> potentialTypes = tempo.get(key);
        if (potentialTypes == null) {
            potentialTypes = new LinkedList<ClassNode>();
            tempo.put(key, potentialTypes);
        }
        potentialTypes.add(typeExpression.getType());
    }

    private boolean typeCheckMultipleAssignmentAndContinue(Expression leftExpression, Expression rightExpression) {
        // multiple assignment check
        if (!(leftExpression instanceof TupleExpression)) return true;

        if (!(rightExpression instanceof ListExpression)) {
            addStaticTypeError("Multiple assignments without list expressions on the right hand side are unsupported in static type checking mode", rightExpression);
            return false;
        }

        TupleExpression tuple = (TupleExpression) leftExpression;
        ListExpression list = (ListExpression) rightExpression;
        List<Expression> listExpressions = list.getExpressions();
        List<Expression> tupleExpressions = tuple.getExpressions();
        if (listExpressions.size() < tupleExpressions.size()) {
            addStaticTypeError("Incorrect number of values. Expected:" + tupleExpressions.size() + " Was:" + listExpressions.size(), list);
            return false;
        }
        for (int i = 0, tupleExpressionsSize = tupleExpressions.size(); i < tupleExpressionsSize; i++) {
            Expression tupleExpression = tupleExpressions.get(i);
            Expression listExpression = listExpressions.get(i);
            ClassNode elemType = getType(listExpression);
            ClassNode tupleType = getType(tupleExpression);
            if (!isAssignableTo(elemType, tupleType)) {
                addStaticTypeError("Cannot assign value of type " + elemType.toString(false) + " to variable of type " + tupleType.toString(false), rightExpression);
                return false; // avoids too many errors
            }
        }

        return true;
    }

    private ClassNode adjustTypeForSpreading(ClassNode inferredRightExpressionType, Expression leftExpression) {
        // imagine we have: list*.foo = 100
        // then the assignment must be checked against [100], not 100
        ClassNode wrappedRHS = inferredRightExpressionType;
        if (leftExpression instanceof PropertyExpression && ((PropertyExpression) leftExpression).isSpreadSafe()) {
            wrappedRHS = LIST_TYPE.getPlainNodeReference();
            wrappedRHS.setGenericsTypes(new GenericsType[]{
                    new GenericsType(getWrapper(inferredRightExpressionType))
            });
        }
        return wrappedRHS;
    }

    private boolean addedReadOnlyPropertyError(Expression expr) {
        // if expr is of READONLY_PROPERTY_RETURN type, then it means we are on a missing property
        if (expr.getNodeMetaData(StaticTypesMarker.READONLY_PROPERTY) == null) return false;
        String name = null;
        if (expr instanceof VariableExpression) {
            name = ((VariableExpression) expr).getName();
        } else {
            name = ((PropertyExpression) expr).getPropertyAsString();
        }
        addStaticTypeError("Cannot set read-only property: " + name, expr);
        return true;
    }

    private void addPrecisionErrors(ClassNode leftRedirect, ClassNode lhsType, ClassNode inferredrhsType, Expression rightExpression) {
        if (isNumberType(leftRedirect) && isNumberType(inferredrhsType)) {
            if (checkPossibleLooseOfPrecision(leftRedirect, inferredrhsType, rightExpression)) {
                addStaticTypeError("Possible loose of precision from " + inferredrhsType + " to " + leftRedirect, rightExpression);
                return;
            }
        }
        // if left type is array, we should check the right component types
        if (!lhsType.isArray()) return;
        ClassNode leftComponentType = lhsType.getComponentType();
        ClassNode rightRedirect = rightExpression.getType().redirect();
        if (rightRedirect.isArray()) {
            ClassNode rightComponentType = rightRedirect.getComponentType();
            if (!checkCompatibleAssignmentTypes(leftComponentType, rightComponentType)) {
                addStaticTypeError("Cannot assign value of type " + rightComponentType.toString(false) + " into array of type " + lhsType.toString(false), rightExpression);
            }
        } else if (rightExpression instanceof ListExpression) {
            for (Expression element : ((ListExpression) rightExpression).getExpressions()) {
                ClassNode rightComponentType = element.getType().redirect();
                if (!checkCompatibleAssignmentTypes(leftComponentType, rightComponentType)
                        && !(isNullConstant(element) && !isPrimitiveType(leftComponentType))) {
                    addStaticTypeError("Cannot assign value of type " + rightComponentType.toString(false) + " into array of type " + lhsType.toString(false), rightExpression);
                }
            }
        }
    }

    private void addListAssignmentConstructorErrors(
            ClassNode leftRedirect, ClassNode leftExpressionType,
            ClassNode inferredRightExpressionType, Expression rightExpression,
            Expression assignmentExpression)
    {
        // if left type is not a list but right type is a list, then we're in the case of a groovy
        // constructor type : Dimension d = [100,200]
        // In that case, more checks can be performed
        if (rightExpression instanceof ListExpression && !implementsInterfaceOrIsSubclassOf(LIST_TYPE, leftRedirect)) {
            ArgumentListExpression argList = new ArgumentListExpression(((ListExpression) rightExpression).getExpressions());
            ClassNode[] args = getArgumentTypes(argList);
            MethodNode methodNode = checkGroovyStyleConstructor(leftRedirect, args, assignmentExpression);
            if (methodNode!=null) {
                rightExpression.putNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, methodNode);
            }
        } else if (!implementsInterfaceOrIsSubclassOf(inferredRightExpressionType, leftRedirect)
                && implementsInterfaceOrIsSubclassOf(inferredRightExpressionType, LIST_TYPE)
                && !isWildcardLeftHandSide(leftExpressionType)) {
                if (!extension.handleIncompatibleAssignment(leftExpressionType, inferredRightExpressionType, assignmentExpression)) {
                    addAssignmentError(leftExpressionType, inferredRightExpressionType, assignmentExpression);
                }
        }
    }

    private void addMapAssignmentConstructorErrors(ClassNode leftRedirect, Expression leftExpression, Expression rightExpression) {
        // if left type is not a list but right type is a map, then we're in the case of a groovy
        // constructor type : A a = [x:2, y:3]
        // In this case, more checks can be performed
        if (!implementsInterfaceOrIsSubclassOf(leftRedirect, MAP_TYPE) && rightExpression instanceof MapExpression) {
            if (!(leftExpression instanceof VariableExpression) || !((VariableExpression) leftExpression).isDynamicTyped()) {
                ArgumentListExpression argList = new ArgumentListExpression(rightExpression);
                ClassNode[] args = getArgumentTypes(argList);
                checkGroovyStyleConstructor(leftRedirect, args, rightExpression);
                // perform additional type checking on arguments
                MapExpression mapExpression = (MapExpression) rightExpression;
                checkGroovyConstructorMap(leftExpression, leftRedirect, mapExpression);
            }
        }
    }

    private void checkTypeGenerics(ClassNode leftExpressionType, ClassNode wrappedRHS, Expression rightExpression) {
        // last, check generic type information to ensure that inferred types are compatible
        if (!leftExpressionType.isUsingGenerics()) return;
        // List<Foo> l = new List() is an example for incomplete generics type info
        // we assume arity related errors are already handled here.
        if (hasRHSIncompleteGenericTypeInfo(wrappedRHS)) return;

        GenericsType gt = GenericsUtils.buildWildcardType(leftExpressionType);
        if (    UNKNOWN_PARAMETER_TYPE.equals(wrappedRHS) ||
                gt.isCompatibleWith(wrappedRHS) ||
                isNullConstant(rightExpression)) return;

        addStaticTypeError("Incompatible generic argument types. Cannot assign "
                + wrappedRHS.toString(false)
                + " to: " + leftExpressionType.toString(false), rightExpression);
    }

    private boolean hasGStringStringError(ClassNode leftExpressionType, ClassNode wrappedRHS, Expression rightExpression) {
        if (isParameterizedWithString(leftExpressionType) && isParameterizedWithGStringOrGStringString(wrappedRHS)) {
            addStaticTypeError("You are trying to use a GString in place of a String in a type which explicitly declares accepting String. " +
                    "Make sure to call toString() on all GString values.", rightExpression);
            return true;
        }
        return false;
    }

    protected void typeCheckAssignment(
            final BinaryExpression assignmentExpression,
            final Expression leftExpression,
            final ClassNode leftExpressionType,
            final Expression rightExpression,
            final ClassNode inferredRightExpressionType)
    {

        if (!typeCheckMultipleAssignmentAndContinue(leftExpression, rightExpression)) return;

        //TODO: need errors for write-only too!
        if (addedReadOnlyPropertyError(leftExpression)) return;

        ClassNode leftRedirect = leftExpressionType.redirect();
        ClassNode wrappedRHS = adjustTypeForSpreading(inferredRightExpressionType, leftExpression);

        // check types are compatible for assignment
        boolean compatible = checkCompatibleAssignmentTypes(leftRedirect, wrappedRHS, rightExpression);


        if (!compatible) {
            if (!extension.handleIncompatibleAssignment(leftExpressionType, inferredRightExpressionType, assignmentExpression)) {
                addAssignmentError(leftExpressionType, inferredRightExpressionType, assignmentExpression.getRightExpression());
            }
        } else {
            addPrecisionErrors(leftRedirect, leftExpressionType, inferredRightExpressionType, rightExpression);
            addListAssignmentConstructorErrors(leftRedirect, leftExpressionType, inferredRightExpressionType, rightExpression, assignmentExpression);
            addMapAssignmentConstructorErrors(leftRedirect, leftExpression, rightExpression);
            if (hasGStringStringError(leftExpressionType, wrappedRHS, rightExpression)) return;
            checkTypeGenerics(leftExpressionType, wrappedRHS, rightExpression);
        }
    }

    protected void checkGroovyConstructorMap(final Expression receiver, final ClassNode receiverType, final MapExpression mapExpression) {
        for (MapEntryExpression entryExpression : mapExpression.getMapEntryExpressions()) {
            Expression keyExpr = entryExpression.getKeyExpression();
            if (!(keyExpr instanceof ConstantExpression)) {
                addStaticTypeError("Dynamic keys in map-style constructors are unsupported in static type checking", keyExpr);
            } else {
                AtomicReference<ClassNode> lookup = new AtomicReference<ClassNode>();
                PropertyExpression pexp = new PropertyExpression(new VariableExpression("_", receiverType), keyExpr.getText());
                boolean hasProperty = existsProperty(pexp, false, new PropertyLookupVisitor(lookup));
                if (!hasProperty) {
                    addStaticTypeError("No such property: " + keyExpr.getText() +
                            " for class: " + receiverType.getName(), receiver);
                } else {
                    ClassNode valueType = getType(entryExpression.getValueExpression());
                    ClassNode toBeAssignedTo = lookup.get();
                    if (!isAssignableTo(valueType, toBeAssignedTo)
                            && !extension.handleIncompatibleAssignment(toBeAssignedTo, valueType, entryExpression)) {
                        addAssignmentError(toBeAssignedTo, valueType, entryExpression);
                    }
                }
            }
        }
    }

    protected static boolean hasRHSIncompleteGenericTypeInfo(final ClassNode inferredRightExpressionType) {
        boolean replaceType = false;
        GenericsType[] genericsTypes = inferredRightExpressionType.getGenericsTypes();
        if (genericsTypes != null) {
            for (GenericsType genericsType : genericsTypes) {
                if (genericsType.isPlaceholder()) {
                    replaceType = true;
                    break;
                }
            }
        }
        return replaceType;
    }

    /**
     * Checks that a constructor style expression is valid regarding the number of arguments and the argument types.
     *
     * @param node      the class node for which we will try to find a matching constructor
     * @param arguments the constructor arguments
     * @deprecated use {@link #checkGroovyStyleConstructor(org.codehaus.groovy.ast.ClassNode, org.codehaus.groovy.ast.ClassNode[], org.codehaus.groovy.ast.ASTNode)} )}
     */
    @Deprecated
    protected void checkGroovyStyleConstructor(final ClassNode node, final ClassNode[] arguments) {
        checkGroovyStyleConstructor(node, arguments, typeCheckingContext.getEnclosingClassNode());
    }

    /**
     * Checks that a constructor style expression is valid regarding the number of arguments and the argument types.
     *
     * @param node      the class node for which we will try to find a matching constructor
     * @param arguments the constructor arguments
     */
    protected MethodNode checkGroovyStyleConstructor(final ClassNode node, final ClassNode[] arguments, final ASTNode source) {
        if (node.equals(ClassHelper.OBJECT_TYPE) || node.equals(ClassHelper.DYNAMIC_TYPE)) {
            // in that case, we are facing a list constructor assigned to a def or object
            return null;
        }
        List<ConstructorNode> constructors = node.getDeclaredConstructors();
        if (constructors.isEmpty() && arguments.length == 0) {
            return null;
        }
        List<MethodNode> constructorList = findMethod(node, "<init>", arguments);
        if (constructorList.isEmpty()) {
            if (isBeingCompiled(node) && arguments.length==1 && LINKEDHASHMAP_CLASSNODE.equals(arguments[0])) {
                // there will be a default hash map constructor added later
                ConstructorNode cn = new ConstructorNode(Opcodes.ACC_PUBLIC, new Parameter[]{
                        new Parameter(LINKEDHASHMAP_CLASSNODE, "args")
                }, ClassNode.EMPTY_ARRAY, EmptyStatement.INSTANCE);
                return cn;
            } else {
                addStaticTypeError("No matching constructor found: " + node + toMethodParametersString("<init>", arguments), source);
                return null;
            }
        } else if (constructorList.size()>1) {
            addStaticTypeError("Ambiguous constructor call " + node + toMethodParametersString("<init>", arguments), source);
            return null;
        }
        return constructorList.get(0);
    }

    /**
     * When instanceof checks are found in the code, we store temporary type information data in the {@link
     * TypeCheckingContext#temporaryIfBranchTypeInformation} table. This method computes the key which must be used to store this type
     * info.
     *
     * @param expression the expression for which to compute the key
     * @return a key to be used for {@link TypeCheckingContext#temporaryIfBranchTypeInformation}
     */
    protected Object extractTemporaryTypeInfoKey(final Expression expression) {
        return expression instanceof VariableExpression ? findTargetVariable((VariableExpression) expression) : expression.getText();
    }

    /**
     * A helper method which determines which receiver class should be used in error messages when a field or attribute
     * is not found. The returned type class depends on whether we have temporary type information availble (due to
     * instanceof checks) and whether there is a single candidate in that case.
     *
     * @param expr the expression for which an unknown field has been found
     * @param type the type of the expression (used as fallback type)
     * @return if temporary information is available and there's only one type, returns the temporary type class
     *         otherwise falls back to the provided type class.
     */
    protected ClassNode findCurrentInstanceOfClass(final Expression expr, final ClassNode type) {
        if (!typeCheckingContext.temporaryIfBranchTypeInformation.empty()) {
            List<ClassNode> nodes = getTemporaryTypesForExpression(expr);
            if (nodes != null && nodes.size() == 1) return nodes.get(0);
        }
        return type;
    }

    protected boolean existsProperty(final PropertyExpression pexp, final boolean checkForReadOnly) {
        return existsProperty(pexp, checkForReadOnly, null);
    }

    /**
     * Checks whether a property exists on the receiver, or on any of the possible receiver classes (found in the
     * temporary type information table)
     *
     * @param pexp             a property expression
     * @param readMode         if true, look for property read, else for property set
     * @param visitor          if not null, when the property node is found, visit it with the provided visitor
     * @return true if the property is defined in any of the possible receiver classes
     */
    protected boolean existsProperty(final PropertyExpression pexp, final boolean readMode, final ClassCodeVisitorSupport visitor) {
        super.visitPropertyExpression(pexp);

        String propertyName = pexp.getPropertyAsString();
        if (propertyName == null) return false;

        Expression objectExpression = pexp.getObjectExpression();
        final ClassNode objectExpressionType = getType(objectExpression);

        boolean staticOnlyAccess = isClassClassNodeWrappingConcreteType(objectExpressionType);
        if ("this".equals(propertyName) && staticOnlyAccess) {
            // Outer.this
            ClassNode outerNode = objectExpressionType.getGenericsTypes()[0].getType();
            ClassNode current = typeCheckingContext.getEnclosingClassNode();
            if (!current.isStaticClass() && current instanceof InnerClassNode) {
                InnerClassNode icn = (InnerClassNode) current;
                if (outerNode.equals(icn.getOuterClass())) {
                    storeType(pexp, outerNode);
                    return true;
                }
            }
        }

        if (objectExpressionType.isArray() && "length".equals(pexp.getPropertyAsString())) {
            storeType(pexp, int_TYPE);
            if (visitor != null) {
                PropertyNode node = new PropertyNode("length", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, int_TYPE, objectExpressionType, null, null, null);
                visitor.visitProperty(node);
            }
            return true;
        }

        boolean foundGetterOrSetter = false;
        List<Receiver<String>> receivers = new LinkedList<Receiver<String>>();
        List<Receiver<String>> owners = makeOwnerList(objectExpression);
        addReceivers(receivers, owners, pexp.isImplicitThis());

        String capName = MetaClassHelper.capitalize(propertyName);
        boolean isAttributeExpression = pexp instanceof AttributeExpression;
        HashSet<ClassNode> handledNodes = new HashSet<ClassNode>();
        for (Receiver<String> receiver : receivers) {
            ClassNode testClass = receiver.getType();
                LinkedList<ClassNode> queue = new LinkedList<ClassNode>();
                queue.add(testClass);
                while (!queue.isEmpty()) {
                    ClassNode current = queue.removeFirst();
                    if (handledNodes.contains(current)) continue;
                    handledNodes.add(current);
                    Set<ClassNode> allInterfaces = current.getAllInterfaces();
                    for (ClassNode intf : allInterfaces) {
                        //TODO: apply right generics here!
                        queue.add(GenericsUtils.parameterizeType(current, intf));
                    }

                    // in case of a lookup on Class we look for instance methods on Class
                    // as well, since in case of a static property access we have the class
                    // itself in the list of receivers already;
                    boolean staticOnly;
                    if (isClassClassNodeWrappingConcreteType(current)) {
                        staticOnly = false;
                    } else {
                        staticOnly = staticOnlyAccess;
                    }

                    FieldNode field = current.getDeclaredField(propertyName);
                    field  = allowStaticAccessToMember(field, staticOnly);
                    if (storeField(field, isAttributeExpression, pexp, current, visitor, receiver.getData())) return true;

                    PropertyNode propertyNode = current.getProperty(propertyName);
                    propertyNode = allowStaticAccessToMember(propertyNode, staticOnly);
                    if (storeProperty(propertyNode, pexp, current, visitor, receiver.getData())) return true;

                    boolean isThisExpression = objectExpression instanceof VariableExpression &&
                                                ((VariableExpression)objectExpression).isThisExpression();
                    if (storeField(field, isThisExpression, pexp, receiver.getType(), visitor, receiver.getData())) return true;

                    MethodNode getter = current.getGetterMethod("get" + capName);
                    getter = allowStaticAccessToMember(getter, staticOnly);
                    if (getter == null) getter = current.getGetterMethod("is" + capName);
                    getter = allowStaticAccessToMember(getter, staticOnly);
                    final String setterName = "set" + capName;
                    List<MethodNode> setters = findSetters(current, setterName, false);
                    setters = allowStaticAccessToMember(setters, staticOnly);

                    // TODO: remove this visit
                    // need to visit even if we only look for a setters for compatibility
                    if (visitor != null && getter!=null) visitor.visitMethod(getter);

                    if (readMode) {
                        if (getter!=null) {
                            ClassNode cn = inferReturnTypeGenerics(current, getter, ArgumentListExpression.EMPTY_ARGUMENTS);
                            storeInferredTypeForPropertyExpression(pexp, cn);
                            pexp.removeNodeMetaData(StaticTypesMarker.READONLY_PROPERTY);
                            String delegationData = receiver.getData();
                            if (delegationData!=null) pexp.putNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER, delegationData);
                            return true;
                        }
                    } else {
                        if (!setters.isEmpty()) {
                            if (visitor != null) {
                                if (field!=null) {
                                    visitor.visitField(field);
                                } else {
                                    for (MethodNode setter : setters) {
                                        ClassNode setterType = setter.getParameters()[0].getOriginType();
                                        FieldNode virtual = new FieldNode(propertyName, 0, setterType, current, EmptyExpression.INSTANCE);
                                        visitor.visitField(virtual);
                                    }
                                }
                            }
                            //TODO: apply generics on parameter[0]?
//                                storeType(pexp, setter.getParameters()[0].getType());
                            SetterInfo info = new SetterInfo(current, setterName, setters);
                            BinaryExpression enclosingBinaryExpression = typeCheckingContext.getEnclosingBinaryExpression();
                            if (enclosingBinaryExpression != null) {
                                putSetterInfo(enclosingBinaryExpression.getLeftExpression(), info);
                            }
                            String delegationData = receiver.getData();
                            if (delegationData != null) {
                                pexp.putNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER, delegationData);
                            }
                            return true;
                        } else if (getter!=null) {
                            pexp.putNodeMetaData(StaticTypesMarker.READONLY_PROPERTY, true);
                        }
                    }
                    foundGetterOrSetter = foundGetterOrSetter || !setters.isEmpty() || getter!=null;

                    if (storeField(field, true, pexp, current, visitor, receiver.getData())) return true;
                    // if the property expression is an attribute expression (o.@attr), then
                    // we stop now, otherwise we must check the parent class
                    if (/*!isAttributeExpression && */current.getSuperClass() != null) {
                        queue.add(current.getUnresolvedSuperClass());
                    }
                }
                // GROOVY-5568, the property may be defined by DGM
                List<MethodNode> methods = findDGMMethodsByNameAndArguments(getTransformLoader(), testClass, "get" + capName, ClassNode.EMPTY_ARRAY);
                if (!methods.isEmpty()) {
                    List<MethodNode> methodNodes = chooseBestMethod(testClass, methods, ClassNode.EMPTY_ARRAY);
                    if (methodNodes.size() == 1) {
                        MethodNode getter = methodNodes.get(0);
                        if (visitor != null) {
                            visitor.visitMethod(getter);
                        }
                        ClassNode cn = inferReturnTypeGenerics(testClass, getter, ArgumentListExpression.EMPTY_ARGUMENTS);
                        storeInferredTypeForPropertyExpression(pexp, cn);

                        return true;
                    }
                }
        }

        for (Receiver<String> receiver : receivers) {
            ClassNode testClass = receiver.getType();
            ClassNode propertyType = getTypeForMapPropertyExpression(testClass, objectExpressionType, pexp);
            if (propertyType==null) propertyType = getTypeForListPropertyExpression(testClass, objectExpressionType, pexp);
            if (propertyType==null) propertyType = getTypeForSpreadExpression(testClass, objectExpressionType, pexp);
            if (propertyType==null) continue;
            if (visitor!=null) {
                // todo : type inference on maps and lists, if possible
                PropertyNode node = new PropertyNode(propertyName, Opcodes.ACC_PUBLIC, propertyType, receiver.getType(), null, null, null);
                node.setDeclaringClass(receiver.getType());
                visitor.visitProperty(node);
            }
            storeType(pexp, propertyType);
            String delegationData = receiver.getData();
            if (delegationData!=null) pexp.putNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER, delegationData);
            return true;
        }
        return foundGetterOrSetter;
    }

    private ClassNode getTypeForSpreadExpression(ClassNode testClass, ClassNode objectExpressionType, PropertyExpression pexp) {
        if (!pexp.isSpreadSafe()) return null;
        MethodCallExpression mce = new MethodCallExpression(new VariableExpression("_", testClass), "iterator", ArgumentListExpression.EMPTY_ARGUMENTS);
        mce.visit(this);
        ClassNode callType = getType(mce);
        if (!implementsInterfaceOrIsSubclassOf(callType, Iterator_TYPE)) return null;
        GenericsType[] types = callType.getGenericsTypes();
        ClassNode contentType = OBJECT_TYPE;
        if (types!=null && types.length==1) contentType = types[0].getType();
        PropertyExpression subExp = new PropertyExpression(
                new VariableExpression("{}", contentType),
                pexp.getPropertyAsString());
        AtomicReference<ClassNode> result = new AtomicReference<ClassNode>();
        if (existsProperty(subExp, true, new PropertyLookupVisitor(result))) {
            ClassNode intf = LIST_TYPE.getPlainNodeReference();
            intf.setGenericsTypes(new GenericsType[] { new GenericsType(getWrapper(result.get()))});
            return intf;
        }
        return null;
    }

    private ClassNode getTypeForListPropertyExpression(ClassNode testClass, ClassNode objectExpressionType, PropertyExpression pexp) {
        if (!implementsInterfaceOrIsSubclassOf(testClass, LIST_TYPE)) return null;
        ClassNode intf = GenericsUtils.parameterizeType(objectExpressionType, LIST_TYPE.getPlainNodeReference());
        GenericsType[] types = intf.getGenericsTypes();
        if (types==null || types.length!=1) return OBJECT_TYPE;

        PropertyExpression subExp = new PropertyExpression(
                new VariableExpression("{}", types[0].getType()),
                pexp.getPropertyAsString());
        AtomicReference<ClassNode> result = new AtomicReference<ClassNode>();
        if (existsProperty(subExp, true, new PropertyLookupVisitor(result))) {
            intf = LIST_TYPE.getPlainNodeReference();
            intf.setGenericsTypes(new GenericsType[] { new GenericsType(getWrapper(result.get()))});
            return intf;
        }
        return null;
    }

    private ClassNode getTypeForMapPropertyExpression(ClassNode testClass, ClassNode objectExpressionType, PropertyExpression pexp) {
        if (!implementsInterfaceOrIsSubclassOf(testClass, MAP_TYPE)) return null;
        ClassNode intf;
        if (objectExpressionType.getGenericsTypes()!=null) {
            intf = GenericsUtils.parameterizeType(objectExpressionType, MAP_TYPE.getPlainNodeReference());
        } else {
            intf = MAP_TYPE.getPlainNodeReference();
        }
        // 0 is the key, 1 is the value
        GenericsType[] types = intf.getGenericsTypes();
        if (types == null || types.length != 2) return OBJECT_TYPE;

        if (pexp.isSpreadSafe()) {
            // map*.property syntax
            // only "key" and "value" are allowed
            if ("key".equals(pexp.getPropertyAsString())) {
                ClassNode listKey = LIST_TYPE.getPlainNodeReference();
                listKey.setGenericsTypes(new GenericsType[]{types[0]});
                return listKey;
            } else if ("value".equals(pexp.getPropertyAsString())) {
                ClassNode listValue = LIST_TYPE.getPlainNodeReference();
                listValue.setGenericsTypes(new GenericsType[]{types[1]});
                return listValue;
            } else {
                addStaticTypeError("Spread operator on map only allows one of [key,value]", pexp);
            }
        } else {
            return types[1].getType();
        }
        return null;
    }

    /**
     * This method is used to filter search results in which null means "no match",
     * to filter out illegal access to instance members from a static context.
     *
     * Return null if the given member is not static, but we want to access in
     * a static way (staticOnly=true). If we want to access in a non-static way
     * we always return the member, since then access to static members and
     * non-static members is allowed.
     */
    @SuppressWarnings("unchecked")
    private <T> T allowStaticAccessToMember(T member, boolean staticOnly) {
        if (member == null) return null;
        if (!staticOnly) return member;
        boolean isStatic = false;
        if (member instanceof Variable) {
            Variable v = (Variable) member;
            isStatic = Modifier.isStatic(v.getModifiers());
        } else if (member instanceof List)  {
            List<MethodNode> list = (List<MethodNode>) member;
            if (list.size()==1) {
                return (T) Arrays.asList(allowStaticAccessToMember(list.get(0), staticOnly));
            }
            return (T) Collections.emptyList();
        } else {
            MethodNode mn = (MethodNode) member;
            isStatic = mn.isStatic();
        }
        if (staticOnly && !isStatic) return null;
        return member;
    }

    private void storeWithResolve(ClassNode typeToResolve, ClassNode receiver, ClassNode declaringClass, boolean isStatic, PropertyExpression expressionToStoreOn) {
        ClassNode type = typeToResolve;
        if (getGenericsWithoutArray(type)!=null) {
            Map<String, GenericsType> resolvedPlaceholders = resolvePlaceHoldersFromDeclaration(receiver, declaringClass, null, isStatic);
            type = resolveGenericsWithContext(resolvedPlaceholders, type);
        }
        storeInferredTypeForPropertyExpression(expressionToStoreOn, type);
        storeType(expressionToStoreOn, type);
    }

    private boolean storeField(FieldNode field, boolean returnTrueIfFieldExists, PropertyExpression expressionToStoreOn, ClassNode receiver, ClassCodeVisitorSupport visitor, String delegationData) {
        if (field==null || !returnTrueIfFieldExists) return false;
        if (visitor != null) visitor.visitField(field);
        storeWithResolve(field.getOriginType(), receiver, field.getDeclaringClass(), field.isStatic(), expressionToStoreOn);
        checkOrMarkPrivateAccess(field);
        if (delegationData!=null) {
            expressionToStoreOn.putNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER, delegationData);
        }
        return true;
    }

    private boolean storeProperty(PropertyNode propertyNode, PropertyExpression expressionToStoreOn, ClassNode receiver, ClassCodeVisitorSupport visitor, String delegationData) {
        if (propertyNode == null) return false;
        if (visitor != null) visitor.visitProperty(propertyNode);
        storeWithResolve(propertyNode.getOriginType(), receiver, propertyNode.getDeclaringClass(), propertyNode.isStatic(), expressionToStoreOn);
        if (delegationData!=null) {
            expressionToStoreOn.putNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER, delegationData);
        }
        return true;
    }


    protected void storeInferredTypeForPropertyExpression(final PropertyExpression pexp, final ClassNode flatInferredType) {
        if (pexp.isSpreadSafe()) {
            ClassNode list = LIST_TYPE.getPlainNodeReference();
            list.setGenericsTypes(new GenericsType[] {
                    new GenericsType(flatInferredType)
            });
            storeType(pexp, list);
        } else {
            storeType(pexp, flatInferredType);
        }
    }

    @Deprecated
    protected SetterInfo hasSetter(final PropertyExpression pexp) {
        String propertyName = pexp.getPropertyAsString();
        if (propertyName == null) return null;

        Expression objectExpression = pexp.getObjectExpression();
        List<Receiver<String>> receivers = new LinkedList<Receiver<String>>();
        List<Receiver<String>> owners = makeOwnerList(objectExpression);
        addReceivers(receivers, owners, pexp.isImplicitThis());

        String capName = MetaClassHelper.capitalize(propertyName);
        boolean isAttributeExpression = pexp instanceof AttributeExpression;

        for (Receiver<String> receiver: receivers) {
            ClassNode testClass = receiver.getType();
            LinkedList<ClassNode> queue = new LinkedList<ClassNode>();
            queue.add(testClass);
            if (testClass.isInterface()) {
                queue.addAll(testClass.getAllInterfaces());
            }
            while (!queue.isEmpty()) {
                ClassNode current = queue.removeFirst();
                current = current.redirect();
                // check that a setter also exists
                String setterName = "set" + capName;
                List<MethodNode> setterMethods = findSetters(current, setterName, false);
                if (!setterMethods.isEmpty()) {
//                    storeType(pexp, setterMethod.getParameters()[0].getType());
                    return new SetterInfo(current, setterName, setterMethods);
                }
                if (!isAttributeExpression && current.getSuperClass() != null) {
                    queue.add(current.getSuperClass());
                }
            }
        }
        return null;
    }

    @Override
    public void visitProperty(PropertyNode node) {
        final boolean osc = typeCheckingContext.isInStaticContext;
        try {
            typeCheckingContext.isInStaticContext = node.isInStaticContext();
            super.visitProperty(node);
        } finally {
            typeCheckingContext.isInStaticContext = osc;
        }
    }

    @Override
    public void visitField(final FieldNode node) {
        final boolean osc = typeCheckingContext.isInStaticContext;
        try {
            typeCheckingContext.isInStaticContext = node.isInStaticContext();
            super.visitField(node);
            Expression init = node.getInitialExpression();
            if (init != null) {
                FieldExpression left = new FieldExpression(node);
                BinaryExpression bexp = new BinaryExpression(
                        left,
                        Token.newSymbol("=", node.getLineNumber(), node.getColumnNumber()),
                        init
                );
                bexp.setSourcePosition(init);
                typeCheckAssignment(bexp, left, node.getOriginType(), init, getType(init));
                if (init instanceof ConstructorCallExpression) {
                    inferDiamondType((ConstructorCallExpression) init, node.getOriginType());
                }
            }
        } finally {
            typeCheckingContext.isInStaticContext = osc;
        }
    }

    @Override
    public void visitForLoop(final ForStatement forLoop) {
        // collect every variable expression used in the loop body
        final Map<VariableExpression, ClassNode> varOrigType = new HashMap<VariableExpression, ClassNode>();
        forLoop.getLoopBlock().visit(new VariableExpressionTypeMemoizer(varOrigType));

        // visit body
        Map<VariableExpression, List<ClassNode>> oldTracker = pushAssignmentTracking();
        Expression collectionExpression = forLoop.getCollectionExpression();
        if (collectionExpression instanceof ClosureListExpression) {
            // for (int i=0; i<...; i++) style loop
            super.visitForLoop(forLoop);
        } else {
            collectionExpression.visit(this);
            final ClassNode collectionType = getType(collectionExpression);
            ClassNode componentType = inferLoopElementType(collectionType);
            ClassNode forLoopVariableType = forLoop.getVariableType();
            if (ClassHelper.getUnwrapper(componentType) == forLoopVariableType) {
                // prefer primitive type over boxed type
                componentType = forLoopVariableType;
            }
            if (!checkCompatibleAssignmentTypes(forLoopVariableType, componentType)) {
                addStaticTypeError("Cannot loop with element of type " + forLoopVariableType.toString(false) + " with collection of type " + collectionType.toString(false), forLoop);
            }
            if (forLoopVariableType != DYNAMIC_TYPE) {
                // user has specified a type, prefer it over the inferred type
                componentType = forLoopVariableType;
            }
            typeCheckingContext.controlStructureVariables.put(forLoop.getVariable(), componentType);
            try {
                super.visitForLoop(forLoop);
            } finally {
                typeCheckingContext.controlStructureVariables.remove(forLoop.getVariable());
            }
        }
        boolean typeChanged = isSecondPassNeededForControlStructure(varOrigType, oldTracker);
        if (typeChanged) visitForLoop(forLoop);
    }

    /**
     * Given a loop collection type, returns the inferred type of the loop element. Used, for
     * example, to infer the element type of a (for e in list) loop.
     *
     * @param collectionType the type of the collection
     * @return the inferred component type
     */
    public static ClassNode inferLoopElementType(final ClassNode collectionType) {
        ClassNode componentType = collectionType.getComponentType();
        if (componentType == null) {
            if (collectionType.implementsInterface(ITERABLE_TYPE)) {
                ClassNode intf = GenericsUtils.parameterizeType(collectionType, ITERABLE_TYPE);
                GenericsType[] genericsTypes = intf.getGenericsTypes();
                componentType = genericsTypes[0].getType();
            } else if (collectionType == ClassHelper.STRING_TYPE) {
                componentType = ClassHelper.Character_TYPE;
            } else {
                componentType = ClassHelper.OBJECT_TYPE;
            }
        }
        return componentType;
    }

    protected boolean isSecondPassNeededForControlStructure(final Map<VariableExpression, ClassNode> varOrigType, final Map<VariableExpression, List<ClassNode>> oldTracker) {
        Map<VariableExpression, ClassNode> assignedVars = popAssignmentTracking(oldTracker);
        for (Map.Entry<VariableExpression, ClassNode> entry : assignedVars.entrySet()) {
            Variable key = findTargetVariable(entry.getKey());
            if (key instanceof VariableExpression) {
                ClassNode origType = varOrigType.get(key);
                ClassNode newType = entry.getValue();
                if (varOrigType.containsKey(key) && (origType == null || !newType.equals(origType))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void visitWhileLoop(final WhileStatement loop) {
        Map<VariableExpression, List<ClassNode>> oldTracker = pushAssignmentTracking();
        super.visitWhileLoop(loop);
        popAssignmentTracking(oldTracker);
    }

    @Override
    public void visitBitwiseNegationExpression(BitwiseNegationExpression expression) {
        super.visitBitwiseNegationExpression(expression);
        ClassNode type = getType(expression);
        ClassNode typeRe = type.redirect();
        ClassNode resultType;
        if (isBigIntCategory(typeRe)) {
            // allow any internal number that is not a floating point one
            resultType = type;
        } else if (typeRe == STRING_TYPE || typeRe == GSTRING_TYPE) {
            resultType = PATTERN_TYPE;
        } else if (typeRe == ArrayList_TYPE) {
            resultType = ArrayList_TYPE;
        } else if (typeRe.equals(PATTERN_TYPE)) {
            resultType = PATTERN_TYPE;
        } else {
            MethodNode mn = findMethodOrFail(expression, type, "bitwiseNegate");
            if (mn!=null) {
                resultType = mn.getReturnType();
            } else {
                resultType = OBJECT_TYPE;
            }
        }
        storeType(expression, resultType);
    }

    @Override
    public void visitUnaryPlusExpression(UnaryPlusExpression expression) {
        super.visitUnaryPlusExpression(expression);
        negativeOrPositiveUnary(expression, "positive");
    }

    @Override
    public void visitUnaryMinusExpression(UnaryMinusExpression expression) {
        super.visitUnaryMinusExpression(expression);
        negativeOrPositiveUnary(expression, "negative");
    }

    @Override
    public void visitPostfixExpression(final PostfixExpression expression) {
        super.visitPostfixExpression(expression);
        Expression inner = expression.getExpression();
        int op = expression.getOperation().getType();
        visitPrefixOrPostifExpression(expression, inner, op);
    }

    @Override
    public void visitPrefixExpression(final PrefixExpression expression) {
        super.visitPrefixExpression(expression);
        Expression inner = expression.getExpression();
        int type = expression.getOperation().getType();
        visitPrefixOrPostifExpression(expression, inner, type);
    }

    private static ClassNode getMathWideningClassNode(ClassNode type) {
        if (byte_TYPE.equals(type) || short_TYPE.equals(type) || int_TYPE.equals(type)) {
            return int_TYPE;
        }
        if (Byte_TYPE.equals(type) || Short_TYPE.equals(type) || Integer_TYPE.equals(type)) {
            return Integer_TYPE;
        }
        if (float_TYPE.equals(type)) return double_TYPE;
        if (Float_TYPE.equals(type)) return Double_TYPE;
        return type;
    }

    private void visitPrefixOrPostifExpression(final Expression origin, final Expression innerExpression, final int operationType) {
        boolean isPostfix = origin instanceof PostfixExpression;
        ClassNode exprType = getType(innerExpression);
        String name = operationType == PLUS_PLUS ? "next" : operationType == MINUS_MINUS ? "previous" : null;
        if (isPrimitiveType(exprType) || isPrimitiveType(getUnwrapper(exprType))) {
            if (operationType == PLUS_PLUS || operationType == MINUS_MINUS) {
                if (!isPrimitiveType(exprType)) {
                    MethodNode node = findMethodOrFail(new VariableExpression("_dummy_", exprType), exprType, name);
                    if (node != null) {
                        storeTargetMethod(origin, node);
                        storeType(origin,
                                isPostfix?exprType:getMathWideningClassNode(exprType));
                        return;
                    }
                }
                storeType(origin, exprType);
                return;
            }
            addUnsupportedPreOrPostfixExpressionError(origin);
            return;
        } else if (implementsInterfaceOrIsSubclassOf(exprType, Number_TYPE) && (operationType == PLUS_PLUS || operationType == MINUS_MINUS)) {
            // special case for numbers, improve type checking as we can expect ++ and -- to return the same type
            MethodNode node = findMethodOrFail(innerExpression, exprType, name);
            if (node != null) {
                storeTargetMethod(origin, node);
                storeType(origin, getMathWideningClassNode(exprType));
                return;
            }
        }
        // not a primitive type. We must find a method which is called next
        if (name == null) {
            addUnsupportedPreOrPostfixExpressionError(origin);
            return;
        }
        MethodNode node = findMethodOrFail(innerExpression, exprType, name);
        if (node != null) {
            storeTargetMethod(origin, node);
            storeType(origin,  isPostfix?exprType:inferReturnTypeGenerics(exprType, node, ArgumentListExpression.EMPTY_ARGUMENTS));
        }
    }

    private void negativeOrPositiveUnary(Expression expression, String name) {
        ClassNode type = getType(expression);
        ClassNode typeRe = type.redirect();
        ClassNode resultType;
        if (isDoubleCategory(ClassHelper.getUnwrapper(typeRe))) {
            resultType = type;
        } else if (typeRe == ArrayList_TYPE) {
            resultType = ArrayList_TYPE;
        } else {
            MethodNode mn = findMethodOrFail(expression, type, name);
            if (mn != null) {
                resultType = mn.getReturnType();
            } else {
                resultType = type;
            }
        }
        storeType(expression, resultType);
    }

    @Override
    protected void visitConstructorOrMethod(MethodNode node, boolean isConstructor) {
        typeCheckingContext.pushEnclosingMethod(node);
        if (!isSkipMode(node) && !shouldSkipMethodNode(node)) {
            super.visitConstructorOrMethod(node, isConstructor);
        }
        if (!isConstructor) {
            returnAdder.visitMethod(node);
        }
        typeCheckingContext.popEnclosingMethod();
    }

    @Override
    public void visitReturnStatement(ReturnStatement statement) {
        super.visitReturnStatement(statement);
        returnListener.returnStatementAdded(statement);
    }

    protected ClassNode checkReturnType(final ReturnStatement statement) {
        Expression expression = statement.getExpression();
        ClassNode type = getType(expression);
        if (typeCheckingContext.getEnclosingClosure()!=null) {
            return type;
        }
        MethodNode enclosingMethod = typeCheckingContext.getEnclosingMethod();
        if (enclosingMethod != null && typeCheckingContext.getEnclosingClosure()==null) {
            if (!enclosingMethod.isVoidMethod()
                    && !type.equals(void_WRAPPER_TYPE)
                    && !type.equals(VOID_TYPE)
                    && !checkCompatibleAssignmentTypes(enclosingMethod.getReturnType(), type, null, false)
                    && !(isNullConstant(expression))) {
                if (!extension.handleIncompatibleReturnType(statement, type)) {
                    addStaticTypeError("Cannot return value of type " + type.toString(false) + " on method returning type " + enclosingMethod.getReturnType().toString(false), expression);
                }
            } else if (!enclosingMethod.isVoidMethod()) {
                ClassNode previousType = getInferredReturnType(enclosingMethod);
                ClassNode inferred = previousType == null ? type : lowestUpperBound(type, previousType);
                if (implementsInterfaceOrIsSubclassOf(inferred, enclosingMethod.getReturnType())) {
                    if (missesGenericsTypes(inferred)) {
                        DeclarationExpression virtualDecl = new DeclarationExpression(
                                new VariableExpression("{target}", enclosingMethod.getReturnType()),
                                Token.newSymbol(EQUAL, -1, -1),
                                new VariableExpression("{source}", type)
                        );
                        virtualDecl.setSourcePosition(statement);
                        virtualDecl.visit(this);
                        ClassNode newlyInferred = (ClassNode) virtualDecl.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
                        if (!missesGenericsTypes(newlyInferred)) type = newlyInferred;
                    }
                    return type;
                } else {
                    return enclosingMethod.getReturnType();
                }
            }
        }
        return type;
    }

    protected void addClosureReturnType(ClassNode returnType) {
        typeCheckingContext.getEnclosingClosure().addReturnType(returnType);
    }

    @Override
    public void visitConstructorCallExpression(ConstructorCallExpression call) {
        super.visitConstructorCallExpression(call);
        if (extension.beforeMethodCall(call)) {
            extension.afterMethodCall(call);
            return;
        }
        ClassNode receiver = call.isThisCall() ? typeCheckingContext.getEnclosingClassNode() :
                call.isSuperCall() ? typeCheckingContext.getEnclosingClassNode().getSuperClass() : call.getType();
        Expression arguments = call.getArguments();

        ArgumentListExpression argumentList = InvocationWriter.makeArgumentList(arguments);

        checkForbiddenSpreadArgument(argumentList);

        ClassNode[] args = getArgumentTypes(argumentList);
        if (    args.length>0 &&
                typeCheckingContext.getEnclosingClosure()!=null &&
                argumentList.getExpression(0) instanceof VariableExpression &&
                ((VariableExpression) argumentList.getExpression(0)).isThisExpression() &&
                call.getType() instanceof InnerClassNode &&
                call.getType().getOuterClass().equals(args[0]) &&
                !call.getType().isStaticClass())
        {
            args[0] = CLOSURE_TYPE;
        }


        MethodNode node = null;
        if (args.length == 1
                && implementsInterfaceOrIsSubclassOf(args[0], MAP_TYPE)
                && findMethod(receiver, "<init>", ClassNode.EMPTY_ARRAY).size() == 1
                && findMethod(receiver, "<init>", args).isEmpty()) {
            // bean-style constructor
            node = typeCheckMapConstructor(call, receiver, arguments);
            if (node != null) {
                storeTargetMethod(call, node);
                extension.afterMethodCall(call);
                return;
            }
        }
        node = findMethodOrFail(call, receiver, "<init>", args);
        if (node != null) {
            if (node.getParameters().length == 0 && args.length == 1 && implementsInterfaceOrIsSubclassOf(args[0], MAP_TYPE)) {
                node = typeCheckMapConstructor(call, receiver, arguments);
            } else {
                typeCheckMethodsWithGenericsOrFail(receiver, args, node, call);
            }
            if (node != null) storeTargetMethod(call, node);
        }
        extension.afterMethodCall(call);
    }

    protected MethodNode typeCheckMapConstructor(final ConstructorCallExpression call, final ClassNode receiver, final Expression arguments) {
        MethodNode node = null;
        if (arguments instanceof TupleExpression) {
            TupleExpression texp = (TupleExpression) arguments;
            List<Expression> expressions = texp.getExpressions();
            if (expressions.size() == 1) {
                Expression expression = expressions.get(0);
                if (expression instanceof MapExpression) {
                    MapExpression argList = (MapExpression) expression;
                    checkGroovyConstructorMap(call, receiver, argList);
                    node = new ConstructorNode(Opcodes.ACC_PUBLIC, new Parameter[]{new Parameter(MAP_TYPE, "map")}, ClassNode.EMPTY_ARRAY, GENERATED_EMPTY_STATEMENT);
                    node.setDeclaringClass(receiver);
                }
            }
        }
        return node;
    }

    protected ClassNode[] getArgumentTypes(ArgumentListExpression args) {
        List<Expression> arglist = args.getExpressions();
        ClassNode[] ret = new ClassNode[arglist.size()];
        int i = 0;
        Map<Object, List<ClassNode>> info = typeCheckingContext.temporaryIfBranchTypeInformation.empty() ? null : typeCheckingContext.temporaryIfBranchTypeInformation.peek();
        for (Expression exp : arglist) {
            if (isNullConstant(exp)) {
                ret[i] = UNKNOWN_PARAMETER_TYPE;
            } else {
                ret[i] = getType(exp);
                if (exp instanceof VariableExpression && info != null) {
                    List<ClassNode> classNodes = getTemporaryTypesForExpression(exp);
                    if (classNodes != null && !classNodes.isEmpty()) {
                        ArrayList<ClassNode> arr = new ArrayList<ClassNode>(classNodes.size() + 1);
                        arr.add(ret[i]);
                        arr.addAll(classNodes);
                        ret[i] = new UnionTypeClassNode(arr.toArray(new ClassNode[arr.size()]));
                    }
                }
            }
            i++;
        }
        return ret;
    }

    @Override
    public void visitClosureExpression(final ClosureExpression expression) {
        boolean oldStaticContext = typeCheckingContext.isInStaticContext;
        typeCheckingContext.isInStaticContext = false;

        // collect every variable expression used in the loop body
        final Map<VariableExpression, ClassNode> varOrigType = new HashMap<VariableExpression, ClassNode>();
        Statement code = expression.getCode();
        code.visit(new VariableExpressionTypeMemoizer(varOrigType));
        Map<VariableExpression, List<ClassNode>> oldTracker = pushAssignmentTracking();

        // first, collect closure shared variables and reinitialize types
        SharedVariableCollector collector = new SharedVariableCollector(getSourceUnit());
        collector.visitClosureExpression(expression);
        Set<VariableExpression> closureSharedExpressions = collector.getClosureSharedExpressions();
        Map<VariableExpression, ListHashMap> typesBeforeVisit = null;
        if (!closureSharedExpressions.isEmpty()) {
            typesBeforeVisit = new HashMap<VariableExpression, ListHashMap>();
            saveVariableExpressionMetadata(closureSharedExpressions, typesBeforeVisit);
        }

        // perform visit
        typeCheckingContext.pushEnclosingClosureExpression(expression);
        DelegationMetadata dmd = getDelegationMetadata(expression);
        if (dmd ==null) {
            typeCheckingContext.delegationMetadata = new DelegationMetadata(
                    typeCheckingContext.getEnclosingClassNode(), Closure.OWNER_FIRST, typeCheckingContext.delegationMetadata
            );
        } else {
            typeCheckingContext.delegationMetadata = new DelegationMetadata(
                    dmd.getType(),
                    dmd.getStrategy(),
                    typeCheckingContext.delegationMetadata
            );
        }
        super.visitClosureExpression(expression);
        typeCheckingContext.delegationMetadata = typeCheckingContext.delegationMetadata.getParent();
        MethodNode node = new MethodNode("dummy", 0, ClassHelper.OBJECT_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, code);
        returnAdder.visitMethod(node);

        TypeCheckingContext.EnclosingClosure enclosingClosure = typeCheckingContext.getEnclosingClosure();
        if (!enclosingClosure.getReturnTypes().isEmpty()) {
            ClassNode returnType = lowestUpperBound(enclosingClosure.getReturnTypes());
            storeInferredReturnType(expression, returnType);
            ClassNode inferredType = wrapClosureType(returnType);
            storeType(enclosingClosure.getClosureExpression(), inferredType);
        }

        typeCheckingContext.popEnclosingClosure();

        boolean typeChanged = isSecondPassNeededForControlStructure(varOrigType, oldTracker);
        if (typeChanged) visitClosureExpression(expression);

        // restore original metadata
        restoreVariableExpressionMetadata(typesBeforeVisit);
        typeCheckingContext.isInStaticContext = oldStaticContext;
        Parameter[] parameters = expression.getParameters();
        if (parameters!=null) {
            for (Parameter parameter : parameters) {
                typeCheckingContext.controlStructureVariables.remove(parameter);
            }
        }
    }

    private ClassNode wrapClosureType(final ClassNode returnType) {
        ClassNode inferredType = CLOSURE_TYPE.getPlainNodeReference();
        inferredType.setGenericsTypes(new GenericsType[]{new GenericsType(wrapTypeIfNecessary(returnType))});
        return inferredType;
    }

    protected DelegationMetadata getDelegationMetadata(final ClosureExpression expression) {
        return (DelegationMetadata) expression.getNodeMetaData(StaticTypesMarker.DELEGATION_METADATA);
    }

    protected void restoreVariableExpressionMetadata(final Map<VariableExpression, ListHashMap> typesBeforeVisit) {
        if (typesBeforeVisit != null) {
            for (Map.Entry<VariableExpression, ListHashMap> entry : typesBeforeVisit.entrySet()) {
                VariableExpression ve = entry.getKey();
                ListHashMap metadata = entry.getValue();
                for (StaticTypesMarker marker : StaticTypesMarker.values()) {
                    ve.removeNodeMetaData(marker);
                    Object value = metadata.get(marker);
                    if (value != null) ve.setNodeMetaData(marker, value);
                }
            }
        }
    }

    protected void saveVariableExpressionMetadata(final Set<VariableExpression> closureSharedExpressions, final Map<VariableExpression, ListHashMap> typesBeforeVisit) {
        for (VariableExpression ve : closureSharedExpressions) {
            // GROOVY-6921: We must force a call to getType in order to update closure shared variable which types are
            // inferred thanks to closure parameter type inference
            ClassNode cn = getType(ve);
            ListHashMap<StaticTypesMarker, Object> metadata = new ListHashMap<StaticTypesMarker, Object>();
            for (StaticTypesMarker marker : StaticTypesMarker.values()) {
                Object value = ve.getNodeMetaData(marker);
                if (value != null) {
                    metadata.put(marker, value);
                }
            }
            typesBeforeVisit.put(ve, metadata);
            Variable accessedVariable = ve.getAccessedVariable();
            if (accessedVariable != ve && accessedVariable instanceof VariableExpression) {
                saveVariableExpressionMetadata(Collections.singleton((VariableExpression) accessedVariable), typesBeforeVisit);
            }
        }
    }

    protected boolean shouldSkipMethodNode(final MethodNode node) {
        Object type = node.getNodeMetaData(StaticTypeCheckingVisitor.class);
        return Boolean.TRUE.equals(type);
    }

    @Override
    public void visitMethod(final MethodNode node) {
        if (shouldSkipMethodNode(node)) {
            // method has already been visited by a static type checking visitor
            return;
        }
        if (!extension.beforeVisitMethod(node)) {
        ErrorCollector collector = (ErrorCollector) node.getNodeMetaData(ERROR_COLLECTOR);
        if (collector != null) {
            typeCheckingContext.getErrorCollector().addCollectorContents(collector);
        } else {
            startMethodInference(node, typeCheckingContext.getErrorCollector());
        }
        node.removeNodeMetaData(ERROR_COLLECTOR);
        }
        extension.afterVisitMethod(node);
    }

    @Override
    public void visitConstructor(final ConstructorNode node) {
        if (shouldSkipMethodNode(node)) {
            // method has already been visited by a static type checking visitor
            return;
        }
        for (Parameter parameter : node.getParameters()) {
            if (parameter.getInitialExpression()!=null) {
                parameter.getInitialExpression().visit(this);
            }
        }
        super.visitConstructor(node);
    }

    protected void startMethodInference(final MethodNode node, ErrorCollector collector) {
        if (isSkipMode(node)) return;

        // second, we must ensure that this method MUST be statically checked
        // for example, in a mixed mode where only some methods are statically checked
        // we must not visit a method which used dynamic dispatch.
        // We do not check for an annotation because some other AST transformations
        // may use this visitor without the annotation being explicitely set
        if (!typeCheckingContext.methodsToBeVisited.isEmpty() && !typeCheckingContext.methodsToBeVisited.contains(node)) return;

        // alreadyVisitedMethods prevents from visiting the same method multiple times
        // and prevents from infinite loops
        if (typeCheckingContext.alreadyVisitedMethods.contains(node)) return;
        typeCheckingContext.alreadyVisitedMethods.add(node);

        typeCheckingContext.pushErrorCollector(collector);

        final boolean osc = typeCheckingContext.isInStaticContext;
        try {
            typeCheckingContext.isInStaticContext = node.isStatic();
            super.visitMethod(node);
            for (Parameter parameter : node.getParameters()) {
                if (parameter.getInitialExpression()!=null) {
                    parameter.getInitialExpression().visit(this);
                }
            }
/*
            ClassNode rtype = getInferredReturnType(node);
            if (rtype == null) {
                storeInferredReturnType(node, node.getReturnType());
            }
            addTypeCheckingInfoAnnotation(node);
*/
        } finally {
            typeCheckingContext.isInStaticContext = osc;
        }

        typeCheckingContext.popErrorCollector();
        node.putNodeMetaData(ERROR_COLLECTOR, collector);
    }

    protected void addTypeCheckingInfoAnnotation(final MethodNode node) {
        // TypeChecked$TypeCheckingInfo can not be applied on constructors
        if (node instanceof ConstructorNode) return;

        // if a returned inferred type is available and no @TypeCheckingInfo is on node, then add an
        // annotation to the method node
        ClassNode rtype = getInferredReturnType(node);
        if (rtype != null && node.getAnnotations(TYPECHECKING_INFO_NODE).isEmpty()) {
            AnnotationNode anno = new AnnotationNode(TYPECHECKING_INFO_NODE);
            anno.setMember("version", CURRENT_SIGNATURE_PROTOCOL);
            SignatureCodec codec = SignatureCodecFactory.getCodec(CURRENT_SIGNATURE_PROTOCOL_VERSION, getTransformLoader());
            String genericsSignature = codec.encode(rtype);
            if (genericsSignature != null) {
                ConstantExpression signature = new ConstantExpression(genericsSignature);
                signature.setType(STRING_TYPE);
                anno.setMember("inferredType", signature);
                node.addAnnotation(anno);
            }
        }
    }

    @Override
    public void visitStaticMethodCallExpression(final StaticMethodCallExpression call) {
        final String name = call.getMethod();
        if (name == null) {
            addStaticTypeError("cannot resolve dynamic method name at compile time.", call);
            return;
        }

        if (extension.beforeMethodCall(call)) {
            extension.afterMethodCall(call);
            return;
        }

        final ClassNode rememberLastItType = typeCheckingContext.lastImplicitItType;
        Expression callArguments = call.getArguments();

        ArgumentListExpression argumentList = InvocationWriter.makeArgumentList(callArguments);

        checkForbiddenSpreadArgument(argumentList);

        final ClassNode receiver = call.getOwnerType();
        visitMethodCallArguments(receiver, argumentList, false, null);

        ClassNode[] args = getArgumentTypes(argumentList);

        try {

            // method call receivers are :
            //   - possible "with" receivers
            //   - the actual receiver as found in the method call expression
            //   - any of the potential receivers found in the instanceof temporary table
            // in that order
            List<Receiver<String>> receivers = new LinkedList<Receiver<String>>();
            addReceivers(receivers, makeOwnerList(new ClassExpression(receiver)), false);
            List<MethodNode> mn = null;
            Receiver<String> chosenReceiver = null;
            for (Receiver<String> currentReceiver : receivers) {
                mn = findMethod(currentReceiver.getType(), name, args);
                if (!mn.isEmpty()) {
                    if (mn.size() == 1) typeCheckMethodsWithGenericsOrFail(currentReceiver.getType(), args, mn.get(0), call);
                    chosenReceiver = currentReceiver;
                    break;
                }
            }
            if (mn.isEmpty()) {
                mn = extension.handleMissingMethod(receiver, name, argumentList, args, call);
            }
            boolean callArgsVisited = false;
            if (mn.isEmpty()) {
                addNoMatchingMethodError(receiver, name, args, call);
            } else {
                mn = disambiguateMethods(mn, receiver, args, call);
                if (mn.size() == 1) {
                    MethodNode directMethodCallCandidate = mn.get(0);
                    ClassNode returnType = getType(directMethodCallCandidate);
                    if (returnType.isUsingGenerics() && !returnType.isEnum()) {
                        visitMethodCallArguments(receiver, argumentList, true, directMethodCallCandidate);
                        ClassNode irtg = inferReturnTypeGenerics(chosenReceiver.getType(), directMethodCallCandidate, callArguments);
                        returnType = irtg != null && implementsInterfaceOrIsSubclassOf(irtg, returnType) ? irtg : returnType;
                        callArgsVisited = true;
                    }
                    storeType(call, returnType);
                    storeTargetMethod(call, directMethodCallCandidate);

                } else {
                    addAmbiguousErrorMessage(mn, name, args, call);
                }
                if (!callArgsVisited) {
                    visitMethodCallArguments(receiver, argumentList, true, (MethodNode)call.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET));
                }
            }
        } finally {
            extension.afterMethodCall(call);
        }
    }

    /**
     * @deprecated this method is unused, replaced with {@link DelegatesTo} inference.
     * @param callArguments
     * @param receiver
     */
    protected void checkClosureParameters(final Expression callArguments, final ClassNode receiver) {
        if (callArguments instanceof ArgumentListExpression) {
            ArgumentListExpression argList = (ArgumentListExpression) callArguments;
            ClosureExpression closure = (ClosureExpression) argList.getExpression(0);
            Parameter[] parameters = closure.getParameters();
            if (parameters.length > 1) {
                addStaticTypeError("Unexpected number of parameters for a with call", argList);
            } else if (parameters.length == 1) {
                Parameter param = parameters[0];
                if (!param.isDynamicTyped() && !isAssignableTo(receiver, param.getType().redirect())) {
                    addStaticTypeError("Expected parameter type: " + receiver.toString(false) + " but was: " + param.getType().redirect().toString(false), param);
                }
            }
            closure.putNodeMetaData(StaticTypesMarker.DELEGATION_METADATA, new DelegationMetadata(
                    receiver,
                    Closure.DELEGATE_FIRST,
                    typeCheckingContext.delegationMetadata
            ));
        }
    }

    /**
     * visit a method call target, to infer the type. Don't report errors right
     * away, that will be done by a later visitMethod call
     */
    protected void silentlyVisitMethodNode(final MethodNode directMethodCallCandidate) {
        // visit is authorized because the classnode belongs to the same source unit
        ErrorCollector collector = new ErrorCollector(typeCheckingContext.getErrorCollector().getConfiguration());
        startMethodInference(directMethodCallCandidate, collector);
    }

    protected void visitMethodCallArguments(final ClassNode receiver, ArgumentListExpression arguments, boolean visitClosures, final MethodNode selectedMethod) {
        Parameter[] params = selectedMethod!=null?selectedMethod.getParameters():Parameter.EMPTY_ARRAY;
        List<Expression> expressions = new LinkedList<Expression>(arguments.getExpressions());
        if (selectedMethod instanceof ExtensionMethodNode) {
            params = ((ExtensionMethodNode) selectedMethod).getExtensionMethodNode().getParameters();
            expressions.add(0, new VariableExpression("$self", receiver));
        }
        ArgumentListExpression newArgs = new ArgumentListExpression(expressions);

        for (int i = 0, expressionsSize = expressions.size(); i < expressionsSize; i++) {
            final Expression expression = expressions.get(i);
            if (visitClosures && expression instanceof ClosureExpression
                    || !visitClosures && !(expression instanceof ClosureExpression)) {
                if (i<params.length && visitClosures) {
                    Parameter param = params[i];
                    checkClosureWithDelegatesTo(newArgs,params , expression, param);
                    if (selectedMethod instanceof ExtensionMethodNode) {
                        if (i>0) {
                            inferClosureParameterTypes(receiver, arguments, (ClosureExpression)expression, param, selectedMethod);
                        }
                    } else {
                        inferClosureParameterTypes(receiver, newArgs, (ClosureExpression) expression, param, selectedMethod);
                    }
                }
                expression.visit(this);
                if (expression.getNodeMetaData(StaticTypesMarker.DELEGATION_METADATA)!=null) {
                    expression.removeNodeMetaData(StaticTypesMarker.DELEGATION_METADATA);
                }
            }
        }
    }

    /**
     * This method is responsible for performing type inference on closure argument types whenever code like this is
     * found: <code>foo.collect { it.toUpperCase() }</code>.
     * In this case, the type checker tries to find if the <code>collect</code> method has its {@link Closure} argument
     * annotated with {@link groovy.transform.stc.ClosureParams}. If yes, then additional type inference can be performed
     * and the type of <code>it</code> may be inferred.
     *
     * @param receiver
     * @param arguments
     * @param expression a closure expression for which the argument types should be inferred
     * @param param the parameter where to look for a {@link groovy.transform.stc.ClosureParams} annotation.
     * @param selectedMethod the method accepting a closure
     */
    protected void inferClosureParameterTypes(final ClassNode receiver, final Expression arguments, final ClosureExpression expression, final Parameter param, final MethodNode selectedMethod) {
        List<AnnotationNode> annotations = param.getAnnotations(CLOSUREPARAMS_CLASSNODE);
        if (annotations!=null && !annotations.isEmpty()) {
            for (AnnotationNode annotation : annotations) {
                Expression hintClass = annotation.getMember("value");
                Expression options = annotation.getMember("options");
                if (hintClass instanceof ClassExpression) {
                    doInferClosureParameterTypes(receiver, arguments, expression, selectedMethod, hintClass, options);
                }
            }
        } else if (isSAMType(param.getOriginType())) {
            // SAM coercion
            inferSAMType(param, receiver, selectedMethod, InvocationWriter.makeArgumentList(arguments), expression);
        }
    }


    private void inferSAMType(Parameter param, ClassNode receiver, MethodNode methodWithSAMParameter, ArgumentListExpression originalMethodCallArguments, ClosureExpression openBlock) {
        // In a method call with SAM coercion the inference is to be
        // understood as a two phase process. We have the normal method call
        // to the target method with the closure argument and we have the 
        // SAM method that will be called inside the normal target method. 
        // To infer correctly we have to "simulate" this process. We know the
        // call to the closure will be done through the SAM type, so the SAM
        // type generics deliver information about the Closure. At the same
        // time the SAM class is used in the target method parameter, 
        // providing a connection from the SAM type and the target method
        // declaration class.

        // First we try to get as much information about the declaration
        // class through the receiver
        Map<String,GenericsType> targetMethodDeclarationClassConnections = new HashMap<String,GenericsType>();
        extractGenericsConnections(targetMethodDeclarationClassConnections, receiver, receiver.redirect());
        // then we use the method with the SAM parameter to get more information about the declaration
        Parameter[] parametersOfMethodContainingSAM = methodWithSAMParameter.getParameters();
        for (int i=0; i<parametersOfMethodContainingSAM.length; i++) {
            Expression callArg = originalMethodCallArguments.getExpression(i);
            // we look at the closure later in detail, so skip it here
            if (callArg==openBlock) continue;
            ClassNode parameterType = parametersOfMethodContainingSAM[i].getType();
            extractGenericsConnections(targetMethodDeclarationClassConnections, getType(callArg), parameterType);
        }

        // To make a connection to the SAM class we use that new information
        // to replace the generics in the SAM type parameter of the target
        // method and than that to make the connections to the SAM type generics
        ClassNode paramTypeWithReceiverInformation = applyGenericsContext(targetMethodDeclarationClassConnections, param.getOriginType());
        Map<String,GenericsType> SAMTypeConnections = new HashMap<String,GenericsType>();
        ClassNode classForSAM = paramTypeWithReceiverInformation.redirect();
        extractGenericsConnections(SAMTypeConnections, paramTypeWithReceiverInformation, classForSAM);

        // should the open block provide final information we apply that
        // to the corresponding parameters of the SAM type method
        MethodNode methodForSAM = findSAM(classForSAM);
        ClassNode[] parameterTypesForSAM = extractTypesFromParameters(methodForSAM.getParameters());
        ClassNode[] blockParameterTypes = (ClassNode[]) openBlock.getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS);
        if (blockParameterTypes==null) {
            Parameter[] p = openBlock.getParameters();
            if (p.length==0 && parameterTypesForSAM.length!=0) {
                // implicit it
                blockParameterTypes = parameterTypesForSAM;
            } else {
                blockParameterTypes = extractTypesFromParameters(p);
            }
        }
        for (int i=0; i<blockParameterTypes.length; i++) { //TODO: equal length guaranteed?
            extractGenericsConnections(SAMTypeConnections, blockParameterTypes[i], parameterTypesForSAM[i]);
        }

        // and finally we apply the generics information to the parameters and 
        // store the type of parameter and block type as meta information
        for (int i=0; i<blockParameterTypes.length; i++) { //TODO: equal length guaranteed?
            ClassNode resolvedParameter =
                    applyGenericsContext(SAMTypeConnections, parameterTypesForSAM[i]);
            blockParameterTypes[i] = resolvedParameter;
        }
        openBlock.putNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS, blockParameterTypes);
    }

    private List<ClassNode[]> getSignaturesFromHint(final ClosureExpression expression, final MethodNode selectedMethod, final Expression hintClass, final Expression options) {
        // initialize hints
        List<ClassNode[]> closureSignatures = null;
        try {
            ClassLoader transformLoader = getTransformLoader();
            @SuppressWarnings("unchecked")
            Class<? extends ClosureSignatureHint> hint = (Class<? extends ClosureSignatureHint>) transformLoader.loadClass(hintClass.getText());
            ClosureSignatureHint hintInstance = hint.newInstance();
            closureSignatures = hintInstance.getClosureSignatures(
                    selectedMethod instanceof ExtensionMethodNode ?((ExtensionMethodNode) selectedMethod).getExtensionMethodNode():selectedMethod,
                    typeCheckingContext.source,
                    typeCheckingContext.compilationUnit,
                    convertToStringArray(options), expression);
        } catch (ClassNotFoundException e) {
            throw new GroovyBugError(e);
        } catch (InstantiationException e) {
            throw new GroovyBugError(e);
        } catch (IllegalAccessException e) {
            throw new GroovyBugError(e);
        }
        return closureSignatures;
    }

    private ClassLoader getTransformLoader() {
        CompilationUnit compilationUnit = typeCheckingContext.getCompilationUnit();
        return compilationUnit!=null?compilationUnit.getTransformLoader():getSourceUnit().getClassLoader();
    }

    private void doInferClosureParameterTypes(final ClassNode receiver, final Expression arguments, final ClosureExpression expression, final MethodNode selectedMethod, final Expression hintClass, final Expression options) {
        List<ClassNode[]> closureSignatures = getSignaturesFromHint(expression,selectedMethod,hintClass,options);
        List<ClassNode[]> candidates = new LinkedList<ClassNode[]>();
        for (ClassNode[] signature : closureSignatures) {
            // in order to compute the inferred types of the closure parameters, we're using the following trick:
            // 1. create a dummy MethodNode for which the return type is a class node for which the generic types are the types returned by the hint
            // 2. call inferReturnTypeGenerics
            // 3. fetch inferred types from the result of inferReturnTypeGenerics
            // In practice, it could be done differently but it has the main advantage of reusing
            // existing code, hence reducing the amount of code to debug in case of failure.
            final int id = System.identityHashCode(expression);
            ClassNode dummyResultNode = new ClassNode("cl$" + id, 0, OBJECT_TYPE).getPlainNodeReference();
            final GenericsType[] genericTypes = new GenericsType[signature.length];
            for (int i = 0; i < signature.length; i++) {
                genericTypes[i] = new GenericsType(signature[i]);
            }
            dummyResultNode.setGenericsTypes(genericTypes);
            MethodNode dummyMN = selectedMethod instanceof ExtensionMethodNode ? ((ExtensionMethodNode) selectedMethod).getExtensionMethodNode() : selectedMethod;
            dummyMN = new MethodNode(
                    dummyMN.getName(),
                    dummyMN.getModifiers(),
                    dummyResultNode,
                    dummyMN.getParameters(),
                    dummyMN.getExceptions(),
                    EmptyStatement.INSTANCE
                    );
            dummyMN.setDeclaringClass(selectedMethod.getDeclaringClass());
            dummyMN.setGenericsTypes(selectedMethod.getGenericsTypes());
            if (selectedMethod instanceof ExtensionMethodNode) {
                ExtensionMethodNode orig = (ExtensionMethodNode) selectedMethod;
                dummyMN = new ExtensionMethodNode(
                        dummyMN,
                        dummyMN.getName(),
                        dummyMN.getModifiers(),
                        dummyResultNode,
                        orig.getParameters(),
                        orig.getExceptions(),
                        EmptyStatement.INSTANCE,
                        orig.isStaticExtension()
                        );
                dummyMN.setDeclaringClass(orig.getDeclaringClass());
                dummyMN.setGenericsTypes(orig.getGenericsTypes());
            }
            ClassNode classNode = inferReturnTypeGenerics(receiver, dummyMN, arguments);
            ClassNode[] inferred = new ClassNode[classNode.getGenericsTypes().length];
            for (int i = 0; i < classNode.getGenericsTypes().length; i++) {
                GenericsType genericsType = classNode.getGenericsTypes()[i];
                ClassNode value = createUsableClassNodeFromGenericsType(genericsType);
                inferred[i] = value;
            }
            Parameter[] closureParams = expression.getParameters();
            if (signature.length == closureParams.length // same number of arguments
                    || (signature.length == 1 && closureParams.length == 0) // implicit it
                    || (closureParams.length > signature.length && inferred[inferred.length - 1].isArray())) { // vargs
                candidates.add(inferred);
            }
        }
        Parameter[] closureParams = expression.getParameters();
        if (candidates.size()>1) {
            Iterator<ClassNode[]> candIt = candidates.iterator();
            while (candIt.hasNext()) {
                ClassNode[] inferred = candIt.next();
                final int length = closureParams.length;
                for (int i = 0; i < length; i++) {
                    Parameter closureParam = closureParams[i];
                    final ClassNode originType = closureParam.getOriginType();
                    ClassNode inferredType;
                    if (i<inferred.length-1 || inferred.length==closureParams.length) {
                        inferredType = inferred[i];
                    } else { // vargs?
                        ClassNode lastArgInferred = inferred[inferred.length-1];
                        if (lastArgInferred.isArray()) {
                            inferredType = lastArgInferred.getComponentType();
                        } else {
                            candIt.remove();
                            continue;
                        }
                    }
                    if (!typeCheckMethodArgumentWithGenerics(originType, inferredType, i== length -1)) {
                        candIt.remove();
                    }
                }
            }
            if (candidates.size()>1) {
                addError("Ambiguous prototypes for closure. More than one target method matches. Please use explicit argument types.", expression);
            }
        }
        if (candidates.size()==1) {
            ClassNode[] inferred = candidates.get(0);
            if (closureParams.length==0 && inferred.length==1) {
                expression.putNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS, inferred);
            } else {
                final int length = closureParams.length;
                for (int i = 0; i < length; i++) {
                    Parameter closureParam = closureParams[i];
                    final ClassNode originType = closureParam.getOriginType();
                    ClassNode inferredType = OBJECT_TYPE;
                    if (i<inferred.length-1 || inferred.length==closureParams.length) {
                        inferredType = inferred[i];
                    } else { // vargs?
                            ClassNode lastArgInferred = inferred[inferred.length-1];
                            if (lastArgInferred.isArray()) {
                                inferredType = lastArgInferred.getComponentType();
                            } else {
                                addError("Incorrect number of parameters. Expected "+inferred.length+" but found "+closureParams.length, expression);
                            }
                    }
                    boolean lastArg = i == length - 1;
                    if (lastArg && inferredType.isArray()) {
                        if (inferredType.getComponentType().equals(originType)) {
                            inferredType = originType;
                        }
                    } else if (!typeCheckMethodArgumentWithGenerics(originType, inferredType, lastArg)) {
                        addError("Expected parameter of type "+ inferredType.toString(false)+" but got "+originType.toString(false), closureParam.getType());
                    }
                    typeCheckingContext.controlStructureVariables.put(closureParam, inferredType);
                }
            }
        }
    }

    /**
     * Given a GenericsType instance, returns a ClassNode which can be used as an inferred type.
     * @param genericsType a {@link org.codehaus.groovy.ast.GenericsType} representing either a type, a placeholder or a wildcard
     * @return a class node usable as an inferred type
     */
    private static ClassNode createUsableClassNodeFromGenericsType(final GenericsType genericsType) {
        ClassNode value = genericsType.getType();
        if (genericsType.isPlaceholder()) {
            value = OBJECT_TYPE;
        }
        ClassNode lowerBound = genericsType.getLowerBound();
        if (lowerBound !=null) {
            value = lowerBound;
        } else {
            ClassNode[] upperBounds = genericsType.getUpperBounds();
            if (upperBounds !=null) {
                value = WideningCategories.lowestUpperBound(Arrays.asList(upperBounds));
            }
        }
        return value;
    }

    private static String[] convertToStringArray(final Expression options) {
        if (options==null) {
            return new String[0];
        }
        if (options instanceof ConstantExpression) {
            return new String[] { options.getText() };
        }
        if (options instanceof ListExpression) {
            List<Expression> list = ((ListExpression) options).getExpressions();
            List<String> result = new ArrayList<String>(list.size());
            for (Expression expression : list) {
                result.add(expression.getText());
            }
            return result.toArray(new String[result.size()]);
        }
        throw new IllegalArgumentException("Unexpected options for @ClosureParams:"+options);
    }

    private void checkClosureWithDelegatesTo(final ArgumentListExpression arguments, final Parameter[] params, final Expression expression, final Parameter param) {
        List<AnnotationNode> annotations = param.getAnnotations(DELEGATES_TO);
        if (annotations!=null && !annotations.isEmpty()) {
            for (AnnotationNode annotation : annotations) {
                // in theory, there can only be one annotation of that type
                Expression value = annotation.getMember("value");
                Expression strategy = annotation.getMember("strategy");
                Expression genericTypeIndex = annotation.getMember("genericTypeIndex");
                Integer stInt = Closure.OWNER_FIRST;
                if (strategy!=null) {
                    stInt = (Integer) evaluateExpression(new CastExpression(ClassHelper.Integer_TYPE,strategy), typeCheckingContext.source.getConfiguration());
                }
                if (value instanceof ClassExpression && !value.getType().equals(DELEGATES_TO_TARGET)) {
                    if (genericTypeIndex!=null) {
                        addStaticTypeError("Cannot use @DelegatesTo(genericTypeIndex="+genericTypeIndex.getText()
                                +") without @DelegatesTo.Target because generic argument types are not available at runtime", value);
                    }
                    // temporarily store the delegation strategy and the delegate type
                    expression.putNodeMetaData(StaticTypesMarker.DELEGATION_METADATA, new DelegationMetadata(value.getType(), stInt, typeCheckingContext.delegationMetadata));
                } else {
                    final List<Expression> expressions = arguments.getExpressions();
                    final int expressionsSize = expressions.size();
                    Expression parameter = annotation.getMember("target");
                    String parameterName = parameter!=null && parameter instanceof ConstantExpression ?parameter.getText():"";
                    // todo: handle vargs!
                    for (int j = 0, paramsLength = params.length; j < paramsLength; j++) {
                        final Parameter methodParam = params[j];
                        List<AnnotationNode> targets = methodParam.getAnnotations(DELEGATES_TO_TARGET);
                        if (targets != null && targets.size() == 1) {
                            AnnotationNode targetAnnotation = targets.get(0); // @DelegatesTo.Target Obj foo
                            Expression idMember = targetAnnotation.getMember("value");
                            String id = idMember != null && idMember instanceof ConstantExpression ? idMember.getText() : "";
                            if (id.equals(parameterName)) {
                                if (j < expressionsSize) {
                                    Expression actualArgument = expressions.get(j);
                                    ClassNode actualType = getType(actualArgument);
                                    if (genericTypeIndex!=null && genericTypeIndex instanceof ConstantExpression) {
                                        int gti = Integer.valueOf(genericTypeIndex.getText());
                                        ClassNode paramType = methodParam.getType(); // type annotated with @DelegatesTo.Target
                                        GenericsType[] genericsTypes = paramType.getGenericsTypes();
                                        if (genericsTypes==null) {
                                            addStaticTypeError("Cannot use @DelegatesTo(genericTypeIndex="+genericTypeIndex.getText()
                                                + ") with a type that doesn't use generics", methodParam);
                                        } else if (gti<0 || gti>=genericsTypes.length) {
                                            addStaticTypeError("Index of generic type @DelegatesTo(genericTypeIndex="+genericTypeIndex.getText()
                                                    + ") "+(gti<0?"lower":"greater")+" than those of the selected type", methodParam);
                                        } else {
                                            ClassNode pType = GenericsUtils.parameterizeType(actualType, paramType);
                                            GenericsType[] pTypeGenerics = pType.getGenericsTypes();
                                            if (pTypeGenerics!=null && pTypeGenerics.length>gti) {
                                                actualType = pTypeGenerics[gti].getType();
                                            } else {
                                                addStaticTypeError("Unable to map actual type ["+actualType.toString(false)+"] onto "+paramType.toString(false), methodParam);
                                            }
                                        }
                                    }
                                    expression.putNodeMetaData(StaticTypesMarker.DELEGATION_METADATA, new DelegationMetadata(actualType, stInt, typeCheckingContext.delegationMetadata));
                                    break;
                                }
                            }
                        }
                    }
                    if (expression.getNodeMetaData(StaticTypesMarker.DELEGATION_METADATA)==null) {
                        addError("Not enough arguments found for a @DelegatesTo method call. Please check that you either use an explicit class or @DelegatesTo.Target with a correct id", arguments);
                    }
                }
            }
        }
    }

    protected void addReceivers(final List<Receiver<String>> receivers,
                              final Collection<Receiver<String>> owners,
                              final boolean implicitThis) {
        if (typeCheckingContext.delegationMetadata ==null || !implicitThis) {
            receivers.addAll(owners);
            return;
        }

        DelegationMetadata dmd = typeCheckingContext.delegationMetadata;
        StringBuilder path = new StringBuilder();
        while (dmd != null) {
            int strategy = dmd.getStrategy();
            ClassNode delegate = dmd.getType();
            dmd = dmd.getParent();

            switch (strategy) {
                case Closure.OWNER_FIRST:
                    receivers.addAll(owners);
                    path.append("delegate");
                    receivers.add(new Receiver<String>(delegate, path.toString()));
                    break;
                case Closure.DELEGATE_FIRST:
                    path.append("delegate");
                    receivers.add(new Receiver<String>(delegate, path.toString()));
                    receivers.addAll(owners);
                    break;
                case Closure.OWNER_ONLY:
                    receivers.addAll(owners);
                    dmd = null;
                    break;
                case Closure.DELEGATE_ONLY:
                    path.append("delegate");
                    receivers.add(new Receiver<String>(delegate, path.toString()));
                    dmd = null;
                    break;
            }
            path.append('.');
        }
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
        final String name = call.getMethodAsString();
        if (name == null) {
            addStaticTypeError("cannot resolve dynamic method name at compile time.", call.getMethod());
            return;
        }
        if (extension.beforeMethodCall(call)) {
            extension.afterMethodCall(call);
            return;
        }
        typeCheckingContext.pushEnclosingMethodCall(call);
        final Expression objectExpression = call.getObjectExpression();

        objectExpression.visit(this);
        call.getMethod().visit(this);

        // if the call expression is a spread operator call, then we must make sure that
        // the call is made on a collection type
        if (call.isSpreadSafe()) {
            //TODO check if this should not be change to iterator based call logic
            ClassNode expressionType = getType(objectExpression);
            if (!implementsInterfaceOrIsSubclassOf(expressionType, Collection_TYPE) && !expressionType.isArray()) {
                addStaticTypeError("Spread operator can only be used on collection types", objectExpression);
                return;
            } else {
                // type check call as if it was made on component type
                ClassNode componentType = inferComponentType(expressionType, int_TYPE);
                MethodCallExpression subcall = new MethodCallExpression(
                        new CastExpression(componentType, EmptyExpression.INSTANCE),
                        name,
                        call.getArguments()
                );
                subcall.setLineNumber(call.getLineNumber());
                subcall.setColumnNumber(call.getColumnNumber());
                subcall.setImplicitThis(call.isImplicitThis());
                visitMethodCallExpression(subcall);
                // the inferred type here should be a list of what the subcall returns
                ClassNode subcallReturnType = getType(subcall);
                ClassNode listNode = LIST_TYPE.getPlainNodeReference();
                listNode.setGenericsTypes(new GenericsType[]{new GenericsType(wrapTypeIfNecessary(subcallReturnType))});
                storeType(call, listNode);
                // store target method
                storeTargetMethod(call, (MethodNode) subcall.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET));
                typeCheckingContext.popEnclosingMethodCall();
                return;
            }
        }

        final ClassNode rememberLastItType = typeCheckingContext.lastImplicitItType;
        Expression callArguments = call.getArguments();
        ArgumentListExpression argumentList = InvocationWriter.makeArgumentList(callArguments);

        checkForbiddenSpreadArgument(argumentList);

        // for arguments, we need to visit closures *after* the method has been chosen


        final ClassNode receiver = getType(objectExpression);
        visitMethodCallArguments(receiver, argumentList, false, null);

        ClassNode[] args = getArgumentTypes(argumentList);
        final boolean isCallOnClosure = isClosureCall(name, objectExpression, callArguments);

        try {
            boolean callArgsVisited = false;
            if (isCallOnClosure) {
                // this is a closure.call() call
                if (objectExpression == VariableExpression.THIS_EXPRESSION) {
                    // isClosureCall() check verified earlier that a field exists
                    FieldNode field = typeCheckingContext.getEnclosingClassNode().getDeclaredField(name);
                    GenericsType[] genericsTypes = field.getType().getGenericsTypes();
                    if (genericsTypes != null) {
                        ClassNode closureReturnType = genericsTypes[0].getType();
                        Object data = field.getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS);
                        if (data != null) {
                            Parameter[] parameters = (Parameter[]) data;
                            typeCheckClosureCall(callArguments, args, parameters);
                        }
                        storeType(call, closureReturnType);
                    }
                } else if (objectExpression instanceof VariableExpression) {
                    Variable variable = findTargetVariable((VariableExpression) objectExpression);
                    if (variable instanceof ASTNode) {
                        Object data = ((ASTNode) variable).getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS);
                        if (data != null) {
                            Parameter[] parameters = (Parameter[]) data;
                            typeCheckClosureCall(callArguments, args, parameters);
                        }
                        ClassNode type = getType(((ASTNode) variable));
                        if (type!=null && type.equals(CLOSURE_TYPE)) {
                            GenericsType[] genericsTypes = type.getGenericsTypes();
                            type = OBJECT_TYPE;
                            if (genericsTypes != null) {
                                if (!genericsTypes[0].isPlaceholder()) {
                                    type = genericsTypes[0].getType();
                                }
                            }
                        }
                        if (type != null) {
                            storeType(call, type);
                        }
                    }
                } else if (objectExpression instanceof ClosureExpression) {
                    // we can get actual parameters directly
                    Parameter[] parameters = ((ClosureExpression) objectExpression).getParameters();
                    typeCheckClosureCall(callArguments, args, parameters);
                    ClassNode data = getInferredReturnType(objectExpression);
                    if (data != null) {
                        storeType(call, data);
                    }
                }
                int nbOfArgs = 0;
                if (callArguments instanceof ArgumentListExpression) {
                    ArgumentListExpression list = (ArgumentListExpression) callArguments;
                    nbOfArgs = list.getExpressions().size();
                } else {
                    // todo : other cases
                    nbOfArgs = 0;
                }
                storeTargetMethod(call,
                        nbOfArgs == 0 ? CLOSURE_CALL_NO_ARG :
                                nbOfArgs == 1 ? CLOSURE_CALL_ONE_ARG :
                                        CLOSURE_CALL_VARGS);
            } else {
                // method call receivers are :
                //   - possible "with" receivers
                //   - the actual receiver as found in the method call expression
                //   - any of the potential receivers found in the instanceof temporary table
                // in that order
                List<Receiver<String>> receivers = new LinkedList<Receiver<String>>();
                List<Receiver<String>> owners = makeOwnerList(objectExpression);
                addReceivers(receivers, owners, call.isImplicitThis());
                List<MethodNode> mn = null;
                Receiver<String> chosenReceiver = null;
                for (Receiver<String> currentReceiver : receivers) {
                    mn = findMethod(currentReceiver.getType(), name, args);

                    // if the receiver is "this" or "implicit this", then we must make sure that the compatible
                    // methods are only static if we are in a static context
                    if (!mn.isEmpty() && typeCheckingContext.isInStaticContext && (call.isImplicitThis()
                            || (objectExpression instanceof VariableExpression && ((VariableExpression) objectExpression).isThisExpression()))) {
                        // we create a separate method list just to be able to print out
                        // a nice error message to the user
                        List<MethodNode> staticMethods = new LinkedList<MethodNode>();
                        List<MethodNode> nonStaticMethods = new LinkedList<MethodNode>();
                        for (final MethodNode node : mn) {
                            if (node.isStatic()) {
                                staticMethods.add(node);
                            } else {
                                nonStaticMethods.add(node);
                            }
                        }
                        mn = staticMethods;
                        if (staticMethods.isEmpty()) {
                            // choose an arbitrary method to display an error message
                            MethodNode node = nonStaticMethods.get(0);
                            ClassNode owner = node.getDeclaringClass();
                            addStaticTypeError("Non static method " + owner.getName() + "#" + node.getName() + " cannot be called from static context", call);
                        }
                    }

                    if (!mn.isEmpty()) {
                        chosenReceiver = currentReceiver;
                        break;
                    }
                }
                if (mn.isEmpty() && typeCheckingContext.getEnclosingClosure() != null && args.length == 0) {
                    // add special handling of getDelegate() and getOwner()
                    if ("getDelegate".equals(name)) {
                        mn = Collections.singletonList(GET_DELEGATE);
                    } else if ("getOwner".equals(name)) {
                        mn = Collections.singletonList(GET_OWNER);
                    } else if ("getThisObject".equals(name)) {
                        mn = Collections.singletonList(GET_THISOBJECT);
                    }
                }
                if (mn.isEmpty()) {
                    mn = extension.handleMissingMethod(receiver, name, argumentList, args, call);
                }
                if (mn.isEmpty()) {
                    addNoMatchingMethodError(receiver, name, args, call);
                } else {
                    if (areCategoryMethodCalls(mn, name, args)) {
                        addCategoryMethodCallError(call);
                    }
                    mn = disambiguateMethods(mn, chosenReceiver!=null?chosenReceiver.getType():null, args, call);
                    if (mn.size() == 1) {
                        MethodNode directMethodCallCandidate = mn.get(0);
                        if (chosenReceiver==null) {
                            chosenReceiver = Receiver.make(directMethodCallCandidate.getDeclaringClass());
                            if (chosenReceiver==null) {
                                chosenReceiver = owners.get(0);
                            }
                        }

                        ClassNode returnType = null;

                        if (returnType == null) {
                            returnType = getType(directMethodCallCandidate);
                        }

                        if (isUsingGenericsOrIsArrayUsingGenerics(returnType)) {
                            visitMethodCallArguments(chosenReceiver.getType(), argumentList, true, directMethodCallCandidate);
                            ClassNode irtg = inferReturnTypeGenerics(chosenReceiver.getType(), directMethodCallCandidate, callArguments);
                            returnType = irtg != null && implementsInterfaceOrIsSubclassOf(irtg, returnType) ? irtg : returnType;
                            callArgsVisited = true;
                        }
                        if (directMethodCallCandidate==GET_DELEGATE && typeCheckingContext.getEnclosingClosure()!=null) {
                            DelegationMetadata md = getDelegationMetadata(typeCheckingContext.getEnclosingClosure().getClosureExpression());
                            returnType = typeCheckingContext.getEnclosingClassNode();
                            if (md!=null) {
                                returnType = md.getType();
                            }
                        }
                        if (typeCheckMethodsWithGenericsOrFail(chosenReceiver.getType(), args, mn.get(0), call)) {
                            returnType = adjustWithTraits(directMethodCallCandidate,chosenReceiver.getType(), args, returnType);
                            storeType(call, returnType);
                            storeTargetMethod(call, directMethodCallCandidate);
                            String data = chosenReceiver != null ? chosenReceiver.getData() : null;
                            if (data != null) {
                                // the method which has been chosen is supposed to be a call on delegate or owner
                                // so we store the information so that the static compiler may reuse it
                                call.putNodeMetaData(StaticTypesMarker.IMPLICIT_RECEIVER, data);
                            }
                            // if the object expression is a closure shared variable, we will have to perform a second pass
                            if (objectExpression instanceof VariableExpression) {
                                VariableExpression var = (VariableExpression) objectExpression;
                                if (var.isClosureSharedVariable()) {
                                    SecondPassExpression<ClassNode[]> wrapper = new SecondPassExpression<ClassNode[]>(
                                            call,
                                            args
                                    );
                                    typeCheckingContext.secondPassExpressions.add(wrapper);
                                }
                            }
                        }
                    } else {
                        addAmbiguousErrorMessage(mn, name, args, call);
                    }
                }
            }
            // now that a method has been chosen, we are allowed to visit the closures
            if (!callArgsVisited) {
                MethodNode mn = (MethodNode) call.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                visitMethodCallArguments(receiver, argumentList, true, mn);
                // GROOVY-6219
                if (mn!=null) {
                    List<Expression> argExpressions = argumentList.getExpressions();
                    Parameter[] parameters = mn.getParameters();
                    for (int i = 0; i < argExpressions.size() && i< parameters.length; i++) {
                          Expression arg = argExpressions.get(i);
                          ClassNode pType = parameters[i].getType();
                          ClassNode aType = getType(arg);
                          if (CLOSURE_TYPE.equals(pType) && CLOSURE_TYPE.equals(aType)) {
                              if (!isAssignableTo(aType, pType)) {
                                  addNoMatchingMethodError(receiver, name, getArgumentTypes(argumentList), call);
                                  call.removeNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                              }
                          }
                    }
                }
            }
        } finally {
            typeCheckingContext.popEnclosingMethodCall();
            extension.afterMethodCall(call);
        }
    }

    /**
     * A special method handling the "withTrait" call for which the type checker knows more than
     * what the type signature is able to tell. If "withTrait" is detected, then a new class node
     * is created representing the list of trait interfaces.
     *
     * @param directMethodCallCandidate a method selected by the type checker
     * @param receiver the receiver of the method call
     *@param args the arguments of the method call
     * @param returnType the original return type, as inferred by the type checker   @return fixed return type if the selected method is {@link org.codehaus.groovy.runtime.DefaultGroovyMethods#withTraits(Object, Class[]) withTraits}
     */
    private ClassNode adjustWithTraits(final MethodNode directMethodCallCandidate, final ClassNode receiver, final ClassNode[] args, final ClassNode returnType) {
        if (directMethodCallCandidate instanceof ExtensionMethodNode) {
            ExtensionMethodNode emn = (ExtensionMethodNode) directMethodCallCandidate;
            if ("withTraits".equals(emn.getName()) && "DefaultGroovyMethods".equals(emn.getExtensionMethodNode().getDeclaringClass().getNameWithoutPackage())) {
                List<ClassNode> nodes = new LinkedList<ClassNode>();
                Collections.addAll(nodes, receiver.getInterfaces());
                for (ClassNode arg : args) {
                    if (isClassClassNodeWrappingConcreteType(arg)) {
                        nodes.add(arg.getGenericsTypes()[0].getType());
                    } else {
                        nodes.add(arg);
                    }
                }
                return new LowestUpperBoundClassNode(returnType.getName()+"Composed", OBJECT_TYPE, nodes.toArray(new ClassNode[nodes.size()]));
            }
        }
        return returnType;
    }

    /**
     * add various getAt and setAt methods for primitive arrays
     * @param receiver the receiver class
     * @param name  the name of the method
     * @param args the argument classes
     */
    private void addArrayMethods(List<MethodNode> methods, ClassNode receiver, String name, ClassNode[] args) {
        if (args.length!=1) return;
        if (!receiver.isArray()) return;
        if (!isIntCategory(args[0])) return;
        if ("getAt".equals(name)) {
            MethodNode node = new MethodNode(name, Opcodes.ACC_PUBLIC, receiver.getComponentType(), new Parameter[]{new Parameter(args[0],"arg")}, null, null);
            node.setDeclaringClass(receiver.redirect());
            methods.add(node);
        } else if ("setAt".equals(name)) {
            MethodNode node = new MethodNode(name, Opcodes.ACC_PUBLIC, VOID_TYPE, new Parameter[]{new Parameter(args[0],"arg")}, null, null);
            node.setDeclaringClass(receiver.redirect());
            methods.add(node);
        }
    }

    /**
     * In the case of a <em>Object.with { ... }</em> call, this method is supposed to retrieve
     * the inferred closure return type.
     *
     * @param callArguments the argument list from the <em>Object#with(Closure)</em> call, ie. a single closure expression
     * @return the inferred closure return type or <em>null</em>
     */
    protected ClassNode getInferredReturnTypeFromWithClosureArgument(Expression callArguments) {
        if (!(callArguments instanceof ArgumentListExpression)) return null;

        ArgumentListExpression argList = (ArgumentListExpression) callArguments;
        ClosureExpression closure = (ClosureExpression) argList.getExpression(0);

        visitClosureExpression(closure);

        if (getInferredReturnType(closure) != null)  {
            return getInferredReturnType(closure);
        }

        return null;
    }

    /**
     * Given an object expression (a receiver expression), generate the list of potential receiver types.
     * @param objectExpression the receiver expression
     * @return the list of types the receiver may be
     */
    protected List<Receiver<String>> makeOwnerList(final Expression objectExpression) {
        final ClassNode receiver = getType(objectExpression);
        List<Receiver<String>> owners = new LinkedList<Receiver<String>>();
        owners.add(Receiver.<String>make(receiver));
        if (isClassClassNodeWrappingConcreteType(receiver)) {
            GenericsType clazzGT = receiver.getGenericsTypes()[0];
            owners.add(0,Receiver.<String>make(clazzGT.getType()));
        }
        if (receiver.isInterface()) {
            // GROOVY-xxxx
            owners.add(Receiver.<String>make(OBJECT_TYPE));
        }
        if (!typeCheckingContext.temporaryIfBranchTypeInformation.empty()) {
            List<ClassNode> potentialReceiverType = getTemporaryTypesForExpression(objectExpression);
            if (potentialReceiverType != null) {
                for (ClassNode node : potentialReceiverType) {
                    owners.add(Receiver.<String>make(node));
                }
            }
        }
        if (typeCheckingContext.lastImplicitItType != null
                && objectExpression instanceof VariableExpression
                && ((VariableExpression) objectExpression).getName().equals("it")) {
            owners.add(Receiver.<String>make(typeCheckingContext.lastImplicitItType));
        }
        return owners;
    }

    protected void checkForbiddenSpreadArgument(ArgumentListExpression argumentList) {
        for (Expression arg : argumentList.getExpressions()) {
            if (arg instanceof SpreadExpression) {
                addStaticTypeError("The spread operator cannot be used as argument of method or closure calls with static type checking because the number of arguments cannot be determined at compile time", arg);
            }
        }
    }

    protected List<ClassNode> getTemporaryTypesForExpression(final Expression objectExpression) {
        List<ClassNode> classNodes = null;
        int depth = typeCheckingContext.temporaryIfBranchTypeInformation.size();
        while (classNodes == null && depth > 0) {
            final Map<Object, List<ClassNode>> tempo = typeCheckingContext.temporaryIfBranchTypeInformation.get(--depth);
            Object key = extractTemporaryTypeInfoKey(objectExpression);
            classNodes = tempo.get(key);
        }
        return classNodes;
    }

    protected void storeTargetMethod(final Expression call, final MethodNode directMethodCallCandidate) {
        call.putNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, directMethodCallCandidate);
        checkOrMarkPrivateAccess(directMethodCallCandidate);
        extension.onMethodSelection(call, directMethodCallCandidate);
    }

    protected boolean isClosureCall(final String name, final Expression objectExpression, final Expression arguments) {
        if (objectExpression instanceof ClosureExpression && ("call".equals(name)||"doCall".equals(name))) return true;
        if (objectExpression == VariableExpression.THIS_EXPRESSION) {
            FieldNode fieldNode = typeCheckingContext.getEnclosingClassNode().getDeclaredField(name);
            if (fieldNode != null) {
                ClassNode type = fieldNode.getType();
                if (CLOSURE_TYPE.equals(type) && !typeCheckingContext.getEnclosingClassNode().hasPossibleMethod(name, arguments)) {
                    return true;
                }
            }
        } else {
            if (!"call".equals(name) && !"doCall".equals(name)) return false;
        }
        return (getType(objectExpression).equals(CLOSURE_TYPE));
    }

    protected void typeCheckClosureCall(final Expression callArguments, final ClassNode[] args, final Parameter[] parameters) {
        if (allParametersAndArgumentsMatch(parameters, args) < 0 &&
                lastArgMatchesVarg(parameters, args) < 0) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                final Parameter parameter = parameters[i];
                sb.append(parameter.getType().getName());
                if (i < parametersLength - 1) sb.append(", ");
            }
            sb.append("]");
            addStaticTypeError("Closure argument types: " + sb + " do not match with parameter types: " + formatArgumentList(args), callArguments);
        }
    }

    @Override
    public void visitIfElse(final IfStatement ifElse) {
        Map<VariableExpression, List<ClassNode>> oldTracker = pushAssignmentTracking();

        try {
            // create a new temporary element in the if-then-else type info
            typeCheckingContext.pushTemporaryTypeInfo();
            visitStatement(ifElse);
            ifElse.getBooleanExpression().visit(this);
            ifElse.getIfBlock().visit(this);

            // pop if-then-else temporary type info
            typeCheckingContext.popTemporaryTypeInfo();

            // GROOVY-6099: restore assignment info as before the if branch
            restoreTypeBeforeConditional();

            Statement elseBlock = ifElse.getElseBlock();
            if (elseBlock instanceof EmptyStatement) {
                // dispatching to EmptyStatement will not call back visitor,
                // must call our visitEmptyStatement explicitly
                visitEmptyStatement((EmptyStatement) elseBlock);
            } else {
                elseBlock.visit(this);
            }
        } finally {
            popAssignmentTracking(oldTracker);
        }
    }

    @Override
    public void visitSwitch(final SwitchStatement statement) {
        Map<VariableExpression, List<ClassNode>> oldTracker = pushAssignmentTracking();
        try {
            super.visitSwitch(statement);
        } finally {
            popAssignmentTracking(oldTracker);
        }
    }

    @Override
    public void visitCaseStatement(final CaseStatement statement) {
        super.visitCaseStatement(statement);
        restoreTypeBeforeConditional();
    }

    private void restoreTypeBeforeConditional() {
        Set<Map.Entry<VariableExpression, List<ClassNode>>> entries = typeCheckingContext.ifElseForWhileAssignmentTracker.entrySet();
        for (Map.Entry<VariableExpression, List<ClassNode>> entry : entries) {
            VariableExpression var = entry.getKey();
            List<ClassNode> items = entry.getValue();
            ClassNode originValue = items.get(0);
            storeType(var, originValue);
        }
    }

    protected Map<VariableExpression, ClassNode> popAssignmentTracking(final Map<VariableExpression, List<ClassNode>> oldTracker) {
        Map<VariableExpression, ClassNode> assignments = new HashMap<VariableExpression, ClassNode>();
        if (!typeCheckingContext.ifElseForWhileAssignmentTracker.isEmpty()) {
            for (Map.Entry<VariableExpression, List<ClassNode>> entry : typeCheckingContext.ifElseForWhileAssignmentTracker.entrySet()) {
                VariableExpression key = entry.getKey();
                List<ClassNode> allValues = entry.getValue();
                // GROOVY-6099: First element of the list may be null, if no assignment was made before the branch
                List<ClassNode> nonNullValues = new ArrayList<ClassNode>(allValues.size());
                for (ClassNode value : allValues) {
                    if (value!=null) nonNullValues.add(value);
                }
                ClassNode cn = lowestUpperBound(nonNullValues);
                storeType(key, cn);
                assignments.put(key, cn);
            }
        }
        typeCheckingContext.ifElseForWhileAssignmentTracker = oldTracker;
        return assignments;
    }

    protected Map<VariableExpression, List<ClassNode>> pushAssignmentTracking() {
        // memorize current assignment context
        Map<VariableExpression, List<ClassNode>> oldTracker = typeCheckingContext.ifElseForWhileAssignmentTracker;
        typeCheckingContext.ifElseForWhileAssignmentTracker = new HashMap<VariableExpression, List<ClassNode>>();
        return oldTracker;
    }

    @Override
    public void visitCastExpression(final CastExpression expression) {
        super.visitCastExpression(expression);
        if (!expression.isCoerce()) {
            ClassNode targetType = expression.getType();
            Expression source = expression.getExpression();
            ClassNode expressionType = getType(source);
            if (!checkCast(targetType, source) && !isDelegateOrOwnerInClosure(source)) {
                addStaticTypeError("Inconvertible types: cannot cast " + expressionType.toString(false) + " to " + targetType.toString(false), expression);
            }
        }
        storeType(expression, expression.getType());
    }

    private boolean isDelegateOrOwnerInClosure(Expression exp) {
        return typeCheckingContext.getEnclosingClosure()!=null &&
                exp instanceof VariableExpression &&
                (("delegate".equals(((VariableExpression) exp).getName())) || ("owner".equals(((VariableExpression) exp).getName())));
    }

    protected boolean checkCast(final ClassNode targetType, final Expression source) {
        boolean sourceIsNull = isNullConstant(source);
        ClassNode expressionType = getType(source);
        if (targetType.isArray() && expressionType.isArray()) {
            return checkCast(targetType.getComponentType(), new VariableExpression("foo", expressionType.getComponentType()));
        } else if (targetType.equals(char_TYPE) && expressionType == STRING_TYPE
                && source instanceof ConstantExpression && source.getText().length() == 1) {
            // ex: (char) 'c'
        } else if (targetType.equals(Character_TYPE) && (expressionType == STRING_TYPE || sourceIsNull)
                && (sourceIsNull || source instanceof ConstantExpression && source.getText().length() == 1)) {
            // ex : (Character) 'c'
        } else if (isNumberCategory(getWrapper(targetType)) && (isNumberCategory(getWrapper(expressionType)) || char_TYPE == expressionType)) {
            // ex: short s = (short) 0
        } else if (sourceIsNull && !isPrimitiveType(targetType)) {
            // ex: (Date)null
        } else if (char_TYPE == targetType && isPrimitiveType(expressionType) && isNumberType(expressionType)) {
            // char c = (char) ...
        } else if (sourceIsNull && isPrimitiveType(targetType) && !boolean_TYPE.equals(targetType)) {
            return false;
        } else if ((expressionType.getModifiers()&Opcodes.ACC_FINAL)==0 && targetType.isInterface()) {
            return true;
        } else if (!isAssignableTo(targetType, expressionType) && !implementsInterfaceOrIsSubclassOf(expressionType, targetType)) {
            return false;
        }
        return true;
    }

    @Override
    public void visitTernaryExpression(final TernaryExpression expression) {
        Map<VariableExpression, List<ClassNode>> oldTracker = pushAssignmentTracking();
        // create a new temporary element in the if-then-else type info
        typeCheckingContext.pushTemporaryTypeInfo();
        expression.getBooleanExpression().visit(this);
        Expression trueExpression = expression.getTrueExpression();
        Expression falseExpression = expression.getFalseExpression();
        trueExpression.visit(this);
        // pop if-then-else temporary type info
        typeCheckingContext.popTemporaryTypeInfo();
        falseExpression.visit(this);
        ClassNode resultType;
        if (isNullConstant(trueExpression) || isNullConstant(falseExpression)) {
            BinaryExpression enclosingBinaryExpression = typeCheckingContext.getEnclosingBinaryExpression();
            if (enclosingBinaryExpression != null && enclosingBinaryExpression.getRightExpression()==expression) {
                resultType = getType(enclosingBinaryExpression.getLeftExpression());
            } else if (isNullConstant(trueExpression) && isNullConstant(falseExpression)) {
                resultType = OBJECT_TYPE;
            } else if (isNullConstant(trueExpression)) {
                resultType = wrapTypeIfNecessary(getType(falseExpression));
            } else {
                resultType = wrapTypeIfNecessary(getType(trueExpression));
            }
        } else {
            // store type information
            final ClassNode typeOfTrue = getType(trueExpression);
            final ClassNode typeOfFalse = getType(falseExpression);
            resultType = lowestUpperBound(typeOfTrue, typeOfFalse);
        }
        storeType(expression, resultType);
        popAssignmentTracking(oldTracker);
    }

    @Override
    public void visitTryCatchFinally(final TryCatchStatement statement) {
        final List<CatchStatement> catchStatements = statement.getCatchStatements();
        for (CatchStatement catchStatement : catchStatements) {
            ClassNode exceptionType = catchStatement.getExceptionType();
            typeCheckingContext.controlStructureVariables.put(catchStatement.getVariable(), exceptionType);
        }
        try {
            super.visitTryCatchFinally(statement);
        } finally {
            for (CatchStatement catchStatement : catchStatements) {
                typeCheckingContext.controlStructureVariables.remove(catchStatement.getVariable());
            }
        }

    }

    protected void storeType(Expression exp, ClassNode cn) {
        if (exp instanceof VariableExpression && ((VariableExpression) exp).isClosureSharedVariable() && isPrimitiveType(cn)) {
            cn = getWrapper(cn);
        } else if (exp instanceof MethodCallExpression && ((MethodCallExpression) exp).isSafe() && isPrimitiveType(cn)) {
            cn = getWrapper(cn);
        }
        if (cn == UNKNOWN_PARAMETER_TYPE) {
            // this can happen for example when "null" is used in an assignment or a method parameter.
            // In that case, instead of storing the virtual type, we must "reset" type information
            // by determining the declaration type of the expression
            storeType(exp, getOriginalDeclarationType(exp));
            return;
        }
        ClassNode oldValue = (ClassNode) exp.putNodeMetaData(StaticTypesMarker.INFERRED_TYPE, cn);
        if (oldValue != null) {
            // this may happen when a variable declaration type is wider than the subsequent assignment values
            // for example :
            // def o = 1 // first, an int
            // o = 'String' // then a string
            // o = new Object() // and eventually an object !
            // in that case, the INFERRED_TYPE corresponds to the current inferred type, while
            // DECLARATION_INFERRED_TYPE is the type which should be used for the initial type declaration
            ClassNode oldDIT = (ClassNode) exp.getNodeMetaData(StaticTypesMarker.DECLARATION_INFERRED_TYPE);
            if (oldDIT != null) {
                exp.putNodeMetaData(StaticTypesMarker.DECLARATION_INFERRED_TYPE, cn==null?oldDIT : lowestUpperBound(oldDIT, cn));
            } else {
                exp.putNodeMetaData(StaticTypesMarker.DECLARATION_INFERRED_TYPE, cn==null?null : lowestUpperBound(oldValue, cn));
            }
        }
        if (exp instanceof VariableExpression) {
            VariableExpression var = (VariableExpression) exp;
            final Variable accessedVariable = var.getAccessedVariable();
            if (accessedVariable != null && accessedVariable != exp && accessedVariable instanceof VariableExpression) {
                storeType((Expression) accessedVariable, cn);
            }
            if (var.isClosureSharedVariable() && cn!=null) {
                List<ClassNode> assignedTypes = typeCheckingContext.closureSharedVariablesAssignmentTypes.get(var);
                if (assignedTypes == null) {
                    assignedTypes = new LinkedList<ClassNode>();
                    typeCheckingContext.closureSharedVariablesAssignmentTypes.put(var, assignedTypes);
                }
                assignedTypes.add(cn);
            }
            if (!typeCheckingContext.temporaryIfBranchTypeInformation.empty()) {
                List<ClassNode> temporaryTypesForExpression = getTemporaryTypesForExpression(exp);
                if (temporaryTypesForExpression != null && !temporaryTypesForExpression.isEmpty()) {
                    // a type inference has been made on a variable which type was defined in an instanceof block
                    // we erase available information with the new type
                    temporaryTypesForExpression.clear();
                }
            }
        }
    }

    protected ClassNode getResultType(ClassNode left, int op, ClassNode right, BinaryExpression expr) {
        ClassNode leftRedirect = left.redirect();
        ClassNode rightRedirect = right.redirect();

        Expression leftExpression = expr.getLeftExpression();
        Expression rightExpression = expr.getRightExpression();
        if (op == ASSIGN || op == ASSIGNMENT_OPERATOR) {
            if (leftRedirect.isArray() && implementsInterfaceOrIsSubclassOf(rightRedirect, Collection_TYPE)) return leftRedirect;
            if (leftRedirect.implementsInterface(Collection_TYPE) && rightRedirect.implementsInterface(Collection_TYPE)) {
                // because of type inferrence, we must perform an additional check if the right expression
                // is an empty list expression ([]). In that case and only in that case, the inferred type
                // will be wrong, so we will prefer the left type
                if (rightExpression instanceof ListExpression) {
                    List<Expression> list = ((ListExpression) rightExpression).getExpressions();
                    if (list.isEmpty()) return left;
                }
                return right;
            }
            if (rightRedirect.implementsInterface(Collection_TYPE) && rightRedirect.isDerivedFrom(leftRedirect)) {
                // ex : def foos = ['a','b','c']
                return right;
            }
            if (rightRedirect.isDerivedFrom(CLOSURE_TYPE) && isSAMType(leftRedirect) && rightExpression instanceof ClosureExpression) {
                return inferSAMTypeGenericsInAssignment(left, findSAM(left),right,(ClosureExpression) rightExpression);
            }

            if (leftExpression instanceof VariableExpression) {
                ClassNode initialType = getOriginalDeclarationType(leftExpression).redirect();
                if (isPrimitiveType(right) && initialType.isDerivedFrom(Number_TYPE)) {
                    return getWrapper(right);
                }

                if (isPrimitiveType(initialType) && rightRedirect.isDerivedFrom(Number_TYPE))  {
                    return getUnwrapper(right);
                }

                // as anything can be assigned to a String, Class or boolean, return the left type instead
                if (STRING_TYPE.equals(initialType)
                        || CLASS_Type.equals(initialType)
                        || Boolean_TYPE.equals(initialType)
                        || boolean_TYPE.equals(initialType)) {
                    return initialType;
                }
            }
            return right;
        } else if (isBoolIntrinsicOp(op)) {
            return boolean_TYPE;
        } else if (isArrayOp(op)) {
            // using getPNR() to ignore generics at this point
            // and a different binary expression not to pollute the AST
            BinaryExpression newExpr = new BinaryExpression(
                    expr.getLeftExpression(),
                    expr.getOperation(),
                    rightExpression
            );
            newExpr.setSourcePosition(expr);
            MethodNode method = findMethodOrFail(newExpr, left.getPlainNodeReference(), "getAt", right.getPlainNodeReference());
            if (method!=null && implementsInterfaceOrIsSubclassOf(right, RANGE_TYPE)) {
                return inferReturnTypeGenerics(left, method, rightExpression);
            }
            return method!=null?inferComponentType(left, right):null;
        } else if (op == FIND_REGEX) {
            // this case always succeeds the result is a Matcher
            return Matcher_TYPE;
        }
        // the left operand is determining the result of the operation
        // for primitives and their wrapper we use a fixed table here
        else if (isNumberType(leftRedirect) && isNumberType(rightRedirect)) {
            if (isOperationInGroup(op)) {
                if (isIntCategory(leftRedirect) && isIntCategory(rightRedirect)) return int_TYPE;
                if (isLongCategory(leftRedirect) && isLongCategory(rightRedirect)) return long_TYPE;
                if (isFloat(leftRedirect) && isFloat(rightRedirect)) return float_TYPE;
                if (isDouble(leftRedirect) && isDouble(rightRedirect)) return double_TYPE;
            } else if (isPowerOperator(op)) {
                return Number_TYPE;
            } else if (isBitOperator(op)) {
                if (isIntCategory(leftRedirect) && isIntCategory(rightRedirect)) return int_TYPE;
                if (isLongCategory(leftRedirect) && isLongCategory(rightRedirect)) return long_TYPE;
                if (isBigIntCategory(leftRedirect) && isBigIntCategory(rightRedirect)) return BigInteger_TYPE;
            } else if (isCompareToBoolean(op) || op == COMPARE_EQUAL || op == COMPARE_NOT_EQUAL) {
                return boolean_TYPE;
            }
        }


        // try to find a method for the operation
        String operationName = getOperationName(op);
        if (isShiftOperation(operationName) && isNumberCategory(leftRedirect) && (isIntCategory(rightRedirect) || isLongCategory(rightRedirect))) {
            return leftRedirect;
        }

        // Divisions may produce different results depending on operand types
        if (isNumberCategory(getWrapper(rightRedirect)) && (isNumberCategory(getWrapper(leftRedirect)) && (DIVIDE == op || DIVIDE_EQUAL == op))) {
            if (isFloatingCategory(leftRedirect) || isFloatingCategory(rightRedirect)) {
                if (!isPrimitiveType(leftRedirect) || !isPrimitiveType(rightRedirect)) {
                    return Double_TYPE;
                }
                return double_TYPE;
            }
            if (DIVIDE == op) {
                return BigDecimal_TYPE;
            }
            return leftRedirect;
        } else if (isOperationInGroup(op)) {
            if (isNumberCategory(getWrapper(leftRedirect)) && isNumberCategory(getWrapper(rightRedirect))) {
                return getGroupOperationResultType(leftRedirect, rightRedirect);
            }
        }
        if (isNumberCategory(getWrapper(rightRedirect)) && isNumberCategory(getWrapper(leftRedirect)) && (MOD == op || MOD_EQUAL == op)) {
            return leftRedirect;
        }

        // GROOVY-5890
        // do not mix Class<Foo> with Foo
        if (leftExpression instanceof ClassExpression) {
            left = CLASS_Type.getPlainNodeReference();
        }

        MethodNode method = findMethodOrFail(expr, left, operationName, right);
        if (method != null) {
            storeTargetMethod(expr, method);
            typeCheckMethodsWithGenericsOrFail(left, new ClassNode[]{right}, method, expr);
            if (isAssignment(op)) return left;
            if (isCompareToBoolean(op)) return boolean_TYPE;
            if (op == COMPARE_TO) return int_TYPE;
            return inferReturnTypeGenerics(left, method, new ArgumentListExpression(rightExpression));
        }
        //TODO: other cases
        return null;
    }

    private ClassNode inferSAMTypeGenericsInAssignment(ClassNode samUsage, MethodNode sam, ClassNode closureType, ClosureExpression closureExpression) {
        // if the sam type or closure type do not provide generics information, 
        // we cannot infer anything, thus we simply return the provided samUsage
        GenericsType[] samGt = samUsage.getGenericsTypes();
        GenericsType[] closureGt = closureType.getGenericsTypes();
        if (samGt==null || closureGt==null) return samUsage;

        // extract the generics from the return type
        Map<String,GenericsType> connections = new HashMap<String,GenericsType>();
        extractGenericsConnections(connections,closureType,sam.getReturnType());

        // next we get the block parameter types and set the generics 
        // information just like before
        // TODO: add vargs handling
        Parameter[] closureParams = closureExpression.getParameters();
        Parameter[] methodParams = sam.getParameters();
        for (int i=0; i<closureParams.length; i++) {
            ClassNode fromClosure = closureParams[i].getType();
            ClassNode fromMethod = methodParams[i].getType();
            extractGenericsConnections(connections,fromClosure,fromMethod);
        }
        ClassNode result = applyGenericsContext(connections, samUsage.redirect());
        return result;
    }

    protected static ClassNode getGroupOperationResultType(ClassNode a, ClassNode b) {
        if (isBigIntCategory(a) && isBigIntCategory(b)) return BigInteger_TYPE;
        if (isBigDecCategory(a) && isBigDecCategory(b)) return BigDecimal_TYPE;
        if (BigDecimal_TYPE.equals(a) || BigDecimal_TYPE.equals(b)) return BigDecimal_TYPE;
        if (BigInteger_TYPE.equals(a) || BigInteger_TYPE.equals(b)) {
            if (isBigIntCategory(a) && isBigIntCategory(b)) return BigInteger_TYPE;
            return BigDecimal_TYPE;
        }
        if (double_TYPE.equals(a) || double_TYPE.equals(b)) return double_TYPE;
        if (Double_TYPE.equals(a) || Double_TYPE.equals(b)) return Double_TYPE;
        if (float_TYPE.equals(a) || float_TYPE.equals(b)) return float_TYPE;
        if (Float_TYPE.equals(a) || Float_TYPE.equals(b)) return Float_TYPE;
        if (long_TYPE.equals(a) || long_TYPE.equals(b)) return long_TYPE;
        if (Long_TYPE.equals(a) || Long_TYPE.equals(b)) return Long_TYPE;
        if (int_TYPE.equals(a) || int_TYPE.equals(b)) return int_TYPE;
        if (Integer_TYPE.equals(a) || Integer_TYPE.equals(b)) return Integer_TYPE;
        if (short_TYPE.equals(a) || short_TYPE.equals(b)) return short_TYPE;
        if (Short_TYPE.equals(a) || Short_TYPE.equals(b)) return Short_TYPE;
        if (byte_TYPE.equals(a) || byte_TYPE.equals(b)) return byte_TYPE;
        if (Byte_TYPE.equals(a) || Byte_TYPE.equals(b)) return Byte_TYPE;
        if (char_TYPE.equals(a) || char_TYPE.equals(b)) return char_TYPE;
        if (Character_TYPE.equals(a) || Character_TYPE.equals(b)) return Character_TYPE;
        return Number_TYPE;
    }

    protected ClassNode inferComponentType(final ClassNode containerType, final ClassNode indexType) {
        final ClassNode componentType = containerType.getComponentType();
        if (componentType == null) {
            // GROOVY-5521
            // try to identify a getAt method
            typeCheckingContext.pushErrorCollector();
            MethodCallExpression vcall = new MethodCallExpression(new VariableExpression("_hash_", containerType), "getAt", new VariableExpression("_index_", indexType));
            try {
                visitMethodCallExpression(vcall);
            } finally {
                typeCheckingContext.popErrorCollector();
            }
            return getType(vcall);
        } else {
            return componentType;
        }
    }

    protected MethodNode findMethodOrFail(
            Expression expr,
            ClassNode receiver, String name, ClassNode... args) {
        List<MethodNode> methods = findMethod(receiver, name, args);
        if (methods.isEmpty() && (expr instanceof BinaryExpression)) {
            BinaryExpression be = (BinaryExpression) expr;
            MethodCallExpression call = new MethodCallExpression(
                    be.getLeftExpression(),
                    name,
                    be.getRightExpression()
            );
            methods = extension.handleMissingMethod(receiver, name, new ArgumentListExpression(be.getLeftExpression()), args, call);
        }
        if (methods.isEmpty()) {
            addNoMatchingMethodError(receiver, name, args, expr);
        } else {
            if (areCategoryMethodCalls(methods, name, args)) {
                addCategoryMethodCallError(expr);
            }
            methods = disambiguateMethods(methods, receiver, args, expr);
            if (methods.size() == 1) {
                return methods.get(0);
            } else {
                addAmbiguousErrorMessage(methods, name, args, expr);
            }
        }
        return null;
    }

    private List<MethodNode> disambiguateMethods(List<MethodNode> methods, ClassNode receiver, ClassNode[] argTypes, final Expression expr) {
        if (methods.size()>1 && receiver!=null && argTypes!=null) {
            List<MethodNode> filteredWithGenerics = new LinkedList<MethodNode>();
            for (MethodNode methodNode : methods) {
                if (typeCheckMethodsWithGenerics(receiver, argTypes, methodNode)) {
                    filteredWithGenerics.add(methodNode);
                }
            }
            if (filteredWithGenerics.size()==1) {
                return filteredWithGenerics;
            }
            methods = extension.handleAmbiguousMethods(methods, expr);
        }
        return methods;
    }

    protected static String prettyPrintMethodList(List<MethodNode> nodes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0, nodesSize = nodes.size(); i < nodesSize; i++) {
            final MethodNode node = nodes.get(i);
            sb.append(node.getReturnType().toString(false));
            sb.append(" ");
            sb.append(node.getDeclaringClass().toString(false));
            sb.append("#");
            sb.append(toMethodParametersString(node.getName(), extractTypesFromParameters(node.getParameters())));
            if (i<nodesSize-1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    protected boolean areCategoryMethodCalls(final List<MethodNode> foundMethods, final String name, final ClassNode[] args) {
        boolean category = false;
        if ("use".equals(name) && args != null && args.length == 2 && args[1].equals(ClassHelper.CLOSURE_TYPE)) {
            category = true;
            for (MethodNode method : foundMethods) {
                if (!(method instanceof ExtensionMethodNode) || !((ExtensionMethodNode) method).getExtensionMethodNode().getDeclaringClass().equals(DGM_CLASSNODE)) {
                    category = false;
                    break;
                }
            }
        }
        return category;
    }

    /**
     * This method returns the list of methods named against the supplied parameter that
     * are defined on the specified receiver, but it will also add "non existing" methods
     * that will be generated afterwards by the compiler, for example if a method is using
     * default values and that the specified class node isn't compiled yet.
     * @param receiver the receiver where to find methods
     * @param name the name of the methods to return
     * @return the methods that are defined on the receiver completed with stubs for future methods
     */
    protected List<MethodNode> findMethodsWithGenerated(ClassNode receiver, String name) {
        List<MethodNode> methods = receiver.getMethods(name);
        if (methods.isEmpty() || receiver.isResolved()) return methods;
        List<MethodNode> result = addGeneratedMethods(receiver, methods);

        return result;
    }

    private List<MethodNode> addGeneratedMethods(final ClassNode receiver, final List<MethodNode> methods) {
        // using a comparator of parameters
        List<MethodNode> result = new LinkedList<MethodNode>();
        for (MethodNode method : methods) {
            result.add(method);
            Parameter[] parameters = method.getParameters();
            int counter = 0;
            int size = parameters.length;
            for (int i = size - 1; i >= 0; i--) {
                Parameter parameter = parameters[i];
                if (parameter != null && parameter.hasInitialExpression()) {
                    counter++;
                }
            }

            for (int j = 1; j <= counter; j++) {
                Parameter[] newParams = new Parameter[parameters.length - j];
                int index = 0;
                int k = 1;
                for (int i = 0; i < parameters.length; i++) {
                    if (k > counter - j && parameters[i] != null && parameters[i].hasInitialExpression()) {
                        k++;
                    } else if (parameters[i] != null && parameters[i].hasInitialExpression()) {
                        newParams[index++] = parameters[i];
                        k++;
                    } else {
                        newParams[index++] = parameters[i];
                    }
                }
                MethodNode stubbed;
                if ("<init>".equals(method.getName())) {
                    stubbed= new ConstructorNode(
                            method.getModifiers(),
                            newParams,
                            method.getExceptions(),
                            GENERATED_EMPTY_STATEMENT
                    );

                } else {
                    stubbed= new MethodNode(
                            method.getName(),
                            method.getModifiers(),
                            method.getReturnType(),
                            newParams,
                            method.getExceptions(),
                            GENERATED_EMPTY_STATEMENT
                    );
                }
                stubbed.setDeclaringClass(receiver);
                result.add(stubbed);
            }
        }
        return result;
    }

    protected List<MethodNode> findMethod(
            ClassNode receiver, String name, ClassNode... args) {
        if (isPrimitiveType(receiver)) receiver = getWrapper(receiver);
        List<MethodNode> methods;
        if (!receiver.isInterface() && "<init>".equals(name)) {
            methods = addGeneratedMethods(receiver,new ArrayList<MethodNode>(receiver.getDeclaredConstructors()));
            if (methods.isEmpty()) {
                MethodNode node = new ConstructorNode(Opcodes.ACC_PUBLIC, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, GENERATED_EMPTY_STATEMENT);
                node.setDeclaringClass(receiver);
                return Collections.singletonList(node);
            }
        } else {
            methods = findMethodsWithGenerated(receiver,name);
            if (receiver.isInterface()) {
                collectAllInterfaceMethodsByName(receiver, name, methods);
                methods.addAll(OBJECT_TYPE.getMethods(name));
            }
            if (typeCheckingContext.getEnclosingClosure() == null) {
                // not in a closure
                ClassNode parent = receiver;
                while (parent instanceof InnerClassNode && !parent.isStaticClass()) {
                    parent = parent.getOuterClass();
                    methods.addAll(findMethodsWithGenerated(parent,name));
                }
            }
            if (methods.isEmpty()) {
                addArrayMethods(methods, receiver, name, args);
            }
            if (methods.isEmpty() && (args == null || args.length == 0)) {
                // check if it's a property
                String pname = extractPropertyNameFromMethodName("get", name);
                if (pname==null) {
                    pname = extractPropertyNameFromMethodName("is", name);
                }
                if (pname != null) {
                    // we don't use property exists there because findMethod is called on super clases recursively
                    PropertyNode property = null;
                    ClassNode curNode = receiver;
                    while (property == null && curNode != null) {
                        property = curNode.getProperty(pname);
                        ClassNode svCur = curNode;
                        while (property==null && svCur instanceof InnerClassNode && !svCur.isStaticClass()) {
                            svCur = svCur.getOuterClass();
                            property = svCur.getProperty(pname);
                            if (property!=null) {
                                receiver = svCur;
                                break;
                            }
                        }
                        curNode = curNode.getSuperClass();
                    }
                    if (property != null) {
                        MethodNode node = new MethodNode(name, Opcodes.ACC_PUBLIC, property.getType(), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, GENERATED_EMPTY_STATEMENT);
                        if (property.isStatic()) {
                            node.setModifiers(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC);
                        }
                        node.setDeclaringClass(receiver);
                        return Collections.singletonList(
                                node);

                    }
                }
            } else if (methods.isEmpty() && args != null && args.length == 1) {
                // maybe we are looking for a setter ?
                String pname = extractPropertyNameFromMethodName("set", name);
                if (pname!=null) {
                    ClassNode curNode = receiver;
                    PropertyNode property = null;
                    while (property == null && curNode != null) {
                        property = curNode.getProperty(pname);
                        curNode = curNode.getSuperClass();
                    }
                    if (property != null) {
                        ClassNode type = property.getOriginType();
                        if (implementsInterfaceOrIsSubclassOf(wrapTypeIfNecessary(args[0]), wrapTypeIfNecessary(type))) {
                            MethodNode node = new MethodNode(name, Opcodes.ACC_PUBLIC, VOID_TYPE, new Parameter[]{
                                    new Parameter(type, "arg")
                            }, ClassNode.EMPTY_ARRAY, GENERATED_EMPTY_STATEMENT);
                            if (property.isStatic()) {
                                node.setModifiers(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC);
                            }
                            node.setDeclaringClass(receiver);
                            return Collections.singletonList(node);
                        }
                    }
                }
            }
        }

        if (methods.isEmpty()) {
            // look at the interfaces, there's a chance that a method is not implemented and we should not hide the
            // error from the compiler
            collectAllInterfaceMethodsByName(receiver, name, methods);
        }

        // lookup in DGM methods too
        findDGMMethodsByNameAndArguments(getTransformLoader(), receiver, name, args, methods);
        List<MethodNode> chosen = chooseBestMethod(receiver, methods, args);
        if (!chosen.isEmpty()) return chosen;

        // GROOVY-5566
        if (receiver instanceof InnerClassNode && ((InnerClassNode) receiver).isAnonymous() && methods.size() == 1 && args != null && "<init>".equals(name)) {
            MethodNode constructor = methods.get(0);
            if (constructor.getParameters().length == args.length) {
                return methods;
            }
        }

        if (receiver.equals(CLASS_Type) && receiver.getGenericsTypes() != null) {
            List<MethodNode> result = findMethod(receiver.getGenericsTypes()[0].getType(), name, args);
            if (!result.isEmpty()) return result;
        }

        if (ClassHelper.GSTRING_TYPE.equals(receiver)) return findMethod(ClassHelper.STRING_TYPE, name, args);

        if (isBeingCompiled(receiver)) {
            chosen = findMethod(GROOVY_OBJECT_TYPE, name, args);
            if (!chosen.isEmpty()) return chosen;
        }

        return EMPTY_METHODNODE_LIST;
    }

    /**
     * Given a method name and a prefix, returns the name of the property that should be looked up,
     * following the java beans rules. For example, "getName" would return "name", while
     * "getFullName" would return "fullName".
     * If the prefix is not found, returns null.
     * @param prefix the method name prefix ("get", "is", "set", ...)
     * @param methodName the method name
     * @return a property name if the prefix is found and the method matches the java beans rules, null otherwise
     */
    public static String extractPropertyNameFromMethodName(String prefix, String methodName) {
        if (prefix==null || methodName==null) return null;
        if (methodName.startsWith(prefix) && prefix.length()<methodName.length()) {
            String result = methodName.substring(prefix.length());
            String propertyName = java.beans.Introspector.decapitalize(result);
            if (result.equals(MetaClassHelper.capitalize(propertyName))) return propertyName;
        }
        return null;
    }

    protected void collectAllInterfaceMethodsByName(final ClassNode receiver, final String name, final List<MethodNode> methods) {
        ClassNode[] interfaces = receiver.getInterfaces();
        if (interfaces != null && interfaces.length > 0) {
            for (ClassNode node : interfaces) {
                List<MethodNode> intfMethods = node.getMethods(name);
                methods.addAll(intfMethods);
                collectAllInterfaceMethodsByName(node, name, methods);
            }
        }
    }

    protected ClassNode getType(ASTNode exp) {
        ClassNode cn = exp.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
        if (cn != null) return cn;
        if (exp instanceof ClassExpression) {
            ClassNode node = CLASS_Type.getPlainNodeReference();
            node.setGenericsTypes(new GenericsType[]{
                    new GenericsType(((ClassExpression) exp).getType())
            });
            return node;
        } else if (exp instanceof VariableExpression) {
            VariableExpression vexp = (VariableExpression) exp;
            if (vexp == VariableExpression.THIS_EXPRESSION) return makeThis();
            if (vexp == VariableExpression.SUPER_EXPRESSION) return makeSuper();
            final Variable variable = vexp.getAccessedVariable();
            if (variable instanceof FieldNode) {
                checkOrMarkPrivateAccess((FieldNode) variable);
            }
            if (variable != null && variable != vexp && variable instanceof VariableExpression) {
                return getType((Expression) variable);
            }
            if (variable instanceof Parameter) {
                Parameter parameter = (Parameter) variable;
                ClassNode type = typeCheckingContext.controlStructureVariables.get(parameter);
                TypeCheckingContext.EnclosingClosure enclosingClosure = typeCheckingContext.getEnclosingClosure();
                ClassNode[] closureParamTypes = (ClassNode[])(enclosingClosure!=null?enclosingClosure.getClosureExpression().getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS):null);
                if (type==null && enclosingClosure !=null && "it".equals(variable.getName()) && closureParamTypes!=null) {
                    final Parameter[] parameters = enclosingClosure.getClosureExpression().getParameters();
                    if (parameters.length==0 && getTemporaryTypesForExpression(vexp)==null) {
                        type = closureParamTypes[0];
                    }
                }
                if (type != null) {
                    storeType((VariableExpression)exp, type);
                    return type;
                }
            }
        }

        if (exp instanceof ListExpression) {
            return inferListExpressionType((ListExpression) exp);
        } else if (exp instanceof MapExpression) {
            return inferMapExpressionType((MapExpression) exp);
        }
        if (exp instanceof ConstructorCallExpression) {
            return ((ConstructorCallExpression) exp).getType();
        }
        if (exp instanceof MethodNode) {
            if ((exp == GET_DELEGATE || exp == GET_OWNER || exp == GET_THISOBJECT) && typeCheckingContext.getEnclosingClosure() != null) {
                return typeCheckingContext.getEnclosingClassNode();
            }
            ClassNode ret = getInferredReturnType(exp);
            return ret != null ? ret : ((MethodNode) exp).getReturnType();
        }
        if (exp instanceof ClosureExpression) {
            ClassNode irt = getInferredReturnType(exp);
            if (irt != null) {
                irt = wrapTypeIfNecessary(irt);
                ClassNode result = CLOSURE_TYPE.getPlainNodeReference();
                result.setGenericsTypes(new GenericsType[]{new GenericsType(irt)});
                return result;
            }
        }
        if (exp instanceof RangeExpression) {
            ClassNode plain = ClassHelper.RANGE_TYPE.getPlainNodeReference();
            RangeExpression re = (RangeExpression) exp;
            ClassNode fromType = getType(re.getFrom());
            ClassNode toType = getType(re.getTo());
            if (fromType.equals(toType)) {
                plain.setGenericsTypes(new GenericsType[]{
                        new GenericsType(wrapTypeIfNecessary(fromType))
                });
            } else {
                plain.setGenericsTypes(new GenericsType[]{
                        new GenericsType(wrapTypeIfNecessary(lowestUpperBound(fromType, toType)))
                });
            }
            return plain;
        }
        if (exp instanceof UnaryPlusExpression) {
            return getType(((UnaryPlusExpression) exp).getExpression());
        }
        if (exp instanceof UnaryMinusExpression) {
            return getType(((UnaryMinusExpression) exp).getExpression());
        }
        if (exp instanceof BitwiseNegationExpression) {
            return getType(((BitwiseNegationExpression) exp).getExpression());
        }
        if (exp instanceof MethodCall) {
            MethodNode target = (MethodNode) exp.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
            if (target!=null) {
                return getType(target);
            }
        }
        if (exp instanceof Parameter) {
            return ((Parameter) exp).getOriginType();
        }
        if (exp instanceof FieldNode) {
            return ((FieldNode) exp).getOriginType();
        }
        if (exp instanceof PropertyNode) {
            return ((PropertyNode) exp).getOriginType();
        }
        return exp instanceof VariableExpression ? ((VariableExpression) exp).getOriginType() : ((Expression) exp).getType();
    }

    private ClassNode makeSuper() {
        ClassNode ret = typeCheckingContext.getEnclosingClassNode().getSuperClass();
        if (typeCheckingContext.isInStaticContext) {
            ClassNode staticRet = CLASS_Type.getPlainNodeReference();
            GenericsType gt = new GenericsType(ret);
            staticRet.setGenericsTypes(new GenericsType[]{gt});
            ret = staticRet;
        }
        return ret;
    }

    private ClassNode makeThis() {
        ClassNode ret = typeCheckingContext.getEnclosingClassNode();
        if (typeCheckingContext.isInStaticContext) {
            ClassNode staticRet = CLASS_Type.getPlainNodeReference();
            GenericsType gt = new GenericsType(ret);
            staticRet.setGenericsTypes(new GenericsType[]{gt});
            ret = staticRet;
        }
        return ret;
    }

    /**
     * Stores the inferred return type of a closure or a method. We are using a separate key to store
     * inferred return type because the inferred type of a closure is {@link Closure}, which is different
     * from the inferred type of the code of the closure.
     *
     * @param node a {@link ClosureExpression} or a {@link MethodNode}
     * @param type the inferred return type of the code
     * @return the old value of the inferred type
     */
    protected ClassNode storeInferredReturnType(final ASTNode node, final ClassNode type) {
        if (!(node instanceof ClosureExpression)) {
            throw new IllegalArgumentException("Storing inferred return type is only allowed on closures but found "+node.getClass());
        }
        return (ClassNode) node.putNodeMetaData(StaticTypesMarker.INFERRED_RETURN_TYPE, type);
    }

    /**
     * Returns the inferred return type of a closure or a method, if stored on the AST node. This method
     * doesn't perform any type inference by itself.
     * @param exp a {@link ClosureExpression} or {@link MethodNode}
     * @return the inferred type, as stored on node metadata.
     */
    protected ClassNode getInferredReturnType(final ASTNode exp) {
        return (ClassNode) exp.getNodeMetaData(StaticTypesMarker.INFERRED_RETURN_TYPE);
    }

    protected ClassNode inferListExpressionType(final ListExpression list) {
        List<Expression> expressions = list.getExpressions();
        if (expressions.isEmpty()) {
            // cannot infer, return list type
            return list.getType();
        }
        ClassNode listType = list.getType();
        GenericsType[] genericsTypes = listType.getGenericsTypes();
        if ((genericsTypes == null
                || genericsTypes.length == 0
                || (genericsTypes.length == 1 && OBJECT_TYPE.equals(genericsTypes[0].getType())))
                && (!expressions.isEmpty())) {
            // maybe we can infer the component type
            List<ClassNode> nodes = new LinkedList<ClassNode>();
            for (Expression expression : expressions) {
                if (isNullConstant(expression)) {
                    // a null element is found in the list, skip it because we'll use the other elements from the list
                } else {
                    nodes.add(getType(expression));
                }
            }
            if (nodes.isEmpty()) {
                // every element was the null constant
                return listType;
            }
            ClassNode superType = getWrapper(lowestUpperBound(nodes)); // to be used in generics, type must be boxed
            ClassNode inferred = listType.getPlainNodeReference();
            inferred.setGenericsTypes(new GenericsType[]{new GenericsType(wrapTypeIfNecessary(superType))});
            return inferred;
        }
        return listType;
    }

    protected static boolean isNullConstant(final Expression expression) {
        return expression instanceof ConstantExpression && ((ConstantExpression) expression).getValue() == null;
    }

    protected ClassNode inferMapExpressionType(final MapExpression map) {
        ClassNode mapType = LINKEDHASHMAP_CLASSNODE.getPlainNodeReference();
        List<MapEntryExpression> entryExpressions = map.getMapEntryExpressions();
        if (entryExpressions.isEmpty()) return mapType;
        GenericsType[] genericsTypes = mapType.getGenericsTypes();
        if (genericsTypes == null
                || genericsTypes.length < 2
                || (genericsTypes.length == 2 && OBJECT_TYPE.equals(genericsTypes[0].getType()) && OBJECT_TYPE.equals(genericsTypes[1].getType()))) {
            List<ClassNode> keyTypes = new LinkedList<ClassNode>();
            List<ClassNode> valueTypes = new LinkedList<ClassNode>();
            for (MapEntryExpression entryExpression : entryExpressions) {
                keyTypes.add(getType(entryExpression.getKeyExpression()));
                valueTypes.add(getType(entryExpression.getValueExpression()));
            }
            ClassNode keyType = getWrapper(lowestUpperBound(keyTypes));  // to be used in generics, type must be boxed
            ClassNode valueType = getWrapper(lowestUpperBound(valueTypes));  // to be used in generics, type must be boxed
            if (!OBJECT_TYPE.equals(keyType) || !OBJECT_TYPE.equals(valueType)) {
                ClassNode inferred = mapType.getPlainNodeReference();
                inferred.setGenericsTypes(new GenericsType[]{new GenericsType(wrapTypeIfNecessary(keyType)), new GenericsType(wrapTypeIfNecessary(valueType))});
                return inferred;
            }
        }
        return mapType;
    }

    private static class ExtensionMethodDeclaringClass{}

    /**
     * If a method call returns a parameterized type, then we can perform additional inference on the
     * return type, so that the type gets actual type parameters. For example, the method
     * Arrays.asList(T...) is generified with type T which can be deduced from actual type
     * arguments.
     *
     * @param method    the method node
     * @param arguments the method call arguments
     * @return parameterized, infered, class node
     */
    protected ClassNode inferReturnTypeGenerics(final ClassNode receiver, final MethodNode method, final Expression arguments) {
        ClassNode returnType = method.getReturnType();
        if (method instanceof ExtensionMethodNode
                && (isUsingGenericsOrIsArrayUsingGenerics(returnType))) {
            // check if the placeholder corresponds to the placeholder of the first parameter
            ExtensionMethodNode emn = (ExtensionMethodNode) method;
            MethodNode dgmMethod = emn.getExtensionMethodNode();
            ClassNode dc = emn.getDeclaringClass();
            ArgumentListExpression argList = new ArgumentListExpression();
            VariableExpression vexp = new VariableExpression("$foo", receiver);
            vexp.setNodeMetaData(ExtensionMethodDeclaringClass.class, dc);
            argList.addExpression(vexp);
            if (arguments instanceof ArgumentListExpression) {
                List<Expression> expressions = ((ArgumentListExpression) arguments).getExpressions();
                for (Expression arg : expressions) {
                    argList.addExpression(arg);
                }
            } else {
                argList.addExpression(arguments);
            }
            return inferReturnTypeGenerics(receiver, dgmMethod, argList);
        }
        if (!isUsingGenericsOrIsArrayUsingGenerics(returnType)) return returnType;
        if (getGenericsWithoutArray(returnType)==null) return returnType;

        Map<String, GenericsType> resolvedPlaceholders = resolvePlaceHoldersFromDeclaration(receiver, getDeclaringClass(method, arguments), method, method.isStatic());
        if (resolvedPlaceholders.isEmpty()) return returnType;
        Map<String, GenericsType> placeholdersFromContext = extractGenericsParameterMapOfThis(typeCheckingContext.getEnclosingMethod());
        applyGenericsConnections(placeholdersFromContext,resolvedPlaceholders);

        // then resolve receivers from method arguments
        Parameter[] parameters = method.getParameters();
        boolean isVargs = isVargs(parameters);
        ArgumentListExpression argList = InvocationWriter.makeArgumentList(arguments);
        List<Expression> expressions = argList.getExpressions();
        int paramLength = parameters.length;
        if (expressions.size() >= paramLength) {
            for (int i = 0; i < paramLength; i++) {
                boolean lastArg = i == paramLength - 1;
                ClassNode type = parameters[i].getType();
                ClassNode actualType = getType(expressions.get(i));
                while (!type.isUsingGenerics() && type.isArray() && actualType.isArray()) {
                    type = type.getComponentType();
                    actualType = actualType.getComponentType();
                }
                if (isUsingGenericsOrIsArrayUsingGenerics(type)) {
                    if (implementsInterfaceOrIsSubclassOf(actualType, CLOSURE_TYPE) && !implementsInterfaceOrIsSubclassOf(type, CLOSURE_TYPE)) {
                        // implicit closure coercion in action!
                        Map<String,GenericsType> pholders = applyGenericsContextToParameterClass(resolvedPlaceholders, type);
                        actualType = convertClosureTypeToSAMType(expressions.get(i), actualType, type, pholders);
                    }
                    if (isVargs && lastArg && actualType.isArray()) {
                        actualType = actualType.getComponentType();
                    }
                    if (isVargs && lastArg && type.isArray()) {
                        type = type.getComponentType();
                    }
                    actualType = wrapTypeIfNecessary(actualType);

                    Map<String, GenericsType> connections  = new HashMap<String, GenericsType>();
                    extractGenericsConnections(connections, actualType,type);
                    applyGenericsConnections(connections, resolvedPlaceholders);
                }
            }
        }

        return applyGenericsContext(resolvedPlaceholders, returnType);
    }

    /**
     * This method will convert a closure type to the appropriate SAM type, which will be used
     * to infer return type generics.
     *
     * @param closureType the inferred type of a closure (Closure&lt;ClosureReturnType&gt;)
     * @param samType the type into which the closure is coerced into
     * @return same SAM type, but completed with information from the closure node
     */
    private static ClassNode convertClosureTypeToSAMType(final Expression expression, final ClassNode closureType, final ClassNode samType, final Map<String,GenericsType> placeholders) {
        if (!samType.isUsingGenerics()) return samType;

        // use the generics information from the Closure to further specify the type
        MethodNode sam = findSAM(samType);
        if (closureType.isUsingGenerics() && sam!=null) {
           //correct SAM type for generics
           //sam = applyGenericsContext(placeholders, sam);

           // the return type of the SAM method exactly corresponds to the inferred return type
           ClassNode samReturnType = sam.getReturnType();
           ClassNode closureReturnType = expression.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
           if (closureReturnType!=null && closureReturnType.isUsingGenerics()) {
               ClassNode unwrapped = closureReturnType.getGenericsTypes()[0].getType();
               extractGenericsConnections(placeholders, unwrapped, samReturnType);
           } else if (samReturnType.isGenericsPlaceHolder()) {
               placeholders.put(samReturnType.getGenericsTypes()[0].getName(), closureType.getGenericsTypes()[0]);
           }

           // now repeat the same for each parameter given in the ClosureExpression
           if (expression instanceof ClosureExpression) {
               List<ClassNode[]> genericsToConnect = new LinkedList<ClassNode[]>();
               Parameter[] closureParams = ((ClosureExpression) expression).getParameters();
               ClassNode[] closureParamTypes = extractTypesFromParameters(closureParams);
               if (expression.getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS)!=null) {
                   closureParamTypes = expression.getNodeMetaData(StaticTypesMarker.CLOSURE_ARGUMENTS);
               }
               final Parameter[] parameters = sam.getParameters();
               for (int i = 0; i < parameters.length; i++) {
                   final Parameter parameter = parameters[i];
                   if (parameter.getOriginType().isUsingGenerics() && closureParamTypes.length>i) {
                       genericsToConnect.add(new ClassNode[]{closureParamTypes[i], parameter.getOriginType()});
                   }
               }
               for (ClassNode[] classNodes : genericsToConnect) {
                   ClassNode found = classNodes[0];
                   ClassNode expected = classNodes[1];
                   if (!isAssignableTo(found, expected)) {
                       // probably facing a type mismatch
                       continue;
                   }
                   ClassNode generifiedType = GenericsUtils.parameterizeType(found, expected);
                   while (expected.isArray()) {
                       expected = expected.getComponentType();
                       generifiedType = generifiedType.getComponentType();
                   }
                   if (expected.isGenericsPlaceHolder()) {
                       placeholders.put(expected.getGenericsTypes()[0].getName(), new GenericsType(generifiedType));
                   } else {
                       GenericsType[] expectedGenericsTypes = expected.getGenericsTypes();
                       GenericsType[] foundGenericsTypes = generifiedType.getGenericsTypes();

                       for (int i = 0; i < expectedGenericsTypes.length; i++) {
                           final GenericsType type = expectedGenericsTypes[i];
                           if (type.isPlaceholder()) {
                               String name = type.getName();
                               placeholders.put(name, foundGenericsTypes[i]);
                           }
                       }
                   }
               }
           }
        }
        ClassNode result = applyGenericsContext(placeholders,samType.redirect());
        return result;
    }

    private ClassNode resolveGenericsWithContext(Map<String, GenericsType> resolvedPlaceholders, ClassNode currentType) {
        Map<String, GenericsType> placeholdersFromContext = extractGenericsParameterMapOfThis(typeCheckingContext.getEnclosingMethod());
        return resolveClassNodeGenerics(resolvedPlaceholders, placeholdersFromContext, currentType);
    }

    private static ClassNode getDeclaringClass(MethodNode method, Expression arguments) {
        ClassNode declaringClass = method.getDeclaringClass();

        // correcting declaring class for extension methods:
        if (arguments instanceof ArgumentListExpression) {
            ArgumentListExpression al = (ArgumentListExpression) arguments;
            List<Expression> list = al.getExpressions();
            if (list.size()==0) return declaringClass;
            Expression exp = list.get(0);
            ClassNode cn = exp.getNodeMetaData(ExtensionMethodDeclaringClass.class);
            if (cn!=null) return cn;
        }
        return declaringClass;
    }

    private Map<String, GenericsType> resolvePlaceHoldersFromDeclaration(ClassNode receiver, ClassNode declaration, MethodNode method, boolean isStaticTarget) {
        Map<String, GenericsType> resolvedPlaceholders;
        if (    isStaticTarget && CLASS_Type.equals(receiver) &&
                receiver.isUsingGenerics() &&
                receiver.getGenericsTypes().length>0 &&
                !OBJECT_TYPE.equals(receiver.getGenericsTypes()[0].getType()))
        {
            return resolvePlaceHoldersFromDeclaration(receiver.getGenericsTypes()[0].getType(), declaration, method, isStaticTarget);
        } else {
            resolvedPlaceholders = extractPlaceHolders(method, receiver, declaration);
        }
        return resolvedPlaceholders;
    }

    private static boolean isGenericsPlaceHolderOrArrayOf(ClassNode cn) {
        if (cn.isArray()) return isGenericsPlaceHolderOrArrayOf(cn.getComponentType());
        return cn.isGenericsPlaceHolder();
    }


    private static Map<String, GenericsType> extractPlaceHolders(MethodNode method, ClassNode receiver, ClassNode declaringClass) {
        if (declaringClass.equals(OBJECT_TYPE)) {
            Map<String, GenericsType> resolvedPlaceholders = new HashMap<String, GenericsType>();
            if (method!=null) addMethodLevelDeclaredGenerics(method, resolvedPlaceholders);
            return resolvedPlaceholders;
        }

        Map<String, GenericsType> resolvedPlaceholders = null;
        if (isPrimitiveType(receiver) && !isPrimitiveType(declaringClass)) {
            receiver = getWrapper(receiver);
        }
        ClassNode current = receiver;
        while (true) {
            boolean continueLoop = true;
            //extract the place holders
            Map<String, GenericsType> currentPlaceHolders = new HashMap<String, GenericsType>();
            if (isGenericsPlaceHolderOrArrayOf(declaringClass) || declaringClass.equals(current)) {
                extractGenericsConnections(currentPlaceHolders, current, declaringClass);
                if (method!=null) addMethodLevelDeclaredGenerics(method, currentPlaceHolders);
                continueLoop = false;
            } else {
                GenericsUtils.extractPlaceholders(current, currentPlaceHolders);
            }

            if (resolvedPlaceholders!=null) {
                // merge maps 
                Set<Map.Entry<String,GenericsType>> entries = currentPlaceHolders.entrySet();
                for (Map.Entry<String,GenericsType> entry : entries) {
                    GenericsType gt = entry.getValue();
                    if (!gt.isPlaceholder()) continue;
                    GenericsType referenced = resolvedPlaceholders.get(gt.getName());
                    if (referenced==null) continue;
                    entry.setValue(referenced);
                }
            }
            resolvedPlaceholders = currentPlaceHolders;

            // we are done if we are now in the declaring class
            if (!continueLoop) break;

            current = getNextSuperClass(current, declaringClass);
            if (current==null && CLASS_Type.equals(declaringClass)) {
                // this can happen if the receiver is Class<Foo>, then
                // the actual receiver is Foo and declaringClass is Class
                current = declaringClass;
            }
            if (current==null) {
                String descriptor = "<>";
                if (method!=null) descriptor = method.getTypeDescriptor();
                throw new GroovyBugError(
                        "Declaring class for method call to '" +
                        descriptor + "' declared in " + declaringClass.getName() +
                        " was not matched with found receiver "+ receiver.getName() + "." +
                        " This should not have happened!");
            }
        }
        return resolvedPlaceholders;
    }

    protected boolean typeCheckMethodsWithGenericsOrFail(ClassNode receiver, ClassNode[] arguments, MethodNode candidateMethod, Expression location) {
        if (!typeCheckMethodsWithGenerics(receiver, arguments, candidateMethod)) {
            Map<String, GenericsType> classGTs = GenericsUtils.extractPlaceholders(receiver);
            ClassNode[] ptypes = new ClassNode[candidateMethod.getParameters().length];
            final Parameter[] parameters = candidateMethod.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                final Parameter parameter = parameters[i];
                ClassNode type = parameter.getType();
                ptypes[i] = fullyResolveType(type, classGTs);
            }
            addStaticTypeError("Cannot call " + toMethodGenericTypesString(candidateMethod) + receiver.toString(false) + "#" +
                    toMethodParametersString(candidateMethod.getName(), ptypes) +
                    " with arguments " + formatArgumentList(arguments), location);
            return false;
        }
        return true;
    }

    private String toMethodGenericTypesString(MethodNode node) {
        GenericsType[] genericsTypes = node.getGenericsTypes();
        if (genericsTypes ==null) return "";
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < genericsTypes.length; i++) {
            final GenericsType genericsType = genericsTypes[i];
            sb.append(genericsType.toString());
            if (i<genericsTypes.length-1) {
                sb.append(",");
            }
        }
        sb.append("> ");
        return sb.toString();
    }

    protected static String formatArgumentList(ClassNode[] nodes) {
        if (nodes == null || nodes.length==0) return "[]";
        StringBuilder sb = new StringBuilder(24 * nodes.length);
        sb.append("[");
        for (ClassNode node : nodes) {
            sb.append(prettyPrintType(node));
            sb.append(", ");
        }
        if (sb.length() > 1) {
            sb.setCharAt(sb.length() - 2, ']');
        }
        return sb.toString();
    }

    private void putSetterInfo(Expression exp, SetterInfo info) {
        exp.putNodeMetaData(SetterInfo.class, info);
    }

    private SetterInfo removeSetterInfo(Expression exp) {
        Object nodeMetaData = exp.getNodeMetaData(SetterInfo.class);
        if (nodeMetaData!=null) {
            exp.removeNodeMetaData(SetterInfo.class);
            return (SetterInfo) nodeMetaData;
        }
        return null;
    }

    @Override
    protected void addError(final String msg, final ASTNode expr) {
        Long err = ((long) expr.getLineNumber()) << 16 + expr.getColumnNumber();
        if ((DEBUG_GENERATED_CODE && expr.getLineNumber()<0) || !typeCheckingContext.reportedErrors.contains(err)) {
            typeCheckingContext.getErrorCollector().addErrorAndContinue(new SyntaxErrorMessage(
                    new SyntaxException(msg + '\n', expr.getLineNumber(), expr.getColumnNumber(), expr.getLastLineNumber(), expr.getLastColumnNumber()),
                    typeCheckingContext.source)
            );
            typeCheckingContext.reportedErrors.add(err);
        }
    }

    protected void addStaticTypeError(final String msg, final ASTNode expr) {
        if (expr.getColumnNumber() > 0 && expr.getLineNumber() > 0) {
            addError(StaticTypesTransformation.STATIC_ERROR_PREFIX + msg, expr);
        } else {
            if (DEBUG_GENERATED_CODE) {
                addError(StaticTypesTransformation.STATIC_ERROR_PREFIX + "Error in generated code ["+expr.getText()+"] - "+ msg, expr);
            }
            // ignore errors which are related to unknown source locations
            // because they are likely related to generated code
        }
    }

    protected void addNoMatchingMethodError(ClassNode receiver, final String name, final ClassNode[] args, final Expression call) {
        if (isClassClassNodeWrappingConcreteType(receiver)) {
            receiver = receiver.getGenericsTypes()[0].getType();
        }
        addStaticTypeError("Cannot find matching method " + receiver.getText() + "#" + toMethodParametersString(name, args) + ". Please check if the declared type is right and if the method exists.", call);
    }

    protected void addAmbiguousErrorMessage(final List<MethodNode> foundMethods, final String name, final ClassNode[] args, final Expression expr) {
        addStaticTypeError("Reference to method is ambiguous. Cannot choose between " + prettyPrintMethodList(foundMethods), expr);
    }

    protected void addCategoryMethodCallError(final Expression call) {
        addStaticTypeError("Due to their dynamic nature, usage of categories is not possible with static type checking active", call);
    }

    protected void addAssignmentError(final ClassNode leftType, final ClassNode rightType, final Expression assignmentExpression) {
        addStaticTypeError("Cannot assign value of type " + rightType.toString(false) + " to variable of type " + leftType.toString(false), assignmentExpression);
    }

    protected void addUnsupportedPreOrPostfixExpressionError(final Expression expression) {
        if (expression instanceof PostfixExpression) {
            addStaticTypeError("Unsupported postfix operation type [" + ((PostfixExpression)expression).getOperation() + "]", expression);
        } else if (expression instanceof PrefixExpression) {
            addStaticTypeError("Unsupported prefix operation type [" + ((PrefixExpression)expression).getOperation() + "]", expression);
        } else {
            throw new IllegalArgumentException("Method should be called with a PostfixExpression or a PrefixExpression");
        }
    }

    public void setMethodsToBeVisited(final Set<MethodNode> methodsToBeVisited) {
        this.typeCheckingContext.methodsToBeVisited = methodsToBeVisited;
    }

    public void performSecondPass() {
        for (SecondPassExpression wrapper : typeCheckingContext.secondPassExpressions) {
            Expression expression = wrapper.getExpression();
            if (expression instanceof BinaryExpression) {
                Expression left = ((BinaryExpression) expression).getLeftExpression();
                if (left instanceof VariableExpression) {
                    // should always be the case
                    // this should always be the case, but adding a test is safer
                    Variable target = findTargetVariable((VariableExpression) left);
                    if (target instanceof VariableExpression) {
                        VariableExpression var = (VariableExpression) target;
                        List<ClassNode> classNodes = typeCheckingContext.closureSharedVariablesAssignmentTypes.get(var);
                        if (classNodes != null && classNodes.size() > 1) {
                            ClassNode lub = lowestUpperBound(classNodes);
                            String message = getOperationName(((BinaryExpression) expression).getOperation().getType());
                            if (message!=null) {
                                List<MethodNode> method = findMethod(lub, message, getType(((BinaryExpression) expression).getRightExpression()));
                                if (method.isEmpty()) {
                                    addStaticTypeError("A closure shared variable [" + target.getName() + "] has been assigned with various types and the method" +
                                            " [" + toMethodParametersString(message, getType(((BinaryExpression) expression).getRightExpression())) + "]" +
                                            " does not exist in the lowest upper bound of those types: [" +
                                            lub.toString(false) + "]. In general, this is a bad practice (variable reuse) because the compiler cannot" +
                                            " determine safely what is the type of the variable at the moment of the call in a multithreaded context.", expression);
                                }
                            }
                        }
                    }
                }
            } else if (expression instanceof MethodCallExpression) {
                MethodCallExpression call = (MethodCallExpression) expression;
                Expression objectExpression = call.getObjectExpression();
                if (objectExpression instanceof VariableExpression) {
                    // this should always be the case, but adding a test is safer
                    Variable target = findTargetVariable((VariableExpression) objectExpression);
                    if (target instanceof VariableExpression) {
                        VariableExpression var = (VariableExpression) target;
                        List<ClassNode> classNodes = typeCheckingContext.closureSharedVariablesAssignmentTypes.get(var);
                        if (classNodes != null && classNodes.size() > 1) {
                            ClassNode lub = lowestUpperBound(classNodes);
                            MethodNode methodNode = (MethodNode) call.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET);
                            // we must check that such a method exists on the LUB
                            Parameter[] parameters = methodNode.getParameters();
                            ClassNode[] params = extractTypesFromParameters(parameters);
                            ClassNode[] argTypes = (ClassNode[]) wrapper.getData();
                            List<MethodNode> method = findMethod(lub, methodNode.getName(), argTypes);
                            if (method.size() != 1) {
                                addStaticTypeError("A closure shared variable [" + target.getName() + "] has been assigned with various types and the method" +
                                        " [" + toMethodParametersString(methodNode.getName(), params) + "]" +
                                        " does not exist in the lowest upper bound of those types: [" +
                                        lub.toString(false) + "]. In general, this is a bad practice (variable reuse) because the compiler cannot" +
                                        " determine safely what is the type of the variable at the moment of the call in a multithreaded context.", call);
                            }
                        }
                    }
                }
            }
        }
        // give a chance to type checker extensions to throw errors based on information gathered afterwards
        extension.finish();
    }

    protected static ClassNode[] extractTypesFromParameters(final Parameter[] parameters) {
        ClassNode[] params = new ClassNode[parameters.length];
        for (int i = 0; i < params.length; i++) {
            params[i] = parameters[i].getType();
        }
        return params;
    }

    /**
     * Returns a wrapped type if, and only if, the provided class node is a primitive type.
     * This method differs from {@link ClassHelper#getWrapper(org.codehaus.groovy.ast.ClassNode)} as it will
     * return the same instance if the provided type is not a generic type.
     *
     * @param type
     * @return the wrapped type
     */
    protected static ClassNode wrapTypeIfNecessary(ClassNode type) {
        if (isPrimitiveType(type)) return getWrapper(type);
        return type;
    }

    protected static boolean isClassInnerClassOrEqualTo(ClassNode toBeChecked, ClassNode start) {
        if (start == toBeChecked) return true;
        if (start instanceof InnerClassNode) {
            return isClassInnerClassOrEqualTo(toBeChecked, start.getOuterClass());
        }
        return false;
    }


    protected class VariableExpressionTypeMemoizer extends ClassCodeVisitorSupport {
        private final Map<VariableExpression, ClassNode> varOrigType;

        public VariableExpressionTypeMemoizer(final Map<VariableExpression, ClassNode> varOrigType) {
            this.varOrigType = varOrigType;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return typeCheckingContext.source;
        }

        @Override
        public void visitVariableExpression(final VariableExpression expression) {
            super.visitVariableExpression(expression);
            Variable var = findTargetVariable(expression);
            if (var instanceof VariableExpression) {
                VariableExpression ve = (VariableExpression) var;
                varOrigType.put(ve, (ClassNode) ve.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE));
            }
        }
    }

    // ------------------- codecs for method return type signatures ------------------------------

    public static class SignatureCodecFactory {
        public static SignatureCodec getCodec(int version, final ClassLoader classLoader) {
            switch (version) {
                case 1:  return new SignatureCodecVersion1(classLoader);
                default: return null;
            }
        }
    }

    // class only used to store setter information when an expression of type
    // a.x = foo or x=foo is found and that it corresponds to a setter call
    private static class SetterInfo {
        final ClassNode receiverType;
        final String name;
        final List<MethodNode> setters;

        private SetterInfo(final ClassNode receiverType, final String name, final List<MethodNode> setters) {
            this.receiverType = receiverType;
            this.setters = setters;
            this.name = name;
        }
    }

}
