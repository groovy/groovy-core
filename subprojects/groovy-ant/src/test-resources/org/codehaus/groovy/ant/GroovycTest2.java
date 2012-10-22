/*
 * Copyright 2003-2012 the original author or authors.
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
package org.codehaus.groovy.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class GroovycTest2 {
    static void main(String[] args) throws IOException {
        File f = new File("target/classes/test/org/codehaus/groovy/ant/GroovycTest2_Result.txt");
        FileOutputStream fout = new FileOutputStream(f);
        try {
            fout.write("OK.".getBytes());
        } finally {
            try {
                fout.close();
            } catch (IOException ioe) {
            }
        }
    }
}
