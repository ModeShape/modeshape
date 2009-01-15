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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.RepositorySourceListener;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * The {@link RepositoryConnection} implementation for the file system connector. The bulk of the work is performed by the
 * {@link FileSystemRequestProcessor}.
 * 
 * @author Randall Hauch
 */
public class FileSystemConnection implements RepositoryConnection {

    private final String sourceName;
    private final Map<String, File> rootsByName;
    private final CachePolicy cachePolicy;
    private final CopyOnWriteArrayList<RepositorySourceListener> listeners = new CopyOnWriteArrayList<RepositorySourceListener>();
    private final FilenameFilter filenameFilter;
    private final boolean updatesAllowed;

    FileSystemConnection( String sourceName,
                          Map<String, File> rootsByName,
                          CachePolicy cachePolicy,
                          FilenameFilter filenameFilter,
                          boolean updatesAllowed ) {
        assert sourceName != null;
        assert sourceName.trim().length() != 0;
        assert rootsByName != null;
        assert rootsByName.size() >= 1;
        this.sourceName = sourceName;
        this.rootsByName = rootsByName;
        this.cachePolicy = cachePolicy;
        this.filenameFilter = filenameFilter;
        this.updatesAllowed = updatesAllowed;
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
     * @see org.jboss.dna.graph.connector.RepositoryConnection#setListener(org.jboss.dna.graph.connector.RepositorySourceListener)
     */
    public void setListener( RepositorySourceListener listener ) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
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
        RequestProcessor proc = new FileSystemRequestProcessor(sourceName, context, rootsByName, filenameFilter, updatesAllowed);
        try {
            proc.process(request);
        } finally {
            proc.close();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#close()
     */
    public void close() {
    }
}
