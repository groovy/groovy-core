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

import java.util.Iterator;
import java.util.Map;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import groovy.lang.Closure;

/**
 * Lazy evaluated representation of child nodes filtered by a Closure.
 *
 * @author John Wilson
 */
public class FilteredNodeChildren extends NodeChildren {
    private final Closure closure;

    /**
     * @param parent the GPathResult prior to the application of the expression creating this GPathResult
     * @param closure the Closure to use to filter the nodes
     * @param namespaceTagHints the known tag to namespace mappings
     */
    public FilteredNodeChildren(final GPathResult parent, final Closure closure, final Map<String, String> namespaceTagHints) {
        super(parent, parent.name, namespaceTagHints);
        this.closure = closure;
    }

    public Iterator nodeIterator() {
        return new NodeIterator(this.parent.nodeIterator()) {
            protected Object getNextNode(final Iterator iter) {
                while (iter.hasNext()) {
                    final Object node = iter.next();
                    if (closureYieldsTrueForNode(new NodeChild((Node) node, FilteredNodeChildren.this.parent, FilteredNodeChildren.this.namespaceTagHints))) {
                        return node;
                    }
                }
                return null;
            }
        };
    }

    private boolean closureYieldsTrueForNode(Object childNode) {
        return DefaultTypeTransformation.castToBoolean(FilteredNodeChildren.this.closure.call(new Object[]{childNode}));
    }
}
