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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * 
 */
public interface MutableCachedNode extends CachedNode {

    /**
     * Return whether this node was created since last saved.
     * 
     * @return true if this node was created since the session was last saved, or false otherwise
     */
    boolean isNew();

    /**
     * Return whether the property with the supplied name was created since the session was last saved. This method will always
     * return {@code true} if {@link #isNew()} returns {@code true}.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param propertyName the name of the property; may not be null
     * @return true if this node contains a property that was created since the session was last saved, or false otherwise
     * @see #isPropertyModified
     * @see #isNew
     */
    boolean isPropertyNew( SessionCache cache,
                           Name propertyName );

    /**
     * Return whether the property with the supplied name was modified since the session was last saved.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param propertyName the name of the property; may not be null
     * @return true if this node contains a property that was created since the session was last saved, or false otherwise
     * @see #isPropertyNew
     */
    boolean isPropertyModified( SessionCache cache,
                                Name propertyName );

    /**
     * Return whether this node has changes, including property-related changes and other changes not related to properties. This
     * is equivalent to calling <code>hasNonPropertyChanges() || hasPropertyChanges()</code>.
     * 
     * @return true if this node has changes, or false otherwise
     * @see #hasNonPropertyChanges()
     * @see #hasPropertyChanges()
     */
    boolean hasChanges();

    /**
     * Return whether this node has changes unrelated to properties.
     * 
     * @return true if this node has changes unrelated to properties, or false otherwise
     * @see #hasChanges()
     * @see #hasPropertyChanges()
     */
    boolean hasNonPropertyChanges();

    /**
     * Return whether this node has changes in the properties.
     * 
     * @return true if this node has added, removed, or changed properties, or false otherwise
     * @see #hasChanges()
     * @see #hasNonPropertyChanges()
     */
    boolean hasPropertyChanges();

    /**
     * Lock this node.
     * 
     * @param sessionScoped true if the lock should be limited in scope to the lifetime of the session, or false otherwise
     */
    void lock( boolean sessionScoped );

    /**
     * Unlock this node.
     */
    void unlock();

    /**
     * Set the property with the given name.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param property the property; may not be null
     * @throws NodeNotFoundException if this node no longer exists
     */
    void setProperty( SessionCache cache,
                      Property property );

    /**
     * Sets a property of type reference in the case when there's an active system cache and the property is a reference towards a
     * transient node from the system cache.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param property the property; may not be null
     * @param systemCache an existing system cache which contains transient nodes towards which the property points.
     * @throws NodeNotFoundException if this node no longer exists
     */
    void setReference( SessionCache cache,
                       Property property,
                       SessionCache systemCache );

    /**
     * Set the given property only if it has not been set previously and therefore appear as changed.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param property the property; may not be null
     * @throws NodeNotFoundException if this node no longer exists
     */
    void setPropertyIfUnchanged( SessionCache cache,
                                 Property property );

    /**
     * Set the properties on this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param properties the properties to be set; may not be null
     * @throws NodeNotFoundException if this node no longer exists
     */
    void setProperties( SessionCache cache,
                        Iterable<Property> properties );

    /**
     * Set the properties on this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param properties the iterator over the properties to be set; may not be null
     * @throws NodeNotFoundException if this node no longer exists
     */
    void setProperties( SessionCache cache,
                        Iterator<Property> properties );

    /**
     * Remove all of the properties from this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @throws NodeNotFoundException if this node no longer exists
     */
    void removeAllProperties( SessionCache cache );

    /**
     * Remove the property with the given name.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param name the name of the property to be removed; may not be null
     * @throws NodeNotFoundException if this node no longer exists
     */
    void removeProperty( SessionCache cache,
                         Name name );

    /**
     * Add the supplied mixin type if not already an explicitly referenced.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param mixinName the name of the mixin to be removed; may not be null
     */
    void addMixin( SessionCache cache,
                   Name mixinName );

    /**
     * Remove the supplied mixin type if already an explicitly referenced.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param mixinName the name of the mixin to be removed; may not be null
     */
    void removeMixin( SessionCache cache,
                      Name mixinName );

    /**
     * Get the set of mixin names that were added to this node but not yet saved.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @return the newly-added mixin type names; never null but possibly empty
     */
    Set<Name> getAddedMixins( SessionCache cache );

