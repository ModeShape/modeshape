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
package org.jboss.dna.graph.connector.inmemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.map.AbstractMapWorkspace;
import org.jboss.dna.graph.connector.map.MapNode;
import org.jboss.dna.graph.connector.map.MapRepository;
import org.jboss.dna.graph.connector.map.MapWorkspace;
import org.jboss.dna.graph.property.Name;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class InMemoryRepository extends MapRepository {

    public InMemoryRepository( String sourceName,
                               UUID rootNodeUuid ) {
        super(sourceName, rootNodeUuid, null);
        initialize();
    }

    public InMemoryRepository( String sourceName,
                               UUID rootNodeUuid,
                               String defaultWorkspaceName ) {
        super(sourceName, rootNodeUuid, defaultWorkspaceName);
        initialize();
    }

    @Override
    @GuardedBy( "getLock()" )
    protected MapWorkspace createWorkspace( ExecutionContext context,
                                            String name ) {
        return new Workspace(this, name);
    }

    protected class Workspace extends AbstractMapWorkspace {
        private final Map<UUID, MapNode> nodesByUuid = new HashMap<UUID, MapNode>();

        public Workspace( MapRepository repository,
                          String name ) {
            super(repository, name);

            initialize();
        }

        @Override
        protected void addNodeToMap( MapNode node ) {
            assert node != null;
            assert nodesByUuid != null;
            nodesByUuid.put(node.getUuid(), node);
        }

        @Override
        protected MapNode removeNodeFromMap( UUID nodeUuid ) {
            assert nodeUuid != null;
            return nodesByUuid.remove(nodeUuid);
        }

        @Override
        protected void removeAllNodesFromMap() {
            nodesByUuid.clear();
        }

        @Override
        public MapNode getNode( UUID nodeUuid ) {
            assert nodeUuid != null;
            return nodesByUuid.get(nodeUuid);
        }

        /**
         * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note
         * that internal references between nodes within the original subgraph must be reflected as internal nodes within the new
         * subgraph.
         * <p>
         * This method is trivially overridden for testing purposes.
         * </p>
         * 
         * @param context the context; may not be null
         * @param original the node to be copied; may not be null
         * @param newWorkspace the workspace containing the new parent node; may not be null
         * @param newParent the parent where the copy is to be placed; may not be null
         * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
         * @param recursive true if the copy should be recursive
         * @param oldToNewUuids the map of UUIDs of nodes in the new subgraph keyed by the UUIDs of nodes in the original; may be
         *        null if the UUIDs are to be maintained
         * @return the new node, which is the top of the new subgraph
         */
        @Override
        protected MapNode copyNode( ExecutionContext context,
                                    MapNode original,
                                    MapWorkspace newWorkspace,
                                    MapNode newParent,
                                    Name desiredName,
                                    boolean recursive,
                                    Map<UUID, UUID> oldToNewUuids ) {
            return super.copyNode(context, original, newWorkspace, newParent, desiredName, recursive, oldToNewUuids);
        }

        /**
         * Method added to support testing
         * 
         * @return the number of nodes in the backing map
         */
        final int size() {
            return nodesByUuid.size();
        }
    }
}
