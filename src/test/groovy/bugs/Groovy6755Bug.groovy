package groovy.bugs

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.tools.FileSystemCompiler

class Groovy6755Bug extends GroovyTestCase {

    public void testDeeepGenericJointCompilationImportResolution() {
        File workDir = new File("target/tmp/Groovy6755Bug")
        workDir.deleteDir()
        workDir.mkdirs()

        File classesDir = new File(workDir, "classes")
        classesDir.mkdirs()

        final File itemJava = new File(workDir, "a/Item.java");
        itemJava.parentFile.mkdirs()
        itemJava.text = """
package a;

public interface Item {
}

"""

        final File itemListListGroovy = new File(workDir, "b/ItemListList.groovy");
        itemListListGroovy.parentFile.mkdirs()
        itemListListGroovy.text = """
package b
// Use a star import from a different package here to expose the bug
import a.*
import java.util.List

class ItemListList {
    // The local class must be a generic type of a generic type
    static List<List<Item>> ITEMS
}

"""

        try {
// needs to be joint compiler
            FileSystemCompiler.commandLineCompile(
                    "-j -sourcepath ${workDir.path} -d ${classesDir.path} ${itemJava.path} ${itemListListGroovy.path}".
                            split(' '), true)

            // So the bug would produce an ItemListList with a field of type:
            //   java.util.List<java.util.List<Item>>
            // note that Item is not fully qualified as a.Item as it should be
            // and thus reference to the class will fail during loading. Instead
            // of analysing the class, We'll just trigger a classload of it
            // to ensure that it ended up being valid. If the bug exists,
            // then we should get an error like 'Type Item not present'
            CompilerConfiguration cc = new CompilerConfiguration();
            cc.setClasspath(classesDir.path);
            // I'm sure there's a nicer way of triggering this via the classloader, but this will do
            new GroovyShell(cc).evaluate("""
import b.ItemListList

def x = ItemListList.ITEMS

""")
        } finally {
            workDir.deleteDir()
        }

    }
}
