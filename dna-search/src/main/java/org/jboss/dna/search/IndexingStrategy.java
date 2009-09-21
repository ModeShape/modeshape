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
package org.jboss.dna.search;

import java.io.IOException;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.request.ChangeRequest;

/**
 * Interface defining the behaviors associated with indexing graph content.
 */
interface IndexingStrategy {

    int getChangeCountForAutomaticOptimization();

    TextEncoder getNamespaceEncoder();

    /**
     * Index the node given the index writers. Note that implementors should simply just use the writers to add documents to the
     * index(es), and should never call any of the writer lifecycle methods (e.g., {@link IndexWriter#commit()},
     * {@link IndexWriter#rollback()}, etc.).
     * 
     * @param node the node to be indexed; never null
     * @param indexes the set of index readers and writers; never null
     * @throws IOException if there is a problem indexing or using the writers
     */
    void index( Node node,
                IndexContext indexes ) throws IOException;

    /**
     * Update the indexes to reflect the supplied changes to the graph content. Note that implementors should simply just use the
     * writers to add documents to the index(es), and should never call any of the writer lifecycle methods (e.g.,
     * {@link IndexWriter#commit()}, {@link IndexWriter#rollback()}, etc.).
     * 
     * @param changes the set of changes to the content
     * @param indexes the set of index readers and writers; never null
     * @return the (approximate) number of nodes that were affected by the changes
     * @throws IOException if there is a problem indexing or using the writers
     */
    int apply( Iterable<ChangeRequest> changes,
               IndexContext indexes ) throws IOException;

    /**
     * Remove from the index(es) all of the information pertaining to the nodes at or below the supplied path. Note that
     * implementors should simply just use the writers to add documents to the index(es), and should never call any of the writer
     * lifecycle methods (e.g., {@link IndexWriter#commit()}, {@link IndexWriter#rollback()}, etc.).
     * 
     * @param path the path identifying the graph content that is to be removed; never null
     * @param indexes the set of index readers and writers; never null
     * @return the (approximate) number of nodes that were affected by the changes
     * @throws IOException if there is a problem indexing or using the writers
     */
    int deleteBelow( Path path,
                     IndexContext indexes ) throws IOException;

    /**
     * Create the analyzer that is used for reading and updating the indexes.
     * 
     * @return the analyzer; may not be null
     */
    Analyzer createAnalyzer();

    /**
     * Perform a full-text search given the supplied query.
     * 
     * @param fullTextString the full-text query; never null or blank
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @param indexes the set of index readers and writers; never null
     * @param results the list where the results should be accumulated; never null
     * @throws IOException if there is a problem indexing or using the writers
     * @throws ParseException if there is a problem parsing the query
     */
    void performQuery( String fullTextString,
                       int maxResults,
                       int offset,
                       IndexContext indexes,
                       List<Location> results ) throws IOException, ParseException;

    /**
     * Perform a query of the content.
     * 
     * @param query the query; never null
     * @param indexes the set of index readers and writers; never null
     * @return the results of the query
     * @throws IOException if there is a problem indexing or using the writers
     * @throws ParseException if there is a problem parsing the query
     */
    QueryResults performQuery( QueryCommand query,
                               IndexContext indexes ) throws IOException, ParseException;
}
