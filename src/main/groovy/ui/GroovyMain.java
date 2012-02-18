/*
 * Copyright 2003-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.ui;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StackTraceUtils;

/**
 * A Command line to execute groovy.
 *
 * @author Jeremy Rayner
 * @author Yuri Schimke
 * @author Roshan Dawrani
 * @version $Revision$
 */
public class GroovyMain {

    // arguments to the script
    private List args;

    // selectors for 
    private static enum ARGS_TYPE { GROOVYCMD, SCRIPT };

    // is this a file on disk
    private boolean isScriptFile;

    // filename or content of script
    private String script;

    // process args as input files
    private boolean processFiles;

    // edit input files in place
    private boolean editFiles;

    // automatically output the result of each script
    private boolean autoOutput;

    // automatically split each line using the splitpattern
    private boolean autoSplit;

    // The pattern used to split the current line
    private String splitPattern = " ";

    // process sockets
    private boolean processSockets;

    // port to listen on when processing sockets
    private int port;

    // backup input files with extension
    private String backupExtension;

    // do you want full stack traces in script exceptions?
    private boolean debug = false;

    // Compiler configuration, used to set the encodings of the scripts/classes
    private CompilerConfiguration conf = new CompilerConfiguration(System.getProperties());

    /**
     * Main CLI interface.
     *
     * @param args all command line args.
     */
    public static void main(String args[]) {
        processArgs(args, System.out);
    }

    // package-level visibility for testing purposes (just usage/errors at this stage)
    // TODO: should we have an 'err' printstream too for ParseException?
    static void processArgs(String[] args, final PrintStream out) {
        Options options = buildOptions();

        try {
            //first split up the args into args for the groovy command line and for a possible script, which should
            //be invoked.
            Map<ARGS_TYPE, List<String>> splittedArgs = splitArgsByArgTypeWithOptions(options, args);
            List<String> groovyCmdLineArgs = splittedArgs.get(ARGS_TYPE.GROOVYCMD);
            CommandLine groovyCmdLine = parseCommandLine(options, groovyCmdLineArgs.toArray(new String[groovyCmdLineArgs.size()]));

            //there should not be any leftover args, which are not allowed by the options
            //Therefore we are introducing a convention, as seen in printHelp()
            //Convention:
            //    1. first specify all options for the 'groovy' command.
            //    2. specify an optinal scriptName (File, URL).
            //    3. specify args for the given scriptName.
            //
            //We need to do this, so that the groovy script, we want to invoke, can have CLI-options with the
            //same parameter abbreviations than the 'groovy' command itself.
            //
            //See also: GROOVY-5191 http://jira.codehaus.org/browse/GROOVY-5191
            //

            if (groovyCmdLine.hasOption('h') || !groovyCmdLine.getArgList().isEmpty()) {
                printHelp(out, options);
            } else if (groovyCmdLine.hasOption('v')) {
                String version = GroovySystem.getVersion();
                out.println("Groovy Version: " + version + " JVM: " + System.getProperty("java.version") + 
                        " Vendor: " + System.getProperty("java.vm.vendor")  + " OS: " + System.getProperty("os.name"));
            } else {
                // If we fail, then exit with an error so scripting frameworks can catch it
                // TODO: pass printstream(s) down through process

                //the process must know, which args belong to the groovyCmdLine and which to the script
                List<String> scriptArgs = splittedArgs.get(ARGS_TYPE.SCRIPT);
                if (!process(groovyCmdLine, scriptArgs)) {
                    System.exit(1);
                }
            }
        } catch (ParseException pe) {
            out.println("error: " + pe.getMessage());
            printHelp(out, options);
        }
    }

    private static void printHelp(PrintStream out, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(out);

        formatter.printHelp(
            pw,
            80,
            "groovy [options] [scriptName scriptArgs]",
            "options:",
            options,
            2,
            4,
            null, // footer
            false);
       
        pw.flush();
    }

    /**
     * Parse the command line.
     *
     * @param options the options parser.
     * @param args    the command line args.
     * @return parsed command line.
     * @throws ParseException if there was a problem.
     */
    private static CommandLine parseCommandLine(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        return parser.parse(options, args, true);
    }



