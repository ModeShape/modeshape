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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.connector.RepositorySourceException;
import org.jboss.dna.spi.connector.RepositorySourceListener;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PathNotFoundException;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactory;
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
     * @return cache
     */
    /*package*/Cache<Name, Object> getCache() {
        return cache;
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
    protected Name getUuidPropertyName( ExecutionContext context ) {
        if (uuidPropertyName == null) {
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            uuidPropertyName = nameFactory.create(this.source.getUuidPropertyName());
        }
        return this.uuidPropertyName;
    }

    protected Fqn<Path.Segment> getFullyQualifiedName( Path path ) {
        assert path != null;
        return Fqn.fromList(path.getSegmentsList());
    }

    /**
     * Get a relative fully-qualified name that consists only of the supplied segment.
     * 
     * @param pathSegment the segment from which the fully qualified name is to be created
     * @return the relative fully-qualified name
     */
    protected Fqn<Path.Segment> getFullyQualifiedName( Path.Segment pathSegment ) {
        assert pathSegment != null;
        return Fqn.fromElements(pathSegment);
    }

    protected Path getPath( PathFactory factory,
                            Fqn<Path.Segment> fqn ) {
        return factory.create(factory.createRootPath(), fqn.peekElements());
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
        assert original != null;
        assert newParent != null;
        // Get or create the new node ...
        Segment name = (Segment)original.getFqn().getLastElement();
        Node<Name, Object> copy = newParent.addChild(getFullyQualifiedName(name));
        // Copy the properties ...
        copy.clearData();
        copy.putAll(original.getData());
        if (uuidProperty != null) {
            // Generate a new UUID for the new node, overwriting any existing value from the original ...
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

    /**
     * Update (or create) the array of {@link Path.Segment path segments} for the children of the supplied node. This array
     * maintains the ordered list of children (since the {@link Cache} does not maintain the order). Invoking this method will
     * change any existing children that a {@link Path.Segment#getName() name part} that matches the supplied
     * <code>changedName</code> to have the appropriate {@link Path.Segment#getIndex() same-name sibling index}.
     * 
     * @param parent the parent node; may not be null
     * @param changedName the name that should be compared to the existing node siblings to determine whether the same-name
     *        sibling indexes should be updated; may not be null
     * @param context the execution context; may not be null
     */
    @SuppressWarnings( "unchecked" )
    protected void updateChildList( Node<Name, Object> parent,
                                    Name changedName,
                                    ExecutionContext context ) {
        assert parent != null;
        assert changedName != null;
        assert context != null;
        Set<Node<Name, Object>> children = parent.getChildren();
        final int numChildren = children.size();
        if (numChildren == 0) return;
        // Go through the children, looking for any children with the same name as the 'changedName'
        List<ChildInfo> childrenWithChangedName = new LinkedList<ChildInfo>();
        Path.Segment[] childSegments = new Path.Segment[children.size()];
        int index = 0;
        for (Node<Name, Object> child : children) {
            Path.Segment childSegment = (Path.Segment)child.getFqn().getLastElement();
            Name childName = childSegment.getName();
            if (childName.equals(changedName)) {
                ChildInfo info = new ChildInfo(child.getFqn(), index);
                childrenWithChangedName.add(info);
            }
            childSegments[index++] = childSegment;
        }
        // Go through the children with the same name as the 'changedName', making sure their indexes are correct ...
        assert childrenWithChangedName.isEmpty() == false;
        if (childrenWithChangedName.size() == 1) {
            // The child should have no indexes ...
            ChildInfo child = childrenWithChangedName.get(0);
            Fqn<Path.Segment> fqn = child.getFqn();
            Path.Segment segment = fqn.getLastElement();
            if (segment.hasIndex()) {
                // Determine the new name and index ...
                Path.Segment newSegment = context.getValueFactories().getPathFactory().createSegment(changedName);
                // Replace the child with the correct FQN ...
                changeNodeName(parent, fqn, newSegment, context);
                // Change the segment in the child list ...
                childSegments[child.getChildIndex()] = newSegment;
            }
        } else {
            // There is more than one child with the same name ...
            int i = 0;
            for (ChildInfo child : childrenWithChangedName) {
                Fqn<Path.Segment> fqn = child.getFqn();
                Path.Segment childSegment = fqn.getLastElement();
                if (childSegment.getIndex() != i) {
                    // Determine the new name and index ...
                    Path.Segment newSegment = context.getValueFactories().getPathFactory().createSegment(changedName, i);
                    // Replace the child with the correct FQN ...
                    changeNodeName(parent, fqn, newSegment, context);
                    // Change the segment in the child list ...
                    childSegments[child.getChildIndex()] = newSegment;
                }
                ++i;
            }
        }
        // Record the list of children as a property on the parent ...
        // (Do this last, as it doesn't need to be done if there's an exception in the above logic)
        parent.put(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST, childSegments); // replaces any existing value
    }

    /**
     * Utility class used by the {@link JBossCacheConnection#updateChildList(Node, Name, ExecutionContext)} method.
     * 
     * @author Randall Hauch
     */
    private static class ChildInfo {
        private final Fqn<Path.Segment> fqn;
        private final int childIndex;

        protected ChildInfo( Fqn<Path.Segment> fqn,
                             int childIndex ) {
            assert fqn != null;
            this.fqn = fqn;
            this.childIndex = childIndex;
        }

        public int getChildIndex() {
            return childIndex;
        }

        public Fqn<Path.Segment> getFqn() {
            return fqn;
        }
    }

    /**
     * Changes the name of the node in the cache (but does not update the list of child segments stored on the parent).
     * 
     * @param parent
     * @param existing
     * @param newSegment
     * @param context
     */
    protected void changeNodeName( Node<Name, Object> parent,
                                   Fqn<Path.Segment> existing,
                                   Path.Segment newSegment,
                                   ExecutionContext context ) {
        assert parent != null;
        assert existing != null;
        assert newSegment != null;
        assert context != null;
        parent.removeChild(existing);
        List<Path.Segment> elements = existing.peekElements();
        assert elements.size() > 0;
        elements.set(elements.size() - 1, newSegment);
        existing = Fqn.fromList(elements);
        parent.addChild(existing);

    }

    protected class Executor extends AbstractCommandExecutor {

        private final PropertyFactory propertyFactory;
        private final ValueFactory<UUID> uuidFactory;

        protected Executor( ExecutionContext context,
                            String sourceName ) {
            super(context, sourceName);
            this.propertyFactory = context.getPropertyFactory();
            this.uuidFactory = context.getValueFactories().getUuidFactory();
        }

        @Override
        public void execute( CreateNodeCommand command ) {
            Path path = command.getPath();
            Path parent = path.getAncestor();
            Fqn<Segment> childFqn = getFullyQualifiedName(path.getLastSegment());
            // Look up the parent node, which must exist ...
            Node<Name, Object> parentNode = getNode(parent);
            Node<Name, Object> node = parentNode.addChild(childFqn);

            // Update the children to account for same-name siblings.
            // This not only updates the FQN of the child nodes, but it also sets the property that stores the
            // the array of Path.Segment for the children (since the cache doesn't maintain order).
            updateChildList(parentNode, path.getLastSegment().getName(), getExecutionContext());

            // Add the UUID property (if required), which may be overwritten by a supplied property ...
            Name uuidPropertyName = getUuidPropertyName(getExecutionContext());
            if (uuidPropertyName != null) {
                node.put(uuidPropertyName, generateUuid());
            }
            // Now add the properties to the supplied node ...
            for (Property property : command.getPropertyIterator()) {
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

        @SuppressWarnings( "unchecked" )
        @Override
        public void execute( GetChildrenCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            Name uuidPropertyName = getUuidPropertyName(getExecutionContext());
            List<Path.Segment> segments = node.getFqn().peekElements();
            segments.add(null);
            // Get the names of the children, using the child list ...
            Path.Segment[] childList = (Path.Segment[])node.get(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST);
            for (Path.Segment child : childList) {
                // We have the child segment, but we need the UUID property ...
                segments.set(segments.size() - 1, child); // each iteration sets this last list element ...
                Fqn<Path.Segment> fqn = Fqn.fromList(segments);
                Node<Name, Object> childNode = node.getChild(fqn);
                Object uuid = childNode.getData().get(uuidPropertyName);
                if (uuid == null) {
                    uuid = generateUuid();
                    childNode.getData().put(uuidPropertyName, uuid);
                } else {
                    uuid = uuidFactory.create(uuid);
                }
                Property uuidProperty = propertyFactory.create(uuidPropertyName, uuid);
                command.addChild(child, uuidProperty);
            }
        }

        @Override
        public void execute( GetPropertiesCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            Map<Name, Object> dataMap = node.getData();
            for (Map.Entry<Name, Object> data : dataMap.entrySet()) {
                Name propertyName = data.getKey();
                // Don't allow the child list property to be accessed
                if (propertyName.equals(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST)) continue;
                Object values = data.getValue();
                Property property = propertyFactory.create(propertyName, values);
                command.setProperty(property);
            }
        }

        @Override
        public void execute( SetPropertiesCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            // Now set (or remove) the properties to the supplied node ...
            for (Property property : command.getPropertyIterator()) {
                Name propName = property.getName();
                // Don't allow the child list property to be removed or changed
                if (propName.equals(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST)) continue;
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
            // Update the children to account for same-name siblings.
            // This not only updates the FQN of the child nodes, but it also sets the property that stores the
            // the array of Path.Segment for the children (since the cache doesn't maintain order).
            updateChildList(newParent, newPath.getLastSegment().getName(), getExecutionContext());
        }

        @Override
        public void execute( CopyBranchCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            // Look up the new parent, which must exist ...
            Path newPath = command.getNewPath();
            Node<Name, Object> newParent = getNode(newPath.getAncestor());
            copyNode(node, newParent, true, null);
            // Update the children to account for same-name siblings.
            // This not only updates the FQN of the child nodes, but it also sets the property that stores the
            // the array of Path.Segment for the children (since the cache doesn't maintain order).
            updateChildList(newParent, newPath.getLastSegment().getName(), getExecutionContext());
        }

        @Override
        public void execute( MoveBranchCommand command ) {
            Node<Name, Object> node = getNode(command.getPath());
            boolean recursive = true;
            Name uuidProperty = getUuidPropertyName(getExecutionContext());
            // Look up the new parent, which must exist ...
            Path newPath = command.getNewPath();
            Node<Name, Object> newParent = getNode(newPath.getAncestor());
            copyNode(node, newParent, recursive, uuidProperty);
            // Update the children to account for same-name siblings.
            // This not only updates the FQN of the child nodes, but it also sets the property that stores the
            // the array of Path.Segment for the children (since the cache doesn't maintain order).
            updateChildList(newParent, newPath.getLastSegment().getName(), getExecutionContext());
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
            return JBossCacheConnection.this.getNode(getExecutionContext(), path);
        }

    }

}
