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

import groovy.lang.MetaMethod;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.MixinInMetaClass;

/**
 * MetaMethod for mixed in classes
 */
public class MixinInstanceMetaMethod extends MetaMethod{
    private final MetaMethod method;
    private final MixinInMetaClass mixinInMetaClass;

    public MixinInstanceMetaMethod(MetaMethod method, MixinInMetaClass mixinInMetaClass) {
        this.method = method;
        this.mixinInMetaClass = mixinInMetaClass;
    }

    public int getModifiers() {
        return method.getModifiers();
    }

    public String getName() {
        return method.getName();
    }

    public Class getReturnType() {
        return method.getReturnType();
    }

    public CachedClass getDeclaringClass() {
        return mixinInMetaClass.getInstanceClass();
    }

    public Object invoke(Object object, Object[] arguments) {
        // make sure parameterTypes gets set
        method.getParameterTypes();
        return method.invoke(mixinInMetaClass.getMixinInstance(object), method.correctArguments(arguments));
    }

    protected Class[] getPT() {
        return method.getNativeParameterTypes();
    }
}
