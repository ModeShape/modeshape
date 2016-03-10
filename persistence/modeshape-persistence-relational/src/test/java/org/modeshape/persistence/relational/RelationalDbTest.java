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
package org.modeshape.persistence.relational;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import org.junit.After;
import org.junit.Before;
import org.modeshape.schematic.AbstractSchematicDBTest;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicDb;

/**
 * Unit test for {@link RelationalDb}. The configuration used for this test is filtered by Maven based on the active
 * DB profile.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RelationalDbTest extends AbstractSchematicDBTest {

    @Override
    protected SchematicDb getDb() throws Exception {
        return Schematic.getDb(RelationalDbTest.class.getClassLoader().getResourceAsStream("db-config.json"));
    }

    @Before
    public void before() throws Exception {
        super.before();
        // run a query to validate that the table has been created and is empty.
        assertEquals(0, db.keys().size());
    }

    @After
    public void after() throws Exception {
        super.after();
        // run a query to check that the table has been removed
        try {
            db.keys();
            fail("The DB table should have been dropped...");
        } catch (RelationalProviderException e) {
            //expected
        }
    }
}
