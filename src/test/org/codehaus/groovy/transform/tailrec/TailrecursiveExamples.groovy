package org.codehaus.groovy.transform.tailrec

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.TailRecursive
import org.junit.Test

/**
 * @author Johannes Link
 */
class TailRecursiveExamples {

    @Test
    void staticallyCompiledFactorial() {
        assert StaticTargetClass.factorial(1) == 1
        assert StaticTargetClass.factorial(3) == 6
        assert StaticTargetClass.factorial(10) == 3628800
        assert StaticTargetClass.factorial(20) == 2432902008176640000L
        assert StaticTargetClass.factorial(10000).bitCount() == 54134
    }

    @Test
    void staticallyCompiledSumDown() {
        def target = new StaticTargetClass()
        assert target.sumDown(0) == 0
        assert target.sumDown(5) == 5 + 4 + 3 + 2 + 1
        assert target.sumDown(100) == 5050
        assert target.sumDown(1000000) == 500000500000
    }

    @Test
    void dynamicallyCompiledRevert() {
        assert DynamicTargetClass.revert([]) == []
        assert DynamicTargetClass.revert([1]) == [1]
        assert DynamicTargetClass.revert([1, 2, 3, 4, 5]) == [5, 4, 3, 2, 1]
    }

    @Test
    void dynamicallyCompiledStringSize() {
        def target = new DynamicTargetClass()
        assert target.stringSize("") == 0
        assert target.stringSize("a") == 1
        assert target.stringSize("abcdefghijklmnopqrstuvwxyz") == 26
    }

    @Test
    void dynamicallyCompiledReduce() {
        def target = new DynamicTargetClass()
        def plus = { Number x, Number y -> x + y }
        assert target.reduce(0, plus,) == 0
        assert target.reduce(0, plus, 1) == 1
        assert target.reduce(0, plus, 1, 5, 10) == 16
        assert target.reduce(99, plus, 1, 5, 10, 98) == 213

        def numbersFrom1to1000 = (1..1000).collect { new BigInteger(it) }.toArray()
        assert target.reduce(new BigInteger(1), { BigInteger a, BigInteger b -> a * b }, numbersFrom1to1000).bitCount() == 3788
    }

    @Test
    void twoDifferentRecursiveCallsInOneMethod() {
        def target = new DynamicTargetClass()
        assert target.enumerate(1, 0) == []
        assert target.enumerate(0, 0) == [0]
        assert target.enumerate(1, 9) == [-1, 2, -3, 4, -5, 6, -7, 8, -9]
    }

    @Test
    void cpsFactorial() {
        def target = new ContinuousPassingStyle()
        assert target.factorial(1) == 1
        assert target.factorial(3) == 6
        assert target.factorial(20) == 2432902008176640000L
    }

    @Test
    void cpsFibonacci() {
        def target = new ContinuousPassingStyle()
        assert (0..7).collect { target.fibonacci(it) } == [1, 1, 2, 3, 5, 8, 13, 21]
    }

}


@CompileStatic
class StaticTargetClass {

    @TailRecursive
    static BigInteger factorial(BigInteger number, BigInteger result = 1) {
        if (number <= 1)
            return result
        return factorial(number - 1, number * result)
    }

    @TailRecursive
    long sumDown(long number, long sum = 0) {
        (number == 0) ? sum : sumDown(number - 1, sum + number)
    }
}

class DynamicTargetClass {

    @TailRecursive
    static revert(List elements, result = []) {
        if (!elements)
            return result
        else {
            def element = elements.pop()
            result.add(element)
            return revert(elements, result)
        }
    }

    @TailRecursive
    def stringSize(aString, int size = 0) {
        if (!aString)
            return size
        stringSize(aString.substring(1), ++size)
    }

    @TailRecursive
    def reduce(startValue, function, Object... elements) {
        if (elements.length == 0)
            return startValue
        def newValue = function(startValue, elements[0])
        def rest = elements.drop(1)
        return reduce(newValue, function, rest)
    }

    @TailRecursive
    def enumerate(int lower, int upper, list = []) {
        if (lower > upper)
            return list
        if (lower % 2 == 0)
            return enumerate(lower + 1, upper, list << lower)
        else
            return enumerate(lower + 1, upper, list << -lower)
    }
}

/**
 * A way to make more functions tail recursive
 */
class ContinuousPassingStyle {
    @TailRecursive
    long factorial(long number, Closure continuation = { it }) {
        if (number <= 1)
            return continuation(1)
        return factorial(number - 1, { continuation(it * number) })
    }

    /**
     * Currently this solution does not work with @TailRecursive and will run forever
     * Cause: Groovy closure are not real closures, they don't close around everything at creation time
     */
//    @TailRecursive
    int fibonacci(int n, Closure c = { it }) {
        if (n == 0)
            return c(1)
        if (n == 1)
            return c(1)
        def next = { r1 ->
            return fibonacci(n - 2) { r2 ->
                return c(r1 + r2)
            }
        }
        return fibonacci(n - 1, next)
    }
}
