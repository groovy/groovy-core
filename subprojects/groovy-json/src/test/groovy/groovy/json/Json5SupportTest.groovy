package groovy.json

class Json5SupportTest extends GroovyTestCase {
    JsonSlurper parser
    
    @Override
    void setUp() {
        parser = new JsonSlurper().setType(JsonParserType.JSON5)
    }
    
    void testBashCommentsRemoved() {
        shouldFail {
            parser.parseText("""\
            # This is a Comment
            {
                key: "value" # This is another comment
            }
            """.stripIndent())
        }
    }
    
    void testComments() {
        assert parser.parseText("""\
            /* Block Comment */
            {
                key: "value" // Single line comments
            }
        """.stripIndent()) == [
            key: "value"
        ]
    }
    
    void testTrailingCommas() {
        assert parser.parseText("""\
            {
                key: "value",
                anotherKey: "anotherValue",
            }
        """.stripIndent()) == [
            key: "value",
            anotherKey: "anotherValue"
        ]
    }
    
    void testPositiveNumbers() {
        assert parser.parseText("""\
            {
                key: +1
            }
        """) == [
            key: 1    
        ]
    }
}
