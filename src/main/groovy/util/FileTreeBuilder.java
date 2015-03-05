/*
 * Copyright 2003-2015 the original author or authors.
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
package groovy.util;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * A builder dedicated at generating a file directory structure from a
 * specification. For example, imagine that you want to create the following tree:
 * <pre>
 * src/
 *  |--- main
 *  |     |--- groovy
 *  |            |--- Foo.groovy
 *  |--- test
 *        |--- groovy
 *               |--- FooTest.groovy
 *
 * </pre>
 *
 * <p>Then you can create the structure using:</p>
 * <pre><code>
 *     def tree = new FileTreeBuilder()
 *     tree.dir('src') {
 *        dir('main') {
 *           dir('groovy') {
 *              file('Foo.groovy', 'println "Hello"')
 *           }
 *        }
 *        dir('test') {
 *           dir('groovy') {
 *              file('FooTest.groovy', 'class FooTest extends GroovyTestCase {}')
 *           }
 *        }
 *     }
 * </code></pre>
 *
 * <p>or with this shorthand syntax:</p>
 * <pre><code>
 *     def tree = new FileTreeBuilder()
 *     tree.src {
 *        main {
 *           groovy {
 *              'Foo.groovy'('println "Hello"')
 *           }
 *        }
 *        test {
 *           groovy {
 *              'FooTest.groovy'('class FooTest extends GroovyTestCase {}')
 *           }
 *        }
 *     }
 * </code></pre>
 * @since 2.5.0
 * @author Cédric Champeau
 * @author Simon Buettner
 */
public class FileTreeBuilder extends BuilderSupport {

	/**
	 * Create a new <code>FileTreeBuilder</code> using the current directory as the base directory.
	 */
	public FileTreeBuilder() {
		this(new File("."));
	}

	/**
	 * Create a new <code>FileTreeBuilder</code> for the given base directory.
	 *
	 * @param baseDir base directory for further filesystem activities.
	 */
	public FileTreeBuilder(File baseDir) {
		setCurrent(baseDir);
	}

	public File getBaseDir() {
		return (File) getCurrent();
	}

	@Override
	protected void setParent(Object parent, Object child) {

	}

	@Override
	protected Object createNode(Object name) {
		// Shorthand call to create a directory.
		return dir(String.valueOf(name));
	}

	@Override
	protected Object createNode(Object name, Object value) {
		final File newBase;
		final String fileName = String.valueOf(name);
		if(value instanceof Closure) {
			// Shorthand call to create a directory.
			newBase = dir(fileName, (Closure) value);
		}
		else {
			try {
				// Shorthand call to create a file.
				if(value instanceof byte[]) {
					newBase = file(fileName, (byte[]) value);
				}
				else if(value instanceof File) {
					newBase = file(fileName, (File) value);
				}
				else {
					newBase = file(fileName, String.valueOf(value));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return newBase;
	}

	@Override
	protected Object createNode(Object name, Map attributes) {
		throw new RuntimeException("Not supported.");
	}

	@Override
	protected Object createNode(Object name, Map attributes, Object value) {
		throw new RuntimeException("Not supported.");
	}

	/**
	 * Executes the given specification <code>Closure</code> against this <code>FileTreeBuilder</code>.
	 * @param spec specification of the subdirectory structure
	 * @return the base directory
	 */
	public File call(@DelegatesTo(value = FileTreeBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure spec) {
		Closure clone = (Closure) spec.clone();
		clone.setDelegate(this);
		clone.setResolveStrategy(Closure.DELEGATE_FIRST);
		clone.call();
		return getBaseDir();
	}

	/**
	 * Creates a file with the specified name and the text contents using the system default encoding.
	 * @param name name of the file to be created
	 * @param contents the contents of the file, written using the system default encoding
	 * @return the file being created
	 */
	public File file(String name, CharSequence contents) throws IOException {
		return ResourceGroovyMethods.leftShift(new File(getBaseDir(), name), contents);
	}

	/**
	 * Creates a file with the specified name and the specified binary contents
	 * @param name name of the file to be created
	 * @param contents the contents of the file
	 * @return the file being created
	 */
	public File file(String name, byte[] contents) throws IOException {
		return ResourceGroovyMethods.leftShift(new File(getBaseDir(), name), contents);
	}

	/**
	 * Creates a file with the specified name and the contents from the source file (copy).
	 * @param name name of the file to be created
	 * @param source the source file
	 * @return the file being created
	 */
	public File file(String name, File source) throws IOException {
		// TODO: Avoid using bytes and prefer streaming copy
		return file(name, ResourceGroovyMethods.getBytes(source));
	}

	/**
	 * Creates a new file in the current directory, whose contents is going to be generated in the
	 * closure. The delegate of the closure is the file being created.
	 * @param name name of the file to create
	 * @param spec closure for generating the file contents
	 * @return the created file
	 */
	public File file(String name, @DelegatesTo(value = File.class, strategy = Closure.DELEGATE_FIRST) Closure spec) {
		File file = new File(getBaseDir(), name);
		Closure clone = (Closure) spec.clone();
		clone.setDelegate(file);
		clone.setResolveStrategy(Closure.DELEGATE_FIRST);
		clone.call(file);
		return file;
	}

	/**
	 * Creates a new empty directory
	 * @param name the name of the directory to create
	 * @return the created directory
	 */
	public File dir(String name) {
		File dir = new File(getBaseDir(), String.valueOf(name));
		dir.mkdirs();
		return dir;
	}

	/**
	 * Creates a new directory and allows to specify a subdirectory structure using the closure as a specification
	 * @param name name of the directory to be created
	 * @param spec specification of the subdirectory structure
	 * @return the created directory
	 */
	public File dir(String name, @DelegatesTo(value = FileTreeBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure spec) {
		File oldBase = getBaseDir();
		File newBase = dir(String.valueOf(name));
		try {
			setCurrent(newBase);
			spec.setDelegate(this);
			spec.setResolveStrategy(Closure.DELEGATE_FIRST);
			spec.call();
		} finally {
			setCurrent(oldBase);
		}
		return newBase;
	}

}
