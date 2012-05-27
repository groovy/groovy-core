/*
 * Copyright 2003-2008 the original author or authors.
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

package groovy.swing.factory

import javax.swing.JList
import groovy.swing.binding.JListMetaMethods

/**
 * Create a JList, and handle the optional items attribute.
 *
 * @author HuberB1
 */
public class ListFactory extends AbstractFactory {

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        // FactoryBuilderSupport.checkValueIsType(value, name, JList)

        JList list
        Object items = attributes.get("items")

        if (items instanceof Vector) {
            list = new JList(attributes.remove("items"))
        } else if (items instanceof List) {
            List l = (List) attributes.remove("items")
            list = new JList(l.toArray())
        } else if (items instanceof Object[]) {
            list = new JList(attributes.remove("items"))
        } else if (value instanceof JList) {
            list = value
        } else if (value instanceof Vector) {
            list = new JList(value)
        } else if (value instanceof List) {
            List l = (List) value
            list = new JList(l.toArray())
        } else if (value instanceof Object[]) {
            list = new JList(value)
        } else {
            list = new JList()
        }

        JListMetaMethods.enhanceMetaClass(list)
        return list
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.containsKey("listData")) {
            def listData = attributes.remove("listData")
            if (listData instanceof Vector || listData instanceof Object[]) {
                node.listData = listData
            } else if (listData instanceof Collection) {
                node.listData = listData.toArray()
            } else {
                // allow any iterable ??
                node.listData = listData.collect([]){it} as Object[]
            }
        }
        return true
    }
}

