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
package org.modeshape.connector.store.jpa.model.simple;

import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.Logger;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.map.MapRepositoryTransaction;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * The repository connection to JPA repository sources that use the {@link SimpleModel simple model}.
 */
@NotThreadSafe
public class SimpleJpaConnection implements RepositoryConnection {

    private SimpleJpaRepository repository;
    private final JpaSource source;
    private EntityManager entityManager;

    public SimpleJpaConnection( JpaSource source ) {
        this.source = source;

    }

    public boolean ping( long time,
                         TimeUnit unit ) {
        // Most pings will occur before or after an execute() call, when there is no entityManger
        // If there is no entity manager, the connection is still valid!
        return entityManager == null || entityManager.isOpen();
    }

    public CachePolicy getDefaultCachePolicy() {
        return source.getCachePolicy();
    }

    public String getSourceName() {
        return source.getName();
    }

    public XAResource getXAResource() {
        return null;
    }

    private void acquireRepository() {
        this.entityManager = source.getEntityManagers().checkout();
        this.entityManager.getTransaction().begin();
        this.repository = new SimpleJpaRepository(source.getName(), source.getRootUuid(), source.getDefaultWorkspaceName(),
                                                  source.getPredefinedWorkspaceNames(), entityManager,
                                                  source.getRepositoryContext().getExecutionContext(), source.isCompressData(),
                                                  source.isCreatingWorkspacesAllowed(), source.getLargeValueSizeInBytes(),
                                                  source.getDialect());

    }

    private void releaseRepository() {
        this.repository = null;
        if (entityManager != null) {
            try {
                source.getEntityManagers().checkin(entityManager);
            } finally {
                entityManager = null;
            }
        }

    }

    public void close() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        Logger logger = context.getLogger(getClass());
        Stopwatch sw = null;
        if (logger.isTraceEnabled()) {
            sw = new Stopwatch();
            sw.start();
        }

        acquireRepository();

        // Do any commands update/write?
        Observer observer = this.source.getRepositoryContext().getObserver();
        RequestProcessor processor = new SimpleRequestProcessor(context, this.repository, observer, source.areUpdatesAllowed());

        boolean commit = true;
        MapRepositoryTransaction txn = repository.startTransaction(request.isReadOnly());
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
                    if (commit) {
                        txn.commit();
                    } else {
                        // Need to rollback the changes made to the repository ...
                        txn.rollback();
                    }
                } catch (Throwable commitOrRollbackError) {
                    if (commit && !request.hasError()) {
                        // Record the error on the request ...
                        request.setError(commitOrRollbackError);
                    }
                    commit = false; // couldn't do it
                }
                if (commit) {
                    // Now that we've closed our transaction, notify the observer of the committed changes ...
                    processor.notifyObserverOfChanges();
                }
            }
        }

        releaseRepository();

        if (logger.isTraceEnabled()) {
            assert sw != null;
            sw.stop();
            logger.trace(this.getClass().getSimpleName() + ".execute(...) took " + sw.getTotalDuration());
        }
    }

    /*
     
      This method is needed only to support the SimpleJpaSourceTest#shouldAllowChangingIsolationLevel() test.
     
    EntityManager entityManager() {
        EntityManager entityManager = this.entityManager;

        if (entityManager == null) {
            acquireRepository();
            entityManager = this.entityManager;
        }

        return entityManager;
    }
    */
}
