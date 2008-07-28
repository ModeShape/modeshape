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
package org.jboss.dna.connector.jbosscache;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PathNotFoundException;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.CopyBranchCommand;
import org.jboss.dna.spi.graph.commands.CopyNodeCommand;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.DeleteBranchCommand;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.MoveBranchCommand;
import org.jboss.dna.spi.graph.commands.RecordBranchCommand;
import org.jboss.dna.spi.graph.commands.SetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.executor.AbstractCommandExecutor;
import org.jboss.dna.spi.graph.commands.executor.CommandExecutor;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;

/**
 * @author Randall Hauch
 */
public class JBossCacheConnection implements RepositoryConnection {

    protected static final RepositorySourceListener NO_OP_LISTENER = new RepositorySourceListener() {

        /**
         * {@inheritDoc}
         */
        public void notify( String sourceName,
                            Object... events ) {
            // do nothing
        }
    };

    private boolean initializedUuidPropertyName = false;
    private Name uuidPropertyName;
    private final JBossCacheSource source;
    private final Cache<Name, Object> cache;
    private RepositorySourceListener listener = NO_OP_LISTENER;

    JBossCacheConnection( JBossCacheSource source,
                          Cache<Name, Object> cache ) {
        assert source != null;
        assert cache != null;
        this.source = source;
        this.cache = cache;
    }

