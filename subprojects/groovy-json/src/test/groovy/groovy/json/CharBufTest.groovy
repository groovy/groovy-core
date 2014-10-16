package groovy.json

import groovy.json.internal.CharBuf

/**
 * Test the internal CharBuf class
 *
 * @author Rick Hightower
 * @author Guillaume Laforge
 */
class CharBufTest extends GroovyTestCase {

    void testUnicodeAndControl() {
        String str = CharBuf.create(0).addJsonEscapedString("\u0001").toString()
        assert str == '"\\u0001"'
        
        str =  CharBuf.create(0).addJsonEscapedString("\u00ff").toString()
        assert str == '"\\u00ff"'

        str =  CharBuf.create(0).addJsonEscapedString("\u0fff").toString()
        assert str == '"\\u0fff"'

        str =  CharBuf.create(0).addJsonEscapedString("\uefef").toString()
        assert str == '"\\uefef"'

        str =  CharBuf.create(0).addJsonEscapedString(" \b ").toString()
        assert str == '" \\b "'

        str =  CharBuf.create(0).addJsonEscapedString(" \r ").toString()
        assert str == '" \\r "'

        str =  CharBuf.create(0).addJsonEscapedString(" \n ").toString()
        assert str == '" \\n "'

        str =  CharBuf.create(0).addJsonEscapedString(" \n ").toString()
        assert str == '" \\n "'

        str =  CharBuf.create(0).addJsonEscapedString(" \f ").toString()
        assert str == '" \\f "'

        str =  CharBuf.create(0).addJsonEscapedString(' " Hi mom " ').toString()
        assert str == '" \\" Hi mom \\" "'

        str =  CharBuf.create(0).addJsonEscapedString(" \\ ").toString()
        assert str == '" \\\\ "'
    }

    /**
     * http://jira.codehaus.org/browse/GROOVY-6937
     * http://jira.codehaus.org/browse/GROOVY-6852
     */
    void testGroovy6937and6852() {
        // using raw CharBuf directly
        String s = CharBuf.create(0).addJsonEscapedString('\u0391\u03a6\u039f\u0399 \u039a\u039f\u039b\u039b\u0399\u0391 \u039a\u03a1\u0395\u03a9\u03a0\u039f\u039b\u0395\u0399\u039f \u03a4\u0391\u0392\u0395\u03a1\u039d\u0391').toString()
        assert s == '"\\u0391\\u03a6\\u039f\\u0399 \\u039a\\u039f\\u039b\\u039b\\u0399\\u0391 \\u039a\\u03a1\\u0395\\u03a9\\u03a0\\u039f\\u039b\\u0395\\u0399\\u039f \\u03a4\\u0391\\u0392\\u0395\\u03a1\\u039d\\u0391"'

        // CharBuf used underneath through JsonBuilder and JsonOutput
        def obj = [
                "\u0391\u03a6\u039f\u0399 \u039a\u039f\u039b\u039b\u0399\u0391 \u039a\u03a1\u0395\u03a9\u03a0\u039f\u039b\u0395\u0399\u039f \u03a4\u0391\u0392\u0395\u03a1\u039d\u0391",
                   "\u039a\u03b1\u03bb\u03cd\u03b2\u03b9\u03b1 \u0398\u03bf\u03c1\u03b9\u03ba\u03bf\u03cd"
        ]
        def result = new JsonBuilder(obj).toString()
        assert result == '["\\u0391\\u03a6\\u039f\\u0399 \\u039a\\u039f\\u039b\\u039b\\u0399\\u0391 \\u039a\\u03a1\\u0395\\u03a9\\u03a0\\u039f\\u039b\\u0395\\u0399\\u039f \\u03a4\\u0391\\u0392\\u0395\\u03a1\\u039d\\u0391","\\u039a\\u03b1\\u03bb\\u03cd\\u03b2\\u03b9\\u03b1 \\u0398\\u03bf\\u03c1\\u03b9\\u03ba\\u03bf\\u03cd"]'

        obj = ["\u20AC" * 20_000]
        result = new JsonBuilder(obj).toString()
        assert result == /["${'\\u20ac' * 20_000}"]/
    }
}
