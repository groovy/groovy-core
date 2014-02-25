/*
 * Copyright 2008-2010 the original author or authors.
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

import javax.swing.ListModel
import javax.swing.JList

/**
 * @author <a href="mailto:shemnon@yahoo.com">Danno Ferrin</a>
 * @author Andres Almiray
 * @since 1.7.5
 */
class JListMetaMethods {
    public static void enhanceMetaClass(JList list) {
        AbstractSyntheticMetaMethods.enhance(list, [
            getElements:{->
                ListModel model = delegate.model;
                def results = []
                int size = model.size
                for (int i = 0; i < size; i++) {
                    results += model.getElementAt(i)
                }
                return results
            },

            getSelectedElement:{->
                return delegate.selectedValue
            },

            getSelectedElements:{->
                return delegate.selectedValues
            },

            setSelectedElement:{def item->
                return delegate.setSelectedValue(item, true)
            },

            setSelectedValue:{def item->
                return delegate.setSelectedValue(item, true)
            },
        ]);
    }
}
