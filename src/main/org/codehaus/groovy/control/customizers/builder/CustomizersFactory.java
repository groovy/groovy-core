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
package org.codehaus.groovy.control.customizers.builder;

import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This factory generates an array of compilation customizers.
 *
 * @author Cedric Champeau
 * @since 2.1.0
 */
public class CustomizersFactory extends AbstractFactory implements PostCompletionFactory {

    public Object newInstance(final FactoryBuilderSupport builder, final Object name, final Object value, final Map attributes) throws InstantiationException, IllegalAccessException {
        return new LinkedList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setChild(final FactoryBuilderSupport builder, final Object parent, final Object child) {
        if (parent instanceof Collection && child instanceof CompilationCustomizer) {
            ((Collection) parent).add(child);
        }
    }


    @SuppressWarnings("unchecked")
    public Object postCompleteNode(final FactoryBuilderSupport factory, final Object parent, final Object node) {
        if (node instanceof List) {
            List col = (List) node;
            return col.toArray(new CompilationCustomizer[col.size()]);
        }
        return node;
    }
}
