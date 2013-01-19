/*
 * Copyright 2003-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.util;

import groovy.lang.Closure;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

/**
 * Set decorator that will trigger PropertyChangeEvents when a value changes.<br>
 * An optional Closure may be specified and will work as a filter, if it returns true the property
 * will trigger an event (if the value indeed changed), otherwise it won't. The Closure may receive
 * 1 or 2 parameters, the single one being the value, the other one both the key and value, for
 * example:
 * <p/>
 * <pre>
 * // skip all properties whose value is a closure
 * def set = new ObservableSet( {!(it instanceof Closure)} )
 * &lt;p/&gt;
 * // skip all properties whose name matches a regex
 * def set = new ObservableSet( { name, value -&gt; !(name =&tilde; /[A-Z+]/) } )
 * </pre>
 * <p/>
 * <p>
 * The current implementation will trigger specialized events in the following scenarios, you need
 * not register a different listener as those events extend from PropertyChangeEvent
 * <ul>
 * <li>ObservableSet.ElementAddedEvent - a new element is added to the set</li>
 * <li>ObservableSet.ElementRemovedEvent - an element is removed from the set</li>
 * <li>ObservableSet.ElementUpdatedEvent - an element changes value (same as regular
 * PropertyChangeEvent)</li>
 * <li>ObservableSet.ElementClearedEvent - all elements have been removed from the list</li>
 * <li>ObservableSet.MultiElementAddedEvent - triggered by calling set.addAll()</li>
 * <li>ObservableSet.MultiElementRemovedEvent - triggered by calling
 * set.removeAll()/set.retainAll()</li>
 * </ul>
 * </p>
 * <p/>
 * <p>
 * <strong>Bound properties</strong>
 * <ul>
 * <li><tt>content</tt> - read-only.</li>
 * <li><tt>size</tt> - read-only.</li>
 * </ul>
 * </p>
 *
 * @author <a href="mailto:aalmiray@users.sourceforge.net">Andres Almiray</a>
 */
public class ObservableSet<E> implements Set<E> {
    private Set<E> delegate;
    private PropertyChangeSupport pcs;
    private Closure test;

    public static final String SIZE_PROPERTY = "size";
    public static final String CONTENT_PROPERTY = "content";

    public ObservableSet() {
        this(new HashSet<E>(), null);
    }

    public ObservableSet(Set<E> delegate) {
        this(delegate, null);
    }

    public ObservableSet(Closure test) {
        this(new HashSet<E>(), test);
    }

    public ObservableSet(Set<E> delegate, Closure test) {
        this.delegate = delegate;
        this.test = test;
        this.pcs = new PropertyChangeSupport(this);
    }

    public Set<E> getContent() {
        return Collections.unmodifiableSet(delegate);
    }

    protected Set<E> getDelegateSet() {
        return delegate;
    }

    protected Closure getTest() {
        return test;
    }

    protected void fireElementAddedEvent(Object element) {
        fireElementEvent(new ElementAddedEvent(this, element));
    }

    protected void fireMultiElementAddedEvent(List values) {
        fireElementEvent(new MultiElementAddedEvent(this, values));
    }

    protected void fireElementClearedEvent(List values) {
        fireElementEvent(new ElementClearedEvent(this, values));
    }

    protected void fireElementRemovedEvent(Object element) {
        fireElementEvent(new ElementRemovedEvent(this, element));
    }

    protected void fireMultiElementRemovedEvent(List values) {
        fireElementEvent(new MultiElementRemovedEvent(this, values));
    }

    protected void fireElementEvent(ElementEvent event) {
        pcs.firePropertyChange(event);
    }

    protected void fireSizeChangedEvent(int oldValue, int newValue) {
        pcs.firePropertyChange(new PropertyChangeEvent(this, SIZE_PROPERTY, oldValue, newValue));
    }

    // observable interface

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    public boolean hasListeners(String propertyName) {
        return pcs.hasListeners(propertyName);
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    public Iterator<E> iterator() {
        return new ObservableIterator<E>(delegate.iterator());
    }

    public Object[] toArray() {
        return delegate.toArray();
    }

    public <T> T[] toArray(T[] ts) {
        return (T[]) delegate.toArray(ts);
    }

    public boolean add(E e) {
        int oldSize = size();
        boolean success = delegate.add(e);
        if (success) {
            if (test != null) {
                Object result = test.call(e);
                if (result != null && result instanceof Boolean && (Boolean) result) {
                    fireElementAddedEvent(e);
                    fireSizeChangedEvent(oldSize, size());
                }
            } else {
                fireElementAddedEvent(e);
                fireSizeChangedEvent(oldSize, size());
            }
        }
        return success;
    }

    public boolean remove(Object o) {
        int oldSize = size();
        boolean success = delegate.remove(o);
        if (success) {
            fireElementRemovedEvent(o);
            fireSizeChangedEvent(oldSize, size());
        }
        return success;
    }

    public boolean containsAll(Collection<?> objects) {
        return delegate.containsAll(objects);
    }

    public boolean addAll(Collection<? extends E> c) {
        Set<E> duplicates = new HashSet<E>();
        if (null != c) {
            for (E e : c) {
                if (!delegate.contains(e)) continue;
                duplicates.add(e);
            }
        }

        int oldSize = size();
        boolean success = delegate.addAll(c);

        if (success && c != null) {
            List<E> values = new ArrayList<E>();
            for (E element : c) {
                if (test != null) {
                    Object result = test.call(element);
                    if (result != null && result instanceof Boolean && (Boolean) result && !duplicates.contains(element)) {
                        values.add(element);
                    }
                } else if (!duplicates.contains(element)) {
                    values.add(element);
                }
            }
            if (values.size() > 0) {
                fireMultiElementAddedEvent(values);
                fireSizeChangedEvent(oldSize, size());
            }
        }

        return success;
    }

    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            return false;
        }

