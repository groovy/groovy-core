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
package groovy.servlet;

import groovy.lang.Closure;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.runtime.GroovyCategorySupport;

/**
 * This servlet will run Groovy scripts as Groovlets.  Groovlets are scripts
 * with these objects implicit in their scope:
 *
 * <ul>
 *  <li>request - the HttpServletRequest</li>
 *  <li>response - the HttpServletResponse</li>
 *  <li>application - the ServletContext associated with the servlet</li>
 *  <li>session - the HttpSession associated with the HttpServletRequest</li>
 *  <li>out - the PrintWriter associated with the ServletRequest</li>
 * </ul>
 *
 * <p>Your script sources can be placed either in your web application's normal
 * web root (allows for subdirectories) or in /WEB-INF/groovy/* (also allows
 * subdirectories).
 *
 * <p>To make your web application more groovy, you must add the GroovyServlet
 * to your application's web.xml configuration using any mapping you like, so
 * long as it follows the pattern *.* (more on this below).  Here is the
 * web.xml entry:
 *
 * <pre>
 *    &lt;servlet>
 *      &lt;servlet-name>Groovy&lt;/servlet-name>
 *      &lt;servlet-class>groovy.servlet.GroovyServlet&lt;/servlet-class>
 *    &lt;/servlet>
 *
 *    &lt;servlet-mapping>
 *      &lt;servlet-name>Groovy&lt;/servlet-name>
 *      &lt;url-pattern>*.groovy&lt;/url-pattern>
 *      &lt;url-pattern>*.gdo&lt;/url-pattern>
 *    &lt;/servlet-mapping>
 * </pre>
 *
 * <p>The URL pattern does not require the "*.groovy" mapping.  You can, for
 * example, make it more Struts-like but groovy by making your mapping "*.gdo".
 *
 * @author Sam Pullara
 * @author Mark Turansky (markturansky at hotmail.com)
 * @author Guillaume Laforge
 * @author Christian Stein
 * @author Marcel Overdijk
 *
 * @see groovy.servlet.ServletBinding
 */
public class GroovyServlet extends AbstractHttpServlet {

    /**
     * The script engine executing the Groovy scripts for this servlet
     */
    private GroovyScriptEngine gse;

    /**
     * Initialize the GroovyServlet.
     *
     * @throws ServletException
     *  if this method encountered difficulties
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Set up the scripting engine
        gse = createGroovyScriptEngine();

        servletContext.log("Groovy servlet initialized on " + gse + ".");
    }

    /**
     * Handle web requests to the GroovyServlet
     */
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Get the script path from the request - include aware (GROOVY-815)
        final String scriptUri = getScriptUri(request);

        // Set it to HTML by default
        response.setContentType("text/html; charset="+encoding);

        // Set up the script context
        final ServletBinding binding = new ServletBinding(request, response, servletContext);
        setVariables(binding);

        // Run the script
        try {
            Closure closure = new Closure(gse) {

                public Object call() {
                    try {
                        return ((GroovyScriptEngine) getDelegate()).run(scriptUri, binding);
                    } catch (ResourceException e) {
                        throw new RuntimeException(e);
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                }

            };
            GroovyCategorySupport.use(ServletCategory.class, closure);
        } catch (RuntimeException runtimeException) {
            StringBuffer error = new StringBuffer("GroovyServlet Error: ");
            error.append(" script: '");
            error.append(scriptUri);
            error.append("': ");
            Throwable e = runtimeException.getCause();
            /*
             * Null cause?!
             */
            if (e == null) {
                error.append(" Script processing failed.");
                error.append(runtimeException.getMessage());
                if (runtimeException.getStackTrace().length > 0)
                    error.append(runtimeException.getStackTrace()[0].toString());
                servletContext.log(error.toString());
                System.err.println(error.toString());
                runtimeException.printStackTrace(System.err);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.toString());
                return;
            }
            /*
             * Resource not found.
             */
            if (e instanceof ResourceException) {
                error.append(" Script not found, sending 404.");
                servletContext.log(error.toString());
                System.err.println(error.toString());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            /*
             * Other internal error. Perhaps syntax?!
             */
            servletContext.log("An error occurred processing the request", runtimeException);
            error.append(e.getMessage());
            if (e.getStackTrace().length > 0)
                error.append(e.getStackTrace()[0].toString());
            servletContext.log(e.toString());
            System.err.println(e.toString());
            runtimeException.printStackTrace(System.err);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    /**
     * Hook method to setup the GroovyScriptEngine to use.<br/>
     * Subclasses may override this method to provide a custom
     * engine.
     */
    protected GroovyScriptEngine createGroovyScriptEngine(){
        return new GroovyScriptEngine(this);
    }
}
