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
package groovy.util.slurpersupport;

import groovy.lang.Buildable;
import groovy.lang.GroovyObject;
import groovy.lang.Writable;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Helper base class used for lazy updates.
 *
 * @author John Wilson
 */
public abstract class ReplacementNode implements Buildable, Writable {
    public abstract void build(GroovyObject builder, Map namespaceMap, Map<String, String> namespaceTagHints);
    
    public void build(final GroovyObject builder) {
        build(builder, null, null);
    }
    
    public Writer writeTo(final Writer out) throws IOException {
        return out;
    }
}
