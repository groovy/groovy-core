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

package groovy.util.immutable;

import java.util.Deque;

/**
 * An immutable and persistent deque.
 * The elements can be null.
 * <p/>
 * You can create an instance by {@code [] as ImmutableDeque}.
 * <p/>
 * Example:
 * <pre class="groovyTestCase">
 * def deque = [] as ImmutableDeque
 * deque += 1
 * assert 1 == deque.peek()
 * deque -= [1]
 * assert 0 == deque.size()
 * </pre>
 *
 * @author mtklein
 * @author Yu Kobayashi
 * @since 2.4.0
 */
public interface ImmutableDeque<E> extends ImmutableCollection<E>, Deque<E> {
    /**
     * Complexity: O(1)
     *
     * @return the first element of this deque
     */
    E peek();

    /**
     * Complexity: O(1)
     *
     * @return the first element of this deque
     */
    E peekFirst();

    /**
     * Complexity: O(1)
     *
     * @return the last element of this deque
     */
    E peekLast();

    /**
     * Complexity: O(1)
     *
     * @return the first element of this deque
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    E element();

    /**
     * Complexity: O(1)
     *
     * @return the first element of this deque
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    E getFirst();

    /**
     * Complexity: O(1)
     *
     * @return the last element of this deque
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    E getLast();

    /**
     * Complexity: O(1)
     *
     * @return the first element of this deque
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    E first();

    /**
     * Complexity: O(1)
     *
     * @return the first element of this deque
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    E head();

    /**
     * Complexity: O(1)
     *
     * @return the last element of this deque
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    E last();

    /**
     * Complexity:<br>
     * <table>
     * <thead><tr><th>&nbsp;</th><th>Average-case</th><th>Amortized</th><th>Worst-case</th></tr></thead>
     * <tbody>
     * <tr><td>Used as Deque</td><td>O(1)</td><td>O(n)</td><td>O(n)</td></tr>
     * <tr><td>Used as Queue</td><td>O(1)</td><td>O(1)</td><td>O(n)</td></tr>
     * <tr><td>Used as Stack</td><td>O(1)</td><td>O(1)</td><td>O(1)</td></tr>
     * </tbody>
     * </table>
     *
     * @return a deque without its first element
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    ImmutableDeque<E> tail();

    /**
     * Complexity:<br>
     * <table>
     * <thead><tr><th>&nbsp;</th><th>Average-case</th><th>Amortized</th><th>Worst-case</th></tr></thead>
     * <tbody>
     * <tr><td>Used as Deque</td><td>O(1)</td><td>O(n)</td><td>O(n)</td></tr>
     * <tr><td>Used as Queue</td><td>O(1)</td><td>O(1)</td><td>O(n)</td></tr>
     * <tr><td>Used as Stack</td><td>O(1)</td><td>O(1)</td><td>O(1)</td></tr>
     * </tbody>
     * </table>
     *
     * @return a deque without its last element
     * @throws java.util.NoSuchElementException if this deque is empty
     */
    ImmutableDeque<E> init();

    /**
     * Complexity: O(1)
     *
     * @param element an element to append
     * @return a deque which contains the element and all of the elements of this
     */
    ImmutableDeque<E> plus(E element);

    /**
     * Complexity: O(1)
     *
     * @param element an element to append
     * @return a deque which contains the element and all of the elements of this
     */
    ImmutableDeque<E> plusFirst(E element);

    /**
     * Complexity: O(1)
     *
     * @param element an element to append
     * @return a deque which contains the element and all of the elements of this
     */
    ImmutableDeque<E> plusLast(E element);

    /**
     * Complexity: O(iterable.size())
     *
     * @param iterable elements to append
     * @return a deque which contains all of the elements of iterable and this
     */
    ImmutableDeque<E> plus(Iterable<? extends E> iterable);

    /**
     * Complexity: O(iterable.size())
     *
     * @param iterable elements to append
     * @return a deque which contains all of the elements of iterable and this
     */
    ImmutableDeque<E> plusFirst(Iterable<? extends E> iterable);

    /**
     * Complexity: O(iterable.size())
     *
     * @param iterable elements to append
     * @return a deque which contains all of the elements of iterable and this
     */
    ImmutableDeque<E> plusLast(Iterable<? extends E> iterable);

    /**
     * Complexity: O(n)
     *
     * @param element an element to remove
     * @return this with a single instance of the element removed, if the element is in this deque
     */
    ImmutableDeque<E> minus(Object element);

    /**
     * Complexity: O(n + iterable.size())
     *
     * @param iterable elements to remove
     * @return this with all elements of the iterable completely removed
     */
    ImmutableDeque<E> minus(Iterable<?> iterable);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean offer(E e);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E poll();

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E remove();

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    void addFirst(E e);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    void addLast(E e);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean offerFirst(E e);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean offerLast(E e);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E removeFirst();

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E removeLast();

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E pollFirst();

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E pollLast();

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean removeFirstOccurrence(Object o);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean removeLastOccurrence(Object o);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    void push(E e);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E pop();
}
