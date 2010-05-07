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
 * The {@link MapRepository} class and its supporting classes provide a default implementation of the connector
 * classes for connectors that support the transient or persistent mapping of a UUID to a {@link MapNode standard
 * representation of a node}.
 * To implement a connector based on this framework, one must create an implementation of {@link MapRepositorySource the repository source},
 * an implementation of {@link MapRepository the repository itself}, and an implementation of {@link MapWorkspace the workspace}.
 * <p>
 * The {@link MapRepositorySource repository source implementation} contains properties for the repository configuration and caching policies.  A key
 * method in the {@code MapRepositorySource} implementation if the {@link org.modeshape.graph.connector.RepositorySource#getConnection()} method,
 * which should generally be implemented using the {@link MapRepositoryConnection default connection implementation}.
 * <pre>
 * if (repository == null) {
 *  repository = new InMemoryRepository(name, rootNodeUuid, defaultWorkspaceName);
 * }
 * return new MapRepositoryConnection(this, repository); 
 * </pre>
 * </p>
 * <p>
 * The {@link MapRepository repository implementation} is only required to provide an implementation of the {@link MapRepository#createWorkspace(org.modeshape.graph.ExecutionContext, String)}
 * method that returns the appropriate {@link MapWorkspace workspace} implementation for the connector.  However, all constructors for the repository must 
 * call {@link MapRepository#initialize()} after the constructor has completed its initialization, as demonstrated below:
 * <pre>
 * public InMemoryRepository( String sourceName,
 *                            UUID rootNodeUuid,
 *                            String defaultWorkspaceName ) {
 *   initialize();
 * }
 * </pre>
 * </p>
 * <p>
 * Finally, the {@link MapWorkspace workspace implementation} must be created.  Implementors should consider extending the {@link AbstractMapWorkspace} class, which provides reasonable default implementations (assuming that the backing map provides O(1) lookups - a sine qua non for maps) for almost
 * this class imposes a requirement that its {@link AbstractMapWorkspace#initialize()} method also be called at the end of each constructor, like so:
 * <pre>
 *  public Workspace( MapRepository repository,
 *                    String name ) {
 *      super(repository, name);
 *
 *      initialize();
 *  }
 * </pre>
 * </p>
 * 
 * @see org.modeshape.graph.connector.inmemory.InMemoryRepositorySource
 */

package org.modeshape.graph.connector.map;

