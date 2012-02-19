package groovy.ui

class GroovyMainTest extends GroovyTestCase {
    private baos = new ByteArrayOutputStream()
    private ps = new PrintStream(baos)

    void testHelp() {
        String[] args = ['-h']
        GroovyMain.processArgs(args, ps)
        def out = baos.toString()
        assert out.contains('usage: groovy')
        ['-a', '-c', '-d', '-e', '-h', '-i', '-l', '-n', '-p', '-v'].each{
            assert out.contains(it)
        }
    }

    void testVersion() {
        String[] args = ['-v']
        GroovyMain.processArgs(args, ps)
        def out = baos.toString()
        assert out.contains('Groovy Version:')
        assert out.contains('JVM:')
    }

    void testNoArgs() {
        String[] args = []
        GroovyMain.processArgs(args, ps)
        def out = baos.toString()
        assert out.contains('error: neither -e or filename provided')
    }

    void testAttemptToRunJavaFile() {
        String[] args = ['abc.java']
        GroovyMain.processArgs(args, ps)
        def out = baos.toString()
        assert out.contains('error: error: cannot compile file with .java extension: abc.java')
    }

    /**
     * GROOVY-1512: Add support for begin() and end() methods when processing files by line with -a -ne
     */
    void testAandNEparametersWithBeginEndFunctions() {
        def originalErr = System.err
        System.setErr(ps)
        def tempFile = File.createTempFile("dummy", "txt")
        tempFile << "dummy text\n" * 10
        try {
            String[] args = ['-a', '-ne', 'def begin() { nb = 0 }; def end() { System.err.println nb }; nb++', tempFile.absolutePath]
            GroovyMain.main(args)
            def out = baos.toString()
            assert out.contains('10')
        } finally {
            tempFile.delete()
            System.setErr(originalErr)
        }
    }

    /**
     *
     * Running scriptfile while reusing args, which are defined in GroovyMain CliOptions.
     *
     * GROOVY-5191: Running script with '--encoding' param and some script parameters
     * (http://jira.codehaus.org/browse/GROOVY-5191)
     */
    void testArgsParsingForScriptFile() {
        //create a temporary file, which contains a groovy script, which should get executed
        def tempScriptFile = File.createTempFile("printArgs", "tmp")
        tempScriptFile << 'def file = new File(args[0]);file << args.toString()'

        //we catch the output of the tempScriptFile in an tempOutputFile, since we are interested of the
        //output of the tempScriptFile and not the Output of GroovyMain (as all other tests)
        def tempOutputFile = File.createTempFile("printArgsOut", "tmp")

        //The previous logic on how GroovyMain args get parsed failed, if one has to specify
        //args for the GroovyMain part and his own script part and both args share share the same args
        //abbreviation or start with the same letter.
        //e.g. GroovyMain allows for the parameter: '-p' defined in its CliOptions. If I now wanted to pass the
        //parameter '-param' to my Scriptfile, the paramter: '-param' would get parsed against the CliOptions so
        //that I would get '[-p, aram]' as parameters.
        //Therefore to test the new ArgsParsing for Scriptfiles Only!, we pass some args to GroovyMain, whilst
        //others should go to our scriptfile.

        def argsForGroovyCmd = ['--encoding=UTF-8', tempScriptFile.canonicalPath]
        def argsForTempFile = [tempOutputFile.canonicalPath, '-script', '-param']
        List<String> fullArgs = new ArrayList<String>()
        fullArgs.addAll(argsForGroovyCmd)
        fullArgs.addAll(argsForTempFile)
        String[] args = fullArgs.toArray(new String[fullArgs.size()])
        try {
            GroovyMain.main(args)
            assert tempOutputFile.text == argsForTempFile.toString()
        } finally {
            tempScriptFile.delete()
            tempOutputFile.delete()
        }
    }
}