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
package org.modeshape.graph.connector.federation;

import java.util.List;
import java.util.Map;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CacheableRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
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
import org.modeshape.graph.request.RequestType;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.UpdateValuesRequest;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A RequestProcessor used by the {@link JoinRequestProcessor} in cases where there is a single source-request with the same
 * location(s). This processor merely copies the results from each source request for the
 * {@link #setFederatedRequest(FederatedRequest) current federated request} into the request passed as a parameter.
 */
class JoinMirrorRequestProcessor extends RequestProcessor {

    private FederatedRequest federatedRequest;

    JoinMirrorRequestProcessor( String sourceName,
                                ExecutionContext context,
                                Observer observer,
                                DateTime now,
                                CachePolicy defaultCachePolicy ) {
        super(sourceName, context, observer, now, defaultCachePolicy);

    }

    /**
     * @param federatedRequest Sets federatedRequest to the specified value.
     */
    void setFederatedRequest( FederatedRequest federatedRequest ) {
        this.federatedRequest = federatedRequest;
    }

    protected boolean checkErrorOrCancel( Request request,
                                          Request sourceRequest ) {
        if (sourceRequest.hasError()) {
            request.setError(sourceRequest.getError());
            return true;
        }
        if (sourceRequest.isCancelled()) {
            request.cancel();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        ReadNodeRequest source = (ReadNodeRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        for (Location childInSource : source.getChildren()) {
            request.addChild(childInSource);
        }
        for (Property propertyInSource : source.getProperties()) {
            request.addProperties(propertyInSource);
        }
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        ReadAllChildrenRequest source = (ReadAllChildrenRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        for (Location childInSource : source.getChildren()) {
            request.addChild(childInSource);
        }
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        ReadAllPropertiesRequest source = (ReadAllPropertiesRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        for (Property propertyInSource : source.getProperties()) {
            request.addProperties(propertyInSource);
        }
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadBlockOfChildrenRequest request ) {
        ReadBlockOfChildrenRequest source = (ReadBlockOfChildrenRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        for (Location childInSource : source.getChildren()) {
            request.addChild(childInSource);
        }
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNextBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadNextBlockOfChildrenRequest request ) {
        ReadNextBlockOfChildrenRequest source = (ReadNextBlockOfChildrenRequest)federatedRequest.getFirstProjectedRequest()
                                                                                                .getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfStartingAfterNode(source.getActualLocationOfStartingAfterNode());
        for (Location childInSource : source.getChildren()) {
            request.addChild(childInSource);
        }
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadPropertyRequest)
     */
    @Override
    public void process( ReadPropertyRequest request ) {
        ReadPropertyRequest source = (ReadPropertyRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        request.setProperty(source.getProperty());
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.SetPropertyRequest)
     */
    @Override
    public void process( SetPropertyRequest request ) {
        SetPropertyRequest source = (SetPropertyRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        // Set the actual location and created flags ...
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        request.setNewProperty(source.isNewProperty());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyNodeExistsRequest)
     */
    @Override
    public void process( VerifyNodeExistsRequest request ) {
        VerifyNodeExistsRequest source = (VerifyNodeExistsRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RemovePropertyRequest)
     */
    @Override
    public void process( RemovePropertyRequest request ) {
        RemovePropertyRequest source = (RemovePropertyRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        RenameNodeRequest source = (RenameNodeRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocations(source.getActualLocationBefore(), source.getActualLocationAfter());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        CacheableRequest source = (CacheableRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        if (RequestType.READ_BRANCH == source.getType()) {
            ReadBranchRequest readSource = (ReadBranchRequest)source;
            request.setActualLocationOfNode(readSource.getActualLocationOfNode());
            for (Location node : readSource) {
                List<Location> children = readSource.getChildren(node);
                if (children != null) request.setChildren(node, children);
                Map<Name, Property> props = readSource.getPropertiesFor(node);
                if (props != null) request.setProperties(node, props.values());
            }
        } else if (RequestType.READ_NODE == source.getType()) {
            ReadNodeRequest readSource = (ReadNodeRequest)source;
            request.setActualLocationOfNode(readSource.getActualLocationOfNode());
            Location parent = readSource.getActualLocationOfNode();
            request.setChildren(parent, readSource.getChildren());
            request.setProperties(parent, readSource.getPropertiesByName().values());
        }
        request.setCachePolicy(getDefaultCachePolicy());
        setCacheableInfo(request, source.getCachePolicy());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        CreateNodeRequest source = (CreateNodeRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        UpdatePropertiesRequest source = (UpdatePropertiesRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
        request.setNewProperties(source.getNewPropertyNames());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdateValuesRequest)
     */
    @Override
    public void process( UpdateValuesRequest request ) {
        UpdateValuesRequest source = (UpdateValuesRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocation(source.getActualLocationOfNode(),
                                  request.getActualAddedValues(),
                                  request.getActualRemovedValues());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        CopyBranchRequest source = (CopyBranchRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocations(source.getActualLocationBefore(), source.getActualLocationAfter());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        CloneBranchRequest source = (CloneBranchRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocations(source.getActualLocationBefore(), source.getActualLocationAfter());
        request.setRemovedNodes(source.getRemovedNodes());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        DeleteBranchRequest source = (DeleteBranchRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteChildrenRequest)
     */
    @Override
    public void process( DeleteChildrenRequest request ) {
        DeleteChildrenRequest source = (DeleteChildrenRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        MoveBranchRequest source = (MoveBranchRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocations(source.getActualLocationBefore(), source.getActualLocationAfter());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.LockBranchRequest)
     */
    @Override
    public void process( LockBranchRequest request ) {
        LockBranchRequest source = (LockBranchRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocation(source.getActualLocation());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UnlockBranchRequest)
     */
    @Override
    public void process( UnlockBranchRequest request ) {
        UnlockBranchRequest source = (UnlockBranchRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocation(source.getActualLocation());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        VerifyNodeExistsRequest source = (VerifyNodeExistsRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        assert source.getActualLocationOfNode().getPath().isRoot();
        request.setActualRootLocation(source.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.AccessQueryRequest)
     */
    @Override
    public void process( AccessQueryRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.FullTextSearchRequest)
     */
    @Override
    public void process( FullTextSearchRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }
}
