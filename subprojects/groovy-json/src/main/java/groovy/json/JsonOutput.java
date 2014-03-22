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
 */
package groovy.json;

import groovy.json.internal.CharBuf;
import groovy.json.internal.Chr;
import groovy.lang.Closure;
import groovy.util.Expando;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class responsible for the actual String serialization of the possible values of a JSON structure.
 * This class can also be used as a category, so as to add <code>toJson()</code> methods to various types.
 *
 * @author Guillaume Laforge
 * @author Roshan Dawrani
 * @author Andrey Bloschetsov
 * @author Rick Hightower
 * @since 1.8.0
 */
public class JsonOutput {

    /**
     * @return a string representation for object.
     */
    public static String toJson(Object object) {
        CharBuf buffer = CharBuf.create(255);
        writeObject(object, buffer);

        return buffer.toString();
    }

    /**
     * Serializes object and writes it into specified buffer.
     */
    private static void writeObject(Object object, CharBuf buffer) {
        if (object == null) {
            buffer.addNull();
        } else {
            Class<?> objectClass = object.getClass();

            if (CharSequence.class.isAssignableFrom(objectClass)) { // Handle String, StringBuilder, GString and other CharSequence implemenations
                CharSequence seq = (CharSequence) object;
                if (seq.length() > 0) {
                    buffer.addJsonEscapedString(seq.toString());
                } else {
                    buffer.addChars(Chr.array('"', '"'));
                }
            } else if (objectClass == Boolean.class) {
                buffer.addBoolean(((Boolean) object).booleanValue());
            } else if (objectClass == Integer.class) {
                buffer.addInt((Integer) object);
            } else if (objectClass == Long.class) {
                buffer.addLong((Long) object);
            } else if (objectClass == BigInteger.class) {
                buffer.addBigInteger((BigInteger) object);
            } else if (objectClass == BigDecimal.class) {
                buffer.addBigDecimal((BigDecimal) object);
            } else if (Date.class.isAssignableFrom(objectClass)) {
                writeDate((Date) object, buffer);
            } else if (Calendar.class.isAssignableFrom(objectClass)) {
                writeDate(((Calendar) object).getTime(), buffer);
            } else if (Map.class.isAssignableFrom(objectClass)) {
                writeMap((Map) object, buffer);
            } else if (Iterable.class.isAssignableFrom(objectClass)) {
                writeIterator(((Iterable<?>) object).iterator(), buffer);
            } else if (Iterator.class.isAssignableFrom(objectClass)) {
                writeIterator((Iterator) object, buffer);
            } else if (objectClass == Character.class) {
                buffer.addJsonEscapedString(Chr.array((Character) object));
            } else if (objectClass == Double.class) {
                Double doubleValue = (Double) object;
                if (doubleValue.isInfinite()) {
                    throw new JsonException("Number " + object + " can't be serialized as JSON: infinite are not allowed in JSON.");
                }
                if (doubleValue.isNaN()) {
                    throw new JsonException("Number " + object + " can't be serialized as JSON: NaN are not allowed in JSON.");
                }

                buffer.addDouble(doubleValue);
            } else if (objectClass == Float.class) {
                Float floatValue = (Float) object;
                if (floatValue.isInfinite()) {
                    throw new JsonException("Number " + object + " can't be serialized as JSON: infinite are not allowed in JSON.");
                }
                if (floatValue.isNaN()) {
                    throw new JsonException("Number " + object + " can't be serialized as JSON: NaN are not allowed in JSON.");
                }

                buffer.addFloat(floatValue);
            } else if (objectClass == Byte.class) {
                buffer.addByte((Byte) object);
            } else if (objectClass == Short.class) {
                buffer.addShort((Short) object);
            } else if (objectClass == URL.class) {
                buffer.addJsonEscapedString(object.toString());
            } else if (objectClass == UUID.class) {
                buffer.addQuoted(object.toString());
            } else if (Closure.class.isAssignableFrom(objectClass)) {
                writeMap(JsonDelegate.cloneDelegateAndGetContent((Closure<?>) object), buffer);
            } else if (Expando.class.isAssignableFrom(objectClass)) {
                Map<?, ?> map = ((Expando) object).getProperties();
                writeMap(map, buffer);
            } else if (Enumeration.class.isAssignableFrom(objectClass)) {
                List<?> list = Collections.list((Enumeration<?>) object);
                writeIterator(list.iterator(), buffer);
            } else if (objectClass.isArray()) {
                writeArray(objectClass, object, buffer);
            } else if (Enum.class.isAssignableFrom(objectClass)) {
                buffer.addQuoted(((Enum<?>) object).name());
            } else if (Number.class.isAssignableFrom(objectClass)) { // Handle other Number implementations
                buffer.addString(((Number) object).toString());
            } else {
                Map<?, ?> properties = DefaultGroovyMethods.getProperties(object);
                properties.remove("class");
                properties.remove("declaringClass");
                properties.remove("metaClass");
                writeMap(properties, buffer);
            }
        }
    }

