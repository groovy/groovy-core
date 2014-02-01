/*
 * Copyright 2003-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class LogImprovementsASTTransformsTest extends GroovyTestCase {

    // Log4j2 requires at least Java 1.6
    static final boolean testEnabled = true
    static {
        if (System.getProperty("java.version").startsWith("1.5.")) {
            testEnabled = false
        }
        if (System.getProperty('groovy.target.indy') && System.getProperty("java.version").startsWith("1.7.")) {
            // temporarily disable tests for indy if running on JDK 7 because of a bug in Log4j2
            // todo: re-enable when Log4J2 beta10 is out
            testEnabled = false
        }
    }

    void testLogASTTransformation() {
        assertScript '''
// tag::log_spec[]
@groovy.util.logging.Log
class Greeter {
    void greet() {
        log.info 'Called greeter'
        println 'Hello, world!'
    }
}
// end::log_spec[]
def g = new Greeter()
g.greet()
        '''
        assertScript '''
// tag::log_equiv[]
import java.util.logging.Level
import java.util.logging.Logger

class Greeter {
    private final static Logger log = Logger.getLogger(Greeter.name)
    void greet() {
        if (log.isLoggable(Level.INFO)) {
            log.info 'Called greeter'
        }
        println 'Hello, world!'
    }
}
// end::log_equiv[]
def g = new Greeter()
g.greet()

'''
    }

    void testCommonsASTTransformation() {
        assertScript '''
// tag::commons_spec[]
@groovy.util.logging.Commons
class Greeter {
    void greet() {
        log.debug 'Called greeter'
        println 'Hello, world!'
    }
}
// end::commons_spec[]
def g = new Greeter()
g.greet()
        '''
        assertScript '''
// tag::commons_equiv[]
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

class Greeter {
    private final static Log log = LogFactory.getLog(Greeter)
    void greet() {
        if (log.isDebugEnabled()) {
            log.debug 'Called greeter'
        }
        println 'Hello, world!'
    }
}
// end::commons_equiv[]
def g = new Greeter()
g.greet()
'''
    }

    void testLog4jASTTransformation() {
        assertScript '''
// tag::log4j_spec[]
@groovy.util.logging.Log4j
class Greeter {
    void greet() {
        log.debug 'Called greeter'
        println 'Hello, world!'
    }
}
// end::log4j_spec[]
def g = new Greeter()
g.greet()
        '''
        assertScript '''
// tag::log4j_equiv[]
import org.apache.log4j.Logger

class Greeter {
    private final static Logger log = Logger.getLogger(Greeter)
    void greet() {
        if (log.isDebugEnabled()) {
            log.debug 'Called greeter'
        }
        println 'Hello, world!'
    }
}
// end::log4j_equiv[]
def g = new Greeter()
g.greet()
'''
    }

    void testLog4j2ASTTransformation() {
        if (testEnabled) {
            try {
                assertScript '''
    // tag::log4j2_spec[]
    @groovy.util.logging.Log4j2
    class Greeter {
        void greet() {
            log.debug 'Called greeter'
            println 'Hello, world!'
        }
    }
    // end::log4j2_spec[]
    def g = new Greeter()
    g.greet()
            '''

                assertScript '''
    // tag::log4j2_equiv[]
    import org.apache.logging.log4j.LogManager
    import org.apache.logging.log4j.Logger

    class Greeter {
        private final static Logger log = LogManager.getLogger(Greeter)
        void greet() {
            if (log.isDebugEnabled()) {
                log.debug 'Called greeter'
            }
            println 'Hello, world!'
        }
    }
    // end::log4j2_equiv[]
    def g = new Greeter()
    g.greet()
    '''
            } catch (UnsupportedClassVersionError e) {
                // running on older, unsupported, JDK
            }
        }
    }

    void testSlf4jASTTransformation() {
        assertScript '''
// tag::slf4j_spec[]
@groovy.util.logging.Slf4j
class Greeter {
    void greet() {
        log.debug 'Called greeter'
        println 'Hello, world!'
    }
}
// end::slf4j_spec[]
def g = new Greeter()
g.greet()
        '''

        assertScript '''
// tag::slf4j_equiv[]
import org.slf4j.LoggerFactory
import org.slf4j.Logger

class Greeter {
    private final static Logger log = LoggerFactory.getLogger(Greeter)
    void greet() {
        if (log.isDebugEnabled()) {
            log.debug 'Called greeter'
        }
        println 'Hello, world!'
    }
}
// end::slf4j_equiv[]
def g = new Greeter()
g.greet()
'''
    }

}
