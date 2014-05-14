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
import groovy.cli.CliParser;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSpecBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author paulk
 */
public class JoptSimpleParserWrapper implements CliParser {

    OptionParser parser = new OptionParser();

    public CliOptions parse(String[] arguments) {
        return new JoptSimpleOptionsWrapper(parser.parse(arguments));
    }

    public CliOptions parse(String[] arguments, boolean stopAtNonOption) {
        parser.posixlyCorrect(stopAtNonOption);
        return new JoptSimpleOptionsWrapper(parser.parse(arguments));
    }

    public void addOption(Map<String, Object> map) {
        List<String> aliases = new ArrayList<String>();
        aliases.add(getString(map, "opt"));
        String longOpt = getString(map, "longOpt");
        if (longOpt != null && longOpt.length() != 0) {
            aliases.add(longOpt);
        }
        OptionSpecBuilder builder = parser.acceptsAll(aliases, getString(map, "description"));
        Boolean optionalArg = (Boolean) map.get("optionalArg");
        int numArgs = (Integer) map.get("numberOfArgs");
        if (numArgs != -1 || (optionalArg != null && optionalArg)) {
            String argName = getString(map, "argName");
            ArgumentAcceptingOptionSpec aaoc;
            if (optionalArg != null && optionalArg) {
                aaoc = builder.withOptionalArg().describedAs(argName);
            } else {
                aaoc = builder.withRequiredArg().describedAs(argName);
            }
            Character valueSep = (Character) map.get("valueSep");
            if (valueSep != null && valueSep != 0) {
                aaoc = aaoc.withValuesSeparatedBy(valueSep);
            }
            Boolean required = (Boolean) map.get("required");
            if (required != null && required) {
                aaoc.required();
            }
//        aaoc.ofType(type);
        }
    }

    private String getString(Map<String, Object> map, String optionName) {
        Object result = map.get(optionName);
        return result == null ? null : result.toString();
    }

    public void addOptionGroup(List<Map<String, Object>> maps) {
        // TODO make it a real group
        for (Map<String, Object> map : maps) {
            addOption(map);
        }
    }

    public void displayHelp(String cmdLineSyntax, String header) {
        System.out.print("usage: " + cmdLineSyntax + "\n------\n");
        BuiltinHelpFormatter formatter = new BuiltinHelpFormatter(80, 4);
        try {
            parser.formatHelpWith(formatter);
            parser.printHelpOn(System.out);
        } catch (IOException ignore) {
        }
    }

    public void displayHelp(PrintWriter pw, String cmdLineSyntax, String header) {
        displayHelp(pw, 80, cmdLineSyntax, header, null);
    }

    public void displayHelp(PrintWriter pw, int width, String cmdLineSyntax, String header, String footer) {
        displayHelp(pw, width, cmdLineSyntax, header, 2, 4, footer);
    }

    public void displayHelp(PrintWriter pw, int width, String cmdLineSyntax, String header, int leftPad, int descPad, String footer) {
        // TODO support header, footer, etc.
        pw.print("usage: " + cmdLineSyntax + "\n------\n");
        BuiltinHelpFormatter formatter = new BuiltinHelpFormatter(width, descPad);
//        formatter.printHelp(pw, width, cmdLineSyntax, header, cliOptions, leftPad, descPad, footer, false);
        try {
            parser.formatHelpWith(formatter);
            parser.printHelpOn(pw);
        } catch (IOException ignore) {
        }
    }
}
