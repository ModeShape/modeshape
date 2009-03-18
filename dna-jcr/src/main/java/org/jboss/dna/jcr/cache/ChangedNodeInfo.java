/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.jcr.NodeDefinitionId;

/**
 * The information that describes a node. This is the information that is kept in the cache.
 * <p>
 * Each instance maintains a reference to the original (usually immutable) NodeInfo representation that was probably read from the
 * repository.
 */
@Immutable
public class ChangedNodeInfo implements NodeInfo {

    protected static final PropertyInfo DELETED_PROPERTY = null;

    /**
     * A reference to the original representation of the node.
     */
    private final NodeInfo original;

    /**
     * The new parent for this node if it has been changed, or null if the parent has not be changed.
     */
    private UUID newParent;

    /**
     * The updated children, or null if the children have not been changed from the original's.
     */
    private ChangedChildren changedChildren;

    /**
     * This map, if it is non-null, contains the changed properties, overriding whatever is in the original. If a property is
     * removed from the original, an entry is added to this map with the name of the removed property and a null PropertyInfo.
     */
    private Map<Name, PropertyInfo> changedProperties;

    /**
     * Create an immutable NodeInfo instance.
     * 
     * @param original the original node information, may not be null
     */
    public ChangedNodeInfo( NodeInfo original ) {
        assert original != null;
        this.original = original;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getOriginalLocation()
     */
    public Location getOriginalLocation() {
        return original.getOriginalLocation();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getUuid()
     */
    public UUID getUuid() {
        return original.getUuid();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getParent()
     */
    public UUID getParent() {
        // Even if this is used for recording changes to the root node (which has no parent),
        // the root node cannot be moved to a different node (and no other node can be moved to
        // the root). Therefore, if this represents the root node, the original's parent UUID will
        // be the correct parent (null), and this representation will not need to have a different
        // (non-null) value.
        if (newParent != null) return newParent;
        return original.getParent();
    }

    /**
     * Record that this node has been moved under a new parent. This method does <i>not</i> change the ChildNode references in the
     * old or new parent.
     * 
     * @param parent the new parent, or null if the original's parent should be used
     * @return the previous parent (either the original's or the last new parent); may be null
     */
    public UUID setParent( UUID parent ) {
        UUID result = newParent != null ? newParent : original.getParent(); // may still be null
        newParent = parent;
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getPrimaryTypeName()
     */
    public Name getPrimaryTypeName() {
        return original.getPrimaryTypeName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getDefinitionId()
     */
    public NodeDefinitionId getDefinitionId() {
        return original.getDefinitionId();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getChildren()
     */
    public Children getChildren() {
        if (changedChildren != null) return changedChildren;
        return original.getChildren();
    }

    /**
     * Add a child to the children. This method does nothing if the child is already in the children.
     * 
     * @param childName the name of the child that is to be added; may not be null
     * @param childUuid the UUID of the child that is to be added; may not be null
     * @param factory the path factory that should be used to create a {@link Segment} for the new {@link ChildNode} object
     * @return the child node that was just added; never null
     */
    public ChildNode addChild( Name childName,
                               UUID childUuid,
                               PathFactory factory ) {
        if (changedChildren == null) {
            // We need to capture the original children as a changed contained ...
            changedChildren = new ChangedChildren(original.getChildren());
        }
        return changedChildren.add(childName, childUuid, factory);
    }

    /**
     * Remove a child from the children. This method only uses the child's UUID to identify the contained ChildNode instance that
     * should be removed.
     * <p>
     * Note that this method returns the new {@link Children} container, which is the same as would be returned by
     * {@link #getChildren()} called immediately after this method.
     * </p>
     * 
     * @param childUUID the UUID of the child that is to be removed; may not be null
     * @param factory the path factory that should be used to create a {@link Segment} for replacement {@link ChildNode} objects
     *        for nodes with the same name that and higher same-name-sibiling indexes.
     * @return the Children object that has the modified children
     */
    public Children removeChild( UUID childUUID,
                                 PathFactory factory ) {
        if (changedChildren == null) {
            // Create the changed children. First check whether there are 0 or 1 child ...
            Children existing = original.getChildren();
            int numExisting = existing.size();
            if (numExisting == 0) {
                // nothing to do, so return the original's children
                return existing;
            }
            if (existing.getChild(childUUID) == null) {
                // The requested child doesn't exist in the children, so return silently ...
                return existing;
            }
            if (numExisting == 1) {
                // We're removing the only child in the original ...
                changedChildren = new ChangedChildren(existing.getParentUuid());
                return changedChildren;
            }
            // There is at least one child, so create the new children container ...
            assert existing instanceof InternalChildren;
            InternalChildren internal = (InternalChildren)existing;
            changedChildren = internal.without(childUUID, factory);
        } else {
            changedChildren = changedChildren.without(childUUID, factory);
        }
        return changedChildren;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#hasProperties()
     */
    public boolean hasProperties() {
        if (changedProperties == null) return original.hasProperties();
        int numUnchanged = original.getPropertyCount();
        int numChangedOrDeleted = changedProperties.size();
        if (numUnchanged > numChangedOrDeleted) return true; // more unchanged than could be deleted
        // They could all be changed or deleted, so we need to find one changed property ...
        for (Map.Entry<Name, PropertyInfo> entry : changedProperties.entrySet()) {
            if (entry.getValue() != DELETED_PROPERTY) return true;
        }
        return false; // all properties must have been deleted ...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getPropertyCount()
     */
    public int getPropertyCount() {
        int numUnchanged = original.getPropertyCount();
        if (changedProperties == null) return numUnchanged;
        return getPropertyNames().size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getPropertyNames()
     */
    public Set<Name> getPropertyNames() {
        if (changedProperties != null) {
            Set<Name> result = new HashSet<Name>(original.getPropertyNames());
            for (Map.Entry<Name, PropertyInfo> entry : changedProperties.entrySet()) {
                if (entry.getValue() != DELETED_PROPERTY) {
                    result.add(entry.getKey());
                } else {
                    result.remove(entry.getKey());
                }
            }
            return result; // don't make unmod wrapper, since we've already made a copy ...
        }
        return original.getPropertyNames();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getProperty(org.jboss.dna.graph.property.Name)
     */
    public PropertyInfo getProperty( Name name ) {
        if (changedProperties != null && changedProperties.containsKey(name)) {
            return changedProperties.get(name); // either the changed PropertyInfo, or null if property was deleted
        }
        return original.getProperty(name);
    }

    public PropertyInfo setProperty( PropertyInfo newProperty ) {
        Name name = newProperty.getPropertyName();
        if (changedProperties == null) {
            // There were no changes made yet ...

            // Create the map of changed properties ...
            changedProperties = new HashMap<Name, PropertyInfo>();
            changedProperties.put(name, newProperty);

            // And return the original property (or null if there was none) ...
            return original.getProperty(name);
        }
        // The property may already have been changed, in which case we need to return the changed one ...
        if (changedProperties.containsKey(name)) {
            PropertyInfo changed = changedProperties.put(name, null);
            // The named property was indeed deleted ...
            return changed;
        }
        // Otherwise, the property was not yet changed or deleted ...
        PropertyInfo changed = original.getProperty(name);
        changedProperties.put(name, newProperty);
        return changed;
    }

    public PropertyInfo removeProperty( Name name ) {
        if (changedProperties == null) {
            // Make sure the property was in the original ...
            PropertyInfo existing = original.getProperty(name);
            if (existing == null) {
                // The named property didn't exist in the original, nor was it added and deleted in this object ...
                return null;
            }

            // Create the map of changed properties ...
            changedProperties = new HashMap<Name, PropertyInfo>();
            changedProperties.put(name, DELETED_PROPERTY);
            return existing;
        }
        // The property may already have been changed, in which case we need to return the changed one ...
        if (changedProperties.containsKey(name)) {
            PropertyInfo changed = changedProperties.put(name, null);
            // The named property was indeed deleted ...
            return changed;
        }
        // Otherwise, the property was not yet changed or deleted ...
        PropertyInfo changed = original.getProperty(name);
        changedProperties.put(name, null);
        return changed;
    }
}
