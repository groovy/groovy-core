/*
 * Copyright 2003-2013 the original author or authors.
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

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.ast.tools.WideningCategories;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.DefaultGroovyStaticMethods;
import org.codehaus.groovy.runtime.m12n.ExtensionModule;
import org.codehaus.groovy.runtime.m12n.ExtensionModuleScanner;
import org.codehaus.groovy.runtime.m12n.MetaInfExtensionModule;
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl;
import org.codehaus.groovy.tools.GroovyClass;
import org.objectweb.asm.Opcodes;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;

import static org.codehaus.groovy.ast.ClassHelper.*;
import static org.codehaus.groovy.syntax.Types.*;

/**
 * Static support methods for {@link StaticTypeCheckingVisitor}.
 */
public abstract class StaticTypeCheckingSupport {
    protected static final ClassNode
            Collection_TYPE = makeWithoutCaching(Collection.class);
    protected static final ClassNode Deprecated_TYPE = makeWithoutCaching(Deprecated.class);
    protected static final ClassNode Matcher_TYPE = makeWithoutCaching(Matcher.class);
    protected static final ClassNode ArrayList_TYPE = makeWithoutCaching(ArrayList.class);
    protected static final ExtensionMethodCache EXTENSION_METHOD_CACHE = new ExtensionMethodCache();
    protected static final Map<ClassNode, Integer> NUMBER_TYPES = Collections.unmodifiableMap(
            new HashMap<ClassNode, Integer>() {{
                put(ClassHelper.byte_TYPE, 0);
                put(ClassHelper.Byte_TYPE, 0);
                put(ClassHelper.short_TYPE, 1);
                put(ClassHelper.Short_TYPE, 1);
                put(ClassHelper.int_TYPE, 2);
                put(ClassHelper.Integer_TYPE, 2);
                put(ClassHelper.Long_TYPE, 3);
                put(ClassHelper.long_TYPE, 3);
                put(ClassHelper.float_TYPE, 4);
                put(ClassHelper.Float_TYPE, 4);
                put(ClassHelper.double_TYPE, 5);
                put(ClassHelper.Double_TYPE, 5);
            }});

    protected static final ClassNode GSTRING_STRING_CLASSNODE = WideningCategories.lowestUpperBound(
            ClassHelper.STRING_TYPE,
            ClassHelper.GSTRING_TYPE
    );

    /**
     * This is for internal use only. When an argument method is null, we cannot determine its type, so
     * we use this one as a wildcard.
     */
    protected static final ClassNode UNKNOWN_PARAMETER_TYPE = ClassHelper.make("<unknown parameter type>");

    /**
     * This comparator is used when we return the list of methods from DGM which name correspond to a given
     * name. As we also lookup for DGM methods of superclasses or interfaces, it may be possible to find
     * two methods which have the same name and the same arguments. In that case, we should not add the method
     * from superclass or interface otherwise the system won't be able to select the correct method, resulting
     * in an ambiguous method selection for similar methods.
     */
    protected static final Comparator<MethodNode> DGM_METHOD_NODE_COMPARATOR = new Comparator<MethodNode>() {
        public int compare(final MethodNode o1, final MethodNode o2) {
            if (o1.getName().equals(o2.getName())) {
                Parameter[] o1ps = o1.getParameters();
                Parameter[] o2ps = o2.getParameters();
                if (o1ps.length == o2ps.length) {
                    boolean allEqual = true;
                    for (int i = 0; i < o1ps.length && allEqual; i++) {
                        allEqual = o1ps[i].getType().equals(o2ps[i].getType());
                    }
                    if (allEqual) {
                        if (o1 instanceof ExtensionMethodNode && o2 instanceof ExtensionMethodNode) {
                            return compare(((ExtensionMethodNode) o1).getExtensionMethodNode(), ((ExtensionMethodNode) o2).getExtensionMethodNode());
                        }
                        return 0;
                    }
                } else {
                    return o1ps.length - o2ps.length;
                }
            }
            return 1;
        }
    };

    /**
     * Returns true for expressions of the form x[...]
     * @param expression an expression
     * @return true for array access expressions
     */
    protected static boolean isArrayAccessExpression(Expression expression) {
        return expression instanceof BinaryExpression && isArrayOp(((BinaryExpression) expression).getOperation().getType());
    }

    /**
     * Called on method call checks in order to determine if a method call corresponds to the
     * idiomatic o.with { ... } structure
     * @param name name of the method called
     * @param callArguments arguments of the method
     * @return true if the name is "with" and arguments consist of a single closure
     */
    public static boolean isWithCall(final String name, final Expression callArguments) {
        boolean isWithCall = "with".equals(name) && callArguments instanceof ArgumentListExpression;
        if (isWithCall) {
            ArgumentListExpression argList = (ArgumentListExpression) callArguments;
            List<Expression> expressions = argList.getExpressions();
            isWithCall = expressions.size() == 1 && expressions.get(0) instanceof ClosureExpression;
        }
        return isWithCall;
    }

    /**
     * Given a variable expression, returns the ultimately accessed variable.
     * @param ve a variable expression
     * @return the target variable
     */
    protected static Variable findTargetVariable(VariableExpression ve) {
        final Variable accessedVariable = ve.getAccessedVariable() != null ? ve.getAccessedVariable() : ve;
        if (accessedVariable != ve) {
            if (accessedVariable instanceof VariableExpression)
                return findTargetVariable((VariableExpression) accessedVariable);
        }
        return accessedVariable;
    }


    /**
     * @deprecated Use {@link #findDGMMethodsForClassNode(ClassLoader,ClassNode,String)} instead
     */
    @Deprecated
    protected static Set<MethodNode> findDGMMethodsForClassNode(ClassNode clazz, String name) {
        return findDGMMethodsForClassNode(MetaClassRegistryImpl.class.getClassLoader(), clazz,  name);
    }

    protected static Set<MethodNode> findDGMMethodsForClassNode(final ClassLoader loader, ClassNode clazz, String name) {
        TreeSet<MethodNode> accumulator = new TreeSet<MethodNode>(DGM_METHOD_NODE_COMPARATOR);
        findDGMMethodsForClassNode(loader, clazz, name, accumulator);
        return accumulator;
    }


    /**
     * @deprecated Use {@link #findDGMMethodsForClassNode(ClassLoader, ClassNode, String, TreeSet)} instead
     */
    @Deprecated
    protected static void findDGMMethodsForClassNode(ClassNode clazz, String name, TreeSet<MethodNode> accumulator) {
        findDGMMethodsForClassNode(MetaClassRegistryImpl.class.getClassLoader(), clazz, name, accumulator);
    }

    protected static void findDGMMethodsForClassNode(final ClassLoader loader, ClassNode clazz, String name, TreeSet<MethodNode> accumulator) {
        List<MethodNode> fromDGM = EXTENSION_METHOD_CACHE.getExtensionMethods(loader).get(clazz.getName());
        if (fromDGM != null) {
            for (MethodNode node : fromDGM) {
                if (node.getName().equals(name)) accumulator.add(node);
            }
        }
        for (ClassNode node : clazz.getInterfaces()) {
            findDGMMethodsForClassNode(loader, node, name, accumulator);
        }
        if (clazz.isArray()) {
            ClassNode componentClass = clazz.getComponentType();
            if (!componentClass.equals(OBJECT_TYPE) && !ClassHelper.isPrimitiveType(componentClass)) {
                if (componentClass.isInterface()) {
                    findDGMMethodsForClassNode(loader, OBJECT_TYPE.makeArray(), name, accumulator);
                } else {
                    findDGMMethodsForClassNode(loader, componentClass.getSuperClass().makeArray(), name, accumulator);
                }
            }
        }
        if (clazz.getSuperClass() != null) {
            findDGMMethodsForClassNode(loader, clazz.getSuperClass(), name, accumulator);
        } else if (!clazz.equals(ClassHelper.OBJECT_TYPE)) {
            findDGMMethodsForClassNode(loader, ClassHelper.OBJECT_TYPE, name, accumulator);
        }
    }

    /**
     * Checks that arguments and parameter types match.
     * @param params method parameters
     * @param args type arguments
     * @return -1 if arguments do not match, 0 if arguments are of the exact type and >0 when one or more argument is
     * not of the exact type but still match
     */
    public static int allParametersAndArgumentsMatch(Parameter[] params, ClassNode[] args) {
        if (params==null) {
            params = Parameter.EMPTY_ARRAY;
        }
        int dist = 0;
        if (args.length<params.length) return -1;
        // we already know the lengths are equal
        for (int i = 0; i < params.length; i++) {
            ClassNode paramType = params[i].getType();
            ClassNode argType = args[i];
            if (!isAssignableTo(argType, paramType)) return -1;
            else {
                if (!paramType.equals(argType)) dist+=getDistance(argType, paramType);
            }
        }
        return dist;
    }

    /**
     * Checks that arguments and parameter types match, expecting that the number of parameters is strictly greater
     * than the number of arguments, allowing possible inclusion of default parameters.
     * @param params method parameters
     * @param args type arguments
     * @return -1 if arguments do not match, 0 if arguments are of the exact type and >0 when one or more argument is
     * not of the exact type but still match
     */
    static int allParametersAndArgumentsMatchWithDefaultParams(Parameter[] params, ClassNode[] args) {
        int dist = 0;
        ClassNode ptype = null;
        // we already know the lengths are equal
        for (int i = 0, j=0; i < params.length; i++) {
            Parameter param = params[i];
            ClassNode paramType = param.getType();
            ClassNode arg = j>=args.length?null:args[j];
            if (arg==null || !isAssignableTo(arg, paramType)){
                if (!param.hasInitialExpression() && (ptype==null || !ptype.equals(paramType))) {
                    return -1; // no default value
                }
                // a default value exists, we can skip this param
                ptype = null;
            } else {
                j++;
                if (!paramType.equals(arg)) dist+=getDistance(arg, paramType);
                if (param.hasInitialExpression()) {
                    ptype = arg;
                } else {
                    ptype = null;
                }
            }
        }
        return dist;
    }

    /**
     * Checks that excess arguments match the vararg signature parameter.
     * @param params
     * @param args
     * @return -1 if no match, 0 if all arguments matches the vararg type and >0 if one or more vararg argument is
     * assignable to the vararg type, but still not an exact match
     */
    static int excessArgumentsMatchesVargsParameter(Parameter[] params, ClassNode[] args) {
        // we already know parameter length is bigger zero and last is a vargs
        // the excess arguments are all put in an array for the vargs call
        // so check against the component type
        int dist = 0;
        ClassNode vargsBase = params[params.length - 1].getType().getComponentType();
        for (int i = params.length; i < args.length; i++) {
            if (!isAssignableTo(args[i],vargsBase)) return -1;
            else if (!args[i].equals(vargsBase)) dist+=getDistance(args[i], vargsBase);
        }
        return dist;
    }

