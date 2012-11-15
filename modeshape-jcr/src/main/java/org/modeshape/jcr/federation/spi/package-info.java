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
 * This package provides a Service Provider Interface (SPI) for connectors to external systems. The ModeShape federation system
 * uses these {@link org.modeshape.jcr.federation.spi.Connector} implementations to enable a repository to access external systems
 * and project the external information as structured nodes within the repository. External sources are configured in the 
 * {@link org.modeshape.jcr.RepositoryConfiguration repository configuration JSON file} to use a specific 
 * {@link org.modeshape.jcr.federation.spi.Connector} implementation (including connector-specific configuration properties), 
 * while the projections that define how and where the external content is bound into the repository content are created using the 
 * {@link org.modeshape.jcr.api.federation.FederationManager}:
 * <pre>
 * javax.jcr.Session jcrSession = ...
 * Session session = (org.modeshape.jcr.api.Session)jcrSession;
 * FederationManager fedMgr = session.getWorkspace().getFederationManager();
 * 
 * javax.jcr.Node parentNode = ... // the parent of the federated content
 * String parentPath = parentNode.getPath();
 * String sourceName = ... // the name of the external source
 * String externalPath = ... // the path of the node in the external source that is to appear as a child of 'parentNode'
 * String alias = ... // Optional alias for the external node
 * fedMgr.createExternalProjection(parentPath, sourceName, externalPath, alias);
 * </pre>
 * <p>
 * To create a custom connector, simply create a {@link org.modeshape.jcr.federation.spi.Connector} subclass
 * and implement the necessary methods. If your connector is only to read information and never will update any external content,
 * you can instead subclass the {@link org.modeshape.jcr.federation.spi.ReadOnlyConnector} class, which implements the methods
 * used to create/update/delete content by throwing the proper exception.
 * </p>
 */

package org.modeshape.jcr.federation.spi;


