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

import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaProperty;

import org.codehaus.groovy.runtime.GroovyCategorySupport;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;

public class GetEffectivePojoPropertySite extends AbstractCallSite {
    private final MetaClassImpl metaClass;
    private final MetaProperty effective;
    private final int version;

    public GetEffectivePojoPropertySite(CallSite site, MetaClassImpl metaClass, MetaProperty effective) {
        super(site);
        this.metaClass = metaClass;
        this.effective = effective;
        version = metaClass.getVersion();
    }

//    public final Object callGetProperty (Object receiver) throws Throwable {
//        if (GroovyCategorySupport.hasCategoryInCurrentThread() || receiver.getClass() != metaClass.getTheClass()) {
//            return createGetPropertySite(receiver).getProperty(receiver);
//        } else {
//            try {
//                return effective.getProperty(receiver);
//            } catch (GroovyRuntimeException gre) {
//                throw ScriptBytecodeAdapter.unwrap(gre);
//            }
//        }
//    }

    public final CallSite acceptGetProperty(Object receiver) {
//        if (GroovyCategorySupport.hasCategoryInCurrentThread() || !(receiver instanceof GroovyObject) || ((GroovyObject)receiver).getMetaClass() != metaClass) {
        if (GroovyCategorySupport.hasCategoryInCurrentThread() || receiver==null || receiver.getClass() != metaClass.getTheClass()
            || version != metaClass.getVersion()) { // metaClass is invalid
            return createGetPropertySite(receiver);
        } else {
            return this;
        }
    }

    public final Object getProperty(Object receiver) throws Throwable {
        try {
            return effective.getProperty(receiver);
        } catch (GroovyRuntimeException gre) {
            throw ScriptBytecodeAdapter.unwrap(gre);
        }
    }
}
