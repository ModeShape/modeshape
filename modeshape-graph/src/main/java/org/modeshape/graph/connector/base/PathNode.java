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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * A {@link Node} implementation used by the path-based connector (see {@link PathWorkspace} and {@link PathTransaction}), which
 * stores all node state in a tree of content, where a specific path exists to each node in the tree.
 * <p>
 * Strictly speaking, this class is not immutable or thread safe. However, the persisted state cannot be changed. Instead, any
 * changes made to the object are stored in a transient area and are made "persistable"via the {@link #freeze()} method.
 * </p>
 * <p>
 * The {@link PathTransaction} maintains an unfrozen, changed instance within it transactional state, and always puts the
 * {@link #freeze() frozen}, read-only representation inside the
 */
public class PathNode implements Node, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /* These members MUST be treated as "final", even though they cannot be to correctly implement Serializable */
    private/*final*/UUID uuid;
    private/*final*/Path parent;
    private/*final*/Segment name;
    private/*final*/Map<Name, Property> properties;
    private/*final*/List<Segment> children;
    private/*final*/int version = 1;

    /** The changes made to this object, making it unfrozen */
    protected transient Changes changes;

    /**
     * Create a new node instance.
     * 
     * @param uuid the UUID of the node; may be null
     * @param parent the path to the parent node; may be null only if the name is null
     * @param name the name of this node, relative to the parent
     * @param properties the unmodifiable map of properties; may be null or empty
     * @param children the unmodifiable list of child segments; may be null or empty
     */
    public PathNode( UUID uuid,
                     Path parent,
                     Segment name,
                     Map<Name, Property> properties,
                     List<Segment> children ) {
        this.uuid = uuid;
        this.parent = parent;
        this.name = name;
        this.properties = properties != null ? properties : Collections.<Name, Property>emptyMap();
        this.children = children != null ? children : Collections.<Segment>emptyList();
        assert this.properties != null;
        assert this.children != null;
        assert this.name != null ? this.parent != null : this.parent == null;
    }

    /**
     * Create a new node instance.
     * 
     * @param uuid the UUID of the node; may be null
     * @param parent the path to the parent node; may be null only if the name is null
     * @param name the name of this node, relative to the parent
     * @param properties the unmodifiable map of properties; may be null or empty
     * @param children the unmodifiable list of child segments; may be null or empty
     * @param version the version number
     */
    protected PathNode( UUID uuid,
                        Path parent,
                        Segment name,
                        Map<Name, Property> properties,
                        List<Segment> children,
                        int version ) {
        this.uuid = uuid;
        this.parent = parent;
        this.name = name;
        this.properties = properties != null ? properties : Collections.<Name, Property>emptyMap();
        this.children = children != null ? children : Collections.<Segment>emptyList();
        this.version = version;
        assert this.properties != null;
        assert this.children != null;
        assert this.name != null ? this.parent != null : this.parent == null;
    }

    /**
     * Create a new node instance.
     * 
     * @param uuid the UUID of the node; may be null
     * @param parent the path to the parent node; may be null only if the name is null
     * @param name the name of this node, relative to the parent
     * @param properties the properties that are to be copied into the new node; may be null or empty
     * @param children the unmodifiable list of child segments; may be null or empty
     */
    public PathNode( UUID uuid,
                     Path parent,
                     Segment name,
                     Iterable<Property> properties,
                     List<Segment> children ) {
        this.uuid = uuid;
        this.parent = parent;
        this.name = name;
        if (properties != null) {
            Map<Name, Property> props = new HashMap<Name, Property>();
            for (Property prop : properties) {
                props.put(prop.getName(), prop);
            }
            this.properties = props.isEmpty() ? Collections.<Name, Property>emptyMap() : Collections.unmodifiableMap(props);
        } else {
            this.properties = Collections.emptyMap();
        }
        this.children = children != null ? children : Collections.<Segment>emptyList();
        assert this.properties != null;
        assert this.children != null;
        assert this.name != null ? this.parent != null : this.parent == null;
    }

    /**
     * Create a root node with the supplied UUID.
     * 
     * @param uuid the UUID of the root node; may not be null
     */
    public PathNode( UUID uuid ) {
        this.uuid = uuid;
        this.parent = null;
        this.name = null;
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
     * @return parent
     */
    public Path getParent() {
        return changes != null ? changes.getParent() : parent;
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
     * @return children
     */
    public List<Segment> getChildren() {
        return changes != null ? changes.getChildren(false) : children;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((children == null) ? 0 : children.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PathNode other = (PathNode)obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (parent == null) {
            if (other.parent != null) return false;
        } else if (!parent.equals(other.parent)) return false;
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.parent == null) {
            sb.append("/");
        } else {
            sb.append(this.getParent()).append("/").append(this.getName());
        }
        sb.append(" (");
        sb.append(this.getUuid()).append(")");
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
    public PathNode clone() {
        return new PathNode(uuid, parent, name, new HashMap<Name, Property>(properties), new ArrayList<Segment>(children));
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
    public PathNode freeze() {
        if (!hasChanges()) return this;
        return new PathNode(uuid, changes.getParent(), changes.getName(), changes.getUnmodifiableProperties(),
                            changes.getUnmodifiableChildren(), version + 1);
    }

    /**
     * Create a copy of this node except using the supplied path.
     * 
     * @param parent sets parent to the specified value.
     * @return the new path node; never null
     */
    public PathNode withParent( Path parent ) {
        if (changes == null) {
            PathNode copy = clone();
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
     * @param name sets name to the specified value.
     * @return the new path node; never null
     */
    public PathNode withName( Segment name ) {
        if (changes == null) {
            PathNode copy = clone();
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
     * @param child the segment of the child that is to be added; may not be null
     * @return the new path node; never null
     */
    public PathNode withChild( Segment child ) {
        assert child != null;
        if (getChildren().indexOf(child) != -1) return this;
        if (changes == null) {
            PathNode copy = clone();
            List<Segment> children = new LinkedList<Segment>(getChildren());
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
     * @param child the segment of the child that is to be added at the end of the existing children
     * @return the new path node; never null
     */
    public PathNode withChild( int index,
                               Segment child ) {
        assert child != null;
        assert index >= 0;
        int existingIndex = getChildren().indexOf(child);
        if (existingIndex == index) {
            // No need to add twice, so simply return (have not yet made any changes)
            return this;
        }
        if (changes == null) {
            PathNode copy = clone();
            List<Segment> children = new LinkedList<Segment>(getChildren());
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
        List<Segment> children = changes.getChildren(true);
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
     * @param child the segment of the child that is to be removed; may not be null
     * @return the new path node; never null
     */
    public PathNode withoutChild( Segment child ) {
        assert child != null;
        if (changes == null) {
            PathNode copy = clone();
            List<Segment> children = new LinkedList<Segment>(getChildren());
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
     * @return the new path node; never null
     */
    public PathNode withoutChildren() {
        if (getChildren().isEmpty()) return this;
        if (changes == null) {
            PathNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setChildren(new LinkedList<Segment>());
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
     * @return the unfrozen path node; never null
     */
    public PathNode withProperties( Iterable<Property> propertiesToSet,
                                    Iterable<Name> propertiesToRemove,
                                    boolean removeAllExisting ) {
        if (propertiesToSet == null && propertiesToRemove == null && !removeAllExisting) {
            // no changes ...
            return this;
        }
        Map<Name, Property> newProperties = null;
        PathNode result = this;
        if (changes == null) {
            PathNode copy = clone();
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
     * @return this path node
     */
    public PathNode withProperty( Property property ) {
        if (property == null) return this;
        if (changes == null) {
            PathNode copy = clone();
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
     * Create a copy of this node except without the new property.
     * 
     * @param propertyName the name of the property that is to be removed
     * @return this path node, or this node if the named properties does not exist on this node
     */
    public PathNode withoutProperty( Name propertyName ) {
        if (propertyName == null || !getProperties().containsKey(propertyName)) return this;
        if (changes == null) {
            PathNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setProperties(new HashMap<Name, Property>(this.properties));
            return copy;
        }
        changes.getProperties(true).remove(propertyName);
        return this;
    }

    /**
     * Create a copy of this node without any properties
     * 
     * @return this path node, or this node if this node has no properties
     */
    public PathNode withoutProperties() {
        if (getProperties().isEmpty()) return this;
        if (changes == null) {
            PathNode copy = clone();
            copy.changes = newChanges();
            copy.changes.setProperties(new HashMap<Name, Property>());
            return copy;
        }
        changes.getProperties(true).clear();
        return this;

    }

    @SuppressWarnings( "synthetic-access" )
    protected class Changes {
        private Path parent;
        private Segment name;
        private Map<Name, Property> properties;
        private List<Segment> children;

        public Path getParent() {
            return parent != null ? parent : PathNode.this.parent;
        }

        public void setParent( Path parent ) {
            this.parent = parent;
        }

        public Segment getName() {
            return name != null ? name : PathNode.this.name;
        }

        public void setName( Segment name ) {
            this.name = name;
        }

        public Map<Name, Property> getProperties( boolean createIfMissing ) {
            if (properties == null) {
                if (createIfMissing) {
                    properties = new HashMap<Name, Property>(PathNode.this.properties);
                    return properties;
                }
                return PathNode.this.properties;
            }
            return properties;
        }

        public Map<Name, Property> getUnmodifiableProperties() {
            return properties != null ? Collections.unmodifiableMap(properties) : PathNode.this.properties;
        }

        public void setProperties( Map<Name, Property> properties ) {
            this.properties = properties;
        }

        public List<Segment> getChildren( boolean createIfMissing ) {
            if (children == null) {
                if (createIfMissing) {
                    children = new LinkedList<Segment>();
                    return children;
                }
                return PathNode.this.children;
            }
            return children;
        }

        public List<Segment> getUnmodifiableChildren() {
            return children != null ? Collections.unmodifiableList(children) : PathNode.this.children;
        }

        public void setChildren( List<Segment> children ) {
            this.children = children;
        }
    }

}
