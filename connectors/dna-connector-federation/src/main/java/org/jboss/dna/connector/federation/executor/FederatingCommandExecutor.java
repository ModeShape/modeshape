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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.MergePlan;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PathNotFoundException;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetNodeCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.NodeConflictBehavior;
import org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor;
import org.jboss.dna.spi.graph.commands.impl.BasicCreateNodeCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetPropertiesCommand;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactories;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class FederatingCommandExecutor extends AbstractCommandExecutor {

    private final Name mergePlanPropertyName;
    private final Projection cacheProjection;
    private final List<Projection> sourceProjections;
    private final Set<String> sourceNames;
    private final RepositoryConnectionFactories connectionFactories;
    /** The set of all connections, including the cache connection */
    private final Map<String, RepositoryConnection> connectionsBySourceName;
    /** A direct reference to the cache connection */
    private RepositoryConnection cacheConnection;

    /**
     * Create a command executor that federates (merges) the information from multiple sources described by the source
     * projections. The resulting command executor does not first consult a cache for the merged information; if a cache is
     * desired, see {@link #FederatingCommandExecutor(ExecutionContext, String, Projection, List, RepositoryConnectionFactories)
     * constructor} that takes a {@link Projection cache projection}.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param sourceProjections the source projections; may not be null
     * @param connectionFactories the factory for connection factory instances
     */
    public FederatingCommandExecutor( ExecutionContext context,
                                      String sourceName,
                                      List<Projection> sourceProjections,
                                      RepositoryConnectionFactories connectionFactories ) {
        this(context, sourceName, null, sourceProjections, connectionFactories);
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
     * @param sourceProjections the source projections; may not be null
     * @param connectionFactories the factory for connection factory instances
     */
    public FederatingCommandExecutor( ExecutionContext context,
                                      String sourceName,
                                      Projection cacheProjection,
                                      List<Projection> sourceProjections,
                                      RepositoryConnectionFactories connectionFactories ) {
        super(context, sourceName);
        assert sourceProjections != null;
        assert connectionFactories != null;
        this.cacheProjection = cacheProjection;
        this.sourceProjections = sourceProjections;
        this.connectionFactories = connectionFactories;
        this.connectionsBySourceName = new HashMap<String, RepositoryConnection>();
        this.mergePlanPropertyName = context.getValueFactories().getNameFactory().create("dna:mergePlan");
        this.sourceNames = new HashSet<String>();
        for (Projection projection : this.sourceProjections) {
            this.sourceNames.add(projection.getSourceName());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#close()
     */
    @Override
    public void close() throws InterruptedException {
        try {
            super.close();
        } finally {
            // Make sure to close ALL open connections ...
            for (RepositoryConnection connection : connectionsBySourceName.values()) {
                if (connection == null) continue;
                try {
                    connection.close();
                } catch (Throwable t) {
                    Logger.getLogger(getClass()).debug("Error while closing connection to {0}", connection.getSourceName());
                }
            }
            connectionsBySourceName.clear();
        }
    }

    protected RepositoryConnection getConnectionToCache() throws RepositorySourceException, InterruptedException {
        if (this.cacheConnection == null) {
            this.cacheConnection = getConnection(this.cacheProjection);
        }
        assert this.cacheConnection != null;
        return this.cacheConnection;
    }

    protected RepositoryConnection getConnection( Projection projection ) throws RepositorySourceException, InterruptedException {
        String sourceName = projection.getSourceName();
        RepositoryConnection connection = connectionsBySourceName.get(sourceName);
        if (connection == null) {
            RepositoryConnectionFactory connectionFactory = connectionFactories.getConnectionFactory(sourceName);
            if (connectionFactory != null) {
                connection = connectionFactory.getConnection();
            }
            connectionsBySourceName.put(sourceName, connection);
        }
        return connection;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This class overrides the {@link AbstractCommandExecutor#execute(GetNodeCommand) default behavior} and instead processes the
     * command in a more efficient manner.
     * </p>
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetNodeCommand)
     */
    @Override
    public void execute( GetNodeCommand command ) throws RepositorySourceException, InterruptedException {
        BasicGetNodeCommand nodeInfo = getNode(command.getPath());
        for (Property property : nodeInfo.getProperties().values()) {
            command.setProperty(property);
        }
        command.setChildren(nodeInfo.getChildren());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetPropertiesCommand)
     */
    @Override
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
        BasicGetNodeCommand nodeInfo = getNode(command.getPath());
        for (Property property : nodeInfo.getProperties().values()) {
            command.setProperty(property);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetChildrenCommand)
     */
    @Override
    public void execute( GetChildrenCommand command ) throws RepositorySourceException, InterruptedException {
        BasicGetNodeCommand nodeInfo = getNode(command.getPath());
        command.setChildren(nodeInfo.getChildren());
    }

    /**
     * Get the node information from the underlying sources or, if possible, from the cache.
     * 
     * @param path the path of the node to be returned
     * @return the node information
     * @throws RepositorySourceException
     * @throws InterruptedException
     */
    protected BasicGetNodeCommand getNode( Path path ) throws RepositorySourceException, InterruptedException {
        // Check the cache first ...
        RepositoryConnection cacheConnection = getConnectionToCache();
        BasicGetNodeCommand fromCache = new BasicGetNodeCommand(path);
        cacheConnection.execute(getEnvironment(), fromCache);
        if (fromCache.hasError()) {
            if (fromCache.getError() instanceof PathNotFoundException) {
                // Start at the root and populate the cache down to this node ...
            }
        }

        if (fromCache.hasError()) return fromCache;

        // Look up the merge plan ...
        MergePlan mergePlan = getMergePlan(fromCache);
        if (mergePlan != null) {
            if (isCurrent(path, mergePlan)) return fromCache;
            // Some of the merge plan is out of date, so we need to read the information from those regions that are expired ...
            // MergePlan newMergePlan = new BasicMergePlan();
            // for (Projection projection : sourceProjections) {
            // // Does this region apply to the path ...
            // if (projection.appliesTo(path)) {
            // // Get any existing contribution ...
            // Contribution contribution = mergePlan.getContributionFrom(region.getSourceName());
            // if (contribution == null || contribution.isExpired(getCurrentTimeInUtc())) {
            // contribution = getContribution(contribution.getSourceName(), path);
            // }
            // newMergePlan.addContribution(contribution);
            // }
            // }
            // for ( Contribution contribution : mergePlan.getContributions() ) {
            // // Is the contribution still represented by a region?
            //                
            // if ( contribution.isExpired(getCurrentTimeInUtc())) {
            // contribution = getContribution(contribution.getSourceName(),path);
            // }
            // newMergePlan.addContribution(contribution);
            // }

            // What about other (new) regions?
        } else {
            // At this point, there is no merge plan, so read information from the sources ...
            PathFactory pathFactory = getEnvironment().getValueFactories().getPathFactory();
            List<Contribution> contributions = new LinkedList<Contribution>();
            for (Projection projection : this.sourceProjections) {
                final String source = projection.getSourceName();
                // Get the paths-in-source where we should fetch node contributions ...
                Set<Path> pathsInSource = projection.getPathsInSource(path, pathFactory);
                if (pathsInSource.isEmpty()) {
                    // The source has no contributions ...
                    contributions.add(Contribution.create(source));
                } else {
                    // There is at least one contribution ...
                    RepositoryConnection sourceConnection = getConnection(projection);

                    // Get the contributions ...
                    final int numPaths = pathsInSource.size();
                    if (numPaths == 1) {
                        Path pathInSource = pathsInSource.iterator().next();
                        BasicGetNodeCommand fromSource = new BasicGetNodeCommand(pathInSource);
                        sourceConnection.execute(getEnvironment(), fromSource);
                        if (!fromSource.hasError()) {
                            Collection<Property> properties = fromSource.getProperties().values();
                            Collection<Segment> children = fromSource.getChildren();
                            Contribution contribution = Contribution.create(source, pathInSource, properties, children);
                            contributions.add(contribution);
                        }
                    } else {
                        BasicGetNodeCommand[] fromSourceCommands = new BasicGetNodeCommand[numPaths];
                        int i = 0;
                        for (Path pathInSource : pathsInSource) {
                            fromSourceCommands[i++] = new BasicGetNodeCommand(pathInSource);
                        }
                        sourceConnection.execute(getEnvironment(), fromSourceCommands);
                        for (BasicGetNodeCommand fromSource : fromSourceCommands) {
                            if (fromSource.hasError()) continue;
                            Collection<Property> properties = fromSource.getProperties().values();
                            Collection<Segment> children = fromSource.getChildren();
                            Contribution contribution = Contribution.create(source, fromSource.getPath(), properties, children);
                            contributions.add(contribution);
                        }
                    }
                }
            }
            // Merge the results into a single set of results ...
            mergePlan = MergePlan.create(contributions);
            BasicGetNodeCommand mergedNode = new BasicGetNodeCommand(null);

            // Place the results into the cache ...
            NodeConflictBehavior conflictBehavior = NodeConflictBehavior.UPDATE;
            BasicCreateNodeCommand newNode = new BasicCreateNodeCommand(path, mergedNode.getProperties().values(),
                                                                        conflictBehavior);
            List<Segment> children = mergedNode.getChildren();
            GraphCommand[] intoCache = new GraphCommand[1 + children.size()];
            int i = 0;
            intoCache[i++] = newNode;
            List<Property> noProperties = Collections.emptyList();
            for (Segment child : mergedNode.getChildren()) {
                intoCache[i++] = new BasicCreateNodeCommand(pathFactory.create(path, child), noProperties, conflictBehavior);
            }
            cacheConnection.execute(getEnvironment(), mergedNode);

            // Return the results ...
            return mergedNode;
        }

        return null;
    }

    protected Contribution getContribution( RepositoryConnection connection,
                                            String sourceName,
                                            Path path ) throws RepositorySourceException, InterruptedException {
        BasicGetNodeCommand fromSource = new BasicGetNodeCommand(path);
        connection.execute(getEnvironment(), fromSource);
        if (fromSource.hasError()) return null;

        Collection<Property> properties = fromSource.getProperties().values();
        Collection<Segment> children = fromSource.getChildren();
        Contribution contribution = Contribution.create(sourceName, path, properties, children);
        return contribution;
    }

    protected MergePlan getMergePlan( BasicGetPropertiesCommand command ) {
        Property mergePlanProperty = command.getProperties().get(mergePlanPropertyName);
        if (mergePlanProperty == null || mergePlanProperty.isEmpty()) {
            return null;
        }
        Object value = mergePlanProperty.getValues().next();
        return value instanceof MergePlan ? (MergePlan)value : null;
    }

    /**
     * Determine if the supplied plan is considered current
     * 
     * @param path the path of the node at which (or below which) the merge plan applies
     * @param plan the merge plan
     * @return true if the merge plan is current, or false if it needs to be (at least partially) rebuilt
     */
    protected boolean isCurrent( Path path,
                                 MergePlan plan ) {
        // First check the time ...
        DateTime now = getCurrentTimeInUtc();
        if (plan.isExpired(now)) return false;

        // Does the plan have any contributions from sources that don't exist ?
        for (Contribution contribution : plan) {
            if (!sourceNames.contains(contribution.getSourceName())) return false;
        }
        //
        // // Determine if any new source projections exists that aren't part of the plan ...
        // for (String sourceName : sourceNames) {
        // if (plan.isSource(sourceName)) continue;
        // // The source is new ... see whether there are any regions that apply ...
        // // for (FederatedRegion region : this.regionsBySourceName.get(sourceName)) {
        // // // If the region's path is not at/above the path, the region doesn't matter
        // // if (!region.appliesTo(path)) continue;
        // // // The region applies to the path ...
        // // return false;
        // // }
        // }
        return true;
    }

}