    /**
     * @return uuidPropertyName
     */
    public Name getUuidPropertyName() {
        return this.uuidPropertyName;
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
        this.cache.getRoot();
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
    public void execute( ExecutionContext context,
                         GraphCommand... commands ) throws RepositorySourceException, InterruptedException {
        // Now execute the commands ...
        CommandExecutor executor = new Executor(context, this.getSourceName());
        for (GraphCommand command : commands) {
            executor.execute(command);
        }
    }

    /**
     * @return listener
     */
    protected RepositorySourceListener getListener() {
        return this.listener;
    }

    /**
     * Utility method to calculate (if required) and obtain the name that should be used to store the UUID values for each node.
     * This method may be called without regard to synchronization, since it should return the same value if it happens to be
     * called concurrently while not yet initialized.
     * 
     * @param context the execution context
     * @return the name, or null if the UUID should not be stored
     */
    protected Name getUuidProperty( ExecutionContext context ) {
        if (!initializedUuidPropertyName) {
            this.uuidPropertyName = this.source.getUuidPropertyName(context.getValueFactories().getNameFactory());
            initializedUuidPropertyName = true;
        }
        return this.uuidPropertyName;
    }

    protected Fqn<Path.Segment> getFullyQualifiedName( Path path ) {
        return Fqn.fromList(path.getSegmentsList());
    }

    /**
     * Get a relative fully-qualified name that consists only of the supplied segment.
     * 
     * @param pathSegment the segment from which the fully qualified name is to be created
     * @return the relative fully-qualified name
     */
    protected Fqn<Path.Segment> getFullyQualifiedName( Path.Segment pathSegment ) {
        return Fqn.fromElements(pathSegment);
    }

    protected Path getPath( PathFactory factory,
                            Fqn<Path.Segment> fqn ) {
        Path.Segment[] segments = new Path.Segment[fqn.size()];
        for (int i = 0; i != segments.length; ++i) {
            segments[i] = fqn.get(i);
        }
        return factory.create(factory.createRootPath(), segments);
    }

    protected Node<Name, Object> getNode( ExecutionContext context,
                                          Path path ) {
        // Look up the node with the supplied path ...
        Fqn<Segment> fqn = getFullyQualifiedName(path);
        Node<Name, Object> node = cache.getNode(fqn);
        if (node == null) {
            String nodePath = path.getString(context.getNamespaceRegistry());
            Path lowestExisting = null;
            while (fqn != null) {
                fqn = fqn.getParent();
                node = cache.getNode(fqn);
                if (node != null) {
                    lowestExisting = getPath(context.getValueFactories().getPathFactory(), fqn);
                    fqn = null;
                }
            }
            throw new PathNotFoundException(path, lowestExisting, JBossCacheConnectorI18n.nodeDoesNotExist.text(nodePath));
        }
        return node;

    }

    protected UUID generateUuid() {
        return UUID.randomUUID();
    }

    protected int copyNode( Node<Name, Object> original,
                            Node<Name, Object> newParent,
                            boolean recursive,
                            Name uuidProperty ) {
        // Get or create the new node ...
        Segment name = (Segment)original.getFqn().getLastElement();
        Node<Name, Object> copy = newParent.addChild(getFullyQualifiedName(name));
        // Copy the properties ...
        copy.clearData();
        copy.putAll(original.getData());
        if (uuidProperty != null) {
            // Generate a new UUID for the new node ...
            copy.put(uuidProperty, generateUuid());
        }
        int numNodesCopied = 1;
        if (recursive) {
            // Loop over each child and call this method ...
            for (Node<Name, Object> child : original.getChildren()) {
                numNodesCopied += copyNode(child, copy, true, uuidProperty);
            }
        }
        return numNodesCopied;
    }

    protected class Executor extends AbstractCommandExecutor {

        private final PropertyFactory propertyFactory;

        protected Executor( ExecutionContext context,
                            String sourceName ) {
            super(context, sourceName);
            this.propertyFactory = context.getPropertyFactory();
        }

        @Override
        public void execute( CreateNodeCommand command ) {
            Path path = command.getPath();
            Path parent = path.getAncestor();
            Fqn<Segment> childFqn = getFullyQualifiedName(path.getLastSegment());
            // Look up the parent node, which must exist ...
            Node<Name, Object> parentNode = getNode(parent);
            Node<Name, Object> node = parentNode.addChild(childFqn);
            // Add the UUID property (if required), which may be overwritten by a supplied property ...
            Name uuidPropertyName = getUuidProperty(getEnvironment());
            if (uuidPropertyName != null) {
                node.put(uuidPropertyName, generateUuid());
            }
            // Now add the properties to the supplied node ...
            for (Property property : command.getProperties()) {
                if (property.size() == 0) continue;
                Name propName = property.getName();
                Object value = null;
                if (property.size() == 1) {
                    value = property.iterator().next();
                } else {
                    value = property.getValuesAsArray();
                }
                node.put(propName, value);
            }
        }

        @Override
        public void execute( GetChildrenCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            // Get the names of the children ...
            List<Segment> childSegments = new ArrayList<Segment>();
            for (Node<Name, Object> child : node.getChildren()) {
                childSegments.add((Segment)child.getFqn().getLastElement());
            }
            command.setChildren(childSegments);
        }

        @Override
        public void execute( GetPropertiesCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            Map<Name, Object> dataMap = node.getData();
            for (Map.Entry<Name, Object> data : dataMap.entrySet()) {
                Name propertyName = data.getKey();
                Object values = data.getValue();
                Property property = propertyFactory.create(propertyName, values);
                command.setProperty(property);
            }
        }

        @Override
        public void execute( SetPropertiesCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            // Now set (or remove) the properties to the supplied node ...
            for (Property property : command.getProperties()) {
                Name propName = property.getName();
                if (property.size() == 0) {
                    node.remove(propName);
                    continue;
                }
                Object value = null;
                if (property.size() == 1) {
                    value = property.iterator().next();
                } else {
                    value = property.getValuesAsArray();
                }
                node.put(propName, value);
            }
        }

        @Override
        public void execute( DeleteBranchCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            node.getParent().removeChild(node.getFqn().getLastElement());
        }

        @Override
        public void execute( CopyNodeCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            // Look up the new parent, which must exist ...
            Path newPath = command.getNewPath();
            Node<Name, Object> newParent = getNode(newPath.getAncestor());
            copyNode(node, newParent, false, null);
        }

        @Override
        public void execute( CopyBranchCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            // Look up the new parent, which must exist ...
            Path newPath = command.getNewPath();
            Node<Name, Object> newParent = getNode(newPath.getAncestor());
            copyNode(node, newParent, true, null);
        }

        @Override
        public void execute( MoveBranchCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            boolean recursive = true;
            Name uuidProperty = getUuidProperty(getEnvironment());
            // Look up the new parent, which must exist ...
            Path newPath = command.getNewPath();
            Node<Name, Object> newParent = getNode(newPath.getAncestor());
            copyNode(node, newParent, recursive, uuidProperty);
            // Now delete the old node ...
            Node<Name, Object> oldParent = node.getParent();
            boolean removed = oldParent.removeChild(node.getFqn().getLastElement());
            assert removed;
        }

        @Override
        public void execute( RecordBranchCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            recordNode(command, node);
        }

        protected void recordNode( RecordBranchCommand command,
                                   Node<Name, Object> node ) {
            // Record the properties ...
            Map<Name, Object> dataMap = node.getData();
            List<Property> properties = new LinkedList<Property>();
            for (Map.Entry<Name, Object> data : dataMap.entrySet()) {
                Name propertyName = data.getKey();
                Object values = data.getValue();
                Property property = propertyFactory.create(propertyName, values);
                properties.add(property);
            }
            command.record(command.getPath(), properties);
            // Now record the children ...
            for (Node<Name, Object> child : node.getChildren()) {
                recordNode(command, child);
            }
        }

        protected Node<Name, Object> getNode( Path path ) {
            return JBossCacheConnection.this.getNode(getEnvironment(), path);
        }

    }

}
