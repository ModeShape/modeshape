/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector.path;

import java.util.Map;
import java.util.UUID;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.NodeConflictBehavior;
import org.jboss.dna.graph.connector.LockFailedException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.request.AccessQueryRequest;
import org.jboss.dna.graph.request.LockBranchRequest.LockScope;

/**
 * Implementation of some methods from {@link WritablePathWorkspace} to assist in the development of path-based connectors.
 * Subclasses of this class should be made thread-safe.
 */
public abstract class AbstractWritablePathWorkspace implements WritablePathWorkspace {

    private final String name;
    protected final UUID rootNodeUuid;

    public AbstractWritablePathWorkspace( String name,
                                          UUID rootNodeUuid ) {
        super();
        this.name = name;
        this.rootNodeUuid = rootNodeUuid;
    }

    /**
     * This should copy the subgraph rooted at the original node and place the new copy under the supplied new parent. Note that
     * internal references between nodes within the original subgraph must be reflected as internal nodes within the new subgraph.
     * 
     * @param context the context; may not be null
     * @param original the node to be copied; may not be null
     * @param originalWorkspace the workspace containing the original node; may not be null
     * @param newParent the parent where the copy is to be placed; may not be null
     * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
     * @param recursive true if the copy should be recursive
     * @return the new node, which is the top of the new subgraph
     */
    public PathNode copyNode( ExecutionContext context,
                              PathNode original,
                              PathWorkspace originalWorkspace,
                              PathNode newParent,
                              Name desiredName,
                              boolean recursive ) {
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        PathNode copy = createNode(context, newParent, desiredName, original.getProperties(), NodeConflictBehavior.REPLACE);

        if (recursive) {
            Path originalPath = original.getPath();

            for (Segment childSegment : original.getChildSegments()) {
                Path childPath = pathFactory.create(originalPath, childSegment);
                PathNode childNode = originalWorkspace.getNode(childPath);
                copyNode(context, childNode, originalWorkspace, copy, childSegment.getName(), true);
            }
        }
        return copy;
    }

    public PathNode createNode( ExecutionContext context,
                                String pathToNewNode,
                                Map<Name, Property> properties,
                                NodeConflictBehavior conflictBehavior ) {
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path newPath = pathFactory.create(pathToNewNode);

        return createNode(context, getNode(newPath.getParent()), newPath.getLastSegment().getName(), properties, conflictBehavior);
    }

    public PathNode moveNode( ExecutionContext context,
                              PathNode node,
                              Name desiredNewName,
                              WritablePathWorkspace originalWorkspace,
                              PathNode newParent,
                              PathNode beforeNode ) {
        if (desiredNewName == null) {
            assert !node.getPath().isRoot();
            desiredNewName = node.getPath().getLastSegment().getName();
        }

        PathNode newCopy = copyNode(context, node, originalWorkspace, newParent, desiredNewName, true);
        originalWorkspace.removeNode(context, node.getPath());
        return newCopy;
    }

    public QueryResults query( ExecutionContext context,
                               AccessQueryRequest accessQuery ) {
        return null;
    }

    public QueryResults search( ExecutionContext context,
                                String fullTextSearchExpression ) {
        return null;
    }

    public String getName() {
        return this.name;
    }

    public void lockNode( PathNode node,
                          LockScope lockScope,
                          long lockTimeoutInMillis ) throws LockFailedException {
    }

    public void unlockNode( PathNode node ) {
    }

}
