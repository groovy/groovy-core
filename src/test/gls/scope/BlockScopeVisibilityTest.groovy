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
package gls.scope

import gls.CompilableTestSupport

class BlockScopeVisibilityTest extends CompilableTestSupport {

    public void testForLoopVariableNotVisibleOutside() {
        assertScript """
            i=1
            for (i in [2,3]) {}
            assert i==1
        """
    }

    public void testCatchParameterNotVisibleInOtherCatch() {
        shouldFail(MissingPropertyException) {
            try {
                throw new RuntimeException("not important");
            } catch (AssertionError e) {
                // here e is defined
            } catch (Throwable t) {
                // here e should not be accessible
                println e
            }
        }
    }

    public void testInnerClosureCanAccessImplicitItOfOuterClosure() {
        def c = { {-> it}}
        assert c(1)() == 1
    }

    public void testForLoopStatement() {
        // this example requires not to put the declaration
        // into a block !
        if (false)
        int number = 1

        shouldFail{ number }
    }

}
