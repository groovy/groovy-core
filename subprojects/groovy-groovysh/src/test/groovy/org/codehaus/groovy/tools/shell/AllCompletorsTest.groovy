/*
 * Copyright 2003-2013 the original author or authors.
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
import jline.console.history.FileHistory


/**
 * Test the combination of multiple completers via JLine ConsoleReader
 */
class AllCompletorsTest extends GroovyTestCase {

    IO testio
    BufferedOutputStream mockOut
    BufferedOutputStream mockErr
    List<Completer> completers

    /**
     * code copied from Jline console Handler,
     * need this logic to ensure completers are combined in the right way
     * The Jline contract is that completers are tried in sequence, and as
     * soon as one returns something else than -1, his canidates are used and following
     * completers ignored.
     *
     */
    private List complete(String buffer, cursor) throws IOException {
        // debug ("tab for (" + buf + ")");
        if (completers.size() == 0) {
            return null;
        }
        List candidates = new LinkedList();
        String bufstr = buffer;
        int position = -1;
        for (Completer comp : completers) {
            if ((position = comp.complete(bufstr, cursor, candidates)) != -1) {
                break;
            }
        }
        // no candidates? Fail.
        if (candidates.size() == 0) {
            return null;
        }
        return [candidates, position]
    }

    void setUp() {
        super.setUp()
        mockOut = new BufferedOutputStream(
                new ByteArrayOutputStream());

        mockErr = new BufferedOutputStream(
                new ByteArrayOutputStream());

        testio = new IO(
                new ByteArrayInputStream(),
                mockOut,
                mockErr)


        Groovysh groovysh = new Groovysh(testio)

        def filemock = new File("aaaa") {
            @Override
            boolean delete() {
                return true
            }

            @Override
            boolean isFile() {
                return true
            }
        }
        groovysh.history = new FileHistory(filemock)
        InteractiveShellRunner shellRun = new InteractiveShellRunner(groovysh, { ">"})
        // setup completers in run()
        shellRun.run()
        completers = shellRun.reader.getCompleters()
    }

    void testEmpty() {
        def result = complete("", 0)
        assertTrue(':help' in result[0])
        assertTrue(':exit' in result[0])
        assertTrue('import' in result[0])
        assertTrue(':show' in result[0])
        assertTrue(':set' in result[0])
        assertTrue(':inspect' in result[0])
        assertTrue(':doc' in result[0])
        assert 0 == result[1]
    }

    void testExitEdit() {
        assert [[":exit ", ":e", ":edit"], 0] == complete(":e", 0)
    }

    void testShow() {
        String prompt = ":show "
        assert [["all", "classes", "imports", "preferences", "variables"], prompt.length()] == complete(prompt, prompt.length())
    }

    void testShowV() {
        String prompt = ":show v"
        assert [["variables "], prompt.length() - 1] == complete(prompt, prompt.length())
    }

    void testShowVariables() {
        String prompt = ":show variables "
        assertNull(complete(prompt, prompt.length()))
    }

    void testImportJava() {
        // tests interaction with ReflectionCompleter
        String prompt = "import j"
        def result = complete(prompt, prompt.length())
        assert result
        assert prompt.length() - 1 == result[1]
        assertTrue(result.toString() ,"java." in result[0])
    }

    void testShowVariablesJava() {
        // tests against interaction with ReflectionCompleter
        String prompt = "show variables java"
        assertNull(complete(prompt, prompt.length()))
    }

    void testKeyword() {
        // tests against interaction with ReflectionCompleter
        String prompt = "pub"
        assert [["public "], 0] == complete(prompt, prompt.length())
    }

    void testCommandAndKeyword() {
        // tests against interaction with ReflectionCompleter
        String prompt = ":pu" // purge, public
        assert [[":purge "], 0] == complete(prompt, prompt.length())
    }

    void testDoc() {
        String prompt = ":doc j"
        def result = complete(prompt, prompt.length())
        assert result
        assert prompt.length() - 1 == result[1]
        assertTrue(result.toString() ,"java." in result[0])
    }

}