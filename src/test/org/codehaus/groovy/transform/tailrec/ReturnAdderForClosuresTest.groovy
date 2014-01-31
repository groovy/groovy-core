package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstAssert
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.junit.Before
import org.junit.Test

/**
 * @author Johannes Link
 */
class ReturnAdderForClosuresTest {

    ReturnAdderForClosures adder

    @Before
    void init() {
        adder = new ReturnAdderForClosures()
    }

    @Test
    public void returnIsAddToRecursiveCallEmbeddedInClosure() throws Exception {
        MethodNode method = new AstBuilder().buildFromString(CompilePhase.SEMANTIC_ANALYSIS, true, '''
            class Target {
                int myMethod(int n) {
                    def next = { r1 ->
                        myMethod(n - 2)
                    }
                    return next()
                }
            }
		''')[1].getMethods('myMethod')[0]

        MethodNode methodExpected = new AstBuilder().buildFromString(CompilePhase.SEMANTIC_ANALYSIS, true, '''
            class Target {
                int myMethod(int n) {
                    def next = { r1 ->
                        return myMethod(n - 2)
                    }
                    return next()
                }
            }
		''')[1].getMethods('myMethod')[0]

        adder.addReturnsIfNeeded(method)

        AstAssert.assertSyntaxTree([methodExpected], [method])
    }

}
