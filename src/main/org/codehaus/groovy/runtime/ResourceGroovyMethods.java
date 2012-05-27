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
package org.codehaus.groovy.runtime;

import groovy.io.EncodingAwareBufferedWriter;
import groovy.io.FileType;
import groovy.io.FileVisitResult;
import groovy.io.GroovyPrintWriter;
import groovy.lang.Closure;
import groovy.lang.MetaClass;
import groovy.lang.Writable;
import groovy.util.CharsetToolkit;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.get;

/**
 * This class defines new groovy methods for Readers, Writers, InputStreams and
 * OutputStreams which appear on normal JDK classes inside the Groovy environment.
 * Static methods are used with the first parameter being the destination class,
 * i.e. <code>public static T eachLine(InputStream self, Closure c)</code>
 * provides a <code>eachLine(Closure c)</code> method for <code>InputStream</code>.
 * <p/>
 * NOTE: While this class contains many 'public' static methods, it is
 * primarily regarded as an internal class (its internal package name
 * suggests this also). We value backwards compatibility of these
 * methods when used within Groovy but value less backwards compatibility
 * at the Java method call level. I.e. future versions of Groovy may
 * remove or move a method call in this file but would normally
 * aim to keep the method available from within Groovy.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author Jeremy Rayner
 * @author Sam Pullara
 * @author Rod Cope
 * @author Guillaume Laforge
 * @author John Wilson
 * @author Hein Meling
 * @author Dierk Koenig
 * @author Pilho Kim
 * @author Marc Guillemot
 * @author Russel Winder
 * @author bing ran
 * @author Jochen Theodorou
 * @author Paul King
 * @author Michael Baehr
 * @author Joachim Baumann
 * @author Alex Tkachman
 * @author Ted Naleid
 * @author Brad Long
 * @author Jim Jagielski
 * @author Rodolfo Velasco
 * @author jeremi Joslin
 * @author Hamlet D'Arcy
 * @author Cedric Champeau
 * @author Tim Yates
 * @author Dinko Srkoc
 */
public class ResourceGroovyMethods extends DefaultGroovyMethodsSupport {

//    private static final Logger LOG = Logger.getLogger(FileIOGroovyMethods.class.getName());

    /**
     * Provide the standard Groovy <code>size()</code> method for <code>File</code>.
     *
     * @param self a file object
     * @return the file's size (length)
     * @since 1.5.0
     */
    public static long size(File self) {
        return self.length();
    }

    /**
     * Create an object output stream for this file.
     *
     * @param file a file
     * @return an object output stream
     * @throws IOException if an IOException occurs.
     * @since 1.5.0
     */
    public static ObjectOutputStream newObjectOutputStream(File file) throws IOException {
        return new ObjectOutputStream(new FileOutputStream(file));
    }

    /**
     * Create a new ObjectOutputStream for this file and then pass it to the
     * closure.  This method ensures the stream is closed after the closure
     * returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.OutputStream, groovy.lang.Closure)
     * @since 1.5.0
     */
    public static <T> T withObjectOutputStream(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withStream(newObjectOutputStream(file), closure);
    }

    /**
     * Create an object input stream for this file.
     *
     * @param file a file
     * @return an object input stream
     * @throws IOException if an IOException occurs.
     * @since 1.5.0
     */
    public static ObjectInputStream newObjectInputStream(File file) throws IOException {
        return new ObjectInputStream(new FileInputStream(file));
    }

    /**
     * Create an object input stream for this file using the given class loader.
     *
     * @param file        a file
     * @param classLoader the class loader to use when loading the class
     * @return an object input stream
     * @throws IOException if an IOException occurs.
     * @since 1.5.0
     */
    public static ObjectInputStream newObjectInputStream(File file, final ClassLoader classLoader) throws IOException {
        return IOGroovyMethods.newObjectInputStream(new FileInputStream(file), classLoader);
    }

    /**
     * Iterates through the given file object by object.
     *
     * @param self    a File
     * @param closure a closure
     * @throws IOException            if an IOException occurs.
     * @throws ClassNotFoundException if the class  is not found.
     * @see IOGroovyMethods#eachObject(java.io.ObjectInputStream, groovy.lang.Closure)
     * @since 1.0
     */
    public static void eachObject(File self, Closure closure) throws IOException, ClassNotFoundException {
        IOGroovyMethods.eachObject(newObjectInputStream(self), closure);
    }

    /**
     * Create a new ObjectInputStream for this file and pass it to the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.InputStream, groovy.lang.Closure)
     * @since 1.5.2
     */
    public static <T> T withObjectInputStream(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withStream(newObjectInputStream(file), closure);
    }

    /**
     * Create a new ObjectInputStream for this file associated with the given class loader and pass it to the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file        a File
     * @param classLoader the class loader to use when loading the class
     * @param closure     a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.InputStream, groovy.lang.Closure)
     * @since 1.5.2
     */
    public static <T> T withObjectInputStream(File file, ClassLoader classLoader, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withStream(newObjectInputStream(file, classLoader), closure);
    }

    /**
     * Iterates through this file line by line.  Each line is passed to the
     * given 1 or 2 arg closure.  The file is read using a reader which
     * is closed before this method returns.
     *
     * @param self    a File
     * @param closure a closure (arg 1 is line, optional arg 2 is line number starting at line 1)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.File, int, groovy.lang.Closure)
     * @since 1.5.5
     */
    public static <T> T eachLine(File self, Closure<T> closure) throws IOException {
        return eachLine(self, 1, closure);
    }

    /**
     * Iterates through this file line by line.  Each line is passed to the
     * given 1 or 2 arg closure.  The file is read using a reader which
     * is closed before this method returns.
     *
     * @param self    a File
     * @param charset opens the file with a specified charset
     * @param closure a closure (arg 1 is line, optional arg 2 is line number starting at line 1)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.File, java.lang.String, int, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T eachLine(File self, String charset, Closure<T> closure) throws IOException {
        return eachLine(self, charset, 1, closure);
    }

    /**
     * Iterates through this file line by line.  Each line is passed
     * to the given 1 or 2 arg closure.  The file is read using a reader
     * which is closed before this method returns.
     *
     * @param self      a File
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure   a closure (arg 1 is line, optional arg 2 is line number)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachLine(java.io.Reader, int, groovy.lang.Closure)
     * @since 1.5.7
     */
    public static <T> T eachLine(File self, int firstLine, Closure<T> closure) throws IOException {
        return IOGroovyMethods.eachLine(newReader(self), firstLine, closure);
    }

    /**
     * Iterates through this file line by line.  Each line is passed
     * to the given 1 or 2 arg closure.  The file is read using a reader
     * which is closed before this method returns.
     *
     * @param self      a File
     * @param charset   opens the file with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure   a closure (arg 1 is line, optional arg 2 is line number)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachLine(java.io.Reader, int, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T eachLine(File self, String charset, int firstLine, Closure<T> closure) throws IOException {
        return IOGroovyMethods.eachLine(newReader(self, charset), firstLine, closure);
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 or 2 arg closure. The stream is closed before this method returns.
     *
     * @param url     a URL to open and read
     * @param closure a closure to apply on each line (arg 1 is line, optional arg 2 is line number starting at line 1)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.net.URL, int, groovy.lang.Closure)
     * @since 1.5.6
     */
    public static <T> T eachLine(URL url, Closure<T> closure) throws IOException {
        return eachLine(url, 1, closure);
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 or 2 arg closure. The stream is closed before this method returns.
     *
     * @param url       a URL to open and read
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure   a closure to apply on each line (arg 1 is line, optional arg 2 is line number)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachLine(java.io.InputStream, int, groovy.lang.Closure)
     * @since 1.5.7
     */
    public static <T> T eachLine(URL url, int firstLine, Closure<T> closure) throws IOException {
        return IOGroovyMethods.eachLine(url.openConnection().getInputStream(), firstLine, closure);
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 or 2 arg closure. The stream is closed before this method returns.
     *
     * @param url     a URL to open and read
     * @param charset opens the stream with a specified charset
     * @param closure a closure to apply on each line (arg 1 is line, optional arg 2 is line number starting at line 1)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.net.URL, java.lang.String, int, groovy.lang.Closure)
     * @since 1.5.6
     */
    public static <T> T eachLine(URL url, String charset, Closure<T> closure) throws IOException {
        return eachLine(url, charset, 1, closure);
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 or 2 arg closure. The stream is closed before this method returns.
     *
     * @param url       a URL to open and read
     * @param charset   opens the stream with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure   a closure to apply on each line (arg 1 is line, optional arg 2 is line number)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachLine(java.io.Reader, int, groovy.lang.Closure)
     * @since 1.5.7
     */
    public static <T> T eachLine(URL url, String charset, int firstLine, Closure<T> closure) throws IOException {
        return IOGroovyMethods.eachLine(newReader(url, charset), firstLine, closure);
    }

    /**
     * Iterates through this file line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the file are closed.
     *
     * @param self    a File
     * @param regex   the delimiting regular expression
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException
     *                     if the regular expression's syntax is invalid
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Closure)
     * @since 1.5.5
     */
    public static <T> T splitEachLine(File self, String regex, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self), regex, closure);
    }

