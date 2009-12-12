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
package org.jboss.dna.connector.store.jpa.model.basic;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.transaction.xa.XAResource;
import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.store.jpa.EntityManagers;
import org.jboss.dna.connector.store.jpa.JpaConnectorI18n;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * The repository connection to JPA repository sources that use the {@link BasicModel basic model}.
 */
class BasicJpaConnection implements RepositoryConnection {

    private final String name;
    private final CachePolicy cachePolicy;
    private final EntityManagers entityManagers;
    private EntityManager entityManager;
    private final UUID rootNodeUuid;
    private final String nameOfDefaultWorkspace;
    private final String[] predefinedWorkspaceNames;
    private final boolean creatingWorkspacesAllowed;
    private final long largeValueMinimumSizeInBytes;
    private final boolean compressData;
    private final boolean enforceReferentialIntegrity;
    private final Observer observer;

    public BasicJpaConnection( String sourceName,
                               Observer observer,
                               CachePolicy cachePolicy,
                               EntityManagers entityManagers,
                               UUID rootNodeUuid,
                               String nameOfDefaultWorkspace,
                               String[] predefinedWorkspaceNames,
                               long largeValueMinimumSizeInBytes,
                               boolean creatingWorkspacesAllowed,
                               boolean compressData,
                               boolean enforceReferentialIntegrity ) {
        assert sourceName != null;
        assert entityManagers != null;
        assert rootNodeUuid != null;
        this.observer = observer;
        this.name = sourceName;
        this.cachePolicy = cachePolicy; // may be null
        this.entityManagers = entityManagers;
        this.entityManager = entityManagers.checkout();
        assert this.entityManagers != null;
        this.rootNodeUuid = rootNodeUuid;
        this.largeValueMinimumSizeInBytes = largeValueMinimumSizeInBytes;
        this.compressData = compressData;
        this.enforceReferentialIntegrity = enforceReferentialIntegrity;
        this.nameOfDefaultWorkspace = nameOfDefaultWorkspace;
        this.predefinedWorkspaceNames = predefinedWorkspaceNames != null ? predefinedWorkspaceNames : new String[] {};
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
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
        return entityManager != null ? entityManager.isOpen() : false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        if (entityManager == null) {
            throw new RepositorySourceException(JpaConnectorI18n.connectionIsNoLongerOpen.text(name));
        }

        Logger logger = context.getLogger(getClass());
        Stopwatch sw = null;
        if (logger.isTraceEnabled()) {
            sw = new Stopwatch();
            sw.start();
        }
        // Do any commands update/write?
        RequestProcessor processor = new BasicRequestProcessor(name, context, observer, entityManager, rootNodeUuid,
                                                               nameOfDefaultWorkspace, predefinedWorkspaceNames,
                                                               largeValueMinimumSizeInBytes, creatingWorkspacesAllowed,
                                                               compressData, enforceReferentialIntegrity);

        boolean commit = true;
        try {
            // Obtain the lock and execute the commands ...
            processor.process(request);
            if (request.hasError() && !request.isReadOnly()) {
                // The changes failed, so we need to rollback so we have 'all-or-nothing' behavior
                commit = false;
            }
        } finally {
            try {
                processor.close();
            } finally {
                // Now commit or rollback ...
                try {
                    EntityTransaction txn = entityManager.getTransaction();
                    if (txn != null) {
                        if (commit) {
                            // Now commit the transaction ...
                            txn.commit();
                        } else {
                            // Need to rollback the changes made to the repository ...
                            txn.rollback();
                        }
                    }
                } catch (Throwable commitOrRollbackError) {
                    if (commit && !request.hasError()) {
                        // Record the error on the request ...
                        request.setError(commitOrRollbackError);
                    }
                    commit = false; // couldn't do it
                }
                if (commit) {
                    // Now that we're not in a transaction anymore, notify the observer of the committed changes ...
                    processor.notifyObserverOfChanges();
                }
            }
        }
        if (logger.isTraceEnabled()) {
            assert sw != null;
            sw.stop();
            logger.trace(this.getClass().getSimpleName() + ".execute(...) took " + sw.getTotalDuration());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#close()
     */
    public void close() {
        if (entityManager != null) {
            // Do this only once ...
            try {
                entityManagers.checkin(entityManager);
            } finally {
                entityManager = null;
            }
        }
    }

}
