package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.control.CompilationFailedException

/**
 * @author Johannes Link
 */
class TailRecursiveCompilationFailuresTest extends GroovyShellTestCase {

	void testFailIfNotAllRecursiveCallsCanBeTransformed() {
		shouldFail(CompilationFailedException) { evaluate("""
            import groovy.transform.TailRecursive
            class TargetClass {
            	@TailRecursive
            	int aNonTailRecursiveMethod() {
            		return 1 + aNonTailRecursiveMethod() 
            	}
            }
        """) }
	}

	void testFailIfNotAllStaticRecursiveCallsCanBeTransformed() {
		shouldFail(CompilationFailedException) { evaluate("""
            import groovy.transform.TailRecursive
            class TargetClass {
            	@TailRecursive
            	static int aNonTailRecursiveMethod() {
            		return 1 + aNonTailRecursiveMethod() 
            	}
            }
        """) }
	}

    void testFailIfRecursiveMethodCannotBeStaticallyCompiled() {
        shouldFail(CompilationFailedException) { evaluate("""
            import groovy.transform.TailRecursive
            import groovy.transform.CompileStatic

            @CompileStatic
            class TargetClass {
            	@TailRecursive
            	static int staticCountDown(zahl) {
            		if (zahl == 0)
            			return zahl
            		return staticCountDown(zahl - 1)
            	}
            }
            new TargetClass()
        """) }
    }


}
