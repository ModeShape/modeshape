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

package org.modeshape.common.util;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit test for {@link DelegatingClassLoader}
 *
 * @author Horia Chiorean
 */
public class DelegatingClassLoaderTest {

    private static final URLClassLoader EMPTY_URL_CLASS_LOADER = new URLClassLoader(new URL[0], null);

    @Test(expected = IllegalArgumentException.class )
    public void shouldNotAllowNullDelegates() throws Exception {
        new DelegatingClassLoader(null, null);
    }

    @Test
    public void shouldAllowNullParent() throws Exception {
        new DelegatingClassLoader(null, Collections.<ClassLoader>emptyList());
    }

    @Test(expected = ClassNotFoundException.class)
    public void shouldNotClassIfNoDelegateCanFindIt() throws Exception {
        DelegatingClassLoader classLoader = new DelegatingClassLoader(null, Arrays.asList(EMPTY_URL_CLASS_LOADER));
        classLoader.loadClass(DelegatingClassLoaderTest.class.getName());
    }

    @Test
    public void shouldFindClassIfOneDelegateCanFindIt() throws Exception {
        DelegatingClassLoader classLoader = new DelegatingClassLoader(null, Arrays.asList(EMPTY_URL_CLASS_LOADER,
                                                                                          DelegatingClassLoaderTest.class.getClassLoader()));
        assertNotNull(classLoader.loadClass(DelegatingClassLoaderTest.class.getName()));
    }

    @Test
    public void shouldFindClassIfParentCanFindIt() throws Exception {
        DelegatingClassLoader classLoader = new DelegatingClassLoader(DelegatingClassLoaderTest.class.getClassLoader(),
                                                                      Arrays.asList(EMPTY_URL_CLASS_LOADER));
        assertNotNull(classLoader.loadClass(DelegatingClassLoaderTest.class.getName()));
    }
}
