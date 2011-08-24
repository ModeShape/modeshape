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
     * Return whether this node has changes.
     * 
     * @return true if this node has changes, or false otherwise
     */
    boolean hasChanges();

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
     * Set the properties on this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param properties the properties to be set; may not be null
     * @throws NodeNotFoundException if this node no longer exists
     */
    void setProperties( SessionCache cache,
                        Iterable<Property> properties );

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
     * Create a new node as a child of this node with the supplied name and properties.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param key the key for the new node; may not be null
     * @param name the name for the new node; may not be null
     * @param firstProperty the first property; may not be null
     * @param additionalProperties the properties that should be set on the node; may be null or empty
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
     * @param key the key for the new node; may not be null
     * @param name the name for the new node; may not be null
     * @param properties the properties that should be set on the node; may be null or empty
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
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param childKey the key for the child that is to be removed; may not be null
     * @param name the name for the (linked) node, or null if the existing name is to be used
     * @throws NodeNotFoundException if the node does not exist
     */
    void linkChild( SessionCache cache,
                    NodeKey childKey,
                    Name name );

    /**
     * Remove the node from being a child of this node and append it as a child of the supplied node.
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
     * @param referrerKey the key for the node that has a new reference to this node; may not be null
     * @param type the reference type; may not be null
     */
    void addReferrer( SessionCache cache,
                      NodeKey referrerKey,
                      ReferenceType type );

    /**
     * Remove from this node a reference with the given type from the node with the supplied key to this node.
     * 
     * @param cache the cache to which this node belongs; may not be null
     * @param referrerKey the key for the node that no longer has a reference to this node; may not be null
     * @param type the reference type; may not be null
     */
    void removeReferrer( SessionCache cache,
                         NodeKey referrerKey,
                         ReferenceType type );

}
