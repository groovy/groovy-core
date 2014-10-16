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
package org.codehaus.groovy.vmplugin;

import org.codehaus.groovy.ast.*;

import java.lang.reflect.Method;

/**
 * Interface to access VM version based actions.
 * This interface is for internal use only!
 * 
 * @author Jochen Theodorou
 */
public interface VMPlugin {
    void setAdditionalClassInformation(ClassNode c);
    Class[] getPluginDefaultGroovyMethods();
    Class[] getPluginStaticGroovyMethods();
    void configureAnnotation(AnnotationNode an);
    void configureClassNode(CompileUnit compileUnit, ClassNode classNode);
    void invalidateCallSites();
    /**
     * Returns a handle with bound receiver to invokeSpecial the given method.
     * This method will require at least Java 7, but since the source has to compile
     * on older Java versions as well it is not marked to return a MethodHandle and
     * uses Object instead
     * @return  null in case of jdk<7, otherwise a handel that takes the method call
     *          arguments for the invokespecial call
     */
    Object getInvokeSpecialHandle(Method m, Object receiver);

    /**
     * Invokes a handle produced by #getInvokeSpecialdHandle
     * @param handle the handle
     * @param args arguments for the method call, can be empty but not null
     * @return the result of the method call
     */
    Object invokeHandle(Object handle, Object[] args) throws Throwable;

    /**
     * Gives the version the plguin is made for
     * @return 5 for jdk5, 6 for jdk6, 7 for jdk7 or higher
     */
    int getVersion();

}
