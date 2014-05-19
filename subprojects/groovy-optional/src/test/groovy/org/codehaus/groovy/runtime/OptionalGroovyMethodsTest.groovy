package org.codehaus.groovy.runtime;

import java.util.Optional;
import groovy.io.FileType
import spock.lang.Specification

class OptionalGroovyMethodsTest extends Specification {

    Optional<String> s1 = Optional.<String>of("Abc")
    Optional<String> s2 = Optional.<String>of("Def")
    Optional<Integer> i1 = Optional.<Integer>of(3)
    Optional<Integer> i2 = Optional.<Integer>of(4)
    Optional<Integer> e = Optional.empty()

    void testStringPlus1() {
        expect:
        s1+s2 == Optional.of("AbcDef")
        s1+e == Optional.empty()
        e+s2 == Optional.empty()
        e+e == Optional.empty()
    }

    void testIntegerPlus1() {
        expect:
        i1+i2 == Optional.of(7)
        i1+e == Optional.empty()
        e+i2 == Optional.empty()
        e+e == Optional.empty()
    }

    def toUpperAndToLower1(a,b) {
        a.toUpperCase()+b.toLowerCase()
    }

    void testToUpperAndToLower() {
        expect:
        toUpperAndToLower1(s1, s2) == Optional.of("ABCdef")
        toUpperAndToLower1(s1, e) == e
        toUpperAndToLower1(e, s2) == e
        toUpperAndToLower1(e, e) == e
    }

    def mult1(a, b) {
        a * 3 + b * 2
    }

    void testMult1() {
        expect:
        mult1(s1, s2) == Optional.of("AbcAbcAbcDefDef")
        mult1(e, s2) == e
        mult1(s1, e) == e
        mult1(s1, e) == e
        mult1(s1, Optional.of(3)) == Optional.of("AbcAbcAbc6")
    }

    //    @groovy.transform.TypeChecked
    Optional<Integer> mult2(Optional<Integer>a, Optional<Integer>b) {
        a * 3 + b * 2
    }

    @groovy.transform.TypeChecked
    void testMult2() {
        expect:
        mult2(i1, i2) == Optional.of(9+8)
        mult2(e, i2) == e
        mult2(i1, e) == e
        mult2(e, e) == e
    }

    // Optional.methodMissing() never worked with static method.
    // Because static method is not the method of Optional. To apply
    // static method to Optional-embedded-value, you can embed a
    // closure into Opional and call the Opitonal like
    // closure. Optional.call is defined to delagate to Closure.call()
    // on the embedded Closure, so you can apply static method to
    // Optional embedded value transparently.
    void testStatic() {
        expect:
        Optional.of({fmt,...args->String.format(fmt,*args)})("%02d", i1) == Optional.of("03")
        Optional.of(String.&format)("%02d", i1) == Optional.of("03")
        Optional.of(String.&format)(Optional.of("%02d"), i1) == Optional.of("03")
    }

    // Optional.toString() is not delegated to the toString() on the
    // embeded value. Because of compatibility to Java's originaln
    // Original behavior.
    void testToString() {
        def a = Optional.of(3.3)
        expect:
        a.toString() == "Optional[3.3]" // != "3.3"
        a.get().toString() == "3.3"
        a.map{"<"+it.toString()+">"}.get() == "<3.3>"
    }

    // Optional.toString() is not delegated to the toString() on the
    // embeded value. Because of compatibility to Java's originaln
    // Original behavior.
    void testToString() {
        def a = Optional.of(3.3)
        expect:
        a.toString() == "Optional[3.3]" // != "3.3"
        a.get().toString() == "3.3"
        a.map{"<"+it.toString()+">"}.get() == "<3.3>"
    }

    void testMap() {
        expect:
        Optional.of("abc").map{it.toUpperCase()} == Optional.of("ABC")
    }
    
    void testFlatMap() {
        def divide = {it == 0 ? Optional.empty() : Optional.of(3.0/it) }
        expect:
        Optional.of(3.0).flatMap(divide) != Optional.of(1.0) // equals i not dispatched to target.
        Optional.of(3.0).flatMap(divide).get() == Optional.of(1.0).get()
        Optional.of(0).flatMap(divide) == Optional.empty()
    }
    
    void testFilter() {
        expect:
        Optional.of(3).filter { it % 2 == 0 } == Optional.empty()
        Optional.of(3).filter { it % 2 == 1 } == Optional.of(3)
    }
    
}
