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
package org.jboss.dna.spi.graph.commands.executor;

import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.connector.RepositorySource;
import org.jboss.dna.spi.connector.RepositorySourceException;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.commands.CompositeCommand;
import org.jboss.dna.spi.graph.commands.CopyBranchCommand;
import org.jboss.dna.spi.graph.commands.CopyNodeCommand;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.DeleteBranchCommand;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetNodeCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.MoveBranchCommand;
import org.jboss.dna.spi.graph.commands.RecordBranchCommand;
import org.jboss.dna.spi.graph.commands.SetPropertiesCommand;

/**
 * @author Randall Hauch
 */
public class NoOpCommandExecutor extends AbstractCommandExecutor {

    /**
     * Create a command executor that does nothing.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     */
    public NoOpCommandExecutor( ExecutionContext context,
                                String sourceName ) {
        super(context, sourceName);
    }

    /**
     * Create a command executor that does nothing.
     * 
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param now the current time; may be null if the system time is to be used
     */
    public NoOpCommandExecutor( ExecutionContext context,
                                String sourceName,
                                DateTime now ) {
        super(context, sourceName, now);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GraphCommand)
     */
    @Override
    public void execute( GraphCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CompositeCommand)
     */
    @Override
    public void execute( CompositeCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetNodeCommand)
     */
    @Override
    public void execute( GetNodeCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyBranchCommand)
     */
    @Override
    public void execute( CopyBranchCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyNodeCommand)
     */
    @Override
    public void execute( CopyNodeCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CreateNodeCommand)
     */
    @Override
    public void execute( CreateNodeCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.DeleteBranchCommand)
     */
    @Override
    public void execute( DeleteBranchCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetChildrenCommand)
     */
    @Override
    public void execute( GetChildrenCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetPropertiesCommand)
     */
    @Override
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.MoveBranchCommand)
     */
    @Override
    public void execute( MoveBranchCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.RecordBranchCommand)
     */
    @Override
    public void execute( RecordBranchCommand command ) throws RepositorySourceException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.spi.graph.commands.SetPropertiesCommand)
     */
    @Override
    public void execute( SetPropertiesCommand command ) throws RepositorySourceException {
        // do nothing
    }
}
