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
package org.codehaus.groovy.tools.ast

import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import java.security.CodeSource
import org.codehaus.groovy.control.CompilationUnit.PrimaryClassNodeOperation
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.ast.ClassNode

/*
* This TestHarness exists so that a global transform can be run without
* using the Jar services mechanism, which requires building a jar.
* 
* To use this simply create an instance of TransformTestHelper with
* an ASTTransformation and CompilePhase, then invoke parse(File) or
* parse(String). 
* 
* This test harness is not exactly the same as executing a global transformation
* but can greatly aide in debugging and testing a transform. You should still
* test your global transformation when packaged as a jar service before
* releasing it. 
* 
* @author Hamlet D'Arcy
*/
class TransformTestHelper {

    private ASTTransformation transform
    private CompilePhase phase

    /**
     * Creates the test helper.
     * @param transform
     *      the transform to run when compiling the file later
     * @param phase
     *      the phase to run the transform in 
     */
    def TransformTestHelper(ASTTransformation transform, CompilePhase phase) {
        this.transform = transform
        this.phase = phase
    }

    /**
     * Compiles the File into a Class applying the transform specified in the constructor.
     * @input input
     *      must be a groovy source file
     */
    public Class parse(File input) {
        TestHarnessClassLoader loader = new TestHarnessClassLoader(transform, phase)
        return loader.parseClass(input)
    }

    /**
     * Compiles the String into a Class applying the transform specified in the constructor.
     * @input input
     *      must be a valid groovy source string
     */
    public Class parse(String input) {
        TestHarnessClassLoader loader = new TestHarnessClassLoader(transform, phase)
        return loader.parseClass(input)
    }
}

/**
* ClassLoader exists so that TestHarnessOperation can be wired into the compile. 
*
* @author Hamlet D'Arcy
*/
@groovy.transform.PackageScope class TestHarnessClassLoader extends GroovyClassLoader {

    private ASTTransformation transform
    private CompilePhase phase

    TestHarnessClassLoader(ASTTransformation transform, CompilePhase phase) {
        this.transform = transform
        this.phase = phase
    }

    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource codeSource) {

        CompilationUnit cu = super.createCompilationUnit(config, codeSource)
        cu.addPhaseOperation(new TestHarnessOperation(transform), phase.getPhaseNumber())
        return cu
    }
}

/**
* Operation exists so that an AstTransformation can be run against the SourceUnit.
*
* @author Hamlet D'Arcy
*/
@groovy.transform.PackageScope class TestHarnessOperation extends PrimaryClassNodeOperation {

    private ASTTransformation transform

    def TestHarnessOperation(transform) {
        this.transform = transform;
    }

    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        transform.visit(null, source)
    }
}
