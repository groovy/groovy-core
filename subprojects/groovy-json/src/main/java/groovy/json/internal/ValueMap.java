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

import java.util.Map;

/**
 * @author Richard Hightower
 */
public interface ValueMap<K, V> extends Map<K, V> {

    /* add a map item value. */
    void add(MapItemValue miv);

    /**
     * Return size w/o hydrating the map.
     */
    int len();

    /**
     * Has the map been hydrated.
     */
    boolean hydrated();

    /**
     * Give me the items in the map without hydrating the map.
     * Realize that the array is likely larger than the length so array items can be null.
     */
    Entry<String, Value>[] items();

}