    /**
     * Checks if the last argument matches the vararg type.
     * @param params
     * @param args
     * @return -1 if no match, 0 if the last argument is exactly the vararg type and 1 if of an assignable type
     */
    static int lastArgMatchesVarg(Parameter[] params, ClassNode... args) {
        if (!isVargs(params)) return -1;
        // case length ==0 handled already
        // we have now two cases,
        // the argument is wrapped in the vargs array or
        // the argument is an array that can be used for the vargs part directly
        // we test only the wrapping part, since the non wrapping is done already
        ClassNode lastParamType = params[params.length - 1].getType();
        ClassNode ptype = lastParamType.getComponentType();
        ClassNode arg = args[args.length - 1];
        if (isNumberType(ptype) && isNumberType(arg) && !ptype.equals(arg)) return -1;
        return isAssignableTo(arg, ptype)?Math.min(getDistance(arg, lastParamType), getDistance(arg, ptype)):-1;
    }

    /**
     * Checks if a class node is assignable to another. This is used for example in
     * assignment checks where you want to verify that the assignment is valid.
     * @param type
     * @param toBeAssignedTo
     * @return true if the class node is assignable to the other class node, false otherwise
     */
    static boolean isAssignableTo(ClassNode type, ClassNode toBeAssignedTo) {
        if (UNKNOWN_PARAMETER_TYPE==type) return true;
        if (type==toBeAssignedTo) return true;
        if (toBeAssignedTo.redirect() == STRING_TYPE && type.redirect() == GSTRING_TYPE) {
            return true;
        }
        if (isPrimitiveType(toBeAssignedTo)) toBeAssignedTo = getWrapper(toBeAssignedTo);
        if (isPrimitiveType(type)) type = getWrapper(type);
        if (ClassHelper.Double_TYPE==toBeAssignedTo) {
            return type.isDerivedFrom(Number_TYPE);
        }
        if (ClassHelper.Float_TYPE==toBeAssignedTo) {
            return type.isDerivedFrom(Number_TYPE) && ClassHelper.Double_TYPE!=type.redirect();
        }
        if (ClassHelper.Long_TYPE==toBeAssignedTo) {
            return type.isDerivedFrom(Number_TYPE)
                    && ClassHelper.Double_TYPE!=type.redirect()
                    && ClassHelper.Float_TYPE!=type.redirect();
        }
        if (ClassHelper.Integer_TYPE==toBeAssignedTo) {
            return type.isDerivedFrom(Number_TYPE)
                    && ClassHelper.Double_TYPE!=type.redirect()
                    && ClassHelper.Float_TYPE!=type.redirect()
                    && ClassHelper.Long_TYPE!=type.redirect();
        }
        if (ClassHelper.Short_TYPE==toBeAssignedTo) {
            return type.isDerivedFrom(Number_TYPE)
                    && ClassHelper.Double_TYPE!=type.redirect()
                    && ClassHelper.Float_TYPE!=type.redirect()
                    && ClassHelper.Long_TYPE!=type.redirect()
                    && ClassHelper.Integer_TYPE!=type.redirect();
        }
        if (ClassHelper.Byte_TYPE==toBeAssignedTo) {
            return type.redirect() == ClassHelper.Byte_TYPE;
        }
        if (type.isArray() && toBeAssignedTo.isArray()) {
            return isAssignableTo(type.getComponentType(),toBeAssignedTo.getComponentType());
        }
        if (type.isDerivedFrom(GSTRING_TYPE) && STRING_TYPE.equals(toBeAssignedTo)) {
            return true;
        }
        if (toBeAssignedTo.isDerivedFrom(GSTRING_TYPE) && STRING_TYPE.equals(type)) {
            return true;
        }
        if (implementsInterfaceOrIsSubclassOf(type, toBeAssignedTo)) {
            if (OBJECT_TYPE.equals(toBeAssignedTo)) return true;
            if (toBeAssignedTo.isUsingGenerics()) {
                // perform additional check on generics
                // ? extends toBeAssignedTo
                GenericsType gt = GenericsUtils.buildWildcardType(toBeAssignedTo);
                return gt.isCompatibleWith(type);
            }
            return true;
        }

        //SAM check
        if (type.isDerivedFrom(CLOSURE_TYPE) && isSAMType(toBeAssignedTo)) {
            return true;
        }

        return false;
    }

    static boolean isVargs(Parameter[] params) {
        if (params.length == 0) return false;
        if (params[params.length - 1].getType().isArray()) return true;
        return false;
    }

    static boolean isCompareToBoolean(int op) {
        return op == COMPARE_GREATER_THAN ||
                op == COMPARE_GREATER_THAN_EQUAL ||
                op == COMPARE_LESS_THAN ||
                op == COMPARE_LESS_THAN_EQUAL;
    }

    static boolean isArrayOp(int op) {
        return op == LEFT_SQUARE_BRACKET;
    }

    static boolean isBoolIntrinsicOp(int op) {
        return op == LOGICAL_AND || op == LOGICAL_OR ||
                op == MATCH_REGEX || op == KEYWORD_INSTANCEOF;
    }

    static boolean isPowerOperator(int op) {
        return op == POWER || op == POWER_EQUAL;
    }

    static String getOperationName(int op) {
        switch (op) {
            case COMPARE_EQUAL:
            case COMPARE_NOT_EQUAL:
                // this is only correct in this context here, normally
                // we would have to compile against compareTo if available
                // but since we don't compile here, this one is enough
                return "equals";

            case COMPARE_TO:
            case COMPARE_GREATER_THAN:
            case COMPARE_GREATER_THAN_EQUAL:
            case COMPARE_LESS_THAN:
            case COMPARE_LESS_THAN_EQUAL:
                return "compareTo";

            case BITWISE_AND:
            case BITWISE_AND_EQUAL:
                return "and";

            case BITWISE_OR:
            case BITWISE_OR_EQUAL:
                return "or";

            case BITWISE_XOR:
            case BITWISE_XOR_EQUAL:
                return "xor";

            case PLUS:
            case PLUS_EQUAL:
                return "plus";

            case MINUS:
            case MINUS_EQUAL:
                return "minus";

            case MULTIPLY:
            case MULTIPLY_EQUAL:
                return "multiply";

            case DIVIDE:
            case DIVIDE_EQUAL:
                return "div";

            case INTDIV:
            case INTDIV_EQUAL:
                return "intdiv";

            case MOD:
            case MOD_EQUAL:
                return "mod";

            case POWER:
            case POWER_EQUAL:
                return "power";

            case LEFT_SHIFT:
            case LEFT_SHIFT_EQUAL:
                return "leftShift";

            case RIGHT_SHIFT:
            case RIGHT_SHIFT_EQUAL:
                return "rightShift";

            case RIGHT_SHIFT_UNSIGNED:
            case RIGHT_SHIFT_UNSIGNED_EQUAL:
                return "rightShiftUnsigned";

            case KEYWORD_IN:
                return "isCase";

            default:
                return null;
        }
    }

    static boolean isShiftOperation(String name) {
        return "leftShift".equals(name) || "rightShift".equals(name) || "rightShiftUnsigned".equals(name);
    }

    /**
     * Returns true for operations that are of the class, that given a common type class for left and right, the
     * operation "left op right" will have a result in the same type class In Groovy on numbers that is +,-,* as well as
     * their variants with equals.
     */
    static boolean isOperationInGroup(int op) {
        switch (op) {
            case PLUS:
            case PLUS_EQUAL:
            case MINUS:
            case MINUS_EQUAL:
            case MULTIPLY:
            case MULTIPLY_EQUAL:
                return true;
            default:
                return false;
        }
    }

    static boolean isBitOperator(int op) {
        switch (op) {
            case BITWISE_OR_EQUAL:
            case BITWISE_OR:
            case BITWISE_AND_EQUAL:
            case BITWISE_AND:
            case BITWISE_XOR_EQUAL:
            case BITWISE_XOR:
                return true;
            default:
                return false;
        }
    }

