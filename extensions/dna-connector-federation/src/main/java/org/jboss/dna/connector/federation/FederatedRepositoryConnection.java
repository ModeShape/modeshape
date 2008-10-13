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
package org.jboss.dna.connector.federation;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.commands.GraphCommand;
import org.jboss.dna.graph.commands.executor.CommandExecutor;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.connectors.RepositorySourceListener;
import org.jboss.dna.graph.requests.Request;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedRepositoryConnection implements RepositoryConnection {

    protected static final RepositorySourceListener NO_OP_LISTENER = new RepositorySourceListener() {
        public void notify( String sourceName,
                            Object... events ) {
            // do nothing
        }
    };

    private final FederatedRepository repository;
    private final String sourceName;
    private final AtomicReference<RepositorySourceListener> listener;

    protected FederatedRepositoryConnection( FederatedRepository repository,
                                             String sourceName ) {
        assert sourceName != null;
        assert repository != null;
        this.sourceName = sourceName;
        this.repository = repository;
        this.listener = new AtomicReference<RepositorySourceListener>(NO_OP_LISTENER);
        this.repository.register(this);
    }

    /**
     * @return repository
     */
    protected FederatedRepository getRepository() {
        return this.repository;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return this.sourceName;
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return this.repository.getConfiguration().getDefaultCachePolicy();
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
    public void setListener( RepositorySourceListener listener ) {
        if (listener == null) listener = NO_OP_LISTENER;
        RepositorySourceListener oldListener = this.listener.getAndSet(listener);
        this.repository.addListener(listener);
        if (oldListener != NO_OP_LISTENER) {
            this.repository.removeListener(oldListener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        return this.repository.isRunning();
    }

    /**
     * {@inheritDoc}
     */
    public void execute( ExecutionContext context,
                         GraphCommand... commands ) throws RepositorySourceException {
        if (!this.repository.isRunning()) {
            throw new RepositorySourceException(FederationI18n.repositoryHasBeenShutDown.text(this.repository.getName()));
        }
        if (commands == null || commands.length == 0) return;

        CommandExecutor executor = this.repository.getExecutor(context, sourceName);
        try {
            assert executor != null;
            for (GraphCommand command : commands) {
                executor.execute(command);
            }
        } finally {
            executor.close();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.requests.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        try {
            this.repository.removeListener(this.listener.get());
        } finally {
            this.repository.unregister(this);
        }
    }

}
