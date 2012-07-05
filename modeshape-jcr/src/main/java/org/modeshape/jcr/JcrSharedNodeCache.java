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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Path;

/**
 * A class that manages for a single {@link JcrSession} the various sets of shared nodes for each of the shareable nodes. Some
 * terminology:
 * <ul>
 * <li><i>shareable node</i> - A node that has the <code>mix:shareable</code> mixin, and which is capable of being shared.</li>
 * <li><i>shared node</i> - The result of sharing a shareable node. Each shared node has its own name and location.</li>
 * </ul>
 * <p>
 * The set of Node instances that represent the shared nodes for a given shareable node could be stored in the AbstractJcrNode
 * instance that is the shareable node. However, since the "mix:shareable" can be added or removed at any time, we can't create a
 * subclass of AbstractJcrNode to store this information, and we also don't want to store a null reference in <i>every</i>
 * AbstractJcrNode instance.
 * </p>
 * <p>
 * Therefore, this class is created lazily within the {@link JcrSession} and is used to maintain the Node instances for each of
 * the shared nodes for each shareable node. This cache is required so that clients always get the same Node instance.
 * </p>
 */
final class JcrSharedNodeCache {

    private final ConcurrentMap<NodeKey, SharedSet> sharedSets = new ConcurrentHashMap<NodeKey, SharedSet>();

    protected final JcrSession session;

    JcrSharedNodeCache( JcrSession session ) {
        this.session = session;
    }

    public JcrSession session() {
        return session;
    }

    /**
     * Get the {@link SharedSet} for the given shareable node.
     * 
     * @param shareableNode the shareable node
     * @return the SharedSet; never null
     */
    public SharedSet getSharedSet( AbstractJcrNode shareableNode ) {
        NodeKey shareableNodeKey = shareableNode.key();
        SharedSet sharedSet = sharedSets.get(shareableNodeKey);
        if (sharedSet == null) {
            SharedSet newSharedSet = new SharedSet(shareableNode);
            sharedSet = sharedSets.putIfAbsent(shareableNodeKey, newSharedSet);
            if (sharedSet == null) sharedSet = newSharedSet;
        }
        return sharedSet;
    }

    /**
     * The set of shared nodes for a single shareable node.
     */
    final class SharedSet {
        /** The sharable node */
        protected final AbstractJcrNode shareableNode;

        /** The cache of Node instances that represent the shared nodes for a single shareable node. */
        private final ConcurrentMap<NodeKey, JcrSharedNode> sharedNodesByParentKey = new ConcurrentHashMap<NodeKey, JcrSharedNode>();

        protected SharedSet( AbstractJcrNode shareableNode ) {
            this.shareableNode = shareableNode;
            assert this.shareableNode != null;
        }

        /**
         * Get the NodeKey for the shareable node.
         * 
         * @return the key; never null and always the same
         */
        public final NodeKey key() {
            return shareableNode.key();
        }

        /**
         * Get the {@link Session} to which this set belongs.
         * 
         * @return the session instance; never null and always the same
         */
        public final JcrSession session() {
            return JcrSharedNodeCache.this.session();
        }

        /**
         * Get the {@link JcrSharedNode shared Node instance} that is a child of the specified parent.
         * 
         * @param cachedNode the cached node; may not be null
         * @param parentKey the key of the parent node; may not b enull
         * @return the shared node instance; never null
         */
        public AbstractJcrNode getSharedNode( CachedNode cachedNode,
                                              NodeKey parentKey ) {
            assert parentKey != null;
            if (!key().equals(parentKey)) {

                // Obtain the set of keys for all parents ...
                final SessionCache cache = session.cache();
                Set<NodeKey> additionalParents = cachedNode.getAdditionalParentKeys(cache);

                // And get the shared node for the parent key, first making sure the parent exists ...
                if (additionalParents.contains(parentKey) && session.nodeExists(parentKey)) {
                    return getOrCreateSharedNode(parentKey);
                }
            }
            return shareableNode;
        }

        /**
         * Determine and return the first of any nodes in the shared set exist at a location that is at or below the supplied
         * path.
         * 
         * @param path the path; may not be null
         * @return the node in the shared set that exists at or below the supplied path; or null if none of the nodes in the
         *         shared set are at or below the supplied path
         * @throws ItemNotFoundException
         * @throws InvalidItemStateException
         * @throws RepositoryException
         */
        public AbstractJcrNode getSharedNodeAtOrBelow( Path path )
            throws RepositoryException, ItemNotFoundException, InvalidItemStateException {
            NodeIterator iter = getSharedNodes();
            while (iter.hasNext()) {
                AbstractJcrNode shared = (AbstractJcrNode)iter.nextNode();
                if (shared.path().isAtOrBelow(path)) return shared;
            }
            return null;
        }

        protected void remove( JcrSharedNode sharedNode ) {
            sharedNodesByParentKey.remove(sharedNode.parentKey());
        }

        private JcrSharedNode getOrCreateSharedNode( NodeKey parentKey ) {
            assert parentKey != null;
            JcrSharedNode sharedNode = sharedNodesByParentKey.get(parentKey);
            if (sharedNode == null) {
                JcrSharedNode newShared = new JcrSharedNode(this, parentKey);
                sharedNode = sharedNodesByParentKey.putIfAbsent(parentKey, newShared);
                if (sharedNode == null) sharedNode = newShared;
            }
            return sharedNode;
        }

        /**
         * Get the number of shared nodes within this shared set.
         * 
         * @return the number of shared nodes; always 1 or more
         * @throws RepositoryException if there's a problem getting the size
         */
        public int getSize() throws RepositoryException {
            final SessionCache cache = session().cache();
            Set<NodeKey> additionalParents = shareableNode.node().getAdditionalParentKeys(cache);
            return additionalParents.size() + 1;
        }

        /**
         * Find all of the {@link javax.jcr.Node}s that make up the shared set.
         * 
         * @return the iterator over the nodes in the node set; never null, but possibly empty if this is not shareable, or of
         *         size 1 if the node is shareable but hasn't been shared
         * @throws RepositoryException if there is a problem finding the nodes in the set
         */
        public NodeIterator getSharedNodes() throws RepositoryException {
            // Obtain the set of keys for all parents ...
            final SessionCache cache = session().cache();
            Set<NodeKey> additionalParents = shareableNode.node().getAdditionalParentKeys(cache);

            // Obtain the Node objects for each of these ...
            final NodeKey key = shareableNode.key();
            final String workspaceKey = key.getWorkspaceKey();
            Collection<AbstractJcrNode> sharedNodes = new ArrayList<AbstractJcrNode>(additionalParents.size() + 1);
            sharedNodes.add(shareableNode); // add the shareable node
            for (NodeKey parentKey : additionalParents) {
                if (!workspaceKey.equals(parentKey.getWorkspaceKey())) {
                    // The parent node has to be in this workspace ...
                    continue;
                }
                if (session.nodeExists(parentKey)) {
                    sharedNodes.add(getOrCreateSharedNode(parentKey));
                }
            }

            // Return an iterator ...
            return new JcrNodeListIterator(sharedNodes.iterator(), sharedNodes.size());
        }
    }
}
