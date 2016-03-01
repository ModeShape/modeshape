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
import org.junit.After;
import org.junit.Before;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.document.ParsingException;

/**
 * Base class for the unit tests around {@link org.modeshape.schematic.SchematicDb}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class AbstractRelationalDbTest {
    protected static final Document DEFAULT_CONTENT ;

    protected RelationalDb db;
    protected boolean print = true;
    
    static {
        try {
            DEFAULT_CONTENT = Json.read(AbstractRelationalDbTest.class.getClassLoader().getResourceAsStream("document.json"));
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Before
    public void before() throws Exception {
        db = Schematic.getDb(RelationalDbTest.class.getClassLoader().getResourceAsStream("db-config.json"));
        db.start();
        // run a query to validate that the table has been created and is empty.
        assertEquals(0, db.keys().size());
    }

    @After
    public void after() throws Exception {
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
            SchematicEntry entry = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
            db.putEntry(entry.source());
            return entry;
        });
    }


    protected List<SchematicEntry> randomEntries(int sampleSize) throws Exception {
        return IntStream.range(0, sampleSize).mapToObj(i -> SchematicEntry.create(
                UUID.randomUUID().toString(), DEFAULT_CONTENT)).collect(Collectors.toList());
    }
    
    protected void print(String s) {
        if (print) {
            System.out.println(Thread.currentThread().getName() + ": " + s);
        }
    }
}
