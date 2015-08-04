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
package org.modeshape.jcr.spi.federation;

import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * This interface represents an atomic set of changes that have occurred to resources in the remit of a {@link Connector}.
 * <p>
 * Note that ModeShape will correctly generate events for all changes to the external system <i>initiated by ModeShape</i>, and
 * therefore the connector should not record any such changes. However, if the external system undergoes changes due to
 * non-ModeShape activities, such changes can/should be captured by the connector so that ModeShape and its clients are notified
 * of these changes. Therefore, the recording of changes will likely be through an asynchronous activity.
 * </p>
 * <p>
 * When an external system undergoes some independent changes, the connector can {@link Connector#newConnectorChangedSet() obtain}
 * a new change set, call methods on the change set to record events of interest in the property order, and then
 * {@link #publish(Map)} all of the changes that have been recorded. Only when a change set is {@link #publish(Map) published}
 * will the repository be notified of the recorded changes and forward them to its appropriate listeners.
 * </p>
 * <p>
 * Connectors may expose a single document at multiple paths. In such cases the Connector will likely generate events for only one
 * of those paths (i.e., the "primary" path, whatever the Connector chooses to use). However, Connectors may choose to generate
 * mutliple events for single resources available at multiple paths.
 * </p>
 * <p>
 * It is safe to only call methods on a {@link ConnectorChangeSet} instance from a single thread. If a connector uses multiple
 * threads to record changes, the connector should either use separate ConnectorChangeSet instances for each thread or (if the
 * changes from multiple threads are to be recorded as a single atomic set) ensure that it calls the methods in a synchronized
 * fashion.
 * </p>
 * 
 * @author ajs6f
 * @author rhauch@redhat.com
 * @see Connector#newConnectorChangedSet()
 */
@NotThreadSafe
public interface ConnectorChangeSet {

    /**
     * Signal that a new node resource was created.
     * @param docId the connector's identifier for the new node; may not be null
     * @param parentDocId the connector's identifier for the parent of the new node; may not be null
     * @param path the path to the new node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param properties the properties in the new node, or null if there are none
     */
    void nodeCreated( String docId,
                      String parentDocId,
                      String path,
                      Name primaryType,
                      Set<Name> mixinTypes,
                      Map<Name, Property> properties );

    /**
     * Signal that a node resource (and all descendants) was removed. Note that it is not common to fire an event for all nodes
     * below a node that is also deleted within the same change set.
     * 
     * @param docId the connector's identifier for the removed node; may not be null
     * @param parentDocId the connector's identifier for the parent of the removed node; may not be null
     * @param path the path to the removed node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param parentPrimaryType the primary type of the parent node; may be null if the information is not available.
     * @param parentMixinTypes the mixin types of the parent node; may be null if the information is not available 
     */
    void nodeRemoved( String docId,
                      String parentDocId,
                      String path,
                      Name primaryType,
                      Set<Name> mixinTypes,
                      Name parentPrimaryType,
                      Set<Name> parentMixinTypes );

    /**
     * Signal that a node resource (and all descendants) was moved from one parent to another.
     * 
     * @param docId the connector's identifier for the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param newParentDocId the connector's identifier for the new parent of the node; may not be null
     * @param oldParentDocId the connector's identifier for the old parent for the node; may not be null
     * @param newPath the new path for the node after it has been moved; may not be null
     * @param oldPath the old path for the node before it was moved; may not be null
     */
    void nodeMoved( String docId,
                    Name primaryType,
                    Set<Name> mixinTypes,
                    String newParentDocId,
                    String oldParentDocId,
                    String newPath,
                    String oldPath );

    /**
     * Signal that a node resource (and all descendants) was placed into a new location within the same parent.
     *
     * @param docId the connector's identifier for the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types of the node; may not be null
     * @param parentDocId the connector's identifier for the parent of the node; may not be null
     * @param newPath the new path for the node after it has been reordered; may not be null
     * @param oldNameSegment the name segment (i.e., the name and if applicable the SNS index) for the node before it was
     *        reordered; may not be null
     * @param reorderedBeforeNameSegment the name segment of the node (in the same parent) before which the node was moved
     */
    void nodeReordered( String docId,
                        Name primaryType,
                        Set<Name> mixinTypes,
                        String parentDocId,
                        String newPath,
                        String oldNameSegment,
                        String reorderedBeforeNameSegment );

    /**
     * Signal that a property was added to a node resource.
     * 
     * @param docId the connector's identifier for the node; may not be null
     * @param nodePrimaryType the primary type of the node; may not be null
     * @param nodeMixinTypes the mixin types of the node; may not be null
     * @param nodePath the path of the node that was changed
     * @param property the new property, with name and value(s); may not be null
     */
    void propertyAdded( String docId,
                        Name nodePrimaryType,
                        Set<Name> nodeMixinTypes,
                        String nodePath,
                        Property property );

    /**
     * Signal that a property was removed from a node resource.
     * 
     * @param docId the connector's identifier for the node; may not be null
     * @param nodePrimaryType the primary type of the node; may not be null
     * @param nodeMixinTypes the mixin types of the node; may not be null
     * @param nodePath the path of the node that was changed
     * @param property the property that was removed, with name and value(s); may not be null
     */
    void propertyRemoved( String docId,
                          Name nodePrimaryType,
                          Set<Name> nodeMixinTypes,
                          String nodePath,
                          Property property );

    /**
     * Signal that a property resource was changed on a node resource.
     * 
     * @param docId the connector's identifier for the node; may not be null
     * @param nodePrimaryType the primary type of the node; may not be null
     * @param nodeMixinTypes the mixin types of the node; may not be null
     * @param nodePath the path of the node that was changed
     * @param oldProperty the old property, with name and value(s); may be null
     * @param newProperty the new property, with name and value(s); may not be null
     */
    void propertyChanged( String docId,
                          Name nodePrimaryType,
                          Set<Name> nodeMixinTypes,
                          String nodePath,
                          Property oldProperty,
                          Property newProperty );

    /**
     * Finish the construction of this change-set and make it available for publication into the repository. This also empties the
     * record of change events and prepares to accept a new record.
     * <p>
     * Once a change set has been published, it may not be used again.
     * </p>
     * 
     * @param data the name-value pairs that may be associated with the set of changes; may be null or empty
     */
    void publish( Map<String, String> data );
}
