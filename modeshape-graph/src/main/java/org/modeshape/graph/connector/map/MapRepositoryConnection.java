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
package org.modeshape.graph.connector.map;

import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A connection to a {@link MapRepository}.
 */
public class MapRepositoryConnection implements RepositoryConnection {
    private final MapRepositorySource source;
    private final MapRepository repository;

    public MapRepositoryConnection( MapRepositorySource source,
                                    MapRepository repository ) {
        assert source != null;
        assert repository != null;
        this.source = source;
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return source.getName();
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return source.getDefaultCachePolicy();
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        // do nothing
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
        // Do any commands update/write?
        RepositoryContext repositoryContext = this.source.getRepositoryContext();
        Observer observer = repositoryContext != null ? repositoryContext.getObserver() : null;
        RequestProcessor processor = new MapRequestProcessor(context, this.repository, observer, source.areUpdatesAllowed());

        boolean commit = true;
        MapRepositoryTransaction txn = repository.startTransaction(request.isReadOnly());
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
        if (logger.isTraceEnabled()) {
            assert sw != null;
            sw.stop();
            logger.trace("MapRepositoryConnection.execute(...) took " + sw.getTotalDuration());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Connection to the \"" + getSourceName() + "\" " + repository.getClass().getSimpleName();
    }
}
