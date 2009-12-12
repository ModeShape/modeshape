/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.filesystem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.map.MapRepositoryTransaction;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * The {@link RepositoryConnection} implementation for the file system connector. The bulk of the work is performed by the
 * {@link FileSystemRequestProcessor}.
 */
public class FileSystemConnection implements RepositoryConnection {

    private final String sourceName;
    private final String defaultWorkspaceName;
    private final CachePolicy cachePolicy;
    private final Map<String, File> availableWorkspaces;
    private final boolean creatingWorkspacesAllowed;
    private final FilenameFilter filenameFilter;
    private final UUID rootNodeUuid;
    private final int maxPathLength;
    private final String workspaceRootPath;
    private final boolean updatesAllowed;
    private final CustomPropertiesFactory customPropertiesFactory;

    FileSystemConnection( String sourceName,
                          String defaultWorkspaceName,
                          Map<String, File> availableWorkspaces,
                          boolean creatingWorkspacesAllowed,
                          CachePolicy cachePolicy,
                          UUID rootNodeUuid,
                          String workspaceRootPath,
                          int maxPathLength,
                          FilenameFilter filenameFilter,
                          boolean updatesAllowed,
                          CustomPropertiesFactory customPropertiesFactory ) {
        assert sourceName != null;
        assert sourceName.trim().length() != 0;
        assert availableWorkspaces != null;
        assert rootNodeUuid != null;
        assert customPropertiesFactory != null;
        this.sourceName = sourceName;
        this.defaultWorkspaceName = defaultWorkspaceName;
        this.availableWorkspaces = availableWorkspaces;
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
        this.cachePolicy = cachePolicy;
        this.rootNodeUuid = rootNodeUuid;
        this.workspaceRootPath = workspaceRootPath;
        this.maxPathLength = maxPathLength;
        this.filenameFilter = filenameFilter;
        this.updatesAllowed = updatesAllowed;
        this.customPropertiesFactory = customPropertiesFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getSourceName()
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getDefaultCachePolicy()
     */
    public CachePolicy getDefaultCachePolicy() {
        return cachePolicy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getXAResource()
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        FileSystemTransaction txn = startTransaction(request.isReadOnly());
        RequestProcessor processor = new FileSystemRequestProcessor(sourceName, defaultWorkspaceName, availableWorkspaces,
                                                                    creatingWorkspacesAllowed, rootNodeUuid, workspaceRootPath,
                                                                    maxPathLength, context, filenameFilter, updatesAllowed,
                                                                    customPropertiesFactory, txn);
        boolean commit = true;
        try {
            // Obtain the lock and execute the commands ...
            processor.process(request);
            if (request.hasError() && !request.isReadOnly()) {
                // The changes failed, so we need to rollback so we have 'all-or-nothing' behavior
                commit = false;
            }
        } catch (Throwable error) {
            commit = false;
        } finally {
            try {
                processor.close();
            } finally {
                // Now commit or rollback ...
                try {
                    if (commit) {
                        txn.commit();
                    } else {
                        // Need to rollback the changes made to the repository ...
                        txn.rollback();
                    }
                } catch (Throwable commitOrRollbackError) {
                    if (commit && !request.hasError() && !request.isFrozen()) {
                        // Record the error on the request ...
                        request.setError(commitOrRollbackError);
                    }
                    commit = false; // couldn't do it
                }
                if (commit) {
                    // Now that we've closed our transaction, we can notify the observer of the committed changes ...
                    processor.notifyObserverOfChanges();
                }
            }
        }
    }

    /**
     * Begin a transaction, hinting whether the transaction will be used only to read the content. If this is called, then the
     * transaction must be either {@link MapRepositoryTransaction#commit() committed} or
     * {@link MapRepositoryTransaction#rollback() rolled back}.
     * 
     * @param readonly true if the transaction will not modify any content, or false if changes are to be made
     * @return the transaction; never null
     * @see MapRepositoryTransaction#commit()
     * @see MapRepositoryTransaction#rollback()
     */
    protected FileSystemTransaction startTransaction( boolean readonly ) {
        return new FileSystemTransaction();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#close()
     */
    public void close() {
    }
}
