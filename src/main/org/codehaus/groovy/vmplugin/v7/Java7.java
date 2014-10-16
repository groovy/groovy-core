/*
 * Copyright 2003-2010 the original author or authors.
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

package org.codehaus.groovy.vmplugin.v7;

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.vmplugin.v6.Java6;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Java 7 based functions. Currently just a stub but you can
 * add your own methods to your own version and place it on the classpath
 * ahead of this one.
 *
 * @author Jochen Theodorou
 */
public class Java7 extends Java6 {
    private final static Constructor<MethodHandles.Lookup> LOOKUP_Constructor;
    static {
        try {
            LOOKUP_Constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new GroovyBugError(e);
        }
        if (!LOOKUP_Constructor.isAccessible()) {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    LOOKUP_Constructor.setAccessible(true);
                    return null;
                }
            });
        }
    }

    @Override
    public void invalidateCallSites() {
    	IndyInterface.invalidateSwitchPoints();
    }

    @Override
    public int getVersion() {
        return 7;
    }

    @Override
    public Object getInvokeSpecialHandle(final Method method, final Object receiver) {
        if (!method.isAccessible()) {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    method.setAccessible(true);
                    return null;
                }
            });
        }
        Class declaringClass = method.getDeclaringClass();
        try {
            return LOOKUP_Constructor.
                    newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).
                    unreflectSpecial(method, declaringClass).
                    bindTo(receiver);
        } catch (ReflectiveOperationException e) {
            throw new GroovyBugError(e);
        }
    }

    @Override
    public Object invokeHandle(Object handle, Object[] args) throws Throwable {
        MethodHandle mh = (MethodHandle) handle;
        return mh.invokeWithArguments(args);
    }
}
