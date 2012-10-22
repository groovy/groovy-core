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
package org.codehaus.groovy.vmplugin.v7;

import groovy.lang.GString;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.groovy.GroovyBugError;

/**
 * This class contains several transformers for used during method invocation.
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 */
public class TypeTransformers {
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodHandle 
        TO_STRING, TO_BYTE,   TO_INT,     TO_LONG,    TO_SHORT,
        TO_FLOAT,  TO_DOUBLE, TO_BIG_INT, TO_BIG_DEC;
    static {
        try {
            TO_STRING   = LOOKUP.findVirtual(Object.class, "toString",      MethodType.methodType(String.class));
            TO_BYTE	    = LOOKUP.findVirtual(Number.class, "byteValue",     MethodType.methodType(Byte.TYPE));
            TO_SHORT    = LOOKUP.findVirtual(Number.class, "shortValue",    MethodType.methodType(Short.TYPE));
            TO_INT      = LOOKUP.findVirtual(Number.class, "intValue",      MethodType.methodType(Integer.TYPE));
            TO_LONG     = LOOKUP.findVirtual(Number.class, "longValue",     MethodType.methodType(Long.TYPE));
            TO_FLOAT    = LOOKUP.findVirtual(Number.class, "floatValue",    MethodType.methodType(Float.TYPE));
            TO_DOUBLE   = LOOKUP.findVirtual(Number.class, "doubleValue",   MethodType.methodType(Double.TYPE));

            // BigDecimal conversion is done by using the double value
            // if the given number.
            MethodHandle tmp = LOOKUP.findConstructor(BigDecimal.class, MethodType.methodType(Void.TYPE, Double.TYPE));
            TO_BIG_DEC  = MethodHandles.filterReturnValue(TO_DOUBLE, tmp);

            // BigInteger conversion is done by using the string representation
            // if the given number
            tmp = LOOKUP.findConstructor(BigInteger.class, MethodType.methodType(Void.TYPE, String.class));
            TO_BIG_INT  = MethodHandles.filterReturnValue(TO_STRING, tmp);
        } catch (Exception e) {
            throw new GroovyBugError(e);
        }
    }

    protected static MethodHandle addTransformer(MethodHandle handle, int pos, Object arg, Class parameter) {
        MethodHandle transformer=null;
    	if (arg instanceof GString) {
    		transformer = TO_STRING;
        } else if (Number.class.isAssignableFrom(parameter)) {
            transformer = selectNumberTransformer(parameter, arg);
        } 
        if (transformer==null) throw new GroovyBugError("Unknown transformation for argument "+arg+" at position "+pos+" with "+arg.getClass()+" for parameter of type "+parameter);
        return applyUnsharpFilter(handle, pos, transformer);
    }
    
    public static MethodHandle applyUnsharpFilter(MethodHandle handle, int pos, MethodHandle transformer) {
        MethodType type = transformer.type();
        Class given = handle.type().parameterType(pos);
        if (type.returnType() != given || type.parameterType(0) != given) {
            transformer = transformer.asType(MethodType.methodType(given, type.parameterType(0)));
        }
        return MethodHandles.filterArguments(handle, pos, transformer);
    }

    private static MethodHandle selectNumberTransformer(Class param, Object arg) {
        param = TypeHelper.getWrapperClass(param);
        if (param == Byte.class) {
            return TO_BYTE;
        } else if (param == Character.class || param == Integer.class) {
            return TO_INT;
        } else  if (param == Long.class) {
            return TO_LONG;
        } else if (param == Float.class) {
            return TO_FLOAT;
        } else if (param == Double.class) {
            return TO_DOUBLE;
        } else if (param == BigInteger.class) {
            return TO_BIG_INT;
        } else if (param == BigDecimal.class) {
            return TO_BIG_DEC;
        } else if (param == Short.class) {
            return TO_SHORT;
        } else {
             return null;
        }
    }
}
