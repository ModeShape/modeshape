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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.FederatedNode;
import org.jboss.dna.connector.federation.merge.MergePlan;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.NodeConflictBehavior;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class FederatingRequestProcessor extends RequestProcessor {

    private static final Set<Name> HIDDEN_PROPERTIES = Collections.singleton(DnaLexicon.MERGE_PLAN);

    private final Map<String, FederatedWorkspace> workspaces;
    private final FederatedWorkspace defaultWorkspace;
    private final RepositoryConnectionFactory connectionFactory;
    /** The set of all connections, including the cache connection */
    private final Map<String, RepositoryConnection> connectionsBySourceName;
    private Logger logger;

    /**
     * Create a command executor that federates (merges) the information from multiple sources described by the source projections
     * for the particular workspace specified by the request(s). The request processor will use the {@link Projection cache
     * projection} of each {@link FederatedWorkspace workspace} to identify the {@link Projection#getSourceName() repository
     * source} for the cache as well as the {@link Projection#getRules() rules} for how the paths are mapped in the cache. This
     * cache will be consulted first for the requested information, and will be kept up to date as changes are made to the
     * federated information.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param workspaces the configuration for each workspace, keyed by workspace name; may not be null
     * @param defaultWorkspace the default workspace; null if there is no default
     * @param connectionFactory the factory for {@link RepositoryConnection} instances
     */
    public FederatingRequestProcessor( ExecutionContext context,
                                       String sourceName,
                                       Map<String, FederatedWorkspace> workspaces,
                                       FederatedWorkspace defaultWorkspace,
                                       RepositoryConnectionFactory connectionFactory ) {
        super(sourceName, context);
        CheckArg.isNotEmpty(workspaces, "workspaces");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        this.workspaces = workspaces;
        this.connectionFactory = connectionFactory;
        this.logger = context.getLogger(getClass());
        this.connectionsBySourceName = new HashMap<String, RepositoryConnection>();
        this.defaultWorkspace = defaultWorkspace; // may be null
    }

    protected DateTime getCurrentTimeInUtc() {
        return getExecutionContext().getValueFactories().getDateFactory().createUtc();
    }

    /**
     * {@inheritDoc}
     * 
     * @see RequestProcessor#close()
     */
    @Override
    public void close() {
        try {
            super.close();
        } finally {
            // Make sure to close ALL open connections ...
            for (RepositoryConnection connection : connectionsBySourceName.values()) {
                if (connection == null) continue;
                try {
                    connection.close();
                } catch (Throwable t) {
                    logger.debug("Error while closing connection to {0}", connection.getSourceName());
                }
            }
            connectionsBySourceName.clear();
        }
    }

    protected RepositoryConnection getConnectionToCacheFor( FederatedWorkspace workspace ) throws RepositorySourceException {
        return getConnection(workspace.getCacheProjection());
    }

    protected RepositoryConnection getConnection( Projection projection ) throws RepositorySourceException {
        String sourceName = projection.getSourceName();
        RepositoryConnection connection = connectionsBySourceName.get(sourceName);
        if (connection == null) {
            connection = connectionFactory.createConnection(sourceName);
            connectionsBySourceName.put(sourceName, connection);
        }
        return connection;
    }

    protected Set<String> getOpenConnections() {
        return connectionsBySourceName.keySet();
    }

    /**
     * Utility to obtain the federated workspace referenced by the request. This method supports using the default workspace if
     * the workspace name is null. If no such workspace, the request is marked with an appropriate error.
     * 
     * @param request the request; may not be null
     * @param workspaceName the name of the workspace; may be null if the default workspace should be used
     * @return the federated workspace, or null if none was found
     */
    protected FederatedWorkspace getWorkspace( Request request,
                                               String workspaceName ) {
        FederatedWorkspace workspace = null;
        if (workspaceName == null) {
            if (defaultWorkspace != null) return defaultWorkspace;
            // There is no default, so record the error ...
            String msg = FederationI18n.noDefaultWorkspace.text(getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        }
        workspace = workspaces.get(workspaceName);
        if (workspace == null) {
            // There is no workspace with this name, so record an error ...
            String msg = FederationI18n.workspaceDoesNotExist.text(getSourceName(), workspaceName);
            request.setError(new InvalidWorkspaceException(msg));
        }
        return workspace;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        FederatedWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        ReadNodeRequest nodeInfo = getNode(request.of(), workspace);
        if (nodeInfo.hasError()) return;
        for (Location child : nodeInfo.getChildren()) {
            request.addChild(child);
        }
        request.setActualLocationOfNode(nodeInfo.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        FederatedWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        ReadNodeRequest nodeInfo = getNode(request.at(), workspace);
        if (nodeInfo.hasError()) return;
        for (Property property : nodeInfo.getProperties()) {
            if (HIDDEN_PROPERTIES.contains(property.getName())) continue;
            request.addProperty(property);
        }
        request.setActualLocationOfNode(nodeInfo.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        FederatedWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        ReadNodeRequest nodeInfo = getNode(request.at(), workspace);
        if (nodeInfo.hasError()) return;
        for (Property property : nodeInfo.getProperties()) {
            if (HIDDEN_PROPERTIES.contains(property.getName())) continue;
            request.addProperty(property);
        }
        for (Location child : nodeInfo.getChildren()) {
            request.addChild(child);
        }
        request.setActualLocationOfNode(nodeInfo.getActualLocationOfNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        FederatedWorkspace workspace = getWorkspace(request, request.workspaceName());
        if (workspace != null) {
            request.setActualWorkspaceName(workspace.getName());
            Location root = Location.create(getExecutionContext().getValueFactories().getPathFactory().createRootPath());
            ReadNodeRequest nodeInfo = getNode(root, workspace);
            if (nodeInfo.hasError()) return;
            request.setActualRootLocation(nodeInfo.getActualLocationOfNode());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        request.setAvailableWorkspaceNames(workspaces.keySet());
        super.setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the node information from the underlying sources or, if possible, from the cache.
     * 
     * @param location the location of the node to be returned
     * @param workspace the federated workspace configuration; may be null
     * @return the node information
     * @throws RepositorySourceException
     */
    protected ReadNodeRequest getNode( Location location,
                                       FederatedWorkspace workspace ) throws RepositorySourceException {
        final ExecutionContext context = getExecutionContext();

        // Check the cache first ...
        RepositoryConnection cacheConnection = getConnectionToCacheFor(workspace);
        ReadNodeRequest fromCache = new ReadNodeRequest(location, workspace.getCacheProjection().getWorkspaceName());
        cacheConnection.execute(context, fromCache);

        // Look at the cache results from the cache for problems, or if found a plan in the cache look
        // at the contributions. We'll be putting together the set of source names for which we need to
        // get the contributions.
        Set<String> sourceNames = null;
        List<Contribution> contributions = new LinkedList<Contribution>();

        if (fromCache.hasError()) {
            Throwable error = fromCache.getError();
            if (!(error instanceof PathNotFoundException)) return fromCache;

            // The path was not found in the cache, so since we don't know whether the ancestors are federated
            // from multiple source nodes, we need to populate the cache starting with the lowest ancestor
            // that already exists in the cache.
            PathNotFoundException notFound = (PathNotFoundException)fromCache.getError();
            Path lowestExistingAncestor = notFound.getLowestAncestorThatDoesExist();
            if (location.hasPath()) {
                Path path = location.getPath();
                Path ancestor = path.getParent();
                if (!ancestor.equals(lowestExistingAncestor)) {
                    // Load the nodes along the path below the existing ancestor, down to (but excluding) the desired path
                    Path pathToLoad = ancestor;
                    while (!pathToLoad.equals(lowestExistingAncestor)) {
                        Location locationToLoad = Location.create(pathToLoad);
                        loadContributionsFromSources(locationToLoad, workspace, null, contributions); // sourceNames may be
                        // null or empty
                        FederatedNode mergedNode = createFederatedNode(locationToLoad, workspace, contributions, true);
                        if (mergedNode == null) {
                            // No source had a contribution ...
                            I18n msg = FederationI18n.nodeDoesNotExistAtPath;
                            fromCache.setError(new PathNotFoundException(location, ancestor, msg.text(path, ancestor)));
                            return fromCache;
                        }
                        contributions.clear();
                        // Move to the next child along the path ...
                        pathToLoad = pathToLoad.getParent();
                    }
                }

            }

            // At this point, all ancestors exist ...
        } else {
            // There is no error, so look for the merge plan ...
            MergePlan mergePlan = getMergePlan(fromCache);
            if (mergePlan != null) {
                // We found the merge plan, so check whether it's still valid ...
                final DateTime now = getCurrentTimeInUtc();
                if (!mergePlan.isExpired(now)) {
                    // It is still valid, so check whether any contribution is from a non-existant projection ...
                    for (Contribution contribution : mergePlan) {
                        if (!workspace.contains(contribution.getSourceName(), contribution.getWorkspaceName())) {
                            // TODO: Record that the cached contribution is from a source that is no longer in this repository
                        }
                    }
                    return fromCache;
                }

                // At least one of the contributions is expired, so go through the contributions and place
                // the valid contributions in the 'contributions' list; any expired contribution
                // needs to be loaded by adding the name to the 'sourceNames'
                if (mergePlan.getContributionCount() > 0) {
                    sourceNames = new HashSet<String>();
                    for (Contribution contribution : mergePlan) {
                        if (contribution.isExpired(now)) {
                            sourceNames.add(contribution.getSourceName());
                            contributions.add(contribution);
                        }
                    }
                }
            }
        }

        // Get the contributions from the sources given their names ...
        location = fromCache.getActualLocationOfNode();
        if (location == null) {
            // Not yet in the cache ...
            location = fromCache.at();
        }
        loadContributionsFromSources(location, workspace, sourceNames, contributions); // sourceNames may be null or empty
        FederatedNode mergedNode = createFederatedNode(location, workspace, contributions, true);
        if (mergedNode == null) {
            // No source had a contribution ...
            if (location.hasPath()) {
                Path ancestor = location.getPath().getParent();
                I18n msg = FederationI18n.nodeDoesNotExistAtPath;
                fromCache.setError(new PathNotFoundException(location, ancestor, msg.text(location, ancestor)));
                return fromCache;
            }
            I18n msg = FederationI18n.nodeDoesNotExistAtLocation;
            fromCache.setError(new PathNotFoundException(location, null, msg.text(location)));
            return fromCache;
        }
        return mergedNode;
    }

    protected FederatedNode createFederatedNode( Location location,
                                                 FederatedWorkspace federatedWorkspace,
                                                 List<Contribution> contributions,
                                                 boolean updateCache ) throws RepositorySourceException {
        assert location != null;

        // If there are no contributions from any source ...
        boolean foundNonEmptyContribution = false;
        for (Contribution contribution : contributions) {
            assert contribution != null;
            if (!contribution.isEmpty()) {
                foundNonEmptyContribution = true;
                break;
            }
        }
        if (!foundNonEmptyContribution) return null;
        if (logger.isTraceEnabled()) {
            logger.trace("Loaded {0} from sources, resulting in these contributions:", location);
            int i = 0;
            for (Contribution contribution : contributions) {
                logger.trace("  {0} {1}", ++i, contribution);
            }
        }

        // Create the node, and use the existing UUID if one is found in the cache ...
        ExecutionContext context = getExecutionContext();
        assert context != null;
        FederatedNode mergedNode = new FederatedNode(location.with((UUID)null), federatedWorkspace.getName());

        // Merge the results into a single set of results ...
        assert contributions.size() > 0;
        federatedWorkspace.getMergingStrategy().merge(mergedNode, contributions, context);
        if (mergedNode.getCachePolicy() == null) {
            mergedNode.setCachePolicy(federatedWorkspace.getCachePolicy());
        }
        if (updateCache) {
            // Place the results into the cache ...
            updateCache(federatedWorkspace, mergedNode);
        }
        // And return the results ...
        return mergedNode;
    }

    /**
     * Load the node at the supplied location from the sources with the supplied name, returning the information. This method
     * always obtains the information from the sources and does not use or update the cache.
     * 
     * @param location the location of the node that is to be loaded
     * @param federatedWorkspace the federated workspace
     * @param sourceNames the names of the sources from which contributions are to be loaded; may be empty or null if all
     *        contributions from all sources are to be loaded
     * @param contributions the list into which the contributions are to be placed
     * @throws RepositorySourceException
     */
    protected void loadContributionsFromSources( Location location,
                                                 FederatedWorkspace federatedWorkspace,
                                                 Set<String> sourceNames,
                                                 List<Contribution> contributions ) throws RepositorySourceException {
        // At this point, there is no merge plan, so read information from the sources ...
        final ExecutionContext context = getExecutionContext();
        final PathFactory pathFactory = context.getValueFactories().getPathFactory();

        CachePolicy cachePolicy = federatedWorkspace.getCachePolicy();
        // If the location has no path, then we have to submit a request to ALL sources ...
        if (!location.hasPath()) {
            for (Projection projection : federatedWorkspace.getSourceProjections()) {
                final String source = projection.getSourceName();
                final String workspace = projection.getSourceName();
                if (sourceNames != null && !sourceNames.contains(source)) continue;
                final RepositoryConnection sourceConnection = getConnection(projection);
                if (sourceConnection == null) continue; // No source exists by this name
                // Submit the request ...
                ReadNodeRequest request = new ReadNodeRequest(location, federatedWorkspace.getName());
                sourceConnection.execute(context, request);
                if (request.hasError()) continue;

                // Figure out how long we can cache this contribution ...
                long minimumTimeToLive = Long.MAX_VALUE;
                if (cachePolicy != null) {
                    minimumTimeToLive = Math.min(minimumTimeToLive, cachePolicy.getTimeToLive());
                }
                CachePolicy requestCachePolicy = request.getCachePolicy();
                if (requestCachePolicy != null) {
                    minimumTimeToLive = Math.min(minimumTimeToLive, requestCachePolicy.getTimeToLive());
                } else {
                    // See if the source has a default policy ...
                    CachePolicy sourceCachePolicy = sourceConnection.getDefaultCachePolicy();
                    if (sourceCachePolicy != null) {
                        minimumTimeToLive = Math.min(minimumTimeToLive, sourceCachePolicy.getTimeToLive());
                    }
                }
                // The expiration time should be the smallest of the minimum TTL values ...
                DateTime expirationTime = null;
                if (minimumTimeToLive < Long.MAX_VALUE) {
                    expirationTime = getCurrentTimeInUtc().plus(minimumTimeToLive, TimeUnit.MILLISECONDS);
                }

                // Convert the locations of the children (relative to the source) to be relative to this node
                Contribution contribution = Contribution.create(source,
                                                                workspace,
                                                                request.getActualLocationOfNode(),
                                                                expirationTime,
                                                                request.getProperties(),
                                                                request.getChildren());
                contributions.add(contribution);
            }
        }

        // Otherwise, we can do it by path and projections ...
        Path path = location.getPath();
        for (Projection projection : federatedWorkspace.getSourceProjections()) {
            final String source = projection.getSourceName();
            final String workspace = projection.getWorkspaceName();
            if (sourceNames != null && !sourceNames.contains(source)) continue;
            final RepositoryConnection sourceConnection = getConnection(projection);
            if (sourceConnection == null) continue; // No source exists by this name
            // Get the cached information ...
            DateTime expirationTime = null;
            if (cachePolicy != null) {
                expirationTime = getCurrentTimeInUtc().plus(cachePolicy.getTimeToLive(), TimeUnit.MILLISECONDS);
            }
            // Get the paths-in-source where we should fetch node contributions ...
            Set<Path> pathsInSource = projection.getPathsInSource(path, pathFactory);
            if (pathsInSource.isEmpty()) {
                // The source has no contributions, but see whether the project exists BELOW this path.
                // We do this by getting the top-level repository paths of the projection, and then
                // use those to figure out the children of the nodes.
                Contribution contribution = null;
                List<Path> topLevelPaths = projection.getTopLevelPathsInRepository(pathFactory);
                Location input = Location.create(path);
                switch (topLevelPaths.size()) {
                    case 0:
                        break;
                    case 1: {
                        Path topLevelPath = topLevelPaths.iterator().next();
                        if (path.isAncestorOf(topLevelPath)) {
                            Location child = Location.create(topLevelPath);
                            contribution = Contribution.createPlaceholder(source, workspace, input, expirationTime, child);
                        }
                        break;
                    }
                    default: {
                        // We assume that the top-level paths do not overlap ...
                        List<Location> children = new ArrayList<Location>(topLevelPaths.size());
                        for (Path topLevelPath : topLevelPaths) {
                            if (path.isAncestorOf(topLevelPath)) {
                                children.add(Location.create(topLevelPath));
                            }
                        }
                        if (children.size() > 0) {
                            contribution = Contribution.createPlaceholder(source, workspace, input, expirationTime, children);
                        }
                    }
                }
                if (contribution == null) contribution = Contribution.create(source, workspace, expirationTime);
                contributions.add(contribution);
            } else {
                // There is at least one (real) contribution ...

                // Get the contributions ...
                final int numPaths = pathsInSource.size();
                if (numPaths == 1) {
                    Path pathInSource = pathsInSource.iterator().next();
                    ReadNodeRequest fromSource = new ReadNodeRequest(Location.create(pathInSource), workspace);
                    sourceConnection.execute(getExecutionContext(), fromSource);
                    if (!fromSource.hasError()) {
                        Collection<Property> properties = fromSource.getProperties();
                        List<Location> children = fromSource.getChildren();

                        // Figure out how long we can cache this contribution ...
                        long minimumTimeToLive = Long.MAX_VALUE;
                        if (cachePolicy != null) {
                            minimumTimeToLive = Math.min(minimumTimeToLive, cachePolicy.getTimeToLive());
                        }
                        CachePolicy requestCachePolicy = fromSource.getCachePolicy();
                        if (requestCachePolicy != null) {
                            minimumTimeToLive = Math.min(minimumTimeToLive, requestCachePolicy.getTimeToLive());
                        } else {
                            // See if the source has a default policy ...
                            CachePolicy sourceCachePolicy = sourceConnection.getDefaultCachePolicy();
                            if (sourceCachePolicy != null) {
                                minimumTimeToLive = Math.min(minimumTimeToLive, sourceCachePolicy.getTimeToLive());
                            }
                        }
                        // The expiration time should be the smallest of the minimum TTL values ...
                        expirationTime = null;
                        if (minimumTimeToLive < Long.MAX_VALUE) {
                            expirationTime = getCurrentTimeInUtc().plus(minimumTimeToLive, TimeUnit.MILLISECONDS);
                        }

                        Location actualLocation = fromSource.getActualLocationOfNode();
                        Contribution contribution = Contribution.create(source,
                                                                        workspace,
                                                                        actualLocation,
                                                                        expirationTime,
                                                                        properties,
                                                                        children);
                        contributions.add(contribution);
                    }
                } else {
                    List<Request> fromSourceCommands = new ArrayList<Request>(numPaths);
                    for (Path pathInSource : pathsInSource) {
                        fromSourceCommands.add(new ReadNodeRequest(Location.create(pathInSource), workspace));
                    }
                    Request request = CompositeRequest.with(fromSourceCommands);
                    sourceConnection.execute(context, request);
                    for (Request requestObj : fromSourceCommands) {
                        ReadNodeRequest fromSource = (ReadNodeRequest)requestObj;
                        if (fromSource.hasError()) continue;

                        // Figure out how long we can cache this contribution ...
                        long minimumTimeToLive = Long.MAX_VALUE;
                        if (cachePolicy != null) {
                            minimumTimeToLive = Math.min(minimumTimeToLive, cachePolicy.getTimeToLive());
                        }
                        CachePolicy requestCachePolicy = fromSource.getCachePolicy();
                        if (requestCachePolicy != null) {
                            minimumTimeToLive = Math.min(minimumTimeToLive, requestCachePolicy.getTimeToLive());
                        } else {
                            // See if the source has a default policy ...
                            CachePolicy sourceCachePolicy = sourceConnection.getDefaultCachePolicy();
                            if (sourceCachePolicy != null) {
                                minimumTimeToLive = Math.min(minimumTimeToLive, sourceCachePolicy.getTimeToLive());
                            }
                        }
                        // The expiration time should be the smallest of the minimum TTL values ...
                        expirationTime = null;
                        if (minimumTimeToLive < Long.MAX_VALUE) {
                            expirationTime = getCurrentTimeInUtc().plus(minimumTimeToLive, TimeUnit.MILLISECONDS);
                        }

                        List<Location> children = fromSource.getChildren();
                        Contribution contribution = Contribution.create(source,
                                                                        workspace,
                                                                        fromSource.getActualLocationOfNode(),
                                                                        expirationTime,
                                                                        fromSource.getProperties(),
                                                                        children);
                        contributions.add(contribution);
                    }
                }
            }
        }
    }

    protected MergePlan getMergePlan( ReadNodeRequest request ) {
        Property mergePlanProperty = request.getPropertiesByName().get(DnaLexicon.MERGE_PLAN);
        if (mergePlanProperty == null || mergePlanProperty.isEmpty()) {
            return null;
        }
        Object value = mergePlanProperty.getValues().next();
        return value instanceof MergePlan ? (MergePlan)value : null;
    }

    protected void updateCache( FederatedWorkspace federatedWorkspace,
                                FederatedNode mergedNode ) throws RepositorySourceException {
        final ExecutionContext context = getExecutionContext();
        final RepositoryConnection cacheConnection = getConnectionToCacheFor(federatedWorkspace);
        final Location location = mergedNode.at();
        final Path path = location.getPath();
        final String cacheWorkspace = federatedWorkspace.getCacheProjection().getWorkspaceName();
        assert path != null;
        List<Request> requests = new ArrayList<Request>();
        Name childName = null;

        // If the merged node has a merge plan, then add it to the properties if it is not already there ...
        Map<Name, Property> properties = mergedNode.getPropertiesByName();
        MergePlan mergePlan = mergedNode.getMergePlan();
        if (mergePlan != null) {
            // Record the merge plan on the merged node ...
            Property mergePlanProperty = getExecutionContext().getPropertyFactory().create(DnaLexicon.MERGE_PLAN,
                                                                                           (Object)mergePlan);
            properties.put(mergePlanProperty.getName(), mergePlanProperty);
        }

        if (path.isRoot()) {
            // Set the properties ...
            requests.add(new UpdatePropertiesRequest(location, cacheWorkspace, properties));
        } else {
            // This is not the root node, so we need to create the node ...
            final Location parentLocation = Location.create(path.getParent());
            childName = path.getLastSegment().getName();
            requests.add(new CreateNodeRequest(parentLocation, cacheWorkspace, childName, NodeConflictBehavior.REPLACE,
                                               mergedNode.getProperties()));
        }

        // Now create all of the children that this federated node knows of ...
        for (Location child : mergedNode.getChildren()) {
            childName = child.getPath().getLastSegment().getName();
            requests.add(new CreateNodeRequest(location, cacheWorkspace, childName, NodeConflictBehavior.APPEND));
        }
        cacheConnection.execute(context, CompositeRequest.with(requests));
    }
}
