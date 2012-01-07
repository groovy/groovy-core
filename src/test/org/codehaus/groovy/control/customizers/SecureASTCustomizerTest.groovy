/*
 * Copyright 2003-2011 the original author or authors.
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

package org.codehaus.groovy.control.customizers

import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.syntax.Types

/**
 * Tests for the {@link SecureASTCustomizer} class.
 */
class SecureASTCustomizerTest extends GroovyTestCase {
    CompilerConfiguration configuration
    SecureASTCustomizer customizer

    void setUp() {
        configuration = new CompilerConfiguration()
        customizer = new SecureASTCustomizer()
        configuration.addCompilationCustomizers(customizer)
    }

    private boolean hasSecurityException(Closure closure) {
        boolean result = false;
        try {
            closure()
        } catch (SecurityException e) {
            result = true
        } catch (MultipleCompilationErrorsException e) {
            result = e.errorCollector.errors.any {it.cause?.class == SecurityException }
        }

        result
    }

    void testPackageDefinition() {
        def shell = new GroovyShell(configuration)
        String script = """
            package dummy
            class A {
            }
            new A()
        """
        shell.evaluate(script)
        // no error means success
        customizer.packageAllowed = false
        assert hasSecurityException {
            shell.evaluate(script)
        }
    }

    void testMethodDefinition() {
        def shell = new GroovyShell(configuration)
        String script = """
            def method() {
                true
            }
            method()
        """
        shell.evaluate(script)
        // no error means success
        customizer.methodDefinitionAllowed = false
        assert hasSecurityException {
            shell.evaluate(script)
        }
    }

