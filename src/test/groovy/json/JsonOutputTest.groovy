/*
 * Copyright 2003-2011 the original author or authors.
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
package groovy.json

import static groovy.json.JsonOutput.toJson
import groovy.transform.Canonical

/**
 * 
 * @author Guillaume Laforge
 */
class JsonOutputTest extends GroovyTestCase {
    
    void testBooleanValues() {
        assert toJson(Boolean.TRUE) == "true"
        assert toJson(Boolean.FALSE) == "false"
        assert toJson(true) == "true"
        assert toJson(false) == "false"
    }

    void testNullValue() {
        assert toJson(null) == "null"
    }

    void testNumbers() {
        assert toJson(-1) == "-1"
        assert toJson(1) == "1"
        assert toJson(0) == "0"
        assert toJson(100) == "100"
        assert toJson(100) == "100"

        assert toJson((short)100) == "100"
        assert toJson((byte)100) == "100"

        // Long
        assert toJson(1000000000000000000) == "1000000000000000000"
        // BigInteger
        assert toJson(1000000000000000000000000) == "1000000000000000000000000"

        // BigDecimal
        assert toJson(0.0) == "0.0"
        assert toJson(0.0) == "0.0"

        // Double
        assert toJson(Math.PI) == "3.141592653589793"
        // Float
        assert toJson(1.2345f) == "1.2345"

        // exponant
        assert toJson(1234.1234e12) == "1.2341234E+15"

        shouldFail { toJson(Double.NaN) }
        shouldFail { toJson(Double.POSITIVE_INFINITY) }
        shouldFail { toJson(Double.NEGATIVE_INFINITY) }
        shouldFail { toJson(Float.NaN) }
        shouldFail { toJson(Float.POSITIVE_INFINITY) }
        shouldFail { toJson(Float.NEGATIVE_INFINITY) }
    }

    void testEmptyListOrArray() {
        assert toJson([]) == "[]"
        assert toJson([] as Object[]) == "[]"
    }

    void testListOfPrimitives() {
        assert toJson([true, false, null, true, 4, 1.1234]) == "[true,false,null,true,4,1.1234]"
        assert toJson([true, [false, null], true, [4, [1.1234]]]) == "[true,[false,null],true,[4,[1.1234]]]"
    }

    void testPrimitiveArray() {
        assert toJson([1, 2, 3, 4] as byte[]) == "[1,2,3,4]"
        assert toJson([1, 2, 3, 4] as short[]) == "[1,2,3,4]"
        assert toJson([1, 2, 3, 4] as int[]) == "[1,2,3,4]"
        assert toJson([1, 2, 3, 4] as long[]) == "[1,2,3,4]"
    }

    void testEmptyMap() {
        assert toJson([:]) == "{}"
    }

    void testMap() {
        assert toJson([a: 1]) == '{"a":1}'
        assert toJson([a: 1, b: 2]) == '{"a":1,"b":2}'
        assert toJson([a: 1, b: true, c: null, d: [], e: 'hello']) == '{"a":1,"b":true,"c":null,"d":[],"e":"hello"}'
    }

    void testString() {
        assert toJson("") == '""'

        assert toJson("a") == '"a"'
        assert toJson("abcdef") == '"abcdef"'

        assert toJson("\b") == '"\\b"'
        assert toJson("\f") == '"\\f"'
        assert toJson("\n") == '"\\n"'
        assert toJson("\r") == '"\\r"'
        assert toJson("\t") == '"\\t"'

        assert toJson('"') == '"\\""'
//        assert toJson("/") == '"\\/"'
        assert toJson("\\") == '"\\\\"'

        assert toJson("\u0001") == '"\\u0001"' 
        assert toJson("\u0002") == '"\\u0002"'
        assert toJson("\u0003") == '"\\u0003"'
        assert toJson("\u0004") == '"\\u0004"'
        assert toJson("\u0005") == '"\\u0005"'
        assert toJson("\u0006") == '"\\u0006"'
        assert toJson("\u0007") == '"\\u0007"'
        assert toJson("\u0010") == '"\\u0010"'
        assert toJson("\u0011") == '"\\u0011"'
        assert toJson("\u0012") == '"\\u0012"'
        assert toJson("\u0013") == '"\\u0013"'
        assert toJson("\u0014") == '"\\u0014"'
        assert toJson("\u0015") == '"\\u0015"'
        assert toJson("\u0016") == '"\\u0016"'
        assert toJson("\u0017") == '"\\u0017"'
        assert toJson("\u0018") == '"\\u0018"'
        assert toJson("\u0019") == '"\\u0019"'
    }

    void testCharArray() {
        char[] charArray = ['a', 'b', 'c']

        assert toJson(charArray) == '["a","b","c"]'
    }

    void testDate() {
        def d = Date.parse("yyyy/MM/dd HH:mm:ss Z", "2008/03/04 13:50:00 +0100")

        assert toJson(d) == '"2008-03-04T12:50:00+0000"'
    }

    void testURL() {
        assert toJson(new URL("http://glaforge.appspot.com")) == '"http://glaforge.appspot.com"'
    }

