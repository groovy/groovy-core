/*
 * Copyright 2003-2011 the original author or authors.
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

package groovy.bugs


class Groovy4958Bug extends GroovyTestCase {
    void testConfigSlurperParsingConfigFilesWithDollarSymbolInPropValues() {
        def config = new ConfigObject()
        config.instObject = '1. Dyn. Aktber. ($IO_AKTBER)   # Aktenbereich \' " (V-AKTBER)'
        
        def beforeVersion = config.toString()
        
        def sw = new StringWriter()
        config.writeTo(sw)
        
        config = new ConfigSlurper().parse(sw.toString())
        def afterVersion = config.toString()
        
        assert beforeVersion == afterVersion
    }
}