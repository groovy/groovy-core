package groovy.xml

/**
 * Test the building of namespaced XML using GroovyMarkup
 */
class NamespaceNodeTest extends TestXmlSupport {

    void testNodeBuilderWithNamespace() {
        def n = new Namespace('http://foo/bar')
        def builder = NamespaceBuilder.newInstance(new NodeBuilder(), n.uri)
        def result = builder.outer(id: "3") {
            inner(name: "foo")
            inner("bar")
        }
        assert result[n.inner][0].@name == 'foo'
        assert result[n.inner][1].text() == 'bar'
    }
    
    void testTree() {
        def builder = NodeBuilder.newInstance()
        def xmlns = new NamespaceBuilder(builder)
        def xsd = xmlns.namespace('http://www.w3.org/2001/XMLSchema', 'xsd')

        def root = xsd.schema {
            annotation {
                documentation("Purchase order schema for Example.com.")
            }
            element(name: 'purchaseOrder', type: 'PurchaseOrderType')
            element(name: 'comment', type: 'xsd:string')
            complexType(name: 'PurchaseOrderType') {
                sequence {
                    element(name: 'shipTo', type: 'USAddress')
                    element(name: 'billTo', type: 'USAddress')
                    element(minOccurs: '0', ref: 'comment')
                    element(name: 'items', type: 'Items')
                }
                attribute(name: 'orderDate', type: 'xsd:date')
            }
        }
        assert root != null

        assertGPaths(root)
    }

    void assertGPaths(Node root) {
        // check root node
        def name = root.name()
        assert name instanceof QName
        assert name.namespaceURI == 'http://www.w3.org/2001/XMLSchema'
        assert name.localPart == 'schema'
        assert name.prefix == 'xsd'

        // check 'xsd' qname factory
        Namespace xsd = new Namespace('http://www.w3.org/2001/XMLSchema', 'xsd')
        def qname = xsd.annotation
        assert qname.namespaceURI == 'http://www.w3.org/2001/XMLSchema'
        assert qname.localPart == 'annotation'
        assert qname.prefix == 'xsd'

        def docNode = root[xsd.annotation][xsd.documentation]
        assert docNode[0].text() == "Purchase order schema for Example.com."

        def attrNode = root[xsd.complexType][xsd.attribute]
        assert attrNode[0].@name == 'orderDate'
    }

    void testNodeBuilderWithImplicitNamespace() {
        def n = new Namespace('http://foo/bar')
        def builder = NamespaceBuilder.newInstance(new NodeBuilder(), n.uri)

        def result = builder.outer(id: "3") {
            'ns1:innerWithNewNamespace'('xmlns:ns1': "http://foo/other", someAttr: 'someValue') {
                'ns1:nested'("foo")
            }
            innerWithoutNewNamespace("bar")
        }
        
        def expected = """<?xml version="1.0" encoding="UTF-8"?>\
<outer xmlns="http://foo/bar" id="3">
  <ns1:innerWithNewNamespace xmlns:ns1="http://foo/other" someAttr="someValue">
    <ns1:nested>foo</ns1:nested>
  </ns1:innerWithNewNamespace>
  <innerWithoutNewNamespace>bar</innerWithoutNewNamespace>
</outer>
""".replaceAll('[\r\n]','')
        def actual = XmlUtil.serialize(result).replaceAll("[\r\n]", "")
        assert actual == expected
    }

    void testNamespaceBuilderWithoutNamespace() {
        def builder = NamespaceBuilder.newInstance(new NodeBuilder())
        def result = builder.outer(id: "3") {
            inner(name: "foo")
            inner("bar")
        }
        def expected = """<?xml version="1.0" encoding="UTF-8"?>\
<outer id="3">
  <inner name="foo"/>
  <inner>bar</inner>
</outer>
""".replaceAll('[\r\n]','')
        def actual = XmlUtil.serialize(result).replaceAll("[\r\n]", "")
        assert actual == expected
    }
}