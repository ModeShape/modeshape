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
package org.modeshape.connector.jcr;

import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.Repository;
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

/**
 * 
 */
public class JcrRepositoryConnection implements RepositoryConnection {

    private final JcrRepositorySource source;
    private final Repository repository;
    private final Credentials credentials;

    public JcrRepositoryConnection( JcrRepositorySource source,
                                    Repository repository,
                                    Credentials credentials ) {
        assert source != null;
        assert repository != null;
        this.source = source;
        this.repository = repository;
        this.credentials = credentials;
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
    public void execute( final ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        Logger logger = context.getLogger(getClass());
        Stopwatch sw = null;
        if (logger.isTraceEnabled()) {
            sw = new Stopwatch();
            sw.start();
        }

        // Do any commands update/write?
        RepositoryContext repositoryContext = source.getRepositoryContext();
        Observer observer = repositoryContext != null ? repositoryContext.getObserver() : null;
        JcrRequestProcessor processor = new JcrRequestProcessor(source.getName(), context, repository, observer, credentials,
                                                                source.getDefaultCachePolicy());

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
                if (commit) {
                    processor.commit();
                } else {
                    // Need to rollback the changes made to the repository ...
                    processor.rollback();
                }
            } catch (Throwable commitOrRollbackError) {
                if (commit && !request.hasError() && !request.isFrozen()) {
                    // Record the error on the request ...
                    request.setError(commitOrRollbackError);
                }
                commit = false; // couldn't do it
            } finally {
                try {
                    // Release any resources ...
                    processor.close();
                } finally {
                    if (commit) {
                        // Now that we've closed our transaction, we can notify the observer of the committed changes ...
                        processor.notifyObserverOfChanges();
                    }
                }
            }
        }
        if (logger.isTraceEnabled()) {
            assert sw != null;
            sw.stop();
            logger.trace("JcrRepositoryConnection.execute(...) took " + sw.getTotalDuration());
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
