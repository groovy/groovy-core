/*
 * Copyright 2003-2007 the original author or authors.
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
package groovy.lang;

/**
 * Distinguish a parameter array from Object[].
 *
 * @author Pilho Kim
 * @version $Revision$
 */
public class ParameterArray {

    private Object parameters;

    public ParameterArray(Object data) {
        parameters = packArray(data);
    }

    private Object packArray(Object object) {
        if (object instanceof Object[])
            return object;
        else
            return object;
    }

    public Object get() {
        return parameters;
    }

    public String toString() {
        if (parameters == null)
            return "<null parameter>";
        return parameters.toString();
    }
}