    public static boolean isAssignment(int op) {
        switch (op) {
            case ASSIGN:
            case LOGICAL_OR_EQUAL:
            case LOGICAL_AND_EQUAL:
            case PLUS_EQUAL:
            case MINUS_EQUAL:
            case MULTIPLY_EQUAL:
            case DIVIDE_EQUAL:
            case INTDIV_EQUAL:
            case MOD_EQUAL:
            case POWER_EQUAL:
            case LEFT_SHIFT_EQUAL:
            case RIGHT_SHIFT_EQUAL:
            case RIGHT_SHIFT_UNSIGNED_EQUAL:
            case BITWISE_OR_EQUAL:
            case BITWISE_AND_EQUAL:
            case BITWISE_XOR_EQUAL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true or false depending on whether the right classnode can be assigned to the left classnode. This method
     * should not add errors by itself: we let the caller decide what to do if an incompatible assignment is found.
     *
     * @param left  the class to be assigned to
     * @param right the assignee class
     * @return false if types are incompatible
     */
    public static boolean checkCompatibleAssignmentTypes(ClassNode left, ClassNode right) {
        return checkCompatibleAssignmentTypes(left, right, null);
    }

    public static boolean checkCompatibleAssignmentTypes(ClassNode left, ClassNode right, Expression rightExpression) {
        return checkCompatibleAssignmentTypes(left, right, rightExpression, true);
    }

    public static boolean checkCompatibleAssignmentTypes(ClassNode left, ClassNode right, Expression rightExpression, boolean allowConstructorCoercion) {
        ClassNode leftRedirect = left.redirect();
        ClassNode rightRedirect = right.redirect();
        if (leftRedirect==rightRedirect) return true;

        if (leftRedirect.isArray() && rightRedirect.isArray()) {
            return checkCompatibleAssignmentTypes(leftRedirect.getComponentType(), rightRedirect.getComponentType(), rightExpression, allowConstructorCoercion);
        }

        if (right==VOID_TYPE||right==void_WRAPPER_TYPE) {
            return left==VOID_TYPE||left==void_WRAPPER_TYPE;
        }

        if ((isNumberType(rightRedirect)||WideningCategories.isNumberCategory(rightRedirect))) {
           if (BigDecimal_TYPE==leftRedirect) {
               // any number can be assigned to a big decimal
               return true;
           }
            if (BigInteger_TYPE==leftRedirect) {
                return WideningCategories.isBigIntCategory(getUnwrapper(rightRedirect)) ||
                        rightRedirect.isDerivedFrom(BigInteger_TYPE);
            }
        }

        // if rightExpression is null and leftExpression is not a primitive type, it's ok
        boolean rightExpressionIsNull = rightExpression instanceof ConstantExpression && ((ConstantExpression) rightExpression).getValue()==null;
        if (rightExpressionIsNull && !isPrimitiveType(left)) {
            return true;
        }

        // on an assignment everything that can be done by a GroovyCast is allowed

        // anything can be assigned to an Object, String, Boolean
        // or Class typed variable
        if (isWildcardLeftHandSide(leftRedirect)
                && !(boolean_TYPE.equals(left) && rightExpressionIsNull)) return true;

        // char as left expression
        if (leftRedirect == char_TYPE && rightRedirect==STRING_TYPE) {
            if (rightExpression!=null && rightExpression instanceof ConstantExpression) {
                String value = rightExpression.getText();
                return value.length()==1;
            }
        }
        if (leftRedirect == Character_TYPE && (rightRedirect==STRING_TYPE||rightExpressionIsNull)) {
            return rightExpressionIsNull || (rightExpression instanceof ConstantExpression && rightExpression.getText().length()==1);
        }

        // if left is Enum and right is String or GString we do valueOf
        if (leftRedirect.isDerivedFrom(Enum_Type) &&
                (rightRedirect == GSTRING_TYPE || rightRedirect == STRING_TYPE)) {
            return true;
        }

        // if right is array, map or collection we try invoking the
        // constructor
        if (allowConstructorCoercion && (rightRedirect.implementsInterface(MAP_TYPE) ||
                rightRedirect.implementsInterface(Collection_TYPE) ||
                rightRedirect.equals(MAP_TYPE) ||
                rightRedirect.equals(Collection_TYPE) ||
                rightRedirect.isArray())) {
            //TODO: in case of the array we could maybe make a partial check
            if (leftRedirect.isArray() && rightRedirect.isArray()) {
                return checkCompatibleAssignmentTypes(leftRedirect.getComponentType(), rightRedirect.getComponentType());
            } else if (rightRedirect.isArray() && !leftRedirect.isArray()) {
                return false;
            }
            return true;
        }

        // simple check on being subclass
        if (right.isDerivedFrom(left) || (left.isInterface() && right.implementsInterface(left))) return true;

        // if left and right are primitives or numbers allow
        if (isPrimitiveType(leftRedirect) && isPrimitiveType(rightRedirect)) return true;
        if (isNumberType(leftRedirect) && isNumberType(rightRedirect)) return true;

        // left is a float/double and right is a BigDecimal
        if (WideningCategories.isFloatingCategory(leftRedirect) && BigDecimal_TYPE.equals(rightRedirect)) {
            return true;
        }

        if (GROOVY_OBJECT_TYPE.equals(leftRedirect) && isBeingCompiled(right)) {
            return true;
        }

        return false;
    }

    /**
     * Tells if a class is one of the "accept all" classes as the left hand side of an
     * assignment.
     * @param node the classnode to test
     * @return true if it's an Object, String, boolean, Boolean or Class.
     */
    public static boolean isWildcardLeftHandSide(final ClassNode node) {
        if (OBJECT_TYPE.equals(node) ||
            STRING_TYPE.equals(node) ||
            boolean_TYPE.equals(node) ||
            Boolean_TYPE.equals(node) ||
            CLASS_Type.equals(node)) {
            return true;
        }
        return false;
    }

    public static boolean isBeingCompiled(ClassNode node) {
        return node.getCompileUnit() != null;
    }

    static boolean checkPossibleLooseOfPrecision(ClassNode left, ClassNode right, Expression rightExpr) {
        if (left == right || left.equals(right)) return false; // identical types
        int leftIndex = NUMBER_TYPES.get(left);
        int rightIndex = NUMBER_TYPES.get(right);
        if (leftIndex >= rightIndex) return false;
        // here we must check if the right number is short enough to fit in the left type
        if (rightExpr instanceof ConstantExpression) {
            Object value = ((ConstantExpression) rightExpr).getValue();
            if (!(value instanceof Number)) return true;
            Number number = (Number) value;
            switch (leftIndex) {
                case 0: { // byte
                    byte val = number.byteValue();
                    if (number instanceof Short) {
                        return !Short.valueOf(val).equals(number);
                    }
                    if (number instanceof Integer) {
                        return !Integer.valueOf(val).equals(number);
                    }
                    if (number instanceof Long) {
                        return !Long.valueOf(val).equals(number);
                    }
                    if (number instanceof Float) {
                        return !Float.valueOf(val).equals(number);
                    }
                    return !Double.valueOf(val).equals(number);
                }
                case 1: { // short
                    short val = number.shortValue();
                    if (number instanceof Integer) {
                        return !Integer.valueOf(val).equals(number);
                    }
                    if (number instanceof Long) {
                        return !Long.valueOf(val).equals(number);
                    }
                    if (number instanceof Float) {
                        return !Float.valueOf(val).equals(number);
                    }
                    return !Double.valueOf(val).equals(number);
                }
                case 2: { // integer
                    int val = number.intValue();
                    if (number instanceof Long) {
                        return !Long.valueOf(val).equals(number);
                    }
                    if (number instanceof Float) {
                        return !Float.valueOf(val).equals(number);
                    }
                    return !Double.valueOf(val).equals(number);
                }
                case 3: { // long
                    long val = number.longValue();
                    if (number instanceof Float) {
                        return !Float.valueOf(val).equals(number);
                    }
                    return !Double.valueOf(val).equals(number);
                }
                case 4: { // float
                    float val = number.floatValue();
                    return !Double.valueOf(val).equals(number);
                }
                default: // double
                    return false; // no possible loose here
            }
        }
        return true; // possible loose of precision
    }

    static String toMethodParametersString(String methodName, ClassNode... parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append("(");
        if (parameters != null) {
            for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                final ClassNode parameter = parameters[i];
                sb.append(prettyPrintType(parameter));
                if (i < parametersLength - 1) sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    static String prettyPrintType(ClassNode type) {
        if (type.isArray()) {
            return prettyPrintType(type.getComponentType())+"[]";
        }
        return type.toString(false);
    }

    public static boolean implementsInterfaceOrIsSubclassOf(ClassNode type, ClassNode superOrInterface) {
        boolean result = type.equals(superOrInterface)
                || type.isDerivedFrom(superOrInterface)
                || type.implementsInterface(superOrInterface)
                || type == UNKNOWN_PARAMETER_TYPE;
        if (result) {
            return true;
        }
        if (superOrInterface instanceof WideningCategories.LowestUpperBoundClassNode) {
            WideningCategories.LowestUpperBoundClassNode cn = (WideningCategories.LowestUpperBoundClassNode) superOrInterface;
            result = implementsInterfaceOrIsSubclassOf(type, cn.getSuperClass());
            if (result) {
                for (ClassNode interfaceNode : cn.getInterfaces()) {
                    result = type.implementsInterface(interfaceNode);
                    if (!result) break;
                }
            }
            if (result) return true;
        } else if (superOrInterface instanceof UnionTypeClassNode) {
            UnionTypeClassNode union = (UnionTypeClassNode) superOrInterface;
            for (ClassNode delegate : union.getDelegates()) {
                if (implementsInterfaceOrIsSubclassOf(type, delegate)) return true;
            }
        }
        if (type.isArray() && superOrInterface.isArray()) {
            return implementsInterfaceOrIsSubclassOf(type.getComponentType(), superOrInterface.getComponentType());
        }
        if (GROOVY_OBJECT_TYPE.equals(superOrInterface) && !type.isInterface() && isBeingCompiled(type)) {
            return true;
        }
        return false;
    }

    static int getPrimitiveDistance(ClassNode primA, ClassNode primB) {
        return Math.abs(NUMBER_TYPES.get(primA) - NUMBER_TYPES.get(primB));
    }

    static int getDistance(final ClassNode receiver, final ClassNode compare) {
        if (receiver.isArray() && compare.isArray()) {
            return getDistance(receiver.getComponentType(), compare.getComponentType());
        }
        int dist = 0;
        ClassNode unwrapReceiver = ClassHelper.getUnwrapper(receiver);
        ClassNode unwrapCompare = ClassHelper.getUnwrapper(compare);
        if (ClassHelper.isPrimitiveType(unwrapReceiver)
                && ClassHelper.isPrimitiveType(unwrapCompare)
                && unwrapReceiver!=unwrapCompare) {
            dist = getPrimitiveDistance(unwrapReceiver, unwrapCompare);
        }
        if (isPrimitiveType(receiver) && !isPrimitiveType(compare)) {
            dist = (dist+1)<<1;
        }
        if (unwrapCompare.equals(unwrapReceiver)) return dist;
        if (receiver.isArray() && !compare.isArray()) {
            // Object[] vs Object
            dist += 256;
        }

        if (receiver == UNKNOWN_PARAMETER_TYPE) {
            return dist;
        }

        ClassNode ref = receiver;
        while (ref!=null) {
            if (compare.equals(ref)) {
                break;
            }
            if (compare.isInterface() && ref.implementsInterface(compare)) {
                dist += getMaximumInterfaceDistance(ref, compare);
                break;
            }
            ref = ref.getSuperClass();
            dist++;
            if (ref == null) dist++ ;
            dist = (dist+1)<<1;
        }
        return dist;
    }

    private static int getMaximumInterfaceDistance(ClassNode c, ClassNode interfaceClass) {
        // -1 means a mismatch
        if (c == null) return -1;
        // 0 means a direct match
        if (c.equals(interfaceClass)) return 0;
        ClassNode[] interfaces = c.getInterfaces();
        int max = -1;
        for (int i = 0; i < interfaces.length; i++) {
            final ClassNode anInterface = interfaces[i];
            int sub = getMaximumInterfaceDistance(anInterface, interfaceClass);
            // we need to keep the -1 to track the mismatch, a +1
            // by any means could let it look like a direct match
            // we want to add one, because there is an interface between
            // the interface we search for and the interface we are in.
            if (sub != -1) {
                sub+=(i+1); // GROOVY-6970: Make sure we can choose between equivalent methods
            }
            // we are interested in the longest path only
            max = Math.max(max, sub);
        }
        // we do not add one for super classes, only for interfaces
        int superClassMax = getMaximumInterfaceDistance(c.getSuperClass(), interfaceClass);
        return Math.max(max, superClassMax);
    }

    /**
     * @deprecated Use {@link #findDGMMethodsByNameAndArguments(ClassLoader, org.codehaus.groovy.ast.ClassNode, String, org.codehaus.groovy.ast.ClassNode[], java.util.List)} instead
     */
    @Deprecated
    public static List<MethodNode> findDGMMethodsByNameAndArguments(final ClassNode receiver, final String name, final ClassNode[] args) {
        return findDGMMethodsByNameAndArguments(MetaClassRegistryImpl.class.getClassLoader(), receiver, name, args);
    }

    public static List<MethodNode> findDGMMethodsByNameAndArguments(final ClassLoader loader, final ClassNode receiver, final String name, final ClassNode[] args) {
        return findDGMMethodsByNameAndArguments(loader, receiver, name, args, new LinkedList<MethodNode>());
    }

    /**
     * @deprecated Use {@link #findDGMMethodsByNameAndArguments(ClassLoader, org.codehaus.groovy.ast.ClassNode, String, org.codehaus.groovy.ast.ClassNode[], List)} instead
     */
    @Deprecated
    public static List<MethodNode> findDGMMethodsByNameAndArguments(final ClassNode receiver, final String name, final ClassNode[] args, final List<MethodNode> methods) {
        return findDGMMethodsByNameAndArguments(MetaClassRegistryImpl.class.getClassLoader(), receiver, name, args, methods);
    }

    public static List<MethodNode> findDGMMethodsByNameAndArguments(final ClassLoader loader, final ClassNode receiver, final String name, final ClassNode[] args, final List<MethodNode> methods) {
        final List<MethodNode> chosen;
        methods.addAll(findDGMMethodsForClassNode(loader, receiver, name));
        if (methods.isEmpty()) return methods;

        chosen = chooseBestMethod(receiver, methods, args);
        return chosen;
    }

    /**
     * Returns true if the provided class node, when considered as a receiver of a message or as a parameter,
     * is using a placeholder in its generics type. In this case, we're facing unchecked generics and type
     * checking is limited (ex: void foo(Set s) { s.keySet() }
     * @param node the node to test
     * @return true if it is using any placeholder in generics types
     */
    public static boolean isUsingUncheckedGenerics(ClassNode node) {
        if (node.isArray()) return isUsingUncheckedGenerics(node.getComponentType());
        if (node.isUsingGenerics()) {
            GenericsType[] genericsTypes = node.getGenericsTypes();
            if (genericsTypes!=null) {
                for (GenericsType genericsType : genericsTypes) {
                    if (genericsType.isPlaceholder()) {
                        return true;
                    } else {
                        if (isUsingUncheckedGenerics(genericsType.getType())) {
                            return true;
                        }
                    }
                }
            }
        } else {
            return false;
        }
        return false;
    }

    /**
     * Given a list of candidate methods, returns the one which best matches the argument types
     *
     * @param receiver
     * @param methods candidate methods
     * @param args argument types
     * @return the list of methods which best matches the argument types. It is still possible that multiple
     * methods match the argument types.
     */
    public static List<MethodNode> chooseBestMethod(final ClassNode receiver, Collection<MethodNode> methods, ClassNode... args) {
        if (methods.isEmpty()) return Collections.emptyList();
        if (isUsingUncheckedGenerics(receiver)) {
            ClassNode raw = makeRawType(receiver);
            return chooseBestMethod(raw, methods, args);
        }
        List<MethodNode> bestChoices = new LinkedList<MethodNode>();
        int bestDist = Integer.MAX_VALUE;
        Collection<MethodNode> choicesLeft = removeCovariants(methods);
        for (MethodNode candidateNode : choicesLeft) {
            ClassNode declaringClass = candidateNode.getDeclaringClass();
            ClassNode actualReceiver = receiver!=null?receiver: declaringClass;
            final ClassNode declaringClassForDistance = declaringClass;
            final ClassNode actualReceiverForDistance = actualReceiver;
            MethodNode safeNode = candidateNode;
            ClassNode[] safeArgs = args;
            boolean isExtensionMethodNode = candidateNode instanceof ExtensionMethodNode;
            if (isExtensionMethodNode) {
                safeArgs = new ClassNode[args.length+1];
                System.arraycopy(args, 0, safeArgs, 1, args.length);
                safeArgs[0] = receiver;
                safeNode = ((ExtensionMethodNode) candidateNode).getExtensionMethodNode();
                declaringClass = safeNode.getDeclaringClass();
                actualReceiver = declaringClass;
            }

            // todo : corner case
            /*
                class B extends A {}

                Animal foo(A o) {...}
                Person foo(B i){...}

                B  a = new B()
                Person p = foo(b)
             */

            Parameter[] params = makeRawTypes(safeNode.getParameters());
            if (params.length == safeArgs.length) {
                int allPMatch = allParametersAndArgumentsMatch(params, safeArgs);
                boolean firstParamMatches = true;
                // check first parameters
                if (safeArgs.length > 0) {
                    Parameter[] firstParams = new Parameter[params.length - 1];
                    System.arraycopy(params, 0, firstParams, 0, firstParams.length);
                    firstParamMatches = allParametersAndArgumentsMatch(firstParams, safeArgs) >= 0;
                }
                int lastArgMatch = isVargs(params) && firstParamMatches?lastArgMatchesVarg(params, safeArgs):-1;
                if (lastArgMatch>=0) {
                    lastArgMatch += 256-params.length; // ensure exact matches are preferred over vargs
                }
                int dist = allPMatch>=0?Math.max(allPMatch, lastArgMatch):lastArgMatch;
                if (dist>=0 && !actualReceiverForDistance.equals(declaringClassForDistance)) dist+=getDistance(actualReceiverForDistance, declaringClassForDistance);
                if (dist>=0 && !isExtensionMethodNode) {
                    dist++;
                }
                if (dist>=0 && dist<bestDist) {
                    bestChoices.clear();
                    bestChoices.add(candidateNode);
                    bestDist = dist;
                } else if (dist>=0 && dist==bestDist) {
                    bestChoices.add(candidateNode);
                }
            } else if (isVargs(params)) {
                boolean firstParamMatches = true;
                int dist = -1;
                // check first parameters
                if (safeArgs.length > 0) {
                    Parameter[] firstParams = new Parameter[params.length - 1];
                    System.arraycopy(params, 0, firstParams, 0, firstParams.length);
                    dist = allParametersAndArgumentsMatch(firstParams, safeArgs);
                    firstParamMatches =  dist >= 0;
                } else {
                    dist = 0;
                }
                if (firstParamMatches) {
                    // there are three case for vargs
                    // (1) varg part is left out
                    if (params.length == safeArgs.length + 1) {
                        if (dist>=0) {
                            dist += 256-params.length; // ensure exact matches are preferred over vargs
                        }
                        if (bestDist > 1+dist) {
                            bestChoices.clear();
                            bestChoices.add(candidateNode);
                            bestDist = 1+dist; // 1+dist to discriminate foo(Object,String) vs foo(Object,String, Object...)
                        }
                    } else {
                        // (2) last argument is put in the vargs array
                        //      that case is handled above already
                        // (3) there is more than one argument for the vargs array
                        int excessArgumentsDistance = excessArgumentsMatchesVargsParameter(params, safeArgs);
                        if (excessArgumentsDistance < 0) continue;
                        dist += excessArgumentsDistance;
                        if (dist >= 0 && !actualReceiverForDistance.equals(declaringClassForDistance)) dist+=getDistance(actualReceiverForDistance, declaringClassForDistance);
                        // varargs methods must not be preferred to methods without varargs
                        // for example :
                        // int sum(int x) should be preferred to int sum(int x, int... y)
                        dist+=256-params.length;
                        if (dist>=0 && !isExtensionMethodNode) {
                            dist++;
                        }
                        if (params.length < safeArgs.length && dist >= 0) {
                            if (dist >= 0 && dist < bestDist) {
                                bestChoices.clear();
                                bestChoices.add(candidateNode);
                                bestDist = dist;
                            } else if (dist >= 0 && dist == bestDist) {
                                bestChoices.add(candidateNode);
                            }
                        }
                    }
                }
            }
        }
        if (bestChoices.size()>1) {
            // GROOVY-6849: prefer extension methods in case of ambiguity
            List<MethodNode> onlyExtensionMethods = new LinkedList<MethodNode>();
            for (MethodNode choice : bestChoices) {
                if (choice instanceof ExtensionMethodNode) {
                    onlyExtensionMethods.add(choice);
                }
            }
            if (onlyExtensionMethods.size()==1) {
                return onlyExtensionMethods;
            }
        }
        return bestChoices;
    }

    private static Parameter[] makeRawTypes(Parameter[] params) {
        Parameter[] newParam = new Parameter[params.length];
        for (int i=0; i<params.length; i++) {
            Parameter oldP = params[i];
            Parameter newP = new Parameter(makeRawType(oldP.getType()),oldP.getName());
            newParam[i] = newP;
        }
        return newParam;
    }

    private static ClassNode makeRawType(final ClassNode receiver) {
        if (receiver.isArray()) {
            return makeRawType(receiver.getComponentType()).makeArray();
        }
        ClassNode raw = receiver.getPlainNodeReference();
        raw.setUsingGenerics(false);
        raw.setGenericsTypes(null);
        return raw;
    }

    private static Collection<MethodNode> removeCovariants(Collection<MethodNode> collection) {
        if (collection.size()<=1) return collection;
        List<MethodNode> toBeRemoved = new LinkedList<MethodNode>();
        List<MethodNode> list = new LinkedList<MethodNode>(new HashSet<MethodNode>(collection));
        for (int i=0;i<list.size()-1;i++) {
            MethodNode one = list.get(i);
            if (toBeRemoved.contains(one)) continue;
            for (int j=i+1;j<list.size();j++) {
                MethodNode two = list.get(j);
                if (toBeRemoved.contains(two)) continue;
                if (one.getName().equals(two.getName()) && one.getDeclaringClass()==two.getDeclaringClass()) {
                    Parameter[] onePars = one.getParameters();
                    Parameter[] twoPars = two.getParameters();
                    if (onePars.length == twoPars.length) {
                        boolean sameTypes = true;
                        for (int k = 0; k < onePars.length; k++) {
                            Parameter onePar = onePars[k];
                            Parameter twoPar = twoPars[k];
                            if (!onePar.getType().equals(twoPar.getType())) {
                                sameTypes = false;
                                break;
                            }
                        }
                        if (sameTypes) {
                            ClassNode oneRT = one.getReturnType();
                            ClassNode twoRT = two.getReturnType();
                            if (oneRT.isDerivedFrom(twoRT) || oneRT.implementsInterface(twoRT)) {
                                toBeRemoved.add(two);
                            } else if (twoRT.isDerivedFrom(oneRT) || twoRT.implementsInterface(oneRT)) {
                                toBeRemoved.add(one);
                            }
                        } else {
                            // this is an imperfect solution to determining if two methods are
                            // equivalent, for example String#compareTo(Object) and String#compareTo(String)
                            // in that case, Java marks the Object version as synthetic
                            if (one.isSynthetic() && !two.isSynthetic()) {
                                toBeRemoved.add(one);
                            } else if (two.isSynthetic() && !one.isSynthetic()) {
                                toBeRemoved.add(two);
                            }
                        }
                    }
                }                
            }
        }
        if (toBeRemoved.isEmpty()) return list;
        List<MethodNode> result = new LinkedList<MethodNode>(list);
        result.removeAll(toBeRemoved);
        return result;
    }

    /**
     * Given a receiver and a method node, parameterize the method arguments using
     * available generic type information.
     *
     * @param receiver the class
     * @param m the method
     * @return the parameterized arguments
     */
    public static Parameter[] parameterizeArguments(final ClassNode receiver, final MethodNode m) {
        Map<String, GenericsType> genericFromReceiver = GenericsUtils.extractPlaceholders(receiver);
        Map<String, GenericsType> contextPlaceholders = extractGenericsParameterMapOfThis(m);
        Parameter[] methodParameters = m.getParameters();
        Parameter[] params = new Parameter[methodParameters.length];
        for (int i = 0; i < methodParameters.length; i++) {
            Parameter methodParameter = methodParameters[i];
            ClassNode paramType = methodParameter.getType();
            params[i] = buildParameter(genericFromReceiver, contextPlaceholders, methodParameter, paramType);
        }
        return params;
    }

    /**
     * Given a parameter, builds a new parameter for which the known generics placeholders are resolved.
     * @param genericFromReceiver resolved generics from the receiver of the message
     * @param placeholdersFromContext, resolved generics from the method context
     * @param methodParameter the method parameter for which we want to resolve generic types
     * @param paramType the (unresolved) type of the method parameter
     * @return a new parameter with the same name and type as the original one, but with resolved generic types
     */
    private static Parameter buildParameter(final Map<String, GenericsType> genericFromReceiver, final Map<String, GenericsType> placeholdersFromContext, final Parameter methodParameter, final ClassNode paramType) {
        if (genericFromReceiver.isEmpty() && (placeholdersFromContext==null||placeholdersFromContext.isEmpty())) {
            return methodParameter;
        }
        if (paramType.isArray()) {
            ClassNode componentType = paramType.getComponentType();
            Parameter subMethodParameter = new Parameter(componentType, methodParameter.getName());
            Parameter component = buildParameter(genericFromReceiver, placeholdersFromContext, subMethodParameter, componentType);
            return new Parameter(component.getType().makeArray(), component.getName());
        }
        ClassNode resolved = resolveClassNodeGenerics(genericFromReceiver, placeholdersFromContext, paramType);

        return new Parameter(resolved, methodParameter.getName());
    }

    /**
     * Returns true if a class node makes use of generic types. If the class node represents an
     * array type, then checks if the component type is using generics.
     * @param cn a class node for which to check if it is using generics
     * @return true if the type (or component type) is using generics
     */
    public static boolean isUsingGenericsOrIsArrayUsingGenerics(ClassNode cn) {
        if (cn.isArray()) {
            return isUsingGenericsOrIsArrayUsingGenerics(cn.getComponentType());
        }
        return (cn.isUsingGenerics() && cn.getGenericsTypes()!=null);
    }

    /**
     * Given a generics type representing SomeClass&lt;T,V&gt; and a resolved placeholder map, returns a new generics type
     * for which placeholders are resolved recursively.
     */
    protected static GenericsType fullyResolve(GenericsType gt, Map<String, GenericsType> placeholders) {
        GenericsType fromMap = placeholders.get(gt.getName());
        if (gt.isPlaceholder() && fromMap!=null) {
            gt = fromMap;
        }

        ClassNode type = fullyResolveType(gt.getType(), placeholders);
        ClassNode lowerBound = gt.getLowerBound();
        if (lowerBound != null) lowerBound = fullyResolveType(lowerBound, placeholders);
        ClassNode[] upperBounds = gt.getUpperBounds();
        if (upperBounds != null) {
            ClassNode[] copy = new ClassNode[upperBounds.length];
            for (int i = 0, upperBoundsLength = upperBounds.length; i < upperBoundsLength; i++) {
                final ClassNode upperBound = upperBounds[i];
                copy[i] = fullyResolveType(upperBound, placeholders);
            }
            upperBounds = copy;
        }
        GenericsType genericsType = new GenericsType(type, upperBounds, lowerBound);
        genericsType.setWildcard(gt.isWildcard());
        return genericsType;
    }

    protected static ClassNode fullyResolveType(final ClassNode type, final Map<String, GenericsType> placeholders) {
        if (type.isUsingGenerics() && !type.isGenericsPlaceHolder()) {
            GenericsType[] gts = type.getGenericsTypes();
            if (gts != null) {
                GenericsType[] copy = new GenericsType[gts.length];
                for (int i = 0; i < gts.length; i++) {
                    GenericsType genericsType = gts[i];
                    if (genericsType.isPlaceholder() && placeholders.containsKey(genericsType.getName())) {
                        copy[i] = placeholders.get(genericsType.getName());
                    } else {
                        copy[i] = fullyResolve(genericsType, placeholders);
                    }
                }
                gts = copy;
            }
            ClassNode result = type.getPlainNodeReference();
            result.setGenericsTypes(gts);
            return result;
        } else if (type.isUsingGenerics() && OBJECT_TYPE.equals(type) && type.getGenericsTypes() != null) {
            // Object<T>
            GenericsType genericsType = placeholders.get(type.getGenericsTypes()[0].getName());
            if (genericsType != null) {
                return genericsType.getType();
            }
        } else if (type.isArray()) {
            return fullyResolveType(type.getComponentType(), placeholders).makeArray();
        }
        return type;
    }

    /**
     * Checks that the parameterized generics of an argument are compatible with the generics of the parameter.
     *
     * @param parameterType the parameter type of a method
     * @param argumentType  the type of the argument passed to the method
     */
    protected static boolean typeCheckMethodArgumentWithGenerics(ClassNode parameterType, ClassNode argumentType, boolean lastArg) {
        if (UNKNOWN_PARAMETER_TYPE == argumentType) {
            // called with null
            return !ClassHelper.isPrimitiveType(parameterType);
        }
        if (!isAssignableTo(argumentType, parameterType) && !lastArg) {
            // incompatible assignment
            return false;
        }
        if (!isAssignableTo(argumentType, parameterType) && lastArg) {
            if (parameterType.isArray()) {
                if (!isAssignableTo(argumentType, parameterType.getComponentType())) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (parameterType.isUsingGenerics() && argumentType.isUsingGenerics()) {
            GenericsType gt = GenericsUtils.buildWildcardType(parameterType);
            if (!gt.isCompatibleWith(argumentType)) {
                boolean samCoercion = isSAMType(parameterType) && argumentType.equals(CLOSURE_TYPE);
                if (!samCoercion) return false;
            }
        } else if (parameterType.isArray() && argumentType.isArray()) {
            // verify component type
            return typeCheckMethodArgumentWithGenerics(parameterType.getComponentType(), argumentType.getComponentType(), lastArg);
        } else if (lastArg && parameterType.isArray()) {
            // verify component type, but if we reach that point, the only possibility is that the argument is
            // the last one of the call, so we're in the cast of a vargs call
            // (otherwise, we face a type checker bug)
            return typeCheckMethodArgumentWithGenerics(parameterType.getComponentType(), argumentType, lastArg);
        }
        return true;
    }

    static void addMethodLevelDeclaredGenerics(MethodNode method, Map<String, GenericsType> resolvedPlaceholders) {
        ClassNode dummy = OBJECT_TYPE.getPlainNodeReference();
        dummy.setGenericsTypes(method.getGenericsTypes());
        GenericsUtils.extractPlaceholders(dummy, resolvedPlaceholders);
    }

    protected static boolean typeCheckMethodsWithGenerics(ClassNode receiver, ClassNode[] arguments, MethodNode candidateMethod) {
        if (isUsingUncheckedGenerics(receiver)) {
            return true;
        }
        if (CLASS_Type.equals(receiver)
                && receiver.isUsingGenerics()
                && !candidateMethod.getDeclaringClass().equals(receiver)
                && !(candidateMethod instanceof ExtensionMethodNode)) {
            return typeCheckMethodsWithGenerics(receiver.getGenericsTypes()[0].getType(), arguments, candidateMethod);
        }
        // both candidate method and receiver have generic information so a check is possible
        Parameter[] parameters = candidateMethod.getParameters();
        GenericsType[] genericsTypes = candidateMethod.getGenericsTypes();
        boolean methodUsesGenerics = (genericsTypes != null && genericsTypes.length > 0);
        boolean isExtensionMethod = candidateMethod instanceof ExtensionMethodNode;
        if (isExtensionMethod && methodUsesGenerics) {
            ClassNode[] dgmArgs = new ClassNode[arguments.length + 1];
            dgmArgs[0] = receiver;
            System.arraycopy(arguments, 0, dgmArgs, 1, arguments.length);
            MethodNode extensionMethodNode = ((ExtensionMethodNode) candidateMethod).getExtensionMethodNode();
            return typeCheckMethodsWithGenerics(extensionMethodNode.getDeclaringClass(), dgmArgs, extensionMethodNode, true);
        } else {
            return typeCheckMethodsWithGenerics(receiver, arguments, candidateMethod, false);
        }
    }

    private static boolean typeCheckMethodsWithGenerics(ClassNode receiver, ClassNode[] arguments, MethodNode candidateMethod, boolean isExtensionMethod) {
        boolean failure = false;

        // correct receiver for inner class
        // we assume the receiver is an instance of the declaring class of the 
        // candidate method, but findMethod returns also outer class methods
        // for that receiver. For now we skip receiver based checks in that case
        // TODO: correct generics for when receiver is to be skipped
        boolean skipBecauseOfInnerClassNotReceiver = isOuterClassOf(receiver, candidateMethod.getDeclaringClass());

        Parameter[] parameters = candidateMethod.getParameters();
        Map<String, GenericsType> classGTs;
        if (skipBecauseOfInnerClassNotReceiver) {
            classGTs = Collections.EMPTY_MAP;
        } else {
            classGTs = GenericsUtils.extractPlaceholders(receiver);
        }
        if (parameters.length > arguments.length || parameters.length==0) {
            // this is a limitation that must be removed in a future version
            // we cannot check generic type arguments if there are default parameters!
            return true;
        }

        // we have here different generics contexts we have to deal with.
        // There is firstly the context given through the class, and the method.
        // The method context may hide generics given through the class, but use 
        // the non-hidden ones.
        Map<String, GenericsType> resolvedMethodGenerics = new HashMap<String, GenericsType>();
        if (!skipBecauseOfInnerClassNotReceiver) {
            addMethodLevelDeclaredGenerics(candidateMethod,resolvedMethodGenerics);
        }
        // so first we remove hidden generics
        for (String key: resolvedMethodGenerics.keySet()) classGTs.remove(key); 
        // then we use the remaining information to refine the given generics
        applyGenericsConnections(classGTs,resolvedMethodGenerics);
        // and then start our checks with the receiver
        if (!skipBecauseOfInnerClassNotReceiver) {
            failure |= inferenceCheck(Collections.EMPTY_SET, resolvedMethodGenerics, candidateMethod.getDeclaringClass(), receiver, false);
        }
        // the outside context parts till now define placeholder we are not allowed to
        // generalize, thus we save that for later use...
        // extension methods are special, since they set the receiver as 
        // first parameter. While we normally allow generalization for the first
        // parameter, in case of an extension method we must not. 
        Set<String> fixedGenericsPlaceHolders = extractResolvedPlaceHolders(resolvedMethodGenerics);

        for (int i = 0; i < arguments.length; i++) {
            int pindex = Math.min(i, parameters.length - 1);
            ClassNode wrappedArgument = arguments[i];
            ClassNode type = parameters[pindex].getOriginType();

            failure |= inferenceCheck(fixedGenericsPlaceHolders, resolvedMethodGenerics, type, wrappedArgument,i >= parameters.length - 1);

            // set real fixed generics for extension methods
            if (isExtensionMethod && i==0) fixedGenericsPlaceHolders = extractResolvedPlaceHolders(resolvedMethodGenerics);
        }
        return !failure;
    }

    private static boolean isOuterClassOf(ClassNode receiver, ClassNode type) {
        if (implementsInterfaceOrIsSubclassOf(receiver,type)) return false;
        return true;
    }

    private static Set<String> extractResolvedPlaceHolders(Map<String, GenericsType> resolvedMethodGenerics) {
        if (resolvedMethodGenerics.isEmpty()) return Collections.EMPTY_SET;
        Set<String> result = new HashSet<String>();
        for (Map.Entry<String, GenericsType> entry : resolvedMethodGenerics.entrySet()) {
            GenericsType value = entry.getValue();
            if (value.isPlaceholder() || value.isPlaceholder()) continue;
            result.add(entry.getKey());
        }
        return result;
    }

    private static boolean inferenceCheck(Set<String> fixedGenericsPlaceHolders, Map<String, GenericsType> resolvedMethodGenerics, ClassNode type, ClassNode wrappedArgument, boolean lastArg) {
        Map<String, GenericsType> connections = new HashMap<String, GenericsType>();
        if (ClassHelper.isPrimitiveType(wrappedArgument)) wrappedArgument = ClassHelper.getWrapper(wrappedArgument);
        // the context we compare with in the end is the one of the callsite
        // so far we specified the context of the method declaration only
        // thus for each argument, we try to find the connected generics first
        extractGenericsConnections(connections, wrappedArgument, type);
        // each found connection must comply with already found connections
        boolean failure = !compatibleConnections(connections, resolvedMethodGenerics, fixedGenericsPlaceHolders);
        // and then apply the found information to refine the method level
        // information. This way the method level information slowly turns
        // into information for the callsite
        applyGenericsConnections(connections, resolvedMethodGenerics);
        // since it is possible that the callsite uses some generics as well, 
        // we may have to add additional elements here
        addMissingEntries(connections, resolvedMethodGenerics);
        // to finally see if the parameter and the argument fit togehter,
        // we use the provided information to transform the parameter
        // into something that can exist in the callsite context
        type = applyGenericsContext(resolvedMethodGenerics, type);
        // there of course transformed parameter type and argument must fit
        failure |= !typeCheckMethodArgumentWithGenerics(type, wrappedArgument, lastArg);
        return failure;
    }

    private static GenericsType buildWildcardType(GenericsType origin) {
        ClassNode lowerBound = origin.getType().getPlainNodeReference();
        if (origin.getLowerBound()!=null || origin.getUpperBounds()!=null) {
            lowerBound.setGenericsTypes(new GenericsType[]{origin});
        }
        ClassNode base = ClassHelper.makeWithoutCaching("?");
        GenericsType gt = new GenericsType(base, null, lowerBound);
        gt.setWildcard(true);
        return gt;
    }

    private static boolean compatibleConnections(Map<String, GenericsType> connections, Map<String, GenericsType> resolvedMethodGenerics, Set<String> fixedGenericsPlaceHolders) 
    {
        for (Map.Entry<String, GenericsType> entry : connections.entrySet()) {
            GenericsType resolved = resolvedMethodGenerics.get(entry.getKey());
            if (resolved==null) continue;
            GenericsType connection = entry.getValue();
            if (    connection.isPlaceholder() && 
                    connection.getUpperBounds()==null &&
                    connection.getLowerBound()==null)
            {
                continue;
            }
            if (!compatibleConnection(resolved,connection)) {
                if (    !(resolved.isPlaceholder() || resolved.isWildcard()) &&
                        !fixedGenericsPlaceHolders.contains(entry.getKey()) &&
                        compatibleConnection(connection,resolved)) 
                {
                    // we did for example find T=String and now check against
                    // T=Object, which fails the first compatibleConnection check
                    // but since T=Object works for both, the second one will pass
                    // and we need to change the type for T to the more general one
                    resolvedMethodGenerics.put(entry.getKey(),connection);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean compatibleConnection(GenericsType resolved, GenericsType connection) {
        GenericsType gt = connection;
        if (!connection.isWildcard()) gt = buildWildcardType(connection);
        if (    resolved.isPlaceholder() && resolved.getUpperBounds()!=null && 
                resolved.getUpperBounds().length==1 && !resolved.getUpperBounds()[0].isGenericsPlaceHolder() && 
                resolved.getUpperBounds()[0].getName().equals("java.lang.Object")) 
        {
            return true;
        }
        ClassNode compareNode;
        if (resolved.getLowerBound()!=null || resolved.getUpperBounds()!=null) {
            compareNode = getCombinedBoundType(resolved);
            compareNode = compareNode.redirect().getPlainNodeReference();
        } else {
            compareNode = resolved.getType().getPlainNodeReference();
        }
        return gt.isCompatibleWith(compareNode);
    }

    private static void addMissingEntries(Map<String, GenericsType> connections, Map<String, GenericsType> resolved) {
        for (Entry<String, GenericsType> entry : connections.entrySet()) {
            if (resolved.containsKey(entry.getKey())) continue;
            GenericsType gt = entry.getValue();
            ClassNode cn = gt.getType();
            if (cn.redirect()==UNKNOWN_PARAMETER_TYPE) continue;
            resolved.put(entry.getKey(), gt);
        }
    }

    public static ClassNode resolveClassNodeGenerics(Map<String, GenericsType> resolvedPlaceholders, final Map<String, GenericsType> placeholdersFromContext, ClassNode currentType) {
        ClassNode target = currentType.redirect();
        resolvedPlaceholders = new HashMap<String, GenericsType>(resolvedPlaceholders);
        applyContextGenerics(resolvedPlaceholders,placeholdersFromContext);

        Map<String, GenericsType> connections  = new HashMap<String, GenericsType>();
        extractGenericsConnections(connections, currentType,target);
        applyGenericsConnections(connections, resolvedPlaceholders);
        currentType = applyGenericsContext(resolvedPlaceholders, currentType);
        return currentType;
    }

    static void applyGenericsConnections(
            Map<String, GenericsType> connections,
            Map<String, GenericsType> resolvedPlaceholders
    ) {
        if (connections==null) return;
        int count = 0;
        while (count<10000) {
            count++;
            boolean checkForMorePlaceHolders=false;
            for (Map.Entry<String, GenericsType> entry: resolvedPlaceholders.entrySet()){
                String name = entry.getKey();
                GenericsType replacement = connections.get(name);
                if (replacement==null) {
                    GenericsType value = entry.getValue();
                    GenericsType newValue = applyGenericsContext(connections, value);
                    entry.setValue(newValue);
                    checkForMorePlaceHolders = checkForMorePlaceHolders || !equalIncludingGenerics(value,newValue);
                    continue;
                }
                GenericsType original = entry.getValue(); 
                if (!original.isWildcard() && !original.isPlaceholder()) {
                    continue;
                } else if (!replacement.isPlaceholder()) {
                    entry.setValue(replacement);
                    continue;
                }
                GenericsType connectedType = resolvedPlaceholders.get(replacement.getName());
                if (replacement==connectedType) continue;
                entry.setValue(replacement);
                checkForMorePlaceHolders = checkForMorePlaceHolders || !equalIncludingGenerics(original,replacement);
            }
            if (!checkForMorePlaceHolders) break;
        }
        if  (count>=10000) throw new GroovyBugError("unable to handle generics in "+resolvedPlaceholders+" with connections "+connections);
    }

    private static boolean equalIncludingGenerics(GenericsType orig, GenericsType copy) {
        if (orig==copy) return true;
        if (orig.isPlaceholder()!=copy.isPlaceholder()) return false;
        if (orig.isWildcard()!=copy.isWildcard()) return false;
        if (!equalIncludingGenerics(orig.getType(), copy.getType())) return false;
        ClassNode lower1 = orig.getLowerBound();
        ClassNode lower2 = copy.getLowerBound();
        if ((lower1==null || lower2 ==null) && lower1!=lower2) return false;
        if (lower1==lower2) return true;
        if (!equalIncludingGenerics(lower1,lower2)) return false;
        ClassNode[] upper1 = orig.getUpperBounds();
        ClassNode[] upper2 = copy.getUpperBounds();
        if ((upper1==null || upper2==null) && upper1!=upper2) return false;
        if (upper1==upper2) return true;
        if (upper1.length!=upper2.length) return false;
        for (int i=0; i<upper1.length; i++) {
            if (!equalIncludingGenerics(upper1[i],upper2[i])) return false;
        }
        return true;
    }

    private static boolean equalIncludingGenerics(ClassNode orig, ClassNode copy) {
        if (orig==copy) return true;
        if (orig.isGenericsPlaceHolder()!=copy.isGenericsPlaceHolder()) return false;
        if (!orig.equals(copy)) return false;
        GenericsType[] gt1 = orig.getGenericsTypes();
        GenericsType[] gt2 = orig.getGenericsTypes();
        if ((gt1==null || gt2==null) && gt1!=gt2) return false;
        if (gt1==gt2) return true;
        if (gt1.length!=gt2.length) return false;
        for (int i=0; i<gt1.length; i++) {
            if (!equalIncludingGenerics(gt1[i],gt2[i])) return false;
        }
        return true;
    }

    /**
     * use supplied type to make a connection from usage to declaration
     * The method operates in two modes. 
     * * For type !instanceof target a structural compare will be done 
     *   (for example Dummy<T> and List<R> to get T=R) 
     * * If type equals target, a structural match is done as well 
     *   (for example Colection<U> and Collection<E> to get U=E)
     * * otherwise we climb the hierarchy to find a case of type equals target
     *   to then execute the structural match, while applying possibly existing
     *   generics contexts on the way (for example for IntRange and Collection<E>
     *   to get E=Integer, since IntRange is an AbstractList<Integer>)
     * Should the target not have any generics this method does nothing.
     */
    static void extractGenericsConnections(Map<String, GenericsType> connections, ClassNode type, ClassNode target) {
        if (target==null || type==target || !isUsingGenericsOrIsArrayUsingGenerics(target)) return;
        if (type==UNKNOWN_PARAMETER_TYPE) return;
        if (type.isArray() && target.isArray()) {
            extractGenericsConnections(connections, type.getComponentType(), target.getComponentType());
        } else if (target.isGenericsPlaceHolder() || type.equals(target) || !implementsInterfaceOrIsSubclassOf(type, target)) {
            // structural match route
            if (target.isGenericsPlaceHolder()) {
                connections.put(target.getGenericsTypes()[0].getName(),new GenericsType(type));
            } else {
                extractGenericsConnections(connections, type.getGenericsTypes(), target.getGenericsTypes());
            }
        } else {
            // have first to find matching super class or interface
            Map <String,ClassNode> genSpec = GenericsUtils.createGenericsSpec(type);
            ClassNode superClass = getNextSuperClass(type,target);
            if (superClass!=null){
                ClassNode corrected;
                if (missesGenericsTypes(type)) {
                    corrected = superClass.getPlainNodeReference();
                } else {
                    corrected = GenericsUtils.correctToGenericsSpecRecurse(genSpec, superClass);
                }
                extractGenericsConnections(connections, corrected, target);
            } else {
                // if we reach here, we have an unhandled case 
                throw new GroovyBugError("The type "+type+" seems not to normally extend "+target+". Sorry, I cannot handle this.");
            }
        }
    }

    private static void extractGenericsConnections(Map<String, GenericsType> connections, GenericsType[] usage, GenericsType[] declaration) {
        // if declaration does not provide generics, there is no connection to make 
        if (usage==null || declaration==null || declaration.length==0) return;
        if (usage.length!=declaration.length) return;

        // both have generics
        for (int i=0; i<usage.length; i++) {
            GenericsType ui = usage[i];
            GenericsType di = declaration[i];
            if (di.isPlaceholder()) {
                connections.put(di.getName(), ui);
            } else if (di.isWildcard()){
                if (ui.isWildcard()) {
                    extractGenericsConnections(connections, ui.getLowerBound(), di.getLowerBound());
                    extractGenericsConnections(connections, ui.getUpperBounds(), di.getUpperBounds());
                } else {
                    ClassNode cu = ui.getType();
                    extractGenericsConnections(connections, cu, di.getLowerBound());
                    ClassNode[] upperBounds = di.getUpperBounds();
                    if (upperBounds!=null) {
                        for (ClassNode cn : upperBounds) {
                            extractGenericsConnections(connections, cu, cn);
                        }
                    }
                }
            } else {
                extractGenericsConnections(connections, ui.getType(), di.getType());
            }
        }
    }

    private static void extractGenericsConnections(Map<String, GenericsType> connections, ClassNode[] usage, ClassNode[] declaration) {
        if (usage==null || declaration==null || declaration.length==0) return;
        // both have generics
        for (int i=0; i<usage.length; i++) {
            ClassNode ui = usage[i];
            ClassNode di = declaration[i];
            if (di.isGenericsPlaceHolder()) {
                GenericsType gt = new GenericsType(di);
                gt.setPlaceholder(di.isGenericsPlaceHolder());
                connections.put(di.getGenericsTypes()[0].getName(), gt);
            } else if (di.isUsingGenerics()){
                extractGenericsConnections(connections, ui.getGenericsTypes(), di.getGenericsTypes());
            }
        }
    }

    static GenericsType[] getGenericsWithoutArray(ClassNode type) {
        if (type.isArray()) return getGenericsWithoutArray(type.getComponentType());
        return type.getGenericsTypes();
    }

    static Map<String, GenericsType> applyGenericsContextToParameterClass(
            Map<String, GenericsType> spec, ClassNode parameterUsage
    ) {
        GenericsType[] gts = parameterUsage.getGenericsTypes();
        if (gts==null) return Collections.EMPTY_MAP;

        GenericsType[] newGTs = applyGenericsContext(spec, gts);
        ClassNode newTarget = parameterUsage.redirect().getPlainNodeReference();
        newTarget.setGenericsTypes(newGTs);
        return GenericsUtils.extractPlaceholders(newTarget);
    }
    
    private static GenericsType[] applyGenericsContext(
            Map<String, GenericsType> spec, GenericsType[] gts
    ) {
        if (gts==null) return null;
        GenericsType[] newGTs = new GenericsType[gts.length];
        for (int i=0; i<gts.length; i++) {
            GenericsType gt = gts[i];
            newGTs[i] = applyGenericsContext(spec, gt);
        }
        return newGTs;
    }

    private static GenericsType applyGenericsContext(
            Map<String, GenericsType> spec, GenericsType gt
    ) {
        if (gt.isPlaceholder()) {
            String name = gt.getName();
            GenericsType specType = spec.get(name);
            if (specType!=null) return specType;
            if (gt.getLowerBound()!=null || gt.getUpperBounds()!=null) {
                GenericsType newType = new GenericsType(gt.getType(), applyGenericsContext(spec, gt.getUpperBounds()), applyGenericsContext(spec, gt.getLowerBound()));
                newType.setPlaceholder(true);
                return newType;
            } else {
                return gt;
            }
        } else if (gt.isWildcard()) {
            GenericsType newGT = new GenericsType(gt.getType(), applyGenericsContext(spec, gt.getUpperBounds()), applyGenericsContext(spec, gt.getLowerBound()));
            newGT.setWildcard(true);
            return newGT;
        }
        ClassNode type = gt.getType();
        if (type.getGenericsTypes()==null) return gt;
        ClassNode newType = type.getPlainNodeReference();
        newType.setGenericsPlaceHolder(type.isGenericsPlaceHolder());
        newType.setGenericsTypes(applyGenericsContext(spec, type.getGenericsTypes()));
        GenericsType newGT = new GenericsType(newType);
        return newGT;
    }

    private static ClassNode[] applyGenericsContext(
            Map<String, GenericsType> spec, ClassNode[] bounds
    ) {
        if (bounds==null) return null;
        ClassNode[] newBounds = new ClassNode[bounds.length];
        for(int i=0; i<bounds.length; i++) {
            newBounds[i] = applyGenericsContext(spec,bounds[i]);
        }
        return newBounds;
    }

    static ClassNode applyGenericsContext(
            Map<String, GenericsType> spec, ClassNode bound
    ) {
        if (bound==null) return null;
        if (bound.isArray()) {
            return applyGenericsContext(spec,bound.getComponentType()).makeArray();
        }
        if (!bound.isUsingGenerics()) return bound;
        ClassNode newBound = bound.getPlainNodeReference();
        newBound.setGenericsTypes(applyGenericsContext(spec, bound.getGenericsTypes()));
        if (bound.isGenericsPlaceHolder()) {
            GenericsType[] gt= newBound.getGenericsTypes();
            boolean hasBounds = gt[0].getLowerBound()!=null || gt[0].getUpperBounds()!=null;
            if (hasBounds || !gt[0].isPlaceholder()) return getCombinedBoundType(gt[0]);
            String placeHolderName = newBound.getGenericsTypes()[0].getName();
            if (!placeHolderName.equals(newBound.getUnresolvedName())) {
                // we should produce a clean placeholder ClassNode here
                ClassNode clean = ClassHelper.make(placeHolderName);
                clean.setGenericsTypes(newBound.getGenericsTypes());
                clean.setRedirect(newBound);
                newBound = clean;
            }
            newBound.setGenericsPlaceHolder(true);
        }
        return newBound;
    }

    private static ClassNode getCombinedBoundType(GenericsType genericsType) {
        //TODO: this method should really return some kind of meta ClassNode
        // representing the combination of all bounds. The code here, just picks
        // something out to be able to proceed and is not actually correct
        if (genericsType.getLowerBound()!=null) return genericsType.getLowerBound();
        if (genericsType.getUpperBounds()!=null) return genericsType.getUpperBounds()[0];
        return genericsType.getType();
    }

    private static void applyContextGenerics(Map<String, GenericsType> resolvedPlaceholders, Map<String, GenericsType> placeholdersFromContext) {
        if (placeholdersFromContext==null) return;
        for (Map.Entry<String, GenericsType> entry : resolvedPlaceholders.entrySet()) {
            GenericsType gt = entry.getValue();
            if (gt.isPlaceholder()) {
                String name = gt.getName();
                GenericsType outer = placeholdersFromContext.get(name);
                if (outer==null) continue;
                entry.setValue(outer);
            }
        }
    }

    private static Map<String, GenericsType> getGenericsParameterMapOfThis(ClassNode cn) {
        if (cn==null) return null;
        Map<String, GenericsType> map = null;
        if (cn.getEnclosingMethod()!=null) {
            map = extractGenericsParameterMapOfThis(cn.getEnclosingMethod());
        } else if (cn.getOuterClass()!=null) {
            map = getGenericsParameterMapOfThis(cn.getOuterClass());
        }
        map = mergeGenerics(map, cn.getGenericsTypes());
        return map;
    }

    static Map<String, GenericsType> extractGenericsParameterMapOfThis(MethodNode mn) {
        if (mn==null) return null;
        Map<String, GenericsType> map = getGenericsParameterMapOfThis(mn.getDeclaringClass());
        map = mergeGenerics(map, mn.getGenericsTypes());
        return map;
    }

    private static Map<String, GenericsType> mergeGenerics(Map<String, GenericsType> current, GenericsType[] newGenerics) {
        if (newGenerics == null || newGenerics.length == 0) return current;
        if (current==null) current = new HashMap<String, GenericsType>();
        for (int i = 0; i < newGenerics.length; i++) {
            GenericsType gt = newGenerics[i];
            if (!gt.isPlaceholder()) continue;
            String name = gt.getName();
            if (!current.containsKey(name)) current.put(name, newGenerics[i]);
        }
        return current;
    }

    /**
     * A DGM-like method which adds support for method calls which are handled
     * specifically by the Groovy compiler.
     */
    private static class ObjectArrayStaticTypesHelper {
        public static <T> T getAt(T[] arr, int index) { return null;} 
        public static <T,U extends T> void putAt(T[] arr, int index, U object) { }
    }

    /**
     * This class is used to make extension methods lookup faster. Basically, it will only
     * collect the list of extension methods (see {@link ExtensionModule} if the list of
     * extension modules has changed. It avoids recomputing the whole list each time we perform
     * a method lookup.
     */
    private static class ExtensionMethodCache {

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private List<ExtensionModule> modules = Collections.emptyList();
        private Map<String, List<MethodNode>> cachedMethods = null;
        private WeakReference<ClassLoader> origin = new WeakReference<ClassLoader>(null);

        public Map<String, List<MethodNode>> getExtensionMethods(ClassLoader loader) {
            lock.readLock().lock();
            if (loader!=origin.get()) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    final List<ExtensionModule> modules = new LinkedList<ExtensionModule>();
                    ExtensionModuleScanner scanner = new ExtensionModuleScanner(new ExtensionModuleScanner.ExtensionModuleListener() {
                        public void onModule(final ExtensionModule module) {
                            boolean skip = false;
                            for (ExtensionModule extensionModule : modules) {
                                if (extensionModule.getName().equals(module.getName())) {
                                    skip = true;
                                    break;
                                }
                            }
                            if (!skip) modules.add(module);
                        }
                    }, loader);
                    scanner.scanClasspathModules();
                    cachedMethods = getDGMMethods(modules);
                    origin = new WeakReference<ClassLoader>(loader);
                    lock.readLock().lock();
                } finally {
                    lock.writeLock().unlock();
                }
            }
            try {
                return Collections.unmodifiableMap(cachedMethods);
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Returns a map which contains, as the key, the name of a class. The value
         * consists of a list of MethodNode, one for each default groovy method found
         * which is applicable for this class.
         * @return
         * @param modules
         */
        private static Map<String, List<MethodNode>> getDGMMethods(List<ExtensionModule> modules) {
           Set<Class> instanceExtClasses = new LinkedHashSet<Class>();
           Set<Class> staticExtClasses = new LinkedHashSet<Class>();
            for (ExtensionModule module : modules) {
                if (module instanceof MetaInfExtensionModule) {
                    MetaInfExtensionModule extensionModule = (MetaInfExtensionModule) module;
                    instanceExtClasses.addAll(extensionModule.getInstanceMethodsExtensionClasses());
                    staticExtClasses.addAll(extensionModule.getStaticMethodsExtensionClasses());
                }
            }
            Map<String, List<MethodNode>> methods = new HashMap<String, List<MethodNode>>();
            Collections.addAll(instanceExtClasses, DefaultGroovyMethods.DGM_LIKE_CLASSES);
            Collections.addAll(instanceExtClasses, DefaultGroovyMethods.additionals);
            staticExtClasses.add(DefaultGroovyStaticMethods.class);
            instanceExtClasses.add(ObjectArrayStaticTypesHelper.class);
            List<Class> allClasses = new ArrayList<Class>(instanceExtClasses.size()+staticExtClasses.size());
            allClasses.addAll(instanceExtClasses);
            allClasses.addAll(staticExtClasses);
            for (Class dgmLikeClass : allClasses) {
                ClassNode cn = ClassHelper.makeWithoutCaching(dgmLikeClass, true);
                for (MethodNode metaMethod : cn.getMethods()) {
                    Parameter[] types = metaMethod.getParameters();
                    if (metaMethod.isStatic() && metaMethod.isPublic() && types.length > 0
                            && metaMethod.getAnnotations(Deprecated_TYPE).isEmpty()) {
                        Parameter[] parameters = new Parameter[types.length - 1];
                        System.arraycopy(types, 1, parameters, 0, parameters.length);
                        ExtensionMethodNode node = new ExtensionMethodNode(
                                metaMethod,
                                metaMethod.getName(),
                                metaMethod.getModifiers(),
                                metaMethod.getReturnType(),
                                parameters,
                                ClassNode.EMPTY_ARRAY, null,
                                staticExtClasses.contains(dgmLikeClass));
                        node.setGenericsTypes(metaMethod.getGenericsTypes());
                        ClassNode declaringClass = types[0].getType();
                        String declaringClassName = declaringClass.getName();
                        node.setDeclaringClass(declaringClass);

                        List<MethodNode> nodes = methods.get(declaringClassName);
                        if (nodes == null) {
                            nodes = new LinkedList<MethodNode>();
                            methods.put(declaringClassName, nodes);
                        }
                        nodes.add(node);
                    }
                }
            }
            return methods;
        }

    }

    /**
     * @return true if the class node is either a GString or the LUB of String and GString.
     */
    public static boolean isGStringOrGStringStringLUB(ClassNode node) {
        return ClassHelper.GSTRING_TYPE.equals(node)
                || GSTRING_STRING_CLASSNODE.equals(node);
    }

    /**
     * @param node the node to be tested
     * @return true if the node is using generics types and one of those types is a gstring or string/gstring lub
     */
    public static boolean isParameterizedWithGStringOrGStringString(ClassNode node) {
        if (node.isArray()) return isParameterizedWithGStringOrGStringString(node.getComponentType());
        if (node.isUsingGenerics()) {
            GenericsType[] genericsTypes = node.getGenericsTypes();
            if (genericsTypes!=null) {
                for (GenericsType genericsType : genericsTypes) {
                    if (isGStringOrGStringStringLUB(genericsType.getType())) return true;
                }
            }
        }
        return node.getSuperClass() != null && isParameterizedWithGStringOrGStringString(node.getUnresolvedSuperClass());
    }

    /**
     * @param node the node to be tested
     * @return true if the node is using generics types and one of those types is a string
     */
    public static boolean isParameterizedWithString(ClassNode node) {
        if (node.isArray()) return isParameterizedWithString(node.getComponentType());
        if (node.isUsingGenerics()) {
            GenericsType[] genericsTypes = node.getGenericsTypes();
            if (genericsTypes!=null) {
                for (GenericsType genericsType : genericsTypes) {
                    if (STRING_TYPE.equals(genericsType.getType())) return true;
                }
            }
        }
        return node.getSuperClass() != null && isParameterizedWithString(node.getUnresolvedSuperClass());
    }

    public static boolean missesGenericsTypes(ClassNode cn) {
        if (cn.isArray()) return missesGenericsTypes(cn.getComponentType());
        GenericsType[] cnTypes = cn.getGenericsTypes();
        GenericsType[] rnTypes = cn.redirect().getGenericsTypes();
        if (rnTypes!=null && cnTypes==null) return true;
        if (cnTypes!=null) {
            for (GenericsType genericsType : cnTypes) {
                if (genericsType.isPlaceholder()) return true;
            }
        }
        return false;
    }

    /**
     * A helper method that can be used to evaluate expressions as found in annotation
     * parameters. For example, it will evaluate a constant, be it referenced directly as
     * an integer or as a reference to a field.
     *
     * If this method throws an exception, then the expression cannot be evaluated on its own.
     *
     * @param expr the expression to be evaluated
     * @param config the compiler configuration
     * @return the result of the expression
     */
    public static Object evaluateExpression(Expression expr, CompilerConfiguration config) {
        String className = "Expression$" + UUID.randomUUID().toString().replace('-', '$');
        ClassNode node = new ClassNode(className, Opcodes.ACC_PUBLIC, ClassHelper.OBJECT_TYPE);
        ReturnStatement code = new ReturnStatement(expr);
        node.addMethod(new MethodNode("eval", Opcodes.ACC_PUBLIC+Opcodes.ACC_STATIC, ClassHelper.OBJECT_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, code));
        CompilerConfiguration copyConf = new CompilerConfiguration(config);
        CompilationUnit cu = new CompilationUnit(copyConf);
        cu.addClassNode(node);
        cu.compile(Phases.CLASS_GENERATION);
        @SuppressWarnings("unchecked")
        List<GroovyClass> classes = (List<GroovyClass>)cu.getClasses();
        Class aClass = cu.getClassLoader().defineClass(className, classes.get(0).getBytes());
        try {
            return aClass.getMethod("eval").invoke(null);
        } catch (IllegalAccessException e) {
            throw new GroovyBugError(e);
        } catch (InvocationTargetException e) {
            throw new GroovyBugError(e);
        } catch (NoSuchMethodException e) {
            throw new GroovyBugError(e);
        }
    }

    /**
     * Collects all interfaces of a class node, including those defined by the
     * super class.
     * @param node a class for which we want to retrieve all interfaces
     * @return a set of interfaces implemented by this class node
     */
    public static Set<ClassNode> collectAllInterfaces(ClassNode node) {
        HashSet<ClassNode> result = new HashSet<ClassNode>();
        collectAllInterfaces(node, result);
        return result;
    }

    /**
     * Collects all interfaces of a class node, including those defined by the
     * super class.
     * @param node a class for which we want to retrieve all interfaces
     * @param out the set where to collect interfaces
     */
    private static void collectAllInterfaces(final ClassNode node, final Set<ClassNode> out) {
        if (node==null) return;
        Set<ClassNode> allInterfaces = node.getAllInterfaces();
        out.addAll(allInterfaces);
        collectAllInterfaces(node.getSuperClass(), out);
    }

    /**
     * Returns true if the class node represents a the class node for the Class class
     * and if the parametrized type is a neither a placeholder or a wildcard. For example,
     * the class node Class&lt;Foo&gt; where Foo is a class would return true, but the class
     * node for Class&lt;?&gt; would return false.
     * @param classNode a class node to be tested
     * @return true if it is the class node for Class and its generic type is a real class
     */
    public static boolean isClassClassNodeWrappingConcreteType(ClassNode classNode) {
        GenericsType[] genericsTypes = classNode.getGenericsTypes();
        return ClassHelper.CLASS_Type.equals(classNode)
                && classNode.isUsingGenerics()
                && genericsTypes!=null
                && !genericsTypes[0].isPlaceholder()
                && !genericsTypes[0].isWildcard();
    }

    public static List<MethodNode> findSetters(ClassNode cn, String setterName, boolean voidOnly) {
        List<MethodNode> result = null;
        for (MethodNode method : cn.getDeclaredMethods(setterName)) {
            if (setterName.equals(method.getName())
                    && (!voidOnly || ClassHelper.VOID_TYPE==method.getReturnType())
                    && method.getParameters().length == 1) {
                if (result==null) {
                    result = new LinkedList<MethodNode>();
                }
                result.add(method);
            }
        }
        if (result==null) {
            ClassNode parent = cn.getSuperClass();
            if (parent != null) {
                return findSetters(parent, setterName, voidOnly);
            }
            return Collections.emptyList();
        }
        return result;
    }
}
