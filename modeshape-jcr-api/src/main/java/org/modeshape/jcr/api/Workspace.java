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

import java.util.concurrent.Future;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.query.QueryManager;

/**
 * An extension of JCR 2.0's {@link javax.jcr.Workspace} interface, with a few ModeShape-specific enhancements.
 */
public interface Workspace extends javax.jcr.Workspace {

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the ModeShape-specific specialization of the standard {@link javax.jcr.nodetype.NodeTypeManager}
     * interface.
     * </p>
     */
    @Override
    NodeTypeManager getNodeTypeManager() throws RepositoryException;

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the ModeShape-specific specialization of the standard {@link javax.jcr.query.QueryManager} interface.
     * </p>
     */
    @Override
    public QueryManager getQueryManager() throws RepositoryException;

    /**
     * Return a {@link RepositoryManager} that can be used to administer the Repository instance through which this workspace's
     * session was acquired.
     * 
     * @return the {@link RepositoryManager} instance.
     * @throws AccessDeniedException if the caller does not have authorization to obtain the manager.
     * @throws RepositoryException if another error occurred.
     * @since 3.0
     */
    RepositoryManager getRepositoryManager() throws AccessDeniedException, RepositoryException;

    /**
     * Crawl and re-index the content in this workspace. This method blocks until the indexing is completed.
     * 
     * @throws AccessDeniedException if the session does not have the privileges to reindex the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindexAsync()
     * @see #reindexAsync(String)
     * @see #reindex(String)
     */
    void reindex() throws RepositoryException;

    /**
     * Crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     * 
     * @param path the path of the content to be indexed
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws AccessDeniedException if the session does not have the privileges to reindex this part of the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindexAsync()
     * @see #reindexAsync(String)
     */
    void reindex( String path ) throws RepositoryException;

    /**
     * Asynchronously crawl and re-index the content in this workspace.
     * 
     * @return a future representing the asynchronous operation; never null
     * @throws AccessDeniedException if the session does not have the privileges to reindex the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindex(String)
     * @see #reindexAsync(String)
     */
    Future<Boolean> reindexAsync() throws RepositoryException;

    /**
     * Asynchronously crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     * 
     * @param path the path of the content to be indexed
     * @return a future representing the asynchronous operation; never null
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws AccessDeniedException if the session does not have the privileges to reindex this part of the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindex(String)
     * @see #reindexAsync()
     */
    Future<Boolean> reindexAsync( String path ) throws RepositoryException;

}
