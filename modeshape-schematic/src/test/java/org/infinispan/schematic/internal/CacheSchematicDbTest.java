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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.InputStream;
import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.SchemaValidationTest;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.TestUtil;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.document.BasicDocument;
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

        cm = new DefaultCacheManager(configurationBuilder.build());
        cache = (AdvancedCache)cm.getCache("documents");
        db = new CacheSchematicDb(cache);
    }

    @After
    public void afterTest() {
        TestUtil.killCacheContainers(cm);
        cache = null;
        db = null;
        // tm = null;
    }

    protected static InputStream resource( String resourcePath ) {
        InputStream result = SchemaValidationTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull("Could not find resource \"" + resourcePath + "\"", result);
        return result;
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNullMetadata() {
        BasicDocument doc = new BasicDocument();
        doc.put("k1", "value1");
        doc.put("k2", 2);
        String key = "can be anything";
        db.put(key, doc);
        SchematicEntry entry = db.get(key);
        assertNotNull("Should have found the entry", entry);
        Document read = entry.getContent();
        assertNotNull(read);
        assertEquals("value1", read.getString("k1"));
        assertEquals(2, read.getInteger("k2").intValue());
    }
}
