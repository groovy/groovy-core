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
import org.codehaus.groovy.tools.shell.Shell

/**
 * Tests for the {@link RegisterCommand} class.
 *
 * @author <a href="mailto:chris@wensel.net">Chris K Wensel</a>
 */
class RegisterCommandTest
    extends CommandTestSupport
{
    void testRegister() {
        shell << ':register org.codehaus.groovy.tools.shell.commands.EchoCommand'
    }

    void testRegisterDupes() {
        shell << ':register org.codehaus.groovy.tools.shell.commands.EchoCommand'
        shell << ':register org.codehaus.groovy.tools.shell.commands.EchoCommand echo2 \\e2'
    }

    void testRegisterDupesFail() {
        shell << ':register org.codehaus.groovy.tools.shell.commands.EchoCommand'
        shell << ':register org.codehaus.groovy.tools.shell.commands.EchoCommand'
    }

    void testRegisterFail() {
            shell << ':register'
        }
}

class EchoCommand
    extends CommandSupport
{
    EchoCommand(final Shell shell, final String name, final String alias) {
        super(shell, name, alias)
    }

    EchoCommand(final Shell shell) {
        super(shell, ':echo', ':ec')
    }

    Object execute(final List args) {
        io.out.println(args.join(' ')) //  TODO: i18n
    }
}


