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
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.connectors.RepositorySourceListener;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.ValueFactory;
import org.jboss.dna.graph.requests.Request;
import org.jboss.dna.graph.requests.processor.RequestProcessor;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
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

    private final String sourceName;
    private final CachePolicy cachePolicy;
    private final SVNRepository repository;
    private final boolean updatesAllowed;
    private RepositorySourceListener listener = NO_OP_LISTENER;

    public SVNRepositoryConnection( String sourceName,
                                    CachePolicy cachePolicy,
                                    boolean updatesAllowed,
                                    SVNRepository repository ) {
        assert (sourceName != null);
        assert (repository != null);

        SVNNodeKind nodeKind = null;
        try {
            nodeKind = repository.checkPath("", -1);
            if (nodeKind == SVNNodeKind.NONE) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                               "No entry at URL ''{0}''",
                                                               repository.getLocation().getPath());
                throw new SVNException(error);
            } else if (nodeKind == SVNNodeKind.UNKNOWN) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                               "Entry at URL ''{0}'' is a file while directory was expected",
                                                               repository.getLocation().getPath());
                throw new SVNException(error);
            } else if (nodeKind == SVNNodeKind.FILE) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                               "Entry at URL ''{0}'' is a file while directory was expected",
                                                               repository.getLocation().getPath());
                throw new SVNException(error);
            }
        } catch (SVNException e) {
            // deal with the exception
            throw new RuntimeException(e);
        }

        this.sourceName = sourceName;
        this.cachePolicy = cachePolicy;
        this.repository = repository;
        this.updatesAllowed = updatesAllowed;
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
     *      org.jboss.dna.graph.requests.Request)
     */
    @SuppressWarnings( "unused" )
    public void execute( final ExecutionContext context,
                         final Request request ) throws RepositorySourceException {

        final PathFactory pathFactory = context.getValueFactories().getPathFactory();
        final PropertyFactory propertyFactory = context.getPropertyFactory();
        final ValueFactory<UUID> uuidFactory = context.getValueFactories().getUuidFactory();

        RequestProcessor processor = new SVNRepositoryRequestProcessor(getSourceName(), context, repository, updatesAllowed);
        try {
            processor.process(request);
        } finally {
            processor.close();
        }
    }

    /**
     * @return listener
     */
    protected RepositorySourceListener getListener() {
        return this.listener;
    }

}
