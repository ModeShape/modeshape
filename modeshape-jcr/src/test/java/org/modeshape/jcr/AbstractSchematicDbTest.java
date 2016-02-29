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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.api.txn.TransactionManagerLookup;
import org.modeshape.jcr.cache.document.TestRepositoryEnvironment;
import org.modeshape.jcr.txn.DefaultTransactionManagerLookup;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;

/**
 * An abstract base class for unit tests that require an testable SchematicDb instance.
 */
public abstract class AbstractSchematicDbTest {

    protected SchematicDb schematicDb;
    protected RepositoryEnvironment repoEnv;

    private Logger logger;

    @Before
    public void beforeEach() {
        logger = Logger.getLogger(getClass());
        // default to an in-memory h2
        schematicDb = Schematic.getDb(new TestingEnvironment().defaultPersistenceConfiguration());
        schematicDb.start();
        TransactionManagerLookup txManagerLookup = new DefaultTransactionManagerLookup();
        TransactionManager txManager = txManagerLookup.getTransactionManager();
        assertNotNull("Was not able to locate a transaction manager in the test classpath", txManager);
        repoEnv = new TestRepositoryEnvironment(txManager, schematicDb);
    }

    @After
    public void afterEach() {
        try {
            schematicDb.stop();
        } finally {
            schematicDb = null;
            repoEnv = null;
        }
    }

    protected Transactions transactions() {
        return repoEnv.getTransactions();
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
        runInTransaction(() -> {
            Document document = Json.read(stream);
            List<?> data = document.getArray("data");
            if (data != null) {
                for (Object value : data) {
                    if (value instanceof Document) {
                        Document dataDoc = (Document) value;
                        // Get the key ...
                        Document content = dataDoc.getDocument("content");
                        Document metadata = dataDoc.getDocument("metadata");
                        String key = metadata.getString("id");
                        schematicDb.put(key, content);
                    }
                }
            }
            return null;
        });
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
    
    protected <V> V runInTransaction(Callable<V> operation) {
        Transactions transactions = transactions();
        try {
            try {
                transactions.begin();
                V result = operation.call();
                transactions.commit();
                return result;
            } catch (RuntimeException re) {
                transactions.rollback();
                throw re;
            } catch (Throwable t) {
                transactions.rollback();
                throw new RuntimeException(t);
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
