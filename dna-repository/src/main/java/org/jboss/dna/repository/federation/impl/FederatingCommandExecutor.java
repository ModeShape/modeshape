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
package org.jboss.dna.repository.federation.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.federation.FederatedRegion;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.PathNotFoundException;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor;
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
    private final FederatedRegion cacheRegion;
    private final List<FederatedRegion> regions;
    private final RepositoryConnectionFactories connectionFactories;
    /** The set of all connections, including the cache connection */
    private final Map<String, RepositoryConnection> connectionsBySourceName;
    /** A direct reference to the cache connection */
    private RepositoryConnection cacheConnection;

    /**
     * @param env the execution environment in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param cacheRegion the region used for the cached information; may not be null
     * @param regions the federated regions; may not be null
     * @param connectionFactories the factory for connection factory instances
     */
    public FederatingCommandExecutor( ExecutionEnvironment env,
                                      String sourceName,
                                      FederatedRegion cacheRegion,
                                      List<FederatedRegion> regions,
                                      RepositoryConnectionFactories connectionFactories ) {
        super(env, sourceName);
        assert regions != null;
        assert connectionFactories != null;
        assert cacheRegion != null;
        this.cacheRegion = cacheRegion;
        this.regions = regions;
        this.connectionFactories = connectionFactories;
        this.connectionsBySourceName = new HashMap<String, RepositoryConnection>();
        this.mergePlanPropertyName = env.getValueFactories().getNameFactory().create("dna:mergePlan");
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
            this.cacheConnection = getConnection(this.cacheRegion);
        }
        assert this.cacheConnection != null;
        return this.cacheConnection;
    }

    protected RepositoryConnection getConnection( FederatedRegion region ) throws RepositorySourceException, InterruptedException {
        String sourceName = region.getSourceName();
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
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetPropertiesCommand)
     */
    @Override
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {

        // Check the cache first ...
        RepositoryConnection cacheConnection = getConnectionToCache();
        BasicGetPropertiesCommand fromCache = new BasicGetPropertiesCommand(command.getPath());
        cacheConnection.execute(getEnvironment(), fromCache);
        if (fromCache.getError() instanceof PathNotFoundException) {
        } else {
            // Get the existing merge plan from the cached properties ...
            // MergePlan mergePlan = getMergePlan(fromCache);
            // DateTime
            // if ( mergePlan.getExpirationTimeInUtc() )
        }
    }

    // protected MergePlan getMergePlan( BasicGetPropertiesCommand command ) {
    // Property mergePlanProperty = command.getProperties().get(mergePlanPropertyName);
    // if (mergePlanProperty == null || mergePlanProperty.isEmpty()) {
    // return null;
    // }
    // ValueFactory<Binary> binaryFactory = getEnvironment().getValueFactories().getBinaryFactory();
    // Binary binaryValue = binaryFactory.create(mergePlanProperty.getValues().next());
    // binaryValue.acquire();
    // ObjectInputStream stream = null;
    // MergePlan mergePlan = null;
    // RepositorySourceException error = null;
    // try {
    // stream = new ObjectInputStream(binaryValue.getStream());
    // mergePlan = (MergePlan)stream.readObject();
    // } catch (Throwable err) {
    // I18n msg = RepositoryI18n.errorReadingMergePlan;
    // error = new RepositorySourceException(getSourceName(), msg.text(command.getPath()), err);
    // throw error;
    // } finally {
    // try {
    // if (stream != null) stream.close();
    // } catch (Throwable err) {
    // if (error == null) {
    // I18n msg = RepositoryI18n.errorReadingMergePlan;
    // error = new RepositorySourceException(getSourceName(), msg.text(command.getPath()), err);
    // throw error;
    // }
    // } finally {
    // binaryValue.release();
    // }
    // }
    // return mergePlan;
    // }
    //
}
