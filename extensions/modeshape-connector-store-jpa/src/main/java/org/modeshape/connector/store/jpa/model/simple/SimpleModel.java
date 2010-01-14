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

import org.hibernate.ejb.Ejb3Configuration;
import org.modeshape.connector.store.jpa.JpaConnectorI18n;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.connector.store.jpa.Model;
import org.modeshape.connector.store.jpa.model.common.NamespaceEntity;
import org.modeshape.connector.store.jpa.model.common.WorkspaceEntity;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadBranchRequest;

/**
 * Database model that stores each node (transparently) and its properties (opaquely) in a single row. Large property values are
 * stored separately and can be shared between nodes.
 * <p>
 * The set of tables used in this model includes:
 * <ul>
 * <li>Namespaces - the set of namespace URIs used in paths, property names, and property values.</li>
 * <li>Nodes - each node along with its name, seralized properties, parent, UUID, and position within its parent. This approach
 * makes it possible to efficiently work with nodes containing large numbers of children, where adding and removing child nodes is
 * largely independent of the number of children. Also, working with properties is also completely independent of the number of
 * child nodes.</li>
 * <li>Large values - property values larger than a certain size will be broken out into this table, where they are tracked by
 * their SHA-1 hash and shared by all properties that have that same value. The values are stored in a binary (and optionally
 * compressed) form.</li>
 * <li>ReferenceChanges - the references from one node to another</li>
 * <li>Subgraph - a working area for efficiently computing the space of a subgraph; see below</li>
 * <li>Change log - a record of the changes that have been made to the repository. This is used to distribute change events across
 * multiple distributed processes, and to allow a recently-connected client to identify the set of changes that have been made
 * since a particular time or date. Changes are serialized into a binary, compressed format.</i></li>
 * <li>Options - the parameters for this store's configuration (common to all models)</li>
 * </ul>
 * </p>
 * <h3>Subgraph queries</h3>
 * <p>
 * This database model contains two tables that are used in an efficient mechanism to find all of the nodes in the subgraph below
 * a certain node. This process starts by creating a record for the subgraph query, and then proceeds by executing a join to find
 * all the children of the top-level node, and inserting them into the database (in a working area associated with the subgraph
 * query). Then, another join finds all the children of those children and inserts them into the same working area. This continues
 * until the maximum depth has been reached, or until there are no more children (whichever comes first). All of the nodes in the
 * subgraph are then represented by records in the working area, and can be used to quickly and efficient work with the subgraph
 * nodes. When finished, the mechanism deletes the records in the working area associated with the subgraph query.
 * </p>
 * <p>
 * This subgraph query mechanism is extremely efficient, performing one join/insert statement <i>per level of the subgraph</i>,
 * and is completely independent of the number of nodes in the subgraph. For example, consider a subgraph of node A, where A has
 * 10 children, and each child contains 10 children, and each grandchild contains 10 children. This subgraph has a total of 1111
 * nodes (1 root + 10 children + 10*10 grandchildren + 10*10*10 great-grandchildren). Finding the nodes in this subgraph would
 * normally require 1 query per node (in other words, 1111 queries). But with this subgraph query mechanism, all of the nodes in
 * the subgraph can be found with 1 insert plus 4 additional join/inserts.
 * </p>
 * <p>
 * This mechanism has the added benefit that the set of nodes in the subgraph are kept in a working area in the database, meaning
 * they don't have to be pulled into memory.
 * </p>
 * <p>
 * Subgraph queries are used to efficiently process a number of different requests, including {@link ReadBranchRequest},
 * {@link DeleteBranchRequest}, {@link MoveBranchRequest}, and {@link CopyBranchRequest}. Processing each of these kinds of
 * requests requires knowledge of the subgraph, and in fact all but the <code>ReadBranchRequest</code> need to know the complete
 * subgraph.
 * </p>
 */
public class SimpleModel extends Model {

    public SimpleModel() {
        super("Simple", JpaConnectorI18n.simpleModelDescription);
    }

    /**
     * Configure the entity class that will be used by JPA to store information in the database.
     * 
     * @param configurator the Hibernate {@link Ejb3Configuration} component; never null
     */
    @Override
    public void configure( Ejb3Configuration configurator ) {
        // Add the annotated classes ...
        configurator.addAnnotatedClass(WorkspaceEntity.class);
        configurator.addAnnotatedClass(NamespaceEntity.class);
        configurator.addAnnotatedClass(LargeValueEntity.class);
        configurator.addAnnotatedClass(NodeEntity.class);
        configurator.addAnnotatedClass(SubgraphNodeEntity.class);
        configurator.addAnnotatedClass(SubgraphQueryEntity.class);
    }

    @Override
    public RepositoryConnection createConnection( JpaSource source ) {
        return new SimpleJpaConnection(source);
    }
}
