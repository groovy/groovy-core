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
package org.codehaus.groovy.antlr.java;

import groovy.cli.CliOptions;
import groovy.cli.CliParser;
import groovy.cli.CliParserFactory;
import java.util.Arrays;

public class Java2GroovyMain {

    public static void main(String[] args) {
        try {
            CliParser cliParser = CliParserFactory.newParser();
            CliOptions cli = cliParser.parse(args);
            String[] filenames = cli.remainingArgs();
            if (filenames.length == 0) {
                System.err.println("Needs at least one filename");
            }
            Java2GroovyProcessor.processFiles(Arrays.asList(filenames));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
