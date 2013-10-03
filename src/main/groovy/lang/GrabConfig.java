/*
 * Copyright 2003-2013 the original author or authors.
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
package groovy.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to modify the grape configuration for grab requests.
 * <p>
 * An example involving databases:
 * <pre>
 * {@code @Grab}('mysql:mysql-connector-java:5.1.6'),
 * {@code @GrabConfig}(systemClassLoader=true)
 * import groovy.sql.Sql
 *
 * def sql=Sql.newInstance("jdbc:mysql://localhost/test", "user", "password", "com.mysql.jdbc.Driver")
 * println sql.firstRow('SELECT * FROM INFORMATION_SCHEMA.COLUMNS')
 * </pre>
 * Another example involving XStream:
 * <pre>
 * {@code @Grapes}([
 *     {@code @Grab}('com.thoughtworks.xstream:xstream:1.3.1'),
 *     {@code @Grab}('xpp3:xpp3_min:1.1.4c'),
 *     {@code @GrabConfig}(systemClassLoader=true, initContextClassLoader=true)
 * ])
 * import com.thoughtworks.xstream.*
 *
 * class Staff {
 *     String firstname, lastname, position
 * }
 *
 * def xstream = new XStream()
 * def john1 = new Staff(firstname:'John',
 *                      lastname:'Connor',
 *                      position:'Resistance Leader')
 *
 * // write out to XML file
 * new File("john.xml").withOutputStream { out ->
 *     xstream.toXML(john1, out)
 * }
 *
 * // now read back in
 * def john2
 * new File("john.xml").withInputStream { ins ->
 *     john2 = xstream.fromXML(ins)
 * }
 *
 * println john2.dump()
 * </pre>
 * <p>
 * Further information about customising grape behavior can be found on the Grape documentation page:
 * <a href="http://groovy.codehaus.org/Grape">http://groovy.codehaus.org/Grape</a>.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.TYPE})
public @interface GrabConfig {
    /**
     * Set to true if you want to use the system classloader when loading the grape.
     * This is normally only required when a core Java class needs to reference the grabbed
     * classes, e.g. for a database driver accessed using DriverManager.
     */
    boolean systemClassLoader() default false;

    /**
     * Set to true if you want the context classloader to be initialised to the classloader
     * of the current class or script. This is useful for libraries or frameworks that assume
     * that the context classloader has been set. But be careful when using this flag as your
     * script or class might behave differently when called directly (from the command line or
     * from an IDE) versus when called from within a container, e.g. a web container or a JEE container.
     */
    boolean initContextClassLoader() default false;

    /**
     * Set to false if you want to disable automatic downloading of locally missing jars.
     */
    boolean autoDownload() default true;

    /**
     * Set to true if you want to disable checksum checking.
     */
    boolean disableChecksums() default false;
}