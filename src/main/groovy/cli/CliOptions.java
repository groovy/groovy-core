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

package groovy.cli;

/**
 * Represents the results of parsing some command-line arguments according to an option specification.
 * There are dual versions of most methods. The simple versions of the methods assume option values are Strings
 * and accept a String option name as the first parameter.
 * The typed versions will return typed values. The {@code TypedOption} used to specify the option is the
 * first parameter.
 *
 * @author paulk
 */
public interface CliOptions {
    /**
     * Has an option with the given name been specified.
     */
    boolean hasOption(String optionName);

    /**
     * Get the option value for the option with the given name.
     */
    String getOptionValue(String optionName);

    /**
     * Get the option value for the option with the given name or its default value (if any).
     */
    String getOptionValue(String optionName, String defaultValue);

    /**
     * Get the option values for the option with the given name using the option's value separator.
     */
    String[] getOptionValues(String optionName);

    /**
     * Has an option with the given name been specified.
     */
    <T> boolean hasOption(TypedOption<T> option);

    /**
     * Get the option value for the typed option with the given name.
     */
    <T> T getOptionValue(TypedOption<T> option);

    /**
     * Get the option value for the typed option with the given name or its default value (if any).
     */
    <T> T getOptionValue(TypedOption<T> option, T defaultValue);

    /**
     * Get the typed option values for the option with the given name using the option's value separator.
     */
    <T> T[] getOptionValues(TypedOption<T> option);

    /**
     * Any remaining arguments not representing options.
     */
    String[] remainingArgs();
}
