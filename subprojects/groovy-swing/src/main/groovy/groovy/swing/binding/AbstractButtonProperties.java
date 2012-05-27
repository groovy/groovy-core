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

import org.codehaus.groovy.binding.*;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:shemnon@yahoo.com">Danno Ferrin</a>
 * @version $Revision$
 * @since Groovy 1.1
 */
public class AbstractButtonProperties {
    public static Map<String, TriggerBinding> getSyntheticProperties() {
        Map<String, TriggerBinding> result = new HashMap<String, TriggerBinding>();
        result.put(AbstractButton.class.getName() + "#selected",
            new TriggerBinding() {
                public FullBinding createBinding(SourceBinding source, TargetBinding target) {
                    return new AbstractButtonSelectedBinding((PropertyBinding) source, target);
                }
            });
        return result;
    }
}


class AbstractButtonSelectedBinding extends AbstractSyntheticBinding implements PropertyChangeListener, ItemListener {
    AbstractButton boundButton;

    public AbstractButtonSelectedBinding(PropertyBinding source, TargetBinding target) {
        super(source, target, AbstractButton.class, "selected");
    }

    public synchronized void syntheticBind() {
            boundButton = (AbstractButton) ((PropertyBinding) sourceBinding).getBean();
                boundButton.addPropertyChangeListener("model", this);
                boundButton.getModel().addItemListener(this);
    }

    public synchronized void syntheticUnbind() {
            boundButton.removePropertyChangeListener("model", this);
            boundButton.getModel().removeItemListener(this);
            boundButton = null;
    }

    public void propertyChange(PropertyChangeEvent event) {
        update();
        ((ButtonModel)event.getOldValue()).removeItemListener(this);
        ((ButtonModel)event.getNewValue()).addItemListener(this);
    }

    public void itemStateChanged(ItemEvent e) {
        update();
    }
}
