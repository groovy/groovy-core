package org.codehaus.groovy.classgen.asm.sc

import org.codehaus.groovy.classgen.asm.AbstractBytecodeTestCase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport

class StaticCompilationTest extends AbstractBytecodeTestCase {
    void testEmptyMethod() {
        def bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            void m() {}
        ''')
        assert bytecode.hasStrictSequence(
                ['public m()V',
                        'L0',
                        'RETURN']
        )
    }

    void testPrimitiveReturn1() {
        def bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            int m() { 1 }
        ''')
        assert bytecode.hasStrictSequence(
                ['ICONST_1', 'IRETURN']
        )
    }

    void testPrimitiveReturn2() {
        def bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            long m() { 1L }
        ''')
        assert bytecode.hasStrictSequence(
                ['LCONST_1', 'LRETURN']
        )
    }

    void testPrimitiveReturn3() {
        def bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            short m() { 1 }
        ''')
        assert bytecode.hasStrictSequence(
                ['ICONST_1', 'I2S', 'IRETURN']
        )
    }

    void testPrimitiveReturn4() {
        def bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            byte m() { 1 }
        ''')
        assert bytecode.hasStrictSequence(
                ['ICONST_1', 'I2B', 'IRETURN']
        )
    }

    void testIdentityReturns() {
        def bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            int m(int i) { i }
        ''')
        assert bytecode.hasStrictSequence(
                ['ILOAD', 'IRETURN']
        )

        bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            long m(long l) { l }
        ''')
        assert bytecode.hasStrictSequence(
                ['LLOAD', 'LRETURN']
        )

        bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            short m(short l) { l }
        ''')
        assert bytecode.hasStrictSequence(
                ['ILOAD', 'IRETURN']
        )

        bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            float m(float l) { l }
        ''')
        assert bytecode.hasStrictSequence(
                ['FLOAD', 'FRETURN']
        )

        bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            double m(double l) { l }
        ''')
        assert bytecode.hasStrictSequence(
                ['DLOAD', 'DRETURN']
        )

        bytecode = compile([method:'m'],'''
            @groovy.transform.CompileStatic
            Object m(Object l) { l }
        ''')
        assert bytecode.hasStrictSequence(
                ['ALOAD', 'ARETURN']
        )
    }

    void testSingleAssignment() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m() {
            int a = 1
        }''').hasSequence([
                "ICONST_1",
                "ISTORE",
                "RETURN"
        ])
    }

    void testReturnSingleAssignment() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        int m() {
            int a = 1
        }''').hasSequence([
                "ICONST_1",
                "ISTORE",
                "ILOAD",
                "IRETURN"
        ])
    }

    void testIntLeftShift() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m() {
            int a = 1
            int b = a << 32
        }''').hasStrictSequence([
                "ILOAD",
                "BIPUSH 32",
                "ISHL"
        ])
    }

    void testLongLeftShift() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m() {
            long a = 1L
            long b = a << 32
        }''').hasStrictSequence([
                "LLOAD",
                "BIPUSH 32",
                "LSHL"
        ])
    }

    void testArrayGet() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m(int[] arr) {
            arr[0]
        }''').hasStrictSequence([
                "ALOAD 1",
                "ICONST_0",
                "INVOKESTATIC org/codehaus/groovy/runtime/BytecodeInterface8.intArrayGet ([II)I"
        ])
    }

    void testArraySet() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m(int[] arr) {
            arr[0] = 0
        }''').hasStrictSequence([
                "ICONST_0",
                "ISTORE 2",
                "ALOAD 1",
                "ICONST_0",
                "ILOAD 2",
                "INVOKESTATIC org/codehaus/groovy/runtime/BytecodeInterface8.intArraySet ([III)V"
        ])
    }

