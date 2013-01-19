/*
 * Copyright 2003-2012 the original author or authors.
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
package org.codehaus.groovy.ast.builder.testpackage

import org.codehaus.groovy.ast.builder.*

/**
 * Test package imports in AstBuilder.
 * 
 * It is important that this class contains an import of builder.* and not 
 * the AstBuilder class individually. 
 *
 * @author Hamlet D'Arcy
 */
@WithAstBuilder
public class AstBuilderFromCodePackageImportTest extends GroovyTestCase {
    public void testPackageImport() {
        def expected = new AstBuilder().buildFromString(""" println "Hello World" """)

        def result = new AstBuilder().buildFromCode {
            println "Hello World"
        }
        AstAssert.assertSyntaxTree(expected, result)
    }

}
