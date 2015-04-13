/*
 * Copyright 2003-2014 the original author or authors.
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

package org.codehaus.groovy.macro.matcher

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.macro.matcher.internal.AnyTokenMatch
import org.codehaus.groovy.macro.matcher.internal.ConstraintPredicate
import org.codehaus.groovy.syntax.Token

/**
 * Represents constraints in AST pattern matching.
 *
 * @author Cedric Champeau
 * @since 2.5.0
 */
@CompileStatic
@Immutable(knownImmutableClasses = [ConstraintPredicate])
class MatchingConstraints {
    public final static ConstraintPredicate<Token> ANY_TOKEN = AnyTokenMatch.INSTANCE

    final Set<String> placeholders
    final ConstraintPredicate<Token> tokenPredicate
    final ConstraintPredicate<TreeContext> eventually

}
