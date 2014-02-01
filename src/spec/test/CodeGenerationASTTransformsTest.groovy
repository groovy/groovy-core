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

class CodeGenerationASTTransformsTest extends GroovyTestCase {

    // specification tests for the @ToString AST transformation
    void testToString() {
        assertScript '''
// tag::tostring_import[]
import groovy.transform.ToString
// end::tostring_import[]

// tag::tostring_simple[]
@ToString
class Person {
    String firstName
    String lastName
}
// end::tostring_simple[]

// tag::tostring_simple_assert[]
def p = new Person(firstName: 'Jack', lastName: 'Nicholson')
assert p.toString() == 'Person(Jack, Nicholson)'
// end::tostring_simple_assert[]

'''
        assertScript '''
import groovy.transform.ToString

// tag::tostring_example_includeNames[]
@ToString(includeNames=true)
class Person {
    String firstName
    String lastName
}

def p = new Person(firstName: 'Jack', lastName: 'Nicholson')
assert p.toString() == 'Person(firstName:Jack, lastName:Nicholson)'
// end::tostring_example_includeNames[]

'''
        assertScript '''
import groovy.transform.ToString

// tag::tostring_example_excludes[]
@ToString(excludes=['firstName'])
class Person {
    String firstName
    String lastName
}

def p = new Person(firstName: 'Jack', lastName: 'Nicholson')
assert p.toString() == 'Person(Nicholson)'
// end::tostring_example_excludes[]

'''

        assertScript '''
import groovy.transform.ToString

// tag::tostring_example_includes[]
@ToString(includes=['lastName'])
class Person {
    String firstName
    String lastName
}

def p = new Person(firstName: 'Jack', lastName: 'Nicholson')
assert p.toString() == 'Person(Nicholson)'
// end::tostring_example_includes[]

'''

        assertScript '''
import groovy.transform.ToString

// tag::tostring_example_includeFields[]
@ToString(includeFields=true)
class Person {
    String firstName
    String lastName
    private int age
    void test() {
       age = 42
    }
}

def p = new Person(firstName: 'Jack', lastName: 'Nicholson')
p.test()
assert p.toString() == 'Person(Jack, Nicholson, 42)'
// end::tostring_example_includeFields[]

'''

        assertScript '''
import groovy.transform.ToString

// tag::tostring_example_includeSuper[]

@ToString
class Id { long id }

@ToString(includeSuper=true)
class Person extends Id {
    String firstName
    String lastName
}

def p = new Person(id:1, firstName: 'Jack', lastName: 'Nicholson')
assert p.toString() == 'Person(Jack, Nicholson, Id(1))'
// end::tostring_example_includeSuper[]

'''

        assertScript '''
import groovy.transform.ToString

// tag::tostring_example_ignoreNulls[]
@ToString(ignoreNulls=true)
class Person {
    String firstName
    String lastName
}

def p = new Person(firstName: 'Jack')
assert p.toString() == 'Person(Jack)'
// end::tostring_example_ignoreNulls[]

'''
        assertScript '''import groovy.transform.ToString

// tag::tostring_example_cache[]
@ToString(cache=true)
class Person {
    String firstName
    String lastName
}

def p = new Person(firstName: 'Jack', lastName:'Nicholson')
def s1 = p.toString()
def s2 = p.toString()
assert s1 == s2
assert s1 == 'Person(Jack, Nicholson)'
assert s1.is(s2) // same instance
// end::tostring_example_cache[]

'''

        assertScript '''package acme
import groovy.transform.ToString

// tag::tostring_example_includePackage[]
@ToString(includePackage=true)
class Person {
    String firstName
    String lastName
}

def p = new Person(firstName: 'Jack', lastName:'Nicholson')
assert p.toString() == 'acme.Person(Jack, Nicholson)'
// end::tostring_example_includePackage[]

'''

    }