    /**
     * Return whether the primary type for the node has changed.
     * 
     * @return true if the primary type for the node has changed, or false otherwise
     */
    boolean hasChangedPrimaryType();

    /**
     * Adds a new federated segment with the given name and key to this node.
     * 
     * @param segmentName the name of the segment (i.e. the name of the alias under which an external child is linked); may not be
     *        null
     * @param externalNodeKey the key of the external node which should be linked under this name; may not be null
     */
    void addFederatedSegment( String externalNodeKey,
                              String segmentName );

    /**
     * Removes the federated segment towards an external node.
     * 
     * @param externalNodeKey the key of the external node which should be linked under this name; may not be null
     */
    void removeFederatedSegment( String externalNodeKey );

    /**
     * Create a new node as a child of this node with the supplied name and properties.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param key the key for the new node; may be null
     * @param name the name for the new node; may not be null
     * @param firstProperty the first property; may not be null
     * @param additionalProperties the properties that should be set on the node; may be null or empty, and any null property
     *        references will be ignored
     * @return the new child node
     */
    MutableCachedNode createChild( SessionCache cache,
                                   NodeKey key,
                                   Name name,
                                   Property firstProperty,
                                   Property... additionalProperties );

    /**
     * Create a new node as a child of this node with the supplied name and properties.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param key the key for the new node; may be null
     * @param name the name for the new node; may not be null
     * @param properties the properties that should be set on the node; may be null or empty, and any null property references
     *        will be ignored
     * @return the new child node
     */
    MutableCachedNode createChild( SessionCache cache,
                                   NodeKey key,
                                   Name name,
                                   Iterable<Property> properties );

    /**
     * Remove the node from being a child of this node. <strong>NOTE: THIS METHOD DOES NOT DELETE THE NODE</strong>
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param key the key for the child that is to be removed; may not be null
     * @throws NodeNotFoundException if the node does not exist as a child of this node
     */
    void removeChild( SessionCache cache,
                      NodeKey key );

    /**
     * Remove the node from being a child of this node and append it as a child of the supplied node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param key the key for the child that is to be removed; may not be null
     * @param newParent the new parent for the node; may not be null and may not be this node
     * @param newName the new name for the node, or null if the existing name is to be used
     * @throws NodeNotFoundException if the node does not exist as a child of this node
     */
    void moveChild( SessionCache cache,
                    NodeKey key,
                    MutableCachedNode newParent,
                    Name newName );

    /**
     * Link the existing node with the supplied key to be appended as a child of this node. After this method, the referenced node
     * is considered a child of this node as well as a child of its original parent(s).
     * <p>
     * The link can be removed by simply {@link #removeChild(SessionCache, NodeKey) removing} the linked child from the parent,
     * and this works whether or not the parent is the original parent or an additional parent.
     * </p>
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param childKey the key for the child that is to be removed; may not be null
     * @param name the name for the (linked) node, or null if the existing name is to be used
     * @return true if the link was created, or false if the link already existed as a child of this node
     * @throws NodeNotFoundException if the node does not exist
     */
    boolean linkChild( SessionCache cache,
                       NodeKey childKey,
                       Name name );

    /**
     * Remove the node from being a child of this node and append it as a child before the supplied node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param key the key for the child that is to be removed; may not be null
     * @param nextNode the key for the node before which the node should be moved; may be null if the node should be moved to the
     *        end of the parents
     * @throws NodeNotFoundException if the node does not exist as a child of this node
     */
    void reorderChild( SessionCache cache,
                       NodeKey key,
                       NodeKey nextNode );

    /**
     * Renames the child node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param key the key for the child that is to be removed; may not be null
     * @param newName the new name for the node; may not be null
     * @throws NodeNotFoundException if the node does not exist as a child of this node
     */
    void renameChild( SessionCache cache,
                      NodeKey key,
                      Name newName );

    /**
     * Adds to this node a reference with the given type from the node with the supplied key to this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param property the {@link org.modeshape.jcr.value.Property} of the referrer node; may not be null
     * @param referrerKey the key for the node that has a new reference to this node; may not be null
     * @param type the reference type; may not be null
     */
    void addReferrer( SessionCache cache,
                      Property property,
                      NodeKey referrerKey,
                      ReferenceType type );

