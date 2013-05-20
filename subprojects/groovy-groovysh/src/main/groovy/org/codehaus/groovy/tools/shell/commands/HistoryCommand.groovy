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

import jline.History
import org.codehaus.groovy.tools.shell.ComplexCommandSupport
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.Shell

import org.codehaus.groovy.tools.shell.util.SimpleCompletor

/**
 * The 'history' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class HistoryCommand
    extends ComplexCommandSupport
{
    HistoryCommand(final Groovysh shell) {
        super(shell, 'history', '\\H', [ 'show', 'clear', 'flush', 'recall' ], 'show')
    }

    protected List createCompletors() {
        def loader = {
            def list = []

            getFunctions().each { String fun -> list << fun }

            return list
        }

        return [
            new SimpleCompletor(loader),
            null
        ]
    }

    Object execute(List args) {
        if (!history) {
            fail("Shell does not appear to be interactive; Can not query history")
        }

        super.execute(args)

        // Don't return anything
        return null
    }

    def do_show = {
        history.historyList.eachWithIndex { item, i ->
            i = i.toString().padLeft(3, ' ')

            io.out.println(" @|bold $i|@  $item")
        }
    }

    def do_clear = {
        history.clear()

        if (io.verbose) {
            io.out.println('History cleared')
        }
    }

    def do_flush = {
        history.flushBuffer()

        if (io.verbose) {
            io.out.println('History flushed')
        }
    }

    def do_recall = {args ->
        String line

        if (!args || ((List)args).size() != 1) {
            fail("History recall requires a single history identifer")
        }

        String ids = ((List<String>)args)[0]

        //
        // FIXME: This won't work as desired because the history shifts when we run recall and could internally shift more from alias redirection
        //

        try {
            int id = Integer.parseInt(ids)
            if (shell.historyFull) {
                // if history was full before execution of the command, then the recall command itself
                // has been added to history before it actually gets executed
                // so we need to shift by one
                id--
            };
            if (id < 0) {
                line = shell.evictedLine
            } else {
                List histList = history.getHistoryList()
                line = histList.get(id)
            }
        }
        catch (NumberFormatException e) {
            fail("Invalid history identifier: $ids", e)
        }

        log.debug("Recalling history item #$ids: $line")

        return shell.execute(line)
    }
}
