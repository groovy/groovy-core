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

import groovy.ui.ConsoleTextEditor
import groovy.ui.Console
import java.awt.*
import java.awt.image.BufferedImage
import static javax.swing.JSplitPane.VERTICAL_SPLIT
import javax.swing.text.Style
import javax.swing.text.StyleContext
import javax.swing.text.StyledDocument
import java.util.prefs.Preferences
import javax.swing.text.StyleConstants
import javax.swing.WindowConstants
import javax.swing.JSplitPane

def prefs = Preferences.userNodeForPackage(Console)
def detachedOutputFlag = prefs.getBoolean('detachedOutput', false)
outputWindow = frame(visible:false, defaultCloseOperation: WindowConstants.HIDE_ON_CLOSE) {
    blank = glue()
    blank.preferredSize = [0, 0] as Dimension
}
splitPane = splitPane(resizeWeight: 0.5, orientation: VERTICAL_SPLIT) {
    inputEditor = widget(new ConsoleTextEditor(), border:emptyBorder(0))
    buildOutputArea(prefs)
}

private def buildOutputArea(prefs) {
    scrollArea = scrollPane(border: emptyBorder(0)) {
        outputArea = textPane(
                editable: false,
                name: 'outputArea',
                contentType: 'text/html',
                background: new Color(255, 255, 218),
                font: new Font('Monospaced', Font.PLAIN, prefs.getInt('fontSize', 12)),
                border: emptyBorder(4)
        )
    }
}


inputArea = inputEditor.textEditor
// attach ctrl-enter to input area
// need to wrap in actions to keep it from being added as a component
actions {
    container(inputArea, name: 'inputArea', font:new Font('Monospaced', Font.PLAIN, prefs.getInt('fontSize', 12)), border:emptyBorder(4)) {
        action(runAction)
        action(runSelectionAction)
        action(showOutputWindowAction)
    }
    container(outputArea, name: 'outputArea') {
        action(hideOutputWindowAction1)
        action(hideOutputWindowAction2)
        action(hideOutputWindowAction3)
        action(hideOutputWindowAction4)
    }
}

// add styles to the output area, should this be moved into SwingBuilder somehow?
outputArea.font = new Font('Monospaced', outputArea.font.style, outputArea.font.size)
StyledDocument doc = outputArea.styledDocument

Style defStyle = StyleContext.defaultStyleContext.getStyle(StyleContext.DEFAULT_STYLE)

def applyStyle = {Style style, values -> values.each{k, v -> style.addAttribute(k, v)}}

Style regular = doc.addStyle('regular', defStyle)
applyStyle(regular, styles.regular)

promptStyle = doc.addStyle('prompt', regular)
applyStyle(promptStyle, styles.prompt)

commandStyle = doc.addStyle('command', regular)
applyStyle(commandStyle, styles.command)

outputStyle = doc.addStyle('output', regular)
applyStyle(outputStyle, styles.output)

resultStyle = doc.addStyle('result', regular)
applyStyle(resultStyle, styles.result)

stacktraceStyle = doc.addStyle('stacktrace', regular)
applyStyle(stacktraceStyle, styles.stacktrace)

hyperlinkStyle = doc.addStyle('hyperlink', regular)
applyStyle(hyperlinkStyle, styles.hyperlink)

// redo styles for editor
doc = inputArea.styledDocument
StyleContext styleContext = StyleContext.defaultStyleContext
styles.each {styleName, defs ->
    Style style = styleContext.getStyle(styleName)
    if (style) {
        applyStyle(style, defs)
        String family = defs[StyleConstants.FontFamily]
        if (style.name == 'default' && family) {
            inputEditor.defaultFamily = family
            inputArea.font = new Font(family, Font.PLAIN, inputArea.font.size)
        }
    }
}

// set the preferred size of the input and output areas
// this is a good enough solution, there are margins and scrollbars and such to worry about for 80x12x2
Graphics g = GraphicsEnvironment.localGraphicsEnvironment.createGraphics (new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB))
FontMetrics fm = g.getFontMetrics(outputArea.font)

outputArea.preferredSize = [
    prefs.getInt('outputAreaWidth', fm.charWidth(0x77) * 81),
    prefs.getInt('outputAreaHeight', (fm.getHeight() + fm.leading) * 12)
] as Dimension

inputEditor.preferredSize = [
    prefs.getInt('inputAreaWidth', fm.charWidth(0x77) * 81),
    prefs.getInt('inputAreaHeight', (fm.getHeight() + fm.leading) * 12)
] as Dimension

origDividerSize = -1
if (detachedOutputFlag) {
    splitPane.add(blank, JSplitPane.BOTTOM)
    origDividerSize = splitPane.dividerSize
    splitPane.dividerSize = 0
    splitPane.resizeWeight = 1.0
    outputWindow.add(scrollArea, BorderLayout.CENTER)
}
