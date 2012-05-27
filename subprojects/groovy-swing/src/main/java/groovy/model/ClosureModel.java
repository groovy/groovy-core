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

package groovy.model;

import groovy.lang.Closure;

/**
 * Represents a value model using a closure to extract
 * the value from some source model and an optional write closure
 * for updating the value.
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class ClosureModel implements ValueModel, NestedValueModel {

    private final ValueModel sourceModel;
    private final Closure readClosure;
    private final Closure writeClosure;
    private final Class type;

    public ClosureModel(ValueModel sourceModel, Closure readClosure) {
        this(sourceModel, readClosure, null);
    }

    public ClosureModel(ValueModel sourceModel, Closure readClosure, Closure writeClosure) {
        this(sourceModel, readClosure, writeClosure, Object.class);
    }

    public ClosureModel(ValueModel sourceModel, Closure readClosure, Closure writeClosure, Class type) {
        this.sourceModel = sourceModel;
        this.readClosure = readClosure;
        this.writeClosure = writeClosure;
        this.type = type;
    }

    public ValueModel getSourceModel() {
        return sourceModel;
    }

    public Object getValue() {
        Object source = sourceModel.getValue();
        if (source != null) {
            return readClosure.call(source);
        }
        return null;
    }

    public void setValue(Object value) {
        if (writeClosure != null) {
            Object source = sourceModel.getValue();
            if (source != null) {
                writeClosure.call(new Object[] { source, value });
            }
        }
    }

    public Class getType() {
        return type;
    }

    public boolean isEditable() {
        return writeClosure != null;
    }
}
