package org.codehaus.groovy.tools.shell.util

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created with IntelliJ IDEA.
 * User: kruset
 * Date: 4/7/13
 * Time: 8:15 PM
 * To change this template use File | Settings | File Templates.
 */
class PackageHelper {

    public final static Pattern NAME_PATTERN = java.util.regex.Pattern.compile("^[A-Z][^.\$_]+\$")
    //protected static final Logger log = Logger.create(PackageHelper.class)
    Map<String, CachedPackage> rootPackages = new HashMap<String, CachedPackage>()
    boolean initialized = false
    GroovyClassLoader groovyClassLoader

    PackageHelper(GroovyClassLoader groovyClassLoader) {
        this.groovyClassLoader = groovyClassLoader
    }

    void initialize() throws IOException {

        Set<URL> urls = new HashSet<URL>()

        for (ClassLoader loader = groovyClassLoader; loader != null; loader = loader.parent) {
            if (!(loader instanceof URLClassLoader)) {
                continue
            }

            urls.addAll(((URLClassLoader)loader).URLs)
        }

        //log.debug(urls)

        def systemClasses = [String.class, javax.swing.JFrame.class, GroovyObject.class] as Class[]
        systemClasses.each { Class systemClass ->
            def classURL = systemClass.getResource("/${systemClass.name.replace('.', '/')}.class")
            if (classURL != null) {
                URLConnection uc = classURL.openConnection()
                if (uc instanceof JarURLConnection) {
                    urls.add(((JarURLConnection) uc).getJarFileURL())
                }
            }
        }
        StringTokenizer tokenizer
        for (url in urls) {
            URLClassLoader urlLoader = new URLClassLoader([url] as URL[])
            //log.debug(urlLoader.packages.getClass())
            urlLoader.packages.each {Package pack ->
                tokenizer = new StringTokenizer(pack.name, '.')
                String rootname = tokenizer.nextToken()
                CachedPackage cp
                CachedPackage childp
                cp = rootPackages.get(rootname, null) as CachedPackage
                if (cp == null) {
                    cp = new CachedPackage(rootname, [url] as Set)
                    rootPackages.put(rootname, cp)
                }

                while(tokenizer.hasMoreTokens()) {
                    String packname = tokenizer.nextToken()
                    if (cp.childPackages == null) {
                        // small initial size, to save memory
                        cp.childPackages = new HashMap<String, CachedPackage>(1)
                    }
                    childp = cp.childPackages.get(packname, null) as CachedPackage
                    if (childp == null) {
                        // start with small arraylist, to save memory
                        Set<URL> urllist = new HashSet<URL>(1)
                        urllist.add(url)
                        childp = new CachedPackage(packname, urllist)
                        cp.childPackages.put(packname, childp)
                    } else {
                        childp.sources.add(url)
                    }
                    cp = childp
                }
            }
        }
        initialized = true
    }

    Set<String> getContents(String packagename) {
        if (! initialized) {
            initialize()
        }
        if (! packagename) {
            return rootPackages.collect { String key, CachedPackage v -> key } as Set
        }
        if (packagename.endsWith(".*")) {
            packagename = packagename[0..-3]
        }

        StringTokenizer tokenizer = new StringTokenizer(packagename, '.')
        CachedPackage cp = rootPackages.get(tokenizer.nextToken())
        while (cp != null && tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken()
            cp = cp.childPackages.get(token) as CachedPackage
        }
        if (cp == null) {
            return null
        }
        Set<String> children = cp.childPackages.collect { String key, CachedPackage v -> key } as Set
        if (cp.checked && !cp.containsClasses) {
            return children
        }

        Set<String> classnames = getClassnames(cp.sources, packagename)

        cp.checked = true
        if (classnames) {
            cp.containsClasses = true
            children.addAll(classnames)
        }
        return children
    }

    /**
     * Copied from JLine 1.0 ClassNameCompletor
     * @param urls
     * @param packagename
     * @return
     */
    static Set<String> getClassnames(Set<URL> urls, String packagename) {
        List<String> classes = new LinkedList<String>()
        String pathname = packagename.replace('.', '/')
        for (Iterator i = urls.iterator(); i.hasNext();) {
            URL url = (URL) i.next();
            File file = new File(url.getFile());

            if (file.isDirectory()) {
                // difficult to test
                continue;
            }

            if ((file == null) || !file.isFile()) // TODO: handle directories
            {
                continue;
            }
            if (!file.toString().endsWith (".jar"))
                continue;

            JarFile jf = new JarFile(file);

            for (Enumeration e = jf.entries(); e.hasMoreElements();) {
                JarEntry entry = (JarEntry) e.nextElement();

                if (entry == null) {
                    continue;
                }

                String name = entry.getName();

                // only use class files
                if (!name.endsWith(".class"))
                {
                    continue;
                }
                int lastslash = name.lastIndexOf('/')
                if (name.substring(0, lastslash) != pathname) {
                    continue
                }
                name = name.substring(lastslash + 1, name.length() - 6)
                Matcher matcher = NAME_PATTERN.matcher(name)
                if (!matcher.find()) {
                    continue
                }
                classes.add(name)
            }
        }

        // now filter classes by changing "/" to "." and trimming the
        // trailing ".class"
        Set classNames = new TreeSet();

        for (Iterator i = classes.iterator(); i.hasNext();) {
            String name = (String) i.next();
            classNames.add(name);
        }

        return classNames
    }
}


class CachedPackage {
    String name
    boolean containsClasses
    boolean checked
    Map<String, CachedPackage> childPackages
    Set<URL> sources

    CachedPackage(String name, Set<URL> sources) {
        this.sources = sources
        this.name = name
    }

    void addChildPackage(CachedPackage cp) {
        if (childPackages == null) {
            childPackages = [cp.name, cp]
        } else {
            childPackages.put(cp.name, cp)
        }
    }
}