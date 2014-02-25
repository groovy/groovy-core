/*
 * Copyright 2003-2013 the original author or authors.
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

package groovy.lang;

import java.util.*;

/**
 * Interceptor that registers the timestamp of each method call
 * before and after invocation. The timestamps are stored internally
 * and can be retrieved through the with the <pre>getCalls()</pre>
 * and <pre>statistic()</pre> API.
 * <p/>
 * Example usage:
 * <pre>
 * def proxy = ProxyMetaClass.getInstance(ArrayList.class)
 * proxy.interceptor = new BenchmarkInterceptor()
 * proxy.use {
 *     def list = (0..10000).collect{ it }
 *     4.times { list.size() }
 *     4000.times { list.set(it, it+1) }
 * }
 * proxy.interceptor.statistic()
 * </pre>
 * Which produces the following output:
 * <pre>
 * [[size, 4, 0], [set, 4000, 21]]
 * </pre>
 */
public class BenchmarkInterceptor implements Interceptor {

    protected Map<String, LinkedList<Long>> calls = new LinkedHashMap<String, LinkedList<Long>>(); // keys to list of invocation times and before and after

    /**
     * Returns the raw data associated with the current benchmark run.
     */
    public Map<String, LinkedList<Long>> getCalls() {
        return calls;
    }

    /**
     * Resets all the benchmark data on this object.
     */
    public void reset() {
        calls = new HashMap<String, LinkedList<Long>>();
    }

    public Object beforeInvoke(Object object, String methodName, Object[] arguments) {
        if (!calls.containsKey(methodName)) calls.put(methodName, new LinkedList<Long>());
        (calls.get(methodName)).add(System.currentTimeMillis());

        return null;
    }

    public Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
        (calls.get(methodName)).add(System.currentTimeMillis());
        return result;
    }

    public boolean doInvoke() {
        return true;
    }

    /**
     * Returns benchmark statistics as a List&lt;Object[]&gt;.
     * AccumulateTime is measured in milliseconds and is as accurate as
     * System.currentTimeMillis() allows it to be.
     *
     * @return a list of lines, each item is [methodname, numberOfCalls, accumulatedTime]
     */
    public List<Object[]> statistic() {
        List<Object[]> result = new LinkedList<Object[]>();
        for (String s : calls.keySet()) {
            Object[] line = new Object[3];
            result.add(line);
            line[0] = s;
            List<Long> times = calls.get(line[0]);
            line[1] = times.size() / 2;
            int accTime = 0;
            for (Iterator<Long> it = times.iterator(); it.hasNext(); ) {
                Long start = it.next();
                Long end = it.next();
                accTime += end - start;
            }
            line[2] = (long) accTime;
        }
        return result;
    }
}
