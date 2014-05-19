/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * This package provides a Service Provider Interface (SPI) for connectors to external systems. The ModeShape federation system
 * uses these {@link org.modeshape.jcr.spi.federation.Connector} implementations to enable a repository to access external systems
 * and project the external information as structured nodes within the repository. External sources are configured in the 
 * {@link org.modeshape.jcr.RepositoryConfiguration repository configuration JSON file} to use a specific 
 * {@link org.modeshape.jcr.spi.federation.Connector} implementation (including connector-specific configuration properties),
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
 * To create a custom connector, simply create a {@link org.modeshape.jcr.spi.federation.Connector} subclass
 * and implement the necessary methods. If your connector is only to read information and never will update any external content,
 * you can instead subclass the {@link org.modeshape.jcr.spi.federation.ReadOnlyConnector} class, which implements the methods
 * used to create/update/delete content by throwing the proper exception.
 * </p>
 */

package org.modeshape.jcr.spi.federation;


