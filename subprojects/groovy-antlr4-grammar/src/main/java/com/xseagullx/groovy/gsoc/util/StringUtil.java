package com.xseagullx.groovy.gsoc.util;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtil {
    public static String replaceHexEscapes(String text) {
        Pattern p = Pattern.compile("\\\\u([0-9abcdefABCDEF]{4})");
	    return DefaultGroovyMethods.replaceAll(text, p, new Closure<Void>(null, null) {
		    Object doCall(String _0, String _1) {
			    return Character.toChars(Integer.parseInt(_1, 16));
		    }
	    });
    }

	public static String replaceOctalEscapes(String text) {
	    Pattern p = Pattern.compile("\\\\([0-3]?[0-7]?[0-7])");
	    return DefaultGroovyMethods.replaceAll(text, p, new Closure<Void>(null, null) {
		    Object doCall(String _0, String _1) {
			    return Character.toChars(Integer.parseInt(_1, 8));
		    }
	    });
    }

    private static Map<Character, Character> standardEscapes = new HashMap<Character, Character>();
	static {
        standardEscapes.put('b', '\b');
        standardEscapes.put('t', '\t');
        standardEscapes.put('n', '\n');
        standardEscapes.put('f', '\f');
        standardEscapes.put('r', '\r');
    }

	public static String replaceStandardEscapes(String text) {
	    Pattern p = Pattern.compile("\\\\([btnfr\"'\\\\])");
	    return DefaultGroovyMethods.replaceAll(text, p, new Closure<Void>(null, null) {
		    Object doCall(String _0, String _1) {
			    Character character = standardEscapes.get(_1.charAt(0));
			    return character != null ? character : _1;
		    }
	    });
    }
}
