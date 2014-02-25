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
package groovy.lang;

import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.Serializable;

/**
 * Represents a reference to a value
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class Reference<T> extends GroovyObjectSupport implements Serializable {

    private T value;

    public Reference() {
    }

    public Reference(T value) {
        this.value = value;
    }

    public Object getProperty(String property) {
        Object value = get();
        if (value != null) {
            return InvokerHelper.getProperty(value, property);
        }
        return super.getProperty(property);
    }

    public void setProperty(String property, Object newValue) {
        Object value = get();
        if (value != null) {
            InvokerHelper.setProperty(value, property, newValue);
        } else {
            super.setProperty(property, newValue);
        }
    }

    public Object invokeMethod(String name, Object args) {
        Object value = get();
        if (value != null) {
            try {
                return InvokerHelper.invokeMethod(value, name, args);
            } catch (Exception e) {
                return super.invokeMethod(name, args);
            }
        } else {
            return super.invokeMethod(name, args);
        }
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
