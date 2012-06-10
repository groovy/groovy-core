/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.inspect.swingui

import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.table.TableCellEditor

/**
 A table cell editor that will return a component automatically if it is the cell value,
 a text field if the value exists, or null otherwise (non editable cell).
 This hack allows to interact with buttons in a cell.

 @author Guillaume Balaine
  **/
public class ComponentOrTextEditor extends AbstractCellEditor implements TableCellEditor {
    /** The Swing component being edited. */
    protected JComponent editorComponent;

    public ComponentOrTextEditor() {
    }
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        if(value instanceof JComponent) {
            this.editorComponent = value;
        } else if(value) {
            this.editorComponent = new JTextArea(value.toString());
        } else {
            this.editorComponent = null
        }
        return editorComponent;
    }

    @Override
    Object getCellEditorValue() {
        return editorComponent;
    }
}

