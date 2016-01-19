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
package org.modeshape.jcr.cache.document;

import static org.junit.Assert.assertNotNull;
import javax.transaction.TransactionManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.modeshape.jcr.RepositoryEnvironment;
import org.modeshape.jcr.api.txn.TransactionManagerLookup;
import org.modeshape.jcr.txn.DefaultTransactionManagerLookup;
import org.modeshape.jcr.txn.Transactions;

public abstract class AbstractDocumentStoreTest {

    protected RepositoryEnvironment repoEnv;
    protected LocalDocumentStore localStore;
    protected SchematicDb db;

    @Before
    public void beforeTest() throws Exception {
        // Now create the SchematicDb ...
        db = Schematic.getDb("mem");
        db.start();
        TransactionManagerLookup txLookup = new DefaultTransactionManagerLookup();
        TransactionManager tm = txLookup.getTransactionManager();
        assertNotNull("Cannot find a transaction manager", tm);        
        repoEnv = new TestRepositoryEnvironment(tm);
        localStore = new LocalDocumentStore(db, repoEnv);
    }

    @After
    public void afterTest() {
        try {
            db.stop();
        } finally {
            try {
                TestUtil.killTransaction(transactions().getTransactionManager());
            } finally {
                repoEnv = null;
            }
        }
    }

    protected Transactions transactions() {
        return repoEnv.getTransactions();
    }
}
