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
// tag::tupleconstructor_example_callSuper[]
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
// end::tupleconstructor_example_callSuper[]
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

    void testSortable() {
        assertScript '''
// tag::sortable_simple[]
import groovy.transform.Sortable

@Sortable class Person {
    String first
    String last
    Integer born
}
// end::sortable_simple[]

// tag::sortable_simple_usage[]
def people = [
    new Person(first: 'Johnny', last: 'Depp', born: 1963),
    new Person(first: 'Keira', last: 'Knightley', born: 1985),
    new Person(first: 'Geoffrey', last: 'Rush', born: 1951),
    new Person(first: 'Orlando', last: 'Bloom', born: 1977)
]

assert people[0] > people[2]
assert people.sort()*.last == ['Rush', 'Depp', 'Knightley', 'Bloom']
assert people.sort(false, Person.comparatorByFirst())*.first == ['Geoffrey', 'Johnny', 'Keira', 'Orlando']
assert people.sort(false, Person.comparatorByLast())*.last == ['Bloom', 'Depp', 'Knightley', 'Rush']
assert people.sort(false, Person.comparatorByBorn())*.last == ['Rush', 'Depp', 'Bloom', 'Knightley']
// end::sortable_simple_usage[]
/* generates the following:
// tag::sortable_simple_generated_compareTo[]
public int compareTo(java.lang.Object obj) {
    if (this.is(obj)) {
        return 0
    }
    if (!(obj instanceof Person)) {
        return -1
    }
    java.lang.Integer value = this.first <=> obj.first
    if (value != 0) {
        return value
    }
    value = this.last <=> obj.last
    if (value != 0) {
        return value
    }
    value = this.born <=> obj.born
    if (value != 0) {
        return value
    }
    return 0
}
// end::sortable_simple_generated_compareTo[]
// tag::sortable_simple_generated_comparatorByFirst[]
public int compare(java.lang.Object arg0, java.lang.Object arg1) {
    if (arg0 == arg1) {
        return 0
    }
    if (arg0 != null && arg1 == null) {
        return -1
    }
    if (arg0 == null && arg1 != null) {
        return 1
    }
    return arg0.first <=> arg1.first
}
// end::sortable_simple_generated_comparatorByFirst[]
*/
        '''
        assertScript '''
import groovy.transform.Sortable

// tag::sortable_custom[]
@Sortable(includes='first,born') class Person {
    String last
    int born
    String first
}
// end::sortable_custom[]

// tag::sortable_custom_usage[]
def people = [
    new Person(first: 'Ben', last: 'Affleck', born: 1972),
    new Person(first: 'Ben', last: 'Stiller', born: 1965)
]

assert people.sort()*.last == ['Stiller', 'Affleck']
// end::sortable_custom_usage[]
/* generates the following:
// tag::sortable_custom_generated_compareTo[]
public int compareTo(java.lang.Object obj) {
    if (this.is(obj)) {
        return 0
    }
    if (!(obj instanceof Person)) {
        return -1
    }
    java.lang.Integer value = this.first <=> obj.first
    if (value != 0) {
        return value
    }
    value = this.born <=> obj.born
    if (value != 0) {
        return value
    }
    return 0
}
// end::sortable_custom_generated_compareTo[]
*/
        '''
    }

    void testBuilderSimple() {
        assertScript '''
// tag::builder_simple[]
import groovy.transform.builder.*

@Builder(builderStrategy=SimpleStrategy)
class Person {
    String first
    String last
    Integer born
}
// end::builder_simple[]

// tag::builder_simple_usage[]
def p1 = new Person().setFirst('Johnny').setLast('Depp').setBorn(1963)
assert "$p1.first $p1.last" == 'Johnny Depp'
// end::builder_simple_usage[]
// tag::builder_simple_alternatives[]
def p2 = new Person(first: 'Keira', last: 'Knightley', born: 1985)
def p3 = new Person().with {
    first = 'Geoffrey'
    last = 'Rush'
    born = 1951
}
// end::builder_simple_alternatives[]
/* generates the following:
// tag::builder_simple_generated_setter[]
public Person setFirst(java.lang.String first) {
    this.first = first
    return this
}
// end::builder_simple_generated_setter[]
*/
        '''
        assertScript '''
// tag::builder_simple_prefix[]
import groovy.transform.builder.*

@Builder(builderStrategy=SimpleStrategy, prefix="")
class Person {
    String first
    String last
    Integer born
}
// end::builder_simple_prefix[]

// tag::builder_simple_prefix_usage[]
def p = new Person().first('Johnny').last('Depp').born(1963)
assert "$p.first $p.last" == 'Johnny Depp'
// end::builder_simple_prefix_usage[]
        '''
    }

    void testBuilderExternal() {
        assertScript '''
// tag::builder_external_buildee[]
class Person {
    String first
    String last
    int born
}
// end::builder_external_buildee[]

// tag::builder_external[]
import groovy.transform.builder.*

@Builder(builderStrategy=ExternalStrategy, forClass=Person)
class PersonBuilder { }

def p = new PersonBuilder().first('Johnny').last('Depp').born(1963).build()
assert "$p.first $p.last" == 'Johnny Depp'
// end::builder_external[]

/* generates the following build method:
// tag::builder_external_generated_build[]
public Person build() {
    Person _thePerson = new Person()
    _thePerson.first = first
    _thePerson.last = last
    _thePerson.born = born
    return _thePerson
}
// end::builder_external_generated_build[]
*/
        '''
        assertScript '''
// tag::builder_external_java[]
import groovy.transform.builder.*

@Builder(builderStrategy=ExternalStrategy, forClass=javax.swing.DefaultButtonModel)
class ButtonModelBuilder {}

def model = new ButtonModelBuilder().enabled(true).pressed(true).armed(true).rollover(true).selected(true).build()
assert model.isArmed()
assert model.isPressed()
assert model.isEnabled()
assert model.isSelected()
assert model.isRollover()
// end::builder_external_java[]
        '''
        assertScript '''
// tag::builder_external_custom[]
import groovy.transform.builder.*
import groovy.transform.Canonical

@Canonical
class Person {
    String first
    String last
    int born
}

@Builder(builderStrategy=ExternalStrategy, forClass=Person, includes=['first', 'last'], buildMethodName='create', prefix='with')
class PersonBuilder { }

def p = new PersonBuilder().withFirst('Johnny').withLast('Depp').create()
assert "$p.first $p.last" == 'Johnny Depp'
// end::builder_external_custom[]
        '''
    }

    void testBuilderDefault() {
        assertScript '''
// tag::builder_default[]
import groovy.transform.builder.Builder

@Builder
class Person {
    String firstName
    String lastName
    int age
}

def person = Person.builder().firstName("Robert").lastName("Lewandowski").age(21).build()
assert person.firstName == "Robert"
assert person.lastName == "Lewandowski"
assert person.age == 21
// end::builder_default[]
        '''
        assertScript '''
// tag::builder_default_custom[]
import groovy.transform.builder.Builder

@Builder(buildMethodName='make', builderMethodName='maker', prefix='with', excludes='age')
class Person {
    String firstName
    String lastName
    int age
}

def p = Person.maker().withFirstName("Robert").withLastName("Lewandowski").make()
assert "$p.firstName $p.lastName" == "Robert Lewandowski"
// end::builder_default_custom[]
        '''
        assertScript '''
// tag::builder_default_methods[]
import groovy.transform.builder.*
import groovy.transform.*

@ToString
@Builder
class Person {
  String first, last
  int born

  Person(){}

  @Builder(builderClassName='MovieBuilder', builderMethodName='byRoleBuilder')
  Person(String roleName) {
     if (roleName == 'Jack Sparrow') {
         this.first = 'Johnny'; this.last = 'Depp'; this.born = 1963
     }
  }

  @Builder(builderClassName='NameBuilder', builderMethodName='nameBuilder', prefix='having', buildMethodName='fullName')
  static String join(String first, String last) {
      first + ' ' + last
  }

  @Builder(builderClassName='SplitBuilder', builderMethodName='splitBuilder')
  static Person split(String name, int year) {
      def parts = name.split(' ')
      new Person(first: parts[0], last: parts[1], born: year)
  }
}

assert Person.splitBuilder().name("Johnny Depp").year(1963).build().toString() == 'Person(Johnny, Depp, 1963)'
assert Person.byRoleBuilder().roleName("Jack Sparrow").build().toString() == 'Person(Johnny, Depp, 1963)'
assert Person.nameBuilder().havingFirst('Johnny').havingLast('Depp').fullName() == 'Johnny Depp'
assert Person.builder().first("Johnny").last('Depp').born(1963).build().toString() == 'Person(Johnny, Depp, 1963)'
// end::builder_default_methods[]
        '''
    }

    void testBuilderInitializer() {
        assertScript '''
// tag::builder_initializer[]
import groovy.transform.builder.*
import groovy.transform.*

@ToString
@Builder(builderStrategy=InitializerStrategy)
class Person {
    String firstName
    String lastName
    int age
}
// end::builder_initializer[]
// tag::builder_initializer_usage[]
@CompileStatic
def firstLastAge() {
    assert new Person(Person.createInitializer().firstName("John").lastName("Smith").age(21)).toString() == 'Person(John, Smith, 21)'
}
firstLastAge()
// end::builder_initializer_usage[]
        '''
        assertScript '''
// tag::builder_initializer_immutable[]
import groovy.transform.builder.*
import groovy.transform.*

@Builder(builderStrategy=InitializerStrategy)
@Immutable
class Person {
    String first
    String last
    int born
}

@CompileStatic
def createFirstLastBorn() {
  def p = new Person(Person.createInitializer().first('Johnny').last('Depp').born(1963))
  assert "$p.first $p.last $p.born" == 'Johnny Depp 1963'
}

createFirstLastBorn()
// end::builder_initializer_immutable[]
        '''
    }
}
