/*
 * Copyright 2003-2008 the original author or authors.
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
package org.codehaus.groovy.transform

/**
 * @author Alex Tkachman
 */
class SingletonTransformTest extends GroovyShellTestCase {

    void testSingleton() {
        def res = evaluate("""
              @Singleton
              class X {
                 def getHello () {
                   "Hello, World!"
                 }
              }

              X.instance.hello
        """)

        assertEquals("Hello, World!", res)
    }

    void testLazySingleton() {
        def res = evaluate("""
              @Singleton(lazy=true)
              class X {
                 def getHello () {
                   "Hello, World!"
                 }
              }

              assert X.@instance == null

              X.instance.hello
        """)

        assertEquals("Hello, World!", res)
    }

    void testSingletonInstantiationFails() {
        shouldFail {
            evaluate("""
                  @Singleton
                  class X {
                     def getHello () {
                       "Hello, World!"
                     }
                  }

                  new X ()
            """)
        }
    }

    void testSingletonOverideConstructorFails() {
            def res = evaluate("""
                  @Singleton
                  class X {
                     static hello = "Bye-bye world"

                     X () {
                        hello = "Hello, World!"
                     }
                  }

                  X.instance.hello
            """)

            assertEquals("Hello, World!", res)
    }
    
    void testSingletonCustomPropertyName() {
        def propertyName = 'myProp'
        def getterName = 'getMyProp'
        def className = 'X'
        def defaultPropertyName = 'instance'

        def invoker = new GroovyClassLoader()
        def clazz = invoker.parseClass("""
            @Singleton(property ='$propertyName')
            public class $className {
            } """);
        def modifiers = clazz.getDeclaredField(propertyName).modifiers //should be public final static for non-lazy singleton
        assert isPublic(modifiers) && isFinal(modifiers) && isStatic(modifiers)
        def object = clazz.getMethod(getterName).invoke(null);
        assertEquals className, object.class.name;
        try {
            clazz.newInstance() //should throw exception here
            fail() //shouldn't get here
        } catch (RuntimeException e) {//for tests run in Groovy (which can access privates)
            assert e.message.contains(propertyName)
        }
        try {
            clazz.getField(defaultPropertyName) //should throw exception here
            fail() //shouldn't get here
        } catch (NoSuchFieldException e) {
            assert e.message.contains(defaultPropertyName)
        }
    }

}