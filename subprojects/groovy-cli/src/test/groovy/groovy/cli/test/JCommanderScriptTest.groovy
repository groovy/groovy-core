/*
 * Copyright 2003-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.cli.test

import groovy.transform.SourceURI

/**
 * @author Jim White
 */

public class JCommanderScriptTest extends GroovyTestCase {
    @SourceURI URI sourceURI

    void testParameterizedScript() {
        GroovyShell shell = new GroovyShell()
        shell.context.setVariable('args',
                ["--codepath", "/usr/x.jar", "placeholder", "-cp", "/bin/y.jar", "-cp", "z", "another"] as String[])
        def result = shell.evaluate '''
@groovy.transform.BaseScript(groovy.cli.JCommanderScript)
import com.beust.jcommander.Parameter
import groovy.transform.Field

@Parameter
@Field List<String> parameters

@Parameter(names = ["-cp", "--codepath"])
@Field List<String> codepath = []

//println parameters
//println codepath

assert parameters == ['placeholder', 'another']
assert codepath == ['/usr/x.jar', '/bin/y.jar', 'z']

[parameters.size(), codepath.size()]
'''
        assert result == [2, 3]
    }

    void testSimpleCommandScript() {
        GroovyShell shell = new GroovyShell()
        shell.context.setVariable('args',
                [ "--codepath", "/usr/x.jar", "placeholder", "-cp", "/bin/y.jar", "another" ] as String[])
        def result = shell.evaluate(new File(new File(sourceURI).parentFile, 'SimpleJCommanderScriptTest.groovy'))
        assert result == [777]
    }

    void testMultipleCommandScript() {
        GroovyShell shell = new GroovyShell()
        def result = shell.evaluate(new File(new File(sourceURI).parentFile, 'MultipleJCommanderScriptTest.groovy'))
        assert result == [33]
    }
}
