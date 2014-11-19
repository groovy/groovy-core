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
import org.pcollections.AmortizedPDeque;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Yu Kobayashi
 * @since 2.4.0
 */
@SuppressWarnings("deprecation")
class ImmutableDequeImpl<E> implements ImmutableDeque<E>, Serializable {
    private static final ImmutableDequeImpl<Object> EMPTY = new ImmutableDequeImpl<Object>(AmortizedPDeque.empty());
    private static final long serialVersionUID = 8383495243255576114L;

    private final AmortizedPDeque<E> deque;

    private ImmutableDequeImpl(AmortizedPDeque<E> deque) {
        this.deque = deque;
    }

    @SuppressWarnings("unchecked")
    static <E> ImmutableDequeImpl<E> empty() {
        return (ImmutableDequeImpl<E>) EMPTY;
    }

    static <E> ImmutableDequeImpl<E> from(Iterable<? extends E> iterable) {
        return (ImmutableDequeImpl<E>) empty().plus(iterable);
    }

    public E peek() {
        return deque.peek();
    }

    public E peekFirst() {
        return deque.peekFirst();
    }

    public E peekLast() {
        return deque.peekLast();
    }

    public E element() {
        return deque.element();
    }

    public E getFirst() {
        return deque.getFirst();
    }

    public E getLast() {
        return deque.getLast();
    }

    public E first() {
        return deque.first();
    }

    public E head() {
        return deque.head();
    }

    public E last() {
        return deque.last();
    }

    public ImmutableDeque<E> tail() {
        return new ImmutableDequeImpl<E>(deque.tail());
    }

    public ImmutableDeque<E> init() {
        return new ImmutableDequeImpl<E>(deque.init());
    }

    public ImmutableDeque<E> plus(E element) {
        return new ImmutableDequeImpl<E>(deque.plus(element));
    }

    public ImmutableDeque<E> plusFirst(E element) {
        return new ImmutableDequeImpl<E>(deque.plusFirst(element));
    }

    public ImmutableDeque<E> plusLast(E element) {
        return new ImmutableDequeImpl<E>(deque.plusLast(element));
    }

    public ImmutableDeque<E> plus(Iterable<? extends E> iterable) {
        return new ImmutableDequeImpl<E>(deque.plusAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public ImmutableDeque<E> plusFirst(Iterable<? extends E> iterable) {
        return new ImmutableDequeImpl<E>(deque.plusFirstAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public ImmutableDeque<E> plusLast(Iterable<? extends E> iterable) {
        return new ImmutableDequeImpl<E>(deque.plusLastAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public ImmutableDeque<E> minus(Object element) {
        return new ImmutableDequeImpl<E>(deque.minus(element));
    }

    public ImmutableDeque<E> minus(Iterable<?> iterable) {
        return new ImmutableDequeImpl<E>(deque.minusAll(DefaultGroovyMethods.asCollection(iterable)));
    }

    public int size() {
        return deque.size();
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }

    public boolean contains(Object o) {
        return deque.contains(o);
    }

    public Iterator<E> iterator() {
        return deque.iterator();
    }

    public Object[] toArray() {
        return deque.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return deque.toArray(a);
    }

    public boolean add(E o) {
        return deque.add(o);
    }

    public boolean remove(Object o) {
        return deque.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return deque.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return deque.addAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return deque.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return deque.retainAll(c);
    }

    public void clear() {
        deque.clear();
    }

    public boolean offer(E e) {
        return deque.offer(e);
    }

    public E poll() {
        return deque.poll();
    }

    public E remove() {
        return deque.remove();
    }

    public void addFirst(E e) {
        deque.addFirst(e);
    }

    public void addLast(E e) {
        deque.addLast(e);
    }

    public boolean offerFirst(E e) {
        return deque.offerFirst(e);
    }

    public boolean offerLast(E e) {
        return deque.offerLast(e);
    }

    public E removeFirst() {
        return deque.removeFirst();
    }

    public E removeLast() {
        return deque.removeLast();
    }

    public E pollFirst() {
        return deque.pollFirst();
    }

    public E pollLast() {
        return deque.pollLast();
    }

    public boolean removeFirstOccurrence(Object o) {
        return deque.removeFirstOccurrence(o);
    }

    public boolean removeLastOccurrence(Object o) {
        return deque.removeLastOccurrence(o);
    }

    public void push(E e) {
        deque.push(e);
    }

    public E pop() {
        return deque.pop();
    }

    public Iterator<E> descendingIterator() {
        return deque.descendingIterator();
    }

    public int hashCode() {
        return deque.hashCode();
    }

    public boolean equals(Object obj) {
        return (obj instanceof ImmutableDequeImpl) && deque.equals(((ImmutableDequeImpl) obj).deque);
    }

    public String toString() {
        return DefaultGroovyMethods.toString(deque);
    }
}
