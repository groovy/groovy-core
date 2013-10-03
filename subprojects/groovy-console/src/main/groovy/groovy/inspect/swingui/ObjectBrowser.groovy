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

package groovy.inspect.swingui

import java.awt.*
import javax.swing.*
import groovy.swing.SwingBuilder
import groovy.inspect.Inspector

/**
A little GUI to show some of the Inspector capabilities.
Starting this script opens the ObjectBrowser on "some String".
Use it in groovysh or groovyConsole to inspect your object of
interest with
<code>
ObjectBrowser.inspect(myObject)
</code>.

@author Dierk Koenig
**/
class ObjectBrowser {
    
    def inspector
    def swing, frame, fieldTable, methodTable, itemTable, mapTable

    static void main(args) {
        inspect("some String")
    }
    static void inspect(objectUnderInspection){
        def browser = new ObjectBrowser()
        browser.inspector = new Inspector(objectUnderInspection)
        browser.run()
    }
    
    void run() {
        swing = new SwingBuilder()
        
        frame = swing.frame(title:'Groovy Object Browser', location:[200,200],
                size:[800,600], pack: true, show:true,
                iconImage: swing.imageIcon(groovy.ui.Console.ICON_PATH).image,
                defaultCloseOperation:WindowConstants.DISPOSE_ON_CLOSE) {
                
            menuBar {
                menu(text:'Help') {
                    menuItem {action(name:'About', closure: this.&showAbout)}
                }
            }
            panel {
                borderLayout()
                panel(  name:"Class Info",
                        border: emptyBorder([5,10,5,10]),
                        constraints:NORTH) {
                    flowLayout(alignment:FlowLayout.LEFT)
                    def props = inspector.classProps
                    def classLabel = '<html>' + props.join('<br>')
                    label(classLabel)
                }
                tabbedPane(constraints:CENTER){
                    if (inspector.object instanceof Collection) {
                        scrollPane(name: ' Collection data ') {
                            itemTable = table {
                                int i = 0
                                def data = inspector.object.collect { val -> [i++, val] }
                                tableModel(list:data) {
                                    closureColumn(header:'Index', read:{it[0]})
                                    closureColumn(header:'Value', read:{it[1]})
                                }
                            }
                        }
                    }
                    if (inspector.object instanceof Map) {
                        scrollPane(name: ' Map data ') {
                            itemTable = table {
                                int i = 0
                                def data = inspector.object.collect { key, val -> [i++, key, val] }
                                tableModel(list:data) {
                                    closureColumn(header:'Index', read:{it[0]})
                                    closureColumn(header:'Key',   read:{it[1]})
                                    closureColumn(header:'Value', read:{it[2]})
                                }
                            }
                        }
                    }
                    scrollPane(name: ' Public Fields and Properties ') {
                        fieldTable = table {
                            def data = Inspector.sort(inspector.publicFields.toList())
                            data.addAll(Inspector.sort(inspector.propertyInfo.toList()))
                            tableModel(list:data) {
                                closureColumn(header:'Name',        read:{it[Inspector.MEMBER_NAME_IDX]})
                                closureColumn(header:'Value',       read:{it[Inspector.MEMBER_VALUE_IDX]})
                                closureColumn(header:'Type',        read:{it[Inspector.MEMBER_TYPE_IDX]})
                                closureColumn(header:'Origin',      read:{it[Inspector.MEMBER_ORIGIN_IDX]})
                                closureColumn(header:'Modifier',    read:{it[Inspector.MEMBER_MODIFIER_IDX]})
                                closureColumn(header:'Declarer',    read:{it[Inspector.MEMBER_DECLARER_IDX]})
                            }
                        }
                    }
                    scrollPane(name:' (Meta) Methods ' ) {
                        methodTable = table {
                            def data = Inspector.sort(inspector.methods.toList())
                            data.addAll(Inspector.sort(inspector.metaMethods.toList()))

                            tableModel(list:data) {
                                closureColumn(header:'Name',        read:{it[Inspector.MEMBER_NAME_IDX]})
                                closureColumn(header:'Params',      read:{it[Inspector.MEMBER_PARAMS_IDX]})
                                closureColumn(header:'Type',        read:{it[Inspector.MEMBER_TYPE_IDX]})
                                closureColumn(header:'Origin',      read:{it[Inspector.MEMBER_ORIGIN_IDX]})
                                closureColumn(header:'Modifier',    read:{it[Inspector.MEMBER_MODIFIER_IDX]})
                                closureColumn(header:'Declarer',    read:{it[Inspector.MEMBER_DECLARER_IDX]})
                                closureColumn(header:'Exceptions',  read:{it[Inspector.MEMBER_EXCEPTIONS_IDX]})
                            }
                        }
                    }
                }
            }
        }
        
        // Add a bit of formatting
        addSorter(itemTable)
        addSorter(mapTable)
        addSorter(fieldTable)
        addSorter(methodTable)
        
        frame.toFront()
    }
    
    void addSorter(table) {
        if (table != null) {
            def sorter = new TableSorter(table.model)
            table.model = sorter
            sorter.addMouseListenerToHeaderInTable(table)
        }
    }
    
    void showAbout(EventObject evt) {
         def pane = swing.optionPane()
         // work around GROOVY-1048
         pane.setMessage('An interactive GUI to explore object capabilities.')
         def dialog = pane.createDialog(frame, 'About Groovy Object Browser')
         dialog.show()
    }
}
