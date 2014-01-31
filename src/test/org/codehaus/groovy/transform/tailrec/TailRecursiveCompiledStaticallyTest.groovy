package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.control.CompilationFailedException

/**
 * @author Johannes Link
 */
class TailRecursiveCompiledStaticallyTest extends GroovyShellTestCase {

    void testStaticallyCompiledRecursiveMethod() {
        def target = evaluate("""
            import groovy.transform.TailRecursive
            import groovy.transform.CompileStatic

            @CompileStatic
            class TargetClass {
            	@TailRecursive
            	static int staticCountDown(int zahl) {
            		if (zahl == 0)
            			return zahl
            		return staticCountDown(zahl - 1)
            	}
            }
            new TargetClass()
        """)
        assert target.staticCountDown(5) == 0
        assert target.staticCountDown(100000) == 0
    }

    void testTypeCheckedRecursiveMethod() {
        def target = evaluate('''
            import groovy.transform.TailRecursive
            import groovy.transform.TypeChecked

            @TypeChecked
            class TargetClass {
				@TailRecursive
				String fillString(long number, String filled) {
					if (number == 0)
						return filled;
					fillString(number - 1, filled + "+")
				}
        	}
            new TargetClass()
		''')

        assert target.fillString(0, "") == ""
        assert target.fillString(1, "") == "+"
        assert target.fillString(5, "") == "+++++"
        assert target.fillString(10000, "") == "+" * 10000
    }

    void testStaticallyCompiledSumDown() {
        def target = evaluate('''
            import groovy.transform.TailRecursive
            import groovy.transform.CompileStatic

            @CompileStatic
            class TargetClass {
				@TailRecursive
                long sumDown(long number, long sum = 0) {
                    (number == 0) ? sum : sumDown(number - 1, sum + number)
                }
        	}
            new TargetClass()
		''')

        assert target.sumDown(0) == 0
        assert target.sumDown(5) == 5 + 4 + 3 + 2 + 1
        assert target.sumDown(100) == 5050
        assert target.sumDown(1000000) == 500000500000
    }

    void testStaticallyCompiledRecursiveFunctionWithTwoParameters() {
        def target = evaluate('''
            import groovy.transform.TailRecursive
            import groovy.transform.CompileStatic

            @CompileStatic
            class TargetClass {
				@TailRecursive
				String fillString(long number, String filled) {
					if (number == 0)
						return filled;
					fillString(number - 1, filled + "+")
				}
        	}
            new TargetClass()
		''')

        assert target.fillString(0, "") == ""
        assert target.fillString(1, "") == "+"
        assert target.fillString(5, "") == "+++++"
        assert target.fillString(10000, "") == "+" * 10000
    }

}
