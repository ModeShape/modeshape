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
package org.jboss.dna.graph.search;

import java.io.IOException;
import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.request.ChangeRequest;

/**
 * The representation of a single layout of one or more Lucene indexes.
 */
@ThreadSafe
public interface SearchProvider {

    /**
     * Create a new session to the indexes.
     * 
     * @param context the execution context for which this session is to be established; may not be null
     * @param sourceName the name of the source; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param overwrite true if the existing indexes should be overwritten, or false if they should be used
     * @param readOnly true if the resulting session can be optimized for use in read-only situations, or false if the session
     *        needs to allow calling the write methods
     * @return the session to the indexes; never null
     */
    Session createSession( ExecutionContext context,
                           String sourceName,
                           String workspaceName,
                           boolean overwrite,
                           boolean readOnly );

    /**
     * Destroy the indexes for the workspace with the supplied name.
     * 
     * @param context the execution context in which the destruction should be performed; may not be null
     * @param sourceName the name of the source; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @return true if the indexes for the workspace were destroyed, or false if there was no such workspace index
     * @throws IOException if there is a problem destroying the indexes
     */
    boolean destroyIndexes( ExecutionContext context,
                            String sourceName,
                            String workspaceName ) throws IOException;

    /**
     * A stateful session that is used to interact with the search provider to search a particular source and workspace.
     */
    public interface Session {

        /**
         * Get the name of the {@link RepositorySource repository source} for which this session exists. A session instance will
         * always return the same name.
         * 
         * @return the source name; never null
         */
        String getSourceName();

        /**
         * Get the name of the workspace for which this session exists. A session instance will always return the same name.
         * 
         * @return the workspace name; never null
         */
        String getWorkspaceName();

        /**
         * Get the execution context in which this session is operating.
         * 
         * @return the execution context; never null
         */
        ExecutionContext getContext();

        /**
         * Return whether this session made changes to the indexed state.
         * 
         * @return true if change were made, or false otherwise
         */
        boolean hasChanges();

        /**
         * Perform a full-text search given the supplied query.
         * 
         * @param context the context in which the search should be executed; may not be null
         * @param fullTextString the full-text query; never null or blank
         * @param maxResults the maximum number of results that are to be returned; always positive
         * @param offset the number of initial results to skip, or 0 if the first results are to be returned
         * @param results the list where the results should be accumulated; never null
         */
        void search( ExecutionContext context,
                     String fullTextString,
                     int maxResults,
                     int offset,
                     List<Location> results );

        /**
         * Perform a query of the content. The {@link QueryCommand query} is supplied in the form of the Abstract Query Model,
         * with the {@link Schemata} that defines the tables and views that are available to the query, and the set of index
         * readers (and writers) that should be used.
         * 
         * @param queryContext the context in which the query should be executed; may not be null
         * @param query the query; never null
         * @return the results of the query; never null
         */
        QueryResults query( QueryContext queryContext,
                            QueryCommand query );

        /**
         * Index the node. Changes are recorded only when {@link #commit()} is called.
         * 
         * @param node the node to be indexed; never null
         */
        void index( Node node );

        /**
         * Update the indexes to reflect the supplied changes to the graph content.
         * 
         * @param changes the set of changes to the content
         * @return the (approximate) number of nodes that were affected by the changes
         * @throws SearchEngineException if there is a problem executing the query
         */
        int apply( Iterable<ChangeRequest> changes );

        /**
         * Remove from the index(es) all of the information pertaining to the nodes at or below the supplied path.
         * 
         * @param path the path identifying the graph content that is to be removed; never null
         * @return the (approximate) number of nodes that were affected by the changes
         */
        int deleteBelow( Path path );

        /**
         * Optimize the indexes, if required.
         */
        void optimize();

        /**
         * Close this session by committing all of the changes. This session is no longer usable after this method is called.
         */
        void commit();

        /**
         * Close this session by rolling back all of the changes that have been made. This session is no longer usable after this
         * method is called.
         */
        void rollback();
    }

}
