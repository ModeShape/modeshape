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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.transaction.TransactionManager;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.TestUtil;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.logging.Logger;

/**
 * An abstract base class for unit tests that require an testable SchematicDb instance.
 */
public abstract class AbstractSchematicDbTest {

    protected SchematicDb schematicDb;

    private EmbeddedCacheManager cm;
    private TransactionManager tm;
    private Logger logger;

    @Before
    public void beforeEach() {
        logger = Logger.getLogger(getClass());
        GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
        globalConfigurationBuilder.globalJmxStatistics().disable().allowDuplicateDomains(true);
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching().enable().transaction()
                            .transactionManagerLookup(new DummyTransactionManagerLookup()).lockingMode(LockingMode.PESSIMISTIC);

        cm = new DefaultCacheManager(globalConfigurationBuilder.build(), configurationBuilder.build(), true);
        // Now create the SchematicDb ...
        schematicDb = Schematic.get(cm, "documents");
        tm = schematicDb.getCache().getAdvancedCache().getTransactionManager();
    }

    @After
    public void afterEach() {
        try {
            TestUtil.killCacheContainers(cm);
        } finally {
            schematicDb = null;
            tm = null;
        }
    }

    protected TransactionManager txnManager() {
        return tm;
    }

    protected EmbeddedCacheManager cacheManager() {
        return cm;
    }

    protected Logger logger() {
        return logger;
    }

    /**
     * Reads the input stream to load the JSON data as a Document, and then loads all of the documents within the "data" array
     * field into the database.
     * 
     * @param stream the stream containing the JSON data Document
     */
    protected void loadJsonDocuments( InputStream stream ) {
        try {
            Document document = Json.read(stream);
            List<?> data = document.getArray("data");
            if (data != null) {
                for (Object value : data) {
                    if (value instanceof Document) {
                        Document dataDoc = (Document)value;
                        // Get the key ...
                        Document content = dataDoc.getDocument("content");
                        Document metadata = dataDoc.getDocument("metadata");
                        String key = metadata.getString("id");
                        schematicDb.put(key, content);
                    }
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    protected InputStream resource( String resourcePath ) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            File file = new File(resourcePath);
            if (!file.exists()) {
                file = new File("src/test/resources" + resourcePath);
            }
            if (!file.exists()) {
                file = new File("src/test/resources/" + resourcePath);
            }
            if (file.exists()) {
                try {
                    stream = new FileInputStream(file);
                } catch (IOException e) {
                    fail("Failed to open stream to \"" + file.getAbsolutePath() + "\"");
                }
            }
        }
        assertThat(stream, is(notNullValue()));
        return stream;
    }
}
