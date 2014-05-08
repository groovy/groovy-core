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

package groovy.cli;

/**
 * @author paulk
 */
public class CliParserFactory {
    public static CliParser newParser() {
        return newParser("org.codehaus.groovy.cli.CommonsCliParserWrapper");
    }

    public static CliParser newParser(ClassLoader loader) {
        return newParser(loader, "org.codehaus.groovy.cli.CommonsCliParserWrapper");
    }

    public static CliParser newParser(String wrapperName) {
        String message = "Unable to load " + wrapperName + " or one of its dependent classes. Missing jar on classpath?";
        try {
            return (CliParser) Class.forName(wrapperName).newInstance();
        } catch (InstantiationException e) {
            throw new CliParseException(message, e);
        } catch (IllegalAccessException e) {
            throw new CliParseException(message, e);
        } catch (ClassNotFoundException e) {
            throw new CliParseException(message, e);
        }
    }

    public static CliParser newParser(ClassLoader loader, String wrapperName) {
        String message = "Unable to load " + wrapperName + " or one of its dependent classes. Missing jar on classpath?";
        try {
            return (CliParser) loader.loadClass(wrapperName).newInstance();
        } catch (InstantiationException e) {
            throw new CliParseException(message, e);
        } catch (IllegalAccessException e) {
            throw new CliParseException(message, e);
        } catch (ClassNotFoundException e) {
            throw new CliParseException(message, e);
        }
    }
}