    /**
     * Iterates through this file line by line, splitting each line using
     * the given separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression Pattern.
     * Finally the resources used for processing the file are closed.
     *
     * @param self    a File
     * @param pattern the regular expression Pattern for the delimiter
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T splitEachLine(File self, Pattern pattern, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self), pattern, closure);
    }

    /**
     * Iterates through this file line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the file are closed.
     *
     * @param self    a File
     * @param regex   the delimiting regular expression
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException
     *                     if the regular expression's syntax is invalid
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T splitEachLine(File self, String regex, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self, charset), regex, closure);
    }

    /**
     * Iterates through this file line by line, splitting each line using
     * the given regex separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the file are closed.
     *
     * @param self    a File
     * @param pattern the regular expression Pattern for the delimiter
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T splitEachLine(File self, Pattern pattern, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self, charset), pattern, closure);
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self    a URL to open and read
     * @param regex   the delimiting regular expression
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException
     *                     if the regular expression's syntax is invalid
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T splitEachLine(URL self, String regex, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self), regex, closure);
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self    a URL to open and read
     * @param pattern the regular expression Pattern for the delimiter
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T splitEachLine(URL self, Pattern pattern, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self), pattern, closure);
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self    a URL to open and read
     * @param regex   the delimiting regular expression
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException
     *                     if the regular expression's syntax is invalid
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T splitEachLine(URL self, String regex, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self, charset), regex, closure);
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self    a URL to open and read
     * @param pattern the regular expression Pattern for the delimiter
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static <T> T splitEachLine(URL self, Pattern pattern, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.splitEachLine(newReader(self, charset), pattern, closure);
    }

    /**
     * Reads the file into a list of Strings, with one item for each line.
     *
     * @param file a File
     * @return a List of lines
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#readLines(java.io.Reader)
     * @since 1.0
     */
    public static List<String> readLines(File file) throws IOException {
        return IOGroovyMethods.readLines(newReader(file));
    }

    /**
     * Reads the file into a list of Strings, with one item for each line.
     *
     * @param file    a File
     * @param charset opens the file with a specified charset
     * @return a List of lines
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#readLines(java.io.Reader)
     * @since 1.6.8
     */
    public static List<String> readLines(File file, String charset) throws IOException {
        return IOGroovyMethods.readLines(newReader(file, charset));
    }

    /**
     * Reads the URL contents into a list, with one element for each line.
     *
     * @param self a URL
     * @return a List of lines
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#readLines(java.io.Reader)
     * @since 1.6.8
     */
    public static List<String> readLines(URL self) throws IOException {
        return IOGroovyMethods.readLines(newReader(self));
    }

    /**
     * Reads the URL contents into a list, with one element for each line.
     *
     * @param self    a URL
     * @param charset opens the URL with a specified charset
     * @return a List of lines
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#readLines(java.io.Reader)
     * @since 1.6.8
     */
    public static List<String> readLines(URL self, String charset) throws IOException {
        return IOGroovyMethods.readLines(newReader(self, charset));
    }

    /**
     * Read the content of the File using the specified encoding and return it
     * as a String.
     *
     * @param file    the file whose content we want to read
     * @param charset the charset used to read the content of the file
     * @return a String containing the content of the file
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static String getText(File file, String charset) throws IOException {
        return IOGroovyMethods.getText(newReader(file, charset));
    }

    /**
     * Read the content of the File and returns it as a String.
     *
     * @param file the file whose content we want to read
     * @return a String containing the content of the file
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static String getText(File file) throws IOException {
        return IOGroovyMethods.getText(newReader(file));
    }

    /**
     * Read the content of this URL and returns it as a String.
     *
     * @param url URL to read content from
     * @return the text from that URL
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static String getText(URL url) throws IOException {
        return getText(url, CharsetToolkit.getDefaultSystemCharset().toString());
    }

    /**
     * Read the content of this URL and returns it as a String.
     *
     * @param url        URL to read content from
     * @param parameters connection parameters
     * @return the text from that URL
     * @throws IOException if an IOException occurs.
     * @since 1.8.1
     */
    public static String getText(URL url, Map parameters) throws IOException {
        return getText(url, parameters, CharsetToolkit.getDefaultSystemCharset().toString());
    }

    /**
     * Read the data from this URL and return it as a String.  The connection
     * stream is closed before this method returns.
     *
     * @param url     URL to read content from
     * @param charset opens the stream with a specified charset
     * @return the text from that URL
     * @throws IOException if an IOException occurs.
     * @see java.net.URLConnection#getInputStream()
     * @since 1.0
     */
    public static String getText(URL url, String charset) throws IOException {
        BufferedReader reader = newReader(url, charset);
        return IOGroovyMethods.getText(reader);
    }

    /**
     * Read the data from this URL and return it as a String.  The connection
     * stream is closed before this method returns.
     *
     * @param url        URL to read content from
     * @param parameters connection parameters
     * @param charset    opens the stream with a specified charset
     * @return the text from that URL
     * @throws IOException if an IOException occurs.
     * @see java.net.URLConnection#getInputStream()
     * @since 1.8.1
     */
    public static String getText(URL url, Map parameters, String charset) throws IOException {
        BufferedReader reader = newReader(url, parameters, charset);
        return IOGroovyMethods.getText(reader);
    }

    /**
     * Read the content of the File and returns it as a byte[].
     *
     * @param file the file whose content we want to read
     * @return a String containing the content of the file
     * @throws IOException if an IOException occurs.
     * @since 1.7.1
     */
    public static byte[] getBytes(File file) throws IOException {
        return IOGroovyMethods.getBytes(new FileInputStream(file));
    }

    /**
     * Read the content of this URL and returns it as a byte[].
     *
     * @param url URL to read content from
     * @return the byte[] from that URL
     * @throws IOException if an IOException occurs.
     * @since 1.7.1
     */
    public static byte[] getBytes(URL url) throws IOException {
        return IOGroovyMethods.getBytes(url.openConnection().getInputStream());
    }

    /**
     * Write the bytes from the byte array to the File.
     *
     * @param file  the file to write to
     * @param bytes the byte[] to write to the file
     * @throws IOException if an IOException occurs.
     * @since 1.7.1
     */
    public static void setBytes(File file, byte[] bytes) throws IOException {
        IOGroovyMethods.setBytes(new FileOutputStream(file), bytes);
    }

    /**
     * Write the text to the File.
     *
     * @param file a File
     * @param text the text to write to the File
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static void write(File file, String text) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = newWriter(file);
            writer.write(text);
            writer.flush();

            Writer temp = writer;
            writer = null;
            temp.close();
        } finally {
            closeWithWarning(writer);
        }
    }

    /**
     * Synonym for write(text) allowing file.text = 'foo'.
     *
     * @param file a File
     * @param text the text to write to the File
     * @throws IOException if an IOException occurs.
     * @see #write(java.io.File, java.lang.String)
     * @since 1.5.1
     */
    public static void setText(File file, String text) throws IOException {
        write(file, text);
    }

