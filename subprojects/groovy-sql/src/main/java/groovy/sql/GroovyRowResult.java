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
package groovy.sql;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents an extent of objects.
 * It's used in the oneRow method to be able to access the result
 * of a SQL query by the name of the column, or by the column number.
 *
 * @version $Revision$
 * @author Jean-Louis Berliet
 */
public class GroovyRowResult extends GroovyObjectSupport implements Map {

    private final Map result;

    public GroovyRowResult(Map result) {
        this.result = result;
    }

    /**
     * Retrieve the value of the property by its name
     *
     * @param property is the name of the property to look at
     * @return the value of the property
     */
    public Object getProperty(String property) {
        try {
            Object key = lookupKeyIgnoringCase(property);
            if (key != null) {
                return result.get(key);
            }
            throw new MissingPropertyException(property, GroovyRowResult.class);
        }
        catch (Exception e) {
            throw new MissingPropertyException(property, GroovyRowResult.class, e);
        }
    }

    private Object lookupKeyIgnoringCase(Object key) {
        // try some special cases first for efficiency
        if (result.containsKey(key))
            return key;
        if (!(key instanceof CharSequence))
            return null;
        String keyStr = key.toString();
        for (Object next : result.keySet()) {
            if (!(next instanceof String))
                continue;
            if (keyStr.equalsIgnoreCase((String)next))
                return next;
        }
        return null;
    }

    /**
     * Retrieve the value of the property by its index.
     * A negative index will count backwards from the last column.
     *
     * @param index is the number of the column to look at
     * @return the value of the property
     */
    public Object getAt(int index) {
        try {
            // a negative index will count backwards from the last column.
            if (index < 0)
                index += result.size();
            Iterator it = result.values().iterator();
            int i = 0;
            Object obj = null;
            while ((obj == null) && (it.hasNext())) {
                if (i == index)
                    obj = it.next();
                else
                    it.next();
                i++;
            }
            return obj;
        }
        catch (Exception e) {
            throw new MissingPropertyException(Integer.toString(index), GroovyRowResult.class, e);
        }
    }

    public String toString() {
        return result.toString();
    }

    /*
     * The following methods are needed for implementing the Map interface.
     * They are mostly delegating the request to the provided Map.
     */
     
    public void clear() {
        result.clear();
    }

    public boolean containsKey(Object key) {
        return lookupKeyIgnoringCase(key) != null;
    }

    public boolean containsValue(Object value) {
        return result.containsValue(value);
    }

    public Set entrySet() {
        return result.entrySet();
    }

    public boolean equals(Object o) {
        return result.equals(o);
    }

    public Object get(Object property) {
        if (property instanceof String)
            return getProperty((String)property);
        return null;
    }

    public int hashCode() {
        return result.hashCode();
    }

    public boolean isEmpty() {
        return result.isEmpty();
    }

    public Set keySet() {
        return result.keySet();
    }

    @SuppressWarnings("unchecked")
    public Object put(Object key, Object value) {
        return result.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public void putAll(Map t) {
        result.putAll(t);
    }

    public Object remove(Object rawKey) {
        return result.remove(lookupKeyIgnoringCase(rawKey));
    }

    public int size() {
        return result.size();
    }

    public Collection values() {
        return result.values();
    }
}
