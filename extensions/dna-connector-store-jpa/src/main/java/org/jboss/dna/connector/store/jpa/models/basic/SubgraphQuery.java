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
package org.jboss.dna.connector.store.jpa.models.basic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;

/**
 * Represents a temporary working area for a query that efficiently retrieves the nodes in a subgraph. This class uses the
 * database to build up the content of the subgraph, and therefore requires write privilege on the database. The benefit is that
 * it minimizes the amount of memory required to process the subgraph, plus the set of nodes that make up the subgraph can be
 * produced with database joins.
 * <p>
 * The use of database joins also produces another benefit: the number of SQL statements necessary to build the set of nodes in a
 * subgraph is equal to the depth of the subgraph, regardless of the number of child nodes at any level.
 * </p>
 * 
 * @author Randall Hauch
 */
public class SubgraphQuery {

    /**
     * Create a query that returns a subgraph at and below the node with the supplied path and the supplied UUID.
     * 
     * @param context the execution context; may not be null
     * @param entities the entity manager; may not be null
     * @param subgraphRootUuid the UUID (in string form) of the root node in the subgraph
     * @param subgraphRootPath the path of the root node in the subgraph
     * @param maxDepth the maximum depth of the subgraph, or 0 if there is no maximum depth
     * @return the object representing the subgraph
     */
    public static SubgraphQuery create( ExecutionContext context,
                                        EntityManager entities,
                                        UUID subgraphRootUuid,
                                        Path subgraphRootPath,
                                        int maxDepth ) {
        assert entities != null;
        assert subgraphRootUuid != null;
        assert maxDepth >= 0;
        if (maxDepth == 0) maxDepth = Integer.MAX_VALUE;
        final String subgraphRootUuidString = subgraphRootUuid.toString();
        // Create a new subgraph query, and add a child for the root ...
        SubgraphQueryEntity query = new SubgraphQueryEntity(subgraphRootUuidString);
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
        return new SubgraphQuery(context, entities, query, subgraphRootPath, maxDepth);
    }

    private final ExecutionContext context;
    private final EntityManager manager;
    private SubgraphQueryEntity query;
    private final int maxDepth;
    private final Path subgraphRootPath;

