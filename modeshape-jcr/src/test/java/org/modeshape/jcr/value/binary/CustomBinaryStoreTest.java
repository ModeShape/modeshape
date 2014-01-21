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
package org.modeshape.jcr.value.binary;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * @author kulikov
 */
public class CustomBinaryStoreTest {
    private RepositoryConfiguration config;

    public CustomBinaryStoreTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        config = RepositoryConfiguration.read("config/custom-binary-storage.json");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void shouldLoadAndConfigureCustomBinaryStore() throws Exception {
        CustomBinaryStoreImpl store = (CustomBinaryStoreImpl)config.getBinaryStorage().getBinaryStore();
        assertThat(store, is(notNullValue()));
        assertEquals("value", store.getKey());
    }

}
