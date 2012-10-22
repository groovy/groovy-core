/*
 * Copyright 2008-2012 the original author or authors.
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

package groovy.beans

/**
 * @author Danno Ferrin (shemnon)
 */
class VetoableSwingTest extends GroovySwingTestCase {
    public void testExtendsComponent() {
        testInEDT {
            GroovyShell shell = new GroovyShell()
            shell.evaluate("""
                import groovy.beans.Vetoable

                class VetoableTestBean7 extends javax.swing.JPanel {
                    @Vetoable String testField
                }

                sb = new VetoableTestBean7()
                sb.testField = "bar"
                changed = false
                sb.vetoableChange = {changed = true}
                sb.testField = "foo"
                assert changed
            """)
        }
    }
}