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
 * Represents a list of 7 typed Objects.
 *
 * @since 2.4.0
 */
public class Tuple7<T1, T2, T3, T4, T5, T6, T7> extends Tuple {
    public Tuple7(T1 first, T2 second, T3 third, T4 fourth, T5 fifth, T6 sixth, T7 seventh) {
        super(new Object[]{first, second, third, fourth, fifth, sixth, seventh});
    }

    @SuppressWarnings("unchecked")
    public T1 getFirst() {
        return (T1) get(0);
    }

    @SuppressWarnings("unchecked")
    public T2 getSecond() {
        return (T2) get(1);
    }

    @SuppressWarnings("unchecked")
    public T3 getThird() {
        return (T3) get(2);
    }

    @SuppressWarnings("unchecked")
    public T4 getFourth() {
        return (T4) get(3);
    }

    @SuppressWarnings("unchecked")
    public T5 getFifth() {
        return (T5) get(4);
    }

    @SuppressWarnings("unchecked")
    public T6 getSixth() {
        return (T6) get(5);
    }

    @SuppressWarnings("unchecked")
    public T7 getSeventh() {
        return (T7) get(6);
    }
}
