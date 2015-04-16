/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.codehaus.groovy.ast.tools;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.beans.Introspector.decapitalize;

public class BeanUtils {
    static final String GET_PREFIX = "get";
//    static final String SET_PREFIX = "set"; // TODO: do we want setter support as well? An extra flag?
    static final String IS_PREFIX = "is";

    /**
     * Get all properties including JavaBean pseudo properties matching getter conventions.
     *
     * @param type the ClassNode
     * @param includeSuperProperties whether to include super properties
     * @param includeStatic whether to include static properties
     * @param includePseudo whether to include JavaBean pseudo (getXXX/isYYY) properties with no corresponding field
     * @return the list of found property nodes
     */
    public static List<PropertyNode> getAllProperties(ClassNode type, boolean includeSuperProperties, boolean includeStatic, boolean includePseudo) {
        ClassNode node = type;
        List<PropertyNode> result = new ArrayList<PropertyNode>();
        Set<String> names = new HashSet<String>();
        while (node != null) {
            addExplicitProperties(node, result, names, includeStatic, includePseudo);
            if (includePseudo) addPseudoProperties(node, result, names, includeStatic);
            if (!includeSuperProperties) break;
            node = node.getSuperClass();
        }
        return result;
    }

    private static void addExplicitProperties(ClassNode cNode, List<PropertyNode> result, Set<String> names, boolean includeStatic, boolean includePseudo) {
        for (PropertyNode pNode : cNode.getProperties()) {
            if (includeStatic || !pNode.isStatic()) {
                result.add(pNode);
                if (includePseudo) names.add(pNode.getName());
            }
        }
    }

    private static void addPseudoProperties(ClassNode cNode, List<PropertyNode> result, Set<String> names, boolean includeStatic) {
        for (MethodNode mNode : cNode.getAllDeclaredMethods()) {
            if (!includeStatic && mNode.isStatic()) continue;
            String name = mNode.getName();
            if ((name.length() <= 3 && !name.startsWith(IS_PREFIX)) || name.equals("getClass") || name.equals("getMetaClass") || name.equals("getDeclaringClass")) {
                // Optimization: skip invalid propertyNames
                continue;
            }
            int paramCount = mNode.getParameters().length;
            ClassNode returnType = mNode.getReturnType();
            if (paramCount == 0) {
                if (name.startsWith(GET_PREFIX)) {
                    // Simple getter
                    String propName = decapitalize(name.substring(3));
                    if (!names.contains(propName)) {
                        result.add(new PropertyNode(propName, mNode.getModifiers(), returnType, cNode, null, mNode.getCode(), null));
                    }
                } else if (returnType.equals(ClassHelper.Boolean_TYPE) && name.startsWith(IS_PREFIX)) {
                    // Boolean getter
                    String propName = decapitalize(name.substring(2));
                    if (!names.contains(propName)) {
                        result.add(new PropertyNode(propName, mNode.getModifiers(), returnType, cNode, null, mNode.getCode(), null));
                    }
                }
//            } else if (paramCount == 1 && mNode.getParameters()[0].getType().equals(ClassHelper.void_WRAPPER_TYPE) && name.startsWith(SET_PREFIX)) {
//                // Simple setter
//                String propName = decapitalize(name.substring(3));
//                if (!names.contains(propName)) {
//                    result.add(new PropertyNode(propName, mNode.getModifiers(), returnType, cNode, null, null, mNode.getCode()));
//                }
            }
        }
    }
}
