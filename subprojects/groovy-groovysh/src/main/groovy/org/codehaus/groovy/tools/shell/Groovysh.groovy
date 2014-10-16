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

package org.codehaus.groovy.tools.shell

import antlr.TokenStreamException
import groovy.transform.CompileStatic
import jline.Terminal
import jline.WindowsTerminal
import jline.console.history.FileHistory
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.StackTraceUtils
import org.codehaus.groovy.tools.shell.commands.LoadCommand
import org.codehaus.groovy.tools.shell.commands.RecordCommand
import org.codehaus.groovy.tools.shell.util.*
import org.codehaus.groovy.tools.shell.util.PackageHelper
import org.codehaus.groovy.tools.shell.util.ScriptVariableAnalyzer
import org.fusesource.jansi.AnsiRenderer

import java.util.regex.Pattern

/**
 * An interactive shell for evaluating Groovy code from the command-line (aka. groovysh).
 *
 * The set of available commands can be modified by placing a file in the classpath named
 * <code>org/codehaus/groovy/tools/shell/commands.xml</code>
 *
 * See {@link XmlCommandRegistrar}
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class Groovysh extends Shell {


    private static final MessageSource messages = new MessageSource(Groovysh)

    private static final Pattern TYPEDEF_PATTERN = ~'^\\s*((?:public|protected|private|static|abstract|final)\\s+)*(?:class|enum|interface).*'
    private static final Pattern METHODDEF_PATTERN = ~'^\\s*((?:public|protected|private|static|abstract|final|synchronized)\\s+)*[a-zA-Z_.]+[a-zA-Z_.<>]+\\s+[a-zA-Z_]+\\(.*'

    public static final String COLLECTED_BOUND_VARS_MAP_VARNAME = 'groovysh_collected_boundvars'

    public static final String INTERPRETER_MODE_PREFERENCE_KEY = 'interpreterMode'
    public static final String AUTOINDENT_PREFERENCE_KEY = 'autoindent'
    public static final String COLORS_PREFERENCE_KEY = 'colors'
    // after how many prefix characters we start displaying all metaclass methods
    public static final String METACLASS_COMPLETION_PREFIX_LENGTH_PREFERENCE_KEY = 'meta-completion-prefix-length'


    final BufferManager buffers = new BufferManager()

    final Parser parser

    final Interpreter interp

    final List<String> imports = []

    int indentSize = 2

    InteractiveShellRunner runner

    FileHistory history

    boolean historyFull  // used as a workaround for GROOVY-2177

    String evictedLine  // remembers the command which will get evicted if history is full

    PackageHelper packageHelper

    Groovysh(final ClassLoader classLoader, final Binding binding, final IO io, final Closure registrar) {
        super(io)

        assert classLoader
        assert binding
        assert registrar

        parser = new Parser()

        interp = new Interpreter(classLoader, binding)

        registrar.call(this)

        this.packageHelper = new PackageHelperImpl(classLoader)
    }

    private static Closure createDefaultRegistrar(final ClassLoader classLoader) {

        return {Groovysh shell ->
            URL xmlCommandResource = getClass().getResource('commands.xml')
            if (xmlCommandResource != null) {
                def r = new XmlCommandRegistrar(shell, classLoader)
                r.register(xmlCommandResource)
            } else {
                new DefaultCommandsRegistrar(shell).register()
            }
        }
    }

    Groovysh(final ClassLoader classLoader, final Binding binding, final IO io) {
        this(classLoader, binding, io, createDefaultRegistrar(classLoader))
    }

    Groovysh(final Binding binding, final IO io) {
        this(Thread.currentThread().contextClassLoader, binding, io)
    }

    Groovysh(final IO io) {
        this(new Binding(), io)
    }

    Groovysh() {
        this(new IO())
    }

    //
    // Execution
    //

    /**
     * Execute a single line, where the line may be a command or Groovy code (complete or incomplete).
     */
    @Override
    Object execute(final String line) {
        assert line != null

        // Ignore empty lines
        if (line.trim().size() == 0) {
            return null
        }

        maybeRecordInput(line)

        Object result

        // First try normal command execution
        if (isExecutable(line)) {
            result = executeCommand(line)

            // For commands, only set the last result when its non-null/true
            if (result) {
                setLastResult(result)
            }

            return result
        }

        // Otherwise treat the line as Groovy
        List<String> current = new ArrayList<String>(buffers.current())

        // Append the line to the current buffer
        current << line

        String importsSpec = this.getImportStatements()

        // Attempt to parse the current buffer
        def status = parser.parse([importsSpec] + current)

        switch (status.code) {
            case ParseCode.COMPLETE:
                log.debug('Evaluating buffer...')

                if (io.verbose) {
                    displayBuffer(current)
                }

                if (!Boolean.valueOf(Preferences.get(INTERPRETER_MODE_PREFERENCE_KEY, 'false')) || isTypeOrMethodDeclaration(current)) {
                    // Evaluate the current buffer w/imports and dummy statement
                    List buff = [importsSpec] + [ 'true' ] + current
                    setLastResult(result = interp.evaluate(buff))
                } else {
                    // Evaluate Buffer wrapped with code storing bounded vars
                    result = evaluateWithStoredBoundVars(current)
                }

                buffers.clearSelected()
                break

            case ParseCode.INCOMPLETE:
                // Save the current buffer so user can build up complex multi-line code blocks
                buffers.updateSelected(current)
                break

            case ParseCode.ERROR:
                throw status.cause

            default:
                // Should never happen
                throw new Error("Invalid parse status: $status.code")
        }

        return result
    }

    /**
     * return true if the buffer can be recognized as a type declaration statement
     * @param strings
     * @return
     */
    @CompileStatic
    static boolean isTypeOrMethodDeclaration(final List<String> buffer) {
        final String joined = buffer.join('')
        return joined.matches(TYPEDEF_PATTERN) || joined.matches(METHODDEF_PATTERN)
    }
