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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.ChildReferences.ChildInsertions;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.NodeNotFoundInParentException;
import org.modeshape.jcr.cache.PathCache;
import org.modeshape.jcr.cache.ReferrerCounts;
import org.modeshape.jcr.cache.ReferrerCounts.MutableReferrerCounts;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.WrappedException;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.modeshape.jcr.value.basic.StringReference;
import org.modeshape.jcr.value.binary.AbstractBinary;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * A node used within a {@link SessionCache session} when that node has (or may have) transient (unsaved) changes. This node is an
 * extension of a {@link CachedNode}, and thus can be used as a regular cached node. All transient changes are captured as a delta
 * on top of the underlying {@link CachedNode workspace node}, and so any changes to the workspace node (made by other sessions'
 * save operations) are immediately reflected.
 */
@ThreadSafe
public class SessionNode implements MutableCachedNode {

    public enum LockChange {
        LOCK_FOR_SESSION,
        LOCK_FOR_NON_SESSION,
        UNLOCK;
    }

    private final NodeKey key;
    private final ConcurrentMap<Name, Property> changedProperties = new ConcurrentHashMap<Name, Property>();
    private final ConcurrentMap<Name, Name> removedProperties = new ConcurrentHashMap<Name, Name>();
    private final AtomicReference<FederatedSegmentChanges> federatedSegments = new AtomicReference<FederatedSegmentChanges>();
    private volatile NodeKey newParent;
    private final AtomicReference<ChangedAdditionalParents> additionalParents = new AtomicReference<ChangedAdditionalParents>();
    private final ChangedChildren changedChildren = new ChangedChildren();
    private final AtomicReference<MutableChildReferences> appended = new AtomicReference<MutableChildReferences>();
    private final AtomicReference<MixinChanges> mixinChanges = new AtomicReference<MixinChanges>();
    private final AtomicReference<ReferrerChanges> referrerChanges = new AtomicReference<ReferrerChanges>();
    private final AtomicReference<Boolean> excludeFromSearch = new AtomicReference<Boolean>();
    private final boolean isNew;
    private volatile LockChange lockChange;
    private final AtomicReference<PermissionChanges> permissionChanges = new AtomicReference<>();
    private final ConcurrentMap<String, Object> addedInternalProperties = new ConcurrentHashMap<>();
    private final Set<String> removedInternalProperties = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public SessionNode( NodeKey key,
                        boolean isNew ) {
        this.key = key;
        this.isNew = isNew;
        assert this.key != null;
    }

    protected final ChangedChildren changedChildren() {
        return changedChildren;
    }

    protected final Set<Name> removedProperties() {
        return removedProperties.keySet();
    }

    protected final ConcurrentMap<Name, Property> changedProperties() {
        return changedProperties;
    }

    protected final NodeKey newParent() {
        return newParent;
    }

    @Override
    public final boolean isNew() {
        return isNew;
    }

    @Override
    public final boolean isPropertyNew( SessionCache cache,
                                        Name propertyName ) {
        return isNew || (changedProperties.containsKey(propertyName) && !isPropertyInWorkspaceCache(cache, propertyName));
    }

    @Override
    public final boolean isPropertyModified( SessionCache cache,
                                             Name propertyName ) {
        return !isNew && changedProperties.containsKey(propertyName) && isPropertyInWorkspaceCache(cache, propertyName);
    }

    private boolean isPropertyInWorkspaceCache( SessionCache cache,
                                                Name propertyName ) {
        AbstractSessionCache session = session(cache);
        CachedNode raw = nodeInWorkspace(session);
        if (raw == null) {
            return false; // the node doesn't exist in the workspace cache
        }
        WorkspaceCache workspaceCache = workspace(cache);
        return raw.hasProperty(propertyName, workspaceCache);
    }

    @Override
    public boolean hasChanges() {
        return hasPropertyChanges() || hasNonPropertyChanges();
    }

    @Override
    public boolean hasNonPropertyChanges() {
        if (isNew) return true;
        if (newParent != null) return true;
        ChangedChildren changedChildren = changedChildren();
        if (changedChildren != null && !changedChildren.isEmpty()) return true;
        MutableChildReferences childRefChanges = appended(false);
        if (childRefChanges != null && !childRefChanges.isEmpty()) return true;
        ChangedAdditionalParents additionalParents = additionalParents();
        if (additionalParents != null && !additionalParents.isEmpty()) return true;
        ReferrerChanges referrerChanges = referrerChanges(false);
        if (referrerChanges != null && !referrerChanges.isEmpty()) return true;
        return false;
    }

    @Override
    public boolean hasPropertyChanges() {
        if (isNew) return true;
        if (!changedProperties.isEmpty()) return true;
        if (!removedProperties.isEmpty()) return true;
        return false;
    }

    @Override
    public void lock( boolean sessionScoped ) {
        this.lockChange = sessionScoped ? LockChange.LOCK_FOR_SESSION : LockChange.LOCK_FOR_NON_SESSION;
    }

    @Override
    public void unlock() {
        this.lockChange = LockChange.UNLOCK;
    }

    public LockChange getLockChange() {
        return lockChange;
    }

    private boolean addAdditionalParent( NodeCache cache,
                                         NodeKey newParent ) {
        assert newParent != null;
        if (newParent.equals(this.newParent)) return false;
        NodeKey existingParentKey = getParentKey(cache);
        if (newParent.equals(existingParentKey)) {
            // This child is already the primary child of the new parent ...
            return false;
        }
        ChangedAdditionalParents additionalParents = this.additionalParents.get();
        if (additionalParents == null) {
            additionalParents = new ChangedAdditionalParents();
            if (!this.additionalParents.compareAndSet(null, additionalParents)) {
                additionalParents = this.additionalParents.get();
            }
        }
        return additionalParents.add(newParent);
    }

    private boolean removeAdditionalParent( NodeCache cache,
                                            NodeKey oldParent ) {
        if (getAdditionalParentKeys(cache).contains(oldParent)) {
            ChangedAdditionalParents additionalParents = this.additionalParents.get();
            if (additionalParents == null) {
                additionalParents = new ChangedAdditionalParents();
                if (!this.additionalParents.compareAndSet(null, additionalParents)) {
                    additionalParents = this.additionalParents.get();
                }
            }
            return additionalParents.remove(oldParent);
        }
        return false;
    }

    protected void replaceParentWithAdditionalParent( NodeCache cache,
                                                      NodeKey oldParent,
                                                      NodeKey existingAdditionalParent ) {
        assert getAdditionalParentKeys(cache).contains(existingAdditionalParent);
        newParent = existingAdditionalParent;
        ChangedAdditionalParents additionalParents = this.additionalParents.get();
        if (additionalParents == null) {
            additionalParents = new ChangedAdditionalParents();
            if (!this.additionalParents.compareAndSet(null, additionalParents)) {
                additionalParents = this.additionalParents.get();
            }
        }
        additionalParents.remove(existingAdditionalParent);
        additionalParents.remove(oldParent); // also record that we're removing primary parent
    }

    protected MutableChildReferences appended( boolean createIfMissing ) {
        MutableChildReferences appended = this.appended.get();
        if (appended == null && createIfMissing) {
            appended = new MutableChildReferences();
            if (!this.appended.compareAndSet(null, appended)) {
                appended = this.appended.get();
            }
        }
        return appended;
    }

    protected MixinChanges mixinChanges( boolean createIfMissing ) {
        MixinChanges changes = this.mixinChanges.get();
        if (changes == null && createIfMissing) {
            changes = new MixinChanges();
            if (!this.mixinChanges.compareAndSet(null, changes)) {
                changes = this.mixinChanges.get();
            }
        }
        return changes;
    }

    protected ReferrerChanges referrerChanges( boolean createIfMissing ) {
        ReferrerChanges changes = this.referrerChanges.get();
        if (changes == null && createIfMissing) {
            changes = new ReferrerChanges();
            if (!this.referrerChanges.compareAndSet(null, changes)) {
                changes = this.referrerChanges.get();
            }
        }
        return changes;
    }

    protected final WritableSessionCache writableSession( NodeCache cache ) {
        return (WritableSessionCache)cache.unwrap();
    }

    protected final AbstractSessionCache session( NodeCache cache ) {
        return (AbstractSessionCache)cache.unwrap();
    }

    protected final WorkspaceCache workspace( NodeCache cache ) {
        return ((DocumentCache)cache.unwrap()).workspaceCache();
    }

    /**
     * Get the CachedNode within the workspace cache.
     * 
     * @param session the session; may not be null
     * @return the workspace cache's node, or null if this node is new
     */
    protected CachedNode nodeInWorkspace( AbstractSessionCache session ) {
        return isNew() ? null : session.getWorkspace().getNode(key);
    }

    @Override
    public NodeKey getParentKey( NodeCache cache ) {
        if (newParent != null) {
            return newParent;
        }
        CachedNode cachedNode = nodeInWorkspace(session(cache));
        // if it is null, it means it has been removed in the meantime from the ws
        return cachedNode != null ? cachedNode.getParentKey(cache) : null;
    }

    @Override
    public NodeKey getParentKeyInAnyWorkspace( NodeCache cache ) {
        if (newParent != null) {
            return newParent;
        }
        CachedNode cachedNode = nodeInWorkspace(session(cache));
        // if it is null, it means it has been removed in the meantime from the ws
        return cachedNode != null ? cachedNode.getParentKeyInAnyWorkspace(cache) : null;
    }

    protected CachedNode parent( AbstractSessionCache session ) {
        NodeKey parentKey = getParentKey(session);
        if (parentKey == null) {
            return null;
        }
        return session.getNode(parentKey);
    }

    protected ChangedAdditionalParents additionalParents() {
        return this.additionalParents.get();
    }

    @Override
    public Set<NodeKey> getAdditionalParentKeys( NodeCache cache ) {
        // Get the additional parents on the persisted node ...
        AbstractSessionCache session = session(cache);
        CachedNode raw = nodeInWorkspace(session);
        Set<NodeKey> persisted = raw != null ? raw.getAdditionalParentKeys(cache) : null;

        ChangedAdditionalParents additionalParents = this.additionalParents.get();
        if (additionalParents == null || additionalParents.isEmpty()) {
            return persisted != null ? persisted : Collections.<NodeKey>emptySet();
        }

        // Make an immutable copy ...
        Set<NodeKey> copy = persisted != null ? new LinkedHashSet<NodeKey>(persisted) : new LinkedHashSet<NodeKey>();
        copy.addAll(additionalParents.getAdditions());
        copy.removeAll(additionalParents.getRemovals());
        return Collections.unmodifiableSet(copy);
    }

    @Override
    public boolean hasOnlyChangesToAdditionalParents() {
        if (isNew) return false;
        if (newParent != null) return false;
        if (!changedProperties.isEmpty()) return false;
        if (!removedProperties.isEmpty()) return false;
        ChangedChildren changedChildren = changedChildren();
        if (changedChildren != null && !changedChildren.isEmpty()) return false;
        MutableChildReferences childRefChanges = appended(false);
        if (childRefChanges != null && !childRefChanges.isEmpty()) return false;
        ChangedAdditionalParents additionalParents = additionalParents();
        if (additionalParents != null && !additionalParents.isEmpty()) return true;
        return false;
    }

