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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GroovyInternalPosixParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.List;
import java.util.Map;

/**
 * @author paulk
 */
public class CommonsCliParserWrapper implements CliParser {

    private CommandLineParser cliParser = new GroovyInternalPosixParser();
    private Options cliOptions = new Options();

    public CliOptions parse(String[] arguments) {
        CommandLine cli = null;
        try {
            cli = cliParser.parse(cliOptions, arguments);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return new CommonsCliOptionsWrapper(cli, cliOptions);
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
        Option option = new Option((String) map.get("opt"), (String) map.get("description"));
        option.setLongOpt((String) map.get("longOpt"));
        Boolean required = (Boolean) map.get("required");
        if (required != null) option.setRequired(required);
        Boolean optionalArg = (Boolean) map.get("optionalArg");
        if (optionalArg != null) option.setOptionalArg(optionalArg);
        option.setArgs((Integer) map.get("numberOfArgs"));
//            option.setType(type);
        Object valueSep = map.get("valueSep");
        if (valueSep != null) option.setValueSeparator(valueSep.toString().charAt(0));
        option.setArgName((String) map.get("argName"));
        return option;
    }
}
