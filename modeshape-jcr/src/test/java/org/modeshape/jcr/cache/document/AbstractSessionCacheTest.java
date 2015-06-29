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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.transaction.TransactionManager;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.bus.RepositoryChangeBus;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.change.PrintingChangeSetListener;
import org.modeshape.jcr.txn.NoClientTransactions;
import org.modeshape.jcr.txn.Transactions;

/**
 * Abstract base class for tests that operate against a SessionCache. Note that all methods must be able to operate against all
 * SessionCache implementations (e.g., {@link ReadOnlySessionCache} and {@link WritableSessionCache}).
 */
public abstract class AbstractSessionCacheTest extends AbstractNodeCacheTest {

    private ExecutorService executor;
    private RepositoryChangeBus changeBus;
    private PrintingChangeSetListener listener;
    protected WorkspaceCache workspaceCache;
    protected SessionCache session1;
    protected SessionCache session2;

    @Override
    protected NodeCache createCache() {
        executor = Executors.newCachedThreadPool();
        changeBus = new RepositoryChangeBus("repo", executor);
        listener = new PrintingChangeSetListener();
        changeBus.register(listener);
        ConcurrentMap<NodeKey, CachedNode> nodeCache = new ConcurrentHashMap<NodeKey, CachedNode>();
        DocumentStore documentStore = new LocalDocumentStore(schematicDb);
        DocumentTranslator translator = new DocumentTranslator(context, documentStore, 100L);
        workspaceCache = new WorkspaceCache(context, "repo", "ws", null, documentStore, translator, ROOT_KEY_WS1, nodeCache,
                                            changeBus, null);
        loadJsonDocuments(resource(resourceNameForWorkspaceContentDocument()));
        RepositoryEnvironment sessionEnv = createRepositoryEnvironment();
        session1 = createSessionCache(context, workspaceCache, sessionEnv);
        session2 = createSessionCache(context, workspaceCache, sessionEnv);
        return session1;
    }

    @Override
    protected void shutdownCache( NodeCache cache ) {
        super.shutdownCache(cache);
        executor.shutdown();
    }

    protected abstract SessionCache createSessionCache( ExecutionContext context,
                                                        WorkspaceCache cache,
                                                        RepositoryEnvironment sessionEnv );

    protected RepositoryEnvironment createRepositoryEnvironment() {
        final TransactionManager txnMgr = txnManager();
        return new RepositoryEnvironment() {
            private final Transactions transactions = new NoClientTransactions(txnMgr);
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
            public String journalId() {
                return null;
            }

            @Override
            public NodeTypes nodeTypes() {
                return null;
            }
        };
    }

    protected SessionCache session() {
        return (SessionCache)cache;
    }

    /**
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
