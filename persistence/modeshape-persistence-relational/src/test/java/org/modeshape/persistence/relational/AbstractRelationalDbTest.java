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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;

/**
 * Base class for the unit tests around {@link org.modeshape.schematic.SchematicDb}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class AbstractRelationalDbTest {
    protected static Document defaultContent;
    protected static RelationalDb db;
    
    protected static boolean print = false;

    @BeforeClass
    public static void beforeAll() throws Exception {
        ClassLoader cl = RelationalDbTest.class.getClassLoader();
        defaultContent  = Json.read(cl.getResourceAsStream("document.json"));
        db = Schematic.getDb(cl.getResourceAsStream("db-config.json"));
        db.start();
        // run a query to validate that the table has been created and is empty.
        assertEquals(0, db.keys().count());
    }

    @AfterClass
    public static void afterAll() throws Exception {
        db.stop();
        // run a query to check that the table has been removed
        try {
            db.keys();
            fail("The DB table should have been dropped...");
        } catch (RelationalProviderException e) {
            //expected
        }
    }

    protected  <T> T simulateTransaction(Callable<T> operation) throws Exception {
        db.txStarted("0");
        T result = operation.call();
        db.txCommitted("0");
        return result;
    }

    protected SchematicEntry writeSingleEntry() throws Exception {
        return simulateTransaction(() -> {
            SchematicEntry entry = SchematicEntry.create(UUID.randomUUID().toString(), defaultContent);
            db.putEntry(entry.source());
            return entry;
        });
    }


    protected List<SchematicEntry> randomEntries(int sampleSize) throws Exception {
        return IntStream.range(0, sampleSize).mapToObj(i -> SchematicEntry.create(
                UUID.randomUUID().toString(), defaultContent)).collect(Collectors.toList());
    }
}
