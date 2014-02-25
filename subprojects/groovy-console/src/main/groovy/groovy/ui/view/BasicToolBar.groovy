/*
 * Copyright 2003-2010 the original author or authors.
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

import javax.swing.SwingConstants
import java.awt.BorderLayout

toolbar = toolBar(rollover:true, visible:controller.showToolbar, constraints:BorderLayout.NORTH) {
    button(newFileAction, text:null)
    button(openAction, text:null)
    button(saveAction, text:null)
    separator(orientation:SwingConstants.VERTICAL)
    button(undoAction, text:null)
    button(redoAction, text:null)
    separator(orientation:SwingConstants.VERTICAL)
    button(cutAction, text:null)
    button(copyAction, text:null)
    button(pasteAction, text:null)
    separator(orientation:SwingConstants.VERTICAL)
    button(findAction, text:null)
    button(replaceAction, text:null)
    separator(orientation:SwingConstants.VERTICAL)
    button(historyPrevAction, text:null)
    button(historyNextAction, text:null)
    separator(orientation:SwingConstants.VERTICAL)
    button(runAction, text:null)
    button(interruptAction, text:null)
}
