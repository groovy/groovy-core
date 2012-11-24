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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Helper to make logging from Ant easier.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class LoggingHelper {
    private Task owner;

    public LoggingHelper(final Task owner) {
        assert owner != null;
        this.owner = owner;
    }

    public void error(final String msg) {
        owner.log(msg, Project.MSG_ERR);
    }

    public void error(final String msg, final Throwable t) {
        owner.log(msg, t, Project.MSG_ERR);
    }

    public void warn(final String msg) {
        owner.log(msg, Project.MSG_WARN);
    }

    public void info(final String msg) {
        owner.log(msg, Project.MSG_INFO);
    }

    public void verbose(final String msg) {
        owner.log(msg, Project.MSG_VERBOSE);
    }

    public void debug(final String msg) {
        owner.log(msg, Project.MSG_DEBUG);
    }
}