    void testEqualsAndHashCode() {
        assertScript '''
// tag::equalshashcode[]
import groovy.transform.EqualsAndHashCode
@EqualsAndHashCode
class Person {
    String firstName
    String lastName
}

def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
def p2 = new Person(firstName: 'Jack', lastName: 'Nicholson')

assert p1==p2
assert p1.hashCode() == p2.hashCode()
// end::equalshashcode[]


'''
        assertScript '''
// tag::equalshashcode_example_excludes[]
import groovy.transform.EqualsAndHashCode
@EqualsAndHashCode(excludes=['firstName'])
class Person {
    String firstName
    String lastName
}

def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
def p2 = new Person(firstName: 'Bob', lastName: 'Nicholson')

assert p1==p2
assert p1.hashCode() == p2.hashCode()
// end::equalshashcode_example_excludes[]

'''

        assertScript '''
// tag::equalshashcode_example_includes[]
import groovy.transform.EqualsAndHashCode
@EqualsAndHashCode(includes=['lastName'])
class Person {
    String firstName
    String lastName
}

def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
def p2 = new Person(firstName: 'Bob', lastName: 'Nicholson')

assert p1==p2
assert p1.hashCode() == p2.hashCode()
// end::equalshashcode_example_includes[]

'''

        assertScript '''
// tag::equalshashcode_example_super[]
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Living {
    String race
}

@EqualsAndHashCode(callSuper=true)
class Person extends Living {
    String firstName
    String lastName
}

def p1 = new Person(race:'Human', firstName: 'Jack', lastName: 'Nicholson')
def p2 = new Person(race: 'Human beeing', firstName: 'Jack', lastName: 'Nicholson')

assert p1!=p2
assert p1.hashCode() != p2.hashCode()
// end::equalshashcode_example_super[]

'''
    }

