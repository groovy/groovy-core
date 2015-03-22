/*
 * Copyright 2008-2014 the original author or authors.
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
package org.codehaus.groovy.transform;

import groovy.transform.ToString;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.*;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

/**
 * Handles generation of code for the @ToString annotation.
 *
 * @author Paul King
 * @author Andre Steingress
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ToStringASTTransformation extends AbstractASTTransformation {

    static final Class MY_CLASS = ToString.class;
    static final ClassNode MY_TYPE = make(MY_CLASS);
    static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode STRINGBUILDER_TYPE = make(StringBuilder.class);
    // FIXME not sure it's the right thing to exclude generics here but otherwise i get "A transform used a generics containing ClassNode" error
    private static final ClassNode THREADLOCAL_TYPE = make(ThreadLocal.class, false);
    private static final ClassNode INVOKER_TYPE = make(InvokerHelper.class);

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode anno = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(anno.getClassNode())) return;

        if (parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            if (!checkNotInterface(cNode, MY_TYPE_NAME)) return;
            boolean includeSuper = memberHasValue(anno, "includeSuper", true);
            boolean includeSuperProperties = memberHasValue(anno, "includeSuperProperties", true);
            boolean cacheToString = memberHasValue(anno, "cache", true);
            if (includeSuper && cNode.getSuperClass().getName().equals("java.lang.Object")) {
                addError("Error during " + MY_TYPE_NAME + " processing: includeSuper=true but '" + cNode.getName() + "' has no super class.", anno);
            }
            boolean includeNames = memberHasValue(anno, "includeNames", true);
            boolean includeFields = memberHasValue(anno, "includeFields", true);
            List<String> excludes = getMemberList(anno, "excludes");
            List<String> includes = getMemberList(anno, "includes");
            boolean ignoreNulls = memberHasValue(anno, "ignoreNulls", true);
            boolean includePackage = !memberHasValue(anno, "includePackage", false);
            boolean handleCycles = memberHasValue(anno, "handleCycles", true);

            if (hasAnnotation(cNode, CanonicalASTTransformation.MY_TYPE)) {
                AnnotationNode canonical = cNode.getAnnotations(CanonicalASTTransformation.MY_TYPE).get(0);
                if (excludes == null || excludes.isEmpty()) excludes = getMemberList(canonical, "excludes");
                if (includes == null || includes.isEmpty()) includes = getMemberList(canonical, "includes");
            }
            if (!checkIncludeExclude(anno, excludes, includes, MY_TYPE_NAME)) return;
            createToString(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includePackage, cacheToString, includeSuperProperties, handleCycles);
        }
    }

    public static void createToString(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames) {
        createToString(cNode, includeSuper, includeFields, excludes, includes, includeNames, false);
    }

    public static void createToString(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls) {
        createToString(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, true);
    }

    public static void createToString(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls, boolean includePackage) {
        createToString(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includePackage, false);
    }

    public static void createToString(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls, boolean includePackage, boolean cache) {
        createToString(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includePackage, cache, false);
    }

    public static void createToString(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls, boolean includePackage, boolean cache, boolean includeSuperProperties) {
        createToString(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includePackage, cache, includeSuperProperties, false);
    }

    public static void createToString(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls, boolean includePackage, boolean cache, boolean includeSuperProperties, boolean handleCycles) {
        // make a public method if none exists otherwise try a private method with leading underscore
        boolean hasExistingToString = hasDeclaredMethod(cNode, "toString", 0);
        if (hasExistingToString && hasDeclaredMethod(cNode, "_toString", 0)) return;

        final BlockStatement body = new BlockStatement();
        Expression tempToString;
        if (cache) {
            final FieldNode cacheField = cNode.addField("$to$string", ACC_PRIVATE | ACC_SYNTHETIC, ClassHelper.STRING_TYPE, null);
            final Expression savedToString = varX(cacheField);
            body.addStatement(ifS(
                    equalsNullX(savedToString),
                    assignS(savedToString, calculateToStringStatements(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includePackage, includeSuperProperties, handleCycles, body))
            ));
            tempToString = savedToString;
        } else {
            tempToString = calculateToStringStatements(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includePackage, includeSuperProperties, handleCycles, body);
        }
        body.addStatement(returnS(tempToString));

        cNode.addMethod(new MethodNode(hasExistingToString ? "_toString" : "toString", hasExistingToString ? ACC_PRIVATE : ACC_PUBLIC,
                ClassHelper.STRING_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, body));
    }

    private static Expression calculateToStringStatements(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls, boolean includePackage, boolean includeSuperProperties, boolean handleCycles, BlockStatement body) {

        // def _result = new StringBuilder()
        final Expression result = varX("_result");
        body.addStatement(declS(result, ctorX(STRINGBUILDER_TYPE)));

        String className = (includePackage) ? cNode.getName() : cNode.getNameWithoutPackage();


        if (handleCycles) {
            // private ThreadLocal $to$string$generating = new ThreadLocal();
            final FieldNode callCounterField = cNode.addField("$to$string$callCounter", ACC_PRIVATE | ACC_SYNTHETIC, THREADLOCAL_TYPE, ctorX(THREADLOCAL_TYPE));

            // if (!((Boolean)$to$string$generating.get())) { /* initialAppendStatements */ } else { /* reenteringAppendStatements */ }
            final VariableExpression callCounter = varX(callCounterField);
            TryCatchStatement initialAppendStatements = createInitialAppendStatements(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includeSuperProperties, result, className, callCounter);
            BlockStatement reenteringAppendStatements = createSubsequentAppendStatements(callCounter, result, className);
            body.addStatement
                    (ifElseS(equalsNullX(threadLocalGetX(callCounter)),
                            initialAppendStatements,
                            reenteringAppendStatements
                    ));

        } else {
            appendClassContextStatements(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includeSuperProperties, body, result, className);
        }
        // wrap up
        MethodCallExpression toString = callX(result, "toString");
        toString.setImplicitThis(false);
        return toString;
    }

    // Generates the append statements for initial call to toString
    // try { $to$string$generating.set(0); _result.append(XXX).../* append class content*/} finally {$to$string$generating.remove()}
    private static TryCatchStatement createInitialAppendStatements(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls, boolean includeSuperProperties, Expression result, String className, VariableExpression generatingThreadLocal) {
        BlockStatement tryBlock = new BlockStatement();
        tryBlock.addStatement(threadLocalSetS(generatingThreadLocal, constX(0)));
        appendClassContextStatements(cNode, includeSuper, includeFields, excludes, includes, includeNames, ignoreNulls, includeSuperProperties, tryBlock, result, className);

        return new TryCatchStatement(
                tryBlock,
                threadLocalRemoveS(generatingThreadLocal));
    }

    private static BlockStatement createSubsequentAppendStatements(VariableExpression callCounter, Expression result, String className) {
        BlockStatement block = new BlockStatement();

        block.addStatement(threadLocalSetS(callCounter, plusX(castX(Integer_TYPE, threadLocalGetX(callCounter)), constX(1))));

        // append <class_name>(...)
        block.addStatement(appendS(result, constX(className)));
        block.addStatement(appendS(result, constX("(...)")));
        return block;
    }

    private static void appendClassContextStatements(ClassNode cNode, boolean includeSuper, boolean includeFields, List<String> excludes, List<String> includes, boolean includeNames, boolean ignoreNulls, boolean includeSuperProperties, BlockStatement block, Expression result, String className) {

        // append <class_name>(
        block.addStatement(appendS(result, constX(className + "(")));

        // def $toStringFirst = true
        final VariableExpression first = varX("$toStringFirst");
        block.addStatement(declS(first, constX(Boolean.TRUE)));


        // append properties
        List<PropertyNode> pList;
        if (includeSuperProperties) {
            pList = getAllProperties(cNode);
            Iterator<PropertyNode> pIterator = pList.iterator();
            while (pIterator.hasNext()) {
                if (pIterator.next().isStatic()) {
                    pIterator.remove();
                }
            }
        } else {
            pList = getInstanceProperties(cNode);
        }
        for (PropertyNode pNode : pList) {
            if (shouldSkip(pNode.getName(), excludes, includes)) continue;
            Expression getter = callX(INVOKER_TYPE, "getProperty", args(varX("this"), constX(pNode.getName())));
            appendValue(block, result, first, getter, pNode.getName(), includeNames, ignoreNulls);
        }

        // append fields if needed
        if (includeFields) {
            List<FieldNode> fList = new ArrayList<FieldNode>();
            fList.addAll(getInstanceNonPropertyFields(cNode));
            for (FieldNode fNode : fList) {
                if (shouldSkip(fNode.getName(), excludes, includes)) continue;
                appendValue(block, result, first, varX(fNode), fNode.getName(), includeNames, ignoreNulls);
            }
        }

        // append super if needed
        if (includeSuper) {
            appendCommaIfNotFirst(block, result, first);
            appendPrefix(block, result, "super", includeNames);
            // not through MOP to avoid infinite recursion
            block.addStatement(appendS(result, callSuperX("toString")));
        }


        block.addStatement(appendS(result, constX(")")));
    }

    private static void appendValue(BlockStatement body, Expression result, VariableExpression first, Expression value, String name, boolean includeNames, boolean ignoreNulls) {
        final BlockStatement thenBlock = new BlockStatement();
        final Statement appendValue = ignoreNulls ? ifS(notNullX(value), thenBlock) : thenBlock;
        appendCommaIfNotFirst(thenBlock, result, first);
        appendPrefix(thenBlock, result, name, includeNames);
        thenBlock.addStatement(ifElseS(
                sameX(value, VariableExpression.THIS_EXPRESSION),
                appendS(result, constX("(this)")),
                appendS(result, callX(INVOKER_TYPE, "toString", value))));
        body.addStatement(appendValue);
    }

    private static void appendCommaIfNotFirst(BlockStatement body, Expression result, VariableExpression first) {
        // if ($toStringFirst) $toStringFirst = false else result.append(", ")
        body.addStatement(ifElseS(
                first,
                assignS(first, ConstantExpression.FALSE),
                appendS(result, constX(", "))));
    }

    private static void appendPrefix(BlockStatement body, Expression result, String name, boolean includeNames) {
        if (includeNames) body.addStatement(toStringPropertyName(result, name));
    }

    private static Statement toStringPropertyName(Expression result, String fName) {
        final BlockStatement body = new BlockStatement();
        body.addStatement(appendS(result, constX(fName + ":")));
        return body;
    }

    private static Statement appendS(Expression result, Expression expr) {
        MethodCallExpression append = callX(result, "append", expr);
        append.setImplicitThis(false);
        return stmt(append);
    }

    private static Statement threadLocalSetS(Expression threadLocal, Expression value) {
        MethodCallExpression set = callX(threadLocal, "set", args(value));
        set.setImplicitThis(false);
        return stmt(set);
    }

    private static Statement threadLocalRemoveS(Expression threadLocal) {
        MethodCallExpression set = callX(threadLocal, "remove");
        set.setImplicitThis(false);
        return stmt(set);
    }


    private static Expression threadLocalGetX(Expression threadLocal) {
        MethodCallExpression getX = callX(threadLocal, "get");
        getX.setImplicitThis(false);
        return getX;
    }

}
