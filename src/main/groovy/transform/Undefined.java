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

package groovy.transform;

import org.codehaus.groovy.ast.ClassNode;

/**
 * Java doesn't allow you to have null as an attribute value. It wants you to indicate what you really
 * mean by null, so that is what we do here - as ugly as it is.
 */
public final class Undefined {
    private Undefined() {}
    public static final String STRING = "<DummyUndefinedMarkerString-DoNotUse>";
    public static final class CLASS {}
    public static boolean isUndefined(String other) { return STRING.equals(other); }
    public static boolean isUndefined(ClassNode other) { return CLASS.class.getName().equals(other.getName()); }
}
