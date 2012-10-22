/*
 * Copyright 2003-2009 the original author or authors.
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
package gls.enums

import gls.CompilableTestSupport

/**
 * Tests various properties of enums.
 *
 * @author Paul King
 * @author Roshan Dawrani
 */
class EnumTest extends CompilableTestSupport {

    void testValues() {
        assert UsCoin.values().size() == 4
        assert UsCoin.values().toList().sum{ it.value } == 41
    }

    void testNext() {
        def coin = UsCoin.penny
        def coins = [coin++, coin++, coin++, coin++, coin]
        assert coins == [UsCoin.penny, UsCoin.nickel, UsCoin.dime, UsCoin.quarter, UsCoin.penny]
    }

    void testPrevious() {
        def coin = UsCoin.quarter
        def coins = [coin--, coin--, coin--, coin--, coin]
        assert coins == [UsCoin.quarter, UsCoin.dime, UsCoin.nickel, UsCoin.penny, UsCoin.quarter]
    }

    void testRange() {
        def coinRange1 = UsCoin.penny..UsCoin.dime
        assert (UsCoin.nickel in coinRange1)
        assert !(UsCoin.quarter in coinRange1)
    }

    void testMinValue() {
        assert UsCoin.MIN_VALUE == UsCoin.penny
        shouldFail(MissingPropertyException) {
           EmptyEnum.MIN_VALUE
        }
    }

    void testMaxValue() {
        assert UsCoin.MAX_VALUE == UsCoin.quarter
        shouldFail(MissingPropertyException) {
           EmptyEnum.MAX_VALUE
        }
    }

    void testComparators() {
        assert UsCoin.nickel <=> UsCoin.penny  ==  1
        assert UsCoin.nickel <=> UsCoin.nickel ==  0
        assert UsCoin.nickel <=> UsCoin.dime   == -1
        assert UsCoin.nickel <=  UsCoin.nickel
        assert UsCoin.nickel <=  UsCoin.dime
        assert UsCoin.nickel >=  UsCoin.penny
        assert UsCoin.nickel >=  UsCoin.nickel
    }

    void testStepWithRange() {
        def coinRange2 = UsCoin.nickel..UsCoin.quarter
        def coins = coinRange2.toList()
        assert coins == [UsCoin.nickel, UsCoin.dime, UsCoin.quarter]
        coins = coinRange2.step(2)
        assert coins == [UsCoin.nickel, UsCoin.quarter]
        coins = coinRange2.step(3)
        assert coins == [UsCoin.nickel]
        coins = coinRange2.step(4)
        assert coins == [UsCoin.nickel]
        coins = coinRange2.step(-1)
        assert coins == [UsCoin.quarter, UsCoin.dime, UsCoin.nickel]
        coins = coinRange2.step(-2)
        assert coins == [UsCoin.quarter, UsCoin.nickel]
        coins = coinRange2.step(-3)
        assert coins == [UsCoin.quarter]
        coins = coinRange2.step(-4)
        assert coins == [UsCoin.quarter]
    }

    void testStepWithReverseRange() {
        def coinRange2 = UsCoin.quarter..UsCoin.nickel
        def coins = coinRange2.toList()
        assert coins == [UsCoin.quarter, UsCoin.dime, UsCoin.nickel]
        coins = coinRange2.step(2)
        assert coins == [UsCoin.quarter, UsCoin.nickel]
        coins = coinRange2.step(3)
        assert coins == [UsCoin.quarter]
        coins = coinRange2.step(4)
        assert coins == [UsCoin.quarter]
        coins = coinRange2.step(-1)
        assert coins == [UsCoin.nickel, UsCoin.dime, UsCoin.quarter]
        coins = coinRange2.step(-2)
        assert coins == [UsCoin.nickel, UsCoin.quarter]
        coins = coinRange2.step(-3)
        assert coins == [UsCoin.nickel]
        coins = coinRange2.step(-4)
        assert coins == [UsCoin.nickel]
    }