    void testCalendar() {
        def c = GregorianCalendar.getInstance(TimeZone.getTimeZone('GMT+1'))
        c.clearTime()
        c.set(year: 2008, month: Calendar.MARCH, date: 4, hourOfDay: 13, minute: 50)

        assert toJson(c) == '"2008-03-04T12:50:00+0000"'
    }

    void testComplexObject() {
        assert toJson([name: 'Guillaume', age: 33, address: [line1: "1 main street", line2: "", zip: 1234], pets: ['dog', 'cat']]) ==
                '{"name":"Guillaume","age":33,"address":{"line1":"1 main street","line2":"","zip":1234},"pets":["dog","cat"]}'

        assert toJson([[:],[:]]) == '[{},{}]'
    }

    void testClosure() {
        assert toJson({
            a 1
            b {
                c 2
                d {
                    e 3, {
                        f 4
                    }
                }
            }
        }) == '{"a":1,"b":{"c":2,"d":{"e":[3,{"f":4}]}}}'
    }

    void testIteratorEnumeration() {
        assert toJson([1, 2, 3].iterator()) == '[1,2,3]'
        assert toJson(Collections.enumeration([1, 2, 3])) == '[1,2,3]'
    }

    void testPrettyPrint() {
        def json = new JsonBuilder()

        json.trends {
            "2010-06-22 17:20" ([
                    name: "Groovy rules",
                    query: "Groovy rules"
            ], {
                name "#worldcup"
                query "#worldcup"
            }, [
                    name: "Uruguai",
                    query: "Uruguai"
            ])
            "2010-06-22 06:20" ({
                name "#groovy"
                query "#groovy"
            }, [
                    name: "#java",
                    query: "#java"
            ])
        }

        assert json.toPrettyString() == """\
            {
                "trends": {
                    "2010-06-22 17:20": [
                        {
                            "name": "Groovy rules",
                            "query": "Groovy rules"
                        },
                        {
                            "name": "#worldcup",
                            "query": "#worldcup"
                        },
                        {
                            "name": "Uruguai",
                            "query": "Uruguai"
                        }
                    ],
                    "2010-06-22 06:20": [
                        {
                            "name": "#groovy",
                            "query": "#groovy"
                        },
                        {
                            "name": "#java",
                            "query": "#java"
                        }
                    ]
                }
            }""".stripIndent()
    }

    void testPrettyPrintDoubleQuoteEscape() {
        def json = new JsonBuilder()

        json.text { content 'abc"def' }

        assert json.toPrettyString() == """\
            {
                "text": {
                    "content": "abc\\"def"
                }
            }""".stripIndent()
    }

    void testSerializePogos() {
        def city = new JsonCity("Paris", [
                new JsonDistrict(1, [
                        new JsonStreet("Saint-Honore", JsonStreetKind.street),
                        new JsonStreet("de l'Opera",   JsonStreetKind.avenue)
                ] as JsonStreet[]),
                new JsonDistrict(2, [
                        new JsonStreet("des Italiens",   JsonStreetKind.boulevard),
                        new JsonStreet("Bonne Nouvelle", JsonStreetKind.boulevard)
                ] as JsonStreet[])
        ])

        assert JsonOutput.prettyPrint(JsonOutput.toJson(city)) == '''\
            {
                "name": "Paris",
                "districts": [
                    {
                        "streets": [
                            {
                                "kind": "street",
                                "streetName": "Saint-Honore"
                            },
                            {
                                "kind": "avenue",
                                "streetName": "de l'Opera"
                            }
                        ],
                        "number": 1
                    },
                    {
                        "streets": [
                            {
                                "kind": "boulevard",
                                "streetName": "des Italiens"
                            },
                            {
                                "kind": "boulevard",
                                "streetName": "Bonne Nouvelle"
                            }
                        ],
                        "number": 2
                    }
                ]
            }'''.stripIndent()
    }

    void testMapWithNullKey() {
        shouldFail IllegalArgumentException, {
            toJson([(null): 1])
        }
    }

    void testGROOVY5247() {
        def m = new TreeMap()
        m.a = 1
        assert toJson(m) == '{"a":1}'
    }

	void testObjectWithDeclaredPropertiesField() {
		def person = new JsonObject(name: "pillow", properties: [state: "fluffy", color: "white"])
		def json = toJson(person)
		assert json == '{"properties":{"state":"fluffy","color":"white"},"name":"pillow"}'
	}
	
	void testGROOVY5494() {
		def json = toJson(new JsonFoo(name: "foo"))
		assert json == '{"properties":0,"name":"foo"}'
	}
}

@Canonical
class JsonCity {
    String name
    List<JsonDistrict> districts
}

@Canonical
class JsonDistrict {
    int number
    JsonStreet[] streets
}

@Canonical
class JsonStreet {
    String streetName
    JsonStreetKind kind
}

class JsonObject {
	String name
	Map properties
}

class JsonFoo {
	String name
	int getProperties() { return 0 }
}

enum JsonStreetKind {
    street, boulevard, avenue
}
