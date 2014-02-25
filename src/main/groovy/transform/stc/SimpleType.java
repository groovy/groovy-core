/*
 * Copyright 2003-2013 the original author or authors.
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
package groovy.transform.stc;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;

public class SimpleType extends SingleSignatureClosureHint {
    @Override
    public ClassNode[] getParameterTypes(final MethodNode node, final String[] options, final SourceUnit sourceUnit, final CompilationUnit unit, final ASTNode usage) {
        ClassNode[] result = new ClassNode[options.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = findClassNode(sourceUnit, unit, options[i]);
        }
        return result;
    }
 }
