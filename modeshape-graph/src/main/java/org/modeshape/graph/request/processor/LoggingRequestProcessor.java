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
package org.modeshape.graph.request.processor;

import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CollectGarbageRequest;
import org.modeshape.graph.request.CompositeRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DeleteChildrenRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.LockBranchRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.request.ReadNextBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.ReadPropertyRequest;
import org.modeshape.graph.request.RemovePropertyRequest;
import org.modeshape.graph.request.RenameNodeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.UpdateValuesRequest;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;

/**
 * A {@link RequestProcessor} implementation that wraps another and that logs messages at the supplied level.
 */
@Immutable
public class LoggingRequestProcessor extends RequestProcessor {

    private final RequestProcessor delegate;
    private static final Logger LOGGER = Logger.getLogger(LoggingRequestProcessor.class);
    private final Logger.Level level;

    /**
     * @param delegate the processor to which this processor delegates
     * @param logger the LOGGER that should be used
     * @param level the level of the log messages; defaults to {@link Logger.Level#TRACE}
     */
    public LoggingRequestProcessor( RequestProcessor delegate,
                                    Logger logger,
                                    Logger.Level level ) {
        super(delegate.getSourceName(), delegate.getExecutionContext(), null);
        CheckArg.isNotNull(logger, "logger");
        this.delegate = delegate;
        this.level = level != null ? level : Logger.Level.TRACE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        // LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteChildrenRequest)
     */
    @Override
    public void process( DeleteChildrenRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyNodeExistsRequest)
     */
    @Override
    public void process( VerifyNodeExistsRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdateValuesRequest)
     */
    @Override
    public void process( UpdateValuesRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CompositeRequest)
     */
    @Override
    public void process( CompositeRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadBlockOfChildrenRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNextBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadNextBlockOfChildrenRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadPropertyRequest)
     */
    @Override
    public void process( ReadPropertyRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.SetPropertyRequest)
     */
    @Override
    public void process( SetPropertyRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RemovePropertyRequest)
     */
    @Override
    public void process( RemovePropertyRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.LockBranchRequest)
     */
    @Override
    public void process( LockBranchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UnlockBranchRequest)
     */
    @Override
    public void process( UnlockBranchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.AccessQueryRequest)
     */
    @Override
    public void process( AccessQueryRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.FullTextSearchRequest)
     */
    @Override
    public void process( FullTextSearchRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CollectGarbageRequest)
     */
    @Override
    public void process( CollectGarbageRequest request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.Request)
     */
    @Override
    public void process( Request request ) {
        LOGGER.log(level, GraphI18n.executingRequest, request);
        delegate.process(request);
        LOGGER.log(level, GraphI18n.executedRequest, request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        LOGGER.log(level, GraphI18n.closingRequestProcessor);
        delegate.close();
        LOGGER.log(level, GraphI18n.closedRequestProcessor);
    }

}
