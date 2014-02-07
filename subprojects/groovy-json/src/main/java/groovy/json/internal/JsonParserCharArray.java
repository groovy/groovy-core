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

import groovy.json.JsonException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static groovy.json.internal.CharScanner.isInteger;


/**
 * Converts an input JSON String into Java objects works with String or char array
 * as input. Produces an Object which can be any of the basic JSON types mapped
 * to Java.
 * <p/>
 *
 * @author Rick Hightower
 */
public class JsonParserCharArray extends BaseJsonParser {

    protected char[] charArray;
    protected int __index;
    protected char __currentChar;


    private int lastIndex;

    protected Object decodeFromChars( char[] cs ) {
        __index = 0;
        charArray = cs;
        lastIndex = cs.length - 1;
        Object value = decodeValue();
        return value;
    }


    protected final boolean hasMore() {
        return __index < lastIndex;
    }


    protected final boolean hasCurrent() {
        return __index <= lastIndex;
    }


    protected final void skipWhiteSpace() {
        int ix = __index;


        if ( hasCurrent() ) {
            this.__currentChar = this.charArray[ix];
        }

        if ( __currentChar <= 32 ) {
            ix = skipWhiteSpaceFast( this.charArray, ix );
            this.__currentChar = this.charArray[ix];
            __index = ix;
        }


    }


    protected final char nextChar() {

        try {
            if ( hasMore() ) {
                __index++;
                return __currentChar = charArray[__index];
            } else {
                return '\u0000';
            }
        } catch ( Exception ex ) {
            throw new JsonException( exceptionDetails( "unable to advance character" ), ex );
        }
    }


    protected String exceptionDetails( String message ) {
        return CharScanner.errorDetails( message, charArray, __index, __currentChar );
    }


    private static int skipWhiteSpaceFast( char[] array, int index ) {
        char c;
        for (; index < array.length; index++ ) {
            c = array[index];
            if ( c > 32 ) {

                return index;
            }
        }
        return index - 1;
    }


    protected final Object decodeJsonObject() {


        if ( __currentChar == '{' ) {
            __index++;
        }

        LazyMap map = new LazyMap();

        for (; __index < this.charArray.length; __index++ ) {

            skipWhiteSpace();


            if ( __currentChar == '"' ) {

                String key =
                        decodeString();

                if ( internKeys ) {
                    String keyPrime = internedKeysCache.get( key );
                    if ( keyPrime == null ) {
                        key = key.intern();
                        internedKeysCache.put( key, key );
                    } else {
                        key = keyPrime;
                    }
                }

                skipWhiteSpace();

                if ( __currentChar != ':' ) {

                    complain( "expecting current character to be " + charDescription( __currentChar ) + "\n" );
                }
                __index++;

                skipWhiteSpace();

                Object value = decodeValueInternal();

                skipWhiteSpace();
                map.put( key, value );


            }
            if ( __currentChar == '}' ) {
                __index++;
                break;
            } else if ( __currentChar == ',' ) {
                continue;
            } else {
                complain(
                        "expecting '}' or ',' but got current char " + charDescription( __currentChar ) );

            }
        }


        return map;
    }


    protected final void complain( String complaint ) {
        throw new JsonException( exceptionDetails( complaint ) );
    }


    protected Object decodeValue() {
        return decodeValueInternal();
    }

    private final Object decodeValueInternal() {
        Object value = null;
        skipWhiteSpace();

        switch ( __currentChar ) {

            case '"':
                value = decodeString();
                break;


            case 't':
                value = decodeTrue();
                break;

            case 'f':
                value = decodeFalse();
                break;

            case 'n':
                value = decodeNull();
                break;

            case '[':
                value = decodeJsonArray();
                break;


            case '{':
                value = decodeJsonObject();
                break;

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                value = decodeNumber( false );
                break;
            case '-':
                value = decodeNumber( true );
                break;

            default:
                throw new JsonException( exceptionDetails( "Unable to determine the " +
                        "current character, it is not a string, number, array, or object" ) );

        }

        return value;
    }


    private final Object decodeNumber( boolean minus ) {

        char[] array = charArray;

        final int startIndex = __index;
        int index = __index;
        char currentChar;
        boolean doubleFloat = false;
        boolean simple = true;
        int digitsPastPoint = 0;
        int sign = 1;


        if ( minus ) {
            minus = true;
            sign = -1;
            nextChar();
        }


        while ( true ) {
            currentChar = array[index];

            if ( doubleFloat ) {
                digitsPastPoint++;
            }
            if ( isNumberDigit( currentChar ) ) {
                //noop
            } else if ( currentChar <= 32 ) { //white
                break;
            } else if ( isDelimiter( currentChar ) ) {
                break;
            } else if ( isDecimalChar( currentChar ) ) {
                doubleFloat = true;
                if ( currentChar != '.' ) {
                    simple = false;
                }
            }
            index++;
            if ( index >= array.length ) break;
        }

        __index = index;
        __currentChar = currentChar;

        return getNumberFromSpan( startIndex, doubleFloat, simple, digitsPastPoint, minus, sign );
    }

