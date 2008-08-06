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

import org.jboss.dna.common.util.Logger;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.connector.RepositorySourceException;
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
public class LoggingCommandExecutor extends DelegatingCommandExecutor {

    private final Logger logger;
    private final Logger.Level level;

    /**
     * Create a command executor that logs before and after each method call, logging messages at the {@link Logger.Level#TRACE
     * trace} level.
     * 
     * @param delegate the delegate executor
     * @param logger the logger
     */
    public LoggingCommandExecutor( CommandExecutor delegate,
                                   Logger logger ) {
        this(delegate, logger, Logger.Level.TRACE);
    }

    /**
     * Create a command executor that logs before and after each method call, logging messages at the supplied
     * {@link Logger.Level level}.
     * 
     * @param delegate the delegate executor
     * @param logger the logger
     * @param level the logging level, or null if {@link Logger.Level#TRACE trace-level} logging should be used.
     */
    public LoggingCommandExecutor( CommandExecutor delegate,
                                   Logger logger,
                                   Logger.Level level ) {
        super(delegate);
        assert logger != null;
        this.logger = logger;
        this.level = level != null ? level : Logger.Level.TRACE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#close()
     */
    @Override
    public void close() throws InterruptedException {
        this.logger.log(level, SpiI18n.closingCommandExecutor);
        super.close();
        this.logger.log(level, SpiI18n.closedCommandExecutor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CompositeCommand)
     */
    @Override
    public void execute( CompositeCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyBranchCommand)
     */
    @Override
    public void execute( CopyBranchCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CopyNodeCommand)
     */
    @Override
    public void execute( CopyNodeCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.CreateNodeCommand)
     */
    @Override
    public void execute( CreateNodeCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.DeleteBranchCommand)
     */
    @Override
    public void execute( DeleteBranchCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetChildrenCommand)
     */
    @Override
    public void execute( GetChildrenCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetNodeCommand)
     */
    @Override
    public void execute( GetNodeCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GetPropertiesCommand)
     */
    @Override
    public void execute( GetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.GraphCommand)
     */
    @Override
    public void execute( GraphCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.MoveBranchCommand)
     */
    @Override
    public void execute( MoveBranchCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.RecordBranchCommand)
     */
    @Override
    public void execute( RecordBranchCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.executor.DelegatingCommandExecutor#execute(org.jboss.dna.spi.graph.commands.SetPropertiesCommand)
     */
    @Override
    public void execute( SetPropertiesCommand command ) throws RepositorySourceException, InterruptedException {
        this.logger.log(level, SpiI18n.executingGraphCommand, command);
        super.execute(command);
        this.logger.log(level, SpiI18n.executedGraphCommand, command);
    }

}
