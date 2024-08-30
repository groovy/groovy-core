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
class ImmutableSetTest extends GroovyTestCase {
    void testSupportedOperation() {
        def set = [] as ImmutableSet<Integer>
        check([], set)

        shouldFail(NullPointerException) {
            set + (Integer) null
        }

        set -= 1
        check([], set)

        set = set + 1 + 2
        check([1, 2], set)

        set = set + 1 + 2
        check([1, 2], set)

        set = set - 2
        check([1], set)

        set += [2, 3]
        check([1, 2, 3], set)

        set -= [2, 1]
        check([3], set)
    }

    private void check(List<Integer> ans, ImmutableSet<Integer> set) {
        Set<Integer> answer = ans as Set<Integer>

        // toString, toArray
        if (answer.size() <= 1) {
            assert answer.toString() == set.toString()
            assert Arrays.equals(answer as Integer[], set.toArray())
        }

        // equals, hashCode
        assert ImmutableCollections.set(answer) == set
        assert ImmutableCollections.set(answer).hashCode() == set.hashCode()

        // size, isEmpty
        assert answer.size() == set.size()
        assert answer.isEmpty() == set.isEmpty()

        // contains
        for (int i = 0; i <= 4; i++) {
            assert answer.contains(i) == set.contains(i)
        }

        // containsAll
        assert set.containsAll([])
        assert set.containsAll(answer)
        if (answer.size() >= 3) {
            assert set.containsAll([answer[0], answer.last()])
        }

        // Iterator
        Iterator<Integer> iterAnswer = answer.iterator()
        Iterator<Integer> iter = set.iterator()
        while (true) {
            if (iterAnswer.hasNext()) {
                assert iter.hasNext()
                assert answer.contains(iter.next())
                iterAnswer.next()
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
            ImmutableCollections.set().add(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.set().remove(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.set().addAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.set().removeAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.set().retainAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.set().clear()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.set([0]).iterator().remove()
        }
    }
}
