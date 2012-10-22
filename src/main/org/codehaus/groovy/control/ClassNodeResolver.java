/*
 * Copyright 2003-2012 the original author or authors.
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
package org.codehaus.groovy.control;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.Verifier;
import org.objectweb.asm.Opcodes;

/**
 * This class is used as a plugable way to resolve class names.
 * An instance of this class has to be added to {@link CompilationUnit} using 
 * {@link CompilationUnit#setClassNodeResolver(ClassNodeResolver)}. The 
 * CompilationUnit will then set the resolver on the {@link ResolveVisitor} each 
 * time new. The ResolveVisitor will prepare name lookup and then finally ask
 * the resolver if the class exists. This resolver then can return either a 
 * SourceUnit or a ClassNode. In case of a SourceUnit the compiler is notified
 * that a new source is to be added to the compilation queue. In case of a
 * ClassNode no further action than the resolving is done. The lookup result
 * is stored in the helper class {@link LookupResult}. This class provides a
 * class cache to cache lookups. If you don't want this, you have to override
 * the methods {@link ClassNodeResolver#cacheClass(String, ClassNode)} and 
 * {@link ClassNodeResolver#getFromClassCache(String)}. Custom lookup logic is
 * supposed to go into the method 
 * {@link ClassNodeResolver#findClassNode(String, CompilationUnit)} while the 
 * entry method is {@link ClassNodeResolver#resolveName(String, CompilationUnit)}
 * 
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 */
public class ClassNodeResolver {

    /**
     * Helper class to return either a SourceUnit or ClassNode.
     * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
     */
    public static class LookupResult {
        private SourceUnit su;
        private ClassNode cn;
        /**
         * creates a new LookupResult. You are not supposed to supply
         * a SourceUnit and a ClassNode at the same time
         */
        public LookupResult(SourceUnit su, ClassNode cn) {
            this.su = su;
            this.cn = cn;
            if (su==null && cn==null) throw new IllegalArgumentException("Either the SourceUnit or the ClassNode must not be null.");
            if (su!=null && cn!=null) throw new IllegalArgumentException("SourceUnit and ClassNode cannot be set at the same time.");
        }
        /**
         * returns true if a ClassNode is stored
         */
        public boolean isClassNode() { return cn!=null; }
        /**
         * returns true if a SourecUnit is stored
         */
        public boolean isSourceUnit() { return su!=null; }
        /**
         * returns the SourceUnit
         */
        public SourceUnit getSourceUnit() { return su; }
        /**
         * returns the ClassNode
         */
        public ClassNode getClassNode() { return cn; }
    }
    
    // Map to store cached classes
    private Map<String,ClassNode> cachedClasses = new HashMap();
    /**
     * Internal helper used to indicate a cache hit for a class that does not exist. 
     * This way further lookups through a slow {@link #findClassNode(String, CompilationUnit)} 
     * path can be avoided.
     * WARNING: This class is not to be used outside of ClassNodeResolver.
     */
    protected static final ClassNode NO_CLASS = new ClassNode("NO_CLASS", Opcodes.ACC_PUBLIC,ClassHelper.OBJECT_TYPE){
        public void setRedirect(ClassNode cn) {
            throw new GroovyBugError("This is a dummy class node only! Never use it for real classes.");
        };
    };
    
    /**
     * Resolves the name of a class to a SourceUnit or ClassNode. If no
     * class or source is found this method returns null. A lookup is done
     * by first asking the cache if there is an entry for the class already available
     * to then call {@link #findClassNode(String, CompilationUnit)}. The result 
     * of that method call will be cached if a ClassNode is found. If a SourceUnit
     * is found, this method will not be asked later on again for that class, because
     * ResolveVisitor will first ask the CompilationUnit for classes in the
     * compilation queue and it will find the class for that SourceUnit there then.
     * method return a ClassNode instead of a SourceUnit, the res 
     * @param name - the name of the class
     * @param compilationUnit - the current CompilationUnit
     * @return the LookupResult
     */
    public LookupResult resolveName(String name, CompilationUnit compilationUnit) {
        ClassNode res = getFromClassCache(name);
        if (res==NO_CLASS) return null;
        if (res!=null) return new LookupResult(null,res);
        
        LookupResult lr = findClassNode(name, compilationUnit);
        if (lr != null) {
            if (lr.isClassNode()) cacheClass(name, lr.getClassNode());
            return lr;
        } else {
            cacheClass(name, NO_CLASS);
            return null;
        }
    }
    
    /**
     * caches a ClassNode
     * @param name - the name of the class
     * @param res - the ClassNode for that name
     */
    public void cacheClass(String name, ClassNode res) {
        cachedClasses.put(name, res);
    }
    
