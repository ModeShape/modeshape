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
package org.modeshape.graph.connector.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * A {@link Node} implementation used by the hashed-based connector (see {@link MapWorkspace} and {@link MapTransaction}), which
 * stores all node state keyed by the node's hash (or identifier).
 * <p>
 * Strictly speaking, this class is not immutable or thread safe. However, the persisted state cannot be changed. Instead, any
 * changes made to the object are stored in a transient area and are made "persistable"via the {@link #freeze()} method.
 * </p>
 * <p>
 * The {@link MapTransaction} maintains an unfrozen, changed instance within it transactional state, and always puts the
 * {@link #freeze() frozen}, read-only representation inside the
 */
public class MapNode implements Node, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /* These members MUST be treated as "final", even though they cannot be to correctly implement Serializable */
    private/*final*/UUID uuid;
    private/*final*/Segment name;
    private/*final*/UUID parent;
    private/*final*/Map<Name, Property> properties;
    private/*final*/List<UUID> children;
    private/*final*/int version = 1;

    /** The changes made to this object, making it unfrozen */
    protected transient Changes changes;

    /**
     * Create a new node instance.
     * 
     * @param uuid the UUID of the node; may not be null
     * @param name the name of the node; may be null only if the parent is also null
     * @param parent the UUID of the parent node; may be null only if the name is null
     * @param properties the unmodifiable map of properties; may be null or empty
     * @param children the unmodificable list of child UUIDs; may be null or empty
     */
    public MapNode( UUID uuid,
                    Segment name,
                    UUID parent,
                    Map<Name, Property> properties,
                    List<UUID> children ) {
        this.uuid = uuid;
        this.name = name;
        this.parent = parent;
        this.properties = properties != null ? properties : Collections.<Name, Property>emptyMap();
        this.children = children != null ? children : Collections.<UUID>emptyList();
        assert this.uuid != null;
        assert this.properties != null;
        assert this.children != null;
        assert this.name != null ? this.parent != null : this.parent == null;
    }

    /**
     * Create a new node instance.
     * 
     * @param uuid the UUID of the node; may not be null
     * @param name the name of the node; may be null only if the parent is also null
     * @param parent the UUID of the parent node; may be null only if the name is null
     * @param properties the unmodifiable map of properties; may be null or empty
     * @param children the unmodificable list of child UUIDs; may be null or empty
     * @param version the version number
     */
    protected MapNode( UUID uuid,
                       Segment name,
                       UUID parent,
                       Map<Name, Property> properties,
                       List<UUID> children,
                       int version ) {
        this.uuid = uuid;
        this.name = name;
        this.parent = parent;
        this.properties = properties != null ? properties : Collections.<Name, Property>emptyMap();
        this.children = children != null ? children : Collections.<UUID>emptyList();
        this.version = version;
        assert this.uuid != null;
        assert this.properties != null;
        assert this.children != null;
        assert this.name != null ? this.parent != null : this.parent == null;
    }

    /**
     * Create a new node instance.
     * 
     * @param uuid the UUID of the node; may not be null
     * @param name the name of the node; may be null only if the parent is also null
     * @param parent the UUID of the parent node; may be null only if the name is null
     * @param properties the properties that are to be copied into the new node; may be null or empty
     * @param children the unmodificable list of child UUIDs; may be null or empty
     */
    public MapNode( UUID uuid,
                    Segment name,
                    UUID parent,
                    Iterable<Property> properties,
                    List<UUID> children ) {
        this.uuid = uuid;
        this.name = name;
        this.parent = parent;
        if (properties != null) {
            Map<Name, Property> props = new HashMap<Name, Property>();
            for (Property prop : properties) {
                props.put(prop.getName(), prop);
            }
            this.properties = props.isEmpty() ? Collections.<Name, Property>emptyMap() : Collections.unmodifiableMap(props);
        } else {
            this.properties = Collections.emptyMap();
        }
        this.children = children != null ? children : Collections.<UUID>emptyList();
        assert this.uuid != null;
        assert this.properties != null;
        assert this.children != null;
        assert this.name != null ? this.parent != null : this.parent == null;
    }

    /**
     * Create a root node with the supplied UUID.
     * 
     * @param uuid the UUID of the root node; may not be null
     */
    public MapNode( UUID uuid ) {
        this.uuid = uuid;
        this.name = null;
        this.parent = null;
        this.properties = Collections.emptyMap();
        this.children = Collections.emptyList();
        assert this.uuid != null;
        assert this.properties != null;
        assert this.children != null;
    }

    /**
     * Get the version number of this node.
     * 
     * @return the version number
     */
    public int getVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Node#getUuid()
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Node#getName()
     */
    public Segment getName() {
        return changes != null ? changes.getName() : name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Node#getProperties()
     */
    public Map<Name, Property> getProperties() {
        return changes != null ? changes.getProperties(false) : properties;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Node#getProperty(org.modeshape.graph.property.Name)
     */
    public Property getProperty( Name name ) {
        return getProperties().get(name);
    }

    /**
     * @return parent
     */
    public UUID getParent() {
        return changes != null ? changes.getParent() : parent;
    }

    /**
     * @return children
     */
    public List<UUID> getChildren() {
        return changes != null ? changes.getChildren(false) : children;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Node) {
            Node that = (Node)obj;
            if (!this.getUuid().equals(that.getUuid())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.name == null) {
            sb.append("(");
        } else {
            sb.append(this.name).append(" (");
        }
        sb.append(uuid).append(")");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method never clones the {@link #hasChanges() changes}.
     * </p>
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MapNode clone() {
        return new MapNode(uuid, name, parent, properties, children);
    }

    /**
     * Determine if this node has any unsaved changes.
     * 
     * @return true if there are unsaved changes, or false otherwise
     */
    protected boolean hasChanges() {
        return changes != null;
    }

    /**
     * Create the {@link Changes} implementation. Subclasses that require a specialized class should overwrite this method. Note
     * that this method does not modify any internal state; it should just instantiate and return the correct Changes class.
     * 
     * @return the changes object.
     */
    protected Changes newChanges() {
        return new Changes();
    }

    /**
     * Return the frozen node with all internal state reflective of any changes. If this node has no changes, this method simply
     * returns this same node. Otherwise, this method creates a new node that has no changes and that mirrors this node's current
     * state, and this new node will have an incremented {@link #getVersion() version} number.
     * 
     * @return the unfrozen node; never null
     */
    public MapNode freeze() {
        if (!hasChanges()) return this;
        return new MapNode(uuid, changes.getName(), changes.getParent(), changes.getUnmodifiableProperties(),
                           changes.getUnmodifiableChildren(), version + 1);
    }

    /**
     * Create a copy of this node except using the supplied parent.
     * 
     * @param parent Sets parent to the specified value.
     * @return the new map node; never null
     */
    public MapNode withParent( UUID parent ) {
        if (changes == null) {
            MapNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setParent(parent);
            return copy;
        }
        changes.setParent(parent);
        return this;
    }

    /**
     * Create a copy of this node except using the supplied name.
     * 
     * @param name Sets name to the specified value.
     * @return the new map node; never null
     */
    public MapNode withName( Segment name ) {
        if (changes == null) {
            MapNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setName(name);
            return copy;
        }
        changes.setName(name);
        return this;
    }

    /**
     * Create a copy of this node except adding the supplied node at the end of the existing children.
     * 
     * @param child the UUID of the child that is to be added; may not be null
     * @return the new map node; never null
     */
    public MapNode withChild( UUID child ) {
        assert child != null;
        if (getChildren().indexOf(child) != -1) return this;
        if (changes == null) {
            MapNode copy = clone();
            List<UUID> children = new ArrayList<UUID>(getChildren());
            assert !children.contains(child);
            children.add(child);
            copy.changes = newChanges();
            copy.changes.setChildren(children);
            return copy;
        }
        changes.getChildren(true).add(child);
        return this;
    }

    /**
     * Create a copy of this node except adding the supplied node into the existing children at the specified index.
     * 
     * @param index the index at which the child is to appear
     * @param child the UUID of the child that is to be added at the end of the existing children
     * @return the new map node; never null
     */
    public MapNode withChild( int index,
                              UUID child ) {
        assert child != null;
        assert index >= 0;
        int existingIndex = getChildren().indexOf(child);
        if (existingIndex == index) {
            // No need to add twice, so simply return (have not yet made any changes)
            return this;
        }
        if (changes == null) {
            MapNode copy = clone();
            List<UUID> children = new ArrayList<UUID>(getChildren());
            if (existingIndex >= 0) {
                // The child is moving positions, so remove it before we add it ...
                children.remove(existingIndex);
                if (existingIndex < index) --index;
            }
            children.add(index, child);
            copy.changes = newChanges();
            copy.changes.setChildren(children);
            return copy;
        }
        List<UUID> children = changes.getChildren(true);
        if (existingIndex >= 0) {
            // The child is moving positions, so remove it before we add it ...
            children.remove(existingIndex);
            if (existingIndex < index) --index;
        }
        children.add(index, child);
        return this;
    }

    /**
     * Create a copy of this node except without the supplied child node.
     * 
     * @param child the UUID of the child that is to be removed; may not be null
     * @return the new map node; never null
     */
    public MapNode withoutChild( UUID child ) {
        assert child != null;
        if (changes == null) {
            MapNode copy = clone();
            List<UUID> children = new ArrayList<UUID>(getChildren());
            children.remove(child);
            copy.changes = newChanges();
            copy.changes.setChildren(children);
            return copy;
        }
        changes.getChildren(true).remove(child);
        return this;
    }

    /**
     * Create a copy of this node except with none of the children.
     * 
     * @return the new map node; never null
     */
    public MapNode withoutChildren() {
        if (getChildren().isEmpty()) return this;
        if (changes == null) {
            MapNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setChildren(new ArrayList<UUID>());
            return copy;
        }
        changes.getChildren(true).clear();
        return this;
    }

    /**
     * Create a copy of this node except with the changes to the properties.
     * 
     * @param propertiesToSet the properties that are to be set; may be null if no properties are to be set
     * @param propertiesToRemove the names of the properties that are to be removed; may be null if no properties are to be
     *        removed
     * @param removeAllExisting true if all existing properties should be removed
     * @return the unfrozen map node; never null
     */
    public MapNode withProperties( Iterable<Property> propertiesToSet,
                                   Iterable<Name> propertiesToRemove,
                                   boolean removeAllExisting ) {
        if (propertiesToSet == null && propertiesToRemove == null && !removeAllExisting) {
            // no changes ...
            return this;
        }
        Map<Name, Property> newProperties = null;
        MapNode result = this;
        if (changes == null) {
            MapNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setProperties(new HashMap<Name, Property>(this.properties));
            newProperties = copy.changes.getProperties(true);
            result = copy;
        } else {
            newProperties = changes.getProperties(true);
        }
        if (removeAllExisting) {
            newProperties.clear();
        } else {
            if (propertiesToRemove != null) {
                for (Name name : propertiesToRemove) {
                    if (JcrLexicon.UUID.equals(name) || ModeShapeLexicon.UUID.equals(name)) continue;
                    newProperties.remove(name);
                }
            } else if (propertiesToSet == null) {
                return this;
            }
        }
        if (propertiesToSet != null) {
            for (Property property : propertiesToSet) {
                newProperties.put(property.getName(), property);
            }
        }
        return result;
    }

    /**
     * Create a copy of this node except with the new property.
     * 
     * @param property the property to set
     * @return this map node
     */
    public MapNode withProperty( Property property ) {
        if (property == null) return this;
        if (changes == null) {
            MapNode copy = clone();
            copy.changes = newChanges();
            Map<Name, Property> newProps = new HashMap<Name, Property>(this.properties);
            newProps.put(property.getName(), property);
            copy.changes.setProperties(newProps);
            return copy;
        }
        changes.getProperties(true).put(property.getName(), property);
        return this;
    }

    /**
     * Create a copy of this node except with the new property.
     * 
     * @param propertyName the name of the property that is to be removed
     * @return this map node, or this node if the named properties does not exist on this node
     */
    public MapNode withoutProperty( Name propertyName ) {
        if (propertyName == null || !getProperties().containsKey(propertyName)) return this;
        if (changes == null) {
            MapNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setProperties(new HashMap<Name, Property>(this.properties));
            return copy;
        }
        changes.getProperties(true).remove(propertyName);
        return this;
    }

    public MapNode withoutProperties() {
        if (getProperties().isEmpty()) return this;
        if (changes == null) {
            MapNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setProperties(new HashMap<Name, Property>());
            return copy;
        }
        changes.getProperties(true).clear();
        return this;

    }

    @SuppressWarnings( "synthetic-access" )
    protected class Changes {
        private Segment name;
        private UUID parent;
        private Map<Name, Property> properties;
        private List<UUID> children;

        public Segment getName() {
            return name != null ? name : MapNode.this.name;
        }

        public void setName( Segment name ) {
            this.name = name;
        }

        public UUID getParent() {
            return parent != null ? parent : MapNode.this.parent;
        }

        public void setParent( UUID parent ) {
            this.parent = parent;
        }

        public Map<Name, Property> getProperties( boolean createIfMissing ) {
            if (properties == null) {
                if (createIfMissing) {
                    properties = new HashMap<Name, Property>(MapNode.this.properties);
                    return properties;
                }
                return MapNode.this.properties;
            }
            return properties;
        }

        public Map<Name, Property> getUnmodifiableProperties() {
            return properties != null ? Collections.unmodifiableMap(properties) : MapNode.this.properties;
        }

        public void setProperties( Map<Name, Property> properties ) {
            this.properties = properties;
        }

        public List<UUID> getChildren( boolean createIfMissing ) {
            if (children == null) {
                if (createIfMissing) {
                    children = new ArrayList<UUID>();
                    return children;
                }
                return MapNode.this.children;
            }
            return children;
        }

        public List<UUID> getUnmodifiableChildren() {
            return children != null ? Collections.unmodifiableList(children) : MapNode.this.children;
        }

        public void setChildren( List<UUID> children ) {
            this.children = children;
        }
    }

}
