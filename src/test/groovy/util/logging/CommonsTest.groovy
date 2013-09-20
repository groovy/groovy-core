package groovy.util.logging

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Unit test for the commons logging @Log based annotation.
 *
 * @author Hamlet D'Arcy
 * @author Matthias Cullmann
 * 
 */
class CommonsTest extends GroovyTestCase {

    public void testPrivateFinalStaticLogFieldAppears() {

        Class clazz = new GroovyClassLoader().parseClass('''
              @groovy.util.logging.Commons
              class MyClass {
              } ''')

        assert clazz.declaredFields.find { Field field ->
            field.name == "log" &&
                    Modifier.isPrivate(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isTransient(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers())
        }
    }

    public void testPrivateFinalStaticNamedLogFieldAppears() {

        Class clazz = new GroovyClassLoader().parseClass('''
              @groovy.util.logging.Commons('logger')
              class MyClass {
              } ''')

        assert clazz.declaredFields.find { Field field ->
            field.name == "logger" &&
                    Modifier.isPrivate(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isTransient(field.getModifiers()) &&
                    Modifier.isFinal(field.getModifiers())
        }
    }

    public void testClassAlreadyHasLogField() {

        shouldFail {

            Class clazz = new GroovyClassLoader().parseClass('''
                @groovy.util.logging.Commons
                class MyClass {
                    String log
                } ''')

            assert clazz.newInstance()
        }
    }

    public void testClassAlreadyHasNamedLogField() {

        shouldFail {

            Class clazz = new GroovyClassLoader().parseClass('''
                @groovy.util.logging.Commons('logger')
                class MyClass {
                    String logger
                } ''')

            assert clazz.newInstance()
        }
    }

    /**
     * This test output must be observed manually.
     * There is unfortunately no good way to add an appender to Commons Logging.
     */
    public void testLogInfo_IntegrationTest() {

        Class clazz = new GroovyClassLoader().parseClass('''
            @groovy.util.logging.Commons
            class MyClass {

                def loggingMethod() {
                    log.error ("error called")
                    log.warn  ("warn called")
                    log.info  ("info called")
                    log.debug ("debug called")
                }
            }
            new MyClass().loggingMethod() ''')

        Script s = (Script) clazz.newInstance()
        s.run()
    }

    void testLogFromStaticMethods() {
        Class clazz = new GroovyClassLoader().parseClass("""
            @groovy.util.logging.Commons
            class MyClass {
                static loggingMethod() {
                  log.info   ("(static) info called")
                }
            }
            MyClass.loggingMethod()""")

        Script s = (Script) clazz.newInstance()
        s.run()
    }


    /**
     * This test output must be observed manually.
     * There is unfortunately no good way to add an appender to Commons Logging.
     */
    public void testNamedLogInfo_IntegrationTest() {

        Class clazz = new GroovyClassLoader().parseClass('''
            @groovy.util.logging.Commons('logger')
            class MyClass {

                def loggingMethod() {
                    logger.error ("error called")
                    logger.warn  ("warn called")
                    logger.info  ("info called")
                    logger.debug ("debug called")
                }
            }
            new MyClass().loggingMethod() ''')

        Script s = (Script) clazz.newInstance()
        s.run()
    }

    public void testLogGuards() {
        Class clazz = new GroovyClassLoader().parseClass('''

            class LogDecorator extends groovy.util.Proxy {
                boolean isTraceEnabled() { false }
            }

            @groovy.util.logging.Commons
            class MyClass {
                boolean traceCalled = false
                MyClass() {
                    log = new LogDecorator().wrap(log) as org.apache.commons.logging.Log
                }

                def loggingMethod() {
                    log.trace (traceCalled = true)
                }
            }
            def o = new MyClass()
            o.loggingMethod()
            o.traceCalled''')

        Script s = (Script) clazz.newInstance()
        def result = s.run()
        assert !result
    }

    void testDefaultCategory() {
        PrintStream savedSystemOut = System.out
        try {
            ByteArrayOutputStream redirectedSystemOut = new ByteArrayOutputStream()
            System.out = new PrintStream(redirectedSystemOut)

            Class clazz = new GroovyClassLoader().parseClass("""
            @groovy.util.logging.Commons
            class MyClass {
                static loggingMethod() {
                  log.error("error called")
                }
            }""")
            def s = clazz.newInstance()

            s.loggingMethod()

            assert redirectedSystemOut.toString().contains('MyClass')
        }
        finally {
            System.out = savedSystemOut
        }
    }

    public void testCustomCategory() {
        PrintStream savedSystemOut = System.out
        try {
            ByteArrayOutputStream redirectedSystemOut = new ByteArrayOutputStream()
            System.out = new PrintStream(redirectedSystemOut)

            Class clazz = new GroovyClassLoader().parseClass("""
            @groovy.util.logging.Commons(category='customCategory')
            class MyClass {
                static loggingMethod() {
                  log.error("error called")
                }
            }""")
            def s = clazz.newInstance()

            s.loggingMethod()

            assert redirectedSystemOut.toString().contains('customCategory')
        }
        finally {
            System.out = savedSystemOut
        }
    }
}
