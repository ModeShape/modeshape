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
package org.jboss.dna.connector.federation;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.ThreadSafe;
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
        return null;
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
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        if (!this.repository.isRunning()) {
            throw new RepositorySourceException(FederationI18n.repositoryHasBeenShutDown.text(this.repository.getName()));
        }
        if (request == null) return;

        RequestProcessor processor = this.repository.getProcessor(context, sourceName);
        assert processor != null;
        try {
            processor.process(request);
        } finally {
            processor.close();
        }
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