    /**
     * Synonym for write(text, charset) allowing:
     * <pre>
     * myFile.setText('some text', charset)
     * </pre>
     * or with some help from <code>ExpandoMetaClass</code>, you could do something like:
     * <pre>
     * myFile.metaClass.setText = { String s -> delegate.setText(s, 'UTF-8') }
     * myfile.text = 'some text'
     * </pre>
     *
     * @param file    A File
     * @param charset The charset used when writing to the file
     * @param text    The text to write to the File
     * @throws IOException if an IOException occurs.
     * @see #write(java.io.File, java.lang.String, java.lang.String)
     * @since 1.7.3
     */
    public static void setText(File file, String text, String charset) throws IOException {
        write(file, text, charset);
    }

    /**
     * Write the text to the File.
     *
     * @param file a File
     * @param text the text to write to the File
     * @return the original file
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static File leftShift(File file, Object text) throws IOException {
        append(file, text);
        return file;
    }

    /**
     * Write bytes to a File.
     *
     * @param file  a File
     * @param bytes the byte array to append to the end of the File
     * @return the original file
     * @throws IOException if an IOException occurs.
     * @since 1.5.0
     */
    public static File leftShift(File file, byte[] bytes) throws IOException {
        append(file, bytes);
        return file;
    }

    /**
     * Append binary data to the file.  See {@link #append(java.io.File, java.io.InputStream)}
     *
     * @param file a File
     * @param data an InputStream of data to write to the file
     * @return the file
     * @throws IOException if an IOException occurs.
     * @since 1.5.0
     */
    public static File leftShift(File file, InputStream data) throws IOException {
        append(file, data);
        return file;
    }

    /**
     * Write the text to the File, using the specified encoding.
     *
     * @param file    a File
     * @param text    the text to write to the File
     * @param charset the charset used
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static void write(File file, String text, String charset) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = newWriter(file, charset);
            writer.write(text);
            writer.flush();

            Writer temp = writer;
            writer = null;
            temp.close();
        } finally {
            closeWithWarning(writer);
        }
    }

    /**
     * Append the text at the end of the File.
     *
     * @param file a File
     * @param text the text to append at the end of the File
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static void append(File file, Object text) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = newWriter(file, true);
            InvokerHelper.write(writer, text);
            writer.flush();

            Writer temp = writer;
            writer = null;
            temp.close();
        } finally {
            closeWithWarning(writer);
        }
    }

    /**
     * Append bytes to the end of a File.
     *
     * @param file  a File
     * @param bytes the byte array to append to the end of the File
     * @throws IOException if an IOException occurs.
     * @since 1.5.1
     */
    public static void append(File file, byte[] bytes) throws IOException {
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(file, true));
            stream.write(bytes, 0, bytes.length);
            stream.flush();

