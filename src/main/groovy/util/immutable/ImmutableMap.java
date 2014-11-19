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

import java.util.Map;

/**
 * An immutable and persistent map from non-null keys of type K to nullable values of type V.
 * <p/>
 * You can create an instance by {@code [:] as ImmutableMap}.
 * <p/>
 * Example:
 * <pre class="groovyTestCase">
 * def map = [:] as ImmutableMap
 * map += [a: 1]
 * assert 1 == map["a"]
 * map -= ["a"]
 * assert 0 == map.size()
 * </pre>
 *
 * @author harold
 * @author Yu Kobayashi
 * @since 2.4.0
 */
public interface ImmutableMap<K, V> extends Map<K, V> {
    /**
     * Complexity: O(log n)
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key
     */
    V get(Object key);

    /**
     * Complexity: O(log n)
     *
     * @param key   a non-null key
     * @param value a value
     * @return a map with the mappings of this but with key mapped to value
     */
    ImmutableMap<K, V> plus(K key, V value);

    /**
     * Complexity: O((log n) * map.size())
     *
     * @param map a map to append
     * @return this combined with map, with map's mappings used for any keys in both map and this
     */
    ImmutableMap<K, V> plus(Map<? extends K, ? extends V> map);

    /**
     * Complexity: O(log n)
     *
     * @param key a non-null key
     * @return a map with the mappings of this but with no value for key
     */
    ImmutableMap<K, V> minus(Object key);

    /**
     * Complexity: O((log n) * keys.size())
     *
     * @param keys non-null keys
     * @return a map with the mappings of this but with no value for any element of keys
     */
    ImmutableMap<K, V> minus(Iterable<?> keys);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    V putAt(K k, V v);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    V put(K k, V v);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    V remove(Object k);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    void putAll(Map<? extends K, ? extends V> m);

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Deprecated
    void clear();
}
