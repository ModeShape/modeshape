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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ValueFactories;

/**
 * 
 */
public abstract class AbstractSessionCache implements SessionCache, DocumentCache {

    @Immutable
    protected static final class BasicSaveContext implements SaveContext {
        private final DateTime now;
        private final String userId;

        protected BasicSaveContext( ExecutionContext context ) {
            this.now = context.getValueFactories().getDateFactory().create();
            this.userId = context.getSecurityContext().getUserName();
        }

        @Override
        public DateTime getTime() {
            return now;
        }

        @Override
        public String getUserId() {
            return userId;
        }
    }

    private final WorkspaceCache sharedWorkspaceCache;
    private final AtomicReference<WorkspaceCache> workspaceCache = new AtomicReference<WorkspaceCache>();
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final Path rootPath;
    private final RepositoryEnvironment repositoryEnvironment;
    private final ConcurrentHashMap<Integer, Transactions.TransactionFunction> completeTxFunctionByTxId = new ConcurrentHashMap<>();
    
    private ExecutionContext context;

    protected AbstractSessionCache( ExecutionContext context,
                                    WorkspaceCache sharedWorkspaceCache,
                                    RepositoryEnvironment repositoryEnvironment ) {
        this.context = context;
        this.sharedWorkspaceCache = sharedWorkspaceCache;
        this.workspaceCache.set(sharedWorkspaceCache);
        ValueFactories factories = this.context.getValueFactories();
        this.nameFactory = factories.getNameFactory();
        this.pathFactory = factories.getPathFactory();
        this.rootPath = this.pathFactory.createRootPath();
        this.repositoryEnvironment = repositoryEnvironment;
        assert this.repositoryEnvironment != null;
        checkForTransaction();
    }

    protected abstract Logger logger();

    /**
     * Signal that this session cache should check for an existing transaction and use the appropriate workspace cache. If there
     * is a (new to this session) transaction, then this session will use a transaction-specific workspace cache (shared by other
     * sessions participating in the same transaction), and upon completion of the transaction the session will switch back to the
     * shared workspace cache.
     */
    @Override
    public void checkForTransaction() {
        try {
            Transactions transactions = repositoryEnvironment.getTransactions();
            Transaction txn = transactions.getTransactionManager().getTransaction();
            if (txn != null && txn.getStatus() == Status.STATUS_ACTIVE) {
                // There is an active transaction, so we need a transaction-specific workspace cache ...
                workspaceCache.set(repositoryEnvironment.getTransactionalWorkspaceCacheFactory()
                                                 .getTransactionalWorkspaceCache(sharedWorkspaceCache));
                // only register the function if there's an active ModeShape transaction because we need to run the
                // function *only after* ISPN has committed its transaction & updated the cache
                // if there isn't an active ModeShape transaction, one will become active later during "save"
                // otherwise, "save" is never called meaning this cache should be discarded
                Transactions.Transaction modeshapeTx = transactions.currentTransaction();
                if (modeshapeTx != null) {
                    // we can use the identity hash code as a tx id, because we essentially want a different tx function for each
                    // different transaction and as long as a tx is active, it should not be garbage collected, hence we should
                    // get different IDs for different transactions
                    final int txId = System.identityHashCode(modeshapeTx);
                    if (!completeTxFunctionByTxId.containsKey(txId)) {
                        // create and register the complete transaction function only once
                        Transactions.TransactionFunction completeFunction = new Transactions.TransactionFunction() {
                            @Override
                            public void execute() {
                                completeTransaction(txId);
                            }
                        };
                        if (completeTxFunctionByTxId.putIfAbsent(txId, completeFunction) == null) {
                            // we only want 1 completion function per tx id
                            modeshapeTx.uponCompletion(completeFunction);    
                        }
                    }
                }
            } else {
                // There is no active transaction, so just use the shared workspace cache ...
                completeTransaction(null);
            }
        } catch (SystemException e) {
            logger().error(e, JcrI18n.errorDeterminingCurrentTransactionAssumingNone, workspaceName(), e.getMessage());
        } catch (RollbackException e) {
            logger().error(e, JcrI18n.errorDeterminingCurrentTransactionAssumingNone, workspaceName(), e.getMessage());
        }
    }

