package groovy.util;

import groovy.lang.*;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * {@link Script} that performs method invocations and property access like {@link Closure} does.
 * <p/>
 * <p/>
 * {@link DelegatingScript} is a convenient basis for loading a custom-defined DSL as a {@link Script}, then execute it.
 * The following sample code illustrates how to do it:
 * <p/>
 * <pre>
 * class MyDSL {
 *     public void foo(int x, int y, Closure z) { ... }
 *     public void setBar(String a) { ... }
 * }
 *
 * CompilerConfiguration cc = new CompilerConfiguration();
 * cc.setScriptBaseClass(DelegatingScript.class);
 * GroovyShell sh = new GroovyShell(cl,new Binding(),cc);
 * DelegatingScript script = (DelegatingScript)sh.parse(new File("my.dsl"))
 * script.setDelegate(new MyDSL());
 * script.run();
 * </pre>
 * <p/>
 * <p/>
 * <tt>my.dsl</tt> can look like this:
 * <p/>
 * <pre>
 * foo(1,2) {
 *     ....
 * }
 * bar = ...;
 * </pre>
 * <p/>
 * <p/>
 * {@link DelegatingScript} does this by delegating property access and method invocation to the <tt>delegate</tt> object.
 * <p/>
 * <p/>
 * More formally speaking, given the following script:
 * <p/>
 * <pre>
 * a = 1;
 * b(2);
 * </pre>
 * <p/>
 * <p/>
 * Using {@link DelegatingScript} as the base class, the code will run as:
 * <p/>
 * <pre>
 * delegate.a = 1;
 * delegate.b(2);
 * </pre>
 * <p/>
 * ... whereas in plain {@link Script}, this will be run as:
 * <p/>
 * <pre>
 * binding.setProperty("a",1);
 * ((Closure)binding.getProperty("b")).call(2);
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class DelegatingScript extends Script {
    private Object delegate;
    private MetaClass metaClass;

    protected DelegatingScript() {
        super();
    }

    protected DelegatingScript(Binding binding) {
        super(binding);
    }

    /**
     * Sets the delegation target.
     */
    public void setDelegate(Object delegate) {
        this.delegate = delegate;
        this.metaClass = InvokerHelper.getMetaClass(delegate.getClass());
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        try {
            return metaClass.invokeMethod(delegate, name, args);
        } catch (MissingMethodException mme) {
            return super.invokeMethod(name, args);
        }
    }

    @Override
    public Object getProperty(String property) {
        try {
            return metaClass.getProperty(delegate, property);
        } catch (MissingPropertyException e) {
            return super.getProperty(property);
        }
    }

    @Override
    public void setProperty(String property, Object newValue) {
        try {
            metaClass.setProperty(delegate, property, newValue);
        } catch (MissingPropertyException e) {
            super.setProperty(property, newValue);
        }
    }

    public Object getDelegate() {
        return delegate;
    }
}