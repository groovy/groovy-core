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

import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * An exception occurred when invoking a Closure with the wrong number and/or
 * types of arguments
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class IncorrectClosureArgumentsException extends GroovyRuntimeException {

    private final Closure closure;
    private final Object arguments;
    private final Class[] expected;

    public IncorrectClosureArgumentsException(Closure closure, Object arguments, Class[] expected) {
        super(
                "Incorrect arguments to closure: "
                        + closure
                        + ". Expected: "
                        + InvokerHelper.toString(expected)
                        + ", actual: "
                        + InvokerHelper.toString(arguments)
        );
        this.closure = closure;
        this.arguments = arguments;
        this.expected = expected;
    }

    public Object getArguments() {
        return arguments;
    }

    public Closure getClosure() {
        return closure;
    }

    public Class[] getExpected() {
        return expected;
    }

}
