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

package groovy.xml.streamingmarkupsupport;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class Builder extends GroovyObjectSupport {
    protected final Map namespaceMethodMap = new HashMap();
    
    public Builder(final Map namespaceMethodMap) {
    final Iterator keyIterator = namespaceMethodMap.keySet().iterator();
        
        while (keyIterator.hasNext()) {
        final Object key = keyIterator.next();
        final List value = (List)namespaceMethodMap.get(key);
        final Closure dg = ((Closure)value.get(1)).asWritable();
        
            this.namespaceMethodMap.put(key, new Object[]{value.get(0), dg, fettleMethodMap(dg, (Map)value.get(2))});
        }
    }
    
    private static Map fettleMethodMap(final Closure defaultGenerator, final Map methodMap) {
    final Map newMethodMap = new HashMap();

        for (Object o : methodMap.keySet()) {
            final Object key = o;
            final Object value = methodMap.get(key);

            if ((value instanceof Closure)) {
                newMethodMap.put(key, value);
            } else {
                newMethodMap.put(key, defaultGenerator.curry((Object[]) value));
            }
        }
        
        return newMethodMap;
    }
    
    public abstract Object bind(Closure root);
    
    protected abstract static class Built extends GroovyObjectSupport {
    protected final Closure root;
    protected final Map namespaceSpecificTags = new HashMap();
        
        public Built(final Closure root, final Map namespaceTagMap) {
            this.namespaceSpecificTags.putAll(namespaceTagMap);
        
            this.root = (Closure)root.clone();
            
            this.root.setDelegate(this);
        }
    }
}
