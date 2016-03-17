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
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryEnvironment;
import org.modeshape.jcr.locking.LockingService;
import org.modeshape.jcr.locking.StandaloneLockingService;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.schematic.SchematicDb;

/**
 * {@link org.modeshape.jcr.RepositoryEnvironment} implementation used by tests.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class TestRepositoryEnvironment implements RepositoryEnvironment {

    private final Transactions transactions;
    private final LockingService lockingService;
    
    public TestRepositoryEnvironment(TransactionManager txMgr, SchematicDb db) {
        CheckArg.isNotNull(txMgr, "txMgr");
        this.transactions = new Transactions(txMgr, db);
        this.lockingService = new StandaloneLockingService(RepositoryConfiguration.Default.LOCK_TIMEOUT);
    }
    
    @Override
    public Transactions getTransactions() {
        return transactions;
    }

    @Override
    public String journalId() {
        return null;
    }

    @Override
    public NodeTypes nodeTypes() {
        return null;
    }

    @Override
    public LockingService lockingService() {
        return lockingService;
    }
}
