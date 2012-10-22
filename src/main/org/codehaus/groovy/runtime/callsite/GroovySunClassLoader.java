/*
 * Copyright 2003-2009 the original author or authors.
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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.codehaus.groovy.reflection.SunClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class GroovySunClassLoader extends SunClassLoader {

    public static final SunClassLoader sunVM;

    static {
            sunVM = AccessController.doPrivileged(new PrivilegedAction<SunClassLoader>() {
                public SunClassLoader run() {
                    try {
                        if (SunClassLoader.sunVM != null) {
                            return new GroovySunClassLoader();
                        }
                    }
                    catch (Throwable t) {//
                    }
                    return null;
                }
            });
    }

    protected GroovySunClassLoader () throws Throwable {
        super();
        loadAbstract ();
        loadFromRes("org.codehaus.groovy.runtime.callsite.MetaClassSite");
        loadFromRes("org.codehaus.groovy.runtime.callsite.MetaMethodSite");
        loadFromRes("org.codehaus.groovy.runtime.callsite.PogoMetaMethodSite");
        loadFromRes("org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite");
        loadFromRes("org.codehaus.groovy.runtime.callsite.StaticMetaMethodSite");
    }

    private void loadAbstract() throws IOException {
        final InputStream asStream = GroovySunClassLoader.class.getClass().getClassLoader().getResourceAsStream(resName("org.codehaus.groovy.runtime.callsite.AbstractCallSite"));
        ClassReader reader = new ClassReader(asStream);
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final ClassVisitor cv = new ClassVisitor(4, cw) {
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, "sun/reflect/GroovyMagic", interfaces);
            }            
        };
        reader.accept(cv, ClassWriter.COMPUTE_MAXS);
        asStream.close();
        define(cw.toByteArray(), "org.codehaus.groovy.runtime.callsite.AbstractCallSite");
    }

}