    void testTupleConstructor() {
        assertScript '''
// tag::tupleconstructor_simple[]
import groovy.transform.TupleConstructor

@TupleConstructor
class Person {
    String firstName
    String lastName
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
// generated tuple constructor
def p2 = new Person('Jack', 'Nicholson')
// generated tuple constructor with default value for second property
def p3 = new Person('Jack')

// end::tupleconstructor_simple[]

assert p1.firstName == p2.firstName
assert p1.firstName == p3.firstName
assert p1.lastName == p2.lastName
assert p3.lastName == null
'''

        assertScript '''
// tag::tupleconstructor_example_excludes[]
import groovy.transform.TupleConstructor

@TupleConstructor(excludes=['lastName'])
class Person {
    String firstName
    String lastName
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
// generated tuple constructor
def p2 = new Person('Jack')
try {
    // will fail because the second property is excluded
    def p3 = new Person('Jack', 'Nicholson')
} catch (e) {
    assert e.message.contains ('Could not find matching constructor')
}
// end::tupleconstructor_example_excludes[]

assert p1.firstName == p2.firstName
assert p2.lastName == null
'''

        assertScript '''
// tag::tupleconstructor_example_includes[]
import groovy.transform.TupleConstructor

@TupleConstructor(includes=['firstName'])
class Person {
    String firstName
    String lastName
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
// generated tuple constructor
def p2 = new Person('Jack')
try {
    // will fail because the second property is not included
    def p3 = new Person('Jack', 'Nicholson')
} catch (e) {
    assert e.message.contains ('Could not find matching constructor')
}
// end::tupleconstructor_example_includes[]

assert p1.firstName == p2.firstName
assert p2.lastName == null
'''

        assertScript '''
// tag::tupleconstructor_example_includeFields[]
import groovy.transform.TupleConstructor

@TupleConstructor(includeFields=true)
class Person {
    String firstName
    String lastName
    private String occupation
    public String toString() {
        "$firstName $lastName: $occupation"
    }
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson', occupation: 'Actor')
// generated tuple constructor
def p2 = new Person('Jack', 'Nicholson', 'Actor')

assert p1.firstName == p2.firstName
assert p1.lastName == p2.lastName
assert p1.toString() == 'Jack Nicholson: Actor'
assert p1.toString() == p2.toString()
// end::tupleconstructor_example_includeFields[]
'''

       assertScript '''
// tag::tupleconstructor_example_includeSuperFields[]
import groovy.transform.TupleConstructor

class Base {
    protected String occupation
    public String occupation() { this.occupation }
}

@TupleConstructor(includeSuperFields=true)
class Person extends Base {
    String firstName
    String lastName
    public String toString() {
        "$firstName $lastName: ${occupation()}"
    }
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson', occupation: 'Actor')

// generated tuple constructor, super fields come first
def p2 = new Person('Actor', 'Jack', 'Nicholson')

assert p1.firstName == p2.firstName
assert p1.lastName == p2.lastName
assert p1.toString() == 'Jack Nicholson: Actor'
assert p2.toString() == p1.toString()
// end::tupleconstructor_example_includeSuperFields[]
'''

        assertScript '''
// tag::tupleconstructor_example_includeProperties[]
import groovy.transform.TupleConstructor

@TupleConstructor(includeProperties=false)
class Person {
    String firstName
    String lastName
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')

try {
    def p2 = new Person('Jack', 'Nicholson')
} catch(e) {
    // will fail because properties are not included
}
// end::tupleconstructor_example_includeProperties[]
'''

        assertScript '''
// tag::tupleconstructor_example_includeSuperProperties[]
import groovy.transform.TupleConstructor

class Base {
    String occupation
}

@TupleConstructor(includeSuperProperties=true)
class Person extends Base {
    String firstName
    String lastName
    public String toString() {
        "$firstName $lastName: $occupation"
    }
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')

// generated tuple constructor, super properties come first
def p2 = new Person('Actor', 'Jack', 'Nicholson')

assert p1.firstName == p2.firstName
assert p1.lastName == p2.lastName
assert p1.toString() == 'Jack Nicholson: null'
assert p2.toString() == 'Jack Nicholson: Actor'
// end::tupleconstructor_example_includeSuperProperties[]
'''

        assertScript '''
// tag::tupleconstructor_example_includeSuperProperties[]
import groovy.transform.TupleConstructor

class Base {
    String occupation
    Base() {}
    Base(String job) { occupation = job?.toLowerCase() }
}

@TupleConstructor(includeSuperProperties = true, callSuper=true)
class Person extends Base {
    String firstName
    String lastName
    public String toString() {
        "$firstName $lastName: $occupation"
    }
}

// traditional map-style constructor
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')

// generated tuple constructor, super properties come first
def p2 = new Person('ACTOR', 'Jack', 'Nicholson')

assert p1.firstName == p2.firstName
assert p1.lastName == p2.lastName
assert p1.toString() == 'Jack Nicholson: null'
assert p2.toString() == 'Jack Nicholson: actor'
// end::tupleconstructor_example_includeSuperProperties[]
'''
    }

    void testCanonical() {
        assertScript '''
// tag::canonical_simple[]
import groovy.transform.Canonical

@Canonical
class Person {
    String firstName
    String lastName
}
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
assert p1.toString() == 'Person(Jack, Nicholson)' // Effect of @ToString

def p2 = new Person('Jack','Nicholson') // Effect of @TupleConstructor
assert p2.toString() == 'Person(Jack, Nicholson)'

assert p1==p2 // Effect of @EqualsAndHashCode
assert p1.hashCode()==p2.hashCode() // Effect of @EqualsAndHashCode
// end::canonical_simple[]
'''

        assertScript '''
// tag::canonical_example_excludes[]
import groovy.transform.Canonical

@Canonical(excludes=['lastName'])
class Person {
    String firstName
    String lastName
}
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
assert p1.toString() == 'Person(Jack)' // Effect of @ToString

def p2 = new Person('Jack') // Effect of @TupleConstructor
assert p2.toString() == 'Person(Jack)'

assert p1==p2 // Effect of @EqualsAndHashCode
assert p1.hashCode()==p2.hashCode() // Effect of @EqualsAndHashCode
// end::canonical_example_excludes[]
'''

        assertScript '''
// tag::canonical_example_includes[]
import groovy.transform.Canonical

@Canonical(includes=['firstName'])
class Person {
    String firstName
    String lastName
}
def p1 = new Person(firstName: 'Jack', lastName: 'Nicholson')
assert p1.toString() == 'Person(Jack)' // Effect of @ToString

def p2 = new Person('Jack') // Effect of @TupleConstructor
assert p2.toString() == 'Person(Jack)'

assert p1==p2 // Effect of @EqualsAndHashCode
assert p1.hashCode()==p2.hashCode() // Effect of @EqualsAndHashCode
// end::canonical_example_includes[]
'''
    }

