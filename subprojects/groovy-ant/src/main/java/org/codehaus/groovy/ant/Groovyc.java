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
package org.codehaus.groovy.ant;

import groovy.cli.CliOptions;
import groovy.cli.CliParser;
import groovy.cli.CliParserFactory;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyResourceLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GroovyInternalPosixParser;
import org.apache.commons.cli.Options;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceExtensionHandler;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.DefaultGroovyStaticMethods;
import org.codehaus.groovy.tools.ErrorReporter;
import org.codehaus.groovy.tools.FileSystemCompiler;
import org.codehaus.groovy.tools.RootLoader;
import org.codehaus.groovy.tools.javac.JavaAwareCompilationUnit;

/**
 * Compiles Groovy source files. This task can take the following arguments:
 * <ul>
 * <li>srcdir</li>
 * <li>scriptExtension</li>
 * <li>targetBytecode</li>
 * <li>destdir</li>
 * <li>sourcepath</li>
 * <li>sourcepathRef</li>
 * <li>classpath</li>
 * <li>classpathRef</li>
 * <li>listfiles</li>
 * <li>failonerror</li>
 * <li>proceed</li>
 * <li>memoryInitialSize</li>
 * <li>memoryMaximumSize</li>
 * <li>encoding</li>
 * <li>verbose</li>
 * <li>includeantruntime</li>
 * <li>includejavaruntime</li>
 * <li>fork</li>
 * <li>javaHome</li>
 * <li>executable</li>
 * <li>updatedProperty</li>
 * <li>errorProperty</li>
 * <li>includeDestClasses</li>
 * <li>jointCompilationOptions</li>
 * <li>stacktrace</li>
 * <li>indy</li>
 * <li>scriptBaseClass</li>
 * <li>stubdir</li>
 * <li>keepStubs</li>
 * <li>forceLookupUnnamedFiles</li>
 * <li>configscript</li>
 * </ul>
 * And these nested tasks:
 * <ul>
 * <li>javac</li>
 * </ul>
 * Of these arguments, the <b>srcdir</b> and <b>destdir</b> are required.
 * <p>
 * <p>When this task executes, it will recursively scan srcdir and destdir looking for Groovy source files
 * to compile. This task makes its compile decision based on timestamp.
 * <p>
 * Based heavily on the Javac implementation in Ant.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author Hein Meling
 * @author <a href="mailto:russel.winder@concertant.com">Russel Winder</a>
 * @author Danno Ferrin
 * @author Paul King
 */
public class Groovyc extends MatchingTask {
    private final LoggingHelper log = new LoggingHelper(this);

    private Path src;
    private File destDir;
    private Path compileClasspath;
    private Path compileSourcepath;
    private String encoding;
    private boolean stacktrace = false;
    private boolean verbose = false;
    private boolean includeAntRuntime = true;
    private boolean includeJavaRuntime = false;
    private boolean fork = false;
    private File forkJavaHome;
    private String forkedExecutable = null;
    private String memoryInitialSize;
    private String memoryMaximumSize;
    private String scriptExtension = "*.groovy";
    private String targetBytecode = null;

    protected boolean failOnError = true;
    protected boolean listFiles = false;
    protected File[] compileList = new File[0];

    private String updatedProperty;
    private String errorProperty;
    private boolean taskSuccess = true;
    private boolean includeDestClasses = true;

    protected CompilerConfiguration configuration;
    private Javac javac;
    private boolean jointCompilation;

    private List<File> temporaryFiles = new ArrayList<File>(2);
    private File stubDir;
    private boolean keepStubs;
    private boolean forceLookupUnnamedFiles;
    private boolean useIndy;
    private String scriptBaseClass;
    private String configscript;

    private Set<String> scriptExtensions = new LinkedHashSet<String>();

    /**
     * Adds a path for source compilation.
     *
     * @return a nested src element.
     */
    public Path createSrc() {
        if (src == null) {
            src = new Path(getProject());
        }
        return src.createPath();
    }

    /**
     * Recreate src.
     *
     * @return a nested src element.
     */
    protected Path recreateSrc() {
        src = null;
        return createSrc();
    }

