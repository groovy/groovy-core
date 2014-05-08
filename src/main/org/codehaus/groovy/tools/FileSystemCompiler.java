/*
 * Copyright 2003-2014 the original author or authors.
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

import groovy.cli.CliOptionBuilder;
import groovy.cli.CliOptions;
import groovy.cli.CliParser;
import groovy.cli.CliParserFactory;
import groovy.lang.Binding;
import groovy.lang.GroovyResourceLoader;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ConfigurationException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyStaticMethods;
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

    public static void displayVersion() {
        String version = GroovySystem.getVersion();
        System.err.println("Groovy compiler version " + version);
        System.err.println("Copyright 2003-2014 The Codehaus. http://groovy.codehaus.org/");
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
        CliParser cliParser = CliParserFactory.newParser();
        createCompilationOptions(cliParser);
        CliOptions cli = cliParser.parse(args);

        if (cli.hasOption("h")) {
            cli.displayHelp("groovyc [options] <source-files>", "options:");
            return;
        }

        if (cli.hasOption("v")) {
            displayVersion();
            return;
        }

        displayStackTraceOnError = cli.hasOption("e");

        CompilerConfiguration configuration = generateCompilerConfigurationFromOptions(cli);

        //
        // Load the file name list
        String[] filenames = generateFileNamesFromOptions(cli);
        boolean fileNameErrors = filenames == null;
        if (!fileNameErrors && (filenames.length == 0)) {
            cli.displayHelp("groovyc [options] <source-files>", "options:");
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
     * <p>
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
     * <p>
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
                && !configuration.getJointCompilationOptions().containsKey("stubDir"))
            {
                tmpDir = DefaultGroovyStaticMethods.createTempDir(null, "groovy-generated-", "-java-source");
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

    public static String[] generateFileNamesFromOptions(CliOptions cli) {
        String[] filenames = cli.remainingArgs();
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

    public static CompilerConfiguration generateCompilerConfigurationFromOptions(CliOptions cli) throws IOException {
        //
        // Setup the configuration data

        CompilerConfiguration configuration = new CompilerConfiguration();

        if (cli.hasOption("classpath")) {
            configuration.setClasspath(cli.getOptionValue("classpath"));
        }

        if (cli.hasOption("d")) {
            configuration.setTargetDirectory(cli.getOptionValue("d"));
        }

        if (cli.hasOption("encoding")) {
            configuration.setSourceEncoding(cli.getOptionValue("encoding"));
        }

        if (cli.hasOption("basescript")) {
            configuration.setScriptBaseClass(cli.getOptionValue("basescript"));
        }

        // joint compilation parameters
        if (cli.hasOption("j")) {
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
    public static void createCompilationOptions(CliParser cliParser) {
        cliParser.addOption(CliOptionBuilder.hasArg().withArgName("path").withDescription("Specify where to find the class files - must be first argument").create("classpath"));
        cliParser.addOption(CliOptionBuilder.withLongOpt("classpath").hasArg().withArgName("path").withDescription("Aliases for '-classpath'").create("cp"));
        cliParser.addOption(CliOptionBuilder.withLongOpt("sourcepath").hasArg().withArgName("path").withDescription("Specify where to find the source files").create());
        cliParser.addOption(CliOptionBuilder.withLongOpt("temp").hasArg().withArgName("temp").withDescription("Specify temporary directory").create());
        cliParser.addOption(CliOptionBuilder.withLongOpt("encoding").hasArg().withArgName("encoding").withDescription("Specify the encoding of the user class files").create());
        cliParser.addOption(CliOptionBuilder.hasArg().withDescription("Specify where to place generated class files").create('d'));
        cliParser.addOption(CliOptionBuilder.withLongOpt("help").withDescription("Print a synopsis of standard options").create('h'));
        cliParser.addOption(CliOptionBuilder.withLongOpt("version").withDescription("Print the version").create('v'));
        cliParser.addOption(CliOptionBuilder.withLongOpt("exception").withDescription("Print stack trace on error").create('e'));
        cliParser.addOption(CliOptionBuilder.withLongOpt("jointCompilation").withDescription("Attach javac compiler to compile .java files").create('j'));
        cliParser.addOption(CliOptionBuilder.withLongOpt("basescript").hasArg().withArgName("class").withDescription("Base class name for scripts (must derive from Script)").create('b'));

        cliParser.addOption(
                CliOptionBuilder.withArgName("property=value")
                        .withValueSeparator()
                        .hasArgs(2)
                        .withDescription("name-value pairs to pass to javac")
                        .create("J"));
        cliParser.addOption(
                CliOptionBuilder.withArgName("flag")
                        .hasArg()
                        .withDescription("passed to javac for joint compilation")
                        .create("F"));

        cliParser.addOption(CliOptionBuilder.withLongOpt("indy").withDescription("enables compilation using invokedynamic").create());
        cliParser.addOption(CliOptionBuilder.withLongOpt("configscript").hasArg().withDescription("A script for tweaking the configuration options").create());
    }

    /**
     * Creates a temporary directory in the default temporary directory (as specified by the system
     * property <i>java.io.tmpdir</i>.
     *
     * @deprecated Use {@link DefaultGroovyStaticMethods#createTempDir(java.io.File, String, String)} instead.
     */
    @Deprecated
    public static File createTempDir() throws IOException {
        return DefaultGroovyStaticMethods.createTempDir(null);
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
