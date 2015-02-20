/*
 * Copyright 2014 the original author or authors.
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
package groovy.xml

import groovy.util.GroovyTestCase

/**
* Tests for the Groovy Xml user guide related to XmlUtil.
*
* @author Groovy Documentation Community
*/
class UserGuideXmlUtilTest  extends GroovyTestCase {

    // tag::responseBookXml[]
    def xml = """
    <response version-api="2.0">
        <value>
            <books>
                <book id="2">
                    <title>Don Xijote</title>
                    <author id="1">Manuel De Cervantes</author>
                </book>
            </books>
        </value>
    </response>
    """
    // end::responseBookXml[]

    void testGettingANode() {
        // tag::testGettingANode[]
        def response = new XmlParser().parseText(xml)
        def nodeToSerialize = response.'**'.find {it.name() == 'author'}
        def nodeAsText = XmlUtil.serialize(nodeToSerialize)

        assert nodeAsText ==
            XmlUtil.serialize('<?xml version="1.0" encoding="UTF-8"?><author id="1">Manuel De Cervantes</author>')
        // end::testGettingANode[]
    }

}
