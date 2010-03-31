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
package org.modeshape.connector.jcr;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeIntLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceException;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.UnsupportedRequestException;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A {@link RequestProcessor} that processes {@link Request}s by operating against the JCR {@link Repository}.
 */
@NotThreadSafe
public class JcrRequestProcessor extends RequestProcessor {

    private static final Set<String> NON_MAPPABLE_NAMESPACE_PREFIXES = Collections.unmodifiableSet(new HashSet<String>(
                                                                                                                       Arrays.asList(new String[] {
                                                                                                                           "jcr",
                                                                                                                           "nt",
                                                                                                                           "mix",
                                                                                                                           ModeShapeLexicon.Namespace.PREFIX,
                                                                                                                           ModeShapeIntLexicon.Namespace.PREFIX})));

    private final Map<String, Session> workspaces = new HashMap<String, Session>();

    private final Repository repository;
    private final Credentials credentials;
    private final ValueFactories factories;
    private final PropertyFactory propertyFactory;

    /**
     * @param sourceName
     * @param context
     * @param repository
     * @param observer
     * @param credentials
     * @param defaultCachePolicy
     */
    public JcrRequestProcessor( String sourceName,
                                ExecutionContext context,
                                Repository repository,
                                Observer observer,
                                Credentials credentials,
                                CachePolicy defaultCachePolicy ) {
        super(sourceName, context, observer, null, defaultCachePolicy);
        this.repository = repository;
        this.credentials = credentials;
        this.factories = context.getValueFactories();
        this.propertyFactory = context.getPropertyFactory();
    }

    /**
     * Obtain the JCR session for the named workspace in the current repository using the credentials given to this processor
     * during instantiation. If no workspace name is supplied, a session for the default workspace is obtained.
     * <p>
     * This method caches a single {@link Session} object for each named (or default) workspace, so multiple calls with the same
     * workspace name will always result in the same {@link Session} object. These cached Session objects are released only when
     * this processor is {@link #close() closed}.
     * </p>
     * 
     * @param workspaceName the name of the workspace for which a Session is to be found, or null if a session to the default
     *        workspace should be returned
     * @return the Session to the workspace, or null if there was an error
     * @throws RepositoryException if there is an error creating a Session
     */
    protected Session sessionFor( String workspaceName ) throws RepositoryException {
        Session session = workspaces.get(workspaceName);
        if (session == null) {
            if (workspaceName != null) {
                // A workspace was specified, so use it to obtain the session ...
                if (credentials != null) {
                    // Try to create the session using the credentials ...
                    session = repository.login(credentials, workspaceName);
                } else {
                    // No credentials ...
                    session = repository.login(workspaceName);
                }
            } else {
                // No workspace name was given, so obtain a session using the default ...
                if (credentials != null) {
                    // Try to create the session using the credentials ...
                    session = repository.login(credentials);
                } else {
                    // No credentials, and no workspace name ...
                    session = repository.login();
                }
                // Use the real workspace name ...
                workspaceName = session.getWorkspace().getName();
                // But also record this session for the null workspace name ...
                workspaces.put(null, session);
            }
            assert session != null;
            workspaces.put(workspaceName, session);
        }
        return session;
    }

    /**
     * Use the first session that's available, or if none is available establish one for the default workspace.
     * 
     * @return the Session to the workspace, or null if there was an error
     * @throws RepositoryException if there is an error creating a Session
     */
    protected Session session() throws RepositoryException {
        if (workspaces.isEmpty()) {
            // No sessions yet, so create a default one ...
            return sessionFor(null);
        }
        // Grab any of the sessions ...
        return workspaces.values().iterator().next();
    }

    /**
     * Determine if there is an existing workspace with the supplied name.
     * 
     * @param workspaceName the name of the workspace
     * @return true if the workspace with the supplied name does exist, or false if it does not
     * @throws RepositoryException if there is an error working with the session
     */
    protected boolean workspaceExistsNamed( String workspaceName ) throws RepositoryException {
        if (workspaces.containsKey(workspaceName)) return true;
        for (String actualName : session().getWorkspace().getAccessibleWorkspaceNames()) {
            if (actualName == null) continue;
            if (actualName.equals(workspaceName)) return true;
        }
        return false;
    }

    /**
     * Obtain the actual location for the supplied node.
     * 
     * @param node the existing node; may not be null
     * @return the actual location, with UUID if the node is "mix:referenceable"
     * @throws RepositoryException if there is an error
     */
    protected Location locationFor( Node node ) throws RepositoryException {
        // Get the path to the node ...
        String pathStr = node.getPath();
        Path path = factories.getPathFactory().create(pathStr);

        // Does the node have a UUID ...
        if (node.isNodeType("mix:referenceable")) {
            String uuidStr = node.getUUID();
            if (uuidStr != null) {
                UUID uuid = UUID.fromString(uuidStr);
                return Location.create(path, uuid);
            }
        }
        return Location.create(path);
    }

