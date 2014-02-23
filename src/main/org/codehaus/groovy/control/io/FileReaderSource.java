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
package org.codehaus.groovy.control.io;

import java.io.*;
import java.nio.charset.Charset;

import org.codehaus.groovy.control.CompilerConfiguration;

/**
 *  A ReaderSource for source files.
 *
 *  @author <a href="mailto:cpoirier@dreaming.org">Chris Poirier</a>
 *  @version $Id$
 */
public class FileReaderSource extends AbstractReaderSource {
    private File file;  // The File from which we produce Readers.
    private final Charset UTF8 = Charset.forName("UTF-8");

   /**
    *  Creates the ReaderSource from a File descriptor.
    * @param file script source file
    * @param configuration configuration for compiling source
    */
    public FileReaderSource( File file, CompilerConfiguration configuration ) {
       super( configuration );
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    /**
    *  Returns a new Reader on the underlying source object.  
    */
    public Reader getReader() throws IOException {
       // we want to remove the BOM windows adds from a file if the encoding is UTF-8
       // in other cases we depend on the charsets 
       Charset cs = Charset.forName(configuration.getSourceEncoding());
       InputStream in = new BufferedInputStream(new FileInputStream(file));
       if (UTF8.name().equalsIgnoreCase(cs.name())) {
           in.mark(3);
           boolean hasBOM = true;
           try {
               int i = in.read();
               hasBOM &= i == 0xEF;
               i = in.read();
               hasBOM &= i == 0xBB;
               i = in.read();
               hasBOM &= i == 0xFF;
           } catch (IOException ioe) {
               hasBOM= false;
           }
           if (!hasBOM) in.reset();
       }
       return new InputStreamReader( in, cs );
    }
    
}
