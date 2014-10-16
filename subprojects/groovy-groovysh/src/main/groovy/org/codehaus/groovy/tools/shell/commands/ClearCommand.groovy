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

package org.codehaus.groovy.tools.shell.commands

import org.codehaus.groovy.tools.shell.CommandSupport
import org.codehaus.groovy.tools.shell.Groovysh

/**
 * The 'clear' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class ClearCommand
    extends CommandSupport
{
    public static final String COMMAND_NAME = ':clear'

    ClearCommand(final Groovysh shell) {
        super(shell, COMMAND_NAME, ':c')
    }

    @Override
    Object execute(final List<String> args) {
        assertNoArguments(args)

        buffer.clear()

        if (io.verbose) {
            io.out.println('Buffer cleared') //  TODO: i18n
        }
    }
}
