package groovy.xml

/**
 * @author <a href="mailto:info@weitz24.de">Jan Weitz</a>
 * @version $Revision$
 */
class XmlUtilTest extends TestXmlSupport {

    //GROOVY-5158: Encoding issue with groovy.xml.XmlUtil.serialize()
    void testXMLUmlautsSerialization() {
        /**
         * Idea: we create a source xml and try to parse it via the XmlSlurper and later on serialize
         * it via XmlUtil and check, if all results will be the same XML-String as we put in.
         * Be aware, that this test might break, if the XMLSlurper will not use a newline \n after
         * the XML-declaration any more.
         *
         * We do two test. One valid test without umlauts and one with umlauts. XmlSlurper and XmlUtil were
         * inconsistent. Now they should be consistent, even with Umlauts.
         *
         */

        def sourceXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>text content</root>"
        def sourceXMLWithUmlauts = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Schlüssel>text content</Schlüssel>"

        def sourceGPath = new XmlSlurper().parseText(sourceXML)
        def sourceGPathWithUmlauts = new XmlSlurper().parseText(sourceXMLWithUmlauts)

        def sourceGPathSerializedViaXMLUtil =  groovy.xml.XmlUtil.serialize(sourceGPath).toString().trim()
        def sourceGPathWithUmlautsSerializedViaXMLUtil =  groovy.xml.XmlUtil.serialize(sourceGPathWithUmlauts).toString().trim()

        def sourceGPathSerializedManuallyViaStreamingMarkupBuilder = new groovy.xml.StreamingMarkupBuilder().with {
            encoding = 'UTF-8'
            '<?xml version="1.0" encoding="UTF-8"?>\n' + bindNode(sourceGPath)
            }.toString().trim()
        
        def sourceGPathWithUmlautsSerializedManuallyViaStreamingMarkupBuilder = new groovy.xml.StreamingMarkupBuilder().with {
            encoding = 'UTF-8'
            '<?xml version="1.0" encoding="UTF-8"?>\n' + bindNode(sourceGPathWithUmlauts)
            }.toString().trim()




        //SOURCE < == > XMLUtil
        assert sourceGPathSerializedViaXMLUtil.equals(sourceXML)
        assert sourceGPathWithUmlautsSerializedViaXMLUtil.equals(sourceXMLWithUmlauts)

        //XMLUtil <==> Manual StreamingMarkupBuilder
        assert sourceGPathSerializedManuallyViaStreamingMarkupBuilder.equals(sourceGPathSerializedViaXMLUtil)
        assert sourceGPathWithUmlautsSerializedManuallyViaStreamingMarkupBuilder.equals(sourceGPathWithUmlautsSerializedViaXMLUtil)

        //Success: => SOURCE <==> manual StreamingMarkupBuilder

    }
}