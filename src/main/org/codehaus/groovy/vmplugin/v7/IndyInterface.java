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

import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistryChangeEvent;
import groovy.lang.MetaClassRegistryChangeEventListener;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.groovy.GroovyBugError;

/**
 * Bytecode level interface for bootstrap methods used by invokedynamic.
 * This class provides a logging ability by using the boolean system property
 * groovy.indy.logging. Other than that this class contains the 
 * interfacing methods with bytecode for invokedynamic as well as some helper
 * methods and classes.
 * 
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 */
public class IndyInterface {

        /**
         * flags for method and property calls
         */
        public static final int 
            SAFE_NAVIGATION = 1,  THIS_CALL     = 2, 
            GROOVY_OBJECT   = 4,  IMPLICIT_THIS = 8,
            SPREAD_CALL     = 16, UNCACHED_CALL = 32;

        /**
         * Enum for easy differentiation between call types
         * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
         */
        public static enum CALL_TYPES {
            /**Method invocation type*/         METHOD("invoke"), 
            /**Constructor invocation type*/    INIT("init"), 
            /**Get property invocation type*/   GET("getProperty"), 
            /**Set property invocation type*/   SET("setProperty");
            /**The name of the call site type*/
            private final String name;
            private CALL_TYPES(String callSiteName) {
                this.name = callSiteName;
            }
            /** Returns the name of the call site type */
            public String getCallSiteName(){ return name; }
        }

        /** Logger */
        protected final static Logger LOG;
        /** boolean to indicate if logging for indy is enabled */
        protected final static boolean LOG_ENABLED;
        static {
            LOG = Logger.getLogger(IndyInterface.class.getName());
            if (System.getProperty("groovy.indy.logging")!=null) {
                LOG.setLevel(Level.ALL);
                LOG_ENABLED = true;
            } else {
                LOG_ENABLED = false;
            }
        }
        /** LOOKUP constant used for for example unreflect calls */
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
        /** handle for the selectMethod method */
        private static final MethodHandle SELECT_METHOD;
        static {
            MethodType mt = MethodType.methodType(Object.class, MutableCallSite.class, Class.class, String.class, int.class, Boolean.class, Boolean.class, Boolean.class, Object.class, Object[].class);
            try {
                SELECT_METHOD = LOOKUP.findStatic(IndyInterface.class, "selectMethod", mt);
            } catch (Exception e) {
                throw new GroovyBugError(e);
            }
        }

        protected static SwitchPoint switchPoint = new SwitchPoint();
        static {
            GroovySystem.getMetaClassRegistry().addMetaClassRegistryChangeEventListener(new MetaClassRegistryChangeEventListener() {
                public void updateConstantMetaClass(MetaClassRegistryChangeEvent cmcu) {
                	invalidateSwitchPoints();
                }
            });
        }

        /**
         * Callback for constant meta class update change
         */
        protected static void invalidateSwitchPoints() {
            if (LOG_ENABLED) {
                 LOG.info("invalidating switch point");
            }
        	SwitchPoint old = switchPoint;
            switchPoint = new SwitchPoint();
            synchronized(IndyInterface.class) { SwitchPoint.invalidateAll(new SwitchPoint[]{old}); }
        }

