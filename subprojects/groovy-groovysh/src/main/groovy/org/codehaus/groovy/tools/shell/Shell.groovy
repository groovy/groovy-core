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

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.tools.shell.util.CommandArgumentParser
import org.codehaus.groovy.tools.shell.util.Logger
import org.fusesource.jansi.Ansi

import static org.fusesource.jansi.Ansi.ansi

/**
 * A simple shell for invoking commands from a command-line.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class Shell
{
    protected final Logger log = Logger.create(this.class)

    final CommandRegistry registry = new CommandRegistry()

    final IO io

    Shell(final IO io) {
        assert(io != null)

        this.io = io
    }

    Shell() {
        this(new IO())
    }



    /**
     * @param line the line to parse
     * @param parsedArgs accumulate the rest of the line after the command
     */
    Command findCommand(final String line, List<String> parsedArgs = null) {
        assert line

        //
        // TODO: Introduce something like 'boolean Command.accepts(String)' to ask
        //       commands if they can take the line?
        //
        //       Would like to get '!66' to invoke the 'history recall' bits, but currently has
        //       to be '! 66' for it to work with an alias like:
        //
        //           alias ! history recall
        //
        //       Or maybe allow commands to register specific syntax hacks into the registry?
        //       then ask the registry for the command for a given line?
        //

        List<String> args = CommandArgumentParser.parseLine(line, parsedArgs == null ? 1 : -1)

        assert args.size() > 0

        Command command = registry.find(args[0])

        if (command != null && args.size() > 1 && parsedArgs != null) {
            parsedArgs.addAll(args[1..-1])
        }

        return command
    }

    boolean isExecutable(final String line) {
        return findCommand(line) != null
    }

    Object execute(final String line) {
        assert line

        List<String> args = []
        Command command = findCommand(line, args)

        def result = null

        if (command) {

            log.debug("Executing command($command.name): $command; w/args: $args")
            try {
                result = command.execute(args)
            } catch (CommandException e) {
                io.err.println(ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(Ansi.Color.RED).a(e.message).reset())
            }
            log.debug("Result: ${InvokerHelper.toString(result)}")
        }

        return result
    }

    Command register(final Command command) {
        return registry.register(command)
    }

    /**
     * this should probably be deprecated
     */
    def leftShift(final String line) {
        return execute(line)
    }


    Command leftShift(final Command command) {
        return register(command)
    }
}
