package org.codehaus.groovy.reflection;

/** Simple delegate to Java 7 and later's {@link java.lang.ClassValue}
 *
 * @param <T>
 */
public class GroovyClassValueJava7<T> extends ClassValue<T> implements GroovyClassValue<T> {
	private final ComputeValue<T> computeValue;
	
	public GroovyClassValueJava7(ComputeValue<T> computeValue){
		this.computeValue = computeValue;
	}

	@Override
	protected T computeValue(Class<?> type) {
		return computeValue.computeValue(type);
	}

}
