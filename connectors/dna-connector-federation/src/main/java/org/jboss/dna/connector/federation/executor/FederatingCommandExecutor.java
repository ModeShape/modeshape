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

import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.federation.FederationI18n;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.BasicMergePlan;
import org.jboss.dna.spi.graph.Binary;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetNodeCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetPropertiesCommand;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
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
     * desired, see
     * {@link #FederatingCommandExecutor(ExecutionEnvironment, String, Projection, List, RepositoryConnectionFactories)
     * constructor} that takes a {@link Projection cache projection}.
     * 
     * @param env the execution environment in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param sourceProjections the source projections; may not be null
     * @param connectionFactories the factory for connection factory instances
     */
    public FederatingCommandExecutor( ExecutionEnvironment env,
                                      String sourceName,
                                      List<Projection> sourceProjections,
                                      RepositoryConnectionFactories connectionFactories ) {
        this(env, sourceName, null, sourceProjections, connectionFactories);
    }

    /**
     * Create a command executor that federates (merges) the information from multiple sources described by the source
     * projections. The resulting command executor will use the supplied {@link Projection cache projection} to identify the
     * {@link Projection#getSourceName() repository source} for the cache as well as the {@link Projection#getRules() rules} for
     * how the paths are mapped in the cache. This cache will be consulted first for the requested information, and will be kept
     * up to date as changes are made to the federated information.
     * 
     * @param env the execution environment in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param cacheProjection the projection used for the cached information; may be null if there is no cache
     * @param sourceProjections the source projections; may not be null
     * @param connectionFactories the factory for connection factory instances
     */
    public FederatingCommandExecutor( ExecutionEnvironment env,
                                      String sourceName,
                                      Projection cacheProjection,
                                      List<Projection> sourceProjections,
                                      RepositoryConnectionFactories connectionFactories ) {
        super(env, sourceName);
        assert sourceProjections != null;
        assert connectionFactories != null;
        this.cacheProjection = cacheProjection;
        this.sourceProjections = sourceProjections;
        this.connectionFactories = connectionFactories;
        this.connectionsBySourceName = new HashMap<String, RepositoryConnection>();
        this.mergePlanPropertyName = env.getValueFactories().getNameFactory().create("dna:mergePlan");
        this.sourceNames = new HashSet<String>();
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
        if (fromCache.hasError()) return fromCache;

        // Look up the merge plan ...
        BasicMergePlan basicMergePlan = getMergePlan(fromCache);
        if (basicMergePlan != null) {
            if (isCurrent(path, basicMergePlan)) return fromCache;
            // Some of the merge plan is out of date, so we need to read the information from those regions that are expired ...
            BasicMergePlan newMergePlan = new BasicMergePlan();
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
            // for (FederatedRegion region : regions) {
            // if (!region.appliesTo(path)) continue;
            // // Issue a command to get the node from the
            // // BasicGetNode
            // }
        }
        // And read the information from any new region ...
        return null;
    }

    protected Contribution getContribution( String sourceName,
                                            Path path ) {
        return null;
    }

    protected BasicMergePlan getMergePlan( BasicGetPropertiesCommand command ) {
        Property mergePlanProperty = command.getProperties().get(mergePlanPropertyName);
        if (mergePlanProperty == null || mergePlanProperty.isEmpty()) {
            return null;
        }
        ValueFactory<Binary> binaryFactory = getEnvironment().getValueFactories().getBinaryFactory();
        Binary binaryValue = binaryFactory.create(mergePlanProperty.getValues().next());
        binaryValue.acquire();
        ObjectInputStream stream = null;
        BasicMergePlan basicMergePlan = null;
        RepositorySourceException error = null;
        try {
            stream = new ObjectInputStream(binaryValue.getStream());
            basicMergePlan = (BasicMergePlan)stream.readObject();
        } catch (Throwable err) {
            I18n msg = FederationI18n.errorReadingMergePlan;
            error = new RepositorySourceException(getSourceName(), msg.text(command.getPath()), err);
            throw error;
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (Throwable err) {
                if (error == null) {
                    I18n msg = FederationI18n.errorReadingMergePlan;
                    error = new RepositorySourceException(getSourceName(), msg.text(command.getPath()), err);
                    throw error;
                }
            } finally {
                binaryValue.release();
            }
        }
        return basicMergePlan;
    }

    /**
     * Determine if the supplied plan is considered current
     * 
     * @param path the path of the node at which (or below which) the merge plan applies
     * @param plan the merge plan
     * @return true if the merge plan is current, or false if it needs to be (at least partially) rebuilt
     */
    protected boolean isCurrent( Path path,
                                 BasicMergePlan plan ) {
        // First check the time ...
        DateTime now = getCurrentTimeInUtc();
        if (plan.isExpired(now)) return false;

        // Does the plan have any contributions from sources that don't exist ?
        for (String contributingSource : plan.getNamesOfContributingSources()) {
            if (!sourceNames.contains(contributingSource)) return false;
        }

        // Determine if any new source projections exists that aren't part of the plan ...
        for (String sourceName : sourceNames) {
            if (plan.isSource(sourceName)) continue;
            // The source is new ... see whether there are any regions that apply ...
            // for (FederatedRegion region : this.regionsBySourceName.get(sourceName)) {
            // // If the region's path is not at/above the path, the region doesn't matter
            // if (!region.appliesTo(path)) continue;
            // // The region applies to the path ...
            // return false;
            // }
        }
        return true;
    }

}
