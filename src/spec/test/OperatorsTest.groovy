import gls.CompilableTestSupport

class OperatorsTest extends CompilableTestSupport {

    void testArithmeticOperators() {
        // tag::binary_arith_ops[]
        assert  1  + 2 == 3
        assert  4  - 3 == 1
        assert  3  * 5 == 15
        assert  3  / 2 == 1.5
        assert 10  % 3 == 1
        assert  2 ** 3 == 8
        // end::binary_arith_ops[]

        // tag::unary_plus_minus[]
        assert +3 == 3
        assert -4 == 0 - 4

        assert -(-1) == 1  // <1>
        // end::unary_plus_minus[]

        // tag::plusplus_minusminus[]
        def a = 2
        def b = a++ * 3             // <1>

        assert a == 3 && b == 6

        def c = 3
        def d = c-- * 2             // <2>

        assert c == 2 && d == 6

        def e = 1
        def f = ++e + 3             // <3>

        assert e == 2 && f == 5

        def g = 4
        def h = --g + 1             // <4>

        assert g == 3 && h == 4
        // end::plusplus_minusminus[]
    }

    void testArithmeticOperatorsWithAssignment() {
        // tag::binary_assign_operators[]
        def a = 4
        a += 3

        assert a == 7

        def b = 5
        b -= 3

        assert b == 2

        def c = 5
        c *= 3

        assert c == 15

        def d = 10
        d /= 2

        assert d == 5

        def e = 10
        e %= 3

        assert e == 1
        // end::binary_assign_operators[]
    }

    void testSimpleRelationalOperators() {
        // tag::simple_relational_op[]
        assert 1 + 2 == 3
        assert 3 != 4

        assert -2 < 3
        assert 2 <= 2
        assert 3 <= 4

        assert 5 > 1
        assert 5 >= -2
        // end::simple_relational_op[]
    }

    void testLogicalOperators() {
        // tag::logical_op[]
        assert !false           // <1>
        assert true && true     // <2>
        assert true || false    // <3>
        // end::logical_op[]
    }

    void testLogicalOperatorPrecedence() {
        // tag::logical_precendence_1[]
        assert !false && true    // <1>
        // end::logical_precendence_1[]

        // tag::logical_precendence_2[]
        assert false || true && true    // <1>
        // end::logical_precendence_2[]
    }

    void testLogicalOrShortCircuit() {
        assertScript '''
            // tag::logical_or_shortcircuit[]
            called = false

            boolean somethingTrueOrFalse(boolean b) {  // <1>
                called = true
                return b
            }

            assert true || somethingTrueOrFalse(false)
            assert !called                              // <2>

            assert false || somethingTrueOrFalse(true)
            assert called                               // <3>
            // end::logical_or_shortcircuit[]
        '''
    }
}