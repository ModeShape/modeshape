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
package org.jboss.dna.graph.connector.federation;

import java.util.List;
import java.util.Map;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.CacheableRequest;
import org.jboss.dna.graph.request.CloneBranchRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DeleteChildrenRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
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
import org.jboss.dna.graph.request.processor.RequestProcessor;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNodeRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBlockOfChildrenRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadPropertyRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.SetPropertyRequest)
     */
    @Override
    public void process( SetPropertyRequest request ) {
        SetPropertyRequest source = (SetPropertyRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyNodeExistsRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RemovePropertyRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RenameNodeRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        CacheableRequest source = (CacheableRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        if (source instanceof ReadBranchRequest) {
            ReadBranchRequest readSource = (ReadBranchRequest)source;
            request.setActualLocationOfNode(readSource.getActualLocationOfNode());
            for (Location node : readSource) {
                List<Location> children = readSource.getChildren(node);
                if (children != null) request.setChildren(node, children);
                Map<Name, Property> props = readSource.getPropertiesFor(node);
                if (props != null) request.setProperties(node, props.values());
            }
        } else if (source instanceof ReadNodeRequest) {
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        UpdatePropertiesRequest source = (UpdatePropertiesRequest)federatedRequest.getFirstProjectedRequest().getRequest();
        if (checkErrorOrCancel(request, source)) return;
        request.setActualLocationOfNode(source.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdateValuesRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneBranchRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteBranchRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteChildrenRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.LockBranchRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UnlockBranchRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called
    }

}
