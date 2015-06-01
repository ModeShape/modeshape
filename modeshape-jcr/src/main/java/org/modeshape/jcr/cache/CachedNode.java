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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
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

    public interface Properties extends Iterable<Property> {
        Property getProperty( Name name );
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
     * @throws NodeNotFoundInParentException if this node no longer exists inside the parent node (and perhaps in no other parent)
     * @throws NodeNotFoundException if this node no longer exists
     * @see #getSegment(NodeCache)
     * @see #getPath(NodeCache)
     */
    Name getName( NodeCache cache );

    /**
     * Get the path segment for this node. The segment consists of a name and a same-name-sibling (SNS) index.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the segment; never null, but the root node will have a zero-length name
     * @throws NodeNotFoundInParentException if this node no longer exists inside the parent node (and perhaps in no other parent)
     * @throws NodeNotFoundException if this node no longer exists
     * @see #getName(NodeCache)
     * @see #getPath(NodeCache)
     */
    Segment getSegment( NodeCache cache );

    /**
     * Get the path to this node.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the node's path; never null with at least one segment for all nodes except the root node
     * @throws NodeNotFoundInParentException if this node no longer exists inside the parent node (and perhaps in no other parent)
     * @throws NodeNotFoundException if this node no longer exists
     * @see #getName(NodeCache)
     * @see #getSegment(NodeCache)
     */
    Path getPath( NodeCache cache ) throws NodeNotFoundException;

    /**
     * Get the path to this node.
     * 
     * @param pathCache the cache of paths that can be used to compute the path for any node; may not be null
     * @return the node's path; never null with at least one segment for all nodes except the root node
     * @throws NodeNotFoundInParentException if this node no longer exists inside the parent node (and perhaps in no other parent)
     * @throws NodeNotFoundException if this node no longer exists
     * @see #getName(NodeCache)
     * @see #getSegment(NodeCache)
     */
    Path getPath( PathCache pathCache ) throws NodeNotFoundException;

    /**
     * Get the depth of this node. The depth is equivalent to the number of segments in the node's path (0 for the root node, 1
     * for "{@code /foo}", 2 for "{@code /foo/bar}", etc.), although this method will likely compute the depth more efficiently
     * that finding the path and asking for the number of segments.
     * <p>
     * The depth is calculated based upon the primary parent (and {@link #getPath(NodeCache) primary path}). This is, although
     * shared nodes are accessible at multiple paths, only the primary path is used to determine the depth of the node.
     * </p>
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the depth of this node; 0 for the root, or a positive number for all other nodes
     * @throws NodeNotFoundException if this node no longer exists
     */
    int getDepth( NodeCache cache ) throws NodeNotFoundException;

    /**
     * Get the node key for this node's primary parent within this workspace.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the parent's key; null if this is the root node or it has been removed from the document by someone else
     */
    NodeKey getParentKey( NodeCache cache );

    /**
     * Get the node key for this node's primary parent in any workspace.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the parent's key; null if this is the root node or it has been removed from the document by someone else
     */
    NodeKey getParentKeyInAnyWorkspace( NodeCache cache );

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
     * Get the properties collection.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the properties; never null but possibly empty
     */
    Properties getPropertiesByName( NodeCache cache );

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

    /**
     * Get a snapshot of the referrers that have REFERENCE and/or WEAK_REFERENCE properties pointing to this node.
     *
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the snapshot, or null if there is none
     */
    ReferrerCounts getReferrerCounts( NodeCache cache );

    /**
     * Determine if this node is effectively at or below the supplied path. Note that because of
     * {@link #getAdditionalParentKeys(NodeCache) additional parents}, a node has multiple effective paths.
     *
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @param path the path to be used for comparison; may not be null
     * @return true if this node can be considered at or below the supplied path; or false otherwise
     */
    boolean isAtOrBelow( NodeCache cache,
                         Path path );

    /**
     * Determine if this node and *all* of its children should be taken into account when searching/indexing or not. 
     * A node which should be excluded from search will be completely ignored when indexing, together with all its children.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return {@code true} if the node should be indexed, {@code false} otherwise
     */
    boolean isExcludedFromSearch( NodeCache cache );

    /**
     * Determine if there is an access control list for this node only, not taking into account any possible parents.
     *
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return {@code true} if this node has an ACL, {@code false} otherwise
     */
    boolean hasACL( NodeCache cache );

    /**
     * Gets the map of privileges by principal name which are in effect for this node. This will not look
     * at any of the parent nodes.
     *
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return a {@code Map} which contains a set of permission names keyed by principal name or {@code null} if no privileges
     * are in effect for this node(i.e. this node does not have an ACL). An empty {@code Map} means a node which has an ACL, but
     * does not have any permissions.
     */
    Map<String, Set<String>> getPermissions(NodeCache cache);

    /**
     * Determine if this node belongs to an external source (via federation) or is local to the repository.
     *
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return {@code true} if the node is local, {@code false} otherwise.
     */
    boolean isExternal(NodeCache cache );
}
