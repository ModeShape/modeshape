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
package org.modeshape.jcr.cache.change;

import java.util.Map;
import java.util.Set;
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
     * Signal that the repository metadata has changed.
     */
    void repositoryMetadataChanged();

    /**
     * Signal that a new node was created.
     * @param key the key for the new node; may not be null
     * @param parentKey the key for the parent of the new node; may not be null
     * @param path the path to the new node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param properties the properties in the new node, or null if there are none
     */
    void nodeCreated( NodeKey key,
                      NodeKey parentKey,
                      Path path,
                      Name primaryType,
                      Set<Name> mixinTypes,
                      Map<Name, Property> properties );

    /**
     * Signal that a node was removed.
     * @param key the key for the removed node; may not be null
     * @param parentKey the key for the old parent of the removed node; may not be null
     * @param path the path to the removed node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param parentPrimaryType the primary type of the parent of the node; may be null if this information is not available
     * @param parentMixinTypes the mixin types of the parent of the node; may be null if this information is not available
     */
    void nodeRemoved( NodeKey key,
                      NodeKey parentKey,
                      Path path,
                      Name primaryType,
                      Set<Name> mixinTypes,
                      Name parentPrimaryType,
                      Set<Name> parentMixinTypes );

    /**
     * Signal that a node was renamed (but still has the same parent)
     * @param key the key for the node; may not be null
     * @param newPath the new path for the node; may not be null
     * @param oldName the old name (including SNS index); may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     */
    void nodeRenamed( NodeKey key,
                      Path newPath,
                      Path.Segment oldName,
                      Name primaryType,
                      Set<Name> mixinTypes );

    /**
     * Signal that a node was moved from one parent to another, and may have also been renamed.
     * @param key the key for the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param newParent the new parent for the node; may not be null
     * @param oldParent the old parent for the node; may not be null
     * @param newPath the new path for the node after it has been moved; may not be null
     * @param oldPath the old path for the node before it was moved; may not be null
     */
    void nodeMoved( NodeKey key,
                    Name primaryType,
                    Set<Name> mixinTypes,
                    NodeKey newParent,
                    NodeKey oldParent,
                    Path newPath,
                    Path oldPath );

    /**
     * Signal that a node was placed into a new location within the same parent.
     *
     * @param key the key for the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param parent the key for the parent of the node; may not be null
     * @param newPath the new path for the node after it has been reordered; may not be null
     * @param oldPath the old path for the node before it was reordered; may be null in the case of transient reorderings
     * @param reorderedBeforePath the path of the node before which the node was moved; or null if the node was reordered to the
     */
    void nodeReordered( NodeKey key,
                        Name primaryType,
                        Set<Name> mixinTypes,
                        NodeKey parent,
                        Path newPath,
                        Path oldPath,
                        Path reorderedBeforePath );

    /**
     * Create an event signifying that something about the node (other than the properties or location) changed.
     * @param key the node key; may not be null
     * @param path the path
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     */
    void nodeChanged( NodeKey key,
                      Path path,
                      Name primaryType,
                      Set<Name> mixinTypes );

    /**
     * Signal that a node was successfully sequenced.
     *
     * @param sequencedNodeKey the key of the node that was used as input and sequenced; may not be null
     * @param sequencedNodePath the path of the node that was used as input and sequenced; may not be null
     * @param sequencedNodePrimaryType the primary type of the node that was used as input and sequenced; may not be null
     * @param sequencedNodeMixinTypes the mixin types of the node that was used as input and sequenced; may not be null
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
                        Name sequencedNodePrimaryType,
                        Set<Name> sequencedNodeMixinTypes,
                        NodeKey outputNodeKey,
                        Path outputNodePath,
                        String outputPath,
                        String userId,
                        String selectedPath,
                        String sequencerName );

    /**
     * Signal that a node was not sequenced successfully.
     * @param sequencedNodeKey the key of the node that was used as input and sequenced; may not be null
     * @param sequencedNodePath the path of the node that was used as input and sequenced; may not be null
     * @param sequencedNodePrimaryType the primary type of the node that was used as input and sequenced; may not be null
     * @param sequencedNodeMixinTypes the mixin types of the node that was used as input and sequenced; may not be null
     * @param outputPath the string representation of the output path of the sequencing operation
     * @param userId the username of the session that generated the change that led to the (failed) sequencing operation
     * @param selectedPath the string representation of the path that led to the (failed) sequencing operation (which may or may
     *        not be the same as the sequenced node path); may not be null
     * @param sequencerName the name of the sequencer; may not be null
     * @param cause the exception that caused the failure; may not be null
     */
    void nodeSequencingFailure( NodeKey sequencedNodeKey,
                                Path sequencedNodePath,
                                Name sequencedNodePrimaryType,
                                Set<Name> sequencedNodeMixinTypes,
                                String outputPath,
                                String userId,
                                String selectedPath,
                                String sequencerName,
                                Throwable cause );

    /**
     * Signal that a property was added to a node.
     * @param key the key of the node that was changed; may not be null
     * @param nodePrimaryType the primary type of the node; may not be null
     * @param nodeMixinTypes the mixin types of the node; may not be null
     * @param nodePath the path of the node that was changed
     * @param property the new property, with name and value(s); may not be null
     */
    void propertyAdded( NodeKey key,
                        Name nodePrimaryType,
                        Set<Name> nodeMixinTypes,
                        Path nodePath,
                        Property property );

    /**
     * Signal that a property was removed from a node.
     * @param key the key of the node that was changed; may not be null
     * @param nodePrimaryType the primary type of the node; may not be null
     * @param nodeMixinTypes the mixin types of the node; may not be null
     * @param nodePath the path of the node that was changed
     * @param property the property that was removed, with name and value(s); may not be null
     */
    void propertyRemoved( NodeKey key,
                          Name nodePrimaryType,
                          Set<Name> nodeMixinTypes,
                          Path nodePath,
                          Property property );

    /**
     * Signal that a property was changed on a node.
     * @param key the key of the node that was changed; may not be null
     * @param nodePrimaryType the primary type of the node; may not be null
     * @param nodeMixinTypes the mixin types of the node; may not be null
     * @param nodePath the path of the node that was changed
     * @param newProperty the new property, with name and value(s); may not be null
     * @param oldProperty the old property, with name and value(s); may not be null
     */
    void propertyChanged( NodeKey key,
                          Name nodePrimaryType,
                          Set<Name> nodeMixinTypes,
                          Path nodePath,
                          Property newProperty,
                          Property oldProperty );

    /**
     * Signal that the binary with the given key is not longer used by any properties.
     *
     * @param key a {@link org.modeshape.jcr.value.BinaryKey} instance; never {@code null}
     */
    void binaryValueNoLongerUsed( BinaryKey key );

    /**
     * Signal that the binary with the given key is being used by some properties.
     *
     * @param key a {@link org.modeshape.jcr.value.BinaryKey} instance; never {@code null}
     */
    void binaryValueUsed(BinaryKey key);
}
