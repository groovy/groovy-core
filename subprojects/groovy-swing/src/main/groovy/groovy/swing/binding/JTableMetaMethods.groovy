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
package groovy.swing.binding

import javax.swing.JTable
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel
import org.codehaus.groovy.runtime.InvokerHelper


/**
 * Created by IntelliJ IDEA.
 * User: Danno.Ferrin
 * Date: Jun 19, 2008
 * Time: 12:46:29 PM
 * To change this template use File | Settings | File Templates.
 */
class JTableMetaMethods {

    public static void enhanceMetaClass(table) {
        AbstractSyntheticMetaMethods.enhance(table, [

            getElements:{->
                def model = delegate.model;
                if (model instanceof javax.swing.table.DefaultTableModel) {
                    return Collections.unmodifiableList(model.getDataVector())
                } else if (model instanceof groovy.model.DefaultTableModel) {
                    return Collections.unmodifiableList(model.rows)
                }
            },
            getSelectedElement:{->
                return getElement(delegate, delegate.selectedRow)
            },
            getSelectedElements:{->
                def myTable = delegate
                return myTable.getSelectedRows().collect { getElement(myTable, it) }
            }
        ]);
    }

    public static Object getElement(JTable table, int row) {
        if (row == -1) {
            return null;
        }
        TableModel model = table.model
        if (model instanceof javax.swing.table.DefaultTableModel) {
            // could be groovier, but it works and is a well understood idiom
            Map value = [:]
            TableColumnModel cmodel = table.columnModel
            for (int i = 0; i < cmodel.getColumnCount(); i++) {
                TableColumn c = cmodel.getColumn(i);
                value.put(c.getIdentifier(), // will fall through to headerValue
                    table.getValueAt(row, c.getModelIndex()))
            }
            return value;
        } else if (model instanceof groovy.model.DefaultTableModel) {
            Object rowValue = model.getRowsModel().value
            if (rowValue == null) {
                return null;
            }
            return InvokerHelper.asList(rowValue)[row]
        }
    }

}
