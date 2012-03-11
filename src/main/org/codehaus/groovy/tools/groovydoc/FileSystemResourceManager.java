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
package org.codehaus.groovy.tools.groovydoc;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

public class FileSystemResourceManager implements ResourceManager {
    private String basedir;
    private static final String FS = "/";

    public FileSystemResourceManager() {
        basedir = "";
    }

    public FileSystemResourceManager(String basedir) {
        this.basedir = basedir + FS;
    }

    public Reader getReader(String resourceName) throws IOException {
        return ResourceGroovyMethods.newReader(new File(basedir + resourceName));
    }
}
