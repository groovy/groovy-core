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
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.SourceAwareCustomizer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>Factory for use with {@link CompilerCustomizationBuilder}. Allows the construction of {@link SourceAwareCustomizer
 * source aware customizers}. Syntax:</p>
 * <pre><code>
 *     // apply CompileStatic AST annotation on .sgroovy files
 *     builder.source(extension: 'sgroovy') {
 *         ast(CompileStatic)
 *     }
 *
 *     // apply CompileStatic AST annotation on .sgroovy or .sg files
 *     builder.source(extensions: ['sgroovy','sg']) {
 *         ast(CompileStatic)
 *     }
 *
 *     // apply CompileStatic AST annotation on .sgroovy or .sg files
 *     builder.source(extensionValidator: { it.name in ['sgroovy','sg']}) {
 *         ast(CompileStatic)
 *     }
 *
 *     // apply CompileStatic AST annotation on files whose name is 'foo'
 *     builder.source(basename: 'foo') {
 *         ast(CompileStatic)
 *     }
 *
 *     // apply CompileStatic AST annotation on files whose name is 'foo' or 'bar'
 *     builder.source(basenames: ['foo', 'bar']) {
 *         ast(CompileStatic)
 *     }
 *
 *     // apply CompileStatic AST annotation on files whose name is 'foo' or 'bar'
 *     builder.source(basenames: { it in ['foo', 'bar'] }) {
 *         ast(CompileStatic)
 *     }
 *
 *     // apply CompileStatic AST annotation on files that do not contain a class named 'Baz'
 *     builder.source(unitValidator: { unit -> !unit.AST.classes.any { it.name == 'Baz' } }) {
 *         ast(CompileStatic)
 *     }
 * </code></pre>
 *
 * @author Cedric Champeau
 */
public class SourceAwareCustomizerFactory extends AbstractFactory implements PostCompletionFactory {

    public Object newInstance(final FactoryBuilderSupport builder, final Object name, final Object value, final Map attributes) throws InstantiationException, IllegalAccessException {
        SourceOptions data = new SourceOptions();
        if (value instanceof CompilationCustomizer) {
            data.delegate = (CompilationCustomizer) value;
        }
        return data;
    }

    @Override
    public void setChild(final FactoryBuilderSupport builder, final Object parent, final Object child) {
        if (child instanceof CompilationCustomizer && parent instanceof SourceOptions) {
            ((SourceOptions) parent).delegate = (CompilationCustomizer) child;
        }
    }

    public Object postCompleteNode(final FactoryBuilderSupport factory, final Object parent, final Object node) {
        SourceOptions data = (SourceOptions) node;
        SourceAwareCustomizer sourceAwareCustomizer = new SourceAwareCustomizer(data.delegate);
        if (data.extensionValidator !=null && (data.extension!=null || data.extensions!=null)) {
            throw new RuntimeException("You must choose between an extension name validator or an explicit extension name");
        }
        if (data.basenameValidator!=null && (data.basename!=null || data.basenames!=null)) {
            throw new RuntimeException("You must choose between an base name validator or an explicit base name");
        }

        addExtensionValidator(sourceAwareCustomizer, data);
        addBasenameValidator(sourceAwareCustomizer, data);
        if (data.unitValidator!=null) sourceAwareCustomizer.setSourceUnitValidator(data.unitValidator);
        return sourceAwareCustomizer;
    }

    private void addExtensionValidator(final SourceAwareCustomizer sourceAwareCustomizer, final SourceOptions data) {
        final List<String> extensions = data.extensions!=null?data.extensions : new LinkedList<String>();
        if (data.extension!=null) extensions.add(data.extension);
        Closure<Boolean> extensionValidator = data.extensionValidator;
        if (extensionValidator==null && !extensions.isEmpty()) {
            extensionValidator = new Closure<Boolean>(sourceAwareCustomizer) {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean call(final Object arguments) {
                    return extensions.contains(arguments);
                }
            };
        }
        sourceAwareCustomizer.setExtensionValidator(extensionValidator);
    }

    private void addBasenameValidator(final SourceAwareCustomizer sourceAwareCustomizer, final SourceOptions data) {
        final List<String> basenames = data.basenames!=null?data.basenames : new LinkedList<String>();
        if (data.basename!=null) basenames.add(data.basename);
        Closure<Boolean> basenameValidator = data.basenameValidator;
        if (basenameValidator==null && !basenames.isEmpty()) {
            basenameValidator = new Closure<Boolean>(sourceAwareCustomizer) {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean call(final Object arguments) {
                    return basenames.contains(arguments);
                }
            };
        }
        sourceAwareCustomizer.setBaseNameValidator(basenameValidator);
    }

    public static class SourceOptions {
        public CompilationCustomizer delegate;
        // validate with closures
        public Closure<Boolean> extensionValidator;
        public Closure<Boolean> unitValidator;
        public Closure<Boolean> basenameValidator;

        // validate with one string
        public String extension;
        public String basename;

        // validate with list of strings
        public List<String> extensions;
        public List<String> basenames;
    }
}
