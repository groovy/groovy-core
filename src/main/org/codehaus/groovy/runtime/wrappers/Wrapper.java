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

package org.codehaus.groovy.runtime.wrappers;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

/**
 * @author John Wilson
 */
public abstract class Wrapper implements GroovyObject {
    protected final Class<?> constrainedType;

    public Wrapper(final Class<?> constrainedType) {
        this.constrainedType = constrainedType;
    }

    /* (non-Javadoc)
    * @see groovy.lang.GroovyObject#getMetaClass()
    */
    public MetaClass getMetaClass() {
        return getDelegatedMetaClass();
    }

    public Class<?> getType() {
        return this.constrainedType;
    }

    public abstract Object unwrap();

    protected abstract Object getWrapped();

    protected abstract MetaClass getDelegatedMetaClass();
}