    @Override
    public boolean isAtOrBelow( NodeCache cache,
                                Path path ) {
        Path aPath = getPath(cache);
        if (path.isAtOrAbove(aPath)) return true;
        ChangedAdditionalParents additionalParents = additionalParents();
        if (additionalParents != null && !additionalParents.isEmpty()) {
            for (NodeKey parentKey : additionalParents.getAdditions()) {
                CachedNode parent = cache.getNode(parentKey);
                if (parent.getPath(cache).isAtOrBelow(path)) {
                    return true;
                }
            }
            for (NodeKey parentKey : additionalParents.getRemovals()) {
                CachedNode parent = cache.getNode(parentKey);
                if (parent != null && parent.getPath(cache).isAtOrBelow(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public NodeKey getKey() {
        return key;
    }

    @Override
    public Name getName( NodeCache cache ) {
        return getSegment(cache).getName();
    }

    @Override
    public Segment getSegment( NodeCache cache ) {
        AbstractSessionCache session = session(cache);
        CachedNode parent = parent(session);
        return getSegment(cache, parent);
    }

    /**
     * Get the segment for this node.
     * 
     * @param cache the cache
     * @param parent the parent node
     * @return the segment
     * @throws NodeNotFoundInParentException if the node doesn't exist in the referenced parent
     */
    protected final Segment getSegment( NodeCache cache,
                                        CachedNode parent ) {
        if (parent != null) {
            ChildReference ref = parent.getChildReferences(cache).getChild(key);
            if (ref == null) {
                // This node doesn't exist in the parent
                throw new NodeNotFoundInParentException(key, parent.getKey());
            }
            return ref.getSegment();
        }
        // This is the root node ...
        return workspace(cache).childReferenceForRoot().getSegment();
    }

    @Override
    public int getDepth( NodeCache cache ) throws NodeNotFoundException {
        AbstractSessionCache session = session(cache);
        CachedNode parent = parent(session);
        if (parent != null) {
            // This is not the root, so get our parent's depth and add 1 ...
            return parent.getDepth(cache) + 1;
        }
        // make sure that this isn't a node which has been removed in the meantime
        CachedNode persistedNode = workspace(cache).getNode(key);
        if (persistedNode == null) {
            throw new NodeNotFoundException(key);
        }
        // This is the root node ...
        return 0;
    }

    @Override
    public Path getPath( NodeCache cache ) {
        AbstractSessionCache session = session(cache);
        CachedNode parent = parent(session);
        if (parent != null) {
            Path parentPath = parent.getPath(session);
            return session.pathFactory().create(parentPath, getSegment(session, parent));
        }
        // make sure that this isn't a node which has been removed in the meantime
        CachedNode persistedNode = workspace(cache).getNode(key);
        if (persistedNode == null) {
            throw new NodeNotFoundException(key);
        }
        // This is the root node ...
        return session.rootPath();
    }

    @Override
    public Path getPath( PathCache pathCache ) throws NodeNotFoundException {
        NodeCache cache = pathCache.getCache();
        AbstractSessionCache session = session(cache);
        CachedNode parent = parent(session);
        if (parent != null) {
            Path parentPath = pathCache.getPath(parent);
            return session.pathFactory().create(parentPath, getSegment(session, parent));
        }
        // make sure that this isn't a node which has been removed in the meantime
        CachedNode persistedNode = workspace(cache).getNode(key);
        if (persistedNode == null) {
            throw new NodeNotFoundException(key);
        }
        // This is the root node ...
        return session.rootPath();
    }

    @Override
    public Name getPrimaryType( NodeCache cache ) {
        AbstractSessionCache session = session(cache);
        Property prop = getProperty(JcrLexicon.PRIMARY_TYPE, session);
        NameFactory nameFactory = session.nameFactory();
        return prop != null ? nameFactory.create(prop.getFirstValue()) : nameFactory.create((Object)null);
    }

    @Override
    public boolean hasChangedPrimaryType() {
        return changedProperties.containsKey(JcrLexicon.PRIMARY_TYPE);
    }

    @Override
    public Set<Name> getMixinTypes( NodeCache cache ) {
        AbstractSessionCache session = session(cache);
        Property prop = getProperty(JcrLexicon.MIXIN_TYPES, session);
        MixinChanges changes = mixinChanges(false);
        if (prop == null || prop.size() == 0) {
            return changes != null ? changes.getAdded() : Collections.<Name>emptySet();
        }

        final NameFactory nameFactory = session.nameFactory();
        if (prop.size() == 1) {
            Name name = nameFactory.create(prop.getFirstValue());
            if (changes == null) return Collections.singleton(name);
            Set<Name> all = new HashSet<Name>(changes.getAdded());
            all.add(name);
            all.removeAll(changes.getRemoved());
            return all;
        }
        Set<Name> names = new HashSet<Name>();
        for (Object value : prop) {
            Name name = nameFactory.create(value);
            if (changes == null || !changes.getRemoved().contains(name)) names.add(name);
        }
        if (changes != null) names.addAll(changes.getAdded());
        return names;
    }

    @Override
    public void addMixin( SessionCache cache,
                          Name mixinName ) {
        assert mixinName != null;
        if (isNew) {
            // We have the actual properties, so just update the jcr:mixinTypes property ...
            Property mixinTypes = cache.getContext().getPropertyFactory().create(JcrLexicon.MIXIN_TYPES, new Name[] {mixinName});
            Property existing = changedProperties().putIfAbsent(mixinTypes.getName(), mixinTypes);
            while (existing != null) {
                // There was an existing property ...
                List<Object> values = new ArrayList<Object>(existing.size());
                for (Object value : existing) {
                    if (mixinName.equals(value)) {
                        // Already a mixin
                        return;
                    }
                    values.add(value);
                }
                values.add(mixinName);
                mixinTypes = cache.getContext().getPropertyFactory().create(JcrLexicon.MIXIN_TYPES, values);
                Property existing2 = changedProperties().put(mixinTypes.getName(), mixinTypes);
                if (existing2 == existing) {
                    // We replaced the property object that we used to create the new one, so we're good
                    existing = null;
                } else {
                    // Someone snuck in there, so try the operation again ...
                    existing = existing2;
                }
            }
        } else {
            mixinChanges(true).add(mixinName);
        }
    }

    @Override
    public void removeMixin( SessionCache cache,
                             Name mixinName ) {
        if (isNew) {
            // We have the actual properties, so just update the jcr:mixinTypes property ...
            Property existing = changedProperties().get(JcrLexicon.MIXIN_TYPES);
            while (existing != null) {
                // There was an existing property ...
                List<Object> values = new ArrayList<Object>(existing.size());
                boolean found = false;
                for (Object value : existing) {
                    if (mixinName.equals(value)) {
                        found = true;
                    } else {
                        values.add(value);
                    }
                }
                if (!found) {
                    // The mixin was not in the list ...
                    return;
                }
                Property mixinTypes = cache.getContext().getPropertyFactory().create(JcrLexicon.MIXIN_TYPES, values);
                Property existing2 = changedProperties().put(mixinTypes.getName(), mixinTypes);
                if (existing2 == existing) {
                    // We replaced the property object that we used to create the new one, so we're good
                    existing = null;
                } else {
                    // Someone snuck in there, so try the operation again ...
                    existing = existing2;
                }
            }
        } else {
            mixinChanges(true).remove(mixinName);
        }
    }

    public MixinChanges getMixinChanges() {
        return mixinChanges(false);
    }

    @Override
    public Set<Name> getAddedMixins( SessionCache cache ) {
        if (!isNew()) {
            MixinChanges mixinChanges = mixinChanges(false);
            if (mixinChanges != null) return mixinChanges.getAdded();
            return Collections.emptySet();
        }
        // Otherwise this is a new node, so we should get the 'jcr:mixinTypes' property ...
        Property prop = changedProperties.get(JcrLexicon.MIXIN_TYPES);
        if (prop == null || prop.size() == 0) {
            return Collections.emptySet();
        }
        final NameFactory nameFactory = session(cache).nameFactory();
        Set<Name> names = new HashSet<Name>();
        for (Object value : prop) {
            Name name = nameFactory.create(value);
            names.add(name);
        }
        return names;
    }

    @Override
    public Set<NodeKey> getReferrers( NodeCache cache,
                                      ReferenceType type ) {
        AbstractSessionCache session = session(cache);
        ReferrerChanges changes = referrerChanges(false);
        // Check the persisted node ...
        CachedNode persisted = nodeInWorkspace(session);
        if (persisted == null) {
            if (changes == null) return Collections.emptySet();
            return new HashSet<NodeKey>(changes.getAddedReferrers(type));
        }
        // Read the referrers from the workspace node ...
        Set<NodeKey> referrers = persisted.getReferrers(workspace(cache), type);
        if (changes != null) {
            // we need to take the transient state into account, so add everything
            referrers.addAll(changes.getAddedReferrers(type));
            referrers.removeAll(changes.getRemovedReferrers(type));
        }
        return referrers;
    }

    @Override
    public ReferrerCounts getReferrerCounts( NodeCache cache ) {
        AbstractSessionCache session = session(cache);
        ReferrerChanges changes = referrerChanges(false);
        // Check the persisted node ...
        CachedNode persisted = nodeInWorkspace(session);
        if (persisted == null) {
            if (changes == null) return null;
            return changes.getReferrerCounts(null);
        }
        // Read the referrers from the workspace node ...
        ReferrerCounts persistedCounts = persisted.getReferrerCounts(workspace(cache));
        return changes == null ? persistedCounts : changes.getReferrerCounts(persistedCounts);
    }

    protected ReferrerChanges getReferrerChanges() {
        return referrerChanges(false);
    }

    @Override
    public void addReferrer( SessionCache cache,
                             Property property,
                             NodeKey referrerKey,
                             ReferenceType type ) {
        ReferrerChanges changes = referrerChanges(true);
        switch (type) {
            case WEAK:
                changes.addWeakReferrer(property, referrerKey);
                break;
            case STRONG:
                changes.addStrongReferrer(property, referrerKey);
                break;
            case BOTH:
                throw new IllegalArgumentException("The type parameter may be WEAK or STRONG, but may not be BOTH");
        }
    }

    @Override
    public void removeReferrer( SessionCache cache,
                                Property property,
                                NodeKey referrerKey,
                                ReferenceType type ) {
        ReferrerChanges changes = referrerChanges(true);
        switch (type) {
            case WEAK:
                changes.removeWeakReferrer(property, referrerKey);
                break;
            case STRONG:
                changes.removeStrongReferrer(property, referrerKey);
                break;
            case BOTH:
                throw new IllegalArgumentException("The type parameter may be WEAK or STRONG, but may not be BOTH");
        }
    }

    @Override
    public int getPropertyCount( NodeCache cache ) {
        int count = changedProperties.size() - removedProperties.size();
        // Delegate to the workspace node (if it exists) ...
        AbstractSessionCache session = session(cache);
        CachedNode raw = nodeInWorkspace(session);
        return raw != null ? count + raw.getPropertyCount(session) : count;
    }

    @Override
    public boolean hasProperties( NodeCache cache ) {
        if (!changedProperties.isEmpty()) return true;
        // Delegate to the workspace node (if it exists) ...
        AbstractSessionCache session = session(cache);
        CachedNode raw = nodeInWorkspace(session);
        return raw != null ? raw.hasProperties(session) : false;
    }

    @Override
    public boolean hasProperty( Name name,
                                NodeCache cache ) {
        if (changedProperties.containsKey(name)) return true;
        if (isPropertyRemoved(name)) return false;
        // Otherwise, delegate to the workspace node (if it exists) ...
        AbstractSessionCache session = session(cache);
        CachedNode raw = nodeInWorkspace(session);
        return raw != null ? raw.hasProperty(name, session) : false;
    }

    @Override
    public Property getProperty( Name name,
                                 NodeCache cache ) {
        Property prop = null;
        if ((prop = changedProperties.get(name)) != null) return prop;
        if (isPropertyRemoved(name)) return null;
        // Otherwise, delegate to the workspace node (if it exists) ...
        AbstractSessionCache session = session(cache);
        CachedNode raw = nodeInWorkspace(session);
        return raw != null ? raw.getProperty(name, session) : null;
    }

    protected final boolean isPropertyRemoved( Name name ) {
        return !isNew && removedProperties.containsKey(name);
    }

    @Override
    public Properties getPropertiesByName( final NodeCache cache ) {
        final AbstractSessionCache session = session(cache);
        final CachedNode raw = nodeInWorkspace(session);
        final ConcurrentMap<Name, Property> changedProperties = this.changedProperties;
        final ConcurrentMap<Name, Name> removedProperties = this.removedProperties;
        return new Properties() {
            @Override
            public Property getProperty( Name name ) {
                // First check the removed properties ...
                if (removedProperties.containsKey(name)) return null;
                // Then check the changed properties ...
                Property property = changedProperties.get(name);
                if (property == null && raw != null) {
                    // Check the raw property ...
                    property = raw.getProperty(name, cache);
                }
                return property;
            }

            @Override
            public Iterator<Property> iterator() {
                return getProperties(cache);
            }
        };
    }

    @Override
    public Iterator<Property> getProperties( final NodeCache cache ) {
        final AbstractSessionCache session = session(cache);
        final CachedNode raw = nodeInWorkspace(session);
        final ConcurrentMap<Name, Property> changedProperties = this.changedProperties;
        Iterable<Property> rawProps = raw == null ? null : new Iterable<Property>() {
            @Override
            public Iterator<Property> iterator() {
                List<Property> values = new LinkedList<Property>();
                for (Iterator<Property> iter = raw.getProperties(workspace(cache)); iter.hasNext();) {
                    Property prop = iter.next();
                    // we need to reflect transient state, so ignore removed and changed properties from the raw values
                    if (isPropertyRemoved(prop.getName()) || changedProperties.containsKey(prop.getName())) {
                        continue;
                    }
                    values.add(prop);
                }
                return values.iterator();
            }
        };
        // Return an iterator that iterates over the changed properties first, then the raw properties ...
        return new UnionIterator<Property>(changedProperties.values().iterator(), rawProps);
    }

    @Override
    public Iterator<Property> getProperties( Collection<?> namePatterns,
                                             NodeCache cache ) {
        final AbstractSessionCache session = session(cache);
        final NamespaceRegistry registry = session.context().getNamespaceRegistry();
        return new PatternIterator<Property>(getProperties(session), namePatterns) {
            @Override
            protected String matchable( Property value ) {
                return value.getName().getString(registry);
            }
        };
    }

    @Override
    public void setProperty( SessionCache cache,
                             Property property ) {
        writableSession(cache).assertInSession(this);
        Name name = property.getName();
        changedProperties.put(name, property);
        if (!isNew) removedProperties.remove(name);
        processPropertyChange(cache, name, null);
    }

    @Override
    public void setReference( SessionCache cache,
                              Property property,
                              SessionCache systemCache ) {
        assert property.isEmpty() || property.isReference();

        writableSession(cache).assertInSession(this);
        Name name = property.getName();
        changedProperties.put(name, property);
        if (!isNew) removedProperties.remove(name);
        processPropertyChange(cache, name, systemCache);
    }

    private void processPropertyChange( SessionCache cache,
                                        Name propertyName,
                                        SessionCache systemCache ) {
        Property propertyWhichWasRemoved = null;
        Property propertyWhichWasAdded = null;

        // first try to determine if there's old reference property with the same name so that old references can be removed
        boolean oldPropertyWasReference = false;
        List<Reference> referencesToRemove = new ArrayList<>();
        Set<BinaryKey> binaryChanges = new HashSet<BinaryKey>();

        if (isPropertyModified(cache, propertyName) || isPropertyRemoved(propertyName)) {
            // remove potential existing references
            CachedNode persistedNode = nodeInWorkspace(session(cache));
            Property oldProperty = persistedNode.getProperty(propertyName, cache);
            if (oldProperty != null) {
                if (oldProperty.isReference()) {
                    oldPropertyWasReference = true;
                    propertyWhichWasRemoved = oldProperty;
                    for (Object referenceObject : oldProperty.getValuesAsArray()) {
                        assert referenceObject instanceof Reference;
                        referencesToRemove.add((Reference)referenceObject);
                    }
                }
                
                if (oldProperty.isBinary()) {
                    // track any removed binaries by this node  because they will need to be processed during save
                    for (Object binaryObject : oldProperty.getValuesAsArray()) {
                        assert binaryObject instanceof Binary;
                        if (binaryObject instanceof AbstractBinary) {
                            BinaryKey binaryKey = ((AbstractBinary)binaryObject).getKey();
                            binaryChanges.add(binaryKey);
                        }
                    }
                }
            } 
        }

        // if the updated property is a reference, determine which are the references that need updating
        boolean updatedPropertyIsReference = false;
        List<Reference> referencesToAdd = new ArrayList<>();
        Property property = changedProperties.get(propertyName);
        boolean keepPropertyAsChanged = false;
        
        if (property != null) {
            if (property.isReference()) {
                updatedPropertyIsReference = true;
                propertyWhichWasAdded = property;
                Object[] valuesAsArray = property.getValuesAsArray();

                for (int i = 0; i < valuesAsArray.length; i++) {
                    Object referenceObject = valuesAsArray[i];
                    assert referenceObject instanceof Reference;
                    Reference updatedReference = (Reference)referenceObject;
                    int referenceToRemoveIdx = referencesToRemove.indexOf(updatedReference);
                    if (referenceToRemoveIdx != -1) {
                        // the reference is already present on a property with the same name, so this is a no-op for that reference
                        // therefore we remove it from the list of references that will be removed,
                        // but we need to keep in mind the fact that references may be reordered  
                        if (referenceToRemoveIdx != i) {
                            // the index of the updated reference differs from the index of the existing reference, meaning
                            // that the reference has been reordered
                            keepPropertyAsChanged = true;
                        }
                        if (referencesToRemove.remove(updatedReference)) {
                            // Make sure that the referenced node is not modified to remove the back-reference to this node.
                            // See MODE-2283 for an example ...
                            NodeKey referredKey = nodeKeyFromReference(updatedReference);
                            CachedNode referredNode = cache.getNode(referredKey);
                            if (referredNode instanceof SessionNode) {
                                // The node was modified during this session and has unsaved changes ...
                                SessionNode changedReferrerNode = (SessionNode)referredNode;
                                ReferrerChanges changes = changedReferrerNode.getReferrerChanges();
                                if (changes != null) {
                                    // There are changes to that node's referrers ...
                                    ReferenceType type = updatedReference.isWeak() ? ReferenceType.WEAK : ReferenceType.STRONG;
                                    if (changes.isRemovedReferrer(key, type)) {
                                        // The referenced node had a back reference to this node, but we're no longer removing
                                        // this node's reference to the referenced node, and that means we should no longer remove
                                        // the referenced node's backreference to this node ...
                                        if (updatedReference.isWeak())
                                            changes.addWeakReferrer(propertyWhichWasRemoved, key);
                                        else
                                            changes.addStrongReferrer(propertyWhichWasRemoved, key);
                                    }
                                }
                            }
                        }
                    } else {
                        keepPropertyAsChanged = true;
                        // this is a new reference (either via key or type)
                        referencesToAdd.add(updatedReference);
                    }
                }
            }

            if (property.isBinary()) {
                //if the changed property is a binary, we need to track the change via the session cache
                for (Object binaryObject : property.getValuesAsArray()) {
                    assert binaryObject instanceof Binary;
                    if (binaryObject instanceof AbstractBinary) {
                        BinaryKey key = ((AbstractBinary)binaryObject).getKey();
                        if (binaryChanges.remove(key)) {
                            // the same binary key already appears in the set, meaning it has been both removed & changed, making it a no-op
                        } else {
                            // this is a new or updated binary reference which we must add 
                            binaryChanges.add(key);
                        }
                    }
                }
            }
        }
        
        // if there are any binary usage changes, send them to the session cache
        if (!binaryChanges.isEmpty()) {
            session(cache).addBinaryReference(key, binaryChanges.toArray(new BinaryKey[binaryChanges.size()]));
        }

        // if an existing reference property was just updated with the same value in the same order, it is a no-op so we should just 
        // remove it from the list of changed properties
        if (!keepPropertyAsChanged &&
            referencesToRemove.isEmpty() &&
            referencesToAdd.isEmpty() &&
            oldPropertyWasReference &&
            updatedPropertyIsReference) {
            changedProperties.remove(propertyName);
            return;
        }

        if (!referencesToRemove.isEmpty()) {
            addOrRemoveReferrers(cache, systemCache, propertyWhichWasRemoved, referencesToRemove.iterator(), false);
        }
        if (!referencesToAdd.isEmpty()) {
            addOrRemoveReferrers(cache, systemCache, propertyWhichWasAdded, referencesToAdd.iterator(), true);
        }
    }

    protected void removeAllReferences( SessionCache cache ) {
        for (Iterator<Property> it = this.getProperties(cache); it.hasNext();) {
            Property property = it.next();
            if (!property.isReference()) {
                continue;
            }

            this.addOrRemoveReferrers(cache, null, property, property.getValues(), false);
        }
    }

    protected void addOrRemoveReferrers( SessionCache cache,
                                         SessionCache systemCache,
                                         Property property,
                                         Iterator<?> referenceValuesIterator,
                                         boolean add ) {

        boolean isFrozenNode = JcrNtLexicon.FROZEN_NODE.equals(this.getPrimaryType(cache));

        while (referenceValuesIterator.hasNext()) {
            Object value = referenceValuesIterator.next();
            assert value instanceof Reference;

            Reference reference = (Reference)value;
            NodeKey referredKey = nodeKeyFromReference(reference);
            boolean isWeak = reference.isWeak();

            if (isFrozenNode && !isWeak) {
                // JCR 3.13.4.6 ignore all strong outgoing references from a frozen node
                return;
            }

            SessionNode referredNode = null;
            WritableSessionCache writableSessionCache = null;
            // first search for a referred node in the cache of the current session and if nothing is found, look in the system
            // session
            if (cache.getNode(referredKey) != null) {
                writableSessionCache = writableSession(cache);
                referredNode = writableSessionCache.mutable(referredKey);
            } else if (systemCache != null && systemCache.getNode(referredKey) != null) {
                writableSessionCache = writableSession(systemCache);
                referredNode = writableSessionCache.mutable(referredKey);
            }

            if (referredNode == null) {
                continue;
            }
            assert writableSessionCache != null;

            ReferenceType referenceType = isWeak ? ReferenceType.WEAK : ReferenceType.STRONG;
            if (add) {
                referredNode.addReferrer(cache, property, key, referenceType);
            } else {
                referredNode.removeReferrer(cache, property, key, referenceType);
            }

            // we may have updated the references multiple times for this node (added/removed) resulting in a no-op in the end
            // so we should clear this node from the cache
            if (!referredNode.hasChanges()) {
                writableSessionCache.clear(referredNode);
            }
        }
    }

    private NodeKey nodeKeyFromReference( Reference reference ) {
        if (reference instanceof NodeKeyReference) {
            return ((NodeKeyReference)reference).getNodeKey();
        } else if (reference instanceof StringReference) {
            return new NodeKey(reference.getString());
        }
        throw new IllegalArgumentException("Unknown reference type: " + reference.getClass().getSimpleName());
    }

    @Override
    public void setPropertyIfUnchanged( SessionCache cache,
                                        Property property ) {
        Name propertyName = property.getName();
        boolean isModified = changedProperties.containsKey(propertyName)
                             && (isNew || isPropertyInWorkspaceCache(cache, propertyName));
        if (!isModified) {
            setProperty(cache, property);
        }
    }

    @Override
    public void setProperties( SessionCache cache,
                               Iterable<Property> properties ) {
        writableSession(cache).assertInSession(this);
        for (Property property : properties) {
            Name name = property.getName();
            changedProperties.put(name, property);
            if (!isNew) removedProperties.remove(name);
            processPropertyChange(cache, name, null);
        }
    }

    @Override
    public void setProperties( SessionCache cache,
                               Iterator<Property> properties ) {
        writableSession(cache).assertInSession(this);
        while (properties.hasNext()) {
            Property property = properties.next();
            Name name = property.getName();
            changedProperties.put(name, property);
            if (!isNew) removedProperties.remove(name);
            processPropertyChange(cache, name, null);
        }
    }

    @Override
    public void removeProperty( SessionCache cache,
                                Name name ) {
        writableSession(cache).assertInSession(this);
        changedProperties.remove(name);
        if (!isNew) {
            // Determine if the existing node already contained this property ...
            AbstractSessionCache session = session(cache);
            CachedNode raw = nodeInWorkspace(session);
            if (raw.hasProperty(name, cache)) {
                removedProperties.put(name, name);
            }
        }
        processPropertyChange(cache, name, null);
    }

    @Override
    public void removeAllProperties( SessionCache cache ) {
        writableSession(cache).assertInSession(this);
        CachedNode raw = null;
        for (Iterator<Property> propertyIterator = getProperties(cache); propertyIterator.hasNext();) {
            Name name = propertyIterator.next().getName();
            changedProperties.remove(name);
            if (!isNew) {
                // Determine if the existing node already contained this property ...
                if (raw == null) {
                    AbstractSessionCache session = session(cache);
                    raw = nodeInWorkspace(session);
                }
                if (raw.hasProperty(name, cache)) {
                    removedProperties.put(name, name);
                }
            }
            processPropertyChange(cache, name, null);
        }
    }

    @Override
    public ChildReferences getChildReferences( NodeCache cache ) {
        boolean allowsSNS = allowsSNS(cache);
        if (isNew) {
            // Then this node was created in this session. Note that it is possible that there still may be a persisted node,
            // meaning that the persisted node was likely removed in this session and (without saving) a new node was created
            // using the same key as the persisted node. Therefore, we do NOT want to use the persisted node in this case ...
            return new SessionChildReferences(null, appended.get(), changedChildren, allowsSNS);
        }
        // Find the persisted information, since the info we have is relative to it ...
        CachedNode persistedNode = nodeInWorkspace(session(cache));
        ChildReferences persisted = persistedNode != null ? persistedNode.getChildReferences(cache) : null;

        // And create a transient implementation ...
        return new SessionChildReferences(persisted, appended.get(), changedChildren, allowsSNS);
    }

    private boolean allowsSNS( NodeCache cache ) {
        Name primaryType = getPrimaryType(cache);
        Set<Name> mixinTypes = getMixinTypes(cache);
        RepositoryEnvironment repositoryEnvironment = workspace(cache).repositoryEnvironment();
        if ( repositoryEnvironment == null) {
            return true;
        }
        NodeTypes nodeTypes = repositoryEnvironment.nodeTypes();
        return nodeTypes == null || nodeTypes.allowsNameSiblings(primaryType, mixinTypes);
    }

    @Override
    public MutableCachedNode createChild( SessionCache cache,
                                          NodeKey key,
                                          Name name,
                                          Property firstProperty,
                                          Property... additionalProperties ) {
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);

        if (key == null) key = getKey().withRandomId();

        // Create the new node ...
        SessionNode child = new SessionNode(key, true);

        // Add the new node to the session's cache (which records the change)...
        child = session.add(child);

        // Add the properties ...
        if (firstProperty != null) {
            child.setProperty(cache, firstProperty);
        }
        if (additionalProperties != null) {
            for (Property property : additionalProperties) {
                if (property != null) child.setProperty(cache, property);
            }
        }

        // And parent reference to this node ...
        child.newParent = this.key;

        // And finally append the new node as a child of this node ...
        appended(true).append(key, name);

        return child;
    }

    @Override
    public MutableCachedNode createChild( SessionCache cache,
                                          NodeKey key,
                                          Name name,
                                          Iterable<Property> properties ) {
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);

        if (key == null) key = getKey().withRandomId();

        // Create the new node ...
        SessionNode child = new SessionNode(key, true);

        // Add the new node to the session's cache (which records the change)...
        child = session.add(child);

        // Add the properties ...
        if (properties != null) {
            for (Property property : properties) {
                if (property != null) child.setProperty(cache, property);
            }
        }

        // And parent reference to this node ...
        child.newParent = this.key;

        // And finally append the new node as a child of this node ...
        appended(true).append(key, name);

        return child;
    }

    @Override
    public void removeChild( SessionCache cache,
                             NodeKey key ) {
        // Remove the child from this node ...
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);
        removeChildFromNode(session, key);
    }

    @Override
    public void moveChild( SessionCache cache,
                           NodeKey key,
                           MutableCachedNode newParent,
                           Name newName ) {
        assert newParent != this;
        assert newParent != null;

        // Remove the child from this node ...
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);
        SessionNode node = removeChildFromNode(session, key);
        // Add it to the new parent ...
        if (newName == null) newName = node.getName(session);
        ((SessionNode)newParent).appended(true).append(key, newName);
        node.newParent = newParent.getKey();
    }

    /**
     * Remove the child from this parent node. This method works whether or not the child is a shared node (e.g., a shareable node
     * that has 2+ nodes in the shared set).
     * 
     * @param session the session
     * @param childKey the child key; may not be null
     * @return the child node; never null
     */
    protected SessionNode removeChildFromNode( AbstractSessionCache session,
                                               NodeKey childKey ) {
        // First, manipulate the child node. But we have to see whether this node is a primary parent or an additional parent ...
        SessionNode child = session.mutable(childKey);
        if (child.getParentKey(session).equals(this.key)) {
            // The child's parent is this node. If there are additional parents, then we should pick the first additional parent
            // and use it as the new primary parent ...
            Set<NodeKey> additionalParentKeys = child.getAdditionalParentKeys(session);
            if (additionalParentKeys.isEmpty()) {
                child.newParent = null;
            } else {
                // There are additional parents, and we're removing the primary parent
                NodeKey newParentKey = additionalParentKeys.iterator().next();
                child.replaceParentWithAdditionalParent(session, this.key, newParentKey);
            }
        } else {
            // The child's parent is NOT this node, so this node must be an additional parent...
            boolean removed = child.removeAdditionalParent(session, this.key);
            if (!removed) {
                // Not a primary or additional parent ...
                if (!getChildReferences(session).hasChild(childKey)) {
                    throw new NodeNotFoundException(childKey);
                }
            }
        }

        // Now, update this node (the parent) ...
        MutableChildReferences appended = this.appended.get();
        ChildReference removed = null;
        if (appended != null) {
            removed = appended.remove(childKey); // The node may have been appended to this node but not yet persisted ...
        }
        if (removed == null) {
            changedChildren.remove(childKey);
        }
        return child;
    }

    @Override
    public void reorderChild( SessionCache cache,
                              NodeKey key,
                              NodeKey nextNode ) {
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);

        ChildReferences references = getChildReferences(session);
        ChildReference before = null;
        if (nextNode != null) {
            before = references.getChild(nextNode);
            if (before == null) throw new NodeNotFoundException(key);
        }

        // Remove the node from where it is ...
        MutableChildReferences appended = this.appended.get();
        ChildReference toBeMoved = null;
        if (appended != null) {
            // Try to remove it from the appended nodes ...
            toBeMoved = appended.remove(key);
        }

        if (toBeMoved == null) {
            // It wasn't appended, so verify it is really a child ...
            toBeMoved = references.getChild(key);
            if (toBeMoved == null) throw new NodeNotFoundException(key);
            if (changedChildren.inserted(key) == null) {
                // And mark it as removed only if it doesn't appear as inserted
                // a node can be transient, not appended but inserted in the case of transient reorderings
                changedChildren.remove(key);
            }
        }

        if (nextNode == null) {
            // The node is to be placed at the end of the children ...
            appended(true).append(key, toBeMoved.getName());
        } else {
            // The node is to be inserted at some point in the children. Note that we do this regardless of
            // where it already is, even if it is in the correct spot already, because someone else may move
            // it between the time we check and the time our session is saved.
            changedChildren.insertBefore(before, toBeMoved);
        }
    }

