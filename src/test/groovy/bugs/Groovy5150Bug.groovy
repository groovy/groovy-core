package groovy.bugs

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.tools.javac.JavaAwareCompilationUnit

class Groovy5150Bug extends GroovyTestCase {
    static class Constants {
        public static final int constant = 2
        public static final char FOOCHAR = 'x'
    }
    void testShouldAllowConstantInSwitch() {
        def config = new CompilerConfiguration()
        config.with {
            targetDirectory = createTempDir()
            jointCompilationOptions = [stubDir: createTempDir()]
        }

        File parentDir = createTempDir()
        try {
            def b = new File(parentDir, 'B.java')
            b.write '''
            public class B {
                public static void main(String...args) {
                    int x = 4;
                    switch (x) {
                        case groovy.bugs.Groovy5150Bug.Constants.constant: x=1;
                    }
                }
            }
        '''
            def loader = new GroovyClassLoader(this.class.classLoader)
            def cu = new JavaAwareCompilationUnit(config, loader)
            cu.addSources([b] as File[])
            cu.compile()
        } finally {
            parentDir.deleteDir()
            config.targetDirectory.deleteDir()
            config.jointCompilationOptions.stubDir.deleteDir()
        }
    }

    void testShouldAllowConstantInSwitchWithStubs() {
        def config = new CompilerConfiguration()
        config.with {
            targetDirectory = createTempDir()
            jointCompilationOptions = [stubDir: createTempDir()]
        }

        File parentDir = createTempDir()
        try {
            def a = new File(parentDir, 'A.groovy')
            a.write '''
                class A {
                    public static final int constant = 1
                }
            '''
            def b = new File(parentDir, 'B.java')
            b.write '''
            public class B {
                public static void main(String...args) {
                    int x = 4;
                    switch (x) {
                        case A.constant: x=1;
                    }
                }
            }
        '''
            def loader = new GroovyClassLoader(this.class.classLoader)
            def cu = new JavaAwareCompilationUnit(config, loader)
            cu.addSources([a,b] as File[])
            cu.compile()
        } finally {
            parentDir.deleteDir()
            config.targetDirectory.deleteDir()
            config.jointCompilationOptions.stubDir.deleteDir()
        }
    }

    void testShouldAllowCharConstantInSwitchWithoutStubs() {
        def config = new CompilerConfiguration()
        config.with {
            targetDirectory = createTempDir()
            jointCompilationOptions = [stubDir: createTempDir()]
        }

        File parentDir = createTempDir()
        try {
            def b = new File(parentDir, 'B.java')
            b.write '''
            public class B {
                public static void main(String...args) {
                    char x = 'z';
                    switch (x) {
                        case groovy.bugs.Groovy5150Bug.Constants.FOOCHAR: x='y';
                    }
                }
            }
        '''
            def loader = new GroovyClassLoader(this.class.classLoader)
            def cu = new JavaAwareCompilationUnit(config, loader)
            cu.addSources([b] as File[])
            cu.compile()
        } finally {
            parentDir.deleteDir()
            config.targetDirectory.deleteDir()
            config.jointCompilationOptions.stubDir.deleteDir()
        }
    }

    void testShouldAllowCharConstantInSwitchWithStubs() {
        def config = new CompilerConfiguration()
        config.with {
            targetDirectory = createTempDir()
            jointCompilationOptions = [stubDir: createTempDir()]
        }

        File parentDir = createTempDir()
        try {
            def a = new File(parentDir, 'A.groovy')
            a.write '''
                class A {
                    public static final char constant = 'x'
                }
            '''
            def b = new File(parentDir, 'B.java')
            b.write '''
            public class B {
                public static void main(String...args) {
                    char x = 'z';
                    switch (x) {
                        case A.constant: x='y';
                    }
                }
            }
        '''
            def loader = new GroovyClassLoader(this.class.classLoader)
            def cu = new JavaAwareCompilationUnit(config, loader)
            cu.addSources([a,b] as File[])
            cu.compile()
        } finally {
            parentDir.deleteDir()
            config.targetDirectory.deleteDir()
            config.jointCompilationOptions.stubDir.deleteDir()
        }
    }

    void testAccessConstantStringFromJavaClass() {
        def config = new CompilerConfiguration()
        config.with {
            targetDirectory = createTempDir()
            jointCompilationOptions = [stubDir: createTempDir()]
        }

        File parentDir = createTempDir()
        try {
            def a = new File(parentDir, 'A.groovy')
            a.write '''
                class A {
                    public static final String CONSTANT = "hello, world!"
                }
            '''
            def b = new File(parentDir, 'B.java')
            b.write '''
            public class B {
                public static void main(String...args) {
                    if (!"hello, world!".equals(A.CONSTANT)) throw new RuntimeException("Constant should not be: ["+A.CONSTANT+"]");
                }
            }
        '''
            def loader = new GroovyClassLoader(this.class.classLoader)
            def cu = new JavaAwareCompilationUnit(config, loader)
            cu.addSources([a,b] as File[])
            cu.compile()
            Class clazz = loader.loadClass("B")
            clazz.newInstance().main()
        } finally {
            parentDir.deleteDir()
            config.targetDirectory.deleteDir()
            config.jointCompilationOptions.stubDir.deleteDir()
        }
    }

    private static File createTempDir() {
        def dir = new File(System.getProperty('java.io.tmpdir'), "groovyTest${System.currentTimeMillis()}")
        dir.delete()
        dir.mkdir()

        dir
    }
}
