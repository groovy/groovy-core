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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * List decorator that will trigger PropertyChangeEvents when a value changes.<br>
 * An optional Closure may be specified and will work as a filter, if it returns true the property
 * will trigger an event (if the value indeed changed), otherwise it won't. The Closure may receive
 * 1 or 2 parameters, the single one being the value, the other one both the key and value, for
 * example:
 * <pre>
 * // skip all properties whose value is a closure
 * def map = new ObservableList( {!(it instanceof Closure)} )
 *
 * // skip all properties whose name matches a regex
 * def map = new ObservableList( { name, value -&gt; !(name =&tilde; /[A-Z+]/) } )
 * </pre>
 * The current implementation will trigger specialized events in the following scenarios, you need
 * not register a different listener as those events extend from PropertyChangeEvent
 * <ul>
 * <li>ObservableList.ElementAddedEvent - a new element is added to the list</li>
 * <li>ObservableList.ElementRemovedEvent - an element is removed from the list</li>
 * <li>ObservableList.ElementUpdatedEvent - an element changes value (same as regular
 * PropertyChangeEvent)</li>
 * <li>ObservableList.ElementClearedEvent - all elements have been removed from the list</li>
 * <li>ObservableList.MultiElementAddedEvent - triggered by calling list.addAll()</li>
 * <li>ObservableList.MultiElementRemovedEvent - triggered by calling
 * list.removeAll()/list.retainAll()</li>
 * </ul>
 * <p>
 * <strong>Bound properties</strong>
 * <ul>
 * <li><tt>content</tt> - read-only.</li>
 * <li><tt>size</tt> - read-only.</li>
 * </ul>
 *
 * @author <a href="mailto:aalmiray@users.sourceforge.net">Andres Almiray</a>
 */
public class ObservableList implements List {
    private List delegate;
    private PropertyChangeSupport pcs;
    private Closure<Boolean> test;

    public static final String SIZE_PROPERTY = "size";
    public static final String CONTENT_PROPERTY = "content";

    public ObservableList() {
        this(new ArrayList(), null);
    }

    public ObservableList(List delegate) {
        this(delegate, null);
    }

    public ObservableList(Closure<Boolean> test) {
        this(new ArrayList(), test);
    }

    public ObservableList(List delegate, Closure<Boolean> test) {
        this.delegate = delegate;
        this.test = test;
        pcs = new PropertyChangeSupport(this);
    }

    public List getContent() {
        return Collections.unmodifiableList(delegate);
    }

    protected List getDelegateList() {
        return delegate;
    }

    protected Closure<Boolean> getTest() {
        return test;
    }

    protected void fireElementAddedEvent(int index, Object element) {
        fireElementEvent(new ElementAddedEvent(this, element, index));
    }

    protected void fireMultiElementAddedEvent(int index, List values) {
        fireElementEvent(new MultiElementAddedEvent(this, index, values));
    }

    protected void fireElementClearedEvent(List values) {
        fireElementEvent(new ElementClearedEvent(this, values));
    }

    protected void fireElementRemovedEvent(int index, Object element) {
        fireElementEvent(new ElementRemovedEvent(this, element, index));
    }

    protected void fireMultiElementRemovedEvent(List values) {
        fireElementEvent(new MultiElementRemovedEvent(this, values));
    }

    protected void fireElementUpdatedEvent(int index, Object oldValue, Object newValue) {
        fireElementEvent(new ElementUpdatedEvent(this, oldValue, newValue, index));
    }

    protected void fireElementEvent(ElementEvent event) {
        pcs.firePropertyChange(event);
    }

    protected void fireSizeChangedEvent(int oldValue, int newValue) {
        pcs.firePropertyChange(new PropertyChangeEvent(this, SIZE_PROPERTY, oldValue, newValue));
    }

