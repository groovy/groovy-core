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
package org.codehaus.groovy.antlr.treewalker;

import org.codehaus.groovy.antlr.GroovySourceAST;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple antlr AST visitor that collects all nodes into a List.
 *
 * @author <a href="mailto:groovy@ross-rayner.com">Jeremy Rayner</a>
 * @version $Revision: 4032 $
 */

public class NodeCollector extends VisitorAdapter {
    private List nodes;
    public NodeCollector() {
        nodes = new ArrayList();
    }
    public List getNodes() {
        return nodes;
    }
    public void visitDefault(GroovySourceAST t,int visit) {
        if (visit == OPENING_VISIT) {
            nodes.add(t);
        }
    }
}