    /**
     * Set the source directories to find the source Java files.
     *
     * @param srcDir the source directories as a path
     */
    public void setSrcdir(Path srcDir) {
        if (src == null) {
            src = srcDir;
        } else {
            src.append(srcDir);
        }
    }

    /**
     * Gets the source dirs to find the source java files.
     *
     * @return the source directories as a path
     */
    public Path getSrcdir() {
        return src;
    }

    /**
     * Set the extension to use when searching for Groovy source files.
     * Accepts extensions in the form *.groovy, .groovy or groovy
     *
     * @param scriptExtension the extension of Groovy source files
     */
    public void setScriptExtension(String scriptExtension) {
        if (scriptExtension.startsWith("*.")) {
            this.scriptExtension = scriptExtension;
        } else if (scriptExtension.startsWith(".")) {
            this.scriptExtension = "*" + scriptExtension;
        } else {
            this.scriptExtension = "*." + scriptExtension;
        }
    }

    /**
     * Get the extension to use when searching for Groovy source files.
     *
     * @return the extension of Groovy source files
     */
    public String getScriptExtension() {
        return scriptExtension;
    }

    /**
     * Sets the bytecode compatibility mode
     *
     * @param version the bytecode compatibility mode
     */
    public void setTargetBytecode(String version) {
        if (CompilerConfiguration.PRE_JDK5.equals(version) || CompilerConfiguration.POST_JDK5.equals(version)) {
            this.targetBytecode = version;
        }
    }

    /**
     * Retrieves the compiler bytecode compatibility mode.
     *
     * @return bytecode compatibility mode. Can be either <tt>1.5</tt> or <tt>1.4</tt>.
     */
    public String getTargetBytecode() {
        return this.targetBytecode;
    }

    /**
     * Set the destination directory into which the Java source
     * files should be compiled.
     *
     * @param destDir the destination director
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Gets the destination directory into which the java source files
     * should be compiled.
     *
     * @return the destination directory
     */
    public File getDestdir() {
        return destDir;
    }

    /**
     * Set the sourcepath to be used for this compilation.
     *
     * @param sourcepath the source path
     */
    public void setSourcepath(Path sourcepath) {
        if (compileSourcepath == null) {
            compileSourcepath = sourcepath;
        } else {
            compileSourcepath.append(sourcepath);
        }
    }

    /**
     * Gets the sourcepath to be used for this compilation.
     *
     * @return the source path
     */
    public Path getSourcepath() {
        return compileSourcepath;
    }

    /**
     * Adds a path to sourcepath.
     *
     * @return a sourcepath to be configured
     */
    public Path createSourcepath() {
        if (compileSourcepath == null) {
            compileSourcepath = new Path(getProject());
        }
        return compileSourcepath.createPath();
    }