    public void add(int index, Object element) {
        int oldSize = size();
        delegate.add(index, element);
        if (test != null) {
            Boolean result = test.call(element);
            if (result != null && result instanceof Boolean && result) {
                fireElementAddedEvent(index, element);
                fireSizeChangedEvent(oldSize, size());
            }
        } else {
            fireElementAddedEvent(index, element);
            fireSizeChangedEvent(oldSize, size());
        }
    }

    public boolean add(Object o) {
        int oldSize = size();
        boolean success = delegate.add(o);
        if (success) {
            if (test != null) {
                Boolean result = test.call(o);
                if (result != null && result instanceof Boolean && result) {
                    fireElementAddedEvent(size() - 1, o);
                    fireSizeChangedEvent(oldSize, size());
                }
            } else {
                fireElementAddedEvent(size() - 1, o);
                fireSizeChangedEvent(oldSize, size());
            }
        }
        return success;
    }

    public boolean addAll(Collection c) {
        int oldSize = size();
        int index = size() - 1;
        index = index < 0 ? 0 : index;

        boolean success = delegate.addAll(c);
        if (success && c != null) {
            List values = new ArrayList();
            for (Object element : c) {
                if (test != null) {
                    Boolean result = test.call(element);
                    if (result != null && result) {
                        values.add(element);
                    }
                } else {
                    values.add(element);
                }
            }
            if (values.size() > 0) {
                fireMultiElementAddedEvent(index, values);
                fireSizeChangedEvent(oldSize, size());
            }
        }

        return success;
    }

    public boolean addAll(int index, Collection c) {
        int oldSize = size();
        boolean success = delegate.addAll(index, c);

        if (success && c != null) {
            List values = new ArrayList();
            for (Object element : c) {
                if (test != null) {
                    Boolean result = test.call(element);
                    if (result != null && result) {
                        values.add(element);
                    }
                } else {
                    values.add(element);
                }
            }
            if (values.size() > 0) {
                fireMultiElementAddedEvent(index, values);
                fireSizeChangedEvent(oldSize, size());
            }
        }

        return success;
    }

