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

import org.codehaus.groovy.GroovyException;

/** Base exception indicating a syntax error.
 *
 *  @author <a href="bob@werken.com">bob mcwhirter</a>
 *
 *  @version $Id$
 */
public class SyntaxException extends GroovyException {

    /** Line upon which the error occurred. */
    private final int startLine;
    private final int endLine;

    /** Column upon which the error occurred. */
    private final int startColumn;
    private final int endColumn;

    private String sourceLocator;

    public SyntaxException(String message, int startLine, int startColumn) {
        this(message, startLine, startColumn, startLine, startColumn+1);
    }

    public SyntaxException(String message, int startLine, int startColumn, int endLine, int endColumn) {
        super(message, false);
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    public SyntaxException(String message, Throwable cause, int startLine, int startColumn) {
        this(message, cause, startLine, startColumn, startLine, startColumn+1);
    }

    public SyntaxException(String message, Throwable cause, int startLine, int startColumn, int endLine, int endColumn) {
        super(message, cause);
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    // Properties
    // ----------------------------------------------------------------------
    public void setSourceLocator(String sourceLocator) {
        this.sourceLocator = sourceLocator;
    }

    public String getSourceLocator() {
        return this.sourceLocator;
    }

    /** Retrieve the line upon which the error occurred.
     *
     *  @return The line.
     */
    public int getLine() {
        return getStartLine();
    }

    /**
     * @return the line on which the error occurs
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Retrieve the column upon which the error occurred.
     *
     *  @return The column.
     */
    public int getStartColumn() {
        return startColumn;
    }

    /**
     * @return the end line on which the error occurs
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * @return the end column on which the error occurs
     */
    public int getEndColumn() {
        return endColumn;
    }

    public String getOriginalMessage() {
        return super.getMessage();
    }

    public String getMessage() {
        return super.getMessage() + " @ line " + startLine + ", column " + startColumn + ".";
    }
}
