package org.codehaus.groovy.reflection;

import org.codehaus.groovy.util.ManagedConcurrentMap;
import org.codehaus.groovy.util.ReferenceBundle;

/** Approximation of Java 7's {@link java.lang.ClassValue} that works on earlier versions of Java.
 * Note that this implementation isn't as good at Java 7's; it doesn't allow for some GC'ing that Java 7 would allow.
 * But, it's good enough for our use.
 *
 * @param <T>
 */
public class GroovyClassValuePreJava7<T> implements GroovyClassValue<T> {
	private static final ReferenceBundle weakBundle = ReferenceBundle.getWeakBundle();

	private class EntryWithValue extends ManagedConcurrentMap.EntryWithValue<Class<?>,T>{

		public EntryWithValue(GroovyClassValuePreJava7Segment segment, Class<?> key, int hash) {
			super(weakBundle, segment, key, hash, computeValue.computeValue(key));
		}

	}
	
	private class GroovyClassValuePreJava7Segment extends ManagedConcurrentMap.Segment<Class<?>,T> {

		GroovyClassValuePreJava7Segment(ReferenceBundle bundle, int initialCapacity) {
			super(bundle, initialCapacity);
		}

		@Override
		protected EntryWithValue createEntry(Class<?> key, int hash,
				T unused) {
			return new EntryWithValue(this, key, hash);
		}
	}
	
	private class GroovyClassValuePreJava7Map extends ManagedConcurrentMap<Class<?>,T> {

		public GroovyClassValuePreJava7Map() {
			super(weakBundle);
		}

		@Override
		protected GroovyClassValuePreJava7Segment createSegment(Object segmentInfo, int cap) {
			ReferenceBundle bundle = (ReferenceBundle) segmentInfo;
			if (bundle==null) throw new IllegalArgumentException("bundle must not be null ");
			return new GroovyClassValuePreJava7Segment(bundle, cap);
		}

	}
	
	private final ComputeValue<T> computeValue;

	private final GroovyClassValuePreJava7Map map = new GroovyClassValuePreJava7Map();
	
	public GroovyClassValuePreJava7(ComputeValue<T> computeValue){
		this.computeValue = computeValue;
	}

	@Override
	public T get(Class<?> type) {
		// the value isn't use in the getOrPut call - see the EntryWithValue constructor above
		T value = map.getOrPut(type, null).getValue(); //all entries are guaranteed to be EntryWithValue - value can never be null
		return value;
	}

	@Override
	public void remove(Class<?> type) {
		map.remove(type);
	}

}
