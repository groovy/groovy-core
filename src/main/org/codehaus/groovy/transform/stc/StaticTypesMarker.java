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
package org.codehaus.groovy.transform.stc;

/**
 * This enumeration is used by the AST transformations which rely on static type checking, either
 * to store or to retrieve information from AST node metadata. The values of this enumeration are
 * used as metadata keys.
 */
public enum StaticTypesMarker {
    INFERRED_TYPE, // used to store type information on class nodes
    DECLARATION_INFERRED_TYPE, // in flow analysis, represents the type of the declaration node lhs
    INFERRED_RETURN_TYPE, // used to store inferred return type for methods and closures
    CLOSURE_ARGUMENTS, // used to store closure argument types on a variable expression
    READONLY_PROPERTY, // used to tell that a property expression refers to a readonly property
    DIRECT_METHOD_CALL_TARGET // used to store the MethodNode a MethodCallExpression should target
}
