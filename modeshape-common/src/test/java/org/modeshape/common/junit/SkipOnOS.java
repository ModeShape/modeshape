/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
