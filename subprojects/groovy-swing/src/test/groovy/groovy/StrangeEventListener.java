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

import java.util.EventListener;
import java.beans.PropertyChangeEvent;

public interface StrangeEventListener extends EventListener {

    /*
     * According to section 6.4.1 of the JavaBeans spec this is legal, but not
     * good practice.  We need to test what can be done not what should be done
     */
    void somethingStrangeHappened(String what, String where);
    
    void somethingChanged(PropertyChangeEvent changeEvent);
    
}
