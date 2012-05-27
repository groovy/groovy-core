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

import groovy.lang.*;

import org.codehaus.groovy.classgen.asm.BytecodeHelper;
import org.codehaus.groovy.runtime.callsite.CallSiteClassLoader;
import org.codehaus.groovy.util.LazyReference;
import org.codehaus.groovy.util.FastArray;
import org.codehaus.groovy.util.ReferenceBundle;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * @author Alex.Tkachman
 */
public class CachedClass {
    private final Class cachedClass;
    public ClassInfo classInfo;
    
    private static ReferenceBundle softBundle = ReferenceBundle.getSoftBundle();

    private final LazyReference<CachedField[]> fields = new LazyReference<CachedField[]>(softBundle) {
        public CachedField[] initValue() {
            final Field[] declaredFields = (Field[])
               AccessController.doPrivileged(new PrivilegedAction/*<Field[]>*/() {
                   public /*Field[]*/ Object run() {
                       final Field[] df = getTheClass().getDeclaredFields();
                       try {
                           AccessibleObject.setAccessible(df, true);
                       } catch (SecurityException e) {
                           // swallow for strict security managers
                       }
                       return df;                                                   
                   }
               });
            CachedField [] fields = new CachedField[declaredFields.length];
            for (int i = 0; i != fields.length; ++i)
                fields[i] = new CachedField(declaredFields[i]);
            return fields;
        }
    };

    private LazyReference<CachedConstructor[]> constructors = new LazyReference<CachedConstructor[]>(softBundle) {
        public CachedConstructor[] initValue() {
            final Constructor[] declaredConstructors = (Constructor[])
               AccessController.doPrivileged(new PrivilegedAction/*<Constructor[]>*/() {
                   public /*Constructor[]*/ Object run() {
                       return getTheClass().getDeclaredConstructors();
                   }
               });
            CachedConstructor [] constructors = new CachedConstructor[declaredConstructors.length];
            for (int i = 0; i != constructors.length; ++i)
                constructors[i] = new CachedConstructor(CachedClass.this, declaredConstructors[i]);
            return constructors;
        }
    };

    private LazyReference<CachedMethod[]> methods = new LazyReference<CachedMethod[]>(softBundle) {
        public CachedMethod[] initValue() {
            final Method[] declaredMethods = (Method[])
               AccessController.doPrivileged(new PrivilegedAction/*<Method[]>*/() {
                   public /*Method[]*/ Object run() {
                       final Method[] dm = getTheClass().getDeclaredMethods();
                       try {
                           AccessibleObject.setAccessible(dm, true);
                       } catch (SecurityException e) {
                           // swallow for strict security managers
                       }
                       return dm;
                   }
               });
            List<CachedMethod> methods = new ArrayList<CachedMethod>(declaredMethods.length);
            List<CachedMethod> mopMethods = new ArrayList<CachedMethod>(declaredMethods.length);
            for (int i = 0; i != declaredMethods.length; ++i) {
                final CachedMethod cachedMethod = new CachedMethod(CachedClass.this, declaredMethods[i]);
                final String name = cachedMethod.getName();

                if (declaredMethods[i].isBridge() || name.indexOf('+') >= 0) {
                    // Skip Synthetic methods inserted by JDK 1.5 compilers and later
                    continue;
                } /*else if (Modifier.isAbstract(reflectionMethod.getModifiers())) {
                   continue;
                }*/

                if (name.startsWith("this$") || name.startsWith("super$"))
                  mopMethods.add(cachedMethod);
                else
                  methods.add(cachedMethod);
            }
            CachedMethod [] resMethods = methods.toArray(new CachedMethod[methods.size()]);
            Arrays.sort(resMethods);

            final CachedClass superClass = getCachedSuperClass();
            if (superClass != null) {
                superClass.getMethods();
                final CachedMethod[] superMopMethods = superClass.mopMethods;
                for (int i = 0; i != superMopMethods.length; ++i)
                  mopMethods.add(superMopMethods[i]);
            }
            CachedClass.this.mopMethods = mopMethods.toArray(new CachedMethod[mopMethods.size()]);
            Arrays.sort(CachedClass.this.mopMethods, CachedMethodComparatorByName.INSTANCE);

            return resMethods;
        }
    };

