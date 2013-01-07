/*
 * Copyright 2003-2013 the original author or authors.
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

package org.codehaus.groovy.tools;

import groovy.lang.Binding;
import groovy.lang.GroovyResourceLoader;
import groovy.lang.GroovyShell;
import org.apache.commons.cli.*;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ConfigurationException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.tools.javac.JavaAwareCompilationUnit;

import groovy.lang.GroovySystem;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Command-line compiler (aka. <tt>groovyc</tt>).
 */
public class FileSystemCompiler {
    private final CompilationUnit unit;

    public FileSystemCompiler(CompilerConfiguration configuration) throws ConfigurationException {
        this(configuration, null);
    }

    public FileSystemCompiler(CompilerConfiguration configuration, CompilationUnit cu) throws ConfigurationException {
        if (cu != null) {
            unit = cu;
        } else if (configuration.getJointCompilationOptions() != null) {
            unit = new JavaAwareCompilationUnit(configuration);
        } else {
            unit = new CompilationUnit(configuration);
        }
    }

    public void compile(String[] paths) throws Exception {
        unit.addSources(paths);
        unit.compile();
    }

    public void compile(File[] files) throws Exception {
        unit.addSources(files);
        unit.compile();
    }

    public static void displayHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, "groovyc [options] <source-files>", "options:", options, "");
    }

    public static void displayVersion() {
        String version = GroovySystem.getVersion();
        System.err.println("Groovy compiler version " + version);
        System.err.println("Copyright 2003-2013 The Codehaus. http://groovy.codehaus.org/");
        System.err.println("");
    }

    public static int checkFiles(String[] filenames) {
        int errors = 0;

        for (String filename : filenames) {
            File file = new File(filename);
            if (!file.exists()) {
                System.err.println("error: file not found: " + file);
                ++errors;
            } else if (!file.canRead()) {
                System.err.println("error: file not readable: " + file);
                ++errors;
            }
        }

        return errors;
    }

    public static boolean validateFiles(String[] filenames) {
        return checkFiles(filenames) == 0;
    }

    private static boolean displayStackTraceOnError = false;

    /**
     * Same as main(args) except that exceptions are thrown out instead of causing
     * the VM to exit.
     */
    public static void commandLineCompile(String[] args) throws Exception {
        commandLineCompile(args, true);
    }

    /**
     * Same as main(args) except that exceptions are thrown out instead of causing
     * the VM to exit and the lookup for .groovy files can be controlled
     */
    public static void commandLineCompile(String[] args, boolean lookupUnnamedFiles) throws Exception {
        Options options = createCompilationOptions();

        CommandLineParser cliParser = new GroovyPosixParser();

        CommandLine cli;
        cli = cliParser.parse(options, args);

        if (cli.hasOption('h')) {
            displayHelp(options);
            return;
        }

        if (cli.hasOption('v')) {
            displayVersion();
            return;
        }

        displayStackTraceOnError = cli.hasOption('e');

        CompilerConfiguration configuration = generateCompilerConfigurationFromOptions(cli);

        //
        // Load the file name list
        String[] filenames = generateFileNamesFromOptions(cli);
        boolean fileNameErrors = filenames == null;
        if (!fileNameErrors && (filenames.length == 0)) {
            displayHelp(options);
            return;
        }

        fileNameErrors = fileNameErrors && !validateFiles(filenames);

        if (!fileNameErrors) {
            doCompilation(configuration, null, filenames, lookupUnnamedFiles);
        }
    }

    /**
     * Primary entry point for compiling from the command line
     * (using the groovyc script).
     * <p/>
     * If calling inside a process and you don't want the JVM to exit on an
     * error call commandLineCompile(String[]), which this method simply wraps
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        commandLineCompileWithErrorHandling(args, true);
    }

    /**
     * Primary entry point for compiling from the command line
     * (using the groovyc script).
     * <p/>
     * If calling inside a process and you don't want the JVM to exit on an
     * error call commandLineCompile(String[]), which this method simply wraps
     *
     * @param args               command line arguments
     * @param lookupUnnamedFiles do a lookup for .groovy files not part of
     *                           the given list of files to compile
     */
    public static void commandLineCompileWithErrorHandling(String[] args, boolean lookupUnnamedFiles) {
        try {
            commandLineCompile(args, lookupUnnamedFiles);
        } catch (Throwable e) {
            new ErrorReporter(e, displayStackTraceOnError).write(System.err);
            System.exit(1);
        }
    }

    public static void doCompilation(CompilerConfiguration configuration, CompilationUnit unit, String[] filenames) throws Exception {
        doCompilation(configuration, unit, filenames, true);
    }

    public static void doCompilation(CompilerConfiguration configuration, CompilationUnit unit, String[] filenames, boolean lookupUnnamedFiles) throws Exception {
        File tmpDir = null;
        // if there are any joint compilation options set stubDir if not set
        try {
            if ((configuration.getJointCompilationOptions() != null)
                    && !configuration.getJointCompilationOptions().containsKey("stubDir")) {
                tmpDir = createTempDir();
                configuration.getJointCompilationOptions().put("stubDir", tmpDir);
            }
            FileSystemCompiler compiler = new FileSystemCompiler(configuration, unit);
            if (lookupUnnamedFiles) {
                for (String filename : filenames) {
                    File file = new File(filename);
                    if (file.isFile()) {
                        URL url = file.getAbsoluteFile().getParentFile().toURI().toURL();
                        compiler.unit.getClassLoader().addURL(url);
                    }
                }
            } else {
                compiler.unit.getClassLoader().setResourceLoader(new GroovyResourceLoader() {
                    public URL loadGroovySource(String filename) throws MalformedURLException {
                        return null;
                    }
                });
            }
            compiler.compile(filenames);
        } finally {
            try {
                if (tmpDir != null) deleteRecursive(tmpDir);
            } catch (Throwable t) {
                System.err.println("error: could not delete temp files - " + tmpDir.getPath());
            }
        }
    }

    public static String[] generateFileNamesFromOptions(CommandLine cli) {
        String[] filenames = cli.getArgs();
        List<String> fileList = new ArrayList<String>(filenames.length);
        boolean errors = false;
        for (String filename : filenames) {
            if (filename.startsWith("@")) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(filename.substring(1)));
                    String file;
                    while ((file = br.readLine()) != null) {
                        fileList.add(file);
                    }
                } catch (IOException ioe) {
                    System.err.println("error: file not readable: " + filename.substring(1));
                    errors = true;
                }
            } else {
                fileList.add(filename);
            }
        }
        if (errors) {
            return null;
        } else {
            return fileList.toArray(new String[fileList.size()]);
        }
    }

    public static CompilerConfiguration generateCompilerConfigurationFromOptions(CommandLine cli) throws IOException {
        //
        // Setup the configuration data

        CompilerConfiguration configuration = new CompilerConfiguration();

        if (cli.hasOption("classpath")) {
            configuration.setClasspath(cli.getOptionValue("classpath"));
        }

        if (cli.hasOption('d')) {
            configuration.setTargetDirectory(cli.getOptionValue('d'));
        }

        if (cli.hasOption("encoding")) {
            configuration.setSourceEncoding(cli.getOptionValue("encoding"));
        }

        if (cli.hasOption("baseScript")) {
            configuration.setScriptBaseClass(cli.getOptionValue("baseScript"));
        }

        // joint compilation parameters
        if (cli.hasOption('j')) {
            Map<String, Object> compilerOptions = new HashMap<String, Object>();

            String[] opts = cli.getOptionValues("J");
            compilerOptions.put("namedValues", opts);

            opts = cli.getOptionValues("F");
            compilerOptions.put("flags", opts);

            configuration.setJointCompilationOptions(compilerOptions);
        }

        if (cli.hasOption("indy")) {
            configuration.getOptimizationOptions().put("int", false);
            configuration.getOptimizationOptions().put("indy", true);
        }

        if (cli.hasOption("configscript")) {
            String path = cli.getOptionValue("configscript");
            File groovyConfigurator = new File(path);
            Binding binding = new Binding();
            binding.setVariable("configuration", configuration);

            CompilerConfiguration configuratorConfig = new CompilerConfiguration();
            ImportCustomizer customizer = new ImportCustomizer();
            customizer.addStaticStars("org.codehaus.groovy.control.customizers.builder.CompilerCustomizationBuilder");
            configuratorConfig.addCompilationCustomizers(customizer);

            GroovyShell shell = new GroovyShell(binding, configuratorConfig);
            shell.evaluate(groovyConfigurator);
        }
        
        return configuration;
    }

    @SuppressWarnings({"AccessStaticViaInstance"})
    public static Options createCompilationOptions() {
        //
        // Parse the command line

        Options options = new Options();

        options.addOption(OptionBuilder.hasArg().withArgName("path").withDescription("Specify where to find the class files - must be first argument").create("classpath"));
        options.addOption(OptionBuilder.withLongOpt("classpath").hasArg().withArgName("path").withDescription("Aliases for '-classpath'").create("cp"));
        options.addOption(OptionBuilder.withLongOpt("sourcepath").hasArg().withArgName("path").withDescription("Specify where to find the source files").create());
        options.addOption(OptionBuilder.withLongOpt("temp").hasArg().withArgName("temp").withDescription("Specify temporary directory").create());
        options.addOption(OptionBuilder.withLongOpt("encoding").hasArg().withArgName("encoding").withDescription("Specify the encoding of the user class files").create());
        options.addOption(OptionBuilder.hasArg().withDescription("Specify where to place generated class files").create('d'));
//            options.addOption(OptionBuilder.withLongOpt("strict").withDescription("Turn on strict type safety.").create('s'));
        options.addOption(OptionBuilder.withLongOpt("help").withDescription("Print a synopsis of standard options").create('h'));
        options.addOption(OptionBuilder.withLongOpt("version").withDescription("Print the version").create('v'));
        options.addOption(OptionBuilder.withLongOpt("exception").withDescription("Print stack trace on error").create('e'));
        options.addOption(OptionBuilder.withLongOpt("jointCompilation").withDescription("Attach javac compiler to compile .java files").create('j'));
        options.addOption(OptionBuilder.withLongOpt("baseScript").hasArg().withArgName("class").withDescription("Base class name for scripts (must derive from Script)").create('b'));

        options.addOption(
                OptionBuilder.withArgName("property=value")
                        .withValueSeparator()
                        .hasArgs(2)
                        .withDescription("name-value pairs to pass to javac")
                        .create("J"));
        options.addOption(
                OptionBuilder.withArgName("flag")
                        .hasArg()
                        .withDescription("passed to javac for joint compilation")
                        .create("F"));

        options.addOption(OptionBuilder.withLongOpt("indy").withDescription("enables compilation using invokedynamic").create());
        options.addOption(OptionBuilder.withLongOpt("configscript").hasArg().withDescription("A script for tweaking the configuration options").create());
        return options;
    }

    public static File createTempDir() throws IOException {
        final int MAXTRIES = 3;
        int accessDeniedCounter = 0;
        File tempFile = null;
        for (int i = 0; i < MAXTRIES; i++) {
            try {
                tempFile = File.createTempFile("groovy-generated-", "-java-source");
                tempFile.delete();
                tempFile.mkdirs();
                break;
            } catch (IOException ioe) {
                if (ioe.getMessage().startsWith("Access is denied")) {
                    accessDeniedCounter++;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        /* ignore */
                    }
                }
                if (i == MAXTRIES - 1) {
                    if (accessDeniedCounter == MAXTRIES) {
                        String msg =
                                "Access is denied.\nWe tried " + accessDeniedCounter +
                                        " times to create a temporary directory" +
                                        " and failed each time. If you are on Windows" +
                                        " you are possibly victim to" +
                                        " http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6325169. " +
                                        " this is not a bug in Groovy.";
                        throw new IOException(msg);
                    } else {
                        throw ioe;
                    }
                }
            }
        }
        return tempFile;
    }

    public static void deleteRecursive(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteRecursive(files[i]);
            }
            file.delete();
        }
    }
}
