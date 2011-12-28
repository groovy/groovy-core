package groovy.util

/**
    Testing the notYetImplemented feature of GroovyTestCase.
    Todo: testing all other features.
    @author Dierk Koenig
*/

class GroovyTestCaseTest extends GroovyTestCase {

    void testNotYetImplementedSubclassUse () {
        if (notYetImplemented()) return
        fail 'here the code that is expected to fail'
    }
    void testNotYetImplementedStaticUse () {
        if (GroovyTestCase.notYetImplemented(this)) return
        fail 'here the code that is expected to fail'
    }

    // we cannot test this automatically...
    // remove the leading x, run the test and see it failing
    void xtestSubclassFailing() {
        if (notYetImplemented()) return
        assert true // passes unexpectedly
    }
    void xtestStaticFailing() {
        if (GroovyTestCase.notYetImplemented(this)) return
        assert true // passes unexpectedly
    }

// ----------------

    void testShouldFailReturnsException() {
        def throwable = shouldFail { throw new MyException(new NullPointerException()) }        
        assertEquals MyException, throwable?.class
    }
    void testShouldFailForClassReturnsException() {
    	def throwable = shouldFail(RuntimeException.class) { throw new MyException(new RuntimeException('x')) }
    	assertEquals MyException, throwable?.class
    }

    void testShouldFail() {
        shouldFail(MyException) {
            new Foo().createBar()
        }
    }

    void testShouldFailWithNestedException() {
        shouldFail(MyException) {
            new Foo().createBarWithNestedException()
        }
    }
    
    void testShouldFailOnUnexpectedExceptionShouldFail() {
    	shouldFail {
    		shouldFail(MyException) {
    			throw new NullPointerException()
    		}
    	}
    }
    	
}

class Foo {
    def createBar() {
        throw new MyException(null)
    }

    def createBarWithNestedException() {
        throw new MyException(new NullPointerException())
    }
}

class MyException extends RuntimeException {
    MyException(Throwable cause) {
        super(cause);
    }
}
