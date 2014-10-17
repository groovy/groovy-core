/*
 * Copyright 2003-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.macro.matcher

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.MethodPointerExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.macro.transform.MacroClass

class ASTMatcherTest extends GroovyTestCase {
    void testMatchesSimpleVar() {
        def ast = macro { a }
        assert ASTMatcher.matches(ast, ast)
    }
    void testMatchesSimpleVarNeg() {
        def ast = macro { a }
        def ast2 = macro { b }
        assert !ASTMatcher.matches(ast, ast2)
        assert !ASTMatcher.matches(ast2, ast)
    }

    void testBinaryExpression() {
        def ast1 = macro { a+b }
        def ast2 = macro { a+b }
        def ast3 = macro { b+a }
        def ast4 = macro { a-b }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testMethodCallExpression() {
        def ast1 = macro { foo() }
        def ast2 = macro { foo() }
        def ast3 = macro { this.foo() }
        def ast4 = macro { bar() }
        def ast5 = macro { foo(bar) }
        def ast6 = macro { foo(bar())}
        def ast7 = macro { foo(bar())}
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert ASTMatcher.matches(ast6, ast7)
    }

    void testPropertyExpression() {
        def ast1 = macro { this.p }
        def ast2 = macro { this.p }
        def ast3 = macro { that.p }
        def ast4 = macro { this.p2 }
        def ast5 = macro { this?.p }
        def ast6 = macro { this*.p }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast1, ast6)
    }

    void testAttributeExpression() {
        def ast1 = macro { this.@p }
        def ast2 = macro { this.@p }
        def ast3 = macro { that.@p }
        def ast4 = macro { this.@p2 }
        def ast5 = macro { this?.@p }
        def ast6 = macro { this*.@p }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast1, ast6)
    }

    void testClassExpression() {
        def ast1 = macro(CompilePhase.SEMANTIC_ANALYSIS) { String }
        def ast2 = macro(CompilePhase.SEMANTIC_ANALYSIS) { String }
        def ast3 = macro(CompilePhase.SEMANTIC_ANALYSIS) { Boolean }
        assert ast1 instanceof ClassExpression
        assert ast2 instanceof ClassExpression
        assert ast3 instanceof ClassExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
    }

    void testTernaryExpression() {
        def ast1 = macro { a?b:c }
        def ast2 = macro { a?b:c }
        def ast3 = macro { b?b:c }
        def ast4 = macro { a?a:c }
        def ast5 = macro { a?b:a }
        def ast6 = macro { a?(a+b):a }
        def ast7 = macro { a?(a+b):a }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert ASTMatcher.matches(ast6, ast7)
    }

    void testElvis() {
        def ast1 = macro { a?:c }
        def ast2 = macro { a?:c }
        def ast3 = macro { b?:c }
        def ast4 = macro { a?:a }
        def ast5 = macro { a?:(a+b) }
        def ast6 = macro { a?:(a+b) }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert ASTMatcher.matches(ast5, ast6)
    }

    void testPrefixExpression() {
        def ast1 = macro { ++a }
        def ast2 = macro { ++a }
        def ast3 = macro { ++b }
        def ast4 = macro { --a }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testPostfixExpression() {
        def ast1 = macro { a++ }
        def ast2 = macro { a++ }
        def ast3 = macro { b++ }
        def ast4 = macro { a-- }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testConstructorCall() {
        def ast1 = macro { new Foo() }
        def ast2 = macro { new Foo() }
        def ast3 = macro { new Bar() }
        def ast4 = macro { new Foo(bar) }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testDeclaration() {
        def ast1 = macro { def x = 1 }
        def ast2 = macro { def x = 1 }
        def ast3 = macro { int x = 1 }
        def ast4 = macro { def y = 1 }
        def ast5 = macro { def x = 2 }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
    }

    void testBooleanExpression() {
        def ast1 = macro { a==1 }
        def ast2 = macro { a==1 }
        def ast3 = macro { 1==a }
        def ast4 = macro { a==a }
        def ast5 = macro { a==2 }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
    }

    void testClosureExpression() {
        def ast1 = macro { {-> a } }
        def ast2 = macro { {-> a } }
        def ast3 = macro { {-> b } }
        def ast4 = macro { { a -> a } }
        def ast5 = macro { { a -> a } }
        def ast6 = macro { { a,b -> a } }
        def ast7 = macro { { int a -> a } }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert ASTMatcher.matches(ast4, ast5)
        assert !ASTMatcher.matches(ast5, ast6)
        assert !ASTMatcher.matches(ast5, ast7)
    }

    void testNotExpression() {
        def ast1 = macro { !a }
        def ast2 = macro { !a }
        def ast3 = macro { a }
        def ast4 = macro { !b }
        def ast5 = macro { !(a+b) }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
    }

    void testMapExpression() {
        def ast1 = macro { [:] }
        def ast2 = macro { [:] }
        def ast3 = macro { [a:''] }
        def ast4 = macro { [b:''] }
        def ast5 = macro { [a:'a'] }
        def ast6 = macro { [a:'a',b:'b'] }
        def ast7 = macro { [b:'b', a:'a'] }
        def ast8 = macro { [a:'a', *:'b'] }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast4, ast5)
        assert !ASTMatcher.matches(ast5, ast6)
        assert !ASTMatcher.matches(ast6, ast7)
        assert !ASTMatcher.matches(ast3, ast8)
        assert !ASTMatcher.matches(ast6, ast8)
    }

    void testRangeExpression() {
        def ast1 = macro { (0..10) }
        def ast2 = macro { (0..10) }
        def ast3 = macro { (1..10) }
        def ast4 = macro { (0..9) }
        def ast5 = macro { ('a'..'z') }
        def ast6 = macro { (0.0..10.0) }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast1, ast6)
    }

    void testListExpression() {
        def ast1 = macro { [] }
        def ast2 = macro { [] }
        def ast3 = macro { [a] }
        def ast4 = macro { [a] }
        def ast5 = macro { [b] }
        def ast6 = macro { [a,b] }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert ASTMatcher.matches(ast3, ast3)
        assert ASTMatcher.matches(ast3, ast4)
        assert ASTMatcher.matches(ast4, ast3)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast4, ast5)
        assert !ASTMatcher.matches(ast5, ast4)
        assert !ASTMatcher.matches(ast5, ast6)
        assert !ASTMatcher.matches(ast1, ast6)
    }

    void testSpreadExpression() {
        def ast1 = macro { [*a] }
        def ast2 = macro { [*a] }
        def ast3 = macro { [*b] }
        def ast4 = macro { [a] }
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testArrayExpression() {
        def ast1 = macro { new Integer[0] }
        def ast2 = macro { new Integer[0] }
        def ast3 = macro { new Integer[1] }
        def ast4 = macro { new int[0] }
        def ast5 = macro { new Integer[a] }
        assert ast1 instanceof ArrayExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
    }

    void testMethodPointerExpression() {
        def ast1 = macro { this.&foo }
        def ast2 = macro { this.&foo }
        def ast3 = macro { that.&foo }
        def ast4 = macro { this.&bar }
        assert ast1 instanceof MethodPointerExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testUnaryMinus() {
        def ast1 = macro { -a }
        def ast2 = macro { -a }
        def ast3 = macro { -0 }
        def ast4 = macro { a }
        assert ast1 instanceof UnaryMinusExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testUnaryPlus() {
        def ast1 = macro { +a }
        def ast2 = macro { +a }
        def ast3 = macro { +0 }
        def ast4 = macro { a }
        assert ast1 instanceof UnaryPlusExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testBitwiseNegate() {
        def ast1 = macro { ~a }
        def ast2 = macro { ~a }
        def ast3 = macro { ~0 }
        def ast4 = macro { a }
        assert ast1 instanceof BitwiseNegationExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testCastExpression() {
        def ast1 = macro { (String) foo }
        def ast2 = macro { (String) foo }
        def ast3 = macro { (Integer) foo }
        def ast4 = macro { (String) bar }
        assert ast1 instanceof CastExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testGStringExpression() {
        def ast1 = macro { "123$a" }
        def ast2 = macro { "123$a" }
        def ast3 = macro { "$a" }
        def ast4 = macro { "123$b" }
        assert ast1 instanceof GStringExpression
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testClassComparison() {
        def ast1 = new MacroClass() {
            class A {}
        }
        def ast2 = new MacroClass() {
            class A {}
        }
        def ast3 = new MacroClass() {
            class A implements Serializable {}
        }
        def ast4 = new MacroClass() {
            class B {}
        }
        def ast5 = new MacroClass() {
            class A extends B {}
        }
        assert ast1 instanceof ClassNode
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
    }

    void testPropertyComparison() {
        def ast1 = new MacroClass() {
            class A { String str }
        }
        def ast2 = new MacroClass() {
            class A { String str }
        }
        def ast3 = new MacroClass() {
            class A { String foo }
        }
        def ast4 = new MacroClass() {
            class A { Integer str }
        }
        def ast5 = new MacroClass() {
            class A { String str = null }
        }
        def ast6 = new MacroClass() {
            class A { String str = 'foo' }
        }
        def ast7 = new MacroClass() {
            class A { String str = 'bar' }
        }
        def ast8 = new MacroClass() {
            class A { @Foo String str }
        }
        def ast9 = new MacroClass() {
            class A { @Bar String str }
        }
        assert ast1 instanceof ClassNode
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast1, ast6)
        assert !ASTMatcher.matches(ast6, ast7)
        assert !ASTMatcher.matches(ast1, ast8)
        assert ASTMatcher.matches(ast8, ast8)
        assert !ASTMatcher.matches(ast8, ast9)
    }
    void testFieldComparison() {
        def ast1 = new MacroClass() {
            class A { public String str }
        }
        def ast2 = new MacroClass() {
            class A { public String str }
        }
        def ast3 = new MacroClass() {
            class A { public String foo }
        }
        def ast4 = new MacroClass() {
            class A { public Integer str }
        }
        def ast5 = new MacroClass() {
            class A { public String str = null }
        }
        def ast6 = new MacroClass() {
            class A { public String str = 'foo' }
        }
        def ast7 = new MacroClass() {
            class A { public String str = 'bar' }
        }
        def ast8 = new MacroClass() {
            class A { public @Foo String str }
        }
        def ast9 = new MacroClass() {
            class A { public @Bar String str }
        }
        def ast10 = new MacroClass() {
            class A { private String str }
        }
        assert ast1 instanceof ClassNode
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast1, ast6)
        assert !ASTMatcher.matches(ast6, ast7)
        assert !ASTMatcher.matches(ast1, ast8)
        assert ASTMatcher.matches(ast8, ast8)
        assert !ASTMatcher.matches(ast8, ast9)
        assert !ASTMatcher.matches(ast1, ast10)
    }

    void testIf() {
        def ast1 = macro { if (a) b }
        def ast2 = macro { if (a) b }
        def ast3 = macro { if (a) c }
        def ast4 = macro { if (b) b }
        def ast5 = macro { if (a) { b } }
        def ast6 = macro { if (a) { b } else { c }}
        def ast7 = macro { if (a) { b } else { c }}
        def ast8 = macro { if (a) { b } else { d }}
        assert ast1 instanceof IfStatement
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert !ASTMatcher.matches(ast1, ast5)
        assert !ASTMatcher.matches(ast1, ast6)
        assert ASTMatcher.matches(ast6, ast7)
        assert !ASTMatcher.matches(ast7, ast8)
    }

    void testForLoop() {
        def ast1 = macro { for (;;) {} }
        def ast2 = macro { for (;;) {} }
        def ast3 = macro { for (int i=0;i<10;i++) {} }
        def ast4 = macro { for (long i=0;i<10;i++) {} }
        def ast5 = macro { for (int i=0;i<100;i++) {} }
        def ast6 = macro { for (int i=0;i<10;i++) { a } }
        assert ast1 instanceof ForStatement
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast3, ast4)
        assert !ASTMatcher.matches(ast3, ast5)
        assert !ASTMatcher.matches(ast3, ast6)
    }

    void testWhileLoop() {
        def ast1 = macro { while (true) {} }
        def ast2 = macro { while (true) {} }
        def ast3 = macro { while (false) {} }
        def ast4 = macro { while (true) { a } }

        assert ast1 instanceof WhileStatement
        assert ASTMatcher.matches(ast1, ast1)
        assert ASTMatcher.matches(ast1, ast2)
        assert ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
    }

    void testWildcardMatchVariable() {
        def ast1 = macro { a }
        def ast2 = macro { _ }
        def ast3 = macro { b }
        assert ASTMatcher.matches(ast1, ast2)
        assert !ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast2, ast3)
    }

    void testWildcardMatchVariableInBinaryExpression() {
        def ast1 = macro { a+b }
        def ast2 = macro { _+_ }
        def ast3 = macro { _+c }
        def ast4 = macro { c+_ }
        def ast5 = macro { a+_ }
        def ast6 = macro { _+b }
        assert ASTMatcher.matches(ast1, ast2)
        assert !ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
        assert !ASTMatcher.matches(ast1, ast4)
        assert ASTMatcher.matches(ast1, ast5)
        assert ASTMatcher.matches(ast1, ast6)
    }

    void testWildcardForSubExpression() {
        def ast1 = macro { a+foo(b) }
        def ast2 = macro { _+foo(b) }
        def ast3 = macro { a+_ }
        assert ASTMatcher.matches(ast1, ast2)
        assert !ASTMatcher.matches(ast2, ast1)
        assert ASTMatcher.matches(ast1, ast3)
    }

    void testWildcardInMethodName() {
        def ast1 = macro { a+foo(b) }
        def ast2 = macro { a+_(b) }
        def ast3 = macro { a+_(c) }
        assert ASTMatcher.matches(ast1, ast2)
        assert !ASTMatcher.matches(ast2, ast1)
        assert !ASTMatcher.matches(ast1, ast3)
    }
}
