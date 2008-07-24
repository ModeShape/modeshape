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

import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.commands.CopyBranchCommand;
import org.jboss.dna.spi.graph.commands.CopyNodeCommand;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.DeleteBranchCommand;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetNodeCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.MoveBranchCommand;
import org.jboss.dna.spi.graph.commands.RecordBranchCommand;
import org.jboss.dna.spi.graph.commands.SetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor;
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
public class SingleProjectionCommandExecutor extends AbstractCommandExecutor {

    private final Projection projection;
    private final PathFactory pathFactory;
    private final RepositoryConnectionFactories factories;
    private RepositoryConnection connection;

    /**
     * @param env the execution environment in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param projection the projection used for the cached information; may not be null and must have exactly one
     *        {@link Projection#getRules() rule}
     * @param connectionFactories the factory for connection factory instances
     */
    public SingleProjectionCommandExecutor( ExecutionEnvironment env,
                                            String sourceName,
                                            Projection projection,
                                            RepositoryConnectionFactories connectionFactories ) {
        this(env, sourceName, null, projection, connectionFactories);
    }

    /**
     * @param env the execution environment in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param now the current time; may be null if the system time is to be used
     * @param projection the projection used for the cached information; may not be null and must have exactly one
     *        {@link Projection#getRules() rule}
     * @param connectionFactories the factory for connection factory instances
     */
    public SingleProjectionCommandExecutor( ExecutionEnvironment env,
                                            String sourceName,
                                            DateTime now,
                                            Projection projection,
                                            RepositoryConnectionFactories connectionFactories ) {
        super(env, sourceName, now);
        assert connectionFactories != null;
        assert projection != null;
        assert projection.getRules().size() == 1;
        this.projection = projection;
        this.factories = connectionFactories;
        this.pathFactory = env.getValueFactories().getPathFactory();
        assert this.pathFactory != null;
    }

    protected RepositoryConnection getConnection() throws RepositorySourceException, InterruptedException {
        if (connection == null) {
            // Create a connection ...
            RepositoryConnectionFactory connectionFactory = this.factories.getConnectionFactory(this.projection.getSourceName());
            connection = connectionFactory.getConnection();
        }
        return connection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#close()
     */
    @Override
    public void close() throws InterruptedException {
        if (this.connection != null) {
            try {
                this.connection.close();
            } finally {
                this.connection = null;
            }
        }
        super.close();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetChildrenCommand)
     */
    @Override
    public void execute( GetChildrenCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        getConnection().execute(this.getEnvironment(), new ProjectedGetChildrenCommand(command, pathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetPropertiesCommand)
     */
    @Override
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        getConnection().execute(this.getEnvironment(), new ProjectedGetPropertiesCommand(command, pathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetNodeCommand)
     */
    @Override
    public void execute( GetNodeCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        getConnection().execute(this.getEnvironment(), new ProjectedGetNodeCommand(command, pathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CreateNodeCommand)
     */
    @Override
    public void execute( CreateNodeCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        getConnection().execute(this.getEnvironment(), new ProjectedCreateNodeCommand(command, pathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.SetPropertiesCommand)
     */
    @Override
    public void execute( SetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        getConnection().execute(this.getEnvironment(), new ProjectedSetPropertiesCommand(command, pathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.DeleteBranchCommand)
     */
    @Override
    public void execute( DeleteBranchCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        getConnection().execute(this.getEnvironment(), new ProjectedDeleteBranchCommand(command, pathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.MoveBranchCommand)
     */
    @Override
    public void execute( MoveBranchCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        Path newPathInSource = getPathInSource(command.getNewPath());
        getConnection().execute(this.getEnvironment(), new ProjectedMoveBranchCommand(command, pathInSource, newPathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.RecordBranchCommand)
     */
    @Override
    public void execute( RecordBranchCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        getConnection().execute(this.getEnvironment(), new ProjectedRecordBranchCommand(command, pathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyBranchCommand)
     */
    @Override
    public void execute( CopyBranchCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        Path newPathInSource = getPathInSource(command.getNewPath());
        getConnection().execute(this.getEnvironment(), new ProjectedCopyBranchCommand(command, pathInSource, newPathInSource));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyNodeCommand)
     */
    @Override
    public void execute( CopyNodeCommand command ) throws RepositorySourceException, InterruptedException {
        Path pathInSource = getPathInSource(command.getPath());
        Path newPathInSource = getPathInSource(command.getNewPath());
        getConnection().execute(this.getEnvironment(), new ProjectedCopyNodeCommand(command, pathInSource, newPathInSource));
    }

    protected Path getPathInSource( Path pathInRepository ) {
        Set<Path> paths = this.projection.getPathsInSource(pathInRepository, pathFactory);
        if (!paths.isEmpty()) {
            return paths.iterator().next();
        }
        return this.projection.getPathsInSource(pathInRepository, pathFactory).iterator().next();
    }

    protected Path getPathInRepository( Path pathInSource ) {
        return this.projection.getPathsInRepository(pathInSource, pathFactory).iterator().next();
    }

}
