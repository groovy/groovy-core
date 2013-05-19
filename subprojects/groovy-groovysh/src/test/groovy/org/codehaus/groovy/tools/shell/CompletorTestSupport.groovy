/*
 * Copyright 2003-2007 the original author or authors.
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

package org.codehaus.groovy.tools.shell

import groovy.mock.interceptor.MockFor
import org.codehaus.groovy.tools.shell.util.PackageHelper
import org.codehaus.groovy.tools.shell.completion.IdentifierCompletor
import org.codehaus.groovy.tools.shell.completion.ReflectionCompletor

/**
 * Created with IntelliJ IDEA.
 * User: kruset
 * Date: 4/6/13
 * Time: 11:26 AM
 * To change this template use File | Settings | File Templates.
 */
abstract class CompletorTestSupport extends GroovyTestCase {

    BufferManager bufferManager = new BufferManager()
    IO testio
    BufferedOutputStream mockOut
    BufferedOutputStream mockErr
    MockFor groovyshMocker
    MockFor packageHelperMocker
    PackageHelper mockPackageHelper

    MockFor reflectionCompletorMocker

    MockFor idCompletorMocker

    void setUp() {
        super.setUp()
        mockOut = new BufferedOutputStream(
                new ByteArrayOutputStream());

        mockErr = new BufferedOutputStream(
                new ByteArrayOutputStream());

        testio = new IO(
                new ByteArrayInputStream(),
                mockOut,
                mockErr)
        reflectionCompletorMocker = new MockFor(ReflectionCompletor)

        idCompletorMocker = new MockFor(IdentifierCompletor)

        groovyshMocker = new MockFor(Groovysh)
        groovyshMocker.demand.createDefaultRegistrar() { { shell -> null } }
        groovyshMocker.demand.getIo(0..2) { testio }
        packageHelperMocker = new MockFor(PackageHelper)
        def registry = new CommandRegistry()
        groovyshMocker.demand.getRegistry(0..1) { registry }
        groovyshMocker.demand.getClass(0..1) { Groovysh }
        packageHelperMocker.demand.getContents(6) { ["java", "test"] }
        groovyshMocker.demand.getIo(0..2) { testio }
        for (i in 1..19) {
            groovyshMocker.demand.getIo(0..1) { testio }
            groovyshMocker.demand.leftShift(0..1) {}
            groovyshMocker.demand.getIo(0..1) { testio }
        }
        groovyshMocker.demand.getRegistry(0..1) { registry }
        groovyshMocker.demand.getBuffers(0..2) {bufferManager}



    }
}
