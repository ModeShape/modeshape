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
package org.jboss.dna.graph.commands.executor;

import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.commands.CopyBranchCommand;
import org.jboss.dna.graph.commands.CopyNodeCommand;
import org.jboss.dna.graph.commands.CreateNodeCommand;
import org.jboss.dna.graph.commands.DeleteBranchCommand;
import org.jboss.dna.graph.commands.GetChildrenCommand;
import org.jboss.dna.graph.commands.GetPropertiesCommand;
import org.jboss.dna.graph.commands.MoveBranchCommand;
import org.jboss.dna.graph.commands.RecordBranchCommand;
import org.jboss.dna.graph.commands.SetPropertiesCommand;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryConnectionFactory;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.DateTime;

/**
 * @author Randall Hauch
 */
public class SingleSourceCommandExecutor extends AbstractCommandExecutor {

    private RepositoryConnection connection;
    private final RepositoryConnectionFactory connectionFactory;

    /**
     * Create a command executor that does nothing.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param connectionFactory the factory for {@link RepositoryConnection} instances
     */
    public SingleSourceCommandExecutor( ExecutionContext context,

                                        String sourceName,
                                        RepositoryConnectionFactory connectionFactory ) {
        this(context, sourceName, null, connectionFactory);
    }

    /**
     * Create a command executor that does nothing.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param now the current time; may be null if the system time is to be used
     * @param connectionFactory the factory for {@link RepositoryConnection} instances
     */
    public SingleSourceCommandExecutor( ExecutionContext context,
                                        String sourceName,
                                        DateTime now,
                                        RepositoryConnectionFactory connectionFactory ) {
        super(context, sourceName, now);
        assert connectionFactory != null;
        this.connectionFactory = connectionFactory;
    }

    protected RepositoryConnection getConnection() throws RepositorySourceException {
        if (connection == null) {
            // Create a connection ...
            connection = this.connectionFactory.createConnection(getSourceName());
        }
        return connection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#close()
     */
    @Override
    public void close() {
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
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.CopyBranchCommand)
     */
    @Override
    public void execute( CopyBranchCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.CopyNodeCommand)
     */
    @Override
    public void execute( CopyNodeCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.CreateNodeCommand)
     */
    @Override
    public void execute( CreateNodeCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.DeleteBranchCommand)
     */
    @Override
    public void execute( DeleteBranchCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.GetChildrenCommand)
     */
    @Override
    public void execute( GetChildrenCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.GetPropertiesCommand)
     */
    @Override
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.MoveBranchCommand)
     */
    @Override
    public void execute( MoveBranchCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.RecordBranchCommand)
     */
    @Override
    public void execute( RecordBranchCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.SetPropertiesCommand)
     */
    @Override
    public void execute( SetPropertiesCommand command ) throws RepositorySourceException {
        getConnection().execute(this.getExecutionContext(), command);
    }
}
