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
public class Tuple1<T1> extends AbstractTuple {
    private final T1 first;

    public Tuple1(T1 first) {
        this.first = first;
    }

    public Object get(int index) {
        switch (index) {
            case 0:
                return first;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public int size() {
        return 1;
    }

    public T1 getFirst() {
        return first;
    }
}