    private final Object getNumberFromSpan( int startIndex, boolean doubleFloat, boolean simple, int digitsPastPoint, boolean minus, int sign ) {
        Object value;
        if ( doubleFloat ) {
            value = CharScanner.simpleDouble( this.charArray, simple, minus, digitsPastPoint - 1, startIndex, __index );
        } else {

            if ( isInteger( this.charArray, startIndex, __index - startIndex, minus ) ) {
                value = CharScanner.parseIntFromTo( charArray, startIndex, __index  ) * sign;
            } else {
                value = CharScanner.parseLongFromTo( charArray, startIndex, __index  ) * sign;
            }

        }

        return value;
    }


    protected static final char[] NULL = Chr.chars( "null" );

    protected final Object decodeNull() {

        if ( __index + NULL.length <= charArray.length ) {
            if ( charArray[__index] == 'n' &&
                    charArray[++__index] == 'u' &&
                    charArray[++__index] == 'l' &&
                    charArray[++__index] == 'l' ) {
                __index++;
                return null;
            }
        }
        throw new JsonException( exceptionDetails( "null not parse properly" ) );
    }


    protected static final char[] TRUE = Chr.chars( "true" );

    protected final boolean decodeTrue() {

        if ( __index + TRUE.length <= charArray.length ) {
            if ( charArray[__index] == 't' &&
                    charArray[++__index] == 'r' &&
                    charArray[++__index] == 'u' &&
                    charArray[++__index] == 'e' ) {

                __index++;
                return true;

            }
        }

        throw new JsonException( exceptionDetails( "true not parsed properly" ) );
    }


    protected static char[] FALSE = Chr.chars( "false" );

    protected final boolean decodeFalse() {

        if ( __index + FALSE.length <= charArray.length ) {
            if ( charArray[__index] == 'f' &&
                    charArray[++__index] == 'a' &&
                    charArray[++__index] == 'l' &&
                    charArray[++__index] == 's' &&
                    charArray[++__index] == 'e' ) {
                __index++;
                return false;
            }
        }
        throw new JsonException( exceptionDetails( "false not parsed properly" ) );
    }


    private CharBuf builder = CharBuf.create( 20 );

    private String decodeString() {

        char[] array = charArray;
        int index = __index;
        char currentChar = array[index];

        if ( index < array.length && currentChar == '"' ) {
            index++;
        }

        final int startIndex = index;


        boolean encoded = hasEscapeChar( array, index, indexHolder );
        index = indexHolder[0];


        String value = null;
        if ( encoded ) {
            index = findEndQuote( array, index );
            value = builder.decodeJsonString( array, startIndex, index ).toString();
            builder.recycle();
        } else {
            value = new String( array, startIndex, ( index - startIndex ) );
        }

        if ( index < charArray.length ) {
            index++;
        }
        __index = index;
        return value;
    }

    protected final List decodeJsonArray() {


        ArrayList<Object> list = null;

        boolean foundEnd = false;
        char[] charArray = this.charArray;

        try {
            if ( __currentChar == '[' ) {
                __index++;
            }

            int lastIndex;

            skipWhiteSpace();


        /* the list might be empty  */
            if ( __currentChar == ']' ) {
                __index++;
                return Collections.EMPTY_LIST;
            }

            list = new ArrayList();


            while ( this.hasMore() ) {

                Object arrayItem = decodeValueInternal();

                list.add( arrayItem );


                char c = charArray[__index];


                if ( c == ',' ) {
                    __index++;
                    continue;
                } else if ( c == ']' ) {
                    __index++;
                    foundEnd = true;
                    break;
                }

                lastIndex = __index;
                skipWhiteSpace();


                c = charArray[__index];

                if ( c == ',' ) {
                    __index++;
                    continue;
                } else if ( c == ']' && lastIndex != __index ) {
                    __index++;
                    foundEnd = true;
                    break;
                } else {

                    String charString = charDescription( c );

                    complain(
                            String.format( "expecting a ',' or a ']', " +
                                    " but got \nthe current character of  %s " +
                                    " on array index of %s \n", charString, list.size() )
                    );

                }
            }

        } catch ( Exception ex ) {
            if ( ex instanceof JsonException ) {
                JsonException jsonException = ( JsonException ) ex;
                throw jsonException;
            }
            throw new JsonException( exceptionDetails( "issue parsing JSON array" ), ex );
        }
        if ( !foundEnd ) {
            complain( "Did not find end of Json Array" );
        }
        return list;

    }

    protected final char currentChar() {
        if ( __index > lastIndex ) {
            return 0;
        } else {
            return charArray[__index];
        }
    }


    public Object parse( char[] chars ) {
        return this.decodeFromChars( chars );
    }


}