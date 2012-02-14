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

import javax.transaction.TransactionManager;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionEnvironment;
import org.modeshape.jcr.cache.change.PrintingChangeSetListener;

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
        workspaceCache = new WorkspaceCache(context, "repo", "ws", database(), 100L, ROOT_KEY_WS1, listener);
        loadJsonDocuments(resource(resourceNameForWorkspaceContentDocument()));
        session1 = createSession(context, workspaceCache);
        session2 = createSession(context, workspaceCache);
        return session1;
    }

    protected abstract SessionCache createSession( ExecutionContext context,
                                                   WorkspaceCache cache );

    protected SessionEnvironment createSessionContext() {
        final TransactionManager txnMgr = txnManager();
        return new SessionEnvironment() {
            @Override
            public TransactionManager getTransactionManager() {
                return txnMgr;
            }

            @Override
            public Monitor createMonitor() {
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
