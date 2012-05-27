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

package groovy.xml

/**
 * A Groovy builder that works with Stax processors.
 * Using Java 6, typical usage is as follows:
 * <pre>
 * def factory = XMLOutputFactory.newInstance()
 * def writer = new StringWriter()
 * def builder = new StaxBuilder(factory.createXMLStreamWriter(writer))
 * builder.root1(a:5, b:7) {
 *     elem1('hello1')
 *     elem2('hello2')
 *     elem3(x:7)
 * }
 * assert writer == """<root1 a="5" b="7"><elem1>hello1</elem1><elem2>hello2</elem2><elem3 x="7" /></root1>"""
 * </pre>
 * Or an external library such as Jettison can be used as follows:
 * <pre>
 * @Grab('org.codehaus.jettison:jettison:1.2')
 * import org.codehaus.jettison.mapped.*
 * import javax.xml.stream.XMLStreamException
 *
 * def conv = new MappedNamespaceConvention()
 * def writer = new StringWriter()
 * def mappedWriter = new MappedXMLStreamWriter(conv, writer)
 * def builder = new groovy.xml.StaxBuilder(mappedWriter)
 * builder.root1(a:5, b:7) {
 *     elem1('hello1')
 *     elem2('hello2')
 *     elem3(x:7)
 * }
 * assert writer.toString() == '''{"root1":{"@a":"5","@b":"7","elem1":"hello1","elem2":"hello2","elem3":{"@x":"7"}}}'''
 * </pre>
 *
 * @author <a href="dejan@nighttale.net">Dejan Bosanac</a>
 * @author Paul King
 */
public class StaxBuilder extends BuilderSupport {

    def writer

    public StaxBuilder(xmlStreamWriter) {
        writer = xmlStreamWriter
        writer.writeStartDocument()
    }

    protected createNode(name) {
        createNode(name, null, null)
    }

    protected createNode(name, value) {
        createNode(name, null, value)
    }

    protected createNode(name, Map attributes) {
        createNode(name, attributes, null)
    }

    protected createNode(name, Map attributes, value) {
        writer.writeStartElement(name.toString())
        if (attributes) {
            attributes.each { k, v ->
                writer.writeAttribute(k.toString(), v.toString())
            }
        }
        if (value) {
            writer.writeCharacters(value.toString())
        }
        name
    }

    protected void nodeCompleted(parent, node) {
        writer.writeEndElement()
        if (!parent) {
            writer.writeEndDocument()
            writer.flush()
        }
    }

    protected void setParent(parent, child) {
    }

}
