/*
 * Copyright 2003-2012 the original author or authors.
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

package org.codehaus.groovy.transform.stc;

import org.codehaus.groovy.ast.ClassNode;

/**
 * Delegation metadata is used to store the delegation strategy and delegate type of
 * closures.
 *
 * As closures can be organized in a hierachy, a delegation metadata may have a parent.
 *
 * @author Cedric Champeau
 */
class DelegationMetadata {
    private final DelegationMetadata parent;
    private final ClassNode type;
    private final int strategy;

    public DelegationMetadata(final ClassNode type, final int strategy, final DelegationMetadata parent) {
        this.strategy = strategy;
        this.type = type;
        this.parent = parent;
    }

    public DelegationMetadata(final ClassNode type, final int strategy) {
        this(type, strategy, null);
    }

    public int getStrategy() {
        return strategy;
    }

    public ClassNode getType() {
        return type;
    }

    public DelegationMetadata getParent() {
        return parent;
    }
}
