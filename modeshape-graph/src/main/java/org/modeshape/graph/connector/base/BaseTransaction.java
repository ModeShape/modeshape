/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.connector.base;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.LockFailedException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.request.LockBranchRequest.LockScope;

/**
 * @param <NodeType> the type of node
 * @param <WorkspaceType> the type of workspace
 */
@NotThreadSafe
public abstract class BaseTransaction<NodeType extends Node, WorkspaceType extends Workspace>
    implements Transaction<NodeType, WorkspaceType> {

    protected final UUID rootNodeUuid;
    protected final ExecutionContext context;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final PropertyFactory propertyFactory;
    protected final ValueFactories valueFactories;
    protected final Location rootLocation;

    /** The repository against which this transaction is operating */
    private final Repository<NodeType, WorkspaceType> repository;

    protected BaseTransaction( ExecutionContext context,
                               Repository<NodeType, WorkspaceType> repository,
                               UUID rootNodeUuid ) {
        this.rootNodeUuid = rootNodeUuid;
        this.context = context;
        this.propertyFactory = context.getPropertyFactory();
        this.valueFactories = context.getValueFactories();
        this.pathFactory = valueFactories.getPathFactory();
        this.nameFactory = valueFactories.getNameFactory();
        this.repository = repository;
        this.rootLocation = Location.create(pathFactory.createRootPath(), rootNodeUuid);
    }

    public Location getRootLocation() {
        return this.rootLocation;
    }

    public UUID getRootUuid() {
        return this.rootNodeUuid;
    }

    protected String readable( Object obj ) {
        return valueFactories.getStringFactory().create(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getContext()
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * Obtain the repository object against which this transaction is running.
     * 
     * @return the repository object; never null
     */
    protected Repository<NodeType, WorkspaceType> getRepository() {
        return repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getWorkspaceNames()
     */
    public Set<String> getWorkspaceNames() {
        return repository.getWorkspaceNames();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getRootNode(org.modeshape.graph.connector.base.Workspace)
     */
    public NodeType getRootNode( WorkspaceType workspace ) {
        return getNode(workspace, rootLocation);
    }

    protected NodeType getNode( WorkspaceType workspace,
                                Path path,
                                Location location ) {
        NodeType node = getRootNode(workspace);
        for (Path.Segment segment : path) {
            NodeType child = getChild(workspace, node, segment);
            if (child == null) {
                List<Path.Segment> segments = new LinkedList<Path.Segment>();
                for (Path.Segment seg : path) {
                    if (seg != segment) segments.add(seg);
                    else break;
                }
                Path lowestExisting = pathFactory.createAbsolutePath(segments);
                throw new PathNotFoundException(location, lowestExisting, GraphI18n.nodeDoesNotExist.text(path));
            }
            node = child;
        }
        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#pathFor(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    public Path pathFor( WorkspaceType workspace,
                         NodeType node ) {
        assert node != null;
        assert pathFactory != null;

        LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
        do {
            segments.addFirst(node.getName());
            node = getParent(workspace, node);
        } while (node != null);
        segments.removeFirst(); // remove the root name, which is meaningless

        return pathFactory.createAbsolutePath(segments);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is implemented by iterating through the children, looking for the first child that has a matching name.
     * Obviously this may be implemented more efficiently in certain systems. For example, an implementation might create a path
     * by appending the child name to the supplied parent, and find the node with this path.
     * </p>
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getFirstChild(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Name)
     */
    public NodeType getFirstChild( WorkspaceType workspace,
                                   NodeType parent,
                                   Name childName ) {
        for (NodeType child : getChildren(workspace, parent)) {
            if (child.getName().getName().equals(childName)) return child;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#lockNode(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.request.LockBranchRequest.LockScope, long)
     */
    public void lockNode( WorkspaceType workspace,
                          NodeType node,
                          LockScope lockScope,
                          long lockTimeoutInMillis ) throws LockFailedException {
        // do nothing by default
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#unlockNode(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    public void unlockNode( WorkspaceType workspace,
                            NodeType node ) {
        // do nothing by default
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#commit()
     */
    public void commit() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#rollback()
     */
    public void rollback() {
    }
}
