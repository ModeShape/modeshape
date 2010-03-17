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
package org.modeshape.connector.store.jpa.model.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Represents a temporary working area for a query that efficiently retrieves the nodes in a subgraph. This class uses the
 * database to build up the content of the subgraph, and therefore requires write privilege on the database. The benefit is that
 * it minimizes the amount of memory required to process the subgraph, plus the set of nodes that make up the subgraph can be
 * produced with database joins.
 * <p>
 * The use of database joins also produces another benefit: the number of SQL statements necessary to build the set of nodes in a
 * subgraph is equal to the depth of the subgraph, regardless of the number of child nodes at any level.
 * </p>
 */
public class SubgraphQuery {

    /**
     * Create a query that returns a subgraph at and below the node with the supplied path and the supplied UUID.
     * 
     * @param entities the entity manager; may not be null
     * @param workspaceId the ID of the workspace; may not be null
     * @param subgraphRootUuid the UUID (in string form) of the root node in the subgraph
     * @param maxDepth the maximum depth of the subgraph, or 0 if there is no maximum depth
     * @return the object representing the subgraph
     */
    public static SubgraphQuery create( EntityManager entities,
                                        Long workspaceId,
                                        UUID subgraphRootUuid,
                                        int maxDepth ) {
        assert entities != null;
        assert subgraphRootUuid != null;
        assert workspaceId != null;
        assert maxDepth >= 0;
        if (maxDepth == 0) maxDepth = Integer.MAX_VALUE;
        final String subgraphRootUuidString = subgraphRootUuid.toString();
        // Create a new subgraph query, and add a child for the root ...

        SubgraphQueryEntity query = new SubgraphQueryEntity(workspaceId, subgraphRootUuidString);
        entities.persist(query);
        Long queryId = query.getId();

        try {
            // Insert a node for the root (this will be the starting point for the recursive operation) ...
            SubgraphNodeEntity root = new SubgraphNodeEntity(queryId, subgraphRootUuidString, 0);
            entities.persist(root);

            // Now add the children by inserting the children, one level at a time.
            // Note that we do this for the root, and for each level until 1 BEYOND
            // the max depth (so that we can get the children for the nodes that are
            // at the maximum depth)...
            Query statement = entities.createNamedQuery("SubgraphNodeEntity.insertChildren");
            int numChildrenInserted = 0;
            int parentLevel = 0;
            while (parentLevel <= maxDepth) {
                // Insert the children of the next level by inserting via a select (join) of the children
                statement.setParameter("queryId", queryId);
                statement.setParameter("workspaceId", workspaceId);
                statement.setParameter("parentDepth", parentLevel);
                numChildrenInserted = statement.executeUpdate();
                if (numChildrenInserted == 0) break;
                parentLevel = parentLevel + 1;
            }
        } catch (RuntimeException t) {
            // Clean up the search and results ...
            try {
                Query search = entities.createNamedQuery("SubgraphNodeEntity.deleteByQueryId");
                search.setParameter("queryId", query.getId());
                search.executeUpdate();
            } finally {
                entities.remove(query);
            }
            throw t;
        }

        return new SubgraphQuery(entities, workspaceId, query, maxDepth);
    }

    private final EntityManager manager;
    private final Long workspaceId;
    private SubgraphQueryEntity query;
    private final int maxDepth;

    protected SubgraphQuery( EntityManager manager,
                             Long workspaceId,
                             SubgraphQueryEntity query,
                             int maxDepth ) {
        assert manager != null;
        assert query != null;
        assert workspaceId != null;

        this.manager = manager;
        this.workspaceId = workspaceId;
        this.query = query;
        this.maxDepth = maxDepth;
    }

    /**
     * @return maxDepth
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * @return manager
     */
    public EntityManager getEntityManager() {
        return manager;
    }

