/*
 * Copyright 2003-2014 the original author or authors.
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

package org.codehaus.groovy.tools.shell.completion

import org.codehaus.groovy.tools.shell.Command
import org.codehaus.groovy.tools.shell.CommandRegistry
import org.codehaus.groovy.tools.shell.util.SimpleCompletor

/**
 * Completor for the command.names
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class CommandNameCompleter extends SimpleCompletor {
    private final CommandRegistry registry

    CommandNameCompleter(final CommandRegistry registry) {
        assert registry

        this.registry = registry
    }

    @Override
    SortedSet<String> getCandidates() {
        SortedSet<String> set = new TreeSet<String>()

        for (Command command in registry.commands()) {
            if (command.hidden) {
                continue
            }

            set << command.name
            set << command.shortcut
        }

        return set
    }
}
