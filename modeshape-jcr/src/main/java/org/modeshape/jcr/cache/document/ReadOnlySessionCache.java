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

import java.util.Collections;
import java.util.Set;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionEnvironment;

/**
 * A read-only {@link SessionCache} implementation.
 */
@ThreadSafe
public class ReadOnlySessionCache extends AbstractSessionCache {

    private static final Logger LOGGER = Logger.getLogger(ReadOnlySessionCache.class);

    public ReadOnlySessionCache( ExecutionContext context,
                                 WorkspaceCache workspaceCache,
                                 SessionEnvironment sessionContext ) {
        super(context, workspaceCache, sessionContext);
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public Set<NodeKey> getChangedNodeKeys() {
        return Collections.emptySet();
    }

    @Override
    public Set<NodeKey> getChangedNodeKeysAtOrBelow( CachedNode node ) {
        return Collections.emptySet();
    }

    @Override
    protected void doClear() {
        // do nothing, as we don't want to clear the shared workspace
    }

    @Override
    protected void doClear( CachedNode node ) {
        // do nothing
    }

    @Override
    public void save() {
        // do nothing
    }

    @Override
    public void save( SessionCache otherSession,
                      PreSave preSaveOperation ) {
        // do nothing
    }

    @Override
    public void save( Set<NodeKey> toBeSaved,
                      SessionCache otherSession,
                      PreSave preSaveOperation ) {
        // do nothing
    }

    @Override
    public SessionNode mutable( NodeKey key ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void destroy( NodeKey key ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDestroyed( NodeKey key ) {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Session ").append(context().getId()).append(" (readonly) to workspace '").append(workspaceName());
        return sb.toString();
    }

}
