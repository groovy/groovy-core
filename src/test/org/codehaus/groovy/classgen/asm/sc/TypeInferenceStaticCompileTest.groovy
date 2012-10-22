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
package org.codehaus.groovy.classgen.asm.sc

import groovy.transform.stc.TypeInferenceSTCTest

/**
 * Unit tests for static type checking : type inference.
 *
 * @author Cedric Champeau
 */
@Mixin(StaticCompilationTestSupport)
class TypeInferenceStaticCompileTest extends TypeInferenceSTCTest {

    @Override
    protected void setUp() {
        super.setUp()
        extraSetup()
    }

    // GROOVY-5655
    void testByteArrayInference() {
        assertScript '''
                @ASTTest(phase=INSTRUCTION_SELECTION, value={
                    assert node.getNodeMetaData(INFERRED_TYPE) == byte_TYPE.makeArray()
                })
                def b = "foo".bytes
                new String(b)
            '''
    }
}

