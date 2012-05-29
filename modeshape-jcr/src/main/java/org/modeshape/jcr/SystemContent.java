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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.RepositoryLockManager.ModeShapeLock;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.PropertyTypeUtil;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry.Namespace;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.basic.BasicName;
import org.modeshape.jcr.value.basic.BasicNamespace;
import org.modeshape.jcr.value.basic.NodeKeyReference;

/**
 * 
 */
public class SystemContent {

    public static final String GENERATED_PREFIX = "ns";
    private static final Name GENERATED_NAMESPACE_NODE_NAME = new BasicName("", GENERATED_PREFIX);
    protected static final Pattern GENERATED_PREFIX_PATTERN = Pattern.compile("ns(\\d{3})");

    private final SessionCache system;
    private NodeKey systemKey;
    private NodeKey nodeTypesKey;
    private NodeKey namespacesKey;
    private NodeKey locksKey;
    private NodeKey versionStorageKey;
    private final PropertyFactory propertyFactory;
    private final ValueFactory<Boolean> booleans;
    private final ValueFactory<String> strings;
    private final NameFactory names;
    private final ReferenceFactory referenceFactory;
    private final javax.jcr.ValueFactory jcrValues;

    SystemContent( SessionCache systemCache ) {
        this.system = systemCache;
        ExecutionContext context = systemCache.getContext();
        this.propertyFactory = context.getPropertyFactory();
        ValueFactories factories = context.getValueFactories();
        this.booleans = factories.getBooleanFactory();
        this.strings = factories.getStringFactory();
        this.names = factories.getNameFactory();
        this.referenceFactory = factories.getReferenceFactory();
        this.jcrValues = new JcrValueFactory(context);
    }

    public void save() {
        system.save();
    }

    private final ExecutionContext context() {
        return system.getContext();
    }

    public final SessionCache cache() {
        return system;
    }

    public NodeKey systemKey() {
        if (systemKey == null) {
            // This is idempotent, so no need to lock
            CachedNode rootNode = system.getNode(system.getRootKey());
            ChildReference systemRef = rootNode.getChildReferences(system).getChild(JcrLexicon.SYSTEM);
            systemKey = systemRef.getKey();
        }
        return systemKey;
    }

    public NodeKey nodeTypesKey() {
        if (nodeTypesKey == null) {
            // This is idempotent, so no need to lock
            CachedNode systemNode = systemNode();
            ChildReference nodeTypesRef = systemNode.getChildReferences(system).getChild(JcrLexicon.NODE_TYPES);
            nodeTypesKey = nodeTypesRef.getKey();
        }
        return nodeTypesKey;
    }

    public NodeKey namespacesKey() {
        if (namespacesKey == null) {
            // This is idempotent, so no need to lock
            CachedNode systemNode = systemNode();
            ChildReference namespacesRef = systemNode.getChildReferences(system).getChild(ModeShapeLexicon.NAMESPACES);
            namespacesKey = namespacesRef.getKey();
        }
        return namespacesKey;
    }

    public NodeKey locksKey() {
        if (locksKey == null) {
            // This is idempotent, so no need to lock
            CachedNode systemNode = systemNode();
            ChildReference locksRef = systemNode.getChildReferences(system).getChild(ModeShapeLexicon.LOCKS);
            locksKey = locksRef.getKey();
        }
        return locksKey;
    }

    public NodeKey versionStorageKey() {
        if (versionStorageKey == null) {
            // This is idempotent, so no need to lock
            CachedNode systemNode = systemNode();
            ChildReference locksRef = systemNode.getChildReferences(system).getChild(JcrLexicon.VERSION_STORAGE);
            versionStorageKey = locksRef.getKey();
        }
        return versionStorageKey;
    }

    public CachedNode systemNode() {
        return system.getNode(systemKey());
    }

    public CachedNode nodeTypesNode() {
        return system.getNode(nodeTypesKey());
    }

    public CachedNode namespacesNode() {
        return system.getNode(namespacesKey());
    }

    public CachedNode locksNode() {
        return system.getNode(locksKey());
    }

    public CachedNode versionStorageNode() {
        return system.getNode(versionStorageKey());
    }

    public MutableCachedNode mutableNodeTypesNode() {
        return system.mutable(nodeTypesKey());
    }

    public MutableCachedNode mutableNamespacesNode() {
        return system.mutable(namespacesKey());
    }

    public MutableCachedNode mutableLocksNode() {
        return system.mutable(locksKey());
    }

    public MutableCachedNode mutableVersionStorageNode() {
        return system.mutable(versionStorageKey());
    }

    /**
     * Stores the node types in the system area under <code>/jcr:system/jcr:nodeTypes</code>.
     * <p>
     * For each node type, a node is created with primary type of <code>nt:nodeType</code> and a name that corresponds to the node
     * type's name. All other properties and child nodes for the newly created node are added in a manner consistent with the
     * guidance provided in section 6.7.22 of the JCR 1.0 specification and section 4.7.24 of the JCR 2.0 specification where
     * possible.
     * </p>
     * 
     * @param nodeTypes the node types to write out; may not be null
     * @param updateExisting a boolean flag denoting whether the new node type definition should be overwrite an existing node
     *        type definition
     */
    public void store( Iterable<JcrNodeType> nodeTypes,
                       boolean updateExisting ) {
        MutableCachedNode nodeTypesNode = mutableNodeTypesNode();
        Set<Name> names = new HashSet<Name>();
        Set<NodeKey> keys = new HashSet<NodeKey>();
        for (JcrNodeType nodeType : nodeTypes) {
            if (!names.add(nodeType.getInternalName())) {
                Logger.getLogger(getClass()).debug("Found duplicate node type: " + nodeType);
            }
            if (!keys.add(nodeType.key())) {
                Logger.getLogger(getClass()).debug("Found duplicate key: " + nodeType);
            }
        }
        for (JcrNodeType nodeType : nodeTypes) {
            store(nodeType, nodeTypesNode, updateExisting);
        }
    }

    /**
     * Stores the node type in the system area under <code>/jcr:system/jcr:nodeTypes</code>.
     * <p>
     * The stored content will contain a node with a primary type of <code>nt:nodeType</code> and a name that corresponds to the
     * node type's name. All other properties and child nodes for the newly created node are added in a manner consistent with the
     * guidance provided in section 6.7.22 of the JCR 1.0 specification and section 4.7.24 of the JCR 2.0 specification where
     * possible.
     * </p>
     * 
     * @param nodeType the node type to write; may not be null
     * @param updateExisting a boolean flag denoting whether the new node type definition should be overwrite an existing node
     *        type definition
     */
    public void store( JcrNodeType nodeType,
                       boolean updateExisting ) {
        MutableCachedNode nodeTypesNode = mutableNodeTypesNode();
        store(nodeType, nodeTypesNode, updateExisting);
    }

