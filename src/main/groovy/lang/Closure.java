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

import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.reflection.stdclasses.CachedClosureClass;
import org.codehaus.groovy.runtime.*;
import org.codehaus.groovy.runtime.callsite.BooleanClosureWrapper;
import org.codehaus.groovy.runtime.memoize.LRUCache;
import org.codehaus.groovy.runtime.memoize.Memoize;
import org.codehaus.groovy.runtime.memoize.UnlimitedConcurrentCache;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Represents any closure object in Groovy.
 * <p/>
 * Groovy allows instances of Closures to be called in a
 * short form. For example:
 * <pre>
 * def a = 1
 * def c = { a }
 * assert c() == 1
 * </pre>
 * To be able to use a Closure in this way with your own
 * subclass, you need to provide a doCall method with any
 * signature you want to. This ensures that
 * {@link #getMaximumNumberOfParameters()} and
 * {@link #getParameterTypes()} will work too without any
 * additional code. If no doCall method is provided a
 * closure must be used in its long form like
 * <pre>
 * def a = 1
 * def c = {a}
 * assert c.call() == 1
 * </pre>
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author <a href="mailto:tug@wilson.co.uk">John Wilson</a>
 * @author <a href="mailto:blackdrag@gmx.org">Jochen Theodorou</a>
 * @author Graeme Rocher
 * @author Paul King
 */
public abstract class Closure<V> extends GroovyObjectSupport implements Cloneable, Runnable, GroovyCallable<V>, Serializable {

    /**
     * With this resolveStrategy set the closure will attempt to resolve property references and methods to the
     * owner first, then the delegate (<b>this is the default strategy</b>).
     *
     * For example the following code :
     * <pre>
     * class Test {
     *     def x = 30
     *     def y = 40
     *
     *     def run() {
     *         def data = [ x: 10, y: 20 ]
     *         def cl = { y = x + y }
     *         cl.delegate = data
     *         cl()
     *         println x
     *         println y
     *         println data
     *     }
     * }
     *
     * new Test().run()
     * </pre>
     * will output :
     * <pre>
     * 30
     * 70
     * [x:10, y:20]
     * </pre>
     * because the x and y fields declared in the Test class the variables in the delegate.<p>
     * <i>Note that local variables are always looked up first, independently of the resolution strategy.</i>
     */
    public static final int OWNER_FIRST = 0;

    /**
     * With this resolveStrategy set the closure will attempt to resolve property references and methods to the
     * delegate first then the owner.
     *
     * For example the following code :
     * <pre>
     * class Test {
     *     def x = 30
     *     def y = 40
     *
     *     def run() {
     *         def data = [ x: 10, y: 20 ]
     *         def cl = { y = x + y }
     *         cl.delegate = data
     *         cl.resolveStrategy = Closure.DELEGATE_FIRST
     *         cl()
     *         println x
     *         println y
     *         println data
     *     }
     * }
     *
     * new Test().run()
     * </pre>
     * will output :
     * <pre>
     * 30
     * 40
     * [x:10, y:30]
     * </pre>
     * because the x and y variables declared in the delegate shadow the fields in the owner class.<p>
     * <i>Note that local variables are always looked up first, independently of the resolution strategy.</i>
     */
    public static final int DELEGATE_FIRST = 1;

    /**
     * With this resolveStrategy set the closure will resolve property references and methods to the owner only
     * and not call the delegate at all. For example the following code :
     *
     * <pre>
     * class Test {
     *     def x = 30
     *     def y = 40
     *
     *     def run() {
     *         def data = [ x: 10, y: 20, z: 30 ]
     *         def cl = { y = x + y + z }
     *         cl.delegate = data
     *         cl.resolveStrategy = Closure.OWNER_ONLY
     *         cl()
     *         println x
     *         println y
     *         println data
     *     }
     * }
     *
     * new Test().run()
     * </pre>
     *
     * will throw "No such property: z" error because even if the z variable is declared in the delegate, no
     * lookup is made.<p>
     * <i>Note that local variables are always looked up first, independently of the resolution strategy.</i>
     */
    public static final int OWNER_ONLY = 2;

    /**
     * With this resolveStrategy set the closure will resolve property references and methods to the delegate
     * only and entirely bypass the owner. For example the following code :
     *
     * <pre>
     * class Test {
     *     def x = 30
     *     def y = 40
     *     def z = 50
     *
     *     def run() {
     *         def data = [ x: 10, y: 20 ]
     *         def cl = { y = x + y + z }
     *         cl.delegate = data
     *         cl.resolveStrategy = Closure.DELEGATE_ONLY
     *         cl()
     *         println x
     *         println y
     *         println data
     *     }
     * }
     *
     * new Test().run()
     * </pre>
     *
     * will throw an error because even if the owner declares a "z" field, the resolution strategy will bypass
     * lookup in the owner.<p>
     * <i>Note that local variables are always looked up first, independently of the resolution strategy.</i>
     */
    public static final int DELEGATE_ONLY = 3;

    /**
     * With this resolveStrategy set the closure will resolve property references to itself and go
     * through the usual MetaClass look-up process. This means that properties and methods are neither resolved
     * from the owner nor the delegate, but only on the closure object itself. This allows the developer to
     * override getProperty using ExpandoMetaClass of the closure itself.<p>
     * <i>Note that local variables are always looked up first, independently of the resolution strategy.</i>
     */
    public static final int TO_SELF = 4;

    public static final int DONE = 1, SKIP = 2;
    private static final Object[] EMPTY_OBJECT_ARRAY = {};
    public static final Closure IDENTITY = new Closure<Object>(null) {
        public Object doCall(Object args) {
            return args;
        }
    };

    private Object delegate;
    private Object owner;
    private Object thisObject;
    private int resolveStrategy = OWNER_FIRST;
    private int directive;
    protected Class[] parameterTypes;
    protected int maximumNumberOfParameters;
    private static final long serialVersionUID = 4368710879820278874L;
    private BooleanClosureWrapper bcw;

    public Closure(Object owner, Object thisObject) {
        this.owner = owner;
        this.delegate = owner;
        this.thisObject = thisObject;

        final CachedClosureClass cachedClass = (CachedClosureClass) ReflectionCache.getCachedClass(getClass());
        parameterTypes = cachedClass.getParameterTypes();
        maximumNumberOfParameters = cachedClass.getMaximumNumberOfParameters();
    }

    /**
     * Constructor used when the "this" object for the Closure is null.
     * This is rarely the case in normal Groovy usage.
     *
     * @param owner the Closure owner
     */
    public Closure(Object owner) {
        this(owner, null);
    }

    /**
     * Sets the strategy which the closure uses to resolve property references and methods.
     * The default is Closure.OWNER_FIRST
     *
     * @param resolveStrategy The resolve strategy to set
     *
     * @see groovy.lang.Closure#DELEGATE_FIRST
     * @see groovy.lang.Closure#DELEGATE_ONLY
     * @see groovy.lang.Closure#OWNER_FIRST
     * @see groovy.lang.Closure#OWNER_ONLY
     * @see groovy.lang.Closure#TO_SELF
     */
    public void setResolveStrategy(int resolveStrategy) {
        this.resolveStrategy = resolveStrategy;
    }

    /**
     * Gets the strategy which the closure users to resolve methods and properties
     *
     * @return The resolve strategy
     *
     * @see groovy.lang.Closure#DELEGATE_FIRST
     * @see groovy.lang.Closure#DELEGATE_ONLY
     * @see groovy.lang.Closure#OWNER_FIRST
     * @see groovy.lang.Closure#OWNER_ONLY
     * @see groovy.lang.Closure#TO_SELF
     */
    public int getResolveStrategy() {
        return resolveStrategy;
    }

    public Object getThisObject(){
        return thisObject;
    }

    public Object getProperty(final String property) {
        if ("delegate".equals(property)) {
            return getDelegate();
        } else if ("owner".equals(property)) {
            return getOwner();
        } else if ("maximumNumberOfParameters".equals(property)) {
            return getMaximumNumberOfParameters();
        } else if ("parameterTypes".equals(property)) {
            return getParameterTypes();
        } else if ("metaClass".equals(property)) {
            return getMetaClass();
        } else if ("class".equals(property)) {
            return getClass();
        } else if ("directive".equals(property)) {
            return getDirective();
        } else if ("resolveStrategy".equals(property)) {
            return getResolveStrategy();
        } else if ("thisObject".equals(property)) {
            return getThisObject();
        } else {
            switch(resolveStrategy) {
                case DELEGATE_FIRST:
                    return getPropertyDelegateFirst(property);
                case DELEGATE_ONLY:
                    return InvokerHelper.getProperty(this.delegate, property);
                case OWNER_ONLY:
                    return InvokerHelper.getProperty(this.owner, property);
                case TO_SELF:
                    return super.getProperty(property);
                default:
                    return getPropertyOwnerFirst(property);
            }
        }
    }

    private Object getPropertyDelegateFirst(String property) {
        if (delegate == null) return getPropertyOwnerFirst(property);
        return getPropertyTryThese(property, this.delegate, this.owner);
    }

    private Object getPropertyOwnerFirst(String property) {
        return getPropertyTryThese(property, this.owner, this.delegate);
    }

    private Object getPropertyTryThese(String property, Object firstTry, Object secondTry) {
        try {
            // let's try getting the property on the first object
            return InvokerHelper.getProperty(firstTry, property);
        } catch (MissingPropertyException e1) {
            if (secondTry != null && firstTry != this && firstTry != secondTry) {
                try {
                    // let's try getting the property on the second object
                    return InvokerHelper.getProperty(secondTry, property);
                } catch (GroovyRuntimeException e2) {
                    // ignore, we'll throw e1
                }
            }
            throw e1;
        }
    }

    public void setProperty(String property, Object newValue) {
        if ("delegate".equals(property)) {
            setDelegate(newValue);
        } else if ("metaClass".equals(property)) {
            setMetaClass((MetaClass) newValue);
        } else if ("resolveStrategy".equals(property)) {
            setResolveStrategy(((Number) newValue).intValue());
        } else if ("directive".equals(property)) {
            setDirective(((Number) newValue).intValue());
        } else {
            switch(resolveStrategy) {
                case DELEGATE_FIRST:
                    setPropertyDelegateFirst(property, newValue);
                break;
                case DELEGATE_ONLY:
                    InvokerHelper.setProperty(this.delegate, property, newValue);
                break;
                case OWNER_ONLY:
                    InvokerHelper.setProperty(this.owner, property, newValue);
                break;
                case TO_SELF:
                    super.setProperty(property, newValue);
                break;
                default:
                    setPropertyOwnerFirst(property, newValue);
            }
        }
    }

    private void setPropertyDelegateFirst(String property, Object newValue) {
        if (delegate == null) setPropertyOwnerFirst(property, newValue);
        else setPropertyTryThese(property, newValue, this.delegate, this.owner);
    }

    private void setPropertyOwnerFirst(String property, Object newValue) {
        setPropertyTryThese(property, newValue, this.owner, this.delegate);
    }

    private void setPropertyTryThese(String property, Object newValue, Object firstTry, Object secondTry) {
        try {
            // let's try setting the property on the first object
            InvokerHelper.setProperty(firstTry, property, newValue);
        } catch (GroovyRuntimeException e1) {
            if (firstTry != null && firstTry != this && firstTry != secondTry) {
                try {
                    // let's try setting the property on the second object
                    InvokerHelper.setProperty(secondTry, property, newValue);
                    return;
                } catch (GroovyRuntimeException e2) {
                    // ignore, we'll throw e1
                }
            }
            throw e1;
        }
    }

    public boolean isCase(Object candidate){
        if (bcw==null) {
            bcw = new BooleanClosureWrapper(this);
        }
        return bcw.call(candidate);
    }

    /**
     * Invokes the closure without any parameters, returning any value if applicable.
     *
     * @return the value if applicable or null if there is no return statement in the closure
     */
    public V call() {
        final Object[] NOARGS = EMPTY_OBJECT_ARRAY;
        return call(NOARGS);
    }

    @SuppressWarnings("unchecked")
    public V call(Object... args) {
        try {
            return (V) getMetaClass().invokeMethod(this,"doCall",args);
        } catch (InvokerInvocationException e) {
            ExceptionUtils.sneakyThrow(e.getCause());
            return null; // unreachable statement
        }  catch (Exception e) {
            return (V) throwRuntimeException(e);
        }
    }

    /**
     * Invokes the closure, returning any value if applicable.
     *
     * @param arguments could be a single value or a List of values
     * @return the value if applicable or null if there is no return statement in the closure
     */
    public V call(final Object arguments) {
        return call(new Object[]{arguments});
    }

    protected static Object throwRuntimeException(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else {
            throw new GroovyRuntimeException(throwable.getMessage(), throwable);
        }
    }

    /**
     * @return the owner Object to which method calls will go which is
     *         typically the outer class when the closure is constructed
     */
    public Object getOwner() {
        return this.owner;
    }

    /**
     * @return the delegate Object to which method calls will go which is
     *         typically the outer class when the closure is constructed
     */
    public Object getDelegate() {
        return this.delegate;
    }

    /**
     * Allows the delegate to be changed such as when performing markup building
     *
     * @param delegate the new delegate
     */
    public void setDelegate(Object delegate) {
        this.delegate = delegate;
    }

    /**
     * @return the parameter types of the longest doCall method
     * of this closure
     */
    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * @return the maximum number of parameters a doCall method
     * of this closure can take
     */
    public int getMaximumNumberOfParameters() {
        return maximumNumberOfParameters;
    }

    /**
     * @return a version of this closure which implements Writable.  Note that
     * the returned Writable also overrides {@link #toString()} in order
     * to allow rendering the result directly to a String.
     */
    public Closure asWritable() {
        return new WritableClosure();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        call();
    }

    /**
     * Support for Closure currying.
     * <p>
     * Typical usage:
     * <pre class="groovyTestCase">
     * def multiply = { a, b -> a * b }
     * def doubler = multiply.curry(2)
     * assert doubler(4) == 8
     * </pre>
     * Note: special treatment is given to Closure vararg-style capability.
     * If you curry a vararg parameter, you don't consume the entire vararg array
     * but instead the first parameter of the vararg array as the following example shows:
     * <pre class="groovyTestCase">
     * def a = { one, two, Object[] others -> one + two + others.sum() }
     * assert a.parameterTypes.name == ['java.lang.Object', 'java.lang.Object', '[Ljava.lang.Object;']
     * assert a(1,2,3,4) == 10
     * def b = a.curry(1)
     * assert b.parameterTypes.name == ['java.lang.Object', '[Ljava.lang.Object;']
     * assert b(2,3,4) == 10
     * def c = b.curry(2)
     * assert c.parameterTypes.name == ['[Ljava.lang.Object;']
     * assert c(3,4) == 10
     * def d = c.curry(3)
     * assert d.parameterTypes.name == ['[Ljava.lang.Object;']
     * assert d(4) == 10
     * def e = d.curry(4)
     * assert e.parameterTypes.name == ['[Ljava.lang.Object;']
     * assert e() == 10
     * assert e(5) == 15
     * </pre>
     *
     *
     * @param arguments the arguments to bind
     * @return the new closure with its arguments bound
     */
    public Closure<V> curry(final Object... arguments) {
        return new CurriedClosure<V>(this, arguments);
    }

    /**
     * Support for Closure currying.
     *
     * @param argument the argument to bind
     * @return the new closure with the argument bound
     * @see #curry(Object...)
     */
    public Closure<V> curry(final Object argument) {
        return curry(new Object[]{argument});
    }

    /**
     * Support for Closure "right" currying.
     * Parameters are supplied on the right rather than left as per the normal curry() method.
     * Typical usage:
     * <pre>
     * def divide = { a, b -> a / b }
     * def halver = divide.rcurry(2)
     * assert halver(8) == 4
     * </pre>
     *
     * @param arguments the arguments to bind
     * @return the new closure with its arguments bound
     * @see #curry(Object...)
     */
    public Closure<V> rcurry(final Object... arguments) {
        return new CurriedClosure<V>(-arguments.length, this, arguments);
    }

    /**
     * Support for Closure "right" currying.
     *
     * @param argument the argument to bind
     * @return the new closure with the argument bound
     * @see #rcurry(Object...)
     */
    public Closure<V> rcurry(final Object argument) {
        return rcurry(new Object[]{argument});
    }

    /**
     * Support for Closure currying at a given index.
     * Parameters are supplied from index position "n".
     * Typical usage:
     * <pre>
     * def caseInsensitive = { a, b -> a.toLowerCase() <=> b.toLowerCase() } as Comparator
     * def caseSensitive = { a, b -> a <=> b } as Comparator
     * def animals1 = ['ant', 'dog', 'BEE']
     * def animals2 = animals1 + ['Cat']
     * // curry middle param of this utility method:
     * // Collections#binarySearch(List list, Object key, Comparator c)
     * def catSearcher = Collections.&binarySearch.ncurry(1, "cat")
     * [[animals1, animals2], [caseInsensitive, caseSensitive]].combinations().each{ a, c ->
     *   def idx = catSearcher(a.sort(c), c)
     *   print a.sort(c).toString().padRight(22)
     *   if (idx < 0) println "Not found but would belong in position ${-idx - 1}"
     *   else println "Found at index $idx"
     * }
     * // =>
     * // [ant, BEE, dog]       Not found but would belong in position 2
     * // [ant, BEE, Cat, dog]  Found at index 2
     * // [BEE, ant, dog]       Not found but would belong in position 2
     * // [BEE, Cat, ant, dog]  Not found but would belong in position 3
     * </pre>
     *
     * @param n the index from which to bind parameters (may be -ve in which case it will be normalized)
     * @param arguments the arguments to bind
     * @return the new closure with its arguments bound
     * @see #curry(Object...)
     */
    public Closure<V> ncurry(int n, final Object... arguments) {
        return new CurriedClosure<V>(n, this, arguments);
    }

    /**
     * Support for Closure currying at a given index.
     *
     * @param argument the argument to bind
     * @return the new closure with the argument bound
     * @see #ncurry(int, Object...)
     */
    public Closure<V> ncurry(int n, final Object argument) {
        return ncurry(n, new Object[]{argument});
    }

    /**
     * Support for Closure forward composition.
     * <p/>
     * Typical usage:
     * <pre>
     * def twice = { a -> a * 2 }
     * def thrice = { a -> a * 3 }
     * def times6 = twice >> thrice
     * // equivalent: times6 = { a -> thrice(twice(a)) }
     * assert times6(3) == 18
     * </pre>
     *
     * @param other the Closure to compose with the current Closure
     * @return the new composed Closure
     */
    public <W> Closure<W> rightShift(final Closure<W> other) {
        return new ComposedClosure<W>(this, other);
    }

    /**
     * Support for Closure reverse composition.
     * <p/>
     * Typical usage:
     * <pre>
     * def twice = { a -> a * 2 }
     * def thrice = { a -> a * 3 }
     * def times6 = thrice << twice
     * // equivalent: times6 = { a -> thrice(twice(a)) }
     * assert times6(3) == 18
     * </pre>
     *
     * @param other the Closure to compose with the current Closure
     * @return the new composed Closure
     */
    public Closure<V> leftShift(final Closure other) {
        return new ComposedClosure<V>(other, this);
    }

    /* *
     * Alias for calling a Closure for non-closure arguments.
     * <p/>
     * Typical usage:
     * <pre>
     * def twice = { a -> a * 2 }
     * def thrice = { a -> a * 3 }
     * assert thrice << twice << 3 == 18
     * </pre>
     *
     * @param arg the argument to call the closure with
     * @return the result of calling the Closure
     */
    public V leftShift(final Object arg) {
        return call(arg);
    }

    /**
     * Creates a caching variant of the closure.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant will keep all cached values forever, i.e. till the closure gets garbage-collected.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     *
     * The cache gets garbage-collected together with the memoized closure.
     *
     * @return A new closure forwarding to the original one while caching the results
     */
    public Closure<V> memoize() {
        return Memoize.buildMemoizeFunction(new UnlimitedConcurrentCache(), this);
    }

    /**
     * Creates a caching variant of the closure with upper limit on the cache size.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant will keep all values until the upper size limit is reached. Then the values in the cache start rotating
     * using the LRU (Last Recently Used) strategy.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     *
     * The cache gets garbage-collected together with the memoized closure.
     *
     * @param maxCacheSize The maximum size the cache can grow to
     * @return A new function forwarding to the original one while caching the results
     */
    public Closure<V> memoizeAtMost(final int maxCacheSize) {
        if (maxCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for memoizeAtMost.");

        return Memoize.buildMemoizeFunction(new LRUCache(maxCacheSize), this);
    }

    /**
     * Creates a caching variant of the closure with automatic cache size adjustment and lower limit
     * on the cache size.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant allows the garbage collector to release entries from the cache and at the same time allows
     * the user to specify how many entries should be protected from the eventual gc-initiated eviction.
     * Cached entries exceeding the specified preservation threshold are made available for eviction based on
     * the LRU (Last Recently Used) strategy.
     * Given the non-deterministic nature of garbage collector, the actual cache size may grow well beyond the limits
     * set by the user if memory is plentiful.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. Also the protectedCacheSize parameter
     * might not be respected accurately in such scenarios for some periods of time. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     *
     * The cache gets garbage-collected together with the memoized closure.
     * @param protectedCacheSize Number of cached return values to protect from garbage collection
     * @return A new function forwarding to the original one while caching the results
     */
    public Closure<V> memoizeAtLeast(final int protectedCacheSize) {
        if (protectedCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the protectedCacheSize parameter for memoizeAtLeast.");

        return Memoize.buildSoftReferenceMemoizeFunction(protectedCacheSize, new UnlimitedConcurrentCache(), this);
    }

    /**
     * Creates a caching variant of the closure with automatic cache size adjustment and lower and upper limits
     * on the cache size.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant allows the garbage collector to release entries from the cache and at the same time allows
     * the user to specify how many entries should be protected from the eventual gc-initiated eviction.
     * Cached entries exceeding the specified preservation threshold are made available for eviction based on
     * the LRU (Last Recently Used) strategy.
     * Given the non-deterministic nature of garbage collector, the actual cache size may grow well beyond the protected
     * size limits set by the user, if memory is plentiful.
     * Also, this variant will never exceed in size the upper size limit. Once the upper size limit has been reached,
     * the values in the cache start rotating using the LRU (Last Recently Used) strategy.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. Also the protectedCacheSize parameter
     * might not be respected accurately in such scenarios for some periods of time. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     *
     * The cache gets garbage-collected together with the memoized closure.
     * @param protectedCacheSize Number of cached return values to protect from garbage collection
     * @param maxCacheSize The maximum size the cache can grow to
     * @return A new function forwarding to the original one while caching the results
     */
    public Closure<V> memoizeBetween(final int protectedCacheSize, final int maxCacheSize) {
        if (protectedCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the protectedCacheSize parameter for memoizeBetween.");
        if (maxCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for memoizeBetween.");
        if (protectedCacheSize > maxCacheSize) throw new IllegalArgumentException("The maxCacheSize parameter to memoizeBetween is required to be greater or equal to the protectedCacheSize parameter.");

        return Memoize.buildSoftReferenceMemoizeFunction(protectedCacheSize, new LRUCache(maxCacheSize), this);
    }

    /**
     * Builds a trampolined variant of the current closure.
     * To prevent stack overflow due to deep recursion, functions can instead leverage the trampoline mechanism
     * and avoid recursive calls altogether. Under trampoline, the function is supposed to perform one step of
     * the calculation and, instead of a recursive call to itself or another function, it return back a new closure,
     * which will be executed by the trampoline as the next step.
     * Once a non-closure value is returned, the trampoline stops and returns the value as the final result.
     * Here is an example:
     * <pre>
     * def fact
     * fact = { n, total ->
     *     n == 0 ? total : fact.trampoline(n - 1, n * total)
     * }.trampoline()
     * def factorial = { n -> fact(n, 1G)}
     * println factorial(20) // => 2432902008176640000
     * </pre>
     *
     * @param args Parameters to the closure, so as the trampoline mechanism can call it
     * @return A closure, which will execute the original closure on a trampoline.
     */
    public Closure<V> trampoline(final Object... args) {
        return new TrampolineClosure<V>(this.curry(args));
    }

    /**
     * Builds a trampolined variant of the current closure.
     * To prevent stack overflow due to deep recursion, functions can instead leverage the trampoline mechanism
     * and avoid recursive calls altogether. Under trampoline, the function is supposed to perform one step of
     * the calculation and, instead of a recursive call to itself or another function, it return back a new closure,
     * which will be executed by the trampoline as the next step.
     * Once a non-closure value is returned, the trampoline stops and returns the value as the final result.
     * @return A closure, which will execute the original closure on a trampoline.
     * @see #trampoline(Object...)
     */
    public Closure<V> trampoline() {
        return new TrampolineClosure<V>(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }

    /*
     * Implementation note:
     *   This has to be an inner class!
     *
     * Reason:
     *   Closure.this.call will call the outer call method, but
     * with the inner class as executing object. This means any
     * invokeMethod or getProperty call will be called on this
     * inner class instead of the outer!
     */
    private class WritableClosure extends Closure implements Writable {
        public WritableClosure() {
            super(Closure.this);
        }

        /* (non-Javadoc)
         * @see groovy.lang.Writable#writeTo(java.io.Writer)
         */
        public Writer writeTo(Writer out) throws IOException {
            Closure.this.call(new Object[]{out});

            return out;
        }

        /* (non-Javadoc)
         * @see groovy.lang.GroovyObject#invokeMethod(java.lang.String, java.lang.Object)
         */
        public Object invokeMethod(String method, Object arguments) {
            if ("clone".equals(method)) {
                return clone();
            } else if ("curry".equals(method)) {
                return curry((Object[]) arguments);
            } else if ("asWritable".equals(method)) {
                return asWritable();
            } else {
                return Closure.this.invokeMethod(method, arguments);
            }
        }

        /* (non-Javadoc)
         * @see groovy.lang.GroovyObject#getProperty(java.lang.String)
         */
        public Object getProperty(String property) {
            return Closure.this.getProperty(property);
        }

        /* (non-Javadoc)
         * @see groovy.lang.GroovyObject#setProperty(java.lang.String, java.lang.Object)
         */
        public void setProperty(String property, Object newValue) {
            Closure.this.setProperty(property, newValue);
        }

        /* (non-Javadoc)
         * @see groovy.lang.Closure#call()
         */
        public Object call() {
            return ((Closure) getOwner()).call();
        }

        /* (non-Javadoc)
         * @see groovy.lang.Closure#call(java.lang.Object)
         */
        public Object call(Object arguments) {
            return ((Closure) getOwner()).call(arguments);
        }

        public Object call(Object... args) {
            return ((Closure) getOwner()).call(args);
        }

        public Object doCall(Object... args) {
            return call(args);
        }

        /* (non-Javadoc)
         * @see groovy.lang.Closure#getDelegate()
         */
        public Object getDelegate() {
            return Closure.this.getDelegate();
        }

        /* (non-Javadoc)
         * @see groovy.lang.Closure#setDelegate(java.lang.Object)
         */
        public void setDelegate(Object delegate) {
            Closure.this.setDelegate(delegate);
        }

        /* (non-Javadoc)
         * @see groovy.lang.Closure#getParameterTypes()
         */
        public Class[] getParameterTypes() {
            return Closure.this.getParameterTypes();
        }

        /* (non-Javadoc)
         * @see groovy.lang.Closure#getParameterTypes()
         */
        public int getMaximumNumberOfParameters() {
            return Closure.this.getMaximumNumberOfParameters();
        }

        /* (non-Javadoc)
         * @see groovy.lang.Closure#asWritable()
         */
        public Closure asWritable() {
            return this;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            Closure.this.run();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#clone()
         */
        public Object clone() {
            return ((Closure) Closure.this.clone()).asWritable();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return Closure.this.hashCode();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object arg0) {
            return Closure.this.equals(arg0);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringWriter writer = new StringWriter();

            try {
                writeTo(writer);
            } catch (IOException e) {
                return null;
            }

            return writer.toString();
        }

        public Closure curry(final Object... arguments) {
            return (new CurriedClosure(this, arguments)).asWritable();
        }

        public void setResolveStrategy(int resolveStrategy) {
            Closure.this.setResolveStrategy(resolveStrategy);
        }

        public int getResolveStrategy() {
            return Closure.this.getResolveStrategy();
        }
    }

    /**
     * @return Returns the directive.
     */
    public int getDirective() {
        return directive;
    }

    /**
     * @param directive The directive to set.
     */
    public void setDirective(int directive) {
        this.directive = directive;
    }

    /**
     * Returns a copy of this closure where the "owner", "delegate" and "thisObject"
     * fields are null, allowing proper serialization when one of them is not serializable.
     *
     * @return a serializable closure.
     *
     * @since 1.8.5
     */
    @SuppressWarnings("unchecked")
    public Closure<V> dehydrate() {
        Closure<V> result = (Closure<V>) this.clone();
        result.delegate = null;
        result.owner = null;
        result.thisObject = null;
        return result;
    }

    /**
     * Returns a copy of this closure for which the delegate, owner and thisObject are
     * replaced with the supplied parameters. Use this when you want to rehydrate a
     * closure which has been made serializable thanks to the {@link #dehydrate()}
     * method.
     * @param delegate the closure delegate
     * @param owner the closure owner
     * @param thisObject the closure "this" object
     * @return a copy of this closure where owner, delegate and thisObject are replaced
     *
     * @since 1.8.5
     */
    @SuppressWarnings("unchecked")
    public Closure<V> rehydrate(Object delegate, Object owner, Object thisObject) {
        Closure<V> result = (Closure<V>) this.clone();
        result.delegate = delegate;
        result.owner = owner;
        result.thisObject = thisObject;
        return result;
    }
}
