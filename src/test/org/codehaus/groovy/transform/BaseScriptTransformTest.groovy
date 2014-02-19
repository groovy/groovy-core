/*
 * Copyright 2008-2013 the original author or authors.
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

import org.codehaus.groovy.control.CompilerConfiguration;

import gls.CompilableTestSupport

/**
 * @author Vladimir Orany
 * @author Paul King
 * @author Cedric Champeau
 */
class BaseScriptTransformTest extends CompilableTestSupport {

    void testInheritsFromCustomScript() {
        assertScript """
            abstract class CustomScript extends Script {}
  
            @groovy.transform.BaseScript CustomScript baseScript
            assert this.class.superclass == CustomScript
        """
    }

    void testBaseScriptMustExtendsScript() {
        shouldNotCompile """
            abstract class CustomScript {}
  
            @groovy.transform.BaseScript CustomScript baseScript
        """
    }

    void testThisObjectIsAssignedToBaseScriptVariable() {
        assertScript """
            abstract class CustomScript extends Script {}
  
            @groovy.transform.BaseScript CustomScript baseScript
            assert this == baseScript
        """
    }

    void testNotAllowedForClassFields() {
        shouldNotCompile """
            import groovy.transform.*

            abstract class CustomScript extends Script {}

            class Inner {
                @BaseScript CustomScript baseScript
            }
        """
    }

    void testNotAllowedForScriptInnerClassFields() {
        shouldNotCompile """
            import groovy.transform.*

            abstract class CustomScript extends Script {}

            class Inner {
                @BaseScript CustomScript baseScript
            }
            println Inner.class.name
        """
    }

    void testNotAllowedInClassMethods() {
        shouldNotCompile """
            import groovy.transform.*

            abstract class CustomScript extends Script {}

            class Inner {
                def bar() {
                    @BaseScript CustomScript baseScript
                }
            }
        """
    }

    void testNotAllowedInScriptInnerClassMethods() {
        shouldNotCompile """
            import groovy.transform.*

            abstract class CustomScript extends Script {}

            class Inner {
                def bar() {
                    @BaseScript CustomScript baseScript
                }
            }
            println Inner.class.name
        """
    }

    void testBaseScriptFromCompiler(){
        CompilerConfiguration config = new CompilerConfiguration()
        config.scriptBaseClass = MyCustomScript.name
        GroovyShell shell = new GroovyShell(config)
        
        shell.evaluate('''
            abstract class DeclaredBaseScript extends Script {
                int meaningOfLife = 42
            }
        
            @groovy.transform.BaseScript DeclaredBaseScript baseScript

            assert meaningOfLife == 42
        ''')
    }

    void testBaseScriptAbstractMethod() {
        // http://jira.codehaus.org/browse/GROOVY-6585
        CompilerConfiguration config = new CompilerConfiguration()
        config.scriptBaseClass = MyCustomScript.name
        GroovyShell shell = new GroovyShell(config)

        def answer = shell.evaluate('''
            abstract class DeclaredBaseScript extends Script {
                int _meaningOfLife = 0
                int getMeaningOfLife() { _meaningOfLife }
                void setMeaningOfLife(int v) { _meaningOfLife = v }

                abstract def runScript()

                def preRun() { meaningOfLife |= 2 }
                def postRun() { meaningOfLife |= 8 }
                def run() {
                   preRun()
                   runScript()
                   postRun()
                   assert meaningOfLife == 42
                   meaningOfLife
                }
            }

            @groovy.transform.BaseScript DeclaredBaseScript baseScript

            meaningOfLife |= 32
            assert meaningOfLife == 34
        ''')

        assert answer == 42
    }

    void testBaseScriptImplementsRunMethod() {
        def result = new GroovyShell().evaluate('''
            class DeclaredBaseScript extends Script {
                boolean iBeenRun
                def run() { iBeenRun = true }
            }

            @groovy.transform.BaseScript DeclaredBaseScript baseScript

            assert !iBeenRun

            super.run()

            assert iBeenRun

            iBeenRun
        ''')

        assert result
    }

}

abstract class MyCustomScript extends Script {}