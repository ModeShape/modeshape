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
package org.modeshape.graph.connector.federation;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;

/**
 * Interface for the components that compute the {@link ProjectedNode} for a given location. Implementations may be provided for
 * different configurations of {@link FederatedWorkspace#getProjections() projections}.
 */
@Immutable
interface Projector {

    /**
     * Project the supplied location in the federated repository into the equivalent projected node(s).
     * 
     * @param context the execution context in which the content is being accessed; may not be null
     * @param location the location in the federated repository; may not be null
     * @param requiresUpdate true if the operation for which this projection is needed will update the content in some way, or
     *        false if read-only operations will be performed
     * @return the projected node, or null if the node does not exist in any projection or if the operation requires update and no
     *         writable projection applies
     */
    ProjectedNode project( ExecutionContext context,
                           Location location,
                           boolean requiresUpdate );
}