    /**
     * Adds a reference to a source path defined elsewhere.
     *
     * @param r a reference to a source path
     */
    public void setSourcepathRef(Reference r) {
        createSourcepath().setRefid(r);
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void setClasspath(Path classpath) {
        if (compileClasspath == null) {
            compileClasspath = classpath;
        } else {
            compileClasspath.append(classpath);
        }
    }

    /**
     * Gets the classpath to be used for this compilation.
     *
     * @return the class path
     */
    public Path getClasspath() {
        return compileClasspath;
    }

    /**
     * Adds a path to the classpath.
     *
     * @return a class path to be configured
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     *
     * @param r a reference to a classpath
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * If true, list the source files being handed off to the compiler.
     * Default is false.
     *
     * @param list if true list the source files
     */
    public void setListfiles(boolean list) {
        listFiles = list;
    }

    /**
     * Get the listfiles flag.
     *
     * @return the listfiles flag
     */
    public boolean getListfiles() {
        return listFiles;
    }

    /**
     * Indicates whether the build will continue
     * even if there are compilation errors; defaults to true.
     *
     * @param fail if true halt the build on failure
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * @param proceed inverse of failonerror
     */
    public void setProceed(boolean proceed) {
        failOnError = !proceed;
    }

    /**
     * Gets the failonerror flag.
     *
     * @return the failonerror flag
     */
    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * The initial size of the memory for the underlying VM
     * if javac is run externally; ignored otherwise.
     * Defaults to the standard VM memory setting.
     * (Examples: 83886080, 81920k, or 80m)
     *
     * @param memoryInitialSize string to pass to VM
     */
    public void setMemoryInitialSize(String memoryInitialSize) {
        this.memoryInitialSize = memoryInitialSize;
    }

    /**
     * Gets the memoryInitialSize flag.
     *
     * @return the memoryInitialSize flag
     */
    public String getMemoryInitialSize() {
        return memoryInitialSize;
    }

    /**
     * The maximum size of the memory for the underlying VM
     * if javac is run externally; ignored otherwise.
     * Defaults to the standard VM memory setting.
     * (Examples: 83886080, 81920k, or 80m)
     *
     * @param memoryMaximumSize string to pass to VM
     */
    public void setMemoryMaximumSize(String memoryMaximumSize) {
        this.memoryMaximumSize = memoryMaximumSize;
    }

    /**
     * Gets the memoryMaximumSize flag.
     *
     * @return the memoryMaximumSize flag
     */
    public String getMemoryMaximumSize() {
        return memoryMaximumSize;
    }

    /**
     * Sets the file encoding for generated files.
     *
     * @param encoding the file encoding to be used
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the encoding to be used when creating files.
     *
     * @return the file encoding to use
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Enable verbose compiling which will display which files
     * are being compiled. Default is false.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Gets the verbose flag.
     *
     * @return the verbose flag
     */
    public boolean getVerbose() {
        return verbose;
    }

    /**
     * If true, includes Ant's own classpath in the classpath. Default is true.
     * If setting to false and using groovyc in conjunction with AntBuilder
     * you might need to explicitly add the Groovy jar(s) to the groovyc
     * classpath using a nested classpath task.
     *
     * @param include if true, includes Ant's own classpath in the classpath
     */
    public void setIncludeantruntime(boolean include) {
        includeAntRuntime = include;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the classpath.
     *
     * @return whether or not the ant classpath is to be included in the classpath
     */
    public boolean getIncludeantruntime() {
        return includeAntRuntime;
    }

    /**
     * If true, includes the Java runtime libraries in the classpath. Default is false.
     *
     * @param include if true, includes the Java runtime libraries in the classpath
     */
    public void setIncludejavaruntime(boolean include) {
        includeJavaRuntime = include;
    }

    /**
     * Gets whether or not the java runtime should be included in this
     * task's classpath.
     *
     * @return the includejavaruntime attribute
     */
    public boolean getIncludejavaruntime() {
        return includeJavaRuntime;
    }

    /**
     * If true forks the Groovy compiler. Default is false.
     *
     * @param f "true|false|on|off|yes|no"
     */
    public void setFork(boolean f) {
        fork = f;
    }

    /**
     * The JDK Home to use when forked.
     * Ignored if "executable" is specified.
     *
     * @param home the java.home value to use, default is the current JDK's home
     */
    public void setJavaHome(File home) {
        forkJavaHome = home;
    }

    /**
     * Sets the name of the java executable to use when
     * invoking the compiler in forked mode, ignored otherwise.
     *
     * @param forkExecPath the name of the executable
     * @since Groovy 1.8.7
     */
    public void setExecutable(String forkExecPath) {
        forkedExecutable = forkExecPath;
    }

    /**
     * The value of the executable attribute, if any.
     *
     * @return the name of the java executable
     * @since Groovy 1.8.7
     */
    public String getExecutable() {
        return forkedExecutable;
    }

    /**
     * The property to set on compilation success.
     * This property will not be set if the compilation
     * fails, or if there are no files to compile.
     *
     * @param updatedProperty the property name to use.
     */
    public void setUpdatedProperty(String updatedProperty) {
        this.updatedProperty = updatedProperty;
    }

    /**
     * The property to set on compilation failure.
     * This property will be set if the compilation
     * fails.
     *
     * @param errorProperty the property name to use.
     */
    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }

    /**
     * This property controls whether to include the
     * destination classes directory in the classpath
     * given to the compiler.
     * The default value is "true".
     *
     * @param includeDestClasses the value to use.
     */
    public void setIncludeDestClasses(boolean includeDestClasses) {
        this.includeDestClasses = includeDestClasses;
    }

    /**
     * Get the value of the includeDestClasses property.
     *
     * @return the value.
     */
    public boolean isIncludeDestClasses() {
        return includeDestClasses;
    }

    /**
     * Get the result of the groovyc task (success or failure).
     *
     * @return true if compilation succeeded, or
     *         was not necessary, false if the compilation failed.
     */
    public boolean getTaskSuccess() {
        return taskSuccess;
    }

    /**
     * Add the configured nested javac task if present to initiate joint compilation.
     */
    public void addConfiguredJavac(final Javac javac) {
        this.javac = javac;
        jointCompilation = true;
    }

    /**
     * Enable compiler to report stack trace information if a problem occurs
     * during compilation. Default is false.
     */
    public void setStacktrace(boolean stacktrace) {
        this.stacktrace = stacktrace;
    }

    /**
     * Set the indy flag.
     *
     * @param useIndy the indy flag
     */
    public void setIndy(boolean useIndy) {
        this.useIndy = useIndy;
    }

    /**
     * Get the value of the indy flag.
     *
     * @return if to use indy
     */
    public boolean getIndy() {
        return this.useIndy;
    }

    /**
     * Set the base script class name for the scripts (must derive from Script)
     *
     * @param scriptBaseClass Base class name for scripts (must derive from Script)
     */
    public void setScriptBaseClass(String scriptBaseClass) {
        this.scriptBaseClass = scriptBaseClass;
    }

    /**
     * Get the base script class name for the scripts (must derive from Script)
     *
     * @return Base class name for scripts (must derive from Script)
     */
    public String getScriptBaseClass() {
        return this.scriptBaseClass;
    }

    /**
     * Get the configuration file used to customize the compilation configuration.
     *
     * @return a path to a configuration script
     */
    public String getConfigscript() {
        return configscript;
    }

    /**
     * Set the configuration file used to customize the compilation configuration.
     *
     * @param configscript a path to a configuration script
     */
    public void setConfigscript(final String configscript) {
        this.configscript = configscript;
    }

    /**
     * Set the stub directory into which the Java source stub
     * files should be generated. The directory need not exist
     * and will not be deleted automatically - though its contents
     * will be cleared unless 'keepStubs' is true. Ignored when forked.
     *
     * @param stubDir the stub directory
     */
    public void setStubdir(File stubDir) {
        jointCompilation = true;
        this.stubDir = stubDir;
    }

    /**
     * Gets the stub directory into which the Java source stub
     * files should be generated
     *
     * @return the stub directory
     */
    public File getStubdir() {
        return stubDir;
    }

    /**
     * Set the keepStubs flag. Defaults to false. Set to true for debugging.
     * Ignored when forked.
     *
     * @param keepStubs should stubs be retained
     */
    public void setKeepStubs(boolean keepStubs) {
        this.keepStubs = keepStubs;
    }

    /**
     * Gets the keepStubs flag.
     *
     * @return the keepStubs flag
     */
    public boolean getKeepStubs() {
        return keepStubs;
    }

    /**
     * Set the forceLookupUnnamedFiles flag. Defaults to false.
     *
     * The Groovyc Ant task is frequently used in the context of a build system
     * that knows the complete list of source files to be compiled. In such a
     * context, it is wasteful for the Groovy compiler to go searching the
     * classpath when looking for source files and hence by default the
     * Groovyc Ant task calls the compiler in a special mode with such searching
     * turned off. If you wish the compiler to search for source files then
     * you need to set this flag to {@code true}.
     *
     * @param forceLookupUnnamedFiles should unnamed source files be searched for on the classpath
     */
    public void setForceLookupUnnamedFiles(boolean forceLookupUnnamedFiles) {
        this.forceLookupUnnamedFiles = forceLookupUnnamedFiles;
    }

    /**
     * Gets the forceLookupUnnamedFiles flag.
     *
     * @return the forceLookupUnnamedFiles flag
     */
    public boolean getForceLookupUnnamedFiles() {
        return forceLookupUnnamedFiles;
    }

    /**
     * Executes the task.
     *
     * @throws BuildException if an error occurs
     */
    public void execute() throws BuildException {
        checkParameters();
        resetFileLists();
        loadRegisteredScriptExtensions();

        if (javac != null) jointCompilation = true;

        // scan source directories and dest directory to build up
        // compile lists
        String[] list = src.list();
        for (String filename : list) {
            File file = getProject().resolveFile(filename);
            if (!file.exists()) {
                throw new BuildException("srcdir \"" + file.getPath() + "\" does not exist!", getLocation());
            }
            DirectoryScanner ds = this.getDirectoryScanner(file);
            String[] files = ds.getIncludedFiles();
            scanDir(file, destDir != null ? destDir : file, files);
        }

        compile();
        if (updatedProperty != null
                && taskSuccess
                && compileList.length != 0) {
            getProject().setNewProperty(updatedProperty, "true");
        }
    }

    /**
     * Clear the list of files to be compiled and copied.
     */
    protected void resetFileLists() {
        compileList = new File[0];
        scriptExtensions = new LinkedHashSet<String>();
    }

    /**
     * Scans the directory looking for source files to be compiled.
     * The results are returned in the class variable compileList
     *
     * @param srcDir  The source directory
     * @param destDir The destination directory
     * @param files   An array of filenames
     */
    protected void scanDir(File srcDir, File destDir, String[] files) {
        GlobPatternMapper m = new GlobPatternMapper();
        SourceFileScanner sfs = new SourceFileScanner(this);
        File[] newFiles;
        for (String extension : getScriptExtensions()) {
            m.setFrom("*." + extension);
            m.setTo("*.class");
            newFiles = sfs.restrictAsFiles(files, srcDir, destDir, m);
            addToCompileList(newFiles);
        }

        if (jointCompilation) {
            m.setFrom("*.java");
            m.setTo("*.class");
            newFiles = sfs.restrictAsFiles(files, srcDir, destDir, m);
            addToCompileList(newFiles);
        }
    }

    protected void addToCompileList(File[] newFiles) {
        if (newFiles.length > 0) {
            File[] newCompileList = new File[compileList.length + newFiles.length];
            System.arraycopy(compileList, 0, newCompileList, 0, compileList.length);
            System.arraycopy(newFiles, 0, newCompileList, compileList.length, newFiles.length);
            compileList = newCompileList;
        }
    }

    /**
     * Gets the list of files to be compiled.
     *
     * @return the list of files as an array
     */
    public File[] getFileList() {
        return compileList;
    }

    protected void checkParameters() throws BuildException {
        if (src == null) {
            throw new BuildException("srcdir attribute must be set!", getLocation());
        }
        if (src.size() == 0) {
            throw new BuildException("srcdir attribute must be set!", getLocation());
        }

        if (destDir != null && !destDir.isDirectory()) {
            throw new BuildException("destination directory \""
                    + destDir
                    + "\" does not exist or is not a directory",
                    getLocation());
        }

        if (encoding != null && !Charset.isSupported(encoding)) {
            throw new BuildException("encoding \"" + encoding + "\" not supported.");
        }
    }

    private void listFiles() {
        if (listFiles) {
            for (File srcFile : compileList) {
                log.info(srcFile.getAbsolutePath());
            }
        }
    }

    private List<String> extractJointOptions(Path classpath) {
        List<String> jointOptions = new ArrayList<String>();
        if (!jointCompilation) return jointOptions;

        // extract joint options, some get pushed up...
        RuntimeConfigurable rc = javac.getRuntimeConfigurableWrapper();
        for (Object o1 : rc.getAttributeMap().entrySet()) {
            final Map.Entry e = (Map.Entry) o1;
            final String key = e.getKey().toString();
            final String value = getProject().replaceProperties(e.getValue().toString());
            if (key.contains("debug")) {
                String level = "";
                if (javac.getDebugLevel() != null) {
                    level = ":" + javac.getDebugLevel();
                }
                jointOptions.add("-Fg" + level);
            } else if (key.contains("debugLevel")) {
                // ignore, taken care of in debug
            } else if ((key.contains("nowarn"))
                    || (key.contains("verbose"))
                    || (key.contains("deprecation"))) {
                // false is default, so something to do only in true case
                if ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value))
                    jointOptions.add("-F" + key);
            } else if (key.contains("classpath")) {
                classpath.add(javac.getClasspath());
            } else if ((key.contains("depend"))
                    || (key.contains("extdirs"))
                    || (key.contains("encoding"))
                    || (key.contains("source"))
                    || (key.contains("target"))
                    || (key.contains("verbose"))) { // already handling verbose but pass on too
                jointOptions.add("-J" + key + "=" + value);
            } else {
                log.warn("The option " + key + " cannot be set on the contained <javac> element. The option will be ignored");
            }
            // TODO includes? excludes?
        }

