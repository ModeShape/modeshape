/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.RepositoryNodeTypeManager.NodeTypes;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Local implementation of @{link NodeTypeManager}. This class handles translation between {@link Name}s and {@link String}s based
 * on the namespace registry from the session's execution context in order to support transient namespace remappings. All
 * {@link NodeType}s returned by this implementation are wrapped with the execution context of the session to allow proper ongoing
 * handling of names. This implies that reference equality is not a safe test for node type equivalence.
 * 
 * @see RepositoryNodeTypeManager
 */
@Immutable
public class JcrNodeTypeManager implements NodeTypeManager {

    private final JcrSession session;
    private final RepositoryNodeTypeManager repositoryTypeManager;

    JcrNodeTypeManager( JcrSession session,
                        RepositoryNodeTypeManager repositoryTypeManager ) {
        this.session = session;
        this.repositoryTypeManager = repositoryTypeManager;
    }

    private final ExecutionContext context() {
        return session.context();
    }

    // TODO: Query
    // Schemata schemata() {
    // if (schemata == null) {
    // schemata = repositoryTypeManager.getRepositorySchemata().getSchemataForSession(session);
    // assert schemata != null;
    // }
    // return schemata;
    // }

    void signalNamespaceChanges() {
        // TODO: Query
        // this.schemata = null;
    }

    final NodeTypes nodeTypes() {
        return repositoryTypeManager.getNodeTypes();
    }

