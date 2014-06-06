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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.lang.reflect.Array;
import java.util.Map;

import static org.codehaus.groovy.cli.CommonsCliParserWrapper.makeDefaultValueKey;
import static org.codehaus.groovy.cli.CommonsCliParserWrapper.stripLeadingHyphens;

/**
 * @author paulk
 */
public class CommonsCliOptionsWrapper implements CliOptions {
    private final CommandLine cli;
    private final Options cliOptions;
    private final Map<String, Object> defaultValues;

    public CommonsCliOptionsWrapper(CommandLine cli, Options cliOptions, Map<String, Object> defaultValues) {
        this.cli = cli;
        this.cliOptions = cliOptions;
        this.defaultValues = defaultValues;
    }

    public String[] remainingArgs() {
        return cli.getArgs();
    }

    public boolean hasOption(String optionName) {
        return cli.hasOption(optionName);
    }

    public <T> boolean hasOption(TypedOption<T> typedOption) {
        return cli.hasOption((String) typedOption.get("longOpt"));
    }

    public String getOptionValue(String optionName) {
        if (hasOption(optionName)) {
            return cli.getOptionValue(optionName);
        }
        Option[] options = cli.getOptions();
        for (Option option : options) {
            if (optionName.equals(stripLeadingHyphens(option.getOpt())) || optionName.equals(stripLeadingHyphens(option.getLongOpt()))) {
                return defaultValues.get(makeDefaultValueKey(option)).toString();
            }
        }
        return null;
    }

    public <T> T getOptionValue(TypedOption<T> typedOption) {
        String optionName = (String) typedOption.get("longOpt");
        if (hasOption(optionName)) {
            return getTypedValueFromName(optionName);
        }
        return typedOption.defaultValue();
    }

    public <T> T getOptionValue(TypedOption<T> typedOption, T defaultValue) {
        String optionName = (String) typedOption.get("longOpt");
        if (hasOption(optionName)) {
            return getTypedValueFromName(optionName);
        }
        return defaultValue;
    }

    public String getOptionValue(String optionName, String defaultValue) {
        return cli.hasOption(optionName) ? cli.getOptionValue(optionName) : defaultValue;
    }

    public String[] getOptionValues(String optionName) {
        return cli.getOptionValues(optionName);
    }

    public <T> T[] getOptionValues(TypedOption<T> typedOption) {
        String optionName = (String) typedOption.get("longOpt");
        String[] optionValues = cli.getOptionValues(optionName);
        Option option = cliOptions.getOption(optionName);
        Object type = option.getType();
        T[] result = null;
        int count = 0;
        for (String optionValue : optionValues) {
            if (result == null) {
                result = (T[]) Array.newInstance((Class<?>) type, optionValues.length);
            }
            result[count++] = (T) getTypedValue(type, optionValue);
        }
        return result;
    }

    private <T> T getTypedValueFromName(String optionName) {
        Option option = cliOptions.getOption(optionName);
        Object type = option.getType();
        String optionValue = cli.getOptionValue(optionName);
        return (T) getTypedValue(type, optionValue);
    }

    private <T> T getTypedValue(Object type, String optionValue) {
        return StringGroovyMethods.asType(optionValue, (Class<T>) type);
    }
}
