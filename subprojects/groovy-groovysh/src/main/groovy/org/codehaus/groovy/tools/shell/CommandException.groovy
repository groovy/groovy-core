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

package org.codehaus.groovy.tools.shell

import groovy.transform.CompileStatic;

/**
 * Thrown to indicate a problem with command execution.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
@CompileStatic
public class CommandException
        extends Exception {

    private final Command command;

    public CommandException(final Command command, String msg) {
        super(msg);
        this.command = command;
    }

    public CommandException(final Command command, String msg, Throwable cause) {
        super(msg, cause);
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }
}
