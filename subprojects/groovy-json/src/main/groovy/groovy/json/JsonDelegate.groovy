/*
 * Copyright 2003-2012 the original author or authors.
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
package groovy.json

/**
 * Utility class used as delegate of closures representing JSON objects.
 *
 * @author Guillaume Laforge
 * @since 1.8.0
 */
class JsonDelegate {
    def content = [:]

    /**
     * Intercepts calls for setting a key and value for a JSON object
     *
     * @param name the key name
     * @param args the value associated with the key
     */
    def invokeMethod(String name, Object args) {
        if (args) {
            if (args.size () == 1) {
                content[name] = args[0]
            } else if (args.size () == 2 && args[0] instanceof Collection && args[1] instanceof Closure) {
                content[name] = args[0].collect {curryDelegateAndGetContent (args[1],it)}
            } else {
                content[name] = args.toList ()
            }
        }
    }

    /**
     * Factory method for creating <code>JsonDelegate</code>s from closures.
     *
     * @param c closure representing JSON objects
     * @return an instance of <code>JsonDelegate</code>
     */
    static cloneDelegateAndGetContent(Closure c) {
        def delegate = new JsonDelegate()
        Closure cloned = c.clone()
        cloned.delegate = delegate
        cloned.resolveStrategy = Closure.DELEGATE_FIRST
        cloned()
        return delegate.content
    }

    /**
     * Factory method for creating <code>JsonDelegate</code>s from closures currying an object
     * argument.
     *
     * @param c closure representing JSON objects
     * @param o an object curried to the closure
     * @return an instance of <code>JsonDelegate</code>
     */
    static curryDelegateAndGetContent(Closure c, Object o) {
        def delegate = new JsonDelegate()
        Closure curried = c.curry (o)
        curried.delegate = delegate
        curried.resolveStrategy = Closure.DELEGATE_FIRST
        curried()
        return delegate.content
    }
}
