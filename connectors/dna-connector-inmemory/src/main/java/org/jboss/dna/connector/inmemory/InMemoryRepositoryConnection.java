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
package org.jboss.dna.connector.inmemory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.transaction.xa.XAResource;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.InvalidPathException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.ActsAsUpdate;
import org.jboss.dna.spi.graph.commands.ActsOnPath;
import org.jboss.dna.spi.graph.commands.CompositeCommand;
import org.jboss.dna.spi.graph.commands.CopyBranchCommand;
import org.jboss.dna.spi.graph.commands.CopyNodeCommand;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.DeleteBranchCommand;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.MoveBranchCommand;
import org.jboss.dna.spi.graph.commands.SetPropertiesCommand;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;

/**
 * @author Randall Hauch
 */
public class InMemoryRepositoryConnection implements RepositoryConnection {

    protected static final RepositorySourceListener NO_OP_LISTENER = new RepositorySourceListener() {

        /**
         * {@inheritDoc}
         */
        public void notify( String sourceName,
                            Object... events ) {
            // do nothing
        }
    };

    private final InMemoryRepositorySource source;
    private final InMemoryRepository content;
    private RepositorySourceListener listener = NO_OP_LISTENER;

    InMemoryRepositoryConnection( InMemoryRepositorySource source,
                                  InMemoryRepository content ) {
        assert source != null;
        assert content != null;
        this.source = source;
        this.content = content;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return source.getName();
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return source.getDefaultCachePolicy();
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        this.content.getRoot();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setListener( RepositorySourceListener listener ) {
        this.listener = listener != null ? listener : NO_OP_LISTENER;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void execute( ExecutionEnvironment env,
                         GraphCommand... commands ) {
        // Do any commands update/write?
        Lock lock = this.content.getLock().readLock();
        for (GraphCommand command : commands) {
            if (command instanceof ActsAsUpdate) {
                lock = this.content.getLock().writeLock();
                break;
            }
        }

        try {
            // Obtain the lock ...
            lock.lock();
            // Now execute the commands ...
            for (GraphCommand command : commands) {
                executeCommand(env, command);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param env
     * @param command
     */
    protected void executeCommand( ExecutionEnvironment env,
                                   GraphCommand command ) {
        // This node reference is available for any command that extends ActsOnPath ...
        Node node = null;

        if (command instanceof CompositeCommand) {
            CompositeCommand theCommand = (CompositeCommand)command;
            for (GraphCommand containedCommand : theCommand) {
                executeCommand(env, containedCommand);
            }
        }

        // First, process the commands that create a new node ...
        if (command instanceof CreateNodeCommand) {
            CreateNodeCommand theCommand = (CreateNodeCommand)command;
            Path path = theCommand.getPath();
            Path parent = path.getAncestor();
            // Look up the parent node, which must exist ...
            Node parentNode = content.getNode(parent);
            node = content.createNode(env, parentNode, path.getLastSegment().getName());
            // Now add the properties to the supplied node ...
            for (Property property : theCommand.getProperties()) {
                Name propName = property.getName();
                if (property.size() == 0) {
                    node.getProperties().remove(propName);
                    continue;
                }
                node.getProperties().put(propName, property);
            }
            assert node != null;
        }

        // Otherwise, check whether the command is applies to a path; all the remaining commands
        // that do so expect the node to exist ...
        else if (command instanceof ActsOnPath) {
            ActsOnPath theCommand = (ActsOnPath)command;
            Path path = theCommand.getPath();
            // Look up the node with the supplied path ...
            node = content.getNode(path);
            if (node == null) throw new InvalidPathException(InMemoryConnectorI18n.nodeDoesNotExist.text(path));
        }

        if (command instanceof GetChildrenCommand) {
            GetChildrenCommand theCommand = (GetChildrenCommand)command;
            assert command instanceof ActsOnPath;
            assert node != null;
            // Get the names of the children ...
            List<Node> children = node.getChildren();
            List<Segment> childSegments = new ArrayList<Segment>(children.size());
            for (Node child : children) {
                childSegments.add(child.getName());
            }
            theCommand.setChildren(childSegments);

        }
        if (command instanceof GetPropertiesCommand) {
            GetPropertiesCommand theCommand = (GetPropertiesCommand)command;
            assert command instanceof ActsOnPath;
            assert node != null;
            for (Property property : node.getProperties().values()) {
                theCommand.setProperty(property);
            }
        }
        if (command instanceof SetPropertiesCommand) {
            SetPropertiesCommand theCommand = (SetPropertiesCommand)command;
            assert command instanceof ActsOnPath;
            assert node != null;
            // Now set (or remove) the properties to the supplied node ...
            for (Property property : theCommand.getProperties()) {
                Name propName = property.getName();
                if (property.size() == 0) {
                    node.getProperties().remove(propName);
                    continue;
                }
                node.getProperties().put(propName, property);
            }
        }
        if (command instanceof DeleteBranchCommand) {
            assert command instanceof ActsOnPath;
            assert node != null;
            content.removeNode(env, node);
        }
        if (command instanceof CopyNodeCommand) {
            CopyNodeCommand theCommand = (CopyNodeCommand)command;
            boolean recursive = command instanceof CopyBranchCommand;
            // Look up the new parent, which must exist ...
            Path newPath = theCommand.getNewPath();
            Node newParent = content.getNode(newPath.getAncestor());
            if (newParent == null) {
                throw new InvalidPathException(InMemoryConnectorI18n.nodeDoesNotExist.text(newPath.getAncestor()));
            }
            content.copyNode(env, node, newParent, recursive);
        }
        if (command instanceof MoveBranchCommand) {
            MoveBranchCommand theCommand = (MoveBranchCommand)command;
            assert command instanceof ActsOnPath;
            assert node != null;
            // Look up the new parent, which must exist ...
            Path newPath = theCommand.getNewPath();
            Node newParent = content.getNode(newPath.getAncestor());
            if (newParent == null) {
                throw new InvalidPathException(InMemoryConnectorI18n.nodeDoesNotExist.text(newPath.getAncestor()));
            }
            node.setParent(newParent);
        }
    }

    /**
     * @return listener
     */
    protected RepositorySourceListener getListener() {
        return this.listener;
    }

}