    @Override
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        session.checkLive();
        return new JcrNodeTypeIterator(nodeTypes().getAllNodeTypes());
    }

    @Override
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        session.checkLive();
        Collection<JcrNodeType> rawTypes = nodeTypes().getMixinNodeTypes();
        List<JcrNodeType> types = new ArrayList<JcrNodeType>(rawTypes.size());

        // Need to return a version of the node type with the current context
        for (JcrNodeType type : rawTypes) {
            types.add(type.with(context()));
        }

        return new JcrNodeTypeIterator(types);
    }

    @Override
    public JcrNodeType getNodeType( String nodeTypeName ) throws NoSuchNodeTypeException, RepositoryException {
        session.checkLive();
        Name ntName = context().getValueFactories().getNameFactory().create(nodeTypeName);
        JcrNodeType type = nodeTypes().getNodeType(ntName);
        if (type != null) {
            type = type.with(context());
            return type;
        }
        throw new NoSuchNodeTypeException(JcrI18n.typeNotFound.text(nodeTypeName));
    }

    /**
     * Returns the node type with the given name (if one exists)
     * 
     * @param nodeTypeName the name of the node type to be returned
     * @return the node type with the given name (if one exists)
     * @see NodeTypes#getNodeType(Name)
     */
    JcrNodeType getNodeType( Name nodeTypeName ) {
        JcrNodeType nodeType = nodeTypes().getNodeType(nodeTypeName);

        if (nodeType != null) {
            nodeType = nodeType.with(context());
        }

        return nodeType;
    }

    /**
     * Returns true if and only if the node type with the given name exists.
     * <p>
     * This is equivalent to the following code:
     * 
     * <pre>
     * try {
     *     getNodeType(nodeTypeName);
     *     return true;
     * } catch (NoSuchNodeTypeException nsnte) {
     *     return false;
     * }
     * </pre>
     * 
     * However, the implementation is more efficient that the approach listed above and does not rely upon exceptions.
     * </p>
     * 
     * @param nodeTypeName the name of the node type
     * @return true if the named node type does exist, or false otherwise
     * @see NodeTypes#hasNodeType(Name)
     */
    @Override
    public boolean hasNodeType( String nodeTypeName ) {
        Name ntName = context().getValueFactories().getNameFactory().create(nodeTypeName);
        return nodeTypes().hasNodeType(ntName);
    }

    @Override
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        session.checkLive();
        Collection<JcrNodeType> rawTypes = nodeTypes().getPrimaryNodeTypes();
        List<JcrNodeType> types = new ArrayList<JcrNodeType>(rawTypes.size());

        // Need to return a version of the node type with the current context
        for (JcrNodeType type : rawTypes) {
            types.add(type.with(context()));
        }

        return new JcrNodeTypeIterator(types);
    }

    /**
     * Get the {@link NodeDefinition} for the root node.
     * 
     * @return the definition; never null
     * @throws RepositoryException
     * @throws NoSuchNodeTypeException
     */
    JcrNodeDefinition getRootNodeDefinition() throws NoSuchNodeTypeException, RepositoryException {
        NodeTypes nodeTypes = nodeTypes();
        for (NodeDefinition definition : nodeTypes.getNodeType(ModeShapeLexicon.ROOT).getChildNodeDefinitions()) {
            if (definition.getName().equals(JcrNodeType.RESIDUAL_ITEM_NAME)) return (JcrNodeDefinition)definition;
        }

        assert false; // should not get here
        return null;
    }

    /**
     * Get the node definition given the supplied identifier.
     * 
     * @param definitionId the identifier of the node definition
     * @return the node definition, or null if there is no such definition (or if the ID was null)
     */
    JcrNodeDefinition getNodeDefinition( NodeDefinitionId definitionId ) {
        if (definitionId == null) return null;
        return nodeTypes().getChildNodeDefinition(definitionId);
    }

    /**
     * Get the property definition given the supplied identifier.
     * 
     * @param definitionId the identifier of the node definition
     * @return the property definition, or null if there is no such definition (or if the ID was null)
     */
    JcrPropertyDefinition getPropertyDefinition( PropertyDefinitionId definitionId ) {
        if (definitionId == null) return null;
        return nodeTypes().getPropertyDefinition(definitionId);
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints} . Otherwise,
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
    final JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                        Collection<Name> mixinTypeNames,
                                                        Name propertyName,
                                                        Value value,
                                                        boolean checkMultiValuedDefinitions,
                                                        boolean skipProtected ) {
        return nodeTypes().findPropertyDefinition(session,
                                                  primaryTypeName,
                                                  mixinTypeNames,
                                                  propertyName,
                                                  value,
                                                  checkMultiValuedDefinitions,
                                                  skipProtected);
    }

    final JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                        Collection<Name> mixinTypeNames,
                                                        Name propertyName,
                                                        Value value,
                                                        boolean checkMultiValuedDefinitions,
                                                        boolean skipProtected,
                                                        boolean checkTypesAndConstraints ) {
        return nodeTypes().findPropertyDefinition(session,
                                                  primaryTypeName,
                                                  mixinTypeNames,
                                                  propertyName,
                                                  value,
                                                  checkMultiValuedDefinitions,
                                                  skipProtected,
                                                  checkTypesAndConstraints);
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value. with the given name and property type that allows the supplied values.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints} . Otherwise,
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
    final JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                        Collection<Name> mixinTypeNames,
                                                        Name propertyName,
                                                        Value[] values,
                                                        boolean skipProtected ) {
        return nodeTypes().findPropertyDefinition(session, primaryTypeName, mixinTypeNames, propertyName, values, skipProtected);
    }

    final JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                        Collection<Name> mixinTypeNames,
                                                        Name propertyName,
                                                        Value[] values,
                                                        boolean skipProtected,
                                                        boolean checkTypeAndConstraints ) {
        return nodeTypes().findPropertyDefinition(session,
                                                  primaryTypeName,
                                                  mixinTypeNames,
                                                  propertyName,
                                                  values,
                                                  skipProtected,
                                                  checkTypeAndConstraints);
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
        return nodeTypes().canRemoveProperty(primaryTypeNameOfParent, mixinTypeNamesOfParent, propertyName, skipProtected);
    }

    /**
     * Searches the supplied primary node type and the mixin node types of a parent node for a child node definition that is the
     * best match for a new child with the given name, primary node type name, and whether there are existing children with the
     * same name.
     * 
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param childName the name of the child to be added to the parent; may not be null
     * @param childPrimaryNodeType the name of the primary node type for the child node, or null if the primary type is not known
     *        and the {@link NodeDefinition#getDefaultPrimaryType() definition's default primary type} will be used
     * @param numberOfExistingChildrenWithSameName the number of existing children with the same name as the child to be added, or
     *        0 if this new child will be the first child with this name (or if the number of children is not known)
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the best child node definition, or <code>null</code> if no node definition allows a new child with the supplied
     *         name, primary type, and whether there are already children with the same name
     */
    final JcrNodeDefinition findChildNodeDefinition( Name primaryTypeNameOfParent,
                                                     Collection<Name> mixinTypeNamesOfParent,
                                                     Name childName,
                                                     Name childPrimaryNodeType,
                                                     int numberOfExistingChildrenWithSameName,
                                                     boolean skipProtected ) {
        return nodeTypes().findChildNodeDefinition(primaryTypeNameOfParent,
                                                   mixinTypeNamesOfParent,
                                                   childName,
                                                   childPrimaryNodeType,
                                                   numberOfExistingChildrenWithSameName,
                                                   skipProtected);
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
    final boolean canRemoveAllChildren( Name primaryTypeNameOfParent,
                                        Collection<Name> mixinTypeNamesOfParent,
                                        Name childName,
                                        boolean skipProtected ) {
        return nodeTypes().canRemoveAllChildren(primaryTypeNameOfParent, mixinTypeNamesOfParent, childName, skipProtected);
    }

    /**
     * Registers a new node type or updates an existing node type using the specified definition and returns the resulting
     * {@link NodeType} object.
     * <p>
     * Typically, the object passed to this method will be a {@link NodeTypeTemplate} (a subclass of {@link NodeTypeDefinition})
     * acquired from {@link JcrNodeTypeManager#createNodeTypeTemplate()} and then filled-in with definition information.
     * </p>
     * 
     * @param template the new node type to register
     * @param allowUpdate this flag is not used
     * @return the {@code newly created node type}
     * @throws InvalidNodeTypeDefinitionException if the {@code NodeTypeDefinition} is invalid
     * @throws NodeTypeExistsException if {@code allowUpdate} is false and the {@code NodeTypeDefinition} specifies a node type
     *         name that already exists
     * @throws UnsupportedRepositoryOperationException if {@code allowUpdate} is true; ModeShape does not allow updating node
     *         types at this time.
     * @throws AccessDeniedException if the current session does not have the {@link ModeShapePermissions#REGISTER_TYPE register
     *         type permission}.
     * @throws RepositoryException if another error occurs
     */
    @Override
    public NodeType registerNodeType( NodeTypeDefinition template,
                                      boolean allowUpdate )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
        AccessDeniedException, RepositoryException {

        session.checkLive();
        try {
            session.checkPermission((Path)null, ModeShapePermissions.REGISTER_TYPE);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }
        return this.repositoryTypeManager.registerNodeType(template, !allowUpdate);
    }

    /**
     * Registers or updates the specified Collection of {@code NodeTypeDefinition} objects. This method is used to register or
     * update a set of node types with mutual dependencies. Returns an iterator over the resulting {@code NodeType} objects.
     * <p>
     * The effect of the method is "all or nothing"; if an error occurs, no node types are registered or updated.
     * </p>
     * 
     * @param templates the new node types to register
     * @param allowUpdates this flag is not used
     * @return the {@code newly created node types}
     * @throws InvalidNodeTypeDefinitionException if a {@code NodeTypeDefinition} within the collection is invalid
     * @throws NodeTypeExistsException if {@code allowUpdate} is false and a {@code NodeTypeDefinition} within the collection
     *         specifies a node type name that already exists
     * @throws UnsupportedRepositoryOperationException if {@code allowUpdate} is true; ModeShape does not allow updating node
     *         types at this time.
     * @throws AccessDeniedException if the current session does not have the {@link ModeShapePermissions#REGISTER_TYPE register
     *         type permission}.
     * @throws RepositoryException if another error occurs
     */
    public NodeTypeIterator registerNodeTypes( Collection<NodeTypeDefinition> templates,
                                               boolean allowUpdates )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
        AccessDeniedException, RepositoryException {

        session.checkLive();
        try {
            session.checkPermission((Path)null, ModeShapePermissions.REGISTER_TYPE);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }
        return new JcrNodeTypeIterator(repositoryTypeManager.registerNodeTypes(templates, !allowUpdates, false, true));
    }

    /**
     * Registers the node types from the given {@code JcrNodeTypeSource}. This method is used to register or update a set of node
     * types with mutual dependencies. Returns an iterator over the resulting {@code NodeType} objects.
     * <p>
     * The effect of the method is "all or nothing"; if an error occurs, no node types are registered or updated.
     * </p>
     * 
     * @param nodeTypes the iterable object containing the new node types to register
     * @return the {@code newly created node types}
     * @throws InvalidNodeTypeDefinitionException if a {@code NodeTypeDefinition} within the collection is invalid
     * @throws NodeTypeExistsException if {@code allowUpdate} is false and a {@code NodeTypeDefinition} within the collection
     *         specifies a node type name that already exists
     * @throws UnsupportedRepositoryOperationException if {@code allowUpdate} is true; ModeShape does not allow updating node
     *         types at this time.
     * @throws AccessDeniedException if the current session does not have the {@link ModeShapePermissions#REGISTER_TYPE register
     *         type permission}.
     * @throws RepositoryException if another error occurs
     */
    public NodeTypeIterator registerNodeTypes( Iterable<NodeTypeDefinition> nodeTypes )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
        AccessDeniedException, RepositoryException {

        try {
            session.checkPermission((Path)null, ModeShapePermissions.REGISTER_TYPE);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }

        return new JcrNodeTypeIterator(this.repositoryTypeManager.registerNodeTypes(nodeTypes));
    }

    /**
     * Registers or updates the specified array of {@code NodeTypeDefinition} objects. This method is used to register or update a
     * set of node types with mutual dependencies. Returns an iterator over the resulting {@code NodeType} objects.
     * <p>
     * The effect of the method is "all or nothing"; if an error occurs, no node types are registered or updated.
     * </p>
     * 
     * @param ntds the new node types to register
     * @param allowUpdate must be {@code false}; ModeShape does not allow updating node types at this time
     * @return the {@code newly created node types}
     * @throws InvalidNodeTypeDefinitionException if a {@code NodeTypeDefinition} within the collection is invalid
     * @throws NodeTypeExistsException if {@code allowUpdate} is false and a {@code NodeTypeDefinition} within the collection
     *         specifies a node type name that already exists
     * @throws UnsupportedRepositoryOperationException if {@code allowUpdate} is true; ModeShape does not allow updating node
     *         types at this time.
     * @throws AccessDeniedException if the current session does not have the {@link ModeShapePermissions#REGISTER_TYPE register
     *         type permission}.
     * @throws RepositoryException if another error occurs
     */
    @Override
    public NodeTypeIterator registerNodeTypes( NodeTypeDefinition[] ntds,
                                               boolean allowUpdate )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
        RepositoryException {

        try {
            session.checkPermission((Path)null, ModeShapePermissions.REGISTER_TYPE);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }

        return new JcrNodeTypeIterator(this.repositoryTypeManager.registerNodeTypes(Arrays.asList(ntds),
                                                                                    !allowUpdate,
                                                                                    false,
                                                                                    true));
    }

    /**
     * Unregisters the named node type if it is not referenced by other node types as a supertype, a default primary type of a
     * child node (or nodes), or a required primary type of a child node (or nodes).
     * 
     * @param nodeTypeName
     * @throws NoSuchNodeTypeException if node type name does not correspond to a registered node type
     * @throws InvalidNodeTypeDefinitionException if the node type with the given name cannot be unregistered because it is the
     *         supertype, one of the required primary types, or a default primary type of another node type
     * @throws AccessDeniedException if the current session does not have the {@link ModeShapePermissions#REGISTER_TYPE register
     *         type permission}.
     * @throws RepositoryException if any other error occurs
     */
    @Override
    public void unregisterNodeType( String nodeTypeName )
        throws NoSuchNodeTypeException, InvalidNodeTypeDefinitionException, RepositoryException {
        unregisterNodeTypes(Collections.singleton(nodeTypeName));
    }

    /**
     * Allows the collection of node types to be unregistered if they are not referenced by other node types as supertypes,
     * default primary types of child nodes, or required primary types of child nodes.
     * 
     * @param nodeTypeNames the names of the node types to be unregistered
     * @throws NoSuchNodeTypeException if any of the node type names do not correspond to a registered node type
     * @throws InvalidNodeTypeDefinitionException if any of the node types with the given names cannot be unregistered because
     *         they are the supertype, one of the required primary types, or a default primary type of a node type that is not
     *         being unregistered.
     * @throws AccessDeniedException if the current session does not have the {@link ModeShapePermissions#REGISTER_TYPE register
     *         type permission}.
     * @throws RepositoryException if any other error occurs
     */
    public void unregisterNodeTypes( Collection<String> nodeTypeNames )
        throws NoSuchNodeTypeException, InvalidNodeTypeDefinitionException, RepositoryException {
        NameFactory nameFactory = context().getValueFactories().getNameFactory();

        try {
            session.checkPermission((Path)null, ModeShapePermissions.REGISTER_TYPE);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }

        Collection<Name> names = new ArrayList<Name>(nodeTypeNames.size());
        for (String name : nodeTypeNames) {
            names.add(nameFactory.create(name));
        }
        repositoryTypeManager.unregisterNodeType(names);
    }

    /**
     * Allows the collection of node types to be unregistered if they are not referenced by other node types as supertypes,
     * default primary types of child nodes, or required primary types of child nodes.
     * 
     * @param names the names of the node types to be unregistered
     * @throws NoSuchNodeTypeException if any of the node type names do not correspond to a registered node type
     * @throws InvalidNodeTypeDefinitionException if any of the node types with the given names cannot be unregistered because
     *         they are the supertype, one of the required primary types, or a default primary type of a node type that is not
     *         being unregistered.
     * @throws AccessDeniedException if the current session does not have the {@link ModeShapePermissions#REGISTER_TYPE register
     *         type permission}.
     * @throws RepositoryException if any other error occurs
     */
    @Override
    public void unregisterNodeTypes( String[] names ) throws NoSuchNodeTypeException, RepositoryException {
        unregisterNodeTypes(Arrays.asList(names));
    }

    /**
     * Returns an empty {@code NodeTypeTemplate} which can then be used to define a node type and passed to
     * {@link JcrNodeTypeManager#registerNodeType(NodeTypeDefinition, boolean)}
     * 
     * @return an empty {@code NodeTypeTemplate} which can then be used to define a node type and passed to
     *         {@link JcrNodeTypeManager#registerNodeType(NodeTypeDefinition, boolean)}.
     * @throws RepositoryException if another error occurs
     */
    @Override
    public NodeTypeTemplate createNodeTypeTemplate() throws RepositoryException {
        return new JcrNodeTypeTemplate(context());
    }

    /**
     * Returns a {@code NodeTypeTemplate} based on the definition given in {@code ntd}. This template can then be used to define a
     * node type and passed to {@link JcrNodeTypeManager#registerNodeType(NodeTypeDefinition, boolean)}
     * 
     * @param ntd an existing node type definition; null values will be ignored
     * @return an empty {@code NodeTypeTemplate} which can then be used to define a node type and passed to
     *         {@link JcrNodeTypeManager#registerNodeType(NodeTypeDefinition, boolean)}.
     * @throws RepositoryException if another error occurs
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public NodeTypeTemplate createNodeTypeTemplate( NodeTypeDefinition ntd ) throws RepositoryException {
        NodeTypeTemplate ntt = new JcrNodeTypeTemplate(context(), true);

        if (ntd != null) {
            ntt.setName(ntd.getName());
            ntt.setAbstract(ntd.isAbstract());
            ntt.setDeclaredSuperTypeNames(ntd.getDeclaredSupertypeNames());
            ntt.setMixin(ntd.isMixin());
            ntt.setOrderableChildNodes(ntd.hasOrderableChildNodes());
            ntt.setPrimaryItemName(ntd.getPrimaryItemName());
            ntt.setQueryable(ntd.isQueryable());

            // copy child nodes and props
            for (NodeDefinition nodeDefinition : ntd.getDeclaredChildNodeDefinitions()) {
                JcrNodeDefinitionTemplate ndt = new JcrNodeDefinitionTemplate(context());

                ndt.setAutoCreated(nodeDefinition.isAutoCreated());
                ndt.setDefaultPrimaryTypeName(nodeDefinition.getDefaultPrimaryTypeName());
                ndt.setMandatory(nodeDefinition.isMandatory());
                if (nodeDefinition.getName() != null) {
                    ndt.setName(nodeDefinition.getName());
                }
                ndt.setOnParentVersion(nodeDefinition.getOnParentVersion());
                ndt.setProtected(nodeDefinition.isProtected());
                ndt.setRequiredPrimaryTypeNames(nodeDefinition.getRequiredPrimaryTypeNames());
                ndt.setSameNameSiblings(nodeDefinition.allowsSameNameSiblings());

                ntt.getNodeDefinitionTemplates().add(ndt);
            }

            for (PropertyDefinition propertyDefinition : ntd.getDeclaredPropertyDefinitions()) {
                JcrPropertyDefinitionTemplate pdt = new JcrPropertyDefinitionTemplate(context());

                pdt.setAutoCreated(propertyDefinition.isAutoCreated());
                pdt.setAvailableQueryOperators(propertyDefinition.getAvailableQueryOperators());
                pdt.setDefaultValues(propertyDefinition.getDefaultValues());
                pdt.setFullTextSearchable(propertyDefinition.isFullTextSearchable());
                pdt.setMandatory(propertyDefinition.isMandatory());
                pdt.setMultiple(propertyDefinition.isMultiple());
                if (propertyDefinition.getName() != null) {
                    pdt.setName(propertyDefinition.getName());
                }
                pdt.setOnParentVersion(propertyDefinition.getOnParentVersion());
                pdt.setProtected(propertyDefinition.isProtected());
                pdt.setQueryOrderable(propertyDefinition.isQueryOrderable());
                pdt.setRequiredType(propertyDefinition.getRequiredType());
                pdt.setValueConstraints(propertyDefinition.getValueConstraints());

                ntt.getPropertyDefinitionTemplates().add(pdt);
            }
        }

        return ntt;
    }

    /**
     * Returns an empty {@code PropertyDefinitionTemplate} which can then be used to create a property definition and attached to
     * a {@code NodeTypeTemplate}.
     * 
     * @return an empty {@code PropertyDefinitionTemplate} which can then be used to create a property definition and attached to
     *         a {@code NodeTypeTemplate}.
     * @throws RepositoryException if another error occurs
     */
    @Override
    public NodeDefinitionTemplate createNodeDefinitionTemplate() throws RepositoryException {
        return new JcrNodeDefinitionTemplate(context());
    }

    /**
     * Returns an empty {@code PropertyDefinitionTemplate} which can then be used to create a property definition and attached to
     * a {@code NodeTypeTemplate}.
     * 
     * @return an empty {@code PropertyDefinitionTemplate} which can then be used to create a property definition and attached to
     *         a {@code NodeTypeTemplate}.
     * @throws RepositoryException if another error occurs
     */
    @Override
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate() throws RepositoryException {
        return new JcrPropertyDefinitionTemplate(context());
    }

    /**
     * Determine if any of the test type names are equal to or have been derived from the primary type or any of the mixins.
     * 
     * @param testTypeNames the names of the types or mixins being tested against (never <code>null</code>)
     * @param primaryTypeName the primary type name (never <code>null</code>)
     * @param mixinNames the mixin names (may be <code>null</code>)
     * @return <code>true</code> if at least one test type name is equal to or derived from the primary type or one of the mixins
     * @throws RepositoryException if there is an exception obtaining node types
     * @throws IllegalArgumentException if <code>testTypeNames</code> is <code>null</code> or empty or if
     *         <code>primaryTypeName</code> is <code>null</code> or zero length
     */
    public boolean isDerivedFrom( String[] testTypeNames,
                                  String primaryTypeName,
                                  String[] mixinNames ) throws RepositoryException {
        CheckArg.isNotEmpty(testTypeNames, "testTypeNames");
        CheckArg.isNotEmpty(primaryTypeName, "primaryTypeName");

        NameFactory nameFactory = context().getValueFactories().getNameFactory();
        Name[] typeNames = nameFactory.create(testTypeNames);

        // first check primary type
        for (Name typeName : typeNames) {
            JcrNodeType nodeType = getNodeType(typeName);

            if ((nodeType != null) && nodeType.isNodeType(primaryTypeName)) {
                return true;
            }
        }

        // now check mixins
        if (mixinNames != null) {
            for (String mixin : mixinNames) {
                for (Name typeName : typeNames) {
                    JcrNodeType nodeType = getNodeType(typeName);

                    if ((nodeType != null) && nodeType.isNodeType(mixin)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected String messageFrom( Problems problems,
                                  String message ) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        for (Problem problem : problems) {
            sb.append('\n').append(problem.getMessageString());
        }
        return sb.toString();
    }

    protected List<NodeTypeDefinition> importFromXml( InputSource source ) throws RepositoryException {
        XmlNodeTypeReader handler = new XmlNodeTypeReader(session);
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(handler);
            parser.parse(source);
        } catch (EnclosingSAXException ese) {
            Exception cause = ese.getException();
            if (cause instanceof RepositoryException) {
                throw (RepositoryException)cause;
            }
            throw new RepositoryException(cause);
        } catch (SAXParseException se) {
            throw new InvalidSerializedDataException(se);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        } catch (IOException ioe) {
            throw new RepositoryException(ioe);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RepositoryException(t);
        }
        return handler.getNodeTypeDefinitions();
    }

    @Override
    public void registerNodeTypeDefinitions( File file ) throws IOException, RepositoryException {
        String content = IoUtil.read(file);
        if (content.startsWith("<?xml")) {
            registerNodeTypes(importFromXml(new InputSource(new FileInputStream(file))));
        } else {
            CndImporter importer = new CndImporter(context(), true);
            Problems problems = new SimpleProblems();
            importer.importFrom(content, problems, file.getAbsolutePath());
            if (problems.hasErrors()) {
                // There are problems, so report the original problems ...
                String msg = JcrI18n.errorsParsingNodeTypeDefinitions.text(file.getAbsolutePath());
                throw new RepositoryException(messageFrom(problems, msg));
            }
            registerNodeTypes(importer.getNodeTypeDefinitions());
        }
    }

    @Override
    public void registerNodeTypeDefinitions( InputStream stream )
        throws IOException, javax.jcr.nodetype.InvalidNodeTypeDefinitionException, javax.jcr.nodetype.NodeTypeExistsException,
        UnsupportedRepositoryOperationException, RepositoryException {

        String content = IoUtil.read(stream);
        if (content.startsWith("<?xml")) {
            registerNodeTypes(importFromXml(new InputSource(new StringReader(content))));
        } else {
            CndImporter importer = new CndImporter(context(), true);
            Problems problems = new SimpleProblems();
            importer.importFrom(content, problems, "stream");
            if (problems.hasErrors()) {
                // There are problems, so report the original problems ...
                String msg = JcrI18n.errorsParsingStreamOfNodeTypeDefinitions.text();
                throw new RepositoryException(messageFrom(problems, msg));
            }
            registerNodeTypes(importer.getNodeTypeDefinitions());
        }
    }

    @Override
    public void registerNodeTypeDefinitions( URL url ) throws IOException, RepositoryException {
        String content = IoUtil.read(url.openStream());
        if (content.startsWith("<?xml")) {
            registerNodeTypes(importFromXml(new InputSource(new StringReader(content))));
        } else {
            CndImporter importer = new CndImporter(context(), true);
            Problems problems = new SimpleProblems();
            importer.importFrom(content, problems, url.toExternalForm());
            if (problems.hasErrors()) {
                // There are problems, so report the original problems ...
                String msg = JcrI18n.errorsParsingNodeTypeDefinitions.text(url.toExternalForm());
                throw new RepositoryException(messageFrom(problems, msg));
            }
            registerNodeTypes(importer.getNodeTypeDefinitions());
        }
    }
}
