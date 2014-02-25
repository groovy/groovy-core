/*
 * Copyright 2003-2008 the original author or authors.
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

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.List;
import java.util.Map;

/**
 * An abstract base class for creating arbitrary nested trees of objects
 * or events
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public abstract class BuilderSupport extends GroovyObjectSupport {

    private Object current;
    private Closure nameMappingClosure;
    private final BuilderSupport proxyBuilder;

    public BuilderSupport() {
        this.proxyBuilder = this;
    }

    public BuilderSupport(BuilderSupport proxyBuilder) {
        this(null, proxyBuilder);
    }

    public BuilderSupport(Closure nameMappingClosure, BuilderSupport proxyBuilder) {
        this.nameMappingClosure = nameMappingClosure;
        this.proxyBuilder = proxyBuilder;
    }

    /**
     * Convenience method when no arguments are required
     *
     * @param methodName the name of the method to invoke
     * @return the result of the call
     */
    public Object invokeMethod(String methodName) {
        return invokeMethod(methodName, null);
    }

    public Object invokeMethod(String methodName, Object args) {
        Object name = getName(methodName);
        return doInvokeMethod(methodName, name, args);
    }

    protected Object doInvokeMethod(String methodName, Object name, Object args) {
        Object node;
        Closure closure = null;
        List list = InvokerHelper.asList(args);

        //System.out.println("Called invokeMethod with name: " + name + " arguments: " + list);

        switch (list.size()) {
            case 0:
                node = proxyBuilder.createNode(name);
                break;
            case 1: {
                Object object = list.get(0);
                if (object instanceof Map) {
                    node = proxyBuilder.createNode(name, (Map) object);
                } else if (object instanceof Closure) {
                    closure = (Closure) object;
                    node = proxyBuilder.createNode(name);
                } else {
                    node = proxyBuilder.createNode(name, object);
                }
            }
            break;
            case 2: {
                Object object1 = list.get(0);
                Object object2 = list.get(1);
                if (object1 instanceof Map) {
                    if (object2 instanceof Closure) {
                        closure = (Closure) object2;
                        node = proxyBuilder.createNode(name, (Map) object1);
                    } else {
                        node = proxyBuilder.createNode(name, (Map) object1, object2);
                    }
                } else {
                    if (object2 instanceof Closure) {
                        closure = (Closure) object2;
                        node = proxyBuilder.createNode(name, object1);
                    } else if (object2 instanceof Map) {
                        node = proxyBuilder.createNode(name, (Map) object2, object1);
                    } else {
                        throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
                    }
                }
            }
            break;
            case 3: {
                Object arg0 = list.get(0);
                Object arg1 = list.get(1);
                Object arg2 = list.get(2);
                if (arg0 instanceof Map && arg2 instanceof Closure) {
                    closure = (Closure) arg2;
                    node = proxyBuilder.createNode(name, (Map) arg0, arg1);
                } else if (arg1 instanceof Map && arg2 instanceof Closure) {
                    closure = (Closure) arg2;
                    node = proxyBuilder.createNode(name, (Map) arg1, arg0);
                } else {
                    throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
                }
            }
            break;
            default: {
                throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
            }

        }

        if (current != null) {
            proxyBuilder.setParent(current, node);
        }

        if (closure != null) {
            // push new node on stack
            Object oldCurrent = getCurrent();
            setCurrent(node);
            // let's register the builder as the delegate
            setClosureDelegate(closure, node);
            closure.call();
            setCurrent(oldCurrent);
        }

        proxyBuilder.nodeCompleted(current, node);
        return proxyBuilder.postNodeCompletion(current, node);
    }

    /**
     * A strategy method to allow derived builders to use
     * builder-trees and switch in different kinds of builders.
     * This method should call the setDelegate() method on the closure
     * which by default passes in this but if node is-a builder
     * we could pass that in instead (or do something wacky too)
     *
     * @param closure the closure on which to call setDelegate()
     * @param node    the node value that we've just created, which could be
     *                a builder
     */
    protected void setClosureDelegate(Closure closure, Object node) {
        closure.setDelegate(this);
    }

    protected abstract void setParent(Object parent, Object child);

    protected abstract Object createNode(Object name);

    protected abstract Object createNode(Object name, Object value);

    protected abstract Object createNode(Object name, Map attributes);

    protected abstract Object createNode(Object name, Map attributes, Object value);

    /**
     * A hook to allow names to be converted into some other object
     * such as a QName in XML or ObjectName in JMX.
     *
     * @param methodName the name of the desired method
     * @return the object representing the name
     */
    protected Object getName(String methodName) {
        if (nameMappingClosure != null) {
            return nameMappingClosure.call(methodName);
        }
        return methodName;
    }


    /**
     * A hook to allow nodes to be processed once they have had all of their
     * children applied.
     *
     * @param node   the current node being processed
     * @param parent the parent of the node being processed
     */
    protected void nodeCompleted(Object parent, Object node) {
    }

    /**
     * A hook to allow nodes to be processed once they have had all of their
     * children applied and allows the actual node object that represents
     * the Markup element to be changed
     *
     * @param node   the current node being processed
     * @param parent the parent of the node being processed
     * @return the node, possibly new, that represents the markup element
     */
    protected Object postNodeCompletion(Object parent, Object node) {
        return node;
    }

    protected Object getCurrent() {
        return current;
    }

    protected void setCurrent(Object current) {
        this.current = current;
    }
}
