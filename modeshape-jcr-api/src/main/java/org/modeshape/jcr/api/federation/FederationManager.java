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
     *
     * @throws RepositoryException if the repository cannot perform the operation.
     *
     */
    public void createExternalProjection( String absNodePath,
                                          String sourceName,
                                          String externalPath,
                                          String alias) throws RepositoryException;
}
