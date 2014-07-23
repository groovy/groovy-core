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
package groovy.text.markup;

import groovy.lang.Closure;
import groovy.lang.Writable;
import groovy.text.Template;
import org.codehaus.groovy.control.io.NullWriter;
import org.codehaus.groovy.runtime.ExceptionUtils;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static groovy.xml.XmlUtil.escapeXml;

/**
 * <p>All templates compiled through {@link groovy.text.markup.MarkupTemplateEngine} extend this abstract class,
 * which provides a number of utility methods to generate markup. An instance of this class can be obtained
 * after calling {@link groovy.text.Template#make()} or {@link groovy.text.Template#make(java.util.Map)})} on
 * a template generated by {@link groovy.text.markup.MarkupTemplateEngine#createTemplate(java.io.Reader)}.</p>
 *
 * <p>It is advised to use a distinct template instance for each thread (or more simply, each rendered document)
 * for thread safety and avoiding mixing models.</p>
 *
 * <p>For the application needs, it is possible to provide more helper methods by extending this class and
 * configuring the base template class using the {@link groovy.text.markup.TemplateConfiguration#setBaseTemplateClass(Class)}
 * method.</p>
 *
 * @author Cedric Champeau
 */
public abstract class BaseTemplate implements Writable {
    private static final Map EMPTY_MODEL = Collections.emptyMap();

    private final Map model;
    private final Map<String,String> modelTypes;
    private final MarkupTemplateEngine engine;
    private final TemplateConfiguration configuration;
    private final Map<String, Template> cachedFragments;

    private Writer out;
    private boolean doWriteIndent;

    public BaseTemplate(final MarkupTemplateEngine templateEngine, final Map model, final Map<String,String> modelTypes, final TemplateConfiguration configuration) {
        this.model = model==null?EMPTY_MODEL:model;
        this.engine = templateEngine;
        this.configuration = configuration;
        this.modelTypes = modelTypes;
        this.cachedFragments = new LinkedHashMap<String, Template>();
    }

    public Map getModel() {
        return model;
    }

    public abstract Object run();

    /**
     * Renders the object provided as parameter using its {@link Object#toString()} method,
     * The contents is rendered as is, unescaped. This means that depending on what the
     * {@link Object#toString()} method call returns, you might create invalid markup.
     * @param obj the object to be rendered unescaped
     * @return this template instance
     * @throws IOException
     */
    public BaseTemplate yieldUnescaped(Object obj) throws IOException {
        writeIndent();
        out.write(obj.toString());
        return this;
    }

    /**
     * Renders the object provided as parameter using its {@link Object#toString()} method,
     * The contents is rendered after being escaped for XML, enforcing valid XML output.
     * @param obj the object to be rendered
     * @return this template instance
     * @throws IOException
     */
    public BaseTemplate yield(Object obj) throws IOException {
        writeIndent();
        out.write(escapeXml(obj.toString()));
        return this;
    }

    public String stringOf(Closure cl) throws IOException {
        Writer old = out;
        StringWriter stringWriter = new StringWriter(32);
        out = stringWriter;
        Object result = cl.call();
        if (result!=null && result!=this) {
            stringWriter.append(result.toString());
        }
        out = old;
        return stringWriter.toString();
    }


    /**
     * Renders the supplied object using its {@link Object#toString} method inside a
     * comment markup block (&lt;!-- ... --&gt;). The object is rendered as is, unescaped.
     * @param cs the object to be rendered inside an XML comment block.
     * @return this template instance.
     * @throws IOException
     */
    public BaseTemplate comment(Object cs) throws IOException {
        writeIndent();
        out.write("<!--");
        out.write(cs.toString());
        out.write("-->");
        return this;
    }

    /**
     * Renders an XML declaration header. If the declaration encoding is set in the
     * {@link TemplateConfiguration#getDeclarationEncoding() template configuration},
     * then the encoding is rendered into the declaration.
     * @return this template instance
     * @throws IOException
     */
    public BaseTemplate xmlDeclaration() throws IOException {
        out.write("<?xml ");
        writeAttribute("version", "1.0");
        if (configuration.getDeclarationEncoding() != null) {
            writeAttribute(" encoding", configuration.getDeclarationEncoding());
        }
        out.write("?>");
        out.write(configuration.getNewLineString());
        return this;
    }

    /**
     * <p>Renders processing instructions. The supplied map contains all elements to be
     * rendered as processing instructions. The key is the name of the element, the value
     * is either a map of attributes, or an object to be rendered directly. For example:</p>
     * <code>
     *     pi("xml-stylesheet":[href:"mystyle.css", type:"text/css"])
     * </code>
     *
     * <p>will be rendered as:</p>
     *
     * <pre>
     *     &lt;?xml-stylesheet href='mystyle.css' type='text/css'?&gt;
     * </pre>
     *
     * @param attrs the attributes to render
     * @return this template instance
     * @throws IOException
     */
    public BaseTemplate pi(Map<?, ?> attrs) throws IOException {
        for (Map.Entry<?, ?> entry : attrs.entrySet()) {
            Object target = entry.getKey();
            Object instruction = entry.getValue();
            out.write("<?");
            if (instruction instanceof Map) {
                out.write(target.toString());
                writeAttributes((Map) instruction);
            } else {
                out.write(target.toString());
                out.write(" ");
                out.write(instruction.toString());
            }
            out.write("?>");
            out.write(configuration.getNewLineString());
        }
        return this;
    }

