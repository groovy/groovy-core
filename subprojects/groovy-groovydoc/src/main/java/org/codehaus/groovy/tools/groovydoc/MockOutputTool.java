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
package org.codehaus.groovy.tools.groovydoc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MockOutputTool implements OutputTool {
    Set outputAreas; // dirs
    Map output;
    
    public MockOutputTool() {
        outputAreas = new HashSet();
        output = new HashMap();
    }
    
    public void makeOutputArea(String filename) {
        outputAreas.add(filename);
    }

    public void writeToOutput(String fileName, String text) throws Exception {
        output.put(fileName, text);
    }
    
    public boolean isValidOutputArea(String fileName) {
        return outputAreas.contains(fileName);
    }

    public String getText(String fileName) {
        return (String) output.get(fileName);
    }
    
    public String toString() {
        return "dirs:" + outputAreas + ", files:" + output.keySet();
    }
}
