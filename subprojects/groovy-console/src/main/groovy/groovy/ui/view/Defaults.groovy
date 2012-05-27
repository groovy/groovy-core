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

import groovy.ui.text.GroovyFilter
import java.awt.Color
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

menuBarClass     = groovy.ui.view.BasicMenuBar
contentPaneClass = groovy.ui.view.BasicContentPane
toolBarClass     = groovy.ui.view.BasicToolBar
statusBarClass   = groovy.ui.view.BasicStatusBar

styles = [
    // output window styles
    regular: [
            (StyleConstants.FontFamily): 'Monospaced'
        ],
    prompt: [
            (StyleConstants.Foreground): new Color(0, 128, 0)
        ],
    command: [
            (StyleConstants.Foreground): Color.BLUE
        ],
    stacktrace: [
            (StyleConstants.Foreground): Color.RED.darker()
        ],
    hyperlink: [
            (StyleConstants.Foreground): Color.BLUE,
            (StyleConstants.Underline): true
        ],
    output: [:],
    result: [
            (StyleConstants.Foreground): Color.BLUE,
            (StyleConstants.Background): Color.YELLOW
        ],

    // syntax highlighting styles
    (StyleContext.DEFAULT_STYLE) : [
            (StyleConstants.FontFamily): 'Monospaced'
        ],
    (GroovyFilter.COMMENT): [
            (StyleConstants.Foreground): Color.LIGHT_GRAY.darker().darker(),
            (StyleConstants.Italic) : true
        ],
    (GroovyFilter.QUOTES): [
            (StyleConstants.Foreground): Color.MAGENTA.darker().darker()
        ],
    (GroovyFilter.SINGLE_QUOTES): [
            (StyleConstants.Foreground): Color.GREEN.darker().darker()
        ],
    (GroovyFilter.SLASHY_QUOTES): [
            (StyleConstants.Foreground): Color.ORANGE.darker()
        ],
    (GroovyFilter.DIGIT): [
            (StyleConstants.Foreground): Color.RED.darker()
        ],
    (GroovyFilter.OPERATION): [
            (StyleConstants.Bold): true
        ],
    (GroovyFilter.IDENT): [:],
    (GroovyFilter.RESERVED_WORD): [
        (StyleConstants.Bold): true,
        (StyleConstants.Foreground): Color.BLUE.darker().darker()
    ]
]
