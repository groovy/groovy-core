/**
 * Test JCommanderScript's multiple command feature in a simple Script.
 * More tests are embedded in JCommanderScriptTest strings.
 *
 * @author: Jim White
 */

import com.beust.jcommander.*
import groovy.cli.*
import groovy.transform.BaseScript
import groovy.transform.Field

@BaseScript JCommanderScript thisScript

// Override the default of using the 'args' binding for our test so we can be run without a special driver.
String[] getScriptArguments() {
    [ "add", "-i", "zoos"] as String[]
}

@Parameter(names = ["-log", "-verbose" ], description = "Level of verbosity")
@Field Integer verbose = 1;

@Parameters(commandNames = "commit", commandDescription = "Record changes to the repository")
class CommandCommit implements Runnable {
    @Parameter(description = "The list of files to commit")
    private List<String> files;

    @Parameter(names = "--amend", description = "Amend")
    private Boolean amend = false;

    @Parameter(names = "--author")
    private String author;

    @Override
    void run() {
        println "$author committed $files ${amend ? "using" : "not using"} amend."
    }
}

@Parameters(commandNames = "add", separators = "=", commandDescription = "Add file contents to the index")
public class CommandAdd {
    @Parameter(description = "File patterns to add to the index")
    List<String> patterns;

    @Parameter(names = "-i")
    Boolean interactive = false;
}

@Subcommand @Field CommandCommit commitCommand = new CommandCommit()
@Subcommand @Field CommandAdd addCommand = new CommandAdd()

println verbose
println scriptJCommander.parsedCommand

switch (scriptJCommander.parsedCommand) {
    case "add" :
        if (addCommand.interactive) {
            println "Adding ${addCommand.patterns} interactively."
        } else {
            println "Adding ${addCommand.patterns} in batch mode."
        }
}

assert scriptJCommander.parsedCommand == "add"
assert addCommand.interactive
assert addCommand.patterns == ["zoos"]

[33]