    /**
     * Find the existing node given the location.
     * 
     * @param session the session that should be used; may not be null
     * @param location the location of the node, which must have a {@link Location#getPath() path} and/or
     *        {@link Location#getUuid() UUID}
     * @return the existing node; never null
     * @throws RepositoryException if there is an error working with the session
     * @throws PathNotFoundException if the node could not be found by its path
     */
    protected Node node( Session session,
                         Location location ) throws RepositoryException {
        Node root = session.getRootNode();
        UUID uuid = location.getUuid();
        if (uuid != null) {
            try {
                return session.getNodeByUUID(uuid.toString());
            } catch (ItemNotFoundException e) {
                if (!location.hasPath()) {
                    String msg = JcrConnectorI18n.unableToFindNodeWithUuid.text(getSourceName(), uuid);
                    throw new PathNotFoundException(location, factories.getPathFactory().createRootPath(), msg);
                }
                // Otherwise, try to find it by its path ...
            }
        }
        if (location.hasPath()) {
            Path relativePath = location.getPath().relativeToRoot();
            ValueFactory<String> stringFactory = factories.getStringFactory();
            String relativePathStr = stringFactory.create(relativePath);
            try {
                return root.getNode(relativePathStr);
            } catch (javax.jcr.PathNotFoundException e) {
                // Figure out the lowest existing path ...
                Node node = root;
                for (Path.Segment segment : relativePath) {
                    try {
                        node = node.getNode(stringFactory.create(segment));
                    } catch (javax.jcr.PathNotFoundException e2) {
                        String pathStr = stringFactory.create(location.getPath());
                        Path lowestPath = factories.getPathFactory().create(node.getPath());
                        throw new PathNotFoundException(location, lowestPath,
                                                        JcrConnectorI18n.nodeDoesNotExist.text(getSourceName(),
                                                                                               session.getWorkspace().getName(),
                                                                                               pathStr));
                    }
                }
            }
        }
        // Otherwise, we can't find the node ...
        String msg = JcrConnectorI18n.unableToFindNodeWithoutPathOrUuid.text(getSourceName(), location);
        throw new IllegalArgumentException(msg);
    }

    /**
     * Get the graph {@link Property property} for the supplied JCR property representation.
     * 
     * @param jcrProperty the JCR property; may not be null
     * @return the graph property
     * @throws RepositoryException if there is an error working with the session
     */
    protected Property propertyFor( javax.jcr.Property jcrProperty ) throws RepositoryException {
        // Get the values ...
        Object[] values = null;
        if (jcrProperty.getDefinition().isMultiple()) {
            Value[] jcrValues = jcrProperty.getValues();
            values = new Object[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                values[i] = convert(jcrValues[i], jcrProperty);
            }
        } else {
            values = new Object[] {convert(jcrProperty.getValue(), jcrProperty)};
        }
        // Get the name, and then create the property ...
        Name name = factories.getNameFactory().create(jcrProperty.getName());
        return propertyFactory.create(name, values);
    }

    /**
     * Utility method used to convert a JCR {@link Value} object into a valid graph property value.
     * 
     * @param value the JCR value; may be null
     * @param jcrProperty the JCR property, used to access the session (if needed)
     * @return the graph representation of the value
     * @throws RepositoryException if there is an error working with the session
     */
    protected Object convert( Value value,
                              javax.jcr.Property jcrProperty ) throws RepositoryException {
        if (value == null) return null;
        switch (value.getType()) {
            case javax.jcr.PropertyType.BINARY:
                return factories.getBinaryFactory().create(value.getStream(), 0);
            case javax.jcr.PropertyType.BOOLEAN:
                return factories.getBooleanFactory().create(value.getBoolean());
            case javax.jcr.PropertyType.DATE:
                return factories.getDateFactory().create(value.getDate());
            case javax.jcr.PropertyType.DOUBLE:
                return factories.getDoubleFactory().create(value.getDouble());
            case javax.jcr.PropertyType.LONG:
                return factories.getLongFactory().create(value.getLong());
            case javax.jcr.PropertyType.NAME:
                try {
                    return factories.getNameFactory().create(value.getString());
                } catch (ValueFormatException e) {
                    registerAllNamespaces(jcrProperty.getSession());
                    return factories.getNameFactory().create(value.getString());
                }
            case javax.jcr.PropertyType.PATH:
                try {
                    return factories.getPathFactory().create(value.getString());
                } catch (NamespaceException e) {
                    registerAllNamespaces(jcrProperty.getSession());
                    return factories.getPathFactory().create(value.getString());
                }
            case javax.jcr.PropertyType.REFERENCE:
                return factories.getReferenceFactory().create(value.getString());
            case javax.jcr.PropertyType.STRING:
                return factories.getStringFactory().create(value.getString());
        }
        return null;
    }

    /**
     * Utility method that extracts from the session all namespaces.
     * 
     * @param session the JCR session; may not be null
     * @throws RepositoryException if there is an error working with the session
     */
    protected void registerAllNamespaces( Session session ) throws RepositoryException {
        NamespaceRegistry registry = getExecutionContext().getNamespaceRegistry();
        for (String nsPrefix : session.getNamespacePrefixes()) {
            if (NON_MAPPABLE_NAMESPACE_PREFIXES.contains(nsPrefix)) continue;
            String nsUri = session.getNamespaceURI(nsPrefix);
            registry.register(nsPrefix, nsUri);
        }
    }

