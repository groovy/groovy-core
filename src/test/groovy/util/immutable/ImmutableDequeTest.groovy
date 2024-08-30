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
class ImmutableDequeTest extends GroovyTestCase {
    void testSupportedOperation() {
        def deque = [] as ImmutableDeque<Integer>
        check([], deque)

        deque -= 1
        check([], deque)

        deque += (Integer) null
        check([null], deque)

        deque -= (Integer) null
        check([], deque)

        deque = deque + 1 + 2
        check([1, 2], deque)

        deque = deque - 2
        check([1], deque)

        deque = deque + 1 + 2
        check([1, 1, 2], deque)

        deque = deque - 2 - 1 - 1
        check([], deque)

        deque += [1]
        deque += [2, 3]
        check([1, 2, 3], deque)

        deque -= [2, 1]
        check([3], deque)

        deque = deque.plusFirst(4)
        check([4, 3], deque)

        deque = deque.plusLast(2)
        check([4, 3, 2], deque)

        deque = deque.plusFirst([5, 6])
        check([6, 5, 4, 3, 2], deque)

        deque = deque.plusLast([1, 0])
        check([6, 5, 4, 3, 2, 1, 0], deque)
    }

    private void check(List<Integer> answer, ImmutableDeque<Integer> deque) {
        assert answer == deque as List<Integer>

        // toString, toArray
        assert answer.toString() == deque.toString()
        assert Arrays.equals(answer as Integer[], deque.toArray())

        // equals, hashCode
        assert ImmutableCollections.deque(answer) == deque
        assert ImmutableCollections.deque(answer).hashCode() == deque.hashCode()

        // size, isEmpty
        assert answer.size() == deque.size()
        assert answer.isEmpty() == deque.isEmpty()

        // getAt
        for (int i = 0; i <= answer.size(); i++) {
            assert answer[i] == deque[i]
        }
        if (answer.size() > 0) {
            assert answer[-1] == deque[-1]
        }

        // contains
        for (int i = 0; i <= 4; i++) {
            assert answer.contains(i) == deque.contains(i)
        }

        // containsAll
        assert deque.containsAll([])
        assert deque.containsAll(answer)
        if (answer.size() >= 3) {
            assert deque.containsAll([answer[0], answer.last()])
        }

        // peek, element, peekFirst, peekLast, getFirst, getLast, first, head, last
        if (answer.size() == 0) {
            assert null == deque.peek()
            assert null == deque.peekFirst()
            assert null == deque.peekLast()
            shouldFail(NoSuchElementException) {
                deque.element()
            }
            shouldFail(NoSuchElementException) {
                deque.getFirst()
            }
            shouldFail(NoSuchElementException){
                deque.getLast()
            }
            shouldFail(NoSuchElementException){
                deque.first()
            }
            shouldFail(NoSuchElementException){
                deque.last()
            }
        } else {
            assert answer[0] == deque.peek()
            assert answer[0] == deque.peekFirst()
            assert answer[0] == deque.element()
            assert answer[0] == deque.getFirst()
            assert answer[0] == deque.first()
            assert answer[0] == deque.head()
            assert answer[-1] == deque.peekLast()
            assert answer[-1] == deque.getLast()
            assert answer[-1] == deque.last()
        }

        // tail
        if (answer.size() == 0) {
            shouldFail(NoSuchElementException) {
                answer.tail()
            }
            shouldFail(NoSuchElementException) {
                deque.tail()
            }
        } else {
            assert answer.tail() == deque.tail() as List<Integer>
        }

        // init
        if (answer.size() == 0) {
            shouldFail(NoSuchElementException) {
                answer.init()
            }
            shouldFail(NoSuchElementException) {
                deque.init()
            }
        } else {
            assert answer.init() == deque.init() as List<Integer>
        }

        // Iterator
        Iterator<Integer> iterAnswer = answer.iterator()
        Iterator<Integer> iter = deque.iterator()
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

        // descendingIterator
        if (!answer.contains(null)) {
            Iterator<Integer> descAnswer = new ArrayDeque<Integer>(answer).descendingIterator()
            Iterator<Integer> desc = deque.descendingIterator()
            while (true) {
                if (descAnswer.hasNext()) {
                    assert desc.hasNext()
                    assert descAnswer.next() == desc.next()
                } else {
                    shouldFail(NoSuchElementException) {
                        desc.next()
                    }
                    break
                }
            }
        }
    }

    @SuppressWarnings("GrDeprecatedAPIUsage")
    void testUnsupportedOperation() {
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque().add(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque().remove(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque().addAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque().removeAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque().retainAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque().clear()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque().offer(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).poll()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).remove()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).addFirst(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).addLast(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).offerFirst(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).offerLast(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).removeFirst()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).removeLast()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).pollFirst()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).pollLast()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).removeFirstOccurrence(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).removeLastOccurrence(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).push(1)
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).pop()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).iterator().remove()
        }
        shouldFail(UnsupportedOperationException) {
            ImmutableCollections.deque([0]).descendingIterator().remove()
        }
    }
}
