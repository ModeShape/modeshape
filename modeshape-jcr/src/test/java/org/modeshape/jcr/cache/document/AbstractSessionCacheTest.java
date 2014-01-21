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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.transaction.TransactionManager;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionEnvironment;
import org.modeshape.jcr.cache.SessionEnvironment.Monitor;
import org.modeshape.jcr.cache.SessionEnvironment.MonitorFactory;
import org.modeshape.jcr.cache.change.PrintingChangeSetListener;
import org.modeshape.jcr.txn.NoClientTransactions;
import org.modeshape.jcr.txn.Transactions;

/**
 * Abstract base class for tests that operate against a SessionCache. Note that all methods must be able to operate against all
 * SessionCache implementations (e.g., {@link ReadOnlySessionCache} and {@link WritableSessionCache}).
 */
public abstract class AbstractSessionCacheTest extends AbstractNodeCacheTest {

    protected PrintingChangeSetListener listener;
    protected WorkspaceCache workspaceCache;
    protected SessionCache session1;
    protected SessionCache session2;

    @Override
    protected NodeCache createCache() {
        listener = new PrintingChangeSetListener();
        ConcurrentMap<NodeKey, CachedNode> nodeCache = new ConcurrentHashMap<NodeKey, CachedNode>();
        DocumentStore documentStore = new LocalDocumentStore(schematicDb);
        DocumentTranslator translator = new DocumentTranslator(context, documentStore, 100L);
        workspaceCache = new WorkspaceCache(context, "repo", "ws", documentStore, translator, ROOT_KEY_WS1, nodeCache, listener);
        loadJsonDocuments(resource(resourceNameForWorkspaceContentDocument()));
        SessionEnvironment sessionEnv = createSessionContext();
        session1 = createSessionCache(context, workspaceCache, sessionEnv);
        session2 = createSessionCache(context, workspaceCache, sessionEnv);
        return session1;
    }

    protected abstract SessionCache createSessionCache( ExecutionContext context,
                                                        WorkspaceCache cache,
                                                        SessionEnvironment sessionEnv );

    protected SessionEnvironment createSessionContext() {
        final TransactionManager txnMgr = txnManager();
        final MonitorFactory monitorFactory = new MonitorFactory() {
            @Override
            public Monitor createMonitor() {
                return null;
            }
        };
        return new SessionEnvironment() {
            private final Transactions transactions = new NoClientTransactions(monitorFactory, txnMgr);
            private final TransactionalWorkspaceCaches transactionalWorkspaceCacheFactory = new TransactionalWorkspaceCaches(
                                                                                                                             transactions);

            @Override
            public Transactions getTransactions() {
                return transactions;
            }

            @Override
            public TransactionalWorkspaceCaches getTransactionalWorkspaceCacheFactory() {
                return transactionalWorkspaceCacheFactory;
            }

            @Override
            public boolean indexingClustered() {
                return false;
            }

            @Override
            public String journalId() {
                return null;
            }
        };
    }

    protected SessionCache session() {
        return (SessionCache)cache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.cache.document.AbstractNodeCacheTest#print(boolean)
     */
    @Override
    protected void print( boolean onOrOff ) {
        super.print(onOrOff);
        listener.print = onOrOff;
    }

    protected NodeKey newKey() {
        return session1.createNodeKey();
    }

    protected NodeKey newKey( String identifier ) {
        return session1.createNodeKeyWithIdentifier(identifier);
    }

}
