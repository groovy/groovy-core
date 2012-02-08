package groovy.xml

import java.nio.charset.Charset

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
        def charsetStringsToTest = ["UTF-8", "ISO-8859-1"]

        charsetStringsToTest.each { charsetStringToTest ->

            Charset charsetToTest = Charset.forName(charsetStringToTest)

            //we can only test the charset, we use in our system. We typically want to test
            //the two encodings used most of the time
            if(charsetToTest.equals(Charset.defaultCharset())) {

                //we need to specify the charset manually, so that the test will not break on systems, which do have a
                //different default charset.
                def sourceXML = new String("<?xml version=\"1.0\" encoding=\"${charsetStringToTest}\"?>\n<root>text Üäcontent</root>".getBytes(), charsetToTest)
                def sourceXMLWithUmlauts = new String("<?xml version=\"1.0\" encoding=\"${charsetStringToTest}\"?>\n<Schlüssel>text Üä content</Schlüssel>".getBytes(), charsetToTest)


                def sourceGPath = new XmlSlurper().parseText(sourceXML)
                def sourceGPathWithUmlauts = new XmlSlurper().parseText(sourceXMLWithUmlauts)

                def sourceGPathSerializedViaXMLUtil =  groovy.xml.XmlUtil.serialize(sourceGPath).toString().trim()
                def sourceGPathWithUmlautsSerializedViaXMLUtil =  groovy.xml.XmlUtil.serialize(sourceGPathWithUmlauts).toString().trim()


                //we set the encoding to UTF-8, since groovy.xml.XmlUtil.serialize can only output UTF-8
                def sourceGPathSerializedManuallyViaStreamingMarkupBuilder = new groovy.xml.StreamingMarkupBuilder().with {
                    encoding = 'UTF-8'
                    '<?xml version="1.0" encoding="UTF-8"?>\n' + bindNode(sourceGPath)
                    }.toString().trim()

                def sourceGPathWithUmlautsSerializedManuallyViaStreamingMarkupBuilder = new groovy.xml.StreamingMarkupBuilder().with {
                    encoding = 'UTF-8'
                    '<?xml version="1.0" encoding="UTF-8"?>\n' + bindNode(sourceGPathWithUmlauts)
                    }.toString().trim()


                //XMLUtil <==> Manual StreamingMarkupBuilder
                assert sourceGPathSerializedManuallyViaStreamingMarkupBuilder.equals(sourceGPathSerializedViaXMLUtil)
                assert sourceGPathWithUmlautsSerializedManuallyViaStreamingMarkupBuilder.equals(sourceGPathWithUmlautsSerializedViaXMLUtil)


                //Source might have different encoding, e.g. ISO-8850-1. Since XmlUtil can only output UTF-8, we cannot check agains
                //sourceXML.
            }
        }
    }
}