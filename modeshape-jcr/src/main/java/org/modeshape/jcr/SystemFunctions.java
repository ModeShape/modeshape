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
package org.modeshape.jcr;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.ReadPropertyRequest;
import org.modeshape.graph.request.RequestBuilder;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.function.Function;
import org.modeshape.graph.request.function.FunctionContext;

/**
 * A set of {@link Function} implementations that can be used with the {@link Graph Graph API}.
 */
public class SystemFunctions {

    /**
     * An shared instance of the immutable {@link InitializeVersionHistoryFunction}.
     */
    public static final Function INITIALIZE_VERSION_HISTORY = new InitializeVersionHistoryFunction();

    /**
     * An shared instance of the immutable {@link CreateVersionNodeFunction}.
     */
    public static final Function CREATE_VERSION_NODE = new CreateVersionNodeFunction();

    /**
     * An abstract {@link Function} subclass that contains some helper methods related to version storage and version history.
     */
    @Immutable
    public static abstract class VersionHistoryFunction extends Function {
        private static final long serialVersionUID = 1L;

        /**
         * Verify whether the version history exists within version storage, and if so return its actual {@link Location}. If a
         * possible UUID is provided, this method first looks for a version history node with the specified UUID at the specified
         * path; if none is found, then this method looks for a version history at the specified path (ignoring the supplied
         * UUID).
         * 
         * @param versionHistoryPath the path to the version history node; may not be null
         * @param possibleVersionHistoryUuid the possible UUID for the existing version history node; may be null if this is not
         *        known
         * @param system the {@link RequestBuilder builder} for creating and executing requests against the connector; may not be
         *        null
         * @param workspace the name of the system workspace
         * @return the actual Location of the version history node, or null if no such version history node yet exists
         */
        protected Location versionHistoryLocationFor( Path versionHistoryPath,
                                                      UUID possibleVersionHistoryUuid,
                                                      RequestBuilder system,
                                                      String workspace ) {
            assert versionHistoryPath != null;
            if (possibleVersionHistoryUuid != null) {
                // Try with the UUID first ...
                VerifyNodeExistsRequest verify = system.verifyNodeExists(Location.create(possibleVersionHistoryUuid), workspace);
                if (verify.exists()) {
                    // Found a node with that UUID, but it may be used for another node. So check if the path matches the supplied
                    // version history path ...
                    Location actual = verify.getActualLocationOfNode();
                    if (versionHistoryPath.equals(actual.getPath())) {
                        return actual;
                    }
                    // The path doesn't match, so we can't use the requested UUID ...
                }
            }
            VerifyNodeExistsRequest verify = system.verifyNodeExists(Location.create(versionHistoryPath), workspace);
            return verify.exists() ? verify.getActualLocationOfNode() : null;
        }

        /**
         * A utility method that will create a new {@link Property} instance if there is not already an existing single-valued
         * property with the supplied name and expected values.
         * 
         * @param properties the existing properties, keyed by name; may not be null
         * @param name the name of the property; may not be null
         * @param expectedValue the expected value of the property; may not be null
         * @param propFactory the factory for creating new Property instances; may not be null
         * @param valueFactory the factory for transforming the existing value into an object that can be compared to the expected
         *        value
         * @return the new Property if the expected value differs from the existing value (or if there is no existing property),
         *         or null if the existing property already has the expected value
         */
        protected Property createPropertyIfDifferent( Map<Name, Property> properties,
                                                      Name name,
                                                      Object expectedValue,
                                                      PropertyFactory propFactory,
                                                      ValueFactory<?> valueFactory ) {
            Property existingProp = properties.get(name);
            if (existingProp != null) {
                Object actualValue = valueFactory.create(existingProp.getFirstValue());
                if (!actualValue.equals(expectedValue)) {
                    return propFactory.create(name, expectedValue);
                }
                return null;
            }
            return propFactory.create(name, expectedValue);
        }

