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

import groovy.transform.ExternalizeMethods;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.getInstanceNonPropertyFields;
import static org.codehaus.groovy.ast.tools.GeneralUtils.getInstancePropertyFields;
import static org.codehaus.groovy.ast.tools.GeneralUtils.param;
import static org.codehaus.groovy.ast.tools.GeneralUtils.params;
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;

/**
 * Handles generation of code for the @ExternalizeMethods annotation.
 *
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ExternalizeMethodsASTTransformation extends AbstractASTTransformation {
    static final Class MY_CLASS = ExternalizeMethods.class;
    static final ClassNode MY_TYPE = make(MY_CLASS);
    static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode EXTERNALIZABLE_TYPE = make(Externalizable.class);
    private static final ClassNode OBJECTOUTPUT_TYPE = make(ObjectOutput.class);
    private static final ClassNode OBJECTINPUT_TYPE = make(ObjectInput.class);

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode anno = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(anno.getClassNode())) return;

        if (parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            if (!checkNotInterface(cNode, MY_TYPE_NAME)) return;
            cNode.addInterface(EXTERNALIZABLE_TYPE);
            boolean includeFields = memberHasValue(anno, "includeFields", true);
            List<String> excludes = getMemberList(anno, "excludes");
            boolean checkPropertyNames = !memberHasValue(anno, "checkPropertyNames", false);
            if (checkPropertyNames) {
                if (!checkPropertyList(cNode, excludes, "excludes", anno, MY_TYPE_NAME, includeFields)) return;
            }
            List<FieldNode> list = getInstancePropertyFields(cNode);
            if (includeFields) {
                list.addAll(getInstanceNonPropertyFields(cNode));
            }
            createWriteExternal(cNode, excludes, list);
            createReadExternal(cNode, excludes, list);
        }
    }

    private void createWriteExternal(ClassNode cNode, List<String> excludes, List<FieldNode> list) {
        final BlockStatement body = new BlockStatement();
        Parameter out = param(OBJECTOUTPUT_TYPE, "out");
        for (FieldNode fNode : list) {
            if (excludes.contains(fNode.getName())) continue;
            if ((fNode.getModifiers() & ACC_TRANSIENT) != 0) continue;
            body.addStatement(stmt(callX(varX(out), "write" + suffixForField(fNode), varX(fNode))));
        }
        ClassNode[] exceptions = {make(IOException.class)};
        cNode.addMethod("writeExternal", ACC_PUBLIC, ClassHelper.VOID_TYPE, params(out), exceptions, body);
    }

    private void createReadExternal(ClassNode cNode, List<String> excludes, List<FieldNode> list) {
        final BlockStatement body = new BlockStatement();
        Parameter oin = param(OBJECTINPUT_TYPE, "oin");
        for (FieldNode fNode : list) {
            if (excludes.contains(fNode.getName())) continue;
            if ((fNode.getModifiers() & ACC_TRANSIENT) != 0) continue;
            String suffix = suffixForField(fNode);
            Expression readObject = callX(varX(oin), "read" + suffix);
            body.addStatement(assignS(varX(fNode), suffix.equals("Object") ? castX(GenericsUtils.nonGeneric(fNode.getType()), readObject) : readObject));
        }
        cNode.addMethod("readExternal", ACC_PUBLIC, ClassHelper.VOID_TYPE, params(oin), ClassNode.EMPTY_ARRAY, body);
    }

    private String suffixForField(FieldNode fNode) {
        // use primitives for efficiency
        if (fNode.getType() == ClassHelper.int_TYPE) return "Int";
        if (fNode.getType() == ClassHelper.boolean_TYPE) return "Boolean";
//        currently char isn't found due to a bug, so go with Object
//        if (fNode.getType() == ClassHelper.char_TYPE) return "Char";
        if (fNode.getType() == ClassHelper.long_TYPE) return "Long";
        if (fNode.getType() == ClassHelper.short_TYPE) return "Short";
        if (fNode.getType() == ClassHelper.byte_TYPE) return "Byte";
        if (fNode.getType() == ClassHelper.float_TYPE) return "Float";
        if (fNode.getType() == ClassHelper.double_TYPE) return "Double";
        return "Object";
    }
}
