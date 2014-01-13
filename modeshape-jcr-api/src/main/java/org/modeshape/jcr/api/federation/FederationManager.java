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
package org.modeshape.jcr.api.federation;

import javax.jcr.RepositoryException;

/**
 * The federation manager object which provides the entry point into ModeShape's federation system, allowing clients to create
 * federated nodes.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface FederationManager {

    /**
     * Creates an external projection by linking an internal node with an external node, from a given source using an optional alias.
     * If this is the first node linked to the existing node, it will convert the existing node to a federated node.
     *
     * @param absNodePath a {@code non-null} string representing the absolute path to an existing internal node.
     * @param sourceName a {@code non-null} string representing the name of an external source, configured in the repository.
     * @param externalPath a {@code non-null} string representing a path in the external source, where at which there is an external
     * node that will be linked.
     * @param alias an optional string representing the name under which the alias should be created. If not present, the {@code externalPath}
     * will be used as the name of the alias.
     * @throws RepositoryException if the repository cannot perform the operation.
     */
    public void createProjection( String absNodePath,
                                  String sourceName,
                                  String externalPath,
                                  String alias ) throws RepositoryException;

    /**
     * Removes a projection located at the given path, in the workspace which was used to get the federation manager.
     * <p>
     * A projection path has the form: [repositoryPath]/[projection alias] as created via
     * {@link #createProjection(String, String, String, String)}
     * </p>
     *
     * @param projectionPath a {@code non-null} String representing the path to a projection
     * @throws IllegalArgumentException if the projection path does not represent a valid path
     * @throws javax.jcr.PathNotFoundException if either the repository path or the projection alias are not valid
     * @throws RepositoryException if anything unexpected fails
     */
    public void removeProjection( String projectionPath ) throws RepositoryException;
}