    /**
     * Get the {@link NodeEntity root node} of the subgraph. This must be called before the query is {@link #close() closed}.
     * 
     * @return the subgraph's root nodes
     */
    public NodeEntity getNode() {
        // Now query for all the nodes and put into a list ...
        Query search = manager.createNamedQuery("SubgraphNodeEntity.getChildEntities");
        search.setParameter("queryId", query.getId());
        search.setParameter("workspaceId", workspaceId);
        search.setParameter("depth", 0);
        search.setParameter("maxDepth", 0);

        // Now process the nodes below the subgraph's root ...
        return (NodeEntity)search.getSingleResult();
    }

    /**
     * Get the {@link NodeEntity nodes} in the subgraph. This must be called before the query is {@link #close() closed}.
     * 
     * @param includeRoot true if the subgraph's root node is to be included, or false otherwise
     * @param includeChildrenOfMaxDepthNodes true if the method is to include nodes that are children of nodes that are at the
     *        maximum depth, or false if only nodes up to the maximum depth are to be included
     * @return the list of nodes, in breadth-first order
     */
    @SuppressWarnings( "unchecked" )
    public List<NodeEntity> getNodes( boolean includeRoot,
                                      boolean includeChildrenOfMaxDepthNodes ) {
        if (query == null) throw new IllegalStateException();
        // Now query for all the nodes and put into a list ...
        Query search = manager.createNamedQuery("SubgraphNodeEntity.getChildEntities");
        search.setParameter("queryId", query.getId());
        search.setParameter("workspaceId", workspaceId);
        search.setParameter("depth", includeRoot ? 0 : 1);
        search.setParameter("maxDepth", includeChildrenOfMaxDepthNodes ? maxDepth : maxDepth - 1);

        // Now process the nodes below the subgraph's root ...
        return search.getResultList();
    }

    /**
     * Delete the nodes in the subgraph.
     * 
     * @param includeRoot true if the root node should also be deleted
     */
    @SuppressWarnings( "unchecked" )
    public void deleteSubgraph( boolean includeRoot ) {
        if (query == null) throw new IllegalStateException();

        List<NodeEntity> nodes = getNodes(true, true);
        List<String> uuids = new ArrayList<String>(nodes.size());
        for (NodeEntity node : nodes) {
            uuids.add(node.getNodeUuidString());
        }

        // Delete the LargeValueEntities ...
        Query withLargeValues = manager.createNamedQuery("SubgraphNodeEntity.getNodeEntitiesWithLargeValues");
        withLargeValues.setParameter("queryId", query.getId());
        withLargeValues.setParameter("depth", includeRoot ? 0 : 1);
        withLargeValues.setParameter("workspaceId", workspaceId);
        List<NodeEntity> nodesWithLargeValues = withLargeValues.getResultList();
        if (nodesWithLargeValues.size() != 0) {
            for (NodeEntity node : nodesWithLargeValues) {
                node.getLargeValues().clear();
            }
            manager.flush();
        }

        // Delete the ChildEntities ...
        Query delete = manager.createNamedQuery("SubgraphNodeEntity.clearParentReferences");
        delete.setParameter("queryId", query.getId());
        delete.setParameter("depth", includeRoot ? 0 : 1);
        delete.setParameter("workspaceId", workspaceId);
        delete.executeUpdate();

        delete = manager.createNamedQuery("SubgraphNodeEntity.deleteChildEntities");
        delete.setParameter("queryId", query.getId());
        delete.setParameter("depth", includeRoot ? 0 : 1);
        delete.setParameter("workspaceId", workspaceId);
        delete.executeUpdate();

        manager.flush();
    }

    /**
     * Close this query object and clean up all in-database records associated with this query. This method <i>must</i> be called
     * when this query is no longer needed, and once it is called, this subgraph query is no longer usable.
     */
    public void close() {
        if (query == null) return;
        // Clean up the search and results ...
        try {
            Query search = manager.createNamedQuery("SubgraphNodeEntity.deleteByQueryId");
            search.setParameter("queryId", query.getId());
            search.executeUpdate();
        } finally {
            try {
                manager.remove(query);
            } finally {
                query = null;
            }
        }
    }
}
