/*
 * Copyright 2003-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package typing;

// tag::precompiled_java_extension[]
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension;


import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;

public class PrecompiledJavaExtension extends AbstractTypeCheckingExtension {                   // <1>

    public PrecompiledJavaExtension(final StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
    }

    @Override
    public boolean handleUnresolvedVariableExpression(final VariableExpression vexp) {          // <2>
        if ("robot".equals(vexp.getName())) {
            storeType(vexp, ClassHelper.make(Robot.class));
            setHandled(true);
            return true;
        }
        return false;
    }

}
// end::precompiled_java_extension[]