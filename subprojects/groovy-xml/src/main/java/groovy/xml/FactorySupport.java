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

package groovy.xml;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import java.security.PrivilegedExceptionAction;
import java.security.AccessController;
import java.security.PrivilegedActionException;

/**
 * Support class for creating XML Factories
 */
public class FactorySupport {
    static Object createFactory(PrivilegedExceptionAction action) throws ParserConfigurationException {
        Object factory;
        try {
            factory = AccessController.doPrivileged(action);
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (e instanceof ParserConfigurationException) {
                throw(ParserConfigurationException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
        return factory;
    }

    public static DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
        return (DocumentBuilderFactory) createFactory(new PrivilegedExceptionAction() {
            public Object run() throws ParserConfigurationException {
                return DocumentBuilderFactory.newInstance();
            }
        });
    }

    public static SAXParserFactory createSaxParserFactory() throws ParserConfigurationException {
        return (SAXParserFactory) createFactory(new PrivilegedExceptionAction() {
                public Object run() throws ParserConfigurationException {
                    return SAXParserFactory.newInstance();
                }
            });
    }
}
