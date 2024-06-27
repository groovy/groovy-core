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

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.util.AbstractList;

/**
 * @since 2.4.0
 */
public abstract class AbstractTuple extends AbstractList {
    private int hashCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AbstractTuple)) return false;

        AbstractTuple that = (AbstractTuple) o;
        if (size() != that.size()) return false;
        for (int i = 0; i < size(); i++) {
            if (!DefaultTypeTransformation.compareEqual(get(i), that.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            for (int i = 0; i < size(); i++) {
                Object value = get(i);
                int hash = (value != null) ? value.hashCode() : 0xbabe;
                hashCode ^= hash;
            }
            if (hashCode == 0) {
                hashCode = 0xbabe;
            }
        }
        return hashCode;
    }
}
