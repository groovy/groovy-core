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
package org.codehaus.groovy.classgen.asm.sc;

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.asm.*;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.sc.StaticCompilationMetadataKeys;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A call site writer which replaces call site caching with static calls. This means that the generated code
 * looks more like Java code than dynamic Groovy code. Best effort is made to use JVM instructions instead of
 * calls to helper methods.
 *
 * @author Cedric Champeau
 */
public class StaticTypesCallSiteWriter extends CallSiteWriter implements Opcodes {

    private static final MethodNode GROOVYOBJECT_GETPROPERTY_METHOD = ClassHelper.GROOVY_OBJECT_TYPE.getMethod("getProperty", new Parameter[]{new Parameter(ClassHelper.STRING_TYPE, "propertyName")});
    private static final ClassNode COLLECTION_TYPE = ClassHelper.make(Collection.class);
    private static final MethodNode COLLECTION_SIZE_METHOD = COLLECTION_TYPE.getMethod("size", Parameter.EMPTY_ARRAY);

    private WriterController controller;

    public StaticTypesCallSiteWriter(final StaticTypesWriterController controller) {
        super(controller);
        this.controller = controller;
    }

    @Override
    public void generateCallSiteArray() {
    }

    @Override
    public void makeCallSite(final Expression receiver, final String message, final Expression arguments, final boolean safe, final boolean implicitThis, final boolean callCurrent, final boolean callStatic) {
    }

