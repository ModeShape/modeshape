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
package org.modeshape.graph.connector;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;

/**
 * Exception that indicates that a lock request failed at the repository level. This may be due to a conflict with an existing
 * lock, an internal timeout that occurred while attempting to lock the target, a lack of permissions, or any other reason.
 */
@Immutable
public class LockFailedException extends RepositorySourceException {

    private static final long serialVersionUID = 1L;

    public LockFailedException( String repositorySourceName,
                                Location target,
                                String workspaceName,
                                Throwable cause ) {
        super(repositorySourceName, GraphI18n.couldNotAcquireLock.text(target, workspaceName), cause);
    }

}
