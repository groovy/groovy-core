/*
 * Copyright 2003-2013 the original author or authors.
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

class CompilerDirectivesASTTransformsTest extends GroovyTestCase {
    void testFieldXForm() {
        shouldFail(MissingPropertyException) {
            assertScript '''
// tag::field_missing_property[]
def x

String line() {
    "="*x
}

x=3
assert "===" == line()
x=5
assert "=====" == line()
// end::field_missing_property[]
'''
        }

        shouldFail(MissingPropertyException) {
            assertScript '''
// tag::field_missing_property_equiv[]
class MyScript extends Script {

    String line() {
        "="*x
    }

    public def run() {
        def x
        x=3
        assert "===" == line()
        x=5
        assert "=====" == line()
    }
}
// end::field_missing_property_equiv[]
new MyScript().run()
'''
        }

        assertScript '''import groovy.transform.Field

// tag::field_fixed[]
@Field def x

String line() {
    "="*x
}

x=3
assert "===" == line()
x=5
assert "=====" == line()
// end::field_fixed[]

'''

        assertScript '''
// tag::field_fixed_equiv[]
class MyScript extends Script {

    def x

    String line() {
        "="*x
    }

    public def run() {
        x=3
        assert "===" == line()
        x=5
        assert "=====" == line()
    }
}
// end::field_fixed_equiv[]
new MyScript().run()
'''
    }

    void testPackageScope() {
        assertScript '''import java.lang.reflect.Modifier
// tag::packagescope_property[]
class Person {
    String name // this is a property
}
// end::packagescope_property[]
assert Modifier.toString(Person.getDeclaredField('name').modifiers)=='private'
'''

        assertScript '''import groovy.transform.PackageScope
import java.lang.reflect.Modifier
// tag::packagescope_property_javalike[]
class Person {
    @PackageScope String name // not a property anymore
}
// end::packagescope_property_javalike[]
assert Modifier.toString(Person.getDeclaredField('name').modifiers)==''
'''
    }
}
