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
package org.jboss.dna.connector.svn;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.commands.GraphCommand;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.connectors.RepositorySourceListener;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.requests.Request;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * The repository connection to a SVN Repository instance.
 * 
 * @author Serge Pagop
 */
public class SVNRepositoryConnection implements RepositoryConnection {

    protected static final RepositorySourceListener NO_OP_LISTENER = new RepositorySourceListener() {

        /**
         * {@inheritDoc}
         */
        public void notify( String sourceName,
                            Object... events ) {
            // do nothing
        }
    };

    private Name uuidPropertyName;
    private final String sourceName;
    private final String uuidPropertyNameString;
    private final CachePolicy cachePolicy;
    private final SVNRepository repository;
    private RepositorySourceListener listener = NO_OP_LISTENER;

    public SVNRepositoryConnection( String sourceName,
                                    CachePolicy cachePolicy,
                                    String uuidPropertyName,
                                    SVNRepository repository ) {
        assert (sourceName != null);
        assert (repository != null);
        assert (uuidPropertyName != null);
        this.sourceName = sourceName;
        this.cachePolicy = cachePolicy;
        this.uuidPropertyNameString = uuidPropertyName;
        this.repository = repository;
    }

    SVNRepository getRepository() {
        return repository;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return cachePolicy;
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
        try {
            this.repository.getRepositoryRoot(true);
        } catch (SVNException e) {
            return false;
        }
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
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#close()
     */
    public void close() {
        // do not care about.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.commands.GraphCommand[])
     */
    public void execute( ExecutionContext context,
                         GraphCommand... commands ) throws RepositorySourceException {
        // Now execute the commands ...
  
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
    }

    /**
     * @return listener
     */
    protected RepositorySourceListener getListener() {
        return this.listener;
    }

    /**
     * Utility method to calculate (if required) and obtain the name that should be used to store the UUID values for each node.
     * This method may be called without regard to synchronization, since it should return the same value if it happens to be
     * called concurrently while not yet initialized.
     * 
     * @param context the execution context
     * @return the name, or null if the UUID should not be stored
     */
    protected Name getUuidPropertyName( ExecutionContext context ) {
        if (uuidPropertyName == null) {
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            uuidPropertyName = nameFactory.create(this.uuidPropertyNameString);
        }
        return this.uuidPropertyName;
    }

    protected UUID generateUuid() {
        return UUID.randomUUID();
    }

}
