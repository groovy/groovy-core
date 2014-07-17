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

import java.util.Set;

/**
 * An immutable and persistent set, containing no duplicate elements.
 * The elements must be non-null.
 * <p/>
 * You can create an instance by {@code [] as ImmutableSet}.
 * <p/>
 * Example:
 * <pre class="groovyTestCase">
 * def set = [] as ImmutableSet
 * set += 1
 * assert set.contains(1)
 * set -= [1]
 * assert 0 == set.size()
 * </pre>
 *
 * @author harold
 * @author Yu Kobayashi
 * @since 2.4.0
 */
public interface ImmutableSet<E> extends ImmutableCollection<E>, Set<E> {
    /**
     * Complexity: O(log n)
     *
     * @param element an non-null element to append
     * @return a set which contains the element and all of the elements of this
     */
    ImmutableSet<E> plus(E element);

    /**
     * Complexity: O((log n) * iterable.size())
     *
     * @param iterable contains non-null elements to append
     * @return a set which contains all of the elements of iterable and this
     */
    ImmutableSet<E> plus(Iterable<? extends E> iterable);

    /**
     * Complexity: O(log n)
     *
     * @param element an element to remove
     * @return this with a single instance of the element removed, if the element is in this set
     */
    ImmutableSet<E> minus(Object element);

    /**
     * Complexity: O((log n) * iterable.size())
     *
     * @param iterable elements to remove
     * @return this with all elements of the iterable completely removed
     */
    ImmutableSet<E> minus(Iterable<?> iterable);
}
