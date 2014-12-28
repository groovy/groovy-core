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

package groovy.lang;

/**
 * Represents a list of 1 typed Object.
 *
 * @since 2.4.0
 */
public class Tuple1<T1> extends Tuple {
    public Tuple1(T1 first) {
        super(new Object[]{first});
    }

    @SuppressWarnings("unchecked")
    public T1 getFirst() {
        return (T1) get(0);
    }
}
