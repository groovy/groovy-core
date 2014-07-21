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
import java.util.List;
import java.util.Map;

/**
 * @author paulk
 */
public interface CliParser {
    CliOptions parse(String[] arguments);

    CliOptions parse(String[] arguments, boolean stopAtNonOption);

    void addOption(Map<String, Object> map);

    void addOptionGroup(List<Map<String, Object>> maps);

    void displayHelp(String cmdLineSyntax, String header);

    void displayHelp(PrintWriter pw, String cmdLineSyntax, String header);

    void displayHelp(PrintWriter pw, int width, String cmdLineSyntax, String header, String footer);

    void displayHelp(PrintWriter pw, int width, String cmdLineSyntax, String header, int leftPad, int descPad, String footer);
}
