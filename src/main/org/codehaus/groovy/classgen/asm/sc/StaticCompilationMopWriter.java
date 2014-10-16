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
package org.codehaus.groovy.classgen.asm.sc;

import org.codehaus.groovy.classgen.asm.MopWriter;
import org.codehaus.groovy.classgen.asm.WriterController;

/**
 * A MOP Writer that skips the generation of MOP methods. This writer is used
 * when a class is *fully* statically compiled. In mixed mode, MOP methods are
 * still generated.
 *
 * @author Cédric Champeau
 * @since 2.4.0
 */
public class StaticCompilationMopWriter extends MopWriter {

    public static final MopWriter.Factory FACTORY = new MopWriter.Factory() {
        @Override
        public MopWriter create(final WriterController controller) {
            return new StaticCompilationMopWriter(controller);
        }
    };

    private final StaticTypesWriterController controller;

    public StaticCompilationMopWriter(final WriterController wc) {
        super(wc);
        this.controller = (StaticTypesWriterController) wc;
    }


    public void createMopMethods() {

    }

}
