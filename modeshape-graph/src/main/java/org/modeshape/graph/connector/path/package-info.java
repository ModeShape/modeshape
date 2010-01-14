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
 * The {@link PathRepository} class and its supporting classes provide a default read-only implementation of the connector
 * classes for connectors that only support path-based access to a {@link PathNode standard
 * representation of a node}.  Connectors to systems that provide a unique identifier for each node would generally be better implemented using the {@link org.modeshape.graph.connector.map.MapRepository map repository implementation instead}.
 * To implement a connector based on this framework, one must create an implementation of {@link PathRepositorySource the repository source},
 * an implementation of {@link PathRepository the repository itself}, and an implementation of {@link PathWorkspace the workspace}.
 * <p>
 * The {@link PathRepositorySource repository source implementation} contains properties for the repository configuration and caching policies.  A key
 * method in the {@code PathRepositorySource} implementation if the {@link org.modeshape.graph.connector.RepositorySource#getConnection()} method,
 * which should generally be implemented using the {@link PathRepositoryConnection default connection implementation}.
 * <pre>
 * if (repository == null) {
 *  repository = new JdbcMetadataRepository(this);
 * }
 * return new MapRepositoryConnection(this, repository); 
 * </pre>
 * </p>
 * <p>
 * The {@link PathRepository repository implementation} is only required to provide an implementation of the {@link PathRepository#initialize()}
 * method to initialize the repository with a default {@link PathWorkspace workspace} implementation for the connector and an implementation of {@link PathWorkspace}.  All constructors for the repository must 
 * call {@link PathRepository#initialize()} after the constructor has completed its initialization, as demonstrated below:
 * <pre>
 * public JdbcMetadataRepository( JdbcMetadataSource source ) {
 *   initialize();
 * }
 * </pre>
 * </p>
 */

package org.modeshape.graph.connector.path;

