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

import java.util.HashMap;
import java.util.Map;

/**
 * modelled on OptionBuilder from commons-cli which has author John Keyes (john at integralsource.com)
 */
public class CliOptionBuilder {
    private static String longOpt;
    private static String description;
    private static String argName;
    private static Boolean required;
    private static Integer numberOfArgs = -1; // unspecified
    //    private static Object type;
    private static Boolean optionalArg;
    private static Character valueSep;

    private static CliOptionBuilder instance = new CliOptionBuilder();

    /**
     * private constructor to prevent instances being created
     */
    private CliOptionBuilder() {
    }

    private static void reset() {
        description = null;
        argName = "arg";
        longOpt = null;
//        type = null;
        required = false;
        numberOfArgs = -1;
        optionalArg = false;
        valueSep = (char) 0;
    }

    public static CliOptionBuilder withLongOpt(String newLongopt) {
        CliOptionBuilder.longOpt = newLongopt;
        return instance;
    }

    public static CliOptionBuilder hasArg() {
        CliOptionBuilder.numberOfArgs = 1;
        return instance;
    }

    public static CliOptionBuilder hasArg(boolean hasArg) {
        CliOptionBuilder.numberOfArgs = hasArg ? 1 : -1;
        return instance;
    }

    public static CliOptionBuilder withArgName(String name) {
        CliOptionBuilder.argName = name;
        return instance;
    }

    public static CliOptionBuilder isRequired() {
        CliOptionBuilder.required = true;
        return instance;
    }

    public static CliOptionBuilder withValueSeparator(char sep) {
        CliOptionBuilder.valueSep = sep;
        return instance;
    }

    public static CliOptionBuilder withValueSeparator() {
        return withValueSeparator('=');
    }

    public static CliOptionBuilder isRequired(boolean newRequired) {
        CliOptionBuilder.required = newRequired;
        return instance;
    }

    public static CliOptionBuilder hasArgs() {
        CliOptionBuilder.numberOfArgs = -2; // unlimited
        return instance;
    }

    public static CliOptionBuilder hasArgs(int num) {
        CliOptionBuilder.numberOfArgs = num;
        return instance;
    }

    public static CliOptionBuilder hasOptionalArg() {
        CliOptionBuilder.numberOfArgs = 1;
        CliOptionBuilder.optionalArg = true;
        return instance;
    }

    public static CliOptionBuilder hasOptionalArgs() {
        CliOptionBuilder.numberOfArgs = -2; // unlimited
        CliOptionBuilder.optionalArg = true;

        return instance;
    }

    public static CliOptionBuilder hasOptionalArgs(int numArgs) {
        CliOptionBuilder.numberOfArgs = numArgs;
        CliOptionBuilder.optionalArg = true;
        return instance;
    }

//    public static CliOptionBuilder withType(Object newType) {
//        CliOptionBuilder.type = newType;
//        return instance;
//    }

    public static CliOptionBuilder withDescription(String newDescription) {
        CliOptionBuilder.description = newDescription;
        return instance;
    }

    public static Map<String, Object> create(char opt) throws IllegalArgumentException {
        return create(String.valueOf(opt));
    }

    public static Map<String, Object> create() throws IllegalArgumentException {
        if (longOpt == null) {
            reset();
            throw new IllegalArgumentException("must specify longOpt");
        }
        return create(null);
    }

    public static Map<String, Object> create(String opt) throws IllegalArgumentException {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("opt", opt);
        result.put("description", description);
        result.put("longOpt", longOpt);
        result.put("required", required);
        result.put("optionalArg", optionalArg);
        result.put("numberOfArgs", numberOfArgs);
        result.put("valueSep", valueSep);
        result.put("argName", argName);
        reset();
        return result;
    }
}