    void testInheritConstructors() {
        assertScript '''
// tag::inheritconstructors_simple[]
import groovy.transform.InheritConstructors

@InheritConstructors
class CustomException extends Exception {}

// all those are generated constructors
new CustomException()
new CustomException("A custom message")
new CustomException("A custom message", new RuntimeException())
new CustomException(new RuntimeException())

// Java 7 only
// new CustomException("A custom message", new RuntimeException(), false, true)
// end::inheritconstructors_simple[]
'''
    }

    void testIndexedProperty() {
        assertScript '''
import groovy.transform.IndexedProperty

// tag::indexedproperty_simple[]
class SomeBean {
    @IndexedProperty String[] someArray = new String[2]
    @IndexedProperty List someList = []
}

def bean = new SomeBean()
bean.setSomeArray(0, 'value')
bean.setSomeList(0, 123)

assert bean.someArray[0] == 'value'
assert bean.someList == [123]
// end::indexedproperty_simple[]
'''
    }

    void testLazy() {
        assertScript '''
// tag::lazy_simple[]
class SomeBean {
    @Lazy LinkedList myField
}
// end::lazy_simple[]
/* generates the following:
// tag::lazy_simple_generated[]
List $myField
List getMyField() {
    if ($myField!=null) { return $myField }
    else {
        $myField = new LinkedList()
        return $myField
    }
}
// end::lazy_simple_generated[]
*/


def bean = new SomeBean()
assert bean.@$myField == null
bean.myField // calls getter
assert bean.@$myField == []
'''

        assertScript '''
// tag::lazy_default[]
class SomeBean {
    @Lazy LinkedList myField = { ['a','b','c']}()
}
// end::lazy_default[]
/* generates the following:
// tag::lazy_default_generated[]
List $myField
List getMyField() {
    if ($myField!=null) { return $myField }
    else {
        $myField = { ['a','b','c']}()
        return $myField
    }
}
// end::lazy_default_generated[]
*/
def bean = new SomeBean()
assert bean.@$myField == null
bean.myField // calls getter
assert bean.@$myField == ['a','b','c']
'''
    }

    void testNewify() {
        assertScript '''
class Tree {
    List children
    Tree() {}
    Tree(Tree... children) { this.children = children?.toList() }
}
class Leaf extends Tree {
    String value
    Leaf(String value) { this.value = value}
}

// tag::newify_python[]
@Newify([Tree,Leaf])
class TreeBuilder {
    Tree tree = Tree(Leaf('A'),Leaf('B'),Tree(Leaf('C')))
}
// end::newify_python[]
def builder = new TreeBuilder()
'''

        assertScript '''
class Tree {
    List children
    Tree() {}
    Tree(Tree... children) { this.children = children?.toList() }
}
class Leaf extends Tree {
    String value
    Leaf(String value) { this.value = value}
}

// tag::newify_ruby[]
@Newify([Tree,Leaf])
class TreeBuilder {
    Tree tree = Tree.new(Leaf.new('A'),Leaf.new('B'),Tree.new(Leaf.new('C')))
}
// end::newify_ruby[]
def builder = new TreeBuilder()
'''
    }

    void testCategoryTransformation() {
        assertScript '''
// tag::oldstyle_category[]
class TripleCategory {
    public static Integer triple(Integer self) {
        3*self
    }
}
use (TripleCategory) {
    assert 9 == 3.triple()
}
// end::oldstyle_category[]
'''

        assertScript '''
// tag::newstyle_category[]
@Category(Integer)
class TripleCategory {
    public Integer triple() { 3*this }
}
use (TripleCategory) {
    assert 9 == 3.triple()
}
// end::newstyle_category[]
'''
    }

}
