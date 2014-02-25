/*
 * Copyright 2007-2008 the original author or authors.
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
package groovy.swing.binding;

import org.codehaus.groovy.binding.AbstractFullBinding;
import org.codehaus.groovy.binding.PropertyBinding;
import org.codehaus.groovy.binding.SourceBinding;
import org.codehaus.groovy.binding.TargetBinding;

/**
 * Created by IntelliJ IDEA.
 * User: Danno.Ferrin
 * Date: Jun 18, 2008
 * Time: 1:41:14 PM
 */
public abstract class AbstractSyntheticBinding extends AbstractFullBinding {
    boolean bound;
    String propertyName;
    Class klass;

    public AbstractSyntheticBinding(PropertyBinding source, TargetBinding target, Class klass, String propertyName) {
        this.propertyName = propertyName;
        this.klass = klass;
        bound = false;
        setSourceBinding(source);
        setTargetBinding(target);
    }

    public void bind() {
        if (!bound) {
            try {
                syntheticBind();
                bound = true;
            } catch (RuntimeException re) {
                try {
                    syntheticUnbind();
                } catch (Exception e) {
                    // ignore as we are re-throwing the original cause
                }
                throw re;
            }
        }
    }

    public void unbind() {
        if (bound) {
            // fail dirty, no checks
            bound = false;
            syntheticUnbind();
        }
    }

    protected abstract void syntheticBind();
    protected abstract void syntheticUnbind();

    public void rebind() {
        if (bound) {
            unbind();
            bind();
        }
    }

    public void setSourceBinding(SourceBinding source) {
        if (!(source instanceof PropertyBinding)) {
            throw new IllegalArgumentException("Only PropertySourceBindings are accepted");
        }

        if (!propertyName.equals(((PropertyBinding) source).getPropertyName())) {
            throw new IllegalArgumentException("PropertyName must be '" + propertyName + "'");
        }
        Object bean = ((PropertyBinding) source).getBean();
        if ((bean == null) || !klass.isAssignableFrom(bean.getClass())) {
            throw new IllegalArgumentException("SourceBean must be a " + klass.getName());
        }
        super.setSourceBinding(source);
    }

    public void setTargetBinding(TargetBinding target) {
        super.setTargetBinding(target);
    }
}