    /**
     * Commit all changes to any sessions.
     * 
     * @throws RepositoryException if there is a problem committing any changes
     */
    public void commit() throws RepositoryException {
        for (Session session : workspaces.values()) {
            // Save the changes in this workspace, or throw an error.
            session.save();
            // Note that this does not really behave like a distributed transaction, because
            // the first may succeed while the second fails (and all subsequent will not be saved).
        }
    }

    /**
     * Rollback all changes made to any sessions.
     * 
     * @throws RepositoryException if there is a problem committing any changes
     */
    public void rollback() throws RepositoryException {
        // don't do anything ...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        try {
            RuntimeException problem = null;
            for (Session session : workspaces.values()) {
                try {
                    session.logout();
                } catch (RuntimeException e) {
                    if (problem == null) problem = e;
                }
            }
            if (problem != null) throw problem;
        } finally {
            try {
                workspaces.clear();
            } finally {
                super.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        if (request == null) return;
        try {
            Session session = sessionFor(request.workspaceName());
            Location actualLocation = locationFor(session.getRootNode());
            request.setActualWorkspaceName(session.getWorkspace().getName());
            request.setActualRootLocation(actualLocation);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        if (request == null) return;
        try {
            Set<String> workspaceNames = new HashSet<String>();
            for (String workspaceName : session().getWorkspace().getAccessibleWorkspaceNames()) {
                workspaceNames.add(workspaceName);
            }
            request.setAvailableWorkspaceNames(workspaceNames);
            setCacheableInfo(request);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        if (request == null) return;
        try {
            // See if the workspace exists, so we record the right error ...
            String desiredName = request.desiredNameOfNewWorkspace();
            if (workspaceExistsNamed(desiredName)) {
                String msg = JcrConnectorI18n.workspaceAlreadyExistsInRepository.text(desiredName, getSourceName());
                request.setError(new InvalidWorkspaceException(msg));
            } else {
                // The workspace does not yet exist, but JCR doesn't let us create it ...
                String msg = JcrConnectorI18n.unableToCreateWorkspaceInRepository.text(desiredName, getSourceName());
                request.setError(new UnsupportedRequestException(msg));
            }
        } catch (Throwable e) {
            request.setError(e);
        }
        assert request.hasError();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        if (request == null) return;
        try {
            // JCR requires that the target workspace already exist, so check it ...
            String desiredName = request.desiredNameOfTargetWorkspace();
            if (workspaceExistsNamed(desiredName)) {
                switch (request.cloneConflictBehavior()) {
                    case DO_NOT_CLONE:
                        String msg = JcrConnectorI18n.workspaceAlreadyExistsInRepository.text(desiredName, getSourceName());
                        request.setError(new InvalidWorkspaceException(msg));
                        break;
                    case SKIP_CLONE:
                        // Perform the clone ...
                        Session session = sessionFor(desiredName);
                        session.getWorkspace().clone(request.nameOfWorkspaceToBeCloned(), "/", "/", true);
                        Location actualLocation = locationFor(session.getRootNode());
                        request.setActualWorkspaceName(session.getWorkspace().getName());
                        request.setActualRootLocation(actualLocation);
                        break;
                }
                return;
            }
            // Otherwise, the workspace doesn't exist and JCR doesn't let us create one ...
            String msg = JcrConnectorI18n.unableToCreateWorkspaceInRepository.text(desiredName, getSourceName());
            request.setError(new UnsupportedRequestException(msg));
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        if (request == null) return;
        String msg = JcrConnectorI18n.unableToDestroyWorkspaceInRepository.text(request.workspaceName(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        if (request == null) return;
        try {
            Node node = node(sessionFor(request.inWorkspace()), request.at());
            request.setActualLocationOfNode(locationFor(node));
            // Read the children ...
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                request.addChild(locationFor(iter.nextNode()));
            }
            // Read the properties ...
            for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                request.addProperty(propertyFor(iter.nextProperty()));
            }
            setCacheableInfo(request);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        if (request == null) return;
        try {
            Node parent = node(sessionFor(request.inWorkspace()), request.of());
            request.setActualLocationOfNode(locationFor(parent));
            for (NodeIterator iter = parent.getNodes(); iter.hasNext();) {
                request.addChild(locationFor(iter.nextNode()));
            }
            setCacheableInfo(request);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        if (request == null) return;
        try {
            Node node = node(sessionFor(request.inWorkspace()), request.at());
            request.setActualLocationOfNode(locationFor(node));
            // Read the properties ...
            for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                request.addProperty(propertyFor(iter.nextProperty()));
            }
            // Get the number of children ...
            NodeIterator childIter = node.getNodes();
            int numChildren = (int)childIter.getSize();
            if (numChildren == -1) {
                numChildren = 0;
                while (childIter.hasNext()) {
                    childIter.nextNode();
                    ++numChildren;
                }
            }
            request.setNumberOfChildren(numChildren);
            setCacheableInfo(request);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
    }

}
