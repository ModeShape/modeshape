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
package org.infinispan.schematic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.junit.Before;
import org.junit.Test;

public class SchematicDbTest extends AbstractSchematicDbTest {

    private volatile boolean print = false;

    @Before
    public void beforeEach() {
        print = false;
    }

    protected static InputStream resource( String resourcePath ) {
        InputStream result = SchemaValidationTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assert result != null : "Could not find resource \"" + resourcePath + "\"";
        return result;
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything"; 
        db.put(key, doc);
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.getContent();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assertThat(readMetadata, is(notNullValue()));
        assertThat(readMetadata.getString("id"), is(key));
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNonNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        db.put(key, doc);

        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.getContent();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assert readMetadata != null;
        assert readMetadata.getString("id").equals(key);
    }

    @Test
    public void shouldStoreDocumentAndFetchAndModifyAndRefetch() throws Exception {
        // Store the document ...
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        db.put(key, doc);
        
        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.getContent();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Modify using an editor ...
        try {
            tm.begin();
            db.lock(key);
            EditableDocument editable = db.editContent(key, true);
            editable.setBoolean("k3", true);
            editable.setNumber("k4", 3.5d);
        } finally {
            tm.commit();
        }

        // Now re-read ...
        SchematicEntry entry2 = db.get(key);
        Document read2 = entry2.getContent();
        assertThat(read2, is(notNullValue()));
        assertThat(read2.getString("k1"), is("value1"));
        assertThat(read2.getInteger("k2"), is(2));
        assertThat(read2.getBoolean("k3"), is(true));
        assertThat(read2.getDouble("k4") > 3.4d, is(true));
    }

    @Test
    public void shouldStoreDocumentAndFetchAndModifyAndRefetchUsingTransaction() throws Exception {
        // Store the document ...
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        db.put(key, doc);
        
        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.getContent();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Modify using an editor ...
        try {
            tm.begin();
            db.lock(key);
            EditableDocument editable = db.editContent(key, true);
            editable.setBoolean("k3", true);
            editable.setNumber("k4", 3.5d);
        } finally {
            tm.commit();
        }

        // Now re-read ...
        SchematicEntry entry2 = db.get(key);
        Document read2 = entry2.getContent();
        assertThat(read2, is(notNullValue()));
        assertThat(read2.getString("k1"), is("value1"));
        assertThat(read2.getInteger("k2"), is(2));
        assertThat(read2.getBoolean("k3"), is(true));
        assertThat(read2.getDouble("k4") > 3.4d, is(true));
    }

    @FixFor( "MODE-1734" )
    @Test
    public void shouldAllowMultipleConcurrentWritersToUpdateEntryInSerialFashion() throws Exception {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        final String key = "can be anything";
        db.put(key, doc);
        SchematicEntry entry = db.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));
        // Start two threads that each attempt to edit the document ...
        ExecutorService executors = Executors.newCachedThreadPool();
        final CountDownLatch latch = new CountDownLatch(1);
        Future f1 = executors.submit(new Callable<Void>() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Void call() throws Exception {
                latch.await(); // synchronize ...
                tm().begin();
                print("Began txn1");
                db.lock(key);
                EditableDocument editor = db.editContent(key, true);
                editor.setNumber("k2", 3); // update an existing field
                print(editor);
                print("Committing txn1");
                tm().commit();
                return null;
            }
        });
        Future f2 = executors.submit(new Callable<Void>() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Void call() throws Exception {
                latch.await(); // synchronize ...
                tm().begin();
                print("Began txn2");
                db.lock(key);
                EditableDocument editor = db.editContent(key, true);
                editor.setNumber("k3", 3); // add a new field
                print(editor);
                print("Committing txn2");
                tm().commit();
                return null;
            }
        });
        // print = true;
        // Start the threads ...
        latch.countDown();
        // Wait for the threads to die ...
        f1.get();
        f2.get();
        // System.out.println("Completed all threads");
        // Now re-read ...
        tm().begin();
        Document read = db.get(key).getContent();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k3"), is(3)); // Thread 2 is last, so this should definitely be there
        assertThat(read.getInteger("k2"), is(3)); // Thread 1 is first, but still shouldn't have been overwritten
        tm().commit();
    }

    protected void print( Object obj ) {
        if (print) { System.out.println(obj); }
    }

}
