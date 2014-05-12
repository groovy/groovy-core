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

/**
 * @author paulk
 */
public class CommonsCliOptionsWrapper implements CliOptions {
    private final CommandLine cli;

    public CommonsCliOptionsWrapper(CommandLine cli) {
        this.cli = cli;
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
        return cli.hasOption(optionName) ? cli.getOptionValue(optionName) : defaultValue;
    }

    public String[] getOptionValues(String optionName) {
        return cli.getOptionValues(optionName);
    }
}
