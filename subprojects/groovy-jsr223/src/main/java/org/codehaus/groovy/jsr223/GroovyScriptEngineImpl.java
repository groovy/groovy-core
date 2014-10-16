/*
 * Original source;
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met: Redistributions of source code 
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of 
 * conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of 
 * is contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission. 

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Subsequent changes:
 * Copyright 2006-2012 the original author or authors.
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
package org.codehaus.groovy.jsr223;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.DelegatingMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MetaClass;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.lang.Tuple;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.util.ManagedConcurrentValueMap;
import org.codehaus.groovy.util.ReferenceBundle;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.runtime.MethodClosure;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import java.lang.Class;
import java.lang.String;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/*
 * @author Mike Grogan
 * @author A. Sundararajan
 * @author Jim White
 * @author Guillaume Laforge
 * @author Jochen Theodorou
 */
public class GroovyScriptEngineImpl
        extends AbstractScriptEngine implements Compilable, Invocable {

    private static boolean debug = false;

    // script-string-to-generated Class map
    private ManagedConcurrentValueMap<String, Class> classMap = new ManagedConcurrentValueMap<String, Class>(ReferenceBundle.getSoftBundle());
    // global closures map - this is used to simulate a single
    // global functions namespace 
    private ManagedConcurrentValueMap<String, Closure> globalClosures = new ManagedConcurrentValueMap<String, Closure>(ReferenceBundle.getHardBundle());
    // class loader for Groovy generated classes
    private GroovyClassLoader loader;
    // lazily initialized factory
    private volatile GroovyScriptEngineFactory factory;

    // counter used to generate unique global Script class names
    private static int counter;

    static {
        counter = 0;
    }

    public GroovyScriptEngineImpl() {
        this(new GroovyClassLoader(getParentLoader(), new CompilerConfiguration()));
    }

    public GroovyScriptEngineImpl(GroovyClassLoader classLoader) {
        if (classLoader == null) throw new IllegalArgumentException("GroovyClassLoader is null");
        this.loader = classLoader;
    }

    public Object eval(Reader reader, ScriptContext ctx)
            throws ScriptException {
        return eval(readFully(reader), ctx);
    }

    public Object eval(String script, ScriptContext ctx)
            throws ScriptException {
        try {
            String val = (String) ctx.getAttribute("#jsr223.groovy.engine.keep.globals", ScriptContext.ENGINE_SCOPE);
            ReferenceBundle bundle = ReferenceBundle.getHardBundle();
            if (val!=null && val.length()>0) {
                if (val.equalsIgnoreCase("soft")) {
                    bundle = ReferenceBundle.getSoftBundle();
                } else if (val.equalsIgnoreCase("weak")) {
                    bundle = ReferenceBundle.getWeakBundle();
                } else if (val.equalsIgnoreCase("phantom")) {
                    bundle = ReferenceBundle.getPhantomBundle();
                }
            }
            globalClosures.setBundle(bundle);
        } catch (ClassCastException cce) { /*ignore.*/ }

        try {
            Class clazz = getScriptClass(script);
            if (clazz == null) throw new ScriptException("Script class is null");
            return eval(clazz, ctx);
        } catch (SyntaxException e) {
            throw new ScriptException(e.getMessage(),
                    e.getSourceLocator(), e.getLine());
        } catch (Exception e) {
            if (debug) e.printStackTrace();
            throw new ScriptException(e);
        }
    }

    public Bindings createBindings() {
        return new SimpleBindings();
    }

    public ScriptEngineFactory getFactory() {
        if (factory == null) {
            synchronized (this) {
                if (factory == null) {
                    factory = new GroovyScriptEngineFactory();
                }
            }
        }
        return factory;
    }

    // javax.script.Compilable methods 
    public CompiledScript compile(String scriptSource) throws ScriptException {
        try {
            return new GroovyCompiledScript(this,
                    getScriptClass(scriptSource));
        } catch (SyntaxException e) {
            throw new ScriptException(e.getMessage(),
                    e.getSourceLocator(), e.getLine());
        } catch (IOException e) {
            throw new ScriptException(e);
        } catch (CompilationFailedException ee) {
            throw new ScriptException(ee);
        }
    }

    public CompiledScript compile(Reader reader) throws ScriptException {
        return compile(readFully(reader));
    }

    // javax.script.Invokable methods.
    public Object invokeFunction(String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        return invokeImpl(null, name, args);
    }

    public Object invokeMethod(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        if (thiz == null) {
            throw new IllegalArgumentException("script object is null");
        }
        return invokeImpl(thiz, name, args);
    }

    public <T> T getInterface(Class<T> clazz) {
        return makeInterface(null, clazz);
    }

    public <T> T getInterface(Object thiz, Class<T> clazz) {
        if (thiz == null) {
            throw new IllegalArgumentException("script object is null");
        }
        return makeInterface(thiz, clazz);
    }

    // package-privates
    Object eval(Class scriptClass, final ScriptContext ctx) throws ScriptException {
        // Bindings so script has access to this environment.
        // Only initialize once.
        if (null == ctx.getAttribute("context", ScriptContext.ENGINE_SCOPE)) {
            // add context to bindings
            ctx.setAttribute("context", ctx, ScriptContext.ENGINE_SCOPE);

            // direct output to ctx.getWriter
            // If we're wrapping with a PrintWriter here,
            // enable autoFlush because otherwise it might not get done!
            final Writer writer = ctx.getWriter();
            ctx.setAttribute("out", (writer instanceof PrintWriter) ?
                    writer :
                    new PrintWriter(writer, true),
                    ScriptContext.ENGINE_SCOPE);

// Not going to do this after all (at least for now).
// Scripts can use context.{reader, writer, errorWriter}.
// That is a modern version of System.{in, out, err} or Console.{reader, writer}().
//
//            // New I/O names consistent with ScriptContext and java.io.Console.
//
//            ctx.setAttribute("writer", writer, ScriptContext.ENGINE_SCOPE);
//
//            // Direct errors to ctx.getErrorWriter
//            final Writer errorWriter = ctx.getErrorWriter();
//            ctx.setAttribute("errorWriter", (errorWriter instanceof PrintWriter) ?
//                                    errorWriter :
//                                    new PrintWriter(errorWriter),
//                                    ScriptContext.ENGINE_SCOPE);
//
//            // Get input from ctx.getReader
//            // We don't wrap with BufferedReader here because we expect that if
//            // the host wants that they do it.  Either way Groovy scripts will
//            // always have readLine because the GDK supplies it for Reader.
//            ctx.setAttribute("reader", ctx.getReader(), ScriptContext.ENGINE_SCOPE);
        }

        // Fix for GROOVY-3669: Can't use several times the same JSR-223 ScriptContext for differents groovy script
        if (ctx.getWriter() != null) {
            ctx.setAttribute("out", new PrintWriter(ctx.getWriter(), true), ScriptContext.ENGINE_SCOPE);
        }

        /*
         * We use the following Binding instance so that global variable lookup
         * will be done in the current ScriptContext instance.
         */
        Binding binding = new Binding(ctx.getBindings(ScriptContext.ENGINE_SCOPE)) {
            @Override
            public Object getVariable(String name) {
                synchronized (ctx) {
                    int scope = ctx.getAttributesScope(name);
                    if (scope != -1) {
                        return ctx.getAttribute(name, scope);
                    }
                }
                throw new MissingPropertyException(name, getClass());
            }

            @Override
            public void setVariable(String name, Object value) {
                synchronized (ctx) {
                    int scope = ctx.getAttributesScope(name);
                    if (scope == -1) {
                        scope = ScriptContext.ENGINE_SCOPE;
                    }
                    ctx.setAttribute(name, value, scope);
                }
            }
        };

        try {
            // if this class is not an instance of Script, it's a full-blown class
            // then simply return that class
            if (!Script.class.isAssignableFrom(scriptClass)) {
                return scriptClass;
            } else {
                // it's a script
                Script scriptObject = InvokerHelper.createScript(scriptClass, binding);

                // save all current closures into global closures map
                Method[] methods = scriptClass.getMethods();
                for (Method m : methods) {
                    String name = m.getName();
                    globalClosures.put(name, new MethodClosure(scriptObject, name));
                }

                MetaClass oldMetaClass = scriptObject.getMetaClass();

                /*
                * We override the MetaClass of this script object so that we can
                * forward calls to global closures (of previous or future "eval" calls)
                * This gives the illusion of working on the same "global" scope.
                */
                scriptObject.setMetaClass(new DelegatingMetaClass(oldMetaClass) {
                    @Override
                    public Object invokeMethod(Object object, String name, Object args) {
                        if (args == null) {
                            return invokeMethod(object, name, MetaClassHelper.EMPTY_ARRAY);
                        }
                        if (args instanceof Tuple) {
                            return invokeMethod(object, name, ((Tuple) args).toArray());
                        }
                        if (args instanceof Object[]) {
                            return invokeMethod(object, name, (Object[]) args);
                        } else {
                            return invokeMethod(object, name, new Object[]{args});
                        }
                    }

                    @Override
                    public Object invokeMethod(Object object, String name, Object[] args) {
                        try {
                            return super.invokeMethod(object, name, args);
                        } catch (MissingMethodException mme) {
                            return callGlobal(name, args, ctx);
                        }
                    }

                    @Override
                    public Object invokeStaticMethod(Object object, String name, Object[] args) {
                        try {
                            return super.invokeStaticMethod(object, name, args);
                        } catch (MissingMethodException mme) {
                            return callGlobal(name, args, ctx);
                        }
                    }
                });

                return scriptObject.run();
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            // Fix for GROOVY-3669: Can't use several times the same JSR-223 ScriptContext for different groovy script
            // Groovy's scripting engine implementation adds those two variables in the binding
            // but should clean up afterwards
            ctx.removeAttribute("context", ScriptContext.ENGINE_SCOPE);
            ctx.removeAttribute("out", ScriptContext.ENGINE_SCOPE);
        }
    }

    Class getScriptClass(String script)
            throws SyntaxException,
            CompilationFailedException,
            IOException {
        Class clazz = classMap.get(script);
        if (clazz != null) {
            return clazz;
        }

        clazz = loader.parseClass(script, generateScriptName());
        classMap.put(script, clazz);
        return clazz;
    }

    public void setClassLoader(GroovyClassLoader classLoader) {
        this.loader = classLoader;
    }

    public GroovyClassLoader getClassLoader() {
        return this.loader;
    }

    //-- Internals only below this point

    // invokes the specified method/function on the given object.
    private Object invokeImpl(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        if (name == null) {
            throw new NullPointerException("method name is null");
        }

        try {
            if (thiz != null) {
                return InvokerHelper.invokeMethod(thiz, name, args);
            } else {
                return callGlobal(name, args);
            }
        } catch (MissingMethodException mme) {
            throw new NoSuchMethodException(mme.getMessage());
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    // call the script global function of the given name
    private Object callGlobal(String name, Object[] args) {
        return callGlobal(name, args, context);
    }

    private Object callGlobal(String name, Object[] args, ScriptContext ctx) {
        Closure closure = globalClosures.get(name);
        if (closure != null) {
            return closure.call(args);
        } else {
            // Look for closure valued variable in the 
            // given ScriptContext. If available, call it.
            Object value = ctx.getAttribute(name);
            if (value instanceof Closure) {
                return ((Closure) value).call(args);
            } // else fall thru..
        }
        throw new MissingMethodException(name, getClass(), args);
    }

    // generate a unique name for top-level Script classes
    private synchronized String generateScriptName() {
        return "Script" + (++counter) + ".groovy";
    }

    @SuppressWarnings("unchecked")
    private <T> T makeInterface(Object obj, Class<T> clazz) {
        final Object thiz = obj;
        if (clazz == null || !clazz.isInterface()) {
            throw new IllegalArgumentException("interface Class expected");
        }
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method m, Object[] args)
                            throws Throwable {
                        return invokeImpl(thiz, m.getName(), args);
                    }
                });
    }

    // determine appropriate class loader to serve as parent loader
    // for GroovyClassLoader instance
    private static ClassLoader getParentLoader() {
        // check whether thread context loader can "see" Groovy Script class
        ClassLoader ctxtLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class c = ctxtLoader.loadClass(Script.class.getName());
            if (c == Script.class) {
                return ctxtLoader;
            }
        } catch (ClassNotFoundException cnfe) {
            /* ignore */
        }
        // exception was thrown or we get wrong class
        return Script.class.getClassLoader();
    }

    private String readFully(Reader reader) throws ScriptException {
        char[] arr = new char[8 * 1024]; // 8K at a time
        StringBuilder buf = new StringBuilder();
        int numChars;
        try {
            while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
                buf.append(arr, 0, numChars);
            }
        } catch (IOException exp) {
            throw new ScriptException(exp);
        }
        return buf.toString();
    }
} 