    private static final DateFormatThreadLocal dateFormatter = new DateFormatThreadLocal();

    /**
     * Serializes date and writes it into specified buffer.
     */
    private static void writeDate(Date date, CharBuf buffer) {
        buffer.addQuoted(dateFormatter.get().format(date));
    }

    /**
     * Serializes array and writes it into specified buffer.
     */
    private static void writeArray(Class<?> arrayClass, Object array, CharBuf buffer) {
        buffer.addChar('[');
        if (Object[].class.isAssignableFrom(arrayClass)) {
            Object[] objArray = (Object[]) array;
            if (objArray.length > 0) {
                writeObject(objArray[0], buffer);
                for (int i = 1; i < objArray.length; i++) {
                    buffer.addChar(',');
                    writeObject(objArray[i], buffer);
                }
            }
        } else if (int[].class.isAssignableFrom(arrayClass)) {
            int[] intArray = (int[]) array;
            if (intArray.length > 0) {
                buffer.addInt(intArray[0]);
                for (int i = 1; i < intArray.length; i++) {
                    buffer.addChar(',').addInt(intArray[i]);
                }
            }
        } else if (long[].class.isAssignableFrom(arrayClass)) {
            long[] longArray = (long[]) array;
            if (longArray.length > 0) {
                buffer.addLong(longArray[0]);
                for (int i = 1; i < longArray.length; i++) {
                    buffer.addChar(',').addLong(longArray[i]);
                }
            }
        } else if (boolean[].class.isAssignableFrom(arrayClass)) {
            boolean[] booleanArray = (boolean[]) array;
            if (booleanArray.length > 0) {
                buffer.addBoolean(booleanArray[0]);
                for (int i = 1; i < booleanArray.length; i++) {
                    buffer.addChar(',').addBoolean(booleanArray[i]);
                }
            }
        } else if (char[].class.isAssignableFrom(arrayClass)) {
            char[] charArray = (char[]) array;
            if (charArray.length > 0) {
                buffer.addJsonEscapedString(Chr.array(charArray[0]));
                for (int i = 1; i < charArray.length; i++) {
                    buffer.addChar(',').addJsonEscapedString(Chr.array(charArray[i]));
                }
            }
        } else if (double[].class.isAssignableFrom(arrayClass)) {
            double[] doubleArray = (double[]) array;
            if (doubleArray.length > 0) {
                buffer.addDouble(doubleArray[0]);
                for (int i = 1; i < doubleArray.length; i++) {
                    buffer.addChar(',').addDouble(doubleArray[i]);
                }
            }
        } else if (float[].class.isAssignableFrom(arrayClass)) {
            float[] floatArray = (float[]) array;
            if (floatArray.length > 0) {
                buffer.addFloat(floatArray[0]);
                for (int i = 1; i < floatArray.length; i++) {
                    buffer.addChar(',').addFloat(floatArray[i]);
                }
            }
        } else if (byte[].class.isAssignableFrom(arrayClass)) {
            byte[] byteArray = (byte[]) array;
            if (byteArray.length > 0) {
                buffer.addByte(byteArray[0]);
                for (int i = 1; i < byteArray.length; i++) {
                    buffer.addChar(',').addByte(byteArray[i]);
                }
            }
        } else if (short[].class.isAssignableFrom(arrayClass)) {
            short[] shortArray = (short[]) array;
            if (shortArray.length > 0) {
                buffer.addShort(shortArray[0]);
                for (int i = 1; i < shortArray.length; i++) {
                    buffer.addChar(',').addShort(shortArray[i]);
                }
            }
        }
        buffer.addChar(']');
    }

