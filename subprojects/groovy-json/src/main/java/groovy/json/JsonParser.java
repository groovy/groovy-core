/*
 * Copyright 2003-2014 the original author or authors.
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
 *
 * Derived from Boon all rights granted to Groovy project for this fork of Boon JSON parser.
 */
package groovy.json;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/**
 * This is the parser interface that backs the new JsonSlurper.
 * It was derived from the Boon JSON parser.
 * @author Rick Hightower
 * @since 2.3.0
 */
public interface JsonParser {

    Object parse(  String jsonString );
    Object parse(  byte[] bytes );
    Object parse(  byte[] bytes, String charset );
    Object parse(  CharSequence charSequence );
    Object parse(  char[] chars );
    Object parse(  Reader reader );
    Object parse(  InputStream input );
    Object parse(  InputStream input, String charset );
    Object parse(  File file, String charset);
}