    /**
     * Signal that the transaction that was active and in which this session participated has completed and that this session
     * should no longer use a transaction-specific workspace cache.
     */
    protected void completeTransaction(final Integer txId) {
        workspaceCache.set(sharedWorkspaceCache);
        if (txId != null) {
            completeTxFunctionByTxId.remove(txId);
        }
    }

    @Override
    public final SessionCache unwrap() {
        return this;
    }

    protected final String workspaceName() {
        return workspaceCache().getWorkspaceName();
    }

    @Override
    public final ExecutionContext getContext() {
        return context;
    }

    @Override
    public final WorkspaceCache workspaceCache() {
        return workspaceCache.get();
    }

    final DocumentTranslator translator() {
        return workspaceCache().translator();
    }

    final ExecutionContext context() {
        return context;
    }

    final NameFactory nameFactory() {
        return nameFactory;
    }

    final PathFactory pathFactory() {
        return pathFactory;
    }

    final Path rootPath() {
        return rootPath;
    }

    RepositoryEnvironment sessionContext() {
        return repositoryEnvironment;
    }

    @Override
    public final void addContextData( String key,
                                      String value ) {
        this.context = context.with(key, value);
    }

    @Override
    public NodeKey createNodeKey() {
        return getRootKey().withId(generateIdentifier());
    }

    @Override
    public NodeKey createNodeKeyWithIdentifier( String identifier ) {
        return getRootKey().withId(identifier);
    }

    @Override
    public NodeKey createNodeKeyWithSource( String sourceName ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        return getRootKey().withSourceKeyAndId(sourceKey, generateIdentifier());
    }

    @Override
    public NodeKey createNodeKey( String sourceName,
                                  String identifier ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        if (identifier == null) identifier = generateIdentifier();
        return getRootKey().withSourceKeyAndId(sourceKey, identifier);
    }

    protected String generateIdentifier() {
        return UUID.randomUUID().toString();
    }

    @Override
    public NodeKey getRootKey() {
        return workspaceCache().getRootKey();
    }

    @Override
    public WorkspaceCache getWorkspace() {
        return workspaceCache();
    }

    @Override
    public CachedNode getNode( NodeKey key ) {
        return workspaceCache().getNode(key);
    }

    @Override
    public CachedNode getNode( ChildReference reference ) {
        return getNode(reference.getKey());
    }

    @Override
    public Set<NodeKey> getNodeKeysAtAndBelow( NodeKey nodeKey ) {
        CachedNode node = this.getNode(nodeKey);
        if (node == null) {
            return Collections.emptySet();
        }
        Set<NodeKey> result = new HashSet<NodeKey>();
        result.add(nodeKey);

        for (ChildReference reference : node.getChildReferences(this)) {
            NodeKey childKey = reference.getKey();
            result.addAll(getNodeKeysAtAndBelow(childKey));
        }
        return result;
    }

    @Override
    public abstract SessionNode mutable( NodeKey key );

    @Override
    public Iterator<NodeKey> getAllNodeKeys() {
        return getAllNodeKeysAtAndBelow(getRootKey());
    }

    @Override
    public Iterator<NodeKey> getAllNodeKeysAtAndBelow( NodeKey startingKey ) {
        return new NodeCacheIterator(this, startingKey);
    }

    @Override
    public final void clear( CachedNode node ) {
        doClear(node);
        WorkspaceCache wscache = workspaceCache.get();
        if (wscache != sharedWorkspaceCache) {
            assert wscache instanceof TransactionalWorkspaceCache;
            wscache.clear();
        }
    }

    @Override
    public final void clear() {
        doClear();
        WorkspaceCache wscache = workspaceCache.get();
        if (wscache != sharedWorkspaceCache) {
            assert wscache instanceof TransactionalWorkspaceCache;
            wscache.clear();
        }
    }

    protected abstract void doClear( CachedNode node );

    protected abstract void doClear();

    /**
     * Register the fact that one or more binary values are being used or not used anymore by a node.
     *
     * @param nodeKey a {@link NodeKey} instance; may not be null.
     * @param binaryKeys an array of {@link BinaryKey} instances; may not be null.
     */
    protected abstract void addBinaryReference( NodeKey nodeKey, BinaryKey... binaryKeys );

}
