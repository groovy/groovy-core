/*
 * $Id$
 *
 * Copyright (c) 2005 The Codehaus - http://groovy.codehaus.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */


package org.codehaus.groovy.control;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;

public class CompilationUnitTest extends MockObjectTestCase {

    public void testAppendsTheClasspathOfTheCompilerConfigurationToCurrentClassLoaderWhenInstantiated() {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setClasspath(System.getProperty("java.class.path"));
        // disabled until checked with fraz
        //new CompilationUnit(configuration, null, createGroovyClassLoaderWithExpectations(configuration));
    }

    public void testClassNodeSerializationAndInjectionIntoScriptASTPriorToCompilation() throws Exception {
        // Compile a regular class
        CompilationUnit classUnit = new CompilationUnit();
        classUnit.addSource("example.HelloWorld", "package example; class HelloWorld { def sayIt() { 'Hello, World!' } }");
        classUnit.compile(Phases.CLASS_GENERATION);

        // Serialize the generated class node
        ClassNode classNode = classUnit.getClassNode("example.HelloWorld");
        byte[] serializedClassNode = serialize(classNode);
        assertNotNull(serializedClassNode);

        // Ensure deserialization works
        ClassNode deserializedClassNode = (ClassNode) deserialize(serializedClassNode);
        assertNotNull(deserializedClassNode);
        assertEquals(classNode.getName(), deserializedClassNode.getName());

        // Add the deserialized class node into a script's AST so it compiles
        CompilationUnit scriptUnit = new CompilationUnit();
        scriptUnit.addSource("HelloWorldScript", "new example.HelloWorld().sayIt()");
        scriptUnit.getAST().addClass(deserializedClassNode);
        scriptUnit.compile(Phases.CLASS_GENERATION);
    }

    private GroovyClassLoader createGroovyClassLoaderWithExpectations(CompilerConfiguration configuration) {
        Mock mockGroovyClassLoader = mock(GroovyClassLoader.class);
        for (Iterator iterator = configuration.getClasspath().iterator(); iterator.hasNext();) {
            mockGroovyClassLoader.expects(once()).method("addClasspath").with(eq(iterator.next()));
        }
        return (GroovyClassLoader) mockGroovyClassLoader.proxy();
    }

    private static byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(baos);
            out.writeObject(obj);
        }
        finally {
            try {
              if (out != null) {
                out.close();
              }
            }
            catch (IOException ex) {
                // ignore close exception
            }
        }
        return baos.toByteArray();
    }

    public static Object deserialize(byte[] objectData) throws ClassNotFoundException, IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(bais);
            return in.readObject();
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (IOException ex) {
                // ignore close exception
            }
        }
    }
}
