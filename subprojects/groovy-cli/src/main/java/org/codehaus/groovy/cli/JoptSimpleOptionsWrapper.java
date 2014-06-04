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
import groovy.cli.TypedOption;
import joptsimple.OptionSet;

import java.util.List;

/**
 * @author paulk
 */
public class JoptSimpleOptionsWrapper implements CliOptions {
    private final OptionSet options;

    public JoptSimpleOptionsWrapper(OptionSet options) {
        this.options = options;
    }

    public String[] remainingArgs() {
        List<?> result = options.nonOptionArguments();
        return result.toArray(new String[result.size()]);
    }

    public boolean hasOption(String optionName) {
        return options.has(optionName);
    }

    public String getOptionValue(String optionName) {
        return (String) options.valueOf(optionName);
    }

    public <T> T getOptionValue(TypedOption<T> option) {
        // TODO support types
        return null;
    }

    public String getOptionValue(String optionName, String defaultValue) {
        return options.has(optionName) ? (String) options.valueOf(optionName) : defaultValue;
    }

    public String[] getOptionValues(String optionName) {
        List<?> result = options.valuesOf(optionName);
        return result.toArray(new String[result.size()]);
    }
}