/*    void testPlusPlus() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m() {
            int i = 0
            i++
        }''').hasStrictSequence([
                "IINC",
        ])

    }

    void testMinusMinus() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m() {
            int i = 0
            i--
        }''').hasStrictSequence([
                "IINC",
        ])

    }

    void testPlusEquals() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        int m() {
            int i = 0
            i += 13
            return i
        }''').hasStrictSequence([
                "ILOAD",
                "ILOAD",
                "IADD",
                "ISTORE"
        ])
    }

    void testPlusEqualsFromArgs() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m(int i, int j) {
            i += j
        }''').hasStrictSequence([
                "ILOAD",
                "ILOAD",
                "IADD",
                "ISTORE"
        ])
    }*/

    void testFlow() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        String m(String str) {
            def obj = 1
            obj = str
            obj.toUpperCase()
        }
        m 'Cedric'
        ''').hasStrictSequence([
                "ICONST",
                "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;",
                "ASTORE",
                "L1",
                "ALOAD 2",
                "POP",
                "L2",
                "LINENUMBER",
                "ALOAD 1",
                "ASTORE 3",
                "ALOAD 3",
                "ASTORE 2",
                "ALOAD 3",
                "POP",
                "L3",
                "LINENUMBER",
                "ALOAD 2",
                "CHECKCAST java/lang/String",
                "INVOKEVIRTUAL java/lang/String.toUpperCase ()Ljava/lang/String;",
                "ARETURN",
                "L4"
        ])
    }

    void testInstanceOf() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m(Object str) {
            if (str instanceof String) {
                str.toUpperCase()
            }
        }
        m 'Cedric'
        ''').hasStrictSequence([
                "ALOAD",
                "CHECKCAST java/lang/String",
                "INVOKEVIRTUAL java/lang/String.toUpperCase ()Ljava/lang/String;"
        ])
    }

    void testShouldGenerateDirectConstructorCall() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        class Foo {
            String msg
            Foo(int x, String y) { msg = y*x }
            static Foo foo() {
                Foo result = [2,'Bar']
            }
        }
        ''').hasStrictSequence([
                'ICONST_2',
                'LDC "Bar"',
                'INVOKESPECIAL Foo.<init> (ILjava/lang/String;)V'
        ])
    }

    void testShouldGenerateDirectArrayConstruct() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m() {
            int[] arr = [123,456]
        }
        ''').hasStrictSequence([
                'ICONST_2',
                'NEWARRAY T_INT',
                'DUP',
                'ICONST_0',
                'BIPUSH 123',
                'IASTORE'
        ])
    }

    void testShouldGenerateDirectBooleanArrayConstruct() {
        assert compile([method:'m'],'''
        @groovy.transform.CompileStatic
        void m() {
            boolean[] arr = [123,false]
        }
        ''').hasStrictSequence([
                'ICONST_2',
                'NEWARRAY T_BOOLEAN',
                'DUP',
                'ICONST_0',
                'BIPUSH 123',
                'INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;',
                'INVOKESTATIC org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation.booleanUnbox (Ljava/lang/Object;)Z',
                'BASTORE'
        ])
    }

    void testShouldTriggerDirectCallToOuterClassGetter() {
        assert compile([method: 'fromInner',classNamePattern:'.*Inner.*'], '''
class Holder {
    String value
}

@groovy.transform.CompileStatic
class Outer {
    String outerProperty = 'outer'
    private class Inner {
        String fromInner() {
            Holder holder = new Holder()
            holder.value = outerProperty
            holder.value
        }
    }

    String blah() {
        new Inner().fromInner()
    }
}

def o = new Outer()
assert o.blah() == 'outer'
''').hasStrictSequence([
        'GETFIELD Outer$Inner.this$0',
        'INVOKEVIRTUAL Outer.getOuterProperty',
        'DUP',
        'ASTORE',
        'ALOAD',
        'ALOAD',
        'INVOKEVIRTUAL Holder.setValue'
])
    }

    void testShouldOptimizeBytecodeByAvoidingCreationOfMopMethods() {
        def shell = new GroovyShell()
        def clazz = shell.evaluate '''
            import groovy.transform.TypeCheckingMode
            import groovy.transform.CompileStatic

            @CompileStatic
            class A {
                def doSomething() { 'A' }
            }

            @CompileStatic
            class B extends A {
                def doSomething() { 'B' + super.doSomething() }
            }

            B
        '''
        assert clazz instanceof Class
        assert clazz.name == 'B'
        def mopMethods = clazz.declaredMethods.findAll { it.name =~ /(super|this)\$/ }
        assert mopMethods.empty
    }

    void testShouldNotOptimizeBytecodeForMopMethodsBecauseOfSkip() {
        def shell = new GroovyShell()
        def clazz = shell.evaluate '''
            import groovy.transform.TypeCheckingMode
            import groovy.transform.CompileStatic

            @CompileStatic
            class A {
                def doSomething() { 'A' }
            }

            @CompileStatic
            class B extends A {
                @CompileStatic(TypeCheckingMode.SKIP)
                def doSomething() { 'B' + super.doSomething() }
            }

            B
        '''
        assert clazz instanceof Class
        assert clazz.name == 'B'
        def mopMethods = clazz.declaredMethods.findAll { it.name =~ /(super|this)\$/ }
        assert !mopMethods.empty
    }


}
