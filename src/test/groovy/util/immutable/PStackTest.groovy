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

import org.pcollections.ConsPStack
import org.pcollections.PStack

/**
 * @author Yu Kobayashi
 */
class PStackTest extends GroovyTestCase {
    void testSupportedOperation() {
        PStack<Integer> stack = ConsPStack.empty()
        check([], stack)

        stack = stack.minus((Object) 1)
        check([], stack)

        stack += (Object) null
        check([null], stack)

        stack = stack.minus((Object) null)
        check([], stack)

        stack += new Integer(1)
        check([1], stack)

        stack += new Integer(1)
        check([1, 1], stack)

        stack = stack.minus((Object) 1)
        check([1], stack)

        stack = stack.plusAll([2, 3])
        check([3, 2, 1], stack)

        stack = stack.minusAll([2, 1])
        check([3], stack)

        stack = stack.plusAll(0, [1, 2])
        check([2, 1, 3], stack)

        stack = stack.minus(0)
        check([1, 3], stack)

        stack = stack.with(1, 4)
        check([1, 4], stack)
    }

    private void check(List<Integer> answer, PStack<Integer> list) {
        assert answer == list as List<Integer>

        // toString, toArray
        assert answer.toString() == list.toString()
        assert Arrays.equals(answer as Integer[], list.toArray())

        // equals, hashCode
        assert ConsPStack.from(answer) == list
        assert ConsPStack.from(answer).hashCode() == list.hashCode()

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
            ConsPStack.empty().add(1)
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().add(1, 1)
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().remove(1)
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().remove((Object) 1)
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().addAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().addAll(1, [1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().removeAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().retainAll([1, 2])
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.empty().clear()
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.from([0]).iterator().remove()
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.from([0]).listIterator().remove()
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.from([0]).listIterator().set(null)
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.from([0]).listIterator().add(null)
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.from([0]).listIterator(0).remove()
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.from([0]).listIterator(0).set(null)
        }
        shouldFail(UnsupportedOperationException) {
            ConsPStack.from([0]).listIterator(0).add(null)
        }
    }

    /**
     * Compares the behavior of LinkedList to the behavior of ImmutableStack.
     */
    public void testRandomlyAgainstJavaList() {
        def pstack = ConsPStack.<Integer>empty()
        def list = new LinkedList<Integer>()
        def r = new Random()
        for (int i = 0; i < 1000; i++) {
            if (pstack.size() == 0 || r.nextBoolean()) { // add
                if (r.nextBoolean()) { // append
                    int v = r.nextInt()

                    assert list.contains(v) == pstack.contains(v)

                    list.add(0, v)
                    pstack += new Integer(v)
                } else { // insert
                    int k = r.nextInt(pstack.size() + 1)
                    int v = r.nextInt()

                    assert list.contains(v) == pstack.contains(v)
                    if (k < pstack.size())
                        assert list[k] == pstack[k]

                    list.add(k, v);
                    pstack = pstack.plus(k, v)
                }
            } else if (r.nextBoolean()) { // replace
                int k = r.nextInt(pstack.size())
                int v = r.nextInt()
                list[k] = v
                pstack = pstack.with(k, v)
            } else { // remove a random element
                int j = r.nextInt(pstack.size())
                int k = 0
                for (int e : pstack) {
                    assert list.contains(e)
                    assert pstack.contains(e)
                    assert e == pstack[k]
                    assert list[k] == pstack[k]
                    assert pstack == pstack.minus(k).plus(k, pstack[k])
                    assert pstack == pstack.plus(k, 10).minus(k)

                    if (k == j) {
                        list.remove(k)
                        pstack = pstack.minus(k)
                        k-- // indices are now smaller
                        j = -1 // don't remove again
                    }
                    k++
                }
            }

            // also try to remove a _totally_ random value:
            int v = r.nextInt()
            assert list.contains(v) == pstack.contains(v)
            list.remove((Object) v)
            pstack = pstack.minus((Object) v)

            // and try out a non-Integer:
            def s = v as String
            assert !pstack.contains(v)
            pstack = pstack.minus(s)

            assert list.size() == pstack.size()
            assert list == pstack

            assert pstack == ConsPStack.from(pstack)
            assert ConsPStack.empty() == pstack.minusAll(pstack)
            assert pstack == ConsPStack.empty().plusAll(pstack.reverse())

            assert pstack == ConsPStack.singleton(10).plusAll(1, pstack.reverse()).minus(0)
        }
    }
}
