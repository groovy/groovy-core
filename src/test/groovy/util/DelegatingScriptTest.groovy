/*
 * Copyright 2003-2011 the original author or authors.
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
package groovy.util

import org.codehaus.groovy.control.CompilerConfiguration

public class DelegatingScriptTest extends GroovyTestCase {
    public void testDelegatingScript() throws Exception {
        def cc = new CompilerConfiguration();
        cc.scriptBase = DelegatingScript.class;
        def sh = new GroovyShell(new Binding(), cc);
        def script = (DelegatingScript)sh.parse("""
            println DelegatingScript.class
            foo(3,2){ a,b -> a*b };
            bar='test';
            assert 'testsetget'==bar
        """)
        def dsl = new MyDSL()
        script.setDelegate(dsl);
        script.run();
        assert dsl.foo==6;
        assert dsl.innerBar()=='testset';
    }
}

class MyDSL {
    protected int foo;
    protected String bar;

    public void foo(int x, int y, Closure z) { foo = z(x, y); }
    public void setBar(String a) {
        this.bar = a+"set";
    }
    public String getBar() {
        return this.bar+"get";
    }

    String innerBar() {
        return this.bar;
    }
}
