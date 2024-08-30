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

package groovy.util.immutable

import java.util.Map.Entry

/**
 * @author Yu Kobayashi
 */
class ImmutableMapTest extends GroovyTestCase {
    void testSupportedOperation() {
        def map = [:] as ImmutableMap<String, Integer>
        check([:], map)

        shouldFail(NullPointerException) {
            map.plus((String) null, 1)
        }

        map -= 1
        check([:], map)

        map += [a: 1]
        check([a: 1], map)

        map += [a: 1]
        check([a: 1], map)

        map -= 'a'
        check([:], map)

        map += [a: 1]
        map += [b: 2, c: 3]
        check([a: 1, b: 2, c: 3], map)

        map -= ["b", "a"]
        check([c: 3], map)
    }

    private static void check(Map<String, Integer> answer, ImmutableMap<String, Integer> map) {
        assert answer == map as Map<String, Integer>

        // toString, toArray
        assert answer.toString() == map.toString()

        // equals, hashCode
        assert ImmutableCollections.map(answer) == map
        assert ImmutableCollections.map(answer).hashCode() == map.hashCode()

        // size, isEmpty
        assert answer.size() == map.size()
        assert answer.isEmpty() == map.isEmpty()

        // contains
        for (int i = 0; i <= 4; i++) {
            def key = (((char) 'a') + i) as String
            assert answer.containsKey(key) == map.containsKey(key)
            assert answer.containsValue(i) == map.containsValue(i)
            assert answer[key] == map[key]
        }

        // keySet, entrySet, values
        assert answer.keySet() == map.keySet()
        assert answer.values() as List<Integer> == map.values() as List<Integer>
        assert answer.entrySet() == map.entrySet()
    }

    @SuppressWarnings("GrDeprecatedAPIUsage")
    void testUnsupportedOperation() {
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.map()["a"] = 1
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.map().put("a", 1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.map().remove(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.map().putAll([a: 1, b: 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.map().clear()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.map([a: 0]).iterator().remove()
        }
    }

    /**
     * Compares the behavior of HashMap to the behavior of ImmutableMap.
     */
    void testRandomlyAgainstJavaMap() {
        def pmap = ImmutableCollections.map() // [:] as ImmutableMap<Integer, Integer>
        def map = new HashMap<Integer, Integer>()
        def r = new Random()
        for (int i = 0; i < 1000; i++) {
            if (pmap.size() == 0 || r.nextBoolean()) { // add
                int k = r.nextInt()
                int v = r.nextInt()

                assert map.containsKey(k) == pmap.containsKey(k)
                assert map[k] == pmap[k]

                map[k] = v
                pmap = pmap.plus(k, v);
            } else { // remove a random key
                int j = r.nextInt(pmap.size())
                for (Entry<Integer, Integer> e : pmap.entrySet()) {
                    int k = e.key

                    assert map.containsKey(k)
                    assert pmap.containsKey(k)
                    assert map[k] == pmap[k]
                    assert map.entrySet().contains(e)
                    assert pmap.entrySet().contains(e)
                    assert pmap == pmap.plus(k, e.value)

                    if (j-- == 0) {
                        map.remove(k)
                        pmap -= k
                        assert !pmap.entrySet().contains(e)
                    }
                }
            }

            // also try to remove a _totally_ random key:
            int k = r.nextInt()
            assert map.containsKey(k) == pmap.containsKey(k)
            assert map[k] == pmap[k]
            map.remove(k)
            pmap -= k

            // and try out a non-Integer:
            def s = Integer.toString(k)
            assert !pmap.containsKey(s)
            assert null == pmap[s]
            assert !pmap.entrySet().contains(s)
            pmap -= s

            assert map.size() == pmap.size()
            assert map == pmap
            assert map.entrySet() == pmap.entrySet()

            assert pmap == ImmutableCollections.map(pmap)
            assert ImmutableCollections.map() == (pmap - pmap.keySet())
            assert pmap == (pmap + pmap)
        }
    }
}
