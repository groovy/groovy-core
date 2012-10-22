package groovy.swing
/*
 * Copyright 2003-2012 the original author or authors.
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

import groovy.ui.Console
import java.awt.Color

class SwingBuilderConsoleTest extends GroovySwingTestCase {
    void testTabbedPane() {
        testInEDT {

            def swing = new SwingBuilder()
            swing.tabbedPane(id: 'tp') {
                panel(id: 'p1', name: 'Title 1')
                panel(id: 'p2')
                panel(id: 'p3', title: 'Title 3')
                panel(id: 'p4', title: 'Title 4', name: 'Name 4')
                panel(id: 'p5', title: 'Title 5', tabIcon: imageIcon(Console.ICON_PATH, id: 'i5'))
                panel(id: 'p6', title: 'Title 6', tabDisabledIcon: imageIcon(Console.ICON_PATH, id: 'i6'))
                panel(id: 'p7', title: 'Title 7', tabToolTip: 'tip 7')
                panel(id: 'p8', title: 'Title 8', tabBackground: Color.GREEN)
                panel(id: 'p9', title: 'Title 9', tabForeground: Color.GREEN)
                panel(id: 'pA', title: 'Title A', tabEnabled: false)
                panel(id: 'pB', title: 'Title B', tabMnemonic: 'T')
                panel(id: 'pC', title: 'Title C', tabDisplayedMnemonicIndex: 2)
                panel(id: 'pD', title: 'Title D', tabMnemonic: "${'T'}")
            }

            assert swing.tp.tabCount == 13
            assert swing.tp.indexOfComponent(swing.p1) == 0
            assert swing.tp.indexOfComponent(swing.p2) == 1
            assert swing.tp.indexOfComponent(swing.p3) == 2
            assert swing.tp.indexOfComponent(swing.p4) == 3
            assert swing.tp.indexOfTab('Title 1') == 0
            assert swing.tp.indexOfTab('Title 3') == 2
            assert swing.tp.indexOfTab('Title 4') == 3
            assert swing.tp.getIconAt(4) == swing.i5
            assert swing.tp.getDisabledIconAt(5) == swing.i6
            assert swing.tp.getToolTipTextAt(6) == 'tip 7'
            assert swing.tp.getBackgroundAt(7) == Color.GREEN
            assert swing.tp.getForegroundAt(8) == Color.GREEN
            assert swing.tp.isEnabledAt(9) == false
            assert swing.tp.getMnemonicAt(10) == 0x54
            assert swing.tp.getDisplayedMnemonicIndexAt(11) == 2
            assert swing.tp.getMnemonicAt(12) == 0x54

            swing.tabbedPane(id: 'tp', selectedComponent: swing.p2) {
                panel(p1, name: 'Title 1')
                panel(p2)
                panel(p3)
            }
            assert swing.tp.selectedIndex == 1
            assert swing.tp.selectedComponent == swing.p2

            swing.tabbedPane(id: 'tp', selectedIndex: 1) {
                panel(p1, name: 'Title 1')
                panel(p2)
                panel(p3)
            }
            assert swing.tp.selectedIndex == 1
            assert swing.tp.selectedComponent == swing.p2

            swing.tabbedPane(id: 'r') {
                label(id: 'a', text: 'a', title: 'ta')
                tabbedPane(id: 'st', title: 'st') {
                    label(id: 'sa', text: 'sa', title: 'sta')
                    label(id: 'sb', text: 'sb', title: 'stb')
                }
            }
            assert swing.a.parent == swing.r
            assert swing.st.parent == swing.r
            assert swing.r.indexOfTab('ta') == swing.r.indexOfComponent(swing.a)
            assert swing.r.indexOfTab('st') == swing.r.indexOfComponent(swing.st)
            assert swing.sa.parent == swing.st
            assert swing.sb.parent == swing.st
            assert swing.st.indexOfTab('sta') == swing.st.indexOfComponent(swing.sa)
            assert swing.st.indexOfTab('stb') == swing.st.indexOfComponent(swing.sb)

            // insure we don't collide with bind node work
            // GROOVY-3288
            def model = [wordValue: 'word']
            swing.tabbedPane {
                panel(title: "a") {
                    textField(id: 'wordValue', columns: 20)
                }
                bean(model, word: bind { wordValue.text })
            }
            // no asserts, the above is pass/fai;
        }
    }

    void testTabbedPaneRenamedProperties() {
        testInEDT {

            def swing = new SwingBuilder()
            swing.tabbedPane(id: 'tp',
                    titleProperty: 'xTitle',
                    tabIconProperty: 'xTabIcon',
                    tabDisabledIconProperty: 'xTabDisabledIcon',
                    tabToolTipProperty: 'xTabToolTip',
                    tabBackgroundProperty: 'xTabBackground',
                    tabForegroundProperty: 'xTabForeground',
                    tabEnabledProperty: 'xTabEnabled',
                    tabMnemonicProperty: 'xTabMnemonic',
                    tabDisplayedMnemonicIndexProperty: 'xTabDisplayedMnemonicIndex'
            ) {
                panel(id: 'p1', name: 'Title 1')
                panel(id: 'p2')
                panel(id: 'p3', xTitle: 'Title 3')
                panel(id: 'p4', xTitle: 'Title 4', name: 'Name 4')
                panel(id: 'p5', xTitle: 'Title 5', xTabIcon: imageIcon(Console.ICON_PATH, id: 'i5'))
                panel(id: 'p6', xTitle: 'Title 6', xTabDisabledIcon: imageIcon(Console.ICON_PATH, id: 'i6'))
                panel(id: 'p7', xTitle: 'Title 7', xTabToolTip: 'tip 7')
                panel(id: 'p8', xTitle: 'Title 8', xTabBackground: Color.GREEN)
                panel(id: 'p9', xTitle: 'Title 9', xTabForeground: Color.GREEN)
                panel(id: 'pA', xTitle: 'Title A', xTabEnabled: false)
                panel(id: 'pB', xTitle: 'Title B', xTabMnemonic: 'T')
                panel(id: 'pC', xTitle: 'Title C', xTabDisplayedMnemonicIndex: 2)
                panel(id: 'pD', xTitle: 'Title D', xTabMnemonic: "${'T'}")
            }

            assert swing.tp.tabCount == 13
            assert swing.tp.indexOfComponent(swing.p1) == 0
            assert swing.tp.indexOfComponent(swing.p2) == 1
            assert swing.tp.indexOfComponent(swing.p3) == 2
            assert swing.tp.indexOfComponent(swing.p4) == 3
            assert swing.tp.indexOfTab('Title 1') == 0
            assert swing.tp.indexOfTab('Title 3') == 2
            assert swing.tp.indexOfTab('Title 4') == 3
            assert swing.tp.getIconAt(4) == swing.i5
            assert swing.tp.getDisabledIconAt(5) == swing.i6
            assert swing.tp.getToolTipTextAt(6) == 'tip 7'
            assert swing.tp.getBackgroundAt(7) == Color.GREEN
            assert swing.tp.getForegroundAt(8) == Color.GREEN
            assert swing.tp.isEnabledAt(9) == false
            assert swing.tp.getMnemonicAt(10) == 0x54
            assert swing.tp.getDisplayedMnemonicIndexAt(11) == 2
            assert swing.tp.getMnemonicAt(12) == 0x54
        }
    }

    void testImageIcon() {
        testInEDT {
            def swing = new SwingBuilder()
            final String ICON_PATH = '/groovy/ui/ConsoleIcon.png'
            String baseDir = new File("src/main/groovy").absolutePath

            String resource = ICON_PATH
            GString gresource = "${ICON_PATH}"
            String path = baseDir + resource
            String gpath = "$baseDir$resource"
            File file = new File(path)
            String relativeResource = file.name
            String grelativeResource = "$file.name"
            URL url = file.toURL()

            swing.imageIcon(path, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(file: path, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(path, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: path, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'


            swing.imageIcon(gpath, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(file: gpath, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(gpath, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: gpath, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'


            swing.imageIcon(url, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(url: url, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(url, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(url: url, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'


            swing.imageIcon(resource, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource: resource, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: resource, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'


            swing.imageIcon(resource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource: resource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: resource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(gresource, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource: gresource, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(gresource, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: gresource, description: '<none>', id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'



            swing.imageIcon(gresource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource: gresource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(gresource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: gresource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'


            swing.imageIcon(relativeResource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource: relativeResource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(relativeResource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: relativeResource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'


            swing.imageIcon(grelativeResource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(resource: grelativeResource, class: Console, id: 'ii')
            assert swing.ii != null

            swing.imageIcon(grelativeResource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

            swing.imageIcon(file: grelativeResource, description: '<none>', class: Console, id: 'ii')
            assert swing.ii != null
            assert swing.ii.description == '<none>'

        }
    }

}
