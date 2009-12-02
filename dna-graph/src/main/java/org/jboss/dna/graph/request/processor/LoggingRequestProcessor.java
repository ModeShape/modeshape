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
package org.jboss.dna.graph.request.processor;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.request.AccessQueryRequest;
import org.jboss.dna.graph.request.CloneBranchRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DeleteChildrenRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.FullTextSearchRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.LockBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.RemovePropertyRequest;
import org.jboss.dna.graph.request.RenameNodeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.SetPropertyRequest;
import org.jboss.dna.graph.request.UnlockBranchRequest;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.UpdateValuesRequest;
import org.jboss.dna.graph.request.VerifyNodeExistsRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;

/**
 * A {@link RequestProcessor} implementation that wraps another and that logs messages at the supplied level.
 */
@Immutable
public class LoggingRequestProcessor extends RequestProcessor {

    private final RequestProcessor delegate;
    private final Logger logger;
    private final Logger.Level level;

    /**
     * @param delegate the processor to which this processor delegates
     * @param logger the logger that should be used
     * @param level the level of the log messages; defaults to {@link Logger.Level#TRACE}
     */
    public LoggingRequestProcessor( RequestProcessor delegate,
                                    Logger logger,
                                    Logger.Level level ) {
        super(delegate.getSourceName(), delegate.getExecutionContext(), null);
        CheckArg.isNotNull(logger, "logger");
        this.delegate = delegate;
        this.logger = logger;
        this.level = level != null ? level : Logger.Level.TRACE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteChildrenRequest)
     */
    @Override
    public void process( DeleteChildrenRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyNodeExistsRequest)
     */
    @Override
    public void process( VerifyNodeExistsRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdateValuesRequest)
     */
    @Override
    public void process( UpdateValuesRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CompositeRequest)
     */
    @Override
    public void process( CompositeRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadBlockOfChildrenRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadNextBlockOfChildrenRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadPropertyRequest)
     */
    @Override
    public void process( ReadPropertyRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.SetPropertyRequest)
     */
    @Override
    public void process( SetPropertyRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RemovePropertyRequest)
     */
    @Override
    public void process( RemovePropertyRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.LockBranchRequest)
     */
    @Override
    public void process( LockBranchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UnlockBranchRequest)
     */
    @Override
    public void process( UnlockBranchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.AccessQueryRequest)
     */
    @Override
    public void process( AccessQueryRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.FullTextSearchRequest)
     */
    @Override
    public void process( FullTextSearchRequest request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.Request)
     */
    @Override
    public void process( Request request ) {
        logger.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        logger.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        logger.log(level, GraphI18n.closingRequestProcessor);
        delegate.close();
        logger.log(level, GraphI18n.closingRequestProcessor);
    }

}
