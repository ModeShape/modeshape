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

package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.HashMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SiblingCounter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.StringFactory;

@Immutable
public class NodeTypes {

    public static interface Supplier {
        /**
         * Get the immutable cache of node types.
         *
         * @return the immutable node types cache; never null
         */
        NodeTypes getNodeTypes();
    }

    public static interface Listener {
        /**
         * Notification that the NodeTypes instance has changed.
         *
         * @param updatedNodeTypes the new NodeTypes instance; never null
         */
        void notify( NodeTypes updatedNodeTypes );
    }

    /**
     * List of ways to filter the returned property definitions
     *
     * @see NodeTypes#findPropertyDefinitions(List, Name, PropertyCardinality, List)
     */
    public enum PropertyCardinality {
        SINGLE_VALUED_ONLY,
        MULTI_VALUED_ONLY,
        ANY
    }

    /**
     * List of ways to filter the returned node definitions
     */
    public enum NodeCardinality {
        NO_SAME_NAME_SIBLINGS,
        SAME_NAME_SIBLINGS,
        ANY
    }

    private final Map<Name, JcrNodeType> nodeTypes = new HashMap<Name, JcrNodeType>();
    private final Map<PropertyDefinitionId, JcrPropertyDefinition> propertyDefinitions = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();
    private final Map<NodeDefinitionId, JcrNodeDefinition> childNodeDefinitions = new HashMap<NodeDefinitionId, JcrNodeDefinition>();

    private final Collection<JcrNodeType> unmodifiableNodeTypes;
    private final Collection<JcrNodeType> unmodifiableMixinNodeTypes;
    private final Collection<JcrNodeType> unmodifiablePrimaryNodeTypes;
    private final Set<Name> unmodifiableNodeTypeNames;
    private final Set<Name> unmodifiableMixinTypeNames;

    private final int nodeTypesVersion;
    private final JcrNodeDefinition ntUnstructuredSnsChildDefinition;
    private final JcrNodeDefinition ntFileChildDefinition;
    private final JcrNodeDefinition ntFolderChildDefinition;

    private final ExecutionContext context;
    private final NameFactory nameFactory;

    /**
     * The set of node type names that require no extra work during pre-save operations, as long as nodes that have this primary
     * type do not have any mixins. Note that this contains all node types not in any of the other sets.
     */
    private static final Set<Name> fullyDefinedNodeTypes = new HashSet<>();

    private static final Set<Name> NONE = Collections.emptySet();

    /** Thread local variable containing the last NodeDefinitionSet used */
    private static final ThreadLocal<ReusableNodeDefinitionSet> nodeDefinitionSet = new ThreadLocal<ReusableNodeDefinitionSet>() {
        @Override
        protected ReusableNodeDefinitionSet initialValue() {
            return new EmptyNodeDefinitionSet();
        }
    };

    /**
     * The set of standard built-in NodeDefinitionSet
     */
    private final List<ReusableNodeDefinitionSet> standardNodeDefinitionSets = new LinkedList<>();

    /**
     * The set of names for the node types that are 'mix:created'. See {@link #isCreated(Name, Set)}
     */
    private final Set<Name> createdNodeTypeNames = new HashSet<>();
    /**
     * The set of names for the node types that are 'mix:lastModified'. See {@link #isLastModified(Name, Set)}
     */
    private final Set<Name> lastModifiedNodeTypeNames = new HashSet<>();
    /**
     * The set of names for the node types that are 'mix:mimeType'. See {@link #isNtResource(Name)}
     */
    private final Set<Name> resourceNodeTypeNames = new HashSet<>();
    /**
     * The set of names for the node types that are 'mix:etag'. See {@link #isETag(Name, Set)}
     */
    private final Set<Name> etagNodeTypeNames = new HashSet<>();
    /**
     * The set of names for the node types that are 'mix:versionable'. See {@link #isVersionable(Name, Collection)}
     */
    private final Set<Name> versionableNodeTypeNames = new HashSet<>();
    /**
     * The set of names for the node types that have child node definitions that allow same name siblings for every combination.
     * In other words, given any valid child, there will always be a satisfying child node definition that allows same name
     * siblings. For example, 'nt:unstructured' contains residual child node definitions that allow and disallow same name
     * siblings, so 'nt:unstructured' will always be in this set. If a node type is not in this set, then nodes of this type must
     * be checked for children to ensure the children satisfy the child node definitions.
     */
    private final Set<Name> nodeTypeNamesThatAllowSameNameSiblings = new HashSet<>();

    private final Set<Name> nodeTypeNamesThatAreReferenceable = new HashSet<>();

    private final Set<Name> nodeTypeNamesThatAreShareable = new HashSet<>();

    private final Set<Name> nodeTypeNamesWithNoChildNodeDefns = new HashSet<>();
    
    private final Set<Name> nodeTypeNamesThatAreUnorderableCollections = new HashSet<>();

    /**
     * The map of mandatory (and perhaps auto-created) property definitions for a node type keyed by the name of the node type.
     * See {@link #hasMandatoryPropertyDefinitions}
     */
    private final Multimap<Name, JcrPropertyDefinition> mandatoryPropertiesNodeTypes = HashMultimap.create();
    /**
     * The map of mandatory (and perhaps auto-created) child node definitions for a node type keyed by the name of the node type.
     * See {@link #hasMandatoryChildNodeDefinitions}
     */
    private final Multimap<Name, JcrNodeDefinition> mandatoryChildrenNodeTypes = HashMultimap.create();

    /**
     * The map of auto-created property definitions for a node type keyed by the name of the node type. See
     * {@link #hasMandatoryPropertyDefinitions}
     */
    private final Multimap<Name, JcrPropertyDefinition> autoCreatedPropertiesNodeTypes = HashMultimap.create();

    /**
     * The map of auto-created child node definitions for a node type keyed by the name of the node type. See
     * {@link #hasMandatoryChildNodeDefinitions}
     */
    private final Multimap<Name, JcrNodeDefinition> autoCreatedChildrenNodeTypes = HashMultimap.create();

    /**
     * A set of all the node types which are defined as non-queryable (noquery)
     */
    private final Set<Name> nonQueryableNodeTypes = new HashSet<>();

    protected NodeTypes( ExecutionContext context ) {
        this(context, null, 0);
    }

    protected NodeTypes( ExecutionContext context,
                         Iterable<JcrNodeType> nodeTypes,
                         int version ) {
        this.nodeTypesVersion = version;
        this.context = context;
        this.nameFactory = context.getValueFactories().getNameFactory();

        Set<Name> mixinNames = new HashSet<Name>();
        List<JcrNodeType> mixins = new ArrayList<JcrNodeType>();
        List<JcrNodeType> primaries = new ArrayList<JcrNodeType>();
        if (nodeTypes != null) {
            JcrNodeType ntUnstructured = null;
            for (JcrNodeType nodeType : nodeTypes) {
                boolean isUnorderedCollection = false;
                Name name = nodeType.getInternalName();

                // Store the node type in the quick-lookup maps ...
                this.nodeTypes.put(name, nodeType);
                for (JcrNodeDefinition childDefinition : nodeType.childNodeDefinitions()) {
                    this.childNodeDefinitions.put(childDefinition.getId(), childDefinition);
                }
                for (JcrPropertyDefinition propertyDefinition : nodeType.propertyDefinitions()) {
                    this.propertyDefinitions.put(propertyDefinition.getId(), propertyDefinition);
                }
                if (nodeType.isMixin()) {
                    mixins.add(nodeType);
                    mixinNames.add(name);
                } else {
                    primaries.add(nodeType);
                }

                if (name.equals(JcrNtLexicon.UNSTRUCTURED)) {
                    ntUnstructured = nodeType;
                    nodeTypeNamesThatAllowSameNameSiblings.add(name);
                }

                if (nodeType.isNodeType(JcrMixLexicon.REFERENCEABLE)) {
                    nodeTypeNamesThatAreReferenceable.add(name);
                }

                if (nodeType.isNodeType(JcrMixLexicon.SHAREABLE)) {
                    nodeTypeNamesThatAreShareable.add(name);
                }

                if (nodeType.isNodeType(ModeShapeLexicon.UNORDERED_COLLECTION)) {
                    nodeTypeNamesThatAreUnorderableCollections.add(name);
                }   
               
                boolean fullyDefined = true;
                if (nodeType.isNodeType(JcrMixLexicon.CREATED)) {
                    createdNodeTypeNames.add(name);
                    fullyDefined = false;
                }
                if (nodeType.isNodeType(JcrMixLexicon.LAST_MODIFIED)) {
                    lastModifiedNodeTypeNames.add(name);
                    fullyDefined = false;
                }
                if (nodeType.isNodeType(JcrNtLexicon.RESOURCE)) {
                    resourceNodeTypeNames.add(name);
                    fullyDefined = false;
                }
                if (nodeType.isNodeType(JcrMixLexicon.ETAG)) {
                    etagNodeTypeNames.add(name);
                    fullyDefined = false;
                }
                if (nodeType.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                    versionableNodeTypeNames.add(name);
                    fullyDefined = false;
                }
                if (nodeType.isNodeType(ModeShapeLexicon.UNORDERED_COLLECTION)) {
                    nodeTypeNamesThatAreUnorderableCollections.add(name);
                    isUnorderedCollection = true;
                }
                for (JcrPropertyDefinition propDefn : nodeType.allPropertyDefinitions()) {
                    if (propDefn.isMandatory() && !propDefn.isProtected()) {
                        mandatoryPropertiesNodeTypes.put(name, propDefn);
                        fullyDefined = false;
                    }
                    if (propDefn.isAutoCreated() && !propDefn.isProtected()) {
                        autoCreatedPropertiesNodeTypes.put(name, propDefn);
                        // This isn't used in the pre-save operations, since auto-created items should be set on node creation
                        // fullDefined = false;
                    }
                }
                Collection<JcrNodeDefinition> allChildNodeDefinitions = nodeType.allChildNodeDefinitions();
                if (allChildNodeDefinitions.isEmpty()) nodeTypeNamesWithNoChildNodeDefns.add(name);
                boolean allowsResidualWithSameNameSiblings = false;
                boolean allowsOnlySameNameSiblings = true;
                boolean mixinWithNoChildNodeDefinitions = nodeType.isMixin() && allChildNodeDefinitions.isEmpty();
                for (JcrNodeDefinition childDefn : allChildNodeDefinitions) {
                    if (childDefn.isMandatory() && !childDefn.isProtected()) {
                        mandatoryChildrenNodeTypes.put(name, childDefn);
                        fullyDefined = false;
                    }
                    if (childDefn.isAutoCreated() && !childDefn.isProtected()) {
                        autoCreatedChildrenNodeTypes.put(name, childDefn);
                        // This isn't used in the pre-save operations, since auto-created items should be set on node creation
                        // fullDefined = false;
                    }
                    if (childDefn.allowsSameNameSiblings()) {
                        if (childDefn.isResidual()) {
                            allowsResidualWithSameNameSiblings = true;
                        }
                    } else {
                        // same name siblings are not allowed ...
                        allowsOnlySameNameSiblings = false;
                    }
                }
                if (!nodeType.isAbstract()
                    && !isUnorderedCollection
                    && (allowsResidualWithSameNameSiblings || allowsOnlySameNameSiblings || mixinWithNoChildNodeDefinitions)) {
                    nodeTypeNamesThatAllowSameNameSiblings.add(name);
                }

                if (fullyDefined) {
                    fullyDefinedNodeTypes.add(name);
                }
                
                if (!nodeType.isQueryable()) {
                    nonQueryableNodeTypes.add(name);
                }
            }

            assert ntUnstructured != null;

            // Find and cache the 'nt:unstructured' residual child node definition that allows multiple SNS ...
            Collection<JcrNodeDefinition> childDefns = ntUnstructured.allChildNodeDefinitions(JcrNodeType.RESIDUAL_NAME, true);
            assert childDefns.size() == 1;
            ntUnstructuredSnsChildDefinition = childDefns.iterator().next();
            assert ntUnstructuredSnsChildDefinition != null;

            Collection<JcrNodeDefinition> fileChildDefns = getNodeType(JcrNtLexicon.FILE).allChildNodeDefinitions();
            Collection<JcrNodeDefinition> folderChildDefns = getNodeType(JcrNtLexicon.FOLDER).allChildNodeDefinitions();
            assert fileChildDefns.size() == 1;
            assert folderChildDefns.size() == 1;
            this.ntFileChildDefinition = fileChildDefns.iterator().next();
            this.ntFolderChildDefinition = folderChildDefns.iterator().next();

            // Add some standard node definition sets ...
            this.standardNodeDefinitionSets.add(new SingleNodeDefinitionSet(JcrNtLexicon.UNSTRUCTURED, NONE,
                                                                            ntUnstructuredSnsChildDefinition));
            this.standardNodeDefinitionSets.add(new SingleNodeDefinitionSet(JcrNtLexicon.FILE, NONE, ntFileChildDefinition));
            this.standardNodeDefinitionSets.add(new SingleNodeDefinitionSet(JcrNtLexicon.FOLDER, NONE, ntFolderChildDefinition));
        } else {
            this.ntUnstructuredSnsChildDefinition = null;
            this.ntFileChildDefinition = null;
            this.ntFolderChildDefinition = null;
        }

        this.unmodifiableNodeTypes = Collections.unmodifiableCollection(this.nodeTypes.values());
        this.unmodifiableNodeTypeNames = Collections.unmodifiableSet(this.nodeTypes.keySet());
        this.unmodifiableMixinTypeNames = Collections.unmodifiableSet(mixinNames);

        this.unmodifiableMixinNodeTypes = Collections.unmodifiableList(mixins);
        this.unmodifiablePrimaryNodeTypes = Collections.unmodifiableList(primaries);
    }

