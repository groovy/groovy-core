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

import org.codehaus.groovy.classgen.TestSupport;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

/**
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public abstract class TestXmlSupport extends TestSupport {

    protected void dump(Node node) throws IOException {
        XmlUtil.serialize((Element) node, System.out);
    }

    protected SAXBuilder createSAXBuilder() throws IOException {
        return new SAXBuilder(new LoggingDefaultHandler());
    }

    private static class LoggingDefaultHandler extends DefaultHandler {
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            System.out.println("Start Element: " + localName);
        }
        public void endElement(String uri, String localName, String qName) throws SAXException {
            System.out.println("End Element: " + localName);
        }
        public void characters(char ch[], int start, int length) throws SAXException {
            System.out.println("Characters: " + new String(ch).substring(start, start + length - 1));
        }
    }
}
