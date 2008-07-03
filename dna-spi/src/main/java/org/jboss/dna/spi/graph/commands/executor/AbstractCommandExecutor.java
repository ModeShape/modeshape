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
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;

/**
 * Abstract implementation of the {@link CommandExecutor} interface that provides implementations for all methods, making this a
 * useful base class for all {@link CommandExecutor} implementations. Because all methods are implemented, subclasses only need to
 * override methods that are appropriate or applicable, and all other commands will be processed correctly (even if new command
 * interfaces are added in later versions). In some cases, as with {@link CompositeCommand} and {@link GetNodeCommand}, these
 * implementations attempt to process the command. In other cases (e.g., {@link GetPropertiesCommand}, and
 * {@link DeleteBranchCommand}), the methods do nothing and should be overridden if the command is to be processed.
 * <p>
 * The implementation is also designed to be instantated as needed. This may be once per call to
 * {@link RepositoryConnection#execute(ExecutionEnvironment, GraphCommand...)}, or may be once per transaction. Either way, this
 * class is designed to allow subclasses to store additional state that may otherwise be expensive or undesirable to obtain
 * repeatedly. However, this state should be independent of the commands that are processed, meaning that implementations should
 * generally not change state as a result of processing specific commands.
 * </p>
 * 
 * @author Randall Hauch
 */
public abstract class AbstractCommandExecutor implements CommandExecutor {

    private final ExecutionEnvironment env;
    private final String sourceName;

    protected AbstractCommandExecutor( ExecutionEnvironment env,
                                       String sourceName ) {
        assert env != null;
        assert sourceName != null && sourceName.trim().length() != 0;
        this.env = env;
        this.sourceName = sourceName;
    }

    /**
     * Get the environment in which these commands are being executed.
     * 
     * @return the execution environment; never null
     */
    public ExecutionEnvironment getEnvironment() {
        return env;
    }

    /**
     * Get the name of the repository source.
     * 
     * @return the source name; never null or empty
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation examines the instance to see which {@link GraphCommand command interfaces} are implemented by the
     * command, and delegates to the appropriate methods.
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.GraphCommand)
     */
    public void execute( GraphCommand command ) throws RepositorySourceException, InterruptedException {
        if (command == null) return;
        if (command instanceof CompositeCommand) {
            execute((CompositeCommand)command);
            // A composite command should only contain other commands and should not do anything on its own
            return;
        }
        // The command could implement multiple "get" behaviors
        if (command instanceof GetPropertiesCommand) {
            execute((GetPropertiesCommand)command);
        }
        if (command instanceof GetChildrenCommand) {
            execute((GetChildrenCommand)command);
        }
        // The command could record the branch even if deleting or moving ...
        if (command instanceof RecordBranchCommand) {
            execute((RecordBranchCommand)command);
        }
        // If the command createa a node, it will have properties to set
        if (command instanceof CreateNodeCommand) {
            execute((CreateNodeCommand)command);
        } else if (command instanceof SetPropertiesCommand) {
            execute((SetPropertiesCommand)command);
        }
        // A copy command will either copy a branch or a node, but not both
        if (command instanceof CopyBranchCommand) {
            execute((CopyBranchCommand)command);
        } else if (command instanceof CopyNodeCommand) {
            execute((CopyNodeCommand)command);
        }
        // The command can either delete or move a branch, but a command can't do both (the move does delete)
        if (command instanceof DeleteBranchCommand) {
            execute((DeleteBranchCommand)command);
        } else if (command instanceof MoveBranchCommand) {
            execute((MoveBranchCommand)command);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CompositeCommand)
     */
    public void execute( CompositeCommand command ) throws RepositorySourceException, InterruptedException {
        assert command != null;
        for (GraphCommand nestedCommand : command) {
            execute(nestedCommand);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetPropertiesCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetChildrenCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( GetChildrenCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CreateNodeCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( CreateNodeCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.SetPropertiesCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( SetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyNodeCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( CopyNodeCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyBranchCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( CopyBranchCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.DeleteBranchCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( DeleteBranchCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.MoveBranchCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( MoveBranchCommand command ) throws RepositorySourceException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.CommandExecutor#execute(org.jboss.dna.spi.graph.commands.RecordBranchCommand)
     */
    @SuppressWarnings( "unused" )
    public void execute( RecordBranchCommand command ) throws RepositorySourceException, InterruptedException {
    }

}
