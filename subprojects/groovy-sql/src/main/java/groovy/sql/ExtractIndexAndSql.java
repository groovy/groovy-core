/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package groovy.sql;

import groovy.lang.Tuple;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and indexes named parameters from a sql string.
 *
 * This class is package-private scoped and is only intended for internal use.
 *
 * @see groovy.sql.Sql
 */
class ExtractIndexAndSql {

    private static final Pattern NAMED_QUERY_PATTERN = Pattern.compile("(?<!:)(:)(\\w+)|\\?(\\d*)(?:\\.(\\w+))?");
    private static final char QUOTE = '\'';

    private final String sql;
    private List<Tuple> indexPropList;
    private List<Object> params;

    private String newSql;
    private List<Object> newParams;

    /**
     * Buffer used to store the sql statement as it's parsed.
     */
    private final StringBuilder buffer;

    /**
     * Used to track the current position within the sql while parsing
     */
    private int index = 0;

    /**
     * Indicates whether parameter placeholders required expansion
     */
    private boolean placeholdersExpanded = false;

    /**
     * Static factory method used to create a new instance.  Since parsing of the input
     * is required, this ensures the object is fully initialized.
     *
     * @param sql statement to be parsed
     * @param params list of parameter values
     * @return an instance of {@link ExtractIndexAndSql}
     */
    static ExtractIndexAndSql from(String sql, List<Object> params) {
        return new ExtractIndexAndSql(sql, params).invoke();
    }

    /**
     * @param sql statement to be parsed
     * @return an instance of {@link ExtractIndexAndSql}
     * @see {@link ExtractIndexAndSql#from(String, java.util.List)}
     */
    static ExtractIndexAndSql from(String sql) {
        return ExtractIndexAndSql.from(sql, new ArrayList<Object>());
    }

    /**
     * Checks a sql statement to determine whether it contains parameters.
     *
     * @param sql statement
     * @return {@code true} if the statement contains named parameters, otherwise {@code false}
     */
    static boolean hasNamedParameters(String sql) {
        return NAMED_QUERY_PATTERN.matcher(sql).find();
    }

    /**
     * Constructs this object from the values provided.  No parsing is performed.
     */
    ExtractIndexAndSql(String sql, List<Object> params, List<Tuple> indexPropList) {
        this.sql = newSql = sql;
        this.params = newParams = params;
        this.indexPropList = indexPropList;
        buffer = null;
    }

    private ExtractIndexAndSql(String sql, List<Object> params) {
        this.sql = sql;
        this.params = (params != null) ? new ArrayList<Object>(params) : new ArrayList<Object>();
        indexPropList = new ArrayList<Tuple>();
        newParams = new ArrayList<Object>();
        buffer =  new StringBuilder(sql.length());
    }

    List<Tuple> getIndexPropList() {
        return indexPropList;
    }

    String getNewSql() {
        return newSql;
    }

    List<Object> getNewParams() {
        return newParams;
    }

    /**
     * Indicates whether or not placeholders within the sql statement were expanded.
     *
     * @return true if sql statement required placeholders to be
     *         expanded (i.e., ? -> (?,?,..), else false
     */
    boolean hasExpandedPlaceholders() {
        return placeholdersExpanded;
    }

    private ExtractIndexAndSql invoke() {
        StringBuilder currentChunk = new StringBuilder();
        int len = sql.length();
        while (index < len) {
            char c = sql.charAt(index);
            switch (c) {
                case QUOTE:
                    appendAndClearChunk(currentChunk);
                    appendToEndOfString();
                    break;
                case '-':
                    if (next() == '-') {
                        appendAndClearChunk(currentChunk);
                        appendToEndOfLine();
                    } else {
                        currentChunk.append(c);
                    }
                    break;
                case '/':
                    if (next() == '*') {
                        appendAndClearChunk(currentChunk);
                        appendToEndOfComment();
                    } else {
                        currentChunk.append(c);
                    }
                    break;
                default:
                    currentChunk.append(c);
            }
            index++;
        }
        appendAndClearChunk(currentChunk);
        newSql = buffer.toString();
        return this;
    }

    private void appendAndClearChunk(StringBuilder chunk) {
        buffer.append(adaptForNamedParams(chunk.toString()));
        chunk.setLength(0);
    }

