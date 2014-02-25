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
package org.codehaus.groovy.runtime.metaclass;

import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.MixinInMetaClass;
import org.codehaus.groovy.reflection.ReflectionCache;

import java.lang.reflect.Modifier;

/**
 * MetaProperty for mixed in classes
 */
public class MixinInstanceMetaProperty extends MetaBeanProperty {
    public MixinInstanceMetaProperty(MetaProperty property, MixinInMetaClass mixinInMetaClass) {
        super(property.getName(), property.getType(), createGetter(property, mixinInMetaClass), createSetter(property, mixinInMetaClass));
    }

    private static MetaMethod createSetter(final MetaProperty property, final MixinInMetaClass mixinInMetaClass) {
      return new MetaMethod() {
          final String name = getSetterName(property.getName());
          {
              setParametersTypes (new CachedClass [] {ReflectionCache.getCachedClass(property.getType())} );
          }

          public int getModifiers() {
              return Modifier.PUBLIC;
          }

          public String getName() {
              return name;
          }

          public Class getReturnType() {
              return property.getType();
          }

          public CachedClass getDeclaringClass() {
              return mixinInMetaClass.getInstanceClass();
          }

          public Object invoke(Object object, Object[] arguments) {
              property.setProperty(mixinInMetaClass.getMixinInstance(object), arguments[0]);
              return null;
          }
      };
    }

    private static MetaMethod createGetter(final MetaProperty property, final MixinInMetaClass mixinInMetaClass) {
        return new MetaMethod() {
            final String name = getGetterName(property.getName(), property.getType());
            {
                setParametersTypes (new CachedClass [0]);
            }

            public int getModifiers() {
                return Modifier.PUBLIC;
            }

            public String getName() {
                return name;
            }

            public Class getReturnType() {
                return property.getType();
            }

            public CachedClass getDeclaringClass() {
                return mixinInMetaClass.getInstanceClass();
            }

            public Object invoke(Object object, Object[] arguments) {
                return property.getProperty(mixinInMetaClass.getMixinInstance(object));
            }
        };
    }
}
