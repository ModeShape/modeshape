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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit test for {@link StringURLClassLoader}
 *
 * @author Horia Chiorean
 */
public class StringURLClassLoaderTest {

    private static final String RESOURCE_PATH = StringURLClassLoaderTest.class.getPackage().getName().replaceAll("\\.", "/") + "/additionalmime.types";
    private static final URL RESOURCE_URL = StringURLClassLoaderTest.class.getClassLoader().getResource(RESOURCE_PATH);

    @Before
    public void setUp() throws Exception {
        assertNotNull(RESOURCE_URL);
    }

    @Test
    public void shouldNotResolveURLsIfEmptyListProvided() throws Exception {
        StringURLClassLoader classLoader = new StringURLClassLoader(Collections.<String>emptyList());
        assertNull(classLoader.getResource(RESOURCE_PATH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNullArgument() throws Exception {
        new StringURLClassLoader(null);
    }

    @Test
    public void shouldResolveResourceIfURLProvided() throws Exception {
        String folderUrl = RESOURCE_URL.toString().substring(0, RESOURCE_URL.toString().indexOf(RESOURCE_PATH));
        StringURLClassLoader classLoader = new StringURLClassLoader(Arrays.asList("invalidURL", folderUrl));
        assertNotNull(classLoader.getResource(RESOURCE_PATH));
    }
}