    public void clear() {
        int oldSize = size();
        List values = new ArrayList();
        values.addAll(delegate);
        delegate.clear();
        if (!values.isEmpty()) {
            fireElementClearedEvent(values);
        }
        fireSizeChangedEvent(oldSize, size());
    }

    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    public boolean containsAll(Collection c) {
        return delegate.containsAll(c);
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public Object get(int index) {
        return delegate.get(index);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Iterator iterator() {
        return new ObservableIterator(delegate.iterator());
    }

    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return new ObservableListIterator(delegate.listIterator(), 0);
    }

    public ListIterator listIterator(int index) {
        return new ObservableListIterator(delegate.listIterator(index), index);
    }

    public Object remove(int index) {
        int oldSize = size();
        Object element = delegate.remove(index);
        fireElementRemovedEvent(index, element);
        fireSizeChangedEvent(oldSize, size());
        return element;
    }

    public boolean remove(Object o) {
        int oldSize = size();
        int index = delegate.indexOf(o);
        boolean success = delegate.remove(o);
        if (success) {
            fireElementRemovedEvent(index, o);
            fireSizeChangedEvent(oldSize, size());
        }
        return success;
    }

    public boolean removeAll(Collection c) {
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

    public boolean retainAll(Collection c) {
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

    public Object set(int index, Object element) {
        Object oldValue = delegate.set(index, element);
        if (test != null) {
            Boolean result = test.call(element);
            if (result != null && result) {
                fireElementUpdatedEvent(index, oldValue, element);
            }
        } else {
            fireElementUpdatedEvent(index, oldValue, element);
        }
        return oldValue;
    }

    public int size() {
        return delegate.size();
    }

    public int getSize() {
        return size();
    }

    public List subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return delegate.toArray();
    }

    public Object[] toArray(Object[] a) {
        return delegate.toArray(a);
    }

    protected class ObservableIterator implements Iterator {
        private Iterator iterDelegate;
        protected int cursor = -1 ;

        public ObservableIterator(Iterator iterDelegate) {
            this.iterDelegate = iterDelegate;
        }

        public Iterator getDelegate() {
            return iterDelegate;
        }

        public boolean hasNext() {
            return iterDelegate.hasNext();
        }

        public Object next() {
            cursor++;
            return iterDelegate.next();
        }

        public void remove() {
            int oldSize = ObservableList.this.size();
            Object element = ObservableList.this.get(cursor);
            iterDelegate.remove();
            fireElementRemovedEvent(cursor, element);
            fireSizeChangedEvent(oldSize, size());
            cursor--;
        }
    }

    protected class ObservableListIterator extends ObservableIterator implements ListIterator {
        public ObservableListIterator(ListIterator iterDelegate, int index) {
            super(iterDelegate);
            cursor = index - 1;
        }

        public ListIterator getListIterator() {
            return (ListIterator) getDelegate();
        }

        public void add(Object o) {
            ObservableList.this.add(o);
            cursor++;
        }

        public boolean hasPrevious() {
            return getListIterator().hasPrevious();
        }

        public int nextIndex() {
            return getListIterator().nextIndex();
        }

        public Object previous() {
            return getListIterator().previous();
        }

        public int previousIndex() {
            return getListIterator().previousIndex();
        }

        public void set(Object o) {
            ObservableList.this.set(cursor, o);
        }
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

    public enum ChangeType {
        ADDED, UPDATED, REMOVED, CLEARED, MULTI_ADD, MULTI_REMOVE, NONE;

        public static final Object oldValue = new Object();
        public static final Object newValue = new Object();

        public static ChangeType resolve(int ordinal) {
            switch (ordinal) {
                case 0:
                    return ADDED;
                case 2:
                    return REMOVED;
                case 3:
                    return CLEARED;
                case 4:
                    return MULTI_ADD;
                case 5:
                    return MULTI_REMOVE;
                case 6:
                    return NONE;
                case 1:
                default:
                    return UPDATED;
            }
        }
    }

    public abstract static class ElementEvent extends PropertyChangeEvent {

        private final ChangeType type;
        private final int index;

        public ElementEvent(ObservableList source, Object oldValue, Object newValue, int index, ChangeType type) {
            super(source, ObservableList.CONTENT_PROPERTY, oldValue, newValue);
            this.type = type;
            this.index = index;
        }

        public int getIndex() {
            return index;
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
        public ElementAddedEvent(ObservableList source, Object newValue, int index) {
            super(source, null, newValue, index, ChangeType.ADDED);
        }
    }

    public static class ElementUpdatedEvent extends ElementEvent {
        public ElementUpdatedEvent(ObservableList source, Object oldValue, Object newValue, int index) {
            super(source, oldValue, newValue, index, ChangeType.UPDATED);
        }
    }

    public static class ElementRemovedEvent extends ElementEvent {
        public ElementRemovedEvent(ObservableList source, Object value, int index) {
            super(source, value, null, index, ChangeType.REMOVED);
        }
    }

    public static class ElementClearedEvent extends ElementEvent {
        private List values = new ArrayList();

        public ElementClearedEvent(ObservableList source, List values) {
            super(source, ChangeType.oldValue, ChangeType.newValue, 0, ChangeType.CLEARED);
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

        public MultiElementAddedEvent(ObservableList source, int index, List values) {
            super(source, ChangeType.oldValue, ChangeType.newValue, index, ChangeType.MULTI_ADD);
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

        public MultiElementRemovedEvent(ObservableList source, List values) {
            super(source, ChangeType.oldValue, ChangeType.newValue, 0, ChangeType.MULTI_REMOVE);
            if (values != null) {
                this.values.addAll(values);
            }
        }

        public List getValues() {
            return Collections.unmodifiableList(values);
        }
    }
}
