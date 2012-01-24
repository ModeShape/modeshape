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
package org.modeshape.jcr.security;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.ModeShapePermissions;
import org.modeshape.jcr.value.Path;

/**
 * An interface that can authorize access to specific resources within repositories.
 */
public interface AuthorizationProvider {

    /**
     * Determine if the supplied execution context has permission for all of the named actions in the named workspace. If not all
     * actions are allowed, the method returns false.
     * 
     * @param context the context in which the subject is performing the actions on the supplied workspace
     * @param repositoryName the name of the repository containing the workspace content
     * @param repositorySourceName <i>This is no longer used and will always be the same as the repositoryName</i>
     * @param workspaceName the name of the workspace in which the path exists
     * @param path the path on which the actions are occurring
     * @param actions the list of {@link ModeShapePermissions actions} to check
     * @return true if the subject has privilege to perform all of the named actions on the content at the supplied path in the
     *         given workspace within the repository, or false otherwise
     */
    boolean hasPermission( ExecutionContext context,
                           String repositoryName,
                           String repositorySourceName,
                           String workspaceName,
                           Path path,
                           String... actions );
}
