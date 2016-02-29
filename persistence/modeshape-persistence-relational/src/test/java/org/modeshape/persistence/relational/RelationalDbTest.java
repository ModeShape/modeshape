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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.internal.document.BasicDocument;

/**
 * Unit test for {@link RelationalDb}. The configuration used for this test is filtered by Maven based on the active
 * DB profile.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RelationalDbTest extends AbstractRelationalDbTest {

    private static final String VALUE_FIELD = "value";
 
    @Test
    public void shouldGetAndPut() throws Exception {
        List<SchematicEntry> dbEntries = randomEntries(3);
        //simulate the start of a transaction
        db.txStarted("0");
        
        //write some entries without committing
        dbEntries.forEach(dbEntry -> db.put(dbEntry.id(), dbEntry.content()));
        Set<String> expectedIds = dbEntries.stream().map(SchematicEntry::id).collect(Collectors.toCollection(TreeSet::new));
        // check that the same connection is used and the entries are still there
        assertTrue(db.keys().containsAll(expectedIds));
        // simulate a commit for the write
        db.txCommitted("0");
        // check that the entries are still there
        assertTrue(db.keys().containsAll(expectedIds));
        // check that for each entry the content is correctly stored
        dbEntries.stream().forEach(entry -> assertEquals(entry.content(), db.getEntry(entry.id()).content()));
        
        // update one of the documents and check the update is correct
        SchematicEntry firstEntry = dbEntries.get(0);
        String idToUpdate = firstEntry.id();
        EditableDocument updatedDocument = firstEntry.content().edit(true);
        updatedDocument.setNumber(VALUE_FIELD, 2);
        
        //simulate a new transaction
        db.txStarted("1");
        db.get(idToUpdate);
        db.put(idToUpdate, updatedDocument);
        assertEquals(updatedDocument, db.getEntry(idToUpdate).content());
        db.txCommitted("1");
        assertEquals(updatedDocument, db.getEntry(idToUpdate).content());
    }
    
    @Test
    public void shouldReadSchematicEntry() throws Exception {
        SchematicEntry entry = writeSingleEntry();
        SchematicEntry schematicEntry = db.getEntry(entry.id());
        assertNotNull(schematicEntry);
        assertTrue(db.containsKey(entry.id()));
    }
    
    @Test
    public void shouldEditContentDirectly() throws Exception {
        // test the editing of content for an existing entry
        SchematicEntry entry = writeSingleEntry();
        EditableDocument editableDocument = simulateTransaction(() -> db.editContent(entry.id(), false));
        assertNotNull(editableDocument);
        assertEquals(entry.content(), editableDocument);
        simulateTransaction(() -> {
            EditableDocument document = db.editContent(entry.id(), false);
            document.setNumber(VALUE_FIELD, 2);
            return null;
        });
        Document doc = db.getEntry(entry.id()).content();
        assertEquals(2, (int) doc.getInteger(VALUE_FIELD));
        
        // test the editing of content for a new entry which should be create
        String newId = UUID.randomUUID().toString();
        EditableDocument newDocument = simulateTransaction(() -> db.editContent(newId, true));
        assertNotNull(newDocument);
        // the content in the DB should be an empty schematic entry...
        SchematicEntry schematicEntry = db.getEntry(newId);
        assertEquals(newId, schematicEntry.id());   
        assertEquals(new BasicDocument(), schematicEntry.content());   
        
        // test the editing of a non-existing id without creating a new entry for it
        newDocument = simulateTransaction(() -> db.editContent(UUID.randomUUID().toString(), false));
        assertNull(newDocument);
    }
    
    @Test
    public void shouldPutIfAbsent() throws Exception {
        SchematicEntry entry = writeSingleEntry();
        EditableDocument editableDocument = entry.content().edit(true);
        editableDocument.setNumber(VALUE_FIELD, 100);
        SchematicEntry updatedEntry = simulateTransaction(() -> db.putIfAbsent(entry.id(), entry.content()));
        assertNotNull(updatedEntry);
        assertEquals(1, (int) updatedEntry.content().getInteger(VALUE_FIELD));
        
        SchematicEntry newEntry = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
        assertNull(simulateTransaction(() -> db.putIfAbsent(newEntry.id(), newEntry.content())));
        updatedEntry = db.getEntry(newEntry.id());
        assertNotNull(updatedEntry);
    }
    
    @Test
    public void shouldPutSchematicEntry() throws Exception {
        SchematicEntry originalEntry = super.randomEntries(1).get(0);
        simulateTransaction(() -> {
            db.putEntry(originalEntry.source());
            return null;
        });
        
        SchematicEntry actualEntry = db.getEntry(originalEntry.id());
        assertNotNull(actualEntry);
        assertEquals(originalEntry.getMetadata(), actualEntry.getMetadata());
        assertEquals(originalEntry.content(), actualEntry.content());
        assertEquals(DEFAULT_CONTENT, actualEntry.content());
    }
    
    @Test
    public void shouldRemoveDocument() throws Exception {
        SchematicEntry entry = writeSingleEntry();
        simulateTransaction(() -> db.remove(entry.id()));
        assertFalse(db.containsKey(entry.id()));
    }
    
    @Test
    public void shouldRemoveAllDocuments() throws Exception {
        int count = 3;
        simulateTransaction(() -> {
            randomEntries(count).forEach(entry -> db.put(entry.id(), entry.content()));
            return null;
        });
        assertFalse(db.keys().isEmpty());
        simulateTransaction(() -> {
            db.removeAll();
            return null;
        });
        assertTrue(db.keys().isEmpty());
    }
    
}
