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

import java.io.File;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfiguration;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfigurationBuilder;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;
import org.junit.Before;
import org.junit.Test;

public class SchematicDbWithLevelDbTest extends AbstractSchematicDbTest {

    @Override
    @Before
    public void beforeTest() {
        TestUtil.delete(new File("target/leveldb"));
        TestUtil.delete(new File("target/leveldbdocuments"));

        GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
        globalConfigurationBuilder.transport().transport(null).serialization().addAdvancedExternalizer(Schematic.externalizers());

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching().enable().transaction()
                            .transactionManagerLookup(new DummyTransactionManagerLookup()).lockingMode(LockingMode.PESSIMISTIC)
                            .locking().isolationLevel(IsolationLevel.READ_COMMITTED);
        configurationBuilder.persistence().addStore(LevelDBStoreConfigurationBuilder.class)
                            .implementationType(LevelDBStoreConfiguration.ImplementationType.JAVA)
                            .location("target/leveldb/store").expiredLocation("target/leveldb/expired").purgeOnStartup(true);

        cm = new DefaultCacheManager(globalConfigurationBuilder.build(), configurationBuilder.build(), true);
        tm = cm.getCache().getAdvancedCache().getTransactionManager();
        // Now create the SchematicDb ...
        db = Schematic.get(cm, "documents");
    }

    @Test
    public void shouldGetNonExistantDocument() {
        String key = "can be anything";
        SchematicEntry entry = db.get(key);
        assert entry == null : "Should not have found a prior entry";
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        db.put(key, doc);
        SchematicEntry entry = db.get(key);
        assert entry != null : "Should have found the entry";

        // Verify the content ...
        Document read = entry.getContent();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
        assert read.containsAll(doc);
        assert read.equals(doc);

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assert readMetadata != null;
        assert readMetadata.getString("id").equals(key);
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNonNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        db.put(key, doc);
        
        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assert entry != null : "Should have found the entry";

        // Verify the content ...
        Document read = entry.getContent();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
        assert read.containsAll(doc);
        assert read.equals(doc);

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
        assert entry != null : "Should have found the entry";

        // Verify the content ...
        Document read = entry.getContent();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
        assert read.containsAll(doc);
        assert read.equals(doc);

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
        assert read2 != null;
        assert "value1".equals(read2.getString("k1"));
        assert 2 == read2.getInteger("k2");
        assert true == read2.getBoolean("k3");
        assert 3.4d < read2.getDouble("k4");
    }
}
