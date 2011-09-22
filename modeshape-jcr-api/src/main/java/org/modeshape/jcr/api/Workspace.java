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
import javax.jcr.RepositoryException;

/**
 * An extension of JCR 2.0's Workspace interface, with a few ModeShape-specific enhancements.
 */
public interface Workspace extends javax.jcr.Workspace {

    /**
     * Crawl and re-index the content in this workspace. This method blocks until the indexing is completed.
     * 
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindexAsync()
     * @see #reindexAsync(String, int)
     * @see #reindex(String, int)
     */
    void reindex() throws RepositoryException;

    /**
     * Crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     * 
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindexAsync()
     * @see #reindexAsync(String, int)
     */
    void reindex( String path,
                  int depth ) throws RepositoryException;

    /**
     * Asynchronously crawl and re-index the content in this workspace.
     * 
     * @return a future representing the asynchronous operation; never null
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindex(String, int)
     * @see #reindexAsync(String, int)
     */
    Future<Boolean> reindexAsync() throws RepositoryException;

    /**
     * Asynchronously crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     * 
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @return a future representing the asynchronous operation; never null
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindex(String, int)
     * @see #reindexAsync()
     */
    Future<Boolean> reindexAsync( String path,
                                  int depth ) throws RepositoryException;

}
