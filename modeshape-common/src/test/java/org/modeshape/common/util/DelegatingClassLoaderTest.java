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
