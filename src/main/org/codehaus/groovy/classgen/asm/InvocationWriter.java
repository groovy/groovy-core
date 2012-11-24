/*
 * Copyright 2003-2009 the original author or authors.
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
package org.codehaus.groovy.classgen.asm;

import java.util.LinkedList;
import java.util.List;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.AsmClassGenerator;
import org.codehaus.groovy.classgen.asm.OptimizingStatementWriter.StatementMeta;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class InvocationWriter {
    // method invocation
    public static final MethodCallerMultiAdapter invokeMethodOnCurrent = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeMethodOnCurrent", true, false);
    public static final MethodCallerMultiAdapter invokeMethodOnSuper = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeMethodOnSuper", true, false);
    public static final MethodCallerMultiAdapter invokeMethod = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeMethod", true, false);
    public static final MethodCallerMultiAdapter invokeStaticMethod = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeStaticMethod", true, true);
    public static final MethodCaller invokeClosureMethod = MethodCaller.newStatic(ScriptBytecodeAdapter.class, "invokeClosure");

    private WriterController controller;
    
    public InvocationWriter(WriterController wc) {
        this.controller = wc;
    }

    private void makeInvokeMethodCall(MethodCallExpression call, boolean useSuper, MethodCallerMultiAdapter adapter) {
        // receiver
        // we operate on GroovyObject if possible
        Expression objectExpression = call.getObjectExpression();
        // message name
        Expression messageName = new CastExpression(ClassHelper.STRING_TYPE, call.getMethod());
        if (useSuper) {
            ClassNode classNode = controller.isInClosure() ? controller.getOutermostClass() : controller.getClassNode(); // GROOVY-4035 
            ClassNode superClass = classNode.getSuperClass();
            makeCall(call, new ClassExpression(superClass),
                    objectExpression, messageName,
                    call.getArguments(), adapter,
                    call.isSafe(), call.isSpreadSafe(),
                    false
            );
        } else {
            makeCall(call, objectExpression, messageName,
                    call.getArguments(), adapter,
                    call.isSafe(), call.isSpreadSafe(),
                    call.isImplicitThis()
            );
        }
    }
    
    public void makeCall(
            Expression origin,
            Expression receiver, Expression message, Expression arguments,
            MethodCallerMultiAdapter adapter,
            boolean safe, boolean spreadSafe, boolean implicitThis
    ) {
        ClassNode cn = controller.getClassNode();
        if (controller.isInClosure() && !implicitThis && AsmClassGenerator.isThisExpression(receiver)) cn=cn.getOuterClass();
        makeCall(origin, new ClassExpression(cn), receiver, message, arguments,
                adapter, safe, spreadSafe, implicitThis);
    }
    
    protected boolean writeDirectMethodCall(MethodNode target, boolean implicitThis,  Expression receiver, TupleExpression args) {
        if (target==null) return false;
        
        String methodName = target.getName();
        CompileStack compileStack = controller.getCompileStack();
        OperandStack operandStack = controller.getOperandStack();
        
        MethodVisitor mv = controller.getMethodVisitor();
        int opcode = INVOKEVIRTUAL;
        if (target.isStatic()) {
            opcode = INVOKESTATIC;
        } else if (target.isPrivate() || ((receiver instanceof VariableExpression && ((VariableExpression) receiver).isSuperExpression()))) {
            opcode = INVOKESPECIAL;
        } else if (target.getDeclaringClass().isInterface()) {
            opcode = INVOKEINTERFACE;
        }

        // handle receiver
        int argumentsToRemove = 0;
        if (opcode!=INVOKESTATIC) {
            if (receiver!=null) {
                // load receiver if not static invocation
                // todo: fix inner class case
                ClassNode declaringClass = target.getDeclaringClass();
                ClassNode classNode = controller.getClassNode();
                if (implicitThis
                        && !classNode.isDerivedFrom(declaringClass)
                        && !classNode.implementsInterface(declaringClass)
                        && classNode instanceof InnerClassNode) {
                    // we are calling an outer class method
                    compileStack.pushImplicitThis(false);
                    if (controller.isInClosure()) {
                        new VariableExpression("owner").visit(controller.getAcg());
                    } else {
                        Expression expr = new PropertyExpression(new ClassExpression(declaringClass), "this");
                        expr.visit(controller.getAcg());
                    }
                } else {
                    compileStack.pushImplicitThis(implicitThis);
                    receiver.visit(controller.getAcg());
                }
                operandStack.doGroovyCast(declaringClass);
                compileStack.popImplicitThis();
                argumentsToRemove++;
            } else {
                mv.visitIntInsn(ALOAD,0);
            }
        }

        int stackSize = operandStack.getStackLength();
        loadArguments(args.getExpressions(), target.getParameters());


        String owner = BytecodeHelper.getClassInternalName(target.getDeclaringClass());
        String desc = BytecodeHelper.getMethodDescriptor(target.getReturnType(), target.getParameters());
        mv.visitMethodInsn(opcode, owner, methodName, desc);
        ClassNode ret = target.getReturnType().redirect();
        if (ret==ClassHelper.VOID_TYPE) {
            ret = ClassHelper.OBJECT_TYPE;
            mv.visitInsn(ACONST_NULL);
        }
        argumentsToRemove += (operandStack.getStackLength()-stackSize);
        controller.getOperandStack().remove(argumentsToRemove);
        controller.getOperandStack().push(ret);
        return true;
    }

    // load arguments
    protected void loadArguments(List<Expression> argumentList, Parameter[] para) {
        if (para.length==0) return;
        ClassNode lastParaType = para[para.length - 1].getOriginType();
        AsmClassGenerator acg = controller.getAcg();
        OperandStack operandStack = controller.getOperandStack();
        if (lastParaType.isArray()
                && (argumentList.size()>para.length || argumentList.size()==para.length-1 || !argumentList.get(para.length-1).getType().isArray())) {
            int stackLen = operandStack.getStackLength()+argumentList.size();
            MethodVisitor mv = controller.getMethodVisitor();
            //mv = new org.objectweb.asm.util.TraceMethodVisitor(mv);
            controller.setMethodVisitor(mv);
            // varg call
            // first parameters as usual
            for (int i = 0; i < para.length-1; i++) {
                argumentList.get(i).visit(acg);
                operandStack.doGroovyCast(para[i].getType());
            }
            // last parameters wrapped in an array
            List<Expression> lastParams = new LinkedList<Expression>();
            for (int i=para.length-1; i<argumentList.size();i++) {
                lastParams.add(argumentList.get(i));
            }
            ArrayExpression array = new ArrayExpression(
                    lastParaType.getComponentType(),
                    lastParams
            );
            array.visit(acg);
            // adjust stack length
            while (operandStack.getStackLength()<stackLen) {
                operandStack.push(ClassHelper.OBJECT_TYPE);
            }
            if (argumentList.size()==para.length-1) {
                operandStack.remove(1);
            }
        } else {
            for (int i = 0; i < argumentList.size(); i++) {
                argumentList.get(i).visit(acg);
                operandStack.doGroovyCast(para[i].getType());
            }
        }
    }

    protected boolean makeDirectCall(
        Expression origin, Expression receiver, 
        Expression message, Expression arguments,
        MethodCallerMultiAdapter adapter,
        boolean implicitThis, boolean containsSpreadExpression
    ) {
        // optimization path
        boolean fittingAdapter =   adapter == invokeMethodOnCurrent ||
                                    adapter == invokeStaticMethod;
        if (fittingAdapter && controller.optimizeForInt && controller.isFastPath()) {
            String methodName = getMethodName(message);
            if (methodName != null) {
                TupleExpression args;
                if (arguments instanceof TupleExpression) {
                    args = (TupleExpression) arguments;
                } else {
                    args = new TupleExpression(receiver);
                }

                StatementMeta meta = null;
                if (origin!=null) meta = (StatementMeta) origin.getNodeMetaData(StatementMeta.class);
                MethodNode mn = null;
                if (meta!=null) mn = meta.target;

                if (writeDirectMethodCall(mn, true, null, args)) return true;
            }
        }

        if (containsSpreadExpression) return false;
        if (origin instanceof MethodCallExpression) {
            MethodCallExpression mce = (MethodCallExpression) origin;
            MethodNode target = mce.getMethodTarget();
            return writeDirectMethodCall(target, implicitThis, receiver, makeArgumentList(arguments));
        }
        return false;
    }

    protected boolean makeCachedCall(
            Expression origin, ClassExpression sender,
            Expression receiver, Expression message, Expression arguments,
            MethodCallerMultiAdapter adapter,
            boolean safe, boolean spreadSafe, boolean implicitThis,
            boolean containsSpreadExpression
    ) {
        // prepare call site
        if ((adapter == invokeMethod || adapter == invokeMethodOnCurrent || adapter == invokeStaticMethod) && !spreadSafe) {
            String methodName = getMethodName(message);

            if (methodName != null) {
                controller.getCallSiteWriter().makeCallSite(
                        receiver, methodName, arguments, safe, implicitThis, 
                        adapter == invokeMethodOnCurrent, 
                        adapter == invokeStaticMethod);
                return true;
            }
        }
        return false;
    }
    
    protected void makeUncachedCall(
            Expression origin, ClassExpression sender,
            Expression receiver, Expression message, Expression arguments,
            MethodCallerMultiAdapter adapter,
            boolean safe, boolean spreadSafe, boolean implicitThis,
            boolean containsSpreadExpression
    ) {
        OperandStack operandStack = controller.getOperandStack();
        CompileStack compileStack = controller.getCompileStack();
        AsmClassGenerator acg = controller.getAcg();
        
        // ensure VariableArguments are read, not stored
        compileStack.pushLHS(false);

        // sender only for call sites
        if (adapter == AsmClassGenerator.setProperty) {
            ConstantExpression.NULL.visit(acg);
        } else {
            sender.visit(acg);
        }
        
        // receiver
        compileStack.pushImplicitThis(implicitThis);
        receiver.visit(acg);
        operandStack.box();
        compileStack.popImplicitThis();
        
        
        int operandsToRemove = 2;
        // message
        if (message != null) {
            message.visit(acg);
            operandStack.box();
            operandsToRemove++;
        }

        // arguments
        int numberOfArguments = containsSpreadExpression ? -1 : AsmClassGenerator.argumentSize(arguments);
        if (numberOfArguments > MethodCallerMultiAdapter.MAX_ARGS || containsSpreadExpression) {
            ArgumentListExpression ae = makeArgumentList(arguments);
            if (containsSpreadExpression) {
                acg.despreadList(ae.getExpressions(), true);
            } else {
                ae.visit(acg);
            }
        } else if (numberOfArguments > 0) {
            operandsToRemove += numberOfArguments;
            TupleExpression te = (TupleExpression) arguments;
            for (int i = 0; i < numberOfArguments; i++) {
                Expression argument = te.getExpression(i);
                argument.visit(acg);
                operandStack.box();
                if (argument instanceof CastExpression) acg.loadWrapper(argument);
            }
        }

        if (adapter==null) adapter = invokeMethod;
        adapter.call(controller.getMethodVisitor(), numberOfArguments, safe, spreadSafe);

        compileStack.popLHS();
        operandStack.replace(ClassHelper.OBJECT_TYPE,operandsToRemove);
    }
    
    protected void makeCall(
            Expression origin, ClassExpression sender,
            Expression receiver, Expression message, Expression arguments,
            MethodCallerMultiAdapter adapter,
            boolean safe, boolean spreadSafe, boolean implicitThis
    ) {
        // direct method call paths
        boolean containsSpreadExpression = AsmClassGenerator.containsSpreadExpression(arguments);
        if (makeDirectCall(origin, receiver, message, arguments, adapter, implicitThis, containsSpreadExpression)) return;

        // normal path
        if (makeCachedCall(origin, sender, receiver, message, arguments, adapter, safe, spreadSafe, implicitThis, containsSpreadExpression)) return;

        // path through ScriptBytecodeAdapter
        makeUncachedCall(origin, sender, receiver, message, arguments, adapter, safe, spreadSafe, implicitThis, containsSpreadExpression);
    }

    public static ArgumentListExpression makeArgumentList(Expression arguments) {
        ArgumentListExpression ae;
        if (arguments instanceof ArgumentListExpression) {
            ae = (ArgumentListExpression) arguments;
        } else if (arguments instanceof TupleExpression) {
            TupleExpression te = (TupleExpression) arguments;
            ae = new ArgumentListExpression(te.getExpressions());
        } else {
            ae = new ArgumentListExpression();
            ae.addExpression(arguments);
        }
        return ae;
    }

    protected String getMethodName(Expression message) {
        String methodName = null;
        if (message instanceof CastExpression) {
            CastExpression msg = (CastExpression) message;
            if (msg.getType() == ClassHelper.STRING_TYPE) {
                final Expression methodExpr = msg.getExpression();
                if (methodExpr instanceof ConstantExpression)
                  methodName = methodExpr.getText();
            }
        }

        if (methodName == null && message instanceof ConstantExpression) {
            ConstantExpression constantExpression = (ConstantExpression) message;
            methodName = constantExpression.getText();
        }
        return methodName;
    }

    public void writeInvokeMethod(MethodCallExpression call) {
        if (isClosureCall(call)) {
            // let's invoke the closure method
            invokeClosure(call.getArguments(), call.getMethodAsString());
        } else {
            boolean isSuperMethodCall = usesSuper(call);
            MethodCallerMultiAdapter adapter = invokeMethod;
            if (AsmClassGenerator.isThisExpression(call.getObjectExpression())) adapter = invokeMethodOnCurrent;
            if (isSuperMethodCall) adapter = invokeMethodOnSuper;
            if (isStaticInvocation(call)) adapter = invokeStaticMethod;
            makeInvokeMethodCall(call, isSuperMethodCall, adapter);
        }
    }

    private boolean isClosureCall(MethodCallExpression call) {
        // are we a local variable?
        // it should not be an explicitly "this" qualified method call
        // and the current class should have a possible method
        ClassNode classNode = controller.getClassNode();
        String methodName = call.getMethodAsString();
        if (methodName==null) return false;
        if (!call.isImplicitThis()) return false;
        if (!AsmClassGenerator.isThisExpression(call.getObjectExpression())) return false;
        FieldNode field = classNode.getDeclaredField(methodName);
        if (field == null) return false;
        if (isStaticInvocation(call) && !field.isStatic()) return false;
        Expression arguments = call.getArguments();
        return ! classNode.hasPossibleMethod(methodName, arguments);
    }

    private void invokeClosure(Expression arguments, String methodName) {
        AsmClassGenerator acg = controller.getAcg();
        acg.visitVariableExpression(new VariableExpression(methodName));
        controller.getOperandStack().box();
        if (arguments instanceof TupleExpression) {
            arguments.visit(acg);
        } else {
            new TupleExpression(arguments).visit(acg);
        }
        invokeClosureMethod.call(controller.getMethodVisitor());
        controller.getOperandStack().replace(ClassHelper.OBJECT_TYPE);
    }

    private boolean isStaticInvocation(MethodCallExpression call) {
        if (!AsmClassGenerator.isThisExpression(call.getObjectExpression())) return false;
        if (controller.isStaticMethod()) return true;
        return controller.isStaticContext() && !call.isImplicitThis();
    }
    
    private static boolean usesSuper(MethodCallExpression call) {
        Expression expression = call.getObjectExpression();
        if (expression instanceof VariableExpression) {
            VariableExpression varExp = (VariableExpression) expression;
            String variable = varExp.getName();
            return variable.equals("super");
        }
        return false;
    }

    public void writeInvokeStaticMethod(StaticMethodCallExpression call) {
        makeCall(call,
                new ClassExpression(call.getOwnerType()),
                new ConstantExpression(call.getMethod()),
                call.getArguments(),
                InvocationWriter.invokeStaticMethod,
                false, false, false);
    }
    
    private boolean writeDirectConstructorCall(ConstructorCallExpression call) {
        if (!controller.isFastPath()) return false;
        
        StatementMeta meta = (StatementMeta) call.getNodeMetaData(StatementMeta.class);
        ConstructorNode cn = null;
        if (meta!=null) cn = (ConstructorNode) meta.target;
        if (cn==null) return false;
        
        String ownerDescriptor = prepareConstructorCall(cn);
        TupleExpression args = makeArgumentList(call.getArguments());
        loadArguments(args.getExpressions(), cn.getParameters());
        finnishConstructorCall(cn, ownerDescriptor, args.getExpressions().size());
        
        return true;
    }
    
    protected String prepareConstructorCall(ConstructorNode cn) {
        String owner = BytecodeHelper.getClassInternalName(cn.getDeclaringClass());
        MethodVisitor mv = controller.getMethodVisitor();
        
        mv.visitTypeInsn(NEW, owner);
        mv.visitInsn(DUP);
        return owner;
    }
    
    protected void finnishConstructorCall(ConstructorNode cn, String ownerDescriptor, int argsToRemove) {
        String desc = BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, cn.getParameters());
        MethodVisitor mv = controller.getMethodVisitor();
        mv.visitMethodInsn(INVOKESPECIAL, ownerDescriptor, "<init>", desc);
        
        controller.getOperandStack().remove(argsToRemove);
        controller.getOperandStack().push(cn.getDeclaringClass());
    }

    protected void writeNormalConstructorCall(ConstructorCallExpression call) {
        Expression arguments = call.getArguments();
        if (arguments instanceof TupleExpression) {
            TupleExpression tupleExpression = (TupleExpression) arguments;
            int size = tupleExpression.getExpressions().size();
            if (size == 0) {
                arguments = MethodCallExpression.NO_ARGUMENTS;
            }
        }

        Expression receiverClass = new ClassExpression(call.getType());
        controller.getCallSiteWriter().makeCallSite(
                receiverClass, CallSiteWriter.CONSTRUCTOR,
                arguments, false, false, false,
                false);
    }
    
    public void writeInvokeConstructor(ConstructorCallExpression call) {
        if (writeDirectConstructorCall(call)) return;
        if (writeAICCall(call)) return;
        writeNormalConstructorCall(call);
    }

    protected boolean writeAICCall(ConstructorCallExpression call) {
        if (!call.isUsingAnonymousInnerClass()) return false;
        ConstructorNode cn = call.getType().getDeclaredConstructors().get(0);
        OperandStack os = controller.getOperandStack();
        
        String ownerDescriptor = prepareConstructorCall(cn);
        
        List<Expression> args = makeArgumentList(call.getArguments()).getExpressions();
        Parameter[] params = cn.getParameters(); 
        for (int i=0; i<params.length; i++) {
            Parameter p = params[i];
            Expression arg = args.get(i);
            if (arg instanceof VariableExpression) {
                VariableExpression var = (VariableExpression) arg;
                loadVariableWithReference(var);
            } else {
                arg.visit(controller.getAcg());
            }
            os.doGroovyCast(p.getType());
        }
        
        finnishConstructorCall(cn, ownerDescriptor, args.size());
        return true;
    }
    
    private void loadVariableWithReference(VariableExpression var) {
        if (!var.isUseReferenceDirectly()) {
            var.visit(controller.getAcg());
        } else {
            ClosureWriter.loadReference(var.getName(), controller);
        }
    }

    public void makeSingleArgumentCall(Expression receiver, String message, Expression arguments) {
        controller.getCallSiteWriter().makeSingleArgumentCall(receiver, message, arguments);
    }
}