    void testEnumWithSingleListInConstructor() {
        def sh = new GroovyShell();
        def enumStr;
        
        enumStr = """
            enum ListEnum1 {
                ONE([111, 222])
                ListEnum1(Object listArg){
                    assert listArg == [111, 222]
                    assert listArg instanceof java.util.ArrayList
                }
            }
            println ListEnum1.ONE
        """
        sh.evaluate(enumStr);
            
        enumStr = """
            enum ListEnum2 {
                TWO([234, [567,12]])
                ListEnum2(Object listArg){
                    assert listArg == [234, [567, 12]]
                    assert listArg instanceof java.util.ArrayList
                }
            }
            println ListEnum2.TWO
        """
        sh.evaluate(enumStr);
    }
    
    void testSingleListDoesNoInfluenceMaps() {
        // the fix for GROOVY-2933 caused map["taku"]
        // to become map[(["take])] instead. -> GROOVY-3214
        assertScript """
            public enum FontFamily {
                ARIAL
            
                static public void obtainMyMap()
                {
                    Map map = [:]
                    map["taku"] = "dio"
                    assert map.taku == "dio"
                }
            }
            FontFamily.obtainMyMap()
        """
    }
 
    void testMutipleValuesDontGetWronglyWrappedInList() {
        // the fix for GROOVY-3214 caused multiple values passed in an enum const
        // to get wrapped in an extra ListExpression. -> GROOVY-3276
        assertScript """
            enum GROOVY3276 {
               A(1,2), B(3,4)
           
               GROOVY3276(int xx, int yy) { 
                   x=xx 
                   y=yy 
               }
               public int x
               public int y
           }
           
           assert GROOVY3276.A.x == 1
           assert GROOVY3276.B.y == 4
        """
    }

    // the fix for GROOVY-3161
    def void testStaticEnumFieldWithEnumValues() {
        def allColors = GroovyColors3161.ALL_COLORS
        assert allColors.size == 3
        assert allColors[0] == GroovyColors3161.red
        assert allColors[1] == GroovyColors3161.blue
        assert allColors[2] == GroovyColors3161.green
    }

    // the fix for GROOVY-3283
    def void testImportStaticMoreThanOneEnum() {
        assertScript """
            enum Foo3283 { A,B }
            enum Bar3283 { X,Y }
            
            import static Foo3283.*
            import static Bar3283.*
            
            a = A
            x = X
        """
    }

    void testCallBehaviorOnEnumForGROOVY3284() {
        // test the usage in a non-script class first
        for (f in Foo3284) {
            assert f() == "A"
        }
        assert Foo3284.A.call() == "A"
        assert Foo3284.A() == "A"
        def a = Foo3284.A
        assert a() == "A"

        // now test the usage in a script but this time type Closure not specified explicitly
        assertScript """
            enum Foo32842 {
                B({ "B" })
                Foo32842(c) {
                  call = c
                }
                def call
            }
            for (f in Foo32842) {
                assert f() == "B"
            }
    
            assert Foo32842.B.call() == "B"
    
            assert Foo32842.B() == "B"
            b = Foo32842.B
            assert b() == "B"
        """
    }

    void testClassResolutionForInnerEnumsWithPackageNameGROOVY3483() {

        assertScript """
            package familie
            
            class Mother3483 {
                Mother3483.Child child
                
                enum Child{
                    Franz,
                    Ferdi,
                    Nand
                }
            }
            
            def mother = new Mother3483(child: Mother3483.Child.Franz)
            
            assert mother.child as String == 'Franz'
        """
    }

    void testInnerEnumUsedInDefiningClassWithUnqualifiedEnumNameUsed() {
        //GROOVY-3110
        assertScript """
            class Class3110 {
                enum Enum3110{FOO, BAR}
                Enum3110 var
                def setEnumVar() {
                    var = Enum3110.FOO
                }
                def getEnumVar() {
                    var
                }
            }
            
            def obj = new Class3110()
            obj.setEnumVar()
            assert obj.getEnumVar() == Class3110.Enum3110.FOO
        """
    }

    void testStaticFieldInitValuesInAStaticBlock() {
        // GROOVY-3693 - trigger enum class load to test it - asserts are present in the enum 
        GrooyColors3693.r
    }
    
