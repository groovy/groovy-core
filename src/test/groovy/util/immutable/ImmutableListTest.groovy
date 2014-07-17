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
class ImmutableListTest extends GroovyTestCase {
    void testSupportedOperation() {
        def list = [] as ImmutableList<Integer>
        check([], list)

        list -= 1
        check([], list)

        list += (Integer) null
        check([null], list)

        list -= (Integer) null
        check([], list)

        list += 1
        check([1], list)

        list += 1
        check([1, 1], list)

        list -= 1
        check([1], list)

        list += [2, 3]
        check([1, 2, 3], list)

        list -= [2, 1]
        check([3], list)

        list = list.plusAt(0, [1, 2])
        check([1, 2, 3], list)

        list = list.minusAt(0)
        check([2, 3], list)

        list = list.replaceAt(1, 4)
        check([2, 4], list)
    }

    private void check(List<Integer> answer, ImmutableList<Integer> list) {
        assert answer == list as List<Integer>

        // toString, toArray
        assert answer.toString() == list.toString()
        assert Arrays.equals(answer as Integer[], list.toArray())

        // equals, hashCode
        assert ImmutableCollections.list(answer) == list
        assert ImmutableCollections.list(answer).hashCode() == list.hashCode()

        // size, isEmpty
        assert answer.size() == list.size()
        assert answer.isEmpty() == list.isEmpty()

        // getAt
        for (int i = 0; i <= answer.size(); i++) {
            assert answer[i] == list[i]
        }
        if (answer.size() > 0) {
            assert answer[-1] == list[-1]
        }
        shouldFail(IndexOutOfBoundsException) {
            list.get(-1)
        }
        shouldFail(IndexOutOfBoundsException) {
            list.get(answer.size())
        }

        // contains, indexOf, lastIndexOf
        for (int i = 0; i <= 4; i++) {
            assert answer.contains(i) == list.contains(i)
            assert answer.indexOf(i) == list.indexOf(i)
            assert answer.lastIndexOf(i) == list.lastIndexOf(i)
        }

        // containsAll
        assert list.containsAll([])
        assert list.containsAll(answer)
        if (answer.size() >= 3) {
            assert list.containsAll([answer[0], answer.last()])
        }

        // subList
        if (answer.size() >= 2) {
            assert answer.subList(1, answer.size()) == list.subList(1)
        }
        if (answer.size() >= 3) {
            assert answer.subList(1, 2) == list.subList(1, 2)
        }

        // Iterator
        Iterator<Integer> iterAnswer = answer.iterator()
        Iterator<Integer> iter = list.iterator()
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

        // ListIterator
        checkListIterator(answer.listIterator(), list.listIterator())
        for (int i = 0; i < answer.size(); i++) {
            checkListIterator(answer.listIterator(i), list.listIterator(i))
        }
        shouldFail(IndexOutOfBoundsException) {
            list.listIterator(-1)
        }
        shouldFail(IndexOutOfBoundsException) {
            list.listIterator(answer.size() + 1)
        }

        // multiply
        assert (list * 0) instanceof ImmutableList<Integer>
        assert (answer * 0) == (list * 0) as List<Integer>
        assert (list * 3) instanceof ImmutableList<Integer>
        assert (answer * 3) == (list * 3) as List<Integer>
    }

    def void checkListIterator(ListIterator<Integer> listIterAnswer, ListIterator<Integer> listIter) {
        while (true) {
            if (listIterAnswer.hasNext()) {
                assert listIter.hasNext()
                assert listIterAnswer.nextIndex() == listIter.nextIndex()
                assert listIterAnswer.previousIndex() == listIter.previousIndex()
                assert listIterAnswer.next() == listIter.next()
            } else {
                shouldFail(NoSuchElementException) {
                    listIter.next()
                }
                break
            }
        }
        while (true) {
            if (listIterAnswer.hasPrevious()) {
                assert listIter.hasPrevious()
                assert listIterAnswer.nextIndex() == listIter.nextIndex()
                assert listIterAnswer.previousIndex() == listIter.previousIndex()
                assert listIterAnswer.previous() == listIter.previous()
            } else {
                shouldFail(NoSuchElementException) {
                    listIter.previous()
                }
                break
            }
        }
    }

    @SuppressWarnings("GrDeprecatedAPIUsage")
    void testUnsupportedOperation() {
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().add(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().add(1, 1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().remove(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().remove((Object) 1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().addAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().addAll(1, [1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().removeAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().retainAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list().clear()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list([0]).iterator().remove()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list([0]).listIterator().remove()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list([0]).listIterator().set(null)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list([0]).listIterator().add(null)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list([0]).listIterator(0).remove()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list([0]).listIterator(0).set(null)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.list([0]).listIterator(0).add(null)
        }
    }
}
