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
/**
 * This package defines a series of classes that can serve as base classes for a connector implementation.
 * The sources that are accessed by connectors organize their data in various ways, so the connector API doesn't
 * require any particular implementation. However, since many connectors can be implemented in similar ways and often
 * do similar kinds of operations, this package provides a number of concrete and abstract classes that reduce
 * the amount of work required to implement a fully-capable connector. Of course, these classes won't work in
 * all situations not may it be as optimized as when writing a completely dedicated connector. But when you
 * can use them, they make writing a connector much easier and faster.
 * <p></p>
 * <h3>Foundation</h3>
 * <p>
 * The main class for a connector is a concrete implementation of the {@link org.modeshape.graph.connector.RepositorySource}
 * interface. This subclass defines JavaBean properties that can be easily configured, and then implements the
 * {@link org.modeshape.graph.connector.RepositorySource#getConnection()} method to return a 
 * {@link org.modeshape.graph.connector.RepositoryConnection}.
 * </p>
 * <p>
 * Providing an abstract class for RepositorySource actually wouldn't provide much benefit, so you still have to
 * provide your own implementation. Along with defining the various JavaBean properties, your class should
 * do two things each time the 
 * {@link org.modeshape.graph.connector.RepositorySource#getConnection() getConnection()} method is called:
 * <ol>
 * <li>Instantiate and manage a single transient {@link Repository} instance (or reuse if already created); and</li>
 * <li>Create and return a new {@link Connection} instance.</li>
 * </ol>
 * However, this framework does define a {@link BaseRepositorySource} interface that extend the 
 * {@link org.modeshape.graph.connector.RepositorySource} interface with a few additional
 * methods needed by this framework.
 * </p>
 * <p>
 * The {@link Connection} class is a concrete implementation of {@link org.modeshape.graph.connector.RepositoryConnection}
 * that {@link org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext, org.modeshape.graph.request.Request) executes}
 * the requests by creating a {@link Transaction}, using a fully-implemented {@link Processor} to process all of the requests,
 * and either {@link Transaction#commit() commits} or {@link Transaction#rollback() rolls back} the transaction if
 * all requests succeeded or if any failed, respectively. 
 * </p>
 * <p>
 * You can use the concrete {@link Connection} class as is, but you will have to write a concrete {@link Transaction}
 * implementation since that is where all of the source-specific logic goes. This package does offer a couple of 
 * different specializations of Transaction that may fit how your connector stores its data (see below).
 * </p>
 * <p>
 * The {@link Repository} class manages a set of named {@link Workspace} objects, but it is responsible for 
 * {@link Repository#startTransaction(org.modeshape.graph.ExecutionContext, boolean) startTransaction(...) creating the Transaction}
 * objects. Thus, Repository is an abstract class, so you must create a concrete subclass an instantiate it in
 * your BaseRepositorySource's {@link BaseRepositorySource#getConnection() getConnection()} method.
 * </p>
 * <p>
 * To summarize, this connector foundation defines the following concepts:
 * <ul>
 * <li>Define a {@link Node} implementation class that represents a graph node.</li>
 * <li>Define a {@link Workspace} implementation class that represents each named workspace in your source.</li>
 * <li>Define a concrete {@link BaseRepositorySource} subclass that manages a single
 * {@link Repository} instance and create {@link Connection} objects.</li>
 * <li>Define a {@link Repository} subclass that manages the {@link Workspace} objects and create {@link Transaction}
 * objects for use by the {@link Connection}.</li>
 * <li>Define a {@link Transaction} class that implements all of the source-specific logic for interacting with the underlying source
 * of data.</li>
 * </ul>
 * Of course, before you do this, see if your source fits one of the patterns described below. If it does, the procedure
 * is a little different and will be less work.
 * </p>
 * <h3>Sources with hierarchical content</h3>
 * <p>
 * Many sources store their content in a hierarchical manner. For example, consider file systems, version control systems,
 * directories, repositories, and registries all organize their content using a folder-like construct that relies upon 
 * the path from the entry point down to the desired location. This framework provides a number of base classes
 * that can be used to easily create a path-oriented connector.
 * </p>
 * <p>
 * </p>
 * <h3>Sources with content keyed by UUID</h3>
 * <p>
 * Other sources store their content based not upon a path but rather using a unique key for each bit of information.
 * For example, consider data grids and maps all store content using a unique key. This framework
 * provides several base classes that can be used to easily create a UUID-based connector.
 * </p>
 * <p>
 * {@link MapNode} is a serializable representation of a node's properties and child references. Each MapNode
 * has a {@link MapNode#getUuid() UUID}, and thus all child references are managed as UUIDs. MapNode instances
 * are stored in a {@link MapWorkspace} that can be thought of as a wrapper around a {@link java.util.Map Map}-like
 * data structure. The {@link MapTransaction} is an abstract class that implements all the Map-oriented operations,
 * creates MapWorkspace objects when needed, and tracks changes to the nodes that are either committed or rolled back
 * by the Connection.
 * </p>
 * <p>
 * A {@link StandardMapWorkspace} is provided to simplify working with a real underlying {@link java.util.Map}
 * where all nodes are stored. If your source does not have a real Map interface, simply subclass {@link MapWorkspace}
 * to define how to {@link MapWorkspace#getNode(java.util.UUID) get} and {@link MapWorkspace#putNode(MapNode) put}
 * nodes into the source's representation of the workspace.
 * </p>
 * <p>
 * To summarize, if your source actually provides a {@link java.util.Map} implementation, you will:
 * <ul>
 * <li>Use directly or create a subclass of {@link MapNode}.</li>
 * <li>Define a {@link MapWorkspace MapWorkspace&lt;YourMapNode>} class that represents each named workspace in your source,
 * or simply reuse {@link StandardMapWorkspace StandardMapWorkspace&lt;YourMapNode>} if your source uses a {@link java.util.Map}.</li>
 * <li>Define a {@link Transaction MapTransaction&lt;YourMapNode,YourMapWorkspace>} concrete subclass that implements all 
 * of the source-specific logic for interacting with the underlying source of data.</li>
 * <li>Define a {@link Repository Repository&lt;YourMapNode,YourMapWorkspace>} subclass that manages the 
 * {@link MapWorkspace MapWorkspace&lt;YourMapNode>} objects and create {@link Transaction MapTransaction&lt;YourMapNode,YourMapWorkspace>}
 * objects for use by the {@link Connection}.</li>
 * <li>Define a concrete {@link BaseRepositorySource} subclass that manages a single
 * {@link Repository Repository&lt;YourMapNode,YourMapWorkspace>} instance and create {@link Connection} objects.</li>
 * </ul>
 * </p>
 * 
 * @see org.modeshape.graph.connector.inmemory.InMemoryRepositorySource
 */

package org.modeshape.graph.connector.base;

