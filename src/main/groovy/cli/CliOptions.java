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

import java.io.PrintWriter;

/**
 * @author paulk
 */
public interface CliOptions {
    String[] remainingArgs();

    boolean hasOption(String optionName);

    String getOptionValue(String optionName);

    String getOptionValue(String optionName, String defaultValue);

    String[] getOptionValues(String optionName);

    void displayHelp(String cmdLineSyntax, String header);

    void displayHelp(PrintWriter pw, String cmdLineSyntax, String header);
}
