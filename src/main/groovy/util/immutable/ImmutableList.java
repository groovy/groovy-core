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

import java.util.Collection;
import java.util.List;

/**
 * An immutable and persistent list.
 * <p/>
 * You can create an instance by {@code [] as ImmutableList}.
 * <p/>
 * Example:
 * <pre class="groovyTestCase">
 * def list = [] as ImmutableList
 * list += 1
 * assert 1 == list[0]
 * list -= [1]
 * assert 0 == list.size()
 * </pre>
 *
 * @author harold
 * @author Yu Kobayashi
 * @since 2.4.0
 */
public interface ImmutableList<E> extends ImmutableCollection<E>, List<E> {
    /**
     * Complexity: O(log n)
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if index &lt; 0 || index &gt;= size()
     */
    E get(int index);

    /**
     * Returns a vector consisting of the elements of this with e appended.
     * <p/>
     * Complexity: O(log n)
     *
     * @param element an element to append
     * @return a list which contains the element and all of the elements of this
     */
    ImmutableList<E> plus(E element);

    /**
     * Returns a vector consisting of the elements of this with list appended.
     * <p/>
     * Complexity: O((log n) * list.size())
     *
     * @param iterable elements to append
     * @return a list which contains all of the elements of iterable and this
     */
    ImmutableList<E> plus(Iterable<? extends E> iterable);

    /**
     * Complexity: O(log n)
     *
     * @param index   an index to insert
     * @param element an element to insert
     * @return a list consisting of the elements of this with the element inserted at the specified index.
     * @throws IndexOutOfBoundsException if index &lt; 0 || index &gt; size()
     */
    ImmutableList<E> plusAt(int index, E element);

    /**
     * Complexity: O((log n) * list.size())
     *
     * @param index    an index to insert
     * @param iterable elements to insert
     * @return a list consisting of the elements of this with the iterable inserted at the specified index.
     * @throws IndexOutOfBoundsException if index &lt; 0 || index &gt; size()
     */
    ImmutableList<E> plusAt(int index, Iterable<? extends E> iterable);

    /**
     * Complexity: O(log n)
     *
     * @param index   an index to replace
     * @param element an element to replace
     * @return a list consisting of the elements of this with the element replacing at the specified index.
     * @throws IndexOutOfBoundsException if index &lt; 0 || index &gt;= size()
     */
    ImmutableList<E> replaceAt(int index, E element);

    /**
     * Returns a sequence consisting of the elements of this without the first occurrence of the specified element.
     * <p/>
     * Complexity: O(log n)
     *
     * @param element an element to remove
     * @return this with a single instance of the element removed, if the element is in this list
     */
    ImmutableList<E> minus(Object element);

    /**
     * Complexity: O(log n * list.size())
     *
     * @param iterable elements to remove
     * @return this with all elements of the iterable completely removed
     */
    ImmutableList<E> minus(Iterable<?> iterable);

    /**
     * Complexity: O(log n)
     *
     * @param index an index to remove
     * @return a list consisting of the elements of this with the element at the specified index removed.
     * @throws IndexOutOfBoundsException if index &lt; 0 || index &gt;= size()
     */
    ImmutableList<E> minusAt(int index);

    /**
     * Complexity: O(log n)
     *
     * @param start a start index
     * @param end   a end index
     * @return a view of the specified range within this list
     */
    ImmutableList<E> subList(int start, int end);

    /**
     * Complexity: O(log n)
     *
     * @param start a start index
     * @return subList(start, this.size())
     */
    ImmutableList<E> subList(int start);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean addAll(int index, Collection<? extends E> c);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    void add(int index, E e);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    E remove(int index);
}
