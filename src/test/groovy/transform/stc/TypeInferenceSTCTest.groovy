/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.transform.stc

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.transform.stc.StaticTypesMarker

/**
 * Unit tests for static type checking : type inference.
 *
 * @author Cedric Champeau
 */
class TypeInferenceSTCTest extends StaticTypeCheckingTestCase {

    void testStringToInteger() {
        assertScript """
        def name = "123" // we want type inference
        name.toInteger() // toInteger() is defined by DGM
        """
    }

    void testGStringMethods() {
        assertScript '''
            def myname = 'Cedric'
            "My upper case name is ${myname.toUpperCase()}"
            println "My upper case name is ${myname}".toUpperCase()
        '''
    }

    void testAnnotationOnSingleMethod() {
        GroovyShell shell = new GroovyShell()
        shell.evaluate '''
            // calling a method which has got some dynamic stuff in it

            import groovy.transform.TypeChecked
            import groovy.xml.MarkupBuilder

            class Greeter {
                @TypeChecked
                String greeting(String name) {
                    generateMarkup(name.toUpperCase())
                }

                // MarkupBuilder is dynamic so we won't do typechecking here
                String generateMarkup(String name) {
                    def sw = new StringWriter()
                    def mkp = new MarkupBuilder()
                    mkp.html {
                        body {
                            div name
                        }
                    }
                    sw
                }
            }

            def g = new Greeter()
            g.greeting("Guillaume")

        '''
    }

    void testInstanceOf() {
        assertScript """
        Object o
        if (o instanceof String) o.toUpperCase()
        """
    }

    void testEmbeddedInstanceOf() {
        assertScript """
        Object o
        if (o instanceof Object) {
            if (o instanceof String) {
                o.toUpperCase()
            }
        }
        """
    }

    void testEmbeddedInstanceOf2() {
        assertScript """
        Object o
        if (o instanceof String) {
            if (true) {
                o.toUpperCase()
            }
        }
        """
    }

    void testEmbeddedInstanceOf3() {
        shouldFailWithMessages '''
        Object o
        if (o instanceof String) {
            if (o instanceof Object) { // causes the inferred type of 'o' to be overwritten
                o.toUpperCase()
            }
        }
        ''', 'Cannot find matching method java.lang.Object#toUpperCase()'
    }

    void testInstanceOfAfterEach() {
        shouldFailWithMessages '''
            Object o
            if (o instanceof String) {
               o.toUpperCase()
            }
            o.toUpperCase() // ensure that type information is lost after if()
        ''', 'Cannot find matching method java.lang.Object#toUpperCase()'
    }

    void testInstanceOfInElseBranch() {
        shouldFailWithMessages '''
            Object o
            if (o instanceof String) {
               o.toUpperCase()
            } else {
                o.toUpperCase() // ensure that type information is lost in else()
            }
        ''', 'Cannot find matching method java.lang.Object#toUpperCase()'
    }

    void testMultipleInstanceOf() {
        assertScript '''
            class A {
               void foo() { println 'ok' }
            }

            class B {
               void foo() { println 'ok' }
               void foo2() { println 'ok 2' }
            }


            def o = new A()

            if (o instanceof A) {
               o.foo()
            }

            if (o instanceof B) {
               o.foo()
            }

            if (o instanceof A || o instanceof B) {
              o.foo()
            }

        '''
    }

    void testInstanceOfInTernaryOp() {
        assertScript '''
            class A {
               int foo() { 1 }
            }

            class B {
               int foo2() { 2 }
            }


            def o = new A()

            int result = o instanceof A?o.foo():(o instanceof B?o.foo2():3)

        '''
    }

    void testShouldNotAllowDynamicVariable() {
        shouldFailWithMessages '''
            String name = 'Guillaume'
            println naamme
        ''', 'The variable [naamme] is undeclared'
    }

    void testInstanceOfInferenceWithImplicitIt() {
        assertScript '''
        ['a', 'b', 'c'].each {
            if (it instanceof String) {
                println it.toUpperCase()
            }
        }
        '''
    }

    void testInstanceOfTypeInferenceWithDef() {
        assertScript '''
            def profile = ['Guillaume', 34, true]
            def item = profile[0]
            if (item instanceof String) {
                println item.toUpperCase()
            }
        '''
    }

    void testInstanceOfTypeInferenceWithoutDef() {
        assertScript '''
            def profile = ['Guillaume', 34, true]
            if (profile[0] instanceof String) {
                println profile[0].toUpperCase()
            }
        '''
    }

    void testInstanceOfInferenceWithField() {
        assertScript '''
            class A {
                int x
            }
            def a
            if (a instanceof A) {
                a.x = 2
            }
        '''
    }

    void testInstanceOfInferenceWithFieldAndAssignment() {
        shouldFailWithMessages '''
            class A {
                int x
            }
            def a = new A()
            if (a instanceof A) {
                a.x = '2'
            }
        ''', 'Cannot assign value of type java.lang.String to variable of type int'
    }

    void testInstanceOfInferenceWithMissingField() {
        shouldFailWithMessages '''
            class A {
                int x
            }
            def a
            if (a instanceof A) {
                a.y = 2
            }
        ''', 'No such property: y for class: A'
    }

    void testShouldNotFailWithWith() {
        assertScript '''
            class A {
                int x
            }
            def a = new A()
            a.with {
                x = 2 // should be recognized as a.x at compile time
            }
        '''
    }

