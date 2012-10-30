/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.cache.document.DocumentStore;

/**
 * An abstract base class for unit tests that require an testable SchematicDb instance.
 */
public abstract class AbstractSchematicDbTest {

    private DocumentStore documentStore;
    private EmbeddedCacheManager cm;
    private TransactionManager tm;
    private Logger logger;

    @Before
    public void beforeEach() {
        logger = Logger.getLogger(getClass());
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching()
                            .enable()
                            .transaction()
                            .transactionManagerLookup(new DummyTransactionManagerLookup());

        cm = TestCacheManagerFactory.createCacheManager(configurationBuilder);
        // Now create the SchematicDb ...
        documentStore = new DocumentStore(Schematic.get(cm, "documents"));
        tm = TestingUtil.getTransactionManager(documentStore.localCache());
    }

    @After
    public void afterEach() {
        try {
            TestingUtil.killCacheManagers(cm);
        } finally {
            documentStore = null;
            tm = null;
        }
    }

    protected TransactionManager txnManager() {
        return tm;
    }

    protected DocumentStore documentStore() {
        return documentStore;
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
                        documentStore.put(key, content, metadata);
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
