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

package org.codehaus.groovy.tools.shell.completion

import org.codehaus.groovy.tools.shell.CompletorTestSupport
import org.codehaus.groovy.tools.shell.Groovysh
import static org.codehaus.groovy.tools.shell.completion.TokenUtilTest.tokenList

class VariableCompletorTest extends CompletorTestSupport {

    void testKnownVar() {
        groovyshMocker.demand.getInterp(1) { [context: [variables: [xyzabc: '']]] }
        groovyshMocker.use {
            Groovysh groovyshMock = new Groovysh()
            VariableSyntaxCompletor completor = new VariableSyntaxCompletor(groovyshMock)
            def candidates = []
            assert completor.complete(tokenList('xyz'), candidates)
            assert ['xyzabc'] == candidates
        }
    }

    void testKnownVarMultiple() {
        groovyshMocker.demand.getInterp(1) { [context: [variables: [bad: '', xyzabc: '', xyzfff: '', nope: '']]] }
        groovyshMocker.use {
            Groovysh groovyshMock = new Groovysh()
            VariableSyntaxCompletor completor = new VariableSyntaxCompletor(groovyshMock)
            def candidates = []
            assert completor.complete(tokenList('xyz'), candidates)
            assert ['xyzabc', 'xyzfff'] == candidates
        }
    }

    void testUnknownVar() {
        groovyshMocker.demand.getInterp(1) { [context: [variables: [:]]] }
        groovyshMocker.use {
            Groovysh groovyshMock = new Groovysh()
            VariableSyntaxCompletor completor = new VariableSyntaxCompletor(groovyshMock)
            def candidates = []
            assert [] == candidates
            assert !completor.complete(tokenList('xyz'), candidates)
        }
    }
}
