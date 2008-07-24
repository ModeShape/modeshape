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
import org.jboss.dna.spi.graph.connection.RepositorySourceException;

/**
 * @author Randall Hauch
 */
public class DelegatingCommandExecutor implements CommandExecutor {

    private final CommandExecutor delegate;

    public DelegatingCommandExecutor( CommandExecutor delegate ) {
        assert delegate != null;
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#close()
     */
    public void close() throws InterruptedException {
        delegate.close();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.GraphCommand)
     */
    public void execute( GraphCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CompositeCommand)
     */
    public void execute( CompositeCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetNodeCommand)
     */
    public void execute( GetNodeCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetPropertiesCommand)
     */
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetChildrenCommand)
     */
    public void execute( GetChildrenCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CreateNodeCommand)
     */
    public void execute( CreateNodeCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.SetPropertiesCommand)
     */
    public void execute( SetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyNodeCommand)
     */
    public void execute( CopyNodeCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyBranchCommand)
     */
    public void execute( CopyBranchCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.RecordBranchCommand)
     */
    public void execute( RecordBranchCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.DeleteBranchCommand)
     */
    public void execute( DeleteBranchCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.MoveBranchCommand)
     */
    public void execute( MoveBranchCommand command ) throws RepositorySourceException, InterruptedException {
        delegate.execute(command);
    }

}
