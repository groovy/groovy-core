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

package org.codehaus.groovy.ant;

import groovy.lang.GroovyClassLoader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.tools.ErrorReporter;

import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Support for compilation related tasks.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public abstract class CompileTaskSupport
    extends MatchingTask
{
    protected final LoggingHelper log = new LoggingHelper(this);

    protected Path src;

    protected File destdir;

    protected Path classpath;

    protected CompilerConfiguration config = new CompilerConfiguration();

    protected boolean failOnError = true;

    public void setFailonerror(final boolean fail) {
        failOnError = fail;
    }

    public boolean getFailonerror() {
        return failOnError;
    }

    public Path createSrc() {
        if (src == null) {
            src = new Path(getProject());
        }
        return src.createPath();
    }

    public void setSrcdir(final Path dir) {
        assert dir != null;

        if (src == null) {
            src = dir;
        }
        else {
            src.append(dir);
        }
    }

    public Path getSrcdir() {
        return src;
    }

    public void setDestdir(final File dir) {
        assert dir != null;

        this.destdir = dir;
    }

    public void setClasspath(final Path path) {
        assert path != null;

        if (classpath == null) {
            classpath = path;
        }
        else {
            classpath.append(path);
        }
    }

    public Path getClasspath() {
        return classpath;
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }

        return classpath.createPath();
    }

    public void setClasspathRef(final Reference r) {
        assert r != null;
        
        createClasspath().setRefid(r);
    }

    public CompilerConfiguration createConfiguration() {
        return config;
    }
    
    protected void validate() throws BuildException {
        if (src == null) {
            throw new BuildException("Missing attribute: srcdir (or one or more nested <src> elements).", getLocation());
        }

        if (destdir == null) {
            throw new BuildException("Missing attribute: destdir", getLocation());
        }

        if (!destdir.exists()) {
            throw new BuildException("Destination directory does not exist: " + destdir, getLocation());
        }
    }

    protected GroovyClassLoader createClassLoader() {
        ClassLoader parent = ClassLoader.getSystemClassLoader();
        GroovyClassLoader gcl = new GroovyClassLoader(parent, config);

        Path path = getClasspath();
        if (path != null) {
            final String[] filePaths = path.list();
            for (int i = 0; i < filePaths.length; i++) {
                String filePath = filePaths[i];
                gcl.addClasspath(filePath);
            }
        }

        return gcl;
    }

    protected void handleException(final Exception e) throws BuildException {
        assert e != null;
        
        StringWriter writer = new StringWriter();
        new ErrorReporter(e, false).write(new PrintWriter(writer));
        String message = writer.toString();

        if (failOnError) {
            throw new BuildException(message, e, getLocation());
        }
        else {
            log.error(message);
        }
    }

    public void execute() throws BuildException {
        validate();

        try {
            compile();
        }
        catch (Exception e) {
            handleException(e);
        }
    }

    protected abstract void compile() throws Exception;
}