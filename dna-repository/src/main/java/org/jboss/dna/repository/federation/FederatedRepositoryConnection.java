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
package org.jboss.dna.repository.federation;

import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedRepositoryConnection implements RepositoryConnection {

    protected static final RepositorySourceListener NO_OP_LISTENER = new RepositorySourceListener() {

        public void notify( String sourceName, Object... events ) {
            // do nothing
        }
    };

    private final FederatedRepository repository;
    private final FederatedRepositorySource source;
    private RepositorySourceListener listener = NO_OP_LISTENER;

    protected FederatedRepositoryConnection( FederatedRepository repository, FederatedRepositorySource source ) {
        assert source != null;
        assert repository != null;
        this.source = source;
        this.repository = repository;
    }

    /**
     * @return repository
     */
    protected FederatedRepository getRepository() {
        return this.repository;
    }

    /**
     * @return source
     */
    protected FederatedRepositorySource getRepositorySource() {
        return this.source;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return this.source.getName();
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return this.repository.getDefaultCachePolicy();
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
        RepositorySourceListener oldListener = this.listener;
        this.listener = listener != null ? listener : NO_OP_LISTENER;
        this.repository.addListener(this.listener);
        if (oldListener != NO_OP_LISTENER) {
            this.repository.removeListener(oldListener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean ping( long time, TimeUnit unit ) {
        return this.repository.getAdministrator().isStarted();
    }

    /**
     * {@inheritDoc}
     */
    public void execute( ExecutionEnvironment env, GraphCommand... commands ) throws RepositorySourceException {
        if (!this.repository.getAdministrator().isStarted()) {
            throw new RepositorySourceException(RepositoryI18n.repositoryHasBeenShutDown.text(this.repository.getName()));
        }
        if (commands == null || commands.length == 0) return;

        for (GraphCommand command : commands) {
            if (command == null) continue;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        this.repository.removeListener(this.listener);
    }

}
