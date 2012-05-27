/*
 * Copyright 2003-2010 the original author or authors.
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

package org.codehaus.groovy.vmplugin.v6;
// TODO M12N move this to groovy-jsr223

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * This class defines new Java 6 specific static groovy methods which extend the normal
 * JDK classes inside the Groovy environment.
 */
public class PluginStaticGroovyMethods extends DefaultGroovyMethodsSupport {
    /**
     * Provides a convenient shorthand for accessing a Scripting Engine with name <code>languageShortName</code>
     * using a newly created <code>ScriptEngineManager</code> instance.
     *
     * @param self              Placeholder variable used by Groovy categories; ignored for default static methods
     * @param languageShortName The short name of the scripting engine of interest
     * @return the ScriptEngine corresponding to the supplied short name or null if no engine was found
     * @since 1.8.0
     */
    public static ScriptEngine $static_propertyMissing(ScriptEngineManager self, String languageShortName) {
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByName(languageShortName);
    }

}
