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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

/**
 * A representation of a node as stored within the cache.
 */
public interface CachedNode {

    public enum ReferenceType {
        STRONG,
        WEAK,
        BOTH;
    }

    /**
     * Get the key for the node.
     * 
     * @return the node's key; never null
     */
    NodeKey getKey();

    /**
     * Get the name for this node, without any same-name-sibiling (SNS) index.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the name; never null, but the root node will have a zero-length name
     * @see #getSegment(NodeCache)
     * @see #getPath(NodeCache)
     */
    Name getName( NodeCache cache );

    /**
     * Get the path segment for this node. The segment consists of a name and a same-name-sibling (SNS) index.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the segment; never null, but the root node will have a zero-length name
     * @see #getName(NodeCache)
     * @see #getPath(NodeCache)
     */
    Segment getSegment( NodeCache cache );

    /**
     * Get the path to this node.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the node's path; never null with at least one segment for all nodes except the root node
     * @see #getName(NodeCache)
     * @see #getSegment(NodeCache)
     * @throws NodeNotFoundException if this node does not exist anymore
     */
    Path getPath( NodeCache cache ) throws NodeNotFoundException;

    /**
     * Get the node key for this node's primary parent.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the parent's key; null if this is the root node or it has been removed from the document by someone else
     */
    NodeKey getParentKey( NodeCache cache );

    /**
     * Get the keys for all of the nodes (other than the {@link #getParentKey(NodeCache) primary parent}) under which this node
     * appears.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the immutable set of keys to the additional parents, excluding the primary parent
     */
    Set<NodeKey> getAdditionalParentKeys( NodeCache cache );

    /**
     * Get the primary type for this node.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the primary type name; never null
     */
    Name getPrimaryType( NodeCache cache );

    /**
     * Get the set of mixin types for this node.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the immutable list of mixin type names; never null but possibly empty
     */
    Set<Name> getMixinTypes( NodeCache cache );

    /**
     * Determine the number of properties that this node contains.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the number of properties; never negative
     */
    int getPropertyCount( NodeCache cache );

    /**
     * Determine if the node contains one or more properties.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return true if the node has at least one property, or false otherwise
     */
    boolean hasProperties( NodeCache cache );

    /**
     * Determine if the node contains a property with the specified name.
     * 
     * @param name the name of the property
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return true if the node has the named property, or false otherwise
     */
    boolean hasProperty( Name name,
                         NodeCache cache );

    /**
     * Get the property with the given name.
     * 
     * @param name the name of the property
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the property, or null if the property does not exist on this node
     * @throws NodeNotFoundException if this node no longer exists
     */
    Property getProperty( Name name,
                          NodeCache cache );

    /**
     * Get an iterator over all of the node's properties.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the property iterator; never null but possibly empty
     */
    Iterator<Property> getProperties( NodeCache cache );

    /**
     * Get an iterator over all of the properties of this node that have names matching at least one of the supplied patterns.
     * 
     * @param namePatterns the regex patterns or string literals describing the names
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the property iterator; never null but possibly empty
     */
    Iterator<Property> getProperties( Collection<?> namePatterns,
                                      NodeCache cache );

    /**
     * Get the set of child references for this node. Note that each child reference will need to be resolved by the caller.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the representation of the children of this node; never null but possibly empty
     */
    ChildReferences getChildReferences( NodeCache cache );

    /**
     * Get the keys of the nodes that have JCR REFERENCE and/or WEAK_REFERENCE properties pointing to this node.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @param type the flag specifying whether nodes with REFERENCE properties and/or WEAK reference properties should be included
     *        in the result; may not be null
     * @return the set of keys to the nodes that have a reference to this node; never null but possibly empty
     */
    Set<NodeKey> getReferrers( NodeCache cache,
                               ReferenceType type );
}