        /**
         * bootstrap method for method calls from Groovy compiled code with indy 
         * enabled. This method gets a flags parameter which uses the following 
         * encoding:<ul>
         * <li>{@value #SAFE_NAVIGATION} is the flag value for safe navigation see {@link #SAFE_NAVIGATION}<li/>
         * <li>{@value #THIS_CALL} is the flag value for a call on this see {@link #THIS_CALL}</li>
         * </ul> 
         * @param caller - the caller
         * @param callType - the type of the call
         * @param type - the call site type
         * @param name - the real method name
         * @param flags - call flags
         * @return the produced CallSite
         * @since Groovy 2.1.0
         */
        public static CallSite bootstrap(Lookup caller, String callType, MethodType type, String name, int flags) {
            boolean safe = (flags&SAFE_NAVIGATION)!=0;
            boolean thisCall = (flags&THIS_CALL)!=0;
            boolean spreadCall = (flags&SPREAD_CALL)!=0;
            int callID;
            if (callType.equals(CALL_TYPES.METHOD.getCallSiteName())) {
                callID = CALL_TYPES.METHOD.ordinal();
            } else if (callType.equals(CALL_TYPES.INIT.getCallSiteName())) {
                callID = CALL_TYPES.INIT.ordinal();
            } else if (callType.equals(CALL_TYPES.GET.getCallSiteName())) {
                callID = CALL_TYPES.GET.ordinal();
            } else if (callType.equals(CALL_TYPES.SET.getCallSiteName())) {
                callID = CALL_TYPES.SET.ordinal();
            } else {
                throw new GroovyBugError("Unknown call type: "+callType);
            }
            return realBootstrap(caller, name, callID, type, safe, thisCall, spreadCall);
        }

        /**
         * bootstrap method for method calls with "this" as receiver
         * @deprecated since Groovy 2.1.0
         */
        public static CallSite bootstrapCurrent(Lookup caller, String name, MethodType type) {
            return realBootstrap(caller, name, CALL_TYPES.METHOD.ordinal(), type, false, true, false);
        }

        /**
         * bootstrap method for method calls with "this" as receiver safe
         * @deprecated since Groovy 2.1.0
         */
        public static CallSite bootstrapCurrentSafe(Lookup caller, String name, MethodType type) {
            return realBootstrap(caller, name, CALL_TYPES.METHOD.ordinal(), type, true, true, false);
        }
        
        /**
         * bootstrap method for standard method calls
         * @deprecated since Groovy 2.1.0
         */
        public static CallSite bootstrap(Lookup caller, String name, MethodType type) {
            return realBootstrap(caller, name, CALL_TYPES.METHOD.ordinal(), type, false, false, false);
        }
        
        /**
         * bootstrap method for null safe standard method calls
         * @deprecated since Groovy 2.1.0
         */
        public static CallSite bootstrapSafe(Lookup caller, String name, MethodType type) {
            return realBootstrap(caller, name, CALL_TYPES.METHOD.ordinal(), type, true, false, false);
        }

        /**
         * backing bootstrap method with all parameters
         */
        private static CallSite realBootstrap(Lookup caller, String name, int callID, MethodType type, boolean safe, boolean thisCall, boolean spreadCall) {
            // since indy does not give us the runtime types
            // we produce first a dummy call site, which then changes the target to one,
            // that does the method selection including the the direct call to the 
            // real method.
            MutableCallSite mc = new MutableCallSite(type);
            MethodHandle mh = makeFallBack(mc,caller.lookupClass(),name,callID,type,safe,thisCall,spreadCall);
            mc.setTarget(mh);
            return mc;
        }

        /**
         * Makes a fallback method for an invalidated method selection
         */
        protected static MethodHandle makeFallBack(MutableCallSite mc, Class<?> sender, String name, int callID, MethodType type, boolean safeNavigation, boolean thisCall, boolean spreadCall) {
            MethodHandle mh = MethodHandles.insertArguments(SELECT_METHOD, 0, mc, sender, name, callID, safeNavigation, thisCall, spreadCall, /*dummy receiver:*/ 1);
            mh =    mh.asCollector(Object[].class, type.parameterCount()).
                    asType(type);
            return mh;
        }

        /**
         * Core method for indy method selection using runtime types.
         */
        public static Object selectMethod(MutableCallSite callSite, Class sender, String methodName, int callID, Boolean safeNavigation, Boolean thisCall, Boolean spreadCall, Object dummyReceiver, Object[] arguments) throws Throwable {
            Selector selector = Selector.getSelector(callSite, sender, methodName, callID, safeNavigation, thisCall, spreadCall, arguments); 
            selector.setCallSiteTarget();

            MethodHandle call = selector.handle.asSpreader(Object[].class, arguments.length);
            call = call.asType(MethodType.methodType(Object.class,Object[].class));
            return call.invokeExact(arguments);
        }
}
