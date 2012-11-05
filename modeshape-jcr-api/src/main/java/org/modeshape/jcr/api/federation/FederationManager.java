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
     * Appends an external node at the given location to an existing node. If this is the first node appended to the existing
     * node, it will convert the existing node to a federated node.
     *
     * @param absNodePath a {@code non-null} string representing the absolute path to an existing node.
     * @param sourceName a {@code non-null} string representing the name of an external source, configured in the repository.
     * @param externalLocation {@code non-null} string representing a path in the external source, where the external
     * node is expected to be located at.
     * @param filters an optional array of filters.
     *
     * @throws RepositoryException if the repository cannot perform the operation.
     * TODO author=Horia Chiorean date=11/2/12 description=Define filters format
     */
    public void linkExternalLocation( String absNodePath,
                                      String sourceName,
                                      String externalLocation,
                                      String... filters ) throws RepositoryException;
}
