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
import org.pcollections.TreePVector;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * @author Yu Kobayashi
 * @since 2.4.0
 */
class ImmutableListImpl<E> implements ImmutableList<E>, Serializable {
    private static final ImmutableListImpl<Object> EMPTY = new ImmutableListImpl<Object>(TreePVector.empty());
    private static final long serialVersionUID = -7182079639404183366L;

    private final TreePVector<E> list;

    private ImmutableListImpl(TreePVector<E> list) {
        this.list = list;
    }

    @SuppressWarnings("unchecked")
    static <E> ImmutableListImpl<E> empty() {
        return (ImmutableListImpl<E>) EMPTY;
    }

    static <E> ImmutableListImpl<E> from(Iterable<? extends E> iterable) {
        return (ImmutableListImpl<E>) empty().plus(iterable);
    }

    public E get(int index) {
        return list.get(index);
    }

    public E set(int index, E element) {
        return list.set(index, element);
    }

    public ImmutableList<E> plus(E element) {
        return new ImmutableListImpl<E>(list.plus(element));
    }

    public ImmutableList<E> plus(Iterable<? extends E> iterable) {
        return new ImmutableListImpl<E>(list.plusAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public ImmutableList<E> plusAt(int index, E element) {
        return new ImmutableListImpl<E>(list.plus(index, element));
    }

    public ImmutableList<E> plusAt(int index, Iterable<? extends E> iterable) {
        return new ImmutableListImpl<E>(list.plusAll(index, DefaultGroovyMethods.asCollection(iterable)));
    }

    public ImmutableList<E> replaceAt(int index, E element) {
        return new ImmutableListImpl<E>(list.with(index, element));
    }

    public ImmutableList<E> minus(Object element) {
        return new ImmutableListImpl<E>(list.minus(element));
    }

    public ImmutableList<E> minus(Iterable<?> iterable) {
        return new ImmutableListImpl<E>(list.minusAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public Iterator<E> iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    public boolean add(E o) {
        return list.add(o);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return list.addAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    public void clear() {
        list.clear();
    }

    public ImmutableList<E> minusAt(int index) {
        return new ImmutableListImpl<E>(list.minus(index));
    }

    public ImmutableList<E> subList(int start, int end) {
        return new ImmutableListImpl<E>(list.subList(start, end));
    }

    public ImmutableList<E> subList(int start) {
        return new ImmutableListImpl<E>(list.subList(start));
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        return list.addAll(index, c);
    }

    public void add(int index, E e) {
        list.add(index, e);
    }

    public E remove(int index) {
        return list.remove(index);
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    public int hashCode() {
        return list.hashCode();
    }

    public boolean equals(Object obj) {
        return (obj instanceof ImmutableListImpl) && list.equals(((ImmutableListImpl) obj).list);
    }

    public String toString() {
        return DefaultGroovyMethods.toString(list);
    }
}