    protected SubgraphQuery( ExecutionContext context,
                             EntityManager manager,
                             SubgraphQueryEntity query,
                             Path subgraphRootPath,
                             int maxDepth ) {
        assert manager != null;
        assert query != null;
        assert context != null;
        assert subgraphRootPath != null;
        this.context = context;
        this.manager = manager;
        this.query = query;
        this.maxDepth = maxDepth;
        this.subgraphRootPath = subgraphRootPath;
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
     * @return subgraphRootPath
     */
    public Path getSubgraphRootPath() {
        return subgraphRootPath;
    }

    /**
     * @return query
     */
    public SubgraphQueryEntity getSubgraphQueryEntity() {
        if (query == null) throw new IllegalStateException();
        return query;
    }

    public int getNodeCount( boolean includeRoot ) {
        if (query == null) throw new IllegalStateException();
        // Now query for all the nodes and put into a list ...
        Query search = manager.createNamedQuery("SubgraphNodeEntity.getCount");
        search.setParameter("queryId", query.getId());

        // Now process the nodes below the subgraph's root ...
        try {
            return (Integer)search.getSingleResult() - (includeRoot ? 0 : 1);
        } catch (NoResultException e) {
            return 0;
        }
    }

    /**
     * Get the {@link ChildEntity root node} of the subgraph. This must be called before the query is {@link #close() closed}.
     * 
     * @return the subgraph's root nodes
     */
    public ChildEntity getNode() {
        // Now query for all the nodes and put into a list ...
        Query search = manager.createNamedQuery("SubgraphNodeEntity.getChildEntities");
        search.setParameter("queryId", query.getId());
        search.setParameter("depth", 0);
        search.setParameter("maxDepth", 0);

        // Now process the nodes below the subgraph's root ...
        return (ChildEntity)search.getSingleResult();
    }

    /**
     * Get the {@link ChildEntity nodes} in the subgraph. This must be called before the query is {@link #close() closed}.
     * 
     * @param includeRoot true if the subgraph's root node is to be included, or false otherwise
     * @param includeChildrenOfMaxDepthNodes true if the method is to include nodes that are children of nodes that are at the
     *        maximum depth, or false if only nodes up to the maximum depth are to be included
     * @return the list of nodes, in breadth-first order
     */
    @SuppressWarnings( "unchecked" )
    public List<ChildEntity> getNodes( boolean includeRoot,
                                       boolean includeChildrenOfMaxDepthNodes ) {
        if (query == null) throw new IllegalStateException();
        // Now query for all the nodes and put into a list ...
        Query search = manager.createNamedQuery("SubgraphNodeEntity.getChildEntities");
        search.setParameter("queryId", query.getId());
        search.setParameter("depth", includeRoot ? 0 : 1);
        search.setParameter("maxDepth", includeChildrenOfMaxDepthNodes ? maxDepth : maxDepth - 1);

        // Now process the nodes below the subgraph's root ...
        return search.getResultList();
    }

    /**
     * Get the {@link PropertiesEntity properties} for each of the nodes in the subgraph. This must be called before the query is
     * {@link #close() closed}.
     * 
     * @param includeRoot true if the properties for the subgraph's root node are to be included, or false otherwise
     * @param includeChildrenOfMaxDepthNodes true if the method is to include nodes that are children of nodes that are at the
     *        maximum depth, or false if only nodes up to the maximum depth are to be included
     * @return the list of properties for each of the nodes, in breadth-first order
     */
    @SuppressWarnings( "unchecked" )
    public List<PropertiesEntity> getProperties( boolean includeRoot,
                                                 boolean includeChildrenOfMaxDepthNodes ) {
        if (query == null) throw new IllegalStateException();
        // Now query for all the nodes and put into a list ...
        Query search = manager.createNamedQuery("SubgraphNodeEntity.getPropertiesEntities");
        search.setParameter("queryId", query.getId());
        search.setParameter("depth", includeRoot ? 0 : 1);
        search.setParameter("maxDepth", includeChildrenOfMaxDepthNodes ? maxDepth : maxDepth - 1);

        // Now process the nodes below the subgraph's root ...
        return search.getResultList();
    }

    /**
     * Get the {@link Location} for each of the nodes in the subgraph. This must be called before the query is {@link #close()
     * closed}.
     * <p>
     * This method calls {@link #getNodes(boolean,boolean)}. Therefore, calling {@link #getNodes(boolean,boolean)} and this method
     * for the same subgraph is not efficient; consider just calling {@link #getNodes(boolean,boolean)} alone.
     * </p>
     * 
     * @param includeRoot true if the properties for the subgraph's root node are to be included, or false otherwise
     * @param includeChildrenOfMaxDepthNodes true if the method is to include nodes that are children of nodes that are at the
     *        maximum depth, or false if only nodes up to the maximum depth are to be included
     * @return the list of {@link Location locations}, one for each of the nodes in the subgraph, in breadth-first order
     */
    public List<Location> getNodeLocations( boolean includeRoot,
                                            boolean includeChildrenOfMaxDepthNodes ) {
        if (query == null) throw new IllegalStateException();
        // Set up a map of the paths to the nodes, keyed by UUIDs. This saves us from having to build
        // the paths every time ...
        Map<String, Path> pathByUuid = new HashMap<String, Path>();
        LinkedList<Location> locations = new LinkedList<Location>();
        String subgraphRootUuid = query.getRootUuid();
        pathByUuid.put(subgraphRootUuid, subgraphRootPath);
        UUID uuid = UUID.fromString(subgraphRootUuid);
        if (includeRoot) {
            locations.add(new Location(subgraphRootPath, uuid));
        }

        // Now iterate over the child nodes in the subgraph (we've already included the root) ...
        final PathFactory pathFactory = context.getValueFactories().getPathFactory();
        final NameFactory nameFactory = context.getValueFactories().getNameFactory();
        for (ChildEntity entity : getNodes(false, includeChildrenOfMaxDepthNodes)) {
            String parentUuid = entity.getId().getParentUuidString();
            Path parentPath = pathByUuid.get(parentUuid);
            assert parentPath != null;
            String nsUri = entity.getChildNamespace().getUri();
            String localName = entity.getChildName();
            int sns = entity.getSameNameSiblingIndex();
            Name childName = nameFactory.create(nsUri, localName);
            Path childPath = pathFactory.create(parentPath, childName, sns);
            String childUuid = entity.getId().getChildUuidString();
            pathByUuid.put(childUuid, childPath);
            uuid = UUID.fromString(childUuid);
            locations.add(new Location(childPath, uuid));

        }
        return locations;
    }

    @SuppressWarnings( "unchecked" )
    public void deleteSubgraph( boolean includeRoot ) {
        if (query == null) throw new IllegalStateException();

        // Delete the PropertiesEntities ...
        //
        // Right now, Hibernate is not able to support deleting PropertiesEntity in bulk because of the
        // large value association (and there's no way to clear the association in bulk).
        // Therefore, the only way to do this with Hibernate is to load each PropertiesEntity that has
        // large values and clear them. (Theoretically, fewer PropertiesEntity objects will have large values
        // than the total number in the subgraph.)
        // Then we can delete the properties.
        Query withLargeValues = manager.createNamedQuery("SubgraphNodeEntity.getPropertiesEntitiesWithLargeValues");
        withLargeValues.setParameter("queryId", query.getId());
        withLargeValues.setParameter("depth", includeRoot ? 0 : 1);
        List<PropertiesEntity> propertiesWithLargeValues = withLargeValues.getResultList();
        if (propertiesWithLargeValues.size() != 0) {
            for (PropertiesEntity props : propertiesWithLargeValues) {
                props.getLargeValues().clear();
            }
            manager.flush();
        }

        // Delete the PropertiesEntities, none of which will have large values ...
        Query delete = manager.createNamedQuery("SubgraphNodeEntity.deletePropertiesEntities");
        delete.setParameter("queryId", query.getId());
        delete.setParameter("depth", includeRoot ? 0 : 1);
        delete.executeUpdate();

        // Delete the ChildEntities ...
        delete = manager.createNamedQuery("SubgraphNodeEntity.deleteChildEntities");
        delete.setParameter("queryId", query.getId());
        delete.setParameter("depth", includeRoot ? 0 : 1);
        delete.executeUpdate();

        // Delete unused large values ...
        LargeValueEntity.deleteUnused(manager);

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
