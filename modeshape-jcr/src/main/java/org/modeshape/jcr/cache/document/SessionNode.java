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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.infinispan.util.concurrent.ConcurrentHashSet;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.text.Inflector;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.ChildReferences.BasicContext;
import org.modeshape.jcr.cache.ChildReferences.ChildInsertions;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

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
    private volatile NodeKey newParent;
    private final AtomicReference<ChangedAdditionalParents> additionalParents = new AtomicReference<ChangedAdditionalParents>();
    private final ChangedChildren changedChildren = new ChangedChildren();
    private final AtomicReference<MutableChildReferences> appended = new AtomicReference<MutableChildReferences>();
    private final AtomicReference<MixinChanges> mixinChanges = new AtomicReference<MixinChanges>();
    private final AtomicReference<ReferrerChanges> referrerChanges = new AtomicReference<ReferrerChanges>();
    private final boolean isNew;
    private volatile LockChange lockChange;

    public SessionNode( NodeKey key,
                        boolean isNew ) {
        this.key = key;
        this.isNew = isNew;
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
        if (isNew) return true;
        if (newParent != null) return true;
        if (!changedProperties.isEmpty()) return true;
        if (!removedProperties.isEmpty()) return true;
        ChangedChildren changedChildren = changedChildren();
        if (changedChildren != null && !changedChildren.isEmpty()) return true;
        MutableChildReferences childRefChanges = appended(false);
        if (childRefChanges != null && !childRefChanges.isEmpty()) return true;
        ChangedAdditionalParents additionalParents = additionalParents();
        if (additionalParents != null && !additionalParents.isEmpty()) return true;
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
        assert newParent != null;
        if (newParent.equals(getParentKey(cache))) {
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
        return additionalParents.remove(oldParent);
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
        return (WritableSessionCache)cache;
    }

    protected final AbstractSessionCache session( NodeCache cache ) {
        return (AbstractSessionCache)cache;
    }

    protected final WorkspaceCache workspace( NodeCache cache ) {
        return ((DocumentCache)cache).workspaceCache();
    }

    /**
     * Get the CachedNode within the workspace cache.
     * 
     * @param session the session; may not be null
     * @return the workspace cache's node, or null if this node is new
     */
    protected CachedNode nodeInWorkspace( AbstractSessionCache session ) {
        return session.getWorkspace().getNode(key);
    }

    @Override
    public NodeKey getParentKey( NodeCache cache ) {
        if (newParent != null) {
            return newParent;
        }
        CachedNode cachedNode = nodeInWorkspace(session(cache));
        //if it is null, it means it has been removed in the meantime from the ws
        return cachedNode != null ? cachedNode.getParentKey(cache) : null;
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
        Set<NodeKey> persisted = raw.getAdditionalParentKeys(cache);

        ChangedAdditionalParents additionalParents = this.additionalParents.get();
        if (additionalParents == null) {
            return persisted;
        }

        // Make an immutable copy ...
        Set<NodeKey> copy = new LinkedHashSet<NodeKey>();
        copy.addAll(additionalParents.getAdditions());
        copy.removeAll(additionalParents.getRemovals());
        return Collections.unmodifiableSet(copy);
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

    protected final Segment getSegment( NodeCache cache,
                                        CachedNode parent ) {
        if (parent != null) {
            ChildReference ref = parent.getChildReferences(cache).getChild(key, new BasicContext());
            return ref.getSegment();
        }
        // This is the root node ...
        return workspace(cache).childReferenceForRoot().getSegment();
    }

    @Override
    public Path getPath( NodeCache cache ) {
        AbstractSessionCache session = session(cache);
        CachedNode parent = parent(session);
        if (parent != null) {
            Path parentPath = parent.getPath(session);
            return session.pathFactory().create(parentPath, getSegment(session, parent));
        }
        // This is the root node ...
        return session.rootPath();
    }

    @Override
    public Name getPrimaryType( NodeCache cache ) {
        AbstractSessionCache session = session(cache);
        Property prop = getProperty(JcrLexicon.PRIMARY_TYPE, session);
        return session.nameFactory().create(prop.getFirstValue());
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
            return changes.getAddedReferrers(type);
        }
        // Read the referrers from the workspace node ...
        Set<NodeKey> referrers = persisted.getReferrers(workspace(cache), type);
        if (changes != null) {
            // Make a copy and then add and remove the changes ...
            referrers.addAll(changes.getAddedReferrers(type));
            referrers.removeAll(changes.getRemovedReferrers(type));
        }
        return referrers;
    }

    public ReferrerChanges getReferrerChanges() {
        return referrerChanges(false);
    }

    @Override
    public void addReferrer( SessionCache cache,
                             NodeKey referrerKey,
                             ReferenceType type ) {
        ReferrerChanges changes = referrerChanges(true);
        switch (type) {
            case WEAK:
                changes.addWeakReferrer(referrerKey);
                break;
            case STRONG:
                changes.addStrongReferrer(referrerKey);
                break;
            case BOTH:
                throw new IllegalArgumentException("The type parameter may be WEAK or STRONG, but may not be BOTH");
        }
    }

    @Override
    public void removeReferrer( SessionCache cache,
                                NodeKey referrerKey,
                                ReferenceType type ) {
        ReferrerChanges changes = referrerChanges(true);
        switch (type) {
            case WEAK:
                changes.removeWeakReferrer(referrerKey);
                break;
            case STRONG:
                changes.removeStrongReferrer(referrerKey);
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
    public Iterator<Property> getProperties( final NodeCache cache ) {
        final AbstractSessionCache session = session(cache);
        final CachedNode raw = nodeInWorkspace(session);
        Iterable<Property> rawProps = raw == null ? null : new Iterable<Property>() {
            @Override
            public Iterator<Property> iterator() {
                List<Property> values = new LinkedList<Property>();
                for (Iterator<Property> iter = raw.getProperties(workspace(cache)); iter.hasNext();) {
                    Property prop = iter.next();
                    if (isPropertyRemoved(prop.getName())) continue;
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
    }

    @Override
    public void setProperties( SessionCache cache,
                               Iterable<Property> properties ) {
        writableSession(cache).assertInSession(this);
        for (Property property : properties) {
            Name name = property.getName();
            changedProperties.put(name, property);
            if (!isNew) removedProperties.remove(name);
        }
    }

    @Override
    public void removeProperty( SessionCache cache,
                                Name name ) {
        writableSession(cache).assertInSession(this);
        changedProperties.remove(name);
        if (!isNew) removedProperties.put(name, name);
    }

    @Override
    public ChildReferences getChildReferences( NodeCache cache ) {
        if (isNew) {
            // Then this node was created in this session. Note that it is possible that there still may be a persisted node,
            // meaning that the persisted node was likely removed in this session and (without saving) a new node was created
            // using the same key as the persisted node. Therefore, we do NOT want to use the persisted node in this case ...
            return new SessionChildReferences(null, appended.get(), changedChildren);
        }
        // Find the persisted information, since the info we have is relative to it ...
        CachedNode persistedNode = nodeInWorkspace(session(cache));
        ChildReferences persisted = persistedNode != null ? persistedNode.getChildReferences(cache) : null;

        // And create a transient implementation ...
        return new SessionChildReferences(persisted, appended.get(), changedChildren);
    }

    @Override
    public MutableCachedNode createChild( SessionCache cache,
                                          NodeKey key,
                                          Name name,
                                          Property firstProperty,
                                          Property... additionalProperties ) {
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);

        // TODO: Federation - how to determine the propery child key ...
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

        // TODO: Federation - how to determine the propery child key ...
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

    protected SessionNode removeChildFromNode( AbstractSessionCache session,
                                               NodeKey key ) {
        // See if the node has this node as a parent or additional parent ...
        SessionNode child = session.mutable(key);
        boolean additional = false;
        if (!child.getParentKey(session).equals(this.key)) {
            // Try to remove it from the additional parents ...
            if (child.removeAdditionalParent(session, this.key)) {
                additional = true;
            } else {
                // Not a primary or additional parent ...
                if (!getChildReferences(session).hasChild(key)) {
                    throw new NodeNotFoundException(key);
                }
            }
        }

        SessionNode node = session.mutable(key);
        assert node != null;
        if (!additional) {
            node.newParent = null;
        }
        MutableChildReferences appended = this.appended.get();
        ChildReference removed = null;
        if (appended != null) {
            removed = appended.remove(key); // The node was appended to this node but not yet persisted ...
        }
        if (removed == null) {
            changedChildren.remove(key);
        }
        return node;
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

        // We need a mutable node in the session for the child, so that we can find changes in the parent ...
        // cache.mutable(key);

        if (toBeMoved == null) {
            // It wasn't appended, so verify it is really a child ...
            toBeMoved = references.getChild(key);
            if (toBeMoved == null) throw new NodeNotFoundException(key);
            // And mark it as removed ...
            changedChildren.remove(key);
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

        // Now perform the rename ...
        changedChildren.renameTo(key, newName);
    }

    @Override
    public void linkChild( SessionCache cache,
                           NodeKey childKey,
                           Name name ) {
        WritableSessionCache session = writableSession(cache);
        session.assertInSession(this);

        // Find the referenced node ...
        SessionNode child = session.mutable(childKey);
        if (!child.isNew() && this.key.equals(child.getParentKey(cache))) return;

        // Add this node as a parent for the child ...
        if (child.addAdditionalParent(cache, this.key)) {
            // Add it as a parent of this node ...
            appended(true).append(childKey, name);
        }
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
        if (isNew) {
            sb.append(" created; ");
        }
        NodeKey newParent = this.newParent;
        if (newParent != null) {
            sb.append(" moved to ").append(newParent).append("; ");
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
        if (!changedProperties.isEmpty()) {
            sb.append(" props: {");
            boolean first = true;
            for (Map.Entry<Name, Property> entry : changedProperties.entrySet()) {
                if (first) first = false;
                else sb.append(',');
                Property property = entry.getValue();
                sb.append(" +").append(property.getString(registry));
            }
            sb.append('}');
        }
        if (!removedProperties.isEmpty()) {
            sb.append(" props: {");
            boolean first = true;
            for (Name name : removedProperties.keySet()) {
                if (first) first = false;
                else sb.append(',');
                sb.append(" -").append(name.getString(registry));
            }
            sb.append('}');
        }
        MutableChildReferences appended = appended(false);
        if (!changedChildren.isEmpty() || (appended != null && !appended.isEmpty())) {
            sb.append(" children: ");
            if (!changedChildren.isEmpty()) {
                sb.append(changedChildren);
            }
            if (appended != null && !appended.isEmpty()) {
                sb.append(" appended: [");
                Iterator<ChildReference> iter = appended.iterator();
                if (iter.hasNext()) {
                    sb.append(iter.next().getString(registry));
                    while (iter.hasNext()) {
                        sb.append(',');
                        sb.append(iter.next().getString(registry));
                    }
                }
                sb.append(']');
            }
        }
        return sb.toString();
    }

    @ThreadSafe
    protected static class ChangedAdditionalParents {
        private final Set<NodeKey> removals = new ConcurrentHashSet<NodeKey>();
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
            return removals == null ? false : removals.contains(ref.getKey());
        }

        @Override
        public boolean isRenamed( ChildReference ref ) {
            Map<NodeKey, Name> renames = this.newNames.get();
            return renames == null ? false : renames.containsKey(ref.getKey());
        }

        @Override
        public boolean isRenamed( Name newName ) {
            Map<NodeKey, Name> renames = this.newNames.get();
            return renames == null ? false : renames.containsValue(newName);
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
                removals = new ConcurrentHashSet<NodeKey>();
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
            if (insertions != null) {
                insertions.toString(sb);
                if (removals != null && !removals.isEmpty()) sb.append("; ");
            }
            if (removals != null) {
                sb.append("removals: " + removals);
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
        public int size() {
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
        public ChildReference inserted( NodeKey key ) {
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

        public Iterator<ChildInsertions> insertions( Name name ) {
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

        public ChildInsertions insertionsBefore( NodeKey key ) {
            // This is safe to return without locking (that's why we used a ConcurrentMap) ...
            return insertedBefore.get(key);
        }

        public void insertBefore( ChildReference before,
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

        public boolean remove( NodeKey key ) {
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

        public boolean remove( ChildReference inserted ) {
            Lock lock = this.lock.writeLock();
            try {
                lock.lock();
                if (this.inserted.remove(inserted.getKey())) {
                    AtomicInteger count = this.insertedNames.get(inserted.getName());
                    if (count != null) {
                        if (count.decrementAndGet() == 0) {
                            this.insertedNames.remove(inserted.getName());
                        }
                    }
                    for (Insertions insertions : insertedBefore.values()) {
                        if (insertions.remove(inserted)) {
                            insertedBefore.remove(insertions.insertedBefore(), new Insertions(insertions.insertedBefore()));
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

        public StringBuilder toString( StringBuilder sb ) {
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

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return inserted + " before " + before;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return this.before.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
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
        private final Set<NodeKey> addedWeak = new HashSet<NodeKey>();
        private final Set<NodeKey> removedWeak = new HashSet<NodeKey>();
        private final Set<NodeKey> addedStrong = new HashSet<NodeKey>();
        private final Set<NodeKey> removedStrong = new HashSet<NodeKey>();

        public void addWeakReferrer( NodeKey nodeKey ) {
            this.addedWeak.add(nodeKey);
            this.removedWeak.remove(nodeKey);
        }

        public void removeWeakReferrer( NodeKey nodeKey ) {
            this.addedWeak.remove(nodeKey);
            this.removedWeak.add(nodeKey);
        }

        public void addStrongReferrer( NodeKey nodeKey ) {
            this.addedStrong.add(nodeKey);
            this.removedStrong.remove(nodeKey);
        }

        public void removeStrongReferrer( NodeKey nodeKey ) {
            this.addedStrong.remove(nodeKey);
            this.removedStrong.add(nodeKey);
        }

        public Set<NodeKey> getAddedReferrers( ReferenceType type ) {
            switch (type) {
                case STRONG:
                    return addedStrong;
                case WEAK:
                    return addedWeak;
                case BOTH:
                    Set<NodeKey> result = new HashSet<NodeKey>();
                    result.addAll(addedWeak);
                    result.addAll(addedStrong);
                    return result;
            }
            assert false : "Should never get here";
            return null;
        }

        public Set<NodeKey> getRemovedReferrers( ReferenceType type ) {
            switch (type) {
                case STRONG:
                    return removedStrong;
                case WEAK:
                    return removedWeak;
                case BOTH:
                    Set<NodeKey> result = new HashSet<NodeKey>();
                    result.addAll(removedStrong);
                    result.addAll(removedWeak);
                    return result;
            }
            assert false : "Should never get here";
            return null;
        }

        public boolean isEmpty() {
            return addedWeak.isEmpty() && removedWeak.isEmpty() && addedStrong.isEmpty() && removedStrong.isEmpty();
        }
    }

}
