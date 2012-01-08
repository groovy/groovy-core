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

package org.codehaus.groovy.control;

import groovy.lang.GroovyRuntimeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Looks for source file extensions in META-INF/services/org.codehaus.groovy.source.Extensions
 */

public class SourceExtensionHandler {

    public static Set<String> getRegisteredExtensions(ClassLoader loader) {
        Set<String> extensions = new LinkedHashSet<String>();
        extensions.add("groovy");
        URL service = null;
        try {
            Enumeration<URL> globalServices = loader.getResources("META-INF/services/org.codehaus.groovy.source.Extensions");
            while (globalServices.hasMoreElements()) {
                service = globalServices.nextElement();
                String extension;
                BufferedReader svcIn = new BufferedReader(new InputStreamReader(service.openStream()));

                extension = svcIn.readLine();
                while (extension != null) {
                    extension = extension.trim();
                    if (!extension.startsWith("#") && extension.length() > 0) {
                        extensions.add(extension);
                    }
                    extension = svcIn.readLine();
                }
            }
        } catch (IOException ex) {
            throw new GroovyRuntimeException("IO Exception attempting to load source extension registerers. Exception: " +
                    ex.toString() + (service == null ? "" : service.toExternalForm()));
        }
        return extensions;
    }
}
