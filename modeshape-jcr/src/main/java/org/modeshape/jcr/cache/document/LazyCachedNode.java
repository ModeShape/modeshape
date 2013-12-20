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

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.NodeNotFoundInParentException;
import org.modeshape.jcr.cache.PathCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

/**
 * This is a (mostly) immutable {@link CachedNode} implementation that lazily loads its content. Technically each instance
 * modifies its internal state, but most of the state is based upon a single Document that is read-in only once and never changed
 * again. And thus externally each instance appears to be immutable and invariant, except anything that has to do with this node's
 * {@link #getName(NodeCache)} (e.g., {@link #getSegment(NodeCache) segment}, and the {@link #getPath(NodeCache) path} {
 * {@link #getPath(PathCache) methods}). That's because the name of this node is actually stored on the <em>parent</em> in the
 * parent's {@link #getChildReferences(NodeCache) child references}, and this node's name, SNS index, and thus the path can all
 * change even though none of the information stored in this node's document will actually change.
 * <p>
 * This class is marked {@link Serializable} so instances can be placed within an Infinispan cache, though this class is never
 * intended to actually be persisted. Instead, it is kept within ModeShape's {@link WorkspaceCache} that uses a purely in-memory
 * Infinispan cache containing a configurable number of the most-recently-used {@link CachedNode} instances. As soon as
 * {@link CachedNode} instances are evicted, they are GCed and no longer used. (Note that they can be easily reconstructed from
 * the entries stored in the repository's main cache.
 * </p>
 * <p>
 * The {@link WorkspaceCache} that keeps these {@link LazyCachedNode} instances is intended to be a cache of the persisted nodes,
 * and thus are accessed by all sessions for that workspace. When a persisted node is changed, the corresponding
 * {@link CachedNode} is purged from the {@link WorkspaceCache}. Therefore, these LazyCachedNode's must be able to be accessed
 * concurrently from multiple threads; this is the reason why all of the lazily-populated fields are either atomic references or
 * volatile. (Since all of these lazily-populated members are idempotent, the implementations do this properly without
 * synchronization or blocking. The exception is the {@link #parentRefToSelf} field, which can change but is done so in an atomic
 * fashion (see {@link #parentReferenceToSelf(WorkspaceCache)} for details).
 * </p>
 */
@ThreadSafe
public class LazyCachedNode implements CachedNode, Serializable {

    private static final long serialVersionUID = 1L;

    // There are two 'final' fields that are always set during construction. The 'document' is the snapshot of node's state
    // (except for the node's name or SNS index, which are stored in the parent's document).
    private final NodeKey key;
    private final Document document;

    // The remaining attributes are all lazily loaded/constructed from the 'document' via the DocumentTranslator methods.\
    // The WorkspaceCache in which these LazyCachedNodes are kept are accessible
    private transient final AtomicReference<Map<Name, Property>> properties = new AtomicReference<Map<Name, Property>>();
    private transient final AtomicReference<ParentReferenceToSelf> parentRefToSelf = new AtomicReference<ParentReferenceToSelf>();
    private transient volatile NodeKey parent;
    private transient volatile Set<NodeKey> additionalParents;
    private transient volatile boolean propertiesFullyLoaded = false;
    private transient volatile ChildReferences childReferences;

    public LazyCachedNode( NodeKey key,
                           Document document ) {
        assert document != null;
        assert key != null;
        this.key = key;
        this.document = document;
    }

    protected final WorkspaceCache workspaceCache( NodeCache cache ) {
        return ((DocumentCache)cache.unwrap()).workspaceCache();
    }

    /**
     * Get the {@link Document} that represents this node.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the document; never null
     * @throws NodeNotFoundException if this node no longer exists
     */
    protected Document document( WorkspaceCache cache ) {
        return document;
    }

    @Override
    public NodeKey getParentKey( NodeCache cache ) {
        if (parent == null) {
            // This is idempotent, so it's okay if another thread sneaks in here and recalculates the object before we do ...
            WorkspaceCache wsCache = workspaceCache(cache);
            parent = wsCache.translator().getParentKey(document(wsCache), wsCache.getWorkspaceKey(), key.getWorkspaceKey());
        }
        return parent;
    }

    @Override
    public NodeKey getParentKeyInAnyWorkspace( NodeCache cache ) {
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().getParentKey(document(wsCache), key.getWorkspaceKey(), key.getWorkspaceKey());
    }

    @Override
    public Set<NodeKey> getAdditionalParentKeys( NodeCache cache ) {
        if (additionalParents == null) {
            WorkspaceCache wsCache = workspaceCache(cache);
            Set<NodeKey> additionalParents = wsCache.translator().getAdditionalParentKeys(document(wsCache));
            this.additionalParents = additionalParents.isEmpty() ? additionalParents : Collections.unmodifiableSet(additionalParents);
        }
        return additionalParents;
    }

