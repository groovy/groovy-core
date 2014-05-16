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
package org.codehaus.groovy.runtime;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.runtime.InvokerHelper;
import java.util.Optional;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.groovy.runtime.InvokerHelper;
    
/**
 * @author Uehara Junji(@uehaj)
 */
public class OptionalGroovyMethods {

    public static Object methodMissing(Optional self, String name, Object args0) {
        Object[] args = (Object[])args0;
        for (int i=0; i<args.length; i++) {
            if (args[i] instanceof Optional) {
                if (((Optional)args[i]).isPresent()) {
                    args[i] = ((Optional)args[i]).get();
                }
                else {
                    return Optional.empty();
                }
            }
        }
        if (self.isPresent()) {
            Object result = InvokerHelper.invokeMethod(self.get(), name, args);
            return Optional.ofNullable(result);
        }
        else {
            return Optional.empty();
        }
    }

}

