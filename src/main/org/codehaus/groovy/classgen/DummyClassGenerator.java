/*
 * Copyright 2003-2007 the original author or authors.
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
package org.codehaus.groovy.classgen;

import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.asm.BytecodeHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Iterator;

/**
 * To generate a class that has all the fields and methods, except that fields are not initialized
 * and methods are empty. It's intended for being used as a place holder during code generation
 * of reference to the "this" class itself.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author <a href="mailto:b55r@sina.com">Bing Ran</a>
 * @version $Revision$
 */
public class DummyClassGenerator extends ClassGenerator {

    private ClassVisitor cv;
    private MethodVisitor mv;
    private GeneratorContext context;

    // current class details
    private ClassNode classNode;
    private String internalClassName;
    private String internalBaseClassName;


    public DummyClassGenerator(
            GeneratorContext context,
            ClassVisitor classVisitor,
            ClassLoader classLoader,
            String sourceFile) {
        this.context = context;
        this.cv = classVisitor;
    }

    // GroovyClassVisitor interface
    //-------------------------------------------------------------------------
    public void visitClass(ClassNode classNode) {
        try {
            this.classNode = classNode;
            this.internalClassName = BytecodeHelper.getClassInternalName(classNode);

            //System.out.println("Generating class: " + classNode.getName());

            this.internalBaseClassName = BytecodeHelper.getClassInternalName(classNode.getSuperClass());

            cv.visit(
                    Opcodes.V1_3,
                    classNode.getModifiers(),
                    internalClassName,
                    (String) null,
                    internalBaseClassName,
                    BytecodeHelper.getClassInternalNames(classNode.getInterfaces())
            );

            classNode.visitContents(this);

            for (Iterator iter = innerClasses.iterator(); iter.hasNext();) {
                ClassNode innerClass = (ClassNode) iter.next();
                ClassNode innerClassType = innerClass;
                String innerClassInternalName = BytecodeHelper.getClassInternalName(innerClassType);
                String outerClassName = internalClassName; // default for inner classes
                MethodNode enclosingMethod = innerClass.getEnclosingMethod();
                if (enclosingMethod != null) {
                    // local inner classes do not specify the outer class name
                    outerClassName = null;
                }
                cv.visitInnerClass(
                        innerClassInternalName,
                        outerClassName,
                        innerClassType.getName(),
                        innerClass.getModifiers());
            }
            cv.visitEnd();
        }
        catch (GroovyRuntimeException e) {
            e.setModule(classNode.getModule());
            throw e;
        }
    }

    public void visitConstructor(ConstructorNode node) {

        visitParameters(node, node.getParameters());

        String methodType = BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, node.getParameters());
        mv = cv.visitMethod(node.getModifiers(), "<init>", methodType, null, null);
        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("not intended for execution");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);
        mv.visitMaxs(0, 0);
    }

    public void visitMethod(MethodNode node) {

        visitParameters(node, node.getParameters());

        String methodType = BytecodeHelper.getMethodDescriptor(node.getReturnType(), node.getParameters());
        mv = cv.visitMethod(node.getModifiers(), node.getName(), methodType, null, null);

        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("not intended for execution");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);

        mv.visitMaxs(0, 0);
    }

    public void visitField(FieldNode fieldNode) {

        cv.visitField(
                fieldNode.getModifiers(),
                fieldNode.getName(),
                BytecodeHelper.getTypeDescription(fieldNode.getType()),
                null, //fieldValue,  //br  all the sudden that one cannot init the field here. init is done in static initializer and instance initializer.
                null);
    }

    /**
     * Creates a getter, setter and field
     */
    public void visitProperty(PropertyNode statement) {
    }

    protected CompileUnit getCompileUnit() {
        CompileUnit answer = classNode.getCompileUnit();
        if (answer == null) {
            answer = context.getCompileUnit();
        }
        return answer;
    }

    protected void visitParameters(ASTNode node, Parameter[] parameters) {
        for (int i = 0, size = parameters.length; i < size; i++) {
            visitParameter(node, parameters[i]);
        }
    }

    protected void visitParameter(ASTNode node, Parameter parameter) {
    }


    public void visitAnnotations(AnnotatedNode node) {
    }
}
