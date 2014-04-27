package jsr292.java.lang;

import java.lang.ref.WeakReference;


// The algorithm works like the double check locking pattern.
// The table of the hash map is stored in a volatile field
// which is written after any modification of the table entries
// Each entry is a pair class/value and the entry is stored in a weak ref
// like ephemerons.
// The get() tries to retrieve the value without synchronization but
// may see intermediary states. Perhaps another thread has already initilized
// the value but the current thread doesn't see the modification,
// in that case it will call lockedGet which works under a lock.
// The other intermediary state is that a thread can see that the entry exist
// but not initialized because initialValue() can call get() or remove(),
// like in the case above, we fallback to lockedGet.
// lockedGet() prunes links from the collision list that doesn't reference an entry anymore
// (because there are weak links) or resize the table if there is not enough space.
// In case of a resize, all empty weak links are removed.
// Before calling initialValue(), both resize() and prune() create a new Entry if necessary
// and lockedGet will store the result of the call to initialValue() just after to call it.
// remove(), like prune() unlinks weak links that doesn't reference an entry anymore.
// Entry that are not fully initialized (stale entry) are not removed because there still not exist.
// 
public abstract class ClassValue<T> {
    private final Object lock = new Object();
    private volatile WeakLink[] weakLinks;
    private int size;  // indicate roughly the number of values, the real number may be lower
    
    private static class WeakLink extends WeakReference<Entry> {
        final WeakLink next;
        
        WeakLink(Entry entry, WeakLink next) {
            super(entry);
            this.next = next;
        }
    }
    
    // type & value are GCable at the same time
    private static class Entry {
        final Class<?> type;
        Object value;  // null means a stale value
        
        Entry(Class<?> type) {
            this.type = type;
        }
    }
    
    private static final Object NULL_VALUE = new Object();
    
    private static Object maskNull(Object value) {
        return (value == null)? NULL_VALUE: value;
    }
    private static Object unmaskNull(Object value) {
        return (value == NULL_VALUE)? null: value;
    }
    
    protected ClassValue() {
        weakLinks = new WeakLink[16];
    }

    protected abstract T computeValue(Class<?> type);

    @SuppressWarnings("unchecked")
    public T get(Class<?> type) {
        WeakLink[] weakLinks = this.weakLinks;   // volatile read
        int index = (type.hashCode() & 0x7fffffff) & (weakLinks.length - 1); 
        WeakLink link = weakLinks[index];
        for(;link != null; link = link.next) {
            Entry entry = link.get();
            if (entry != null && entry.type == type) {
                Object value = entry.value;
                if (value == null) {
                    // stale value, need to retry with the lock
                    break;
                }
                return (T)unmaskNull(value);
            }
        }
        
        return lockedGet(type);
    }
    
    
    
    @SuppressWarnings("unchecked")
    private T lockedGet(Class<?> type) {
        synchronized (lock) {
            WeakLink[] weakLinks = this.weakLinks;
            int length = weakLinks.length;
            
            Entry entry = (this.size == length >> 1)?
                 resize(type, weakLinks, length):
                 prune(type, weakLinks, length);
            
            Object value = entry.value;
            if (value != null) {
                return (T)unmaskNull(value);
            }

            T initialValue = computeValue(type);
            
            value = entry.value;
            if (value != null) {  // entry already initialized ?
                return (T)unmaskNull(value);
            }
            entry.value = maskNull(initialValue);
            this.weakLinks = this.weakLinks;        // volatile write
            return initialValue;
        }
    } 
    
    private Entry prune(Class<?> type, WeakLink[] weakLinks, int length) {
        int index = (type.hashCode() & 0x7fffffff) & (length - 1); 
        
        int size = this.size;
        WeakLink newLink = null;
        for(WeakLink l = weakLinks[index]; l != null; l = l.next) {
            Entry entry = l.get();
            if (entry == null) {
                size--;
                continue;
            }
            if (entry.type == type) {
                return entry;  // another thread may have already initialized the value
                // the table may not be cleanup, but that's not a big deal
                // because the other thread should have clean the thing up
            }
            newLink = new WeakLink(entry, newLink);
        }
        
        // new uninitialized link
        Entry newEntry = new Entry(type);
        weakLinks[index] = new WeakLink(newEntry, newLink);
        this.size = size + 1;
        
        return newEntry;
    }
    
    private Entry resize(Class<?> type, WeakLink[] weakLinks, int length) {
        WeakLink[] newLinks = new WeakLink[length << 1];
        int newLength = newLinks.length;
        
        Entry newEntry = null;
        int size = 0;  // recompute the size
        for(int i=0; i<length; i++) {
            for(WeakLink l = weakLinks[i]; l != null; l = l.next) {
                Entry entry = l.get();
                if (entry == null) {
                    continue;
                }
                
                Class<?> entryType = entry.type;
                if (entryType == type) {
                    newEntry = entry;
                }
                
                int index = (entryType.hashCode() & 0x7fffffff) & (newLength - 1);    
                newLinks[index] = new WeakLink(entry, newLinks[index]);
                size++;
            }
        }
        
        if (newEntry == null) {
            int index = (type.hashCode() & 0x7fffffff) & (newLength - 1); 
            newEntry = new Entry(type);
            newLinks[index] = new WeakLink(newEntry, newLinks[index]);
            size++;
        }
        
        
        this.weakLinks = newLinks;
        this.size = size;
        
        return newEntry;
    }

    public void remove(Class<?> type) {
        synchronized (lock) {
            WeakLink[] weakLinks = this.weakLinks;
            int index = (type.hashCode() & 0x7fffffff) & (weakLinks.length - 1); 
            int size = this.size;
            
            WeakLink newLink = null;
            for(WeakLink link = weakLinks[index]; link != null; link = link.next) {
                Entry entry = link.get(); 
                if (entry == null || (entry.type == type && entry.value != null)) {
                    size--;
                    continue;
                }
                newLink = new WeakLink(entry, newLink);
            }
            weakLinks[index] = newLink;
            this.size = size;
            this.weakLinks = this.weakLinks;      // volatile write
        }
    }
}