    void testCustomMethodOnEnum() {
        // GROOVY-2443
        assertScript """
            enum Day {
                SUNDAY {
                    String activity() { 'Relax' }
                }, MONDAY, TUESDAY, WEDNESDAY,
                THURSDAY, FRIDAY, SATURDAY
                String activity() { 'Work' }
            }
            assert "Work" == Day.MONDAY.activity() 
            assert "Relax" == Day.SUNDAY.activity()
        """
    }

    void testEnumConstantSubClassINITMethodOverrideINITOfEnumClass() {
        try {
            // cause loading of enum that causes its fields to be set and 
            // their instance initializer to be executed
            println Color3985
        }catch(ExceptionInInitializerError err) {
            assert err.cause.message == 'Color3985 RED instance initializer called successfully'
        }
    }

    void testEnumStaticInitWithAFieldUsingEnumValues() {
        //GROOVY-3996
        assertScript """
            enum Color3996 {
                R, G, B
                public static Color3996[] ALL_COLORS = [R, G, B]
            }
            
            assert Color3996.ALL_COLORS.size() == 3
            assert Color3996.ALL_COLORS[0] == Color3996.R
            assert Color3996.ALL_COLORS[1] == Color3996.G
            assert Color3996.ALL_COLORS[2] == Color3996.B
        """
    }

    void testEnumWithTopLevelNoBracketsMethodCall() {
        // GROOVY-3986
        assertScript """
            enum Color3986 {
                RED {
                  String toString() { sprintf '%s', 'foo' }
                },GREEN,BLUE
            }
            assert Color3986.RED.toString() == 'foo'
        """
    }

    void testEnumConstantSeparators() {
        // GROOVY-3047
        shouldCompile """
            enum Foo0 { X }
            enum Foo1 {

              Y

            }
            enum Foo2 {
              Z,
            }
            enum Foo3 { X, Y, Z,
            }
            enum Foo4 {
              X
              ,
            }
            enum Foo5 {
              X
              ,
              Y
            }
            enum Foo6 {
              X
              ,
              Y,
            }
            enum Foo7 {
              X
              ,
              Y
              ,
            }
        """
    }

    void testEnumWithSingleValueAndClassField() {
        // GROOVY-4268
        shouldCompile """
            enum EnumWithSingleValueAndClassField {
                VALUE
                String toString() { "I'm a value" }
            }
        """
    }

    void testConstructorChainingInEnum() {
        // GROOVY-4444
        assertScript """
            enum Foo4444 {
                ONE(1), TWO(1, 2)
              
                int i
                int j
              
                Foo4444(int i) {
                    this(i, 0)
                }
            
                Foo4444(int i, int j) {
                    this.i = i
                    this.j = j
                }
            }
            
            def foos = [Foo4444.ONE, Foo4444.TWO]
            
            assert foos.size() == 2
            
            assert foos[0].i == 1
            assert foos[0].j == 0
            
            assert foos[1].i == 1
            assert foos[1].j == 2
        """
    }
    
    void testNamedArguments() {
        // this test is a result of GROOVY-4219 and should be changed once
        // GROOVY-4582 is implemented.
        shouldNotCompile """
            enum ImageSortField {
                FILENAME(field: null, name: null),
                TIME(field: null, name: null)
                
                def field
                def name
            
                public String toString(){
                    name
                }
            }
        """
    }
}


enum UsCoin {
    penny(1), nickel(5), dime(10), quarter(25)
    UsCoin(int value) { this.value = value }
    private final int value
    int getValue() { value }
}

enum EmptyEnum{}

enum GroovyColors3161 {
    red, blue, green
    static def ALL_COLORS = [red, blue, green]
}

enum GroovyColors3161B {
    red, blue, green,
    static def ALL_COLORS = [red, blue, green]
}

enum Foo3284 {
    A({ "A" })
    Foo3284(Closure c) {
      call = c
    }
    final Closure call
}

enum GrooyColors3693 {
    r, g, b
    static List list = [1, 2]
    static init() {
        assert list == [1, 2]
    }
    static { 
        init() 
    }
}

enum Color3985 {
    RED {
        { 
            throw new RuntimeException('Color3985 RED instance initializer called successfully') 
        }
    },GREEN,BLUE
}
