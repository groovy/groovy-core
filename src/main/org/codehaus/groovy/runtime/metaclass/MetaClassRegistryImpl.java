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
package org.codehaus.groovy.runtime.metaclass;

import groovy.lang.*;

import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.reflection.*;
import org.codehaus.groovy.runtime.*;
import org.codehaus.groovy.runtime.m12n.ExtensionModule;
import org.codehaus.groovy.runtime.m12n.ExtensionModuleRegistry;
import org.codehaus.groovy.runtime.m12n.StandardPropertiesModuleFactory;
import org.codehaus.groovy.vmplugin.VMPluginFactory;
import org.codehaus.groovy.util.FastArray;
import org.codehaus.groovy.util.ManagedLinkedList;
import org.codehaus.groovy.util.ReferenceBundle;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.closeQuietly;

/**
 * A registry of MetaClass instances which caches introspection &
 * reflection information and allows methods to be dynamically added to
 * existing classes at runtime
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author John Wilson
 * @author <a href="mailto:blackdrag@gmx.org">Jochen Theodorou</a>
 * @author Graeme Rocher
 * @author Alex Tkachman
 *
 * @version $Revision$
 */
public class MetaClassRegistryImpl implements MetaClassRegistry{
    public final static String MODULE_META_INF_FILE = "META-INF/services/org.codehaus.groovy.runtime.ExtensionModule";

    private boolean useAccessible;

    private FastArray instanceMethods = new FastArray();
    private FastArray staticMethods = new FastArray();

    private LinkedList<MetaClassRegistryChangeEventListener> changeListenerList = new LinkedList();
    private LinkedList<MetaClassRegistryChangeEventListener> nonRemoveableChangeListenerList = new LinkedList();
    private ManagedLinkedList metaClassInfo = new ManagedLinkedList<MetaClass>(ReferenceBundle.getWeakBundle());
    private ExtensionModuleRegistry moduleRegistry = new ExtensionModuleRegistry();

    public static final int LOAD_DEFAULT = 0;
    public static final int DONT_LOAD_DEFAULT = 1;
    private static MetaClassRegistry instanceInclude;
    private static MetaClassRegistry instanceExclude;

    public MetaClassRegistryImpl() {
        this(LOAD_DEFAULT, true);
    }

    public MetaClassRegistryImpl(int loadDefault) {
        this(loadDefault, true);
    }

    /**
     * @param useAccessible defines whether or not the {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}
     *                      method will be called to enable access to all methods when using reflection
     */
    public MetaClassRegistryImpl(boolean useAccessible) {
        this(LOAD_DEFAULT, useAccessible);
    }
    
    public MetaClassRegistryImpl(final int loadDefault, final boolean useAccessible) {
        this.useAccessible = useAccessible;

        if (loadDefault == LOAD_DEFAULT) {
            Map<CachedClass, List<MetaMethod>> map = new HashMap<CachedClass, List<MetaMethod>>();

            // let's register the default methods
            registerMethods(null, true, true, map);
            final Class[] additionals = DefaultGroovyMethods.additionals;
            for (int i = 0; i != additionals.length; ++i) {
                createMetaMethodFromClass(map, additionals[i]);
            }

            Class[] pluginDGMs = VMPluginFactory.getPlugin().getPluginDefaultGroovyMethods();
            for (Class plugin : pluginDGMs) {
                registerMethods(plugin, false, true, map);
            }
            registerMethods(DefaultGroovyStaticMethods.class, false, false, map);
            Class[] staticPluginDGMs = VMPluginFactory.getPlugin().getPluginStaticGroovyMethods();
            for (Class plugin : staticPluginDGMs) {
                registerMethods(plugin, false, false, map);
            }

            registerClasspathModules(map, this.getClass().getClassLoader());

            refreshMopMethods(map);

        }

        installMetaClassCreationHandle();

        final MetaClass emcMetaClass = metaClassCreationHandle.create(ExpandoMetaClass.class, this);
        emcMetaClass.initialize();
        ClassInfo.getClassInfo(ExpandoMetaClass.class).setStrongMetaClass(emcMetaClass);
        

        addNonRemovableMetaClassRegistryChangeEventListener(new MetaClassRegistryChangeEventListener(){
            public void updateConstantMetaClass(MetaClassRegistryChangeEvent cmcu) {
                synchronized (metaClassInfo) {
                   metaClassInfo.add(cmcu.getNewMetaClass());
                   DefaultMetaClassInfo.getNewConstantMetaClassVersioning();
                   Class c = cmcu.getClassToUpdate();
                   DefaultMetaClassInfo.setPrimitiveMeta(c, cmcu.getNewMetaClass()==null);
                   Field sdyn;
                   try {
                       sdyn = c.getDeclaredField(Verifier.STATIC_METACLASS_BOOL);
                       sdyn.setBoolean(null, cmcu.getNewMetaClass()!=null);
                   } catch (Throwable e) {
                       //DO NOTHING
                   }

                }
            }
        });
   }

