/*
 * Copyright 2008-2012 the original author or authors.
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

package groovy.beans;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;

import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

/**
 * Handles generation of code for the {@code @Vetoable} annotation, and {@code @Bindable}
 * if also present.
 * <p/>
 * Generally, it adds (if needed) a VetoableChangeSupport field and
 * the needed add/removeVetoableChangeListener methods to support the
 * listeners.
 * <p/>
 * It also generates the setter and wires the setter through the
 * VetoableChangeSupport.
 * <p/>
 * If a {@link Bindable} annotation is detected it also adds support similar
 * to what {@link BindableASTTransformation} would do.
 *
 * @author Danno Ferrin (shemnon)
 * @author Chris Reeves
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class VetoableASTTransformation extends BindableASTTransformation {

    protected static ClassNode constrainedClassNode = ClassHelper.make(Vetoable.class);
    protected ClassNode vcsClassNode = ClassHelper.make(VetoableChangeSupport.class);

    /**
     * Convenience method to see if an annotated node is {@code @Vetoable}.
     *
     * @param node the node to check
     * @return true if the node is constrained
     */
    public static boolean hasVetoableAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            if (constrainedClassNode.equals(annotation.getClassNode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the bulk of the processing, mostly delegating to other methods.
     *
     * @param nodes   the AST nodes
     * @param source  the source unit for the nodes
     */
    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }
        AnnotationNode node = (AnnotationNode) nodes[0];

        if (nodes[1] instanceof ClassNode) {
            addListenerToClass(source, node, (ClassNode) nodes[1]);
        } else {
            if ((((FieldNode)nodes[1]).getModifiers() & Opcodes.ACC_FINAL) != 0) {
                source.getErrorCollector().addErrorAndContinue(new SyntaxErrorMessage(
                        new SyntaxException("@groovy.beans.Vetoable cannot annotate a final property.",
                                node.getLineNumber(), node.getColumnNumber(), node.getLastLineNumber(), node.getLastColumnNumber()),
                        source));
            }

            addListenerToProperty(source, node, (AnnotatedNode) nodes[1]);
        }
    }

    private void addListenerToProperty(SourceUnit source, AnnotationNode node, AnnotatedNode parent) {
        ClassNode declaringClass = parent.getDeclaringClass();
        FieldNode field = ((FieldNode) parent);
        String fieldName = field.getName();
        for (PropertyNode propertyNode : declaringClass.getProperties()) {
            boolean bindable = BindableASTTransformation.hasBindableAnnotation(parent)
                    || BindableASTTransformation.hasBindableAnnotation(parent.getDeclaringClass());

            if (propertyNode.getName().equals(fieldName)) {
                if (field.isStatic()) {
                    //noinspection ThrowableInstanceNeverThrown
                    source.getErrorCollector().addErrorAndContinue(new SyntaxErrorMessage(
                            new SyntaxException("@groovy.beans.Vetoable cannot annotate a static property.",
                                    node.getLineNumber(), node.getColumnNumber(), node.getLastLineNumber(), node.getLastColumnNumber()),
                            source));
                } else {
                    createListenerSetter(source, node, bindable, declaringClass, propertyNode);
                }
                return;
            }
        }
        //noinspection ThrowableInstanceNeverThrown
        source.getErrorCollector().addErrorAndContinue(new SyntaxErrorMessage(
                new SyntaxException("@groovy.beans.Vetoable must be on a property, not a field.  Try removing the private, protected, or public modifier.",
                        node.getLineNumber(), node.getColumnNumber(), node.getLastLineNumber(), node.getLastColumnNumber()),
                source));
    }


    private void addListenerToClass(SourceUnit source, AnnotationNode node, ClassNode classNode) {
        boolean bindable = BindableASTTransformation.hasBindableAnnotation(classNode);
        for (PropertyNode propertyNode : classNode.getProperties()) {
            if (!hasVetoableAnnotation(propertyNode.getField())
                && !((propertyNode.getField().getModifiers() & Opcodes.ACC_FINAL) != 0)
                && !propertyNode.getField().isStatic())
            {
                createListenerSetter(source, node,
                    bindable || BindableASTTransformation.hasBindableAnnotation(propertyNode.getField()),
                    classNode, propertyNode);
            }
        }
    }

    /**
     * Wrap an existing setter.
     */
    private void wrapSetterMethod(ClassNode classNode, boolean bindable, String propertyName) {
        String getterName = "get" + MetaClassHelper.capitalize(propertyName);
        MethodNode setter = classNode.getSetterMethod("set" + MetaClassHelper.capitalize(propertyName));

        if (setter != null) {
            // Get the existing code block
            Statement code = setter.getCode();

            VariableExpression oldValue = new VariableExpression("$oldValue");
            VariableExpression newValue = new VariableExpression("$newValue");
            VariableExpression proposedValue = new VariableExpression(setter.getParameters()[0].getName());
            BlockStatement block = new BlockStatement();

            // create a local variable to hold the old value from the getter
            block.addStatement(new ExpressionStatement(
                new DeclarationExpression(oldValue,
                    Token.newSymbol(Types.EQUALS, 0, 0),
                    new MethodCallExpression(VariableExpression.THIS_EXPRESSION, getterName, ArgumentListExpression.EMPTY_ARGUMENTS))));

            // add the fireVetoableChange method call
            block.addStatement(new ExpressionStatement(new MethodCallExpression(
                    VariableExpression.THIS_EXPRESSION,
                    "fireVetoableChange",
                    new ArgumentListExpression(
                            new Expression[]{
                                    new ConstantExpression(propertyName),
                                    oldValue,
                                    proposedValue}))));

            // call the existing block, which will presumably set the value properly
            block.addStatement(code);

            if (bindable) {
                // get the new value to emit in the event
                block.addStatement(new ExpressionStatement(
                    new DeclarationExpression(newValue,
                        Token.newSymbol(Types.EQUALS, 0, 0),
                        new MethodCallExpression(VariableExpression.THIS_EXPRESSION, getterName, ArgumentListExpression.EMPTY_ARGUMENTS))));

                // add the firePropertyChange method call
                block.addStatement(new ExpressionStatement(new MethodCallExpression(
                        VariableExpression.THIS_EXPRESSION,
                        "firePropertyChange",
                        new ArgumentListExpression(
                                new Expression[]{
                                        new ConstantExpression(propertyName),
                                        oldValue,
                                        newValue}))));
            }

            // replace the existing code block with our new one
            setter.setCode(block);
        }
    }

    private void createListenerSetter(SourceUnit source, AnnotationNode node, boolean bindable, ClassNode declaringClass, PropertyNode propertyNode) {
        if (bindable && needsPropertyChangeSupport(declaringClass, source)) {
            addPropertyChangeSupport(declaringClass);
        }
        if (needsVetoableChangeSupport(declaringClass, source)) {
            addVetoableChangeSupport(declaringClass);
        }
        String setterName = "set" + MetaClassHelper.capitalize(propertyNode.getName());
        if (declaringClass.getMethods(setterName).isEmpty()) {
            Expression fieldExpression = new FieldExpression(propertyNode.getField());
            BlockStatement setterBlock = new BlockStatement();
            setterBlock.addStatement(createConstrainedStatement(propertyNode, fieldExpression));
            if (bindable) {
                setterBlock.addStatement(createBindableStatement(propertyNode, fieldExpression));
            } else {
                setterBlock.addStatement(createSetStatement(fieldExpression));
            }

            // create method void <setter>(<type> fieldName)
            createSetterMethod(declaringClass, propertyNode, setterName, setterBlock);
        } else {
            wrapSetterMethod(declaringClass, bindable, propertyNode.getName());
        }
    }

    /**
     * Creates a statement body similar to:
     * <code>this.fireVetoableChange("field", field, field = value)</code>
     *
     * @param propertyNode           the field node for the property
     * @param fieldExpression a field expression for setting the property value
     * @return the created statement
     */
    protected Statement createConstrainedStatement(PropertyNode propertyNode, Expression fieldExpression) {
        return new ExpressionStatement(
                new MethodCallExpression(
                        VariableExpression.THIS_EXPRESSION,
                        "fireVetoableChange",
                        new ArgumentListExpression(
                                new Expression[]{
                                        new ConstantExpression(propertyNode.getName()),
                                        fieldExpression,
                                        new VariableExpression("value")})));
    }

    /**
     * Creates a statement body similar to:
     * <code>field = value</code>.
     * <p/>
     * Used when the field is not also @Bindable
     *
     * @param fieldExpression a field expression for setting the property value
     * @return the created statement
     */
    protected Statement createSetStatement(Expression fieldExpression) {
        return new ExpressionStatement(
                new BinaryExpression(
                        fieldExpression,
                        Token.newSymbol(Types.EQUAL, 0, 0),
                        new VariableExpression("value")));
    }

    /**
     * Snoops through the declaring class and all parents looking for a field
     * of type VetoableChangeSupport.  Remembers the field and returns false
     * if found otherwise returns true to indicate that such support should
     * be added.
     *
     * @param declaringClass the class to search
     * @return true if vetoable change support should be added
     */
    protected boolean needsVetoableChangeSupport(ClassNode declaringClass, SourceUnit sourceUnit) {
        boolean foundAdd = false, foundRemove = false, foundFire = false;
        ClassNode consideredClass = declaringClass;
        while (consideredClass!= null) {
            for (MethodNode method : consideredClass.getMethods()) {
                // just check length, MOP will match it up
                foundAdd = foundAdd || method.getName().equals("addVetoableChangeListener") && method.getParameters().length == 1;
                foundRemove = foundRemove || method.getName().equals("removeVetoableChangeListener") && method.getParameters().length == 1;
                foundFire = foundFire || method.getName().equals("fireVetoableChange") && method.getParameters().length == 3;
                if (foundAdd && foundRemove && foundFire) {
                    return false;
                }
            }
            consideredClass = consideredClass.getSuperClass();
        }
        // check if a super class has @Vetoable annotations
        consideredClass = declaringClass.getSuperClass();
        while (consideredClass!=null) {
            if (hasVetoableAnnotation(consideredClass)) return false;
            for (FieldNode field : consideredClass.getFields()) {
                if (hasVetoableAnnotation(field)) return false;
            }
            consideredClass = consideredClass.getSuperClass();
        }
        if (foundAdd || foundRemove || foundFire) {
            sourceUnit.getErrorCollector().addErrorAndContinue(
                new SimpleMessage("@Vetoable cannot be processed on "
                    + declaringClass.getName()
                    + " because some but not all of addVetoableChangeListener, removeVetoableChange, and fireVetoableChange were declared in the current or super classes.",
                sourceUnit)
            );
            return false;
        }
        return true;
    }

    /**
     * Creates a setter method with the given body.
     * <p/>
     * This differs from normal setters in that we need to add a declared
     * exception java.beans.PropertyVetoException
     *
     * @param declaringClass the class to which we will add the setter
     * @param propertyNode          the field to back the setter
     * @param setterName     the name of the setter
     * @param setterBlock    the statement representing the setter block
     */
    protected void createSetterMethod(ClassNode declaringClass, PropertyNode propertyNode, String setterName, Statement setterBlock) {
        Parameter[] setterParameterTypes = {new Parameter(propertyNode.getType(), "value")};
        ClassNode[] exceptions = {ClassHelper.make(PropertyVetoException.class)};
        MethodNode setter =
                new MethodNode(setterName, propertyNode.getModifiers(), ClassHelper.VOID_TYPE, setterParameterTypes, exceptions, setterBlock);
        setter.setSynthetic(true);
        // add it to the class
        declaringClass.addMethod(setter);
    }

    /**
     * Adds the necessary field and methods to support vetoable change support.
     * <p/>
     * Adds a new field:
     * <code>"protected final java.beans.VetoableChangeSupport this$vetoableChangeSupport = new java.beans.VetoableChangeSupport(this)"</code>
     * <p/>
     * Also adds support methods:
     * <code>public void addVetoableChangeListener(java.beans.VetoableChangeListener)</code>
     * <code>public void addVetoableChangeListener(String, java.beans.VetoableChangeListener)</code>
     * <code>public void removeVetoableChangeListener(java.beans.VetoableChangeListener)</code>
     * <code>public void removeVetoableChangeListener(String, java.beans.VetoableChangeListener)</code>
     * <code>public java.beans.VetoableChangeListener[] getVetoableChangeListeners()</code>
     *
     * @param declaringClass the class to which we add the support field and methods
     */
    protected void addVetoableChangeSupport(ClassNode declaringClass) {
        ClassNode vcsClassNode = ClassHelper.make(VetoableChangeSupport.class);
        ClassNode vclClassNode = ClassHelper.make(VetoableChangeListener.class);

        // add field:
        // protected static VetoableChangeSupport this$vetoableChangeSupport = new java.beans.VetoableChangeSupport(this)
        FieldNode vcsField = declaringClass.addField(
                "this$vetoableChangeSupport",
                ACC_FINAL | ACC_PRIVATE | ACC_SYNTHETIC,
                vcsClassNode,
                new ConstructorCallExpression(vcsClassNode,
                        new ArgumentListExpression(new Expression[]{new VariableExpression("this")})));

        // add method:
        // void addVetoableChangeListener(listener) {
        //     this$vetoableChangeSupport.addVetoableChangeListener(listener)
        //  }
        declaringClass.addMethod(
                new MethodNode(
                        "addVetoableChangeListener",
                        ACC_PUBLIC | ACC_SYNTHETIC,
                        ClassHelper.VOID_TYPE,
                        new Parameter[]{new Parameter(vclClassNode, "listener")},
                        ClassNode.EMPTY_ARRAY,
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        new FieldExpression(vcsField),
                                        "addVetoableChangeListener",
                                        new ArgumentListExpression(
                                                new Expression[]{new VariableExpression("listener")})))));

        // add method:
        // void addVetoableChangeListener(name, listener) {
        //     this$vetoableChangeSupport.addVetoableChangeListener(name, listener)
        //  }
        declaringClass.addMethod(
                new MethodNode(
                        "addVetoableChangeListener",
                        ACC_PUBLIC | ACC_SYNTHETIC,
                        ClassHelper.VOID_TYPE,
                        new Parameter[]{new Parameter(ClassHelper.STRING_TYPE, "name"), new Parameter(vclClassNode, "listener")},
                        ClassNode.EMPTY_ARRAY,
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        new FieldExpression(vcsField),
                                        "addVetoableChangeListener",
                                        new ArgumentListExpression(
                                                new Expression[]{new VariableExpression("name"), new VariableExpression("listener")})))));

        // add method:
        // boolean removeVetoableChangeListener(listener) {
        //    return this$vetoableChangeSupport.removeVetoableChangeListener(listener);
        // }
        declaringClass.addMethod(
                new MethodNode(
                        "removeVetoableChangeListener",
                        ACC_PUBLIC | ACC_SYNTHETIC,
                        ClassHelper.VOID_TYPE,
                        new Parameter[]{new Parameter(vclClassNode, "listener")},
                        ClassNode.EMPTY_ARRAY,
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        new FieldExpression(vcsField),
                                        "removeVetoableChangeListener",
                                        new ArgumentListExpression(
                                                new Expression[]{new VariableExpression("listener")})))));

        // add method: void removeVetoableChangeListener(name, listener)
        declaringClass.addMethod(
                new MethodNode(
                        "removeVetoableChangeListener",
                        ACC_PUBLIC | ACC_SYNTHETIC,
                        ClassHelper.VOID_TYPE,
                        new Parameter[]{new Parameter(ClassHelper.STRING_TYPE, "name"), new Parameter(vclClassNode, "listener")},
                        ClassNode.EMPTY_ARRAY,
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        new FieldExpression(vcsField),
                                        "removeVetoableChangeListener",
                                        new ArgumentListExpression(
                                                new Expression[]{new VariableExpression("name"), new VariableExpression("listener")})))));

        // add method:
        // void fireVetoableChange(String name, Object oldValue, Object newValue)
        //    throws PropertyVetoException
        // {
        //     this$vetoableChangeSupport.fireVetoableChange(name, oldValue, newValue)
        //  }
        declaringClass.addMethod(
                new MethodNode(
                        "fireVetoableChange",
                        ACC_PUBLIC | ACC_SYNTHETIC,
                        ClassHelper.VOID_TYPE,
                        new Parameter[]{new Parameter(ClassHelper.STRING_TYPE, "name"), new Parameter(ClassHelper.OBJECT_TYPE, "oldValue"), new Parameter(ClassHelper.OBJECT_TYPE, "newValue")},
                        new ClassNode[] {ClassHelper.make(PropertyVetoException.class)},
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        new FieldExpression(vcsField),
                                        "fireVetoableChange",
                                        new ArgumentListExpression(
                                                new Expression[]{
                                                        new VariableExpression("name"),
                                                        new VariableExpression("oldValue"),
                                                        new VariableExpression("newValue")})))));

        // add method:
        // VetoableChangeListener[] getVetoableChangeListeners() {
        //   return this$vetoableChangeSupport.getVetoableChangeListeners
        // }
        declaringClass.addMethod(
                new MethodNode(
                        "getVetoableChangeListeners",
                        ACC_PUBLIC | ACC_SYNTHETIC,
                        vclClassNode.makeArray(),
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        new ReturnStatement(
                                new ExpressionStatement(
                                        new MethodCallExpression(
                                                new FieldExpression(vcsField),
                                                "getVetoableChangeListeners",
                                                ArgumentListExpression.EMPTY_ARGUMENTS)))));

        // add method:
        // VetoableChangeListener[] getVetoableChangeListeners(String name) {
        //   return this$vetoableChangeSupport.getVetoableChangeListeners(name)
        // }
        declaringClass.addMethod(
                new MethodNode(
                        "getVetoableChangeListeners",
                        ACC_PUBLIC | ACC_SYNTHETIC,
                        vclClassNode.makeArray(),
                        new Parameter[]{new Parameter(ClassHelper.STRING_TYPE, "name")},
                        ClassNode.EMPTY_ARRAY,
                        new ReturnStatement(
                                new ExpressionStatement(
                                        new MethodCallExpression(
                                                new FieldExpression(vcsField),
                                                "getVetoableChangeListeners",
                                                new ArgumentListExpression(
                                                new Expression[]{new VariableExpression("name")}))))));
    }

}