    @Override
    public boolean isAtOrBelow( NodeCache cache,
                                Path path ) {
        Path aPath = getPath(cache);
        if (path.isAtOrAbove(aPath)) return true;
        Set<NodeKey> additionalParents = getAdditionalParentKeys(cache);
        if (!additionalParents.isEmpty()) {
            Path parentOfPath = path.getParent();
            for (NodeKey parentKey : additionalParents) {
                CachedNode parent = cache.getNode(parentKey);
                if (parent.getPath(cache).isAtOrBelow(parentOfPath)) {
                    ChildReference ref = parent.getChildReferences(cache).getChild(key);
                    if (ref != null && ref.getSegment().equals(path.getLastSegment())) return true;
                }
            }
        }
        return false;
    }

    protected CachedNode parent( WorkspaceCache cache ) {
        NodeKey parentKey = getParentKey(cache);
        if (parentKey == null) {
            return null;
        }
        CachedNode parent = cache.getNode(parentKey);
        if (parent == null) {
            throw new NodeNotFoundException(parentKey);
        }
        return parent;
    }

    /**
     * Get the parent node's child reference to this node. This method atomically determines if the {@link #parentRefToSelf}
     * object is still up-to-date with the parent node's cached representation. If it is not still valid, then it will be
     * recomputed.
     * <p>
     * This method is carefully written to always return an atomically-consistent result without requiring any locking or
     * synchronization. Generally speaking, at any given moment the cached information is idempotent, so we rely upon that and use
     * an AtomicReference to ensure that the reference is updated atomically. The method implementation also reads the
     * AtomicReference only once before deciding what to do; this means that multiple threads can concurrently call this method
     * and it will always return the correct information.
     * </p>
     * 
     * @param cache the cache
     * @return the child reference; never null (even for the root node)
     * @throws NodeNotFoundInParentException if this node is no longer referenced by its parent as a child of the parent node
     *         (which can happen if this node is used while in the midst of being (re)moved.
     */
    protected ChildReference parentReferenceToSelf( WorkspaceCache cache ) {
        CachedNode currentParent = null;

        // If we currently have cached our parent's reference to us (and it is still complete) ...
        ParentReferenceToSelf prts = parentRefToSelf.get();
        if (prts != null && prts.isComplete()) {
            if (prts.isRoot()) {
                // We are the root node (always), so we can immediately return ...
                return prts.childReferenceInParent();
            }
            // Get the current parent to compare with what we've cached ...
            currentParent = parent(cache);
            if (currentParent == null && prts.isRoot()) {
                // We are the root and this never changes. Therefore, our 'parentRefToSelf' is always valid ...
                return prts.childReferenceInParent();
            }
            if (prts.isValid(currentParent)) {
                // Our cached form still looks okay ...
                return prts.childReferenceInParent();
            }
            // Our cached form is no longer valid so we have to build a new one ...
        }

        // We have to (re)find our parent's reference to us ...
        if (currentParent == null) currentParent = parent(cache);
        if (currentParent == null) {
            // This is the root node ...
            parentRefToSelf.compareAndSet(null, new RootParentReferenceToSelf(cache));
            return parentRefToSelf.get().childReferenceInParent(); // always get the most recent
        }

        // The rest of this logic is only for non-root nodes ...
        assert currentParent != null;

        // Get our parent's child references to find which one points to us ...
        ChildReferences currentReferences = currentParent.getChildReferences(cache);
        ChildReference parentRefToMe = null;
        if (currentReferences.supportsGetChildReferenceByKey()) {
            // Just using the node key is faster if it is supported by the implementation ...
            parentRefToMe = currentReferences.getChild(key);
        } else {
            // Directly look up the ChildReference by going to the cache (and possibly connector) ...
            NodeKey parentKey = getParentKey(cache);
            parentRefToMe = cache.getChildReference(parentKey, key);
        }
        if (parentRefToMe != null) {
            // We found a new ChildReference instance from the current parent, so cache it ...
            parentRefToSelf.set(new NonRootParentReferenceToSelf(currentParent, parentRefToMe));
            return parentRefToSelf.get().childReferenceInParent(); // always get the most recent
        }
        assert parentRefToMe == null;
        // This node references a parent, but that parent no longer has a child reference to this node. Perhaps this node is
        // in the midst of being moved or removed. Either way, we don't have much choice but to throw an exception about
        // us not being found...
        throw new NodeNotFoundInParentException(key, getParentKey(cache));
    }

    protected Map<Name, Property> properties() {
        if (properties.get() == null) {
            // Try to create the properties map, but some other thread might have snuck in and done it for us ...
            properties.compareAndSet(null, new ConcurrentHashMap<Name, Property>());
        }
        return properties.get();
    }