    private void appendToEndOfString() {
        buffer.append(QUOTE);
        int startQuoteIndex = index;
        ++index;
        boolean foundClosingQuote = false;
        while (index < sql.length()) {
            char c = sql.charAt(index);
            buffer.append(c);
            if (c == QUOTE && next() != QUOTE) {
                if (startQuoteIndex == (index - 1)) {   // empty quote ''
                    foundClosingQuote = true;
                    break;
                }
                int previousQuotes = countPreviousRepeatingChars(QUOTE);
                if (previousQuotes == 0 ||
                        (previousQuotes % 2 == 0 && (index - previousQuotes) != startQuoteIndex) ||
                        (previousQuotes % 2 != 0 && (index - previousQuotes) == startQuoteIndex)) {
                    foundClosingQuote = true;
                    break;
                }
            }
            ++index;
        }
        if (!foundClosingQuote) {
            throw new IllegalStateException("Failed to process query. Unterminated ' character?");
        }
    }

    private int countPreviousRepeatingChars(char c) {
        int pos = index - 1;
        while (pos >= 0) {
            if (sql.charAt(pos) != c) {
                break;
            }
            --pos;
        }
        return (index - 1) - pos;
    }

    private void appendToEndOfComment() {
        while (index < sql.length()) {
            char c = sql.charAt(index);
            buffer.append(c);
            if (c == '*' && next() == '/') {
                buffer.append('/');
                ++index;
                break;
            }
            ++index;
        }
    }

    private void appendToEndOfLine() {
        while (index < sql.length()) {
            char c = sql.charAt(index);
            buffer.append(c);
            if (c == '\n' || c == '\r') {
                break;
            }
            ++index;
        }
    }

    private char next() {
        return ((index + 1) < sql.length()) ? sql.charAt(index + 1) : '\0';
    }

    private String adaptForNamedParams(String sql) {
        StringBuilder newSql = new StringBuilder();
        int txtIndex = 0;

        Matcher matcher = NAMED_QUERY_PATTERN.matcher(sql);
        while (matcher.find()) {
            String indexStr = matcher.group(1);
            if (indexStr == null) indexStr = matcher.group(3);
            int index = (indexStr == null || indexStr.length() == 0 || ":".equals(indexStr)) ? 0 : new Integer(indexStr) - 1;
            String prop = matcher.group(2);
            if (prop == null) prop = matcher.group(4);
            indexPropList.add(new Tuple(new Object[]{index, prop == null || prop.length() == 0 ? "<this>" : prop}));
            String placeholder = getParamPlaceholder(prop, index);
            newSql.append(sql.substring(txtIndex, matcher.start())).append(placeholder);
            txtIndex = matcher.end();
        }
        newSql.append(sql.substring(txtIndex)); // append ending SQL after last param.
        return newSql.toString();
    }

    private String getParamPlaceholder(String propName, int propIndex) {
        String placeholder = "?";
        if (!params.isEmpty()) {
            int newPropIndex = (propName == null || propName.length() == 0) ? indexPropList.size() - 1 : propIndex;
            Object o = getPropertyFromParams(params, newPropIndex, propName);
            if (isNotNullArrayOrCollection(o)) {
                Object[] paramsArray = DefaultGroovyMethods.asType(o, Object[].class);
                placeholder = expandPlaceholdersFor(paramsArray);
                placeholdersExpanded = true;
            } else {
                newParams.add(o);
            }
        }
        return placeholder;
    }

    private String expandPlaceholdersFor(Object[] paramsArray) {
        if (paramsArray == null || paramsArray.length == 0) {
            newParams.add("");
            return "(?)";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < paramsArray.length; i++) {
            if (i == 0) {
                sb.append("?");
            } else {
                sb.append(",?");
            }
            newParams.add(paramsArray[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    private boolean isNotNullArrayOrCollection(Object o) {
        return o != null &&
                (Collection.class.isAssignableFrom(o.getClass()) || o.getClass().isArray());
    }

    private static Object getPropertyFromParams(List<Object> params, int propIndex, String propName) {
        if (propIndex >= params.size()) {
            return null;
        }
        Object o = params.get(propIndex);
        return (propName == null || propName.equals("<this>")) ? o : InvokerHelper.getProperty(o, propName);
    }

    static List<Object> updateFromIndexedProperties(List<Object> params, List<Tuple> indexProps) {
        List<Object> updatedParams = new ArrayList<Object>();
        for (Tuple tuple : indexProps) {
            int propIndex = (Integer) tuple.get(0);
            String propName = (String) tuple.get(1);
            if (propIndex < 0 || propIndex >= params.size())
                throw new IllegalArgumentException("Invalid index " + propIndex + " should be in range 1.." + params.size());
            updatedParams.add(getPropertyFromParams(params, propIndex, propName));
        }
        return updatedParams;
    }

}
