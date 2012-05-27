/*
 * Copyright 2007 the original author or authors.
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
package org.codehaus.groovy.binding;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * @author <a href="mailto:shemnon@yahoo.com">Danno Ferrin</a>
 * @version $Revision$
 * @since Groovy 1.1
 */
public class EventTriggerBinding implements TriggerBinding {

    Object triggerBean;
    String eventName;

    public EventTriggerBinding(Object triggerBean, String eventName) {
        this.triggerBean = triggerBean;
        this.eventName = eventName;
    }

    public FullBinding createBinding(final SourceBinding sourceBinding, final TargetBinding targetBinding) {
        return new EventTriggerFullBinding(sourceBinding, targetBinding);
    }

    public Object getTriggerBean() {
        return triggerBean;
    }

    public void setTriggerBean(Object triggerBean) {
        this.triggerBean = triggerBean;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    private class EventTriggerFullBinding extends AbstractFullBinding {

        Closure handler;

        public EventTriggerFullBinding(final SourceBinding sourceBinding, TargetBinding targetBinding) {
            setSourceBinding(sourceBinding);
            setTargetBinding(targetBinding);
            handler = new Closure(triggerBean) {
                public Object call(Object... params) {
                    if (sourceBinding instanceof ClosureSourceBinding) {
                        ((ClosureSourceBinding)sourceBinding).setClosureArguments(params);
                    }
                    update();
                    return null;
                }
            };
        }

        public void bind() {
            InvokerHelper.setProperty(triggerBean, eventName, handler);
        }

        public void unbind() {
            throw new UnsupportedOperationException();
        }

        public void rebind() {
            throw new UnsupportedOperationException();
        }
    }
}