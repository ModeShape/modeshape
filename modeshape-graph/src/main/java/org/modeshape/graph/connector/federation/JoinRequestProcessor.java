/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.federation.FederatedRequest.ProjectedRequest;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueComparators;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CacheableRequest;
import org.modeshape.graph.request.ChangeRequest;
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
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.ReadPropertyRequest;
import org.modeshape.graph.request.RemovePropertyRequest;
import org.modeshape.graph.request.RenameNodeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.RequestType;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A {@link RequestProcessor} that performs the join portion of the fork-join operation.
 */
@NotThreadSafe
class JoinRequestProcessor extends RequestProcessor {

    private final PathFactory pathFactory;
    private final PropertyFactory propertyFactory;
    private final JoinMirrorRequestProcessor mirrorProcessor;
    protected FederatedRequest federatedRequest;

    /**
     * Create a new join processor
     * 
     * @param repository the federated repository configuration; never null
     * @param context the execution context in which this processor is executing; may not be null
     * @param observer the observer for change events; may be null
     * @param now the timestamp representing the current time in UTC; may not be null
     */
    public JoinRequestProcessor( FederatedRepository repository,
                                 ExecutionContext context,
                                 Observer observer,
                                 DateTime now ) {
        super(repository.getSourceName(), context, observer, now, repository.getDefaultCachePolicy());
        // this.repository = repository;
        this.propertyFactory = context.getPropertyFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        // The mirror processor should never send anything to an observer, since all requests go to this processor's observer
        this.mirrorProcessor = new JoinMirrorRequestProcessor(repository.getSourceName(), context, null, now,
                                                              repository.getDefaultCachePolicy());
    }

    /**
     * Process all of the {@link FederatedRequest} objects that are in the supplied collection.
     * 
     * @param completedFederatedRequests the collection of {@link FederatedRequest} whose projected requests have already been
     *        processed; may not be null
     * @see FederatedRepositoryConnection#execute(ExecutionContext, org.modeshape.graph.request.Request)
     */
    public void process( final Iterable<FederatedRequest> completedFederatedRequests ) {
        for (FederatedRequest federatedRequest : completedFederatedRequests) {
            // No need to await for the forked request, since it will be done
            process(federatedRequest);
        }
    }

    /**
     * Process the {@link FederatedRequest} objects that are in the supplied queue. The queue contains {@link FederatedRequest}
     * that may have projected requests that have not yet been processed by the respective source, so this method
     * {@link FederatedRequest#await() waits} until all source requests have been processed. This method returns only when it
     * obtains from the queue a {@link NoMoreFederatedRequests} instance.
     * 
     * @param federatedRequestQueue the queue containing the federated requests; may not be null
     * @see FederatedRepositoryConnection#execute(ExecutionContext, org.modeshape.graph.request.Request)
     */
    public void process( final BlockingQueue<FederatedRequest> federatedRequestQueue ) {
        FederatedRequest forked = null;
        try {
            for (;;) {
                forked = federatedRequestQueue.take();
                if (forked instanceof NoMoreFederatedRequests) return;
                // Block until this forked request has completed
                forked.await();
                // Now process ...
                process(forked);
            }
        } catch (InterruptedException e) {
            // This happens when the federated connector has been told to shutdown now, and it shuts down
            // its executor (the worker pool) immediately by interrupting each in-use thread.
            // In this case, we should cancel the current request but should NOT iterate over any remaining requests.
            try {
                if (forked != null) {
                    forked.original().cancel();
                }
            } finally {
                // Clear the interrupted status of the thread ...
                Thread.interrupted();
            }
        }
    }

