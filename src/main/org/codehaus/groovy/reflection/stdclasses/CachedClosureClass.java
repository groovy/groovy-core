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
package org.codehaus.groovy.reflection.stdclasses;

import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.reflection.ClassInfo;

public class CachedClosureClass extends CachedClass {
    private final Class[] parameterTypes;
    private final int maximumNumberOfParameters;

    public CachedClosureClass(Class klazz, ClassInfo classInfo) {
        super(klazz, classInfo);

        CachedMethod methods [] = getMethods();

        // set it to -1 for starters so parameterTypes will always get a type
        int maximumNumberOfParameters = -1;
        Class[] parameterTypes = null;

        for (CachedMethod method : methods) {
            if ("doCall".equals(method.getName())) {
                final Class[] pt = method.getNativeParameterTypes();
                if (pt.length > maximumNumberOfParameters) {
                    parameterTypes = pt;
                    maximumNumberOfParameters = parameterTypes.length;
                }
            }
        }
        // this line should be useless, but well, just in case
        maximumNumberOfParameters = Math.max(maximumNumberOfParameters,0);

        this.maximumNumberOfParameters = maximumNumberOfParameters;
        this.parameterTypes = parameterTypes;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public int getMaximumNumberOfParameters() {
        return maximumNumberOfParameters;
    }
}