        // ant's <javac> supports nested <compilerarg value=""> elements (there can be multiple of them)
        // for additional options to be passed to javac.
        Enumeration children = rc.getChildren();
        while (children.hasMoreElements()) {
            RuntimeConfigurable childrc = (RuntimeConfigurable) children.nextElement();
            if (childrc.getElementTag().equals("compilerarg")) {
                for (Object o : childrc.getAttributeMap().entrySet()) {
                    final Map.Entry e = (Map.Entry) o;
                    final String key = e.getKey().toString();
                    if (key.equals("value")) {
                        final String value = getProject().replaceProperties(e.getValue().toString());
                        StringTokenizer st = new StringTokenizer(value, " ");
                        while (st.hasMoreTokens()) {
                            String optionStr = st.nextToken();
                            String replaced = optionStr.replace("-X", "-FX");
                            if (optionStr.equals(replaced)) {
                                replaced = optionStr.replace("-W", "-FW"); // GROOVY-5063
                            }
                            jointOptions.add(replaced);
                        }
                    }
                }
            }
        }

        return jointOptions;
    }

    private void doForkCommandLineList(List<String> commandLineList, Path classpath, String separator) {
        if (!fork) return;

        if (includeAntRuntime) {
            classpath.addExisting((new Path(getProject())).concatSystemClasspath("last"));
        }
        if (includeJavaRuntime) {
            classpath.addJavaRuntime();
        }

        if (forkedExecutable != null && !forkedExecutable.equals("")) {
            commandLineList.add(forkedExecutable);
        } else {
            String javaHome;
            if (forkJavaHome != null) {
                javaHome = forkJavaHome.getPath();
            } else {
                javaHome = System.getProperty("java.home");
            }
            commandLineList.add(javaHome + separator + "bin" + separator + "java");
        }
        commandLineList.add("-classpath");
        commandLineList.add(classpath.toString());

        final String fileEncodingProp = System.getProperty("file.encoding");
        if ((fileEncodingProp != null) && !fileEncodingProp.equals("")) {
            commandLineList.add("-Dfile.encoding=" + fileEncodingProp);
        }
        if (targetBytecode != null) {
            commandLineList.add("-Dgroovy.target.bytecode=" + targetBytecode);
        }

        if ((memoryInitialSize != null) && !memoryInitialSize.equals("")) {
            commandLineList.add("-Xms" + memoryInitialSize);
        }
        if ((memoryMaximumSize != null) && !memoryMaximumSize.equals("")) {
            commandLineList.add("-Xmx" + memoryMaximumSize);
        }
        if (!"*.groovy".equals(getScriptExtension())) {
            String tmpExtension = getScriptExtension();
            if (tmpExtension.startsWith("*."))
                tmpExtension = tmpExtension.substring(1);
            commandLineList.add("-Dgroovy.default.scriptExtension=" + tmpExtension);
        }
        commandLineList.add(FileSystemCompilerFacade.class.getName());
        if (forceLookupUnnamedFiles) {
            commandLineList.add("--forceLookupUnnamedFiles");
        }
    }

    private void doNormalCommandLineList(List<String> commandLineList, List<String> jointOptions, Path classpath) {
        commandLineList.add("--classpath");
        commandLineList.add(classpath.toString());
        if (jointCompilation) {
            commandLineList.add("-j");
            commandLineList.addAll(jointOptions);
        }
        if (destDir != null) {
            commandLineList.add("-d");
            commandLineList.add(destDir.getPath());
        }
        if (encoding != null) {
            commandLineList.add("--encoding");
            commandLineList.add(encoding);
        }
        if (stacktrace) {
            commandLineList.add("-e");
        }
        if (useIndy) {
            commandLineList.add("--indy");
        }
        if (scriptBaseClass != null) {
            commandLineList.add("-b");
            commandLineList.add(scriptBaseClass);
        }
        if (configscript != null) {
            commandLineList.add("--configscript");
            commandLineList.add(configscript);
        }
    }

    private void addSourceFiles(List<String> commandLineList) {
        // check to see if an external file is needed
        int count = 0;
        if (fork) {
            for (File srcFile : compileList) {
                count += srcFile.getPath().length();
            }
            for (Object commandLineArg : commandLineList) {
                count += commandLineArg.toString().length();
            }
            count += compileList.length;
            count += commandLineList.size();
        }
        // 32767 is the command line length limit on Windows
        if (fork && (count > 32767)) {
            try {
                File tempFile = File.createTempFile("groovyc-files-", ".txt");
                temporaryFiles.add(tempFile);
                PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
                for (File srcFile : compileList) {
                    pw.println(srcFile.getPath());
                }
                pw.close();
                commandLineList.add("@" + tempFile.getPath());
            } catch (IOException e) {
                log.error("Error creating file list", e);
            }
        } else {
            for (File srcFile : compileList) {
                commandLineList.add(srcFile.getPath());
            }
        }
    }

    private String[] makeCommandLine(List<String> commandLineList) {
        final String[] commandLine = new String[commandLineList.size()];
        for (int i = 0; i < commandLine.length; ++i) {
            commandLine[i] = commandLineList.get(i);
        }
        log.verbose("Compilation arguments:");
        log.verbose(DefaultGroovyMethods.join(commandLine, "\n"));
        return commandLine;
    }

    private void runForked(String[] commandLine) {
        // use the main method in FileSystemCompiler
        final Execute executor = new Execute(); // new LogStreamHandler ( attributes , Project.MSG_INFO , Project.MSG_WARN ) ) ;
        executor.setAntRun(getProject());
        executor.setWorkingDirectory(getProject().getBaseDir());
        executor.setCommandline(commandLine);
        try {
            executor.execute();
        } catch (final IOException ioe) {
            throw new BuildException("Error running forked groovyc.", ioe);
        }
        final int returnCode = executor.getExitValue();
        if (returnCode != 0) {
            taskSuccess = false;
            if (errorProperty != null) {
                getProject().setNewProperty(errorProperty, "true");
            }
            if (failOnError) {
                throw new BuildException("Forked groovyc returned error code: " + returnCode);
            } else {
                log.error("Forked groovyc returned error code: " + returnCode);
            }
        }
    }

    private void runCompiler(String[] commandLine) {
        // hand crank it so we can add our own compiler configuration
        try {
            CliParser cliParser = CliParserFactory.newParser();
            FileSystemCompiler.createCompilationOptions(cliParser);
            CliOptions cli = cliParser.parse(commandLine);

            configuration = FileSystemCompiler.generateCompilerConfigurationFromOptions(cli);
            configuration.setScriptExtensions(getScriptExtensions());
            String tmpExtension = getScriptExtension();
            if (tmpExtension.startsWith("*."))
                tmpExtension = tmpExtension.substring(1);
            configuration.setDefaultScriptExtension(tmpExtension);

            // Load the file name list
            String[] filenames = FileSystemCompiler.generateFileNamesFromOptions(cli);
            boolean fileNameErrors = filenames == null;

            fileNameErrors = fileNameErrors && !FileSystemCompiler.validateFiles(filenames);

            if (targetBytecode != null) {
                configuration.setTargetBytecode(targetBytecode);
            }

            if (!fileNameErrors) {
                FileSystemCompiler.doCompilation(configuration, makeCompileUnit(), filenames, forceLookupUnnamedFiles);
            }

        } catch (Exception re) {
            Throwable t = re;
            if ((re.getClass() == RuntimeException.class) && (re.getCause() != null)) {
                // unwrap to the real exception
                t = re.getCause();
            }
            StringWriter writer = new StringWriter();
            new ErrorReporter(t, false).write(new PrintWriter(writer));
            String message = writer.toString();

            taskSuccess = false;
            if (errorProperty != null) {
                getProject().setNewProperty(errorProperty, "true");
            }

            if (failOnError) {
                log.error(message);
                throw new BuildException("Compilation Failed", t, getLocation());
            } else {
                log.error(message);
            }
        }
    }

    protected void compile() {
        if (compileList.length == 0) return;

        try {
            log.info("Compiling " + compileList.length + " source file"
                    + (compileList.length == 1 ? "" : "s")
                    + (destDir != null ? " to " + destDir : ""));

            listFiles();
            Path classpath = getClasspath() != null ? getClasspath() : new Path(getProject());
            List<String> jointOptions = extractJointOptions(classpath);

            String separator = System.getProperty("file.separator");
            List<String> commandLineList = new ArrayList<String>();

            doForkCommandLineList(commandLineList, classpath, separator);
            doNormalCommandLineList(commandLineList, jointOptions, classpath);
            addSourceFiles(commandLineList);

            String[] commandLine = makeCommandLine(commandLineList);

            if (fork) {
                runForked(commandLine);
            } else {
                runCompiler(commandLine);
            }
        } finally {
            for (File temporaryFile : temporaryFiles) {
                try {
                    FileSystemCompiler.deleteRecursive(temporaryFile);
                } catch (Throwable t) {
                    System.err.println("error: could not delete temp files - " + temporaryFile.getPath());
                }
            }
        }
    }

    protected CompilationUnit makeCompileUnit() {
        Map<String, Object> options = configuration.getJointCompilationOptions();
        if (options != null) {
            if (keepStubs) {
                options.put("keepStubs", Boolean.TRUE);
            }
            if (stubDir != null) {
                options.put("stubDir", stubDir);
            } else {
                try {
                    File tempStubDir = DefaultGroovyStaticMethods.createTempDir(null, "groovy-generated-", "-java-source");
                    temporaryFiles.add(tempStubDir);
                    options.put("stubDir", tempStubDir);
                } catch (IOException ioe) {
                    throw new BuildException(ioe);
                }
            }
            return new JavaAwareCompilationUnit(configuration, buildClassLoaderFor());
        } else {
            return new CompilationUnit(configuration, null, buildClassLoaderFor());
        }
    }

    protected GroovyClassLoader buildClassLoaderFor() {
        // GROOVY-5044
        if (!fork && !getIncludeantruntime()) {
            throw new IllegalArgumentException("The includeAntRuntime=false option is not compatible with fork=false");
        }
        ClassLoader parent = getIncludeantruntime()
                ? getClass().getClassLoader()
                : new AntClassLoader(new RootLoader(new URL[0], null), getProject(), getClasspath());
        if (parent instanceof AntClassLoader) {
            AntClassLoader antLoader = (AntClassLoader) parent;
            String[] pathElm = antLoader.getClasspath().split(File.pathSeparator);
            List<String> classpath = configuration.getClasspath();
            /*
             * Iterate over the classpath provided to groovyc, and add any missing path
             * entries to the AntClassLoader.  This is a workaround, since for some reason
             * 'directory' classpath entries were not added to the AntClassLoader' classpath.
             */
            for (String cpEntry : classpath) {
                boolean found = false;
                for (String path : pathElm) {
                    if (cpEntry.equals(path)) {
                        found = true;
                        break;
                    }
                }
                /*
                 * fix for GROOVY-2284
                 * seems like AntClassLoader doesn't check if the file
                 * may not exist in the classpath yet
                 */
                if (!found && new File(cpEntry).exists()) {
                	try {
                		antLoader.addPathElement(cpEntry);
                	}
                	catch(BuildException e) {
                		log.warn("The classpath entry " + cpEntry + " is not a valid Java resource");
                	}
                }
            }
        }

        GroovyClassLoader loader = new GroovyClassLoader(parent, configuration);
        if (!forceLookupUnnamedFiles) {
            // in normal case we don't need to do script lookups
            loader.setResourceLoader(new GroovyResourceLoader() {
                public URL loadGroovySource(String filename) throws MalformedURLException {
                    return null;
                }
            });
        }
        return loader;
    }

    private Set<String> getScriptExtensions() {
        return scriptExtensions;
    }

    private void loadRegisteredScriptExtensions() {
        if (scriptExtensions.isEmpty()) {

            scriptExtensions.add(getScriptExtension().substring(2)); // first extension will be the one set explicitly on <groovyc>

            Path classpath = getClasspath() != null ? getClasspath() : new Path(getProject());
            final String[] pe = classpath.list();
            final GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader());
            for (String file : pe) {
                loader.addClasspath(file);
            }
            scriptExtensions.addAll(SourceExtensionHandler.getRegisteredExtensions(loader));
        }
    }
}
