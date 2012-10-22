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
package groovy;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This bean should encompass all legal, but bad, JavaBeans practices
 */
public class StrangeBean {

    Set<StrangeEventListener> strangeListeners;

    public StrangeBean() {
        strangeListeners = new LinkedHashSet<StrangeEventListener>();
    }

    public void addStrangeEventListener(StrangeEventListener listener) {
        strangeListeners.add(listener);
    }

    public void removeStrangeEventListener(StrangeEventListener listener) {
        strangeListeners.remove(listener);
    }

    public StrangeEventListener[] getStrangeEventListeners() {
        return strangeListeners.toArray(new StrangeEventListener[strangeListeners.size()]);
    }

    public void somethingStrangeHappened(String what, String where) {
        for (StrangeEventListener listener : strangeListeners) {
            listener.somethingStrangeHappened(what, where);
        }
    }

}