    @Override
    public void makeGetPropertySite(final Expression receiver, final String methodName, final boolean safe, final boolean implicitThis) {
        TypeChooser typeChooser = controller.getTypeChooser();
        ClassNode classNode = controller.getClassNode();
        ClassNode receiverType = typeChooser.resolveType(receiver, classNode);
        boolean isClassReceiver = false;
        if (receiverType.equals(ClassHelper.CLASS_Type)
                && receiverType.getGenericsTypes()!=null
                && !receiverType.getGenericsTypes()[0].isPlaceholder()) {
            isClassReceiver = true;
            receiverType = receiverType.getGenericsTypes()[0].getType();
        }
        MethodVisitor mv = controller.getMethodVisitor();
        if (receiverType.isArray() && methodName.equals("length")) {
            receiver.visit(controller.getAcg());
            mv.visitInsn(ARRAYLENGTH);
            controller.getOperandStack().replace(ClassHelper.int_TYPE);
            return;
        } else if (receiverType.implementsInterface(COLLECTION_TYPE) && ("size".equals(methodName) || "length".equals(methodName))) {
            MethodCallExpression expr = new MethodCallExpression(
                    receiver,
                    "size",
                    ArgumentListExpression.EMPTY_ARGUMENTS
            );
            expr.setMethodTarget(COLLECTION_SIZE_METHOD);
            expr.setImplicitThis(implicitThis);
            expr.visit(controller.getAcg());
            return;
        }
        if (makeGetField(receiver, receiverType, methodName, implicitThis, samePackages(receiverType.getPackageName(), classNode.getPackageName()))) return;
        if (makeGetPropertyWithGetter(receiver, receiverType, methodName)) return;
        if (receiverType.isEnum()) {
            mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(receiverType), methodName, BytecodeHelper.getTypeDescription(receiverType));
            controller.getOperandStack().push(receiverType);
            return;
        }
        if (receiver instanceof ClassExpression) {
            if (makeGetField(receiver, receiver.getType(), methodName, implicitThis, samePackages(receiverType.getPackageName(), classNode.getPackageName()))) return;
            if (makeGetPropertyWithGetter(receiver, receiver.getType(), methodName)) return;
        }
        if (isClassReceiver) {
            // we are probably looking for a property of the class
            if (makeGetField(receiver, ClassHelper.CLASS_Type, methodName, false, true)) return;
            if (makeGetPropertyWithGetter(receiver, ClassHelper.CLASS_Type, methodName)) return;
        }
        if (makeGetPrivateFieldWithBridgeMethod(receiver, receiverType, methodName, implicitThis)) return;
        controller.getSourceUnit().addError(
                new SyntaxException(
                        "Access to "+receiverType.toString(false)+"#"+methodName+" is forbidden",
                        receiver.getLineNumber(),
                        receiver.getColumnNumber()
                )
        );
        controller.getMethodVisitor().visitInsn(ACONST_NULL);
        controller.getOperandStack().push(ClassHelper.OBJECT_TYPE);
    }

    @SuppressWarnings("unchecked")
    private boolean makeGetPrivateFieldWithBridgeMethod(final Expression receiver, final ClassNode receiverType, final String fieldName, final boolean implicitThis) {
        FieldNode field = receiverType.getField(fieldName);
        ClassNode classNode = controller.getClassNode();
        if (field!=null && Modifier.isPrivate(field.getModifiers())
                && (StaticInvocationWriter.isPrivateBridgeMethodsCallAllowed(receiverType, classNode) || StaticInvocationWriter.isPrivateBridgeMethodsCallAllowed(classNode,receiverType))
                && !receiverType.equals(classNode)) {
            Map<String, MethodNode> accessors = (Map<String, MethodNode>) receiverType.redirect().getNodeMetaData(StaticCompilationMetadataKeys.PRIVATE_FIELDS_ACCESSORS);
            if (accessors!=null) {
                MethodNode methodNode = accessors.get(fieldName);
                if (methodNode!=null) {
                    MethodCallExpression mce = new MethodCallExpression(receiver, methodNode.getName(), ArgumentListExpression.EMPTY_ARGUMENTS);
                    mce.setMethodTarget(methodNode);
                    mce.setImplicitThis(implicitThis);
                    mce.visit(controller.getAcg());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void makeGroovyObjectGetPropertySite(final Expression receiver, final String methodName, final boolean safe, final boolean implicitThis) {
        TypeChooser typeChooser = controller.getTypeChooser();
        ClassNode classNode = controller.getClassNode();
        ClassNode receiverType = typeChooser.resolveType(receiver, classNode);
        
        String property = methodName;
        if (classNode.getNodeMetaData(StaticCompilationMetadataKeys.WITH_CLOSURE)!=null && "owner".equals(property)) {
            // the current class node is a closure used in a "with"
            property = "delegate";
        }
        
        if (makeGetField(receiver, receiverType, property, implicitThis, samePackages(receiverType.getPackageName(), classNode.getPackageName()))) return;
        if (makeGetPropertyWithGetter(receiver, receiverType, property)) return;
        
        MethodCallExpression call = new MethodCallExpression(
                receiver,
                "getProperty",
                new ArgumentListExpression(new ConstantExpression(property))
        );
        call.setMethodTarget(GROOVYOBJECT_GETPROPERTY_METHOD);
        call.visit(controller.getAcg());
        return;
    }

    @Override
    public void makeCallSiteArrayInitializer() {
    }

    private boolean makeGetPropertyWithGetter(final Expression receiver, final ClassNode receiverType, final String methodName) {
        // does a getter exists ?
        String getterName = "get" + MetaClassHelper.capitalize(methodName);
        MethodNode getterNode = receiverType.getGetterMethod(getterName);
        if (getterNode==null) {
            getterName = "is" + MetaClassHelper.capitalize(methodName);
            getterNode = receiverType.getGetterMethod(getterName);
        }
        if (getterNode!=null) {
            MethodCallExpression call = new MethodCallExpression(
                    receiver,
                    getterName,
                    new ArgumentListExpression()
            );
            call.setMethodTarget(getterNode);
            call.setImplicitThis(false);
            call.visit(controller.getAcg());
            return true;
        }
        return false;
    }

    private boolean makeGetField(final Expression receiver, final ClassNode receiverType, final String fieldName, final boolean implicitThis, final boolean samePackage) {
        FieldNode field = receiverType.getField(fieldName);
        // is direct access possible ?
        if (field !=null 
                && (field.isPublic() 
                    || (samePackage && field.isProtected())
                    || Modifier.isPrivate(field.getModifiers()) && receiverType.redirect()==controller.getClassNode().redirect())) {
            CompileStack compileStack = controller.getCompileStack();
            MethodVisitor mv = controller.getMethodVisitor();
            if (field.isStatic()) {
                mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(receiverType), fieldName, BytecodeHelper.getTypeDescription(field.getOriginType()));
                controller.getOperandStack().push(ClassHelper.OBJECT_TYPE);
            } else {
                if (implicitThis) {
                    compileStack.pushImplicitThis(implicitThis);
                }
                receiver.visit(controller.getAcg());
                if (implicitThis) compileStack.popImplicitThis();
                mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(receiverType), fieldName, BytecodeHelper.getTypeDescription(field.getOriginType()));
            }
            controller.getOperandStack().replace(field.getOriginType());
            return true;
        }
        ClassNode superClass = receiverType.getSuperClass();
        if (superClass !=null) {
            String receiverTypePackageName = receiverType.getPackageName();
            String superClassPackageName = superClass.getPackageName();
            boolean same = samePackage && samePackages(receiverTypePackageName, superClassPackageName);
            return makeGetField(receiver, superClass, fieldName, implicitThis, same);
        }
        return false;
    }

    private static boolean samePackages(final String pkg1, final String pkg2) {
        return (
                (pkg1 ==null && pkg2 ==null)
                || pkg1 !=null && pkg1.equals(pkg2)
                );
    }

    @Override
    public void makeSiteEntry() {
    }

    @Override
    public void prepareCallSite(final String message) {
    }

    @Override
    public void makeSingleArgumentCall(final Expression receiver, final String message, final Expression arguments) {
        TypeChooser typeChooser = controller.getTypeChooser();
        ClassNode classNode = controller.getClassNode();
        ClassNode rType = typeChooser.resolveType(receiver, classNode);
        ClassNode aType = typeChooser.resolveType(arguments, classNode);
        if (ClassHelper.getWrapper(rType).isDerivedFrom(ClassHelper.Number_TYPE)
                && ClassHelper.getWrapper(aType).isDerivedFrom(ClassHelper.Number_TYPE)) {
            if ("plus".equals(message) || "minus".equals(message) || "multiply".equals(message) || "div".equals(message)) {
                writeNumberNumberCall(receiver, message, arguments);
                return;
            } else if ("power".equals(message)) {
                writePowerCall(receiver, arguments, rType, aType);
                return;
            }
        } else if (ClassHelper.STRING_TYPE.equals(rType) && "plus".equals(message)) {
            writeStringPlusCall(receiver, message, arguments);
            return;
        } else if (rType.isArray() && "getAt".equals(message)) {
            writeArrayGet(receiver, arguments, rType, aType);
            return;
        }
        ClassNode[] args = {aType};
        // make sure Map#getAt() and List#getAt handled with the bracket syntax are properly compiled
        boolean acceptAnyMethod =
                ClassHelper.MAP_TYPE.equals(rType) || rType.implementsInterface(ClassHelper.MAP_TYPE)
                || ClassHelper.LIST_TYPE.equals(rType) || rType.implementsInterface(ClassHelper.LIST_TYPE);
        List<MethodNode> nodes = StaticTypeCheckingSupport.findDGMMethodsByNameAndArguments(rType, message, args);
        nodes = StaticTypeCheckingSupport.chooseBestMethod(rType, nodes, args);
        if (nodes.size()==1 || nodes.size()>1 && acceptAnyMethod) {
            MethodNode methodNode = nodes.get(0);
            MethodCallExpression call = new MethodCallExpression(
                    receiver,
                    message,
                    arguments
            );
            call.setMethodTarget(methodNode);
            call.visit(controller.getAcg());
            return;
        }
        // todo: more cases
        throw new GroovyBugError(
                "At line "+receiver.getLineNumber() + " column " + receiver.getColumnNumber() + "\n" +
                "On receiver: "+receiver.getText() + " with message: "+message+" and arguments: "+arguments.getText()+"\n"+
                "This method should not have been called. Please try to create a simple example reproducing this error and file" +
                "a bug report at http://jira.codehaus.org/browse/GROOVY");
    }

    private void writeArrayGet(final Expression receiver, final Expression arguments, final ClassNode rType, final ClassNode aType) {
        OperandStack operandStack = controller.getOperandStack();
        int m1 = operandStack.getStackLength();
        // visit receiver
        receiver.visit(controller.getAcg());
        // visit arguments as array index
        arguments.visit(controller.getAcg());
        operandStack.doGroovyCast(ClassHelper.int_TYPE);
        int m2 = operandStack.getStackLength();
        // array access
        controller.getMethodVisitor().visitInsn(AALOAD);
        operandStack.replace(rType.getComponentType(), m2-m1);
    }

    private void writePowerCall(Expression receiver, Expression arguments, final ClassNode rType, ClassNode aType) {
        OperandStack operandStack = controller.getOperandStack();
        int m1 = operandStack.getStackLength();
        //slow Path
        prepareSiteAndReceiver(receiver, "power", false, controller.getCompileStack().isLHS());
        visitBoxedArgument(arguments);
        int m2 = operandStack.getStackLength();
        MethodVisitor mv = controller.getMethodVisitor();
        if (ClassHelper.BigDecimal_TYPE.equals(rType) && ClassHelper.Integer_TYPE.equals(ClassHelper.getWrapper(aType))) {
            mv.visitMethodInsn(INVOKESTATIC,
                    "org/codehaus/groovy/runtime/DefaultGroovyMethods",
                    "power",
                    "(Ljava/math/BigDecimal;Ljava/lang/Integer;)Ljava/lang/Number;");
        } else if (ClassHelper.BigInteger_TYPE.equals(rType) && ClassHelper.Integer_TYPE.equals(ClassHelper.getWrapper(aType))) {
            mv.visitMethodInsn(INVOKESTATIC,
                    "org/codehaus/groovy/runtime/DefaultGroovyMethods",
                    "power",
                    "(Ljava/math/BigInteger;Ljava/lang/Integer;)Ljava/lang/Number;");
        } else if (ClassHelper.Long_TYPE.equals(ClassHelper.getWrapper(rType)) && ClassHelper.Integer_TYPE.equals(ClassHelper.getWrapper(aType))) {
            mv.visitMethodInsn(INVOKESTATIC,
                    "org/codehaus/groovy/runtime/DefaultGroovyMethods",
                    "power",
                    "(Ljava/lang/Integer;Ljava/lang/Integer;)Ljava/lang/Number;");
        } else if (ClassHelper.Integer_TYPE.equals(ClassHelper.getWrapper(rType)) && ClassHelper.Integer_TYPE.equals(ClassHelper.getWrapper(aType))) {
            mv.visitMethodInsn(INVOKESTATIC,
                    "org/codehaus/groovy/runtime/DefaultGroovyMethods",
                    "power",
                    "(Ljava/lang/Long;Ljava/lang/Integer;)Ljava/lang/Number;");
        } else {
            mv.visitMethodInsn(INVOKESTATIC,
                    "org/codehaus/groovy/runtime/DefaultGroovyMethods",
                    "power",
                    "(Ljava/lang/Number;Ljava/lang/Number;)Ljava/lang/Number;");
        }
        controller.getOperandStack().replace(ClassHelper.Number_TYPE, m2 - m1);
    }

    private void writeStringPlusCall(final Expression receiver, final String message, final Expression arguments) {
        // todo: performance would be better if we created a StringBuilder
        OperandStack operandStack = controller.getOperandStack();
        int m1 = operandStack.getStackLength();
        //slow Path
        prepareSiteAndReceiver(receiver, message, false, controller.getCompileStack().isLHS());
        visitBoxedArgument(arguments);
        int m2 = operandStack.getStackLength();
        MethodVisitor mv = controller.getMethodVisitor();
        mv.visitMethodInsn(INVOKESTATIC,
                "org/codehaus/groovy/runtime/DefaultGroovyMethods",
                "plus",
                "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");
        controller.getOperandStack().replace(ClassHelper.STRING_TYPE, m2-m1);
    }

    private void writeNumberNumberCall(final Expression receiver, final String message, final Expression arguments) {
        OperandStack operandStack = controller.getOperandStack();
        int m1 = operandStack.getStackLength();
        //slow Path
        prepareSiteAndReceiver(receiver, message, false, controller.getCompileStack().isLHS());
        visitBoxedArgument(arguments);
        int m2 = operandStack.getStackLength();
        MethodVisitor mv = controller.getMethodVisitor();
        mv.visitMethodInsn(INVOKESTATIC,
                "org/codehaus/groovy/runtime/dgmimpl/NumberNumber" + MetaClassHelper.capitalize(message),
                message,
                "(Ljava/lang/Number;Ljava/lang/Number;)Ljava/lang/Number;");
        controller.getOperandStack().replace(ClassHelper.Number_TYPE, m2 - m1);
    }


}