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

import org.pcollections.HashTreePBag
import org.pcollections.MapPBag
import org.pcollections.PBag

/**
 * @author Yu Kobayashi
 */
class PBagTest extends GroovyTestCase {
    void testSupportedOperation() {
        MapPBag<Integer> bag = MapPBag.<Integer> empty()
        check([], bag)

        shouldFail(NullPointerException) {
            bag + (Integer) null
        }

        bag -= 1
        check([], bag)

        bag = bag + 1 + 2
        check([1, 2], bag)

        bag = bag + 1 + 2
        check([1, 2, 1, 2], bag)

        bag = bag - 1 - 2 - 2
        check([1], bag)

        bag = bag.plusAll([2, 3])
        check([1, 2, 3], bag)

        bag = bag.minusAll([2, 1])
        check([3], bag)
    }

    private void check(List<Integer> answer, PBag<Integer> bag) {
        // toString, toArray
        if (answer.size() == (answer as Set).size()) {
            assert answer.toString() == bag.toString()
            assert Arrays.equals(answer as Integer[], bag.toArray())
        }

        // equals, hashCode
        assert HashTreePBag.from(answer) == bag
        assert HashTreePBag.from(answer).hashCode() == bag.hashCode()

        // size, isEmpty
        assert answer.size() == bag.size()
        assert answer.isEmpty() == bag.isEmpty()

        // contains
        for (int i = 0; i <= 4; i++) {
            assert answer.contains(i) == bag.contains(i)
        }

        // containsAll
        assert bag.containsAll([])
        assert bag.containsAll(answer)
        if (answer.size() >= 3) {
            assert bag.containsAll([answer[0], answer.last()])
        }

        // Iterator
        Iterator iterAnswer = answer.iterator()
        Iterator iter = bag.iterator()
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
            MapPBag.empty().add(1)
        }
        shouldFail(UnsupportedOperationException) {
            MapPBag.empty().remove(1)
        }
        shouldFail(UnsupportedOperationException) {
            MapPBag.empty().addAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            MapPBag.empty().removeAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            MapPBag.empty().retainAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            MapPBag.empty().clear()
        }
        shouldFail(UnsupportedOperationException) {
            MapPBag.from([0]).iterator().remove()
        }
    }
}