    private static final char[] EMPTY_MAP_CHARS = {'{', '}'};

    /**
     * Serializes map and writes it into specified buffer.
     */
    private static void writeMap(Map<?, ?> map, CharBuf buffer) {
        if (!map.isEmpty()) {
            buffer.addChar('{');
            boolean firstItem = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    throw new IllegalArgumentException("Maps with null keys can\'t be converted to JSON");
                }

                if (!firstItem) {
                    buffer.addChar(',');
                } else {
                    firstItem = false;
                }

                buffer.addJsonFieldName(entry.getKey().toString());
                writeObject(entry.getValue(), buffer);
            }
            buffer.addChar('}');
        } else {
            buffer.addChars(EMPTY_MAP_CHARS);
        }
    }

    private static final char[] EMPTY_LIST_CHARS = {'[', ']'};

    /**
     * Serializes iterator and writes it into specified buffer.
     */
    private static void writeIterator(Iterator<?> iterator, CharBuf buffer) {
        if (iterator.hasNext()) {
            buffer.addChar('[');
            Object it = iterator.next();
            writeObject(it, buffer);
            while (iterator.hasNext()) {
                it = iterator.next();
                buffer.addChar(',');
                writeObject(it, buffer);
            }
            buffer.addChar(']');
        } else {
            buffer.addChars(EMPTY_LIST_CHARS);
        }
    }

    /**
     * Pretty print a JSON payload.
     *
     * @param jsonPayload
     * @return a pretty representation of JSON payload.
     */
    public static String prettyPrint(String jsonPayload) {
        int indentSize = 0;
        // Just a guess that the pretty view will take a 20 percent more than original.
        final CharBuf output = CharBuf.create((int) (jsonPayload.length() * 0.2));

        JsonLexer lexer = new JsonLexer(new StringReader(jsonPayload));
        // Will store already created indents.
        Map<Integer, char[]> indentCache = new HashMap<Integer, char[]>();
        while (lexer.hasNext()) {
            JsonToken token = lexer.next();
            switch (token.getType()) {
                case OPEN_CURLY:
                    indentSize += 4;
                    output.addChars(Chr.array('{', '\n')).addChars(getIndent(indentSize, indentCache));

                    break;
                case CLOSE_CURLY:
                    indentSize -= 4;
                    output.addChar('\n');
                    if (indentSize > 0) {
                        output.addChars(getIndent(indentSize, indentCache));
                    }
                    output.addChar('}');

                    break;
                case OPEN_BRACKET:
                    indentSize += 4;
                    output.addChars(Chr.array('[', '\n')).addChars(getIndent(indentSize, indentCache));

                    break;
                case CLOSE_BRACKET:
                    indentSize -= 4;
                    output.addChar('\n');
                    if (indentSize > 0) {
                        output.addChars(getIndent(indentSize, indentCache));
                    }
                    output.addChar(']');

                    break;
                case COMMA:
                    output.addChars(Chr.array(',', '\n')).addChars(getIndent(indentSize, indentCache));

                    break;
                case COLON:
                    output.addChars(Chr.array(':', ' '));

                    break;
                case STRING:
                    String textStr = token.getText();
                    String textWithoutQuotes = textStr.substring(1, textStr.length() - 1);
                    if (textWithoutQuotes.length() > 0) {
                        output.addJsonEscapedString(textWithoutQuotes);
                    } else {
                        output.addQuoted(Chr.array());
                    }

                    break;
                default:
                    output.addString(token.getText());
            }
        }

        return output.toString();
    }

    /**
     * Creates new indent if it not exists in the indent cache.
     *
     * @return indent with the specified size.
     */
    private static char[] getIndent(int indentSize, Map<Integer, char[]> indentCache) {
        char[] indent = indentCache.get(indentSize);
        if (indent == null) {
            indent = new char[indentSize];
            Arrays.fill(indent, ' ');
            indentCache.put(indentSize, indent);
        }

        return indent;
    }

    private JsonOutput() {}

}
