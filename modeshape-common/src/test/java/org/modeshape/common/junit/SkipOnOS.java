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

package org.modeshape.common.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation used together with the {@link SkipTestRule} JUnit rule, that allows tests to be excluded
 * from the build if they are run on certain platforms.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE})
public @interface SkipOnOS {

    /**
     * Symbolic constant used to determine the Windows operating system, from the "os.name" system property.
     */
    String WINDOWS = "windows";

    /**
     * Symbolic constant used to determine the OS X operating system, from the "os.name" system property.
     */
    String MAC = "mac";

    /**
     * Symbolic constant used to determine the Linux operating system, from the "os.name" system property.
     */
    String LINUX = "linux";

    /**
     * The list of OS names on which the test should be skipped.
     *
     * @return a list of "symbolic" OS names.
     * @see #WINDOWS
     * @see #MAC
     * @see #LINUX
     */
    String[] value();

    /**
     * An optional description which explains why the test should be skipped.
     *
     * @return a string which explains the reasons for skipping the test.
     */
    String description() default "";
}
