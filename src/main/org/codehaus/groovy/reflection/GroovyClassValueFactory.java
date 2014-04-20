package org.codehaus.groovy.reflection;

import java.lang.reflect.Constructor;

import org.codehaus.groovy.reflection.GroovyClassValue.ComputeValue;

public class GroovyClassValueFactory {
	private static final Constructor groovyClassValueConstructor;
	
	static {
		boolean javaLangClassValueExists;
		Class groovyClassValueClass;
		try{
			Class.forName("java.lang.ClassValue");
			javaLangClassValueExists = true;
		}catch(ClassNotFoundException e){
			javaLangClassValueExists = false;
		}
		if(javaLangClassValueExists){
			try {
				groovyClassValueClass =  Class.forName("org.codehaus.groovy.reflection.GroovyClassValueJava7");
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e); // this should never happen, but if it does, let it propagate and be fatal
			}
		}else{
			groovyClassValueClass = GroovyClassValuePreJava7.class;
		}
		try{
			groovyClassValueConstructor = groovyClassValueClass.getConstructor(ComputeValue.class);
		}catch(Exception e){
			throw new RuntimeException(e); // this should never happen, but if it does, let it propagate and be fatal
		}
	}
	
	public static <T> GroovyClassValue<T> createGroovyClassValue(ComputeValue<T> computeValue){
		try {
			return (GroovyClassValue<T>) groovyClassValueConstructor.newInstance(computeValue);
		} catch (Exception e) {
			throw new RuntimeException(e); // this should never happen, but if it does, let it propagate and be fatal
		}
	}
}
