/*
 * Copyright 2014-2014 the original author or authors.
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import groovy.lang.Script;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.join;

/**
 * Base script that provides JCommander declarative (annotation-based) argument processing for scripts.
 *
 * @author Jim White
 */
abstract public class JCommanderScript extends Script {
    protected abstract Object runScriptBody();

    @Override
    public Object run() {
        String[] args = getScriptArguments();
        JCommander jc = getJCommander();
        try {
            parseScriptArguments(jc, args);
            return runScriptBody();
        } catch (ParameterException pe) {
            return handleParameterException(jc, args, pe);
        }
    }

    public String[] getScriptArguments() {
        return (String[]) getProperty("args");
    }

    public JCommander getJCommander() {
        JCommander jc = new JCommander(this);
        jc.setProgramName(this.getClass().getSimpleName());
        return jc;
    }

    public void parseScriptArguments(JCommander jc, String[] args) {
        jc.parse(args);
    }

    public void printErrorMessage(String message) {
        println(message);
    }

    public Object handleParameterException(JCommander jc, String[] args, ParameterException pe) {
        StringBuilder sb = new StringBuilder();

        sb.append(pe.getMessage());
        sb.append("\n");

        sb.append("args: [");
        sb.append(join(args, ", "));
        sb.append("]");
        sb.append("\n");

        jc.usage(sb);

        printErrorMessage(sb.toString());

        return -1;
    }

}
