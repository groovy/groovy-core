package groovy;

import groovy.text.Template;
import groovy.text.GStringTemplateEngine;

/**
 * @author andy
 * @since Jan 11, 2006 1:05:23 PM
 */
public class SimpleGStringTemplateEngineTest extends GroovyTestCase
{
  void testRegressionCommentBug()
  {
    final Template template = new GStringTemplateEngine().createTemplate(
        "<% // This is a comment that will be filtered from output %>\n" +
        "Hello World!"
    );

    final StringWriter sw = new StringWriter();
    template.make().writeTo(sw);
    assertEquals("\nHello World!", sw.toString());
  }
}
