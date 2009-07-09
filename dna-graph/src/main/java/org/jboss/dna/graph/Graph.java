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
package org.jboss.dna.graph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.collection.EmptyIterator;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.io.GraphImporter;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.request.BatchRequestBuilder;
import org.jboss.dna.graph.request.CacheableRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.RequestBuilder;
import org.jboss.dna.graph.request.UnsupportedRequestException;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest.CloneConflictBehavior;
import org.jboss.dna.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import org.xml.sax.SAXException;

/**
 * A graph representation of the content within a {@link RepositorySource}, including mechanisms to interact and manipulate that
 * content. The graph is designed to be an <i><a href="http://en.wikipedia.org/wiki/Domain_Specific_Language">embedded domain
 * specific language</a></i>, meaning calls to it are designed to read like sentences even though they are really just Java
 * methods. And to be more readable, methods can be chained together.
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class Graph {

    protected static final Iterator<Property> EMPTY_PROPERTIES = new EmptyIterator<Property>();
    protected static final Iterable<Property> NO_PROPERTIES = new Iterable<Property>() {
        public final Iterator<Property> iterator() {
            return EMPTY_PROPERTIES;
        }
    };

    /**
     * Create a graph instance that uses the supplied repository and {@link ExecutionContext context}.
     * 
     * @param sourceName the name of the source that should be used
     * @param connectionFactory the factory of repository connections
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the source or context parameters are null
     * @throws RepositorySourceException if a source with the supplied name does not exist
     */
    public static Graph create( String sourceName,
                                RepositoryConnectionFactory connectionFactory,
                                ExecutionContext context ) {
        return new Graph(sourceName, connectionFactory, context);
    }

    /**
     * Create a graph instance that uses the supplied {@link RepositoryConnection} and {@link ExecutionContext context}.
     * 
     * @param connection the connection that should be used
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the connection or context parameters are null
     */
    public static Graph create( final RepositoryConnection connection,
                                ExecutionContext context ) {
        CheckArg.isNotNull(connection, "connection");
        final String connectorSourceName = connection.getSourceName();
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (connectorSourceName.equals(sourceName)) return connection;
                return null;
            }
        };
        return new Graph(connectorSourceName, connectionFactory, context);
    }

    /**
     * Create a graph instance that uses the supplied {@link RepositoryConnection} and {@link ExecutionContext context}.
     * 
     * @param source the source that should be used
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the connection or context parameters are null
     */
    public static Graph create( final RepositorySource source,
                                ExecutionContext context ) {
        CheckArg.isNotNull(source, "source");
        final String connectorSourceName = source.getName();
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (connectorSourceName.equals(sourceName)) return source.getConnection();
                return null;
            }
        };
        return new Graph(connectorSourceName, connectionFactory, context);
    }

    private final String sourceName;
    private final RepositoryConnectionFactory connectionFactory;
    private final ExecutionContext context;
    protected final RequestBuilder requests;
    protected final Conjunction<Graph> nextGraph;
    private Workspace currentWorkspace;

    protected Graph( String sourceName,
                     RepositoryConnectionFactory connectionFactory,
                     ExecutionContext context ) {
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(context, "context");
        this.sourceName = sourceName;
        this.connectionFactory = connectionFactory;
        this.context = context;
        this.nextGraph = new Conjunction<Graph>() {
            public Graph and() {
                return Graph.this;
            }
        };
        this.requests = new RequestBuilder() {
            @Override
            protected <T extends Request> T process( T request ) {
                Graph.this.execute(request);
                return request;
            }
        };
    }

    /**
     * Get the RepositoryConnectionFactory that this graph uses to create {@link RepositoryConnection repository connections}.
     * 
     * @return the factory repository connections used by this graph; never null
     */
    public RepositoryConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * The name of the repository that will be used by this graph. This name is passed to the {@link #getConnectionFactory()
     * connection factory} when this graph needs to {@link RepositoryConnectionFactory#createConnection(String) obtain} a
     * {@link RepositoryConnection repository connection}.
     * 
     * @return the name of the source
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Get the context of execution within which operations on this graph are performed.
     * 
     * @return the execution context; never null
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * Obtain a connection to the source, execute the supplied request, and check the request for {@link Request#getError()
     * errors}. If an error is found, then it is thrown (or wrapped by a {@link RepositorySourceException} if the error is not a
     * {@link RuntimeException}.
     * <p>
     * This method is called automatically when the {@link #requests request builder} creates each request.
     * </p>
     * 
     * @param request the request to be executed (may be a {@link CompositeRequest}.
     * @throws PathNotFoundException if the request used a node that did not exist
     * @throws InvalidRequestException if the request was not valid
     * @throws InvalidWorkspaceException if the workspace used in the request was not valid
     * @throws UnsupportedRequestException if the request was not supported by the source
     * @throws RepositorySourceException if an error occurs during execution
     * @throws RuntimeException if a runtime error occurs during execution
     */
    protected void execute( Request request ) {
        RepositoryConnection connection = Graph.this.getConnectionFactory().createConnection(getSourceName());
        if (connection == null) {
            throw new RepositorySourceException(GraphI18n.unableToFindRepositorySourceWithName.text(getSourceName()));
        }
        try {
            connection.execute(Graph.this.getContext(), request);
        } finally {
            connection.close();
        }
        if (request.hasError()) {
            Throwable error = request.getError();
            if (error instanceof RuntimeException) throw (RuntimeException)error;
            throw new RepositorySourceException(getSourceName(), error);
        }
    }

    /**
     * Get the default cache policy for this graph. May be null if such a policy has not been defined for thie
     * {@link #getSourceName() source}.
     * 
     * @return the default cache policy, or null if no such policy has been defined for the source
     * @throws RepositorySourceException if no repository source with the {@link #getSourceName() name} could be found
     */
    public CachePolicy getDefaultCachePolicy() {
        RepositoryConnection connection = this.connectionFactory.createConnection(getSourceName());
        if (connection == null) {
            throw new RepositorySourceException(GraphI18n.unableToFindRepositorySourceWithName.text(getSourceName()));
        }
        try {
            return connection.getDefaultCachePolicy();
        } finally {
            connection.close();
        }
    }

    /**
     * Utility method to set the workspace that will be used by this graph.
     * 
     * @param workspaceName the name of the workspace; may not be null
     * @param actualRootLocation the actual location of the root node in the workspace; may not be null
     * @return the workspace; never null
     */
    protected Workspace setWorkspace( String workspaceName,
                                      Location actualRootLocation ) {
        assert workspaceName != null;
        assert actualRootLocation != null;
        this.currentWorkspace = new GraphWorkspace(workspaceName, actualRootLocation);
        return this.currentWorkspace;
    }

    /**
     * Get the name of the current workspace being used by this graph. If the graph has not yet been instructed to
     * {@link #useWorkspace(String) use} or {@link #createWorkspace() create} a workspace, this method will assume that the
     * source's default workspace is to be used and will obtain from the source the name of that default workspace.
     * 
     * @return the name of the current workspace; never null
     * @see #getCurrentWorkspace()
     */
    public String getCurrentWorkspaceName() {
        return getCurrentWorkspace().getName();
    }

    /**
     * Get the name of the current workspace being used by this graph. If the graph has not yet been instructed to
     * {@link #useWorkspace(String) use} or {@link #createWorkspace() create} a workspace, this method will assume that the
     * source's default workspace is to be used and will obtain from the source the name of that default workspace. If the source
     * does not have a default workspace, this method will fail with an {@link InvalidWorkspaceException}.
     * 
     * @return the name of the current workspace; never null
     * @see #getCurrentWorkspaceName()
     * @throws InvalidWorkspaceException if there is no current workspace
     */
    public Workspace getCurrentWorkspace() {
        if (this.currentWorkspace == null) {
            useWorkspace(null);
        }
        assert this.currentWorkspace != null;
        return this.currentWorkspace;
    }

    /**
     * Get the set of workspace names that are known to this source and accessible by this {@link #getContext() context}.
     * 
     * @return the set of workspace names; never null
     */
    public Set<String> getWorkspaces() {
        return requests.getWorkspaces().getAvailableWorkspaceNames();
    }

    /**
     * Switch this graph to use another existing workspace in the same source.
     * 
     * @param workspaceName the name of the existing workspace that this graph should begin using, or null if the graph should use
     *        the "default" workspace in the source (if there is one)
     * @return the workspace; never null
     * @throws InvalidWorkspaceException if the workspace with the supplied name does not exist, or if null is supplied as the
     *         workspace name but the source does not have a default workspace
     */
    public Workspace useWorkspace( String workspaceName ) {
        VerifyWorkspaceRequest request = requests.verifyWorkspace(workspaceName);
        return setWorkspace(request.getActualWorkspaceName(), request.getActualLocationOfRoot());
    }

    /**
     * Create a new workspace in the source used by this graph. This graph's workspace will be set as soon as the new workspace is
     * created, and all subsequent operations will use the new workspace (until it is changed again by
     * {@link #useWorkspace(String) using another workspace} or {@link #createWorkspace() creating another}.
     * 
     * @return the interface used to complete the request to create a new workspace; never null
     */
    public CreateWorkspace createWorkspace() {
        return new CreateWorkspace() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.Graph.NameWorkspace#named(java.lang.String)
             */
            public Workspace named( String workspaceName ) {
                CreateWorkspaceRequest request = requests.createWorkspace(workspaceName, CreateConflictBehavior.DO_NOT_CREATE);
                return setWorkspace(request.getActualWorkspaceName(), request.getActualLocationOfRoot());
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.Graph.CreateWorkspace#namedSomethingLike(java.lang.String)
             */
            public Workspace namedSomethingLike( String workspaceName ) {
                CreateWorkspaceRequest request = requests.createWorkspace(workspaceName,
                                                                          CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
                return setWorkspace(request.getActualWorkspaceName(), request.getActualLocationOfRoot());
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.Graph.CreateWorkspace#clonedFrom(java.lang.String)
             */
            public NameWorkspace clonedFrom( final String nameOfWorkspaceToClone ) {
                return new NameWorkspace() {
                    /**
                     * {@inheritDoc}
                     * 
                     * @see org.jboss.dna.graph.Graph.NameWorkspace#named(java.lang.String)
                     */
                    public Workspace named( String nameOfWorkspaceToCreate ) {
                        CloneWorkspaceRequest request = requests.cloneWorkspace(nameOfWorkspaceToClone,
                                                                                nameOfWorkspaceToCreate,
                                                                                CreateConflictBehavior.DO_NOT_CREATE,
                                                                                CloneConflictBehavior.DO_NOT_CLONE);
                        return setWorkspace(request.getActualWorkspaceName(), request.getActualLocationOfRoot());
                    }

                    /**
                     * {@inheritDoc}
                     * 
                     * @see org.jboss.dna.graph.Graph.NameWorkspace#namedSomethingLike(java.lang.String)
                     */
                    public Workspace namedSomethingLike( String nameOfWorkspaceToCreate ) {
                        CloneWorkspaceRequest request = requests.cloneWorkspace(nameOfWorkspaceToClone,
                                                                                nameOfWorkspaceToCreate,
                                                                                CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME,
                                                                                CloneConflictBehavior.DO_NOT_CLONE);
                        return setWorkspace(request.getActualWorkspaceName(), request.getActualLocationOfRoot());
                    }
                };
            }
        };
    }

    /**
     * Begin the request to move the specified node into a parent node at a different location, which is specified via the
     * <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Node from ) {
        return move(from.getLocation());
    }

    /**
     * Begin the request to move a node at the specified location into a parent node at a different location, which is specified
     * via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the location of the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Location from ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, from) {
            @Override
            protected Conjunction<Graph> submit( Locations from,
                                                 Location into,
                                                 Location before,
                                                 Name newName ) {
                String workspaceName = getCurrentWorkspaceName();
                do {
                    requests.moveBranch(from.getLocation(), into, before, workspaceName, newName);
                } while ((from = from.next()) != null);
                return and();
            }
        };
    }

    /**
     * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param fromPath the path to the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( String fromPath ) {
        return move(Location.create(createPath(fromPath)));
    }

    /**
     * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the path to the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Path from ) {
        return move(Location.create(from));
    }

    /**
     * Begin the request to move a node with the specified unique identifier into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the UUID of the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( UUID from ) {
        return move(Location.create(from));
    }

    /**
     * Begin the request to move a node with the specified unique identification property into a parent node at a different
     * location, which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The identification
     * property should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param idProperty the unique identification property of the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Property idProperty ) {
        return move(Location.create(idProperty));
    }

    /**
     * Begin the request to move a node with the specified identification properties into a parent node at a different location,
     * which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The identification properties
     * should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param firstIdProperty the first identification property of the node that is to be moved
     * @param additionalIdProperties the remaining identification properties of the node that is to be moved
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
        return move(Location.create(firstIdProperty, additionalIdProperties));
    }

    /**
     * Begin the request to clone a node at the specified location into a parent node at a different location, which is specified
     * via the <code>into(...)</code> method on the returned {@link Clone} object.
     * <p>
     * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the {@link WithUuids UUID
     * behavior} is specified.
     * </p>
     * <p>
     * The clone operation differs from the copy operation in that it must replicate nodes from one workspace to another (the copy
     * operations supports replicating nodes within a workspace as well as across workspaces) and that it preserves UUIDs (the
     * copy operation always generates new UUIDs).
     * </p>
     * 
     * @param from the location of the node that is to be cloned.
     * @return the object that can be used to specify the location of the node where the node is to be cloned
     */
    public Clone<Graph> clone( Location from ) {
        return new CloneAction<Graph>(this, from) {
            @Override
            protected Graph submit( String fromWorkspaceName,
                                    Location from,
                                    String intoWorkspaceName,
                                    Location into,
                                    Name desiredName,
                                    Segment desiredSegment,
                                    boolean removeExisting ) {
                requests.cloneBranch(from,
                                     fromWorkspaceName,
                                     into,
                                     intoWorkspaceName,
                                     desiredName,
                                     desiredSegment,
                                     removeExisting);
                return and();
            }
        };
    }

    /**
     * Begin the request to clone the specified node into a parent node at a different location, which is specified via the
     * <code>into(...)</code> method on the returned {@link Clone} object.
     * <p>
     * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the {@link WithUuids UUID
     * behavior} is specified.
     * </p>
     * <p>
     * The clone operation differs from the copy operation in that it must replicate nodes from one workspace to another (the copy
     * operations supports replicating nodes within a workspace as well as across workspaces) and that it preserves UUIDs (the
     * copy operation always generates new UUIDs).
     * </p>
     * 
     * @param from the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Clone<Graph> clone( Node from ) {
        return clone(from.getLocation());
    }

    /**
     * Begin the request to clone a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Clone} object.
     * <p>
     * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the {@link WithUuids UUID
     * behavior} is specified.
     * </p>
     * <p>
     * The clone operation differs from the copy operation in that it must replicate nodes from one workspace to another (the copy
     * operations supports replicating nodes within a workspace as well as across workspaces) and that it preserves UUIDs (the
     * copy operation always generates new UUIDs).
     * </p>
     * 
     * @param fromPath the path to the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Clone<Graph> clone( String fromPath ) {
        return clone(Location.create(createPath(fromPath)));
    }

    /**
     * Begin the request to clone a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Clone} object.
     * <p>
     * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the {@link WithUuids UUID
     * behavior} is specified.
     * </p>
     * <p>
     * The clone operation differs from the copy operation in that it must replicate nodes from one workspace to another (the copy
     * operations supports replicating nodes within a workspace as well as across workspaces) and that it preserves UUIDs (the
     * copy operation always generates new UUIDs).
     * </p>
     * 
     * @param from the path to the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Clone<Graph> clone( Path from ) {
        return clone(Location.create(from));
    }

    /**
     * Begin the request to clone a node with the specified unique identifier into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Clone} object.
     * <p>
     * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the {@link WithUuids UUID
     * behavior} is specified.
     * </p>
     * <p>
     * The clone operation differs from the copy operation in that it must replicate nodes from one workspace to another (the copy
     * operations supports replicating nodes within a workspace as well as across workspaces) and that it preserves UUIDs (the
     * copy operation always generates new UUIDs).
     * </p>
     * 
     * @param from the UUID of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Clone<Graph> clone( UUID from ) {
        return clone(Location.create(from));
    }

    /**
     * Begin the request to clone a node with the specified unique identification property into a parent node at a different
     * location, which is specified via the <code>into(...)</code> method on the returned {@link Clone} object. The identification
     * property should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the {@link WithUuids UUID
     * behavior} is specified.
     * </p>
     * <p>
     * The clone operation differs from the copy operation in that it must replicate nodes from one workspace to another (the copy
     * operations supports replicating nodes within a workspace as well as across workspaces) and that it preserves UUIDs (the
     * copy operation always generates new UUIDs).
     * </p>
     * 
     * @param idProperty the unique identification property of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Clone<Graph> clone( Property idProperty ) {
        return clone(Location.create(idProperty));
    }

    /**
     * Begin the request to clone a node with the specified identification properties into a parent node at a different location,
     * which is specified via the <code>into(...)</code> method on the returned {@link Clone} object. The identification
     * properties should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the {@link WithUuids UUID
     * behavior} is specified.
     * </p>
     * <p>
     * The clone operation differs from the copy operation in that it must replicate nodes from one workspace to another (the copy
     * operations supports replicating nodes within a workspace as well as across workspaces) and that it preserves UUIDs (the
     * copy operation always generates new UUIDs).
     * </p>
     * 
     * @param firstIdProperty the first identification property of the node that is to be copied
     * @param additionalIdProperties the remaining identification properties of the node that is to be copied
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Clone<Graph> clone( Property firstIdProperty,
                               Property... additionalIdProperties ) {
        return clone(Location.create(firstIdProperty, additionalIdProperties));
    }

    /**
     * Begin the request to copy the specified node into a parent node at a different location, which is specified via the
     * <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Node from ) {
        return copy(from.getLocation());
    }

    /**
     * Begin the request to copy a node at the specified location into a parent node at a different location, which is specified
     * via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the location of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Location from ) {
        return new CopyAction<Graph>(this, from) {
            @Override
            protected Graph submit( String fromWorkspaceName,
                                    Locations from,
                                    Location into,
                                    Name childName ) {
                String workspaceName = fromWorkspaceName != null ? fromWorkspaceName : getCurrentWorkspaceName();
                do {
                    requests.copyBranch(from.getLocation(),
                                        workspaceName,
                                        into,
                                        getCurrentWorkspaceName(),
                                        childName,
                                        NodeConflictBehavior.APPEND);
                } while ((from = from.next()) != null);
                return and();
            }
        };
    }

    /**
     * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param fromPath the path to the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( String fromPath ) {
        return copy(Location.create(createPath(fromPath)));
    }

    /**
     * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the path to the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Path from ) {
        return copy(Location.create(from));
    }

    /**
     * Begin the request to copy a node with the specified unique identifier into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the UUID of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( UUID from ) {
        return copy(Location.create(from));
    }

    /**
     * Begin the request to copy a node with the specified unique identification property into a parent node at a different
     * location, which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The identification
     * property should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param idProperty the unique identification property of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Property idProperty ) {
        return copy(Location.create(idProperty));
    }

    /**
     * Begin the request to copy a node with the specified identification properties into a parent node at a different location,
     * which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The identification properties
     * should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param firstIdProperty the first identification property of the node that is to be copied
     * @param additionalIdProperties the remaining identification properties of the node that is to be copied
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Property firstIdProperty,
                             Property... additionalIdProperties ) {
        return copy(Location.create(firstIdProperty, additionalIdProperties));
    }

    /**
     * Request to delete the specified node. This request is submitted to the repository immediately.
     * 
     * @param at the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Node at ) {
        requests.deleteBranch(at.getLocation(), getCurrentWorkspaceName());
        return nextGraph;
    }

    /**
     * Request to delete the node at the given location. This request is submitted to the repository immediately.
     * 
     * @param at the location of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Location at ) {
        requests.deleteBranch(at, getCurrentWorkspaceName());
        return nextGraph;
    }

    /**
     * Request to delete the node at the given path. This request is submitted to the repository immediately.
     * 
     * @param atPath the path of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( String atPath ) {
        return delete(Location.create(createPath(atPath)));
    }

    /**
     * Request to delete the node at the given path. This request is submitted to the repository immediately.
     * 
     * @param at the path of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Path at ) {
        return delete(Location.create(at));
    }

    /**
     * Request to delete the node with the given UUID. This request is submitted to the repository immediately.
     * 
     * @param at the UUID of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( UUID at ) {
        return delete(Location.create(at));
    }

    /**
     * Request to delete the node with the given unique identification property. This request is submitted to the repository
     * immediately.
     * 
     * @param idProperty the unique identifying property of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Property idProperty ) {
        return delete(Location.create(idProperty));
    }

    /**
     * Request to delete the node with the given identification properties. The identification properties should uniquely identify
     * a single node. This request is submitted to the repository immediately.
     * 
     * @param firstIdProperty the first identification property of the node that is to be copied
     * @param additionalIdProperties the remaining identification properties of the node that is to be copied
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Property firstIdProperty,
                                      Property... additionalIdProperties ) {
        return delete(Location.create(firstIdProperty, additionalIdProperties));
    }

    /**
     * Begin the request to create a node located at the supplied path, and return an interface used to either add properties for
     * the new node, or complete/submit the request and return the location, node, or graph.
     * <p>
     * If you have the {@link Location} of the parent (for the new node) from a previous request, it is better and more efficient
     * to use {@link #createUnder(Location)}. However, this method work just as well if all you have is the {@link Path} to the
     * parent or new node.
     * </p>
     * 
     * @param atPath the path to the node that is to be created.
     * @return an object that may be used to start another request
     */
    public CreateAt<Graph> createAt( String atPath ) {
        return createAt(createPath(atPath));
    }

    /**
     * Begin the request to create a node located at the supplied path, and return an interface used to either add properties for
     * the new node, or complete/submit the request and return the location, node, or graph.
     * <p>
     * If you have the {@link Location} of the parent (for the new node) from a previous request, it is better and more efficient
     * to use {@link #createUnder(Location)}. However, this method work just as well if all you have is the {@link Path} to the
     * parent or new node.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @return an object that may be used to start another request
     */
    public CreateAt<Graph> createAt( final Path at ) {
        CheckArg.isNotNull(at, "at");
        final Path parent = at.getParent();
        final Name childName = at.getLastSegment().getName();
        final String workspaceName = getCurrentWorkspaceName();
        return new CreateAt<Graph>() {
            private final List<Property> properties = new LinkedList<Property>();

            public CreateAt<Graph> and( UUID uuid ) {
                PropertyFactory factory = getContext().getPropertyFactory();
                properties.add(factory.create(DnaLexicon.UUID, uuid));
                return this;
            }

            public CreateAt<Graph> and( Property property ) {
                properties.add(property);
                return this;
            }

            public CreateAt<Graph> and( Iterable<Property> properties ) {
                for (Property property : properties) {
                    this.properties.add(property);
                }
                return this;
            }

            public CreateAt<Graph> and( String name,
                                        Object... values ) {
                ExecutionContext context = getContext();
                PropertyFactory factory = context.getPropertyFactory();
                NameFactory nameFactory = context.getValueFactories().getNameFactory();
                properties.add(factory.create(nameFactory.create(name), values));
                return this;
            }

            public CreateAt<Graph> and( Name name,
                                        Object... values ) {
                ExecutionContext context = getContext();
                PropertyFactory factory = context.getPropertyFactory();
                properties.add(factory.create(name, values));
                return this;
            }

            public CreateAt<Graph> and( Property property,
                                        Property... additionalProperties ) {
                properties.add(property);
                for (Property additionalProperty : additionalProperties) {
                    properties.add(additionalProperty);
                }
                return this;
            }

            public CreateAt<Graph> with( UUID uuid ) {
                return and(uuid);
            }

            public CreateAt<Graph> with( Property property ) {
                return and(property);
            }

            public CreateAt<Graph> with( Iterable<Property> properties ) {
                return and(properties);
            }

            public CreateAt<Graph> with( Property property,
                                         Property... additionalProperties ) {
                return and(property, additionalProperties);
            }

            public CreateAt<Graph> with( String name,
                                         Object... values ) {
                return and(name, values);
            }

            public CreateAt<Graph> with( Name name,
                                         Object... values ) {
                return and(name, values);
            }

            public Location getLocation() {
                Location parentLoc = Location.create(parent);
                CreateNodeRequest request = requests.createNode(parentLoc, workspaceName, childName, this.properties.iterator());
                return request.getActualLocationOfNode();
            }

            public Node getNode() {
                Location parentLoc = Location.create(parent);
                CreateNodeRequest request = requests.createNode(parentLoc, workspaceName, childName, this.properties.iterator());
                return getNodeAt(request.getActualLocationOfNode());
            }

            public Graph and() {
                requests.createNode(Location.create(parent), workspaceName, childName, this.properties.iterator());
                return Graph.this;
            }
        };
    }

    /**
     * Begin the request to create a node located at the supplied path.
     * <p>
     * Like all other methods on the {@link Graph}, the request will be performed when the no-argument {@link Create#and()} method
     * is called.
     * </p>
     * 
     * @param atPath the path to the node that is to be created.
     * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
     *         node where the node is to be created
     */
    public Create<Graph> create( String atPath ) {
        return create(createPath(atPath));
    }

    /**
     * Begin the request to create a node located at the supplied path.
     * <p>
     * Like all other methods on the {@link Graph}, the request will be performed when the no-argument {@link Create#and()} method
     * is called.
     * </p>
     * 
     * @param atPath the path to the node that is to be created.
     * @param property a property for the new node
     * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
     *         node where the node is to be created
     */
    public Create<Graph> create( String atPath,
                                 Property property ) {
        return create(createPath(atPath)).with(property);
    }

    /**
     * Begin the request to create a node located at the supplied path.
     * <p>
     * Like all other methods on the {@link Graph}, the request will be performed when the no-argument {@link Create#and()} method
     * is called.
     * </p>
     * 
     * @param atPath the path to the node that is to be created.
     * @param firstProperty a property for the new node
     * @param additionalProperties additional properties for the new node
     * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
     *         node where the node is to be created
     */
    public Create<Graph> create( String atPath,
                                 Property firstProperty,
                                 Property... additionalProperties ) {
        return create(createPath(atPath)).with(firstProperty, additionalProperties);
    }

    /**
     * Begin the request to create a node located at the supplied path.
     * <p>
     * Like all other methods on the {@link Graph}, the request will be performed when the no-argument {@link Create#and()} method
     * is called.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
     *         node where the node is to be created
     */
    public final Create<Graph> create( Path at ) {
        CheckArg.isNotNull(at, "at");
        Path parent = at.getParent();
        Name name = at.getLastSegment().getName();
        return create(Location.create(parent), name);
    }

    protected final CreateAction<Graph> create( Location parent,
                                                Name child ) {
        return new CreateAction<Graph>(this, parent, getCurrentWorkspaceName(), child) {
            @Override
            protected Graph submit( Location parent,
                                    String workspaceName,
                                    Name childName,
                                    Collection<Property> properties,
                                    NodeConflictBehavior behavior ) {
                requests.createNode(parent, workspaceName, childName, properties.iterator(), behavior);
                return Graph.this;
            }

        };
    }

    /**
     * Begin the request to create a node located at the supplied path.
     * <p>
     * Like all other methods on the {@link Graph}, the request will be performed when the no-argument {@link Create#and()} method
     * is called.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @param properties the iterator over the properties for the new node
     * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
     *         node where the node is to be created
     */
    public Create<Graph> create( Path at,
                                 Iterable<Property> properties ) {
        Create<Graph> action = create(at);
        for (Property property : properties) {
            action.and(property);
        }
        return action;
    }

    /**
     * Begin the request to create a node located at the supplied path.
     * <p>
     * Like all other methods on the {@link Graph}, the request will be performed when the no-argument {@link Create#and()} method
     * is called.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @param property a property for the new node
     * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
     *         node where the node is to be created
     */
    public Create<Graph> create( Path at,
                                 Property property ) {
        return create(at).with(property);
    }

    /**
     * Begin the request to create a node located at the supplied path.
     * <p>
     * Like all other methods on the {@link Graph}, the request will be performed when the no-argument {@link Create#and()} method
     * is called.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @param firstProperty a property for the new node
     * @param additionalProperties additional properties for the new node
     * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
     *         node where the node is to be created
     */
    public Create<Graph> create( Path at,
                                 Property firstProperty,
                                 Property... additionalProperties ) {
        return create(at).with(firstProperty, additionalProperties);
    }

    /**
     * Begin the request to create a node under the existing parent node at the supplied location. Use this method if you are
     * creating a node when you have the {@link Location} of a parent from a previous request.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>node(...)</code>
     * method is called on the returned object
     * </p>
     * 
     * @param parent the location of the parent
     * @return the object used to start creating a node
     */
    public CreateNode<Conjunction<Graph>> createUnder( final Location parent ) {
        final NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
        CheckArg.isNotNull(parent, "parent");
        return new CreateNode<Conjunction<Graph>>() {
            public Conjunction<Graph> node( String name,
                                            Property... properties ) {
                Name child = nameFactory.create(name);
                requests.createNode(parent, getCurrentWorkspaceName(), child, properties);
                return nextGraph;
            }

            public Conjunction<Graph> node( String name,
                                            Iterator<Property> properties ) {
                Name child = nameFactory.create(name);
                requests.createNode(parent, getCurrentWorkspaceName(), child, properties);
                return nextGraph;
            }

            public Conjunction<Graph> node( String name,
                                            Iterable<Property> properties ) {
                Name child = nameFactory.create(name);
                requests.createNode(parent, getCurrentWorkspaceName(), child, properties.iterator());
                return nextGraph;
            }
        };
    }

    /**
     * Set the properties on a node.
     * 
     * @param properties the properties to set
     * @return the remove request object that should be used to specify the node on which the properties are to be set.
     */
    public On<Conjunction<Graph>> set( final Property... properties ) {
        return new On<Conjunction<Graph>>() {
            public Conjunction<Graph> on( Location location ) {
                requests.setProperties(location, getCurrentWorkspaceName(), properties);
                return nextGraph;
            }

            public Conjunction<Graph> on( String path ) {
                return on(Location.create(createPath(path)));
            }

            public Conjunction<Graph> on( Path path ) {
                return on(Location.create(path));
            }

            public Conjunction<Graph> on( Property idProperty ) {
                return on(Location.create(idProperty));
            }

            public Conjunction<Graph> on( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
                return on(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> on( Iterable<Property> idProperties ) {
                return on(Location.create(idProperties));
            }

            public Conjunction<Graph> on( UUID uuid ) {
                return on(Location.create(uuid));
            }
        };
    }

    /**
     * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
     * value(s) and the location of the node onto which the property should be set.
     * 
     * @param propertyName the property name
     * @return the interface used to specify the values
     */
    public SetValues<Conjunction<Graph>> set( String propertyName ) {
        Name name = getContext().getValueFactories().getNameFactory().create(propertyName);
        return set(name);
    }

    /**
     * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
     * value(s) and the location of the node onto which the property should be set.
     * 
     * @param propertyName the property name
     * @return the interface used to specify the values
     */
    public SetValues<Conjunction<Graph>> set( final Name propertyName ) {
        return new SetValues<Conjunction<Graph>>() {
            public SetValuesTo<Conjunction<Graph>> on( final Location location ) {
                return new SetValuesTo<Conjunction<Graph>>() {
                    public Conjunction<Graph> to( Node value ) {
                        Reference ref = (Reference)convertReferenceValue(value);
                        Property property = getContext().getPropertyFactory().create(propertyName, ref);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Location value ) {
                        Reference ref = (Reference)convertReferenceValue(value);
                        Property property = getContext().getPropertyFactory().create(propertyName, ref);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }

                    protected Conjunction<Graph> toValue( Object value ) {
                        Property property = getContext().getPropertyFactory().create(propertyName, value);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( String value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( int value ) {
                        return toValue(Integer.valueOf(value));
                    }

                    public Conjunction<Graph> to( long value ) {
                        return toValue(Long.valueOf(value));
                    }

                    public Conjunction<Graph> to( boolean value ) {
                        return toValue(Boolean.valueOf(value));
                    }

                    public Conjunction<Graph> to( float value ) {
                        return toValue(Float.valueOf(value));
                    }

                    public Conjunction<Graph> to( double value ) {
                        return toValue(Double.valueOf(value));
                    }

                    public Conjunction<Graph> to( BigDecimal value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Calendar value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Date value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( DateTime value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Name value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Path value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Reference value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( URI value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( UUID value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Binary value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( byte[] value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( InputStream stream,
                                                  long approximateLength ) {
                        Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Reader reader,
                                                  long approximateLength ) {
                        Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Object value ) {
                        value = convertReferenceValue(value);
                        Property property = getContext().getPropertyFactory().create(propertyName, value);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Object firstValue,
                                                  Object... otherValues ) {
                        firstValue = convertReferenceValue(firstValue);
                        for (int i = 0, len = otherValues.length; i != len; ++i) {
                            otherValues[i] = convertReferenceValue(otherValues[i]);
                        }
                        Property property = getContext().getPropertyFactory().create(propertyName, firstValue, otherValues);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Object[] values ) {
                        for (int i = 0, len = values.length; i != len; ++i) {
                            values[i] = convertReferenceValue(values[i]);
                        }
                        Property property = getContext().getPropertyFactory().create(propertyName, values);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Iterable<?> values ) {
                        List<Object> valueList = new LinkedList<Object>();
                        for (Object value : values) {
                            value = convertReferenceValue(value);
                            valueList.add(value);
                        }
                        Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Iterator<?> values ) {
                        List<Object> valueList = new LinkedList<Object>();
                        while (values.hasNext()) {
                            Object value = values.next();
                            valueList.add(value);
                        }
                        Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                        requests.setProperty(location, getCurrentWorkspaceName(), property);
                        return nextGraph;
                    }
                };
            }

            public SetValuesTo<Conjunction<Graph>> on( String path ) {
                return on(Location.create(createPath(path)));
            }

            public SetValuesTo<Conjunction<Graph>> on( Path path ) {
                return on(Location.create(path));
            }

            public SetValuesTo<Conjunction<Graph>> on( Property idProperty ) {
                return on(Location.create(idProperty));
            }

            public SetValuesTo<Conjunction<Graph>> on( Property firstIdProperty,
                                                       Property... additionalIdProperties ) {
                return on(Location.create(firstIdProperty, additionalIdProperties));
            }

            public SetValuesTo<Conjunction<Graph>> on( Iterable<Property> idProperties ) {
                return on(Location.create(idProperties));
            }

            public SetValuesTo<Conjunction<Graph>> on( UUID uuid ) {
                return on(Location.create(uuid));
            }

            public On<Conjunction<Graph>> to( Node node ) {
                Reference value = (Reference)convertReferenceValue(node);
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            public On<Conjunction<Graph>> to( Location location ) {
                Reference value = (Reference)convertReferenceValue(location);
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            protected On<Conjunction<Graph>> toValue( Object value ) {
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            public On<Conjunction<Graph>> to( String value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( int value ) {
                return toValue(Integer.valueOf(value));
            }

            public On<Conjunction<Graph>> to( long value ) {
                return toValue(Long.valueOf(value));
            }

            public On<Conjunction<Graph>> to( boolean value ) {
                return toValue(Boolean.valueOf(value));
            }

            public On<Conjunction<Graph>> to( float value ) {
                return toValue(Float.valueOf(value));
            }

            public On<Conjunction<Graph>> to( double value ) {
                return toValue(Double.valueOf(value));
            }

            public On<Conjunction<Graph>> to( BigDecimal value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Calendar value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Date value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( DateTime value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Name value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Path value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Reference value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( URI value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( UUID value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Binary value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( byte[] value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( InputStream stream,
                                              long approximateLength ) {
                Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Reader reader,
                                              long approximateLength ) {
                Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Object value ) {
                value = convertReferenceValue(value);
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            public On<Conjunction<Graph>> to( Object firstValue,
                                              Object... otherValues ) {
                firstValue = convertReferenceValue(firstValue);
                for (int i = 0, len = otherValues.length; i != len; ++i) {
                    otherValues[i] = convertReferenceValue(otherValues[i]);
                }
                return set(getContext().getPropertyFactory().create(propertyName, firstValue, otherValues));
            }

            public On<Conjunction<Graph>> to( Object[] values ) {
                for (int i = 0, len = values.length; i != len; ++i) {
                    values[i] = convertReferenceValue(values[i]);
                }
                return set(getContext().getPropertyFactory().create(propertyName, values));
            }

            public On<Conjunction<Graph>> to( Iterable<?> values ) {
                List<Object> valueList = new LinkedList<Object>();
                for (Object value : values) {
                    value = convertReferenceValue(value);
                    valueList.add(value);
                }
                return set(getContext().getPropertyFactory().create(propertyName, valueList));
            }

            public On<Conjunction<Graph>> to( Iterator<?> values ) {
                List<Object> valueList = new LinkedList<Object>();
                while (values.hasNext()) {
                    Object value = values.next();
                    valueList.add(value);
                }
                return set(getContext().getPropertyFactory().create(propertyName, valueList));
            }
        };
    }

    /**
     * Remove properties from the node at the given location.
     * 
     * @param propertyNames the names of the properties to be removed
     * @return the remove request object that should be used to specify the node from which the properties are to be removed.
     */
    public On<Conjunction<Graph>> remove( final Name... propertyNames ) {
        return new On<Conjunction<Graph>>() {
            public Conjunction<Graph> on( Location location ) {
                requests.removeProperties(location, getCurrentWorkspaceName(), propertyNames);
                return nextGraph;
            }

            public Conjunction<Graph> on( String path ) {
                return on(Location.create(createPath(path)));
            }

            public Conjunction<Graph> on( Path path ) {
                return on(Location.create(path));
            }

            public Conjunction<Graph> on( Property idProperty ) {
                return on(Location.create(idProperty));
            }

            public Conjunction<Graph> on( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
                return on(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> on( Iterable<Property> idProperties ) {
                return on(Location.create(idProperties));
            }

            public Conjunction<Graph> on( UUID uuid ) {
                return on(Location.create(uuid));
            }
        };
    }

    /**
     * Remove properties from the node at the given location.
     * 
     * @param propertyNames the names of the properties to be removed
     * @return the remove request object that should be used to specify the node from which the properties are to be removed.
     */
    public On<Conjunction<Graph>> remove( final String... propertyNames ) {
        NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
        int number = propertyNames.length;
        final Name[] names = new Name[number];
        for (int i = 0; i != number; ++i) {
            names[i] = nameFactory.create(propertyNames[i]);
        }
        return new On<Conjunction<Graph>>() {
            public Conjunction<Graph> on( Location location ) {
                requests.removeProperties(location, getCurrentWorkspaceName(), names);
                return nextGraph;
            }

            public Conjunction<Graph> on( String path ) {
                return on(Location.create(createPath(path)));
            }

            public Conjunction<Graph> on( Path path ) {
                return on(Location.create(path));
            }

            public Conjunction<Graph> on( Property idProperty ) {
                return on(Location.create(idProperty));
            }

            public Conjunction<Graph> on( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
                return on(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> on( Iterable<Property> idProperties ) {
                return on(Location.create(idProperties));
            }

            public Conjunction<Graph> on( UUID uuid ) {
                return on(Location.create(uuid));
            }
        };
    }

    /**
     * Request that the properties be read on the node defined via the <code>on(...)</code> method on the returned {@link On}
     * object. Once the location is specified, the {@link Collection collection of properties} are read and then returned.
     * 
     * @return the object that is used to specified the node whose properties are to be read, and which will return the properties
     */
    public On<Collection<Property>> getProperties() {
        return new On<Collection<Property>>() {
            public Collection<Property> on( Location location ) {
                return requests.readAllProperties(location, getCurrentWorkspaceName()).getProperties();
            }

            public Collection<Property> on( String path ) {
                return on(Location.create(createPath(path)));
            }

            public Collection<Property> on( Path path ) {
                return on(Location.create(path));
            }

            public Collection<Property> on( Property idProperty ) {
                return on(Location.create(idProperty));
            }

            public Collection<Property> on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                return on(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Collection<Property> on( Iterable<Property> idProperties ) {
                return on(Location.create(idProperties));
            }

            public Collection<Property> on( UUID uuid ) {
                return on(Location.create(uuid));
            }
        };
    }

    /**
     * Request that the properties be read on the node defined via the <code>on(...)</code> method on the returned {@link On}
     * object. Once the location is specified, the {@link Map map of properties} are read and then returned.
     * 
     * @return the object that is used to specified the node whose properties are to be read, and which will return the properties
     *         as a map keyed by their name
     */
    public On<Map<Name, Property>> getPropertiesByName() {
        return new On<Map<Name, Property>>() {
            public Map<Name, Property> on( Location location ) {
                return requests.readAllProperties(location, getCurrentWorkspaceName()).getPropertiesByName();
            }

            public Map<Name, Property> on( String path ) {
                return on(Location.create(createPath(path)));
            }

            public Map<Name, Property> on( Path path ) {
                return on(Location.create(path));
            }

            public Map<Name, Property> on( Property idProperty ) {
                return on(Location.create(idProperty));
            }

            public Map<Name, Property> on( Property firstIdProperty,
                                           Property... additionalIdProperties ) {
                return on(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Map<Name, Property> on( Iterable<Property> idProperties ) {
                return on(Location.create(idProperties));
            }

            public Map<Name, Property> on( UUID uuid ) {
                return on(Location.create(uuid));
            }
        };
    }

    /**
     * Request that the children be read on the node defined via the <code>of(...)</code> method on the returned {@link Of}
     * object. The returned object is used to supply the remaining information, including either the {@link Children#of(Location)
     * location of the parent}, or that a subset of the children should be retrieved {@link Children#inBlockOf(int) in a block}.
     * 
     * @return the object that is used to specify the remaining inputs for the request, and which will return the children
     */
    public Children<List<Location>> getChildren() {
        return new Children<List<Location>>() {
            public List<Location> of( String path ) {
                return of(Location.create(createPath(path)));
            }

            public List<Location> of( Path path ) {
                return of(Location.create(path));
            }

            public List<Location> of( Property idProperty ) {
                return of(Location.create(idProperty));
            }

            public List<Location> of( Property firstIdProperty,
                                      Property... additionalIdProperties ) {
                return of(Location.create(firstIdProperty, additionalIdProperties));
            }

            public List<Location> of( Iterable<Property> idProperties ) {
                return of(Location.create(idProperties));
            }

            public List<Location> of( UUID uuid ) {
                return of(Location.create(uuid));
            }

            public List<Location> of( Location at ) {
                return requests.readAllChildren(at, getCurrentWorkspaceName()).getChildren();
            }

            public BlockOfChildren<List<Location>> inBlockOf( final int blockSize ) {
                return new BlockOfChildren<List<Location>>() {
                    public Under<List<Location>> startingAt( final int startingIndex ) {
                        return new Under<List<Location>>() {
                            public List<Location> under( String path ) {
                                return under(Location.create(createPath(path)));
                            }

                            public List<Location> under( Path path ) {
                                return under(Location.create(path));
                            }

                            public List<Location> under( Property idProperty ) {
                                return under(Location.create(idProperty));
                            }

                            public List<Location> under( Property firstIdProperty,
                                                         Property... additionalIdProperties ) {
                                return under(Location.create(firstIdProperty, additionalIdProperties));
                            }

                            public List<Location> under( UUID uuid ) {
                                return under(Location.create(uuid));
                            }

                            public List<Location> under( Location at ) {
                                return requests.readBlockOfChildren(at, getCurrentWorkspaceName(), startingIndex, blockSize)
                                               .getChildren();
                            }
                        };
                    }

                    public List<Location> startingAfter( final Location previousSibling ) {
                        return requests.readNextBlockOfChildren(previousSibling, getCurrentWorkspaceName(), blockSize)
                                       .getChildren();
                    }

                    public List<Location> startingAfter( String pathOfPreviousSibling ) {
                        return startingAfter(Location.create(createPath(pathOfPreviousSibling)));
                    }

                    public List<Location> startingAfter( Path pathOfPreviousSibling ) {
                        return startingAfter(Location.create(pathOfPreviousSibling));
                    }

                    public List<Location> startingAfter( UUID uuidOfPreviousSibling ) {
                        return startingAfter(Location.create(uuidOfPreviousSibling));
                    }

                    public List<Location> startingAfter( Property idPropertyOfPreviousSibling ) {
                        return startingAfter(Location.create(idPropertyOfPreviousSibling));
                    }

                    public List<Location> startingAfter( Property firstIdProperyOfPreviousSibling,
                                                         Property... additionalIdPropertiesOfPreviousSibling ) {
                        return startingAfter(Location.create(firstIdProperyOfPreviousSibling,
                                                             additionalIdPropertiesOfPreviousSibling));
                    }
                };
            }
        };
    }

    /**
     * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
     * returned {@link On} object. Once the location is specified, the {@link Property property} is read and then returned.
     * 
     * @param name the name of the property that is to be read
     * @return the object that is used to specified the node whose property is to be read, and which will return the property
     */
    public On<Property> getProperty( final String name ) {
        Name nameObj = context.getValueFactories().getNameFactory().create(name);
        return getProperty(nameObj);
    }

    /**
     * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
     * returned {@link On} object. Once the location is specified, the {@link Property property} is read and then returned.
     * 
     * @param name the name of the property that is to be read
     * @return the object that is used to specified the node whose property is to be read, and which will return the property
     */
    public On<Property> getProperty( final Name name ) {
        return new On<Property>() {
            public Property on( String path ) {
                return on(Location.create(createPath(path)));
            }

            public Property on( Path path ) {
                return on(Location.create(path));
            }

            public Property on( Property idProperty ) {
                return on(Location.create(idProperty));
            }

            public Property on( Property firstIdProperty,
                                Property... additionalIdProperties ) {
                return on(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Property on( Iterable<Property> idProperties ) {
                return on(Location.create(idProperties));
            }

            public Property on( UUID uuid ) {
                return on(Location.create(uuid));
            }

            public Property on( Location at ) {
                return requests.readProperty(at, getCurrentWorkspaceName(), name).getProperty();
            }
        };
    }

    /**
     * Request to read the node with the supplied UUID.
     * 
     * @param uuid the UUID of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( UUID uuid ) {
        return getNodeAt(Location.create(uuid));
    }

    /**
     * Request to read the node at the supplied location.
     * 
     * @param location the location of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Location location ) {
        return new GraphNode(requests.readNode(location, getCurrentWorkspaceName()));
    }

    /**
     * Request to read the node at the supplied path.
     * 
     * @param path the path of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( String path ) {
        return getNodeAt(Location.create(createPath(path)));
    }

    /**
     * Request to read the node at the supplied path.
     * 
     * @param path the path of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Path path ) {
        return getNodeAt(Location.create(path));
    }

    /**
     * Request to read the node with the supplied unique identifier property.
     * 
     * @param idProperty the identification property that is unique to the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Property idProperty ) {
        return getNodeAt(Location.create(idProperty));
    }

    /**
     * Request to read the node with the supplied unique identifier properties.
     * 
     * @param firstIdProperty the first of the identification properties that uniquely identify the node that is to be read
     * @param additionalIdProperties the remaining identification properties that uniquely identify the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Property firstIdProperty,
                           Property... additionalIdProperties ) {
        return getNodeAt(Location.create(firstIdProperty, additionalIdProperties));
    }

    /**
     * Request to read the node with the supplied unique identifier properties.
     * 
     * @param idProperties the identification properties that uniquely identify the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Iterable<Property> idProperties ) {
        return getNodeAt(Location.create(idProperties));
    }

    /**
     * Request to read the node given by the supplied reference value.
     * 
     * @param reference the reference property value that is to be resolved into a node
     * @return the node that is read from the repository
     * @throws ValueFormatException if the supplied reference could not be converted to an identifier property value
     */
    public Node resolve( Reference reference ) {
        CheckArg.isNotNull(reference, "reference");
        UUID uuid = context.getValueFactories().getUuidFactory().create(reference);
        return getNodeAt(uuid);
    }

    /**
     * Request to read a subgraph of the specified depth, rooted at a location that will be specified via <code>at(...)</code> in
     * the resulting {@link At} object. All properties and children of every node in the subgraph will be read and returned in the
     * {@link Subgraph} object returned from the <code>at(...)</code> methods.
     * 
     * @param depth the maximum depth of the subgraph that should be read
     * @return the component that should be used to specify the location of the node that is the top of the subgraph, and which
     *         will return the {@link Subgraph} containing the results
     */
    public At<Subgraph> getSubgraphOfDepth( final int depth ) {
        return new At<Subgraph>() {
            public Subgraph at( Location location ) {
                return new SubgraphResults(requests.readBranch(location, getCurrentWorkspaceName(), depth));
            }

            public Subgraph at( String path ) {
                return at(Location.create(createPath(path)));
            }

            public Subgraph at( Path path ) {
                return at(Location.create(path));
            }

            public Subgraph at( UUID uuid ) {
                return at(Location.create(uuid));
            }

            public Subgraph at( Property idProperty ) {
                return at(Location.create(idProperty));
            }

            public Subgraph at( Property firstIdProperty,
                                Property... additionalIdProperties ) {
                return at(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Subgraph at( Iterable<Property> idProperties ) {
                return at(Location.create(idProperties));
            }
        };
    }

    /**
     * Import the content from the provided stream of XML data, specifying via the returned {@link ImportInto object} where the
     * content is to be imported.
     * 
     * @param stream the open stream of XML data that the importer can read the content that is to be imported
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>stream</code> or destination path are null
     */
    public ImportInto<Conjunction<Graph>> importXmlFrom( final InputStream stream ) {
        CheckArg.isNotNull(stream, "stream");

        return new ImportInto<Conjunction<Graph>>() {
            private boolean skipRootElement = false;

            public ImportInto<Conjunction<Graph>> skippingRootElement( boolean skipRootElement ) {
                this.skipRootElement = skipRootElement;
                return this;
            }

            public Conjunction<Graph> into( String path ) throws IOException, SAXException {
                return into(Location.create(createPath(path)));
            }

            public Conjunction<Graph> into( Path path ) throws IOException, SAXException {
                return into(Location.create(path));
            }

            public Conjunction<Graph> into( Property idProperty ) throws IOException, SAXException {
                return into(Location.create(idProperty));
            }

            public Conjunction<Graph> into( Property firstIdProperty,
                                            Property... additionalIdProperties ) throws IOException, SAXException {
                return into(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> into( Iterable<Property> idProperties ) throws IOException, SAXException {
                return into(Location.create(idProperties));
            }

            public Conjunction<Graph> into( UUID uuid ) throws IOException, SAXException {
                return into(Location.create(uuid));
            }

            public Conjunction<Graph> into( Location at ) throws IOException, SAXException {
                GraphImporter importer = new GraphImporter(Graph.this);
                importer.importXml(stream, at, skipRootElement).execute(); // 'importXml' creates and uses a new batch
                return Graph.this.nextGraph;
            }
        };
    }

    /**
     * Import the content from the XML file at the supplied URI, specifying via the returned {@link ImportInto object} where the
     * content is to be imported.
     * 
     * @param uri the URI where the importer can read the content that is to be imported
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportInto<Conjunction<Graph>> importXmlFrom( final URI uri ) {
        return new ImportInto<Conjunction<Graph>>() {
            private boolean skipRootElement = false;

            public ImportInto<Conjunction<Graph>> skippingRootElement( boolean skipRootElement ) {
                this.skipRootElement = skipRootElement;
                return this;
            }

            public Conjunction<Graph> into( String path ) throws IOException, SAXException {
                return into(Location.create(createPath(path)));
            }

            public Conjunction<Graph> into( Path path ) throws IOException, SAXException {
                return into(Location.create(path));
            }

            public Conjunction<Graph> into( Property idProperty ) throws IOException, SAXException {
                return into(Location.create(idProperty));
            }

            public Conjunction<Graph> into( Property firstIdProperty,
                                            Property... additionalIdProperties ) throws IOException, SAXException {
                return into(Location.create(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> into( Iterable<Property> idProperties ) throws IOException, SAXException {
                return into(Location.create(idProperties));
            }

            public Conjunction<Graph> into( UUID uuid ) throws IOException, SAXException {
                return into(Location.create(uuid));
            }

            public Conjunction<Graph> into( Location at ) throws IOException, SAXException {
                GraphImporter importer = new GraphImporter(Graph.this);
                importer.importXml(uri, at, skipRootElement).execute(); // 'importXml' creates and uses a new batch
                return Graph.this.nextGraph;
            }
        };
    }

    /**
     * Import the content from the XML file at the supplied file location, specifying via the returned {@link ImportInto object}
     * where the content is to be imported.
     * 
     * @param pathToFile the path to the XML file that should be imported.
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportInto<Conjunction<Graph>> importXmlFrom( String pathToFile ) {
        CheckArg.isNotNull(pathToFile, "pathToFile");
        return importXmlFrom(new File(pathToFile).toURI());
    }

    /**
     * Import the content from the XML file at the supplied file, specifying via the returned {@link ImportInto object} where the
     * content is to be imported.
     * 
     * @param file the XML file that should be imported.
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportInto<Conjunction<Graph>> importXmlFrom( File file ) {
        CheckArg.isNotNull(file, "file");
        return importXmlFrom(file.toURI());
    }

    protected Path createPath( String path ) {
        return getContext().getValueFactories().getPathFactory().create(path);
    }

    protected List<Segment> getSegments( List<Location> locations ) {
        List<Segment> segments = new ArrayList<Segment>(locations.size());
        for (Location location : locations) {
            segments.add(location.getPath().getLastSegment());
        }
        return segments;
    }

    /**
     * Begin a batch of requests to perform various operations. Use this approach when multiple operations are to be built and
     * then executed with one submission to the underlying {@link #getSourceName() repository source}. The {@link Results results}
     * are not available until the {@link Batch#execute()} method is invoked.
     * 
     * @return the batch object used to build and accumulate multiple requests and to submit them all for processing at once.
     * @see Batch#execute()
     * @see Results
     */
    public Batch batch() {
        return new Batch(new BatchRequestBuilder());
    }

    /**
     * Begin a batch of requests to perform various operations, but specify the queue where all accumulated requests should be
     * placed. Use this approach when multiple operations are to be built and then executed with one submission to the underlying
     * {@link #getSourceName() repository source}. The {@link Results results} are not available until the {@link Batch#execute()}
     * method is invoked.
     * 
     * @param builder the request builder that should be used; may not be null
     * @return the batch object used to build and accumulate multiple requests and to submit them all for processing at once.
     * @see Batch#execute()
     * @see Results
     */
    public Batch batch( BatchRequestBuilder builder ) {
        CheckArg.isNotNull(builder, "builder");
        return new Batch(builder);
    }

    /**
     * Interface for creating multiple requests to perform various operations. Note that all the requests are accumulated until
     * the {@link #execute()} method is called. The results of all the operations are then available in the {@link Results} object
     * returned by the {@link #execute()}.
     * 
     * @author Randall Hauch
     */
    @Immutable
    public final class Batch implements Executable<Node> {
        protected final BatchRequestBuilder requestQueue;
        protected final BatchConjunction nextRequests;
        protected final String workspaceName;
        protected boolean executed = false;

        /*package*/Batch( BatchRequestBuilder builder ) {
            assert builder != null;
            this.requestQueue = builder;
            this.workspaceName = Graph.this.getCurrentWorkspaceName();
            this.nextRequests = new BatchConjunction() {
                public Batch and() {
                    return Batch.this;
                }

                public Results execute() {
                    return Batch.this.execute();
                }
            };
        }

        /**
         * Return whether this batch has been {@link #execute() executed}.
         * 
         * @return true if this batch has already been executed, or false otherwise
         */
        public boolean hasExecuted() {
            return executed;
        }

        /**
         * Determine whether this batch needs to be executed (there are requests and the batch has not been executed yet).
         * 
         * @return true if there are some requests in this batch that need to be executed, or false execution is not required
         */
        public boolean isExecuteRequired() {
            return !executed && requestQueue.hasRequests();
        }

        /**
         * Obtain the graph that this batch uses.
         * 
         * @return the graph; never null
         */
        public Graph getGraph() {
            return Graph.this;
        }

        /**
         * Get the name of the workspace that this batch is using. This is always constant throughout the lifetime of the batch.
         * 
         * @return the name of the workspace; never null
         */
        public String getCurrentWorkspaceName() {
            return this.workspaceName;
        }

        protected final void assertNotExecuted() {
            if (executed) {
                throw new IllegalStateException(GraphI18n.unableToAddMoreRequestsToAlreadyExecutedBatch.text());
            }
        }

        /**
         * Begin the request to move the specified node into a parent node at a different location, which is specified via the
         * <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Node from ) {
            return move(from.getLocation());
        }

        /**
         * Begin the request to move a node at the specified location into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the location of the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public final Move<BatchConjunction> move( Location from ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, from) {
                @Override
                protected BatchConjunction submit( Locations from,
                                                   Location into,
                                                   Location before,
                                                   Name newName ) {
                    String workspaceName = getCurrentWorkspaceName();
                    do {
                        requestQueue.moveBranch(from.getLocation(), into, before, workspaceName, newName);
                    } while ((from = from.next()) != null);
                    return and();
                }
            };
        }

        /**
         * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param fromPath the path to the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( String fromPath ) {
            return move(Location.create(createPath(fromPath)));
        }

        /**
         * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the path to the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Path from ) {
            return move(Location.create(from));
        }

        /**
         * Begin the request to move a node with the specified unique identifier into a parent node at a different location, which
         * is specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the UUID of the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( UUID from ) {
            return move(Location.create(from));
        }

        /**
         * Begin the request to move a node with the specified unique identification property into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The
         * identification property should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the unique identification property of the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Property idProperty ) {
            return move(Location.create(idProperty));
        }

        /**
         * Begin the request to move a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first identification property of the node that is to be moved
         * @param additionalIdProperties the remaining identification properties of the node that is to be moved
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
            return move(Location.create(firstIdProperty, additionalIdProperties));
        }

        /**
         * Begin the request to move a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperties the identification properties of the node that is to be moved
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Iterable<Property> idProperties ) {
            return move(Location.create(idProperties));
        }

        /**
         * Begin the request to clone the specified node into a parent node at a different location, which is specified via the
         * <code>into(...)</code> method on the returned {@link Clone} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( Node from ) {
            return clone(from.getLocation());
        }

        /**
         * Begin the request to clone a node at the specified location into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Clone} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the location of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( Location from ) {
            assertNotExecuted();
            return new CloneAction<BatchConjunction>(this.nextRequests, from) {
                @Override
                protected BatchConjunction submit( String fromWorkspaceName,
                                                   Location from,
                                                   String intoWorkspaceName,
                                                   Location into,
                                                   Name desiredName,
                                                   Segment desiredSegment,
                                                   boolean removeExisting ) {
                    requests.cloneBranch(from,
                                         fromWorkspaceName,
                                         into,
                                         intoWorkspaceName,
                                         desiredName,
                                         desiredSegment,
                                         removeExisting);
                    return and();
                }
            };
        }

        /**
         * Begin the request to clone a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Clone} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param fromPath the path to the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( String fromPath ) {
            return clone(Location.create(createPath(fromPath)));
        }

        /**
         * Begin the request to clone a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Clone} object.
         * <p>
         * Like all other methods on the {@link Graph}, the clone request will be performed immediately when the
         * <code>into(...)</code> method is called.
         * </p>
         * 
         * @param from the path to the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( Path from ) {
            return clone(Location.create(from));
        }

        /**
         * Begin the request to clone a node with the specified unique identifier into a parent node at a different location,
         * which is specified via the <code>into(...)</code> method on the returned {@link Clone} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the UUID of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( UUID from ) {
            return clone(Location.create(from));
        }

        /**
         * Begin the request to clone a node with the specified unique identification property into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Clone} object. The
         * identification property should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the unique identification property of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( Property idProperty ) {
            return clone(Location.create(idProperty));
        }

        /**
         * Begin the request to clone a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Clone} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first identification property of the node that is to be copied
         * @param additionalIdProperties the remaining identification properties of the node that is to be copied
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( Property firstIdProperty,
                                              Property... additionalIdProperties ) {
            return clone(Location.create(firstIdProperty, additionalIdProperties));
        }

        /**
         * Begin the request to clone a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Clone} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperties the identification properties of the node that is to be copied
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Clone<BatchConjunction> clone( Iterable<Property> idProperties ) {
            return clone(Location.create(idProperties));
        }

        /**
         * Begin the request to copy the specified node into a parent node at a different location, which is specified via the
         * <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Node from ) {
            return copy(from.getLocation());
        }

        /**
         * Begin the request to copy a node at the specified location into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the location of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Location from ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(this.nextRequests, from) {
                @Override
                protected BatchConjunction submit( String fromWorkspaceName,
                                                   Locations from,
                                                   Location into,
                                                   Name copyName ) {
                    String workspaceName = fromWorkspaceName != null ? fromWorkspaceName : getCurrentWorkspaceName();
                    do {
                        requestQueue.copyBranch(from.getLocation(), workspaceName, into, workspaceName, copyName, null);
                    } while ((from = from.next()) != null);
                    return and();
                }
            };
        }

        /**
         * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param fromPath the path to the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( String fromPath ) {
            return copy(Location.create(createPath(fromPath)));
        }

        /**
         * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the
         * <code>into(...)</code> method is called.
         * </p>
         * 
         * @param from the path to the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Path from ) {
            return copy(Location.create(from));
        }

        /**
         * Begin the request to copy a node with the specified unique identifier into a parent node at a different location, which
         * is specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the UUID of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( UUID from ) {
            return copy(Location.create(from));
        }

        /**
         * Begin the request to copy a node with the specified unique identification property into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The
         * identification property should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the unique identification property of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Property idProperty ) {
            return copy(Location.create(idProperty));
        }

        /**
         * Begin the request to copy a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first identification property of the node that is to be copied
         * @param additionalIdProperties the remaining identification properties of the node that is to be copied
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
            return copy(Location.create(firstIdProperty, additionalIdProperties));
        }

        /**
         * Begin the request to copy a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperties the identification properties of the node that is to be copied
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Iterable<Property> idProperties ) {
            return copy(Location.create(idProperties));
        }

        /**
         * Request to delete the specified node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Node at ) {
            return delete(at.getLocation());
        }

        /**
         * Request to delete the node at the given location.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the location of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Location at ) {
            assertNotExecuted();
            this.requestQueue.deleteBranch(at, getCurrentWorkspaceName());
            return nextRequests;
        }

        /**
         * Request to delete the node at the given path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( String atPath ) {
            return delete(Location.create(createPath(atPath)));
        }

        /**
         * Request to delete the node at the given path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Path at ) {
            return delete(Location.create(at));
        }

        /**
         * Request to delete the node with the given UUID.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the UUID of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( UUID at ) {
            return delete(Location.create(at));
        }

        /**
         * Request to delete the node with the given unique identification property.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the unique identifying property of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Property idProperty ) {
            return delete(Location.create(idProperty));
        }

        /**
         * Request to delete the node with the given identification properties. The identification properties should uniquely
         * identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first identification property of the node that is to be copied
         * @param additionalIdProperties the remaining identification properties of the node that is to be copied
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Property firstIdProperty,
                                        Property... additionalIdProperties ) {
            return delete(Location.create(firstIdProperty, additionalIdProperties));
        }

        /**
         * Request to delete the node with the given identification properties. The identification properties should uniquely
         * identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperties the identification property of the node that is to be copied
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Iterable<Property> idProperties ) {
            return delete(Location.create(idProperties));
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path to the node that is to be created.
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( String atPath ) {
            return create(createPath(atPath));
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path to the node that is to be created.
         * @param property a property for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( String atPath,
                                     Property property ) {
            return create(createPath(atPath)).with(property);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path to the node that is to be created.
         * @param firstProperty a property for the new node
         * @param additionalProperties additional properties for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( String atPath,
                                     Property firstProperty,
                                     Property... additionalProperties ) {
            return create(createPath(atPath)).with(firstProperty, additionalProperties);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public final Create<Batch> create( Path at ) {
            assertNotExecuted();
            CheckArg.isNotNull(at, "at");
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            return create(Location.create(parent), name);
        }

        protected final CreateAction<Batch> create( Location parent,
                                                    Name child ) {
            return new CreateAction<Batch>(this, parent, getCurrentWorkspaceName(), child) {
                @Override
                protected Batch submit( Location parent,
                                        String workspaceName,
                                        Name childName,
                                        Collection<Property> properties,
                                        NodeConflictBehavior behavior ) {
                    requestQueue.createNode(parent, workspaceName, childName, properties.iterator(), behavior);
                    return Batch.this;
                }
            };
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link Batch#execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @param properties the iterator over the properties for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( Path at,
                                     Iterable<Property> properties ) {
            Create<Batch> action = create(at);
            for (Property property : properties) {
                action.and(property);
            }
            return action;
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @param property a property for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( Path at,
                                     Property property ) {
            return create(at).with(property);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @param firstProperty a property for the new node
         * @param additionalProperties additional properties for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( Path at,
                                     Property firstProperty,
                                     Property... additionalProperties ) {
            return create(at).with(firstProperty, additionalProperties);
        }

        /**
         * Begin the request to create a node under the existing parent node at the supplied location. This request is submitted
         * to the repository after the returned components are completed.
         * 
         * @param parent the location of the parent
         * @return the object used to start creating a node
         */
        public CreateNodeNamed<Batch> createUnder( Location parent ) {
            CheckArg.isNotNull(parent, "parent");
            return new CreateNodeNamedAction<Batch>(this, parent) {
                @Override
                protected CreateAction<Batch> createWith( Batch batch,
                                                          Location parent,
                                                          Name childName ) {
                    return Batch.this.create(parent, childName);
                }
            };
        }

        /**
         * Set the properties on a node.
         * 
         * @param properties the properties to set
         * @return the interface that should be used to specify the node on which the properties are to be set.
         */
        public On<BatchConjunction> set( final Property... properties ) {
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    requestQueue.setProperties(location, getCurrentWorkspaceName(), properties);
                    return nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(Location.create(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(Location.create(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(Location.create(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(Location.create(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( Iterable<Property> idProperties ) {
                    return on(Location.create(idProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(Location.create(uuid));
                }
            };
        }

        /**
         * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
         * value(s) and the location of the node onto which the property should be set.
         * 
         * @param propertyName the property name
         * @return the interface used to specify the values
         */
        public SetValues<BatchConjunction> set( String propertyName ) {
            Name name = getContext().getValueFactories().getNameFactory().create(propertyName);
            return set(name);
        }

        /**
         * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
         * value(s) and the location of the node onto which the property should be set.
         * 
         * @param propertyName the property name
         * @return the interface used to specify the values
         */
        public SetValues<BatchConjunction> set( final Name propertyName ) {
            return new SetValues<BatchConjunction>() {
                public SetValuesTo<BatchConjunction> on( final Location location ) {
                    return new SetValuesTo<BatchConjunction>() {
                        public BatchConjunction to( Node value ) {
                            return to(value.getLocation());
                        }

                        public BatchConjunction to( Location value ) {
                            Reference ref = (Reference)convertReferenceValue(value);
                            Property property = getContext().getPropertyFactory().create(propertyName, ref);
                            requestQueue.setProperty(location, getCurrentWorkspaceName(), property);
                            return nextRequests;
                        }

                        protected BatchConjunction toValue( Object value ) {
                            Property property = getContext().getPropertyFactory().create(propertyName, value);
                            requestQueue.setProperty(location, getCurrentWorkspaceName(), property);
                            return nextRequests;
                        }

                        public BatchConjunction to( String value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( int value ) {
                            return toValue(Integer.valueOf(value));
                        }

                        public BatchConjunction to( long value ) {
                            return toValue(Long.valueOf(value));
                        }

                        public BatchConjunction to( boolean value ) {
                            return toValue(Boolean.valueOf(value));
                        }

                        public BatchConjunction to( float value ) {
                            return toValue(Float.valueOf(value));
                        }

                        public BatchConjunction to( double value ) {
                            return toValue(Double.valueOf(value));
                        }

                        public BatchConjunction to( BigDecimal value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Calendar value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Date value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( DateTime value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Name value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Path value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Reference value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( URI value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( UUID value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Binary value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( byte[] value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( InputStream stream,
                                                    long approximateLength ) {
                            Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                            return toValue(value);
                        }

                        public BatchConjunction to( Reader reader,
                                                    long approximateLength ) {
                            Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                            return toValue(value);
                        }

                        public BatchConjunction to( Object value ) {
                            value = convertReferenceValue(value);
                            Property property = getContext().getPropertyFactory().create(propertyName, value);
                            requestQueue.setProperty(location, getCurrentWorkspaceName(), property);
                            return nextRequests;
                        }

                        public BatchConjunction to( Object firstValue,
                                                    Object... otherValues ) {
                            firstValue = convertReferenceValue(firstValue);
                            for (int i = 0, len = otherValues.length; i != len; ++i) {
                                otherValues[i] = convertReferenceValue(otherValues[i]);
                            }
                            Property property = getContext().getPropertyFactory().create(propertyName, firstValue, otherValues);
                            requestQueue.setProperty(location, getCurrentWorkspaceName(), property);
                            return nextRequests;
                        }

                        public BatchConjunction to( Object[] values ) {
                            for (int i = 0; i != values.length; ++i) {
                                values[i] = convertReferenceValue(values[i]);
                            }
                            Property property = getContext().getPropertyFactory().create(propertyName, values);
                            requestQueue.setProperty(location, getCurrentWorkspaceName(), property);
                            return nextRequests;
                        }

                        public BatchConjunction to( Iterable<?> values ) {
                            List<Object> valueList = new LinkedList<Object>();
                            for (Object value : values) {
                                value = convertReferenceValue(value);
                                valueList.add(value);
                            }
                            Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                            requestQueue.setProperty(location, getCurrentWorkspaceName(), property);
                            return nextRequests;
                        }

                        public BatchConjunction to( Iterator<?> values ) {
                            List<Object> valueList = new LinkedList<Object>();
                            while (values.hasNext()) {
                                Object value = values.next();
                                valueList.add(value);
                            }
                            Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                            requestQueue.setProperty(location, getCurrentWorkspaceName(), property);
                            return nextRequests;
                        }

                    };
                }

                public SetValuesTo<BatchConjunction> on( String path ) {
                    return on(Location.create(createPath(path)));
                }

                public SetValuesTo<BatchConjunction> on( Path path ) {
                    return on(Location.create(path));
                }

                public SetValuesTo<BatchConjunction> on( Property idProperty ) {
                    return on(Location.create(idProperty));
                }

                public SetValuesTo<BatchConjunction> on( Property firstIdProperty,
                                                         Property... additionalIdProperties ) {
                    return on(Location.create(firstIdProperty, additionalIdProperties));
                }

                public SetValuesTo<BatchConjunction> on( Iterable<Property> idProperties ) {
                    return on(Location.create(idProperties));
                }

                public SetValuesTo<BatchConjunction> on( UUID uuid ) {
                    return on(Location.create(uuid));
                }

                public On<BatchConjunction> to( Node value ) {
                    Object reference = convertReferenceValue(value);
                    return set(getContext().getPropertyFactory().create(propertyName, reference));
                }

                public On<BatchConjunction> to( Location value ) {
                    Object reference = convertReferenceValue(value);
                    return set(getContext().getPropertyFactory().create(propertyName, reference));
                }

                protected On<BatchConjunction> toValue( Object value ) {
                    return set(getContext().getPropertyFactory().create(propertyName, value));
                }

                public On<BatchConjunction> to( String value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( int value ) {
                    return toValue(Integer.valueOf(value));
                }

                public On<BatchConjunction> to( long value ) {
                    return toValue(Long.valueOf(value));
                }

                public On<BatchConjunction> to( boolean value ) {
                    return toValue(Boolean.valueOf(value));
                }

                public On<BatchConjunction> to( float value ) {
                    return toValue(Float.valueOf(value));
                }

                public On<BatchConjunction> to( double value ) {
                    return toValue(Double.valueOf(value));
                }

                public On<BatchConjunction> to( BigDecimal value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Calendar value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Date value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( DateTime value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Name value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Path value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Reference value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( URI value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( UUID value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Binary value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( byte[] value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( InputStream stream,
                                                long approximateLength ) {
                    Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                    return toValue(value);
                }

                public On<BatchConjunction> to( Reader reader,
                                                long approximateLength ) {
                    Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                    return toValue(value);
                }

                public On<BatchConjunction> to( Object value ) {
                    value = convertReferenceValue(value);
                    return set(getContext().getPropertyFactory().create(propertyName, value));
                }

                public On<BatchConjunction> to( Object firstValue,
                                                Object... otherValues ) {
                    Object[] values = new Object[otherValues.length + 1];
                    values[0] = convertReferenceValue(firstValue);
                    for (int i = 0, len = otherValues.length; i != len; ++i) {
                        values[i + 1] = convertReferenceValue(otherValues[i]);
                    }
                    return set(getContext().getPropertyFactory().create(propertyName, values));
                }

                public On<BatchConjunction> to( Object[] values ) {
                    for (int i = 0, len = values.length; i != len; ++i) {
                        values[i] = convertReferenceValue(values[i]);
                    }
                    return set(getContext().getPropertyFactory().create(propertyName, values));
                }

                public On<BatchConjunction> to( Iterable<?> values ) {
                    List<Object> valueList = new LinkedList<Object>();
                    for (Object value : values) {
                        value = convertReferenceValue(value);
                        valueList.add(value);
                    }
                    return set(getContext().getPropertyFactory().create(propertyName, valueList));
                }

                public On<BatchConjunction> to( Iterator<?> values ) {
                    List<Object> valueList = new LinkedList<Object>();
                    while (values.hasNext()) {
                        Object value = values.next();
                        valueList.add(value);
                    }
                    return set(getContext().getPropertyFactory().create(propertyName, valueList));
                }
            };
        }

        /**
         * Remove properties from the node at the given location.
         * 
         * @param propertyNames the names of the properties to be removed
         * @return the remove request object that should be used to specify the node from which the properties are to be removed.
         */
        public On<BatchConjunction> remove( final Name... propertyNames ) {
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    requestQueue.removeProperties(location, getCurrentWorkspaceName(), propertyNames);
                    return nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(Location.create(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(Location.create(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(Location.create(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(Location.create(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( Iterable<Property> idProperties ) {
                    return on(Location.create(idProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(Location.create(uuid));
                }
            };
        }

        /**
         * Remove properties from the node at the given location.
         * 
         * @param propertyNames the names of the properties to be removed
         * @return the remove request object that should be used to specify the node from which the properties are to be removed.
         */
        public On<BatchConjunction> remove( String... propertyNames ) {
            NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
            int number = propertyNames.length;
            final Name[] names = new Name[number];
            for (int i = 0; i != number; ++i) {
                names[i] = nameFactory.create(propertyNames[i]);
            }
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    requestQueue.removeProperties(location, getCurrentWorkspaceName(), names);
                    return nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(Location.create(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(Location.create(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(Location.create(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(Location.create(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( Iterable<Property> idProperties ) {
                    return on(Location.create(idProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(Location.create(uuid));
                }
            };
        }

        /**
         * Request to read the node with the supplied UUID.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param uuid the UUID of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( UUID uuid ) {
            return read(Location.create(uuid));
        }

        /**
         * Request to read the node at the supplied location.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param location the location of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Location location ) {
            assertNotExecuted();
            requestQueue.readNode(location, getCurrentWorkspaceName());
            return nextRequests;
        }

        /**
         * Request to read the node at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param path the path of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( String path ) {
            return read(Location.create(createPath(path)));
        }

        /**
         * Request to read the node at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param path the path of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Path path ) {
            return read(Location.create(path));
        }

        /**
         * Request to read the node with the supplied unique identifier property.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the identification property that is unique to the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Property idProperty ) {
            return read(Location.create(idProperty));
        }

        /**
         * Request to read the node with the supplied unique identifier properties.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first of the identification properties that uniquely identify the node that is to be read
         * @param additionalIdProperties the remaining identification properties that uniquely identify the node that is to be
         *        read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Property firstIdProperty,
                                      Property... additionalIdProperties ) {
            return read(Location.create(firstIdProperty, additionalIdProperties));
        }

        /**
         * Request to read the node with the supplied unique identifier properties.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperties the identification properties that uniquely identify the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Iterable<Property> idProperties ) {
            return read(Location.create(idProperties));
        }

        /**
         * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
         * returned {@link On} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param propertyName the name of the property that is to be read
         * @return the object that is used to specified the node whose property is to be read
         */
        public On<BatchConjunction> readProperty( String propertyName ) {
            assertNotExecuted();
            Name name = Graph.this.getContext().getValueFactories().getNameFactory().create(propertyName);
            return readProperty(name);
        }

        /**
         * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
         * returned {@link On} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param name the name of the property that is to be read
         * @return the object that is used to specified the node whose property is to be read
         */
        public On<BatchConjunction> readProperty( final Name name ) {
            assertNotExecuted();
            return new On<BatchConjunction>() {
                public BatchConjunction on( String path ) {
                    return on(Location.create(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(Location.create(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(Location.create(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(Location.create(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( Iterable<Property> idProperties ) {
                    return on(Location.create(idProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(Location.create(uuid));
                }

                public BatchConjunction on( Location at ) {
                    requestQueue.readProperty(at, getCurrentWorkspaceName(), name);
                    return Batch.this.nextRequests;
                }
            };
        }

        /**
         * Request that the properties be read on the node defined via the <code>on(...)</code> method on the returned {@link On}
         * object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @return the object that is used to specified the node whose properties are to be read,
         */
        public On<BatchConjunction> readProperties() {
            assertNotExecuted();
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    requestQueue.readAllProperties(location, getCurrentWorkspaceName());
                    return Batch.this.nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(Location.create(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(Location.create(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(Location.create(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(Location.create(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( Iterable<Property> idProperties ) {
                    return on(Location.create(idProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(Location.create(uuid));
                }
            };
        }

        /**
         * Request that the children be read on the node defined via the <code>of(...)</code> method on the returned {@link Of}
         * object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @return the object that is used to specified the node whose children are to be read
         */
        public Of<BatchConjunction> readChildren() {
            assertNotExecuted();
            return new Of<BatchConjunction>() {
                public BatchConjunction of( String path ) {
                    return of(Location.create(createPath(path)));
                }

                public BatchConjunction of( Path path ) {
                    return of(Location.create(path));
                }

                public BatchConjunction of( Property idProperty ) {
                    return of(Location.create(idProperty));
                }

                public BatchConjunction of( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return of(Location.create(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction of( Iterable<Property> idProperties ) {
                    return of(Location.create(idProperties));
                }

                public BatchConjunction of( UUID uuid ) {
                    return of(Location.create(uuid));
                }

                public BatchConjunction of( Location at ) {
                    requestQueue.readAllChildren(at, getCurrentWorkspaceName());
                    return Batch.this.nextRequests;
                }
            };
        }

        /**
         * Request to read a subgraph of the specified depth, rooted at a location that will be specified via <code>at(...)</code>
         * in the resulting {@link At} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param depth the maximum depth of the subgraph that should be read
         * @return the component that should be used to specify the location of the node that is the top of the subgraph
         */
        public At<BatchConjunction> readSubgraphOfDepth( final int depth ) {
            assertNotExecuted();
            return new At<BatchConjunction>() {
                public BatchConjunction at( Location location ) {
                    requestQueue.readBranch(location, getCurrentWorkspaceName(), depth);
                    return Batch.this.nextRequests;
                }

                public BatchConjunction at( String path ) {
                    return at(Location.create(createPath(path)));
                }

                public BatchConjunction at( Path path ) {
                    return at(Location.create(path));
                }

                public BatchConjunction at( UUID uuid ) {
                    return at(Location.create(uuid));
                }

                public BatchConjunction at( Property idProperty ) {
                    return at(Location.create(idProperty));
                }

                public BatchConjunction at( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return at(Location.create(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction at( Iterable<Property> idProperties ) {
                    return at(Location.create(idProperties));
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Graph.Executable#execute()
         */
        public Results execute() {
            executed = true;
            Request request = requestQueue.pop();
            if (request == null) {
                return new BatchResults();
            }
            Graph.this.execute(request);
            if (request instanceof CompositeRequest) {
                CompositeRequest composite = (CompositeRequest)request;
                return new BatchResults(composite.getRequests());
            }
            return new BatchResults(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Pending requests:\n");
            sb.append(requestQueue.toString());
            return sb.toString();
        }
    }

    /**
     * Utility method for checking a property value. If the value is a {@link Node} or {@link Location}, a {@link Reference} value
     * is created (if the node/location has a UUID); otherwise, the value is returned as is.
     * 
     * @param value the property value
     * @return the property value, which may be a {@link Reference} if the input value is a Node or Location
     */
    protected Object convertReferenceValue( Object value ) {
        if (value instanceof Node) {
            Node node = (Node)value;
            UUID uuid = node.getLocation().getUuid();
            if (uuid == null) {
                // Look for a property ...
                Property uuidProperty = node.getProperty(DnaLexicon.UUID);
                if (uuidProperty != null) {
                    uuid = context.getValueFactories().getUuidFactory().create(uuidProperty.getFirstValue());
                } else {
                    uuidProperty = node.getProperty(JcrLexicon.UUID);
                    if (uuidProperty != null) {
                        uuid = context.getValueFactories().getUuidFactory().create(uuidProperty.getFirstValue());
                    }
                }
            }
            if (uuid == null) {
                String nodeString = node.getLocation().getString(getContext().getNamespaceRegistry());
                String msg = GraphI18n.unableToCreateReferenceToNodeWithoutUuid.text(nodeString);
                throw new IllegalArgumentException(msg);
            }
            return getContext().getValueFactories().getReferenceFactory().create(uuid);
        }
        if (value instanceof Location) {
            Location location = (Location)value;
            UUID uuid = location.getUuid();
            if (uuid == null) {
                String nodeString = location.getString(getContext().getNamespaceRegistry());
                String msg = GraphI18n.unableToCreateReferenceToNodeWithoutUuid.text(nodeString);
                throw new IllegalArgumentException(msg);
            }
            return getContext().getValueFactories().getReferenceFactory().create(uuid);
        }
        return value;
    }

    protected static DateTime computeExpirationTime( CacheableRequest request ) {
        CachePolicy policy = request.getCachePolicy();
        return policy == null ? null : request.getTimeLoaded().plus(policy.getTimeToLive(), TimeUnit.MILLISECONDS);
    }

    /**
     * The interface used to specify the name of a new workspace.
     */
    public interface NameWorkspace {

        /**
         * Specify the name of the new workspace that is to be created.
         * 
         * @param workspaceName the name of the existing workspace that will be cloned to create the new workspace;
         * @return the workspace; never null
         * @throws IllegalArgumentException if the name of the new workspace is null
         * @throws InvalidWorkspaceException if there is already an existing workspace with the supplied name
         */
        Workspace named( String workspaceName );

        /**
         * Specify the name of the new workspace that is to be created. If a workspace with the supplied name already exists, the
         * new workspace name will be adjusted so that it is unique.
         * 
         * @param workspaceName the name of the existing workspace that will be cloned to create the new workspace;
         * @return the workspace; never null
         * @throws IllegalArgumentException if the name of the new workspace is null
         */
        Workspace namedSomethingLike( String workspaceName );
    }

    /**
     * The interface used to create a new workspace.
     */
    public interface CreateWorkspace extends NameWorkspace {
        /**
         * Specify that the new workspace should be initialized as a clone of another existing workspace.
         * 
         * @param originalWorkspaceName the name of the existing workspace that will be cloned to create the new workspace;
         * @return the interface that should be used to set the name of the new workspace; never null
         * @throws IllegalArgumentException if the name of the original workspace is null
         * @throws InvalidWorkspaceException if there is no such workspace with the supplied name
         */
        NameWorkspace clonedFrom( String originalWorkspaceName );
    }

    /**
     * A interface used to execute the accumulated {@link Batch requests}.
     * 
     * @author Randall Hauch
     * @param <NodeType> the type of node that is returned
     */
    public interface Executable<NodeType extends Node> {
        /**
         * Stop accumulating the requests, submit them to the repository source, and return the results.
         * 
         * @return the results containing the requested information from the repository.
         * @throws PathNotFoundException if a request used a node that did not exist
         * @throws InvalidRequestException if a request was not valid
         * @throws InvalidWorkspaceException if the workspace used in a request was not valid
         * @throws UnsupportedRequestException if a request was not supported by the source
         * @throws RepositorySourceException if an error occurs during execution
         * @throws RuntimeException if a runtime error occurs during execution
         */
        Results execute();
    }

    /**
     * A interface that can be used to finish the current request and start another.
     * 
     * @param <Next> the interface that will be used to start another request
     * @author Randall Hauch
     */
    public interface Conjunction<Next> {
        /**
         * Finish the request and prepare to start another.
         * 
         * @return the interface that can be used to start another request; never null
         */
        Next and();
    }

    /**
     * A component that defines the location into which a node should be copied or moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface Into<Next> {
        /**
         * Finish the request by specifying the location of the parent into which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index). If you want to control the name of the node for the newly copied/moved node, use
         * {@link To#to(Location)} instead.
         * 
         * @param parentLocation the location of the new parent
         * @return the interface for additional requests or actions
         * @see To#to(Location)
         */
        Next into( Location parentLocation );

        /**
         * Finish the request by specifying the location of the parent into which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index). If you want to control the name of the node for the newly copied/moved node, use
         * {@link To#to(String)} instead.
         * 
         * @param parentPath the path of the new parent
         * @return the interface for additional requests or actions
         * @see To#to(String)
         */
        Next into( String parentPath );

        /**
         * Finish the request by specifying the location of the parent into which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index). If you want to control the name of the node for the newly copied/moved node, use
         * {@link To#to(Path)} instead.
         * 
         * @param parentPath the path of the new parent
         * @return the interface for additional requests or actions
         * @see To#to(Path)
         */
        Next into( Path parentPath );

        /**
         * Finish the request by specifying the location of the parent into which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index).
         * 
         * @param parentUuid the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next into( UUID parentUuid );

        /**
         * Finish the request by specifying the location of the parent into which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index).
         * 
         * @param parentIdProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next into( Property parentIdProperty );

        /**
         * Finish the request by specifying the location of the parent into which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index).
         * 
         * @param firstParentIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies
         *        the new parent
         * @param additionalParentIdProperties the additional properties that, with the <code>additionalIdProperties</code>,
         *        uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next into( Property firstParentIdProperty,
                   Property... additionalParentIdProperties );
    }

    /**
     * A component that defines the location before which a node should be copied or moved. This is similar to an {@link Into},
     * but it allows for placing a node at a particular location within the new destination, rather than always placing the moved
     * or copied node as the last child of the new parent.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface Before<Next> {
        /**
         * Finish the request by specifying the location of the node before which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index). If you want to control the name of the node for the newly copied/moved node, use
         * {@link To#to(Location)} instead.
         * 
         * @param parentLocation the location of the new parent
         * @return the interface for additional requests or actions
         * @see To#to(Location)
         */
        Next before( Location parentLocation );

        /**
         * Finish the request by specifying the location of the node before which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index). If you want to control the name of the node for the newly copied/moved node, use
         * {@link To#to(String)} instead.
         * 
         * @param parentPath the path of the new parent
         * @return the interface for additional requests or actions
         * @see To#to(String)
         */
        Next before( String parentPath );

        /**
         * Finish the request by specifying the location of the node before which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index). If you want to control the name of the node for the newly copied/moved node, use
         * {@link To#to(Path)} instead.
         * 
         * @param parentPath the path of the new parent
         * @return the interface for additional requests or actions
         * @see To#to(Path)
         */
        Next before( Path parentPath );

        /**
         * Finish the request by specifying the location of the node before which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index).
         * 
         * @param parentUuid the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next before( UUID parentUuid );

        /**
         * Finish the request by specifying the location of the node before which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index).
         * 
         * @param parentIdProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next before( Property parentIdProperty );

        /**
         * Finish the request by specifying the location of the node before which the node should be copied/moved. This operation
         * will result in the copied/moved node having the same name as the original (but with the appropriately-determined
         * same-name-sibling index).
         * 
         * @param firstParentIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies
         *        the new parent
         * @param additionalParentIdProperties the additional properties that, with the <code>additionalIdProperties</code>,
         *        uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next before( Property firstParentIdProperty,
                     Property... additionalParentIdProperties );
    }

    /**
     * A component that defines the location to which a node should be copied or moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface To<Next> {
        /**
         * Finish the request by specifying the Location.create where the node should be copied/moved. Unlike
         * {@link Into#into(Location)}, which specifies the location of the parent and which assumes the new node should have the
         * same name as the original, this method allows the caller to specify a new name for the new node.
         * 
         * @param desiredLocation the desired location for the new node, which must have a {@link Location#getPath() path}
         * @return the interface for additional requests or actions
         * @see Into#into(Location)
         */
        Next to( Location desiredLocation );

        /**
         * Finish the request by specifying the Location.create where the node should be copied/moved. Unlike
         * {@link Into#into(String)}, which specifies the location of the parent and which assumes the new node should have the
         * same name as the original, this method allows the caller to specify a new name for the new node.
         * 
         * @param desiredPath the path for the new node
         * @return the interface for additional requests or actions
         * @see Into#into(String)
         */
        Next to( String desiredPath );

        /**
         * Finish the request by specifying the Location.create where the node should be copied/moved. Unlike
         * {@link Into#into(Path)} , which specifies the location of the parent and which assumes the new node should have the
         * same name as the original, this method allows the caller to specify a new name for the new node.
         * 
         * @param desiredPath the path for the new node
         * @return the interface for additional requests or actions
         * @see Into#into(Path)
         */
        Next to( Path desiredPath );
    }

    /**
     * A component that defines a new child name for a node.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     */
    public interface AsChild<Next> {
        /**
         * Finish the request by specifying the exact segment for the new child node. If no segment already exists, this request
         * should fail.
         * 
         * @param newSegment the new name
         * @return the interface for additional requests or actions
         */
        Next as( Path.Segment newSegment );

        /**
         * Finish the request by specifying the name of the new child node. This method indicates that the child should be added
         * as a new node with the given name at the end of the parents children
         * 
         * @param newName the new name
         * @return the interface for additional requests or actions
         */
        Next as( Name newName );
    }

    /**
     * A component that defines a new name for a node.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface AsName<Next> {
        /**
         * Finish the request by specifying the new name.
         * 
         * @param newName the new name
         * @return the interface for additional requests or actions
         */
        Next as( String newName );

        /**
         * Finish the request by specifying the new name.
         * 
         * @param newName the new name
         * @return the interface for additional requests or actions
         */
        Next as( Name newName );
    }

    /**
     * A interface that is used to add more locations that are to be copied/moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface And<Next> {
        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param from the location of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Location from );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param fromPath the path of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( String fromPath );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param from the path of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Path from );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param from the UUID of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( UUID from );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param idProperty the property that uniquely identifies the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Property idProperty );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        node to be copied or moved
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Property firstIdProperty,
                  Property... additionalIdProperties );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param idProperties the properties that uniquely identifies the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Iterable<Property> idProperties );
    }

    /**
     * The interface for defining additional nodes to be moved and the parent into which the node(s) are to be moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface Move<Next> extends AsName<Into<Next>>, Into<Next>, Before<Next>, And<Move<Next>> {
    }

    /**
     * The interface for defining additional nodes to be copied and the locations where the copy is to be placed. The
     * <code>to(...)</code> methods allow you to specify the location of the copy, including the name for the node that results
     * from the copy. Alternatively, you can use the <code>into(...)</code> methods to specify the parent location where the copy
     * is to be placed, which will assume the new copy will have the same name as the original.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface Copy<Next> extends FromWorkspace<CopyTarget<Next>>, CopyTarget<Next>, And<Copy<Next>> {
    }

    public interface CopyTarget<Next> extends To<Next>, Into<Next> {
    }

    /**
     * The interface for defining a branch of nodes to be cloned and the location where the clone is to be placed. Cloning a
     * branch differs from copying a branch in that:
     * <ol>
     * <li>Nodes can be copied within the same workspace or to another workspace; cloned nodes must be copied from one workspace
     * into another.</li>
     * <li>Copied nodes always get new UUIDs; cloned nodes always maintain their UUIDs and hence must define the behavior that
     * occurs if a node with the same UUID already exists in the new workspace.</li>
     * <li>Nodes can be copied to a specific name under a specific parent, but can only be added as a new child node at the end of
     * the new parent's children; nodes can be cloned to an exact location among the parent's children, replacing the existing
     * node at that location.</li>
     * </ol>
     * 
     * @param <Next>
     */
    public interface Clone<Next> extends FromWorkspace<AsChild<Into<WithUuids<Next>>>> {

    }

    /**
     * The interface for specifying that a node should come from a workspace other than the current workspace.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     */
    public interface FromWorkspace<Next> {
        Next fromWorkspace( String workspaceName );
    }

    /**
     * The interface for specifying how UUID conflicts should be handled.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     */
    public interface WithUuids<Next> {
        Next failingIfAnyUuidsMatch();

        Next replacingExistingNodesWithSameUuids();
    }

    /**
     * The interface for defining additional properties on a new node.
     * 
     * @param <Next> The interface that is to be returned when this create request is completed
     * @author Randall Hauch
     */
    public interface Create<Next> extends Conjunction<Next> {
        /**
         * Create the node only if there is no existing node with the same {@link Path.Segment#getName() name} (ignoring
         * {@link Path.Segment#getIndex() same-name-sibling indexes}).
         * 
         * @return this interface for continued specification of the request
         */
        Create<Next> ifAbsent();

        /**
         * Create the node if it does not exist, or update any existing node that has the same {@link Path.Segment#getName() name}
         * (ignoring {@link Path.Segment#getIndex() same-name-sibling indexes}).
         * 
         * @return this interface for continued specification of the request
         */
        Create<Next> orUpdate();

        /**
         * Create the node if it does not exist, or replace any existing node that has the same {@link Path.Segment#getName()
         * name} (ignoring {@link Path.Segment#getIndex() same-name-sibling indexes}).
         * 
         * @return this interface for continued specification of the request
         */
        Create<Next> orReplace();

        /**
         * Create the node if it does not exist by appending or adjusting the {@link Path.Segment#getIndex() same-name-sibling
         * index}). This is the default behavior.
         * 
         * @return this interface for continued specification of the request
         */
        Create<Next> byAppending();

        /**
         * Specify the UUID that should the new node should have. This is an alias for {@link #and(UUID)}.
         * 
         * @param uuid the UUID
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( UUID uuid );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(Property)}.
         * 
         * @param property the property
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( Property property );

        /**
         * Specify property that should the new node should have. This is an alias for {@link #and(Iterable)}.
         * 
         * @param properties the properties that should be added
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( Iterable<Property> properties );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(String, Object...)}.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( String propertyName,
                           Object... values );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(Name, Object...)}.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( Name propertyName,
                           Object... values );

        /**
         * Specify properties that should the new node should have. This is an alias for {@link #and(Property, Property...)}.
         * 
         * @param firstProperty the first property
         * @param additionalProperties the additional property
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( Property firstProperty,
                           Property... additionalProperties );

        /**
         * Specify the UUID that should the new node should have.
         * 
         * @param uuid the UUID
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( UUID uuid );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param property the property
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( Property property );

        /**
         * Specify property that should the new node should have. This is equivalent to calling {@link #and(Property)} for each of
         * the properties in the supplied {@link Iterable}.
         * 
         * @param properties the properties that should be added
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( Iterable<Property> properties );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( String propertyName,
                          Object... values );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( Name propertyName,
                          Object... values );

        /**
         * Specify properties that should the new node should have.
         * 
         * @param firstProperty the first property
         * @param additionalProperties the additional property
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( Property firstProperty,
                          Property... additionalProperties );
    }

    /**
     * The interface for defining additional properties on a new node.
     * 
     * @param <Next> The interface that is to be returned when this create request is completed
     * @author Randall Hauch
     */
    public interface CreateAt<Next> extends Conjunction<Next> {
        /**
         * Specify the UUID that should the new node should have. This is an alias for {@link #and(UUID)}.
         * 
         * @param uuid the UUID
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> with( UUID uuid );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(Property)}.
         * 
         * @param property the property
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> with( Property property );

        /**
         * Specify property that should the new node should have. This is an alias for {@link #and(Iterable)}.
         * 
         * @param properties the properties that should be added
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> with( Iterable<Property> properties );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(String, Object...)}.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> with( String propertyName,
                             Object... values );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(Name, Object...)}.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> with( Name propertyName,
                             Object... values );

        /**
         * Specify properties that should the new node should have. This is an alias for {@link #and(Property, Property...)}.
         * 
         * @param firstProperty the first property
         * @param additionalProperties the additional property
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> with( Property firstProperty,
                             Property... additionalProperties );

        /**
         * Specify the UUID that should the new node should have.
         * 
         * @param uuid the UUID
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> and( UUID uuid );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param property the property
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> and( Property property );

        /**
         * Specify property that should the new node should have. This is equivalent to calling {@link #and(Property)} for each of
         * the properties in the supplied {@link Iterable}.
         * 
         * @param properties the properties that should be added
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> and( Iterable<Property> properties );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> and( String propertyName,
                            Object... values );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> and( Name propertyName,
                            Object... values );

        /**
         * Specify properties that should the new node should have.
         * 
         * @param firstProperty the first property
         * @param additionalProperties the additional property
         * @return this same interface so additional properties may be added
         */
        CreateAt<Next> and( Property firstProperty,
                            Property... additionalProperties );

        /**
         * Complete this request, submit it, and return the actual location of the created node.
         * 
         * @return the actual location of the just-created node; never null
         */
        Location getLocation();

        /**
         * Complete this request, submit it, and return the actual node that was created.
         * 
         * @return the actual node that was just created; never null
         */
        Node getNode();
    }

    /**
     * The interface for defining the node upon which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface On<Next> {
        /**
         * Specify the location of the node upon which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Location to );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( String toPath );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Path to );

        /**
         * Specify the UUID of the node upon which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( UUID to );

        /**
         * Specify the unique identification property that identifies the node upon which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Property firstIdProperty,
                 Property... additionalIdProperties );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param idProperties the properties that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Iterable<Property> idProperties );
    }

    /**
     * The interface for defining the node upon which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface Of<Next> {
        /**
         * Specify the location of the node upon which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Location to );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( String toPath );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Path to );

        /**
         * Specify the UUID of the node upon which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( UUID to );

        /**
         * Specify the unique identification property that identifies the node upon which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Property firstIdProperty,
                 Property... additionalIdProperties );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param idProperties the properties that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Iterable<Property> idProperties );
    }

    /**
     * The interface for defining the node upon which which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface At<Next> {
        /**
         * Specify the location of the node upon which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Location to );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( String toPath );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Path to );

        /**
         * Specify the UUID of the node upon which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( UUID to );

        /**
         * Specify the unique identification property that identifies the node upon which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Property firstIdProperty,
                 Property... additionalIdProperties );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param idProperties the properties that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Iterable<Property> idProperties );
    }

    /**
     * A component used to supply the details for getting children of another node. If all of the children are to be obtained,
     * then the parent can be specified using one of the <code>of(...)</code> methods on this component. If, however, only some of
     * the nodes are to be returned (e.g., a "block" of children), then specify the {@link #inBlockOf(int) block size} followed by
     * the {@link BlockOfChildren block size and parent}.
     * 
     * @param <Next>
     * @author Randall Hauch
     */
    public interface Children<Next> extends Of<Next> {
        /**
         * Specify that a block of children are to be retreived, and in particular the number of children that are to be returned.
         * 
         * @param blockSize the number of children that are to be retrieved in the block; must be positive
         * @return the interface used to specify the starting point for the block and the parent
         */
        BlockOfChildren<Next> inBlockOf( int blockSize );
    }

    /**
     * A component used to specify a block of children starting either {@link #startingAt(int) at a particular index} or
     * {@link #startingAfter(Location) after a previous sibling}.
     * 
     * @param <Next>
     * @author Randall Hauch
     */
    public interface BlockOfChildren<Next> {
        /**
         * Specify the block of children is to start at the supplied index.
         * 
         * @param startingIndex the zero-based index of the first child to be returned in the block
         * @return interface used to specify the parent of the children; never null
         */
        Under<Next> startingAt( int startingIndex );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param previousSibling the location of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( Location previousSibling );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param pathToPreviousSiblingName the path of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( String pathToPreviousSiblingName );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param previousSibling the path of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( Path previousSibling );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param previousSiblingUuid the UUID of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( UUID previousSiblingUuid );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param idPropertyOfPreviousSibling the property that uniquely identifies the previous sibling
         * @return the children; never null
         */
        Next startingAfter( Property idPropertyOfPreviousSibling );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param firstIdPropertyOfPreviousSibling the first property that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the previous sibling
         * @param additionalIdPropertiesOfPreviousSibling the additional properties that, with the
         *        <code>additionalIdProperties</code>, uniquely identifies the previous sibling
         * @return the children; never null
         */
        Next startingAfter( Property firstIdPropertyOfPreviousSibling,
                            Property... additionalIdPropertiesOfPreviousSibling );
    }

    /**
     * The interface for defining the node under which which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface Under<Next> {
        /**
         * Specify the location of the node under which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Location to );

        /**
         * Specify the path of the node under which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( String toPath );

        /**
         * Specify the path of the node under which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Path to );

        /**
         * Specify the UUID of the node under which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( UUID to );

        /**
         * Specify the unique identification property that identifies the node under which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node under which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Property firstIdProperty,
                    Property... additionalIdProperties );
    }

    /**
     * A component used to set the values on a property.
     * 
     * @param <Next> the next command
     * @author Randall Hauch
     */
    public interface SetValues<Next> extends On<SetValuesTo<Next>>, SetValuesTo<On<Next>> {
    }

    /**
     * A component used to set the values on a property.
     * 
     * @param <Next>
     * @author Randall Hauch
     */
    public interface SetValuesTo<Next> {

        /**
         * Set the property value to be a reference to the given node. Note that it is an error if the Node does not have a
         * {@link Location#getUuid() UUID}.
         * 
         * @param node the node to which a reference should be set
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the value is a Node that has no {@link Location#getUuid() UUID}
         */
        Next to( Node node );

        /**
         * Set the property value to be a reference to the given location. Note that it is an error if the Location does not have
         * a {@link Location#getUuid() UUID}.
         * 
         * @param location the location to which a reference should be set
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the value is a Location that has no {@link Location#getUuid() UUID}
         */
        Next to( Location location );

        /**
         * Set the property value to the given string.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( String value );

        /**
         * Set the property value to the given integer value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( int value );

        /**
         * Set the property value to the given long value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( long value );

        /**
         * Set the property value to the given boolean value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( boolean value );

        /**
         * Set the property value to the given float value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( float value );

        /**
         * Set the property value to the given double value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( double value );

        /**
         * Set the property value to the given decimal value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( BigDecimal value );

        /**
         * Set the property value to the date given by the supplied calendar.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Calendar value );

        /**
         * Set the property value to the given date.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Date value );

        /**
         * Set the property value to the given date-time instant.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( DateTime value );

        /**
         * Set the property value to the given Name.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Name value );

        /**
         * Set the property value to the given Path.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Path value );

        /**
         * Set the property value to the given Reference. See also {@link #to(Node)}.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Reference value );

        /**
         * Set the property value to the given URI.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( URI value );

        /**
         * Set the property value to the given UUID.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( UUID value );

        /**
         * Set the property value to the given binary value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Binary value );

        /**
         * Set the property value to the given byte array.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( byte[] value );

        /**
         * Set the property value to the given string.
         * 
         * @param stream the stream containing the content to be used for the property value
         * @param approximateLength the approximate length of the content (in bytes)
         * @return the interface for additional requests or actions
         */
        Next to( InputStream stream,
                 long approximateLength );

        /**
         * Set the property value to the given string.
         * 
         * @param reader the reader containing the content to be used for the property value
         * @param approximateLength the approximate length of the content (in bytes)
         * @return the interface for additional requests or actions
         */
        Next to( Reader reader,
                 long approximateLength );

        /**
         * Set the property value to the given object. The supplied <code>value</code> should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the value is a Node or Location that has no {@link Location#getUuid() UUID}
         */
        Next to( Object value );

        /**
         * Set the property values to the given object. Each of the supplied <code>values</code> should be a valid property value,
         * or a {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note
         * that it is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param values the property values
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the any of the values is a Node or Location that has no {@link Location#getUuid()
         *         UUID}
         */
        Next to( Object[] values );

        /**
         * Set the property value to the given objects. Each of the supplied values should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param firstValue the first property value
         * @param otherValues the remaining property values
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the any of the values is a Node or Location that has no {@link Location#getUuid()
         *         UUID}
         */
        Next to( Object firstValue,
                 Object... otherValues );

        /**
         * Set the property value to the given object. Each of the supplied values should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param values the container for the property values
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the any of the values is a Node or Location that has no {@link Location#getUuid()
         *         UUID}
         */
        Next to( Iterable<?> values );

        /**
         * Set the property value to the given object. Each of the supplied values should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param values the iterator over the property values
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the any of the values is a Node or Location that has no {@link Location#getUuid()
         *         UUID}
         */
        Next to( Iterator<?> values );
    }

    /**
     * A component that defines a node that is to be created.
     * 
     * @param <Next> The interface that is to be returned to complete the create request
     * @author Randall Hauch
     */
    public interface CreateNode<Next> {
        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @param properties the properties for the new node
         * @return the next component for making additional requests.
         */
        Next node( String nodeName,
                   Property... properties );

        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @param properties the properties for the new node
         * @return the next component for making additional requests.
         */
        Next node( String nodeName,
                   Iterator<Property> properties );

        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @param properties the properties for the new node
         * @return the next component for making additional requests.
         */
        Next node( String nodeName,
                   Iterable<Property> properties );
    }

    /**
     * A component that defines a node that is to be created.
     * 
     * @param <Next> The interface that is to be returned to complete the create request
     * @author Randall Hauch
     */
    public interface CreateNodeNamed<Next> {
        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @return the interface used to complete the request
         */
        Create<Next> nodeNamed( String nodeName );

        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @return the interface used to complete the request
         */
        Create<Next> nodeNamed( Name nodeName );
    }

    /**
     * A component that defines the location into which a node should be copied or moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface ImportInto<Next> {
        /**
         * Specify whether the root element in the XML document should be skipped (that is, not be represented by a node). By
         * default, the root element is not skipped.
         * 
         * @param skip true if the root element should be skipped, or false if a node should be created for the root XML element
         * @return the interface used to specify the location where the content should be placed
         */
        ImportInto<Next> skippingRootElement( boolean skip );

        /**
         * Finish the import by specifying the Location.create into which the node should be copied/moved.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Location to ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the Location.create into which the node should be copied/moved.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( String toPath ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the Location.create into which the node should be copied/moved.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Path to ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the Location.create into which the node should be copied/moved.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( UUID to ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the Location.create into which the node should be copied/moved.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Property idProperty ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the Location.create into which the node should be copied/moved.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Property firstIdProperty,
                   Property... additionalIdProperties ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the Location.create into which the node should be copied/moved.
         * 
         * @param idProperties the properties that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Iterable<Property> idProperties ) throws IOException, SAXException;
    }

    public interface BatchConjunction extends Conjunction<Batch>, Executable<Node> {
    }

    public interface GetNodeConjunction<Next> extends Conjunction<Next> {
        Node andReturn();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Node Implementation
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    protected class GraphNode implements Node {
        private final ReadNodeRequest request;

        /*package*/GraphNode( ReadNodeRequest request ) {
            this.request = request;
        }

        public Location getLocation() {
            return request.getActualLocationOfNode();
        }

        public DateTime getExpirationTime() {
            CachePolicy policy = request.getCachePolicy();
            return policy == null ? null : request.getTimeLoaded().plus(policy.getTimeToLive(), TimeUnit.MILLISECONDS);
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Collection<Property> getProperties() {
            return request.getProperties();
        }

        public Property getProperty( Name name ) {
            return getPropertiesByName().get(name);
        }

        public Property getProperty( String nameStr ) {
            Name name = getContext().getValueFactories().getNameFactory().create(nameStr);
            return getPropertiesByName().get(name);
        }

        public Map<Name, Property> getPropertiesByName() {
            return request.getPropertiesByName();
        }

        public List<Location> getChildren() {
            return request.getChildren();
        }

        public boolean hasChildren() {
            return request.getChildren().size() > 0;
        }

        public List<Segment> getChildrenSegments() {
            return getSegments(getChildren());
        }

        public Iterator<Location> iterator() {
            return request.getChildren().iterator();
        }

        @Override
        public int hashCode() {
            return getLocation().hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Node) {
                Node that = (Node)obj;
                return this.getLocation().equals(that.getLocation());
            }
            return false;
        }

        @Override
        public String toString() {
            return "Node " + getLocation().toString();
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Results implementation for the batched requests
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    class BatchResults implements Results {
        private final Map<Path, BatchResultsNode> nodes = new HashMap<Path, BatchResultsNode>();

        /*package*/BatchResults( List<Request> requests ) {
            for (Request request : requests) {
                if (request instanceof ReadAllPropertiesRequest) {
                    ReadAllPropertiesRequest read = (ReadAllPropertiesRequest)request;
                    DateTime expires = computeExpirationTime(read);
                    getOrCreateNode(read.getActualLocationOfNode(), expires).setProperties(read.getPropertiesByName());
                } else if (request instanceof ReadPropertyRequest) {
                    ReadPropertyRequest read = (ReadPropertyRequest)request;
                    DateTime expires = computeExpirationTime(read);
                    getOrCreateNode(read.getActualLocationOfNode(), expires).addProperty(read.getProperty());
                } else if (request instanceof ReadNodeRequest) {
                    ReadNodeRequest read = (ReadNodeRequest)request;
                    DateTime expires = computeExpirationTime(read);
                    BatchResultsNode node = getOrCreateNode(read.getActualLocationOfNode(), expires);
                    node.setProperties(read.getPropertiesByName());
                    node.setChildren(read.getChildren());
                } else if (request instanceof ReadBlockOfChildrenRequest) {
                    throw new IllegalStateException();
                } else if (request instanceof ReadAllChildrenRequest) {
                    ReadAllChildrenRequest read = (ReadAllChildrenRequest)request;
                    DateTime expires = computeExpirationTime(read);
                    getOrCreateNode(read.getActualLocationOfNode(), expires).setChildren(read.getChildren());
                } else if (request instanceof ReadBranchRequest) {
                    ReadBranchRequest read = (ReadBranchRequest)request;
                    DateTime expires = computeExpirationTime(read);
                    for (Location location : read) {
                        BatchResultsNode node = getOrCreateNode(location, expires);
                        node.setProperties(read.getPropertiesFor(location));
                        node.setChildren(read.getChildren(location));
                    }
                }
            }
            for (Map.Entry<Path, BatchResultsNode> entry : nodes.entrySet()) {
                entry.getValue().freeze();
            }
        }

        /*package*/BatchResults( Request request ) {
            if (request instanceof ReadAllPropertiesRequest) {
                ReadAllPropertiesRequest read = (ReadAllPropertiesRequest)request;
                DateTime expires = computeExpirationTime(read);
                getOrCreateNode(read.getActualLocationOfNode(), expires).setProperties(read.getPropertiesByName());
            } else if (request instanceof ReadPropertyRequest) {
                ReadPropertyRequest read = (ReadPropertyRequest)request;
                DateTime expires = computeExpirationTime(read);
                getOrCreateNode(read.getActualLocationOfNode(), expires).addProperty(read.getProperty());
            } else if (request instanceof ReadNodeRequest) {
                ReadNodeRequest read = (ReadNodeRequest)request;
                DateTime expires = computeExpirationTime(read);
                BatchResultsNode node = getOrCreateNode(read.getActualLocationOfNode(), expires);
                node.setProperties(read.getPropertiesByName());
                node.setChildren(read.getChildren());
            } else if (request instanceof ReadBlockOfChildrenRequest) {
                throw new IllegalStateException();
            } else if (request instanceof ReadAllChildrenRequest) {
                ReadAllChildrenRequest read = (ReadAllChildrenRequest)request;
                DateTime expires = computeExpirationTime(read);
                getOrCreateNode(read.getActualLocationOfNode(), expires).setChildren(read.getChildren());
            } else if (request instanceof ReadBranchRequest) {
                ReadBranchRequest read = (ReadBranchRequest)request;
                DateTime expires = computeExpirationTime(read);
                for (Location location : read) {
                    BatchResultsNode node = getOrCreateNode(location, expires);
                    node.setProperties(read.getPropertiesFor(location));
                    node.setChildren(read.getChildren(location));
                }
            }
            for (Map.Entry<Path, BatchResultsNode> entry : nodes.entrySet()) {
                entry.getValue().freeze();
            }
        }

        /*package*/BatchResults() {
        }

        private BatchResultsNode getOrCreateNode( Location location,
                                                  DateTime expirationTime ) {
            BatchResultsNode node = nodes.get(location);
            if (node == null) {
                node = new BatchResultsNode(location, expirationTime);
                assert location.getPath() != null;
                nodes.put(location.getPath(), node);
            }
            return node;
        }

        public Graph getGraph() {
            return Graph.this;
        }

        protected void checkIsAbsolute( Path path ) {
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(GraphI18n.pathIsNotAbsolute.text(path));
            }
        }

        public Node getNode( String pathStr ) {
            Path path = createPath(pathStr);
            checkIsAbsolute(path);
            return nodes.get(path);
        }

        public Node getNode( Path path ) {
            CheckArg.isNotNull(path, "path");
            checkIsAbsolute(path);
            return nodes.get(path);
        }

        public Node getNode( Location location ) {
            CheckArg.isNotNull(location, "location");
            CheckArg.isNotNull(location.getPath(), "location.getPath()");
            return nodes.get(location.getPath());
        }

        public boolean includes( String path ) {
            return getNode(path) != null;
        }

        public boolean includes( Path path ) {
            return getNode(path) != null;
        }

        public boolean includes( Location location ) {
            return getNode(location) != null;
        }

        public Iterator<Node> iterator() {
            List<Path> paths = new ArrayList<Path>(nodes.keySet());
            Collections.sort(paths);
            final Iterator<Path> pathIter = paths.iterator();
            return new Iterator<Node>() {
                public boolean hasNext() {
                    return pathIter.hasNext();
                }

                public Node next() {
                    Path nextPath = pathIter.next();
                    return getNode(nextPath);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    @Immutable
    class BatchResultsNode implements Node {
        private final Location location;
        private final DateTime expirationTime;
        private Map<Name, Property> properties;
        private List<Location> children;

        BatchResultsNode( Location location,
                          DateTime expirationTime ) {
            this.location = location;
            this.expirationTime = expirationTime;
        }

        public DateTime getExpirationTime() {
            return expirationTime;
        }

        void addProperty( Property property ) {
            if (this.properties == null) this.properties = new HashMap<Name, Property>();
            this.properties.put(property.getName(), property);
        }

        void setProperties( Map<Name, Property> properties ) {
            this.properties = properties;
        }

        void setChildren( List<Location> children ) {
            this.children = children;
        }

        void freeze() {
            if (properties != null) properties = Collections.unmodifiableMap(properties);
            else properties = Collections.emptyMap();
            if (children != null) children = Collections.unmodifiableList(children);
            else children = Collections.emptyList();
        }

        public List<Segment> getChildrenSegments() {
            return getSegments(getChildren());
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Location getLocation() {
            return location;
        }

        public Collection<Property> getProperties() {
            return properties.values();
        }

        public Map<Name, Property> getPropertiesByName() {
            return properties;
        }

        public Property getProperty( Name name ) {
            return properties.get(name);
        }

        public Property getProperty( String nameStr ) {
            Name name = getContext().getValueFactories().getNameFactory().create(nameStr);
            return properties.get(name);
        }

        public List<Location> getChildren() {
            return children;
        }

        public boolean hasChildren() {
            return children.size() != 0;
        }

        public Iterator<Location> iterator() {
            return children.iterator();
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Node) {
                Node that = (Node)obj;
                return this.location.equals(that.getLocation());
            }
            return false;
        }

        @Override
        public String toString() {
            return "Node " + getLocation().toString();
        }

    }

    // ----------------------------------------------------------------------------------------------------------------
    // Subgraph and SubgraphNode implementations
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    class SubgraphResults implements Subgraph {
        private final ReadBranchRequest request;

        SubgraphResults( ReadBranchRequest request ) {
            this.request = request;
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Location getLocation() {
            return request.getActualLocationOfNode();
        }

        public SubgraphNode getRoot() {
            return getNode(getLocation());
        }

        public int getMaximumDepth() {
            return request.maximumDepth();
        }

        public Iterator<SubgraphNode> iterator() {
            final Iterator<Location> iter = request.iterator();
            return new Iterator<SubgraphNode>() {
                public boolean hasNext() {
                    return iter.hasNext();
                }

                public SubgraphNode next() {
                    return getNode(iter.next());
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean includes( Path path ) {
            CheckArg.isNotNull(path, "path");
            path = getAbsolutePath(path);
            return request.includes(path);
        }

        public boolean includes( Location location ) {
            CheckArg.isNotNull(location, "location");
            return request.includes(location);
        }

        public boolean includes( String pathStr ) {
            Path path = createPath(pathStr);
            path = getAbsolutePath(path);
            return includes(path);
        }

        public SubgraphNode getNode( Location location ) {
            if (!location.hasPath()) return null;
            Location actualLocation = request.getLocationFor(location.getPath());
            if (actualLocation == null) return null;
            return new SubgraphNodeImpl(actualLocation, request);
        }

        public SubgraphNode getNode( Path path ) {
            path = getAbsolutePath(path);
            if (!includes(path)) return null;
            Location location = request.getLocationFor(path);
            if (location == null) return null;
            return new SubgraphNodeImpl(location, request);
        }

        public SubgraphNode getNode( String pathStr ) {
            CheckArg.isNotEmpty(pathStr, "path");
            Path path = createPath(pathStr);
            path = getAbsolutePath(path);
            return getNode(path);
        }

        public SubgraphNode getNode( Name relativePath ) {
            Path path = getGraph().getContext()
                                  .getValueFactories()
                                  .getPathFactory()
                                  .create(getLocation().getPath(), relativePath);
            path = path.getNormalizedPath();
            return getNode(path);
        }

        protected Path getAbsolutePath( Path absoluteOrRelative ) {
            Path result = absoluteOrRelative;
            if (!result.isAbsolute()) {
                result = getGraph().getContext().getValueFactories().getPathFactory().create(getLocation().getPath(), result);
                result = result.getNormalizedPath();
            }
            return result;
        }

        @Override
        public int hashCode() {
            return getLocation().hashCode();
        }

        @Override
        public String toString() {
            return "Subgraph " + getLocation().toString();
        }
    }

    protected static final List<Location> NO_CHILDREN = Collections.emptyList();

    @Immutable
    class SubgraphNodeImpl implements SubgraphNode {
        private final Location location;
        private final ReadBranchRequest request;

        SubgraphNodeImpl( Location location,
                          ReadBranchRequest request ) {
            this.location = location;
            this.request = request;
        }

        public DateTime getExpirationTime() {
            return computeExpirationTime(request);
        }

        public List<Location> getChildren() {
            List<Location> children = request.getChildren(location);
            if (children == null) children = NO_CHILDREN;
            return children;
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Location getLocation() {
            return location;
        }

        public Collection<Property> getProperties() {
            return getPropertiesByName().values();
        }

        public Map<Name, Property> getPropertiesByName() {
            return request.getPropertiesFor(location);
        }

        public Property getProperty( Name name ) {
            return getPropertiesByName().get(name);
        }

        public Property getProperty( String nameStr ) {
            Name name = getContext().getValueFactories().getNameFactory().create(nameStr);
            return getPropertiesByName().get(name);
        }

        public boolean hasChildren() {
            return getChildren().size() != 0;
        }

        public List<Segment> getChildrenSegments() {
            return getSegments(getChildren());
        }

        public Iterator<Location> iterator() {
            return getChildren().iterator();
        }

        public SubgraphNode getNode( Name childName ) {
            Path path = getContext().getValueFactories().getPathFactory().create(location.getPath(), childName);
            Location location = request.getLocationFor(path);
            if (location == null) return null;
            return new SubgraphNodeImpl(location, request);
        }

        public SubgraphNode getNode( Path relativePath ) {
            Path path = getContext().getValueFactories().getPathFactory().create(location.getPath(), relativePath);
            path = path.getNormalizedPath();
            Location location = request.getLocationFor(path);
            if (location == null) return null;
            return new SubgraphNodeImpl(location, request);
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Node) {
                Node that = (Node)obj;
                return this.location.equals(that.getLocation());
            }
            return false;
        }

        @Override
        public String toString() {
            return "Node " + getLocation().toString();
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Action Implementations
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    protected abstract class AbstractAction<T> implements Conjunction<T> {
        private final T afterConjunction;

        /*package*/AbstractAction( T afterConjunction ) {
            this.afterConjunction = afterConjunction;
        }

        /*package*/T afterConjunction() {
            return this.afterConjunction;
        }

        public T and() {
            return this.afterConjunction;
        }

        /*package*/Path createPath( String path ) {
            return Graph.this.getContext().getValueFactories().getPathFactory().create(path);
        }

        /*package*/Name createName( String name ) {
            return Graph.this.getContext().getValueFactories().getNameFactory().create(name);
        }
    }

    @NotThreadSafe
    protected abstract class MoveAction<T> extends AbstractAction<T> implements Move<T> {
        private final Locations from;
        private Name newName;

        /*package*/MoveAction( T afterConjunction,
                                Location from ) {
            super(afterConjunction);
            this.from = new Locations(from);
        }

        public Move<T> and( Location from ) {
            this.from.add(from);
            return this;
        }

        public Move<T> and( String from ) {
            this.from.add(Location.create(createPath(from)));
            return this;
        }

        public Move<T> and( Path from ) {
            this.from.add(Location.create(from));
            return this;
        }

        public Move<T> and( Property firstFrom,
                            Property... additionalFroms ) {
            this.from.add(Location.create(firstFrom, additionalFroms));
            return this;
        }

        public Move<T> and( Iterable<Property> idPropertiesFrom ) {
            this.from.add(Location.create(idPropertiesFrom));
            return this;
        }

        public Move<T> and( Property from ) {
            this.from.add(Location.create(from));
            return this;
        }

        public Move<T> and( UUID from ) {
            this.from.add(Location.create(from));
            return this;
        }

        public Into<T> as( Name newName ) {
            this.newName = newName;
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Graph.AsName#as(java.lang.String)
         */
        public Into<T> as( String newName ) {
            return as(createName(newName));
        }

        /**
         * Submit any requests to move the targets into the supplied parent location
         * 
         * @param from the location(s) that are being moved; never null
         * @param into the parent location
         * @param before the location of the child of the parent before which this node should be placed
         * @param newName the new name for the node being moved; may be null
         * @return this object, for method chaining
         */
        protected abstract T submit( Locations from,
                                     Location into,
                                     Location before,
                                     Name newName );

        /**
         * Submit any requests to move the targets into the supplied parent location
         * 
         * @param from the location(s) that are being moved; never null
         * @param into the parent location
         * @param newName the new name for the node being moved; may be null
         * @return this object, for method chaining
         */
        protected T submit( Locations from,
                            Location into,
                            Name newName ) {
            return submit(from, into, null, newName);
        }

        public T into( Location into ) {
            return submit(from, into, null, newName);
        }

        public T into( Path into ) {
            return submit(from, Location.create(into), newName);
        }

        public T into( UUID into ) {
            return submit(from, Location.create(into), newName);
        }

        public T into( Property firstIdProperty,
                       Property... additionalIdProperties ) {
            return submit(from, Location.create(firstIdProperty, additionalIdProperties), newName);
        }

        public T into( Property into ) {
            return submit(from, Location.create(into), newName);
        }

        public T into( String into ) {
            return submit(from, Location.create(createPath(into)), newName);
        }

        public T before( Location before ) {
            return submit(from, null, before, newName);
        }

        public T before( Path before ) {
            return submit(from, null, Location.create(before), newName);
        }

        public T before( UUID before ) {
            return submit(from, null, Location.create(before), newName);
        }

        public T before( Property firstIdProperty,
                         Property... additionalIdProperties ) {
            return submit(from, null, Location.create(firstIdProperty, additionalIdProperties), newName);
        }

        public T before( Property before ) {
            return submit(from, null, Location.create(before), newName);
        }

        public T before( String before ) {
            return submit(from, null, Location.create(createPath(before)), newName);
        }
    }

    @NotThreadSafe
    protected abstract class CopyAction<T> extends AbstractAction<T> implements Copy<T> {
        protected Locations from;
        protected String fromWorkspaceName;

        /*package*/CopyAction( T afterConjunction,
                                Location from ) {
            super(afterConjunction);
            this.from = new Locations(from);
            this.fromWorkspaceName = Graph.this.getCurrentWorkspaceName();
        }

        public Copy<T> and( Location from ) {
            this.from.add(from);
            return this;
        }

        public Copy<T> and( String from ) {
            this.from.add(Location.create(createPath(from)));
            return this;
        }

        public Copy<T> and( Path from ) {
            this.from.add(Location.create(from));
            return this;
        }

        public Copy<T> and( Property firstFrom,
                            Property... additionalFroms ) {
            this.from.add(Location.create(firstFrom, additionalFroms));
            return this;
        }

        public Copy<T> and( Iterable<Property> idProperties ) {
            this.from.add(Location.create(idProperties));
            return this;
        }

        public Copy<T> and( Property from ) {
            this.from.add(Location.create(from));
            return this;
        }

        public Copy<T> and( UUID from ) {
            this.from.add(Location.create(from));
            return this;
        }

        /**
         * Submit any requests to move the targets into the supplied parent location
         * 
         * @param fromWorkspaceName the name of the workspace containing the {@code from} locations
         * @param from the locations that are being copied
         * @param into the parent location
         * @param nameForCopy the name that should be used for the copy, or null if the name should be the same as the original
         * @return this object, for method chaining
         */
        protected abstract T submit( String fromWorkspaceName,
                                     Locations from,
                                     Location into,
                                     Name nameForCopy );

        public CopyTarget<T> fromWorkspace( String workspaceName ) {
            this.fromWorkspaceName = workspaceName;

            return this;
        }

        public T into( Location into ) {
            return submit(this.fromWorkspaceName, this.from, into, null);
        }

        public T into( Path into ) {
            return submit(this.fromWorkspaceName, this.from, Location.create(into), null);
        }

        public T into( UUID into ) {
            return submit(this.fromWorkspaceName, this.from, Location.create(into), null);
        }

        public T into( Property firstIdProperty,
                       Property... additionalIdProperties ) {
            return submit(this.fromWorkspaceName, this.from, Location.create(firstIdProperty, additionalIdProperties), null);
        }

        public T into( Property into ) {
            return submit(this.fromWorkspaceName, this.from, Location.create(into), null);
        }

        public T into( String into ) {
            return submit(this.fromWorkspaceName, this.from, Location.create(createPath(into)), null);
        }

        public T to( Location desiredLocation ) {
            if (!desiredLocation.hasPath()) {
                throw new IllegalArgumentException(GraphI18n.unableToCopyToLocationWithoutAPath.text(this.from, desiredLocation));
            }
            Path desiredPath = desiredLocation.getPath();
            if (desiredPath.isRoot()) {
                throw new IllegalArgumentException(GraphI18n.unableToCopyToTheRoot.text(this.from, desiredLocation));
            }
            Path parent = desiredPath.getParent();
            return submit(this.fromWorkspaceName, this.from, Location.create(parent), desiredPath.getLastSegment().getName());
        }

        public T to( Path desiredPath ) {
            if (desiredPath.isRoot()) {
                throw new IllegalArgumentException(GraphI18n.unableToCopyToTheRoot.text(this.from, desiredPath));
            }
            Path parent = desiredPath.getParent();
            return submit(this.fromWorkspaceName, this.from, Location.create(parent), desiredPath.getLastSegment().getName());
        }

        public T to( String desiredPath ) {
            return to(createPath(desiredPath));
        }
    }

    @NotThreadSafe
    public abstract class CloneAction<T> extends AbstractAction<T> implements Clone<T> {
        private final Location from;

        /*package*/CloneAction( T afterConjunction,
                                 Location from ) {
            super(afterConjunction);
            this.from = from;
        }

        protected abstract T submit( String fromWorkspaceName,
                                     Location from,
                                     String intoWorkspaceName,
                                     Location into,
                                     Name desiredName,
                                     Segment desiredSegment,
                                     boolean removeExisting );

        public AsChild<Into<WithUuids<T>>> fromWorkspace( final String workspaceName ) {
            final CloneAction<T> source = this;
            return new AsChild<Into<WithUuids<T>>>() {
                public Into<WithUuids<T>> as( final Name name ) {
                    return new CloneTargetAction<T>(afterConjunction(), source) {
                        @Override
                        protected T submit( Location into,
                                            boolean removeExisting ) {
                            String intoWorkspaceName = getCurrentWorkspaceName();
                            return source.submit(workspaceName, from, intoWorkspaceName, into, name, null, removeExisting);
                        }
                    };
                }

                public Into<WithUuids<T>> as( final Segment segment ) {
                    return new CloneTargetAction<T>(afterConjunction(), source) {
                        @Override
                        protected T submit( Location into,
                                            boolean removeExisting ) {
                            String intoWorkspaceName = getCurrentWorkspaceName();
                            return source.submit(workspaceName, from, intoWorkspaceName, into, null, segment, removeExisting);
                        }
                    };
                }

            };
        }
    }

    @NotThreadSafe
    public abstract class CloneTargetAction<T> extends AbstractAction<T> implements Into<WithUuids<T>> {
        protected final CloneAction<T> source;

        /*package*/CloneTargetAction( T afterConjunction,
                                       CloneAction<T> source ) {
            super(afterConjunction);
            this.source = source;
        }

        protected abstract T submit( Location into,
                                     boolean removeExisting );

        public WithUuids<T> into( final Location into ) {
            return new WithUuids<T>() {
                public T failingIfAnyUuidsMatch() {
                    submit(into, false);
                    return and();
                }

                public T replacingExistingNodesWithSameUuids() {
                    submit(into, true);
                    return and();

                }
            };
        }

        public WithUuids<T> into( Path into ) {
            return into(Location.create(into));
        }

        public WithUuids<T> into( UUID into ) {
            return into(Location.create(into));
        }

        public WithUuids<T> into( Property firstIdProperty,
                                  Property... additionalIdProperties ) {
            return into(Location.create(firstIdProperty, additionalIdProperties));
        }

        public WithUuids<T> into( Property into ) {
            return into(Location.create(into));
        }

        public WithUuids<T> into( String into ) {
            return into(Location.create(createPath(into)));
        }
    }

    @NotThreadSafe
    protected abstract class CreateAction<T> extends AbstractAction<T> implements Create<T> {
        private final String workspaceName;
        private final Location parent;
        private final Name childName;
        private final Map<Name, Property> properties = new HashMap<Name, Property>();
        private boolean submitted = false;
        private NodeConflictBehavior conflictBehavior = NodeConflictBehavior.APPEND;

        /*package*/CreateAction( T afterConjunction,
                                  Location parent,
                                  String workspaceName,
                                  Name childName ) {
            super(afterConjunction);
            this.parent = parent;
            this.workspaceName = workspaceName;
            this.childName = childName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Graph.Create#ifAbsent()
         */
        public CreateAction<T> ifAbsent() {
            conflictBehavior = NodeConflictBehavior.DO_NOT_REPLACE;
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Graph.Create#orReplace()
         */
        public CreateAction<T> orReplace() {
            conflictBehavior = NodeConflictBehavior.REPLACE;
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Graph.Create#orUpdate()
         */
        public CreateAction<T> orUpdate() {
            conflictBehavior = NodeConflictBehavior.UPDATE;
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Graph.Create#byAppending()
         */
        public CreateAction<T> byAppending() {
            conflictBehavior = NodeConflictBehavior.APPEND;
            return this;
        }

        public Create<T> and( UUID uuid ) {
            PropertyFactory factory = getContext().getPropertyFactory();
            properties.put(DnaLexicon.UUID, factory.create(DnaLexicon.UUID, uuid));
            return this;
        }

        public Create<T> and( Property property ) {
            properties.put(property.getName(), property);
            return this;
        }

        public Create<T> and( Iterable<Property> properties ) {
            for (Property property : properties) {
                this.properties.put(property.getName(), property);
            }
            return this;
        }

        public Create<T> and( String name,
                              Object... values ) {
            ExecutionContext context = getContext();
            PropertyFactory factory = context.getPropertyFactory();
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            Name propertyName = nameFactory.create(name);
            properties.put(propertyName, factory.create(propertyName, values));
            return this;
        }

        public Create<T> and( Name name,
                              Object... values ) {
            PropertyFactory factory = getContext().getPropertyFactory();
            properties.put(name, factory.create(name, values));
            return this;
        }

        public Create<T> and( Property property,
                              Property... additionalProperties ) {
            properties.put(property.getName(), property);
            for (Property additionalProperty : additionalProperties) {
                properties.put(additionalProperty.getName(), additionalProperty);
            }
            return this;
        }

        public Create<T> with( UUID uuid ) {
            return and(uuid);
        }

        public Create<T> with( Property property ) {
            return and(property);
        }

        public Create<T> with( Iterable<Property> properties ) {
            return and(properties);
        }

        public Create<T> with( Property property,
                               Property... additionalProperties ) {
            return and(property, additionalProperties);
        }

        public Create<T> with( String name,
                               Object... values ) {
            return and(name, values);
        }

        public Create<T> with( Name name,
                               Object... values ) {
            return and(name, values);
        }

        protected abstract T submit( Location parent,
                                     String workspaceName,
                                     Name childName,
                                     Collection<Property> properties,
                                     NodeConflictBehavior conflictBehavior );

        @Override
        public T and() {
            if (!submitted) {
                submit(parent, workspaceName, childName, this.properties.values(), this.conflictBehavior);
                submitted = true;
            }
            return super.and();
        }
    }

    @NotThreadSafe
    protected abstract class CreateNodeNamedAction<T> extends AbstractAction<T> implements CreateNodeNamed<T> {
        private final Location parent;

        protected CreateNodeNamedAction( T afterConjunction,
                                         Location parent ) {
            super(afterConjunction);
            this.parent = parent;
        }

        public CreateAction<T> nodeNamed( String name ) {
            NameFactory factory = getContext().getValueFactories().getNameFactory();
            Name nameObj = factory.create(name);
            return createWith(afterConjunction(), parent, nameObj);
        }

        public CreateAction<T> nodeNamed( Name name ) {
            return createWith(afterConjunction(), parent, name);
        }

        protected abstract CreateAction<T> createWith( T afterConjunction,
                                                       Location parent,
                                                       Name nodeName );
    }

    @Immutable
    protected static final class GraphWorkspace implements Workspace {
        private final String name;
        private final Location root;

        GraphWorkspace( String name,
                        Location root ) {
            assert name != null;
            assert root != null;
            this.name = name;
            this.root = root;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Workspace#getName()
         */
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.Workspace#getRoot()
         */
        public Location getRoot() {
            return root;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof GraphWorkspace) {
                GraphWorkspace that = (GraphWorkspace)obj;
                if (!this.getName().equals(that.getName())) return false;
                // all root nodes should be equivalent, so no need to check
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Workspace \"" + this.name + "\" (root = " + this.root + " )";
        }
    }

    /**
     * A set of nodes returned from a {@link Graph graph}, with methods to access the properties and children of the nodes in the
     * result. The {@link #iterator()} method can be used to iterate all over the nodes in the result.
     * 
     * @author Randall Hauch
     * @param <NodeType> the type of node that tis results deals with
     */
    @Immutable
    public interface BaseResults<NodeType extends Node> extends Iterable<NodeType> {

        /**
         * Get the graph containing the node.
         * 
         * @return the graph
         */
        Graph getGraph();

        /**
         * Get the node at the supplied location.
         * 
         * @param path the path of the node in these results
         * @return the node, or null if the node is not {@link #includes(Path) included} in these results
         */
        NodeType getNode( String path );

        /**
         * Get the node at the supplied location.
         * 
         * @param path the path of the node in these results
         * @return the node, or null if the node is not {@link #includes(Path) included} in these results
         */
        NodeType getNode( Path path );

        /**
         * Get the node at the supplied location.
         * 
         * @param location the location of the node
         * @return the node, or null if the node is not {@link #includes(Path) included} in these results
         */
        NodeType getNode( Location location );

        /**
         * Return whether these results include a node at the supplied location.
         * 
         * @param path the path of the node in these results
         * @return true if this subgraph includes the supplied location, or false otherwise
         */
        boolean includes( String path );

        /**
         * Return whether this subgraph has a node at the supplied location.
         * 
         * @param path the path of the node in these results
         * @return true if these results includes the supplied location, or false otherwise
         */
        boolean includes( Path path );

        /**
         * Return whether this subgraph has a node at the supplied location.
         * 
         * @param location the location of the node in these results
         * @return true if these results includes the supplied location, or false otherwise
         */
        boolean includes( Location location );

    }
}