        /**
         * Create and initialize the version history structure for a versionable node with the supplied UUID. This method assumes
         * that the version history node does not exist.
         * <p>
         * The initial version history structure for the versioned node with UUID "e41075cb-a09a-4910-87b1-90ce8b4ca9dd" is as
         * follows:
         * 
         * <pre>
         *  + jcr:system
         *    + jcr:versionStorage   {jcr:primaryType = mode:versionStorage}
         *      + e4   {jcr:primaryType = mode:versionHistoryFolder}
         *        + 10   {jcr:primaryType = mode:versionHistoryFolder}
         *          + 75   {jcr:primaryType = mode:versionHistoryFolder}
         *            + cb   {jcr:primaryType = nt:versionHistory}
         *              + a09a-4910-87b1-90ce8b4ca9dd  {jcr:primaryType = nt:versionHistory}
         *                + jcr:versionLabels  {jcr:primaryType = nt:versionLabels}
         *                + jcr:rootVersion  {jcr:primaryType = nt:version}
         *                  - jcr:uuid = ...
         *                  - jcr:created = ...
         *                  + jcr:frozenNode  {jcr:primaryType = nt:frozenNode}
         *                    - jcr:frozenUuid
         *                    - jcr:frozenPrimaryType
         *                    - jcr:frozenMixinTypes
         * </pre>
         * 
         * Note that the path between "/jcr:system/jcr:versionStorage" and the "nt:versionHistory" node is shown as being
         * {@link JcrVersionManager.HiearchicalPathAlgorithm hiearchical}, but it is also possible to be
         * {@link JcrVersionManager.FlatPathAlgorithm flat} (which is less efficient and generally slower performing:
         * 
         * <pre>
         *  + jcr:system
         *    + jcr:versionStorage   {jcr:primaryType = mode:versionStorage}
         *      + e41075cb-a09a-4910-87b1-90ce8b4ca9dd  {jcr:primaryType = nt:versionHistory}
         *        + jcr:versionLabels  {jcr:primaryType = nt:versionLabels}
         *        + jcr:rootVersion  {jcr:primaryType = nt:version}
         *          - jcr:uuid = ...
         *          - jcr:created = ...
         *          + jcr:frozenNode  {jcr:primaryType = nt:frozenNode}
         *            - jcr:frozenUuid
         *            - jcr:frozenPrimaryType
         *            - jcr:frozenMixinTypes
         * </pre>
         * 
         * The flat structure was used originally and was less efficient and slower performing with larger numbers of versionable
         * nodes. The actual structure is set with the {@link JcrRepository.Option#VERSION_HISTORY_STRUCTURE} repository option,
         * but now defaults to {@link JcrRepository.DefaultOption#VERSION_HISTORY_STRUCTURE hierarchical}.
         * </p>
         * 
         * @param versionableNodeUuid the UUID of the versionable node for which the history is to be created; may not be null
         * @param primaryTypeName the name of the primary type of the versionable node; may not be null
         * @param mixinTypeNames the names of the mixin types for the versionable node; may be null or empty
         * @param versionHistoryLocation the path and UUID of the version history node; may not be null
         * @param originalVersionUuid the UUID of the original node from which the new versionable node was copied; may be null
         * @param rootVersionUuid the UUID of the "jcr:rootVersion" node in the version history; may not be null
         * @param context the execution context; may not be null
         * @param system the {@link RequestBuilder} that can be used to create and execute additional requests; may not be null
         * @param workspace the name of the workspace; may not be null
         * @param now the current date time; may not be null
         * @return the path of the highest node that was modified or created by this method
         */
        protected Path initializeVersionStorage( UUID versionableNodeUuid,
                                                 Name primaryTypeName,
                                                 List<Name> mixinTypeNames,
                                                 Location versionHistoryLocation,
                                                 UUID originalVersionUuid,
                                                 UUID rootVersionUuid,
                                                 ExecutionContext context,
                                                 RequestBuilder system,
                                                 String workspace,
                                                 DateTime now ) {
            Path versionHistoryPath = versionHistoryLocation.getPath();
            UUID versionHistoryUuid = versionHistoryLocation.getUuid();
            assert versionHistoryPath != null;
            assert versionHistoryUuid != null;

            Path highestChangedNode = null;
            Property[] properties = new Property[4]; // the largest size we'll need
            CreateNodeRequest create = null;
            PropertyFactory props = context.getPropertyFactory();

            // Find the path to the version history parent, catching an exception if the node does not exist ...
            ReadNodeRequest readHistory = system.readNode(versionHistoryLocation, workspace);
            if (!readHistory.hasError()) {
                // Now update the version history node (of type "nt:versionHistory")
                // IF AND ONLY IF the existing properties don't match ...
                UuidFactory uuids = context.getValueFactories().getUuidFactory();
                Map<Name, Property> byName = readHistory.getPropertiesByName();
                properties[0] = createPropertyIfDifferent(byName, JcrLexicon.VERSIONABLE_UUID, versionableNodeUuid, props, uuids);
                if (originalVersionUuid != null) {
                    int index = properties[0] == null ? 0 : 1;
                    Name name = JcrLexicon.COPIED_FROM;
                    properties[index] = createPropertyIfDifferent(byName, name, originalVersionUuid, props, uuids);
                }
                if (properties[0] != null) {
                    system.setProperties(versionHistoryLocation, workspace, properties);
                    highestChangedNode = versionHistoryLocation.getPath();
                }
            } else {
                if (readHistory.getError() instanceof PathNotFoundException) {
                    PathNotFoundException pnfe = (PathNotFoundException)readHistory.getError();
                    highestChangedNode = pnfe.getLowestAncestorThatDoesExist();
                }

                // Make sure that all of the nodes from the root, to "jcr:system", down to the parent of the last node exist.
                Path versionHistoryParent = versionHistoryPath.getParent();
                Iterator<Path> pathsToParent = versionHistoryParent.pathsFromRoot();

                // Skip the root ...
                assert pathsToParent.hasNext();
                pathsToParent.next();

                // And the "/jcr:system" node ...
                assert pathsToParent.hasNext();
                pathsToParent.next();

                // And the "/jcr:system/jcr:versionStorage" node ...
                assert pathsToParent.hasNext();
                pathsToParent.next();

                // Now the rest of the intermediate nodes (if absent), but NOT the history node ...
                properties[0] = props.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.VERSION_HISTORY_FOLDER);
                while (pathsToParent.hasNext()) {
                    Path intermediateNode = pathsToParent.next();
                    Name nodeName = intermediateNode.getLastSegment().getName();
                    Location parentLocation = Location.create(intermediateNode.getParent());
                    system.createNode(parentLocation, workspace, nodeName, properties, NodeConflictBehavior.DO_NOT_REPLACE);
                }

                // Now create the version history node (of type "nt:versionHistory") ...
                Location parentLocation = Location.create(versionHistoryParent);
                Name name = versionHistoryPath.getLastSegment().getName();
                properties[0] = props.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION_HISTORY);
                properties[1] = props.create(JcrLexicon.VERSIONABLE_UUID, versionableNodeUuid);
                properties[2] = props.create(JcrLexicon.UUID, versionHistoryUuid);
                if (originalVersionUuid != null) {
                    properties[3] = props.create(JcrLexicon.COPIED_FROM, originalVersionUuid);
                }
                create = system.createNode(parentLocation, workspace, name, properties, NodeConflictBehavior.APPEND);
            }

