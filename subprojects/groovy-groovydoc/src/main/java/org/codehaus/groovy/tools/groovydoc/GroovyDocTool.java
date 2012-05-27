/*
 * Copyright 2007-2012 the original author or authors.
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

import antlr.RecognitionException;
import antlr.TokenStreamException;
import org.codehaus.groovy.groovydoc.GroovyRootDoc;
import org.codehaus.groovy.tools.shell.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Jeremy Rayner
 */
public class GroovyDocTool {
    private final Logger log = Logger.create(GroovyDocTool.class);
    private final GroovyRootDocBuilder rootDocBuilder;
    private final GroovyDocTemplateEngine templateEngine;
    private Properties properties;

    /**
     * Constructor for use by people who only want to interact with the Groovy Doclet Tree (rootDoc)
     *
     * @param sourcepaths where the sources to be added can be found
     */
    public GroovyDocTool(String[] sourcepaths) {
        this(null, sourcepaths, null);
    }

    public GroovyDocTool(ResourceManager resourceManager, String[] sourcepaths, String classTemplate) {
        this(resourceManager, sourcepaths, new String[]{}, new String[]{}, new String[]{classTemplate}, new ArrayList<LinkArgument>(), new Properties());
    }

    public GroovyDocTool(ResourceManager resourceManager, String[] sourcepaths, String[] docTemplates, String[] packageTemplates, String[] classTemplates, List<LinkArgument> links, Properties properties) {
        rootDocBuilder = new GroovyRootDocBuilder(this, sourcepaths, links, properties);
        String charset = properties.getProperty("charset");
        properties.setProperty("charset", charset != null && charset.length() != 0 ? charset : Charset.defaultCharset().name());
        this.properties = properties;


        if (resourceManager == null) {
            templateEngine = null;
        } else {
            templateEngine = new GroovyDocTemplateEngine(this, resourceManager, docTemplates, packageTemplates, classTemplates, properties);
        }
    }

    public void add(List<String> filenames) throws RecognitionException, TokenStreamException, IOException {
        if (templateEngine != null) {
            // only print out if we are being used for template generation
            log.debug("Loading source files for " + filenames);
        }
        rootDocBuilder.buildTree(filenames);
    }

    public GroovyRootDoc getRootDoc() {
        return rootDocBuilder.getRootDoc();
    }

    public void renderToOutput(OutputTool output, String destdir) throws Exception {
        // expect just one scope to be set on the way in but now also set higher levels of visibility
        if ("true".equals(properties.getProperty("privateScope"))) properties.setProperty("packageScope", "true");
        if ("true".equals(properties.getProperty("packageScope"))) properties.setProperty("protectedScope", "true");
        if ("true".equals(properties.getProperty("protectedScope"))) properties.setProperty("publicScope", "true");
        if (templateEngine != null) {
            GroovyDocWriter writer = new GroovyDocWriter(this, output, templateEngine, properties);
            GroovyRootDoc rootDoc = rootDocBuilder.getRootDoc();
            writer.writeRoot(rootDoc, destdir);
            writer.writePackages(rootDoc, destdir);
            writer.writeClasses(rootDoc, destdir);
        } else {
            throw new UnsupportedOperationException("No template engine was found");
        }
    }

    String getPath(String filename) {
        String path = new File(filename).getParent();
        // path length of 1 indicates that probably is 'default package' i.e. "/"
        if (path == null || path.length() == 1) {
            path = "DefaultPackage"; // "DefaultPackage" for 'default package' path, rather than null...
        }
        return path;
    }

    String getFile(String filename) {
        return new File(filename).getName();
    }

}