    /**
     * Remove from this node a reference with the given type from the node with the supplied key to this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param property the {@link org.modeshape.jcr.value.Property} of the referrer node; may not be null
     * @param referrerKey the key for the node that no longer has a reference to this node; may not be null
     * @param type the reference type; may not be null
     */
    void removeReferrer( SessionCache cache,
                         Property property,
                         NodeKey referrerKey,
                         ReferenceType type );

    /**
     * Compute an ETag value for this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @return an ETag value; never null but possibly empty
     */
    String getEtag( SessionCache cache );

    /**
     * Copies into this node all the properties and children (deep copy) from the given source node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param sourceNode the node from which to copy the properties and children; may not be null
     * @param sourceCache the cache in which the source node belongs; may not be null
     * @param systemWorkspaceKey the key of the system workspace; may not be null
     * @param connectors a {@link Connectors} instance which used for processing external nodes.
     * @return a [source key -> target key] which represents the node correspondence after the copy operation.
     */
    public Map<NodeKey, NodeKey> deepCopy( SessionCache cache,
                                           CachedNode sourceNode,
                                           SessionCache sourceCache,
                                           String systemWorkspaceKey,
                                           Connectors connectors );

    /**
     * Clones into this node all the properties and children (deep clone) from the given source node. Each cloned node will have
     * the same identifier as the source node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param sourceNode the node from which to copy the properties and children; may not be null
     * @param sourceCache the cache in which the source node belongs; may not be null
     * @param systemWorkspaceKey the key of the system workspace; may not be null
     * @param connectors a {@link Connectors} instance which used for processing external nodes.
     */
    public void deepClone( SessionCache cache,
                           CachedNode sourceNode,
                           SessionCache sourceCache,
                           String systemWorkspaceKey,
                           Connectors connectors );

    /**
     * Returns a set with the keys of the children which have been removed for this node.
     * 
     * @return a <code>Set&lt;{@link NodeKey}></code>, never null
     * @deprecated use {@link org.modeshape.jcr.cache.MutableCachedNode.NodeChanges#removedChildren()}
     */
    @Deprecated
    public Set<NodeKey> removedChildren();

    /**
     * Returns a set with all the referencing nodes (nodes which are referring this node) which have changed.
     * 
     * @return the set of {@link NodeKey} instances, never null.
     */
    public Set<NodeKey> getChangedReferrerNodes();

    /**
     * Return whether this node contains only changes to the additional parents.
     * 
     * @return true if this node contains only added or removed additional parents.
     */
    public boolean hasOnlyChangesToAdditionalParents();

    /**
     * Sets a flag indicating if this node and anything below it should be excluded from all searches and indexing operations.
     */
    public void excludeFromSearch();

    /**
     * Returns an object encapsulating all the different changes that this session node contains.
     * 
     * @return a {@code non-null} {@link NodeChanges} object.
     */
    public NodeChanges getNodeChanges();

    /**
     * Sets permissions for this node, creating in effect an ACL.
     *
     * @param cache the cache to which this node belongs; may not be null
     * @param permissions a set of privileges enqueued by principal name; may not be null
     * @return a {@link org.modeshape.jcr.cache.MutableCachedNode.PermissionChanges} instance which reflects how the persistent
     * state has been affected by the change; never null
     */
    public PermissionChanges setPermissions(SessionCache cache, Map<String, Set<String>> permissions);

    /**
     * Removes any potential existing ACL for this node.
     *
     * @param cache the cache to which this node belongs; may not be null
     * @return a {@link org.modeshape.jcr.cache.MutableCachedNode.PermissionChanges} instance which reflects which principal
     * have been removed; never null
     */
    public PermissionChanges removeACL(SessionCache cache);

    /**
     * Adds an internal property to this cached node. Internal properties are stored only in the internal document and have no
     * JCR relevance.
     *
     * @param name a {@code String} the name of the property; may not be null.
     * @param value a {@code String} the value of the property; may not be null.
     */
    public void addInternalProperty(String name, Object value);

