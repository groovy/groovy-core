/*
 * Copyright 2003-2012 the original author or authors.
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
package org.codehaus.groovy.syntax;

import groovy.lang.GroovyRuntimeException;

import org.codehaus.groovy.ast.ASTNode;

/**
 * A helper class to allow parser exceptions to be thrown anywhere in the code.
 * Should be replaced when no longer required.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class RuntimeParserException extends GroovyRuntimeException {

    public RuntimeParserException(String message, ASTNode node) {
        super(message + "\n", node);
    }

    public void throwParserException() throws SyntaxException {
        final ASTNode node = getNode();
        throw new SyntaxException(getMessage(), node.getLineNumber(), node.getColumnNumber(), node.getLastLineNumber(), node.getLastColumnNumber());
    }

}