            OutputStream temp = stream;
            stream = null;
            temp.close();
        } finally {
            closeWithWarning(stream);
        }
    }

    /**
     * Append binary data to the file.  It <strong>will not</strong> be
     * interpreted as text.
     *
     * @param self   a File
     * @param stream stream to read data from.
     * @throws IOException if an IOException occurs.
     * @since 1.5.0
     */
    public static void append(File self, InputStream stream) throws IOException {
        OutputStream out = new FileOutputStream(self, true);
        try {
            IOGroovyMethods.leftShift(out, stream);
        } finally {
            closeWithWarning(out);
        }
    }

    /**
     * Append the text at the end of the File, using a specified encoding.
     *
     * @param file    a File
     * @param text    the text to append at the end of the File
     * @param charset the charset used
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static void append(File file, Object text, String charset) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = newWriter(file, charset, true);
            InvokerHelper.write(writer, text);
            writer.flush();

            Writer temp = writer;
            writer = null;
            temp.close();
        } finally {
            closeWithWarning(writer);
        }
    }

    /**
     * This method is used to throw useful exceptions when the eachFile* and eachDir closure methods
     * are used incorrectly.
     *
     * @param dir The directory to check
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @since 1.0
     */
    private static void checkDir(File dir) throws FileNotFoundException, IllegalArgumentException {
        if (!dir.exists())
            throw new FileNotFoundException(dir.getAbsolutePath());
        if (!dir.isDirectory())
            throw new IllegalArgumentException("The provided File object is not a directory: " + dir.getAbsolutePath());
    }

    /**
     * Invokes the closure for each 'child' file in this 'parent' folder/directory.
     * Both regular files and subfolders/subdirectories can be processed depending
     * on the fileType enum value.
     *
     * @param self     a file object
     * @param fileType if normal files or directories or both should be processed
     * @param closure  the closure to invoke
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @since 1.7.1
     */
    public static void eachFile(final File self, final FileType fileType, final Closure closure)
            throws FileNotFoundException, IllegalArgumentException {
        checkDir(self);
        final File[] files = self.listFiles();
        // null check because of http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4803836
        if (files == null) return;
        for (File file : files) {
            if (fileType == FileType.ANY ||
                    (fileType != FileType.FILES && file.isDirectory()) ||
                    (fileType != FileType.DIRECTORIES && file.isFile())) {
                closure.call(file);
            }
        }
    }

    /**
     * Invokes the closure for each 'child' file in this 'parent' folder/directory.
     * Both regular files and subfolders/subdirectories are processed.
     *
     * @param self    a File (that happens to be a folder/directory)
     * @param closure a closure (first parameter is the 'child' file)
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see java.io.File#listFiles()
     * @see #eachFile(java.io.File, groovy.io.FileType, groovy.lang.Closure)
     * @since 1.5.0
     */
    public static void eachFile(final File self, final Closure closure) throws FileNotFoundException, IllegalArgumentException {
        eachFile(self, FileType.ANY, closure);
    }

    /**
     * Invokes the closure for each subdirectory in this directory,
     * ignoring regular files.
     *
     * @param self    a File (that happens to be a folder/directory)
     * @param closure a closure (first parameter is the subdirectory file)
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see java.io.File#listFiles()
     * @see #eachFile(java.io.File, groovy.io.FileType, groovy.lang.Closure)
     * @since 1.0
     */
    public static void eachDir(File self, Closure closure) throws FileNotFoundException, IllegalArgumentException {
        eachFile(self, FileType.DIRECTORIES, closure);
    }

    /**
     * Invokes the closure for each descendant file in this directory.
     * Sub-directories are recursively searched in a depth-first fashion.
     * Both regular files and subdirectories may be passed to the closure
     * depending on the value of fileType.
     *
     * @param self     a file object
     * @param fileType if normal files or directories or both should be processed
     * @param closure  the closure to invoke on each file
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @since 1.7.1
     */
    public static void eachFileRecurse(final File self, final FileType fileType, final Closure closure)
            throws FileNotFoundException, IllegalArgumentException {
        checkDir(self);
        final File[] files = self.listFiles();
        // null check because of http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4803836
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if (fileType != FileType.FILES) closure.call(file);
                eachFileRecurse(file, fileType, closure);
            } else if (fileType != FileType.DIRECTORIES) {
                closure.call(file);
            }
        }
    }

    /**
     * Invokes <code>closure</code> for each descendant file in this directory tree.
     * Sub-directories are recursively traversed as found.
     * The traversal can be adapted by providing various options in the <code>options</code> Map according
     * to the following keys:<dl>
     * <dt>type</dt><dd>A {@link groovy.io.FileType} enum to determine if normal files or directories or both are processed</dd>
     * <dt>preDir</dt><dd>A {@link groovy.lang.Closure} run before each directory is processed and optionally returning a {@link groovy.io.FileVisitResult} value
     * which can be used to control subsequent processing.</dd>
     * <dt>preRoot</dt><dd>A boolean indicating that the 'preDir' closure should be applied at the root level</dd>
     * <dt>postDir</dt><dd>A {@link groovy.lang.Closure} run after each directory is processed and optionally returning a {@link groovy.io.FileVisitResult} value
     * which can be used to control subsequent processing.</dd>
     * <dt>postRoot</dt><dd>A boolean indicating that the 'postDir' closure should be applied at the root level</dd>
     * <dt>visitRoot</dt><dd>A boolean indicating that the given closure should be applied for the root dir
     * (not applicable if the 'type' is set to {@link groovy.io.FileType#FILES})</dd>
     * <dt>maxDepth</dt><dd>The maximum number of directory levels when recursing
     * (default is -1 which means infinite, set to 0 for no recursion)</dd>
     * <dt>filter</dt><dd>A filter to perform on traversed files/directories (using the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method). If set,
     * only files/dirs which match are candidates for visiting.</dd>
     * <dt>nameFilter</dt><dd>A filter to perform on the name of traversed files/directories (using the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method). If set,
     * only files/dirs which match are candidates for visiting. (Must not be set if 'filter' is set)</dd>
     * <dt>excludeFilter</dt><dd>A filter to perform on traversed files/directories (using the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method).
     * If set, any candidates which match won't be visited.</dd>
     * <dt>excludeNameFilter</dt><dd>A filter to perform on the names of traversed files/directories (using the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method).
     * If set, any candidates which match won't be visited. (Must not be set if 'excludeFilter' is set)</dd>
     * <dt>sort</dt><dd>A {@link groovy.lang.Closure} which if set causes the files and subdirectories for each directory to be processed in sorted order.
     * Note that even when processing only files, the order of visited subdirectories will be affected by this parameter.</dd>
     * </dl>
     * This example prints out file counts and size aggregates for groovy source files within a directory tree:
     * <pre>
     * def totalSize = 0
     * def count = 0
     * def sortByTypeThenName = { a, b ->
     *     a.isFile() != b.isFile() ? a.isFile() <=> b.isFile() : a.name <=> b.name
     * }
     * rootDir.traverse(
     *         type         : FILES,
     *         nameFilter   : ~/.*\.groovy/,
     *         preDir       : { if (it.name == '.svn') return SKIP_SUBTREE },
     *         postDir      : { println "Found $count files in $it.name totalling $totalSize bytes"
     *                         totalSize = 0; count = 0 },
     *         postRoot     : true
     *         sort         : sortByTypeThenName
     * ) {it -> totalSize += it.size(); count++ }
     * </pre>
     *
     * @param self    a File
     * @param options a Map of options to alter the traversal behavior
     * @param closure the Closure to invoke on each file/directory and optionally returning a {@link groovy.io.FileVisitResult} value
     *                which can be used to control subsequent processing
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory or illegal filter combinations are supplied
     * @see DefaultGroovyMethods#sort(java.util.Collection, groovy.lang.Closure)
     * @see groovy.io.FileVisitResult
     * @see groovy.io.FileType
     * @since 1.7.1
     */
    public static void traverse(final File self, final Map<String, Object> options, final Closure closure)
            throws FileNotFoundException, IllegalArgumentException {
        Number maxDepthNumber = DefaultGroovyMethods.asType(options.remove("maxDepth"), Number.class);
        int maxDepth = maxDepthNumber == null ? -1 : maxDepthNumber.intValue();
        Boolean visitRoot = DefaultGroovyMethods.asType(get(options, "visitRoot", false), Boolean.class);
        Boolean preRoot = DefaultGroovyMethods.asType(get(options, "preRoot", false), Boolean.class);
        Boolean postRoot = DefaultGroovyMethods.asType(get(options, "postRoot", false), Boolean.class);
        final Closure pre = (Closure) options.get("preDir");
        final Closure post = (Closure) options.get("postDir");
        final FileType type = (FileType) options.get("type");
        final Object filter = options.get("filter");
        final Object nameFilter = options.get("nameFilter");
        final Object excludeFilter = options.get("excludeFilter");
        final Object excludeNameFilter = options.get("excludeNameFilter");
        Object preResult = null;
        if (preRoot && pre != null) {
            preResult = pre.call(self);
        }
        if (preResult == FileVisitResult.TERMINATE ||
                preResult == FileVisitResult.SKIP_SUBTREE) return;

        FileVisitResult terminated = traverse(self, options, closure, maxDepth);

        if (type != FileType.FILES && visitRoot) {
            if (closure != null && notFiltered(self, filter, nameFilter, excludeFilter, excludeNameFilter)) {
                Object closureResult = closure.call(self);
                if (closureResult == FileVisitResult.TERMINATE) return;
            }
        }

        if (postRoot && post != null && terminated != FileVisitResult.TERMINATE) post.call(self);
    }

    private static boolean notFiltered(File file, Object filter, Object nameFilter, Object excludeFilter, Object excludeNameFilter) {
        if (filter == null && nameFilter == null && excludeFilter == null && excludeNameFilter == null) return true;
        if (filter != null && nameFilter != null)
            throw new IllegalArgumentException("Can't set both 'filter' and 'nameFilter'");
        if (excludeFilter != null && excludeNameFilter != null)
            throw new IllegalArgumentException("Can't set both 'excludeFilter' and 'excludeNameFilter'");
        Object filterToUse = null;
        Object filterParam = null;
        if (filter != null) {
            filterToUse = filter;
            filterParam = file;
        } else if (nameFilter != null) {
            filterToUse = nameFilter;
            filterParam = file.getName();
        }
        Object excludeFilterToUse = null;
        Object excludeParam = null;
        if (excludeFilter != null) {
            excludeFilterToUse = excludeFilter;
            excludeParam = file;
        } else if (excludeNameFilter != null) {
            excludeFilterToUse = excludeNameFilter;
            excludeParam = file.getName();
        }
        final MetaClass filterMC = filterToUse == null ? null : InvokerHelper.getMetaClass(filterToUse);
        final MetaClass excludeMC = excludeFilterToUse == null ? null : InvokerHelper.getMetaClass(excludeFilterToUse);
        boolean included = filterToUse == null || DefaultTypeTransformation.castToBoolean(filterMC.invokeMethod(filterToUse, "isCase", filterParam));
        boolean excluded = excludeFilterToUse != null && DefaultTypeTransformation.castToBoolean(excludeMC.invokeMethod(excludeFilterToUse, "isCase", excludeParam));
        return included && !excluded;
    }

    /**
     * Invokes the closure for each descendant file in this directory tree.
     * Sub-directories are recursively traversed in a depth-first fashion.
     * Convenience method for {@link #traverse(java.io.File, java.util.Map, groovy.lang.Closure)} when
     * no options to alter the traversal behavior are required.
     *
     * @param self    a File
     * @param closure the Closure to invoke on each file/directory and optionally returning a {@link groovy.io.FileVisitResult} value
     *                which can be used to control subsequent processing
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see #traverse(java.io.File, java.util.Map, groovy.lang.Closure)
     * @since 1.7.1
     */
    public static void traverse(final File self, final Closure closure)
            throws FileNotFoundException, IllegalArgumentException {
        traverse(self, new HashMap<String, Object>(), closure);
    }

    /**
     * Invokes the closure specified with key 'visit' in the options Map
     * for each descendant file in this directory tree. Convenience method
     * for {@link #traverse(java.io.File, java.util.Map, groovy.lang.Closure)} allowing the 'visit' closure
     * to be included in the options Map rather than as a parameter.
     *
     * @param self    a File
     * @param options a Map of options to alter the traversal behavior
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory or illegal filter combinations are supplied
     * @see #traverse(java.io.File, java.util.Map, groovy.lang.Closure)
     * @since 1.7.1
     */
    public static void traverse(final File self, final Map<String, Object> options)
            throws FileNotFoundException, IllegalArgumentException {
        final Closure visit = (Closure) options.remove("visit");
        traverse(self, options, visit);
    }

    private static FileVisitResult traverse(final File self, final Map<String, Object> options, final Closure closure, final int maxDepth)
            throws FileNotFoundException, IllegalArgumentException {
        checkDir(self);
        final Closure pre = (Closure) options.get("preDir");
        final Closure post = (Closure) options.get("postDir");
        final FileType type = (FileType) options.get("type");
        final Object filter = options.get("filter");
        final Object nameFilter = options.get("nameFilter");
        final Object excludeFilter = options.get("excludeFilter");
        final Object excludeNameFilter = options.get("excludeNameFilter");
        final Closure sort = (Closure) options.get("sort");

        final File[] origFiles = self.listFiles();
        // null check because of http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4803836
        if (origFiles != null) {
            List<File> files = Arrays.asList(origFiles);
            if (sort != null) files = DefaultGroovyMethods.sort(files, sort);
            for (File file : files) {
                if (file.isDirectory()) {
                    if (type != FileType.FILES) {
                        if (closure != null && notFiltered(file, filter, nameFilter, excludeFilter, excludeNameFilter)) {
                            Object closureResult = closure.call(file);
                            if (closureResult == FileVisitResult.SKIP_SIBLINGS) break;
                            if (closureResult == FileVisitResult.TERMINATE) return FileVisitResult.TERMINATE;
                        }
                    }
                    if (maxDepth != 0) {
                        Object preResult = null;
                        if (pre != null) {
                            preResult = pre.call(file);
                        }
                        if (preResult == FileVisitResult.SKIP_SIBLINGS) break;
                        if (preResult == FileVisitResult.TERMINATE) return FileVisitResult.TERMINATE;
                        if (preResult != FileVisitResult.SKIP_SUBTREE) {
                            FileVisitResult terminated = traverse(file, options, closure, maxDepth - 1);
                            if (terminated == FileVisitResult.TERMINATE) return terminated;
                        }
                        Object postResult = null;
                        if (post != null) {
                            postResult = post.call(file);
                        }
                        if (postResult == FileVisitResult.SKIP_SIBLINGS) break;
                        if (postResult == FileVisitResult.TERMINATE) return FileVisitResult.TERMINATE;
                    }
                } else if (type != FileType.DIRECTORIES) {
                    if (closure != null && notFiltered(file, filter, nameFilter, excludeFilter, excludeNameFilter)) {
                        Object closureResult = closure.call(file);
                        if (closureResult == FileVisitResult.SKIP_SIBLINGS) break;
                        if (closureResult == FileVisitResult.TERMINATE) return FileVisitResult.TERMINATE;
                    }
                }
            }
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Invokes the closure for each descendant file in this directory.
     * Sub-directories are recursively searched in a depth-first fashion.
     * Both regular files and subdirectories are passed to the closure.
     *
     * @param self    a File
     * @param closure a closure
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see #eachFileRecurse(java.io.File, groovy.io.FileType, groovy.lang.Closure)
     * @since 1.0
     */
    public static void eachFileRecurse(File self, Closure closure) throws FileNotFoundException, IllegalArgumentException {
        eachFileRecurse(self, FileType.ANY, closure);
    }

    /**
     * Invokes the closure for each descendant directory of this directory.
     * Sub-directories are recursively searched in a depth-first fashion.
     * Only subdirectories are passed to the closure; regular files are ignored.
     *
     * @param self    a directory
     * @param closure a closure
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see #eachFileRecurse(java.io.File, groovy.io.FileType, groovy.lang.Closure)
     * @since 1.5.0
     */
    public static void eachDirRecurse(final File self, final Closure closure) throws FileNotFoundException, IllegalArgumentException {
        eachFileRecurse(self, FileType.DIRECTORIES, closure);
    }

    /**
     * Invokes the closure for each file whose name (file.name) matches the given nameFilter in the given directory
     * - calling the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method to determine if a match occurs.  This method can be used
     * with different kinds of filters like regular expressions, classes, ranges etc.
     * Both regular files and subdirectories may be candidates for matching depending
     * on the value of fileType.
     * <pre>
     * // collect names of files in baseDir matching supplied regex pattern
     * import static groovy.io.FileType.*
     * def names = []
     * baseDir.eachFileMatch FILES, ~/foo\d\.txt/, { names << it.name }
     * assert names == ['foo1.txt', 'foo2.txt']
     *
     * // remove all *.bak files in baseDir
     * baseDir.eachFileMatch FILES, ~/.*\.bak/, { File bak -> bak.delete() }
     *
     * // print out files > 4K in size from baseDir
     * baseDir.eachFileMatch FILES, { new File(baseDir, it).size() > 4096 }, { println "$it.name ${it.size()}" }
     * </pre>
     *
     * @param self       a file
     * @param fileType   whether normal files or directories or both should be processed
     * @param nameFilter the filter to perform on the name of the file/directory (using the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method)
     * @param closure    the closure to invoke
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @since 1.7.1
     */
    public static void eachFileMatch(final File self, final FileType fileType, final Object nameFilter, final Closure closure)
            throws FileNotFoundException, IllegalArgumentException {
        checkDir(self);
        final File[] files = self.listFiles();
        // null check because of http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4803836
        if (files == null) return;
        final MetaClass metaClass = InvokerHelper.getMetaClass(nameFilter);
        for (final File currentFile : files) {
            if ((fileType != FileType.FILES && currentFile.isDirectory()) ||
                    (fileType != FileType.DIRECTORIES && currentFile.isFile())) {
                if (DefaultTypeTransformation.castToBoolean(metaClass.invokeMethod(nameFilter, "isCase", currentFile.getName())))
                    closure.call(currentFile);
            }
        }
    }

    /**
     * Invokes the closure for each file whose name (file.name) matches the given nameFilter in the given directory
     * - calling the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method to determine if a match occurs.  This method can be used
     * with different kinds of filters like regular expressions, classes, ranges etc.
     * Both regular files and subdirectories are matched.
     *
     * @param self       a file
     * @param nameFilter the nameFilter to perform on the name of the file (using the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method)
     * @param closure    the closure to invoke
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see #eachFileMatch(java.io.File, groovy.io.FileType, java.lang.Object, groovy.lang.Closure)
     * @since 1.5.0
     */
    public static void eachFileMatch(final File self, final Object nameFilter, final Closure closure)
            throws FileNotFoundException, IllegalArgumentException {
        eachFileMatch(self, FileType.ANY, nameFilter, closure);
    }

    /**
     * Invokes the closure for each subdirectory whose name (dir.name) matches the given nameFilter in the given directory
     * - calling the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method to determine if a match occurs.  This method can be used
     * with different kinds of filters like regular expressions, classes, ranges etc.
     * Only subdirectories are matched; regular files are ignored.
     *
     * @param self       a file
     * @param nameFilter the nameFilter to perform on the name of the directory (using the {@link DefaultGroovyMethods#isCase(java.lang.Object, java.lang.Object)} method)
     * @param closure    the closure to invoke
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see #eachFileMatch(java.io.File, groovy.io.FileType, java.lang.Object, groovy.lang.Closure)
     * @since 1.5.0
     */
    public static void eachDirMatch(final File self, final Object nameFilter, final Closure closure) throws FileNotFoundException, IllegalArgumentException {
        eachFileMatch(self, FileType.DIRECTORIES, nameFilter, closure);
    }

    /**
     * Deletes a directory with all contained files and subdirectories.
     * <p>The method returns
     * <ul>
     * <li>true, when deletion was successful</li>
     * <li>true, when it is called for a non existing directory</li>
     * <li>false, when it is called for a file which isn't a directory</li>
     * <li>false, when directory couldn't be deleted</li>
     * </ul>
     * </p>
     *
     * @param self a File
     * @return true if the file doesn't exist or deletion was successful
     * @since 1.6.0
     */
    public static boolean deleteDir(final File self) {
        if (!self.exists())
            return true;
        if (!self.isDirectory())
            return false;

        File[] files = self.listFiles();
        if (files == null)
            // couldn't access files
            return false;

        // delete contained files
        boolean result = true;
        for (File file : files) {
            if (file.isDirectory()) {
                if (!deleteDir(file))
                    result = false;
            } else {
                if (!file.delete())
                    result = false;
            }
        }

        // now delete directory itself
        if (!self.delete())
            result = false;

        return result;
    }

    /**
     * Renames the file. It's a shortcut for {@link java.io.File#renameTo(File)}
     *
     * @param self        a File
     * @param newPathName The new pathname for the named file
     * @return <code>true</code> if and only if the renaming succeeded;
     *         <code>false</code> otherwise
     * @since 1.7.4
     */
    public static boolean renameTo(final File self, String newPathName) {
        return self.renameTo(new File(newPathName));
    }

    /**
     * Converts this File to a {@link groovy.lang.Writable}.
     *
     * @param file a File
     * @return a File which wraps the input file and which implements Writable
     * @since 1.0
     */
    public static File asWritable(File file) {
        return new WritableFile(file);
    }

    /**
     * Converts this File to a {@link groovy.lang.Writable} or delegates to default
     * {@link DefaultGroovyMethods#asType(java.lang.Object, java.lang.Class)}.
     *
     * @param f a File
     * @param c the desired class
     * @return the converted object
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    public static <T> T asType(File f, Class<T> c) {
        if (c == Writable.class) {
            return (T) asWritable(f);
        }
        return DefaultGroovyMethods.asType((Object) f, c);
    }

    /**
     * Allows a file to return a Writable implementation that can output itself
     * to a Writer stream.
     *
     * @param file     a File
     * @param encoding the encoding to be used when reading the file's contents
     * @return File which wraps the input file and which implements Writable
     * @since 1.0
     */
    public static File asWritable(File file, String encoding) {
        return new WritableFile(file, encoding);
    }

    /**
     * Create a buffered reader for this file.
     *
     * @param file a File
     * @return a BufferedReader
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static BufferedReader newReader(File file) throws IOException {
        CharsetToolkit toolkit = new CharsetToolkit(file);
        return toolkit.getReader();
    }

    /**
     * Create a buffered reader for this file, using the specified
     * charset as the encoding.
     *
     * @param file    a File
     * @param charset the charset for this File
     * @return a BufferedReader
     * @throws FileNotFoundException        if the File was not found
     * @throws UnsupportedEncodingException if the encoding specified is not supported
     * @since 1.0
     */
    public static BufferedReader newReader(File file, String charset)
            throws FileNotFoundException, UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
    }

    /**
     * Create a new BufferedReader for this file and then
     * passes it into the closure, ensuring the reader is closed after the
     * closure returns.
     *
     * @param file    a file object
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withReader(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withReader(newReader(file), closure);
    }

    /**
     * Create a new BufferedReader for this file using the specified charset and then
     * passes it into the closure, ensuring the reader is closed after the
     * closure returns.
     *
     * @param file    a file object
     * @param charset the charset for this input stream
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.6.0
     */
    public static <T> T withReader(File file, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withReader(newReader(file, charset), closure);
    }

    /**
     * Create a buffered output stream for this file.
     *
     * @param file a file object
     * @return the created OutputStream
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static BufferedOutputStream newOutputStream(File file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file));
    }

    /**
     * Creates a new data output stream for this file.
     *
     * @param file a file object
     * @return the created DataOutputStream
     * @throws IOException if an IOException occurs.
     * @since 1.5.0
     */
    public static DataOutputStream newDataOutputStream(File file) throws IOException {
        return new DataOutputStream(new FileOutputStream(file));
    }

    /**
     * Creates a new OutputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.OutputStream, groovy.lang.Closure)
     * @since 1.5.2
     */
    public static Object withOutputStream(File file, Closure closure) throws IOException {
        return IOGroovyMethods.withStream(newOutputStream(file), closure);
    }

    /**
     * Create a new InputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.InputStream, groovy.lang.Closure)
     * @since 1.5.2
     */
    public static Object withInputStream(File file, Closure closure) throws IOException {
        return IOGroovyMethods.withStream(newInputStream(file), closure);
    }

    /**
     * Creates a new InputStream for this URL and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param url     a URL
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.InputStream, groovy.lang.Closure)
     * @since 1.5.2
     */
    public static <T> T withInputStream(URL url, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withStream(newInputStream(url), closure);
    }

    /**
     * Create a new DataOutputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.OutputStream, groovy.lang.Closure)
     * @since 1.5.2
     */
    public static <T> T withDataOutputStream(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withStream(newDataOutputStream(file), closure);
    }

    /**
     * Create a new DataInputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#withStream(java.io.InputStream, groovy.lang.Closure)
     * @since 1.5.2
     */
    public static <T> T withDataInputStream(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withStream(newDataInputStream(file), closure);
    }

    /**
     * Create a buffered writer for this file.
     *
     * @param file a File
     * @return a BufferedWriter
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static BufferedWriter newWriter(File file) throws IOException {
        return new BufferedWriter(new FileWriter(file));
    }

    /**
     * Creates a buffered writer for this file, optionally appending to the
     * existing file content.
     *
     * @param file   a File
     * @param append true if data should be appended to the file
     * @return a BufferedWriter
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static BufferedWriter newWriter(File file, boolean append) throws IOException {
        return new BufferedWriter(new FileWriter(file, append));
    }

    /**
     * Helper method to create a buffered writer for a file.  If the given
     * charset is "UTF-16BE" or "UTF-16LE", the requisite byte order mark is
     * written to the stream before the writer is returned.
     *
     * @param file    a File
     * @param charset the name of the encoding used to write in this file
     * @param append  true if in append mode
     * @return a BufferedWriter
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static BufferedWriter newWriter(File file, String charset, boolean append) throws IOException {
        if (append) {
            return new EncodingAwareBufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset));
        } else {
            // first write the Byte Order Mark for Unicode encodings
            FileOutputStream stream = new FileOutputStream(file);
            if ("UTF-16BE".equals(charset)) {
                writeUtf16Bom(stream, true);
            } else if ("UTF-16LE".equals(charset)) {
                writeUtf16Bom(stream, false);
            }
            return new EncodingAwareBufferedWriter(new OutputStreamWriter(stream, charset));
        }
    }

    /**
     * Creates a buffered writer for this file, writing data using the given
     * encoding.
     *
     * @param file    a File
     * @param charset the name of the encoding used to write in this file
     * @return a BufferedWriter
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static BufferedWriter newWriter(File file, String charset) throws IOException {
        return newWriter(file, charset, false);
    }

    /**
     * Write a Byte Order Mark at the beginning of the file
     *
     * @param stream    the FileOutputStream to write the BOM to
     * @param bigEndian true if UTF 16 Big Endian or false if Low Endian
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    private static void writeUtf16Bom(FileOutputStream stream, boolean bigEndian) throws IOException {
        if (bigEndian) {
            stream.write(-2);
            stream.write(-1);
        } else {
            stream.write(-1);
            stream.write(-2);
        }
    }

    /**
     * Creates a new BufferedWriter for this file, passes it to the closure, and
     * ensures the stream is flushed and closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withWriter(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withWriter(newWriter(file), closure);
    }

    /**
     * Creates a new BufferedWriter for this file, passes it to the closure, and
     * ensures the stream is flushed and closed after the closure returns.
     * The writer will use the given charset encoding.
     *
     * @param file    a File
     * @param charset the charset used
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withWriter(File file, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withWriter(newWriter(file, charset), closure);
    }

    /**
     * Create a new BufferedWriter which will append to this
     * file.  The writer is passed to the closure and will be closed before
     * this method returns.
     *
     * @param file    a File
     * @param charset the charset used
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withWriterAppend(File file, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withWriter(newWriter(file, charset, true), closure);
    }

    /**
     * Create a new BufferedWriter for this file in append mode.  The writer
     * is passed to the closure and is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withWriterAppend(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withWriter(newWriter(file, true), closure);
    }

    /**
     * Create a new PrintWriter for this file.
     *
     * @param file a File
     * @return the created PrintWriter
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static PrintWriter newPrintWriter(File file) throws IOException {
        return new GroovyPrintWriter(newWriter(file));
    }

    /**
     * Create a new PrintWriter for this file, using specified
     * charset.
     *
     * @param file    a File
     * @param charset the charset
     * @return a PrintWriter
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static PrintWriter newPrintWriter(File file, String charset) throws IOException {
        return new GroovyPrintWriter(newWriter(file, charset));
    }

    /**
     * Create a new PrintWriter for this file which is then
     * passed it into the given closure.  This method ensures its the writer
     * is closed after the closure returns.
     *
     * @param file    a File
     * @param closure the closure to invoke with the PrintWriter
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withPrintWriter(File file, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withWriter(newPrintWriter(file), closure);
    }

    /**
     * Create a new PrintWriter with a specified charset for
     * this file.  The writer is passed to the closure, and will be closed
     * before this method returns.
     *
     * @param file    a File
     * @param charset the charset
     * @param closure the closure to invoke with the PrintWriter
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withPrintWriter(File file, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withWriter(newPrintWriter(file, charset), closure);
    }

    /**
     * Helper method to create a new BufferedReader for a URL and then
     * passes it to the closure.  The reader is closed after the closure returns.
     *
     * @param url     a URL
     * @param closure the closure to invoke with the reader
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.2
     */
    public static <T> T withReader(URL url, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withReader(url.openConnection().getInputStream(), closure);
    }

    /**
     * Helper method to create a new Reader for a URL and then
     * passes it to the closure.  The reader is closed after the closure returns.
     *
     * @param url     a URL
     * @param charset the charset used
     * @param closure the closure to invoke with the reader
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @since 1.5.6
     */
    public static <T> T withReader(URL url, String charset, Closure<T> closure) throws IOException {
        return IOGroovyMethods.withReader(url.openConnection().getInputStream(), charset, closure);
    }

    /**
     * Creates a buffered input stream for this file.
     *
     * @param file a File
     * @return a BufferedInputStream of the file
     * @throws FileNotFoundException if the file is not found.
     * @since 1.0
     */
    public static BufferedInputStream newInputStream(File file) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    /**
     * Creates an inputstream for this URL, with the possibility to set different connection parameters using the
     * <i>parameters map</i>:
     * <ul>
     * <li>connectTimeout : the connection timeout</li>
     * <li>readTimeout : the read timeout</li>
     * <li>useCaches : set the use cache property for the URL connection</li>
     * <li>allowUserInteraction : set the user interaction flag for the URL connection</li>
     * <li>requestProperties : a map of properties to be passed to the URL connection</li>
     * </ul>
     *
     * @param parameters an optional map specifying part or all of supported connection parameters
     * @param url        the url for which to create the inputstream
     * @return an InputStream from the underlying URLConnection
     * @throws IOException if an I/O error occurs while creating the input stream
     * @since 1.8.1
     */
    private static InputStream configuredInputStream(Map parameters, URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        if (parameters != null) {
            if (parameters.containsKey("connectTimeout")) {
                connection.setConnectTimeout(DefaultGroovyMethods.asType(parameters.get("connectTimeout"), Integer.class));
            }
            if (parameters.containsKey("readTimeout")) {
                connection.setReadTimeout(DefaultGroovyMethods.asType(parameters.get("readTimeout"), Integer.class));
            }
            if (parameters.containsKey("useCaches")) {
                connection.setUseCaches(DefaultGroovyMethods.asType(parameters.get("useCaches"), Boolean.class));
            }
            if (parameters.containsKey("allowUserInteraction")) {
                connection.setAllowUserInteraction(DefaultGroovyMethods.asType(parameters.get("allowUserInteraction"), Boolean.class));
            }
            if (parameters.containsKey("requestProperties")) {
                @SuppressWarnings("unchecked")
                Map<String, String> properties = (Map<String, String>) parameters.get("requestProperties");
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

        }
        return connection.getInputStream();
    }

    /**
     * Creates a buffered input stream for this URL.
     *
     * @param url a URL
     * @return a BufferedInputStream for the URL
     * @throws MalformedURLException is thrown if the URL is not well formed
     * @throws IOException           if an I/O error occurs while creating the input stream
     * @since 1.5.2
     */
    public static BufferedInputStream newInputStream(URL url) throws MalformedURLException, IOException {
        return new BufferedInputStream(configuredInputStream(null, url));
    }

    /**
     * Creates a buffered input stream for this URL.
     *
     * @param url        a URL
     * @param parameters connection parameters
     * @return a BufferedInputStream for the URL
     * @throws MalformedURLException is thrown if the URL is not well formed
     * @throws IOException           if an I/O error occurs while creating the input stream
     * @since 1.8.1
     */
    public static BufferedInputStream newInputStream(URL url, Map parameters) throws MalformedURLException, IOException {
        return new BufferedInputStream(configuredInputStream(parameters, url));
    }

    /**
     * Creates a buffered reader for this URL.
     *
     * @param url a URL
     * @return a BufferedReader for the URL
     * @throws MalformedURLException is thrown if the URL is not well formed
     * @throws IOException           if an I/O error occurs while creating the input stream
     * @since 1.5.5
     */
    public static BufferedReader newReader(URL url) throws MalformedURLException, IOException {
        return IOGroovyMethods.newReader(configuredInputStream(null, url));
    }

    /**
     * Creates a buffered reader for this URL.
     *
     * @param url        a URL
     * @param parameters connection parameters
     * @return a BufferedReader for the URL
     * @throws MalformedURLException is thrown if the URL is not well formed
     * @throws IOException           if an I/O error occurs while creating the input stream
     * @since 1.8.1
     */
    public static BufferedReader newReader(URL url, Map parameters) throws MalformedURLException, IOException {
        return IOGroovyMethods.newReader(configuredInputStream(parameters, url));
    }

    /**
     * Creates a buffered reader for this URL using the given encoding.
     *
     * @param url     a URL
     * @param charset opens the stream with a specified charset
     * @return a BufferedReader for the URL
     * @throws MalformedURLException is thrown if the URL is not well formed
     * @throws IOException           if an I/O error occurs while creating the input stream
     * @since 1.5.5
     */
    public static BufferedReader newReader(URL url, String charset) throws MalformedURLException, IOException {
        return new BufferedReader(new InputStreamReader(configuredInputStream(null, url), charset));
    }

    /**
     * Creates a buffered reader for this URL using the given encoding.
     *
     * @param url        a URL
     * @param parameters connection parameters
     * @param charset    opens the stream with a specified charset
     * @return a BufferedReader for the URL
     * @throws MalformedURLException is thrown if the URL is not well formed
     * @throws IOException           if an I/O error occurs while creating the input stream
     * @since 1.8.1
     */
    public static BufferedReader newReader(URL url, Map parameters, String charset) throws MalformedURLException, IOException {
        return new BufferedReader(new InputStreamReader(configuredInputStream(parameters, url), charset));
    }

    /**
     * Create a data input stream for this file
     *
     * @param file a File
     * @return a DataInputStream of the file
     * @throws FileNotFoundException if the file is not found.
     * @since 1.5.0
     */
    public static DataInputStream newDataInputStream(File file) throws FileNotFoundException {
        return new DataInputStream(new FileInputStream(file));
    }

    /**
     * Traverse through each byte of this File
     *
     * @param self    a File
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachByte(java.io.InputStream, groovy.lang.Closure)
     * @since 1.0
     */
    public static void eachByte(File self, Closure closure) throws IOException {
        BufferedInputStream is = newInputStream(self);
        IOGroovyMethods.eachByte(is, closure);
    }

    /**
     * Traverse through the bytes of this File, bufferLen bytes at a time.
     *
     * @param self      a File
     * @param bufferLen the length of the buffer to use.
     * @param closure   a 2 parameter closure which is passed the byte[] and a number of bytes successfully read.
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachByte(java.io.InputStream, int, groovy.lang.Closure)
     * @since 1.7.4
     */
    public static void eachByte(File self, int bufferLen, Closure closure) throws IOException {
        BufferedInputStream is = newInputStream(self);
        IOGroovyMethods.eachByte(is, bufferLen, closure);
    }

    /**
     * Reads the InputStream from this URL, passing each byte to the given
     * closure.  The URL stream will be closed before this method returns.
     *
     * @param url     url to iterate over
     * @param closure closure to apply to each byte
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachByte(java.io.InputStream, groovy.lang.Closure)
     * @since 1.0
     */
    public static void eachByte(URL url, Closure closure) throws IOException {
        InputStream is = url.openConnection().getInputStream();
        IOGroovyMethods.eachByte(is, closure);
    }

    /**
     * Reads the InputStream from this URL, passing a byte[] and a number of bytes
     * to the given closure.  The URL stream will be closed before this method returns.
     *
     * @param url       url to iterate over
     * @param bufferLen the length of the buffer to use.
     * @param closure   a 2 parameter closure which is passed the byte[] and a number of bytes successfully read.
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#eachByte(java.io.InputStream, int, groovy.lang.Closure)
     * @since 1.8
     */
    public static void eachByte(URL url, int bufferLen, Closure closure) throws IOException {
        InputStream is = url.openConnection().getInputStream();
        IOGroovyMethods.eachByte(is, bufferLen, closure);
    }

    /**
     * Filters the lines of a File and creates a Writable in return to
     * stream the filtered lines.
     *
     * @param self    a File
     * @param closure a closure which returns a boolean indicating to filter
     *                the line or not
     * @return a Writable closure
     * @throws IOException if <code>self</code> is not readable
     * @see IOGroovyMethods#filterLine(java.io.Reader, groovy.lang.Closure)
     * @since 1.0
     */
    public static Writable filterLine(File self, Closure closure) throws IOException {
        return IOGroovyMethods.filterLine(newReader(self), closure);
    }

    /**
     * Filters the lines of a File and creates a Writable in return to
     * stream the filtered lines.
     *
     * @param self    a File
     * @param charset opens the file with a specified charset
     * @param closure a closure which returns a boolean indicating to filter
     *                the line or not
     * @return a Writable closure
     * @throws IOException if an IOException occurs
     * @see IOGroovyMethods#filterLine(java.io.Reader, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static Writable filterLine(File self, String charset, Closure closure) throws IOException {
        return IOGroovyMethods.filterLine(newReader(self, charset), closure);
    }

    /**
     * Filter the lines from this File, and write them to the given writer based
     * on the given closure predicate.
     *
     * @param self    a File
     * @param writer  a writer destination to write filtered lines to
     * @param closure a closure which takes each line as a parameter and returns
     *                <code>true</code> if the line should be written to this writer.
     * @throws IOException if <code>self</code> is not readable
     * @see IOGroovyMethods#filterLine(java.io.Reader, java.io.Writer, groovy.lang.Closure)
     * @since 1.0
     */
    public static void filterLine(File self, Writer writer, Closure closure) throws IOException {
        IOGroovyMethods.filterLine(newReader(self), writer, closure);
    }

    /**
     * Filter the lines from this File, and write them to the given writer based
     * on the given closure predicate.
     *
     * @param self    a File
     * @param writer  a writer destination to write filtered lines to
     * @param charset opens the file with a specified charset
     * @param closure a closure which takes each line as a parameter and returns
     *                <code>true</code> if the line should be written to this writer.
     * @throws IOException if an IO error occurs
     * @see IOGroovyMethods#filterLine(java.io.Reader, java.io.Writer, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static void filterLine(File self, Writer writer, String charset, Closure closure) throws IOException {
        IOGroovyMethods.filterLine(newReader(self, charset), writer, closure);
    }

    /**
     * Filter lines from a URL using a closure predicate.  The closure
     * will be passed each line as a String, and it should return
     * <code>true</code> if the line should be passed to the writer.
     *
     * @param self      a URL
     * @param predicate a closure which returns boolean and takes a line
     * @return a writable which writes out the filtered lines
     * @throws IOException if an IO exception occurs
     * @see IOGroovyMethods#filterLine(java.io.Reader, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static Writable filterLine(URL self, Closure predicate) throws IOException {
        return IOGroovyMethods.filterLine(newReader(self), predicate);
    }

    /**
     * Filter lines from a URL using a closure predicate.  The closure
     * will be passed each line as a String, and it should return
     * <code>true</code> if the line should be passed to the writer.
     *
     * @param self      the URL
     * @param charset   opens the URL with a specified charset
     * @param predicate a closure which returns boolean and takes a line
     * @return a writable which writes out the filtered lines
     * @throws IOException if an IO exception occurs
     * @see IOGroovyMethods#filterLine(java.io.Reader, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static Writable filterLine(URL self, String charset, Closure predicate) throws IOException {
        return IOGroovyMethods.filterLine(newReader(self, charset), predicate);
    }

    /**
     * Uses a closure to filter lines from this URL and pass them to
     * the given writer. The closure will be passed each line as a String, and
     * it should return <code>true</code> if the line should be passed to the
     * writer.
     *
     * @param self      the URL
     * @param writer    a writer to write output to
     * @param predicate a closure which returns true if a line should be accepted
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#filterLine(java.io.Reader, java.io.Writer, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static void filterLine(URL self, Writer writer, Closure predicate) throws IOException {
        IOGroovyMethods.filterLine(newReader(self), writer, predicate);
    }

    /**
     * Uses a closure to filter lines from this URL and pass them to
     * the given writer. The closure will be passed each line as a String, and
     * it should return <code>true</code> if the line should be passed to the
     * writer.
     *
     * @param self      the URL
     * @param writer    a writer to write output to
     * @param charset   opens the URL with a specified charset
     * @param predicate a closure which returns true if a line should be accepted
     * @throws IOException if an IOException occurs.
     * @see IOGroovyMethods#filterLine(java.io.Reader, java.io.Writer, groovy.lang.Closure)
     * @since 1.6.8
     */
    public static void filterLine(URL self, Writer writer, String charset, Closure predicate) throws IOException {
        IOGroovyMethods.filterLine(newReader(self, charset), writer, predicate);
    }

    /**
     * Reads the content of the file into a byte array.
     *
     * @param file a File
     * @return a byte array with the contents of the file.
     * @throws IOException if an IOException occurs.
     * @since 1.0
     */
    public static byte[] readBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(fileInputStream);
        try {
            dis.readFully(bytes);
            InputStream temp = dis;
            dis = null;
            temp.close();
        } finally {
            closeWithWarning(dis);
        }
        return bytes;
    }

    /**
     * Transforms a CharSequence representing a URI into a URI object.
     *
     * @param self the CharSequence representing a URI
     * @return a URI
     * @throws java.net.URISyntaxException is thrown if the URI is not well formed.
     * @since 1.8.2
     */
    public static URI toURI(CharSequence self) throws URISyntaxException {
        return new URI(self.toString());
    }

    /**
     * Transforms a String representing a URI into a URI object.
     *
     * @param self the String representing a URI
     * @return a URI
     * @throws java.net.URISyntaxException is thrown if the URI is not well formed.
     * @since 1.0
     */
    public static URI toURI(String self) throws URISyntaxException {
        return new URI(self);
    }

    /**
     * Transforms a CharSequence representing a URL into a URL object.
     *
     * @param self the CharSequence representing a URL
     * @return a URL
     * @throws java.net.MalformedURLException is thrown if the URL is not well formed.
     * @since 1.8.2
     */
    public static URL toURL(CharSequence self) throws MalformedURLException {
        return new URL(self.toString());
    }

    /**
     * Transforms a String representing a URL into a URL object.
     *
     * @param self the String representing a URL
     * @return a URL
     * @throws java.net.MalformedURLException is thrown if the URL is not well formed.
     * @since 1.0
     */
    public static URL toURL(String self) throws MalformedURLException {
        return new URL(self);
    }

}
