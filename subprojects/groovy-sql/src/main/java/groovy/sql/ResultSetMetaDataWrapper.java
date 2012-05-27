/*
 * Copyright 2003-2009 the original author or authors.
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

package groovy.sql;

import java.sql.ResultSetMetaData;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import groovy.lang.ReadOnlyPropertyException;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * <p>This class defines a wrapper for accessing a specific column in <code>ResultSetMetaData</code>.
 * This allows iteration over columns using idiomatic Groovy, e.g.
 * <pre>
 * meta.each {col ->
 *   println col.columnName
 * }
 * </pre>
 * All <code>ResultSetMetaData</code> column methods taking a column index
 * are available on a column either as a no-arg getter or via a property.
 * </p>
 * <p>This wrapper is created by an iterator invoked for <code>ResultSetMetaData</code>.
 *
 * @author John Hurst
 * @see org.codehaus.groovy.runtime.SqlGroovyMethods
 */
public class ResultSetMetaDataWrapper extends GroovyObjectSupport {

    private ResultSetMetaData target;
    private int index;

    public ResultSetMetaDataWrapper(ResultSetMetaData target, int index) {
        this.target = target;
        this.index = index;
    }

    private Object[] getIndexedArgs(Object[] originalArgs) {
        Object[] result = new Object[originalArgs.length + 1];
        result[0] = index;
        for (int i = 0, originalArgsLength = originalArgs.length; i < originalArgsLength; i++) {
            result[i + 1] = originalArgs[i];
        }
        return result;
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        Object[] indexedArgs = getIndexedArgs((Object[]) args);
        return InvokerHelper.invokeMethod(target, name, indexedArgs);
    }

    private String getPropertyGetterName(String prop) {
        if (prop == null || prop.length() < 1) {
            throw new MissingPropertyException(prop, target.getClass());
        }
        return "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
    }

    @Override
    public Object getProperty(String property) {
        return invokeMethod(getPropertyGetterName(property), new Object[0]);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        throw new ReadOnlyPropertyException(property, target.getClass());
    }
}
