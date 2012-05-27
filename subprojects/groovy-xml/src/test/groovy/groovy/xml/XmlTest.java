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
package groovy.xml;

//import groovy.lang.GroovyObject;
//import groovy.lang.GroovyShell;
//import groovy.lang.Script;
import groovy.util.GroovyTestCase;

import java.io.File;

/**
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class XmlTest extends GroovyTestCase {

//    public void testTree() throws Exception {
//        Script script = new GroovyShell().parse(new File("src/test/groovy/xml/dom/NamespaceDOMTest.groovy"));
////        GroovyObject object = compile("src/test/groovy/xml/dom/NamespaceDOMTest.groovy");
//        script.invokeMethod("testXsdSchemaWithBuilderHavingAutoPrefix", null);
//    }

    public void testQName() throws Exception {
        QName qname = new QName("urn:mynamespace", "localPart", "x");
        assertTrue(qname.equals(new QName("urn:mynamespace", "localPart")));
        assertTrue(qname.equals("urn:mynamespace:localPart"));
        assertTrue(qname.equals("x:localPart"));
        assertTrue(!qname.equals(null));
        assertTrue(!qname.equals(""));
        assertTrue(!qname.equals(" "));
        assertTrue(!qname.equals("localPart"));
        assertTrue(!qname.equals("x:"));
        assertTrue(!qname.equals(":"));
        assertTrue(!qname.equals(":localPart"));
    }
}
