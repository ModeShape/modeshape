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
public class ObjectIdTest {

    public ObjectIdTest() {
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
     * Test of valueOf method, of class ObjectId.
     */
    @Test
    public void testContentType() {
        String uuid = "1234-5678--76fs/content";
        ObjectId ID = ObjectId.valueOf(uuid);

        assertEquals(ObjectId.Type.CONTENT, ID.getType());
        assertEquals("1234-5678--76fs", ID.getIdentifier());
    }

    @Test
    public void testRepository() {
        String uuid = "/repository_info";
        ObjectId ID = ObjectId.valueOf(uuid);

        assertEquals(ObjectId.Type.REPOSITORY_INFO, ID.getType());
    }

    @Test
    public void testObjectType() {
        String uuid = "1234-5678--76fs";
        ObjectId ID = ObjectId.valueOf(uuid);

        assertEquals(ObjectId.Type.OBJECT, ID.getType());
        assertEquals("1234-5678--76fs", ID.getIdentifier());
    }

}
