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
package org.codehaus.groovy.runtime.callsite;

import groovy.lang.GroovyObject;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.runtime.GroovyCategorySupport;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * POGO call site
 *   meta class - cached
 *   method - cached
*/
public class PogoMetaMethodSite extends MetaMethodSite {
    public PogoMetaMethodSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class params[]) {
        super(site, metaClass, metaMethod, params);
    }

    public Object invoke(Object receiver, Object[] args) throws Throwable {
        MetaClassHelper.unwrap(args);
        try {
            return metaMethod.doMethodInvoke(receiver,  args);
        } catch (GroovyRuntimeException gre) {
            throw ScriptBytecodeAdapter.unwrap(gre);
        }
    }

    public Object callCurrent(GroovyObject receiver, Object[] args) throws Throwable {
        if(checkCall(receiver, args)) {
            try {
                return invoke(receiver,args);
            } catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }
        } else {
            return CallSiteArray.defaultCallCurrent(this, receiver, args);
        }
    }

    public Object call(Object receiver, Object[] args) throws Throwable {
        if(checkCall(receiver, args)) {
            try {
                return invoke(receiver,args);
            } catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }
        } else {
            return CallSiteArray.defaultCall(this, receiver, args);
        }
    }

    protected boolean checkCall(Object receiver, Object[] args) {
        try {
            return !GroovyCategorySupport.hasCategoryInCurrentThread()
               && ((GroovyObject)receiver).getMetaClass() == metaClass // metaClass still be valid
               && MetaClassHelper.sameClasses(params, args);
        }
        catch (NullPointerException e) {
            if (receiver == null)
              return false;
            throw e;
        }
        catch (ClassCastException e) {
            if (!(receiver instanceof GroovyObject))
              return false;
            throw e;
        }
    }

    protected boolean checkCall(Object receiver) {
        try {
            return !GroovyCategorySupport.hasCategoryInCurrentThread()
               && receiver instanceof GroovyObject
               && ((GroovyObject)receiver).getMetaClass() == metaClass // metaClass still be valid
               && MetaClassHelper.sameClasses(params);
        }
        catch (NullPointerException e) {
            if (receiver == null)
              return false;
            throw e;
        }
        catch (ClassCastException e) {
            if (!(receiver instanceof GroovyObject))
              return false;
            throw e;
        }
    }

    protected boolean checkCall(Object receiver, Object arg1) {
        try {
            return !GroovyCategorySupport.hasCategoryInCurrentThread()
               && receiver instanceof GroovyObject
               && ((GroovyObject)receiver).getMetaClass() == metaClass // metaClass still be valid
               && MetaClassHelper.sameClasses(params, arg1);
        }
        catch (NullPointerException e) {
            if (receiver == null)
              return false;
            throw e;
        }
        catch (ClassCastException e) {
            if (!(receiver instanceof GroovyObject))
              return false;
            throw e;
        }
    }

    protected boolean checkCall(Object receiver, Object arg1, Object arg2) {
        try {
            return !GroovyCategorySupport.hasCategoryInCurrentThread()
               && receiver instanceof GroovyObject
               && ((GroovyObject)receiver).getMetaClass() == metaClass // metaClass still be valid
               && MetaClassHelper.sameClasses(params, arg1, arg2);
        }
        catch (NullPointerException e) {
            if (receiver == null)
              return false;
            throw e;
        }
        catch (ClassCastException e) {
            if (!(receiver instanceof GroovyObject))
              return false;
            throw e;
        }
    }

    protected boolean checkCall(Object receiver, Object arg1, Object arg2, Object arg3) {
        try {
            return !GroovyCategorySupport.hasCategoryInCurrentThread()
               && receiver instanceof GroovyObject
               && ((GroovyObject)receiver).getMetaClass() == metaClass // metaClass still be valid
               && MetaClassHelper.sameClasses(params, arg1, arg2, arg3);
        }
        catch (NullPointerException e) {
            if (receiver == null)
              return false;
            throw e;
        }
        catch (ClassCastException e) {
            if (!(receiver instanceof GroovyObject))
              return false;
            throw e;
        }
    }

    protected boolean checkCall(Object receiver, Object arg1, Object arg2, Object arg3, Object arg4) {
        try {
            return !GroovyCategorySupport.hasCategoryInCurrentThread()
               && receiver instanceof GroovyObject
               && ((GroovyObject)receiver).getMetaClass() == metaClass // metaClass still be valid
               && MetaClassHelper.sameClasses(params, arg1, arg2, arg3, arg4);
        }
        catch (NullPointerException e) {
            if (receiver == null)
              return false;
            throw e;
        }
        catch (ClassCastException e) {
            if (!(receiver instanceof GroovyObject))
              return false;
            throw e;
        }
    }

    public static CallSite createPogoMetaMethodSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class[] params, Object[] args) {
        if (metaMethod.getClass() == CachedMethod.class)
          return createCachedMethodSite (site, metaClass, (CachedMethod) metaMethod, params, args);

        return createNonAwareCallSite(site, metaClass, metaMethod, params, args);
    }

    private static CallSite createNonAwareCallSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class[] params, Object[] args) {
        if (metaMethod.correctArguments(args) == args) {
            if (noWrappers(args)) {
                if (noCoerce(metaMethod,args))
                    return new PogoMetaMethodSiteNoUnwrap(site, metaClass, metaMethod, params);
                else {
                    return new PogoMetaMethodSiteNoUnwrapNoCoerce(site, metaClass, metaMethod, params);
                }
            }
        }
        return new PogoMetaMethodSite(site, metaClass, metaMethod, params);
    }

    public static CallSite createCachedMethodSite(CallSite site, MetaClassImpl metaClass, CachedMethod metaMethod, Class[] params, Object[] args) {
        if (metaMethod.correctArguments(args) == args) {
            if (noWrappers(args)) {
                if (noCoerce(metaMethod,args))
                    return new PogoCachedMethodSiteNoUnwrap(site, metaClass, metaMethod, params);
                else {
                    return metaMethod.createPogoMetaMethodSite(site, metaClass, params);
                }
            }
        }
        return new PogoCachedMethodSite(site, metaClass, metaMethod, params);
    }

    public static class PogoCachedMethodSite extends PogoMetaMethodSite {
        final Method reflect;

        public PogoCachedMethodSite(CallSite site, MetaClassImpl metaClass, CachedMethod metaMethod, Class[] params) {
            super(site, metaClass, metaMethod, params);
            reflect = metaMethod.setAccessible();
        }

        public Object invoke(Object receiver, Object[] args) throws Throwable {
            MetaClassHelper.unwrap(args);
            args = metaMethod.coerceArgumentsToClasses(args);
            try {
                return reflect.invoke(receiver, args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof GroovyRuntimeException) {
                    throw ScriptBytecodeAdapter.unwrap ((GroovyRuntimeException) cause);
                } else {
                    throw cause;
                }
            }
        }
    }

    public static class PogoCachedMethodSiteNoUnwrap extends PogoCachedMethodSite {

        public PogoCachedMethodSiteNoUnwrap(CallSite site, MetaClassImpl metaClass, CachedMethod metaMethod, Class params[]) {
            super(site, metaClass, metaMethod, params);
        }

        public final Object invoke(Object receiver, Object[] args) throws Throwable {
            args = metaMethod.coerceArgumentsToClasses(args);
            try {
                return reflect.invoke(receiver, args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof GroovyRuntimeException) {
                    throw ScriptBytecodeAdapter.unwrap ((GroovyRuntimeException) cause);
                } else {
                    throw cause;
                }
            }
        }
    }

    public static class PogoCachedMethodSiteNoUnwrapNoCoerce extends PogoCachedMethodSite {

        public PogoCachedMethodSiteNoUnwrapNoCoerce(CallSite site, MetaClassImpl metaClass, CachedMethod metaMethod, Class params[]) {
            super(site, metaClass, metaMethod, params);
        }

        public final Object invoke(Object receiver, Object[] args) throws Throwable {
            try {
                return reflect.invoke(receiver, args);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof GroovyRuntimeException) {
                    throw ScriptBytecodeAdapter.unwrap ((GroovyRuntimeException) cause);
                } else {
                    throw cause;
                }
            }
        }
    }

    /**
     * Call site where we know there is no need to unwrap arguments
     */
    public static class PogoMetaMethodSiteNoUnwrap extends PogoMetaMethodSite {

        public PogoMetaMethodSiteNoUnwrap(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class params[]) {
            super(site, metaClass, metaMethod, params);
        }

        public final Object invoke(Object receiver, Object[] args) throws Throwable {
            try {
                return metaMethod.doMethodInvoke(receiver,  args);
            } catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }
        }
    }

    /**
     * Call site where we know there is no need neither unwrap nor coerce arguments
     */
    public static class PogoMetaMethodSiteNoUnwrapNoCoerce extends PogoMetaMethodSite {

        public PogoMetaMethodSiteNoUnwrapNoCoerce(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class params[]) {
            super(site, metaClass, metaMethod, params);
        }

        public final Object invoke(Object receiver, Object[] args) throws Throwable {
            try {
                return metaMethod.invoke(receiver,  args);
            } catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }
        }
    }
}
