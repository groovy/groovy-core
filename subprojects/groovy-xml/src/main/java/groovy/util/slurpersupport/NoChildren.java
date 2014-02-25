/*
 * Copyright 2003-2012 the original author or authors.
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

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyRuntimeException;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * Lazy evaluated representation of a GPath expression returning no children.
 *
 * As this class represents a GPath expression with no results, all methods
 * are either NOPs or return an empty result.
 *
 * @author John Wilson
 */
public class NoChildren extends GPathResult {

    /**
     * @param parent            the GPathResult prior to the application of the expression creating this GPathResult
     * @param name              if the GPathResult corresponds to something with a name, e.g. a node
     * @param namespaceTagHints the known tag to namespace mappings
     */
    public NoChildren(final GPathResult parent, final String name, final Map<String, String> namespaceTagHints) {
        super(parent, name, "*", namespaceTagHints);
    }

    /**
     * Returns <code>0</code>.
     * @return <code>0</code>
     */
    public int size() {
        return 0;
    }

    /**
     * Returns an empty <code>String</code>.
     * @return an empty <code>String</code>
     */
    public String text() {
        return "";
    }

    /**
     * Throws a <code>GroovyRuntimeException</code>, because it is not implemented yet.
     */
    public GPathResult parents() {
        // TODO Auto-generated method stub
        throw new GroovyRuntimeException("parents() not implemented yet");
    }

    /**
     * Returns an empty <code>Iterator</code>.
     * @return an empty <code>Iterator</code>
     */
    public Iterator childNodes() {
        return iterator();
    }

    /**
     * Returns an empty <code>Iterator</code>.
     * @return an empty <code>Iterator</code>
     */
    public Iterator iterator() {
        return new Iterator() {
            public boolean hasNext() {
                return false;
            }

            public Object next() {
                return null;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns <code>this</code>.
     * @return <code>this</code>
     */
    public GPathResult find(final Closure closure) {
        return this;
    }

    /**
     * Returns <code>this</code>.
     * @return <code>this</code>
     */
    public GPathResult findAll(final Closure closure) {
        return this;
    }

    /**
     * Returns an empty iterator.
     * @return an empty iterator
     */
    public Iterator nodeIterator() {
        return iterator();
    }

    /**
     * Does not write any output, just returns the writer.
     *
     * @return the <code>Writer</code> which was passed in
     */
    public Writer writeTo(final Writer out) throws IOException {
        return out;
    }

    /**
     * NOP
     */
    public void build(final GroovyObject builder) {
    }

    /**
     * NOP
     */
    protected void replaceNode(final Closure newValue) {
        // No elements match GPath expression - do nothing
    }

    /**
     * NOP
     */
    protected void replaceBody(final Object newValue) {
        // No elements match GPath expression - do nothing
    }

    /**
     * NOP
     */
    protected void appendNode(final Object newValue) {
        // TODO consider creating an element for this
    }

    /**
     * Returns <code>false</code>.
     * @return <code>false</code>
     */
    public boolean asBoolean() {
        return false;
    }
}
