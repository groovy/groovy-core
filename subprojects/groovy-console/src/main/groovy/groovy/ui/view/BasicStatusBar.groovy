/*
 * Copyright 2003-2010 the original author or authors.
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
package groovy.ui.view

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import javax.swing.SwingConstants

statusPanel = panel(constraints: BorderLayout.SOUTH) {
    gridBagLayout()
    separator(gridwidth:GridBagConstraints.REMAINDER, fill:GridBagConstraints.HORIZONTAL)
    status = label('Welcome to Groovy.',
        weightx:1.0,
        anchor:GridBagConstraints.WEST,
        fill:GridBagConstraints.HORIZONTAL,
        insets: [1,3,1,3])
    separator(orientation:SwingConstants.VERTICAL, fill:GridBagConstraints.VERTICAL)
    rowNumAndColNum = label('1:1', insets: [1,3,1,3])
}