    private void writeAttribute(String attName, String value) throws IOException {
        out.write(attName);
        out.write("=");
        writeQt();
        out.write(escapeQuotes(value));
        writeQt();
    }

    private void writeQt() throws IOException {
        if (configuration.isUseDoubleQuotes()) {
            out.write('"');
        } else {
            out.write('\'');
        }
    }

    private void writeIndent() throws IOException {
        if (out instanceof DelegatingIndentWriter && doWriteIndent) {
            ((DelegatingIndentWriter)out).writeIndent();
            doWriteIndent = false;
        }
    }

    private String escapeQuotes(String str) {
        String quote = configuration.isUseDoubleQuotes() ? "\"" : "'";
        String escape = configuration.isUseDoubleQuotes() ? "&quote;" : "&apos;";
        return str.replace(quote, escape);
    }

    /**
     * This is the main method responsible for writing a tag and its attributes.
     * The arguments may be:
     * <ul>
     *     <li>a closure</li> in which case the closure is rendered inside the tag body
     *     <li>a string</li>, in which case the string is rendered as the tag body
     *     <li>a map of attributes</li> in which case the attributes are rendered inside the opening tag
     * </ul>
     * <p>or a combination of (attributes,string), (attributes,closure)</p>
     * @param tagName the name of the tag
     * @param args tag generation arguments
     * @return this template instance
     * @throws IOException
     */
    public Object methodMissing(String tagName, Object args) throws IOException {
        Object o = model.get(tagName);
        if (o instanceof Closure) {
            if (args instanceof Object[]) {
                yieldUnescaped(((Closure) o).call((Object[])args));
                return this;
            }
            yieldUnescaped(((Closure) o).call(args));
            return this;
        } else if (args instanceof Object[]) {
            final Writer wrt = out;
            TagData tagData = new TagData(args).invoke();
            Object body = tagData.getBody();
            writeIndent();
            wrt.write('<');
            wrt.write(tagName);
            writeAttributes(tagData.getAttributes());
            if (body != null) {
                wrt.write('>');
                writeBody(body);
                writeIndent();
                wrt.write("</");
                wrt.write(tagName);
                wrt.write('>');
            } else {
                if (configuration.isExpandEmptyElements()) {
                    wrt.write("></");
                    wrt.write(tagName);
                    wrt.write('>');
                } else {
                    wrt.write("/>");
                }
            }
        }
        return this;
    }

    private void writeBody(final Object body) throws IOException {
        boolean indent = out instanceof DelegatingIndentWriter;
        if (body instanceof Closure) {
            if (indent) {
                ((DelegatingIndentWriter)(out)).next();
            }
            ((Closure) body).call();
            if (indent) {
                ((DelegatingIndentWriter)(out)).previous();
            }
        } else {
            out.write(body.toString());
        }
    }

    private void writeAttributes(final Map<?, ?> attributes) throws IOException {
        if (attributes == null) {
            return;
        }
        final Writer wrt = out;
        for (Map.Entry entry : attributes.entrySet()) {
            wrt.write(' ');
            String attName = entry.getKey().toString();
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            writeAttribute(attName, value);
        }
    }

    /**
     * Includes another template inside this template.
     * @param templatePath the path to the included resource.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void includeGroovy(String templatePath) throws IOException, ClassNotFoundException {
        URL resource = engine.resolveTemplate(templatePath);
        engine.createTypeCheckedModelTemplate(resource, modelTypes).make(model).writeTo(out);
    }


    /**
     * Includes contents of another file, not as a template but as escaped text.
     *
     * @param templatePath the path to the other file
     * @throws IOException
     */
    public void includeEscaped(String templatePath) throws IOException {
        URL resource = engine.resolveTemplate(templatePath);
        yield(ResourceGroovyMethods.getText(resource, engine.getCompilerConfiguration().getSourceEncoding()));
    }

    /**
     * Includes contents of another file, not as a template but as unescaped text.
     *
     * @param templatePath the path to the other file
     * @throws IOException
     */
    public void includeUnescaped(String templatePath) throws IOException {
        URL resource = engine.resolveTemplate(templatePath);
        yieldUnescaped(ResourceGroovyMethods.getText(resource, engine.getCompilerConfiguration().getSourceEncoding()));
    }

    /**
     * Escapes the string representation of the supplied object if it derives from {@link java.lang.CharSequence},
     * otherwise returns the object itself.
     * @param contents an object to be escaped for XML
     * @return  an escaped string, or the object itself
     */
    public Object tryEscape(Object contents) {
        if (contents instanceof CharSequence) {
            return escapeXml(contents.toString());
        }
        return contents;
    }