    @Override
    public NodeKey getKey() {
        return key;
    }

    @Override
    public Name getName( NodeCache cache ) {
        return parentReferenceToSelf(workspaceCache(cache)).getName();
    }

    @Override
    public Segment getSegment( NodeCache cache ) {
        return parentReferenceToSelf(workspaceCache(cache)).getSegment();
    }

    /**
     * Get the name for this node, without any same-name-sibiling (SNS) index.
     * 
     * @param cache the workspace cache to which this node belongs, required in case this node needs to use the cache; may not be
     *        null
     * @return the name; never null, but the root node will have a zero-length name
     * @throws NodeNotFoundInParentException if this node no longer exists
     * @see #getSegment(NodeCache)
     * @see #getPath(NodeCache)
     */
    protected Segment getSegment( WorkspaceCache cache ) {
        return parentReferenceToSelf(cache).getSegment();
    }

    @Override
    public Path getPath( NodeCache cache ) {
        WorkspaceCache wsCache = workspaceCache(cache);
        CachedNode parent = parent(wsCache);
        if (parent != null) {
            Path parentPath = parent.getPath(wsCache);
            return wsCache.pathFactory().create(parentPath, getSegment(wsCache));
        }
        // check that the node hasn't been removed in the meantime
        if (wsCache.getNode(key) == null) {
            throw new NodeNotFoundException(key);
        }
        // This is the root node ...
        return wsCache.rootPath();
    }

    @Override
    public Path getPath( PathCache pathCache ) throws NodeNotFoundException {
        NodeCache cache = pathCache.getCache();
        WorkspaceCache wsCache = workspaceCache(cache);
        CachedNode parent = parent(wsCache);
        if (parent != null) {
            Path parentPath = pathCache.getPath(parent);
            return wsCache.pathFactory().create(parentPath, getSegment(wsCache));
        }
        // check that the node hasn't been removed in the meantime
        if (wsCache.getNode(key) == null) {
            throw new NodeNotFoundException(key);
        }
        // This is the root node ...
        return wsCache.rootPath();
    }

    @Override
    public Name getPrimaryType( NodeCache cache ) {
        Property prop = getProperty(JcrLexicon.PRIMARY_TYPE, cache);
        assert prop != null;
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.nameFactory().create(prop.getFirstValue());
    }

    @Override
    public Set<Name> getMixinTypes( NodeCache cache ) {
        Property prop = getProperty(JcrLexicon.MIXIN_TYPES, cache);
        if (prop == null || prop.size() == 0) return Collections.emptySet();

        final NameFactory nameFactory = workspaceCache(cache).nameFactory();
        if (prop.size() == 1) {
            Name name = nameFactory.create(prop.getFirstValue());
            return Collections.singleton(name);
        }
        Set<Name> names = new HashSet<Name>();
        for (Object value : prop) {
            Name name = nameFactory.create(value);
            names.add(name);
        }
        return names;
    }

    @Override
    public int getPropertyCount( NodeCache cache ) {
        if (propertiesFullyLoaded) return properties().size();
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().countProperties(document(wsCache));
    }

    @Override
    public boolean hasProperties( NodeCache cache ) {
        Map<Name, Property> props = properties();
        if (!props.isEmpty()) return true;
        if (propertiesFullyLoaded) return false;
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().hasProperties(document(wsCache));
    }

    @Override
    public boolean hasProperty( Name name,
                                NodeCache cache ) {
        Map<Name, Property> props = properties();
        if (props.containsKey(name)) return true;
        if (propertiesFullyLoaded) return false;
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().hasProperty(document(wsCache), name);
    }

    @Override
    public Property getProperty( Name name,
                                 NodeCache cache ) {
        Map<Name, Property> props = properties();
        Property property = props.get(name);
        if (property == null && !propertiesFullyLoaded) {
            WorkspaceCache wsCache = workspaceCache(cache);
            property = wsCache.translator().getProperty(document(wsCache), name);
            if (property != null) {
                props.put(name, property);
            }
        }
        return property;
    }

    @Override
    public Iterator<Property> getProperties( NodeCache cache ) {
        if (!propertiesFullyLoaded) {
            WorkspaceCache wsCache = workspaceCache(cache);
            wsCache.translator().getProperties(document(wsCache), properties());
            this.propertiesFullyLoaded = true;
        }
        return properties().values().iterator();
    }

    @Override
    public Iterator<Property> getProperties( Collection<?> namePatterns,
                                             NodeCache cache ) {
        WorkspaceCache wsCache = workspaceCache(cache);
        final NamespaceRegistry registry = wsCache.context().getNamespaceRegistry();
        return new PatternIterator<Property>(getProperties(wsCache), namePatterns) {
            @Override
            protected String matchable( Property value ) {
                return value.getName().getString(registry);
            }
        };
    }

