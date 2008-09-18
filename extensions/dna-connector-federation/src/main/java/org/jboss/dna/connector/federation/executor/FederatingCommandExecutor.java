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
package org.jboss.dna.connector.federation.executor;

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
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.federation.FederationI18n;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.FederatedNode;
import org.jboss.dna.connector.federation.merge.MergePlan;
import org.jboss.dna.connector.federation.merge.strategy.MergeStrategy;
import org.jboss.dna.connector.federation.merge.strategy.OneContributionMergeStrategy;
import org.jboss.dna.connector.federation.merge.strategy.SimpleMergeStrategy;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.commands.GetChildrenCommand;
import org.jboss.dna.graph.commands.GetNodeCommand;
import org.jboss.dna.graph.commands.GetPropertiesCommand;
import org.jboss.dna.graph.commands.GraphCommand;
import org.jboss.dna.graph.commands.NodeConflictBehavior;
import org.jboss.dna.graph.commands.basic.BasicCreateNodeCommand;
import org.jboss.dna.graph.commands.basic.BasicGetNodeCommand;
import org.jboss.dna.graph.commands.executor.AbstractCommandExecutor;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryConnectionFactory;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.Path.Segment;
import org.jboss.dna.graph.properties.basic.BasicSingleValueProperty;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class FederatingCommandExecutor extends AbstractCommandExecutor {

    private final Name uuidPropertyName;
    private final Name mergePlanPropertyName;
    private final CachePolicy defaultCachePolicy;
    private final Projection cacheProjection;
    private final List<Projection> sourceProjections;
    private final Set<String> sourceNames;
    private final RepositoryConnectionFactory connectionFactory;
    private MergeStrategy mergingStrategy;
    /** The set of all connections, including the cache connection */
    private final Map<String, RepositoryConnection> connectionsBySourceName;
    /** A direct reference to the cache connection */
    private RepositoryConnection cacheConnection;
    private Logger logger;

    /**
     * Create a command executor that federates (merges) the information from multiple sources described by the source
     * projections. The resulting command executor does not first consult a cache for the merged information; if a cache is
     * desired, see
     * {@link #FederatingCommandExecutor(ExecutionContext, String, Projection, CachePolicy, List, RepositoryConnectionFactory)
     * constructor} that takes a {@link Projection cache projection}.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param sourceProjections the source projections; may not be null
     * @param connectionFactory the factory for {@link RepositoryConnection} instances
     */
    public FederatingCommandExecutor( ExecutionContext context,
                                      String sourceName,
                                      List<Projection> sourceProjections,
                                      RepositoryConnectionFactory connectionFactory ) {
        this(context, sourceName, null, null, sourceProjections, connectionFactory);
    }

    /**
     * Create a command executor that federates (merges) the information from multiple sources described by the source
     * projections. The resulting command executor will use the supplied {@link Projection cache projection} to identify the
     * {@link Projection#getSourceName() repository source} for the cache as well as the {@link Projection#getRules() rules} for
     * how the paths are mapped in the cache. This cache will be consulted first for the requested information, and will be kept
     * up to date as changes are made to the federated information.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param cacheProjection the projection used for the cached information; may be null if there is no cache
     * @param defaultCachePolicy the default caching policy that outlines the length of time that information should be cached, or
     *        null if there is no cache or no specific cache policy
     * @param sourceProjections the source projections; may not be null
     * @param connectionFactory the factory for {@link RepositoryConnection} instances
     */
    public FederatingCommandExecutor( ExecutionContext context,
                                      String sourceName,
                                      Projection cacheProjection,
                                      CachePolicy defaultCachePolicy,
                                      List<Projection> sourceProjections,
                                      RepositoryConnectionFactory connectionFactory ) {
        super(context, sourceName);
        assert sourceProjections != null;
        assert connectionFactory != null;
        assert cacheProjection != null ? defaultCachePolicy != null : defaultCachePolicy == null;
        this.cacheProjection = cacheProjection;
        this.defaultCachePolicy = defaultCachePolicy;
        this.sourceProjections = sourceProjections;
        this.connectionFactory = connectionFactory;
        this.logger = context.getLogger(getClass());
        this.connectionsBySourceName = new HashMap<String, RepositoryConnection>();
        this.uuidPropertyName = context.getValueFactories().getNameFactory().create(DnaLexicon.UUID);
        this.mergePlanPropertyName = context.getValueFactories().getNameFactory().create(DnaLexicon.MERGE_PLAN);
        this.sourceNames = new HashSet<String>();
        for (Projection projection : this.sourceProjections) {
            this.sourceNames.add(projection.getSourceName());
        }
        setMergingStrategy(null);
    }

    /**
     * @param mergingStrategy Sets mergingStrategy to the specified value.
     */
    public void setMergingStrategy( MergeStrategy mergingStrategy ) {
        if (mergingStrategy != null) {
            this.mergingStrategy = mergingStrategy;
        } else {
            if (this.sourceProjections.size() == 1 && this.sourceProjections.get(0).isSimple()) {
                this.mergingStrategy = new OneContributionMergeStrategy();
            } else {
                this.mergingStrategy = new SimpleMergeStrategy();
            }
        }
        assert this.mergingStrategy != null;
    }

    /**
     * Get an unmodifiable list of the immutable source projections.
     * 
     * @return the set of projections used as sources; never null
     */
    public List<Projection> getSourceProjections() {
        return Collections.unmodifiableList(sourceProjections);
    }

    /**
     * Get the projection defining the cache.
     * 
     * @return the cache projection
     */
    public Projection getCacheProjection() {
        return cacheProjection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#close()
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
            try {
                if (this.cacheConnection != null) this.cacheConnection.close();
            } finally {
                this.cacheConnection = null;
            }
        }
    }

    protected RepositoryConnection getConnectionToCache() throws RepositorySourceException {
        if (this.cacheConnection == null) {
            this.cacheConnection = getConnection(this.cacheProjection);
        }
        assert this.cacheConnection != null;
        return this.cacheConnection;
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
     * {@inheritDoc}
     * <p>
     * This class overrides the {@link AbstractCommandExecutor#execute(GetNodeCommand) default behavior} and instead processes the
     * command in a more efficient manner.
     * </p>
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.GetNodeCommand)
     */
    @Override
    public void execute( GetNodeCommand command ) throws RepositorySourceException {
        BasicGetNodeCommand nodeInfo = getNode(command.getPath());
        if (nodeInfo.hasError()) return;
        for (Property property : nodeInfo.getProperties()) {
            command.setProperty(property);
        }
        for (Segment child : nodeInfo.getChildren()) {
            command.addChild(child, nodeInfo.getChildIdentityProperties(child));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.GetPropertiesCommand)
     */
    @Override
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException {
        BasicGetNodeCommand nodeInfo = getNode(command.getPath());
        if (nodeInfo.hasError()) return;
        for (Property property : nodeInfo.getProperties()) {
            command.setProperty(property);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.GetChildrenCommand)
     */
    @Override
    public void execute( GetChildrenCommand command ) throws RepositorySourceException {
        BasicGetNodeCommand nodeInfo = getNode(command.getPath());
        if (nodeInfo.hasError()) return;
        for (Segment child : nodeInfo.getChildren()) {
            command.addChild(child, nodeInfo.getChildIdentityProperties(child));
        }
    }

    /**
     * Get the node information from the underlying sources or, if possible, from the cache.
     * 
     * @param path the path of the node to be returned
     * @return the node information
     * @throws RepositorySourceException
     */
    protected BasicGetNodeCommand getNode( Path path ) throws RepositorySourceException {
        // Check the cache first ...
        final ExecutionContext context = getExecutionContext();
        RepositoryConnection cacheConnection = getConnectionToCache();
        BasicGetNodeCommand fromCache = new BasicGetNodeCommand(path);
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
            Path ancestor = path.getParent();

            if (!ancestor.equals(lowestExistingAncestor)) {
                // Load the nodes along the path below the existing ancestor, down to (but excluding) the desired path
                Path pathToLoad = path.getParent();
                while (!pathToLoad.equals(lowestExistingAncestor)) {
                    loadContributionsFromSources(pathToLoad, null, contributions); // sourceNames may be null or empty
                    FederatedNode mergedNode = createFederatedNode(null, pathToLoad, contributions, true);
                    if (mergedNode == null) {
                        // No source had a contribution ...
                        I18n msg = FederationI18n.nodeDoesNotExistAtPath;
                        fromCache.setError(new PathNotFoundException(path, ancestor, msg.text(path, ancestor)));
                        return fromCache;
                    }
                    contributions.clear();
                    // Move to the next child along the path ...
                    pathToLoad = pathToLoad.getParent();
                }
            }
            // At this point, all ancestors exist ...
        } else {
            // There is no error, so look for the merge plan ...
            MergePlan mergePlan = getMergePlan(fromCache);
            if (mergePlan != null) {
                // We found the merge plan, so check whether it's still valid ...
                final DateTime now = getCurrentTimeInUtc();
                if (mergePlan.isExpired(now)) {
                    // It is still valid, so check whether any contribution is from a non-existant projection ...
                    for (Contribution contribution : mergePlan) {
                        if (!this.sourceNames.contains(contribution.getSourceName())) {
                            // TODO: Record that the cached contribution is from a source that is no longer in this repository
                        }
                    }
                    return fromCache;
                }

                // At least one of the contributions is expired, so go through the contributions and place
                // the valid contributions in the 'contributions' list; any expired contribution
                // needs to be loaded by adding the name to the 'sourceNames'
                if (mergePlan.getContributionCount() > 0) {
                    sourceNames = new HashSet<String>(sourceNames);
                    for (Contribution contribution : mergePlan) {
                        if (!contribution.isExpired(now)) {
                            sourceNames.remove(contribution.getSourceName());
                            contributions.add(contribution);
                        }
                    }
                }
            }
        }

        // Get the contributions from the sources given their names ...
        loadContributionsFromSources(path, sourceNames, contributions); // sourceNames may be null or empty
        FederatedNode mergedNode = createFederatedNode(fromCache, path, contributions, true);
        if (mergedNode == null) {
            // No source had a contribution ...
            Path ancestor = path.getParent();
            I18n msg = FederationI18n.nodeDoesNotExistAtPath;
            fromCache.setError(new PathNotFoundException(path, ancestor, msg.text(path, ancestor)));
            return fromCache;
        }
        return mergedNode;
    }

    protected FederatedNode createFederatedNode( BasicGetNodeCommand fromCache,
                                                 Path path,
                                                 List<Contribution> contributions,
                                                 boolean updateCache ) throws RepositorySourceException {

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
            logger.trace("Loaded {0} from sources, resulting in these contributions:", path);
            int i = 0;
            for (Contribution contribution : contributions) {
                logger.trace("  {0} {1}", ++i, contribution);
            }
        }

        // Create the node, and use the existing UUID if one is found in the cache ...
        ExecutionContext context = getExecutionContext();
        assert context != null;
        UUID uuid = null;
        if (fromCache != null) {
            Property uuidProperty = fromCache.getPropertiesByName().get(DnaLexicon.UUID);
            if (uuidProperty != null && !uuidProperty.isEmpty()) {
                uuid = context.getValueFactories().getUuidFactory().create(uuidProperty.getValues().next());
            }
        }
        if (uuid == null) uuid = UUID.randomUUID();
        FederatedNode mergedNode = new FederatedNode(path, uuid);

        // Merge the results into a single set of results ...
        assert contributions.size() > 0;
        mergingStrategy.merge(mergedNode, contributions, context);
        if (mergedNode.getCachePolicy() == null) {
            mergedNode.setCachePolicy(defaultCachePolicy);
        }
        if (updateCache) {
            // Place the results into the cache ...
            updateCache(mergedNode);
        }
        // And return the results ...
        return mergedNode;
    }

    /**
     * Load the node at the supplied path from the sources with the supplied name, returning the information. This method always
     * obtains the information from the sources and does not use or update the cache.
     * 
     * @param path the path of the node that is to be loaded
     * @param sourceNames the names of the sources from which contributions are to be loaded; may be empty or null if all
     *        contributions from all sources are to be loaded
     * @param contributions the list into which the contributions are to be placed
     * @throws RepositorySourceException
     */
    protected void loadContributionsFromSources( Path path,
                                                 Set<String> sourceNames,
                                                 List<Contribution> contributions ) throws RepositorySourceException {
        // At this point, there is no merge plan, so read information from the sources ...
        ExecutionContext context = getExecutionContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        for (Projection projection : this.sourceProjections) {
            final String source = projection.getSourceName();
            if (sourceNames != null && !sourceNames.contains(source)) continue;
            final RepositoryConnection sourceConnection = getConnection(projection);
            if (sourceConnection == null) continue; // No source exists by this name
            // Get the cached information ...
            CachePolicy cachePolicy = sourceConnection.getDefaultCachePolicy();
            if (cachePolicy == null) cachePolicy = this.defaultCachePolicy;
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
                switch (topLevelPaths.size()) {
                    case 0:
                        break;
                    case 1: {
                        Path topLevelPath = topLevelPaths.iterator().next();
                        if (path.isAncestorOf(topLevelPath)) {
                            assert topLevelPath.size() > path.size();
                            Path.Segment child = topLevelPath.getSegment(path.size());
                            contribution = Contribution.createPlaceholder(source, path, expirationTime, child);
                        }
                        break;
                    }
                    default: {
                        // We assume that the top-level paths do not overlap ...
                        List<Path.Segment> children = new ArrayList<Path.Segment>(topLevelPaths.size());
                        for (Path topLevelPath : topLevelPaths) {
                            if (path.isAncestorOf(topLevelPath)) {
                                assert topLevelPath.size() > path.size();
                                Path.Segment child = topLevelPath.getSegment(path.size());
                                children.add(child);
                            }
                        }
                        if (children.size() > 0) {
                            contribution = Contribution.createPlaceholder(source, path, expirationTime, children);
                        }
                    }
                }
                if (contribution == null) contribution = Contribution.create(source, expirationTime);
                contributions.add(contribution);
            } else {
                // There is at least one (real) contribution ...

                // Get the contributions ...
                final int numPaths = pathsInSource.size();
                if (numPaths == 1) {
                    Path pathInSource = pathsInSource.iterator().next();
                    BasicGetNodeCommand fromSource = new BasicGetNodeCommand(pathInSource);
                    sourceConnection.execute(getExecutionContext(), fromSource);
                    if (!fromSource.hasError()) {
                        Collection<Property> properties = fromSource.getProperties();
                        List<Segment> children = fromSource.getChildren();
                        DateTime expTime = fromSource.getCachePolicy() == null ? expirationTime : getCurrentTimeInUtc().plus(fromSource.getCachePolicy().getTimeToLive(),
                                                                                                                             TimeUnit.MILLISECONDS);
                        Contribution contribution = Contribution.create(source, pathInSource, expTime, properties, children);
                        contributions.add(contribution);
                    }
                } else {
                    BasicGetNodeCommand[] fromSourceCommands = new BasicGetNodeCommand[numPaths];
                    int i = 0;
                    for (Path pathInSource : pathsInSource) {
                        fromSourceCommands[i++] = new BasicGetNodeCommand(pathInSource);
                    }
                    sourceConnection.execute(context, fromSourceCommands);
                    for (BasicGetNodeCommand fromSource : fromSourceCommands) {
                        if (fromSource.hasError()) continue;
                        Collection<Property> properties = fromSource.getProperties();
                        List<Segment> children = fromSource.getChildren();
                        DateTime expTime = fromSource.getCachePolicy() == null ? expirationTime : getCurrentTimeInUtc().plus(fromSource.getCachePolicy().getTimeToLive(),
                                                                                                                             TimeUnit.MILLISECONDS);
                        Contribution contribution = Contribution.create(source,
                                                                        fromSource.getPath(),
                                                                        expTime,
                                                                        properties,
                                                                        children);
                        contributions.add(contribution);
                    }
                }
            }
        }
    }

    protected MergePlan getMergePlan( BasicGetNodeCommand command ) {
        Property mergePlanProperty = command.getPropertiesByName().get(mergePlanPropertyName);
        if (mergePlanProperty == null || mergePlanProperty.isEmpty()) {
            return null;
        }
        Object value = mergePlanProperty.getValues().next();
        return value instanceof MergePlan ? (MergePlan)value : null;
    }

    protected void updateCache( FederatedNode mergedNode ) throws RepositorySourceException {
        final ExecutionContext context = getExecutionContext();
        final RepositoryConnection cacheConnection = getConnectionToCache();
        final Path path = mergedNode.getPath();

        NodeConflictBehavior conflictBehavior = NodeConflictBehavior.UPDATE;
        Collection<Property> properties = new ArrayList<Property>(mergedNode.getPropertiesByName().size() + 1);
        properties.add(new BasicSingleValueProperty(this.uuidPropertyName, mergedNode.getUuid()));
        BasicCreateNodeCommand newNode = new BasicCreateNodeCommand(path, properties, conflictBehavior);
        List<Segment> children = mergedNode.getChildren();
        GraphCommand[] intoCache = new GraphCommand[1 + children.size()];
        int i = 0;
        intoCache[i++] = newNode;
        List<Property> noProperties = Collections.emptyList();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        for (Segment child : mergedNode.getChildren()) {
            newNode = new BasicCreateNodeCommand(pathFactory.create(path, child), noProperties, conflictBehavior);
            // newNode.setProperty(new BasicSingleValueProperty(this.uuidPropertyName, mergedNode.getUuid()));
            intoCache[i++] = newNode;
        }
        cacheConnection.execute(context, intoCache);
    }
}
