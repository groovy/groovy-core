/*
 * Copyright 2003-2013 the original author or authors.
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
package groovy.lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

/**
 * Helper to turn a list with an even number of elements into a Map.
 * 
 * @author Pilho Kim
 * @author Tim Tiemens
 */
public class SpreadMap extends HashMap {
    private int hashCode;

    public SpreadMap(Object[] values) {
        int i = 0;
        while (i < values.length) {
            super.put(values[i++], values[i++]);
        }
    }

    public SpreadMap(Map map) {
        super(map);
    }

    /**
     * @since 1.8.0
     * @param list the list to make spreadable
     */
    public SpreadMap(List list) {
        this(list.toArray());
    }

    public Object put(Object key, Object value) {
        throw new RuntimeException("SpreadMap: " + this + " is an immutable map, and so ("
                                   + key + ": " + value + ") cannot be added.");
    }

    public Object remove(Object key) {
        throw new RuntimeException("SpreadMap: " + this + " is an immutable map, and so the key ("
                                   + key + ") cannot be deleted.");
    }

    public void putAll(Map t) {
        throw new RuntimeException("SpreadMap: " + this + " is an immutable map, and so the map ("
                                   + t + ") cannot be put in this spreadMap.");
    }

    public boolean equals(Object that) {
        return that instanceof SpreadMap && equals((SpreadMap) that);
    }

    public boolean equals(SpreadMap that) {
        if (that == null) return false;        

        if (size() == that.size()) {
            for (Object key : keySet()) {
                if (!DefaultTypeTransformation.compareEqual(get(key), that.get(key))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            for (Object key : keySet()) {
                int hash = (key != null) ? key.hashCode() : 0xbabe;
                hashCode ^= hash;
            }
        }
        return hashCode;
    }

    /**
     * @return the string expression of <code>this</code>
     */
    public String toString() {
        if (isEmpty()) {
            return "*:[:]";
        }
        StringBuilder sb = new StringBuilder("*:[");
        Iterator iter = keySet().iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            sb.append(key).append(":").append(get(key));
            if (iter.hasNext())
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
