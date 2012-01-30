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

import java.util.List;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.classgen.AsmClassGenerator;
import org.codehaus.groovy.classgen.asm.OptimizingStatementWriter.StatementMeta;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class InvocationWriter {
    // method invocation
    private static final MethodCallerMultiAdapter invokeMethodOnCurrent = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeMethodOnCurrent", true, false);
    private static final MethodCallerMultiAdapter invokeMethodOnSuper = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeMethodOnSuper", true, false);
    private static final MethodCallerMultiAdapter invokeMethod = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeMethod", true, false);
    private static final MethodCallerMultiAdapter invokeStaticMethod = MethodCallerMultiAdapter.newStatic(ScriptBytecodeAdapter.class, "invokeStaticMethod", true, true);
    private static final MethodCaller invokeClosureMethod = MethodCaller.newStatic(ScriptBytecodeAdapter.class, "invokeClosure");

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
    
    private boolean writeDirectMethodCall(MethodNode target, boolean implicitThis,  Expression receiver, TupleExpression args) {
        if (target==null) return false;
        
        String methodName = target.getName();
        CompileStack compileStack = controller.getCompileStack();
        OperandStack operandStack = controller.getOperandStack();
        
        MethodVisitor mv = controller.getMethodVisitor();
        int opcode = INVOKEVIRTUAL;
        if (target.isStatic()) {
            opcode = INVOKESTATIC;
        } else if (target.isPrivate()) {
            opcode = INVOKESPECIAL;
        }

        // handle receiver
        int argumentsToRemove = 0;
        if (opcode!=INVOKESTATIC) {
            if (receiver!=null) {
                // load receiver if not static invocation
                compileStack.pushImplicitThis(implicitThis);
                receiver.visit(controller.getAcg());
                operandStack.doGroovyCast(target.getDeclaringClass());
                compileStack.popImplicitThis();
                argumentsToRemove++;
            } else {
                mv.visitIntInsn(ALOAD,0);
            }
        }
        
        loadArguments(args.getExpressions(), target.getParameters());

        String owner = BytecodeHelper.getClassInternalName(target.getDeclaringClass());
        String desc = BytecodeHelper.getMethodDescriptor(target.getReturnType(), target.getParameters());
        mv.visitMethodInsn(opcode, owner, methodName, desc);
        ClassNode ret = target.getReturnType().redirect();
        if (ret==ClassHelper.VOID_TYPE) {
            ret = ClassHelper.OBJECT_TYPE;
            mv.visitInsn(ACONST_NULL);
        }
        argumentsToRemove += args.getExpressions().size();
        controller.getOperandStack().remove(argumentsToRemove);
        controller.getOperandStack().push(ret);
        return true;
    }

    // load arguments
    private void loadArguments(List<Expression> argumentList, Parameter[] para) {
        for (int i=0; i<argumentList.size(); i++) {
            argumentList.get(i).visit(controller.getAcg());
            controller.getOperandStack().doGroovyCast(para[i].getType());
        }
    }

    private void makeCall(
            Expression origin, ClassExpression sender,
            Expression receiver, Expression message, Expression arguments,
            MethodCallerMultiAdapter adapter,
            boolean safe, boolean spreadSafe, boolean implicitThis
    ) { 
        // optimization path
        boolean fittingAdapter =    adapter == invokeMethodOnCurrent ||
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
                
                if (writeDirectMethodCall(mn, true, null, args)) return;                
            }
        }
        
        boolean containsSpreadExpression = AsmClassGenerator.containsSpreadExpression(arguments);
        if (!containsSpreadExpression && origin instanceof MethodCallExpression) {
            MethodCallExpression mce = (MethodCallExpression) origin;
            MethodNode target = mce.getMethodTarget();
            if (writeDirectMethodCall(target, implicitThis, receiver, makeArgumentList(arguments))) return;
        }
        
        // prepare call site
        if ((adapter == invokeMethod || adapter == invokeMethodOnCurrent || adapter == invokeStaticMethod) && !spreadSafe) {
            String methodName = getMethodName(message);

            if (methodName != null) {
                controller.getCallSiteWriter().makeCallSite(
                        receiver, methodName, arguments, safe, implicitThis, 
                        adapter == invokeMethodOnCurrent, 
                        adapter == invokeStaticMethod);
                return;
            }
        }

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

        adapter.call(controller.getMethodVisitor(), numberOfArguments, safe, spreadSafe);

        compileStack.popLHS();
        operandStack.replace(ClassHelper.OBJECT_TYPE,operandsToRemove);
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

    private String getMethodName(Expression message) {
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
    
    private String prepareConstructorCall(ConstructorNode cn) {
        String owner = BytecodeHelper.getClassInternalName(cn.getDeclaringClass());
        MethodVisitor mv = controller.getMethodVisitor();
        
        mv.visitTypeInsn(NEW, owner);
        mv.visitInsn(DUP);
        return owner;
    }
    
    private void finnishConstructorCall(ConstructorNode cn, String ownerDescriptor, int argsToRemove) {
        String desc = BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, cn.getParameters());
        MethodVisitor mv = controller.getMethodVisitor();
        mv.visitMethodInsn(INVOKESPECIAL, ownerDescriptor, "<init>", desc);
        
        controller.getOperandStack().remove(argsToRemove);
        controller.getOperandStack().push(cn.getDeclaringClass());
    }

    public void writeInvokeConstructor(ConstructorCallExpression call) {
        if (writeDirectConstructorCall(call)) return;
        if (writeAICCall(call)) return;
        
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

    private boolean writeAICCall(ConstructorCallExpression call) {
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
