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
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.join;

/**
 * Base script that provides JCommander declarative (annotation-based) argument processing for scripts.
 *
 * @author Jim White
 */

abstract public class JCommanderScript extends Script {
    /**
     * Name of the property that holds the JCommander for this script (i.e. 'scriptJCommander').
     */
    public final static String SCRIPT_JCOMMANDER = "scriptJCommander";

    /**
     * The script body
     * @return The result of the script evaluation.
     */
    protected abstract Object runScriptBody();

    @Override
    public Object run() {
        String[] args = getScriptArguments();
        JCommander jc = getScriptJCommanderWithInit();
        try {
            parseScriptArguments(jc, args);
            for (ParameterDescription pd : jc.getParameters()) {
                if (pd.isHelp() && pd.isAssigned()) return exitCode(printHelpMessage(jc, args));
            }
            runScriptCommand(jc);
            return exitCode(runScriptBody());
        } catch (ParameterException pe) {
            return exitCode(handleParameterException(jc, args, pe));
        }
    }

    /**
     * If the given code is numeric and non-zero, then return that from this process using System.exit.
     * Non-numeric values (including null) are taken to be zero and returned as-is.
     *
     * @param code
     * @return the given code
     */
    public Object exitCode(Object code) {
        if (code instanceof Number) {
            int codeValue = ((Number) code).intValue();
            if (codeValue != 0) System.exit(codeValue);
        }
        return code;
    }

    /**
     * Return the script arguments as an array of strings.
     * The default implementation is to get the "args" property.
     *
     * @return the script arguments as an array of strings.
     */
    public String[] getScriptArguments() {
        return (String[]) getProperty("args");
    }

    /**
     * Return the JCommander for this script.
     * If there isn't one already, then create it using createScriptJCommander.
     *
     * @return the JCommander for this script.
     */
    protected JCommander getScriptJCommanderWithInit() {
        try {
            JCommander jc = (JCommander) getProperty(SCRIPT_JCOMMANDER);
            if (jc == null) {
                jc = createScriptJCommander();
                // The script has a real property (a field or getter) but if we let Script.setProperty handle
                // this then it just gets stuffed into a binding that shadows the property.
                // This is somewhat related to other bugged behavior in Script wrt properties and bindings.
                // See http://jira.codehaus.org/browse/GROOVY-6582 for example.
                // The correct behavior for Script.setProperty would be to check whether
                // the property has a setter before creating a new script binding.
                this.getMetaClass().setProperty(this, SCRIPT_JCOMMANDER, jc);
            }
            return jc;
        } catch (MissingPropertyException mpe) {
            JCommander jc = createScriptJCommander();
            // Since no property or binding already exists, we can use plain old setProperty here.
            setProperty(SCRIPT_JCOMMANDER, jc);
            return jc;
        }
    }

    /**
     * Create a new (hopefully just once for this script!) JCommander instance.
     * The default name for the command name in usage is the script's class simple name.
     * This is the time to load it up with command objects, which is done by initializeJCommanderCommands.
     *
     * @return A JCommander instance with the commands (if any) initialized.
     */
    public JCommander createScriptJCommander() {
        JCommander jc = new JCommander(this);
        jc.setProgramName(this.getClass().getSimpleName());

        initializeJCommanderCommands(jc);

        return jc;
    }

    /**
     * Add command objects to the given JCommander.
     * The default behavior is to look for Subcommand annotations.
     *
     * @param jc The JCommander instance to add the commands (if any) to.
     */
    public void initializeJCommanderCommands(JCommander jc) {
        Class cls = this.getClass();
        while (cls != null) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                Annotation annotation = field.getAnnotation(Subcommand.class);
                if (annotation != null) {
                    try {
                        field.setAccessible(true);
                        jc.addCommand(field.get(this));
                    } catch (IllegalAccessException e) {
                        printErrorMessage("Trying to add JCommander @Subcommand but got error '" + e.getMessage()
                                + "' when getting value of field " + field.getName());
                    }
                }
            }

            cls = cls.getSuperclass();
        }
    }

    /**
     * Do JCommander.parse using the given arguments.
     * If you want to do any special checking before the Runnable commands get run,
     * this is the place to do it by overriding.
     *
     * @param jc The JCommander instance for this script instance.
     * @param args  The argument array.
     */
    public void parseScriptArguments(JCommander jc, String[] args) {
        jc.parse(args);
    }

    /**
     * If there are any objects implementing Runnable that are part of this command script,
     * then run them.  If there is a parsed command, then run those objects after the main command objects.
     * Note that this will not run the main script though, we leave that for run to do (which will happen
     * normally since groovy.lang.Script doesn't implement java.lang.Runnable).
     *
     * @param jc
     */
    public void runScriptCommand(JCommander jc) {
        List<Object> objects = jc.getObjects();

        String parsedCommand = jc.getParsedCommand();
        if (parsedCommand != null) {
            JCommander commandCommander = jc.getCommands().get(parsedCommand);
            objects.addAll(commandCommander.getObjects());
        }

        for (Object commandObject : objects) {
            if (commandObject instanceof Runnable) {
                Runnable runnableCommand = (Runnable) commandObject;
                if ((Object) runnableCommand != (Object) this) {
                    runnableCommand.run();
                }
            }
        }
    }

    /**
     * Error messages that arise from command line processing call this.
     * The default is to use the Script's println method (which will go to the
     * 'out' binding, if any, and System.out otherwise).
     * If you want to use System.err, a logger, or something, this is the thing to override.
     *
     * @param message
     */
    public void printErrorMessage(String message) {
        println(message);
    }

    /**
     * If a ParameterException occurs during parseScriptArguments, runScriptCommand, or runScriptBody
     * then this gets called to report the problem.
     * The default behavior is to show the exception message using printErrorMessage, then call printHelpMessage.
     * The return value becomes the return value for the Script.run which will be the exit code
     * if we've been called from the command line.
     *
     * @param jc The JCommander instance
     * @param args The argument array
     * @param pe The ParameterException that occurred
     * @return The value that Script.run should return (2 by default).
     */
    public Object handleParameterException(JCommander jc, String[] args, ParameterException pe) {
        StringBuilder sb = new StringBuilder();

        sb.append("args: [");
        sb.append(join(args, ", "));
        sb.append("]");
        sb.append("\n");

        sb.append(pe.getMessage());

        printErrorMessage(sb.toString());

        printHelpMessage(jc, args);

        return 3;
    }

    /**
     * If a @Parameter whose help attribute is annotated as true appears in the arguments.
     * then the script body is not run and this printHelpMessage method is called instead.
     * The default behavior is to show the arguments and the JCommander.usage using printErrorMessage.
     * The return value becomes the return value for the Script.run which will be the exit code
     * if we've been called from the command line.
     *
     * @param jc The JCommander instance
     * @param args The argument array
     * @return The value that Script.run should return (1 by default).
     */
    public Object printHelpMessage(JCommander jc, String[] args) {
        StringBuilder sb = new StringBuilder();

        jc.usage(sb);

        printErrorMessage(sb.toString());

        return 2;
    }

}