    /**
     * Parse the command line args array given the options, which should be used for the 'groovy' command and split
     * the args array into an args array for the groovy command and the scriptName which should run().
     *
     *
     *
     * @param options the options parser.
     * @param args    the command line args.
     * @return A Map selectable by  ARGS_TYPE containing String[] args array for each ARGS_TYPE
     * @throws ParseException if there was a problem.
     */
    private static Map<ARGS_TYPE, List<String>> splitArgsByArgTypeWithOptions(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();

        //We must distinguish, if the args should belong to the groovy command line options,
        //or the actual groovy script. Otherwise we will not be able to reuse command line options
        //for our scriptname, which we would like to run. (e.g. you were not be able to reuse the --help option for your
        //groovy script. Another examples is the (--encoding, -c ) option. If you would run your groovy script
        //with the --encoding option, you will not be able to specify your own --encoding option in your groovy.script.
        CommandLine fullCommandLine = parser.parse(options, args, true);
        List leftOverArgsWithoutOptions = fullCommandLine.getArgList();

        //try to fetch a scriptname
        String scriptName = null;
        if(!fullCommandLine.hasOption('e')) {
            if(!leftOverArgsWithoutOptions.isEmpty()) {
                scriptName = (String) leftOverArgsWithoutOptions.remove(0);
            }
        }

        //now, since we either know the scriptName, we can order the args and know, which args
        //should be given for the 'groovy' command, and which args should be used for the actual
        //groovy command.
        List<String> argsList = Arrays.asList(args);
        List<String> argsForGroovyCommand = new ArrayList<String>();
        List<String> argsForScriptNameIncludingScriptName = new ArrayList<String>();

        //idea: all args before 'scriptName' are for the groovy command. If no scriptName is given, all args are
        //for the GroovyCommand.
        if(scriptName == null) {
            argsForGroovyCommand = argsList;
        } else {
            //find the index of the scriptname
            int argsCount = argsList.size();
            int i = 0;
            for(i = 0; i < argsCount; i++) {
                if(argsList.get(i).equals(scriptName)) break;
            }

            //we include the scriptName for the scriptNameArgs, so that all later checks can stay the same.
            argsForGroovyCommand =  argsList.subList(0, i);
            argsForScriptNameIncludingScriptName = argsList.subList(i, argsCount);
        }

        //now, we can return a new parsed CommandLine for the 'groovy' command and pass the new String[] args Array
        //for the actual scriptName
        Map<ARGS_TYPE, List<String>> splittedArgsByType = new LinkedHashMap<ARGS_TYPE, List<String>>(2);
        splittedArgsByType.put(ARGS_TYPE.GROOVYCMD, argsForGroovyCommand);
        splittedArgsByType.put(ARGS_TYPE.SCRIPT, argsForScriptNameIncludingScriptName);
        return splittedArgsByType;
    }

