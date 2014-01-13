/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.Ignore;
import org.junit.Test;

/**
 * An annotation that describes a test as for measuring performance. Such tests often take significantly longer than most unit
 * tests, and so they are often {@link Ignore}d most of the time and run only manually. </p>
 * 
 * @see Test
 * @see Ignore
 */
@Documented
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Retention( RetentionPolicy.SOURCE )
public @interface Performance {
}
