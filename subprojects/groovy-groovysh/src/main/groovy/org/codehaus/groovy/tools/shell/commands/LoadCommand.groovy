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

import jline.console.completer.Completer
import org.codehaus.groovy.tools.shell.CommandSupport
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.completion.FileNameCompleter

/**
 * The 'load' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class LoadCommand
    extends CommandSupport
{
    public static final String COMMAND_NAME = ':load'

    LoadCommand(final Groovysh shell) {
        super(shell, COMMAND_NAME, ':l')

        alias('.', ':.')
    }

    @Override
    protected List<Completer> createCompleters() {
        return [ new FileNameCompleter(true, true) ]
    }

    @Override
    Object execute(final List<String> args) {
        assert args != null

        if (args.size() == 0) {
            fail("Command '$COMMAND_NAME' requires at least one argument") // TODO: i18n
        }

        for (source in args) {
            URL url

            log.debug("Attempting to load: \"$url\"")

            try {
                url = new URL("$source")
            }
            catch (MalformedURLException e) {
                def file = new File("$source")

                if (!file.exists()) {
                    fail("File not found: \"$file\"") // TODO: i18n
                }

                url = file.toURI().toURL()
            }

            load(url)
        }
    }

    void load(final URL url) {
        assert url != null

        if (io.verbose) {
            io.out.println("Loading: $url")
        }

        url.eachLine { String it ->
            shell << it as String
        }
    }
}
