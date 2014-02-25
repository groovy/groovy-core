/*
 * Copyright 2003-2007 the original author or authors.
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
package groovy.xml;

import groovy.util.BuilderSupport;

import java.util.Map;

/**
 * A helper class for creating namespaces for GroovyMarkup
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class NamespaceBuilder {

    private BuilderSupport builder;

    public static NamespaceBuilderSupport newInstance(BuilderSupport builder, String uri) {
        return new NamespaceBuilder(builder).namespace(uri);
    }
    
    public static NamespaceBuilderSupport newInstance(BuilderSupport builder) {
        return new NamespaceBuilderSupport(builder);
    }

    public static NamespaceBuilderSupport newInstance(BuilderSupport builder, String uri, String prefix) {
        return new NamespaceBuilder(builder).namespace(uri, prefix);
    }

    public static NamespaceBuilderSupport newInstance(Map nsMap, BuilderSupport builder) {
        return new NamespaceBuilder(builder).declareNamespace(nsMap);
    }

    public NamespaceBuilder(BuilderSupport builder) {
        this.builder = builder;
    }

    public NamespaceBuilderSupport namespace(String uri) {
        return namespace(uri, "");
    }

    public NamespaceBuilderSupport namespace(String uri, String prefix) {
        return new NamespaceBuilderSupport(builder, uri, prefix);
    }

    public NamespaceBuilderSupport declareNamespace(Map ns) {
        return new NamespaceBuilderSupport(builder, ns);
    }
}
