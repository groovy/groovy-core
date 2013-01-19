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

import groovy.lang.Closure;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import java.util.Map;

/**
 * <p>This factory allows the generation of a {@link SecureASTCustomizer}. Embedded elements are delegated
 * to a {@link SecureASTCustomizer} instance.</p>
 *
 * @since 2.1.0
 * @author Cedric Champeau
 */
public class SecureASTCustomizerFactory extends AbstractFactory {
    @Override
    public boolean isHandlesNodeChildren() {
        return true;
    }

    public Object newInstance(final FactoryBuilderSupport builder, final Object name, final Object value, final Map attributes) throws InstantiationException, IllegalAccessException {
        return new SecureASTCustomizer();
    }

    @Override
    public boolean onNodeChildren(final FactoryBuilderSupport builder, final Object node, final Closure childContent) {
        if (node instanceof SecureASTCustomizer) {
            Closure clone = (Closure) childContent.clone();
            clone.setDelegate(node);
            clone.call();
        }
        return false;
    }
}