    /**
     * Returns a map of all the internal properties added for this node. Internal properties are stored as-is in the document and have no
     * JCR relevance.
     * 
     * @return a {@link Map} of (propertyName, propertyValue) pairs. never {@code null} but possibly empty.
     */
    public Map<String, Object> getAddedInternalProperties();

    /**
     * Removes an internal property from this cached node. Internal properties are stored as-is in the document and have no
     * JCR relevance.
     *
     * @param name a {@code String} the name of the property; may not be null.
     * @return {@code true} if the property was removed, {@code false} otherwise.
     */
    public boolean removeInternalProperty(String name);

    /**
     * Returns a set of all the internal properties which have been removed from this node. Internal properties are stored 
     * as-is the document and have no JCR relevance.
     * 
     * @return a {@link Set} of property names; never {@code null} but possibly empty.
     */
    public Set<String> getRemovedInternalProperties();

    /**
     * Interface which exposes all the changes that have occurred on a {@link MutableCachedNode} instance
     */
    public interface NodeChanges {
        /**
         * Returns a set with the names of the properties that have changed. This includes new/modified properties.
         * 
         * @return a {@code non-null} Set
         */
        Set<Name> changedPropertyNames();

        /**
         * Returns a set with the names of the properties that have been removed.
         * 
         * @return a {@code non-null} Set
         */
        public Set<Name> removedPropertyNames();

        /**
         * Returns a set with the names of the mixins that have been added.
         * 
         * @return a {@code non-null} Set
         */
        public Set<Name> addedMixins();

        /**
         * Returns a set with the names of the mixins that have been removed.
         * 
         * @return a {@code non-null} Set
         */
        public Set<Name> removedMixins();

        /**
         * Returns the [childKey, childName] pairs of the children that have been appended (at the end).
         * 
         * @return a {@code non-null} Map
         */
        public LinkedHashMap<NodeKey, Name> appendedChildren();

        /**
         * Returns the set of children that have been removed
         * 
         * @return a {@code non-null} Set
         */
        public Set<NodeKey> removedChildren();

        /**
         * Returns the [childKey, childName] pairs of the children that have been renamed, where "childName" represents the new
         * name after the rename.
         * 
         * @return a {@code non-null} Map
         */
        public Map<NodeKey, Name> renamedChildren();

        /**
         * Returns the [insertBeforeChildKey, [childKey, childName]] structure of the children that been inserted before another
         * existing child. This is normally caused due to reorderings
         * 
         * @return a {@code non-null} Map
         */
        public Map<NodeKey, LinkedHashMap<NodeKey, Name>> childrenInsertedBefore();

        /**
         * Returns the set of parents that have been added
         * 
         * @return a {@code non-null} Set
         */
        public Set<NodeKey> addedParents();

        /**
         * Returns the set of parents that have been removed
         * 
         * @return a {@code non-null} Set
         */
        public Set<NodeKey> removedParents();

        /**
         * Returns the node key of the new primary parent, in case it has changed.
         * 
         * @return either the {@link NodeKey} of the new primary parent or {@code null}
         */
        public NodeKey newPrimaryParent();

        /**
         * Returns a set of node keys with the weak referrers that have been added.
         * 
         * @return a {@code non-null} Set
         */
        public Set<NodeKey> addedWeakReferrers();

        /**
         * Returns a set of node keys with the weak referrers that have been removed.
         * 
         * @return a {@code non-null} Set
         */
        public Set<NodeKey> removedWeakReferrers();

        /**
         * Returns a set of node keys with the strong referrers that have been added.
         * 
         * @return a {@code non-null} Set
         */
        public Set<NodeKey> addedStrongReferrers();

        /**
         * Returns a set of node keys with the strong referrers that have been removed.
         * 
         * @return a {@code non-null} Set
         */
        public Set<NodeKey> removedStrongReferrers();
    }

    /**
     * Interface which exposes the ACL-related changes for mutable cached nodes.
     */
    public interface PermissionChanges {
        /**
         * Returns the number of new principals for which privileges have been added in the node's ACL.
         *
         * @return the number of principals
         */
        public long addedPrincipalsCount();

        /**
         * Returns the number of principals which have been removed from this node's ACL.
         *
         * @return the number of principals
         */
        public long removedPrincipalsCount();
    }
}