    private LazyReference<CachedClass> cachedSuperClass = new LazyReference<CachedClass>(softBundle) {
        public CachedClass initValue() {
            if (!isArray)
              return ReflectionCache.getCachedClass(getTheClass().getSuperclass());
            else
              if (cachedClass.getComponentType().isPrimitive() || cachedClass.getComponentType() == Object.class)
                return ReflectionCache.OBJECT_CLASS;
              else
                return ReflectionCache.OBJECT_ARRAY_CLASS;
        }
    };

    private final LazyReference<CallSiteClassLoader> callSiteClassLoader = new LazyReference<CallSiteClassLoader>(softBundle) {
        public CallSiteClassLoader initValue() {
            return
               AccessController.doPrivileged(new PrivilegedAction<CallSiteClassLoader>() {
                   public CallSiteClassLoader run() {
                       return new CallSiteClassLoader(CachedClass.this.cachedClass);
                   }
               });
        }
    };

    private final LazyReference<LinkedList<ClassInfo>> hierarchy = new LazyReference<LinkedList<ClassInfo>>(softBundle) {
        public LinkedList<ClassInfo> initValue() {
            Set<ClassInfo> res = new LinkedHashSet<ClassInfo> ();

            res.add(classInfo);

            for (CachedClass iface : getDeclaredInterfaces())
              res.addAll(iface.getHierarchy());

            final CachedClass superClass = getCachedSuperClass();
            if (superClass != null)
              res.addAll(superClass.getHierarchy());

            if (isInterface)
              res.add(ReflectionCache.OBJECT_CLASS.classInfo);

            return new LinkedList<ClassInfo> (res);
        }
    };

    static final MetaMethod[] EMPTY = new MetaMethod[0];

    int hashCode;

    public  CachedMethod [] mopMethods;
    public static final CachedClass[] EMPTY_ARRAY = new CachedClass[0];

    private final LazyReference<Set<CachedClass>> declaredInterfaces = new LazyReference<Set<CachedClass>> (softBundle) {
        public Set<CachedClass> initValue() {
            Set<CachedClass> res = new HashSet<CachedClass> (0);

            Class[] classes = getTheClass().getInterfaces();
            for (Class cls : classes) {
                res.add(ReflectionCache.getCachedClass(cls));
            }
            return res;
        }
    };

    private final LazyReference<Set<CachedClass>> interfaces = new LazyReference<Set<CachedClass>> (softBundle) {
        public Set<CachedClass> initValue() {
            Set<CachedClass> res = new HashSet<CachedClass> (0);

            if (getTheClass().isInterface())
              res.add(CachedClass.this);

            Class[] classes = getTheClass().getInterfaces();
            for (Class cls : classes) {
                final CachedClass aClass = ReflectionCache.getCachedClass(cls);
                if (!res.contains(aClass))
                    res.addAll(aClass.getInterfaces());
            }

            final CachedClass superClass = getCachedSuperClass();
            if (superClass != null)
              res.addAll(superClass.getInterfaces());

            return res;
        }
    };

    public final boolean isArray;
    public final boolean isPrimitive;
    public final int modifiers;
    int distance = -1;
    public final boolean isInterface;
    public final boolean isNumber;

    public CachedClass(Class klazz, ClassInfo classInfo) {
        cachedClass = klazz;
        this.classInfo = classInfo;
        isArray = klazz.isArray();
        isPrimitive = klazz.isPrimitive();
        modifiers = klazz.getModifiers();
        isInterface = klazz.isInterface();
        isNumber = Number.class.isAssignableFrom(klazz);

        for (CachedClass inf : getInterfaces()) {
            ReflectionCache.isAssignableFrom(klazz, inf.cachedClass);
        }

        for (CachedClass cur = this; cur != null; cur = cur.getCachedSuperClass()) {
            ReflectionCache.setAssignableFrom(cur.cachedClass, klazz);
        }
    }

    public CachedClass getCachedSuperClass() {
        return cachedSuperClass.get();
    }

    public Set<CachedClass> getInterfaces() {
        return interfaces.get();
    }

