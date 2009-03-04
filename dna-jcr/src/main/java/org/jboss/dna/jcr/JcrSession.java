/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.basic.LocalNamespaceRegistry;
import org.jboss.dna.jcr.JcrNamespaceRegistry.Behavior;
import org.xml.sax.ContentHandler;
import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
@NotThreadSafe
class JcrSession implements Session {

    private static final String[] NO_ATTRIBUTES_NAMES = new String[] {};

    /**
     * The repository that created this session.
     */
    private final JcrRepository repository;

    /**
     * The workspace that corresponds to this session.
     */
    private final JcrWorkspace workspace;

    /**
     * A JCR namespace registry that is specific to this session, with any locally-defined namespaces defined in this session.
     * This is backed by the workspace's namespace registry.
     */
    private final JcrNamespaceRegistry sessionRegistry;

    /**
     * The execution context for this session, which uses the {@link #sessionRegistry session's namespace registry}
     */
    protected final ExecutionContext executionContext;

    /**
     * The graph representing this session, which uses the {@link #graph session's graph}.
     */
    private final Graph graph;

    /**
     * The session-specific attributes that came from the {@link SimpleCredentials}' {@link SimpleCredentials#getAttributeNames()}
     */
    private final Map<String, Object> sessionAttributes;

    private final ReferenceMap<UUID, Node> nodesByUuid;
    private final ReferenceMap<String, Node> nodesByJcrUuid;
    private boolean isLive;
    private JcrRootNode rootNode;
    private PropertyDefinition anyMultiplePropertyDefinition;

    JcrSession( JcrRepository repository,
                JcrWorkspace workspace,
                ExecutionContext workspaceContext,
                Map<String, Object> sessionAttributes ) {
        assert repository != null;
        assert workspace != null;
        assert sessionAttributes != null;
        assert workspaceContext != null;
        this.repository = repository;
        this.sessionAttributes = sessionAttributes;
        this.workspace = workspace;

        // Create an execution context for this session, which should use the local namespace registry ...
        NamespaceRegistry workspaceRegistry = workspaceContext.getNamespaceRegistry();
        NamespaceRegistry local = new LocalNamespaceRegistry(workspaceRegistry);
        this.executionContext = workspaceContext.with(local);
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.JSR170_SESSION, local, workspaceRegistry);

        // Set up the graph to use for this session (which uses the session's namespace registry and context) ...
        this.graph = Graph.create(this.repository.getRepositorySourceName(),
                                  this.repository.getConnectionFactory(),
                                  this.executionContext);

        this.nodesByUuid = new ReferenceMap<UUID, Node>(ReferenceType.STRONG, ReferenceType.SOFT);
        this.nodesByJcrUuid = new ReferenceMap<String, Node>(ReferenceType.STRONG, ReferenceType.SOFT);
        this.isLive = true;

