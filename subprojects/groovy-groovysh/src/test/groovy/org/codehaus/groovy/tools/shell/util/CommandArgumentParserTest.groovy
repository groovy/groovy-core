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

package org.codehaus.groovy.tools.shell.util


class CommandArgumentParserTest extends GroovyTestCase {

    void testParseLine() {
        // empty
        assert [] == CommandArgumentParser.parseLine('')

        // blanks
        assert ['foo', 'bar'] == CommandArgumentParser.parseLine('  foo bar  ')

        // empties
        assert ['', '', '', ''] == CommandArgumentParser.parseLine('\'\'\'\' \'\'  \'\'')
        assert ['', '', '', ''] == CommandArgumentParser.parseLine('"""" ""  ""')

        // hyphen groups
        assert ['foo bar', 'baz bam'] == CommandArgumentParser.parseLine(' \'foo bar\' \'baz bam\'')
        assert ['foo bar', 'baz bam'] == CommandArgumentParser.parseLine(' "foo bar" "baz bam"')

        // escaped hyphens and escape signs
        assert ['foo \\ "\' bar'] == CommandArgumentParser.parseLine('\'foo \\\\ "\\\' bar\'')
        // no space between hyphentokens
        assert ['bar', 'foo', 'bam', 'baz'] == CommandArgumentParser.parseLine('bar"foo"\'bam\'\'baz\'')

        // limited number of tokens
        assert ['foo'] == CommandArgumentParser.parseLine('  foo bar  ', 1)
        assert ['bar', 'foo'] == CommandArgumentParser.parseLine('bar"foo"\'bam\'\'baz\'', 2)

        assert ['map.put('] == CommandArgumentParser.parseLine('map.put(\'a\': 2)', 1)
        assert ['map.put('] == CommandArgumentParser.parseLine('map.put(\'a\'', 1)

    }

}