    @Override
    public ChildReferences getChildReferences( NodeCache cache ) {
        if (childReferences == null) {
            // This is idempotent, so it's okay if another thread sneaks in here and recalculates the object before we do ...
            WorkspaceCache wsCache = workspaceCache(cache);
            childReferences = wsCache.translator().getChildReferences(wsCache, document(wsCache));
        }
        return childReferences;
    }

    @Override
    public Set<NodeKey> getReferrers( NodeCache cache,
                                      ReferenceType type ) {
        // Get the referrers ...
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().getReferrers(document(wsCache), type);
    }

    @Override
    public boolean isQueryable( NodeCache cache ) {
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().isQueryable(document(wsCache));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CachedNode) {
            CachedNode that = (CachedNode)obj;
            return this.getKey().equals(that.getKey());
        }
        return false;
    }

    @Override
    public String toString() {
        return getString(null);
    }

    public String getString( NamespaceRegistry registry ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(key).append(": ");
        if (document != null) sb.append(document);
        else sb.append(" <unloaded>");
        return sb.toString();
    }

    /**
     * A single object used to cache the parent's {@link ChildReference} that points to this node and methods that determine
     * whether this cached information is still valid.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static interface ParentReferenceToSelf {
        /**
         * Get the cached {@link ChildReference} instance.
         * 
         * @return the child reference; never null
         */
        ChildReference childReferenceInParent();

        /**
         * Determine if this object is still complete. Some implementations use weak references that can eventually become nulled
         * as the target is garbage collected. When that happens, this method should return true.
         * 
         * @return true if this object is still complete, or false if any information becomes garbage collected
         */
        boolean isComplete();

        /**
         * Determine if this instance is still valid, given the supplied {@link CachedNode} instance that represents the
         * most-recently acquired parent node representation.
         * 
         * @param recentParent the most recent cached node for the parent
         * @return true if this object is still valid, or false if the parent's information has changed since this object was
         *         created
         */
        boolean isValid( CachedNode recentParent );

        /**
         * Get whether this represents the {@link #childReferenceInParent() child reference} pointing to the root node.
         * 
         * @return true if the object that owns this is the root node, or false otherwise
         */
        boolean isRoot();
    }

    /**
     * A {@link ParentReferenceToSelf} implementation used only for the root node. The root node never changes, but it is the only
     * node that does not have a {@link ChildReference} pointing to it. Since ModeShape keeps all node names within the
     * {@link ChildReference} instances, this method simply holds onto a special {@link WorkspaceCache#childReferenceForRoot()
     * ChildReference that contains the root's name}.
     * <p>
     * This class is immutable, and it is only used for the root {@link LazyCachedNode} instance (which can indeed change as
     * properties and children are added/removed/changed.
     * </p>
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @Immutable
    protected static final class RootParentReferenceToSelf implements ParentReferenceToSelf {
        private final ChildReference childReferenceInParent;

        protected RootParentReferenceToSelf( WorkspaceCache cache ) {
            childReferenceInParent = cache.childReferenceForRoot();
        }

        @Override
        public boolean isRoot() {
            return true;
        }

        @Override
        public ChildReference childReferenceInParent() {
            return this.childReferenceInParent;
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public boolean isValid( CachedNode recentParent ) {
            return true;
        }

        @Override
        public String toString() {
            return "RootParentReferenceToSelf";
        }
    }

    /**
     * A {@link ParentReferenceToSelf} implementation that caches the {@link ChildReference} from the parent plus the actual
     * parent (via a weak reference to the {@link CachedNode}.
     * <p>
     * This class is immutable, because it is simply discarded when it is out of date; see
     * {@link LazyCachedNode#parentReferenceToSelf(WorkspaceCache)}).
     * </p>
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @Immutable
    protected static final class NonRootParentReferenceToSelf implements ParentReferenceToSelf {
        private final ChildReference childReferenceInParent;
        private final WeakReference<CachedNode> parent;

        protected NonRootParentReferenceToSelf( CachedNode parent,
                                                ChildReference childReferenceInParent ) {
            assert parent != null;
            assert childReferenceInParent != null;
            this.childReferenceInParent = childReferenceInParent;
            this.parent = new WeakReference<CachedNode>(parent);
        }

        @Override
        public boolean isRoot() {
            return false;
        }

        @Override
        public ChildReference childReferenceInParent() {
            return this.childReferenceInParent;
        }

        @Override
        public boolean isComplete() {
            return this.parent.get() != null; // null if this was garbage collected
        }

        @Override
        public boolean isValid( CachedNode recentParent ) {
            CachedNode parent = this.parent.get();
            return parent == recentParent;
        }

        @Override
        public String toString() {
            return childReferenceInParent.toString() + " in " + parent.get();
        }
    }

}
