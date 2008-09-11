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
package org.jboss.dna.connector.inmemory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.transaction.xa.XAResource;
import org.jboss.dna.common.stats.Stopwatch;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.connector.RepositorySourceException;
import org.jboss.dna.spi.connector.RepositorySourceListener;
import org.jboss.dna.spi.graph.commands.ActsAsUpdate;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.executor.CommandExecutor;

/**
 * @author Randall Hauch
 */
public class InMemoryRepositoryConnection implements RepositoryConnection {

    protected static final RepositorySourceListener NO_OP_LISTENER = new RepositorySourceListener() {

        /**
         * {@inheritDoc}
         */
        public void notify( String sourceName,
                            Object... events ) {
            // do nothing
        }
    };

    private final InMemoryRepositorySource source;
    private final InMemoryRepository content;
    private RepositorySourceListener listener = NO_OP_LISTENER;

    InMemoryRepositoryConnection( InMemoryRepositorySource source,
                                  InMemoryRepository content ) {
        assert source != null;
        assert content != null;
        this.source = source;
        this.content = content;
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
        this.content.getRoot();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setListener( RepositorySourceListener listener ) {
        this.listener = listener != null ? listener : NO_OP_LISTENER;
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
     * @throws RepositorySourceException
     */
    public void execute( ExecutionContext context,
                         GraphCommand... commands ) throws RepositorySourceException {
        Logger logger = context.getLogger(getClass());
        Stopwatch sw = null;
        if (logger.isTraceEnabled()) {
            sw = new Stopwatch();
            sw.start();
        }
        // Do any commands update/write?
        Lock lock = this.content.getLock().readLock();
        for (GraphCommand command : commands) {
            if (command instanceof ActsAsUpdate) {
                lock = this.content.getLock().writeLock();
                break;
            }
        }

        try {
            // Obtain the lock and execute the commands ...
            CommandExecutor executor = this.content.getCommandExecutor(context, this.getSourceName());
            lock.lock();
            for (GraphCommand command : commands) {
                executor.execute(command);
            }
        } finally {
            lock.unlock();
        }
        if (logger.isTraceEnabled()) {
            assert sw != null;
            sw.stop();
            logger.trace("InMemoryRepositoryConnection.execute(...) took " + sw.getTotalDuration());
        }
    }

    protected InMemoryRepository getContent() {
        return content;
    }

    /**
     * @return listener
     */
    protected RepositorySourceListener getListener() {
        return this.listener;
    }

}
