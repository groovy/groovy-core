/*
 * Copyright 2003-2007 the original author or authors.
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

package org.codehaus.groovy.tools.shell.commands

import org.codehaus.groovy.control.CompilationFailedException

import org.codehaus.groovy.tools.shell.CommandSupport
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.Shell
import org.codehaus.groovy.tools.shell.util.Logger
import org.codehaus.groovy.tools.shell.util.PackageHelper
import org.codehaus.groovy.tools.shell.util.SimpleCompletor

import java.util.regex.Pattern

/**
 * The 'import' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class ImportCommand
    extends CommandSupport
{
    ImportCommand(final Groovysh shell) {
        super(shell, 'import', '\\i')
    }

    protected List createCompletors() {
        return [new ImportCompletor(shell.packageHelper),
                new SimpleCompletor('as'),
                null
        ]
    }
    
    Object execute(final List args) {
        assert args != null

        if (args.isEmpty()) {
            fail("Command 'import' requires one or more arguments") // TODO: i18n
        }

        def buff = [ 'import ' + args.join(' ') ]
        buff << 'def dummp = false'
        
        def type
        try {
            type = classLoader.parseClass(buff.join(NEWLINE))
            
            // No need to keep duplicates, but order may be important so remove the previous def, since
            // the last defined import will win anyways
            
            if (imports.remove(buff[0])) {
                log.debug("Removed duplicate import from list")
            }
            
            log.debug("Adding import: ${buff[0]}")
            
            imports << buff[0]
        }
        catch (CompilationFailedException e) {
            def msg = "Invalid import definition: '${buff[0]}'; reason: $e.message" // TODO: i18n
            log.debug(msg, e)
            fail(msg)
        }
        finally {
            // Remove the class generated while testing the import syntax
            classLoader.classCache.remove(type?.name)
        }
    }
}

class ImportCompletor implements jline.Completor {

    PackageHelper packageHelper
    protected final Logger log = Logger.create(ImportCompletor.class)
    public final static Pattern PACKNAME_PATTERN = java.util.regex.Pattern.compile("^([a-z]+(\\.[a-z]*)*(\\.[A-Z][^.\$_]*)?)?\$")


    public ImportCompletor(PackageHelper packageHelper) {
        this.packageHelper = packageHelper
    }

    @Override
    int complete(String buffer, int cursor, List result) {
        String current = buffer ? buffer.substring(0, cursor) : ""
        if (! PACKNAME_PATTERN.matcher(current).find() || current.contains("..")) {
            return -1
        }
        log.debug(buffer)
        if (current.endsWith('.')) {
            result.add('* ')
            result.addAll(packageHelper.getContents(current[0..-2]).collect { String it ->filterMatches(it) })
            return current.length()
        }
        String prefix
        int lastDot = current.lastIndexOf('.')
        if (lastDot == -1 || current == '') {
            prefix = current
        } else {
            prefix = current.substring(lastDot + 1)
        }

        Set candidates = packageHelper.getContents(current.substring(0, Math.max(lastDot, 0)))
        if (candidates == null || candidates.size() == 0) {
            return -1
        }

        log.debug(prefix)
        Collection<String> matches = candidates.findAll { String it -> it.startsWith(prefix) }
        if (matches) {
            result.addAll(matches.collect { String it -> filterMatches(it) })
            return lastDot <= 0 ? 0 : lastDot + 1
        }
        return -1
    }

    def filterMatches(String it) {
        if (it[0] in 'A' .. 'Z') {
           return it + ' '
        }
        return it + '.'
    }
}
