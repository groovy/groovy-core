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

import groovy.transform.CompileStatic

@CompileStatic
class CommandArgumentParser {

    /**
     * takes a String and tokenizes it according to posix-shell-like rules, meaning
     * arguments are separated by blanks or hyphens, and hyphens wrap tokens regardless
     * of blanks, other hyphens or escaped hyphens within the wrapping hyphens.
     *
     * Example: "foo bar 123'456' 'abc\'def\\' ''"  has 6 tokens:
     * ["foo", "bar", "123", "456", "abc'def\", ""]
     *
     * @param untrimmedLine
     * @param numTokensToCollect stop processing after so many tokens, negative means unlimited
     * @return
     */
    static List<String> parseLine(final String untrimmedLine, final int numTokensToCollect = -1) {
        assert untrimmedLine != null

        final String line = untrimmedLine.trim()
        List<String> tokens = []
        String currentToken = ''
        // state machine being either in neutral state, in singleHyphenOpen state, or in doubleHyphenOpen State.
        boolean singleHyphenOpen = false
        boolean doubleHyphenOpen = false
        int index = 0
        for (; index < line.length(); index++) {
            if (tokens.size() == numTokensToCollect) {
                break
            }
            String ch = line.charAt(index)
            // escaped char?
            if (ch == '\\' && (singleHyphenOpen || doubleHyphenOpen)) {
                ch = (index == line.length() - 1) ? '\\' : line.charAt(index + 1)
                index++
                currentToken += ch
                continue
            }

            if (ch == '"' && !singleHyphenOpen) {
                if (doubleHyphenOpen) {
                    tokens.add(currentToken)
                    currentToken = ''
                    doubleHyphenOpen = false
                } else {
                    if (currentToken.size() > 0) {
                        tokens.add(currentToken)
                        currentToken = ''
                    }
                    doubleHyphenOpen = true
                }
                continue
            }
            if (ch == '\'' && !doubleHyphenOpen) {
                if (singleHyphenOpen) {
                    tokens.add(currentToken)
                    currentToken = ''
                    singleHyphenOpen = false
                } else {
                    if (currentToken.size() > 0) {
                        tokens.add(currentToken)
                        currentToken = ''
                    }
                    singleHyphenOpen = true
                }
                continue
            }
            if (ch == ' ' && !doubleHyphenOpen && !singleHyphenOpen) {
                if (currentToken.size() > 0) {
                    tokens.add(currentToken)
                    currentToken = ''
                }
                continue
            }
            currentToken += ch
        } // end for char in line
        if (index == line.length() && doubleHyphenOpen) {
            throw new IllegalArgumentException('Missing closing " in ' + line + ' -- ' + tokens)
        }
        if (index == line.length() && singleHyphenOpen) {
            throw new IllegalArgumentException('Missing closing \' in ' + line  + ' -- ' + tokens)
        }
        if (currentToken.size() > 0) {
            tokens.add(currentToken)
        }
        return tokens
    }
}
