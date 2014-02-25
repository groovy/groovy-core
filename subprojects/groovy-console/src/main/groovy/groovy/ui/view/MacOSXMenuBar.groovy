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
package groovy.ui.view

def handler = false
if (!handler) {
    try {
        handler = build("""
package groovy.ui

import com.apple.mrj.*

class ConsoleMacOsSupport implements MRJQuitHandler, MRJAboutHandler {

    def quitHandler
    def aboutHandler

    public void handleAbout() {
        aboutHandler()
    }

    public void handleQuit() {
        quitHandler()
    }

}

def handler = new ConsoleMacOsSupport(quitHandler:controller.&exit, aboutHandler:controller.&showAbout)
MRJApplicationUtils.registerAboutHandler(handler)
MRJApplicationUtils.registerQuitHandler(handler)

return handler
""", new GroovyClassLoader(this.class.classLoader))
    } catch (Exception se) {
        // usually an AccessControlException, sometimes applets and JNLP won't let
        // you access MRJ classes.
        // However, in any exceptional case back out and use the BasicMenuBar
        se.printStackTrace()
        build(BasicMenuBar)
        return
    }
}

menuBar {
    menu(text: 'File', mnemonic: 'F') {
        menuItem(newFileAction, icon:null)
        menuItem(newWindowAction, icon:null)
        menuItem(openAction, icon:null)
        separator()
        menuItem(saveAction, icon:null)
        menuItem(saveAsAction, icon:null)
        separator()
        menuItem(printAction, icon:null)
    }

    menu(text: 'Edit', mnemonic: 'E') {
        menuItem(undoAction, icon:null)
        menuItem(redoAction, icon:null)
        separator()
        menuItem(cutAction, icon:null)
        menuItem(copyAction, icon:null)
        menuItem(pasteAction, icon:null)
        separator()
        menuItem(findAction, icon:null)
        menuItem(findNextAction, icon:null)
        menuItem(findPreviousAction, icon:null)
        menuItem(replaceAction, icon:null)
        separator()
        menuItem(selectAllAction, icon:null)
	separator()
	menuItem(commentAction, icon:null)
    }

    menu(text: 'View', mnemonic: 'V') {
        menuItem(clearOutputAction, icon:null)
        separator()
        menuItem(largerFontAction, icon:null)
        menuItem(smallerFontAction, icon:null)
        separator()
        checkBoxMenuItem(captureStdOutAction, selected: controller.captureStdOut)
        checkBoxMenuItem(captureStdErrAction, selected: controller.captureStdErr)
        checkBoxMenuItem(fullStackTracesAction, selected: controller.fullStackTraces)
        checkBoxMenuItem(showScriptInOutputAction, selected: controller.showScriptInOutput)
        checkBoxMenuItem(visualizeScriptResultsAction, selected: controller.visualizeScriptResults)
        checkBoxMenuItem(showToolbarAction, selected: controller.showToolbar)
        checkBoxMenuItem(detachedOutputAction, selected: controller.detachedOutput)
        checkBoxMenuItem(autoClearOutputAction, selected: controller.autoClearOutput)
    }

    menu(text: 'History', mnemonic: 'I') {
        menuItem(historyPrevAction, icon:null)
        menuItem(historyNextAction, icon:null)
    }

    menu(text: 'Script', mnemonic: 'S') {
        menuItem(runAction, icon:null)
        checkBoxMenuItem(saveOnRunAction, selected: controller.saveOnRun)
        menuItem(runSelectionAction, icon:null)
        checkBoxMenuItem(threadInterruptAction, selected: controller.threadInterrupt)
        menuItem(interruptAction, icon:null)
        menuItem(compileAction, icon:null)
        separator()
        menuItem(addClasspathJar)
        menuItem(addClasspathDir)
        menuItem(clearClassloader)
        separator()
        menuItem(inspectLastAction, icon:null)
        menuItem(inspectVariablesAction, icon:null)
        menuItem(inspectAstAction, icon:null)
    }
}

