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

import java.util.Iterator;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Dynamic groovy proxy for another object.  All method
 * invocations get forwarded to actual object, unless the proxy overrides it.
 * See groovy/util/ProxyTest.groovy for usage details.
 *
 * @author Troy Heninger
 * @author Dierk Koenig
 */
public class Proxy extends GroovyObjectSupport {

    private Object adaptee = null;

    /**
     * This method is for convenience.
     * It allows to get around the need for defining dump ctors in subclasses.
     * See unit tests for details.
     */
    public Proxy wrap(Object adaptee){
        setAdaptee(adaptee);
        return this;
    }

    public Object getAdaptee() {
        return adaptee;
    }

    public void setAdaptee(Object adaptee) {
        this.adaptee = adaptee;
    }

    public Object invokeMethod(String name, Object args) {
        try {
            return super.invokeMethod(name, args);
        }
        catch (MissingMethodException e) {
            return InvokerHelper.invokeMethod(adaptee, name, args);
        }
    }
    
    public Iterator iterator() {
        return InvokerHelper.asIterator(adaptee);
    }

}
