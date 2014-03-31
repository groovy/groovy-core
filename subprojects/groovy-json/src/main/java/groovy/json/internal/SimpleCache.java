/*
 * Copyright 2003-2014 the original author or authors.
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
 *
 * Derived from Boon all rights granted to Groovy project for this fork.
 */
package groovy.json.internal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Richard Hightower
 */
public class SimpleCache<K, V> implements Cache<K, V> {

    Map<K, V> map = new LinkedHashMap();

    private static class InternalCacheLinkedList<K, V> extends LinkedHashMap<K, V> {
        final int limit;

        InternalCacheLinkedList(final int limit, final boolean lru) {
            super(16, 0.75f, lru);
            this.limit = limit;
        }

        protected final boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return super.size() > limit;
        }
    }

    public SimpleCache(final int limit, CacheType type) {

        if (type.equals(CacheType.LRU)) {
            map = new InternalCacheLinkedList<K, V>(limit, true);
        } else {
            map = new InternalCacheLinkedList<K, V>(limit, false);
        }
    }

    public SimpleCache(final int limit) {

        map = new InternalCacheLinkedList<K, V>(limit, true);

    }

    public void put(K key, V value) {
        map.put(key, value);
    }

    public V get(K key) {
        return map.get(key);
    }

    //For testing only

    public V getSilent(K key) {
        V value = map.get(key);
        if (value != null) {
            map.remove(key);
            map.put(key, value);
        }
        return value;
    }

    public void remove(K key) {
        map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public String toString() {
        return map.toString();
    }

}
