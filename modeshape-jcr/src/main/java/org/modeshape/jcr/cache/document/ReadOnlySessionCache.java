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
import java.util.Set;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A read-only {@link SessionCache} implementation.
 */
@ThreadSafe
public class ReadOnlySessionCache extends AbstractSessionCache {

    private static final Logger LOGGER = Logger.getLogger(ReadOnlySessionCache.class);

    public ReadOnlySessionCache( ExecutionContext context,
                                 WorkspaceCache workspaceCache,
                                 RepositoryEnvironment repositoryEnvironment ) {
        super(context, workspaceCache, repositoryEnvironment);
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
    protected void addBinaryReference( NodeKey nodeKey, BinaryKey... binaryKeys ) {
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
