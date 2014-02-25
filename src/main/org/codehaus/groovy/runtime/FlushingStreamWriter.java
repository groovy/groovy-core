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
package org.codehaus.groovy.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Stream writer which flushes after each write operation.
 *
 * @author Guillaume Laforge
 */
public class FlushingStreamWriter extends OutputStreamWriter {

    public FlushingStreamWriter(OutputStream out) {
        super(out);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        super.write(cbuf, off, len);
        flush();
    }

    public void write(int c) throws IOException {
        super.write(c);
        flush();
    }

    public void write(String str, int off, int len) throws IOException {
        super.write(str, off, len);
        flush();
    }
}