    /**
     * returns whatever is stored in the class cache for the given name
     * @param name - the name of the class
     * @return the result of the lookup, which may be null
     */
    public ClassNode getFromClassCache(String name) {
        // We use here the class cache cachedClasses to prevent
        // calls to ClassLoader#loadClass. Disabling this cache will
        // cause a major performance hit.
        ClassNode cached = cachedClasses.get(name);
        return cached;
    }
    
    /**
     * Extension point for custom lookup logic of finding ClassNodes. Per default
     * this will use the CompilationUnit class loader to do a lookup on the class
     * path and load the needed class using that loader. Or if a script is found 
     * and that script is seen as "newer", the script will be used instead of the 
     * class.
     * 
     * @param name - the name of the class
     * @param compilationUnit - the current compilation unit
     * @return the lookup result
     */
    public LookupResult findClassNode(String name, CompilationUnit compilationUnit) {
        return tryAsLoaderClassOrScript(name, compilationUnit);
    }
    
    /**
     * This method is used to realize the lookup of a class using the compilation
     * unit class loader. Should no class be found we fall abck to a script lookup.
     * If a class is found we check if there is also a script and maybe use that
     * one in case it is newer.
     */
    private LookupResult tryAsLoaderClassOrScript(String name, CompilationUnit compilationUnit) {
        GroovyClassLoader loader = compilationUnit.getClassLoader();
        Class cls;
        try {
            // NOTE: it's important to do no lookup against script files
            // here since the GroovyClassLoader would create a new CompilationUnit
            cls = loader.loadClass(name, false, true);
        } catch (ClassNotFoundException cnfe) {
            LookupResult lr = tryAsScript(name, compilationUnit, null);
            return lr;
        } catch (CompilationFailedException cfe) {
            throw new GroovyBugError("The lookup for "+name+" caused a failed compilaton. There should not have been any compilation from this call.");
        }
        //TODO: the case of a NoClassDefFoundError needs a bit more research
        // a simple recompilation is not possible it seems. The current class
        // we are searching for is there, so we should mark that somehow.
        // Basically the missing class needs to be completely compiled before
        // we can again search for the current name.
        /*catch (NoClassDefFoundError ncdfe) {
            cachedClasses.put(name,SCRIPT);
            return false;
        }*/
        if (cls == null) return null;
        //NOTE: we might return false here even if we found a class,
        //      because  we want to give a possible script a chance to
        //      recompile. This can only be done if the loader was not
        //      the instance defining the class.
        if (cls.getClassLoader() != loader) {
            return tryAsScript(name, compilationUnit, cls);
        }
        ClassNode cn = ClassHelper.make(cls);
        return new LookupResult(null,cn); 
    }
    
    /**
     * try to find a script using the compilation unit class loader.
     */
    private LookupResult tryAsScript(String name, CompilationUnit compilationUnit, Class oldClass) {
        LookupResult lr = null;
        if (oldClass!=null) {
            ClassNode cn = ClassHelper.make(oldClass);
            lr = new LookupResult(null,cn);
        }
        
        if (name.startsWith("java.")) return lr;
        //TODO: don't ignore inner static classes completely
        if (name.indexOf('$') != -1) return lr;
        
        // try to find a script from classpath*/
        GroovyClassLoader gcl = compilationUnit.getClassLoader();
        URL url = null;
        try {
            url = gcl.getResourceLoader().loadGroovySource(name);
        } catch (MalformedURLException e) {
            // fall through and let the URL be null
        }
        if (url != null && ( oldClass==null || isSourceNewer(url, oldClass))) {
            SourceUnit su = compilationUnit.addSource(url);
            return new LookupResult(su,null);
        }
        return lr;
    }

    /**
     * get the time stamp of a class
     * NOTE: copied from GroovyClassLoader
     */
    private long getTimeStamp(Class cls) {
        return Verifier.getTimestamp(cls);
    }

    /**
     * returns true if the source in URL is newer than the class
     * NOTE: copied from GroovyClassLoader
     */
    private boolean isSourceNewer(URL source, Class cls) {
        try {
            long lastMod;

            // Special handling for file:// protocol, as getLastModified() often reports
            // incorrect results (-1)
            if (source.getProtocol().equals("file")) {
                // Coerce the file URL to a File
                String path = source.getPath().replace('/', File.separatorChar).replace('|', ':');
                File file = new File(path);
                lastMod = file.lastModified();
            } else {
                URLConnection conn = source.openConnection();
                lastMod = conn.getLastModified();
                conn.getInputStream().close();
            }
            return lastMod > getTimeStamp(cls);
        } catch (IOException e) {
            // if the stream can't be opened, let's keep the old reference
            return false;
        }
    }
}