    @Override
    public void renameChild( SessionCache cache,
                             NodeKey key,
                             Name newName ) {
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);
        ChildReferences references = getChildReferences(session);
        if (!references.hasChild(key)) throw new NodeNotFoundException(key);

        // We need a mutable node in the session for the child, so that we can find changes in the parent ...
        cache.mutable(key);

        // If the node was previously appended ...
        MutableChildReferences appended = this.appended.get();
        if (appended != null && appended.hasChild(key)) {
            // Just remove and re-add with the new name ...
            appended.remove(key);
            appended.append(key, newName);
        } else {
            // Now perform the rename ...
            changedChildren.renameTo(key, newName);
        }
    }

    @Override
    public boolean linkChild( SessionCache cache,
                              NodeKey childKey,
                              Name name ) {
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);

        // Find the referenced node ...
        SessionNode child = session.mutable(childKey);
        if (!child.isNew() && this.key.equals(child.getParentKey(cache))) {
            // Already a linked child under this parent
            return false;
        }

        // Add this node as a parent for the child ...
        if (child.addAdditionalParent(cache, this.key)) {
            // Add it as a parent of this node ...
            appended(true).append(childKey, name);
        }
        return true;
    }

    @Override
    public String getEtag( SessionCache cache ) {
        StringBuilder sb = new StringBuilder();
        Iterator<Property> iter = getProperties(cache);
        while (iter.hasNext()) {
            Property prop = iter.next();
            if (prop.isEmpty()) continue;
            for (Object value : prop) {
                if (value instanceof BinaryValue) {
                    BinaryValue binary = (BinaryValue)value;
                    // we don't care about the string encoding, as long as its consistent and will change if a property changes
                    sb.append(binary.getHexHash());
                }
            }
        }
        return sb.toString();
    }

    @Override
    public Map<NodeKey, NodeKey> deepCopy( SessionCache cache,
                                           CachedNode sourceNode,
                                           SessionCache sourceCache,
                                           String systemWorkspaceKey,
                                           Connectors connectors ) {
        final WritableSessionCache writableSessionCache = writableSession(cache);
        writableSessionCache.assertInSession(this);
        DeepCopy copier = new DeepCopy(this, writableSessionCache, sourceNode, sourceCache, systemWorkspaceKey, connectors);
        copier.execute();
        return copier.getSourceToTargetKeys();
    }

    @Override
    public void deepClone( SessionCache cache,
                           CachedNode sourceNode,
                           SessionCache sourceCache,
                           String systemWorkspaceKey,
                           Connectors connectors ) {
        final WritableSessionCache writableSessionCache = writableSession(cache);
        writableSessionCache.assertInSession(this);
        DeepClone cloner = new DeepClone(this, writableSessionCache, sourceNode, sourceCache, systemWorkspaceKey, connectors);
        cloner.execute();
    }

    @Override
    public Set<NodeKey> removedChildren() {
        return changedChildren().getRemovals();
    }

    @Override
    public Set<NodeKey> getChangedReferrerNodes() {
        Set<NodeKey> result = new HashSet<NodeKey>();
        ReferrerChanges referrerChanges = getReferrerChanges();
        if (referrerChanges == null) return Collections.emptySet();
        result.addAll(referrerChanges.getAddedReferrers(ReferenceType.BOTH));
        result.addAll(referrerChanges.getRemovedReferrers(ReferenceType.BOTH));
        return result;
    }

    @Override
    public void addFederatedSegment( String externalNodeKey,
                                     String segmentName ) {
        if (federatedSegments.get() == null) {
            federatedSegments.compareAndSet(null, new FederatedSegmentChanges());
        }
        this.federatedSegments.get().addSegment(externalNodeKey, segmentName);
    }

    protected Map<String, String> getAddedFederatedSegments() {
        return this.federatedSegments.get() != null ? this.federatedSegments.get().getAdditions() : Collections.<String, String>emptyMap();
    }

    @Override
    public void removeFederatedSegment( String externalNodeKey ) {
        if (federatedSegments.get() == null) {
            federatedSegments.compareAndSet(null, new FederatedSegmentChanges());
        }
        this.federatedSegments.get().removeSegment(externalNodeKey);
    }

    protected Set<String> getRemovedFederatedSegments() {
        return this.federatedSegments.get() != null ? this.federatedSegments.get().getRemovals() : Collections.<String>emptySet();
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public NodeChanges getNodeChanges() {
        return new NodeChanges();
    }

    @Override
    public boolean isExcludedFromSearch( NodeCache cache ) {
        Boolean isExcludedFromSearch = this.excludeFromSearch.get();
        if (isExcludedFromSearch != null) {
            return isExcludedFromSearch;
        }
        CachedNode persistedNode = nodeInWorkspace(session(cache));
        // if the node does not exist yet, it is queryable by default
        return persistedNode != null && persistedNode.isExcludedFromSearch(cache);
    }

    public void excludeFromSearch() {
        this.excludeFromSearch.set(Boolean.TRUE);
    }

    @Override
    public boolean hasACL( NodeCache cache ) {
        return getChildReferences(cache).getChild(ModeShapeLexicon.ACCESS_LIST_NODE_NAME) != null;
    }

    @Override
    public Map<String, Set<String>> getPermissions( NodeCache cache ) {
        if (!hasACL(cache)) {
            return null;
        }
        ChildReference aclNodeReference = getChildReferences(cache).getChild(ModeShapeLexicon.ACCESS_LIST_NODE_NAME);
        if (aclNodeReference  == null) {
            return null;
        }
        CachedNode aclNode = cache.getNode(aclNodeReference);
        if (aclNode == null) {
            return null;
        }
        Map<String, Set<String>> result = new HashMap<>();
        ChildReferences permissionsReference = aclNode.getChildReferences(cache);
        for (ChildReference permissionReference : permissionsReference) {
            CachedNode permission = cache.getNode(permissionReference);
            String name = permission.getProperty(ModeShapeLexicon.PERMISSION_PRINCIPAL_NAME, cache).getFirstValue().toString();
            Property privileges = permission.getProperty(ModeShapeLexicon.PERMISSION_PRIVILEGES_NAME, cache);
            Set<String> privilegeNames = new HashSet<>();
            for (Object privilege : privileges.getValuesAsArray()) {
                privilegeNames.add(privilege.toString());
            }
            result.put(name, privilegeNames);
        }
        return result;
    }

    @Override
    public PermissionChanges setPermissions(SessionCache cache, Map<String, Set<String>> privilegesByPrincipalName ) {
        assert privilegesByPrincipalName != null;
        if (this.isExternal(cache)) {
            throw new UnsupportedOperationException(JcrI18n.aclsOnExternalNodesNotAllowed.text());
        }
        if (!this.getMixinTypes(cache).contains(ModeShapeLexicon.ACCESS_CONTROLLABLE)) {
            addMixin(cache, ModeShapeLexicon.ACCESS_CONTROLLABLE);
        }
        ChildReference aclNodeRef = getChildReferences(cache).getChild(ModeShapeLexicon.ACCESS_LIST_NODE_NAME);
        MutableCachedNode aclNode = null;
        PropertyFactory propertyFactory = cache.getContext().getPropertyFactory();
        ChildReferences permissionsReferences = null;
        if (aclNodeRef != null) {
            aclNode = cache.mutable(aclNodeRef.getKey());
            permissionsReferences = aclNode.getChildReferences(cache);
            //there was a previous ACL node present so iterate it and remove all permissions which are not found in the map
            for (ChildReference permissionRef : permissionsReferences) {
                CachedNode permissionNode = cache.getNode(permissionRef);
                String principalName = permissionNode.getProperty(ModeShapeLexicon.PERMISSION_PRINCIPAL_NAME, cache)
                                                     .getFirstValue().toString();
                if (!privilegesByPrincipalName.containsKey(principalName)) {
                    permissionChanges().principalRemoved(principalName);
                    NodeKey permissionNodeKey = permissionNode.getKey();
                    aclNode.removeChild(cache, permissionNodeKey);
                    cache.destroy(permissionNodeKey);
                }
            }
        } else {
            org.modeshape.jcr.value.Property primaryType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE,
                                                                                  ModeShapeLexicon.ACCESS_LIST_NODE_TYPE);
            aclNode = this.createChild(cache, null, ModeShapeLexicon.ACCESS_LIST_NODE_NAME, primaryType);
            permissionsReferences = ImmutableChildReferences.EmptyChildReferences.INSTANCE;
        }

        //go through the new map of permissions and update/create the internal nodes
        NameFactory nameFactory = cache.getContext().getValueFactories().getNameFactory();

        for (String principal : privilegesByPrincipalName.keySet()) {
            Name principalName = nameFactory.create(principal);
            ChildReference permissionRef = permissionsReferences.getChild(principalName);
            if (permissionRef == null) {
                //this is a new principal
                permissionChanges().principalAdded(principal);
                org.modeshape.jcr.value.Property primaryType = propertyFactory.create(
                        JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.PERMISSION);
                Property principalProp = propertyFactory.create(ModeShapeLexicon.PERMISSION_PRINCIPAL_NAME,
                                                                    principal);
                Property privileges = propertyFactory.create(ModeShapeLexicon.PERMISSION_PRIVILEGES_NAME,
                                                             privilegesByPrincipalName.get(principal));
                aclNode.createChild(cache, null, principalName, primaryType, principalProp, privileges);
            } else {
                //there already is a child node for this principal, so we just need to update its privileges
                MutableCachedNode permissionNode = cache.mutable(permissionRef.getKey());
                Property privileges = propertyFactory.create(ModeShapeLexicon.PERMISSION_PRIVILEGES_NAME,
                                                             privilegesByPrincipalName.get(principal));
                permissionNode.setProperty(cache, privileges);
            }
        }
        return permissionChanges();
    }

    protected PermissionChanges permissionChanges() {
        if (permissionChanges.get() == null) {
            permissionChanges.compareAndSet(null, new PermissionChanges());
        }
        return permissionChanges.get();
    }

    @Override
    public MutableCachedNode.PermissionChanges removeACL( SessionCache cache ) {
        if (this.isExternal(cache)) {
            throw new UnsupportedOperationException(JcrI18n.aclsOnExternalNodesNotAllowed.text());
        }
        if (hasACL(cache)) {
            NodeKey aclNodeKey = getChildReferences(cache).getChild(ModeShapeLexicon.ACCESS_LIST_NODE_NAME).getKey();
            MutableCachedNode mutableACLNode = cache.mutable(aclNodeKey);
            for (ChildReference permissionRef : mutableACLNode.getChildReferences(cache)) {
                permissionChanges().principalRemoved(permissionRef.getName().getString());
            }
            if (!cache.isDestroyed(aclNodeKey)) {
                this.removeChild(cache, aclNodeKey);
                cache.destroy(aclNodeKey);
            }
            removeMixin(cache, ModeShapeLexicon.ACCESS_CONTROLLABLE);
        }
        return permissionChanges();
    }

    @Override
    public boolean isExternal( NodeCache cache ) {
        return !getKey().getSourceKey().equals(cache.getRootKey().getSourceKey());
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
    public void addInternalProperty( String name, Object value ) {
        removedInternalProperties.remove(name);
        addedInternalProperties.putIfAbsent(name, value);
    }

    @Override
    public Map<String, Object> getAddedInternalProperties() {
        return addedInternalProperties;
    }

    @Override
    public boolean removeInternalProperty( String name ) {
        addedInternalProperties.remove(name);
        return removedInternalProperties.add(name);
    }

    @Override
    public Set<String> getRemovedInternalProperties() {
        return removedInternalProperties;
    }

    @Override
    public String toString() {
        return getString(null);
    }

    public String getString( NamespaceRegistry registry ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Node '").append(key).append("' ->");
        NodeKey newParent = this.newParent;
        if (isNew) {
            if (newParent != null) {
                sb.append(" created under '").append(newParent).append('\'');
            } else {
                sb.append(" created; ");
            }
        } else {
            if (newParent != null) {
                sb.append(" moved to '").append(newParent).append('\'');
            }
        }
        ChangedAdditionalParents additionalParents = this.additionalParents();
        if (additionalParents != null) {
            sb.append(" parents: [");
            if (!additionalParents.getAdditions().isEmpty()) {
                sb.append("+").append(additionalParents.getAdditions());
            }
            if (!additionalParents.getRemovals().isEmpty()) {
                sb.append("-").append(additionalParents.getRemovals());
            }
            sb.append(']');
        }
        boolean changedProps = !changedProperties.isEmpty();
        boolean removedProps = !removedProperties.isEmpty();
        if (changedProps || removedProps) {
            sb.append(" props: {");
            if (changedProps) {
                boolean first = true;
                for (Map.Entry<Name, Property> entry : changedProperties.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(',');
                    }
                    Property property = entry.getValue();
                    sb.append(" +").append(property.getString(registry));
                }
            }
            if (removedProps) {
                boolean first = true;
                for (Name name : removedProperties.keySet()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(',');
                    }
                    sb.append(" -").append(name.getString(registry));
                }
            }
            sb.append('}');
        }
        boolean addInternalProps = !addedInternalProperties.isEmpty();
        boolean removedInternalProps = !removedInternalProperties.isEmpty();
        if (addInternalProps || removedInternalProps) {
            sb.append(" internal props: {");
            if (addInternalProps) {
                boolean first = true;
                for (Map.Entry<String, Object> entry : addedInternalProperties.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(',');
                    }
                    sb.append(" +").append(entry.getKey()).append("=").append(entry.getValue());
                }
            }
            if (removedInternalProps) {
                boolean first = true;
                for (String name : removedInternalProperties) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(',');
                    }
                    sb.append(" -").append(name);
                }
            }
            sb.append('}');
        }
        MutableChildReferences appended = appended(false);
        if (!changedChildren.isEmpty() || (appended != null && !appended.isEmpty())) {
            sb.append(" children: ");
            if (!changedChildren.isEmpty()) {
                changedChildren.getString(sb);
                sb.append(' ');
            }
            if (appended != null && !appended.isEmpty()) {
                sb.append("appended [");
                Iterator<ChildReference> iter = appended.iterator();
                if (iter.hasNext()) {
                    sb.append(iter.next().toString(registry));
                    while (iter.hasNext()) {
                        sb.append(',');
                        sb.append(iter.next().toString(registry));
                    }
                }
                sb.append(']');
            }
        }
        ReferrerChanges referrerChg = getReferrerChanges();
        if (referrerChg != null && !referrerChg.isEmpty()) {
            sb.append(' ');
            referrerChg.getString(sb);
        }
        return sb.toString();
    }

    /**
     * Value object which contains an "abbreviated" view of the changes that this session node has registered in its internal
     * state.
     */
    private class NodeChanges implements MutableCachedNode.NodeChanges {
        private NodeChanges() {
            // this is not mean to be created from the outside
        }

        @Override
        public Set<Name> changedPropertyNames() {
            Set<Name> result = new HashSet<Name>();
            result.addAll(changedProperties().keySet());
            return result;
        }

        @Override
        public Set<Name> removedPropertyNames() {
            return new HashSet<Name>(removedProperties());
        }

        @Override
        public Set<Name> addedMixins() {
            Set<Name> result = new HashSet<Name>();
            MixinChanges mixinChanges = mixinChanges(false);
            if (mixinChanges != null) {
                result.addAll(mixinChanges.getAdded());
            }
            return result;
        }

        @Override
        public Set<Name> removedMixins() {
            Set<Name> result = new HashSet<Name>();
            MixinChanges mixinChanges = mixinChanges(false);
            if (mixinChanges != null) {
                result.addAll(mixinChanges.getRemoved());
            }
            return result;
        }

        @Override
        public LinkedHashMap<NodeKey, Name> appendedChildren() {
            LinkedHashMap<NodeKey, Name> result = new LinkedHashMap<NodeKey, Name>();
            MutableChildReferences appendedChildReferences = appended(false);
            if (appendedChildReferences != null) {
                for (ChildReference appendedChildReference : appendedChildReferences) {
                    result.put(appendedChildReference.getKey(), appendedChildReference.getName());
                }
            }
            return result;
        }

        @Override
        public Set<NodeKey> removedChildren() {
            Set<NodeKey> result = new HashSet<NodeKey>();
            result.addAll(changedChildren().getRemovals());
            return result;
        }

        @Override
        public Map<NodeKey, Name> renamedChildren() {
            Map<NodeKey, Name> result = new HashMap<NodeKey, Name>();
            result.putAll(changedChildren().getNewNames());
            return result;
        }

        @Override
        public Map<NodeKey, LinkedHashMap<NodeKey, Name>> childrenInsertedBefore() {
            Map<NodeKey, LinkedHashMap<NodeKey, Name>> result = new HashMap<NodeKey, LinkedHashMap<NodeKey, Name>>();

            Map<NodeKey, Insertions> insertionsByBeforeKey = changedChildren().getInsertionsByBeforeKey();
            for (NodeKey beforeNodeKey : insertionsByBeforeKey.keySet()) {
                Insertions insertionsBefore = insertionsByBeforeKey.get(beforeNodeKey);
                if (insertionsBefore != null) {
                    LinkedHashMap<NodeKey, Name> insertionsBeforeMap = new LinkedHashMap<NodeKey, Name>();
                    for (ChildReference childReference : insertionsBefore.inserted()) {
                        insertionsBeforeMap.put(childReference.getKey(), childReference.getName());
                    }
                    result.put(beforeNodeKey, insertionsBeforeMap);
                }
            }
            return result;
        }

        @Override
        public Set<NodeKey> addedParents() {
            Set<NodeKey> result = new HashSet<NodeKey>();
            if (additionalParents() != null) {
                result.addAll(additionalParents().getAdditions());
            }
            return result;
        }

        @Override
        public Set<NodeKey> removedParents() {
            Set<NodeKey> result = new HashSet<NodeKey>();
            if (additionalParents() != null) {
                result.addAll(additionalParents().getRemovals());
            }
            return result;
        }

        @Override
        public NodeKey newPrimaryParent() {
            return newParent();
        }

        @Override
        public Set<NodeKey> addedWeakReferrers() {
            Set<NodeKey> result = new HashSet<NodeKey>();
            ReferrerChanges referrerChanges = referrerChanges(false);
            if (referrerChanges != null) {
                result.addAll(referrerChanges.getAddedReferrers(ReferenceType.WEAK));
            }
            return result;
        }

        @Override
        public Set<NodeKey> removedWeakReferrers() {
            Set<NodeKey> result = new HashSet<NodeKey>();
            ReferrerChanges referrerChanges = referrerChanges(false);
            if (referrerChanges != null) {
                result.addAll(referrerChanges.getRemovedReferrers(ReferenceType.WEAK));
            }
            return result;
        }

        @Override
        public Set<NodeKey> addedStrongReferrers() {
            Set<NodeKey> result = new HashSet<NodeKey>();
            ReferrerChanges referrerChanges = referrerChanges(false);
            if (referrerChanges != null) {
                result.addAll(referrerChanges.getAddedReferrers(ReferenceType.STRONG));
            }
            return result;
        }

        @Override
        public Set<NodeKey> removedStrongReferrers() {
            Set<NodeKey> result = new HashSet<NodeKey>();
            ReferrerChanges referrerChanges = referrerChanges(false);
            if (referrerChanges != null) {
                result.addAll(referrerChanges.getRemovedReferrers(ReferenceType.STRONG));
            }
            return result;
        }
    }

    @ThreadSafe
    protected static class ChangedAdditionalParents {
        private final Set<NodeKey> removals = new CopyOnWriteArraySet<NodeKey>();
        private final Set<NodeKey> additions = new CopyOnWriteArraySet<NodeKey>();

        public boolean isEmpty() {
            return additionCount() == 0 && removalCount() == 0;
        }

        public int additionCount() {
            return additions.size();
        }

        public int removalCount() {
            return removals.size();
        }

        public boolean remove( NodeKey key ) {
            return additions.remove(key) || removals.add(key);
        }

        public boolean add( NodeKey key ) {
            return removals.remove(key) || additions.add(key);
        }

        /**
         * @return additions
         */
        public Set<NodeKey> getAdditions() {
            return additions;
        }

        /**
         * @return removals
         */
        public Set<NodeKey> getRemovals() {
            return removals;
        }
    }

    /**
     * The representation of the changes made to the child references of this node.
     */
    @ThreadSafe
    protected static class ChangedChildren implements ChildReferences.Changes {
        private final AtomicReference<Set<NodeKey>> removals = new AtomicReference<Set<NodeKey>>();
        private final AtomicReference<InsertedChildReferences> insertions = new AtomicReference<InsertedChildReferences>();
        private final AtomicReference<Map<NodeKey, Name>> newNames = new AtomicReference<Map<NodeKey, Name>>();

        @Override
        public boolean isEmpty() {
            return insertionCount() == 0 && removalCount() == 0 && renameCount() == 0;
        }

        @Override
        public int insertionCount() {
            InsertedChildReferences insertions = this.insertions.get();
            return insertions == null ? 0 : insertions.size();
        }

        @Override
        public int insertionCount( Name name ) {
            InsertedChildReferences insertions = this.insertions.get();
            return insertions == null ? 0 : insertions.size(name);
        }

        @Override
        public int removalCount() {
            Set<NodeKey> removals = this.removals.get();
            return removals == null ? 0 : removals.size();
        }

        @Override
        public int renameCount() {
            Map<NodeKey, Name> newNames = this.newNames.get();
            return newNames == null ? 0 : newNames.size();
        }

        @Override
        public ChildReference inserted( NodeKey key ) {
            InsertedChildReferences insertions = this.insertions.get();
            return insertions == null ? null : insertions.inserted(key);
        }

        @Override
        public Iterator<ChildInsertions> insertions( Name name ) {
            InsertedChildReferences insertions = this.insertions.get();
            return insertions == null ? null : insertions.insertions(name);
        }

        @Override
        public ChildInsertions insertionsBefore( ChildReference key ) {
            InsertedChildReferences insertions = this.insertions.get();
            return insertions == null ? null : insertions.insertionsBefore(key.getKey());
        }

        /**
         * Insert the supplied child reference before another child reference.
         * 
         * @param before the existing child reference before which the child reference is to be placed; may not be null
         * @param inserted the reference to the child that is to be inserted; may not be null
         */
        public void insertBefore( ChildReference before,
                                  ChildReference inserted ) {
            if (before == inserted) {
                return;
            }
            // Get or create atomically ...
            InsertedChildReferences insertions = this.insertions.get();
            if (insertions == null) {
                insertions = new InsertedChildReferences();
                if (!this.insertions.compareAndSet(null, insertions)) {
                    insertions = this.insertions.get();
                }
            }
            insertions.insertBefore(before, inserted);
        }

        @Override
        public boolean isRemoved( ChildReference ref ) {
            Set<NodeKey> removals = this.removals.get();
            return removals != null && removals.contains(ref.getKey());
        }

        @Override
        public boolean isRenamed( ChildReference ref ) {
            Map<NodeKey, Name> renames = this.newNames.get();
            return renames != null && renames.containsKey(ref.getKey());
        }

        @Override
        public boolean isRenamed( Name newName ) {
            Map<NodeKey, Name> renames = this.newNames.get();
            return renames != null && renames.containsValue(newName);
        }

        /**
         * Remove the supplied node from this node's list of child references.
         * 
         * @param key the key for the node that is to be removed; may not be null
         * @return true if the node was removed, or false if the node was not referenced as a child
         */
        public boolean remove( NodeKey key ) {
            InsertedChildReferences insertions = this.insertions.get();
            if (insertions != null && insertions.remove(key)) {
                // This was inserted, so make sure it's not in the removed ...
                Set<NodeKey> removals = this.removals.get();
                assert removals == null || !removals.contains(key);
                return true;
            }

            // Wasn't inserted, so mark it as removed ...

            // Get or create atomically ...
            Set<NodeKey> removals = this.removals.get();
            if (removals == null) {
                removals = new CopyOnWriteArraySet<NodeKey>();
                if (!this.removals.compareAndSet(null, removals)) {
                    removals = this.removals.get();
                }
            }
            removals.add(key);
            return true;
        }

        @Override
        public Name renamed( NodeKey key ) {
            Map<NodeKey, Name> newNames = this.newNames.get();
            return newNames == null ? null : newNames.get(key);
        }

        /**
         * Rename the child reference with the given key.
         * 
         * @param key the key for the child reference; may not be null
         * @param newName the new name for the node; may not be null
         */
        public void renameTo( NodeKey key,
                              Name newName ) {
            Map<NodeKey, Name> newNames = this.newNames.get();
            if (newNames == null) {
                newNames = new ConcurrentHashMap<NodeKey, Name>();
                if (!this.newNames.compareAndSet(null, newNames)) {
                    newNames = this.newNames.get();
                }
            }
            newNames.put(key, newName);
        }

        @Override
        public String toString() {
            return getString(new StringBuilder()).toString();
        }

        public StringBuilder getString( StringBuilder sb ) {
            InsertedChildReferences insertions = this.insertions.get();
            Set<NodeKey> removals = this.removals.get();
            Map<NodeKey, Name> renames = this.newNames.get();
            if (insertions != null) {
                insertions.toString(sb);
                if (removals != null && !removals.isEmpty()) sb.append("; ");
            }
            if (removals != null && !removals.isEmpty()) {
                sb.append("removals: ").append(removals);
                if (renames != null && !renames.isEmpty()) {
                    sb.append("; ");
                }
            }
            if (renames != null && !renames.isEmpty()) {
                sb.append("renames: ").append(renames);
            }
            return sb;
        }

        /**
         * Get the names for the new nodes, keyed by their key.
         * 
         * @return the map of node names for the new nodes; never null but possibly empty
         */
        public Map<NodeKey, Name> getNewNames() {
            Map<NodeKey, Name> newNames = this.newNames.get();
            return newNames == null ? Collections.<NodeKey, Name>emptyMap() : newNames;
        }

        /**
         * Get the set of keys for the nodes that were removed from the list of child references.
         * 
         * @return the keys for the removed nodes; never null but possibly empty
         */
        public Set<NodeKey> getRemovals() {
            Set<NodeKey> removals = this.removals.get();
            return removals == null ? Collections.<NodeKey>emptySet() : new HashSet<NodeKey>(removals);
        }

        /**
         * Get the map of insertions keyed by the 'before' node
         * 
         * @return the map of insertions; never null but possibly empty
         */
        public Map<NodeKey, Insertions> getInsertionsByBeforeKey() {
            InsertedChildReferences insertedRefs = insertions.get();
            if (insertedRefs == null || insertedRefs.size() == 0) return Collections.emptyMap();

            Map<NodeKey, Insertions> result = new HashMap<NodeKey, Insertions>();
            for (Insertions insertions : insertedRefs) {
                result.put(insertions.insertedBefore().getKey(), insertions);
            }
            return result;
        }
    }

    @ThreadSafe
    protected static class FederatedSegmentChanges {
        private final ConcurrentHashMap<String, String> additions = new ConcurrentHashMap<String, String>();
        private final Set<String> removals = Collections.synchronizedSet(new HashSet<String>());

        protected void addSegment( String externalNodeKey,
                                   String name ) {
            additions.putIfAbsent(externalNodeKey, name);
        }

        protected void removeSegment( String externalNodeKey ) {
            removals.add(externalNodeKey);
        }

        protected Map<String, String> getAdditions() {
            return Collections.unmodifiableMap(additions);
        }

        protected Set<String> getRemovals() {
            return Collections.unmodifiableSet(removals);
        }
    }

    @ThreadSafe
    protected static class InsertedChildReferences implements Iterable<Insertions> {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final Set<NodeKey> inserted = new HashSet<NodeKey>();
        private final Map<Name, AtomicInteger> insertedNames = new HashMap<Name, AtomicInteger>();
        private final ConcurrentMap<NodeKey, Insertions> insertedBefore = new ConcurrentHashMap<NodeKey, SessionNode.Insertions>();

        /**
         * Get the number of inserted child references.
         * 
         * @return the number of inserted references; never negative
         */
        private int size() {
            return inserted.size();
        }

        @Override
        public Iterator<Insertions> iterator() {
            return insertedBefore.values().iterator();
        }

        /**
         * Get the child reference for the inserted node with the supplied key.
         * 
         * @param key the key for the inserted child reference
         * @return the child reference; may be null if the node was not inserted
         */
        private ChildReference inserted( NodeKey key ) {
            Lock lock = this.lock.readLock();
            try {
                lock.lock();
                if (!inserted.contains(key)) {
                    // This needs to be fast because it is called frequently ...
                    return null;
                }
                // Otherwise, we have to find the reference, but this can be slower because it is rare that there are insertions
                // ...
                for (Insertions insertions : insertedBefore.values()) {
                    for (ChildReference inserted : insertions.inserted()) {
                        if (inserted.getKey().equals(key)) return inserted;
                    }
                }
                return null;
            } finally {
                lock.unlock();
            }

        }

        private Iterator<ChildInsertions> insertions( Name name ) {
            Lock lock = this.lock.readLock();
            try {
                lock.lock();
                if (!insertedNames.containsKey(name)) {
                    // This needs to be fast because it is called frequently ...
                    return null;
                }
                // Otherwise, we have to find the reference, but this can be slower because it is rare that there are insertions
                // ...
                List<ChildInsertions> namedInsertions = new LinkedList<ChildInsertions>();
                for (Insertions insertions : insertedBefore.values()) {
                    Insertions byName = null;
                    for (ChildReference inserted : insertions.inserted()) {
                        if (inserted.getName().equals(name)) {
                            if (byName == null) {
                                byName = new Insertions(insertions.insertedBefore(), inserted);
                            } else {
                                byName.add(inserted);
                            }
                        }
                    }
                    if (byName != null) namedInsertions.add(byName);
                }
                return namedInsertions.iterator();
            } finally {
                lock.unlock();
            }
        }

        private ChildInsertions insertionsBefore( NodeKey key ) {
            // This is safe to return without locking (that's why we used a ConcurrentMap) ...
            return insertedBefore.get(key);
        }

        private void insertBefore( ChildReference before,
                                   ChildReference inserted ) {
            Lock lock = this.lock.writeLock();
            try {
                lock.lock();
                inserted = inserted.with(1);

                // Add to the key ...
                Insertions insertions = insertedBefore.get(before.getKey());
                if (insertions == null) {
                    insertions = new Insertions(before, inserted);
                    insertedBefore.put(before.getKey(), insertions); // locked, so no need to 'putIfAbsent'
                } else {
                    insertions.add(inserted);
                }

                // Add to the names and keys ...
                AtomicInteger count = this.insertedNames.get(inserted.getName());
                if (count == null) {
                    this.insertedNames.put(inserted.getName(), new AtomicInteger(1));
                } else {
                    count.incrementAndGet();
                }
                boolean added = this.inserted.add(inserted.getKey());
                assert added;
            } finally {
                lock.unlock();
            }
        }

        private boolean remove( NodeKey key ) {
            Lock lock = this.lock.writeLock();
            try {
                lock.lock();
                ChildReference removed = null;
                if (this.inserted.remove(key)) {
                    for (Insertions insertions : insertedBefore.values()) {
                        removed = insertions.remove(key);
                        if (removed != null) {
                            insertedBefore.remove(insertions.insertedBefore(), new Insertions(insertions.insertedBefore()));
                            break;
                        }
                    }
                    if (removed != null) {
                        Name name = removed.getName();
                        AtomicInteger count = this.insertedNames.get(name);
                        if (count != null) {
                            if (count.decrementAndGet() == 0) {
                                this.insertedNames.remove(name);
                            }
                        }
                    }
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }
        
        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }

        protected StringBuilder toString( StringBuilder sb ) {
            int number = size();
            sb.append(number).append(' ').append(Inflector.getInstance().pluralize("insertion", number)).append(": ");
            Iterator<Insertions> iter = iterator();
            if (iter.hasNext()) {
                sb.append(iter.next());
                while (iter.hasNext()) {
                    sb.append(", ");
                    sb.append(iter.next());
                }
            }
            return sb;
        }

        protected int size( Name name ) {
            lock.readLock().lock();
            try {
                AtomicInteger count = insertedNames.get(name);
                return count != null ? count.get() : 0;
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    protected static class Insertions implements ChildReferences.ChildInsertions {
        private final List<ChildReference> inserted = new CopyOnWriteArrayList<ChildReference>();
        private final ChildReference before;

        protected Insertions( ChildReference before ) {
            this.before = before;
        }

        protected Insertions( ChildReference before,
                              ChildReference inserted ) {
            this.before = before;
            this.inserted.add(inserted);
        }

        @Override
        public Iterable<ChildReference> inserted() {
            return inserted;
        }

        @Override
        public ChildReference insertedBefore() {
            return before;
        }

        public void add( ChildReference reference ) {
            this.inserted.add(reference);
        }

        public boolean contains( ChildReference reference ) {
            return this.inserted.contains(reference);
        }

        public boolean remove( ChildReference reference ) {
            return this.inserted.remove(reference);
        }

        public ChildReference remove( NodeKey key ) {
            for (ChildReference ref : this.inserted) {
                if (ref.getKey().equals(key) && remove(ref)) return ref;
            }
            return null;
        }

        @Override
        public String toString() {
            return inserted + " before " + before;
        }

        @Override
        public int hashCode() {
            return this.before.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Insertions) {
                Insertions that = (Insertions)obj;
                return this.before.equals(that.before) && this.inserted.equals(that.inserted);
            }
            return false;
        }
    }

    protected static class MixinChanges {
        private final Set<Name> added = new HashSet<Name>();
        private final Set<Name> removed = new HashSet<Name>();

        public void add( Name mixin ) {
            this.added.add(mixin);
            this.removed.remove(mixin);
        }

        public void remove( Name mixin ) {
            this.added.remove(mixin);
            this.removed.add(mixin);
        }

        public Set<Name> getAdded() {
            return added;
        }

        public Set<Name> getRemoved() {
            return removed;
        }

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }

        @Override
        public String toString() {
            return "added: " + added + ", removed: " + removed;
        }
    }

    protected static class ReferrerChanges {
        // we need to be able to have multiple references from the same referrer and also multiple references from the same
        // property of different referrers
        private final Map<String, Set<NodeKey>> addedWeak = new HashMap<>();
        private final Map<String, Set<NodeKey>> removedWeak = new HashMap<>();
        private final Map<String, Set<NodeKey>> addedStrong = new HashMap<>();
        private final Map<String, Set<NodeKey>> removedStrong = new HashMap<>();

        public void addWeakReferrer( Property referenceProperty,
                                     NodeKey referrerKey ) {
            processReferrerChange(referenceProperty, referrerKey, addedWeak, removedWeak);
        }

        public void removeWeakReferrer( Property referenceProperty,
                                        NodeKey referrerKey ) {
            processReferrerChange(referenceProperty, referrerKey, removedWeak, addedWeak);
        }

        public void addStrongReferrer( Property referenceProperty,
                                       NodeKey referrerKey ) {
            processReferrerChange(referenceProperty, referrerKey, addedStrong, removedStrong);
        }

        public void removeStrongReferrer( Property referenceProperty,
                                          NodeKey referrerKey ) {
            processReferrerChange(referenceProperty, referrerKey, removedStrong, addedStrong);
        }

        private void processReferrerChange( Property referenceProperty,
                                            NodeKey referrerKey,
                                            Map<String, Set<NodeKey>> addToMap,
                                            Map<String, Set<NodeKey>> removeFromMap ) {
            String propertyKey = keyFromProperty(referenceProperty);

            boolean shouldAdd = true;
            Set<NodeKey> toRemove = removeFromMap.get(propertyKey);
            if (toRemove != null) {
                shouldAdd = !toRemove.remove(referrerKey);
                if (toRemove.isEmpty()) {
                    removeFromMap.remove(propertyKey);
                }
            }
            if (!shouldAdd) {
                // we're trying to add the same referrer that has already been removed, so this should be a no-op for that referrer
                // i.e. both maps should not contain that referrer
                return;
            }
            Set<NodeKey> toAdd = addToMap.get(propertyKey);
            if (toAdd == null) {
                toAdd = new HashSet<>();
                addToMap.put(propertyKey, toAdd);
            }
            toAdd.add(referrerKey);

        }

        private String keyFromProperty( Property property ) {
            StringBuilder key = new StringBuilder(property.getName().getString()).append("_");
            if (property.isSingle()) {
                key.append("_sv");
            } else {
                key.append("_mv");
            }
            return key.toString();
        }

        public List<NodeKey> getAddedReferrers( ReferenceType type ) {
            switch (type) {
                case STRONG:
                    return collectKeys(addedStrong);
                case WEAK:
                    return collectKeys(addedWeak);
                case BOTH:
                    return collectKeys(addedWeak, addedStrong);
            }
            assert false : "Should never get here";
            return null;
        }

        public boolean isRemovedReferrer( NodeKey key,
                                          ReferenceType type ) {
            switch (type) {
                case STRONG:
                    return containsKey(key, removedStrong);
                case WEAK:
                    return containsKey(key, removedWeak);
                case BOTH:
                    return containsKey(key, removedStrong) || containsKey(key, removedWeak);
            }
            return false;
        }

        public List<NodeKey> getRemovedReferrers( ReferenceType type ) {
            switch (type) {
                case STRONG:
                    return collectKeys(removedStrong);
                case WEAK:
                    return collectKeys(removedWeak);
                case BOTH:
                    return collectKeys(removedStrong, removedWeak);
            }
            assert false : "Should never get here";
            return null;
        }

        private final boolean containsKey( NodeKey key,
                                           Map<String, Set<NodeKey>> source ) {
            for (Set<NodeKey> sourceKeys : source.values()) {
                if (sourceKeys.contains(key)) return true;
            }
            return false;
        }

        @SafeVarargs
        private final List<NodeKey> collectKeys( Map<String, Set<NodeKey>>... sources ) {
            List<NodeKey> keys = new ArrayList<>();
            for (Map<String, Set<NodeKey>> source : sources) {
                for (Set<NodeKey> sourceKeys : source.values()) {
                    keys.addAll(sourceKeys);
                }
            }
            return keys;
        }

        public ReferrerCounts getReferrerCounts( ReferrerCounts persisted ) {
            MutableReferrerCounts mutable = persisted != null ? persisted.mutable() : ReferrerCounts.createMutable();
            for (Set<NodeKey> sourceKeys : addedStrong.values()) {
                for (NodeKey key : sourceKeys) {
                    mutable.addStrong(key, 1);
                }
            }
            for (Set<NodeKey> sourceKeys : addedWeak.values()) {
                for (NodeKey key : sourceKeys) {
                    mutable.addWeak(key, 1);
                }
            }
            for (Set<NodeKey> sourceKeys : removedStrong.values()) {
                for (NodeKey key : sourceKeys) {
                    mutable.addStrong(key, -1);
                }
            }
            for (Set<NodeKey> sourceKeys : removedWeak.values()) {
                for (NodeKey key : sourceKeys) {
                    mutable.addWeak(key, -1);
                }
            }
            return mutable.freeze();
        }

        public boolean isEmpty() {
            return addedWeak.isEmpty() && removedWeak.isEmpty() && addedStrong.isEmpty() && removedStrong.isEmpty();
        }

        @Override
        public String toString() {
            return getString(new StringBuilder());
        }

        public String getString( StringBuilder sb ) {
            sb.append("ReferrerChanges: ");
            if (!addedStrong.isEmpty()) {
                sb.append(" addedStrong=").append(addedStrong);
            }
            if (!addedWeak.isEmpty()) {
                sb.append(" addedWeak=").append(addedWeak);
            }
            if (!removedWeak.isEmpty()) {
                sb.append(" removedWeak=").append(removedWeak);
            }
            if (!removedStrong.isEmpty()) {
                sb.append(" removedStrong=").append(removedStrong);
            }
            return sb.toString();
        }
    }

    protected static class PermissionChanges implements MutableCachedNode.PermissionChanges {
        private final Set<String> removedPrincipals;
        private final Set<String> addedPrincipals;

        protected PermissionChanges() {
            this.removedPrincipals = new HashSet<>();
            this.addedPrincipals = new HashSet<>();
        }

        protected void principalAdded(String principalName) {
            this.removedPrincipals.remove(principalName);
            this.addedPrincipals.add(principalName);
        }

        protected void principalRemoved(String principalName) {
            this.addedPrincipals.remove(principalName);
            this.removedPrincipals.add(principalName);
        }

        @Override
        public long addedPrincipalsCount() {
            return addedPrincipals.size();
        }

        @Override
        public long removedPrincipalsCount() {
            return removedPrincipals.size();
        }
    }

    protected class DeepCopy {
        protected final WritableSessionCache targetCache;
        protected final SessionNode targetNode;
        protected final SessionCache sourceCache;
        protected final CachedNode sourceNode;
        protected final Path startingPathInSource;
        protected final PropertyFactory propertyFactory;
        protected final String targetWorkspaceKey;
        protected final Map<NodeKey, NodeKey> linkedPlaceholdersToOriginal = new HashMap<NodeKey, NodeKey>();
        protected final Map<NodeKey, NodeKey> sourceToTargetKeys = new HashMap<NodeKey, NodeKey>();
        protected final Map<NodeKey, Set<Property>> sourceKeyToReferenceProperties = new HashMap<NodeKey, Set<Property>>();
        protected final DocumentStore documentStore;
        protected final String systemWorkspaceKey;
        protected final Connectors connectors;
        protected final ValueFactories valueFactories;

        protected DeepCopy( SessionNode targetNode,
                            WritableSessionCache cache,
                            CachedNode sourceNode,
                            SessionCache sourceCache,
                            String systemWorkspaceKey,
                            Connectors connectors ) {
            this.targetCache = cache;
            this.targetNode = targetNode;
            this.sourceCache = sourceCache;
            this.sourceNode = sourceNode;
            this.startingPathInSource = sourceNode.getPath(sourceCache);
            this.propertyFactory = this.targetCache.getContext().getPropertyFactory();
            this.targetWorkspaceKey = targetNode.getKey().getWorkspaceKey();
            this.documentStore = ((WorkspaceCache)sourceCache.getWorkspace()).documentStore();
            this.systemWorkspaceKey = systemWorkspaceKey;
            this.connectors = connectors;
            this.valueFactories = this.targetCache.context().getValueFactories();
        }

        public Map<NodeKey, NodeKey> getSourceToTargetKeys() {
            return sourceToTargetKeys;
        }

        public void execute() {
            doPhase1(this.targetNode, this.sourceNode);
            doPhase2();
            resolveReferences();
        }

        /**
         * Perform a copy of the source tree to create a similar tree in the target session. Note that copying linked nodes varies
         * depending upon where the linked nodes are relative to the source tree.
         * <ol>
         * <li>If the linked nodes and the original node are all in the source tree being copied, then the result of the copy will
         * contain a copy of the original and links to the new copy.</li>
         * <li>If the original node is not within the source tree being copied, then the result of the copy will contain links to
         * the original node.</li>
         * </ol>
         * The result of phase 1 will have either created the links correctly or will have add placeholders in the target tree
         * representing where the linked children should exist. Such placeholders will be handled in phase 2.
         * 
         * @param targetNode the (empty) target node that should be made to look like the supplied source node; may not be null
         * @param sourceNode the original node that should be copied; may not be null
         */
        protected void doPhase1( MutableCachedNode targetNode,
                                 CachedNode sourceNode ) {
            final NodeKey sourceKey = sourceNode.getKey();
            final NodeKey targetKey = targetNode.getKey();
            sourceToTargetKeys.put(sourceKey, targetKey);

            if (shouldProcessSourceKey(sourceKey)) {
                copyProperties(targetNode, sourceNode);
            }

            for (ChildReference sourceChildReference : sourceNode.getChildReferences(sourceCache)) {
                NodeKey childKey = sourceChildReference.getKey();
                if (!shouldProcessSourceKey(childKey)) {
                    continue;
                }
                // We'll need the parent key in the source ...
                CachedNode sourceChild = sourceCache.getNode(childKey);
                NodeKey parentSourceKey = sourceChild.getParentKeyInAnyWorkspace(sourceCache);
                Name sourceChildName = sourceChildReference.getName();
                if (sourceKey.equals(parentSourceKey)) {
                    boolean isExternal = !childKey.getSourceKey().equalsIgnoreCase(sourceCache.getRootKey().getSourceKey());
                    MutableCachedNode childCopy = null;
                    String projectionAlias = sourceChildName.getString();
                    if (isExternal && connectors.hasExternalProjection(projectionAlias, childKey.toString())) {
                        // the child is a projection, so we need to create the projection in the parent
                        targetNode.addFederatedSegment(childKey.toString(), projectionAlias);
                        // since the child is a projection, use the external node key to retrieve the node/document from the
                        // connectors
                        childCopy = targetCache.mutable(childKey);
                    } else {
                        // The child is a normal child of this node ...

                        // check if there is a child with the same segment in the target which was not processed yet
                        ChildReferences targetNodeChildReferences = targetNode.getChildReferences(targetCache);
                        ChildReference targetChildSameSegment = targetNodeChildReferences.getChild(sourceChildReference.getSegment());
                        if (targetChildSameSegment != null && !sourceToTargetKeys.containsValue(targetChildSameSegment.getKey())) {
                            // we found a child of the target node which has the same segment and has not been processed yet
                            // meaning it was present in the target before the deep copy/clone started (e.g. an autocreated node)
                            childCopy = targetCache.mutable(targetChildSameSegment.getKey());
                            if (!isExternal) {
                                // if the child is not external, we should remove it because the new child needs to be identical
                                // to the source child
                                targetNode.removeChild(targetCache, targetChildSameSegment.getKey());
                                targetCache.destroy(targetChildSameSegment.getKey());
                                childCopy = null;
                            }
                        }

                        if (childCopy == null) {
                            // we should create a new child in target with a preferred key (different for copy/clone)
                            String childCopyPreferredKey = documentStore.newDocumentKey(targetKey.toString(), sourceChildName,
                                                                                        sourceChild.getPrimaryType(sourceCache));
                            NodeKey newKey = createTargetKeyFor(childKey, targetKey, childCopyPreferredKey);
                            childCopy = targetNode.createChild(targetCache, newKey, sourceChildName, null);
                        }
                    }
                    doPhase1(childCopy, sourceChild);
                } else {
                    // This child is linked and is not owned. See if the original (the shareable node) is in the source tree ...
                    Path sourceChildPath = sourceChild.getPath(sourceCache);
                    NodeKey newKey = null;
                    if (sourceChildPath.isAtOrBelow(startingPathInSource)) {
                        // It is included in the source tree, so see if a new copy was already made ...
                        newKey = sourceToTargetKeys.get(childKey);
                        if (newKey == null) {
                            // See if we can find the existing node with the existing child key (e.g., system node) ...
                            CachedNode nodeInOtherWorkspace = targetCache.getNode(childKey);
                            if (nodeInOtherWorkspace != null) {
                                // The node must exist in another workspace, so we can just link to it ...
                                newKey = childKey;
                            } else {
                                // The node that we're supposed to link to doesn't yet exist (b/c it will be created
                                // later on in phase 1), so we can't simply create a link but instead need
                                // to create a placeholder (with a new key) ...
                                NodeKey placeholderKey = createTargetKeyFor(childKey, targetKey, null);
                                String childCopyPreferredKey = documentStore.newDocumentKey(targetKey.toString(),
                                                                                            sourceChildName,
                                                                                            sourceChild.getPrimaryType(sourceCache));
                                newKey = createTargetKeyFor(childKey, targetKey, childCopyPreferredKey);
                                sourceToTargetKeys.put(childKey, newKey);
                                targetNode.createChild(targetCache, placeholderKey, sourceChildName, null);
                                linkedPlaceholdersToOriginal.put(placeholderKey, newKey);
                                // we don't want to create a link, so we're done with this child (don't copy properties) ...
                                continue;
                            }
                        }
                    } else {
                        newKey = sourceChildReference.getKey();
                    }
                    // The equivalent node already exists, so we can just link to it ...
                    targetNode.linkChild(targetCache, newKey, sourceChildName);
                }
            }
        }

        protected NodeKey createTargetKeyFor( NodeKey sourceKey,
                                              NodeKey parentKeyInTarget,
                                              String preferredKey ) {
            NodeKey newKey = sourceToTargetKeys.get(sourceKey);
            if (newKey != null) {
                return newKey;
            } else if (!StringUtil.isBlank(preferredKey)) {
                return new NodeKey(preferredKey);
            } else {
                return parentKeyInTarget.withRandomId();
            }
        }

        /**
         * This leaves any linked placeholders that failed in the map.
         */
        protected void doPhase2() {
            RuntimeException firstException = null;
            Iterator<Map.Entry<NodeKey, NodeKey>> entryIterator = linkedPlaceholdersToOriginal.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<NodeKey, NodeKey> entry = entryIterator.next();
                try {
                    NodeKey placeholderKey = entry.getKey();
                    NodeKey linkableKey = entry.getValue();
                    // Find the placeholder in the target ...
                    CachedNode placeholder = targetCache.getNode(placeholderKey);
                    // Get the parent and the child reference ...
                    NodeKey parentKey = placeholder.getParentKey(targetCache);
                    MutableCachedNode parent = targetCache.mutable(parentKey);

                    // Add a link at the end ...
                    if (parent.linkChild(targetCache, linkableKey, placeholder.getName(targetCache))) {
                        // Move the link (there can only be one per parent) before the placeholder ...
                        parent.reorderChild(targetCache, linkableKey, placeholderKey);
                    }

                    // And finally remove the placeholder ...
                    parent.removeChild(targetCache, placeholderKey);

                    // Remove the entry ...
                    entryIterator.remove();
                } catch (RuntimeException e) {
                    // Record the problem and continue to try and copy everything else ...
                    if (firstException == null) firstException = e;
                }
            }
            if (firstException != null) {
                throw firstException;
            }
        }

        /**
         * After the entire graph of nodes has been copied, look at each of the target nodes for reference properties pointing
         * towards nodes in the source graph that now have equivalent nodes in the target graph. If that is the case, update those
         * reference properties.
         */
        protected void resolveReferences() {
            for (Map.Entry<NodeKey, NodeKey> entry : sourceToTargetKeys.entrySet()) {
                NodeKey sourceKey = entry.getKey();
                NodeKey targetKey = entry.getValue();

                Set<Property> referenceProperties = sourceKeyToReferenceProperties.get(sourceKey);
                if (referenceProperties == null) {
                    continue;
                }
                MutableCachedNode targetNode = targetCache.mutable(targetKey);
                for (Property property : referenceProperties) {
                    List<Reference> resolvedReferences = new ArrayList<Reference>();
                    for (Iterator<?> valuesIterator = property.getValues(); valuesIterator.hasNext();) {
                        Reference reference = (Reference)valuesIterator.next();
                        resolvedReferences.add(resolveReference(sourceKey, targetKey, property.getName(), reference));
                    }
                    Property updatedProperty = property.isMultiple() ? propertyFactory.create(property.getName(),
                                                                                              resolvedReferences) : propertyFactory.create(property.getName(),
                                                                                                                                           resolvedReferences.get(0));
                    targetNode.setProperty(targetCache, updatedProperty);
                }
            }
        }

        private Reference resolveReference( NodeKey sourceNodeKey,
                                            NodeKey targetNodeKey,
                                            Name propertyName,
                                            Reference referenceInSource ) {
            // try to resolve the reference into a node key that can be located in the source cache
            String referenceStringValue = referenceInSource.getString();
            NodeKey referenceInSourceKey = null;
            if (referenceInSource instanceof NodeKeyReference) {
                referenceInSourceKey = ((NodeKeyReference)referenceInSource).getNodeKey();
            } else if (NodeKey.isValidFormat(referenceStringValue)) {
                referenceInSourceKey = new NodeKey(referenceStringValue);
            } else {
                // the reference value can't be resolved into a node key directly, so try using the owning node's key
                // to construct a reference
                referenceInSourceKey = sourceNodeKey.withId(referenceStringValue);
            }

            if (referenceInSourceKey.getWorkspaceKey().equals(systemWorkspaceKey)) {
                // in the case of the system workspace, we should preserve references as-is
                return referenceInSource;
            }

            // we have a node key for the reference in the source so try to resolve it in the target
            NodeKey referenceInTargetKey = sourceToTargetKeys.get(referenceInSourceKey);
            if (referenceInTargetKey == null) {
                // the source of this reference does not have a corresponding target in the copy/clone graph

                // try to resolve the reference in the whole target (including outside the copy/clone graph)
                referenceInTargetKey = referenceInSourceKey.withWorkspaceKey(targetNodeKey.getWorkspaceKey());
                boolean resolvableInTargetWorkspace = targetCache.getNode(referenceInTargetKey) != null;
                boolean resolvableInSourceWorkspace = sourceCache.getNode(referenceInSourceKey) != null;
                if (!resolvableInTargetWorkspace && resolvableInSourceWorkspace) {
                    // it's not resolvable in the target but it's resolvable in the source, so the clone/copy graph is not
                    // reference-isolated
                    throw new WrappedException(
                                               new RepositoryException(
                                                                       JcrI18n.cannotCopyOrCloneReferenceOutsideGraph.text(propertyName,
                                                                                                                           referenceInSourceKey,
                                                                                                                           startingPathInSource)));
                } else if (!resolvableInSourceWorkspace && !referenceInSource.isWeak() && !referenceInSource.isSimple()) {
                    // it's a non resolvable strong reference, meaning it's corrupt
                    throw new WrappedException(
                                               new RepositoryException(
                                                                       JcrI18n.cannotCopyOrCloneCorruptReference.text(propertyName,
                                                                                                                      referenceInSourceKey)));
                }
            }
            // there is a corresponding target node either in or outside the clone/copy graph or an invalid reference in the
            // source
            if (referenceInSource.isSimple()) {
                return valueFactories.getSimpleReferenceFactory().create(referenceInTargetKey, referenceInSource.isForeign());
            } else if (referenceInSource.isWeak()) {
                return valueFactories.getWeakReferenceFactory().create(referenceInTargetKey, referenceInSource.isForeign());
            } else {
                return valueFactories.getReferenceFactory().create(referenceInTargetKey, referenceInSource.isForeign());
            }
        }

        protected void copyProperties( MutableCachedNode targetNode,
                                       CachedNode sourceNode ) {
            NodeKey sourceNodeKey = sourceNode.getKey();
            for (Iterator<Property> propertyIterator = sourceNode.getProperties(sourceCache); propertyIterator.hasNext();) {
                Property property = propertyIterator.next();
                if (property.isReference() || property.isSimpleReference()) {
                    // reference properties are not copied directly because that would cause incorrect back-pointers
                    // they are processed at the end of the clone/copy operation.
                    Set<Property> referenceProperties = sourceKeyToReferenceProperties.get(sourceNodeKey);
                    if (referenceProperties == null) {
                        referenceProperties = new HashSet<Property>();
                        sourceKeyToReferenceProperties.put(sourceNodeKey, referenceProperties);
                    }
                    referenceProperties.add(property);
                } else if (property.getName().equals(JcrLexicon.UUID)) {
                    // UUIDs need to be handled differently
                    copyUUIDProperty(property, targetNode, sourceNode);
                } else {
                    boolean sourceExternal = !sourceNodeKey.getSourceKey().equals(sourceCache.getRootKey().getSourceKey());
                    boolean targetInternal = targetNode.getKey().getSourceKey().equals(targetCache.getRootKey().getSourceKey());
                    // we're trying to copy an external binary into an internal node
                    if (sourceExternal && targetInternal && property.getFirstValue() instanceof ExternalBinaryValue) {
                        Property newProperty = null;
                        if (property.isMultiple()) {
                            List<Object> values = new ArrayList<Object>(property.size());
                            for (Object value : property) {
                                values.add(convertToInternalBinaryValue(value));
                            }
                            newProperty = propertyFactory.create(property.getName(), values.iterator());
                        } else if (property.isSingle()) {
                            Object value = convertToInternalBinaryValue(property.getFirstValue());
                            newProperty = propertyFactory.create(property.getName(), value);
                        }
                        // otherwise the property is empty

                        if (newProperty != null) targetNode.setProperty(targetCache, newProperty);
                    } else {
                        // it's a regular property, so copy as-is
                        targetNode.setProperty(targetCache, property);
                    }
                }
            }
        }

        protected Object convertToInternalBinaryValue( Object value ) {
            if (value instanceof ExternalBinaryValue) {
                try {
                    return valueFactories.getBinaryFactory().create(((ExternalBinaryValue)value).getStream());
                } catch (RepositoryException e) {
                    throw new WrappedException(e);
                }
            }
            return value;
        }

        protected void copyUUIDProperty( Property sourceProperty,
                                         MutableCachedNode targetNode,
                                         CachedNode sourceNode ) {
            String targetUUID = JcrSession.nodeIdentifier(targetNode.getKey(), targetCache.getRootKey());
            targetNode.setProperty(targetCache, propertyFactory.create(sourceProperty.getName(), targetUUID));
        }

        @Override
        public String toString() {
            return getOperationName() + " '"
                   + this.startingPathInSource.getString(sourceCache.getContext().getNamespaceRegistry()) + "' in workspace '"
                   + sourceCache.getWorkspace().toString() + "' into '"
                   + this.targetNode.getPath(targetCache).getString(targetCache.getContext().getNamespaceRegistry())
                   + "' in workspace '" + targetCache.getWorkspace().toString() + "'";
        }

        protected String getOperationName() {
            return "Copy";
        }

        protected boolean shouldProcessSourceKey( NodeKey sourceKey ) {
            return !sourceKey.equals(sourceCache.getRootKey())
                   && !sourceKey.getWorkspaceKey().equalsIgnoreCase(systemWorkspaceKey);
        }
    }

    protected class DeepClone extends DeepCopy {

        protected DeepClone( SessionNode targetNode,
                             WritableSessionCache cache,
                             CachedNode sourceNode,
                             SessionCache sourceCache,
                             String systemWorkspaceKey,
                             Connectors connectors ) {
            super(targetNode, cache, sourceNode, sourceCache, systemWorkspaceKey, connectors);
        }

        @Override
        protected void copyProperties( MutableCachedNode targetNode,
                                       CachedNode sourceNode ) {
            // First remove all the existing properties ...
            targetNode.removeAllProperties(targetCache);

            // Then perform the normal copyProperties step ...
            super.copyProperties(targetNode, sourceNode);
        }

        @Override
        protected void copyUUIDProperty( Property sourceProperty,
                                         MutableCachedNode targetNode,
                                         CachedNode sourceNode ) {
            targetNode.setProperty(targetCache, sourceProperty);
        }

        @Override
        protected NodeKey createTargetKeyFor( NodeKey sourceKey,
                                              NodeKey parentKeyInTarget,
                                              String preferredKey ) {
            // Reuse the same source and identifier, but a different workspace ...
            return !StringUtil.isBlank(preferredKey) ? new NodeKey(preferredKey) : parentKeyInTarget.withId(sourceKey.getIdentifier());
        }

        @Override
        protected String getOperationName() {
            return "Clone";
        }
    }
}
