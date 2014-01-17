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
package org.modeshape.common.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
