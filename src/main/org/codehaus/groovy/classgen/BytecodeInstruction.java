/*
 * Copyright 2003-2007 the original author or authors.
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

package org.codehaus.groovy.classgen;

import org.objectweb.asm.MethodVisitor;

import java.io.Serializable;

/**
 * Helper class used by the class generator. Usually
 * an inner class is produced, that contains bytecode
 * creation code in the visit method.
 *
 * @author Jochen Theodorou
 */
public abstract class BytecodeInstruction implements Serializable {
    public abstract void visit(MethodVisitor mv);
}
