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

/**
 * @author Richard Hightower
 */
public class ArrayUtils {

    public static char[] copyRange(char[] source, int startIndex, int endIndex) {
        int len = endIndex - startIndex;
        char[] copy = new char[len];
        System.arraycopy(source, startIndex, copy, 0, Math.min(source.length - startIndex, len));
        return copy;
    }
}
