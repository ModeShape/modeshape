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
package org.modeshape.jcr.cache.change;

import java.util.Map;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * An interface used to signal various kinds of changes.
 */
public interface Changes {

    /**
     * Signal that a new workspace has been added.
     * 
     * @param workspaceName the name of the workspace; may not be null
     */
    void workspaceAdded( String workspaceName );

    /**
     * Signal that a new workspace has been removed.
     * 
     * @param workspaceName the name of the workspace; may not be null
     */
    void workspaceRemoved( String workspaceName );

    /**
     * Signal that a new node was created.
     * 
     * @param key the key for the new node; may not be null
     * @param parentKey the key for the parent of the new node; may not be null
     * @param path the path to the new node; may not be null
     * @param properties the properties in the new node, or null if there are none
     */
    void nodeCreated( NodeKey key,
                      NodeKey parentKey,
                      Path path,
                      Map<Name, Property> properties );

    /**
     * Signal that a node was removed.
     * 
     * @param key the key for the removed node; may not be null
     * @param parentKey the key for the old parent of the removed node; may not be null
     * @param path the path to the removed node; may not be null
     */
    void nodeRemoved( NodeKey key,
                      NodeKey parentKey,
                      Path path );

    /**
     * Signal that a node was renamed (but still has the same parent)
     * 
     * @param key the key for the node; may not be null
     * @param newPath the new path for the node; may not be null
     * @param oldName the old name (including SNS index); may not be null
     */
    void nodeRenamed( NodeKey key,
                      Path newPath,
                      Path.Segment oldName );

    /**
     * Signal that a node was moved from one parent to another, and may have also been renamed.
     * 
     * @param key the key for the node; may not be null
     * @param newParent the new parent for the node; may not be null
     * @param oldParent the old parent for the node; may not be null
     * @param newPath the new path for the node after it has been moved; may not be null
     * @param oldPath the old path for the node before it was moved; may not be null
     */
    void nodeMoved( NodeKey key,
                    NodeKey newParent,
                    NodeKey oldParent,
                    Path newPath,
                    Path oldPath );

    /**
     * Signal that a node was placed into a new location within the same parent.
     * 
     * @param key the key for the node; may not be null
     * @param parent the key for the parent of the node; may not be null
     * @param newPath the new path for the node after it has been reordered; may not be null
     * @param oldPath the old path for the node before it was reordered; may not be null
     * @param reorderedBeforePath the path of the node before which the node was moved; or null if the node was reordered to the
     *        end of the list of children of the parent node
     */
    void nodeReordered( NodeKey key,
                        NodeKey parent,
                        Path newPath,
                        Path oldPath,
                        Path reorderedBeforePath );

    /**
     * Create an event signifying that something about the node (other than the properties or location) changed.
     * 
     * @param key the node key; may not be null
     * @param path the path
     */
    void nodeChanged( NodeKey key,
                      Path path );

    /**
     * Signal that a node was successfully sequenced.
     * 
     * @param sequencedNodeKey the key of the node that was used as input and sequenced; may not be null
     * @param sequencedNodePath the path of the node that was used as input and sequenced; may not be null
     * @param outputNodeKey the key of the top-level node output by the sequencing operation; may not be null
     * @param outputNodePath the path of the top-level node output by the sequencing operation; may not be null
     * @param outputPath the string representation of the output path of the sequencing operation
     * @param userId the username of the session that generated the change that led to the sequencing operation
     * @param selectedPath the string representation of the path that led to the sequencing operation (which may or may not be the
     *        same as the sequenced node path); may not be null
     * @param sequencerName the name of the sequencer; may not be null
     */
    void nodeSequenced( NodeKey sequencedNodeKey,
                        Path sequencedNodePath,
                        NodeKey outputNodeKey,
                        Path outputNodePath,
                        String outputPath,
                        String userId,
                        String selectedPath,
                        String sequencerName );

    /**
     * Signal that a node was not sequenced successfully.
     * 
     * @param sequencedNodeKey the key of the node that was used as input and sequenced; may not be null
     * @param sequencedNodePath the path of the node that was used as input and sequenced; may not be null
     * @param outputPath the string representation of the output path of the sequencing operation
     * @param userId the username of the session that generated the change that led to the (failed) sequencing operation
     * @param selectedPath the string representation of the path that led to the (failed) sequencing operation (which may or may
     *        not be the same as the sequenced node path); may not be null
     * @param sequencerName the name of the sequencer; may not be null
     * @param cause the exception that caused the failure; may not be null
     */
    void nodeSequencingFailure( NodeKey sequencedNodeKey,
                                Path sequencedNodePath,
                                String outputPath,
                                String userId,
                                String selectedPath,
                                String sequencerName,
                                Throwable cause );

    /**
     * Signal that a property was added to a node.
     * 
     * @param key the key of the node that was changed; may not be null
     * @param nodePath the path of the node that was changed
     * @param property the new property, with name and value(s); may not be null
     */
    void propertyAdded( NodeKey key,
                        Path nodePath,
                        Property property );

    /**
     * Signal that a property was removed from a node.
     * 
     * @param key the key of the node that was changed; may not be null
     * @param nodePath the path of the node that was changed
     * @param property the property that was removed, with name and value(s); may not be null
     */
    void propertyRemoved( NodeKey key,
                          Path nodePath,
                          Property property );

    /**
     * Signal that a property was changed on a node.
     * 
     * @param key the key of the node that was changed; may not be null
     * @param nodePath the path of the node that was changed
     * @param newProperty the new property, with name and value(s); may not be null
     * @param oldProperty the old property, with name and value(s); may not be null
     */
    void propertyChanged( NodeKey key,
                          Path nodePath,
                          Property newProperty,
                          Property oldProperty );

    /**
     * Create an event that signals that the (stored) binary value with the supplied key is no longer used.
     * 
     * @param key the key for the now-unused binary value; may not be null
     */
    void binaryValueNoLongerUsed( BinaryKey key );

    /**
     * Create an event that signals that the (stored) binary value with the supplied key is now used.
     * 
     * @param key the key for the now-used binary value; may not be null
     */
    void binaryValueNowUsed( BinaryKey key );
}