    /**
     * Convenience method to return the current writer instance.
     *
     * @return the current writer
     */
    public Writer getOut() {
        return out;
    }

    /**
     * Adds a new line to the output. The new line string can be configured by
     * {@link groovy.text.markup.TemplateConfiguration#setNewLineString(String)}
     * @throws IOException
     */
    public void newLine() throws IOException {
        yieldUnescaped(configuration.getNewLineString());
        doWriteIndent = true;
    }

    /**
     * Renders an embedded template as a fragment. Fragments are cached in a template, meaning that
     * if you use the same fragment in a template, it will only be compiled once, but once <b>per template
     * instance</b>. This is less performant than using {@link #layout(java.util.Map, String)}.
     *
     * @param model model to be passed to the template
     * @param templateText template body
     * @return this template instance
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Object fragment(Map model, String templateText) throws IOException, ClassNotFoundException {
        Template template = cachedFragments.get(templateText);
        if (template==null) {
            template = engine.createTemplate(new StringReader(templateText));
            cachedFragments.put(templateText, template);
        }
        template.make(model).writeTo(out);
        return this;
    }

    /**
     * Imports a template and renders it using the specified model, allowing fine grained composition
     * of templates and layouting. This works similarily to a template include but allows a distinct
     * model to be used. This version doesn't inherit the model from the parent. If you need model
     * inheritance, see {@link #layout(java.util.Map, String, boolean)}.
     * @param model model to be passed to the template
     * @param templateName the name of the template to be used as a layout
     * @return this template instance
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Object layout(Map model, String templateName) throws IOException, ClassNotFoundException {
        return layout(model, templateName, false);
    }

    /**
     * Imports a template and renders it using the specified model, allowing fine grained composition of templates and
     * layouting. This works similarily to a template include but allows a distinct model to be used. If the layout
     * inherits from the parent model, a new model is created, with the values from the parent model, eventually
     * overriden with those provided specifically for this layout.
     *
     * @param model        model to be passed to the template
     * @param templateName the name of the template to be used as a layout
     * @param inheritModel a boolean indicating if we should inherit the parent model
     * @return this template instance
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Object layout(Map model, String templateName, boolean inheritModel) throws IOException, ClassNotFoundException {
        Map submodel = inheritModel ? forkModel(model) : model;
        URL resource = engine.resolveTemplate(templateName);
        engine.createTypeCheckedModelTemplate(resource, modelTypes).make(submodel).writeTo(out);
        return this;
    }

    @SuppressWarnings("unchecked")
    private Map forkModel(Map m) {
        Map result = new HashMap();
        result.putAll(model);
        result.putAll(m);
        return result;
    }

    /**
     * Wraps a closure so that it can be used as a prototype for inclusion in layouts. This is useful when
     * you want to use a closure in a model, but that you don't want to render the result of the closure but instead
     * call it as if it was a specification of a template fragment.
     * @param cl the fragment to be wrapped
     * @return a wrapped closure returning an empty string
     */
    public Closure contents(final Closure cl) {
        return new Closure(cl.getOwner(), cl.getThisObject()) {
            @Override
            public Object call() {
                cl.call();
                return "";
            }

            @Override
            public Object call(final Object... args) {
                cl.call(args);
                return "";
            }

            @Override
            public Object call(final Object arguments) {
                cl.call(arguments);
                return "";
            }
        };
    }

    /**
     * Main method used to render a template.
     * @param out the Writer to which this Writable should output its data.
     * @return a writer instance
     * @throws IOException
     */
    public Writer writeTo(final Writer out) throws IOException {
        if (this.out!=null) {
            // StackOverflow prevention
            return NullWriter.DEFAULT;
        }
        try {
            this.out = createWriter(out);
            run();
            return out;
        } finally {
            if (this.out!=null) {
                this.out.flush();
            }
            this.out = null;
        }
    }

    private Writer createWriter(final Writer out) {
        return configuration.isAutoIndent() && !(out instanceof DelegatingIndentWriter)?new DelegatingIndentWriter(out, configuration.getAutoIndentString()):out;
    }

    private class TagData {
        private final Object[] array;
        private Map attributes;
        private Object body;

        public TagData(final Object args) {
            this.array = (Object[])args;
        }

        public Map getAttributes() {
            return attributes;
        }

        public Object getBody() {
            return body;
        }

        public TagData invoke() {
            attributes = null;
            body = null;
            for (Object o : array) {
                if (o instanceof Map) {
                    attributes = (Map) o;
                } else {
                    body = o;
                }
            }
            return this;
        }
    }

    public String toString() {
        StringWriter wrt = new StringWriter(512);
        try {
            writeTo(wrt);
        } catch (IOException e) {
            ExceptionUtils.sneakyThrow(e);
        }
        return wrt.toString();
    }
}
