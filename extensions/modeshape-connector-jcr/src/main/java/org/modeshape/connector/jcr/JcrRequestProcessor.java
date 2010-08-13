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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeIntLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.InvalidRequestException;
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

    private final Map<String, Workspace> workspaces = new HashMap<String, Workspace>();

    private final Repository repository;
    private final Credentials credentials;

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
     * @throws NoSuchWorkspaceException if the named workspace does not exist
     * @throws RepositoryException if there is an error creating a Session
     */
    protected Workspace workspaceFor( String workspaceName ) throws RepositoryException {
        Workspace workspace = workspaces.get(workspaceName);
        if (workspace == null) {
            Session session = null;
            try {
                // A workspace was specified, so use it to obtain the session ...
                if (credentials != null) {
                    // Try to create the session using the credentials ...
                    session = repository.login(credentials, workspaceName);
                } else {
                    // No credentials ...
                    session = repository.login(workspaceName);
                }
            } catch (NoSuchWorkspaceException e) {
                throw new InvalidWorkspaceException(e.getLocalizedMessage());
            }
            assert session != null;
            workspace = new Workspace(session);
            workspaces.put(workspaceName, workspace);
            if (workspaceName == null) {
                // This is the default workspace, so record the session for the null workspace name, too...
                workspaces.put(null, workspace);
            }
        }
        return workspace;
    }

    /**
     * Use the first workspace that's available, or if none is available establish one for the default workspace.
     * 
     * @return the workspace, or null if there was an error
     * @throws RepositoryException if there is an error creating a Session
     */
    protected Workspace workspace() throws RepositoryException {
        if (workspaces.isEmpty()) {
            // No sessions yet, so create a default one ...
            return workspaceFor(null);
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
        for (String actualName : workspace().session().getWorkspace().getAccessibleWorkspaceNames()) {
            if (actualName == null) continue;
            if (actualName.equals(workspaceName)) return true;
        }
        return false;
    }

    /**
     * Commit all changes to any sessions.
     * 
     * @throws RepositoryException if there is a problem committing any changes
     */
    public void commit() throws RepositoryException {
        for (Workspace workspace : workspaces.values()) {
            // Save the changes in this workspace, or throw an error.
            workspace.session().save();
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
            for (Workspace workspace : workspaces.values()) {
                try {
                    workspace.session().logout();
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
            Workspace workspace = workspaceFor(request.workspaceName());
            request.setActualWorkspaceName(workspace.name());
            request.setActualRootLocation(workspace.locationForRootNode());
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
            for (String workspaceName : workspace().session().getWorkspace().getAccessibleWorkspaceNames()) {
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
                request.setError(new InvalidRequestException(msg));
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
                        Workspace workspace = workspaceFor(desiredName);
                        workspace.session().getWorkspace().clone(request.nameOfWorkspaceToBeCloned(), "/", "/", true);
                        Location actualLocation = workspace.locationForRootNode();
                        request.setActualWorkspaceName(workspace.name());
                        request.setActualRootLocation(actualLocation);
                        break;
                }
                return;
            }
            // Otherwise, the workspace doesn't exist and JCR doesn't let us create one ...
            String msg = JcrConnectorI18n.unableToCreateWorkspaceInRepository.text(desiredName, getSourceName());
            request.setError(new InvalidRequestException(msg));
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
            Workspace workspace = workspaceFor(request.inWorkspace());
            Node node = workspace.node(request.at());
            Location actualLocation = workspace.locationFor(node);
            request.setActualLocationOfNode(actualLocation);
            // Read the children ...
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                request.addChild(workspace.locationFor(iter.nextNode()));
            }
            // Read the properties ...
            for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                request.addProperty(workspace.propertyFor(iter.nextProperty()));
            }
            // Add in the 'jcr:uuid' property ...
            if (actualLocation.hasIdProperties()) {
                request.addProperty(workspace.propertyFor(ModeShapeLexicon.UUID, actualLocation.getUuid()));
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
            Workspace workspace = workspaceFor(request.inWorkspace());
            Node parent = workspace.node(request.of());
            request.setActualLocationOfNode(workspace.locationFor(parent));
            for (NodeIterator iter = parent.getNodes(); iter.hasNext();) {
                request.addChild(workspace.locationFor(iter.nextNode()));
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
            Workspace workspace = workspaceFor(request.inWorkspace());
            Node node = workspace.node(request.at());
            Location actualLocation = workspace.locationFor(node);
            request.setActualLocationOfNode(actualLocation);
            // Read the properties ...
            for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                request.addProperty(workspace.propertyFor(iter.nextProperty()));
            }
            // Add in the 'jcr:uuid' property ...
            if (actualLocation.hasIdProperties()) {
                request.addProperty(workspace.propertyFor(ModeShapeLexicon.UUID, actualLocation.getUuid()));
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
        if (request == null) return;
        try {
            Workspace workspace = workspaceFor(request.inWorkspace());
            Node parent = workspace.node(request.under());
            String childName = workspace.stringFor(request.named());

            // Look for the primary type, if it was set on the request ...
            String primaryTypeName = null;
            for (Property property : request.properties()) {
                if (property.getName().equals(JcrLexicon.PRIMARY_TYPE)) {
                    primaryTypeName = workspace.stringFor(property.getFirstValue());
                    break;
                }
            }

            // Create the child node ...
            Node child = null;
            if (primaryTypeName != null) {
                child = parent.addNode(childName, primaryTypeName);
            } else {
                child = parent.addNode(childName);
            }
            assert child != null;

            // And set all of the properties on the new node ...
            workspace.setProperties(child, request);

            // Set up the actual results on the request ...
            request.setActualLocationOfNode(workspace.locationFor(child));
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        if (request == null) return;
        try {
            String fromWorkspaceName = request.fromWorkspace();
            String intoWorkspaceName = request.intoWorkspace();
            boolean sameWorkspace = fromWorkspaceName.equals(intoWorkspaceName);
            Workspace fromWorkspace = workspaceFor(fromWorkspaceName);
            Workspace intoWorkspace = sameWorkspace ? fromWorkspace : workspaceFor(intoWorkspaceName);
            Node sourceNode = fromWorkspace.node(request.from());
            Node targetNode = fromWorkspace.node(request.into());
            Location fromLocation = fromWorkspace.locationFor(sourceNode);

            // Calculate the source and destination paths ...
            String srcAbsPath = sourceNode.getPath();
            String destAbsPath = targetNode.getPath();
            String copyName = request.desiredName() != null ? intoWorkspace.stringFor(request.desiredName()) : sourceNode.getName();
            destAbsPath += '/' + copyName;

            // Perform the clone ...
            javax.jcr.Workspace workspace = intoWorkspace.session().getWorkspace();
            workspace.clone(fromWorkspaceName, srcAbsPath, destAbsPath, request.removeExisting());

            // Find the actual location of the result of the node ...
            Node last = null;
            for (NodeIterator iter = targetNode.getNodes(copyName); iter.hasNext();) {
                last = iter.nextNode();
            }
            Location intoLocation = intoWorkspace.locationFor(last);
            request.setActualLocations(fromLocation, intoLocation);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        if (request == null) return;
        try {
            String fromWorkspaceName = request.fromWorkspace();
            String intoWorkspaceName = request.intoWorkspace();
            boolean sameWorkspace = fromWorkspaceName.equals(intoWorkspaceName);
            Workspace fromWorkspace = workspaceFor(fromWorkspaceName);
            Workspace intoWorkspace = sameWorkspace ? fromWorkspace : workspaceFor(intoWorkspaceName);
            Node sourceNode = fromWorkspace.node(request.from());
            Node targetNode = fromWorkspace.node(request.into());
            Location fromLocation = fromWorkspace.locationFor(sourceNode);

            // Calculate the source and destination paths ...
            String srcAbsPath = sourceNode.getPath();
            String destAbsPath = targetNode.getPath();
            String copyName = request.desiredName() != null ? intoWorkspace.stringFor(request.desiredName()) : sourceNode.getName();
            destAbsPath += '/' + copyName;

            // Perform the copy ...
            javax.jcr.Workspace workspace = intoWorkspace.session().getWorkspace();
            if (sameWorkspace) {
                workspace.copy(srcAbsPath, destAbsPath);
            } else {
                workspace.copy(fromWorkspaceName, srcAbsPath, destAbsPath);
            }

            // Find the actual location of the result of the node ...
            Node last = null;
            for (NodeIterator iter = targetNode.getNodes(copyName); iter.hasNext();) {
                last = iter.nextNode();
            }
            Location intoLocation = intoWorkspace.locationFor(last);
            request.setActualLocations(fromLocation, intoLocation);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        if (request == null) return;
        try {
            Workspace workspace = workspaceFor(request.inWorkspace());
            Node node = workspace.node(request.at());
            Location actual = workspace.locationFor(node);
            node.remove();
            request.setActualLocationOfNode(actual);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        if (request == null) return;
        try {
            Workspace workspace = workspaceFor(request.inWorkspace());
            Node orig = workspace.node(request.from());
            Node into = request.into() != null ? workspace.node(request.into()) : null;
            Node before = request.before() != null ? workspace.node(request.before()) : null;
            Location originalLocation = workspace.locationFor(orig);
            Location newLocation = workspace.move(orig, into, request.desiredName(), before);
            request.setActualLocations(originalLocation, newLocation);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        if (request == null) return;
        try {
            Workspace workspace = workspaceFor(request.inWorkspace());
            Node node = workspace.node(request.on());
            Set<Name> newProperties = new HashSet<Name>();
            for (Map.Entry<Name, Property> entry : request.properties().entrySet()) {
                Name propertyName = entry.getKey();
                String name = workspace.stringFor(propertyName);
                Property property = entry.getValue();

                // We need to remove the existing property (if there is one) in case the
                // old property has a different cardinality than the new property ...
                javax.jcr.Property existing = node.hasProperty(name) ? node.getProperty(name) : null;
                if (existing != null && (property == null || existing.getDefinition().isMultiple() == property.isSingle())) {
                    // Remove the property ...
                    if (!existing.getDefinition().isProtected()) {
                        existing.remove();
                    }
                }
                if (property != null) {
                    if (property.size() == 1) {
                        // Try setting as a single-valued property ...
                        try {
                            workspace.setProperty(node, property, false);
                        } catch (ValueFormatException e) {
                            workspace.setProperty(node, property, true);
                        }
                    } else {
                        // Set as a multi-valued property ...
                        workspace.setProperty(node, property, true);
                    }
                    if (existing == null) newProperties.add(propertyName);
                }
            }

            if (request.removeOtherProperties()) {
                Set<String> stringNames = new HashSet<String>();
                for (Name name : request.properties().keySet()) {
                    stringNames.add(workspace.stringFor(name));
                }
                stringNames.add(workspace.stringFor(JcrLexicon.PRIMARY_TYPE));
                stringNames.add(workspace.stringFor(JcrLexicon.MIXIN_TYPES));
                PropertyIterator propertyIter = node.getProperties();
                while (propertyIter.hasNext()) {
                    javax.jcr.Property property = propertyIter.nextProperty();
                    if (!stringNames.contains(property.getName()) && !property.getDefinition().isProtected()) {
                        property.remove();
                    }
                }
            }

            // Set up the actual results on the request ...
            request.setActualLocationOfNode(workspace.locationFor(node));
            request.setNewProperties(newProperties);
        } catch (Throwable e) {
            request.setError(e);
        }
    }

    /**
     * An encapsulation of a remote JCR Session, with an ExecutionContext that contains a namespace registry which mirrors the
     * session's registry, and with factories that convert the JCR-specific values, paths, and names into their graph-equivalents.
     */
    protected class Workspace {
        private final Session session;
        /** The context used to transform the JCR session values into graph values */
        private final ExecutionContext context;
        /** The factories for creating graph property values from JCR values */
        private final ValueFactories factories;
        /** The factory for creating graph properties from JCR values */
        private final PropertyFactory propertyFactory;
        private final NameFactory nameFactory;
        private final ValueFactory<String> stringFactory;
        private final String name;
        private final javax.jcr.ValueFactory jcrValueFactory;

        protected Workspace( Session jcrSession ) throws RepositoryException {
            this.session = jcrSession;
            this.name = this.session.getWorkspace().getName();
            ExecutionContext connectorContext = getExecutionContext();
            NamespaceRegistry connectorRegistry = connectorContext.getNamespaceRegistry();
            this.context = connectorContext.with(new JcrNamespaceRegistry(getSourceName(), this.session, connectorRegistry));
            this.factories = context.getValueFactories();
            this.propertyFactory = context.getPropertyFactory();
            this.nameFactory = this.factories.getNameFactory();
            this.stringFactory = this.factories.getStringFactory();
            this.jcrValueFactory = this.session.getValueFactory();
        }

        public Location move( Node original,
                              Node newParent,
                              Name newName,
                              Node beforeSibling ) throws RepositoryException {
            // Determine whether the node needs to move ...
            if (newParent == null && beforeSibling != null) {
                newParent = beforeSibling.getParent();
            }

            if (newName != null || (newParent != null && !original.getParent().equals(newParent))) {
                // This is not just a reorder, so we definitely have to move first ...
                String destAbsPath = newParent != null ? newParent.getPath() : original.getParent().getPath();
                assert !destAbsPath.endsWith("/");
                String newNameStr = newName != null ? stringFor(newName) : original.getName();
                destAbsPath += '/' + newNameStr;
                session.move(original.getPath(), destAbsPath);
            }

            if (beforeSibling != null) {
                // Even if moved, the 'orginal' node should still point to the node we just moved ...
                String siblingName = nameFor(beforeSibling);
                String originalName = nameFor(original);
                original.getParent().orderBefore(originalName, siblingName);
            }
            return locationFor(original);
        }

        public String name() {
            return name;
        }

        public Session session() {
            return session;
        }

        protected String stringFor( Object value ) {
            return stringFactory.create(value);
        }

        protected String nameFor( Node node ) throws RepositoryException {
            String name = node.getName();
            int snsIndex = node.getIndex();
            return snsIndex == 1 ? name : name + '[' + snsIndex + ']';
        }

        public Property propertyFor( Name name,
                                     Object... values ) {
            return propertyFactory.create(name, values);
        }

        /**
         * Get the graph {@link Property property} for the supplied JCR property representation.
         * 
         * @param jcrProperty the JCR property; may not be null
         * @return the graph property
         * @throws RepositoryException if there is an error working with the session
         */
        public Property propertyFor( javax.jcr.Property jcrProperty ) throws RepositoryException {
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
            Name name = nameFactory.create(jcrProperty.getName());
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
        public Object convert( Value value,
                               javax.jcr.Property jcrProperty ) throws RepositoryException {
            if (value == null) return null;
            try {
                switch (value.getType()) {
                    case javax.jcr.PropertyType.BINARY:
                        return factories.getBinaryFactory().create(value.getBinary().getStream(), 0);
                    case javax.jcr.PropertyType.BOOLEAN:
                        return factories.getBooleanFactory().create(value.getBoolean());
                    case javax.jcr.PropertyType.DATE:
                        return factories.getDateFactory().create(value.getDate());
                    case javax.jcr.PropertyType.DOUBLE:
                        return factories.getDoubleFactory().create(value.getDouble());
                    case javax.jcr.PropertyType.LONG:
                        return factories.getLongFactory().create(value.getLong());
                    case javax.jcr.PropertyType.NAME:
                        return factories.getNameFactory().create(value.getString());
                    case javax.jcr.PropertyType.PATH:
                        return factories.getPathFactory().create(value.getString());
                    case javax.jcr.PropertyType.REFERENCE:
                        return factories.getReferenceFactory().create(value.getString());
                    case javax.jcr.PropertyType.STRING:
                        return factories.getStringFactory().create(value.getString());
                    case javax.jcr.PropertyType.UNDEFINED:
                        // We don't know what type of values these are, but we need to convert any value
                        // that depends on namespaces (e.g., names and paths) into Graph objects.
                        try {
                            // So first try a name ...
                            return factories.getNameFactory().create(value.getString());
                        } catch (ValueFormatException e) {
                            // Wasn't a name, so try a path ...
                            try {
                                return factories.getPathFactory().create(value.getString());
                            } catch (ValueFormatException e2) {
                                // Wasn't a path, so finally treat as a string ...
                                return factories.getStringFactory().create(value.getString());
                            }
                        }
                }
            } catch (ValueFormatException e) {
                // There was an error converting the JCR value into the appropriate graph value
                String typeName = javax.jcr.PropertyType.nameFromValue(value.getType());
                throw new RepositoryException(JcrConnectorI18n.errorConvertingJcrValueOfType.text(value.getString(), typeName));
            }
            return null;
        }

        /**
         * Obtain the actual location for the supplied node.
         * 
         * @param node the existing node; may not be null
         * @return the actual location, with UUID if the node is "mix:referenceable"
         * @throws RepositoryException if there is an error
         */
        public Location locationFor( Node node ) throws RepositoryException {
            // Get the path to the node ...
            String pathStr = node.getPath();
            Path path = factories.getPathFactory().create(pathStr);

            // Does the node have a UUID ...
            String uuidStr = node.getIdentifier();
            if (uuidStr != null) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    return Location.create(path, uuid);
                } catch (IllegalArgumentException e) {
                    // Ignore, since the identifier is not a valid UUID
                }
            }
            return Location.create(path);
        }

        public Location locationForRootNode() throws RepositoryException {
            return locationFor(session.getRootNode());
        }

        /**
         * Find the existing node given the location.
         * 
         * @param location the location of the node, which must have a {@link Location#getPath() path} and/or
         *        {@link Location#getUuid() UUID}
         * @return the existing node; never null
         * @throws RepositoryException if there is an error working with the session
         * @throws PathNotFoundException if the node could not be found by its path
         */
        public Node node( Location location ) throws RepositoryException {
            Node root = session.getRootNode();
            UUID uuid = location.getUuid();
            if (uuid != null) {
                try {
                    return session.getNodeByIdentifier(uuid.toString());
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
                                                                                                   session.getWorkspace()
                                                                                                          .getName(),
                                                                                                   pathStr));
                        }
                    }
                }
            }
            // Otherwise, we can't find the node ...
            String msg = JcrConnectorI18n.unableToFindNodeWithoutPathOrUuid.text(getSourceName(), location);
            throw new IllegalArgumentException(msg);
        }

        public void setProperties( Node node,
                                   Iterable<Property> properties ) throws RepositoryException {
            // Look for the internal property that ModeShape uses to track which properties are multi-valued w/r/t JCR ...
            Set<Name> multiValued = null;
            for (Property property : properties) {
                if (property.getName().equals(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES)) {
                    // Go through the properties and see which properties are multi-valued ...
                    multiValued = getMultiValuedProperties(property);
                    break;
                }
            }
            if (multiValued == null) multiValued = Collections.emptySet();

            // Now set each of the properties ...
            for (Property property : properties) {
                setProperty(node, property, multiValued.contains(property.getName()));
            }
        }

        protected Set<Name> getMultiValuedProperties( Property multiValuedProperty ) {
            // Go through the properties and see which properties are multi-valued ...
            Set<Name> multiValued = new HashSet<Name>();
            for (Object value : multiValuedProperty) {
                Name multiValuedPropertyName = nameFactory.create(value);
                if (multiValuedPropertyName != null) {
                    multiValued.add(multiValuedPropertyName);
                }
            }
            return multiValued;
        }

        protected void setProperty( Node node,
                                    Property property,
                                    boolean isMultiValued ) throws RepositoryException {
            Name name = property.getName();
            if (name.equals(JcrLexicon.PRIMARY_TYPE)) return;
            if (name.equals(ModeShapeIntLexicon.NODE_DEFINITON)) return;
            if (name.equals(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES)) return;
            if (name.equals(JcrLexicon.MIXIN_TYPES)) {
                for (Object mixinVvalue : property.getValuesAsArray()) {
                    String mixinTypeName = stringFor(mixinVvalue);
                    node.addMixin(mixinTypeName);
                }
                return;
            }

            // Otherwise, just set the normal property. First determine the expected type ...
            String propertyName = stringFor(name);
            if (isMultiValued) {
                Value[] values = new Value[property.size()];
                int index = 0;
                PropertyType propertyType = null;
                for (Object value : property) {
                    if (value == null) continue;
                    if (propertyType == null) propertyType = PropertyType.discoverType(value);
                    values[index] = convertToJcrValue(propertyType, value);
                    ++index;
                }
                node.setProperty(propertyName, values);
            } else {
                Object firstValue = property.getFirstValue();
                PropertyType propertyType = PropertyType.discoverType(firstValue);
                Value value = convertToJcrValue(propertyType, firstValue);
                node.setProperty(propertyName, value);
            }
        }

        protected Value convertToJcrValue( PropertyType graphType,
                                           Object graphValue ) throws RepositoryException {
            if (graphValue == null) return null;
            switch (graphType) {
                case NAME:
                case PATH:
                case UUID:
                case URI:
                case STRING:
                case OBJECT:
                    String stringValue = factories.getStringFactory().create(graphValue);
                    return jcrValueFactory.createValue(stringValue);
                case BOOLEAN:
                    Boolean booleanValue = factories.getBooleanFactory().create(graphValue);
                    return jcrValueFactory.createValue(booleanValue.booleanValue());
                case DECIMAL:
                    BigDecimal decimalValue = factories.getDecimalFactory().create(graphValue);
                    return jcrValueFactory.createValue(decimalValue);
                case DOUBLE:
                    Double doubleValue = factories.getDoubleFactory().create(graphValue);
                    return jcrValueFactory.createValue(doubleValue);
                case LONG:
                    Long longValue = factories.getLongFactory().create(graphValue);
                    return jcrValueFactory.createValue(longValue);
                case DATE:
                    Calendar calValue = factories.getDateFactory().create(graphValue).toCalendar();
                    return jcrValueFactory.createValue(calValue);
                case BINARY:
                    Binary binary = factories.getBinaryFactory().create(graphValue);
                    try {
                        binary.acquire();
                        javax.jcr.Binary jcrBinary = jcrValueFactory.createBinary(binary.getStream());
                        return jcrValueFactory.createValue(jcrBinary);
                    } finally {
                        binary.release();
                    }
                case WEAKREFERENCE:
                case REFERENCE:
                    boolean isWeak = graphType == PropertyType.WEAKREFERENCE;
                    String identifier = factories.getStringFactory().create(graphValue);
                    Node node = session.getNodeByIdentifier(identifier);
                    return jcrValueFactory.createValue(node, isWeak);
            }
            assert false;
            return null;
        }
    }
}