    void testExpressionWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.expressionsWhitelist = [BinaryExpression, ConstantExpression]
        shell.evaluate('1+1')
        assert hasSecurityException {
            shell.evaluate("""
                class A {}
                new A()
            """)
        }
    }

    void testExpressionBlackList() {
        def shell = new GroovyShell(configuration)
        customizer.expressionsBlacklist = [MethodCallExpression]
        shell.evaluate('1+1')
        assert hasSecurityException {
            shell.evaluate("""
                1+1
                if (1+1==2) {
                    "test".length()
                }
            """)
        }
    }

    void testTokenWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.tokensWhitelist = [Types.PLUS, Types.MINUS]
        shell.evaluate('1+1;1-1')
        assert hasSecurityException {
            shell.evaluate("""
                if (i==2) println 'ok'
            """)
        }
    }

    void testTokenBlackList() {
        def shell = new GroovyShell(configuration)
        customizer.tokensBlacklist = [Types.PLUS_PLUS]
        shell.evaluate('1+1;1-1')
        assert hasSecurityException {
            shell.evaluate("""
                i++
            """)
        }
    }

    void testImportWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.importsWhitelist = ['java.util.ArrayList']
        shell.evaluate("""
            import java.util.ArrayList
            new ArrayList()
        """)
        assert hasSecurityException {
            shell.evaluate("""
            import java.util.LinkedList
            new LinkedList()
        """)
        }
    }

    void testStarImportWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.starImportsWhitelist = ['java.util.*']
        shell.evaluate("""
            import java.util.ArrayList
            new ArrayList()
        """)
        assert hasSecurityException {
            shell.evaluate("""
            import java.util.concurrent.atomic.AtomicInteger
            new AtomicInteger(0)
        """)
        }
        assert hasSecurityException {
            shell.evaluate("""
            import java.util.*
            import java.util.concurrent.atomic.*
            new ArrayList()
            new AtomicInteger(0)
        """)
        }
    }

    void testStarImportWhiteListWithImportWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.importsWhitelist = ['java.util.concurrent.atomic.AtomicInteger']
        customizer.starImportsWhitelist = ['java.util.*']
        shell.evaluate("""
            import java.util.ArrayList
            new ArrayList()
        """)
        shell.evaluate("""
            import java.util.concurrent.atomic.AtomicInteger
            new AtomicInteger(0)
        """)
        assert hasSecurityException {
            shell.evaluate("""
            import java.util.concurrent.atomic.AtomicBoolean
            new AtomicBoolean(false)
        """)
        }
    }

    void testImportBlackList() {
        def shell = new GroovyShell(configuration)
        customizer.importsBlacklist = ['java.util.LinkedList']
        shell.evaluate("""
            import java.util.ArrayList
            new ArrayList()
        """)
        assert hasSecurityException {
            shell.evaluate("""
            import java.util.LinkedList
            new LinkedList()
        """)
        }
    }

    void testStarImportBlackListWithImportBlackList() {
        def shell = new GroovyShell(configuration)
        customizer.importsBlacklist = ['java.util.concurrent.atomic.AtomicBoolean']
        customizer.starImportsBlacklist = ['java.util.*']
        assert hasSecurityException {
            shell.evaluate("""
                 import java.util.ArrayList
                 new ArrayList()
             """)
        }
        shell.evaluate("""
             import java.util.concurrent.atomic.AtomicInteger
             new AtomicInteger(0)
         """)
        assert hasSecurityException {
            shell.evaluate("""
             import java.util.concurrent.atomic.AtomicBoolean
             new AtomicBoolean(false)
         """)
        }
    }


    void testIndirectImportWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.importsWhitelist = ['java.util.ArrayList']
        customizer.indirectImportCheckEnabled = true
        shell.evaluate("""
            import java.util.ArrayList
            new ArrayList()
        """)
        assert hasSecurityException {
            shell.evaluate("""            
            new java.util.LinkedList()
        """)

            assert hasSecurityException {
                shell.evaluate("""
            return java.util.LinkedList.&size
        """)
            }
        }
    }

    void testIndirectStarImportWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.starImportsWhitelist = ['java.util.*']
        customizer.indirectImportCheckEnabled = true
        shell.evaluate("""
            import java.util.ArrayList
            new ArrayList()
        """)
        shell.evaluate("""
            new java.util.ArrayList()
        """)
        assert hasSecurityException {
            shell.evaluate("""
            new java.util.concurrent.atomic.AtomicBoolean(false)
        """)

            assert hasSecurityException {
                shell.evaluate("""
            return java.util.concurrent.atomic.AtomicBoolean.&get
        """)
            }
        }
    }

    void testStaticImportWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.staticImportsWhitelist = ['java.lang.Math.PI']
        shell.evaluate("""
            import static java.lang.Math.PI
            PI
        """)
        assert hasSecurityException {
            shell.evaluate("""
            import static java.lang.Math.PI
            import static java.lang.Math.cos
            cos(PI)
        """)
        }
    }

    void testStaticStarImportWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.staticStarImportsWhitelist = ['java.lang.Math.*']
        shell.evaluate("""
            import static java.lang.Math.PI
            import static java.lang.Math.cos
            cos(PI)
        """)
        assert hasSecurityException {
            shell.evaluate("""
            import static java.util.Collections.*
            sort([5,4,2])
        """)
        }
    }

    void testIndirectStaticImport() {
        def shell = new GroovyShell(configuration)
        customizer.staticImportsWhitelist = ['java.lang.Math.PI']
        customizer.indirectImportCheckEnabled = true
        assert hasSecurityException {shell.evaluate('java.lang.Math.cos(1)')}
    }

    void testIndirectStaticStarImport() {
        def shell = new GroovyShell(configuration)
        customizer.staticStarImportsWhitelist = ['java.lang.Math.*']
        customizer.indirectImportCheckEnabled = true
        shell.evaluate('java.lang.Math.cos(1)')
        assert hasSecurityException {shell.evaluate('java.util.Collections.unmodifiableList([1])')}
    }

    void testConstantTypeWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.constantTypesClassesWhiteList = [Integer.TYPE]
        shell.evaluate('1')
        assert hasSecurityException {shell.evaluate('"string"')}
        assert hasSecurityException {shell.evaluate('2d')}
    }

    void testConstantTypeBlackList() {
        def shell = new GroovyShell(configuration)
        customizer.constantTypesClassesBlackList = [String]
        shell.evaluate('1')
        shell.evaluate('2d')
        assert hasSecurityException {shell.evaluate('"string"')}
    }

    void testReceiverWhiteList() {
        def shell = new GroovyShell(configuration)
        customizer.receiversClassesWhiteList = [Integer.TYPE]
        shell.evaluate('1.plus(1)')
        assert hasSecurityException {shell.evaluate('"string".toUpperCase()')}
        assert hasSecurityException {shell.evaluate('2.0.multiply(4)')}
    }

    void testReceiverBlackList() {
        def shell = new GroovyShell(configuration)
        customizer.receiversClassesBlackList = [String]
        shell.evaluate('1.plus(1)')
        shell.evaluate('2.0.multiply(4)')
        assert hasSecurityException {shell.evaluate('"string".toUpperCase()')}
    }

    void testReceiverWhiteListWithStaticMethod() {
        def shell = new GroovyShell(configuration)
        customizer.receiversClassesWhiteList = [Integer.TYPE]
        shell.evaluate('1.plus(1)')
        assert hasSecurityException {shell.evaluate('java.lang.Math.cos(2)')}
    }

    void testReceiverBlackListWithStaticMethod() {
        def shell = new GroovyShell(configuration)
        customizer.receiversClassesBlackList = [Math]
        shell.evaluate('1.plus(1)')
        shell.evaluate('Collections.sort([])')
        assert hasSecurityException {shell.evaluate('java.lang.Math.cos(2)')}
    }

    // Testcase for GROOVY-4978
    void testVisitMethodBody() {
        def shell = new GroovyShell(configuration)
        customizer.importsBlacklist = [
                "java.lang.System",
                "groovy.lang.GroovyShell",
                "groovy.lang.GroovyClassLoader"]
        customizer.indirectImportCheckEnabled = true

        assert hasSecurityException {shell.evaluate('System.println(1)')}
        assert hasSecurityException {shell.evaluate('def x() { System.println(1) }')}
    }
}