    void testShouldFailWithWith() {
        shouldFailWithMessages '''
            class A {
                int x
            }
            def a = new A()
            a.with {
                x = '2' // should be recognized as a.x at compile time and fail because of wrong type
            }
        ''', 'Cannot assign value of type java.lang.String to variable of type int'
    }

    void testShouldNotFailWithWithTwoClasses() {
        // we must make sure that type inference engine in this case
        // takes the same property as at runtime
        assertScript '''
            class A {
                int x
            }
            class B {
                String x
            }
            def a = new A()
            def b = new B()
            a.with {
                b.with {
                    x = '2' // should be recognized as b.x at compile time
                }
            }
        '''
    }

    void testShouldNotFailWithWithAndImplicitIt() {
        assertScript '''
            class A {
                int x
            }
            def a = new A()
            a.with {
                it.x = 2 // should be recognized as a.x at compile time
            }
        '''
    }

    void testShouldNotFailWithWithAndExplicitIt() {
        assertScript '''
            class A {
                int x
            }
            def a = new A()
            a.with { it ->
                it.x = 2 // should be recognized as a.x at compile time
            }
        '''
    }

    void testShouldNotFailWithWithAndExplicitTypedIt() {
        shouldFailWithMessages '''
            class A {
                int x
            }
            def a = new A()
            a.with { String it ->
                it.x = 2 // should be recognized as a.x at compile time
            }
        ''', 'Expected parameter type: A but was: java.lang.String'
    }

    void testShouldNotFailWithInheritanceAndWith() {
         assertScript '''
             class A {
                 int x
                 void method() { println x }
             }
             class B extends A {
             }
             def b = new B()
             b.with {
                 x = 2 // should be recognized as b.x at compile time
             }
         '''
    }

    void testCallMethodInWithContext() {
        assertScript '''
            class A {
                int method() { return 1 }
            }
            def a = new A()
            a.with {
                method()
            }
        '''
    }


   void testCallMethodInWithContextAndShadowing() {
       // make sure that the method which is found in 'with' is actually the one from class A
       // which returns a String
       assertScript '''
            class A {
                String method() { return 'Cedric' }
            }

            int method() { 1 }

            def a = new A()
            a.with {
                method().toUpperCase()
            }
        '''
       // check that if we switch signatures, it fails
       shouldFailWithMessages '''
            class A {
                int method() { 1 }
            }

            String method() { 'Cedric' }

            def a = new A()
            a.with {
                method().toUpperCase()
            }
        ''', 'Cannot find matching method int#toUpperCase()'
   }

    void testDeclarationTypeInference() {
        MethodNode method
        config.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CLASS_GENERATION) {
            @Override
            void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
                method = classNode.methods.find { it.name == 'method' }
            }

        })
        assertScript '''
            void method() {
                def o
                o = 1
                o = 'String'
            }
        '''
        assert method.code.statements[0].expression.leftExpression.getNodeMetaData(StaticTypesMarker.DECLARATION_INFERRED_TYPE) == ClassHelper.make(Comparable)

        assertScript '''
            void method() {
                def o
                o = 1
                o = 2
            }
        '''
        assert method.code.statements[0].expression.leftExpression.getNodeMetaData(StaticTypesMarker.DECLARATION_INFERRED_TYPE) == ClassHelper.int_TYPE

        assertScript '''
            void method() {
                def o
                o = 1L
                o = 2
            }
        '''
        assert method.code.statements[0].expression.leftExpression.getNodeMetaData(StaticTypesMarker.DECLARATION_INFERRED_TYPE) == ClassHelper.Number_TYPE

        assertScript '''
            void method() {
                def o
                o = new HashSet()
                o = new LinkedHashSet()
            }
        '''
        assert method.code.statements[0].expression.leftExpression.getNodeMetaData(StaticTypesMarker.DECLARATION_INFERRED_TYPE) == ClassHelper.make(HashSet)


    }

    void testChooseMethodWithTypeInference() {
        assertScript '''
            void method(Object o) { println 'Object' }
            void method(int i) { println 'int' }
            def obj = 1
            method(obj)
        '''
    }

    void testStarOperatorOnMap() {
        assertScript '''
            List keys = [x:1,y:2,z:3]*.key
            List values = [x:1,y:2,z:3]*.value
        '''
    }

    void testStarOperatorOnMap2() {
        assertScript '''
            List keys = [x:1,y:2,z:3]*.key
            List values = [x:'1',y:'2',z:'3']*.value
            keys*.toUpperCase()
            values*.toUpperCase()
        '''
        
        shouldFailWithMessages '''
            List values = [x:1,y:2,z:3]*.value
            values*.toUpperCase()
        ''', 'Cannot find matching method java.lang.Integer#toUpperCase()'
    }

    void testStarOperatorOnMap3() {
        assertScript '''
            def keys = [x:1,y:2,z:3]*.key
            def values = [x:'1',y:'2',z:'3']*.value
            keys*.toUpperCase()
            values*.toUpperCase()
        '''

        shouldFailWithMessages '''
            def values = [x:1,y:2,z:3]*.value
            values*.toUpperCase()
        ''', 'Cannot find matching method java.lang.Integer#toUpperCase()'
    }

    void testFlowTypingWithStringVariable() {
        // as anything can be assigned to a string, flow typing engine
        // could "erase" the type of the original variable although is must not
        assertScript '''
            String str = new Object() // type checker will not complain, anything assignable to a String
            str.toUpperCase() // should not complain
        '''
    }
}