    private void refreshMopMethods(final Map<CachedClass, List<MetaMethod>> map) {
        for (Map.Entry<CachedClass, List<MetaMethod>> e : map.entrySet()) {
            CachedClass cls = e.getKey();
            cls.setNewMopMethods(e.getValue());
        }
    }

    private void registerClasspathModules(final Map<CachedClass, List<MetaMethod>> map, final ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader.getResources(MODULE_META_INF_FILE);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                registerExtensionModuleFromMetaInf(url, map, classLoader);
            }
        } catch (IOException e) {
            // DO NOTHING
        }
    }

    private void registerExtensionModuleFromMetaInf(final URL metadata, final Map<CachedClass, List<MetaMethod>> map, final ClassLoader classLoader) {
        Properties properties = new Properties();
        InputStream inStream = null;
        try {
            inStream = metadata.openStream();
            properties.load(inStream);
        } catch (IOException e) {
            throw new GroovyRuntimeException("Unable to load module META-INF descriptor", e);
        } finally {
            closeQuietly(inStream);
        }
        registerExtensionModuleFromProperties(properties, classLoader, map);
    }

    public void registerExtensionModuleFromProperties(final Properties properties, final ClassLoader classLoader, final Map<CachedClass, List<MetaMethod>> map) {
        StandardPropertiesModuleFactory factory = new StandardPropertiesModuleFactory();
        ExtensionModule module = factory.newModule(properties, classLoader);
        if (moduleRegistry.hasModule(module.getName())) {
            ExtensionModule loadedModule = moduleRegistry.getModule(module.getName());
            if (loadedModule.getVersion().equals(module.getVersion())) {
                // already registered
                return;
            } else {
                throw new GroovyRuntimeException("Conflicting module versions. Module ["+module.getName()+" is loaded in version "+
                loadedModule.getVersion()+" and you are trying to load version "+module.getVersion());
            }
        }
        moduleRegistry.addModule(module);
        // register MetaMethods
        List<MetaMethod> metaMethods = module.getMetaMethods();
        for (MetaMethod metaMethod : metaMethods) {
            CachedClass cachedClass = metaMethod.getDeclaringClass();
            List<MetaMethod> methods = map.get(cachedClass);
            if (methods==null) {
                methods = new ArrayList<MetaMethod>(4);
                map.put(cachedClass, methods);
            }
            methods.add(metaMethod);
            if (metaMethod.isStatic()) {
                staticMethods.add(metaMethod);
            } else {
                instanceMethods.add(metaMethod);
            }
        }
    }

    public ExtensionModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    /**
     * Looks for a class called 'groovy.runtime.metaclass.CustomMetaClassCreationHandle' and if it exists uses it as the MetaClassCreationHandle
     * otherwise uses the default
     *
     * @see groovy.lang.MetaClassRegistry.MetaClassCreationHandle
     */
    private void installMetaClassCreationHandle() {
           try {
               final Class customMetaClassHandle = Class.forName("groovy.runtime.metaclass.CustomMetaClassCreationHandle");
               final Constructor customMetaClassHandleConstructor = customMetaClassHandle.getConstructor(new Class[]{});
                 this.metaClassCreationHandle = (MetaClassCreationHandle)customMetaClassHandleConstructor.newInstance();
           } catch (final ClassNotFoundException e) {
               this.metaClassCreationHandle = new MetaClassCreationHandle();
           } catch (final Exception e) {
               throw new GroovyRuntimeException("Could not instantiate custom Metaclass creation handle: "+ e, e);
           }
    }
    
    private void registerMethods(final Class theClass, final boolean useMethodWrapper, final boolean useInstanceMethods, Map<CachedClass, List<MetaMethod>> map) {
        if (useMethodWrapper) {
            // Here we instantiate objects representing MetaMethods for DGM methods.
            // Calls for such meta methods done without reflection, so more effectively.

            try {
                List<GeneratedMetaMethod.DgmMethodRecord> records = GeneratedMetaMethod.DgmMethodRecord.loadDgmInfo();

                for (GeneratedMetaMethod.DgmMethodRecord record : records) {
                    Class[] newParams = new Class[record.parameters.length - 1];
                    System.arraycopy(record.parameters, 1, newParams, 0, record.parameters.length-1);

                    MetaMethod method = new GeneratedMetaMethod.Proxy(
                            record.className,
                            record.methodName,
                            ReflectionCache.getCachedClass(record.parameters[0]),
                            record.returnType,
                            newParams
                    );
                    final CachedClass declClass = method.getDeclaringClass();
                    List<MetaMethod> arr = map.get(declClass);
                    if (arr == null) {
                        arr = new ArrayList<MetaMethod>(4);
                        map.put(declClass, arr);
                    }
                    arr.add(method);
                    instanceMethods.add(method);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                // we print the error, but we don't stop with an exception here
                // since it is more comfortable this way for development
            }
        } else {
            CachedMethod[] methods = ReflectionCache.getCachedClass(theClass).getMethods();

            for (CachedMethod method : methods) {
                final int mod = method.getModifiers();
                if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && method.getCachedMethod().getAnnotation(Deprecated.class) == null) {
                    CachedClass[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length > 0) {
                        List<MetaMethod> arr = map.get(paramTypes[0]);
                        if (arr == null) {
                            arr = new ArrayList<MetaMethod>(4);
                            map.put(paramTypes[0], arr);
                        }
                        if (useInstanceMethods) {
                            final NewInstanceMetaMethod metaMethod = new NewInstanceMetaMethod(method);
                            arr.add(metaMethod);
                            instanceMethods.add(metaMethod);
                        } else {
                            final NewStaticMetaMethod metaMethod = new NewStaticMetaMethod(method);
                            arr.add(metaMethod);
                            staticMethods.add(metaMethod);
                        }
                    }
                }
            }
        }
    }

    private void createMetaMethodFromClass(Map<CachedClass, List<MetaMethod>> map, Class aClass) {
        try {
            MetaMethod method = (MetaMethod) aClass.newInstance();
            final CachedClass declClass = method.getDeclaringClass();
            List<MetaMethod> arr = map.get(declClass);
            if (arr == null) {
                arr = new ArrayList<MetaMethod>(4);
                map.put(declClass, arr);
            }
            arr.add(method);
            instanceMethods.add(method);
        } catch (InstantiationException e) { /* ignore */
        } catch (IllegalAccessException e) { /* ignore */
        }
    }

    public final MetaClass getMetaClass(Class theClass) {
        return ClassInfo.getClassInfo(theClass).getMetaClass();
    }

    public MetaClass getMetaClass(Object obj) {
        return ClassInfo.getClassInfo(obj.getClass()).getMetaClass(obj);
    }

    /**
     * if oldMc is null, newMc will replace whatever meta class was used before.
     * if oldMc is not null, then newMc will be used only if he stored mc is
     * the same as oldMc
     */
    private void setMetaClass(Class theClass, MetaClass oldMc, MetaClass newMc) {
        final ClassInfo info = ClassInfo.getClassInfo(theClass);
        
        MetaClass mc = null;
        info.lock();
        try {
            mc = info.getStrongMetaClass();
            info.setStrongMetaClass(newMc);
        } finally {
            info.unlock();
        }
        if ((oldMc == null && mc != newMc) || (oldMc != null && mc != newMc && mc != oldMc)) {
            fireConstantMetaClassUpdate(null, theClass, mc, newMc);
        }
    }
    
    public void removeMetaClass(Class theClass) {
        setMetaClass(theClass, null, null);
    }
    
    /**
     * Registers a new MetaClass in the registry to customize the type
     *
     * @param theClass
     * @param theMetaClass
     */
    public void setMetaClass(Class theClass, MetaClass theMetaClass) {
        setMetaClass(theClass,null,theMetaClass);
    }


    public void setMetaClass(Object obj, MetaClass theMetaClass) {
        Class theClass = obj.getClass ();
        final ClassInfo info = ClassInfo.getClassInfo(theClass);
        MetaClass oldMC = null;
        info.lock();
        try {
            oldMC = info.getPerInstanceMetaClass(obj);
            info.setPerInstanceMetaClass(obj, theMetaClass);
        }
        finally {
            info.unlock();
        }
        
        fireConstantMetaClassUpdate(obj, theClass, oldMC, theMetaClass);
    }


    public boolean useAccessible() {
        return useAccessible;
    }

    // the following is experimental code, not intended for stable use yet
    private volatile MetaClassCreationHandle metaClassCreationHandle = new MetaClassCreationHandle();
    
    /**
     * Gets a handle internally used to create MetaClass implementations
     * WARNING: experimental code, likely to change soon
     * @return the handle
     */    
    public MetaClassCreationHandle getMetaClassCreationHandler() {
        return metaClassCreationHandle;
    }
    
    /**
     * Sets a handle internally used to create MetaClass implementations.
     * When replacing the handle with a custom version, you should
     * reuse the old handle to keep custom logic and to use the
     * default logic as fall back.
     * WARNING: experimental code, likely to change soon
     * @param handle the handle
     */
    public void setMetaClassCreationHandle(MetaClassCreationHandle handle) {
        if(handle == null) throw new IllegalArgumentException("Cannot set MetaClassCreationHandle to null value!");
        ClassInfo.clearModifiedExpandos();
        handle.setDisableCustomMetaClassLookup(metaClassCreationHandle.isDisableCustomMetaClassLookup());
        metaClassCreationHandle = handle;
    }    

    /**
     * Adds a listener for constant meta classes.
     * @param listener the listener
     */
    public void addMetaClassRegistryChangeEventListener(MetaClassRegistryChangeEventListener listener) {
        synchronized (changeListenerList) {
            changeListenerList.add(listener);
        }
    }
    

    /**
     * Adds a listener for constant meta classes. This listener cannot be removed!
     * @param listener the listener
     */
    public void addNonRemovableMetaClassRegistryChangeEventListener(MetaClassRegistryChangeEventListener listener) {
        synchronized (changeListenerList) {
            nonRemoveableChangeListenerList.add(listener);
        }
    }

    /**
     * Removes a constant meta class listener.
     * @param listener the listener
     */
    public void removeMetaClassRegistryChangeEventListener(MetaClassRegistryChangeEventListener listener) {
        synchronized (changeListenerList) {
            changeListenerList.remove(listener);
        }
    }

    /**
     * Causes the execution of all registered listeners. This method is used mostly
     * internal to kick of the listener notification. It can also be used by subclasses
     * to achieve the same.
     *
     * @param obj object instance if the MetaClass change is on a per-instance metaclass (or null if global)
     * @param c the class
     * @param oldMC the old MetaClass
     * @param newMc the new MetaClass
     */
    protected void fireConstantMetaClassUpdate(Object obj, Class c, final MetaClass oldMC, MetaClass newMc) {
        MetaClassRegistryChangeEventListener[]  listener = getMetaClassRegistryChangeEventListeners();
        MetaClassRegistryChangeEvent cmcu = new MetaClassRegistryChangeEvent(this, obj, c, oldMC, newMc);
        for (int i = 0; i<listener.length; i++) {
            listener[i].updateConstantMetaClass(cmcu);
        }
    }

    /**
     * Gets an array of of all registered ConstantMetaClassListener instances.
     */
    public MetaClassRegistryChangeEventListener[] getMetaClassRegistryChangeEventListeners() {
        synchronized (changeListenerList) {
            ArrayList<MetaClassRegistryChangeEventListener> ret = new ArrayList(changeListenerList.size()+nonRemoveableChangeListenerList.size());
            ret.addAll(nonRemoveableChangeListenerList);
            ret.addAll(changeListenerList);
            return (MetaClassRegistryChangeEventListener[]) ret.toArray(new MetaClassRegistryChangeEventListener[ret.size()]);
        }
    }
    
    /**
     * Singleton of MetaClassRegistry. 
     *
     * @param includeExtension
     * @return the registry
     */
    public static MetaClassRegistry getInstance(int includeExtension) {
        if (includeExtension != DONT_LOAD_DEFAULT) {
            if (instanceInclude == null) {
                instanceInclude = new MetaClassRegistryImpl();
            }
            return instanceInclude;
        } else {
            if (instanceExclude == null) {
                instanceExclude = new MetaClassRegistryImpl(DONT_LOAD_DEFAULT);
            }
            return instanceExclude;
        }
    }

    public FastArray getInstanceMethods() {
        return instanceMethods;
    }

    public FastArray getStaticMethods() {
        return staticMethods;
    }
    
    /**
     * Returns an iterator to iterate over all constant meta classes.
     * This iterator can be seen as making a snapshot of the current state
     * of the registry. The snapshot will include all meta classes that has
     * been used unless they are already collected. Collected meta classes 
     * will be skipped automatically, so you can expect that each element
     * of the iteration is not null. Calling this method is thread safe, the
     * usage of the iterator is not.
     *  
     * @return the iterator.
     */    
    public Iterator iterator() {
        final MetaClass[] refs;
        synchronized (metaClassInfo) {
            refs = (MetaClass[]) metaClassInfo.toArray(new MetaClass[0]);
        }
        
        return new Iterator() {
            // index in the ref array
            private int index = 0;
            // the current meta class
            private MetaClass currentMeta;
            // used to ensure that hasNext has been called
            private boolean hasNextCalled = false;
            // the cached hasNext call value
            private boolean hasNext = false;

            public boolean hasNext() {
                if (hasNextCalled) return hasNext;
                hasNextCalled = true;
                if(index < refs.length) {
                    hasNext = true;
                    currentMeta = refs[index];
                    index++;
                } else {
                    hasNext = false;
                }
                return hasNext;
            }
            
            private void ensureNext() {
                // we ensure that hasNext has been called before 
                // next is called
                hasNext();
                hasNextCalled = false;
            }
            
            public Object next() {
                ensureNext();
                return currentMeta;
            }
            
            public void remove() {
                ensureNext();
                setMetaClass(currentMeta.getTheClass(), currentMeta, null);
                currentMeta = null;
            }
        };
    }
}
