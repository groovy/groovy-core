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

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.pcollections.MapPSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Yu Kobayashi
 * @since 2.4.0
 */
@SuppressWarnings("deprecation")
class ImmutableSetImpl<E> implements ImmutableSet<E>, Serializable {
    private static final ImmutableSetImpl<Object> EMPTY = new ImmutableSetImpl<Object>(MapPSet.empty());
    private static final long serialVersionUID = -2986101792651388100L;

    private final MapPSet<E> set;

    private ImmutableSetImpl(MapPSet<E> set) {
        this.set = set;
    }

    @SuppressWarnings("unchecked")
    static <E> ImmutableSetImpl<E> empty() {
        return (ImmutableSetImpl<E>) EMPTY;
    }

    static <E> ImmutableSetImpl<E> from(Iterable<? extends E> iterable) {
        return (ImmutableSetImpl<E>) empty().plus(iterable);
    }

    public ImmutableSet<E> plus(E element) {
        return new ImmutableSetImpl<E>(set.plus(element));
    }

    public ImmutableSet<E> plus(Iterable<? extends E> iterable) {
        return new ImmutableSetImpl<E>(set.plusAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public ImmutableSet<E> minus(Object element) {
        return new ImmutableSetImpl<E>(set.minus(element));
    }

    public ImmutableSet<E> minus(Iterable<?> iterable) {
        return new ImmutableSetImpl<E>(set.minusAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public int size() {
        return set.size();
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public boolean contains(Object o) {
        return set.contains(o);
    }

    public Iterator<E> iterator() {
        return set.iterator();
    }

    public Object[] toArray() {
        return set.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return set.toArray(a);
    }

    public boolean add(E o) {
        return set.add(o);
    }

    public boolean remove(Object o) {
        return set.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return set.addAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return set.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return set.retainAll(c);
    }

    public void clear() {
        set.clear();
    }

    public int hashCode() {
        return set.hashCode();
    }

    public boolean equals(Object obj) {
        return (obj instanceof ImmutableSetImpl) && set.equals(((ImmutableSetImpl) obj).set);
    }

    public String toString() {
        return DefaultGroovyMethods.toString(set);
    }
}
