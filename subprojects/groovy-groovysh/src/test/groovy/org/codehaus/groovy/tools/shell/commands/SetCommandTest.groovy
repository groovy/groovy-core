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

import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.util.PackageHelper
import org.codehaus.groovy.tools.shell.util.Preferences
import org.codehaus.groovy.tools.shell.util.SimpleCompletor

/**
 * Tests for the {@link SetCommand} class.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class SetCommandTest
    extends CommandTestSupport
{
    void testSet() {
        shell.execute(SetCommand.COMMAND_NAME)
    }

    void testComplete() {

        List<String> candidates = []
        SetCommand command = new SetCommand(shell)
        ArrayList<SimpleCompletor> completors = command.createCompleters()
        assert 2 == completors.size()
        assert 0 == completors[0].complete('', 0, candidates)
        assert Groovysh.AUTOINDENT_PREFERENCE_KEY in candidates
        assert PackageHelper.IMPORT_COMPLETION_PREFERENCE_KEY in candidates
        assert Preferences.EDITOR_KEY in candidates
        assert Preferences.PARSER_FLAVOR_KEY in candidates
        assert Preferences.SANITIZE_STACK_TRACE_KEY in candidates
        assert Preferences.SHOW_LAST_RESULT_KEY in candidates
        assert Preferences.VERBOSITY_KEY in candidates
    }
}