    /**
     * Projects the node types onto the provided graph under the location of <code>parentOfTypeNodes</code>. The operations needed
     * to create the node (and any child nodes or properties) will be added to the batch specified in <code>batch</code>.
     * 
     * @param nodeType the node type to be projected
     * @param nodeTypes the parent node under which each node type should be saved; may not be null
     * @param updateExisting a boolean flag denoting whether the new node type definition should be overwrite an existing node
     *        type definition
     */
    private void store( JcrNodeType nodeType,
                        MutableCachedNode nodeTypes,
                        boolean updateExisting ) {
        assert nodeType != null;
        assert system != null;
        assert nodeTypes != null;

        Name name = nodeType.getInternalName();
        final NodeKey key = nodeType.key();
        MutableCachedNode nodeTypeNode = null;
        Set<NodeKey> existingChildKeys = null;
        if (nodeTypes.getChildReferences(system).hasChild(key)) {
            // The node already exists ...
            if (!updateExisting) return;
            nodeTypeNode = system.mutable(key);

            // We'll need to delete any existing child that isn't there anymore ...
            existingChildKeys = new HashSet<NodeKey>();
            for (ChildReference childRef : nodeTypeNode.getChildReferences(system)) {
                existingChildKeys.add(childRef.getKey());
            }
        }

        // Define the properties for this node type ...
        NodeType[] supertypes = nodeType.getDeclaredSupertypes();
        List<Name> supertypeNames = new ArrayList<Name>(supertypes.length);
        for (int i = 0; i < supertypes.length; i++) {
            supertypeNames.add(((JcrNodeType)supertypes[i]).getInternalName());
        }

        List<Property> properties = new ArrayList<Property>();
        properties.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.NODE_TYPE));
        properties.add(propertyFactory.create(JcrLexicon.IS_MIXIN, nodeType.isMixin()));
        properties.add(propertyFactory.create(JcrLexicon.IS_ABSTRACT, nodeType.isAbstract()));
        properties.add(propertyFactory.create(JcrLexicon.IS_QUERYABLE, nodeType.isQueryable()));

        if (nodeType.getPrimaryItemName() != null) {
            properties.add(propertyFactory.create(JcrLexicon.PRIMARY_ITEM_NAME, nodeType.getPrimaryItemName()));
        }

        properties.add(propertyFactory.create(JcrLexicon.NODE_TYPE_NAME, nodeType.getName()));
        properties.add(propertyFactory.create(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, nodeType.hasOrderableChildNodes()));
        properties.add(propertyFactory.create(JcrLexicon.SUPERTYPES, supertypeNames));

        // Now make or adjust the node for the node type ...
        if (nodeTypeNode != null) {
            // Update the properties ...
            nodeTypeNode.setProperties(system, properties);
        } else {
            // We have to create the node type node ...
            nodeTypeNode = nodeTypes.createChild(system, key, name, properties);
        }

        // And the property definitions ...
        for (JcrPropertyDefinition defn : nodeType.getDeclaredPropertyDefinitions()) {
            store(nodeTypeNode, defn);
            if (existingChildKeys != null) existingChildKeys.remove(defn.key());
        }

        // And the child node definitions ...
        for (JcrNodeDefinition defn : nodeType.getDeclaredChildNodeDefinitions()) {
            store(nodeTypeNode, defn);
            if (existingChildKeys != null) existingChildKeys.remove(defn.key());
        }

        // Remove any children that weren't represented by a property definition or child node definition ...
        if (existingChildKeys != null && !existingChildKeys.isEmpty()) {
            for (NodeKey childKey : existingChildKeys) {
                nodeTypeNode.removeChild(system, childKey);
            }
        }
    }

    /**
     * Projects a single property definition onto the provided graph under the location of <code>nodeTypePath</code>. The
     * operations needed to create the property definition and any of its properties will be added to the batch specified in
     * <code>batch</code>.
     * <p>
     * All node creation is performed through the graph layer. If the primary type of the node at <code>nodeTypePath</code> does
     * not contain a residual definition that allows child nodes of type <code>nt:propertyDefinition</code>, this method creates
     * nodes for which the JCR layer cannot determine the corresponding node definition. This WILL corrupt the graph from a JCR
     * standpoint and make it unusable through the ModeShape JCR layer.
     * </p>
     * 
     * @param nodeTypeNode the parent node under which each property definition should be saved; may not be null
     * @param propertyDef the property definition to be projected
     */
    private void store( MutableCachedNode nodeTypeNode,
                        JcrPropertyDefinition propertyDef ) {
        // Find an existing node for this property definition ...
        final NodeKey key = propertyDef.key();
        final Name name = propertyDef.getInternalName();
        MutableCachedNode propDefnNode = null;
        if (!nodeTypeNode.isNew()) {
            if (nodeTypeNode.getChildReferences(system).hasChild(key)) {
                // The node already exists ...
                propDefnNode = system.mutable(key);
            }
        }

        List<Property> properties = new ArrayList<Property>();
        properties.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.PROPERTY_DEFINITION));

        if (!JcrNodeType.RESIDUAL_ITEM_NAME.equals(propertyDef.getName())) {
            properties.add(propertyFactory.create(JcrLexicon.NAME, name));
        }
        properties.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, propertyDef.isAutoCreated()));
        properties.add(propertyFactory.create(JcrLexicon.MANDATORY, propertyDef.isMandatory()));
        properties.add(propertyFactory.create(JcrLexicon.MULTIPLE, propertyDef.isMultiple()));
        properties.add(propertyFactory.create(JcrLexicon.PROTECTED, propertyDef.isProtected()));
        properties.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION,
                                              OnParentVersionAction.nameFromValue(propertyDef.getOnParentVersion())));
        properties.add(propertyFactory.create(JcrLexicon.REQUIRED_TYPE, PropertyType.nameFromValue(propertyDef.getRequiredType())
                                                                                    .toUpperCase()));

        List<String> symbols = new ArrayList<String>();
        for (String value : propertyDef.getAvailableQueryOperators()) {
            if (value != null) symbols.add(value);
        }
        properties.add(propertyFactory.create(JcrLexicon.AVAILABLE_QUERY_OPERATORS, symbols));

        Value[] defaultValues = propertyDef.getDefaultValues();
        if (defaultValues != null && defaultValues.length > 0) {
            String[] defaultsAsString = new String[defaultValues.length];

            for (int i = 0; i < defaultValues.length; i++) {
                try {
                    defaultsAsString[i] = defaultValues[i].getString();
                } catch (RepositoryException re) {
                    // Really shouldn't get here as all values are convertible to string
                    throw new IllegalStateException(re);
                }
            }
            properties.add(propertyFactory.create(JcrLexicon.DEFAULT_VALUES, defaultsAsString));
        }

        String[] valueConstraints = propertyDef.getValueConstraints();
        if (valueConstraints.length > 0) {
            properties.add(propertyFactory.create(JcrLexicon.VALUE_CONSTRAINTS, valueConstraints));
        }

        // Now either update the existing node or create a new node ..
        if (propDefnNode != null) {
            // Update the properties ...
            propDefnNode.setProperties(system, properties);
        } else {
            // We have to create the node type node ...
            propDefnNode = nodeTypeNode.createChild(system, key, name, properties);
        }
    }

    /**
     * Projects a single child node definition onto the provided graph under the location of <code>nodeTypePath</code>. The
     * operations needed to create the child node definition and any of its properties will be added to the batch specified in
     * <code>batch</code>.
     * <p>
     * All node creation is performed through the graph layer. If the primary type of the node at <code>nodeTypePath</code> does
     * not contain a residual definition that allows child nodes of type <code>nt:childNodeDefinition</code>, this method creates
     * nodes for which the JCR layer cannot determine the corresponding node definition. This WILL corrupt the graph from a JCR
     * standpoint and make it unusable through the ModeShape JCR layer.
     * </p>
     * 
     * @param nodeTypeNode the parent node under which each property definition should be saved; may not be null
     * @param childNodeDef the child node definition to be projected
     */
    private void store( MutableCachedNode nodeTypeNode,
                        JcrNodeDefinition childNodeDef ) {
        // Find an existing node for this property definition ...
        final NodeKey key = childNodeDef.key();
        final Name name = childNodeDef.getInternalName();
        MutableCachedNode nodeDefnNode = null;
        if (!nodeTypeNode.isNew()) {
            if (nodeTypeNode.getChildReferences(system).hasChild(key)) {
                // The node already exists ...
                nodeDefnNode = system.mutable(key);
            }
        }

        List<Property> props = new ArrayList<Property>();
        props.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.CHILD_NODE_DEFINITION));

        if (!JcrNodeType.RESIDUAL_ITEM_NAME.equals(childNodeDef.getName())) {
            props.add(propertyFactory.create(JcrLexicon.NAME, name));
        }

        if (childNodeDef.getDefaultPrimaryType() != null) {
            props.add(propertyFactory.create(JcrLexicon.DEFAULT_PRIMARY_TYPE, childNodeDef.getDefaultPrimaryType().getName()));
        }

        props.add(propertyFactory.create(JcrLexicon.REQUIRED_PRIMARY_TYPES, childNodeDef.requiredPrimaryTypeNames()));
        props.add(propertyFactory.create(JcrLexicon.SAME_NAME_SIBLINGS, childNodeDef.allowsSameNameSiblings()));
        props.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION,
                                         OnParentVersionAction.nameFromValue(childNodeDef.getOnParentVersion())));
        props.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, childNodeDef.isAutoCreated()));
        props.add(propertyFactory.create(JcrLexicon.MANDATORY, childNodeDef.isMandatory()));
        props.add(propertyFactory.create(JcrLexicon.PROTECTED, childNodeDef.isProtected()));

        // Now either update the existing node or create a new node ..
        if (nodeDefnNode != null) {
            // Update the properties ...
            nodeDefnNode.setProperties(system, props);
        } else {
            // We have to create the node type node ...
            nodeDefnNode = nodeTypeNode.createChild(system, key, name, props);
        }
    }

    /**
     * Read from system storage the node type definitions with the supplied names.
     * 
     * @param nodeTypesToRefresh
     * @return the node types as read from the system storage
     */
    public List<NodeTypeDefinition> readNodeTypes( Set<Name> nodeTypesToRefresh ) {
        return new ArrayList<NodeTypeDefinition>();
    }

    /**
     * Read from system storage all of the node type definitions.
     * 
     * @return the node types as read from the system storage
     */
    public List<NodeTypeDefinition> readAllNodeTypes() {
        CachedNode nodeTypes = nodeTypesNode();
        List<NodeTypeDefinition> defns = new ArrayList<NodeTypeDefinition>();
        for (ChildReference ref : nodeTypes.getChildReferences(system)) {
            CachedNode nodeType = system.getNode(ref);
            defns.add(readNodeTypeDefinition(nodeType));
        }
        return defns;
    }

    @SuppressWarnings( "unchecked" )
    public NodeTypeDefinition readNodeTypeDefinition( CachedNode nodeType ) {
        try {
            NodeTypeTemplate defn = new JcrNodeTypeTemplate(context());
            defn.setMixin(booleans.create(first(nodeType, JcrLexicon.IS_MIXIN)));
            defn.setAbstract(booleans.create(first(nodeType, JcrLexicon.IS_ABSTRACT)));
            defn.setQueryable(booleans.create(first(nodeType, JcrLexicon.IS_QUERYABLE)));
            defn.setOrderableChildNodes(booleans.create(first(nodeType, JcrLexicon.HAS_ORDERABLE_CHILD_NODES)));
            defn.setName(strings.create(first(nodeType, JcrLexicon.NODE_TYPE_NAME)));
            Property supertypes = nodeType.getProperty(JcrLexicon.SUPERTYPES, system);
            if (supertypes != null && !supertypes.isEmpty()) {
                String[] supertypeNames = new String[supertypes.size()];
                int i = 0;
                for (Object name : supertypes) {
                    supertypeNames[i++] = strings.create(name);
                }
                defn.setDeclaredSuperTypeNames(supertypeNames);
            }

            // Read the children ...
            for (ChildReference ref : nodeType.getChildReferences(system)) {
                CachedNode itemDefn = system.getNode(ref);
                Name primaryType = names.create(first(itemDefn, JcrLexicon.PRIMARY_TYPE));
                if (JcrNtLexicon.PROPERTY_DEFINITION.equals(primaryType)) {
                    PropertyDefinition propDefn = readPropertyDefinition(itemDefn);
                    assert propDefn != null;
                    defn.getPropertyDefinitionTemplates().add(propDefn);
                } else if (JcrNtLexicon.CHILD_NODE_DEFINITION.equals(primaryType)) {
                    NodeDefinition childDefn = readChildNodeDefinition(itemDefn);
                    assert childDefn != null;
                    defn.getNodeDefinitionTemplates().add(childDefn);
                }
            }

            return defn;
        } catch (ConstraintViolationException e) {
            // this is never expected
            throw new SystemFailureException(e);
        }
    }

    protected PropertyDefinition readPropertyDefinition( CachedNode propDefn ) throws ConstraintViolationException {
        PropertyDefinitionTemplate defn = new JcrPropertyDefinitionTemplate(context());
        defn.setName(strings.create(first(propDefn, JcrLexicon.NAME, JcrNodeType.RESIDUAL_ITEM_NAME)));
        defn.setAutoCreated(booleans.create(first(propDefn, JcrLexicon.AUTO_CREATED)));
        defn.setMandatory(booleans.create(first(propDefn, JcrLexicon.MANDATORY)));
        defn.setMultiple(booleans.create(first(propDefn, JcrLexicon.MULTIPLE)));
        defn.setProtected(booleans.create(first(propDefn, JcrLexicon.PROTECTED)));
        defn.setOnParentVersion(OnParentVersionAction.valueFromName(strings.create(first(propDefn, JcrLexicon.ON_PARENT_VERSION))));
        defn.setRequiredType(propertyType(first(propDefn, JcrLexicon.REQUIRED_TYPE)));

        Property queryOps = propDefn.getProperty(JcrLexicon.AVAILABLE_QUERY_OPERATORS, system);
        if (queryOps != null && !queryOps.isEmpty()) {
            String[] queryOperators = new String[queryOps.size()];
            int i = 0;
            for (Object op : queryOps) {
                queryOperators[i++] = strings.create(op);
            }
            defn.setAvailableQueryOperators(queryOperators);
        }

        Property defaultValues = propDefn.getProperty(JcrLexicon.DEFAULT_VALUES, system);
        if (defaultValues != null && !defaultValues.isEmpty()) {
            Value[] values = new Value[defaultValues.size()];
            int i = 0;
            for (Object value : defaultValues) {
                org.modeshape.jcr.value.PropertyType modeType = org.modeshape.jcr.value.PropertyType.discoverType(value);
                int jcrType = PropertyTypeUtil.jcrPropertyTypeFor(modeType);
                String strValue = strings.create(value);
                try {
                    values[i++] = jcrValues.createValue(strValue, jcrType);
                } catch (ValueFormatException err) {
                    values[i++] = jcrValues.createValue(strValue);
                }
                i++;
            }
            defn.setDefaultValues(values);
        }

        Property constraints = propDefn.getProperty(JcrLexicon.VALUE_CONSTRAINTS, system);
        if (constraints != null && !constraints.isEmpty()) {
            String[] values = new String[constraints.size()];
            int i = 0;
            for (Object value : constraints) {
                values[i++] = strings.create(value);
            }
            defn.setValueConstraints(values);
        }

        return defn;
    }

    protected NodeDefinition readChildNodeDefinition( CachedNode childDefn ) throws ConstraintViolationException {
        NodeDefinitionTemplate defn = new JcrNodeDefinitionTemplate(context());
        defn.setName(strings.create(first(childDefn, JcrLexicon.NAME, JcrNodeType.RESIDUAL_ITEM_NAME)));
        defn.setAutoCreated(booleans.create(first(childDefn, JcrLexicon.AUTO_CREATED)));
        defn.setMandatory(booleans.create(first(childDefn, JcrLexicon.MANDATORY)));
        defn.setSameNameSiblings(booleans.create(first(childDefn, JcrLexicon.SAME_NAME_SIBLINGS)));
        defn.setProtected(booleans.create(first(childDefn, JcrLexicon.PROTECTED)));
        defn.setOnParentVersion(OnParentVersionAction.valueFromName(strings.create(first(childDefn, JcrLexicon.ON_PARENT_VERSION))));

        String defaultPrimaryType = strings.create(first(childDefn, JcrLexicon.DEFAULT_PRIMARY_TYPE));
        if (defaultPrimaryType != null) defn.setDefaultPrimaryTypeName(defaultPrimaryType);

        Property requiredPrimaryTypes = childDefn.getProperty(JcrLexicon.REQUIRED_PRIMARY_TYPES, system);
        if (requiredPrimaryTypes != null && !requiredPrimaryTypes.isEmpty()) {
            String[] values = new String[requiredPrimaryTypes.size()];
            int i = 0;
            for (Object op : requiredPrimaryTypes) {
                values[i++] = strings.create(op);
            }
            defn.setRequiredPrimaryTypeNames(values);
        }

        return defn;
    }

    protected final int propertyType( Object value ) {
        org.modeshape.jcr.value.PropertyType type = org.modeshape.jcr.value.PropertyType.valueFor(strings.create(value)
                                                                                                         .toLowerCase());
        return PropertyTypeUtil.jcrPropertyTypeFor(type);
    }

    protected final Iterable<?> all( CachedNode node,
                                     Name propertyName ) {
        return node.getProperty(propertyName, system);
    }

    protected final Object first( CachedNode node,
                                  Name propertyName ) {
        return first(node, propertyName, null);
    }

    protected final Object first( CachedNode node,
                                  Name propertyName,
                                  Object defaultValue ) {
        Property property = node.getProperty(propertyName, system);
        return property != null ? property.getFirstValue() : defaultValue;
    }

    public Collection<Namespace> readAllNamespaces() {
        CachedNode namespaces = namespacesNode();
        List<Namespace> results = new ArrayList<Namespace>();
        // Add in the empty namespace ...
        results.add(new BasicNamespace("", ""));
        // Now read in the persisted namespaces ...
        for (ChildReference ref : namespaces.getChildReferences(system)) {
            CachedNode namespace = system.getNode(ref);
            String prefix = prefixFor(ref.getSegment());
            String uri = strings.create(first(namespace, ModeShapeLexicon.URI));
            results.add(new BasicNamespace(prefix, uri));
        }
        return results;
    }

    private String prefixFor( Segment segment ) {
        Name name = segment.getName();
        if (ModeShapeLexicon.NAMESPACE.equals(name)) {
            // This is the empty prefix ...
            return "";
        }
        String localName = name.getLocalName();
        int index = segment.getIndex();
        return prefixFor(localName, index);
    }

    private String prefixFor( String name,
                              int counter ) {
        if (counter == 1 && !GENERATED_PREFIX.equals(name)) return name;
        if (counter < 10) {
            return name + "00" + counter;
        }
        if (counter < 100) {
            return name + "0" + counter;
        }
        assert counter < 1000;
        return name + counter;
    }

    private Name nameForPrefix( String prefix ) {
        if (prefix.length() == 0) return ModeShapeLexicon.NAMESPACE;
        Matcher matcher = GENERATED_PREFIX_PATTERN.matcher(prefix);
        if (matcher.matches()) {
            prefix = GENERATED_PREFIX;
        }
        return names.create(prefix);
    }

    public Set<String> registerNamespaces( Map<String, String> newUrisByPrefix ) {
        Set<String> removedPrefixes = new HashSet<String>();
        MutableCachedNode namespaces = mutableNamespacesNode();
        ChildReferences childRefs = namespaces.getChildReferences(system);

        // Find any existing namespace nodes for the new namespace URIs ...
        for (Map.Entry<String, String> newNamespaceEntry : newUrisByPrefix.entrySet()) {
            String newPrefix = newNamespaceEntry.getKey().trim();
            String newUri = newNamespaceEntry.getValue().trim();

            // Verify that the prefix is not already used ...
            Name newPrefixName = nameForPrefix(newPrefix);
            ChildReference ref = childRefs.getChild(newPrefixName);
            if (ref != null) {
                // There's an existing node with the same prefix/name ...
                CachedNode existingNode = system.getNode(ref);
                String existingUri = strings.create(existingNode.getProperty(ModeShapeLexicon.URI, system).getFirstValue());
                if (newUri.equals(existingUri)) {
                    // The URI also matches, so nothing to do ...
                    continue;
                }
                // Otherwise, the prefix was bound to another URI, so this means we're taking an existing prefix already bound
                // to one URI and assigning it to another URI. Per the JavaDoc for javax.jcr.Namespace#register(String,String)
                // the old URI is to be unregistered -- meaning we should delete it ...
                namespaces.removeChild(system, ref.getKey());
                system.destroy(ref.getKey());
                continue;
            }

            // Look for an existing namespace node that uses the same URI ...
            NodeKey key = keyForNamespaceUri(newUri);
            CachedNode existingNode = system.getNode(key);
            if (existingNode != null) {
                // Get the prefix for the existing namespace node ...
                Segment segment = existingNode.getSegment(system);
                String existingPrefix = prefixFor(segment);
                if (GENERATED_NAMESPACE_NODE_NAME.equals(segment.getName()) || !existingPrefix.equals(newPrefix)) {
                    // The prefix but was not used elsewhere, so we know we can just change it ...
                    namespaces.renameChild(system, key, names.create(newPrefix));
                    removedPrefixes.add(existingPrefix);
                }
            }
            else if (newUri.length() > 0) {
                //register the new prefix
                Name name = names.create(newPrefix);
                List<Property> props = new ArrayList<Property>(3);
                props.add(propertyFactory.create(ModeShapeLexicon.URI, newUri));
                props.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.NAMESPACE));
                props.add(propertyFactory.create(ModeShapeLexicon.GENERATED, booleans.create(false)));
                namespaces.createChild(system, key, name, props);
            }
        }

        return removedPrefixes;
    }

    public String readNamespacePrefix( String namespaceUri,
                                       boolean generateIfMissing ) {
        NodeKey key = keyForNamespaceUri(namespaceUri);
        CachedNode nsNode = system.getNode(key);
        if (nsNode != null) {
            // There's an existing node, so just read the prefix (e.g., the name) ...
            Segment segment = nsNode.getSegment(system);
            return prefixFor(segment);
        }
        if (!generateIfMissing) return null;

        // Create a new namespace node that uses this URI ...
        MutableCachedNode mutableNamespaces = mutableNamespacesNode();
        List<Property> props = new ArrayList<Property>(3);
        props.add(propertyFactory.create(ModeShapeLexicon.URI, namespaceUri));
        props.add(propertyFactory.create(ModeShapeLexicon.GENERATED, booleans.create(true)));
        props.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.NAMESPACE));
        MutableCachedNode newNsNode = mutableNamespaces.createChild(system, key, GENERATED_NAMESPACE_NODE_NAME, props);
        return prefixFor(newNsNode.getSegment(system));
    }

    public boolean unregisterNamespace( String namespaceUri ) {
        MutableCachedNode namespaces = mutableNamespacesNode();
        NodeKey key = keyForNamespaceUri(namespaceUri);
        CachedNode nsNode = system.getNode(key);
        if (nsNode != null) {
            namespaces.removeChild(system, key);
            system.destroy(key);
            return true;
        }
        return false;
    }

    protected final NodeKey keyForNamespaceUri( String namespaceUri ) {
        return namespacesKey().withId("mode:namespaces-" + namespaceUri);
    }

    /**
     * Clean up the locks within the repository's system content. Any locks held by active sessions are extended/renewed, while
     * those locks that are significantly expired are removed.
     * 
     * @param activeSessionIds the IDs of the sessions that are still active in this repository
     */
    public void cleanUpLocks( Set<String> activeSessionIds ) {
        // Create a new expiration date ...
        DateTimeFactory dates = context().getValueFactories().getDateFactory();
        DateTime now = dates.create();
        DateTime newExpiration = dates.create(now, RepositoryConfiguration.GARBAGE_COLLECTION_SWEEP_PERIOD);
        DateTime expiry = dates.create(now, -RepositoryConfiguration.LOCK_EXPIRY_AGE_IN_MILLIS);

        // Iterate over the locks ...
        MutableCachedNode locksNode = mutableLocksNode();
        for (ChildReference ref : locksNode.getChildReferences(system)) {
            NodeKey key = ref.getKey();
            CachedNode lockNode = system.getNode(key);
            if (!booleans.create(first(lockNode, ModeShapeLexicon.IS_SESSION_SCOPED))) {
                // It's not session-scoped, so continue ...
                continue;
            }
            String lockingSessionId = strings.create(first(lockNode, ModeShapeLexicon.LOCKING_SESSION));
            if (activeSessionIds.contains(lockingSessionId)) {
                // Extend the lock ...
                MutableCachedNode mutableLockNode = system.mutable(key);
                Property prop = propertyFactory.create(ModeShapeLexicon.EXPIRATION_DATE, newExpiration);
                mutableLockNode.setProperty(system, prop);
            } else {
                // It's not used by an active session in this process, but may be in another process.
                // Check the age of the lock ...
                DateTime expired = dates.create(first(lockNode, ModeShapeLexicon.EXPIRATION_DATE));
                if (expired.isBefore(expiry)) {
                    // The lock's expiration time is earlier than our limit, so it is expired and needs to be removed ...
                    locksNode.removeChild(system, key);
                    system.destroy(key);
                }
            }
        }
    }

    void storeLock( JcrSession session,
                    ModeShapeLock lock,
                    DateTime expiration ) {
        MutableCachedNode locksNode = mutableLocksNode();
        Name name = names.create(lock.getLockToken());
        List<Property> properties = new ArrayList<Property>();
        properties.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.LOCK));
        properties.add(propertyFactory.create(JcrLexicon.LOCK_OWNER, lock.getLockOwner()));
        properties.add(propertyFactory.create(JcrLexicon.LOCK_IS_DEEP, lock.isDeep()));
        properties.add(propertyFactory.create(ModeShapeLexicon.WORKSPACE, lock.getWorkspaceName()));
        properties.add(propertyFactory.create(ModeShapeLexicon.LOCK_TOKEN, lock.getLockToken()));
        properties.add(propertyFactory.create(ModeShapeLexicon.IS_SESSION_SCOPED, lock.isSessionScoped()));
        // Locks are always created by sessions and then held by them unless explicitly removed later ...
        properties.add(propertyFactory.create(ModeShapeLexicon.IS_HELD_BY_SESSION, true));
        properties.add(propertyFactory.create(ModeShapeLexicon.LOCKING_SESSION, session.sessionId()));
        properties.add(propertyFactory.create(ModeShapeLexicon.EXPIRATION_DATE, expiration));
        locksNode.createChild(system, lock.getLockKey(), name, properties);
    }

    void removeLock( ModeShapeLock lock ) {
        MutableCachedNode locksNode = mutableLocksNode();
        NodeKey lockKey = lock.getLockKey();
        locksNode.removeChild(system, lockKey);
        system.destroy(lockKey);
    }

    /**
     * Updates the underlying repository directly (i.e., outside the scope of the {@link Session}) to mark the token for the given
     * lock as being held (or not held) by some {@link Session}. Note that this method does not identify <i>which</i> (if any)
     * session holds the token for the lock, just that <i>some</i> session holds the token for the lock.
     * 
     * @param lockToken the lock token for which the "held" status should be modified; may not be null
     * @param value the new value
     * @return true if the lock "held" status was successfully changed to the desired value, or false otherwise
     * @throws LockException if there is no such lock with the supplied token
     */
    boolean changeLockHeldBySession( String lockToken,
                                     boolean value ) throws LockException {
        CachedNode locksNode = locksNode();
        ChildReferences childRefs = locksNode.getChildReferences(system);
        Name name = names.create(lockToken);
        ChildReference ref = childRefs.getChild(name);
        if (ref == null) {
            throw new LockException(JcrI18n.invalidLockToken.text(lockToken));
        }
        MutableCachedNode lockNode = system.mutable(ref.getKey());
        boolean isHeld = booleans.create(first(lockNode, ModeShapeLexicon.IS_HELD_BY_SESSION, false));
        if (isHeld && value) {
            // The lock is already held by a session ...
            return false;
        }
        lockNode.setProperty(system, propertyFactory.create(ModeShapeLexicon.IS_HELD_BY_SESSION, value));
        return true;
    }

    public NodeKey versionHistoryNodeKeyFor( NodeKey versionableNodeKey ) {
        return systemKey().withId(versionableNodeKey.getIdentifier());
    }

    public boolean hasVersionHistory( NodeKey versionableNodeKey ) {
        return cache().getNode(versionHistoryNodeKeyFor(versionableNodeKey)) != null;
    }

    /**
     * Create and initialize the version history structure for a versionable node with the supplied UUID. This method assumes that
     * the version history node does not exist.
     * <p>
     * Given a NodeKey for a node that has an identifier part of "fae2b929-c5ef-4ce5-9fa1-514779ca0ae3", the SHA-1 hash of this
     * identifier part is "b46dde8905f76361779339fa3ccacc4f47664255". The path to the version history for this node is as follows:
     * 
     * <pre>
     *  + jcr:system
     *    + jcr:versionStorage   {jcr:primaryType = mode:versionStorage}
     *      + b4   {jcr:primaryType = mode:versionHistoryFolder}
     *        + 6d   {jcr:primaryType = mode:versionHistoryFolder}
     *          + de   {jcr:primaryType = mode:versionHistoryFolder}
     *            + 298905f76361779339fa3ccacc4f47664255   {jcr:primaryType = nt:versionHistory}
     *              + jcr:versionLabels  {jcr:primaryType = nt:versionLabels}
     *              + jcr:rootVersion  {jcr:primaryType = nt:version}
     *                - jcr:uuid = ...
     *                - jcr:created = ...
     *                + jcr:frozenNode  {jcr:primaryType = nt:frozenNode}
     *                  - jcr:frozenUuid
     *                  - jcr:frozenPrimaryType
     *                  - jcr:frozenMixinTypes
     * </pre>
     * 
     * Note that the path between "/jcr:system/jcr:versionStorage" and the "nt:versionHistory" node is shown as being
     * {@link JcrVersionManager.HiearchicalPathAlgorithm hiearchical}.
     * 
     * @param versionableNodeKey the identifier of the versionable node for which the history is to be created; may not be null
     * @param versionHistoryKey the key to the version history node; may not be null
     * @param versionKey the key to be used for the initial version; may be null if the key should be generated
     * @param primaryTypeName the name of the primary type of the versionable node; may not be null
     * @param mixinTypeNames the names of the mixin types for the versionable node; may be null or empty
     * @param versionHistoryPath the path of the version history node; may not be null
     * @param originalVersionKey the key of the original node from which the new versionable node was copied; may be null
     * @param now the current date time; may not be null
     * @return the history node; never null
     */
    protected MutableCachedNode initializeVersionStorage( NodeKey versionableNodeKey,
                                                          NodeKey versionHistoryKey,
                                                          NodeKey versionKey,
                                                          Name primaryTypeName,
                                                          Set<Name> mixinTypeNames,
                                                          Path versionHistoryPath,
                                                          NodeKey originalVersionKey,
                                                          DateTime now ) {
        assert versionHistoryPath != null;
        assert versionHistoryPath.size() == 6;

        CachedNode node = versionStorageNode();
        MutableCachedNode mutable = null;

        // Find the parent of the version history node by walking the path and creating any missing intermediate folders ...
        Path parentPathInStorage = versionHistoryPath.getParent().subpath(2);
        Property primaryType = null;
        for (Segment segment : parentPathInStorage) {
            ChildReferences childRefs = node.getChildReferences(system);
            ChildReference ref = childRefs.getChild(segment);
            if (ref != null) {
                // Look up the child node ...
                node = system.getNode(ref);
            } else {
                // Create the intermediate node ...
                MutableCachedNode mutableNode = system.mutable(node.getKey());
                NodeKey key = systemKey().withRandomId();
                if (primaryType == null) {
                    primaryType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.VERSION_HISTORY_FOLDER);
                }
                mutable = mutableNode.createChild(system, key, segment.getName(), primaryType);
                node = mutable;
            }
        }

        // See if the version history exists ...
        MutableCachedNode historyParent = mutable != null ? mutable : system.mutable(node.getKey());

        // Now create the version history node itself ...
        List<Property> historyProps = new ArrayList<Property>();
        historyProps.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION_HISTORY));
        historyProps.add(propertyFactory.create(JcrLexicon.VERSIONABLE_UUID, versionableNodeKey.getIdentifier()));
        historyProps.add(propertyFactory.create(JcrLexicon.UUID, versionHistoryKey.getIdentifier()));
        if (originalVersionKey != null) {
            //the tck expects this to be a reference, so that getNode works on it
            historyProps.add(propertyFactory.create(JcrLexicon.COPIED_FROM,
                                                    org.modeshape.jcr.value.PropertyType.WEAKREFERENCE,
                                                    referenceFactory.create(originalVersionKey, true)));
        }
        Name historyName = versionHistoryPath.getLastSegment().getName();
        MutableCachedNode history = historyParent.createChild(system, versionHistoryKey, historyName, historyProps);

        // Now create the 'nt:versionLabels' child node ...
        Property labelProp = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION_LABELS);
        MutableCachedNode labels = history.createChild(system, null, JcrLexicon.VERSION_LABELS, labelProp);
        assert labels != null;

        // And create the 'nt:rootVersion' child node ...
        NodeKey rootVersionKey = versionKey != null ? versionKey : systemKey().withRandomId();
        List<Property> rootProps = new ArrayList<Property>();
        rootProps.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION));
        rootProps.add(propertyFactory.create(JcrLexicon.CREATED, now));
        rootProps.add(propertyFactory.create(JcrLexicon.UUID, rootVersionKey.getIdentifier()));
        MutableCachedNode rootVersion = history.createChild(system, rootVersionKey, JcrLexicon.ROOT_VERSION, rootProps);

        // And create the 'nt:rootVersion/nt:frozenNode' child node ...
        NodeKey frozenNodeKey = rootVersion.getKey().withRandomId();

        List<Property> frozenProps = new ArrayList<Property>();
        frozenProps.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE));
        frozenProps.add(propertyFactory.create(JcrLexicon.FROZEN_UUID, versionableNodeKey.getIdentifier()));
        frozenProps.add(propertyFactory.create(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryTypeName));
        frozenProps.add(propertyFactory.create(JcrLexicon.UUID, frozenNodeKey));
        if (mixinTypeNames != null && !mixinTypeNames.isEmpty()) {
            frozenProps.add(propertyFactory.create(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypeNames));
        }
        MutableCachedNode frozenNode = rootVersion.createChild(system, frozenNodeKey, JcrLexicon.FROZEN_NODE, frozenProps);
        assert frozenNode != null;

        return history;
    }

    /**
     * The method efficiently updates the JCR version history and storage with a new version of a node being checked in. However,
     * it does not update the versionable node with the "mix:versionable" properties.
     * <p>
     * Note that this method will initialize the version history for the node if the version history does not already exist.
     * </p>
     * <p>
     * The names of the different versions has changed since 2.x, and now follows the same convention and algorithm as used in the
     * reference implementation. See {@link #nextNameForVersionNode(org.modeshape.jcr.cache.CachedNode, org.modeshape.jcr.value.Property, org.modeshape.jcr.cache.ChildReferences)} for details.
     * </p>
     * 
     * @param versionableNode the versionable node for which a new version is to be created in the node's version history; may not
     *        be null
     * @param cacheForVersionableNode the cache used to access the versionable node and any descendants; may not be null
     * @param versionHistoryPath the path of the version history node; may not be null
     * @param originalVersionKey the key of the original node from which the new versionable node was copied; may be null
     * @param versionableProperties the versionable node's properties that should be record in the version history (on the frozen
     *        node); may be null or empty
     * @param now the current date time; may not be null
     * @param frozenNodeOutput the reference that should be set upon successful completion to the frozen node created under the
     *        new version; may not be null
     * @return the version node in the version history; never null
     */
    public MutableCachedNode recordNewVersion( CachedNode versionableNode,
                                               SessionCache cacheForVersionableNode,
                                               Path versionHistoryPath,
                                               NodeKey originalVersionKey,
                                               Collection<Property> versionableProperties,
                                               DateTime now,
                                               AtomicReference<MutableCachedNode> frozenNodeOutput ) {
        assert versionHistoryPath != null;
        assert versionHistoryPath.size() == 6;

        // Get the information from this node ...
        NodeKey versionableNodeKey = versionableNode.getKey();
        Name primaryTypeName = versionableNode.getPrimaryType(cacheForVersionableNode);
        Set<Name> mixinTypeNames = versionableNode.getMixinTypes(cacheForVersionableNode);
        NodeKey versionHistoryKey = versionHistoryNodeKeyFor(versionableNodeKey);

        // Find the existing version history for this node (if it exists) ...
        Name versionName = null;
        MutableCachedNode historyNode = system.mutable(versionHistoryKey);
        Property predecessors = null;
        NodeKey versionKey = versionHistoryKey.withRandomId();
        if (historyNode == null) {
            // Initialize the version history ...
            historyNode = initializeVersionStorage(versionableNodeKey,
                                                   versionHistoryKey,
                                                   null,
                                                   primaryTypeName,
                                                   mixinTypeNames,
                                                   versionHistoryPath,
                                                   originalVersionKey,
                                                   now);
            // Overwrite the predecessor's property ...
            NodeKey rootVersionKey = historyNode.getChildReferences(system).getChild(JcrLexicon.ROOT_VERSION).getKey();
            Reference rootVersionRef = referenceFactory.create(rootVersionKey, true);
            predecessors = propertyFactory.create(JcrLexicon.PREDECESSORS, new Object[]{rootVersionRef});
            versionName = names.create("1.0");
        } else {
            ChildReferences historyChildren = historyNode.getChildReferences(system);
            predecessors = versionableNode.getProperty(JcrLexicon.PREDECESSORS, cacheForVersionableNode);
            versionName = nextNameForVersionNode(historyNode, predecessors, historyChildren);
        }

        // Create a 'nt:version' node under the version history node ...
        List<Property> props = new ArrayList<Property>();
        props.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION));
        props.add(predecessors);
        props.add(propertyFactory.create(JcrLexicon.CREATED, now));
        props.add(propertyFactory.create(JcrLexicon.UUID, versionKey.getIdentifier()));
        MutableCachedNode versionNode = historyNode.createChild(system, versionKey, versionName, props);

        // Create a 'nt:frozenNode' node under the 'nt:version' node ...
        NodeKey frozenNodeKey = systemKey().withRandomId();

        props = new ArrayList<Property>();
        props.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE));
        props.add(propertyFactory.create(JcrLexicon.FROZEN_UUID, versionableNodeKey.getIdentifier()));
        props.add(propertyFactory.create(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryTypeName));
        props.add(propertyFactory.create(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypeNames));
        props.add(propertyFactory.create(JcrLexicon.UUID, frozenNodeKey));

        if (versionableProperties != null) props.addAll(versionableProperties);
        MutableCachedNode frozenNode = versionNode.createChild(system, frozenNodeKey, JcrLexicon.FROZEN_NODE, props);
        assert frozenNode != null;
        frozenNodeOutput.set(frozenNode);

        // Now update the predecessor nodes to have the new version node be included as one of their successors ...
        Property successors = null;
        final Set<Reference> successorReferences = new HashSet<Reference>();
        for (Object value : predecessors) {
            NodeKey predecessorKey = ((NodeKeyReference) value).getNodeKey();
            CachedNode predecessor = system.getNode(predecessorKey);

            // Look up the 'jcr:successors' property on the predecessor ...
            successors = predecessor.getProperty(JcrLexicon.SUCCESSORS, system);
            if (successors != null) {
                // There were already successors, so we need to add our new version node the list ...
                successorReferences.clear();
                for (Object successorValue : successors) {
                    NodeKey successorKey = ((NodeKeyReference)successorValue).getNodeKey();
                    successorReferences.add(referenceFactory.create(successorKey, true));
                }
            }

            // Now add the uuid of the versionable node ...
            successorReferences.add(referenceFactory.create(versionKey, true));

            successors = propertyFactory.create(JcrLexicon.SUCCESSORS, org.modeshape.jcr.value.PropertyType.REFERENCE, successorReferences);
            system.mutable(predecessorKey).setProperty(system, successors);
        }

        // Return the newly-created version node ...
        return versionNode;
    }

    /**
     * Compute the name for the next version node in the given history. Note that the naming convention has changed since 2.x, and
     * now follows the same convention and algorithm as used in the reference implementation. See
     * org.apache.jackrabbit.core.version.InternalVersionManagerBase.calculateCheckinVersionName(...) for the original.
     * <p>
     * The basic rules are as follows:
     * <ul>
     * <li>first the predecessor version with the shortest name is searched.
     * <li>if that predecessor version is the root version, the new version gets the name "{number of successors}+1" + ".0"
     * <li>if that predecessor version has no successor, the last digit of it's version number is incremented.
     * <li>if that predecessor version has successors but the incremented name does not exist, that name is used.
     * <li>otherwise a ".0" is added to the name until a non conflicting name is found.
     * <ul>
     * Example Graph:
     * 
     * <pre>
     * jcr:rootVersion
     *  |     |
     * 1.0   2.0
     *  |
     * 1.1
     *  |
     * 1.2 ---\  ------\
     *  |      \        \
     * 1.3   1.2.0   1.2.0.0
     *  |      |
     * 1.4   1.2.1 ----\
     *  |      |        \
     * 1.5   1.2.2   1.2.1.0
     *  |      |        |
     * 1.6     |     1.2.1.1
     *  |------/
     * 1.7
     * </pre>
     * 
     * </p>
     * 
     *
     * @param historyNode
     * @param predecessors the 'jcr:predecessors' property; may not be null
     * @param historyChildren the child references under the version history for the node
     * @return the next name
     */
    protected Name nextNameForVersionNode( CachedNode historyNode,
                                           Property predecessors,
                                           ChildReferences historyChildren ) {
        String proposedName = null;
        CachedNode versionNode = null;

        // Try to find the versions in the history that are considered predecessors ...
        for (Object predecessor : predecessors) {
            if (predecessor == null) continue;
            NodeKey key = ((NodeKeyReference) predecessor).getNodeKey();
            CachedNode predecessorNode = system.getNode(key);
            Name predecessorName = predecessorNode.getName(system);
            if (proposedName == null || predecessorName.getLocalName().length() < proposedName.length()) {
                proposedName = predecessorName.getLocalName();
                versionNode = predecessorNode;
            }
        }
        if (proposedName == null) {
            proposedName = "1.0";
            Name versionName = names.create(proposedName);
            if (historyChildren.getChild(versionName) == null) return versionName;
            // Otherwise use the root version ...
            versionNode = system.getNode(historyChildren.getChild(JcrLexicon.ROOT_VERSION));
        }
        assert versionNode != null;

        // Now make sure the name is not used ...
        int index = proposedName.lastIndexOf('.');
        if (index > 0) {
            proposedName = proposedName.substring(0, index + 1) + (Integer.parseInt(proposedName.substring(index + 1)) + 1);
            Name versionName = names.create(proposedName); // excludes the trailing '.'
            while (historyChildren.getChild(versionName) != null) {
                proposedName = proposedName + ".0";
                versionName = names.create(proposedName);
            }
            return versionName;
        }

        // Get the number of successors of the version
        Property successors = versionNode.getProperty(JcrLexicon.SUCCESSORS, system);
        String baseName = successors != null ? Integer.toString(successors.size() + 1) : "1";
        return names.create(baseName + ".0");
    }
}
