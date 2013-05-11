/*
 * Copyright 2013 the original author or authors.
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

/**
* Lazy evaluated representation of parent nodes without duplicates
*
* @author Jochen Eddel+
*/
public class NodeParents extends NodeChildren {
    
    /**
     * @param parent the GPathResult prior to the application of the expression creating this GPathResult
     * @param namespaceTagHints the known tag to namespace mappings
     */
    public NodeParents(final GPathResult parent, final Map<String, String> namespaceTagHints) {
        super(parent, parent.parent.name, namespaceTagHints);
    }

    public Iterator nodeIterator() {
        return new NodeIterator(this.parent.nodeIterator()) {
            
            private Node prev = null;
            
            protected Object getNextNode(final Iterator iter) {
                while (iter.hasNext()) {
                    final Node node = ((Node)iter.next()).parent();
                    if (node != null && node != prev) {
                        prev = node;
                        return node;
                    }
                }
                return null;
            }
        };
    }
    
}
