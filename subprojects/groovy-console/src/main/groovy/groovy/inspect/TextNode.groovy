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

/**
 * Represents a plain text node for use in the AST tree made by ASTBrowser 
 * 
 * @author Roshan Dawrani
 */
package groovy.inspect

class TextNode {
    Object userObject
    List<List<String>> properties
    TextNode parent
    List children
     
    TextNode(Object userObject) {
        this.userObject = userObject
        children = new ArrayList<TextNode>()
    }

    TextNode(Object userObject, List<List<String>> properties) {
        this(userObject)
        this.properties = properties
    }
    
    public void add(TextNode child) {
        children << child
    }
    
    public void setParent(TextNode newParent) {
        parent = newParent
    }
    
    String toString() {
        userObject ? userObject.toString() : "null" 
    }
}
