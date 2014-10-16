/*
 * Copyright 2003-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.codehaus.groovy.tools.stubgenerator

/**
 * Test that traits do not mess up with stub generation.
 *
 * @author Cedric Champeau
 */
class Groovy7052Bug extends StringSourcesStubTestCase {

    Map<String, String> provideSources() {
        [
                'Foo.groovy': '''
                    trait Foo {}
                ''',
                'Bar.java': '''
                    class Bar implements Foo {}
                ''',
                'Baz.groovy': '''
                    class Baz implements Foo {}
                '''
        ]
    }

    void verifyStubs() {
        def stubSource = stubJavaSourceFor('Foo')
        assert stubSource.contains('interface Foo')
    }
}