    protected final void process( FederatedRequest forked ) {
        // Determine whether this is a single mirror request ...
        Request original = forked.original();
        ProjectedRequest projectedRequest = forked.getFirstProjectedRequest();
        boolean sameLocation = projectedRequest != null && !projectedRequest.hasNext() && projectedRequest.isSameLocation();

        // Set the cachable information ...
        if (original instanceof CacheableRequest) {
            CacheableRequest cacheableOriginal = (CacheableRequest)original;
            cacheableOriginal.setCachePolicy(getDefaultCachePolicy());
            while (projectedRequest != null) {
                Request requestToSource = projectedRequest.getRequest();
                if (cacheableOriginal != null) {
                    setCacheableInfo(cacheableOriginal, ((CacheableRequest)requestToSource).getCachePolicy());
                }
                projectedRequest = projectedRequest.next();
            }
        }

        // Now do the join on this request ...
        if (sameLocation) {
            Request sourceRequest = forked.getFirstProjectedRequest().getRequest();
            if (sourceRequest.hasError()) {
                original.setError(sourceRequest.getError());
            } else if (sourceRequest.isCancelled()) {
                original.cancel();
            }
            mirrorProcessor.setFederatedRequest(forked);
            mirrorProcessor.process(original);
            // If this is a change request, record it on this processor so it goes to the observer ...
            if (original instanceof ChangeRequest && !original.hasError() && !original.isCancelled()) {
                recordChange((ChangeRequest)original);
            }
        } else {
            this.federatedRequest = forked;
            process(original);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyNodeExistsRequest)
     */
    @Override
    public void process( VerifyNodeExistsRequest request ) {
        ProjectedRequest projectedRequest = federatedRequest.getFirstProjectedRequest();

        request.setCachePolicy(getDefaultCachePolicy());
        Location actualLocation = request.at();
        int numMerged = 0;
        while (projectedRequest != null) {
            VerifyNodeExistsRequest readFromSource = (VerifyNodeExistsRequest)projectedRequest.getRequest();
            if (readFromSource.hasError()) {
                projectedRequest = projectedRequest.next();
                continue;
            }
            if (readFromSource.isCancelled()) {
                request.cancel();
                return;
            }

            // Make sure we have an actual location ...
            Location sourceLocation = readFromSource.getActualLocationOfNode();
            actualLocation = determineActualLocation(actualLocation, sourceLocation, projectedRequest.getProjection());

            if (sourceLocation.hasIdProperties()) {
                // Accumulate the identification properties ...
                for (Property propertyInSource : sourceLocation.getIdProperties()) {
                    Name name = propertyInSource.getName();
                    Property existing = actualLocation.getIdProperty(name);
                    if (existing != null) {
                        // Merge the property values ...
                        propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                    }
                    actualLocation = actualLocation.with(propertyInSource);
                }
            }
            setCacheableInfo(request, readFromSource.getCachePolicy());
            projectedRequest = projectedRequest.next();
            ++numMerged;
        }
        if (numMerged == 0) {
            // No source requests had results ...
            setPathNotFound(request, request.at(), federatedRequest.getFirstProjectedRequest());
        } else {
            request.setActualLocationOfNode(actualLocation);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        Path federatedPath = request.at().getPath();
        Map<Name, Property> properties = request.getPropertiesByName();
        Map<Name, Integer> childSnsIndexes = new HashMap<Name, Integer>();
        ProjectedRequest projectedRequest = federatedRequest.getFirstProjectedRequest();

        request.setCachePolicy(getDefaultCachePolicy());
        Location actualLocation = request.at();
        int numMerged = 0;
        while (projectedRequest != null) {
            Request sourceRequest = projectedRequest.getRequest();
            if (sourceRequest.hasError()) {
                projectedRequest = projectedRequest.next();
                continue;
            }
            if (sourceRequest.isCancelled()) {
                request.cancel();
                return;
            }

            Projection projection = projectedRequest.getProjection();
            if (RequestType.VERIFY_NODE_EXISTS == sourceRequest.getType()) {
                // We needed to verify the existance of a child node ...
                VerifyNodeExistsRequest verify = (VerifyNodeExistsRequest)sourceRequest;
                Location childInSource = verify.getActualLocationOfNode();
                Location childInRepos = getChildLocationWithCorrectSnsIndex(childInSource,
                                                                            federatedPath,
                                                                            childSnsIndexes,
                                                                            projection);
                request.addChild(childInRepos);
                if (federatedPath == null) federatedPath = childInRepos.getPath().getParent();
            } else {
                ReadNodeRequest readFromSource = (ReadNodeRequest)sourceRequest;
                Location sourceLocation = readFromSource.getActualLocationOfNode();
                if (sourceLocation.hasIdProperties()) {
                    // Accumulate the identification properties ...
                    for (Property propertyInSource : sourceLocation.getIdProperties()) {
                        Name name = propertyInSource.getName();
                        Property existing = actualLocation.getIdProperty(name);
                        if (existing != null) {
                            // Merge the property values ...
                            propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                        }
                        actualLocation = actualLocation.with(propertyInSource);
                    }
                }

                // Make sure we have an actual location ...
                actualLocation = determineActualLocation(actualLocation, sourceLocation, projection);
                if (federatedPath == null) federatedPath = actualLocation.getPath();

                // Add all the children from the source ...
                for (Location childInSource : readFromSource.getChildren()) {
                    request.addChild(getChildLocationWithCorrectSnsIndex(childInSource,
                                                                         federatedPath,
                                                                         childSnsIndexes,
                                                                         projection));
                }

                // Add all the properties ...
                for (Property propertyInSource : readFromSource.getProperties()) {
                    Name name = propertyInSource.getName();
                    Property existing = properties.get(name);
                    if (existing != null) {
                        // Merge the property values ...
                        propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                    }
                    properties.put(name, propertyInSource);
                }
                setCacheableInfo(request, readFromSource.getCachePolicy());
            }
            projectedRequest = projectedRequest.next();
            ++numMerged;
        }
        if (numMerged == 0) {
            // No source requests had results ...
            setPathNotFound(request, request.at(), federatedRequest.getFirstProjectedRequest());
        } else {
            if (!actualLocation.hasPath()) {
                assert federatedPath != null;
                actualLocation = actualLocation.with(federatedPath);
            }
            assert actualLocation.getPath() != null;
            request.setActualLocationOfNode(actualLocation);
        }
    }

    protected Location getChildLocationWithCorrectSnsIndex( Location childInSource,
                                                            Path federatedPath,
                                                            Map<Name, Integer> childSnsIndexes,
                                                            Projection projection ) {
        // Project back into the federated repository ...
        Path childPath = childInSource.getPath();
        if (childPath.isRoot() || federatedPath == null) {
            // We've lost the name of the child, so we need to recompute the path ...
            for (Path path : projection.getPathsInRepository(childInSource.getPath(), pathFactory)) {
                childPath = path;
                if (federatedPath == null) federatedPath = path.getParent();
                break;
            }
        }

        // Correct the same-name-sibling index for the child ...
        Name childName = childPath.getLastSegment().getName();
        Integer snsIndex = childSnsIndexes.get(childName);
        if (snsIndex == null) {
            snsIndex = new Integer(1);
            childSnsIndexes.put(childName, snsIndex);
        } else {
            snsIndex = new Integer(snsIndex.intValue() + 1);
            childSnsIndexes.put(childName, snsIndex);
        }
        Path newPath = pathFactory.create(federatedPath, childName, snsIndex.intValue());
        return childInSource.with(newPath);
    }

    /**
     * Sets the request {@link Request#setError(Throwable) error} to a {@link PathNotFoundException} that has the lowest existing
     * ancestor computed from the {@link PathNotFoundException}s in the projected requests.
     * 
     * @param original
     * @param originalLocation
     * @param projected
     */
    protected void setPathNotFound( Request original,
                                    Location originalLocation,
                                    ProjectedRequest projected ) {
        Path lowestExistingInFederated = pathFactory.createRootPath();
        while (projected != null) {
            Request projectedRequest = projected.getRequest();
            Throwable error = projectedRequest.getError();
            if (error instanceof PathNotFoundException) {
                PathNotFoundException notFound = (PathNotFoundException)error;
                Path lowestExisting = notFound.getLowestAncestorThatDoesExist();
                // Project back to the repository level ...
                for (Path federatedPath : projected.getProjection().getPathsInRepository(lowestExisting, pathFactory)) {
                    if (federatedPath.isAtOrBelow(lowestExistingInFederated)) {
                        lowestExistingInFederated = federatedPath;
                    }
                }
            }
            projected = projected.next();
        }
        original.setError(new PathNotFoundException(originalLocation, lowestExistingInFederated));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        Path federatedPath = request.of().getPath();
        Map<Name, Integer> childSnsIndexes = new HashMap<Name, Integer>();
        ProjectedRequest projectedRequest = federatedRequest.getFirstProjectedRequest();

        request.setCachePolicy(getDefaultCachePolicy());
        Location actualLocation = request.of();
        int numMerged = 0;
        while (projectedRequest != null) {
            Request sourceRequest = projectedRequest.getRequest();
            if (sourceRequest.hasError()) {
                projectedRequest = projectedRequest.next();
                continue;
            }
            if (sourceRequest.isCancelled()) {
                request.cancel();
                return;
            }

            Projection projection = projectedRequest.getProjection();
            if (RequestType.VERIFY_NODE_EXISTS == sourceRequest.getType()) {
                // We needed to verify the existance of a child node ...
                VerifyNodeExistsRequest verify = (VerifyNodeExistsRequest)sourceRequest;
                Location childInSource = verify.getActualLocationOfNode();
                Location childInRepos = getChildLocationWithCorrectSnsIndex(childInSource,
                                                                            federatedPath,
                                                                            childSnsIndexes,
                                                                            projection);
                request.addChild(childInRepos);
                if (federatedPath == null) federatedPath = childInRepos.getPath().getParent();
            } else {
                ReadAllChildrenRequest readFromSource = (ReadAllChildrenRequest)sourceRequest;
                Location sourceLocation = readFromSource.getActualLocationOfNode();
                if (sourceLocation.hasIdProperties()) {
                    // Accumulate the identification properties ...
                    for (Property propertyInSource : sourceLocation.getIdProperties()) {
                        Name name = propertyInSource.getName();
                        Property existing = actualLocation.getIdProperty(name);
                        if (existing != null) {
                            // Merge the property values ...
                            propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                        }
                        actualLocation = actualLocation.with(propertyInSource);
                    }
                }

                // Make sure we have an actual location ...
                actualLocation = determineActualLocation(actualLocation, readFromSource.getActualLocationOfNode(), projection);
                if (federatedPath == null) federatedPath = actualLocation.getPath();

                // Add all the children from the source ...
                for (Location childInSource : readFromSource.getChildren()) {
                    request.addChild(getChildLocationWithCorrectSnsIndex(childInSource,
                                                                         federatedPath,
                                                                         childSnsIndexes,
                                                                         projection));
                }
                setCacheableInfo(request, readFromSource.getCachePolicy());
            }

            projectedRequest = projectedRequest.next();
            ++numMerged;
        }
        if (numMerged == 0) {
            // No source requests had results ...
            setPathNotFound(request, request.of(), federatedRequest.getFirstProjectedRequest());
        } else {
            if (!actualLocation.hasPath()) {
                assert federatedPath != null;
                actualLocation = actualLocation.with(federatedPath);
            }
            request.setActualLocationOfNode(actualLocation);
        }
    }

    protected Location determineActualLocation( Location actual,
                                                Location inSource,
                                                Projection projection ) {
        if (actual.getPath() == null) {
            if (projection == null) {
                // It must be a placeholder node ...
                return inSource;
            }
            // Get the projection from the source-specific location ...
            Path pathInSource = inSource.getPath();
            for (Path path : projection.getPathsInRepository(pathInSource, pathFactory)) {
                return actual.with(path);
            }
        }
        return actual;
    }

    protected Location determineActualLocation( Location actualInSource,
                                                Projection projection ) {
        assert projection != null;
        // Get the projection from the source-specific location ...
        Path pathInSource = actualInSource.getPath();
        for (Path path : projection.getPathsInRepository(pathInSource, pathFactory)) {
            return actualInSource.with(path);
        }
        return actualInSource;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        Map<Name, Property> properties = request.getPropertiesByName();
        ProjectedRequest projectedRequest = federatedRequest.getFirstProjectedRequest();

        request.setCachePolicy(getDefaultCachePolicy());
        Location actualLocation = request.at();
        int numMerged = 0;
        while (projectedRequest != null) {
            ReadAllPropertiesRequest readFromSource = (ReadAllPropertiesRequest)projectedRequest.getRequest();
            if (readFromSource.hasError()) {
                projectedRequest = projectedRequest.next();
                continue;
            }
            if (readFromSource.isCancelled()) {
                request.cancel();
                return;
            }

            // Make sure we have an actual location ...
            Location sourceLocation = readFromSource.getActualLocationOfNode();
            actualLocation = determineActualLocation(actualLocation, sourceLocation, projectedRequest.getProjection());

            if (sourceLocation.hasIdProperties()) {
                // Accumulate the identification properties ...
                for (Property propertyInSource : sourceLocation.getIdProperties()) {
                    Name name = propertyInSource.getName();
                    Property existing = actualLocation.getIdProperty(name);
                    if (existing != null) {
                        // Merge the property values ...
                        propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                    }
                    actualLocation = actualLocation.with(propertyInSource);
                }
            }

            // Add all the properties ...
            for (Property propertyInSource : readFromSource.getProperties()) {
                Name name = propertyInSource.getName();
                Property existing = properties.get(name);
                if (existing != null) {
                    // Merge the property values ...
                    propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                }
                properties.put(name, propertyInSource);
            }
            setCacheableInfo(request, readFromSource.getCachePolicy());
            projectedRequest = projectedRequest.next();
            ++numMerged;
        }
        if (numMerged == 0) {
            // No source requests had results ...
            setPathNotFound(request, request.at(), federatedRequest.getFirstProjectedRequest());
        } else {
            request.setActualLocationOfNode(actualLocation);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadPropertyRequest)
     */
    @Override
    public void process( ReadPropertyRequest request ) {
        ProjectedRequest projectedRequest = federatedRequest.getFirstProjectedRequest();

        request.setCachePolicy(getDefaultCachePolicy());
        Location actualLocation = request.on();
        int numMerged = 0;
        while (projectedRequest != null) {
            ReadPropertyRequest readFromSource = (ReadPropertyRequest)projectedRequest.getRequest();
            if (readFromSource.hasError()) {
                projectedRequest = projectedRequest.next();
                continue;
            }
            if (readFromSource.isCancelled()) {
                request.cancel();
                return;
            }

            // Make sure we have an actual location ...
            Location sourceLocation = readFromSource.getActualLocationOfNode();
            actualLocation = determineActualLocation(actualLocation, sourceLocation, projectedRequest.getProjection());

            if (sourceLocation.hasIdProperties()) {
                // Accumulate the identification properties ...
                for (Property propertyInSource : sourceLocation.getIdProperties()) {
                    Name name = propertyInSource.getName();
                    Property existing = actualLocation.getIdProperty(name);
                    if (existing != null) {
                        // Merge the property values ...
                        propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                    }
                    actualLocation = actualLocation.with(propertyInSource);
                }
            }

            // Add all the properties ...
            Property read = readFromSource.getProperty();
            if (read != null) {
                Property existing = request.getProperty();
                if (existing != null) {
                    // Merge the property values ...
                    request.setProperty(merge(existing, read, propertyFactory, true));
                } else {
                    request.setProperty(read);
                }
            }
            setCacheableInfo(request, readFromSource.getCachePolicy());
            projectedRequest = projectedRequest.next();
            ++numMerged;
        }
        if (numMerged == 0) {
            // No source requests had results ...
            setPathNotFound(request, request.on(), federatedRequest.getFirstProjectedRequest());
        } else {
            request.setActualLocationOfNode(actualLocation);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        ProjectedRequest projectedRequest = federatedRequest.getFirstProjectedRequest();

        request.setCachePolicy(getDefaultCachePolicy());
        Location actualLocation = request.at();
        int numMerged = 0;
        // The first pass will only capture the actual ReadBranchRequests to the underlying sources ...
        Map<Path, Location> actualLocationsOfProxyNodes = new HashMap<Path, Location>();
        while (projectedRequest != null) {
            CacheableRequest fromSource = (CacheableRequest)projectedRequest.getRequest();
            if (fromSource.hasError()) {
                projectedRequest = projectedRequest.next();
                continue;
            }
            if (fromSource.isCancelled()) {
                request.cancel();
                return;
            }

            Projection projection = projectedRequest.getProjection();
            if (RequestType.READ_BRANCH == fromSource.getType()) {
                ReadBranchRequest readFromSource = (ReadBranchRequest)fromSource;
                for (Location parent : readFromSource) {
                    List<Location> children = readFromSource.getChildren(parent);
                    Map<Name, Property> properties = readFromSource.getPropertiesFor(parent);
                    projectToFederated(actualLocation, projection, request, parent, children, properties);
                }
                Location locationOfProxy = readFromSource.getActualLocationOfNode();
                // The location is in terms of the source, so get the projected location ...
                locationOfProxy = determineActualLocation(locationOfProxy, projection);
                actualLocationsOfProxyNodes.put(locationOfProxy.getPath(), locationOfProxy);
            }
            setCacheableInfo(request, fromSource.getCachePolicy());
            projectedRequest = projectedRequest.next();
            ++numMerged;
        }
        // Go through the requests and process the ReadNodeRequests (which were reading children of placeholders)...
        projectedRequest = federatedRequest.getFirstProjectedRequest();
        while (projectedRequest != null) {
            CacheableRequest fromSource = (CacheableRequest)projectedRequest.getRequest();
            Projection projection = projectedRequest.getProjection();
            if (RequestType.READ_NODE == fromSource.getType()) {
                ReadNodeRequest readFromSource = (ReadNodeRequest)fromSource;
                Location parent = readFromSource.getActualLocationOfNode();
                List<Location> children = readFromSource.getChildren();
                for (int i = 0; i != children.size(); ++i) {
                    Location child = children.get(i);
                    if (!child.hasIdProperties()) {
                        // The the child must have been a proxy node ...
                        Location actual = actualLocationsOfProxyNodes.get(child.getPath());
                        assert actual != null;
                        children.set(i, actual);
                    }
                }
                Map<Name, Property> properties = readFromSource.getPropertiesByName();
                projectToFederated(actualLocation, projection, request, parent, children, properties);
            }
            setCacheableInfo(request, fromSource.getCachePolicy());
            projectedRequest = projectedRequest.next();
        }

        if (numMerged == 0) {
            // No source requests had results ...
            setPathNotFound(request, request.at(), federatedRequest.getFirstProjectedRequest());
        } else {
            request.setActualLocationOfNode(actualLocation);
        }
    }

    /**
     * Project the supplied node information read from a source and update the supplied request.
     * 
     * @param ancestorInFederation the federated node under which this information is being projected; may not be null
     * @param projection the projection used to make the original source request; may not be null
     * @param request the federated request upon which the results are to be recorded; may not be null
     * @param parent the location of the parent in the source; may not be null
     * @param children the location of the children in the source; may be null or empty
     * @param propertiesByName the properties on the parent in the source; may be null or empty
     */
    protected void projectToFederated( Location ancestorInFederation,
                                       Projection projection,
                                       ReadBranchRequest request,
                                       Location parent,
                                       List<Location> children,
                                       Map<Name, Property> propertiesByName ) {
        Path ancestorPath = ancestorInFederation.getPath();
        if (projection == null) {
            // This is a placeholder node ...
            if (children != null) {
                // Add the children (to any existing children) ...
                List<Location> existing = request.getChildren(parent);
                if (existing == null) existing = new ArrayList<Location>(children.size());
                for (Location child : children) {
                    existing.add(child);
                }
                request.setChildren(parent, existing);
            }
            if (propertiesByName != null) {
                // Add the properties to any existing properties ...
                Map<Name, Property> propsByName = request.getPropertiesFor(parent);
                if (propsByName == null) propsByName = new HashMap<Name, Property>();
                for (Property property : propertiesByName.values()) {
                    Property existingProperty = propsByName.get(property.getName());
                    if (existingProperty != null) {
                        // Merge the property values ...
                        property = merge(existingProperty, property, propertyFactory, true);
                    }
                    propsByName.put(property.getName(), property);
                }
                request.setProperties(parent, propsByName.values());
            }
            return;
        }
        for (Path path : projection.getPathsInRepository(parent.getPath(), pathFactory)) {
            if (!path.isAtOrBelow(ancestorPath)) continue;

            // Determine the list of children ...
            Location parentInFederation = parent.with(path);
            if (children != null) {
                // Add the children to any existing children ...
                List<Location> existing = request.getChildren(parentInFederation);
                if (existing == null) existing = new ArrayList<Location>(children.size());
                for (Location child : children) {
                    Path childPath = pathFactory.create(path, child.getPath().getLastSegment());
                    existing.add(child.with(childPath));
                }
                request.setChildren(parentInFederation, existing);
            }

            // Set or update the properties ...
            if (propertiesByName != null) {
                Map<Name, Property> propsByName = request.getPropertiesFor(parentInFederation);
                if (propsByName == null) propsByName = new HashMap<Name, Property>();
                for (Property property : propertiesByName.values()) {
                    Property existingProperty = propsByName.get(property.getName());
                    if (existingProperty != null) {
                        // Merge the property values ...
                        property = merge(existingProperty, property, propertyFactory, true);
                    }
                    propsByName.put(property.getName(), property);
                }
                request.setProperties(parentInFederation, propsByName.values());
            }
            // We're done, since we found a path that's on the ancestor path ...
            return;
        }
    }

    /**
     * Project the supplied location in a source into its federated location. The projection is used to find the location under
     * the supplied ancestor. Any errors are recorded on the original request.
     * 
     * @param ancestorInFederation the ancestor in the federated repository; may not be null
     * @param projection the projection that should be used; may not be null
     * @param actualSourceLocation the actual location in the source that is to be projected back into the federated repository;
     *        may not be null
     * @param originalRequest the original request, if there are errors; may not be null
     * @return the location in the federated repository
     */
    protected Location projectToFederated( Location ancestorInFederation,
                                           Projection projection,
                                           Location actualSourceLocation,
                                           Request originalRequest ) {
        Path ancestorPath = ancestorInFederation.getPath();
        Path actualPathInSource = actualSourceLocation.getPath();
        // Project the actual location ...
        for (Path path : projection.getPathsInRepository(actualPathInSource, pathFactory)) {
            if (path.isAtOrBelow(ancestorPath)) {
                return actualSourceLocation.with(path);
            }
        }
        // Record that there was an error projecting the results ...
        String whereInSource = actualSourceLocation.getString(getExecutionContext().getNamespaceRegistry());
        String msg = GraphI18n.unableToProjectSourceInformationIntoWorkspace.text(whereInSource, getSourceName(), projection);
        originalRequest.setError(new InvalidRequestException(msg));
        return null;
    }

    /**
     * Project the supplied location in a source into its federated location. The projection is used to find the location under
     * the supplied ancestor. Any errors are recorded on the original request.
     * 
     * @param projection the projection that should be used; may not be null
     * @param actualSourceLocation the actual location in the source that is to be projected back into the federated repository;
     *        may not be null
     * @param originalRequest the original request, if there are errors; may not be null
     * @return the location in the federated repository
     */
    protected Location projectToFederated( Projection projection,
                                           Location actualSourceLocation,
                                           Request originalRequest ) {
        Path actualPathInSource = actualSourceLocation.getPath();
        // Project the actual location ...
        for (Path path : projection.getPathsInRepository(actualPathInSource, pathFactory)) {
            return actualSourceLocation.with(path);
        }
        // Record that there was an error projecting the results ...
        String whereInSource = actualSourceLocation.getString(getExecutionContext().getNamespaceRegistry());
        String msg = GraphI18n.unableToProjectSourceInformationIntoWorkspace.text(whereInSource, getSourceName(), projection);
        originalRequest.setError(new InvalidRequestException(msg));
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        Request projectedRequest = projected.getRequest();
        // Check the error on the projected request ...
        if (checkErrorOrCancel(request, projectedRequest)) return;

        // No error, so project the results back to the federated repository ...
        Location sourceLocation = null;
        if (RequestType.CREATE_NODE == projectedRequest.getType()) {
            CreateNodeRequest source = (CreateNodeRequest)projectedRequest;
            sourceLocation = source.getActualLocationOfNode();
        } else if (RequestType.READ_NODE == projectedRequest.getType()) {
            // In this case, the original request was to create the node only if it was missing,
            // but we knew it already exists because the parent was a placeholder and the child
            // mapped to an existing proxy node. Therefore, record the location...
            ReadNodeRequest source = (ReadNodeRequest)projectedRequest;
            sourceLocation = source.getActualLocationOfNode();
        }
        request.setActualLocationOfNode(projectToFederated(request.under(), projected.getProjection(), sourceLocation, request));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        UpdatePropertiesRequest source = (UpdatePropertiesRequest)projected.getRequest();
        if (checkErrorOrCancel(request, source)) return;
        Location sourceLocation = source.getActualLocationOfNode();
        request.setActualLocationOfNode(projectToFederated(request.on(), projected.getProjection(), sourceLocation, request));
        request.setNewProperties(source.getNewPropertyNames());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.SetPropertyRequest)
     */
    @Override
    public void process( SetPropertyRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        SetPropertyRequest source = (SetPropertyRequest)projected.getRequest();
        if (checkErrorOrCancel(request, source)) return;
        // Set the actual location and created flags ...
        Location sourceLocation = source.getActualLocationOfNode();
        request.setActualLocationOfNode(projectToFederated(request.on(), projected.getProjection(), sourceLocation, request));
        request.setNewProperty(source.isNewProperty());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RemovePropertyRequest)
     */
    @Override
    public void process( RemovePropertyRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        SetPropertyRequest source = (SetPropertyRequest)projected.getRequest();
        if (checkErrorOrCancel(request, source)) return;
        Location sourceLocation = source.getActualLocationOfNode();
        request.setActualLocationOfNode(projectToFederated(request.from(), projected.getProjection(), sourceLocation, request));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        // Do an initial check to make sure that there was no error on the source that prevented projection
        Request projectedSource = projected.getRequest();
        if (checkErrorOrCancel(request, projectedSource)) return;

        // Go through the projected requests, and look for the top-most node ...
        Location highest = null;
        while (projected != null) {
            // The projected request should either be a DeleteChildrenRequest (if the node being deleted is
            // at the top of a projection and therefore required to exist) or a DeleteBranchRequest (in all
            // other cases)...
            Location actual = null;
            Request sourceRequest = projected.getRequest();
            if (RequestType.DELETE_BRANCH == sourceRequest.getType()) {
                DeleteBranchRequest source = (DeleteBranchRequest)projected.getRequest();
                actual = source.getActualLocationOfNode();
            } else {
                DeleteChildrenRequest source = (DeleteChildrenRequest)projected.getRequest();
                actual = source.getActualLocationOfNode();
            }
            if (checkErrorOrCancel(request, sourceRequest)) return;
            if (!projected.isSameLocation() && projected.getProjection() != null) {
                actual = projectToFederated(request.at(), projected.getProjection(), actual, request);
            }
            if (highest == null) highest = actual;
            else if (highest.getPath().isDecendantOf(actual.getPath())) highest = actual;
            projected = projected.next();
        }
        assert highest != null;
        request.setActualLocationOfNode(highest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteChildrenRequest)
     */
    @Override
    public void process( DeleteChildrenRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        // Do an initial check to make sure that there was no error on the source that prevented projection
        Request projectedSource = projected.getRequest();
        if (checkErrorOrCancel(request, projectedSource)) return;

        // Go through the projected requests, and look for the top-most node ...
        Location highest = null;
        while (projected != null) {
            // The projected request should a DeleteChildrenRequest ...
            Request sourceRequest = projected.getRequest();
            DeleteChildrenRequest source = (DeleteChildrenRequest)projected.getRequest();
            Location actual = source.getActualLocationOfNode();
            if (checkErrorOrCancel(request, sourceRequest)) return;
            if (!projected.isSameLocation() && projected.getProjection() != null) {
                actual = projectToFederated(request.at(), projected.getProjection(), actual, request);
            }
            if (highest == null) highest = actual;
            else if (highest.getPath().isDecendantOf(actual.getPath())) highest = actual;
            projected = projected.next();
        }
        assert highest != null;
        request.setActualLocationOfNode(highest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        RenameNodeRequest source = (RenameNodeRequest)projected.getRequest();
        if (checkErrorOrCancel(request, source)) return;
        Location locationBefore = source.getActualLocationBefore();
        Location locationAfter = source.getActualLocationBefore();
        locationBefore = projectToFederated(request.at(), projected.getProjection(), locationBefore, request);
        locationAfter = projectToFederated(request.at(), projected.getSecondProjection(), locationAfter, request);
        request.setActualLocations(locationBefore, locationAfter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        CopyBranchRequest source = (CopyBranchRequest)projected.getRequest();
        if (checkErrorOrCancel(request, source)) return;
        Location locationBefore = source.getActualLocationBefore();
        Location locationAfter = source.getActualLocationBefore();
        locationBefore = projectToFederated(request.from(), projected.getProjection(), locationBefore, request);
        locationAfter = projectToFederated(request.into(), projected.getSecondProjection(), locationAfter, request);
        request.setActualLocations(locationBefore, locationAfter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        CloneBranchRequest source = (CloneBranchRequest)projected.getRequest();
        if (checkErrorOrCancel(request, source)) return;
        Location locationBefore = source.getActualLocationBefore();
        Location locationAfter = source.getActualLocationBefore();
        locationBefore = projectToFederated(request.from(), projected.getProjection(), locationBefore, request);
        locationAfter = projectToFederated(request.into(), projected.getSecondProjection(), locationAfter, request);
        request.setActualLocations(locationBefore, locationAfter);
        if (source.removeExisting()) {
            Set<Location> removed = new HashSet<Location>();
            for (Location location : request.getRemovedNodes()) {
                removed.add(projectToFederated(projected.getSecondProjection(), location, request));
            }
            request.setRemovedNodes(Collections.unmodifiableSet(removed));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        ProjectedRequest projected = federatedRequest.getFirstProjectedRequest();
        // Check the projection first ...
        if (checkErrorOrCancel(request, federatedRequest)) return;

        MoveBranchRequest source = (MoveBranchRequest)projected.getRequest();
        if (checkErrorOrCancel(request, source)) return;
        Location locationBefore = source.getActualLocationBefore();
        Location locationAfter = source.getActualLocationBefore();
        locationBefore = projectToFederated(request.from(), projected.getProjection(), locationBefore, request);
        Projection afterProjection = projected.getSecondProjection();
        if (afterProjection == null) projected.getProjection();
        locationAfter = projectToFederated(request.into(), afterProjection, locationAfter, request);
        request.setActualLocations(locationBefore, locationAfter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        ProjectedRequest projectedRequest = federatedRequest.getFirstProjectedRequest();

        Location actualLocation = Location.create(getExecutionContext().getValueFactories().getPathFactory().createRootPath());
        while (projectedRequest != null) {
            VerifyNodeExistsRequest readFromSource = (VerifyNodeExistsRequest)projectedRequest.getRequest();
            if (readFromSource.hasError()) {
                request.setError(readFromSource.getError());
                return;
            }
            request.setError(null);
            if (readFromSource.isCancelled()) {
                request.cancel();
                return;
            }

            Location sourceLocation = readFromSource.getActualLocationOfNode();
            if (sourceLocation.hasIdProperties()) {
                // Accumulate the identification properties ...
                for (Property propertyInSource : sourceLocation.getIdProperties()) {
                    Name name = propertyInSource.getName();
                    Property existing = actualLocation.getIdProperty(name);
                    if (existing != null) {
                        // Merge the property values ...
                        propertyInSource = merge(existing, propertyInSource, propertyFactory, true);
                    }
                    actualLocation = actualLocation.with(propertyInSource);
                }
            }
            projectedRequest = projectedRequest.next();
        }
        request.setActualRootLocation(actualLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        throw new UnsupportedOperationException(); // should never be called, since it's handled in the ForkProcessor
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called, since it's handled in the ForkProcessor
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called, since it's handled in the ForkProcessor
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        throw new UnsupportedOperationException(); // should never be called, since it's handled in the ForkProcessor
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

    protected boolean checkErrorOrCancel( Request request,
                                          FederatedRequest federatedRequest ) {
        if (federatedRequest.getFirstProjectedRequest() == null) {
            Request original = federatedRequest.original();
            if (original.hasError()) {
                // No source requests had results ...
                request.setError(original.getError());
                return true;
            }
            assert original.isCancelled();
            request.cancel();
            return true;
        }
        return false;
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
     * Merge the values from the two properties with the same name, returning a new property with the newly merged values.
     * <p>
     * The current algorithm merges the values by concatenating the values from <code>property1</code> and <code>property2</code>,
     * and if <code>removeDuplicates</code> is true any values in <code>property2</code> that are identical to values found in
     * <code>property1</code> are skipped.
     * </p>
     * 
     * @param property1 the first property; may not be null, and must have the same {@link Property#getName() name} as
     *        <code>property2</code>
     * @param property2 the second property; may not be null, and must have the same {@link Property#getName() name} as
     *        <code>property1</code>
     * @param factory the property factory, used to create the result
     * @param removeDuplicates true if this method removes any values in the second property that duplicate values found in the
     *        first property.
     * @return the property that contains the same {@link Property#getName() name} as the input properties, but with values that
     *         are merged from both of the input properties
     */
    protected Property merge( Property property1,
                              Property property2,
                              PropertyFactory factory,
                              boolean removeDuplicates ) {
        assert property1 != null;
        assert property2 != null;
        assert property1.getName().equals(property2.getName());
        if (property1.isEmpty()) return property2;
        if (property2.isEmpty()) return property1;

        // If they are both single-valued, then we can use a more efficient algorithm ...
        if (property1.isSingle() && property2.isSingle()) {
            Object value1 = property1.getValues().next();
            Object value2 = property2.getValues().next();
            if (removeDuplicates && ValueComparators.OBJECT_COMPARATOR.compare(value1, value2) == 0) return property1;
            return factory.create(property1.getName(), new Object[] {value1, value2});
        }

        // One or both properties are multi-valued, so use an algorithm that works with in all cases ...
        if (!removeDuplicates) {
            Iterator<?> valueIterator = new DualIterator(property1.getValues(), property2.getValues());
            return factory.create(property1.getName(), valueIterator);
        }

        // First copy all the values from property 1 ...
        Object[] values = new Object[property1.size() + property2.size()];
        int index = 0;
        for (Object property1Value : property1) {
            values[index++] = property1Value;
        }
        assert index == property1.size();
        // Now add any values of property2 that don't match a value in property1 ...
        for (Object property2Value : property2) {
            // Brute force, go through the values of property1 and compare ...
            boolean matched = false;
            for (Object property1Value : property1) {
                if (ValueComparators.OBJECT_COMPARATOR.compare(property1Value, property2Value) == 0) {
                    // The values are the same ...
                    matched = true;
                    break;
                }
            }
            if (!matched) values[index++] = property2Value;
        }
        if (index != values.length) {
            Object[] newValues = new Object[index];
            System.arraycopy(values, 0, newValues, 0, index);
            values = newValues;
        }
        return factory.create(property1.getName(), values);
    }

    protected static class DualIterator implements Iterator<Object> {

        private final Iterator<?>[] iterators;
        private Iterator<?> current;
        private int index = 0;

        protected DualIterator( Iterator<?>... iterators ) {
            assert iterators != null;
            assert iterators.length > 0;
            this.iterators = iterators;
            this.current = this.iterators[0];
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            if (this.current != null) return this.current.hasNext();
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            while (this.current != null) {
                if (this.current.hasNext()) return this.current.next();
                // Get the next iterator ...
                if (++this.index < iterators.length) {
                    this.current = this.iterators[this.index];
                } else {
                    this.current = null;
                }
            }
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