        List values = new ArrayList();
        for (Object element : delegate) {
            if (!c.contains(element)) {
                values.add(element);
            }
        }

        int oldSize = size();
        boolean success = delegate.retainAll(c);
        if (success && !values.isEmpty()) {
            fireMultiElementRemovedEvent(values);
            fireSizeChangedEvent(oldSize, size());
        }

        return success;
    }

    public boolean removeAll(Collection<?> c) {
        if (c == null) {
            return false;
        }

        List values = new ArrayList();
        for (Object element : c) {
            if (delegate.contains(element)) {
                values.add(element);
            }
        }

        int oldSize = size();
        boolean success = delegate.removeAll(c);
        if (success && !values.isEmpty()) {
            fireMultiElementRemovedEvent(values);
            fireSizeChangedEvent(oldSize, size());
        }

        return success;
    }

    public void clear() {
        int oldSize = size();
        List<E> values = new ArrayList<E>();
        values.addAll(delegate);
        delegate.clear();
        if (!values.isEmpty()) {
            fireElementClearedEvent(values);
        }
        fireSizeChangedEvent(oldSize, size());
    }

    protected class ObservableIterator<E> implements Iterator<E> {
        private Iterator<E> iterDelegate;
        private final Stack<E> stack = new Stack<E>();

        public ObservableIterator(Iterator<E> iterDelegate) {
            this.iterDelegate = iterDelegate;
        }

        public Iterator<E> getDelegate() {
            return iterDelegate;
        }

        public boolean hasNext() {
            return iterDelegate.hasNext();
        }

        public E next() {
            stack.push(iterDelegate.next());
            return stack.peek();
        }

        public void remove() {
            int oldSize = ObservableSet.this.size();
            iterDelegate.remove();
            fireElementRemovedEvent(stack.pop());
            fireSizeChangedEvent(oldSize, size());
        }
    }

    public enum ChangeType {
        ADDED, REMOVED, CLEARED, MULTI_ADD, MULTI_REMOVE, NONE;

        public static final Object oldValue = new Object();
        public static final Object newValue = new Object();
    }

    public abstract static class ElementEvent extends PropertyChangeEvent {
        private final ChangeType type;

        public ElementEvent(Object source, Object oldValue, Object newValue, ChangeType type) {
            super(source, ObservableSet.CONTENT_PROPERTY, oldValue, newValue);
            this.type = type;
        }

        public int getType() {
            return type.ordinal();
        }

        public ChangeType getChangeType() {
            return type;
        }

        public String getTypeAsString() {
            return type.name().toUpperCase();
        }
    }

    public static class ElementAddedEvent extends ElementEvent {
        public ElementAddedEvent(Object source, Object newValue) {
            super(source, null, newValue, ChangeType.ADDED);
        }
    }

    public static class ElementRemovedEvent extends ElementEvent {
        public ElementRemovedEvent(Object source, Object value) {
            super(source, value, null, ChangeType.REMOVED);
        }
    }

    public static class ElementClearedEvent extends ElementEvent {
        private List values = new ArrayList();

        public ElementClearedEvent(Object source, List values) {
            super(source, ChangeType.oldValue, ChangeType.newValue, ChangeType.CLEARED);
            if (values != null) {
                this.values.addAll(values);
            }
        }

        public List getValues() {
            return Collections.unmodifiableList(values);
        }
    }

    public static class MultiElementAddedEvent extends ElementEvent {
        private List values = new ArrayList();

        public MultiElementAddedEvent(Object source, List values) {
            super(source, ChangeType.oldValue, ChangeType.newValue, ChangeType.MULTI_ADD);
            if (values != null) {
                this.values.addAll(values);
            }
        }

        public List getValues() {
            return Collections.unmodifiableList(values);
        }
    }

    public static class MultiElementRemovedEvent extends ElementEvent {
        private List values = new ArrayList();

        public MultiElementRemovedEvent(Object source, List values) {
            super(source, ChangeType.oldValue, ChangeType.newValue, ChangeType.MULTI_REMOVE);
            if (values != null) {
                this.values.addAll(values);
            }
        }

        public List getValues() {
            return Collections.unmodifiableList(values);
        }
    }
}