            // Create the "historyNode/jcr:versionLabels" node ...
            Location parentLocation = versionHistoryLocation;
            Name name = JcrLexicon.VERSION_LABELS;
            properties[0] = props.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION_LABELS);
            properties[1] = null;
            properties[2] = null;
            properties[3] = null;
            create = system.createNode(parentLocation, workspace, name, properties, NodeConflictBehavior.APPEND);

            // Create the "historyNode/jcr:rootVersion" node ...
            parentLocation = versionHistoryLocation;
            name = JcrLexicon.ROOT_VERSION;
            properties[0] = props.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION);
            properties[1] = props.create(JcrLexicon.CREATED, now);
            properties[2] = props.create(JcrLexicon.UUID, rootVersionUuid);
            properties[3] = null;
            create = system.createNode(parentLocation, workspace, name, properties, NodeConflictBehavior.APPEND);

            // Create the "historyNode/jcr:rootVersion/jcr:frozenNode" node under the "jcr:rootVersion" node ...
            parentLocation = create.getActualLocationOfNode();
            name = JcrLexicon.FROZEN_NODE;
            properties[0] = props.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE);
            properties[1] = props.create(JcrLexicon.FROZEN_UUID, versionableNodeUuid);
            properties[2] = props.create(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryTypeName);
            if (mixinTypeNames != null && !mixinTypeNames.isEmpty()) {
                properties[3] = props.create(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypeNames);
            }
            create = system.createNode(parentLocation, workspace, name, properties, NodeConflictBehavior.APPEND);

            return highestChangedNode;
        }
    }

    /**
     * The {@link Function} implementation that efficiently ensures that the JCR version history and storage for a node exist.
     * This function will correctly initialize the version storage and output the values for the "mix:versionable" properties.
     * <p>
     * Note that this function will work correctly even if there is already an existing version history for the UUID. This is
     * useful in situations like importing content, where an existing node might be deleted from the content and replaced with a
     * new node with the same UUID, where the version history of the older node is to be kept.
     * </p>
     * <p>
     * This function requires the following inputs:
     * <ul>
     * <li>"<code>versionedNodeUuid</code>" - the {@link UUID} of the node that is being versioned; may not be null</li>
     * <li>"<code>originalUuid</code>" - the {@link UUID} of the node that was copied; may be null if not a copy operation</li>
     * <li>"<code>versionHistoryPath</code>" - the {@link Path} to the version history node under
     * <code>"/jcr:system/jcr:versionStorage"</code>; may not be null</li>
     * <li>"<code>versionHistoryUuid</code>" - the proposed {@link UUID} for the version history node; may be null if a new UUID
     * should be generated</li>
     * <li>"<code>versionedUuid</code>" - the proposed {@link UUID} of the first version node; may be null if a new UUID should be
     * generated</li>
     * <li>"<code>primaryTypeName</code>" - the {@link Name} of the primary type of the node being versioned; may not be null</li>
     * <li>"<code>mixinTypeNameList</code>" - the {@link List} of {@link Name} objects for each of the mixin types for the node
     * being versioned; may be null or empty if there are no mixin types</li>
     * </ul>
     * The function produces the following outputs:
     * <ul>
     * <li>"<code>predecessorUuidList</code>" - the {@link List} of {@link UUID}s representing the predecessors for the versioned
     * node (see the " <code>jcr:predecessors</code>" property on "<code>mix:versionable</code>"); never null</li>
     * <li>"<code>versionHistoryUuid</code>" - the {@link UUID} of the version history node (see the "
     * <code>jcr:versionHistory</code>" property on "<code>mix:versionable</code>"); never null</li>
     * <li>"<code>baseVersionUuid</code>" - the {@link UUID} of the versionable node's base version (see the "
     * <code>jcr:baseVersion</code>" property on "<code>mix:versionable</code>"); never null</li>
     * <li>"<code>pathOfHighestModifiedNode</code>" - the {@link Path} of the highest node that was modified by this operation; or
     * null if no nodes were modified</li>
     * </ul>
     */
    @Immutable
    public static class InitializeVersionHistoryFunction extends VersionHistoryFunction {
        private static final long serialVersionUID = 1L;

        public static final String VERSIONED_NODE_UUID = "versionedNodeUuid";
        public static final String ORIGINAL_UUID = "originalUuid";
        public static final String VERSION_HISTORY_PATH = "versionHistoryPath";
        public static final String VERSION_UUID = "versionUuid";
        public static final String PRIMARY_TYPE_NAME = "primaryTypeName";
        public static final String MIXIN_TYPE_NAME_LIST = "mixinTypeNameList";
        public static final String PREDECESSOR_UUID_LIST = "predecessorUuidList";
        public static final String VERSION_HISTORY_UUID = "versionHistoryUuid";
        public static final String BASE_VERSION_UUID = "baseVersionUuid";
        public static final String PATH_OF_HIGHEST_MODIFIED_NODE = "pathOfHighestModifiedNode";

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.function.Function#run(org.modeshape.graph.request.function.FunctionContext)
         */
        @Override
        public void run( FunctionContext context ) {
            // Get the inputs ...
            UUID uuid = context.input(VERSIONED_NODE_UUID, UUID.class);
            UUID originalUuid = context.input(ORIGINAL_UUID, UUID.class);
            Path versionHistoryPath = context.input(VERSION_HISTORY_PATH, Path.class);
            UUID versionHistoryUuid = context.input(VERSION_HISTORY_UUID, UUID.class); // may be null
            UUID versionUuid = context.input(VERSION_UUID, UUID.class); // may be null
            Name primaryType = context.input(PRIMARY_TYPE_NAME, Name.class);
            @SuppressWarnings( "unchecked" )
            List<Name> mixinTypes = (List<Name>)context.input(MIXIN_TYPE_NAME_LIST);

            // Define the output variable(s) ...
            List<UUID> predecessors = null;
            UUID baseVersion = null;
            Path pathOfHighestModifiedNode = null;

            assert uuid != null;
            assert versionHistoryPath != null;

            RequestBuilder builder = context.builder();
            String workspace = context.workspace();

            // Check whether the version history already exists ...
            Location versionHistoryLocation = versionHistoryLocationFor(versionHistoryPath,
                                                                        versionHistoryUuid,
                                                                        builder,
                                                                        workspace);
            if (versionHistoryLocation != null) {
                // The version history already exists, so we need to get find the last version without a 'jcr:successor' ...
                ReadAllChildrenRequest readChildren = builder.readAllChildren(versionHistoryLocation, workspace);
                List<Location> children = readChildren.getChildren();
                int numChildren = children.size();
                if (numChildren < 2) {
                    // There aren't enough nodes, so re-create the version history ...
                    versionHistoryLocation = null;

                } else if (numChildren == 2) {
                    // There is a 'jcr:versionLabel' node and a 'jcr:rootVersion' node, so the predecessor is the rootVersion ...
                    Location rootVersionLocation = children.get(1);
                    UUID rootVersionUuid = rootVersionLocation.getUuid();
                    assert rootVersionUuid != null;
                    predecessors = Collections.singletonList(rootVersionUuid);
                    baseVersion = rootVersionUuid;
                } else {
                    ListIterator<Location> iter = children.listIterator(children.size());
                    while (iter.hasPrevious()) {
                        Location location = iter.previous();
                        ReadPropertyRequest request = builder.readProperty(location, workspace, JcrLexicon.SUCCESSORS);
                        if (request.getProperty() == null) {
                            UUID baseVersionUuid = location.getUuid();
                            assert baseVersionUuid != null;
                            predecessors = Collections.singletonList(baseVersionUuid);
                            baseVersion = baseVersionUuid;
                            break;
                        }
                    }
                }
            }

            if (versionHistoryLocation == null) {

                // The version history does NOT exist and needs to be created ...
                if (versionHistoryUuid == null) versionHistoryUuid = UUID.randomUUID();
                UUID rootVersionUuid = versionUuid != null ? versionUuid : UUID.randomUUID();
                versionHistoryLocation = Location.create(versionHistoryPath, versionHistoryUuid);
                pathOfHighestModifiedNode = initializeVersionStorage(uuid,
                                                                     primaryType,
                                                                     mixinTypes,
                                                                     versionHistoryLocation,
                                                                     originalUuid,
                                                                     rootVersionUuid,
                                                                     context.getExecutionContext(),
                                                                     context.builder(),
                                                                     context.workspace(),
                                                                     context.getNowInUtc());
                // Set the predecessor to the root version ...
                predecessors = Collections.singletonList(rootVersionUuid);
                baseVersion = rootVersionUuid;
            }

            // Now set the output variables ...
            assert predecessors instanceof Serializable;
            assert versionHistoryLocation.getUuid() != null;
            context.setOutput(PREDECESSOR_UUID_LIST, (Serializable)predecessors);
            context.setOutput(VERSION_HISTORY_UUID, versionHistoryLocation.getUuid());
            context.setOutput(BASE_VERSION_UUID, baseVersion);
            context.setOutput(PATH_OF_HIGHEST_MODIFIED_NODE, pathOfHighestModifiedNode);
        }

    }

    /**
     * The {@link Function} implementation that efficiently updates the JCR version history and storage with a new version of a
     * node being checked in. This function will correctly update the version storage and output the values for the
     * "mix:versionable" properties. Note, however, that the caller of this function must walk the children of the versionable
     * node and create version nodes appropriately.
     * <p>
     * Note that this function assumes that the version history already exists for the versionable node.
     * </p>
     * <p>
     * This function requires the following inputs:
     * <ul>
     * <li>"<code>versionedNodeUuid</code>" - the {@link UUID} of the node that is being versioned; may not be null</li>
     * <li>"<code>versionHistoryPath</code>" - the {@link Path} to the version history node under
     * <code>"/jcr:system/jcr:versionStorage"</code>; may not be null</li>
     * <li>"<code>versionName</code>" - the {@link Name} of the new version node under the 'nt:versionHistory' node at
     * <code>"versionHistoryPath"</code>; may be null if the current time should be used for the name</li>
     * <li>"<code>primaryTypeName</code>" - the {@link Name} of the primary type of the node being versioned; may not be null</li>
     * <li>"<code>mixinTypeNameList</code>" - the {@link List} of {@link Name} objects for each of the mixin types for the node
     * being versioned; may be null or empty if there are no mixin types</li>
     * <li>"<code>predecessorUuidList</code>" - the {@link List} of {@link UUID}s representing the predecessors for the existing
     * version node (see the "<code>jcr:predecessors</code>" property on "<code>mix:versionable</code>"); may not be null or empty
     * </li>
     * <li>"<code>versionPropertyList</code>" - the {@link List} of {@link Property} objects that should be included on the new "
     * <code>nt:versionHistory</code>" node; may be null or empty</li>
     * </ul>
     * The function produces the following outputs:
     * <ul>
     * <li>"<code>versionHistoryUuid</code>" - the {@link UUID} of the new version history node; never null</li>
     * <li>"<code>versionUuid</code>" - the {@link UUID} of the new version node; never null</li>
     * <li>"<code>versionPath</code>" - the {@link Path} of the new version node; never null</li>
     * <li>"<code>pathOfHighestModifiedNode</code>" - the {@link Path} of the highest node that was modified by this operation;
     * never null</li>
     * </ul>
     */
    @Immutable
    public static class CreateVersionNodeFunction extends VersionHistoryFunction {
        private static final long serialVersionUID = 1L;

        public static final String VERSION_NAME = "versionName";
        public static final String VERSION_HISTORY_PATH = "versionHistoryPath";
        public static final String VERSIONED_NODE_UUID = "versionedNodeUuid";
        public static final String PRIMARY_TYPE_NAME = "primaryTypeName";
        public static final String MIXIN_TYPE_NAME_LIST = "mixinTypeNameList";
        public static final String PREDECESSOR_PROPERTY = "predecessorProperty";
        public static final String VERSION_PROPERTY_LIST = "versionPropertyList";
        public static final String VERSION_HISTORY_UUID = "versionHistoryUuid";
        public static final String VERSION_UUID = "versionUuid";
        public static final String VERSION_PATH = "versionPath";
        public static final String PATH_OF_HIGHEST_MODIFIED_NODE = "pathOfHighestModifiedNode";

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.function.Function#run(org.modeshape.graph.request.function.FunctionContext)
         */
        @Override
        public void run( FunctionContext context ) {
            // Get the inputs ...
            Path versionHistoryPath = context.input(VERSION_HISTORY_PATH, Path.class);
            Name versionNodeName = context.input(VERSION_NAME, Name.class); // may be null
            UUID versionedNodeUuid = context.input(VERSIONED_NODE_UUID, UUID.class);
            Name primaryType = context.input(PRIMARY_TYPE_NAME, Name.class);
            @SuppressWarnings( "unchecked" )
            List<Name> mixinTypes = (List<Name>)context.input(MIXIN_TYPE_NAME_LIST); // may be null
            Property predecessors = (Property)context.input(PREDECESSOR_PROPERTY);
            @SuppressWarnings( "unchecked" )
            List<Property> versionPropertyList = (List<Property>)context.input(VERSION_PROPERTY_LIST); // may be null
            assert versionHistoryPath != null;
            assert versionedNodeUuid != null;
            assert primaryType != null;
            assert predecessors != null;
            assert !predecessors.isEmpty();

            // Get some of the references and values we'll need ...
            final RequestBuilder builder = context.builder();
            final String workspace = context.workspace();
            final PropertyFactory props = context.getExecutionContext().getPropertyFactory();
            final DateTime now = context.getNowInUtc();
            final UUID versionUuid = UUID.randomUUID();
            final int numVersionProps = versionPropertyList != null ? versionPropertyList.size() : 0;
            final Property[] properties = new Property[numVersionProps + 4]; // the largest size we'll need
            CreateNodeRequest request = null;
            Path pathOfHighestModifiedNode = null;
            if (versionNodeName == null) {
                String name = JcrVersionManager.NODE_ENCODER.encode(now.getString());
                versionNodeName = context.getExecutionContext().getValueFactories().getNameFactory().create(name);
            }

            // Check whether the version history already exists for this node ...
            Location versionHistoryLocation = versionHistoryLocationFor(versionHistoryPath, null, builder, workspace);
            if (versionHistoryLocation == null) {
                // There is no version history yet, so initialize it ...
                UUID versionHistoryUuid = UUID.randomUUID();
                UUID rootVersionUuid = UUID.randomUUID();
                UUID originalUuid = null; // not a copy ...
                versionHistoryLocation = Location.create(versionHistoryPath, versionHistoryUuid);
                pathOfHighestModifiedNode = initializeVersionStorage(versionedNodeUuid,
                                                                     primaryType,
                                                                     mixinTypes,
                                                                     versionHistoryLocation,
                                                                     originalUuid,
                                                                     rootVersionUuid,
                                                                     context.getExecutionContext(),
                                                                     context.builder(),
                                                                     context.workspace(),
                                                                     context.getNowInUtc());
                // Override the predecessors to include the newly-create root version ...
                predecessors = props.create(JcrLexicon.PREDECESSORS, rootVersionUuid);
            }

            // Create a 'nt:version' node under the version history node ...
            properties[0] = props.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION);
            properties[1] = props.create(JcrLexicon.CREATED, now);
            properties[2] = props.create(JcrLexicon.UUID, versionUuid);
            properties[3] = predecessors;
            request = builder.createNode(versionHistoryLocation, workspace, versionNodeName, properties);

            // Create a 'nt:frozenNode' node under the 'nt:version' node ...
            final Location versionNodeLocation = request.getActualLocationOfNode();
            final UUID versionNodeUuid = versionNodeLocation.getUuid();
            properties[0] = props.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE);
            properties[1] = props.create(JcrLexicon.FROZEN_UUID, versionedNodeUuid);
            properties[2] = props.create(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryType);
            properties[3] = props.create(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypes);
            if (versionPropertyList != null) {
                for (int i = 0, p = 4; i != numVersionProps; ++i) {
                    Property prop = versionPropertyList.get(i);
                    // We want to skip the actual primary type, mixin types, and uuid since those are handled above ...
                    Name name = prop.getName();
                    if (JcrLexicon.PRIMARY_TYPE.equals(name)) continue;
                    if (JcrLexicon.MIXIN_TYPES.equals(name)) continue;
                    if (JcrLexicon.UUID.equals(name)) continue;
                    // Otherwise, add in the property ...
                    properties[p++] = versionPropertyList.get(i);
                }
            }
            request = builder.createNode(versionNodeLocation, workspace, JcrLexicon.FROZEN_NODE, properties);

            // Now update the predecessor nodes to have the new version node be included as one of their successors ...
            UuidFactory uuidFactory = context.getExecutionContext().getValueFactories().getUuidFactory();
            Property successors = null;
            final Set<UUID> successorUuids = new HashSet<UUID>();
            for (Object value : predecessors) {
                UUID predecessorUuid = uuidFactory.create(value);
                Location predecessorLocation = Location.create(predecessorUuid);

                // Look up the 'jcr:successors' property on the predecessor ...
                successors = builder.readProperty(predecessorLocation, workspace, JcrLexicon.SUCCESSORS).getProperty();

                if (successors != null) {
                    // There were already successors, so we need to add our new version node the list ...
                    successorUuids.clear();
                    for (Object successorValue : successors) {
                        successorUuids.add(uuidFactory.create(successorValue));
                    }

                    // Now add the uuid of the versionable node ...
                    if (successorUuids.add(versionNodeUuid)) {
                        // It is not already a successor, so we need to update the successors property ...
                        successors = props.create(JcrLexicon.SUCCESSORS, successorUuids);
                        builder.setProperty(predecessorLocation, workspace, successors);
                    }
                } else {
                    // There was no 'jcr:successors' property, so create it ...
                    successors = props.create(JcrLexicon.SUCCESSORS, versionNodeUuid);
                    builder.setProperty(predecessorLocation, workspace, successors);
                }
            }

            // Now set the output variable(s) ...
            Path versionNodePath = versionNodeLocation.getPath();
            context.setOutput(VERSION_HISTORY_UUID, versionHistoryLocation.getUuid());
            context.setOutput(VERSION_UUID, versionNodeUuid);
            context.setOutput(VERSION_PATH, versionNodePath);
            if (pathOfHighestModifiedNode == null) {
                pathOfHighestModifiedNode = versionNodePath;
            }
            context.setOutput(PATH_OF_HIGHEST_MODIFIED_NODE, pathOfHighestModifiedNode);
        }
    }
}
