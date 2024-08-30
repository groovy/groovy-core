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

/**
 * An immutable and persistent collection of elements of type E.
 *
 * @author harold
 * @author Yu Kobayashi
 * @since 2.4.0
 */
public interface ImmutableCollection<E> extends Collection<E> {
    /**
     * @param element an element to append
     * @return a collection which contains the element and all of the elements of this
     */
    ImmutableCollection<E> plus(E element);

    /**
     * @param iterable elements to append
     * @return a collection which contains all of the elements of iterable and this
     */
    ImmutableCollection<E> plus(Iterable<? extends E> iterable);

    /**
     * @param element an element to remove
     * @return this with a single instance of the element removed, if the element is in this collection
     */
    ImmutableCollection<E> minus(Object element);

    /**
     * @param iterable elements to remove
     * @return this with all elements of the iterable completely removed
     */
    ImmutableCollection<E> minus(Iterable<?> iterable);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean add(E o);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean remove(Object o);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean addAll(Collection<? extends E> c);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean removeAll(Collection<?> c);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    boolean retainAll(Collection<?> c);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    void clear();
}
