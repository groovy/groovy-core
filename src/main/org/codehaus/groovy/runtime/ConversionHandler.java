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

package org.codehaus.groovy.runtime;

import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.vmplugin.VMPlugin;
import org.codehaus.groovy.vmplugin.VMPluginFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is a general adapter to map a call to a Java interface
 * to a given delegate.
 *
 * @author Ben Yu
 * @author <a href="mailto:blackdrag@gmx.org">Jochen Theodorou</a>
 */
public abstract class ConversionHandler implements InvocationHandler, Serializable {
    private Object delegate;
    private static final long serialVersionUID = 1162833717190835227L;
    private ConcurrentHashMap handleCache;
    {
        if (VMPluginFactory.getPlugin().getVersion()>=7) handleCache = new ConcurrentHashMap();
    }

    /**
     * Creates a ConversionHandler with an delegate.
     *
     * @param delegate the delegate
     * @throws IllegalArgumentException if the given delegate is null
     */
    public ConversionHandler(Object delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
    }

    /**
     * Returns the delegate.
     *
     * @return the delegate
     */
    public Object getDelegate() {
        return delegate;
    }

    /**
     * This method is a default implementation for the invoke method given in
     * InvocationHandler. Any call to a method with a declaring class that is
     * not Object, excluding toString() and default methods is redirected to invokeCustom.
     * <p>
     * Methods like equals and hashcode are called on the class itself instead
     * of the delegate because they are considered fundamental methods that should
     * not be overwritten. The toString() method gets special treatment as it is
     * deemed to be a method that you might wish to override when called from Groovy.
     * Interface default methods from Java 8 on the other hand are considered being
     * default implementations you don't normally want to change. So they are called
     * directly too
     * </p><p>
     * In many scenarios, it is better to overwrite the invokeCustom method where
     * the core Object related methods are filtered out.
     *</p>
     * @param proxy  the proxy
     * @param method the method
     * @param args   the arguments
     * @return the result of the invocation by method or delegate
     * @throws Throwable if caused by the delegate or the method
     * @see #invokeCustom(Object, Method, Object[])
     * @see InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        VMPlugin plugin = VMPluginFactory.getPlugin();
        if (plugin.getVersion()>=7 && isDefaultMethod(method)) {
            Object handle = handleCache.get(method);
            if (handle == null) {
                handle = plugin.getInvokeSpecialHandle(method, proxy);
                handleCache.put(method, handle);
            }
            return plugin.invokeHandle(handle, args);
        }

        if (!checkMethod(method)) {
            try {
                return invokeCustom(proxy, method, args);
            } catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }
        }

        try {
            return method.invoke(this, args);
        } catch (InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }

    protected boolean isDefaultMethod(Method method) {
        return ((method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) ==
                Modifier.PUBLIC) && method.getDeclaringClass().isInterface();
    }

    protected boolean checkMethod(Method method) {
        return isCoreObjectMethod(method);
    }

    /**
     * This method is called for all Methods not defined on Object.
     * The delegate should be called here.
     *
     * @param proxy  the proxy
     * @param method the method
     * @param args   the arguments
     * @return the result of the invocation of the delegate
     * @throws Throwable any exception causes by the delegate
     * @see #invoke(Object, Method, Object[])
     * @see InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public abstract Object invokeCustom(Object proxy, Method method, Object[] args) throws Throwable;

    /**
     * Indicates whether some other object is "equal to" this one.
     * The delegate is used if the class of the parameter and the
     * current class are equal. In other cases the method will return
     * false. The exact class is here used, if inheritance is needed,
     * this method must be overwritten.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof Proxy) {
            obj = Proxy.getInvocationHandler(obj);
        }

        if (obj instanceof ConversionHandler) {
            return (((ConversionHandler) obj).getDelegate()).equals(delegate);
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code value for the delegate.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * Returns a String version of the delegate.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return delegate.toString();
    }

    /**
     * Checks whether a method is a core method from java.lang.Object.
     * Such methods often receive special treatment because they are
     * deemed fundamental enough to not be tampered with.
     *
     * @param method the method to check
     * @return true if the method is deemed to be a core method
     */
    public static boolean isCoreObjectMethod(Method method) {
        return Object.class.equals(method.getDeclaringClass());
    }

}