/*
     * to simulate an interpreter mode, this method wraps the statements into a try/finally block that
     * stores bound variables like unbound variables
     */
    private Object evaluateWithStoredBoundVars(final List<String> current) {
        Object result
        String variableBlocks = ''
        // To make groovysh behave more like an interpreter, we need to retrive all bound
        // vars at the end of script execution, and then update them into the groovysh Binding context.
        Set<String> boundVars = ScriptVariableAnalyzer.getBoundVars(current.join(Parser.NEWLINE))
        variableBlocks += "$COLLECTED_BOUND_VARS_MAP_VARNAME = new HashMap();"
        if (boundVars) {
            boundVars.each({ String varname ->
                // bound vars can be in global or some local scope.
                // We discard locally scoped vars by ignoring MissingPropertyException
                variableBlocks += """
try {$COLLECTED_BOUND_VARS_MAP_VARNAME[\"$varname\"] = $varname;
} catch (MissingPropertyException e){}"""
            })
        }

        // Evaluate the current buffer w/imports and dummy statement
        List<String> buff = imports + ['try {', 'true'] + current + ['} finally {' + variableBlocks + '}']

        setLastResult(result = interp.evaluate(buff))

        Map<String, Object> boundVarValues = interp.context.getVariable(COLLECTED_BOUND_VARS_MAP_VARNAME)
        boundVarValues.each({ String name, Object value -> interp.context.setVariable(name, value) })
        return result
    }



    protected Object executeCommand(final String line) {
        return super.execute(line)
    }

    /**
     * Display the given buffer.
     */
    void displayBuffer(final List buffer) {
        assert buffer

        buffer.eachWithIndex { line, index ->
            def lineNum = formatLineNumber(index)

            io.out.println(" ${lineNum}@|bold >|@ $line")
        }
    }

    String getImportStatements() {
        return this.imports.collect({String it -> "import $it;"}).join('')
    }

    //
    // Prompt
    //

    private final AnsiRenderer prompt = new AnsiRenderer()

    /*
        Builds the command prompt name in 1 of 3 ways:
           1.  Checks the groovysh.prompt property passed into groovysh script.   -Dgroovysh.prompt="hello"
           2.  Checks an environment variable called GROOVYSH_PROMPT.             export GROOVYSH_PROMPT
           3.  If no value is defined returns the default groovy shell prompt.

        The code will always assume you want the line number in the prompt.  To implement differently overhead the render
        prompt variable.
     */
    private String buildPrompt() {
        def lineNum = formatLineNumber(buffers.current().size())

        def groovyshellProperty = System.getProperty('groovysh.prompt')
        if (groovyshellProperty) {
            return "@|bold ${groovyshellProperty}:|@${lineNum}@|bold >|@ "
        }
        def groovyshellEnv = System.getenv('GROOVYSH_PROMPT')
        if (groovyshellEnv) {
            return  "@|bold ${groovyshellEnv}:|@${lineNum}@|bold >|@ "
        }
        return "@|bold groovy:|@${lineNum}@|bold >|@ "

    }

    /**
     * Calculate probably desired indentation based on parenthesis balance and last char,
     * as well as what the user used last as indentation.
     * @return a string to indent the next line in the buffer
     */
    String getIndentPrefix() {
        List<String> buffer = this.buffers.current()
        if (buffer.size() < 1) {
            return ''
        }
        StringBuilder src = new StringBuilder()
        for (String line: buffer) {
            src.append(line + '\n')
        }

        // not sure whether the same Lexer instance could be reused.
        def lexer = CurlyCountingGroovyLexer.createGroovyLexer(src.toString())

        // read all tokens
        try {
            while (lexer.nextToken().getType() != CurlyCountingGroovyLexer.EOF) {}
        } catch (TokenStreamException e) {
            // pass
        }
        int parenIndent = (lexer.getParenLevel()) * indentSize

        // dedent after closing brackets
        return ' ' * Math.max(parenIndent, 0)
    }

    public String renderPrompt() {
        return prompt.render( buildPrompt() )
    }

    /**
     * Format the given number suitable for rendering as a line number column.
     */
    private String formatLineNumber(final int num) {
        assert num >= 0

        // Make a %03d-like string for the line number
        return num.toString().padLeft(3, '0')
    }

    //
    // User Profile Scripts
    //

    File getUserStateDirectory() {
        def userHome = new File(System.getProperty('user.home'))
        def dir = new File(userHome, '.groovy')
        return dir.canonicalFile
    }

    /**
     * Loads file from within user groovy state directory
     * @param filename
     */
    protected void loadUserScript(final String filename) {
        assert filename

        File file = new File(getUserStateDirectory(), filename)

        if (file.exists()) {
            Command command = registry[LoadCommand.COMMAND_NAME] as Command

            if (command) {
                log.debug("Loading user-script: $file")

                // Disable the result hook for profile scripts
                def previousHook = resultHook
                resultHook = { result -> /* nothing */}

                try {
                    command.load(file.toURI().toURL())
                }
                finally {
                    // Restore the result hook
                    resultHook = previousHook
                }
            } else {
                log.error("Unable to load user-script, missing '$LoadCommand.COMMAND_NAME' command")
            }
        }
    }

    //
    // Recording
    //

    private void maybeRecordInput(final String line) {
        RecordCommand record = registry[RecordCommand.COMMAND_NAME]

        if (record != null) {
            record.recordInput(line)
        }
    }

    private void maybeRecordResult(final Object result) {
        RecordCommand record = registry[RecordCommand.COMMAND_NAME]

        if (record != null) {
            record.recordResult(result)
        }
    }

    private void maybeRecordError(Throwable cause) {
        RecordCommand record = registry[RecordCommand.COMMAND_NAME]

        if (record != null) {
            if (Preferences.sanitizeStackTrace) {
                cause = StackTraceUtils.deepSanitize(cause)
            }
            record.recordError(cause)
        }
    }

    //
    // Hooks
    //

    final Closure defaultResultHook = {Object result ->
        boolean showLastResult = !io.quiet && (io.verbose || Preferences.showLastResult)
        if (showLastResult) {
            // avoid String.valueOf here because it bypasses pretty-printing of Collections,
            // e.g. String.valueOf( ['a': 42] ) != ['a': 42].toString()
            io.out.println("@|bold ===>|@ ${InvokerHelper.toString(result)}")
        }
    }

    Closure resultHook = defaultResultHook

    private void setLastResult(final Object result) {
        if (resultHook == null) {
            throw new IllegalStateException('Result hook is not set')
        }

        resultHook.call((Object)result)

        interp.context['_'] = result

        maybeRecordResult(result)
    }

    final Closure defaultErrorHook = { Throwable cause ->
        assert cause != null

        io.err.println("@|bold,red ERROR|@ ${cause.getClass().name}:")
        io.err.println("@|bold,red ${cause.message}|@")

        maybeRecordError(cause)

        if (log.debug) {
            // If we have debug enabled then skip the fancy bits below
            log.debug(cause)
        }
        else {
            boolean sanitize = Preferences.sanitizeStackTrace

            // Sanitize the stack trace unless we are in verbose mode, or the user has request otherwise
            if (!io.verbose && sanitize) {
                cause = StackTraceUtils.deepSanitize(cause)
            }

            def trace = cause.stackTrace

            def buff = new StringBuffer()

            boolean doBreak = false

            for (e in trace) {
                // Stop the trace once we find the root of the evaluated script
                if (e.className == Interpreter.SCRIPT_FILENAME && e.methodName == 'run') {
                    if (io.verbosity != IO.Verbosity.DEBUG && io.verbosity != IO.Verbosity.VERBOSE) {
                        break
                    }
                    doBreak = true
                }

                buff << "        @|bold at|@ ${e.className}.${e.methodName} (@|bold "

                buff << (e.nativeMethod ? 'Native Method' :
                            (e.fileName != null && e.lineNumber != -1 ? "${e.fileName}:${e.lineNumber}" :
                                (e.fileName != null ? e.fileName : 'Unknown Source')))

                buff << '|@)'

                io.err.println(buff)

                buff.setLength(0) // Reset the buffer
                if (doBreak) {
                    io.err.println('        @|bold ...|@')
                    break
                }
            }
        }
    }

    Closure errorHook = defaultErrorHook

    private void displayError(final Throwable cause) {
        if (errorHook == null) {
            throw new IllegalStateException('Error hook is not set')
        }
        if (cause instanceof MissingPropertyException) {
            if (cause.type && cause.type.canonicalName == Interpreter.SCRIPT_FILENAME) {
                io.err.println("@|bold,red Unknown property|@: " + cause.property)
                return
            }
        }

        errorHook.call(cause)
    }

    /**
    * Run Interactive Shell with optional initial script and files to load
    */
    int run(final String evalString, final List<String> filenames) {
        List<String> startCommands = []

        if (evalString != null && evalString.trim().size() > 0) {
            startCommands.add(evalString)
        }
        if (filenames != null && filenames.size() > 0) {
            startCommands.addAll(filenames.collect({String it -> "${LoadCommand.COMMAND_NAME} $it"}))
        }
        return run(startCommands.join('\n'))
    }

    /**
     * Run Interactive Shell with initial command
     */
    int run(final String commandLine) {
        def code

        try {
            loadUserScript('groovysh.profile')
            loadUserScript('groovysh.rc')

            // Setup the interactive runner
            runner = new InteractiveShellRunner(
                    this,
                    this.&renderPrompt as Closure)

            // if args were passed in, just execute as a command
            // (but cygwin gives an empty string, so ignore that)
            if (commandLine != null && commandLine.trim().size() > 0) {
                runner.wrappedInputStream.insert(commandLine + '\n')
            }

            // Setup the history
            File histFile = new File(userStateDirectory, 'groovysh.history')
            history = new FileHistory(histFile)
            runner.setHistory(history)

            // Setup the error handler
            runner.errorHandler = this.&displayError

            displayWelcomeBanner(runner)

            // And let 'er rip... :-)
            runner.run()


            code = 0
        } catch (ExitNotification n) {
            log.debug("Exiting w/code: ${n.code}")

            code = n.code
        }
        catch (Throwable t) {
            io.err.println(messages.format('info.fatal', t))
            t.printStackTrace(io.err)

            code = 1
        }

        assert code != null // This should never happen

        return code
    }


    /**
     * maybe displays log information and a welcome message
     * @param term
     */
    public void displayWelcomeBanner(InteractiveShellRunner runner) {
        if (!log.debug && io.quiet) {
            // nothing to do here
            return
        }
        Terminal term = runner.reader.terminal
        if (log.debug) {
            log.debug("Terminal ($term)")
            log.debug("    Supported:  $term.supported")
            log.debug("    ECHO:       (enabled: $term.echoEnabled)")
            log.debug("    H x W:      ${term.getHeight()} x ${term.getWidth()}")
            log.debug("    ANSI:       ${term.isAnsiSupported()}")

            if (term instanceof WindowsTerminal) {
                WindowsTerminal winterm = (WindowsTerminal) term
                log.debug("    Direct:     ${winterm.directConsole}")
            }
        }

        // Display the welcome banner
        if (!io.quiet) {
            int width = term.getWidth()

            // If we can't tell, or have something bogus then use a reasonable default
            if (width < 1) {
                width = 80
            }

            io.out.println(messages.format('startup_banner.0', GroovySystem.version, System.properties['java.version']))
            io.out.println(messages['startup_banner.1'])
            io.out.println('-' * (width - 1))
        }
    }
}
