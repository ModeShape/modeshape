/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.store.jpa;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.transaction.xa.XAResource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.RepositorySourceListener;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
public class JpaConnection implements RepositoryConnection {

    private final String name;
    private final CachePolicy cachePolicy;
    private final CopyOnWriteArrayList<RepositorySourceListener> listeners = new CopyOnWriteArrayList<RepositorySourceListener>();
    private final EntityManager entityManager;
    private final Model model;
    private final UUID rootNodeUuid;
    private final long largeValueMinimumSizeInBytes;
    private final boolean compressData;
    private final boolean enforceReferentialIntegrity;

    /*package*/JpaConnection( String sourceName,
                               CachePolicy cachePolicy,
                               EntityManager entityManager,
                               Model model,
                               UUID rootNodeUuid,
                               long largeValueMinimumSizeInBytes,
                               boolean compressData,
                               boolean enforceReferentialIntegrity ) {
        assert sourceName != null;
        assert entityManager != null;
        assert model != null;
        assert rootNodeUuid != null;
        this.name = sourceName;
        this.cachePolicy = cachePolicy; // may be null
        this.entityManager = entityManager;
        this.model = model;
        this.rootNodeUuid = rootNodeUuid;
        this.largeValueMinimumSizeInBytes = largeValueMinimumSizeInBytes;
        this.compressData = compressData;
        this.enforceReferentialIntegrity = enforceReferentialIntegrity;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getSourceName()
     */
    public String getSourceName() {
        return name;
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
        return entityManager.isOpen();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        long size = largeValueMinimumSizeInBytes;
        RequestProcessor proc = model.createRequestProcessor(name,
                                                             context,
                                                             entityManager,
                                                             rootNodeUuid,
                                                             size,
                                                             compressData,
                                                             enforceReferentialIntegrity);
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
