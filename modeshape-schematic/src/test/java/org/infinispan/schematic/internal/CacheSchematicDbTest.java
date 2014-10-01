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
package org.infinispan.schematic.internal;

import java.io.InputStream;
import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.SchemaValidationTest;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CacheSchematicDbTest {

    private CacheSchematicDb db;
    private AdvancedCache<String, SchematicEntry> cache;
    private EmbeddedCacheManager cm;

    @SuppressWarnings( {"rawtypes", "unchecked"} )
    @Before
    public void beforeTest() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching().enable().transaction()
                            .transactionManagerLookup(new DummyTransactionManagerLookup());

        cm = TestCacheManagerFactory.createCacheManager(configurationBuilder);
        cache = (AdvancedCache)cm.getCache("documents");
        db = new CacheSchematicDb(cache);
    }

    @After
    public void afterTest() {
        TestingUtil.killCacheManagers(cm);
        cache = null;
        db = null;
        // tm = null;
    }

    protected static InputStream resource( String resourcePath ) {
        InputStream result = SchemaValidationTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assert result != null : "Could not find resource \"" + resourcePath + "\"";
        return result;
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNullMetadata() {
        BasicDocument doc = new BasicDocument();
        doc.put("k1", "value1");
        doc.put("k2", 2);
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc);
        assert prior == null : "Should not have found a prior entry";
        SchematicEntry entry = db.get(key);
        assert entry != null : "Should have found the entry";
        Document read = entry.getContent();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
    }
}
