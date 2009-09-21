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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.TopDocs;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.search.IndexingRules.Rule;

/**
 * A simple {@link IndexingStrategy} implementation that relies upon very few fields to be stored in the indexes.
 */
@ThreadSafe
class StoreLittleIndexingStrategy implements IndexingStrategy {

    static class PathIndex {
        public static final String PATH = "path";
        public static final String UUID = "uuid";
    }

    static class ContentIndex {
        public static final String UUID = PathIndex.UUID;
        public static final String FULL_TEXT = "fts";
    }

    public static final int SIZE_OF_DELETE_BATCHES = 100;

    private ThreadLocal<DateFormat> dateFormatter = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
        }
    };

    private static final FieldSelector UUID_FIELD_SELECTOR = new FieldSelector() {
        private static final long serialVersionUID = 1L;

        public FieldSelectorResult accept( String fieldName ) {
            return PathIndex.UUID.equals(fieldName) ? FieldSelectorResult.LOAD_AND_BREAK : FieldSelectorResult.NO_LOAD;
        }
    };

    /**
     * The default set of {@link IndexingRules} used by {@link StoreLittleIndexingStrategy} instances when no rules are provided.
     */
    public static final IndexingRules DEFAULT_RULES;

    static {
        IndexingRules.Builder builder = IndexingRules.createBuilder();
        // Configure the default behavior ...
        builder.defaultTo(IndexingRules.INDEX | IndexingRules.ANALYZE);
        // Configure the UUID properties to be just indexed (not stored, not analyzed, not included in full-text) ...
        builder.index(JcrLexicon.UUID, DnaLexicon.UUID);
        // Configure the properties that we'll treat as dates ...
        builder.treatAsDates(JcrLexicon.CREATED, JcrLexicon.LAST_MODIFIED);
        DEFAULT_RULES = builder.build();
    }

    private final IndexingRules rules;
    private final Logger logger;
    private final LuceneQueryEngine queryEngine;

    /**
     * Create a new indexing strategy instance that does not support queries.
     */
    public StoreLittleIndexingStrategy() {
        this(null, null);
    }

    /**
     * Create a new indexing strategy instance.
     * 
     * @param schemata the schemata that defines the structure that can be queried; may be null if queries are not going to be
     *        used
     * @param rules the indexing rules that govern how properties are to be index, or null if the {@link #DEFAULT_RULES default
     *        rules} are to be used
     */
    public StoreLittleIndexingStrategy( Schemata schemata,
                                        IndexingRules rules ) {
        this.rules = rules != null ? rules : DEFAULT_RULES;
        this.logger = Logger.getLogger(getClass());
        this.queryEngine = new LuceneQueryEngine(schemata);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexingStrategy#getNamespaceEncoder()
     */
    public TextEncoder getNamespaceEncoder() {
        return new NoOpEncoder();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexingStrategy#getChangeCountForAutomaticOptimization()
     */
    public int getChangeCountForAutomaticOptimization() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexingStrategy#createAnalyzer()
     */
    public Analyzer createAnalyzer() {
        return new StandardAnalyzer();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because this strategy uses multiple indexes, and since there's no correlation between the documents in those indexes, we
     * need to perform the delete in multiple steps. First, we need to perform a query to find out which nodes exist below a
     * certain path. Then, we need to delete those nodes from the paths index. Finally, we need to delete the corresponding
     * documents in the content index that represent those same nodes.
     * </p>
     * <p>
     * Since we don't know how many documents there will be, we perform these steps in batches, where each batch limits the number
     * of results to a maximum number. We repeat batches as long as we find more results. This approach has the advantage that
     * we'll never bring in a large number of results, and it allows us to delete the documents from the content node using a
     * query.
     * </p>
     * 
     * @see org.jboss.dna.search.IndexingStrategy#deleteBelow(Path, IndexContext)
     */
    public int deleteBelow( Path path,
                            IndexContext indexes ) throws IOException {
        // Perform a query using the reader to find those nodes at/below the path ...
        try {
            IndexReader pathReader = indexes.getPathsReader();
            IndexSearcher pathSearcher = new IndexSearcher(pathReader);
            String pathStr = indexes.stringFactory().create(path) + "/";
            PrefixQuery query = new PrefixQuery(new Term(PathIndex.PATH, pathStr));
            int numberDeleted = 0;
            while (true) {
                // Execute the query and get the results ...
                TopDocs results = pathSearcher.search(query, SIZE_OF_DELETE_BATCHES);
                int numResultsInBatch = results.scoreDocs.length;
                // Walk the results, delete the doc, and add to the query that we'll use against the content index ...
                IndexReader contentReader = indexes.getContentReader();
                for (ScoreDoc result : results.scoreDocs) {
                    int docId = result.doc;
                    // Find the UUID of the node ...
                    Document doc = pathReader.document(docId, UUID_FIELD_SELECTOR);
                    String uuid = doc.get(PathIndex.UUID);
                    // Delete the document from the paths index ...
                    pathReader.deleteDocument(docId);
                    // Delete the corresponding document from the content index ...
                    contentReader.deleteDocuments(new Term(ContentIndex.UUID, uuid));
                }
                numberDeleted += numResultsInBatch;
                if (numResultsInBatch < SIZE_OF_DELETE_BATCHES) break;
            }
            indexes.commit();
            return numberDeleted;
        } catch (FileNotFoundException e) {
            // There are no index files yet, so nothing to delete ...
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexingStrategy#index(Node, IndexContext)
     */
    public void index( Node node,
                       IndexContext indexes ) throws IOException {
        ValueFactory<String> strings = indexes.stringFactory();
        Location location = node.getLocation();
        UUID uuid = location.getUuid();
        if (uuid == null) uuid = UUID.randomUUID();
        Path path = location.getPath();
        String pathStr = path.isRoot() ? "/" : strings.create(location.getPath()) + "/";
        String uuidStr = uuid.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("indexing {0}", pathStr);
        }

        // Create a separate document for the path, which makes it easier to handle moves since the path can
        // be changed without changing any other content fields ...
        Document doc = new Document();
        doc.add(new Field(PathIndex.PATH, pathStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(PathIndex.UUID, uuidStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
        indexes.getPathsWriter().addDocument(doc);

        // Create the document for the content (properties) ...
        doc = new Document();
        doc.add(new Field(ContentIndex.UUID, uuidStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
        String stringValue = null;
        StringBuilder fullTextSearchValue = null;
        for (Property property : node.getProperties()) {
            Name name = property.getName();
            Rule rule = rules.getRule(name);
            if (rule.isSkipped()) continue;
            String nameString = strings.create(name);
            if (rule.isDate()) {
                DateTimeFactory dateFactory = indexes.dateFactory();
                for (Object value : property) {
                    if (value == null) continue;
                    DateTime dateValue = dateFactory.create(value);
                    stringValue = dateFormatter.get().format(dateValue.toDate());
                    // Add a separate field for each property value ...
                    doc.add(new Field(nameString, stringValue, rule.getStoreOption(), rule.getIndexOption()));
                    // Dates are not added to the full-text search field (since this wouldn't make sense)
                }
                continue;
            }
            for (Object value : property) {
                if (value == null) continue;
                if (value instanceof Binary) {
                    // don't include binary values as individual fields but do include them in the full-text search ...
                    // TODO : add to full-text search ...
                    continue;
                }
                stringValue = strings.create(value);
                // Add a separate field for each property value ...
                doc.add(new Field(nameString, stringValue, rule.getStoreOption(), rule.getIndexOption()));
                // And add to the full-text field ...
                if (rule.isFullText()) {
                    if (fullTextSearchValue == null) {
                        fullTextSearchValue = new StringBuilder();
                    } else {
                        fullTextSearchValue.append(' ');
                    }
                    fullTextSearchValue.append(stringValue);
                }
            }
        }
        // Add the full-text-search field ...
        if (fullTextSearchValue != null) {
            doc.add(new Field(ContentIndex.FULL_TEXT, fullTextSearchValue.toString(), Field.Store.NO, Field.Index.ANALYZED));
        }
        indexes.getContentWriter().addDocument(doc);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexingStrategy#performQuery(String, int, int, IndexContext, List)
     */
    public void performQuery( String fullTextString,
                              int maxResults,
                              int offset,
                              IndexContext indexes,
                              List<Location> results ) throws IOException, ParseException {
        assert fullTextString != null;
        assert fullTextString.length() > 0;
        assert offset >= 0;
        assert maxResults > 0;
        assert indexes != null;
        assert results != null;

        // Parse the full-text search and search against the 'fts' field ...
        QueryParser parser = new QueryParser(ContentIndex.FULL_TEXT, createAnalyzer());
        Query query = parser.parse(fullTextString);
        TopDocCollector collector = new TopDocCollector(maxResults + offset);
        indexes.getContentSearcher().search(query, collector);

        // Collect the results ...
        TopDocs docs = collector.topDocs();
        IndexReader contentReader = indexes.getContentReader();
        IndexReader pathReader = indexes.getPathsReader();
        IndexSearcher pathSearcher = indexes.getPathsSearcher();
        ScoreDoc[] scoreDocs = docs.scoreDocs;
        int numberOfResults = scoreDocs.length;
        if (numberOfResults > offset) {
            // There are enough results to satisfy the offset ...
            for (int i = offset, num = scoreDocs.length; i != num; ++i) {
                ScoreDoc result = scoreDocs[i];
                int docId = result.doc;
                // Find the UUID of the node (this UUID might be artificial, so we have to find the path) ...
                Document doc = contentReader.document(docId, UUID_FIELD_SELECTOR);
                String uuid = doc.get(ContentIndex.UUID);
                // Find the path for this node (is there a better way to do this than one search per UUID?) ...
                TopDocs pathDocs = pathSearcher.search(new TermQuery(new Term(PathIndex.UUID, uuid)), 1);
                if (pathDocs.scoreDocs.length < 1) {
                    // No path record found ...
                    continue;
                }
                Document pathDoc = pathReader.document(pathDocs.scoreDocs[0].doc);
                Path path = indexes.pathFactory().create(pathDoc.get(PathIndex.PATH));
                // Now add the location ...
                results.add(Location.create(path, UUID.fromString(uuid)));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexingStrategy#performQuery(QueryCommand, IndexContext)
     */
    public QueryResults performQuery( QueryCommand query,
                                      IndexContext indexes ) throws IOException, ParseException {
        return this.queryEngine.execute(query, indexes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexingStrategy#apply(Iterable, IndexContext)
     */
    public int apply( Iterable<ChangeRequest> changes,
                      IndexContext indexes ) /*throws IOException*/{
        for (ChangeRequest change : changes) {
            if (change != null) continue;
        }
        return 0;
    }
}
