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

import groovy.transform.stc.MiscSTCTest

/**
 * Unit tests for static type checking : miscellaneous tests.
 *
 * @author Cedric Champeau
 */
@Mixin(StaticCompilationTestSupport)
class MiscStaticCompileTest extends MiscSTCTest {

    @Override
    protected void setUp() {
        super.setUp()
        extraSetup()
    }

    void testEachFileRecurse() {
        assertScript '''import groovy.io.FileType
            File dir = new File(System.getProperty('java.io.tmpdir'))
            dir.eachFileRecurse(FileType.FILES) { File spec ->
            }
        '''
    }
}

