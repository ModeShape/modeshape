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
package org.modeshape.jcr.cache;

import java.util.Iterator;
import java.util.Set;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.document.WorkspaceCache;

/**
 * A {@link SessionCache} implementation that wraps another and is suitable to extend and overwrite only those methods that are
 * required.
 */
public class SessionCacheWrapper implements SessionCache {

    private final SessionCache delegate;

    public SessionCacheWrapper( SessionCache delegate ) {
        this.delegate = delegate;
    }

    @Override
    public SessionCache unwrap() {
        return delegate.unwrap();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public NodeKey getRootKey() {
        return delegate.getRootKey();
    }

    @Override
    public CachedNode getNode( NodeKey key ) {
        return delegate.getNode(key);
    }

    @Override
    public CachedNode getNode( ChildReference reference ) {
        return delegate.getNode(reference);
    }

    @Override
    public Iterator<NodeKey> getAllNodeKeys() {
        return delegate.getAllNodeKeys();
    }

    @Override
    public Iterator<NodeKey> getAllNodeKeysAtAndBelow( NodeKey startingKey ) {
        return delegate.getAllNodeKeysAtAndBelow(startingKey);
    }

    @Override
    public ExecutionContext getContext() {
        return delegate.getContext();
    }

    @Override
    public void addContextData( String key,
                                String value ) {
        delegate.addContextData(key, value);
    }

    @Override
    public void save() {
        delegate.save();
    }

    @Override
    public void save( Set<NodeKey> toBeSaved,
                      SessionCache otherSession,
                      PreSave preSaveOperation ) {
        delegate.save(toBeSaved, otherSession, preSaveOperation);
    }

    @Override
    public void save( SessionCache otherSession,
                      PreSave preSaveOperation ) {
        delegate.save(otherSession, preSaveOperation);
    }

    @Override
    public boolean hasChanges() {
        return delegate.hasChanges();
    }

    @Override
    public Set<NodeKey> getChangedNodeKeys() {
        return delegate.getChangedNodeKeys();
    }

    @Override
    public Set<NodeKey> getChangedNodeKeysAtOrBelow( CachedNode node ) throws NodeNotFoundException {
        return delegate.getChangedNodeKeysAtOrBelow(node);
    }

    @Override
    public Set<NodeKey> getNodeKeysAtAndBelow( NodeKey nodeKey ) {
        return delegate.getNodeKeysAtAndBelow(nodeKey);
    }

    @Override
    public void clear( CachedNode node ) {
        delegate.clear(node);
    }

    @Override
    public WorkspaceCache getWorkspace() {
        return delegate.getWorkspace();
    }

    @Override
    public MutableCachedNode mutable( NodeKey key ) throws NodeNotFoundException, UnsupportedOperationException {
        return delegate.mutable(key);
    }

    @Override
    public void destroy( NodeKey key ) throws NodeNotFoundException, UnsupportedOperationException {
        delegate.destroy(key);
    }

    @Override
    public boolean isDestroyed( NodeKey key ) {
        return delegate.isDestroyed(key);
    }

    @Override
    public NodeKey createNodeKey() {
        return delegate.createNodeKey();
    }

    @Override
    public NodeKey createNodeKeyWithIdentifier( String identifier ) {
        return delegate.createNodeKeyWithIdentifier(identifier);
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public NodeKey createNodeKeyWithSource( String sourceName ) {
        return delegate.createNodeKeyWithSource(sourceName);
    }

    @Override
    public NodeKey createNodeKey( String sourceName,
                                  String identifier ) {
        return delegate.createNodeKey(sourceName, identifier);
    }

    @Override
    public void checkForTransaction() {
        delegate.checkForTransaction();
    }

}
