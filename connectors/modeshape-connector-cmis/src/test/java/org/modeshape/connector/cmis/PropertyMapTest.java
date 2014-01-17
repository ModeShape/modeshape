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
package org.modeshape.connector.cmis;

import static org.junit.Assert.assertEquals;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author kulikov
 */
public class PropertyMapTest {

    private Properties pmap = new Properties(null);

    public PropertyMapTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of findJcrName method, of class PropertyMap.
     */
    @Test
    public void testFindJcrName() {
        assertEquals("jcr:uuid", pmap.findJcrName("cmis:objectId"));
        assertEquals("cmis:custom", pmap.findJcrName("cmis:custom"));
    }

    /**
     * Test of findCmisName method, of class PropertyMap.
     */
    @Test
    public void testFindCmisName() {
        assertEquals("cmis:objectId", pmap.findCmisName("jcr:uuid"));
        assertEquals("jcr:custom", pmap.findCmisName("jcr:custom"));
    }
}
