/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.connectors.RepositorySourceListener;
import org.jboss.dna.graph.requests.Request;
import org.jboss.dna.graph.requests.processor.RequestProcessor;

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
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#getSourceName()
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#getDefaultCachePolicy()
     */
    public CachePolicy getDefaultCachePolicy() {
        return cachePolicy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#getXAResource()
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#setListener(org.jboss.dna.graph.connectors.RepositorySourceListener)
     */
    public void setListener( RepositorySourceListener listener ) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.requests.Request)
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
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#close()
     */
    public void close() {
    }
}
