/*
 * Copyright 2008 the original author or authors.
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

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class annotation to make class singleton.
 *
 * Singleton can be initialized in static initialization of the class or lazily (on first access)
 * To make singleton lazy it is enough to use {@code @Singleton(lazy=true)}
 * Lazy singletons implemented with double check locking and volatile field
 *
 * @author Alex Tkachman
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.codehaus.groovy.transform.SingletonASTTransformation")
public @interface Singleton {
    /**
     * @return if this singleton should be lazy
     */
    boolean lazy () default false;


    /**
     * @return the singleton property name
     */
    String property() default "instance";
}
