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
package org.modeshape.jcr.api;

import java.io.File;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;

/**
 * A <code>RepositoryManager</code> object represents a management view of the Session's Repository instance. Each
 * <code>RepositoryManager</code> object is associated one-to-one with a <code>Session</code> object and is defined by the
 * authorization settings of that session object.
 * <p>
 * The <code>RepositoryManager</code> object can be acquired using a {@link Session} by calling
 * <code>Session.getWorkspace().getRepositoryManager()</code> on a session object. Likewise, the Repository being managed can be
 * found for a given RepositoryManager object by calling <code>mgr.getWorkspace().getSession().getRepository()</code>.
 * </p>
 * 
 * @since 3.0
 */
public interface RepositoryManager {

    /**
     * Return the <code>Workspace</code> object through which this repository manager was created.
     * 
     * @return the @ Workspace} object.
     */
    Workspace getWorkspace();

    /**
     * A <code>RepositoryMonitor</code> object represents a monitoring view of the Session's Repository instance. This is useful
     * for applications that embed a JCR repository and need a way to monitor the health, status and performance of that
     * Repository instance. Each <code>RepositoryMonitor</code> object is associated one-to-one with a <code>Session</code> object
     * and is defined by the authorization settings of that session object.
     * <p>
     * The <code>RepositoryMonitor</code> object can be acquired using a {@link Session} by calling
     * <code>Session.getWorkspace().getRepositoryMonitor()</code> on a session object.
     * </p>
     * 
     * @return the repository monitor; never null
     * @throws RepositoryException if there is a problem obtaining the monitory
     */
    RepositoryMonitor getRepositoryMonitor() throws RepositoryException;

    /**
     * Begin a backup operation of the entire repository, writing the files associated with the backup to the specified directory
     * on the local file system.
     * <p>
     * The repository must be active when this operation is invoked, and it can continue to be used during backup (e.g., this can
     * be a "live" backup operation), but this is not recommended if the backup will be used as part of a migration to a different
     * version of ModeShape or to different installation.
     * </p>
     * <p>
     * Multiple backup operations can operate at the same time, so it is the responsibility of the caller to not overload the
     * repository with backup operations.
     * </p>
     * 
     * @param backupDirectory the directory on the local file system into which all backup files will be written; this directory
     *        need not exist, but the process must have write privilege for this directory
     * @return the problems that occurred during the backup operation
     * @throws AccessDeniedException if the current session does not have sufficient privileges to perform the backup
     * @throws RepositoryException if the backup cannot be run
     */
    Problems backupRepository( File backupDirectory ) throws RepositoryException;

    /**
     * Begin a restore operation of the entire repository, reading the backup files in the specified directory on the local file
     * system. Upon completion of the restore operation, the repository will be restarted automatically.
     * <p>
     * The repository must be active when this operation is invoked. However, the repository <em>may not</em> be used by any other
     * activities during the restore operation; doing so will likely result in a corrupt repository.
     * </p>
     * <p>
     * It is the responsibility of the caller to ensure that this method is only invoked once; calling multiple times wil lead to
     * a corrupt repository.
     * </p>
     * 
     * @param backupDirectory the directory on the local file system in which all backup files exist and were written by a
     *        previous {@link #backupRepository(File) backup operation}; this directory must exist, and the process must have read
     *        privilege for all contents in this directory
     * @return the problems that occurred during the restore operation
     * @throws AccessDeniedException if the current session does not have sufficient privileges to perform the restore
     * @throws RepositoryException if the restoration cannot be run
     */
    Problems restoreRepository( File backupDirectory ) throws RepositoryException;

}
