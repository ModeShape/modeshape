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

import javax.transaction.TransactionManager;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.TestUtil;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;
import org.junit.After;
import org.junit.Before;
import org.modeshape.jcr.RepositoryEnvironment;
import org.modeshape.jcr.txn.Transactions;

public abstract class AbstractDocumentStoreTest {

    protected EmbeddedCacheManager cm;
    protected RepositoryEnvironment repoEnv;
    protected LocalDocumentStore localStore;

    @Before
    public void beforeTest() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.invocationBatching()
                            .enable()
                            .transaction()
                            .transactionManagerLookup(new DummyTransactionManagerLookup())
                            .transactionMode(TransactionMode.TRANSACTIONAL)
                            .lockingMode(LockingMode.PESSIMISTIC)
                            .locking()
                            .isolationLevel(IsolationLevel.READ_COMMITTED);
        GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
        globalConfigurationBuilder.globalJmxStatistics().allowDuplicateDomains(true);
        cm = new DefaultCacheManager(globalConfigurationBuilder.build(), configurationBuilder.build());
        // Now create the SchematicDb ...
        SchematicDb db = Schematic.get(cm, "documents");
        TransactionManager tm = db.getCache().getAdvancedCache().getTransactionManager();
        repoEnv = new TestRepositoryEnvironment(tm);
        localStore = new LocalDocumentStore(db, repoEnv);
    }

    @After
    public void afterTest() {
        try {
            TestUtil.killCacheContainers(cm);
        } finally {
            cm = null;
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
