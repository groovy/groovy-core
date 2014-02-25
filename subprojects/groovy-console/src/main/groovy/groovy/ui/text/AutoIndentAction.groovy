/*
 * Copyright 2008 the original author or authors.
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

package groovy.ui.text

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet

class AutoIndentAction extends AbstractAction {
    AttributeSet simpleAttributeSet = new SimpleAttributeSet()

    public void actionPerformed(ActionEvent evt) {
        def inputArea = evt.source
        def rootElement = inputArea.document.defaultRootElement
        def cursorPos = inputArea.getCaretPosition()
        int rowNum = rootElement.getElementIndex(cursorPos)
        def rowElement = rootElement.getElement(rowNum)
        int startOffset = rowElement.getStartOffset()
        int endOffset = rowElement.getEndOffset()
        String rowContent = inputArea.document.getText(startOffset, endOffset - startOffset);
        String contentBeforeCursor = inputArea.document.getText(startOffset, cursorPos - startOffset);
        String whitespaceStr = ''
        def matcher = (rowContent =~ /(?m)^(\s*).*\n$/)
        matcher.each { all, ws ->
            whitespaceStr = ws
        }

        if (contentBeforeCursor ==~ /(\s)*/) {
            whitespaceStr = contentBeforeCursor
        }

        inputArea.document.insertString(cursorPos, '\n' + whitespaceStr, simpleAttributeSet)
    }
}