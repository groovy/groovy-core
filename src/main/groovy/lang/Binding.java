/*
 * Copyright 2003-2008 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the variable bindings of a script which can be altered
 * from outside the script object or created outside of a script and passed
 * into it.
 * <p> Binding instances are not supposed to be used in a multithreaded context.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class Binding extends GroovyObjectSupport {

    private Map variables;

    private final boolean returnNullForMissingVariables;

    public Binding() {
        this(false);
    }

    /**
     * @param returnNullForMissingVariables whether to return an empty
     * <code>String</code> for missing script variables rather than throw a
     * {@link MissingPropertyException} (useful for templates)
     */
    public Binding(boolean returnNullForMissingVariables) {
        this(null, returnNullForMissingVariables);
    }

    /**
     * @param variables the initial binding variables
     */
    public Binding(Map variables) {
        this(variables, false);
    }

    /**
     * @param variables the initial binding variables
     * @param returnNullForMissingVariables whether to return an empty
     * <code>String</code> for missing script variables rather than throw a
     * {@link MissingPropertyException} (useful for templates)
     */
    public Binding(Map variables, boolean returnNullForMissingVariables) {
        this.variables = variables;
        this.returnNullForMissingVariables = returnNullForMissingVariables;
    }

    /**
     * A helper constructor used in main(String[]) method calls
     *
     * @param args are the command line arguments from a main()
     */
    public Binding(String[] args) {
        this();
        setVariable("args", args);
    }

    /**
     * Gets the binding variable.
     *
     * <p>If the variable is not present a
     * <code>MissingPropertyException</code> is thrown unless the binding was
     * constructed with <code>returnNullForMissingVariables == true</code>,
     * in which case an empty <code>String</code> is returned.
     *
     * @param name the name of the variable to lookup
     * @return the variable value
     * @throws MissingPropertyException if the variable is not found and this
     * binding was constructed with
     * <code>returnNullForMissingVariables == false</code>
     */
    public Object getVariable(String name) throws MissingPropertyException {

        if (variables != null && variables.containsKey(name)) {

            return variables.get(name);

        } else {

            if (returnNullForMissingVariables) {
                return null;
            } else {
                throw new MissingPropertyException(name, this.getClass());
            }
        }
    }

    /**
     * Sets the value of the given variable
     *
     * @param name  the name of the variable to set
     * @param value the new value for the given variable
     */
    public void setVariable(String name, Object value) {
        if (variables == null)
            variables = new LinkedHashMap();
        variables.put(name, value);
    }

    public Map getVariables() {
        if (variables == null)
            variables = new LinkedHashMap();
        return variables;
    }

    /**
     * Overloaded to make variables appear as bean properties or via the subscript operator
     */
    public Object getProperty(String property) {
        /** @todo we should check if we have the property with the metaClass instead of try/catch  */
        try {
            return super.getProperty(property);
        }
        catch (MissingPropertyException e) {
            return getVariable(property);
        }
    }

    /**
     * Overloaded to make variables appear as bean properties or via the subscript operator
     */
    public void setProperty(String property, Object newValue) {
        /** @todo we should check if we have the property with the metaClass instead of try/catch  */
        try {
            super.setProperty(property, newValue);
        }
        catch (MissingPropertyException e) {
            setVariable(property, newValue);
        }
    }
}
