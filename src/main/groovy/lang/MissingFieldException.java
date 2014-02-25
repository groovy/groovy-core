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
 * An exception occurred if a dynamic field dispatch fails with an unknown field.
 * <p/>
 * Note that the Missing*Exception classes were named for consistency and
 * to avoid conflicts with JDK exceptions of the same name.
 *
 * @author <a href="mailto:jstrachan@protique.com">James Strachan</a>
 * @version $Revision$
 */
public class MissingFieldException extends GroovyRuntimeException {

    private final String field;
    private final Class type;

    public MissingFieldException(String field, Class type) {
        super("No such field: " + field + " for class: " + type.getName());
        this.field = field;
        this.type = type;
    }

    public MissingFieldException(String field, Class type, Throwable e) {
        super("No such field: " + field + " for class: " + type.getName() + ". Reason: " + e, e);
        this.field = field;
        this.type = type;
    }

    public MissingFieldException(String message, String field, Class type) {
        super(message);
        this.field = field;
        this.type = type;
    }

    /**
     * @return the name of the field that could not be found
     */
    public String getField() {
        return field;
    }

    /**
     * @return The type on which the field was attempted to be called
     */
    public Class getType() {
        return type;
    }
}