    /**
     * Obtain a new version of this cache with the specified node types removed from the new cache.
     *
     * @param removedNodeTypes the node types that are to be removed from the resulting cache; may not be null but may be empty
     * @return the resulting cache that contains all of the node types within this cache but without the supplied node types;
     *         never null
     */
    protected NodeTypes without( Collection<JcrNodeType> removedNodeTypes ) {
        if (removedNodeTypes.isEmpty()) return this;
        Collection<JcrNodeType> nodeTypes = new HashSet<JcrNodeType>(this.nodeTypes.values());
        nodeTypes.removeAll(removedNodeTypes);
        return new NodeTypes(this.context, nodeTypes, getVersion() + 1);
    }

    /**
     * Obtain a new version of this cache with the specified node types added to the new cache.
     *
     * @param addedNodeTypes the node types that are to be added to the resulting cache; may not be null but may be empty
     * @return the resulting cache that contains all of the node types within this cache and the supplied node types; never null
     */
    protected NodeTypes with( Collection<JcrNodeType> addedNodeTypes ) {
        if (addedNodeTypes.isEmpty()) return this;
        Collection<JcrNodeType> nodeTypes = new HashSet<JcrNodeType>(this.nodeTypes.values());
        // if there are updated node types, remove them first (hashcode is based on name alone),
        // else addAll() will ignore the changes.
        nodeTypes.removeAll(addedNodeTypes);
        nodeTypes.addAll(addedNodeTypes);
        return new NodeTypes(this.context, nodeTypes, getVersion() + 1);
    }

    /**
     * @return nameFactory
     */
    protected final NameFactory nameFactory() {
        return nameFactory;
    }

    /**
     * Get the version number of this cache. This essentially acts as an ETag, allowing other components to cache node type
     * information as long as the version number stays the same.
     *
     * @return the version number of this cache
     */
    public int getVersion() {
        return nodeTypesVersion;
    }

    /**
     * Determine if the named node type does not appear in any of the other sets. Such node types are fully-defined, in that nodes
     * using them require no additional processing prior to save.
     * <p>
     * Note that this method's signature is different from the other methods. This is because a node's primary type and mixin
     * types must all be fully-defined types.
     * </p>
     *
     * @param primaryTypeName the name of the primary node type; may not be null
     * @param mixinTypeNames the set of mixin type names; may be null or empty
     * @return true if the named node type is fully-defined, or false otherwise
     */
    public boolean isFullyDefinedType( Name primaryTypeName,
                                       Set<Name> mixinTypeNames ) {
        if (!fullyDefinedNodeTypes.contains(primaryTypeName)) return false;
        if (!mixinTypeNames.isEmpty()) {
            for (Name nodeTypeName : mixinTypeNames) {
                if (!fullyDefinedNodeTypes.contains(nodeTypeName)) return false;
            }
        }
        return true;
    }

    public Set<Name> getAllSubtypes( Name nodeTypeName ) {
        Set<Name> subtypes = new HashSet<>();
        JcrNodeType type = getNodeType(nodeTypeName);
        if (type != null) {
            subtypes.add(nodeTypeName);
            for (JcrNodeType subtype : subtypesFor(type)) {
                subtypes.add(subtype.getInternalName());
            }
        }
        return Collections.unmodifiableSet(subtypes);
    }

    /**
     * Determine whether the node's given node type matches or extends the node type with the supplied name.
     *
     * @param nodeTypeName the name of the node type of a node; may not be null
     * @param candidateSupertypeName the name of the potential supertype node type; may not be null
     * @return true if the node type does extend or match the node type given by the supplied name, or false otherwise
     */
    public boolean isTypeOrSubtype( Name nodeTypeName,
                                    Name candidateSupertypeName ) {
        if (JcrNtLexicon.BASE.equals(candidateSupertypeName)) {
            // If the candidate is 'nt:base', then every node type is a subtype ...
            return true;
        }
        if (nodeTypeName.equals(candidateSupertypeName)) return true;
        JcrNodeType nodeType = getNodeType(nodeTypeName);
        return nodeType != null && nodeType.isNodeType(candidateSupertypeName);
    }

    /**
     * Determine whether at least one of the node's given node types matches or extends the node type with the supplied name.
     *
     * @param nodeTypeNames the names of the node types of a node; may not be null
     * @param candidateSupertypeName the name of the potential supertype node type; may not be null
     * @return true if the node type does extend or match the node type given by at least one of the supplied names, or false
     *         otherwise
     */
    public boolean isTypeOrSubtype( Set<Name> nodeTypeNames,
                                    Name candidateSupertypeName ) {
        for (Name nodeTypeName : nodeTypeNames) {
            if (isTypeOrSubtype(nodeTypeName, candidateSupertypeName)) return true;
        }
        return false;
    }

    /**
     * Determine whether at least one of the node's given node types matches or extends the node type with the supplied name.
     *
     * @param nodeTypeNames the names of the node types of a node; may not be null
     * @param candidateSupertypeName the name of the potential supertype node type; may not be null
     * @return true if the node type does extend or match the node type given by at least one of the supplied names, or false
     *         otherwise
     */
    public boolean isTypeOrSubtype( Name[] nodeTypeNames,
                                    Name candidateSupertypeName ) {
        for (Name nodeTypeName : nodeTypeNames) {
            if (isTypeOrSubtype(nodeTypeName, candidateSupertypeName)) return true;
        }
        return false;
    }

    /**
     * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:created' mixin type.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may be null or empty
     * @return true if any of the named node types is a created type, or false if there are none
     */
    public boolean isCreated( Name primaryType,
                              Set<Name> mixinTypes ) {
        if (createdNodeTypeNames.contains(primaryType)) return true;
        if (mixinTypes != null) {
            for (Name mixinType : mixinTypes) {
                if (createdNodeTypeNames.contains(mixinType)) return true;
            }
        }
        return false;
    }

    /**
     * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:lastModified' mixin type.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may be null or empty
     * @return true if any of the named node types is a last-modified type, or false if there are none
     */
    public boolean isLastModified( Name primaryType,
                                   Set<Name> mixinTypes ) {
        if (lastModifiedNodeTypeNames.contains(primaryType)) return true;
        if (mixinTypes != null) {
            for (Name mixinType : mixinTypes) {
                if (lastModifiedNodeTypeNames.contains(mixinType)) return true;
            }
        }
        return false;
    }

    /**
     * Determine if the named primary node type is or subtypes the 'nt:resource' node type.
     *
     * @param primaryType the primary type name; may not be null
     * @return true if the primary node type is an 'nt:resource' node type (or subtype), or false otherwise
     */
    public boolean isNtResource( Name primaryType ) {
        // 'nt:resource' is a node type (not a mixin), so it can't appear in the mixin types ...
        return resourceNodeTypeNames.contains(primaryType);
    }

    /**
     * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:etag' mixin type.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may be null or empty
     * @return true if any of the named node types has an ETag, or false if there are none
     */
    public boolean isETag( Name primaryType,
                           Set<Name> mixinTypes ) {
        if (etagNodeTypeNames.contains(primaryType)) return true;
        if (mixinTypes != null) {
            for (Name mixinType : mixinTypes) {
                if (etagNodeTypeNames.contains(mixinType)) return true;
            }
        }
        return false;
    }

    /**
     * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:referenceable' mixin type.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may be null or empty
     * @return true if the primary node type is an 'mix:referenceable' node type (or subtype), or false otherwise
     */
    public boolean isReferenceable( Name primaryType,
                                    Set<Name> mixinTypes ) {
        if (nodeTypeNamesThatAreReferenceable.contains(primaryType)) return true;
        if (mixinTypes != null) {
            for (Name mixinType : mixinTypes) {
                if (nodeTypeNamesThatAreReferenceable.contains(mixinType)) return true;
            }
        }
        return false;
    }

    /**
     * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:shareable' mixin type.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may be null or empty
     * @return true if the primary node type is an 'mix:shareable' node type (or subtype), or false otherwise
     */
    public boolean isShareable( Name primaryType,
                                Set<Name> mixinTypes ) {
        if (nodeTypeNamesThatAreShareable.contains(primaryType)) return true;
        if (mixinTypes != null) {
            for (Name mixinType : mixinTypes) {
                if (nodeTypeNamesThatAreShareable.contains(mixinType)) return true;
            }
        }
        return false;
    }

    /**
     * Determine if either the primary type or any of the mixin types allows SNS.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may be null or empty
     * @return {@code true} if either the primary type or any of the mixin types allows SNS. If neither allow SNS,
     * this will return {@code false}
     */
    public boolean allowsNameSiblings( Name primaryType,
                                       Set<Name> mixinTypes ) {
        if (isUnorderedCollection(primaryType, mixinTypes)) {
            // regardless of the actual types, if at least one of them is an unordered collection, SNS are not allowed
            return false;
        }
        if (nodeTypeNamesThatAllowSameNameSiblings.contains(primaryType)) return true;
        if (mixinTypes != null && !mixinTypes.isEmpty()) {
            for (Name mixinType : mixinTypes) {
                if (nodeTypeNamesThatAllowSameNameSiblings.contains(mixinType))
                    return true;
            }
        }
        return false;
    }

    /**
     * Determine if the named node type is or subtypes the 'mix:versionable' mixin type.
     *
     * @param nodeTypeName the node type name; may be null
     * @return true if any of the named node type is versionable, or false otherwise
     */
    public boolean isVersionable( Name nodeTypeName ) {
        return nodeTypeName != null && versionableNodeTypeNames.contains(nodeTypeName);
    }

