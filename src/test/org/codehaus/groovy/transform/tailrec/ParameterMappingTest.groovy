/*
 * Copyright 2013-2014 the original author or authors.
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
package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.builder.AstBuilder
import org.junit.Test

import static org.objectweb.asm.Opcodes.ACC_PUBLIC

/**
 * @author Johannes Link
 */
class ParameterMappingTest {

    TailRecursiveASTTransformation transformation = new TailRecursiveASTTransformation()

    @Test
    public void emptyMethod() throws Exception {
        def myMethod = new AstBuilder().buildFromSpec {
            method('myMethod', ACC_PUBLIC, int.class) {
                parameters {}
                exceptions {}
                block {
                }
            }
        }[0]

        def nameAndTypeMapping, positionMapping
        (nameAndTypeMapping, positionMapping) = transformation.parameterMappingFor(myMethod)

        assert nameAndTypeMapping == [:]
        assert positionMapping == [:]
    }

    @Test
    public void oneParameter() throws Exception {
        def myMethod = new AstBuilder().buildFromSpec {
            method('myMethod', ACC_PUBLIC, int.class) {
                parameters { parameter 'one': int.class }
                exceptions {}
                block {
                }
            }
        }[0]

        def nameAndTypeMapping, positionMapping
        (nameAndTypeMapping, positionMapping) = transformation.parameterMappingFor(myMethod)

        assert nameAndTypeMapping == [one: [name: '_one_', type: ClassHelper.int_TYPE]]
        assert positionMapping == [0: [name: '_one_', type: ClassHelper.int_TYPE]]
    }

    @Test
    public void severalParameters() throws Exception {
        def myMethod = new AstBuilder().buildFromSpec {
            method('myMethod', ACC_PUBLIC, int.class) {
                parameters {
                    parameter 'one': int.class
                    parameter 'two': String.class
                    parameter 'three': Closure.class
                }
                exceptions {}
                block {
                }
            }
        }[0]

        def nameAndTypeMapping, positionMapping
        (nameAndTypeMapping, positionMapping) = transformation.parameterMappingFor(myMethod)

        assert nameAndTypeMapping == [
                one: [name: '_one_', type: ClassHelper.int_TYPE],
                two: [name: '_two_', type: ClassHelper.STRING_TYPE],
                three: [name: '_three_', type: ClassHelper.CLOSURE_TYPE]
        ]
        assert positionMapping == [
                0: [name: '_one_', type: ClassHelper.int_TYPE],
                1: [name: '_two_', type: ClassHelper.STRING_TYPE],
                2: [name: '_three_', type: ClassHelper.CLOSURE_TYPE]
        ]
    }
}
