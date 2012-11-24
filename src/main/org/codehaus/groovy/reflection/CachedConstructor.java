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
package org.codehaus.groovy.reflection;

import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.InvokerInvocationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author Alex.Tkachman
 */
public class CachedConstructor extends ParameterTypes {
    CachedClass clazz;

    public final Constructor cachedConstructor;

    public CachedConstructor(CachedClass clazz, final Constructor c) {
        this.cachedConstructor = c;
        this.clazz = clazz;
        try {
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    c.setAccessible(true);
                    return null;
                }
            });
        }
        catch (SecurityException e) {
            // IGNORE
        }
    }

    public CachedConstructor(Constructor c) {
        this(ReflectionCache.getCachedClass(c.getDeclaringClass()), c);
    }

    protected Class[] getPT() {
        return cachedConstructor.getParameterTypes();
    }

    public static CachedConstructor find(Constructor constructor) {
        CachedConstructor[] constructors = ReflectionCache.getCachedClass(constructor.getDeclaringClass()).getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            CachedConstructor cachedConstructor = constructors[i];
            if (cachedConstructor.cachedConstructor.equals(constructor))
                return cachedConstructor;
        }
        throw new RuntimeException("Couldn't find method: " + constructor);
    }

    public Object doConstructorInvoke(Object[] argumentArray) {
        argumentArray = coerceArgumentsToClasses(argumentArray);
        return invoke(argumentArray);
    }

    public Object invoke(Object[] argumentArray) {
        Constructor constr = cachedConstructor;
        try {
            return constr.newInstance(argumentArray);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof RuntimeException ? (RuntimeException)e.getCause() : new InvokerInvocationException(e);
        } catch (IllegalArgumentException e) {
            throw createExceptionText("failed to invoke constructor: ", constr, argumentArray, e, false);
        } catch (IllegalAccessException e) {
            throw createExceptionText("could not access constructor: ", constr, argumentArray, e, false);
        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException)e;
            else
                throw createExceptionText("failed to invoke constructor: ", constr, argumentArray, e, true);
        }
    }

    private static GroovyRuntimeException createExceptionText(String init, Constructor constructor, Object[] argumentArray, Throwable e, boolean setReason) {
        throw new GroovyRuntimeException(
                init
                        + constructor
                        + " with arguments: "
                        + InvokerHelper.toString(argumentArray)
                        + " reason: "
                        + e,
                setReason ? e : null);
    }

    public int getModifiers () {
        return cachedConstructor.getModifiers();
    }
    
    public CachedClass getCachedClass() {
        return clazz;
    }
}