    public Set<CachedClass> getDeclaredInterfaces() {
        return declaredInterfaces.get();
    }

    public CachedMethod[] getMethods() {
        return methods.get();
    }

    public CachedField[] getFields() {
        return fields.get();
    }

    public CachedConstructor[] getConstructors() {
        return constructors.get();
    }

    public CachedMethod searchMethods(String name, CachedClass[] parameterTypes) {
        CachedMethod[] methods = getMethods();

        CachedMethod res = null;
        for (CachedMethod m : methods) {
            if (m.getName().equals(name)
                    && ReflectionCache.arrayContentsEq(parameterTypes, m.getParameterTypes())
                    && (res == null || res.getReturnType().isAssignableFrom(m.getReturnType())))
                res = m;
        }

        return res;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Object coerceArgument(Object argument) {
        return argument;
    }
    
    public int getSuperClassDistance() {
        synchronized (getTheClass()) {
            if (distance == -1) {
                int distance = 0;
                for (Class klazz= getTheClass(); klazz != null; klazz = klazz.getSuperclass()) {
                    distance++;
                }
                this.distance = distance;
            }
            return distance;
        }
    }

    public int hashCode() {
        if (hashCode == 0) {
          hashCode = super.hashCode();
          if (hashCode == 0)
            hashCode = 0xcafebebe;
        }
        return hashCode;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public boolean isVoid() {
        return getTheClass() == void.class;
    }
    
    public boolean isInterface() {
        return isInterface;
    }

    public String getName() {
        return getTheClass().getName();
    }

    public String getTypeDescription() {
        return BytecodeHelper.getTypeDescription(getTheClass());
    }

    public final Class getTheClass() {
        return cachedClass;
    }

    public MetaMethod[] getNewMetaMethods() {
        List<MetaMethod> arr = new ArrayList<MetaMethod>();
        arr.addAll(Arrays.asList(classInfo.newMetaMethods));

        final MetaClass metaClass = classInfo.getStrongMetaClass();
        if (metaClass != null && metaClass instanceof ExpandoMetaClass) {
            arr.addAll(((ExpandoMetaClass)metaClass).getExpandoMethods());
        }

        if (isInterface) {
            MetaClass mc = ReflectionCache.OBJECT_CLASS.classInfo.getStrongMetaClass();
            addSubclassExpandos(arr, mc);
        }
        else {
            for (CachedClass cls = this; cls != null; cls = cls.getCachedSuperClass()) {
                MetaClass mc = cls.classInfo.getStrongMetaClass();
                addSubclassExpandos(arr, mc);
            }
        }

        for (CachedClass inf : getInterfaces()) {
            MetaClass mc = inf.classInfo.getStrongMetaClass();
            addSubclassExpandos(arr, mc);
        }

        return arr.toArray(new MetaMethod[arr.size()]);
    }

    private void addSubclassExpandos(List<MetaMethod> arr, MetaClass mc) {
        if (mc != null && mc instanceof ExpandoMetaClass) {
            ExpandoMetaClass emc = (ExpandoMetaClass) mc;
            for (Object mm : emc.getExpandoSubclassMethods()) {
                if (mm instanceof MetaMethod) {
                    MetaMethod method = (MetaMethod) mm;
                    if (method.getDeclaringClass() == this)
                      arr.add(method);
                }
                else {
                    FastArray farr = (FastArray) mm;
                    for (int i = 0; i != farr.size; ++i) {
                        MetaMethod method = (MetaMethod) farr.get(i);
                        if (method.getDeclaringClass() == this)
                          arr.add(method);
                    }
                }
            }
        }
    }

    public void setNewMopMethods(List<MetaMethod> arr) {
        final MetaClass metaClass = classInfo.getStrongMetaClass();
        if (metaClass != null) {
          if (metaClass.getClass() == MetaClassImpl.class) {
              classInfo.setStrongMetaClass(null);
              updateSetNewMopMethods(arr);
              classInfo.setStrongMetaClass(new MetaClassImpl(metaClass.getTheClass()));
              return;
          }

          if (metaClass.getClass() == ExpandoMetaClass.class) {
              classInfo.setStrongMetaClass(null);
              updateSetNewMopMethods(arr);
              ExpandoMetaClass newEmc = new ExpandoMetaClass(metaClass.getTheClass());
              newEmc.initialize();
              classInfo.setStrongMetaClass(newEmc);
              return;
          }

          throw new GroovyRuntimeException("Can't add methods to class " + getTheClass().getName() + ". Strong custom meta class already set.");
        }

        classInfo.setWeakMetaClass(null);
        updateSetNewMopMethods(arr);
    }

    private void updateSetNewMopMethods(List<MetaMethod> arr) {
        if (arr != null) {
            final MetaMethod[] metaMethods = arr.toArray(new MetaMethod[arr.size()]);
            classInfo.dgmMetaMethods = metaMethods;
            classInfo.newMetaMethods = metaMethods;
        }
        else
            classInfo.newMetaMethods = classInfo.dgmMetaMethods;
    }

    public void addNewMopMethods(List<MetaMethod> arr) {
        final MetaClass metaClass = classInfo.getStrongMetaClass();
        if (metaClass != null) {
          if (metaClass.getClass() == MetaClassImpl.class) {
              classInfo.setStrongMetaClass(null);
              List<MetaMethod> res = new ArrayList<MetaMethod>();
              Collections.addAll(res, classInfo.newMetaMethods);
              res.addAll(arr);
              updateSetNewMopMethods(res);
              MetaClassImpl answer = new MetaClassImpl(((MetaClassImpl)metaClass).getRegistry(),metaClass.getTheClass());
              answer.initialize();
              classInfo.setStrongMetaClass(answer);
              return;
          }

          if (metaClass.getClass() == ExpandoMetaClass.class) {
              ExpandoMetaClass emc = (ExpandoMetaClass)metaClass;
              classInfo.setStrongMetaClass(null);
              updateAddNewMopMethods(arr);
              ExpandoMetaClass newEmc = new ExpandoMetaClass(metaClass.getTheClass());
              for (MetaMethod mm : emc.getExpandoMethods()) {
                  newEmc.registerInstanceMethod(mm);
              }
              newEmc.initialize();
              classInfo.setStrongMetaClass(newEmc);
              return;
          }

          throw new GroovyRuntimeException("Can't add methods to class " + getTheClass().getName() + ". Strong custom meta class already set.");
        }

        classInfo.setWeakMetaClass(null);

        updateAddNewMopMethods(arr);
    }

    private void updateAddNewMopMethods(List<MetaMethod> arr) {
        List<MetaMethod> res = new ArrayList<MetaMethod>();
        res.addAll(Arrays.asList(classInfo.newMetaMethods));
        res.addAll(arr);
        classInfo.newMetaMethods = res.toArray(new MetaMethod[res.size()]);
    }

    public boolean isAssignableFrom(Class argument) {
        return argument == null || ReflectionCache.isAssignableFrom(getTheClass(), argument);
    }

    public boolean isDirectlyAssignable(Object argument) {
        return ReflectionCache.isAssignableFrom(getTheClass(), argument.getClass());
    }

    public CallSiteClassLoader getCallSiteLoader() {
        return callSiteClassLoader.get();
    }

    public Collection<ClassInfo> getHierarchy() {
        return hierarchy.get();
    }

    public static class CachedMethodComparatorByName implements Comparator {
        public static final Comparator INSTANCE = new CachedMethodComparatorByName();

        public int compare(Object o1, Object o2) {
              return ((CachedMethod)o1).getName().compareTo(((CachedMethod)o2).getName());
        }
    }

    public static class CachedMethodComparatorWithString implements Comparator {
        public static final Comparator INSTANCE = new CachedMethodComparatorWithString();

        public int compare(Object o1, Object o2) {
            if (o1 instanceof CachedMethod)
              return ((CachedMethod)o1).getName().compareTo((String)o2);
            else
              return ((String)o1).compareTo(((CachedMethod)o2).getName());
        }
    }

    public String toString() {
        return cachedClass.toString();
    }

    /**
     * compatibility method
     * @return this
     */
    public CachedClass getCachedClass () {
        return this;
    }
}
