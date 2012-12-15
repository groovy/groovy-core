/*
 * Copyright 2003-2012 the original author or authors.
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
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.classgen.asm.InvocationWriter;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Base class for type checking extensions written in Groovy. Compared to its superclass, {@link TypeCheckingExtension},
 * this class adds a number of utility methods aimed at leveraging the syntax of the Groovy language to improve
 * expressivity and conciseness.
 *
 * @author Cedric Champeau
 * @since 2.1.0
 */
public class GroovyTypeCheckingExtensionSupport extends TypeCheckingExtension {

    // method name to DSL name
    private final static Map<String, String> METHOD_ALIASES = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("onMethodSelection", "onMethodSelection");
                put("afterMethodCall", "afterMethodCall");
                put("beforeMethodCall", "beforeMethodCall");
                put("unresolvedVariable", "handleUnresolvedVariableExpression");
                put("unresolvedProperty", "handleUnresolvedProperty");
                put("unresolvedAttribute", "handleUnresolvedAttribute");
                put("methodNotFound", "handleMissingMethod");
                put("afterVisitMethod", "afterVisitMethod");
                put("beforeVisitMethod", "beforeVisitMethod");
                put("incompatibleAssignment", "handleIncompatibleAssignment");
            }}
    );

    private final Set<MethodNode> generatedMethods = new LinkedHashSet<MethodNode>();
    private final LinkedList<TypeCheckingScope> scopeData = new LinkedList<TypeCheckingScope>();

    // the following fields are closures executed in event-based methods
    private final Map<String, List<Closure>> eventHandlers = new HashMap<String, List<Closure>>();

    private final String scriptPath;
    private final TypeCheckingContext context;

    // this boolean is used through setHandled(boolean)
    private boolean handled = false;

    /**
     * Builds a type checking extension relying on a Groovy script (type checking DSL).
     *
     * @param typeCheckingVisitor the type checking visitor
     * @param scriptPath the path to the type checking script (in classpath)
     */
    public GroovyTypeCheckingExtensionSupport(
            final StaticTypeCheckingVisitor typeCheckingVisitor,
            final String scriptPath) {
        super(typeCheckingVisitor);
        this.scriptPath = scriptPath;
        this.context = typeCheckingVisitor.typeCheckingContext;
    }

    @Override
    public void setup() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass("org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport.TypeCheckingDSL");
        ImportCustomizer ic = new ImportCustomizer();
        ic.addStarImports("org.codehaus.groovy.ast.expr");
        ic.addStaticStars("org.codehaus.groovy.ast.ClassHelper");
        ic.addStaticStars("org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport");
        config.addCompilationCustomizers(ic);
        GroovyShell shell = new GroovyShell(config);

        ClassLoader cl = typeCheckingVisitor.getSourceUnit().getClassLoader();
        InputStream is = cl.getResourceAsStream(scriptPath);
        if (is==null) {
            // fallback to the compiler classloader
            cl = GroovyTypeCheckingExtensionSupport.class.getClassLoader();
            is = cl.getResourceAsStream(scriptPath);
        }
        try {
            TypeCheckingDSL parse = (TypeCheckingDSL) shell.parse(
                    new InputStreamReader(is, typeCheckingVisitor.getSourceUnit().getConfiguration().getSourceEncoding())
            );
            parse.extension = this;
            parse.run();
        } catch (CompilationFailedException e) {
            throw new GroovyBugError("An unexpected error was thrown during custom type checking", e);
        } catch (UnsupportedEncodingException e) {
            throw new GroovyBugError("Unsupported encoding found in compiler configuration", e);
        }
    }

    public void setHandled(final boolean handled) {
        this.handled = handled;
    }

    public TypeCheckingScope newScope() {
        TypeCheckingScope scope = new TypeCheckingScope(scopeData.peek());
        scopeData.addFirst(scope);
        return scope;
    }

    public TypeCheckingScope newScope(Closure code) {
        TypeCheckingScope scope = newScope();
        Closure callback = code.rehydrate(scope, this, this);
        callback.call();
        return scope;
    }

    public TypeCheckingScope scopeExit() {
        return scopeData.removeFirst();
    }

    public TypeCheckingScope getCurrentScope() {
        return scopeData.peek();
    }

    public TypeCheckingScope scopeExit(Closure code) {
        TypeCheckingScope scope = scopeData.peek();
        Closure copy = code.rehydrate(scope, this, this);
        copy.call();
        return scopeExit();
    }

    public boolean isGenerated(MethodNode node) {
        return generatedMethods.contains(node);
    }

    public List<MethodNode> unique(MethodNode node) {
        return Collections.singletonList(node);
    }

    public MethodNode newMethod(final String name, final Class returnType) {
        return newMethod(name, ClassHelper.make(returnType));
    }

    public MethodNode newMethod(final String name, final ClassNode returnType) {
        MethodNode node = new MethodNode(name,
                Opcodes.ACC_PUBLIC,
                returnType,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                EmptyStatement.INSTANCE);
        generatedMethods.add(node);
        return node;
    }

    public MethodNode newMethod(final String name,
                                final Callable<ClassNode> returnType) {
        MethodNode node = new MethodNode(name,
                Opcodes.ACC_PUBLIC,
                ClassHelper.OBJECT_TYPE,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                EmptyStatement.INSTANCE) {
            @Override
            public ClassNode getReturnType() {
                try {
                    return returnType.call();
                } catch (Exception e) {
                    return super.getReturnType();
                }
            }
        };
        generatedMethods.add(node);
        return node;
    }

    public void delegatesTo(ClassNode type) {
        typeCheckingVisitor.typeCheckingContext.delegationMetadata = new DelegationMetadata(type, Closure.OWNER_FIRST);
    }

    public void delegatesTo(ClassNode type, int strategy) {
        typeCheckingVisitor.typeCheckingContext.delegationMetadata = new DelegationMetadata(type, strategy);
    }

    public void delegatesTo(ClassNode type, int strategy, DelegationMetadata parent) {
        typeCheckingVisitor.typeCheckingContext.delegationMetadata = new DelegationMetadata(type, strategy, parent);
    }

    /**
     * Compares the value of an AST node property with an expected value. This method handles ClassNode instances
     * differently, allowing the user to use a Class instead of a ClassNode (for improved readability).
     *
     * @param property property to be used as a reference
     * @param value    value to compare with. May be a Class if the property is a ClassNode.
     * @return true if both values are equal
     */
    private boolean nullSafeEqualsClassNodeAware(Object property, Object value) {
        if (property == null) return value == null;
        if (property instanceof ClassNode && value instanceof Class) {
            return nullSafeEqualsClassNodeAware(property, ClassHelper.make((Class) value));
        }
        return property.equals(value);
    }

    /**
     * This method is used to provide an easy syntax to check if an AST node matches some predicate.
     *
     * @param node       the target node
     * @param predicates a map of predicates, for which the key is a property name and the value is either a value to
     *                   compare with, or a predicate in the form of a closure
     * @return <code>boolean</code> true if the node matches all predicates
     */
    public <R> R ifMatches(Map<String, ?> predicates, ASTNode node, Closure<R> code) {
        boolean match = true;
        Map properties = DefaultGroovyMethods.getProperties(node);
        for (Map.Entry<String, ?> entry : predicates.entrySet()) {
            String propertyName = entry.getKey();
            if (!properties.containsKey(propertyName)) {
                match = false;
            } else {
                Object value = properties.get(propertyName);
                Object predicate = entry.getValue();
                if (predicate instanceof Closure) {
                    Closure clone = ((Closure) predicate).rehydrate(value, this, this);
                    clone.setResolveStrategy(Closure.DELEGATE_FIRST);
                    Object result = clone.call(value);
                    if (result instanceof Boolean) {
                        match = (Boolean) result;
                    } else {
                        match = result == null ? false : DefaultGroovyMethods.asType(result, Boolean.class);
                    }
                } else {
                    match = nullSafeEqualsClassNodeAware(value, predicate);
                }
            }
            if (!match) break;
        }
        if (match) {
            return code.call();
        }
        return null;
    }

    public boolean isDynamic(VariableExpression var) {
        return var.getAccessedVariable() instanceof DynamicVariable;
    }

    public boolean isExtensionMethod(MethodNode node) {
        return node instanceof ExtensionMethodNode;
    }

    public ArgumentListExpression getArguments(MethodCall call) {
        return InvocationWriter.makeArgumentList(call.getArguments());
    }

    private Object safeCall(Closure closure, Object... args) {
        try {
            return closure.call(args);
        } catch (InvokerInvocationException err) {
            typeCheckingVisitor.getSourceUnit().addException(err);
            return null;
        }
    }

    @Override
    public void onMethodSelection(final Expression expression, final MethodNode target) {
        List<Closure> onMethodSelection = eventHandlers.get("onMethodSelection");
        if (onMethodSelection != null) {
            for (Closure closure : onMethodSelection) {
                safeCall(closure, expression, target);
            }
        }
    }

    @Override
    public void afterMethodCall(final MethodCall call) {
        List<Closure> onMethodSelection = eventHandlers.get("afterMethodCall");
        if (onMethodSelection != null) {
            for (Closure closure : onMethodSelection) {
                safeCall(closure, call);
            }
        }
    }

   @Override
    public boolean beforeMethodCall(final MethodCall call) {
       setHandled(false);
       List<Closure> onMethodSelection = eventHandlers.get("beforeMethodCall");
       if (onMethodSelection != null) {
           for (Closure closure : onMethodSelection) {
               safeCall(closure, call);
           }
       }
       return handled;
    }

    @Override
    public boolean handleUnresolvedVariableExpression(final VariableExpression vexp) {
        setHandled(false);
        List<Closure> onMethodSelection = eventHandlers.get("handleUnresolvedVariableExpression");
        if (onMethodSelection != null) {
            for (Closure closure : onMethodSelection) {
                safeCall(closure, vexp);
            }
        }
        return handled;
    }

    @Override
    public boolean handleUnresolvedProperty(final PropertyExpression pexp) {
        setHandled(false);
        List<Closure> list = eventHandlers.get("handleUnresolvedProperty");
        if (list != null) {
            for (Closure closure : list) {
                safeCall(closure, pexp);
            }
        }
        return handled;
    }

    @Override
    public boolean handleUnresolvedAttribute(final AttributeExpression aexp) {
        setHandled(false);
        List<Closure> list = eventHandlers.get("handleUnresolvedAttribute");
        if (list != null) {
            for (Closure closure : list) {
                safeCall(closure, aexp);
            }
        }
        return handled;
    }

    @Override
    public void afterVisitMethod(final MethodNode node) {
        List<Closure> list = eventHandlers.get("afterVisitMethod");
        if (list != null) {
            for (Closure closure : list) {
                safeCall(closure, node);
            }
        }
    }

    @Override
    public boolean beforeVisitMethod(final MethodNode node) {
        setHandled(false);
        List<Closure> list = eventHandlers.get("beforeVisitMethod");
        if (list != null) {
            for (Closure closure : list) {
                safeCall(closure, node);
            }
        }
        return false;
    }

    @Override
    public boolean handleIncompatibleAssignment(final ClassNode lhsType, final ClassNode rhsType, final Expression assignmentExpression) {
        setHandled(false);
        List<Closure> list = eventHandlers.get("handleIncompatibleAssignment");
        if (list != null) {
            for (Closure closure : list) {
                safeCall(closure, lhsType, rhsType, assignmentExpression);
            }
        }
        return handled;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MethodNode> handleMissingMethod(final ClassNode receiver, final String name, final ArgumentListExpression argumentList, final ClassNode[] argumentTypes, final MethodCall call) {
        List<Closure> onMethodSelection = eventHandlers.get("handleMissingMethod");
        List<MethodNode> methodList = new LinkedList<MethodNode>();
        if (onMethodSelection != null) {
            for (Closure closure : onMethodSelection) {
                Object result = safeCall(closure, receiver, name, argumentList, argumentTypes, call);
                if (result != null) {
                    if (result instanceof MethodNode) {
                        methodList.add((MethodNode) result);
                    } else if (result instanceof Collection) {
                        methodList.addAll((Collection<? extends MethodNode>) result);
                    } else {
                        throw new GroovyBugError("Type checking extension returned unexpected method list: " + result);
                    }
                }
            }
        }
        return methodList;
    }

    public boolean isMethodCall(Object o) {
        return o instanceof MethodCallExpression;
    }

    public boolean argTypesMatches(ClassNode[] argTypes, Class... classes) {
        if (classes == null) return argTypes == null || argTypes.length == 0;
        if (argTypes.length != classes.length) return false;
        boolean match = true;
        for (int i = 0; i < argTypes.length && match; i++) {
            match = argTypes[i].equals(ClassHelper.make(classes[i]));
        }
        return match;
    }

    public boolean argTypeMatches(MethodCall call, Class... classes) {
        ArgumentListExpression argumentListExpression = InvocationWriter.makeArgumentList(call.getArguments());
        ClassNode[] argumentTypes = typeCheckingVisitor.getArgumentTypes(argumentListExpression);
        return argTypesMatches(argumentTypes, classes);
    }

    public boolean firstArgTypesMatches(ClassNode[] argTypes, Class... classes) {
        if (classes == null) return argTypes == null || argTypes.length == 0;
        boolean match = true;
        int minNumOfArgs = Math.min(argTypes.length, classes.length);
        for (int i = 0; i < minNumOfArgs && match; i++) {
            match = argTypes[i].equals(ClassHelper.make(classes[i]));
        }
        return match;
    }

    public boolean firstArgTypesMatches(MethodCall call, Class... classes) {
        ArgumentListExpression argumentListExpression = InvocationWriter.makeArgumentList(call.getArguments());
        ClassNode[] argumentTypes = typeCheckingVisitor.getArgumentTypes(argumentListExpression);
        return firstArgTypesMatches(argumentTypes, classes);
    }

    public boolean argTypeMatches(ClassNode[] argTypes, int index, Class clazz) {
        if (index >= argTypes.length) return false;
        return argTypes[index].equals(ClassHelper.make(clazz));
    }

    public boolean argTypeMatches(MethodCall call, int index, Class clazz) {
        ArgumentListExpression argumentListExpression = InvocationWriter.makeArgumentList(call.getArguments());
        ClassNode[] argumentTypes = typeCheckingVisitor.getArgumentTypes(argumentListExpression);
        return argTypeMatches(argumentTypes, index, clazz);
    }

    @SuppressWarnings("unchecked")
    public <R> R withTypeChecker(Closure<R> code) {
        Closure<R> clone = (Closure<R>) code.clone();
        clone.setDelegate(code);
        clone.setResolveStrategy(Closure.DELEGATE_FIRST);
        return clone.call();
    }

    // -------------------------------------
    // delegate to the type checking context
    // -------------------------------------

    public BinaryExpression getEnclosingBinaryExpression() {
        return context.getEnclosingBinaryExpression();
    }

    public void pushEnclosingBinaryExpression(final BinaryExpression binaryExpression) {
        context.pushEnclosingBinaryExpression(binaryExpression);
    }

    public void pushEnclosingClosureExpression(final ClosureExpression closureExpression) {
        context.pushEnclosingClosureExpression(closureExpression);
    }

    public Expression getEnclosingMethodCall() {
        return context.getEnclosingMethodCall();
    }

    public Expression popEnclosingMethodCall() {
        return context.popEnclosingMethodCall();
    }

    public MethodNode popEnclosingMethod() {
        return context.popEnclosingMethod();
    }

    public ClassNode getEnclosingClassNode() {
        return context.getEnclosingClassNode();
    }

    public List<MethodNode> getEnclosingMethods() {
        return context.getEnclosingMethods();
    }

    public MethodNode getEnclosingMethod() {
        return context.getEnclosingMethod();
    }

    public void popTemporaryTypeInfo() {
        context.popTemporaryTypeInfo();
    }

    public void pushEnclosingClassNode(final ClassNode classNode) {
        context.pushEnclosingClassNode(classNode);
    }

    public BinaryExpression popEnclosingBinaryExpression() {
        return context.popEnclosingBinaryExpression();
    }

    public List<ClassNode> getEnclosingClassNodes() {
        return context.getEnclosingClassNodes();
    }

    public List<TypeCheckingContext.EnclosingClosure> getEnclosingClosureStack() {
        return context.getEnclosingClosureStack();
    }

    public ClassNode popEnclosingClassNode() {
        return context.popEnclosingClassNode();
    }

    public void pushEnclosingMethod(final MethodNode methodNode) {
        context.pushEnclosingMethod(methodNode);
    }

    public List<BinaryExpression> getEnclosingBinaryExpressionStack() {
        return context.getEnclosingBinaryExpressionStack();
    }

    public TypeCheckingContext.EnclosingClosure getEnclosingClosure() {
        return context.getEnclosingClosure();
    }

    public List<Expression> getEnclosingMethodCalls() {
        return context.getEnclosingMethodCalls();
    }

    public void pushEnclosingMethodCall(final Expression call) {
        context.pushEnclosingMethodCall(call);
    }

    public TypeCheckingContext.EnclosingClosure popEnclosingClosure() {
        return context.popEnclosingClosure();
    }

    public void pushTemporaryTypeInfo() {
        context.pushTemporaryTypeInfo();
    }

    // --------------------------------------------
    // end of delegate to the type checking context
    // --------------------------------------------

    private class TypeCheckingScope extends LinkedHashMap<String, Object> {
        private final TypeCheckingScope parent;

        private TypeCheckingScope(final TypeCheckingScope parentScope) {
            this.parent = parentScope;
        }

        public TypeCheckingScope getParent() {
            return parent;
        }

    }

    public abstract static class TypeCheckingDSL extends Script {
        private GroovyTypeCheckingExtensionSupport extension;

        @Override
        public Object getProperty(final String property) {
            try {
                return InvokerHelper.getProperty(extension, property);
            } catch (Exception e) {
                return super.getProperty(property);
            }
        }

        @Override
        public void setProperty(final String property, final Object newValue) {
            try {
                InvokerHelper.setProperty(extension, property, newValue);
            } catch (Exception e) {
                super.setProperty(property, newValue);
            }
        }

        @Override
        public Object invokeMethod(final String name, final Object args) {
            if (name.startsWith("is") && name.endsWith("Expression") && args instanceof Object[] && ((Object[]) args).length == 1) {
                String type = name.substring(2);
                Object target = ((Object[]) args)[0];
                if (target==null) return false;
                try {
                    Class typeClass = Class.forName("org.codehaus.groovy.ast.expr."+type);
                    return typeClass.isAssignableFrom(target.getClass());
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }
            if (args instanceof Object[] && ((Object[]) args).length == 1 && ((Object[]) args)[0] instanceof Closure) {
                Object[] argsArray = (Object[]) args;
                String methodName = METHOD_ALIASES.get(name);
                if (methodName == null) {
                    return InvokerHelper.invokeMethod(extension, name, args);
                }
                List<Closure> closures = extension.eventHandlers.get(methodName);
                if (closures == null) {
                    closures = new LinkedList<Closure>();
                    extension.eventHandlers.put(methodName, closures);
                }
                closures.add((Closure) argsArray[0]);
                return null;
            } else {
                return InvokerHelper.invokeMethod(extension, name, args);
            }
        }
    }
}
