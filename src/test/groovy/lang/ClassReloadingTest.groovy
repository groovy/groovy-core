package groovy.lang

import org.codehaus.groovy.control.CompilerConfiguration

class ClassReloadingTest extends GroovyTestCase {

    public void testReloading() {
        def file = File.createTempFile("TestReload", ".groovy", new File("target"))
        file.deleteOnExit()
        def className = file.name - ".groovy"

        def cl = new GroovyClassLoader(this.class.classLoader);
        def currentDir = file.parentFile.absolutePath
        cl.addClasspath(currentDir)
        cl.shouldRecompile = true

        try {
            file.write """
              class $className {
                def greeting = "hello"
              }
            """
            def groovyClass = cl.loadClass(className, true, false)
            def message = groovyClass.newInstance().greeting
            assert "hello" == message

            sleep 1500

            // change class
            file.write """
              class $className {
                def greeting = "goodbye"
              }
            """
            def success = file.setLastModified(System.currentTimeMillis())
            assert success
            sleep 500

            // reload
            groovyClass = cl.loadClass(className, true, false)
            message = groovyClass.newInstance().greeting
            assert "goodbye" == message
        } finally {
            file.delete()
        }
    }

    public void testReloadingInStringStringVersion() {
        def fileName = "Dummy3981.groovy"

        def cl = new GroovyClassLoader(this.class.classLoader);

        def classStr = """
            class Groovy3981 {
                def greeting = "hello"
            }
        """

        def groovyClass = cl.parseClass(classStr, fileName)
        def message = groovyClass.newInstance().greeting
        assert "hello" == message

        // (string, string) version should not do the caching as it breaks Spring integration (bean refreh)
        classStr = """
            class Groovy3981 {
                def greeting = "goodbye"
            }
        """
        groovyClass = cl.parseClass(classStr, fileName)
        message = groovyClass.newInstance().greeting
        assert "goodbye" == message
    }


    public void testReloadingIfInitialFileMissesTimestamp() {
        def parent = File.createTempDir("reload","test")
        def file = File.createTempFile("TestReload", ".groovy", parent)
        file.deleteOnExit()
        def className = file.name - ".groovy"
        def cc = new CompilerConfiguration()
        def currentDir = file.parentFile.absolutePath
//        cc.targetDirectory = parent
        def cl = new GroovyClassLoader(this.class.classLoader, cc)
        cl.addClasspath(currentDir)
        cl.shouldRecompile = false

        try {
            file.write """
              class $className {
                def greeting = "hello"
              }
            """
            def groovyClass = cl.loadClass(className, true, false)
            println System.identityHashCode(groovyClass)
            assert !groovyClass.declaredFields.any { it.name.contains('__timeStamp') }
            def message = groovyClass.newInstance().greeting
            assert "hello" == message

            cl = new GroovyClassLoader(this.class.classLoader, cc)
            cl.addClasspath(currentDir)
            cl.shouldRecompile = true

            sleep 1500

            // change class
            file.write """
              class $className {
                def greeting = "goodbye"
              }
            """
            def success = file.setLastModified(System.currentTimeMillis())
            assert success
            sleep 500

            // reload
            groovyClass = cl.loadClass(className, true, false)
            println System.identityHashCode(groovyClass)
            assert groovyClass.declaredFields.any { it.name.contains('__timeStamp') }
            message = groovyClass.newInstance().greeting
            assert "goodbye" == message
        } finally {
            println parent.listFiles()
            parent.eachFile {it.delete()}
            parent.delete()
        }
    }

}