    /**
     * Determine if the named node type or any of the mixin types subtypes the 'mode:unorderedCollection' type.
     *
     * @param nodeTypeName the node type name; may be null
     * @param mixinTypes the mixin type names; may be null or empty
     * @return true if any of the named node type is an unordered collection, or false otherwise
     */
    public boolean isUnorderedCollection( Name nodeTypeName, Collection<Name> mixinTypes ) {
        if (nodeTypeName != null && nodeTypeNamesThatAreUnorderableCollections.contains(nodeTypeName)) {
            return true;
        }
        if (mixinTypes != null && !mixinTypes.isEmpty()) {
            for (Name mixin : mixinTypes) {
                if (nodeTypeNamesThatAreUnorderableCollections.contains(mixin)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine the length of a bucket ID for an unordered collection, based on its type and possible mixins.
     * 
     * @param nodeTypeName the primary type of the collection; may not be null.
     * @param mixinTypes the mixin type names; may be null or empty
     * @return the order of magnitude, as a power of 16
     */
    public int getBucketIdLengthForUnorderedCollection( Name nodeTypeName, Set<Name> mixinTypes ) {
        Set<Name> allTypes = new LinkedHashSet<>();
        allTypes.add(nodeTypeName);
        if (mixinTypes != null && !mixinTypes.isEmpty()) {
            allTypes.addAll(mixinTypes);
        }
        for (Name typeName : allTypes) {
            if (isTypeOrSubtype(typeName, ModeShapeLexicon.TINY_UNORDERED_COLLECTION)) {
                return 1;
            } else if (isTypeOrSubtype(typeName, ModeShapeLexicon.SMALL_UNORDERED_COLLECTION)) {
                return 2;
            } else if (isTypeOrSubtype(typeName, ModeShapeLexicon.LARGE_UNORDERED_COLLECTION)) {
                return 3;
            } else if (isTypeOrSubtype(typeName, ModeShapeLexicon.HUGE_UNORDERED_COLLECTION)) {
                return 4;
            }
        }
        throw new IllegalArgumentException("None of the node types are known unordered collection types: " + allTypes);
    }

    /**
     * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:versionable' mixin type.
     *
     * @param primaryType the primary type name; may be null
     * @param mixinTypes the mixin type names; may not be null but may be empty
     * @return true if any of the named node types is versionable, or false if there are none
     */
    public boolean isVersionable( Name primaryType,
                                  Collection<Name> mixinTypes ) {
        if (primaryType != null && versionableNodeTypeNames.contains(primaryType)) return true;
        for (Name mixinType : mixinTypes) {
            if (versionableNodeTypeNames.contains(mixinType)) return true;
        }
        return false;
    }

    /**
     * Determine if the named property on the node type is a reference property.
     *
     * @param nodeTypeName the name of the node type; may not be null
     * @param propertyName the name of the property definition; may not be null
     * @return true if the property is a {@link PropertyType#REFERENCE}, {@link PropertyType#WEAKREFERENCE}, or
     *         {@link org.modeshape.jcr.api.PropertyType#SIMPLE_REFERENCE}, or false otherwise
     */
    public boolean isReferenceProperty( Name nodeTypeName,
                                        Name propertyName ) {
        JcrNodeType type = getNodeType(nodeTypeName);
        if (type != null) {
            for (JcrPropertyDefinition propDefn : type.allPropertyDefinitions(propertyName)) {
                int requiredType = propDefn.getRequiredType();
                if (requiredType == PropertyType.REFERENCE || requiredType == PropertyType.WEAKREFERENCE
                    || requiredType == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine if the named primary node type or mixin types has at least one mandatory property definitions declared on it or
     * any of its supertypes.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may not be null but may be empty
     * @return true if any of the named node types has one or more mandatory property definitions, or false if there are none
     */
    public boolean hasMandatoryPropertyDefinitions( Name primaryType,
                                                    Set<Name> mixinTypes ) {
        if (mandatoryPropertiesNodeTypes.containsKey(primaryType)) return true;
        for (Name mixinType : mixinTypes) {
            if (mandatoryPropertiesNodeTypes.containsKey(mixinType)) return true;
        }
        return false;
    }

    /**
     * Determine if the named primary node type or mixin types has at least one mandatory child node definitions declared on it or
     * any of its supertypes.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may not be null but may be empty
     * @return true if any of the the named node types has one or more mandatory child node definitions, or false if there are
     *         none
     */
    public boolean hasMandatoryChildNodeDefinitions( Name primaryType,
                                                     Set<Name> mixinTypes ) {
        if (mandatoryChildrenNodeTypes.containsKey(primaryType)) return true;
        for (Name mixinType : mixinTypes) {
            if (mandatoryChildrenNodeTypes.containsKey(mixinType)) return true;
        }
        return false;
    }

    /**
     * Get the mandatory property definitions for a node with the named primary type and mixin types. Note that the
     * {@link #hasMandatoryPropertyDefinitions(Name, Set)} method should first be called with the primary type and mixin types; if
     * that method returns <code>true</code>, then this method will never return an empty collection.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may not be null but may be empty
     * @return the collection of mandatory property definitions; never null but possibly empty
     */
    public Collection<JcrPropertyDefinition> getMandatoryPropertyDefinitions( Name primaryType,
                                                                              Set<Name> mixinTypes ) {
        if (mixinTypes.isEmpty()) {
            return mandatoryPropertiesNodeTypes.get(primaryType);
        }
        Set<JcrPropertyDefinition> defn = new HashSet<JcrPropertyDefinition>();
        defn.addAll(mandatoryPropertiesNodeTypes.get(primaryType));
        for (Name mixinType : mixinTypes) {
            defn.addAll(mandatoryPropertiesNodeTypes.get(mixinType));
        }
        return defn;
    }

    /**
     * Get the mandatory child node definitions for a node with the named primary type and mixin types. Note that the
     * {@link #hasMandatoryChildNodeDefinitions(Name, Set)} method should first be called with the primary type and mixin types;
     * if that method returns <code>true</code>, then this method will never return an empty collection.
     *
     * @param primaryType the primary type name; may not be null
     * @param mixinTypes the mixin type names; may not be null but may be empty
     * @return the collection of mandatory child node definitions; never null but possibly empty
     */
    public Collection<JcrNodeDefinition> getMandatoryChildNodeDefinitions( Name primaryType,
                                                                           Set<Name> mixinTypes ) {
        if (mixinTypes.isEmpty()) {
            return mandatoryChildrenNodeTypes.get(primaryType);
        }
        Set<JcrNodeDefinition> defn = new HashSet<JcrNodeDefinition>();
        defn.addAll(mandatoryChildrenNodeTypes.get(primaryType));
        for (Name mixinType : mixinTypes) {
            defn.addAll(mandatoryChildrenNodeTypes.get(mixinType));
        }
        return defn;
    }

    /**
     * Get the auto-created property definitions for the named node type. This method is used when
     * {@link AbstractJcrNode#addChildNode(Name, Name, NodeKey, boolean, boolean) creating nodes}, which only needs the
     * auto-created properties for the primary type. It's also used when {@link AbstractJcrNode#addMixin(String) adding a mixin}.
     *
     * @param nodeType the node type name; may not be null
     * @return the collection of auto-created property definitions; never null but possibly empty
     */
    public Collection<JcrPropertyDefinition> getAutoCreatedPropertyDefinitions( Name nodeType ) {
        return autoCreatedPropertiesNodeTypes.get(nodeType);
    }

    /**
     * Get the auto-created child node definitions for the named node type. This method is used when
     * {@link AbstractJcrNode#addChildNode(Name, Name, NodeKey, boolean, boolean) creating nodes}, which only needs the
     * auto-created properties for the primary type. It's also used when {@link AbstractJcrNode#addMixin(String) adding a mixin}.
     *
     * @param nodeType the node type name; may not be null
     * @return the collection of auto-created child node definitions; never null but possibly empty
     */
    public Collection<JcrNodeDefinition> getAutoCreatedChildNodeDefinitions( Name nodeType ) {
        return autoCreatedChildrenNodeTypes.get(nodeType);
    }

    /**
     * Return the immutable list of node types that are currently registered in this node type manager.
     *
     * @return the immutable collection of (immutable) node types; never null
     */
    public Collection<JcrNodeType> getAllNodeTypes() {
        return this.unmodifiableNodeTypes;
    }

    /**
     * Return an immutable snapshot of the names of the node types currently registered in this node type manager.
     *
     * @return the immutable collection of (immutable) node type names; never null
     */
    public Set<Name> getAllNodeTypeNames() {
        return this.unmodifiableNodeTypeNames;
    }

    /**
     * Return an immutable snapshot of the mixin node types that are currently registered in this node type manager.
     *
     * @return the immutable collection of (immutable) mixin node types; never null
     * @see #getPrimaryNodeTypes()
     */
    public Collection<JcrNodeType> getMixinNodeTypes() {
        return this.unmodifiableMixinNodeTypes;
    }

    /**
     * Determine whether the node type given by the supplied name is a mixin node type.
     *
     * @param nodeTypeName the name of the node type
     * @return true if there is an existing mixin node type with the supplied name, or false otherwise
     */
    public boolean isMixin( Name nodeTypeName ) {
        return unmodifiableMixinTypeNames.contains(nodeTypeName);
    }

    /**
     * Return an immutable snapshot of the primary node types that are currently registered in this node type manager.
     *
     * @return the immutable collection of (immutable) primary node types; never null
     * @see #getMixinNodeTypes()
     */
    public Collection<JcrNodeType> getPrimaryNodeTypes() {
        return this.unmodifiablePrimaryNodeTypes;
    }

    public JcrPropertyDefinition getPropertyDefinition( PropertyDefinitionId id ) {
        return propertyDefinitions.get(id);
    }

    public Collection<JcrPropertyDefinition> getAllPropertyDefinitions() {
        return propertyDefinitions.values();
    }

    public JcrNodeDefinition getChildNodeDefinition( NodeDefinitionId id ) {
        return childNodeDefinitions.get(id);
    }

    JcrNodeType getNodeType( Name nodeTypeName ) {
        return nodeTypes.get(nodeTypeName);
    }

    public NodeType getJcrNodeType( Name nodeTypeName ) {
        return nodeTypes.get(nodeTypeName);
    }

    /**
     * Check if the node type and mixin types are queryable or not. 
     *
     * @param nodeTypeName a {@link Name}, never {@code null}
     * @param mixinTypes the mixin type names; may not be null but may be empty 
     * 
     * @return {@code false} if at least one of the node types is not queryable, {@code true} otherwise
     */
    public boolean isQueryable(Name nodeTypeName, Set<Name> mixinTypes) {
        if (nonQueryableNodeTypes.contains(nodeTypeName)) {
            return false;
        }
        if (!mixinTypes.isEmpty()) {
            for (Name mixinType : mixinTypes) {
                if (nonQueryableNodeTypes.contains(mixinType)) {
                    return false;
                }
            }
        }
        return true;    
    }

    /**
     * Tests if the named node type is registered.
     * <p>
     * The return value of this method is equivalent to {@code getNodeType(nodeTypeName) != null}, although the implementation is
     * marginally more efficient that this approach.
     * </p>
     *
     * @param nodeTypeName the name of the node type to check
     * @return true if a node type with the given name is registered, false otherwise
     */
    boolean hasNodeType( Name nodeTypeName ) {
        return nodeTypes.containsKey(nodeTypeName);
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints}. Otherwise,
     * the process continues with each of the mixin types, in the order they are named.
     * </p>
     * <p>
     * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
     * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
     * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
     * </p>
     * <p>
     * If no matching property definition could be found, and the process repeats by searching the primary type (and then mixin
     * types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
     * definition's property type and still satisfy the definition's constraints.
     * </p>
     * <p>
     * If no matching property definition could be found, the previous step is repeated with multi-valued property definitions.
     * </p>
     * <p>
     * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
     * process repeats for residual property definitions (e.g., those that are defined with a {@link JcrNodeType#RESIDUAL_NAME "*"
     * name}).
     * </p>
     * <p>
     * Finally, if no satisfactory property definition could be found, this method returns null.
     * </p>
     *
     * @param session the session in which the constraints are to be checked; may not be null
     * @param primaryTypeName the name of the primary type; may not be null
     * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the search
     * @param propertyName the name of the property for which the definition should be retrieved. This method will automatically
     *        look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve only the best
     *        residual property definition (if any).
     * @param value the value, or null if the property is being removed
     * @param checkMultiValuedDefinitions true if the type's multi-valued property definitions should be considered, or false if
     *        only single-value property definitions should be considered
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the best property definition, or <code>null</code> if no property definition allows the property with the supplied
     *         name, type and number of values
     */
    JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                  Name primaryTypeName,
                                                  Collection<Name> mixinTypeNames,
                                                  Name propertyName,
                                                  Value value,
                                                  boolean checkMultiValuedDefinitions,
                                                  boolean skipProtected ) {
        return findPropertyDefinition(session, primaryTypeName, mixinTypeNames, propertyName, value, checkMultiValuedDefinitions,
                                      skipProtected, true);
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints}. Otherwise,
     * the process continues with each of the mixin types, in the order they are named.
     * </p>
     * <p>
     * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
     * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
     * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
     * </p>
     * <p>
     * If no matching property definition could be found, and the process repeats by searching the primary type (and then mixin
     * types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
     * definition's property type and still satisfy the definition's constraints.
     * </p>
     * <p>
     * If no matching property definition could be found, the previous step is repeated with multi-valued property definitions.
     * </p>
     * <p>
     * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
     * process repeats for residual property definitions (e.g., those that are defined with a {@link JcrNodeType#RESIDUAL_NAME "*"
     * name}).
     * </p>
     * <p>
     * Finally, if no satisfactory property definition could be found, this method returns null.
     * </p>
     *
     * @param session the session in which the constraints are to be checked; may not be null
     * @param primaryTypeName the name of the primary type; may not be null
     * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the search
     * @param propertyName the name of the property for which the definition should be retrieved. This method will automatically
     *        look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve only the best
     *        residual property definition (if any).
     * @param value the value, or null if the property is being removed
     * @param checkMultiValuedDefinitions true if the type's multi-valued property definitions should be considered, or false if
     *        only single-value property definitions should be considered
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @param checkTypeAndConstraints true if the type and constraints of the property definition should be checked, or false
     *        otherwise
     * @return the best property definition, or <code>null</code> if no property definition allows the property with the supplied
     *         name, type and number of values
     */
    JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                  Name primaryTypeName,
                                                  Collection<Name> mixinTypeNames,
                                                  Name propertyName,
                                                  Value value,
                                                  boolean checkMultiValuedDefinitions,
                                                  boolean skipProtected,
                                                  boolean checkTypeAndConstraints ) {
        boolean setToEmpty = value == null;

        /*
         * We use this flag to indicate that there was a definition encountered with the same name.  If
         * a named definition (or definitions - for example the same node type could define a LONG and BOOLEAN
         * version of the same property) is encountered and no match is found for the name, then processing should not
         * proceed.  If processing did proceed, a residual definition might be found and matched.  This would
         * lead to a situation where a node defined a type for a named property, but contained a property with
         * the same name and the wrong type.
         */
        boolean matchedOnName = false;

        // Look for a single-value property definition on the primary type that matches by name and type ...
        JcrNodeType primaryType = getNodeType(primaryTypeName);
        if (primaryType != null) {
            for (JcrPropertyDefinition definition : primaryType.allSingleValuePropertyDefinitions(propertyName)) {
                matchedOnName = true;
                // See if the definition allows the value ...
                if (skipProtected && definition.isProtected()) return null;
                if (setToEmpty) {
                    if (!definition.isMandatory()) return definition;
                    // Otherwise this definition doesn't work, so continue with the next ...
                    continue;
                }
                assert value != null;
                // We can use the definition if it matches the type and satisfies the constraints ...
                int type = definition.getRequiredType();
                // Don't check constraints on reference properties
                if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                if (type == PropertyType.UNDEFINED || type == value.getType()) {
                    if (!checkTypeAndConstraints) return definition;
                    if (definition.satisfiesConstraints(value, session)) return definition;
                }
            }

            if (matchedOnName) {
                if (value != null) {
                    for (JcrPropertyDefinition definition : primaryType.allSingleValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        // Don't check constraints on reference properties
                        int type = definition.getRequiredType();
                        if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                            return definition;
                        }
                        if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                            return definition;
                        }
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                            return definition;
                        }
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                    }
                }

                if (checkMultiValuedDefinitions) {
                    // Look for a multi-value property definition on the primary type that matches by name and type ...
                    for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        if (setToEmpty) {
                            if (!definition.isMandatory()) return definition;
                            // Otherwise this definition doesn't work, so continue with the next ...
                            continue;
                        }
                        assert value != null;
                        // We can use the definition if it matches the type and satisfies the constraints ...
                        int type = definition.getRequiredType();
                        // Don't check constraints on reference properties
                        if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.UNDEFINED || type == value.getType()) {
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.satisfiesConstraints(value, session)) return definition;
                        }
                    }
                    if (value != null) {
                        for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            assert definition.getRequiredType() != PropertyType.UNDEFINED;
                            // Don't check constraints on reference properties
                            int type = definition.getRequiredType();
                            if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                        }
                    }
                }
                return null;
            }
        }

        // Look for a single-value property definition on the mixin types that matches by name and type ...
        List<JcrNodeType> mixinTypes = null;
        if (mixinTypeNames != null) {
            mixinTypes = new LinkedList<JcrNodeType>();
            for (Name mixinTypeName : mixinTypeNames) {
                JcrNodeType mixinType = getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                mixinTypes.add(mixinType);
                for (JcrPropertyDefinition definition : mixinType.allSingleValuePropertyDefinitions(propertyName)) {
                    matchedOnName = true;
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    if (setToEmpty) {
                        if (!definition.isMandatory()) return definition;
                        // Otherwise this definition doesn't work, so continue with the next ...
                        continue;
                    }
                    assert value != null;
                    // We can use the definition if it matches the type and satisfies the constraints ...
                    int type = definition.getRequiredType();
                    // Don't check constraints on reference properties
                    if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                    if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                    if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                    if (type == PropertyType.UNDEFINED || type == value.getType()) {
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.satisfiesConstraints(value, session)) return definition;
                    }
                }
                if (matchedOnName) {
                    if (value != null) {
                        for (JcrPropertyDefinition definition : mixinType.allSingleValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            assert definition.getRequiredType() != PropertyType.UNDEFINED;
                            // Don't check constraints on reference properties
                            int type = definition.getRequiredType();
                            if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                        }
                    }

                    if (checkMultiValuedDefinitions) {
                        for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            if (setToEmpty) {
                                if (!definition.isMandatory()) return definition;
                                // Otherwise this definition doesn't work, so continue with the next ...
                                continue;
                            }
                            assert value != null;
                            // We can use the definition if it matches the type and satisfies the constraints ...
                            int type = definition.getRequiredType();
                            // Don't check constraints on reference properties
                            if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                            if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                            if (type == PropertyType.UNDEFINED || type == value.getType()) {
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.satisfiesConstraints(value, session)) return definition;
                            }
                        }
                        if (value != null) {
                            for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                                matchedOnName = true;
                                // See if the definition allows the value ...
                                if (skipProtected && definition.isProtected()) return null;
                                assert definition.getRequiredType() != PropertyType.UNDEFINED;
                                // Don't check constraints on reference properties
                                int type = definition.getRequiredType();
                                if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE
                                    && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                            }
                        }
                    }

                    return null;
                }
            }
        }

        if (checkMultiValuedDefinitions) {
            if (primaryType != null) {
                // Look for a multi-value property definition on the primary type that matches by name and type ...
                for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                    matchedOnName = true;
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    if (setToEmpty) {
                        if (!definition.isMandatory()) return definition;
                        // Otherwise this definition doesn't work, so continue with the next ...
                        continue;
                    }
                    assert value != null;
                    // We can use the definition if it matches the type and satisfies the constraints ...
                    int type = definition.getRequiredType();
                    // Don't check constraints on reference properties
                    if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                    if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                    if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                    if (type == PropertyType.UNDEFINED || type == value.getType()) {
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.satisfiesConstraints(value, session)) return definition;
                    }
                }
                if (value != null) {
                    for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                        matchedOnName = true;
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        assert definition.getRequiredType() != PropertyType.UNDEFINED;
                        // Don't check constraints on reference properties
                        int type = definition.getRequiredType();
                        if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                            return definition;
                        }
                        if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                            return definition;
                        }
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                            return definition;
                        }
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                    }
                }
            }

            if (matchedOnName) return null;

            if (mixinTypeNames != null) {
                mixinTypes = new LinkedList<JcrNodeType>();
                for (Name mixinTypeName : mixinTypeNames) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    mixinTypes.add(mixinType);
                    for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                        matchedOnName = true;
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        if (setToEmpty) {
                            if (!definition.isMandatory()) return definition;
                            // Otherwise this definition doesn't work, so continue with the next ...
                            continue;
                        }
                        assert value != null;
                        // We can use the definition if it matches the type and satisfies the constraints ...
                        int type = definition.getRequiredType();
                        // Don't check constraints on reference properties
                        if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.UNDEFINED || type == value.getType()) {
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.satisfiesConstraints(value, session)) return definition;
                        }
                    }
                    if (value != null) {
                        for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                            matchedOnName = true;
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            assert definition.getRequiredType() != PropertyType.UNDEFINED;
                            // Don't check constraints on reference properties
                            int type = definition.getRequiredType();
                            if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                        }
                    }
                }
            }
            if (matchedOnName) return null;

        }

        // Nothing was found, so look for residual property definitions ...
        if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return findPropertyDefinition(session, primaryTypeName,
                                                                                           mixinTypeNames,
                                                                                           JcrNodeType.RESIDUAL_NAME, value,
                                                                                           checkMultiValuedDefinitions,
                                                                                           skipProtected, checkTypeAndConstraints);
        return null;
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints}. Otherwise,
     * the process continues with each of the mixin types, in the order they are named.
     * </p>
     * <p>
     * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
     * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
     * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
     * </p>
     * <p>
     * If no matching property definition could be found, and the process repeats by searching the primary type (and then mixin
     * types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
     * definition's property type and still satisfy the definition's constraints.
     * </p>
     * <p>
     * If no matching property definition could be found, the previous step is repeated with multi-valued property definitions.
     * </p>
     * <p>
     * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
     * process repeats for residual property definitions (e.g., those that are defined with a {@link JcrNodeType#RESIDUAL_NAME "*"
     * name}).
     * </p>
     * <p>
     * Finally, if no satisfactory property definition could be found, this method returns null.
     * </p>
     *
     * @param session the session in which the constraints are to be checked; may not be null
     * @param primaryTypeName the name of the primary type; may not be null
     * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the search
     * @param propertyName the name of the property for which the definition should be retrieved. This method will automatically
     *        look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve only the best
     *        residual property definition (if any).
     * @param values the values
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the best property definition, or <code>null</code> if no property definition allows the property with the supplied
     *         name, type and number of values
     */
    JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                  Name primaryTypeName,
                                                  Collection<Name> mixinTypeNames,
                                                  Name propertyName,
                                                  Value[] values,
                                                  boolean skipProtected ) {
        return findPropertyDefinition(session, primaryTypeName, mixinTypeNames, propertyName, values, skipProtected, true);
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints}. Otherwise,
     * the process continues with each of the mixin types, in the order they are named.
     * </p>
     * <p>
     * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
     * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
     * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
     * </p>
     * <p>
     * If no matching property definition could be found, and the process repeats by searching the primary type (and then mixin
     * types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
     * definition's property type and still satisfy the definition's constraints.
     * </p>
     * <p>
     * If no matching property definition could be found, the previous step is repeated with multi-valued property definitions.
     * </p>
     * <p>
     * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
     * process repeats for residual property definitions (e.g., those that are defined with a {@link JcrNodeType#RESIDUAL_NAME "*"
     * name}).
     * </p>
     * <p>
     * Finally, if no satisfactory property definition could be found, this method returns null.
     * </p>
     *
     * @param session the session in which the constraints are to be checked; may not be null
     * @param primaryTypeName the name of the primary type; may not be null
     * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the search
     * @param propertyName the name of the property for which the definition should be retrieved. This method will automatically
     *        look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve only the best
     *        residual property definition (if any).
     * @param values the values
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @param checkTypeAndConstraints true if the type and constraints of the property definition should be checked, or false
     *        otherwise
     * @return the best property definition, or <code>null</code> if no property definition allows the property with the supplied
     *         name, type and number of values
     */
    JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                  Name primaryTypeName,
                                                  Collection<Name> mixinTypeNames,
                                                  Name propertyName,
                                                  Value[] values,
                                                  boolean skipProtected,
                                                  boolean checkTypeAndConstraints ) {
        boolean setToEmpty = values == null;
        int propertyType = values == null || values.length == 0 ? PropertyType.STRING : values[0].getType();

        /*
         * We use this flag to indicate that there was a definition encountered with the same name.  If
         * a named definition (or definitions - for example the same node type could define a LONG and BOOLEAN
         * version of the same property) is encountered and no match is found for the name, then processing should not
         * proceed.  If processing did proceed, a residual definition might be found and matched.  This would
         * lead to a situation where a node defined a type for a named property, but contained a property with
         * the same name and the wrong type.
         */
        boolean matchedOnName = false;

        // Look for a multi-value property definition on the primary type that matches by name and type ...
        JcrNodeType primaryType = getNodeType(primaryTypeName);
        if (primaryType != null) {
            for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                matchedOnName = true;
                // See if the definition allows the value ...
                if (skipProtected && definition.isProtected()) return null;
                if (setToEmpty) {
                    if (!definition.isMandatory()) return definition;
                    // Otherwise this definition doesn't work, so continue with the next ...
                    continue;
                }
                assert values != null;
                // We can use the definition if it matches the type and satisfies the constraints ...
                int type = definition.getRequiredType();
                boolean typeMatches = values.length == 0 || type == PropertyType.UNDEFINED || type == propertyType;
                // Don't check constraints on reference properties
                if (typeMatches) {
                    if (type == PropertyType.REFERENCE) return definition;
                    if (type == PropertyType.WEAKREFERENCE) return definition;
                    if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE) return definition;
                    if (!checkTypeAndConstraints) return definition;
                    if (definition.satisfiesConstraints(values, session)) return definition;
                }
            }

            if (matchedOnName) {
                if (values != null && values.length != 0) {
                    // Nothing was found with matching name and type, so look for definitions with
                    // matching name and an undefined or castable type ...

                    // Look for a multi-value property definition on the primary type that matches by name and type ...
                    for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        assert definition.getRequiredType() != PropertyType.UNDEFINED;
                        // Don't check constraints on reference properties
                        if (definition.getRequiredType() == PropertyType.REFERENCE && definition.canCastToType(values)) return definition;
                        if (definition.getRequiredType() == PropertyType.WEAKREFERENCE && definition.canCastToType(values)) return definition;
                        if (definition.getRequiredType() == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE
                            && definition.canCastToType(values)) return definition;
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.canCastToTypeAndSatisfyConstraints(values, session)) return definition;
                    }
                }

                return null;
            }
        }

        // Look for a multi-value property definition on the mixin types that matches by name and type ...
        List<JcrNodeType> mixinTypes = null;
        if (mixinTypeNames != null) {
            mixinTypes = new LinkedList<JcrNodeType>();
            for (Name mixinTypeName : mixinTypeNames) {
                JcrNodeType mixinType = getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                mixinTypes.add(mixinType);
                for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                    matchedOnName = true;
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    if (setToEmpty) {
                        if (!definition.isMandatory()) return definition;
                        // Otherwise this definition doesn't work, so continue with the next ...
                        continue;
                    }
                    assert values != null;
                    // We can use the definition if it matches the type and satisfies the constraints ...
                    int type = definition.getRequiredType();
                    boolean typeMatches = values.length == 0 || type == PropertyType.UNDEFINED || type == propertyType;
                    // Don't check constraints on reference properties
                    if (typeMatches) {
                        if (type == PropertyType.REFERENCE) return definition;
                        if (type == PropertyType.WEAKREFERENCE) return definition;
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE) return definition;
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.satisfiesConstraints(values, session)) return definition;
                    }
                }
                if (matchedOnName) {
                    if (values != null && values.length != 0) {
                        // Nothing was found with matching name and type, so look for definitions with
                        // matching name and an undefined or castable type ...

                        // Look for a multi-value property definition on the mixin type that matches by name and type ...
                        for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            assert definition.getRequiredType() != PropertyType.UNDEFINED;
                            // Don't check constraints on reference properties
                            if (definition.getRequiredType() == PropertyType.REFERENCE && definition.canCastToType(values)) return definition;
                            if (definition.getRequiredType() == PropertyType.WEAKREFERENCE && definition.canCastToType(values)) return definition;
                            if (definition.getRequiredType() == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE
                                && definition.canCastToType(values)) return definition;
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.canCastToTypeAndSatisfyConstraints(values, session)) return definition;
                        }
                    }

                    return null;
                }

            }
        }

        // Nothing was found, so look for residual property definitions ...
        if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return findPropertyDefinition(session, primaryTypeName,
                                                                                           mixinTypeNames,
                                                                                           JcrNodeType.RESIDUAL_NAME, values,
                                                                                           skipProtected, checkTypeAndConstraints);
        return null;
    }

    /**
     * Searches the supplied primary and mixin node types for all valid property definitions that match the given property name
     * and cardinality.
     * <p>
     * If no satisfactory property definition could be found, this method returns an empty list.
     * </p>
     *
     * @param typeNamesToCheck the name of the types to check; may not be null
     * @param propertyName the name of the property for which the definitions should be retrieved
     * @param typeToCheck the type of definitions to consider (single-valued only, multi-valued only, or all)
     * @param pendingTypes a list of types that have been created during type registration but not yet registered in the type map
     * @return a list of all valid property definitions that match the given property name and cardinality
     */
    List<JcrPropertyDefinition> findPropertyDefinitions( List<Name> typeNamesToCheck,
                                                         Name propertyName,
                                                         PropertyCardinality typeToCheck,
                                                         List<JcrNodeType> pendingTypes ) {
        assert typeNamesToCheck != null;

        Collection<JcrPropertyDefinition> propDefs = null;
        List<JcrPropertyDefinition> matchingDefs = new ArrayList<JcrPropertyDefinition>();

        // Look for a single-value property definition on the mixin types that matches by name and type ...
        for (Name typeNameToCheck : typeNamesToCheck) {
            JcrNodeType typeName = findTypeInMapOrList(typeNameToCheck, pendingTypes);
            if (typeName == null) continue;

            switch (typeToCheck) {
                case SINGLE_VALUED_ONLY:
                    propDefs = typeName.allSingleValuePropertyDefinitions(propertyName);
                    break;
                case MULTI_VALUED_ONLY:
                    propDefs = typeName.allMultiValuePropertyDefinitions(propertyName);
                    break;
                case ANY:
                    propDefs = typeName.allPropertyDefinitions(propertyName);
                    break;
                default:
                    throw new IllegalStateException("Should be unreachable: " + typeToCheck);
            }

            if (!propDefs.isEmpty()) matchingDefs.addAll(propDefs);
        }

        return matchingDefs;
    }

    /**
     * Determine if the property definitions of the supplied primary type and mixin types allow the property with the supplied
     * name to be removed.
     *
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param propertyName the name of the property to be removed; may not be null
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return true if at least one child node definition does not require children with the supplied name to exist, or false
     *         otherwise
     */
    boolean canRemoveProperty( Name primaryTypeNameOfParent,
                               List<Name> mixinTypeNamesOfParent,
                               Name propertyName,
                               boolean skipProtected ) {
        // First look in the primary type ...
        JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
        if (primaryType != null) {
            for (JcrPropertyDefinition definition : primaryType.allPropertyDefinitions(propertyName)) {
                // Skip protected definitions ...
                if (skipProtected && definition.isProtected()) continue;
                // If this definition is not mandatory, then we have found that we CAN remove the property ...
                return !definition.isMandatory();
            }
        }

        // Then, look in the mixin types ...
        if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
            for (Name mixinTypeName : mixinTypeNamesOfParent) {
                JcrNodeType mixinType = getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                for (JcrPropertyDefinition definition : mixinType.allPropertyDefinitions(propertyName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove the property ...
                    return !definition.isMandatory();
                }
            }
        }

        // Nothing was found, so look for residual node definitions ...
        if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return canRemoveProperty(primaryTypeNameOfParent,
                                                                                      mixinTypeNamesOfParent,
                                                                                      JcrNodeType.RESIDUAL_NAME, skipProtected);
        return false;
    }

    /**
     * Determine if the node and property definitions of the supplied primary type and mixin types allow the item with the
     * supplied name to be removed.
     *
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param itemName the name of the item to be removed; may not be null
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return true if at least one child node definition does not require children with the supplied name to exist, or false
     *         otherwise
     */
    boolean canRemoveItem( Name primaryTypeNameOfParent,
                           List<Name> mixinTypeNamesOfParent,
                           Name itemName,
                           boolean skipProtected ) {
        // First look in the primary type for a matching property definition...
        JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
        if (primaryType != null) {
            for (JcrPropertyDefinition definition : primaryType.allPropertyDefinitions(itemName)) {
                // Skip protected definitions ...
                if (skipProtected && definition.isProtected()) continue;
                // If this definition is not mandatory, then we have found that we CAN remove the property ...
                return !definition.isMandatory();
            }
        }

        // Then, look in the primary type for a matching child node definition...
        if (primaryType != null) {
            for (JcrNodeDefinition definition : primaryType.allChildNodeDefinitions(itemName)) {
                // Skip protected definitions ...
                if (skipProtected && definition.isProtected()) continue;
                // If this definition is not mandatory, then we have found that we CAN remove all children ...
                return !definition.isMandatory();
            }
        }

        // Then, look in the mixin types for a matching property definition...
        if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
            for (Name mixinTypeName : mixinTypeNamesOfParent) {
                JcrNodeType mixinType = getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                for (JcrPropertyDefinition definition : mixinType.allPropertyDefinitions(itemName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove the property ...
                    return !definition.isMandatory();
                }
            }
        }

        // Then, look in the mixin types for a matching child node definition...
        if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
            for (Name mixinTypeName : mixinTypeNamesOfParent) {
                JcrNodeType mixinType = getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                for (JcrNodeDefinition definition : mixinType.allChildNodeDefinitions(itemName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove all children ...
                    return !definition.isMandatory();
                }
            }
        }

        // Nothing was found, so look for residual item definitions ...
        if (!itemName.equals(JcrNodeType.RESIDUAL_NAME)) return canRemoveItem(primaryTypeNameOfParent, mixinTypeNamesOfParent,
                                                                              JcrNodeType.RESIDUAL_NAME, skipProtected);
        return false;
    }

    protected final JcrNodeDefinition findChildNodeDefinitionForUnstructured() {
        return ntUnstructuredSnsChildDefinition;
    }

    private Set<Name> mixinsWithChildNodeDefinitions( Set<Name> mixinTypes ) {
        if (mixinTypes == null || mixinTypes.isEmpty()) return NONE;
        if (nodeTypeNamesWithNoChildNodeDefns.containsAll(mixinTypes)) {
            // None of the mixins has a child node definition. This is usually true of all mixins, so this will happen a lot ...
            return NONE;
        }
        // We know at least one of the mixins defines child node definitions ...
        if (mixinTypes.size() == 1) return mixinTypes;

        // This happens pretty infrequently ...
        Set<Name> result = new HashSet<Name>();
        for (Name mixinType : mixinTypes) {
            if (!nodeTypeNamesWithNoChildNodeDefns.contains(mixinType)) result.add(mixinType);
        }
        assert !result.isEmpty();
        return result;
    }

    private NodeDefinitionSet use( ReusableNodeDefinitionSet defnSet ) {
        nodeDefinitionSet.set(defnSet);
        return defnSet;
    }

    /**
     * Searches the supplied primary and mixin node types for all valid child node definitions that match the given child node
     * name and cardinality.
     * <p>
     * If no satisfactory child node definition could be found, this method returns an empty list.
     * </p>
     *
     * @param typeNamesToCheck the name of the types to check; may not be null
     * @param childNodeName the name of the child node for which the definitions should be retrieved
     * @param typesToCheck the type of definitions to consider (allows SNS or does not allow SNS)
     * @param pendingTypes a list of types that have been created during type registration but not yet registered in the type map
     * @return a list of all valid chlid node definitions that match the given child node name and cardinality
     */
    List<JcrNodeDefinition> findChildNodeDefinitions( List<Name> typeNamesToCheck,
                                                      Name childNodeName,
                                                      NodeCardinality typesToCheck,
                                                      List<JcrNodeType> pendingTypes ) {
        assert typeNamesToCheck != null;
        Collection<JcrNodeDefinition> nodeDefs = null;
        List<JcrNodeDefinition> matchingDefs = new ArrayList<JcrNodeDefinition>();

        for (Name typeNameToCheck : typeNamesToCheck) {
            JcrNodeType typeName = findTypeInMapOrList(typeNameToCheck, pendingTypes);
            if (typeName == null) continue;

            switch (typesToCheck) {
                case NO_SAME_NAME_SIBLINGS:
                    nodeDefs = typeName.allChildNodeDefinitions(childNodeName, false);
                    break;
                case SAME_NAME_SIBLINGS:
                    nodeDefs = typeName.allChildNodeDefinitions(childNodeName, true);
                    break;
                case ANY:
                    nodeDefs = typeName.allChildNodeDefinitions(childNodeName);
                    break;
            }

            assert nodeDefs != null;
            for (JcrNodeDefinition definition : nodeDefs) {
                if (NodeCardinality.NO_SAME_NAME_SIBLINGS == typesToCheck && definition.allowsSameNameSiblings()) continue;
                matchingDefs.add(definition);
            }
        }

        return matchingDefs;
    }

    /**
     * Determine if the child node definitions of the supplied primary type and mixin types of a parent node allow all of the
     * children with the supplied name to be removed.
     *
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param childName the name of the child to be added to the parent; may not be null
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return true if at least one child node definition does not require children with the supplied name to exist, or false
     *         otherwise
     */
    boolean canRemoveAllChildren( Name primaryTypeNameOfParent,
                                  Collection<Name> mixinTypeNamesOfParent,
                                  Name childName,
                                  boolean skipProtected ) {
        // First look in the primary type ...
        JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
        if (primaryType != null) {
            for (JcrNodeDefinition definition : primaryType.allChildNodeDefinitions(childName)) {
                // Skip protected definitions ...
                if (skipProtected && definition.isProtected()) continue;
                // If this definition is not mandatory, then we have found that we CAN remove all children ...
                return !definition.isMandatory();
            }
        }

        // Then, look in the mixin types ...
        if (mixinTypeNamesOfParent != null) {
            for (Name mixinTypeName : mixinTypeNamesOfParent) {
                JcrNodeType mixinType = getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                for (JcrNodeDefinition definition : mixinType.allChildNodeDefinitions(childName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove all children ...
                    return !definition.isMandatory();
                }
            }
        }

        // Nothing was found, so look for residual node definitions ...
        if (!childName.equals(JcrNodeType.RESIDUAL_NAME)) return canRemoveAllChildren(primaryTypeNameOfParent,
                                                                                      mixinTypeNamesOfParent,
                                                                                      JcrNodeType.RESIDUAL_NAME, skipProtected);
        return false;
    }

    /**
     * Finds the named type in the given collection of types pending registration if it exists, else returns the type definition
     * from the repository
     *
     * @param typeName the name of the type to retrieve
     * @param pendingList a collection of types that have passed validation but have not yet been committed to the repository
     * @return the node type with the given name from {@code pendingList} if it exists in the collection or from the
     *         {@link #nodeTypes registered types} if it exists there; may be null
     */
    protected JcrNodeType findTypeInMapOrList( Name typeName,
                                               Collection<JcrNodeType> pendingList ) {
        for (JcrNodeType pendingNodeType : pendingList) {
            if (pendingNodeType.getInternalName().equals(typeName)) {
                return pendingNodeType;
            }
        }

        return nodeTypes.get(typeName);
    }

    /**
     * Returns the list of node types for the supertypes defined in the given node type.
     *
     * @param nodeType a node type with a non-null array of supertypes
     * @param pendingTypes the list of types that have been processed in this type batch but not yet committed to the repository's
     *        set of types
     * @return a list of node types where each element is the node type for the corresponding element of the array of supertype
     *         names
     * @throws RepositoryException if any of the names in the array of supertype names does not correspond to an
     *         already-registered node type or a node type that is pending registration
     */
    protected List<JcrNodeType> supertypesFor( NodeTypeDefinition nodeType,
                                               Collection<JcrNodeType> pendingTypes ) throws RepositoryException {
        assert nodeType != null;

        List<JcrNodeType> supertypes = new LinkedList<JcrNodeType>();

        boolean isMixin = nodeType.isMixin();
        boolean needsPrimaryAncestor = !isMixin;
        String nodeTypeName = nodeType.getName();

        for (String supertypeNameStr : nodeType.getDeclaredSupertypeNames()) {
            Name supertypeName = nameFactory.create(supertypeNameStr);
            JcrNodeType supertype = findTypeInMapOrList(supertypeName, pendingTypes);
            if (supertype == null) {
                throw new InvalidNodeTypeDefinitionException(JcrI18n.invalidSupertypeName.text(supertypeNameStr, nodeTypeName));
            }
            needsPrimaryAncestor &= supertype.isMixin();
            supertypes.add(supertype);
        }

        // primary types (other than nt:base) always have at least one ancestor that's a primary type - nt:base
        if (needsPrimaryAncestor) {
            Name nodeName = nameFactory.create(nodeTypeName);
            if (!JcrNtLexicon.BASE.equals(nodeName)) {
                JcrNodeType ntBase = findTypeInMapOrList(JcrNtLexicon.BASE, pendingTypes);
                assert ntBase != null;
                supertypes.add(0, ntBase);
            }
        }
        return supertypes;
    }

    /**
     * Returns the list of subtypes for the given node.
     *
     * @param nodeType the node type for which subtypes should be returned; may not be null
     * @return the subtypes for the node
     */
    final Collection<JcrNodeType> subtypesFor( JcrNodeType nodeType ) {
        List<JcrNodeType> subtypes = new LinkedList<JcrNodeType>();
        for (JcrNodeType type : this.nodeTypes.values()) {
            if (type.supertypes().contains(nodeType)) {
                subtypes.add(type);
            }
        }
        return subtypes;
    }

    /**
     * Returns the list of declared subtypes for the given node.
     *
     * @param nodeType the node type for which declared subtypes should be returned; may not be null
     * @return the subtypes for the node
     */
    final Collection<JcrNodeType> declaredSubtypesFor( JcrNodeType nodeType ) {
        CheckArg.isNotNull(nodeType, "nodeType");
        String nodeTypeName = nodeType.getName();
        List<JcrNodeType> subtypes = new LinkedList<JcrNodeType>();
        for (JcrNodeType type : this.nodeTypes.values()) {
            if (Arrays.asList(type.getDeclaredSupertypeNames()).contains(nodeTypeName)) {
                subtypes.add(type);
            }
        }
        return subtypes;
    }

    /**
     * Validates that the supertypes are compatible under ModeShape restrictions.
     * <p>
     * ModeShape imposes the following rules on the supertypes of a type:
     * <ol>
     * <li>The type must have at least one supertype (unless the type is {@code nt:base}.</li>
     * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a property ({@code p1} and {@code p2}) with the
     * same name and cardinality ({@code p1.isMultiple() == p2.isMultiple()}). Note that this does prohibit each {@code t1} and
     * {@code t2} from having a common supertype (or super-supertype, etc.) that declares a property).</li>
     * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a child node ({@code n1} and {@code n2}) with the
     * same name and SNS status ({@code p1.allowsSameNameSiblings() == p2.allowsSameNameSiblings()}). Note that this does prohibit
     * each {@code t1} and {@code t2} from having a common supertype (or super-supertype, etc.) that declares a child node).</li>
     * </ol>
     * </p>
     * <p>
     * If any of these rules are violated, a {@link RepositoryException} is thrown.
     * </p>
     *
     * @param supertypes the supertypes of this node type
     * @throws RepositoryException if any of the rules described above are violated
     */
    private void validateSupertypes( List<JcrNodeType> supertypes ) throws RepositoryException {
        assert supertypes != null;

        Map<PropertyDefinitionId, JcrPropertyDefinition> props = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();

        for (JcrNodeType supertype : supertypes) {
            for (JcrPropertyDefinition property : supertype.propertyDefinitions()) {
                JcrPropertyDefinition oldProp = props.put(new PropertyDefinitionId(property.getInternalName(),
                                                                                   property.getInternalName(),
                                                                                   PropertyType.UNDEFINED, property.isMultiple()),
                                                          property);
                if (oldProp != null) {
                    String oldPropTypeName = oldProp.getDeclaringNodeType().getName();
                    String propTypeName = property.getDeclaringNodeType().getName();
                    if (!oldPropTypeName.equals(propTypeName)) {
                        throw new InvalidNodeTypeDefinitionException(JcrI18n.supertypesConflict.text(oldPropTypeName,
                                                                                                     propTypeName, "property",
                                                                                                     property.getName()));
                    }
                }
            }
        }

        Map<NodeDefinitionId, JcrNodeDefinition> childNodes = new HashMap<NodeDefinitionId, JcrNodeDefinition>();

        for (JcrNodeType supertype : supertypes) {
            for (JcrNodeDefinition childNode : supertype.childNodeDefinitions()) {
                JcrNodeDefinition oldNode = childNodes.put(new NodeDefinitionId(childNode.getInternalName(),
                                                                                childNode.getInternalName(), new Name[0]),
                                                           childNode);
                if (oldNode != null) {
                    String oldNodeTypeName = oldNode.getDeclaringNodeType().getName();
                    String childNodeTypeName = childNode.getDeclaringNodeType().getName();
                    if (!oldNodeTypeName.equals(childNodeTypeName)) {
                        throw new InvalidNodeTypeDefinitionException(JcrI18n.supertypesConflict.text(oldNodeTypeName,
                                                                                                     childNodeTypeName,
                                                                                                     "child node",
                                                                                                     childNode.getName()));
                    }
                }
            }
        }
    }

    /**
     * Validates that the given node type definition is valid under the ModeShape and JCR type rules within the given context.
     *
     * @param nodeType the node type to attempt to validate
     * @param supertypes the names of the supertypes of the node type
     * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
     * @throws RepositoryException if the given node type template is not valid
     */
    protected void validate( JcrNodeType nodeType,
                             List<JcrNodeType> supertypes,
                             List<JcrNodeType> pendingTypes ) throws RepositoryException {
        validateSupertypes(supertypes);

        List<Name> supertypeNames = new ArrayList<Name>(supertypes.size());
        for (JcrNodeType supertype : supertypes) {
            supertypeNames.add(supertype.getInternalName());
        }

        boolean foundExact = false;
        boolean foundResidual = false;
        boolean foundSNS = false;
        Name primaryItemName = nodeType.getInternalPrimaryItemName();

        for (JcrNodeDefinition node : nodeType.getDeclaredChildNodeDefinitions()) {
            validateChildNodeDefinition(node, supertypeNames, pendingTypes);
            if (node.isResidual()) {
                foundResidual = true;
            }

            if (primaryItemName != null && primaryItemName.equals(node.getInternalName())) {
                foundExact = true;
            }
            
            if (node.allowsSameNameSiblings()) {
                foundSNS = true;
            }
        }

        for (JcrPropertyDefinition prop : nodeType.getDeclaredPropertyDefinitions()) {
            validatePropertyDefinition(prop, supertypeNames, pendingTypes);
            if (prop.isResidual()) {
                foundResidual = true;
            }
            if (primaryItemName != null && primaryItemName.equals(prop.getInternalName())) {
                if (foundExact) {
                    throw new RepositoryException(JcrI18n.ambiguousPrimaryItemName.text(primaryItemName));
                }
                foundExact = true;
            }
        }

        if (primaryItemName != null && !foundExact && !foundResidual) {
            throw new RepositoryException(JcrI18n.invalidPrimaryItemName.text(primaryItemName));
        }

        Name internalName = nodeType.getInternalName();
        if (isUnorderedCollection(internalName, supertypeNames)) {
            boolean isVersionable = isVersionable(internalName, supertypeNames);
            if (isVersionable || foundSNS || nodeType.hasOrderableChildNodes()) {
                throw new RepositoryException(JcrI18n.invalidUnorderedCollectionType.text(internalName.toString()));
            }
        }
    }

    /**
     * Validates that the given child node definition is valid under the ModeShape and JCR type rules within the given context.
     * <p>
     * ModeShape considers a child node definition valid if it meets these criteria:
     * <ol>
     * <li>Residual child node definitions cannot be mandatory</li>
     * <li>If the child node is auto-created, it must specify a default primary type name</li>
     * <li>If the child node overrides an existing child node definition from a supertype, the new definition must be mandatory if
     * the old definition was mandatory</li>
     * <li>The child node cannot override an existing child node definition from a supertype if the ancestor definition is
     * protected</li>
     * <li>If the child node overrides an existing child node definition from a supertype, the required primary types of the new
     * definition must be more restrictive than the required primary types of the old definition - that is, the new primary types
     * must defined such that any type that satisfies all of the required primary types for the new definition must also satisfy
     * all of the required primary types for the old definition. This requirement is analogous to the requirement that overriding
     * property definitions have a required type that is always convertible to the required type of the overridden definition.</li>
     * </ol>
     * </p>
     *
     * @param childNodeDefinition the child node definition to be validated
     * @param supertypes the names of the supertypes of the node type to which this child node belongs
     * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
     * @throws RepositoryException if the child node definition is not valid
     */
    private void validateChildNodeDefinition( JcrNodeDefinition childNodeDefinition,
                                              List<Name> supertypes,
                                              List<JcrNodeType> pendingTypes ) throws RepositoryException {
        if (childNodeDefinition.isAutoCreated() && !childNodeDefinition.isProtected()
            && childNodeDefinition.defaultPrimaryTypeName() == null) {
            throw new InvalidNodeTypeDefinitionException(JcrI18n.autocreatedNodesNeedDefaults.text(childNodeDefinition.getName()));
        }
        boolean residual = JcrNodeType.RESIDUAL_ITEM_NAME.equals(childNodeDefinition.getName());
        if (childNodeDefinition.isMandatory() && residual) {
            throw new InvalidNodeTypeDefinitionException(
                                                         JcrI18n.residualNodeDefinitionsCannotBeMandatory.text(childNodeDefinition.getName()));
        }
        if (childNodeDefinition.isAutoCreated() && residual) {
            throw new InvalidNodeTypeDefinitionException(
                                                         JcrI18n.residualNodeDefinitionsCannotBeAutoCreated.text(childNodeDefinition.getName()));
        }

        Name childNodeName = context.getValueFactories().getNameFactory().create(childNodeDefinition.getName());
        childNodeName = childNodeName == null ? JcrNodeType.RESIDUAL_NAME : childNodeName;

        List<JcrNodeDefinition> childNodesInAncestors = findChildNodeDefinitions(supertypes, childNodeName, NodeCardinality.ANY,
                                                                                 pendingTypes);

        for (JcrNodeDefinition childNodeFromAncestor : childNodesInAncestors) {
            if (childNodeFromAncestor.isProtected()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotOverrideProtectedDefinition.text(childNodeFromAncestor.getDeclaringNodeType()
                                                                                                                                 .getName(),
                                                                                                            "child node"));
            }

            if (childNodeFromAncestor.isMandatory() && !childNodeDefinition.isMandatory()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotMakeMandatoryDefinitionOptional.text(childNodeFromAncestor.getDeclaringNodeType()
                                                                                                                                     .getName(),
                                                                                                                "child node"));

            }

            Name[] requiredPrimaryTypeNames = childNodeFromAncestor.requiredPrimaryTypeNames();
            for (Name requiredPrimaryTypeName : requiredPrimaryTypeNames) {
                NodeType requiredPrimaryTypeFromAncestor = findTypeInMapOrList(requiredPrimaryTypeName, pendingTypes);

                if (requiredPrimaryTypeFromAncestor == null) {
                    I18n msg = JcrI18n.couldNotFindDefinitionOfRequiredPrimaryType;
                    throw new InvalidNodeTypeDefinitionException(msg.text(requiredPrimaryTypeName, childNodeDefinition.getName(),
                                                                          childNodeDefinition.getDeclaringNodeType()));

                }

                boolean found = false;

                for (Name name : childNodeDefinition.requiredPrimaryTypeNames()) {
                    JcrNodeType childNodePrimaryType = findTypeInMapOrList(name, pendingTypes);

                    if (childNodePrimaryType != null
                        && childNodePrimaryType.isNodeType(requiredPrimaryTypeFromAncestor.getName())) {
                        found = true;
                        break;
                    }
                }

                // Allow side-by-side definitions of residual child nodes per JCR 1.0.1 spec 6.7.8
                if (!found && !residual) {
                    I18n msg = JcrI18n.cannotRedefineChildNodeWithIncompatibleDefinition;
                    throw new InvalidNodeTypeDefinitionException(msg.text(childNodeName,
                                                                          requiredPrimaryTypeFromAncestor.getName(),
                                                                          childNodeDefinition.getDeclaringNodeType()));
                }
            }
        }
    }

    /**
     * Validates that the given property definition is valid under the ModeShape and JCR type rules within the given context.
     * <p>
     * ModeShape considers a property definition valid if it meets these criteria:
     * <ol>
     * <li>Residual properties cannot be mandatory</li>
     * <li>If the property is auto-created, it must specify a default value</li>
     * <li>If the property is single-valued, it can only specify a single default value</li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must be mandatory if the
     * old definition was mandatory</li>
     * <li>The property cannot override an existing property definition from a supertype if the ancestor definition is protected</li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must have the same
     * required type as the old definition or a required type that can ALWAYS be cast to the required type of the ancestor (see
     * section 3.6.4 of the JCR 2.0 specification)</li>
     * </ol>
     * Note that an empty set of properties would meet the criteria above.
     * </p>
     *
     * @param propertyDefinition the property definition to be validated
     * @param supertypes the names of the supertypes of the node type to which this property belongs
     * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
     * @throws RepositoryException if the property definition is not valid
     */
    private void validatePropertyDefinition( JcrPropertyDefinition propertyDefinition,
                                             List<Name> supertypes,
                                             List<JcrNodeType> pendingTypes ) throws RepositoryException {
        assert propertyDefinition != null;
        assert supertypes != null;
        assert pendingTypes != null;

        boolean residual = JcrNodeType.RESIDUAL_ITEM_NAME.equals(propertyDefinition.getName());
        if (propertyDefinition.isMandatory() && !propertyDefinition.isProtected() && residual) {
            throw new InvalidNodeTypeDefinitionException(
                                                         JcrI18n.residualPropertyDefinitionsCannotBeMandatory.text(propertyDefinition.getName()));
        }
        if (propertyDefinition.isAutoCreated() && residual) {
            throw new InvalidNodeTypeDefinitionException(
                                                         JcrI18n.residualPropertyDefinitionsCannotBeAutoCreated.text(propertyDefinition.getName()));
        }

        Value[] defaultValues = propertyDefinition.getDefaultValues();
        if (propertyDefinition.isAutoCreated() && !propertyDefinition.isProtected()
            && (defaultValues == null || defaultValues.length == 0)) {
            throw new InvalidNodeTypeDefinitionException(
                                                         JcrI18n.autocreatedPropertyNeedsDefault.text(propertyDefinition.getName(),
                                                                                                      propertyDefinition.getDeclaringNodeType()
                                                                                                                        .getName()));
        }

        if (!propertyDefinition.isMultiple() && (defaultValues != null && defaultValues.length > 1)) {
            throw new InvalidNodeTypeDefinitionException(
                                                         JcrI18n.singleValuedPropertyNeedsSingleValuedDefault.text(propertyDefinition.getName(),
                                                                                                                   propertyDefinition.getDeclaringNodeType()
                                                                                                                                     .getName()));
        }

        Name propName = context.getValueFactories().getNameFactory().create(propertyDefinition.getName());
        propName = propName == null ? JcrNodeType.RESIDUAL_NAME : propName;

        List<JcrPropertyDefinition> propertyDefinitionsFromAncestors = findPropertyDefinitions(supertypes,
                                                                                               propName,
                                                                                               propertyDefinition.isMultiple() ? PropertyCardinality.MULTI_VALUED_ONLY : PropertyCardinality.SINGLE_VALUED_ONLY,
                                                                                               pendingTypes);

        for (JcrPropertyDefinition propertyDefinitionFromAncestor : propertyDefinitionsFromAncestors) {
            if (propertyDefinitionFromAncestor.isProtected()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotOverrideProtectedDefinition.text(propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                                          .getName(),
                                                                                                            "property"));
            }

            if (propertyDefinitionFromAncestor.isMandatory() && !propertyDefinition.isMandatory()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotMakeMandatoryDefinitionOptional.text(propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                                              .getName(),
                                                                                                                "property"));

            }

            if (!propertyDefinition.isAsOrMoreConstrainedThan(propertyDefinitionFromAncestor, context)) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.constraintsChangedInSubtype.text(propName,
                                                                                                      propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                                    .getName()));
            }

            if (!isAlwaysSafeConversion(propertyDefinition.getRequiredType(), propertyDefinitionFromAncestor.getRequiredType())) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotRedefineProperty.text(propName,
                                                                                                 org.modeshape.jcr.api.PropertyType.nameFromValue(propertyDefinition.getRequiredType()),
                                                                                                 propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                               .getName(),
                                                                                                 org.modeshape.jcr.api.PropertyType.nameFromValue(propertyDefinitionFromAncestor.getRequiredType())));

            }
        }
    }

    /**
     * Returns whether it is always possible to convert a value with JCR property type {@code fromType} to {@code toType}.
     * <p>
     * This method is based on the conversions which can never throw an exception in the chart in section 3.6.4 of the JCR 2.0
     * specification.
     * </p>
     *
     * @param fromType the type to be converted from
     * @param toType the type to convert to
     * @return true if any value with type {@code fromType} can be converted to a type of {@code toType} without a
     *         {@link ValueFormatException} being thrown.
     * @see PropertyType
     */
    private boolean isAlwaysSafeConversion( int fromType,
                                            int toType ) {

        if (fromType == toType) return true;

        switch (toType) {
            case PropertyType.BOOLEAN:
                return fromType == PropertyType.BINARY || fromType == PropertyType.STRING;

            case PropertyType.DATE:
                return fromType == PropertyType.DOUBLE || fromType == PropertyType.LONG;

            case PropertyType.DOUBLE:
                // Conversion from DATE could result in out-of-range value
                return fromType == PropertyType.LONG;
            case PropertyType.LONG:
                // Conversion from DATE could result in out-of-range value
                return fromType == PropertyType.DOUBLE;

            case PropertyType.PATH:
                return fromType == PropertyType.NAME;

                // Values of any type MAY fail when converting to these types
            case PropertyType.NAME:
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
            case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                return false;

                // Any type can be converted to these types
            case PropertyType.BINARY:
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
                return true;

            default:
                throw new IllegalStateException("Unexpected state: " + toType);
        }
    }

    @Override
    public String toString() {
        return getAllNodeTypes().toString();
    }

    /**
     * Return the {@link NodeDefinitionSet set of child node definitions} for a parent's primary node type and mixin node types.
     * The resulting object can be used to find the best child node definition for a new child with the given name and primary
     * primary node type name. The algorithm used will prefer child node definitions that allow same name siblings in an attempt
     * to delay as much as possible the (potentially very expensive) determination of whether there are existing children with the
     * same name.
     * <p>
     * This method also uses a thread-based cache so that sequential calls to add children under the same parent node will be
     * noticeably faster.
     *
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @return the set of child node definitions that can be used to find an appropriate child node definition for one or more
     *         children under the same parent; never null
     */
    NodeDefinitionSet findChildNodeDefinitions( Name primaryTypeNameOfParent,
                                                Set<Name> mixinTypeNamesOfParent ) {
        // See if this is the same parent primaryType & mixins that we just used. This is often the case for a session
        // adding multiple children to the same parent ...
        ReusableNodeDefinitionSet lastDefnSet = nodeDefinitionSet.get();
        if (lastDefnSet.appliesTo(this, primaryTypeNameOfParent, mixinTypeNamesOfParent)) return lastDefnSet;

        Set<Name> mixinsWithChildDefns = mixinsWithChildNodeDefinitions(mixinTypeNamesOfParent);

        // If this is one of the special built-in cases ...
        for (ReusableNodeDefinitionSet defnSet : standardNodeDefinitionSets) {
            if (defnSet.appliesTo(this, primaryTypeNameOfParent, mixinsWithChildDefns)) {
                return use(defnSet);
            }
        }

        // Go through the primary type ...
        JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
        assert primaryType != null;
        Collection<JcrNodeDefinition> defns = primaryType.allChildNodeDefinitions();
        if (mixinsWithChildDefns.isEmpty()) {
            if (defns.isEmpty()) {
                // No child node definitions ...
                return use(new NoChildrenNodeDefinitionSet(primaryTypeNameOfParent, mixinsWithChildDefns));
            }
            if (defns.size() == 1) {
                JcrNodeDefinition defn = defns.iterator().next();
                return use(new SingleNodeDefinitionSet(primaryTypeNameOfParent, mixinsWithChildDefns, defn));
            }
            // There are multiple child node definitions in the primary type, and no mixins with child node defns ...
            return use(new MultipleNodeDefinitionSet(primaryTypeNameOfParent, null));
        }

        // There is a primary type and at least one mixin with child node definitions ...
        return use(new MultipleNodeDefinitionSet(primaryTypeNameOfParent, mixinsWithChildDefns));
    }

    /**
     * A set of child node definitions under a parent with a specific primary type and optional mixin types.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface NodeDefinitionSet {
        /**
         * Find the best child node definition for creating a new child with the given name and primary type. If none of the
         * parent's child node definitions allow a child with the given name and type, then null is returned and the caller can
         * call {@link #determineReasonForMismatch} for an appropriate exception.
         *
         * @param childName the name of the proposed child; may be null if the child name is to be determined by the definition
         * @param childPrimaryType the primary type of the proposed child; may be null if the default type is to be used
         * @param skipProtected true if this method should not consider any of the parent's child node definitions that are
         *        protected, or false if it should consider all child node definitions
         * @param siblingCounter a function that this method can call to determine how many siblings with the child's name already
         *        exist under the parent
         * @return the child node definition for the child, or null if one could not be found
         */
        JcrNodeDefinition findBestDefinitionForChild( Name childName,
                                                      Name childPrimaryType,
                                                      boolean skipProtected,
                                                      SiblingCounter siblingCounter );

        /**
         * Method that can be called after {@link #findBestDefinitionForChild(Name, Name, boolean, SiblingCounter)} returns null
         * to throw an appropriate exception for why each of the parent's child node definition are not applicable for the
         * proposed child.
         *
         * @param childName the name of the proposed child; may be null if the child name is to be determined by the definition
         * @param childPrimaryType the primary type of the proposed child; may be null if the default type is to be used
         * @param skipProtected true if this method should not consider any of the parent's child node definitions that are
         *        protected, or false if it should consider all child node definitions
         * @param siblingCounter a function that this method can call to determine how many siblings with the child's name already
         *        exist under the parent
         * @param parentPrimaryType the primary type of the parent node; may not be null
         * @param parentMixinTypes the mixin types of the parent node
         * @param parentPath
         * @param workspaceName
         * @param repositoryName
         * @param context
         * @throws ConstraintViolationException
         * @throws ItemExistsException
         */
        void determineReasonForMismatch( Name childName,
                                         Name childPrimaryType,
                                         boolean skipProtected,
                                         SiblingCounter siblingCounter,
                                         Name parentPrimaryType,
                                         Set<Name> parentMixinTypes,
                                         Path parentPath,
                                         String workspaceName,
                                         String repositoryName,
                                         ExecutionContext context ) throws ConstraintViolationException, ItemExistsException;
    }

    /**
     * This class is used to build up a useful error message that includes why each of the child node definitions did not match.
     * When this class is used, we already know that no match was found and the algorithm is repeated with an instance of this
     * class to capture the reasons why each child node definition could not be used. So this classIt is fairly expensive, but
     * we're already in a failed attempt that will result in an exception.
     */
    private final class MatchResults {
        private final String childName;
        private final String childPrimaryType;
        private final String parentPrimaryType;
        private final String parentMixinTypes;
        private final String parentPath;
        private final String workspaceName;
        private final String repositoryName;
        private final StringBuilder reasons = new StringBuilder();
        private boolean constraintViolation = true;
        private final Set<JcrNodeDefinition> recorded = new HashSet<>();

        protected MatchResults( Name childName,
                                Name childPrimaryType,
                                boolean skipProtected,
                                SiblingCounter siblingCounter,
                                Name parentPrimaryType,
                                Set<Name> parentMixinTypes,
                                Path parentPath,
                                String workspaceName,
                                String repositoryName,
                                ExecutionContext context ) {
            StringFactory strings = context.getValueFactories().getStringFactory();
            this.childName = strings.create(childName);
            this.childPrimaryType = strings.create(childPrimaryType);
            this.parentPrimaryType = strings.create(parentPrimaryType);
            StringBuilder parentMixinTypesStr = new StringBuilder("{");
            for (Name mixin : parentMixinTypes) {
                if (parentMixinTypesStr.length() > 1) parentMixinTypesStr.append(',');
                parentMixinTypesStr.append(strings.create(mixin));
            }
            parentMixinTypesStr.append('}');
            this.parentMixinTypes = parentMixinTypesStr.toString();
            this.parentPath = strings.create(parentPath);
            this.workspaceName = workspaceName;
            this.repositoryName = repositoryName;
        }

        protected String readable( JcrNodeDefinition defn ) {
            return defn.toString();
        }

        public void nameNotSatisfied( JcrNodeDefinition defn ) {
            if (!recorded.add(defn)) return;
            reasons.append("\n ");
            reasons.append(JcrI18n.childNameDoesNotSatisfyParentChildNodeDefinition.text(childName, readable(defn)));
        }

        public void requiredTypesNotSatisfied( JcrNodeDefinition defn ) {
            if (!recorded.add(defn)) return;
            reasons.append("\n ");
            reasons.append(JcrI18n.childPrimaryTypeDoesNotSatisfyParentChildNodeDefinition.text(childPrimaryType, readable(defn)));
        }

        public void noSameNameSiblingsAllowed( JcrNodeDefinition defn ) {
            if (!recorded.add(defn)) return;
            constraintViolation = false;
            reasons.append("\n ");
            reasons.append(JcrI18n.parentChildNodeDefinitionDoesNotAllowSameNameSiblings.text(childName, readable(defn)));
        }

        public void noProtectedDefinitionsAllowed( JcrNodeDefinition defn ) {
            if (!recorded.add(defn)) return;
            reasons.append("\n ");
            reasons.append(JcrI18n.parentChildNodeDefinitionIsProtected.text(readable(defn)));
        }

        public void throwFailure() throws ConstraintViolationException, ItemExistsException {
            String msg = JcrI18n.unableToAddChildUnderParent.text(childName, childPrimaryType, parentPath, parentPrimaryType,
                                                                  parentMixinTypes, workspaceName, repositoryName, reasons);
            if (constraintViolation) throw new ConstraintViolationException(msg);
            throw new ItemExistsException(msg);
        }
    }

    /**
     * An internal extension to NodeDefinitionSet that adds the #appliesTo method so that the NodeDefinitionSet can be cached and
     * reused.
     */
    private static interface ReusableNodeDefinitionSet extends NodeDefinitionSet {
        boolean appliesTo( NodeTypes nodeTypes,
                           Name primaryType,
                           Set<Name> mixinTypes );
    }

    /**
     * Basic implementation of {@link ReusableNodeDefinitionSet} that provides several utility methods for finding the best child
     * node definition and for determining the reasons why child node definitions did not match.
     */
    private abstract class AbstractNodeDefinitionSet implements ReusableNodeDefinitionSet {
        protected final Name primaryType;
        protected final Set<Name> mixinTypes;

        protected AbstractNodeDefinitionSet( Name primaryType,
                                             Set<Name> mixinTypes ) {
            this.primaryType = primaryType;
            this.mixinTypes = mixinTypes;
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public boolean appliesTo( NodeTypes nodeTypes,
                                  Name primaryType,
                                  Set<Name> mixinTypes ) {
            // If the 'NodeType' instance is different, at least one node type has changed, so we should't reuse this object ...
            if (NodeTypes.this != nodeTypes) return false;
            if (!this.primaryType.equals(primaryType)) return false;
            if (this.mixinTypes == null || this.mixinTypes.isEmpty()) {
                if (mixinTypes == null || mixinTypes.isEmpty()) return true;
                if (nodeTypeNamesWithNoChildNodeDefns.containsAll(mixinTypes)) return true;
            }
            return false;
        }

        protected JcrNodeDefinition findBest( JcrNodeDefinition defn,
                                              Name childName,
                                              Name childPrimaryType,
                                              boolean skipProtected,
                                              SiblingCounter siblingCounter ) {
            return childDefinitionSatisfies(defn, childName, childPrimaryType, skipProtected, siblingCounter, null);
        }

        protected void reasonNotMatched( JcrNodeDefinition defn,
                                         Name childName,
                                         Name childPrimaryType,
                                         boolean skipProtected,
                                         SiblingCounter siblingCounter,
                                         Name parentPrimaryType,
                                         Set<Name> parentMixinTypes,
                                         Path parentPath,
                                         String workspaceName,
                                         String repositoryName,
                                         ExecutionContext context ) throws ConstraintViolationException, ItemExistsException {
            MatchResults builder = new MatchResults(childName, childPrimaryType, skipProtected, siblingCounter,
                                                    parentPrimaryType, parentMixinTypes, parentPath, workspaceName,
                                                    repositoryName, context);
            childDefinitionSatisfies(defn, childName, childPrimaryType, skipProtected, siblingCounter, builder);
            builder.throwFailure();
        }

        protected JcrNodeDefinition findBest( JcrNodeType primaryType,
                                              JcrNodeType[] additionalTypes,
                                              Name childName,
                                              Name childPrimaryType,
                                              boolean skipProtected,
                                              SiblingCounter siblingCounter ) {
            return childDefinitionSatisfies(primaryType, additionalTypes, childName, childPrimaryType, skipProtected,
                                            siblingCounter, null);
        }

        protected void reasonNotMatched( JcrNodeType primaryType,
                                         JcrNodeType[] additionalTypes,
                                         Name childName,
                                         Name childPrimaryType,
                                         boolean skipProtected,
                                         SiblingCounter siblingCounter,
                                         Name parentPrimaryType,
                                         Set<Name> parentMixinTypes,
                                         Path parentPath,
                                         String workspaceName,
                                         String repositoryName,
                                         ExecutionContext context ) throws ConstraintViolationException, ItemExistsException {
            MatchResults builder = new MatchResults(childName, childPrimaryType, skipProtected, siblingCounter,
                                                    parentPrimaryType, parentMixinTypes, parentPath, workspaceName,
                                                    repositoryName, context);
            childDefinitionSatisfies(primaryType, additionalTypes, childName, childPrimaryType, skipProtected, siblingCounter,
                                     builder);
            builder.throwFailure();
        }

        /**
         * Figure out whether a child with the given name and primary type can be added under a parent with only the given child
         * node definition. This logic is much more streamlined than
         * {@link #childDefinitionSatisfies(JcrNodeType, JcrNodeType[], Name, Name, boolean, SiblingCounter, MatchResults)}, and
         * it's worth having because such cases are very prevalent.
         *
         * @param defn the parent's sole child node definition; may not be null
         * @param childName the name of the proposed child; may be null if the child name is to be determined by the definition
         * @param childPrimaryType the primary type of the proposed child; may be null if the default type is to be used
         * @param skipProtected true if this method should not consider any of the parent's child node definitions that are
         *        protected, or false if it should consider all child node definitions
         * @param siblingCounter a function that this method can call to determine how many siblings with the child's name already
         *        exist under the parent
         * @param matches the mismatch results object that, if the child definition could not be used for this child, records why;
         *        may be null if only attempting to find a matching definition (pass 1)
         * @return the matching node definition, or null if the definition does not match
         * @see #childDefinitionSatisfies(JcrNodeType, JcrNodeType[], Name, Name, boolean, SiblingCounter, MatchResults)
         */
        private JcrNodeDefinition childDefinitionSatisfies( JcrNodeDefinition defn,
                                                            Name childName,
                                                            Name childPrimaryType,
                                                            boolean skipProtected,
                                                            SiblingCounter siblingCounter,
                                                            MatchResults matches ) {
            if (defn.isProtected() && skipProtected) {
                if (matches != null) matches.noProtectedDefinitionsAllowed(defn);
                return null;
            }

            // The node's name must satisfy the definition's name requirements ...
            if (childName != null && !defn.isResidual()) {
                // A specific name is required ...
                if (!defn.getInternalName().equals(childName)) {
                    // The child name doesn't match the definition name, so this definition won't work ...
                    if (matches != null) matches.nameNotSatisfied(defn);
                    return null;
                }
            }

            // The node's type must satisfy the definition's type requirements ...
            if (childPrimaryType != null && defn.hasRequiredPrimaryTypes()) {
                JcrNodeType childPrimaryTypeObj = getNodeType(childPrimaryType);
                if (!defn.allowsChildWithType(childPrimaryTypeObj)) {
                    // The child's type doesn't satisfy the definition's requirements, so this definition won't work ...
                    if (matches != null) matches.requiredTypesNotSatisfied(defn);
                    return null;
                }
            }

            if (!defn.allowsSameNameSiblings()) {
                // We have to know how many same-name-siblings there already are in the parent ...
                if (siblingCounter.countSiblingsNamed(childName) > 0) {
                    // The child's type doesn't allow SNS, and there is already at least one child with that name ...
                    if (matches != null) matches.noSameNameSiblingsAllowed(defn);
                    return null;
                }
            }

            // The definition matches all of the requirements ...
            return defn;
        }

        /**
         * Figure out whether a child with the given name and primary type can be added under a parent with a primary type and
         * additional mixin types that have child node definitions. This logic is more complicated than the more efficient
         * {@link #childDefinitionSatisfies(JcrNodeDefinition, Name, Name, boolean, SiblingCounter, MatchResults)}.
         *
         * @param primaryType the parent's primary node type; may not be null
         * @param additionalTypes the parent's mixin node types; may be null, but generally should not be empty
         * @param childName the name of the proposed child; may be null if the child name is to be determined by the definition
         * @param childPrimaryType the primary type of the proposed child; may be null if the default type is to be used
         * @param skipProtected true if this method should not consider any of the parent's child node definitions that are
         *        protected, or false if it should consider all child node definitions
         * @param siblingCounter a function that this method can call to determine how many siblings with the child's name already
         *        exist under the parent
         * @param matches the mismatch results object that, if the child definition could not be used for this child, records why;
         *        may be null if only attempting to find a matching definition (pass 1)
         * @return the matching node definition, or null if the definition does not match
         * @see #childDefinitionSatisfies(JcrNodeDefinition, Name, Name, boolean, SiblingCounter, MatchResults)
         */
        private JcrNodeDefinition childDefinitionSatisfies( JcrNodeType primaryType,
                                                            JcrNodeType[] additionalTypes,
                                                            Name childName,
                                                            Name childPrimaryType,
                                                            boolean skipProtected,
                                                            SiblingCounter siblingCounter,
                                                            MatchResults matches ) {
            assert primaryType != null;
            JcrNodeType childType = childPrimaryType != null ? getNodeType(childPrimaryType) : null;
            JcrNodeDefinition defn = null;
            // First check for child node defns that match by name, type, and allow SNS ...
            defn = childDefinitionSatisfies(primaryType, childType, childName, skipProtected, true, matches);
            if (defn != null) return defn;
            if (additionalTypes != null) {
                for (JcrNodeType additionalType : additionalTypes) {
                    defn = childDefinitionSatisfies(additionalType, childType, childName, skipProtected, true, matches);
                    if (defn != null) return defn;
                }
            }
            // Then check for child node defns that match by name and type but that do not allow SNS ...
            defn = childDefinitionSatisfies(primaryType, childType, childName, skipProtected, false, matches);
            if (defn == null && additionalTypes != null) {
                for (JcrNodeType additionalType : additionalTypes) {
                    defn = childDefinitionSatisfies(additionalType, childType, childName, skipProtected, false, matches);
                    if (defn != null) break;
                }
            }
            if (defn != null) {
                // We found a child node defn that matches exactly by at least name but does not allow SNS.
                // Even though it might be expensive to count the existing children, a non-SNS child node definition that
                // matches the same name as our new child generally implies we should use that child node defn ...
                if (siblingCounter.countSiblingsNamed(childName) == 0) {
                    // There are no existing siblings with this name, so we're good ...
                    return defn;
                }
                // There are existing siblings, but the name matches a non-SNS child node definition.
                if (matches != null) matches.noSameNameSiblingsAllowed(defn);
                // And see what else is available ...
            }

            // Check for residual child node defns that match by type and allow SNS ...
            defn = childDefinitionSatisfies(primaryType, childType, JcrNodeType.RESIDUAL_NAME, skipProtected, true, matches);
            if (defn != null) return defn;
            if (additionalTypes != null) {
                for (JcrNodeType additionalType : additionalTypes) {
                    defn = childDefinitionSatisfies(additionalType, childType, JcrNodeType.RESIDUAL_NAME, skipProtected, true,
                                                    matches);
                    if (defn != null) return defn;
                }
            }

            if (childName != null) {
                // No child definitions were found that allow SNS, so see how many existing same-name-siblings there are ...
                if (siblingCounter.countSiblingsNamed(childName) > 0) {
                    // This child will require a SNS but we didn't find any child node definitions that matched ...
                    if (matches != null) {
                        // Go through those child defns to mark with a reason ...
                        for (JcrNodeDefinition childDefn : primaryType.allChildNodeDefinitions()) {
                            if (!childDefn.allowsSameNameSiblings()) matches.nameNotSatisfied(childDefn);
                            if (!childDefn.isResidual() && !childDefn.getName().equals(childName)) {
                                // Allows SNS, not residual, doesn't match name ...
                                matches.nameNotSatisfied(childDefn);
                            } else {
                                // Allows SNS, is residual (matching any name) or matches name;
                                // if the defn was not already used, it has to be because the type is wrong ...
                                matches.requiredTypesNotSatisfied(childDefn);
                            }
                        }
                    }
                    return null;
                }

                // There are no siblings with same name, so we can look for child defns that match by name, type and no SNS ...
                defn = childDefinitionSatisfies(primaryType, childType, childName, skipProtected, false, matches);
                if (defn != null) return defn;
                if (additionalTypes != null) {
                    for (JcrNodeType additionalType : additionalTypes) {
                        defn = childDefinitionSatisfies(additionalType, childType, childName, skipProtected, false, matches);
                        if (defn != null) return defn;
                    }
                }
            }
            // Check for residual child node defns that match by type and no SNS ...
            defn = childDefinitionSatisfies(primaryType, childType, JcrNodeType.RESIDUAL_NAME, skipProtected, false, matches);
            if (defn != null) return defn;
            if (additionalTypes != null) {
                for (JcrNodeType additionalType : additionalTypes) {
                    defn = childDefinitionSatisfies(additionalType, childType, JcrNodeType.RESIDUAL_NAME, skipProtected, false,
                                                    matches);
                    if (defn != null) return defn;
                }
            }
            // None were matched ...
            return null;
        }

        /**
         * Figure out whether a child with the given name and primary type can be added under a parent with the given node type.
         * Note that this is called from within
         * {@link #childDefinitionSatisfies(JcrNodeType, JcrNodeType[], Name, Name, boolean, SiblingCounter, MatchResults)}.
         *
         * @param type the parent node type; may not be null
         * @param childPrimaryType the primary type for the child; may be null if the primary type is not known and would be
         *        determined by the matching child node definition's default type
         * @param childName the name of the proposed child; may be null if the child name is to be determined by the definition
         * @param skipProtected true if this method should not consider any of the parent's child node definitions that are
         *        protected, or false if it should consider all child node definitions
         * @param allowSns true if this method should consider only those child node definitions that allow SNS, or false
         *        otherwise
         * @param matches the mismatch results object that, if the child definition could not be used for this child, records why;
         *        may be null if only attempting to find a matching definition (pass 1)
         * @return the matching node definition, or null if the definition does not match
         * @see #findBest(JcrNodeType, JcrNodeType[], Name, Name, boolean, SiblingCounter)
         */
        private JcrNodeDefinition childDefinitionSatisfies( JcrNodeType type,
                                                            JcrNodeType childPrimaryType,
                                                            Name childName,
                                                            boolean skipProtected,
                                                            boolean allowSns,
                                                            MatchResults matches ) {
            if (childName != null) {
                // Either a residual or a name ...
                // See if the primary type has any child node defns that match by name and allow/disallow SNS ...
                for (JcrNodeDefinition defn : type.allChildNodeDefinitions(childName, allowSns)) {
                    // Skip protected definitions ...
                    if (skipProtected && defn.isProtected()) {
                        if (matches != null) matches.noProtectedDefinitionsAllowed(defn);
                        continue;
                    }
                    // See if the definition allows a child with the supplied primary type ...
                    if (defn.allowsChildWithType(childPrimaryType)) return defn;

                    // Otherwise, the child's primary type does not satisfy the required types ...
                    if (matches != null) matches.requiredTypesNotSatisfied(defn);
                }
            } else {
                // There is no name, so look thru all non-residual child node defns that have a matching type ...
                for (JcrNodeDefinition defn : type.allChildNodeDefinitions()) {
                    if (defn.isResidual()) continue;
                    if (!allowSns && !defn.allowsSameNameSiblings()) continue;
                    // Skip protected definitions ...
                    if (skipProtected && defn.isProtected()) {
                        if (matches != null) matches.noProtectedDefinitionsAllowed(defn);
                        continue;
                    }
                    if (defn.allowsChildWithType(childPrimaryType)) {
                        // We found one that matched the type
                        return defn;
                    }
                    if (matches != null) matches.requiredTypesNotSatisfied(defn);
                }
            }
            return null;
        }

    }

    private class SingleNodeDefinitionSet extends AbstractNodeDefinitionSet {
        private final JcrNodeDefinition defn;

        protected SingleNodeDefinitionSet( Name primaryType,
                                           Set<Name> mixinTypes,
                                           JcrNodeDefinition defn ) {
            super(primaryType, mixinTypes);
            this.defn = defn;
        }

        @Override
        public JcrNodeDefinition findBestDefinitionForChild( Name childName,
                                                             Name childPrimaryType,
                                                             boolean skipProtected,
                                                             SiblingCounter siblingCounter ) {
            return findBest(defn, childName, childPrimaryType, skipProtected, siblingCounter);
        }

        @Override
        public void determineReasonForMismatch( Name childName,
                                                Name childPrimaryType,
                                                boolean skipProtected,
                                                SiblingCounter siblingCounter,
                                                Name parentPrimaryType,
                                                Set<Name> parentMixinTypes,
                                                Path parentPath,
                                                String workspaceName,
                                                String repositoryName,
                                                ExecutionContext context )
            throws ConstraintViolationException, ItemExistsException {
            reasonNotMatched(defn, childName, childPrimaryType, skipProtected, siblingCounter, parentPrimaryType,
                             parentMixinTypes, parentPath, workspaceName, repositoryName, context);
        }
    }

    private class MultipleNodeDefinitionSet extends AbstractNodeDefinitionSet {
        private final JcrNodeType nodeType;
        private final JcrNodeType[] additionalTypes;

        protected MultipleNodeDefinitionSet( Name primaryType,
                                             Set<Name> mixinTypes ) {
            super(primaryType, mixinTypes);
            this.nodeType = getNodeType(primaryType);
            if (mixinTypes == null || mixinTypes.isEmpty()) {
                this.additionalTypes = null;
            } else {
                this.additionalTypes = new JcrNodeType[mixinTypes.size()];
                int index = -1;
                for (Name mixinType : mixinTypes) {
                    this.additionalTypes[++index] = getNodeType(mixinType);
                }
            }
        }

        @Override
        public JcrNodeDefinition findBestDefinitionForChild( Name childName,
                                                             Name childPrimaryType,
                                                             boolean skipProtected,
                                                             SiblingCounter siblingCounter ) {
            return findBest(nodeType, additionalTypes, childName, childPrimaryType, skipProtected, siblingCounter);
        }

        @Override
        public void determineReasonForMismatch( Name childName,
                                                Name childPrimaryType,
                                                boolean skipProtected,
                                                SiblingCounter siblingCounter,
                                                Name parentPrimaryType,
                                                Set<Name> parentMixinTypes,
                                                Path parentPath,
                                                String workspaceName,
                                                String repositoryName,
                                                ExecutionContext context )
            throws ConstraintViolationException, ItemExistsException {
            reasonNotMatched(nodeType, additionalTypes, childName, childPrimaryType, skipProtected, siblingCounter,
                             parentPrimaryType, parentMixinTypes, parentPath, workspaceName, repositoryName, context);
        }
    }

    /**
     * A {@link NodeDefinitionSet} implementation that never applies any primary type and mixin types.
     */
    private final class NoChildrenNodeDefinitionSet extends AbstractNodeDefinitionSet {
        protected NoChildrenNodeDefinitionSet( Name primaryType,
                                               Set<Name> mixinTypes ) {
            super(primaryType, mixinTypes);
        }

        @Override
        public JcrNodeDefinition findBestDefinitionForChild( Name childName,
                                                             Name childPrimaryType,
                                                             boolean skipProtected,
                                                             SiblingCounter siblingCounter ) {
            return null;
        }

        @Override
        public void determineReasonForMismatch( Name childName,
                                                Name childPrimaryType,
                                                boolean skipProtected,
                                                SiblingCounter siblingCounter,
                                                Name parentPrimaryType,
                                                Set<Name> parentMixinTypes,
                                                Path parentPath,
                                                String workspaceName,
                                                String repositoryName,
                                                ExecutionContext context ) throws ConstraintViolationException {
            StringFactory strings = context.getValueFactories().getStringFactory();
            String parentType = strings.create(parentPrimaryType);
            StringBuilder parentMixinTypesStr = new StringBuilder('{');
            for (Name mixin : parentMixinTypes) {
                if (parentMixinTypesStr.length() > 1) parentMixinTypesStr.append(',');
                parentMixinTypesStr.append(strings.create(mixin));
            }
            parentMixinTypesStr.append('}');
            throw new ConstraintViolationException(JcrI18n.noChildNodeDefinitions.text(parentPath, parentType,
                                                                                       parentMixinTypesStr));
        }
    }

    /**
     * A {@link NodeDefinitionSet} implementation that never applies any primary type and mixin types.
     */
    private static final class EmptyNodeDefinitionSet implements ReusableNodeDefinitionSet {
        protected EmptyNodeDefinitionSet() {
        }

        @Override
        public boolean appliesTo( NodeTypes nodeTypes,
                                  Name primaryType,
                                  Set<Name> mixinTypes ) {
            return false;
        }

        @Override
        public JcrNodeDefinition findBestDefinitionForChild( Name childName,
                                                             Name childPrimaryType,
                                                             boolean skipProtected,
                                                             SiblingCounter siblingCounter ) {
            return null;
        }

        @Override
        public void determineReasonForMismatch( Name childName,
                                                Name childPrimaryType,
                                                boolean skipProtected,
                                                SiblingCounter siblingCounter,
                                                Name parentPrimaryType,
                                                Set<Name> parentMixinTypes,
                                                Path parentPath,
                                                String workspaceName,
                                                String repositoryName,
                                                ExecutionContext context ) {
            assert false : "This method should never be called";
        }
    }

}
