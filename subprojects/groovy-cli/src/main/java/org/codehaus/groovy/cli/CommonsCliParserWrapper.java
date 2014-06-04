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
import groovy.cli.CliParseException;
import groovy.cli.CliParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GroovyInternalPosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author paulk
 */
public class CommonsCliParserWrapper implements CliParser {

    protected CommandLineParser cliParser = getParserDelegate();
    protected Options cliOptions = new Options();
    protected Map<String, Object> defaultValues = new HashMap<String, Object>();

    protected CommandLineParser getParserDelegate() {
        return new GroovyInternalPosixParser();
    }

    public CliOptions parse(String[] arguments) {
        return parse(arguments, true);
    }

    public CliOptions parse(String[] arguments, boolean stopAtNonOption) {
        CommandLine cli = null;
        try {
            cli = cliParser.parse(cliOptions, arguments, stopAtNonOption);
        } catch (ParseException e) {
            throw new CliParseException(e.getMessage());
        }
        return new CommonsCliOptionsWrapper(cli, cliOptions, defaultValues);
    }

    public void addOption(Map<String, Object> map) {
        cliOptions.addOption(makeOption(map));
    }

    public void addOptionGroup(List<Map<String, Object>> maps) {
        OptionGroup group = new OptionGroup();
        for (Map<String, Object> map : maps) {
            group.addOption(makeOption(map));
        }
        cliOptions.addOptionGroup(group);
    }

    private Option makeOption(Map<String, Object> map) {
        String opt = getString(map, "opt");
        Option option = new Option(opt == null ? "" : opt, getString(map, "description"));
        option.setLongOpt(getString(map, "longOpt"));
        Boolean required = (Boolean) map.get("required");
        if (required != null) option.setRequired(required);
        Boolean optionalArg = (Boolean) map.get("optionalArg");
        if (optionalArg != null) option.setOptionalArg(optionalArg);
        Object defaultValue = map.get("defaultValue");
        defaultValues.put(makeDefaultValueKey(option), defaultValue);
        option.setArgs((Integer) map.get("numberOfArgs"));
        Class type = (Class) map.get("type");
        if (type != null) option.setType(type);
        Object valueSep = map.get("valueSep");
        if (valueSep != null) option.setValueSeparator(valueSep.toString().charAt(0));
        option.setArgName(getString(map, "argName"));
        return option;
    }

    static String makeDefaultValueKey(Option option) {
        return stripLeadingHyphens(option.getOpt()) + "_" + stripLeadingHyphens(option.getLongOpt());
    }

    static String stripLeadingHyphens(String s) {
        if (s == null) return "";
        if (s.startsWith("--")) return s.substring(2, s.length());
        if (s.startsWith("-")) return s.substring(1, s.length());
        return s;
    }
    private String getString(Map<String, Object> map, String optionName) {
        Object result = map.get(optionName);
        return result == null ? null : result.toString();
    }

    public void displayHelp(String cmdLineSyntax, String header) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, cmdLineSyntax, header, cliOptions, "");
    }

    public void displayHelp(PrintWriter pw, String cmdLineSyntax, String header) {
        displayHelp(pw, 80, cmdLineSyntax, header, null);
    }

    public void displayHelp(PrintWriter pw, int width, String cmdLineSyntax, String header, String footer) {
        displayHelp(pw, width, cmdLineSyntax, header, 2, 4, footer);
    }

    public void displayHelp(PrintWriter pw, int width, String cmdLineSyntax, String header, int leftPad, int descPad, String footer) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, width, cmdLineSyntax, header, cliOptions, leftPad, descPad, footer, false);
    }
}