        assert this.repository != null;
        assert this.sessionAttributes != null;
        assert this.workspace != null;
        assert this.executionContext != null;
        assert this.sessionRegistry != null;
        assert this.graph != null;
    }

    ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    /**
     * Return an unmodifiable map of nodes given then UUID.
     * 
     * @return nodesByUuid
     */
    Map<UUID, Node> getNodesByUuid() {
        return Collections.unmodifiableMap(nodesByUuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getWorkspace()
     */
    public Workspace getWorkspace() {
        return this.workspace;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRepository()
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>null</code>
     * @see javax.jcr.Session#getAttribute(java.lang.String)
     */
    public Object getAttribute( String name ) {
        return sessionAttributes.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @return An empty array
     * @see javax.jcr.Session#getAttributeNames()
     */
    public String[] getAttributeNames() {
        Set<String> names = sessionAttributes.keySet();
        if (names.isEmpty()) return NO_ATTRIBUTES_NAMES;
        return names.toArray(new String[names.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefix(java.lang.String)
     */
    public String getNamespacePrefix( String uri ) throws RepositoryException {
        return sessionRegistry.getPrefix(uri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefixes()
     */
    public String[] getNamespacePrefixes() {
        return sessionRegistry.getPrefixes();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespaceURI(java.lang.String)
     */
    public String getNamespaceURI( String prefix ) throws RepositoryException {
        return sessionRegistry.getURI(prefix);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#setNamespacePrefix(java.lang.String, java.lang.String)
     */
    public void setNamespacePrefix( String newPrefix,
                                    String existingUri ) throws NamespaceException, RepositoryException {
        sessionRegistry.registerNamespace(newPrefix, existingUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#addLockToken(java.lang.String)
     */
    public void addLockToken( String lt ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if either <code>path</code> or <code>actions</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#checkPermission(java.lang.String, java.lang.String)
     */
    public void checkPermission( String path,
                                 String actions ) {
        CheckArg.isNotEmpty(path, "path");
        CheckArg.isNotEmpty(actions, "actions");
        if (!"read".equals(actions)) {
            throw new AccessControlException(JcrI18n.permissionDenied.text(path, actions));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#exportDocumentView(java.lang.String, org.xml.sax.ContentHandler, boolean, boolean)
     */
    public void exportDocumentView( String absPath,
                                    ContentHandler contentHandler,
                                    boolean skipBinary,
                                    boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#exportDocumentView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportDocumentView( String absPath,
                                    OutputStream out,
                                    boolean skipBinary,
                                    boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#exportSystemView(java.lang.String, org.xml.sax.ContentHandler, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  ContentHandler contentHandler,
                                  boolean skipBinary,
                                  boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#exportSystemView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  OutputStream out,
                                  boolean skipBinary,
                                  boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#getImportContentHandler(java.lang.String, int)
     */
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#getItem(java.lang.String)
     */
    public Item getItem( String absolutePath ) throws RepositoryException {
        CheckArg.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = executionContext.getValueFactories().getPathFactory().create(absolutePath);
        if (path.isRoot()) {
            return getRootNode();
        }
        // Since we don't know whether path refers to a node or a property, look to see if we can tell it's a node ...
        if (path.getLastSegment().hasIndex()) {
            try {
                return getNode(path);
            } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
                // If the node isn't found, throw a PathNotFoundException
                throw new PathNotFoundException(JcrI18n.pathNotFound.text(path));
            }
        }
        // We can't tell from the name, so try a node first ...
        try {
            return getNode(path);
        } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
            // A node was not found, so treat look for a node using the parent as the node's path ...
            Path parentPath = path.getParent();
            Name propertyName = path.getLastSegment().getName();
            try {
                return getNode(parentPath).getProperty(propertyName.getString(executionContext.getNamespaceRegistry()));
            } catch (org.jboss.dna.graph.property.PathNotFoundException e2) {
                // If the node isn't found, throw a PathNotFoundException
                throw new PathNotFoundException(JcrI18n.pathNotFound.text(path));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#getLockTokens()
     */
    public String[] getLockTokens() {
        throw new UnsupportedOperationException();
    }

    private Node getNode( Path path ) throws RepositoryException {
        // Get node from source
        org.jboss.dna.graph.Node graphNode = graph.getNodeAt(path);
        // First check if node already exists. We don't need to check for changes since that will be handled by an observer
        org.jboss.dna.graph.property.Property dnaUuidProp = graphNode.getPropertiesByName().get(JcrLexicon.UUID);
        if (dnaUuidProp == null) dnaUuidProp = graphNode.getPropertiesByName().get(DnaLexicon.UUID);
        if (dnaUuidProp != null) {
            UUID uuid = executionContext.getValueFactories().getUuidFactory().create(dnaUuidProp.getValues()).next();
            Node node = getNode(uuid);
            if (node != null) {
                return node;
            }
        }
        // If not create a new one & populate it
        JcrNode node;
        Path parentPath = path.getParent();
        if (parentPath.isRoot()) node = new JcrNode(this, ((JcrRootNode)getRootNode()).getInternalUuid(), path.getLastSegment());
        else node = new JcrNode(this, ((JcrNode)getNode(parentPath)).getInternalUuid(), path.getLastSegment());
        populateNode(node, graphNode);
        return node;
    }

    Node getNode( UUID uuid ) {
        return nodesByUuid.get(uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNodeByUUID(java.lang.String)
     */
    public Node getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        Node result = null;
        try {
            result = nodesByUuid.get(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            throw new ItemNotFoundException(JcrI18n.itemNotFoundWithUuid.text(uuid, workspace.getName()));
        } catch (Throwable e) {
            throw new RepositoryException(JcrI18n.errorWhileFindingNodeWithUuid.text(uuid, workspace.getName()));
        }
        if (result == null) {
            throw new ItemNotFoundException(JcrI18n.itemNotFoundWithUuid.text(uuid, workspace.getName()));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRootNode()
     */
    public Node getRootNode() throws RepositoryException {
        // Return cached root node if available
        if (rootNode != null) {
            return rootNode;
        }
        // Get root node from source
        assert executionContext.getValueFactories() != null;
        assert executionContext.getValueFactories().getPathFactory() != null;
        rootNode = new JcrRootNode(this);

        // Get root node from source
        Path rootPath = executionContext.getValueFactories().getPathFactory().createRootPath();
        org.jboss.dna.graph.Node dnaRootNode = graph.getNodeAt(rootPath);
        if (dnaRootNode.getProperty(JcrLexicon.PRIMARY_TYPE) == null) {
            // Add the primary type and update the source ...
            graph.set(JcrLexicon.PRIMARY_TYPE).to(JcrNtLexicon.BASE).on(rootPath);
            dnaRootNode = graph.getNodeAt(rootPath);
        }
        populateNode(rootNode, dnaRootNode);
        return rootNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getUserID()
     */
    public String getUserID() {
        Subject subject = executionContext.getSubject();
        if (subject == null) return null;
        Set<Principal> principals = subject.getPrincipals();
        if (principals == null || principals.isEmpty()) return null;
        return principals.iterator().next().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getValueFactory()
     */
    public ValueFactory getValueFactory() {
        final ValueFactories valueFactories = executionContext.getValueFactories();
        return new ValueFactory() {

            public Value createValue( String value,
                                      int propertyType ) {
                return new JcrValue(valueFactories, propertyType, value);
            }

            public Value createValue( Node value ) throws RepositoryException {
                return new JcrValue(valueFactories, PropertyType.REFERENCE, UUID.fromString(value.getUUID()));
            }

            public Value createValue( InputStream value ) {
                return new JcrValue(valueFactories, PropertyType.BINARY, value);
            }

            public Value createValue( Calendar value ) {
                return new JcrValue(valueFactories, PropertyType.DATE, value);
            }

            public Value createValue( boolean value ) {
                return new JcrValue(valueFactories, PropertyType.BOOLEAN, value);
            }

            public Value createValue( double value ) {
                return new JcrValue(valueFactories, PropertyType.DOUBLE, value);
            }

            public Value createValue( long value ) {
                return new JcrValue(valueFactories, PropertyType.LONG, value);
            }

            public Value createValue( String value ) {
                return new JcrValue(valueFactories, PropertyType.STRING, value);
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @return false
     * @see javax.jcr.Session#hasPendingChanges()
     */
    public boolean hasPendingChanges() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    @SuppressWarnings( "unused" )
    public Session impersonate( Credentials credentials ) throws RepositoryException {
        // this is not right:
        // return repository.login(credentials);
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#importXML(java.lang.String, java.io.InputStream, int)
     */
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#isLive()
     */
    public boolean isLive() {
        return isLive;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#itemExists(java.lang.String)
     */
    public boolean itemExists( String absolutePath ) throws RepositoryException {
        try {
            return (getItem(absolutePath) != null);
        } catch (PathNotFoundException error) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#logout()
     */
    public void logout() {
        if (!isLive()) {
            return;
        }
        LoginContext loginContext = executionContext.getLoginContext();
        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (LoginException error) {
                // TODO: Change to DnaException once DNA-180 is addressed
                throw new RuntimeException(error);
            }
        }
        isLive = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#move(java.lang.String, java.lang.String)
     */
    public void move( String srcAbsPath,
                      String destAbsPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute the JCR {@link PropertyType} for the given DNA {@link org.jboss.dna.graph.property.PropertyType}.
     * 
     * @param dnaPropertyType the DNA property type; never null
     * @return the JCR property type
     */
    static final int jcrPropertyTypeFor( org.jboss.dna.graph.property.PropertyType dnaPropertyType ) {
        // Get the DNA property type for this ...
        switch (dnaPropertyType) {
            case STRING:
                return PropertyType.STRING;
            case NAME:
                return PropertyType.NAME;
            case LONG:
                return PropertyType.LONG;
            case UUID:
                return PropertyType.STRING; // JCR treats UUID properties as strings
            case URI:
                return PropertyType.STRING;
            case PATH:
                return PropertyType.PATH;
            case BOOLEAN:
                return PropertyType.BOOLEAN;
            case DATE:
                return PropertyType.DATE;
            case DECIMAL:
                return PropertyType.UNDEFINED;
            case DOUBLE:
                return PropertyType.DOUBLE;
            case BINARY:
                return PropertyType.BINARY;
            case OBJECT:
                return PropertyType.UNDEFINED;
            case REFERENCE:
                return PropertyType.REFERENCE;
        }
        assert false;
        return PropertyType.UNDEFINED;
    }

    private void populateNode( AbstractJcrNode node,
                               org.jboss.dna.graph.Node graphNode ) throws RepositoryException {
        assert node != null;
        assert graphNode != null;

        // --------------------------------------------------
        // Create JCR children for corresponding DNA children
        // --------------------------------------------------
        node.setChildren(graphNode.getChildrenSegments());

        // ------------------------------------------------------
        // Create JCR properties for corresponding DNA properties
        // ------------------------------------------------------
        // First get the property type for each property, based upon the primary type and mixins ...
        Map<Name, PropertyDefinition> propertyDefinitionsByPropertyName = new HashMap<Name, PropertyDefinition>();
        boolean referenceable = false;

        NamespaceRegistry registry = executionContext.getNamespaceRegistry();
        ValueFactories factories = executionContext.getValueFactories();
        NameFactory nameFactory = factories.getNameFactory();
        NodeTypeManager nodeTypeManager = getWorkspace().getNodeTypeManager();
        org.jboss.dna.graph.property.Property primaryTypeProperty = graphNode.getProperty(JcrLexicon.PRIMARY_TYPE);
        if (primaryTypeProperty != null && !primaryTypeProperty.isEmpty()) {
            Name primaryTypeName = nameFactory.create(primaryTypeProperty.getFirstValue());
            String primaryTypeNameString = primaryTypeName.getString(registry);
            NodeType primaryType = nodeTypeManager.getNodeType(primaryTypeNameString);
            for (PropertyDefinition propertyDefn : primaryType.getPropertyDefinitions()) {
                Name name = nameFactory.create(propertyDefn.getName());
                propertyDefinitionsByPropertyName.put(name, propertyDefn);
            }
        }
        org.jboss.dna.graph.property.Property mixinTypesProperty = graphNode.getProperty(JcrLexicon.MIXIN_TYPES);
        if (mixinTypesProperty != null && !mixinTypesProperty.isEmpty()) {
            for (Object mixinTypeValue : mixinTypesProperty) {
                Name mixinTypeName = nameFactory.create(mixinTypeValue);
                if (!referenceable && JcrMixLexicon.REFERENCEABLE.equals(mixinTypeName)) referenceable = true;
                String mixinTypeNameString = mixinTypeName.getString(registry);
                NodeType primaryType = nodeTypeManager.getNodeType(mixinTypeNameString);
                for (PropertyDefinition propertyDefn : primaryType.getPropertyDefinitions()) {
                    Name name = nameFactory.create(propertyDefn.getName());
                    propertyDefinitionsByPropertyName.put(name, propertyDefn);
                }
            }
        }

        // Look for the UUID property ...
        UUID uuid = null;
        org.jboss.dna.graph.property.Property uuidProperty = graphNode.getProperty(JcrLexicon.UUID);
        Name jcrUuidPropertyName = null;
        Name dnaUuidPropertyName = null;
        if (uuidProperty != null) {
            jcrUuidPropertyName = uuidProperty.getName();
            // Grab the first 'good' UUID value ...
            for (Object uuidValue : uuidProperty) {
                try {
                    uuid = factories.getUuidFactory().create(uuidValue);
                    break;
                } catch (ValueFormatException e) {
                    // Ignore; just continue with the next property value
                }
            }
        }
        if (uuid == null) {
            // Look for the DNA UUID property ...
            org.jboss.dna.graph.property.Property dnaUuidProperty = graphNode.getProperty(DnaLexicon.UUID);
            if (dnaUuidProperty != null) {
                dnaUuidPropertyName = dnaUuidProperty.getName();
                // Grab the first 'good' UUID value ...
                for (Object uuidValue : dnaUuidProperty) {
                    try {
                        uuid = factories.getUuidFactory().create(uuidValue);
                        break;
                    } catch (ValueFormatException e) {
                        // Ignore; just continue with the next property value
                    }
                }
            }
        }

        // Now create the JCR property object wrapper around the "jcr:uuid" property ...
        Set<Property> properties = new HashSet<Property>();
        if (uuid == null) uuid = UUID.randomUUID();
        if (referenceable) {
            if (uuidProperty == null) uuidProperty = executionContext.getPropertyFactory().create(JcrLexicon.UUID, uuid);
            PropertyDefinition propertyDefinition = propertyDefinitionsByPropertyName.get(JcrLexicon.UUID);
            properties.add(new JcrSingleValueProperty(node, executionContext, propertyDefinition, uuidProperty));
        }

        // Now create the JCR property object wrappers around the other properties ...
        for (org.jboss.dna.graph.property.Property dnaProp : graphNode.getProperties()) {
            Name name = dnaProp.getName();

            // Skip the JCR and DNA UUID properties (using the EXACT Name instances on the Property) ...
            if (name == jcrUuidPropertyName || name == dnaUuidPropertyName) continue;

            // Figure out the JCR property type for this property ...
            PropertyDefinition propertyDefinition = propertyDefinitionsByPropertyName.get(name);

            // If no property type was specified, then use "nt:unstructured" property definitions for any property
            if (propertyDefinition == null) {
                if (anyMultiplePropertyDefinition == null) {
                    String unstructuredName = JcrNtLexicon.UNSTRUCTURED.getString(registry);
                    NodeType unstructured = nodeTypeManager.getNodeType(unstructuredName);
                    for (PropertyDefinition definition : unstructured.getDeclaredPropertyDefinitions()) {
                        if (definition.isMultiple()) {
                            anyMultiplePropertyDefinition = definition;
                        }
                    }
                }
                assert anyMultiplePropertyDefinition != null;
                propertyDefinition = anyMultiplePropertyDefinition;
            }
            assert propertyDefinition != null;

            // Figure out if this is a multi-valued property ...
            boolean isMultiple = propertyDefinition.isMultiple();
            if (!isMultiple && dnaProp.isEmpty()) {
                // Only multi-valued properties can have no values; so if not multi-valued, then skip ...
                continue;
            }

            // Create the appropriate JCR property wrapper ...
            if (isMultiple) {
                properties.add(new JcrMultiValueProperty(node, executionContext, propertyDefinition, dnaProp));
            } else {
                properties.add(new JcrSingleValueProperty(node, executionContext, propertyDefinition, dnaProp));
            }
        }

        if (referenceable) {
            assert uuidProperty != null;
        }

        // Now set the properties on the node ...
        node.setProperties(properties);

        // Set node's UUID, creating one if necessary
        nodesByJcrUuid.put(uuid.toString(), node);
        node.setInternalUuid(uuid);
        // Setup node to be retrieved by DNA UUID
        nodesByUuid.put(uuid, node);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#removeLockToken(java.lang.String)
     */
    public void removeLockToken( String lt ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#save()
     */
    public void save() {
        throw new UnsupportedOperationException();
    }
}
