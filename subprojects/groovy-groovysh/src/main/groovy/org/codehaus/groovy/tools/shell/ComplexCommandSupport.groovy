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

package org.codehaus.groovy.tools.shell

import jline.console.completer.Completer
import org.codehaus.groovy.tools.shell.util.SimpleCompletor

/**
 * Support for more complex commands.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
abstract class ComplexCommandSupport
    extends CommandSupport
{

    protected final List<String> functions

    protected final String defaultFunction

    protected ComplexCommandSupport(final Groovysh shell, final String name, final String shortcut, final List<String> comFunctions) {
        this(shell, name, shortcut, comFunctions, null)
    }

    protected ComplexCommandSupport(final Groovysh shell, final String name, final String shortcut,
                                    final List<String> comFunctions, final String defaultFunction) {
        super(shell, name, shortcut)
        this.functions = comFunctions
        this.defaultFunction = defaultFunction
        assert(defaultFunction  == null || defaultFunction in functions)
    }

    @Override
    protected List<Completer> createCompleters() {
        def c = new SimpleCompletor()

        functions.each { String it -> c.add(it) }

        return [ c, null ]
    }

    @Override
    Object execute(List<String> args) {
        assert args != null

        if (args.size() == 0) {
            if (defaultFunction) {
                args = [ defaultFunction ]
            } else {
                fail("Command '$name' requires at least one argument of ${functions}")
            }
        }

        return executeFunction(args[0], args.tail())
    }

    protected executeFunction(final String fname, final List<String> args) {
        assert args != null

        List<String> myFunctions = functions

        if (fname in myFunctions) {
            Closure func = loadFunction(fname)

            log.debug("Invoking function '$fname' w/args: $args")

            return func.call(args)
        }
        fail("Unknown function name: '$fname'. Valid arguments: $myFunctions")
    }

    protected Closure loadFunction(final String name) {
        assert name

        try {
            return this."do_${name}"
        } catch (MissingPropertyException e) {
            fail("Failed to load delegate function: $e")
        }
    }

    def do_all = {
        functions.findAll {it != 'all'}.collect {executeFunction(it, [])}
    }
}

