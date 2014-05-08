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

package org.codehaus.groovy.cli;

import groovy.cli.CliOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.PrintWriter;

/**
 * @author paulk
 */
public class CommonsCliOptionsWrapper implements CliOptions {
    private final CommandLine cli;
    private final Options cliOptions;

    public CommonsCliOptionsWrapper(CommandLine cli, Options cliOptions) {
        this.cli = cli;
        this.cliOptions = cliOptions;
    }

    public String[] remainingArgs() {
        return cli.getArgs();
    }

    public boolean hasOption(String optionName) {
        return cli.hasOption(optionName);
    }

    public String getOptionValue(String optionName) {
        return cli.getOptionValue(optionName);
    }

    public String getOptionValue(String optionName, String defaultValue) {
        String optionValue = cli.getOptionValue(optionName);
        return optionValue == null ? defaultValue : optionValue;
    }

    public String[] getOptionValues(String optionName) {
        return cli.getOptionValues(optionName);
    }

    public void displayHelp(String cmdLineSyntax, String header) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, cmdLineSyntax, header, cliOptions, "");
    }

    public void displayHelp(PrintWriter pw, String cmdLineSyntax, String header) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, 80, cmdLineSyntax, header, cliOptions, 2, 4, null, false);
    }
}
