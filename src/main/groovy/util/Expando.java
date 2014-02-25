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
package groovy.util;

import groovy.lang.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Represents a dynamically expandable bean.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author Hein Meling
 * @author Pilho Kim
 * @version $Revision$
 */
public class Expando extends GroovyObjectSupport {

    private Map<String, Object> expandoProperties;

    public Expando() {
    }

    public Expando(Map<String, Object> expandoProperties) {
        this.expandoProperties = expandoProperties;
    }

    /**
     * @return the dynamically expanded properties
     */
    public Map<String, Object> getProperties() {
        if (expandoProperties == null) {
            expandoProperties = createMap();
        }
        return expandoProperties;
    }

    public List<MetaExpandoProperty> getMetaPropertyValues() {
        // run through all our current properties and create MetaProperty objects
        List<MetaExpandoProperty> ret = new ArrayList<MetaExpandoProperty>();
        for (Object o : getProperties().entrySet()) {
            Entry entry = (Entry) o;
            ret.add(new MetaExpandoProperty(entry));
        }

        return ret;
    }

    public Object getProperty(String property) {
        // always use the expando properties first
        Object result = getProperties().get(property);
        if (result != null) return result;
        try {
            return super.getProperty(property);
        }
        catch (MissingPropertyException e) {
            // IGNORE
        }
        return null;
    }

    public void setProperty(String property, Object newValue) {
        // always use the expando properties
        getProperties().put(property, newValue);
    }

    public Object invokeMethod(String name, Object args) {
        try {
            return super.invokeMethod(name, args);
        }
        catch (GroovyRuntimeException e) {
            // br should get a "native" property match first. getProperty includes such fall-back logic
            Object value = this.getProperty(name);
            if (value instanceof Closure) {
                Closure closure = (Closure) value;
                closure = (Closure) closure.clone();
                closure.setDelegate(this);
                return closure.call((Object[]) args);
            } else {
                throw e;
            }
        }

    }

    /**
     * This allows toString to be overridden by a closure <i>field</i> method attached
     * to the expando object.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        Object method = getProperties().get("toString");
        if (method != null && method instanceof Closure) {
            // invoke overridden toString closure method
            Closure closure = (Closure) method;
            closure.setDelegate(this);
            return closure.call().toString();
        } else {
            return expandoProperties.toString();
        }
    }

    /**
     * This allows equals to be overridden by a closure <i>field</i> method attached
     * to the expando object.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        Object method = getProperties().get("equals");
        if (method != null && method instanceof Closure) {
            // invoke overridden equals closure method
            Closure closure = (Closure) method;
            closure.setDelegate(this);
            return (Boolean) closure.call(obj);
        } else {
            return super.equals(obj);
        }
    }

    /**
     * This allows hashCode to be overridden by a closure <i>field</i> method attached
     * to the expando object.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        Object method = getProperties().get("hashCode");
        if (method != null && method instanceof Closure) {
            // invoke overridden hashCode closure method
            Closure closure = (Closure) method;
            closure.setDelegate(this);
            return (Integer) closure.call();
        } else {
            return super.hashCode();
        }
    }

    /**
     * Factory method to create a new Map used to store the expando properties map
     *
     * @return a newly created Map implementation
     */
    protected Map<String, Object> createMap() {
        return new HashMap<String, Object>();
    }

}
