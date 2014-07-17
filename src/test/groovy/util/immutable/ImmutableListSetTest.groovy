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

/**
 * @author Yu Kobayashi
 */
class ImmutableListSetTest extends GroovyTestCase {
    void testSupportedOperation() {
        def listSet = [] as ImmutableListSet<Integer>
        check([], listSet)

        shouldFail(NullPointerException) {
            listSet + (Integer) null
        }

        listSet -= 1
        check([], listSet)

        listSet = listSet + 1 + 2
        check([1, 2], listSet)

        listSet = listSet + 1 + 2
        check([1, 2], listSet)

        listSet = listSet - 1 - 2
        check([], listSet)

        listSet += [1]
        listSet += [2, 3]
        check([1, 2, 3], listSet)

        listSet -= [2, 1]
        check([3], listSet)
    }

    private void check(List<Integer> answer, ImmutableListSet<Integer> listSet) {
        assert answer == listSet as List<Integer>

        // toString, toArray
        assert answer.toString() == listSet.toString()
        assert Arrays.equals(answer as Integer[], listSet.toArray())

        // equals, hashCode
        assert ImmutableCollections.listSet(answer) == listSet
        assert ImmutableCollections.listSet(answer).hashCode() == listSet.hashCode()

        // size, isEmpty
        assert answer.size() == listSet.size()
        assert answer.isEmpty() == listSet.isEmpty()

        // getAt
        for (int i = 0; i <= answer.size(); i++) {
            assert answer[i] == listSet[i]
        }
        if (answer.size() > 0) {
            assert answer[-1] == listSet[-1]
        }
        shouldFail(IndexOutOfBoundsException) {
            listSet.get(-1)
        }
        shouldFail(IndexOutOfBoundsException) {
            listSet.get(answer.size())
        }

        // contains
        for (int i = 0; i <= 4; i++) {
            assert answer.contains(i) == listSet.contains(i)
            assert answer.indexOf(i) == listSet.indexOf(i)
            assert answer.lastIndexOf(i) == listSet.lastIndexOf(i)
        }

        // containsAll
        assert listSet.containsAll([])
        assert listSet.containsAll(answer)
        if (answer.size() >= 3) {
            assert listSet.containsAll([answer[0], answer.last()])
        }

        // Iterator
        Iterator iterAnswer = answer.iterator()
        Iterator iter = listSet.iterator()
        while (true) {
            if (iterAnswer.hasNext()) {
                assert iter.hasNext()
                assert iterAnswer.next() == iter.next()
            } else {
                shouldFail(NoSuchElementException) {
                    iter.next()
                }
                break
            }
        }
    }

    @SuppressWarnings("GrDeprecatedAPIUsage")
    void testUnsupportedOperation() {
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.listSet().add(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.listSet().remove(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.listSet().addAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.listSet().removeAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.listSet().retainAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.listSet().clear()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.listSet([0]).iterator().remove()
        }
    }

    public void testBehavesLikeImmutableSet() {
        def set = ImmutableCollections.set()
        def listSet = ImmutableCollections.listSet()

        Random r = new Random();
        for (int i = 0; i < 100000; i++) {
            int v = r.nextInt(1000)
            if (r.nextFloat() < 0.8) {
                set += v
                listSet += v
            } else {
                set -= v
                listSet -= v
            }
        }

        assert set == listSet
    }
}
