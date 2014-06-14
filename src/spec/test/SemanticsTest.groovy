import gls.CompilableTestSupport

class SemanticsTest extends CompilableTestSupport {

    void testVariableDefinition() {
        // tag::variable_definition_example[]
        String x
        def o
        // end::variable_definition_example[]
    }

    void testVariableAssignment() {
        assertScript '''
        // tag::variable_assignment_example[]
        x = 1
        println x

        x = new java.util.Date()
        println x

        x = -3.1499392
        println x

        x = false
        println x

        x = "Hi"
        println x
        // end::variable_assignment_example[]
        '''
    }

    void testMultipleAssignment() {
        // tag::multiple_assignment_example[]
        def (a, b, c) = [10, 20, 'foo']
        assert a == 10 && b == 20 && c == 'foo'
        // end::multiple_assignment_example[]
    }

    void testMultipleAssignmentWithTypes() {
        // tag::multiple_assignment_with_types[]
        def (int i, String j) = [10, 'foo']
        assert i == 10 && j == 'foo'
        // end::multiple_assignment_with_types[]
    }

    void testMultipleAssignmentWithExistingVariables() {
        // tag::multiple_assignment_with_existing_variables[]
        def nums = [1, 3, 5]
        def a, b, c
        (a, b, c) = nums
        assert a == 1 && b == 3 && c == 5
        // end::multiple_assignment_with_existing_variables[]
    }

    void testMultipleAssignmentWithArraysAndLists() {
        // tag::multiple_assignment_with_arrays_and_lists[]
        def (_, month, year) = "18th June 2009".split()
        assert "In $month of $year" == 'In June of 2009'
        // end::multiple_assignment_with_arrays_and_lists[]
    }

    void testMultipleAssignmentOverflow() {
        // tag::multiple_assignment_overflow[]
        def (a, b, c) = [1, 2]
        assert a == 1 && b == 2 && c == null
        // end::multiple_assignment_overflow[]
    }

    void testMultipleAssignmentUnderflow() {
        // tag::multiple_assignment_underflow[]
        def (a, b) = [1, 2, 3]
        assert a == 1 && b == 2
        // end::multiple_assignment_underflow[]
    }

    void testIfElse() {
        // tag::if_else_example[]
        def x = false
        def y = false

        if ( !x ) {
            x = true
        }

        assert x == true

        if ( x ) {
            x = false
        } else {
            y = true
        }

        assert x == y
        // end::if_else_example[]
    }

    void testSwitchCase() {
        // tag::switch_case_example[]
        def x = 1.23
        def result = ""

        switch ( x ) {
            case "foo":
                result = "found foo"
                // lets fall through

            case "bar":
                result += "bar"

            case [4, 5, 6, 'inList']:
                result = "list"
                break

            case 12..30:
                result = "range"
                break

            case Integer:
                result = "integer"
                break

            case Number:
                result = "number"
                break

            default:
                result = "default"
        }

        assert result == "number"
        // end::switch_case_example[]
    }

    void testClassicForLoop() {
        // tag::classic_for_loop_example[]
        String message = ''
        for (i = 0; i < 5; i++) {
            message += 'Hi '
        }
        assert message == 'Hi Hi Hi Hi Hi '
        // end::classic_for_loop_example[]
    }

    void testGroovyForLoop() {
        // tag::groovy_for_loop_example[]
        // iterate over a range
        def x = 0
        for ( i in 0..9 ) {
            x += i
        }
        assert x == 45

        // iterate over a list
        x = 0
        for ( i in [0, 1, 2, 3, 4] ) {
            x += i
        }
        assert x == 10

        // iterate over an array
        array = (0..4).toArray()
        x = 0
        for ( i in array ) {
            x += i
        }
        assert x == 10

        // iterate over a map
        def map = ['abc':1, 'def':2, 'xyz':3]
        x = 0
        for ( e in map ) {
            x += e.value
        }
        assert x == 6

        // iterate over values in a map
        x = 0
        for ( v in map.values() ) {
            x += v
        }
        assert x == 6

        // iterate over the characters in a string
        def text = "abc"
        def list = []
        for (c in text) {
            list.add(c)
        }
        assert list == ["a", "b", "c"]
        // end::groovy_for_loop_example[]
    }

    void testWhileLoop() {
        // tag::while_loop_example[]
        def x = 0
        def y = 5

        while ( y-- > 0 ) {
            x++
        }

        assert x == 5
        // end::while_loop_example[]
    }

    void testTryCatch() {
        // tag::try_catch_example[]
        try {
            'moo'.toLong()   // this will generate an exception
            assert false     // asserting that this point should never be reached
        } catch ( e ) {
            assert e in NumberFormatException
        }
        // end::try_catch_example[]
    }

    void testTryCatchFinally() {
        // tag::try_catch_finally_example[]
        def z
        try {
            def i = 7, j = 0
            try {
                def k = i / j
                assert false        //never reached due to Exception in previous line
            } finally {
                z = 'reached here'  //always executed even if Exception thrown
            }
        } catch ( e ) {
            assert e in ArithmeticException
            assert z == 'reached here'
        }
        // end::try_catch_finally_example[]
    }
}