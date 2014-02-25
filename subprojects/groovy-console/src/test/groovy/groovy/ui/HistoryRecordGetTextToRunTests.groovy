package groovy.ui

import junit.framework.TestCase

/*
 * @author Sergii Bondarenko
 * @author Pascal Schumacher
 */
class HistoryRecordGetTextToRunTests extends TestCase {
     
    void testImport() {
        assert java.text.SimpleDateFormat == runSelected('import java.text.SimpleDateFormat', 'return SimpleDateFormat')
    }

    void testImportWithAlias() {
        assert java.text.SimpleDateFormat == runSelected('import java.text.SimpleDateFormat as SF', 'return SF')
    }

    void testImportPackage() {
        assert java.text.SimpleDateFormat == runSelected('import java.text.*', 'return SimpleDateFormat')
    }

    void testStaticImportClass() {
        assert Long.MAX_VALUE == runSelected('import static java.lang.Long.*', 'return MAX_VALUE')
    }

    void testStaticImportMethod() {
        assert '12345' == runSelected('import static java.lang.String.valueOf;', 'return valueOf(12345)')
    }

    void testStaticImportField() {
        assert Integer.MAX_VALUE == runSelected('import static java.lang.Integer.MAX_VALUE', 'return MAX_VALUE')
    }
    
    void testMultipleStaticImports() {
        String importText = '''import static java.lang.Integer.MAX_VALUE
        import static java.lang.String.valueOf'''
        
        assert Integer.MAX_VALUE.toString() == runSelected(importText, 'return valueOf(MAX_VALUE)')
    }

    void testErrorsInScript() {
        assert 1 == runSelected('aa#@!#@$', 'return 1')
    }
    
    private def runSelected(String importText, String selectedText) {
        HistoryRecord hr = new HistoryRecord()
        String allText = importText + '\n' + selectedText
        hr.allText = allText
        hr.selectionStart = allText.indexOf(selectedText)
        hr.selectionEnd = allText.indexOf(selectedText) + selectedText.size()
        GroovyShell shell = new GroovyShell(Console.class.classLoader?.getRootLoader(), new Binding())
        return shell.run(hr.getTextToRun(true), name, [])
    }
}