/*
 * Copyright 2003-2013 the original author or authors.
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

public class TokenException extends SyntaxException {
    public TokenException(String message, Token token) {
        super(
                (token == null)
                        ? message + ". No token"
                        : message,
                getLine(token),
                getColumn(token));
    }

    public TokenException(String message, Throwable cause, int line, int column) {
        super(message, cause, line, column);
    }

    public TokenException(String message, Throwable cause, int line, int column, int endLine, int endColumn) {
        super(message, cause, line, column, endLine, endColumn);
    }

    private static int getColumn(Token token) {
        return (token != null) ? token.getStartColumn() : -1;
    }

    private static int getLine(Token token) {
        return (token != null) ? token.getStartLine() : -1;
    }

}
