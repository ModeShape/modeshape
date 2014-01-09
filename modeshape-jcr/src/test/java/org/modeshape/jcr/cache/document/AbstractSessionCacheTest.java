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
