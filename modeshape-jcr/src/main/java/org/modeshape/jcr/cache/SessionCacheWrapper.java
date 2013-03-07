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
package org.modeshape.jcr.cache;

import java.util.Iterator;
import java.util.Set;
import org.modeshape.jcr.ExecutionContext;

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
    public NodeCache getWorkspace() {
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
