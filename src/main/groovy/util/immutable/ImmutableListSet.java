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

/**
 * An immutable and presistent list set like {@link ImmutableSet} but preserves insertion order.
 * Persistent equivalent of {@link java.util.LinkedHashSet}.
 * <p/>
 * The elements must be non-null.
 * <p/>
 * You can create an instance by {@code [] as ImmutableListSet}.
 * <p/>
 * Example:
 * <pre class="groovyTestCase">
 * def listSet = [] as ImmutableListSet
 * listSet += 1
 * assert 1 == listSet[0]
 * listSet -= [1]
 * assert 0 == listSet.size()
 * </pre>
 *
 * @author Tassilo Horn &lt;horn@uni-koblenz.de&gt;
 * @author Yu Kobayashi
 * @since 2.4.0
 */
public interface ImmutableListSet<E> extends ImmutableSet<E> {
    /**
     * Complexity: O(log n)
     *
     * @param element an non-null element to append
     * @return a list set which contains the element and all of the elements of this
     */
    ImmutableListSet<E> plus(E element);

    /**
     * Complexity: O((log n) * iterable.size())
     *
     * @param iterable contains no null elements to append
     * @return a list set which contains all of the elements of list and this
     */
    ImmutableListSet<E> plus(Iterable<? extends E> iterable);

    /**
     * Complexity: O(log n)
     *
     * @param element an element to remove
     * @return this with a single instance of the element removed, if the element is in this list set
     */
    ImmutableListSet<E> minus(Object element);

    /**
     * Complexity: O((log n) * iterable.size())
     *
     * @param iterable elements to remove
     * @return this with all elements of the iterable completely removed
     */
    ImmutableListSet<E> minus(Iterable<?> iterable);

    /**
     * Complexity: O(log n)
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list set
     * @throws IndexOutOfBoundsException if index &lt; 0 || index &gt;= size()
     */
    E get(int index);

    /**
     * Complexity: O(log n)
     *
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in this list, or -1 if this list does not contain the element
     */
    int indexOf(Object o);

    /**
     * This always returns the same value of {@link #indexOf(Object)}.
     * <p/>
     * Complexity: O(log n)
     *
     * @param o element to search for
     * @return the index of the last occurrence of the specified element in this list, or -1 if this list does not contain the element
     */
    int lastIndexOf(Object o);
}
