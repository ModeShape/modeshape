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
public interface CommandExecutor {

    /**
     * Execute a graph command. This method should examine the command's types to determine which other <code>execute</code>
     * methods should be called, and should then call those methods. This method should also do nothing if the command is null.
     * 
     * @param command the command to be executed
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( GraphCommand command ) throws RepositorySourceException;

    /**
     * Execute a composite command that contains other commands. This method should simply obtain and execute each of the nested
     * commands.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( CompositeCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to get the properties and children of a node. {@link GetNodeCommand} is a subtype of both
     * {@link GetPropertiesCommand} and {@link GetChildrenCommand}, so this method will be called in place of the
     * {@link #execute(GetPropertiesCommand)} and {@link #execute(GetChildrenCommand)} methods.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( GetNodeCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to get the properties of a node.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( GetPropertiesCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to get the children of a node.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( GetChildrenCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to create a node and set the node's properties.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( CreateNodeCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to set some (or all) of the properties on a node.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( SetPropertiesCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to copy a node to a new location.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( CopyNodeCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to copy an entire branch to a new location.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( CopyBranchCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to record the structure of a branch.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( RecordBranchCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to delete an entire branch.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( DeleteBranchCommand command ) throws RepositorySourceException;

    /**
     * Execute a command to move a branch from one location to another.
     * 
     * @param command the command to be executed; may not be null
     * @throws RepositorySourceException if there is an error executing the command
     */
    void execute( MoveBranchCommand command ) throws RepositorySourceException;

    /**
     * Close this executor, allowing it to clean up any open resources.
     */
    void close();
}
