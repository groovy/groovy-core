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
 * Derived from Boon all rights granted to Groovy project for this fork.
 */
package groovy.json.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author Richard Hightower
 */
public class ReaderCharacterSource implements CharacterSource {

    private static final int MAX_TOKEN_SIZE = 5;

    private final Reader reader;
    private int readAheadSize;
    private int ch = -2;

    private boolean foundEscape;

    private char[] readBuf;

    private int index;

    private int length;

    boolean more = true;
    private boolean done = false;

    public ReaderCharacterSource(final Reader reader, final int readAheadSize) {
        this.reader = reader;
        this.readBuf = new char[readAheadSize + MAX_TOKEN_SIZE];
        this.readAheadSize = readAheadSize;
    }

    public ReaderCharacterSource(final Reader reader) {
        this.reader = reader;
        this.readAheadSize = 10000;
        this.readBuf = new char[readAheadSize + MAX_TOKEN_SIZE];
    }

    public ReaderCharacterSource(final String string) {
        this(new StringReader(string));
    }

    private void readForToken() {
        try {
            length += reader.read(readBuf, readBuf.length - MAX_TOKEN_SIZE, MAX_TOKEN_SIZE);
        } catch (IOException e) {
            Exceptions.handle(e);
        }
    }

    private void ensureBuffer() {

        try {
            if (index >= length && !done) {
                readNextBuffer();
            } else if (done && index >= length) {
                more = false;
            } else {
                more = true;
            }
        } catch (Exception ex) {
            String str = CharScanner.errorDetails("ensureBuffer issue", readBuf, index, ch);
            Exceptions.handle(str, ex);
        }
    }

    private void readNextBuffer() throws IOException {
        length = reader.read(readBuf, 0, readAheadSize);

        index = 0;
        if (length == -1) {
            ch = -1;
            length = 0;
            more = false;
            done = true;
        } else {
            more = true;
        }
    }

    public final int nextChar() {
        ensureBuffer();
        return ch = readBuf[index++];
    }

    public final int currentChar() {
        ensureBuffer();
        return readBuf[index];
    }

    public final boolean hasChar() {
        ensureBuffer();
        return more;
    }

    public final boolean consumeIfMatch(char[] match) {
        try {

            char[] _chars = readBuf;
            int i = 0;
            int idx = index;
            boolean ok = true;

            if (idx + match.length > length) {
                readForToken();
            }

            for (; i < match.length; i++, idx++) {
                ok &= (match[i] == _chars[idx]);
                if (!ok) break;
            }

            if (ok) {
                index = idx;
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            String str = CharScanner.errorDetails("consumeIfMatch issue", readBuf, index, ch);
            return Exceptions.handle(boolean.class, str, ex);
        }

    }

    public final int location() {
        return index;
    }

    public final int safeNextChar() {
        try {
            ensureBuffer();
            return index + 1 < readBuf.length ? readBuf[index++] : -1;
        } catch (Exception ex) {
            String str = CharScanner.errorDetails("safeNextChar issue", readBuf, index, ch);
            return Exceptions.handle(int.class, str, ex);
        }
    }

    private final char[] EMPTY_CHARS = new char[0];

    public char[] findNextChar(int match, int esc) {
        try {
            ensureBuffer();

            int idx = index;
            char[] _chars = readBuf;

            int ch = this.ch;
            if (ch == '"') {

            } else if (idx < length - 1) {
                ch = _chars[idx];

                if (ch == '"') {
                    idx++;
                }
            }

            if (idx < length) {
                ch = _chars[idx];
            }

            if (ch == '"') {
                index = idx;
                index++;
                return EMPTY_CHARS;
            }
            int start = idx;

            foundEscape = false;

            boolean foundEnd = false;
            char[] results;

            for (; idx < length; idx++) {
                ch = _chars[idx];
                if (ch == match || ch == esc) {
                    if (ch == match) {
                        foundEnd = true;

                        break;
                    } else if (ch == esc) {
                        foundEscape = true;
                        /** if we are dealing with an escape then see if the escaped char is a match
                         *  if so, skip it.
                         */
                        if (idx + 1 < length) {
                            idx++;
                        }
                    }
                }
            }

            if (idx == 0) {
                results = EMPTY_CHARS;
            } else {
                results = ArrayUtils.copyRange(_chars, start, idx);
            }
            index = idx;

            if (foundEnd) {
                index++;
                if (index < length) {
                    ch = _chars[index];
                    this.ch = ch;
                }
                return results;
            } else {

                if (index >= length && !done) {
                    ensureBuffer();
                    char results2[] = findNextChar(match, esc);
                    return Chr.add(results, results2);
                } else {
                    return Exceptions.die(char[].class, "Unable to find close char " + (char) match + " " + new String(results));
                }
            }
        } catch (Exception ex) {
            String str = CharScanner.errorDetails("findNextChar issue", readBuf, index, ch);
            return Exceptions.handle(char[].class, str, ex);
        }

    }

    public boolean hadEscape() {
        return foundEscape;
    }

    public void skipWhiteSpace() {
        try {
            index = CharScanner.skipWhiteSpace(readBuf, index, length);
            if (index >= length && more) {

                ensureBuffer();

                skipWhiteSpace();
            }
        } catch (Exception ex) {
            String str = CharScanner.errorDetails("skipWhiteSpace issue", readBuf, index, ch);
            Exceptions.handle(str, ex);
        }
    }

    public char[] readNumber() {
        try {
            ensureBuffer();

            char[] results = CharScanner.readNumber(readBuf, index, length);
            index += results.length;

            if (index >= length && more) {
                ensureBuffer();
                if (length != 0) {
                    char results2[] = readNumber();
                    return Chr.add(results, results2);
                } else {
                    return results;
                }
            } else {
                return results;
            }
        } catch (Exception ex) {
            String str = CharScanner.errorDetails("readNumber issue", readBuf, index, ch);
            return Exceptions.handle(char[].class, str, ex);
        }

    }

    public String errorDetails(String message) {

        return CharScanner.errorDetails(message, readBuf, index, ch);
    }

}