    /**
     * Build the options parser.  Has to be synchronized because of the way Options are constructed.
     *
     * @return an options parser.
     */
    @SuppressWarnings("static-access")
    private static synchronized Options buildOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.hasArg().withArgName("path").withDescription("Specify where to find the class files - must be first argument").create("classpath"));
        options.addOption(OptionBuilder.withLongOpt("classpath").hasArg().withArgName("path").withDescription("Aliases for '-classpath'").create("cp"));

        options.addOption(
            OptionBuilder.withLongOpt("define").
            withDescription("define a system property").
            hasArg(true).
            withArgName("name=value").
            create('D'));
        options.addOption(
            OptionBuilder.withLongOpt("disableopt").
            withDescription("disables one or all optimization elements. " +
                            "optlist can be a comma separated list with the elements: " +
                            "all (disables all optimizations), " +
                            "int (disable any int based optimizations)").
            hasArg(true).
            withArgName("optlist").
            create());
        options.addOption(
            OptionBuilder.hasArg(false)
            .withDescription("usage information")
            .withLongOpt("help")
            .create('h'));
        options.addOption(
            OptionBuilder.hasArg(false)
            .withDescription("debug mode will print out full stack traces")
            .withLongOpt("debug")
            .create('d'));
        options.addOption(
            OptionBuilder.hasArg(false)
            .withDescription("display the Groovy and JVM versions")
            .withLongOpt("version")
            .create('v'));
        options.addOption(
            OptionBuilder.withArgName("charset")
            .hasArg()
            .withDescription("specify the encoding of the files")
            .withLongOpt("encoding")
            .create('c'));
        options.addOption(
            OptionBuilder.withArgName("script")
            .hasArg()
            .withDescription("specify a command line script")
            .create('e'));
        options.addOption(
            OptionBuilder.withArgName("extension")
            .hasOptionalArg()
            .withDescription("modify files in place; create backup if extension is given (e.g. \'.bak\')")
            .create('i'));
        options.addOption(
            OptionBuilder.hasArg(false)
            .withDescription("process files line by line using implicit 'line' variable")
            .create('n'));
        options.addOption(
            OptionBuilder.hasArg(false)
            .withDescription("process files line by line and print result (see also -n)")
            .create('p'));
        options.addOption(
            OptionBuilder.withArgName("port")
            .hasOptionalArg()
            .withDescription("listen on a port and process inbound lines (default: 1960)")
            .create('l'));
        options.addOption(
            OptionBuilder.withArgName("splitPattern")
            .hasOptionalArg()
            .withDescription("split lines using splitPattern (default '\\s') using implicit 'split' variable")
            .withLongOpt("autosplit")
            .create('a'));
        options.addOption(
            OptionBuilder.withLongOpt("indy")
            .withDescription("enables compilation using invokedynamic")
            .create());

        return options;
    }

    private static void setSystemPropertyFrom(final String nameValue) {
        if(nameValue==null) throw new IllegalArgumentException("argument should not be null");

        String name, value;
        int i = nameValue.indexOf("=");

        if (i == -1) {
            name = nameValue;
            value = Boolean.TRUE.toString();
        }
        else {
            name = nameValue.substring(0, i);
            value = nameValue.substring(i + 1, nameValue.length());
        }
        name = name.trim();

        System.setProperty(name, value);
    }

    /**
     * Process the users request.
     *
     * @param line the parsed command line, which is used for the 'GroovyMain' class.
     * @param scriptArgs the scriptArgs, which should be passed to the 'scriptFile' if given in groovyCmdLine.
     * @throws ParseException if invalid options are chosen
     */
    private static boolean process(CommandLine line, List<String> scriptArgs) throws ParseException {
        //make mutable copy
        List<String> args = new ArrayList<String>(scriptArgs.size());
        args.addAll(scriptArgs);

        if (line.hasOption('D')) {
            String[] values = line.getOptionValues('D');

            for (int i=0; i<values.length; i++) {
                setSystemPropertyFrom(values[i]);
            }
        }

        GroovyMain main = new GroovyMain();
        
        // add the ability to parse scripts with a specified encoding
        main.conf.setSourceEncoding(line.getOptionValue('c',main.conf.getSourceEncoding()));

        main.isScriptFile = !line.hasOption('e');
        main.debug = line.hasOption('d');
        main.conf.setDebug(main.debug);
        main.processFiles = line.hasOption('p') || line.hasOption('n');
        main.autoOutput = line.hasOption('p');
        main.editFiles = line.hasOption('i');
        if (main.editFiles) {
            main.backupExtension = line.getOptionValue('i');
        }
        main.autoSplit = line.hasOption('a');
        String sp = line.getOptionValue('a');
        if (sp != null)
            main.splitPattern = sp;

        if (main.isScriptFile) {
            if (args.isEmpty())
                throw new ParseException("neither -e or filename provided");

            main.script = (String) args.remove(0);
            if (main.script.endsWith(".java"))
                throw new ParseException("error: cannot compile file with .java extension: " + main.script);
        } else {
            main.script = line.getOptionValue('e');
        }

        main.processSockets = line.hasOption('l');
        if (main.processSockets) {
            String p = line.getOptionValue('l', "1960"); // default port to listen to
            main.port = Integer.parseInt(p);
        }
        
        // we use "," as default, because then split will create
        // an empty array if no option is set
        String disabled = line.getOptionValue("disableopt", ",");
        String[] deopts = disabled.split(",");
        for (String deopt_i : deopts) {
            main.conf.getOptimizationOptions().put(deopt_i,false);
        }
        
        if (line.hasOption("indy")) {
            main.conf.getOptimizationOptions().put("indy", true);
        }

        main.args = args;

        return main.run();
    }


    /**
     * Run the script.
     */
    private boolean run() {
        try {
            if (processSockets) {
                processSockets();
            } else if (processFiles) {
                processFiles();
            } else {
                processOnce();
            }
            return true;
        } catch (CompilationFailedException e) {
            System.err.println(e);
            return false;
        } catch (Throwable e) {
            if (e instanceof InvokerInvocationException) {
                InvokerInvocationException iie = (InvokerInvocationException) e;
                e = iie.getCause();
            }
            System.err.println("Caught: " + e);
            if (!debug) {
                StackTraceUtils.deepSanitize(e);
            }
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Process Sockets.
     */
    private void processSockets() throws CompilationFailedException, IOException {
        GroovyShell groovy = new GroovyShell(conf);
        //check the script is currently valid before starting a server against the script
        if (isScriptFile) {
            groovy.parse(getText(script));
        } else {
            groovy.parse(script);
        }
        new GroovySocketServer(groovy, isScriptFile, script, autoOutput, port);
    }

    public String getText(String urlOrFilename) throws IOException {
        if (isScriptUrl(urlOrFilename)) {
            try {
                return DefaultGroovyMethods.getText(new URL(urlOrFilename));
            } catch (Exception e) {
                throw new GroovyRuntimeException("Unable to get script from URL: ", e);
            }
        }
        return DefaultGroovyMethods.getText(huntForTheScriptFile(urlOrFilename));
    }

    private boolean isScriptUrl(String urlOrFilename) {
        return urlOrFilename.startsWith("http://") || urlOrFilename.startsWith("https://") || urlOrFilename.startsWith("file:");
    }

    /**
     * Hunt for the script file, doesn't bother if it is named precisely.
     *
     * Tries in this order:
     * - actual supplied name
     * - name.groovy
     * - name.gvy
     * - name.gy
     * - name.gsh
     */
    public File huntForTheScriptFile(String input) {
        String scriptFileName = input.trim();
        File scriptFile = new File(scriptFileName);
        String[] standardExtensions = {".groovy",".gvy",".gy",".gsh"};
        int i = 0;
        while (i < standardExtensions.length && !scriptFile.exists()) {
            scriptFile = new File(scriptFileName + standardExtensions[i]);
            i++;
        }
        // if we still haven't found the file, point back to the originally specified filename
        if (!scriptFile.exists()) {
            scriptFile = new File(scriptFileName);
        }
        return scriptFile;
    }

    /**
     * Process the input files.
     */
    private void processFiles() throws CompilationFailedException, IOException {
        GroovyShell groovy = new GroovyShell(conf);

        Script s;

        if (isScriptFile) {
            if (isScriptUrl(script)) {
                s = groovy.parse(getText(script), script.substring(script.lastIndexOf("/") + 1));
            } else {
                s = groovy.parse(huntForTheScriptFile(script));
            }
        } else {
            s = groovy.parse(script, "main");
        }

        if (args.isEmpty()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter writer = new PrintWriter(System.out);

            try {
                processReader(s, reader, writer);
            } finally {
                reader.close();
                writer.close();
            }

        } else {
            Iterator i = args.iterator();
            while (i.hasNext()) {
                String filename = (String) i.next();
                File file = huntForTheScriptFile(filename);
                processFile(s, file);
            }
        }
    }

    /**
     * Process a single input file.
     *
     * @param s    the script to execute.
     * @param file the input file.
     */
    private void processFile(Script s, File file) throws IOException {
        if (!file.exists())
            throw new FileNotFoundException(file.getName());

        if (!editFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            try {
                PrintWriter writer = new PrintWriter(System.out);
                processReader(s, reader, writer);
                writer.flush();
            } finally {
                reader.close();
            }
        } else {
            File backup;
            if (backupExtension == null) {
                backup = File.createTempFile("groovy_", ".tmp");
                backup.deleteOnExit();
            } else {
                backup = new File(file.getPath() + backupExtension);
            }
            backup.delete();
            if (!file.renameTo(backup))
                throw new IOException("unable to rename " + file + " to " + backup);

            BufferedReader reader = new BufferedReader(new FileReader(backup));
            try {
                PrintWriter writer = new PrintWriter(new FileWriter(file));
                try {
                    processReader(s, reader, writer);
                } finally {
                    writer.close();
                }
            } finally {
                reader.close();
            }
        }
    }

    /**
     * Process a script against a single input file.
     *
     * @param s      script to execute.
     * @param reader input file.
     * @param pw     output sink.
     */
    private void processReader(Script s, BufferedReader reader, PrintWriter pw) throws IOException {
        String line;
        String lineCountName = "count";
        s.setProperty(lineCountName, BigInteger.ZERO);
        String autoSplitName = "split";
        s.setProperty("out", pw);

        try {
            InvokerHelper.invokeMethod(s, "begin", null);
        } catch (MissingMethodException mme) {
            // ignore the missing method exception
            // as it means no begin() method is present
        }

        while ((line = reader.readLine()) != null) {
            s.setProperty("line", line);
            s.setProperty(lineCountName, ((BigInteger)s.getProperty(lineCountName)).add(BigInteger.ONE));

            if(autoSplit) {
                s.setProperty(autoSplitName, line.split(splitPattern));
            }

            Object o = s.run();

            if (autoOutput && o != null) {
                pw.println(o);
            }
        }

        try {
            InvokerHelper.invokeMethod(s, "end", null);
        } catch (MissingMethodException mme) {
            // ignore the missing method exception
            // as it means no end() method is present
        }
    }

    /**
     * Process the standard, single script with args.
     */
    private void processOnce() throws CompilationFailedException, IOException {
        GroovyShell groovy = new GroovyShell(conf);

        if (isScriptFile) {
            if (isScriptUrl(script)) {
                groovy.run(getText(script), script.substring(script.lastIndexOf("/") + 1), args);
            } else {
                groovy.run(huntForTheScriptFile(script), args);
            }
        } else {
            groovy.run(script, "script_from_command_line", args);
        }
    }
}
