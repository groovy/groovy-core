/*
 * Copyright 2003-2009 the original author or authors.
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
package groovy.transform;

/**
 * This enumeration can be used whenever it is preferred to annotate a class as
 * {@link TypeChecked} in general, but where only one or more methods are "dynamic". This allows the user
 * to annotate the class itself then annotate only the methods which require exclusion.
 *
 * @author Cedric Champeau
 */
public enum TypeCheckingMode {
    PASS,
    SKIP
}
