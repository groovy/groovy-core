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

package org.codehaus.groovy.tools.shell.commands

/**
 * Tests for the {@link EditCommand} class.
 *
 * @author <a href="mailto:4larryj@gmail.com">Larry Jacobson</a>
 */
class EditCommandTest
    extends CommandTestSupport
{
    void testProcessBuilderInit() {
        def mockEdit = new EditCommand(shell)
        ProcessBuilder pb = mockEdit.getEditorProcessBuilder('/usr/bin/vim',
                '/var/folders/tu/tuATI/-Tmp-/groovysh-buffer1761911.groovy')

        assert pb.redirectErrorStream()

        // GROOVY-6201: Editor should inherit I/O from the current process.
        //    Fixed only for java >= 1.7 using new ProcessBuilder api
        def javaVer = Double.valueOf(System.getProperty('java.specification.version'))
        if (javaVer >= 1.7) {
            assert pb.redirectInput() == ProcessBuilder.Redirect.INHERIT
            assert pb.redirectOutput() == ProcessBuilder.Redirect.INHERIT
        }
    }

    void testEditorCompletingGroovyExpression() {
        String partialExpression1 = 'x = {'
        String partialExpression2 = '  println 2+2'
        String partialExpression3 = '}'

        def mockEdit = new EditCommand(shell)

        // type an incomplete groovy expression
        shell << partialExpression1

        // simulate a user launching an editor and completing the expression
        List<String> mockEditorContents = [partialExpression1, partialExpression2, partialExpression3]
        mockEdit.replaceCurrentBuffer(mockEditorContents)

        // assert the buffer is empty because the expression was parsed and executed on editor close
        assert shell.buffers.current() == []
    }

    void testEditorReplacingPartialGroovyExpression() {
        String partialExpression1 = 'x = {'
        String partialExpression2 = '  println 2+2'

        def mockEdit = new EditCommand(shell)

        // type an incomplete groovy expression
        shell << partialExpression1

        // simulate a user launching an editor and adding to, but not completing, the expression
        List<String> mockEditorContents = [partialExpression1, partialExpression2]
        mockEdit.replaceCurrentBuffer(mockEditorContents)

        // assert the buffer has been replaced with the editor's contents
        assert shell.buffers.current() == mockEditorContents
    }
}
