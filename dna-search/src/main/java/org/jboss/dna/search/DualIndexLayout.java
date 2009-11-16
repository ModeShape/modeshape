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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.regex.JavaUtilRegexCapabilities;
import org.apache.lucene.search.regex.RegexQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryEngine;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.NodeDepth;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.NodePath;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.optimize.Optimizer;
import org.jboss.dna.graph.query.optimize.OptimizerRule;
import org.jboss.dna.graph.query.optimize.RuleBasedOptimizer;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.Planner;
import org.jboss.dna.graph.query.process.ProcessingComponent;
import org.jboss.dna.graph.query.process.QueryProcessor;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.search.IndexRules.Rule;
import org.jboss.dna.search.query.CompareNameQuery;
import org.jboss.dna.search.query.ComparePathQuery;
import org.jboss.dna.search.query.CompareStringQuery;
import org.jboss.dna.search.query.MatchNoneQuery;
import org.jboss.dna.search.query.NotQuery;
import org.jboss.dna.search.query.UuidsQuery;

/**
 * A simple {@link IndexLayout} implementation that relies upon two separate indexes: one for the node content and a second one
 * for paths and UUIDs.
 */
@ThreadSafe
public abstract class DualIndexLayout implements IndexLayout {

    protected static final long MIN_DATE = 0;
    protected static final long MAX_DATE = Long.MAX_VALUE;
    protected static final long MIN_LONG = Long.MIN_VALUE;
    protected static final long MAX_LONG = Long.MAX_VALUE;
    protected static final double MIN_DOUBLE = Double.MIN_VALUE;
    protected static final double MAX_DOUBLE = Double.MAX_VALUE;
    protected static final int MIN_DEPTH = 0;
    protected static final int MAX_DEPTH = 100;

    protected static final String PATHS_INDEX_NAME = "paths";
    protected static final String CONTENT_INDEX_NAME = "content";

    protected static final String UUID_FIELD = "uuid";
    protected static final String FULL_TEXT_SUFFIX = "/fs"; // the slash character is not allowed in a property name unescaped

    static class PathIndex {
        public static final String PATH = "path";
        public static final String LOCAL_NAME = "name";
        public static final String SNS_INDEX = "sns";
        public static final String UUID = UUID_FIELD;
        public static final String DEPTH = "depth";
    }

    static class ContentIndex {
        public static final String UUID = UUID_FIELD;
        public static final String FULL_TEXT = "fts";
    }

    /**
     * The number of results that should be returned when performing queries while deleting entire branches of content. The
     * current value is {@value} .
     */
    protected static final int SIZE_OF_DELETE_BATCHES = 1000;

    private ThreadLocal<DateFormat> dateFormatter = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
        }
    };

    /**
     * Obtain an immutable {@link FieldSelector} instance that accesses the UUID field.
     */
    protected static final FieldSelector UUID_FIELD_SELECTOR = new FieldSelector() {
        private static final long serialVersionUID = 1L;

        public FieldSelectorResult accept( String fieldName ) {
            return PathIndex.UUID.equals(fieldName) ? FieldSelectorResult.LOAD_AND_BREAK : FieldSelectorResult.NO_LOAD;
        }
    };

    /**
     * Get the date formatter that can be reused safely within the current thread.
     * 
     * @return the date formatter; never null
     */
    protected DateFormat dateFormatter() {
        return dateFormatter.get();
    }

    /**
     * Get the text encoder that should be used to encode namespaces in the search index.
     * 
     * @return the namespace text encoder; never null
     */
    protected TextEncoder getNamespaceEncoder() {
        return new NoOpEncoder();
    }

    /**
     * Create a Lucene {@link Analyzer} analyzer that should be used for indexing and searching.
     * 
     * @return the analyzer; never null
     */
    protected Analyzer createAnalyzer() {
        return new StandardAnalyzer(Version.LUCENE_CURRENT);
    }

    protected abstract class LuceneSession implements IndexSession {
        protected final ExecutionContext context;
        protected final String sourceName;
        protected final String workspaceName;
        protected final IndexRules rules;
        private final QueryEngine queryEngine;
        private final Analyzer analyzer;
        private final Directory pathsIndexDirectory;
        private final Directory contentIndexDirectory;
        protected final boolean overwrite;
        protected final boolean readOnly;
        protected final ValueFactory<String> stringFactory;
        protected final DateTimeFactory dateFactory;
        protected final PathFactory pathFactory;
        private int changeCount;
        private IndexReader pathsReader;
        private IndexWriter pathsWriter;
        private IndexSearcher pathsSearcher;
        private IndexReader contentReader;
        private IndexWriter contentWriter;
        private IndexSearcher contentSearcher;

        protected LuceneSession( ExecutionContext context,
                                 String sourceName,
                                 String workspaceName,
                                 IndexRules rules,
                                 Directory pathsIndexDirectory,
                                 Directory contentIndexDirectory,
                                 boolean overwrite,
                                 boolean readOnly ) {
            this.context = context;
            this.sourceName = sourceName;
            this.workspaceName = workspaceName;
            this.rules = rules;
            this.overwrite = overwrite;
            this.readOnly = readOnly;
            this.pathsIndexDirectory = pathsIndexDirectory;
            this.contentIndexDirectory = contentIndexDirectory;
            this.analyzer = createAnalyzer();
            this.stringFactory = context.getValueFactories().getStringFactory();
            this.dateFactory = context.getValueFactories().getDateFactory();
            this.pathFactory = context.getValueFactories().getPathFactory();
            assert this.context != null;
            assert this.sourceName != null;
            assert this.workspaceName != null;
            assert this.rules != null;
            assert this.analyzer != null;
            assert this.pathsIndexDirectory != null;
            assert this.contentIndexDirectory != null;
            assert this.stringFactory != null;
            assert this.dateFactory != null;
            // do this last ...
            this.queryEngine = createQueryProcessor();
            assert this.queryEngine != null;
        }

        /**
         * Create the field name that will be used to store the full-text searchable property values.
         * 
         * @param propertyName the name of the property; may not be null
         * @return the field name for the full-text searchable property values; never null
         */
        protected String fullTextFieldName( String propertyName ) {
            return propertyName + FULL_TEXT_SUFFIX;
        }

        protected IndexReader getPathsReader() throws IOException {
            if (pathsReader == null) {
                pathsReader = IndexReader.open(pathsIndexDirectory, readOnly);
            }
            return pathsReader;
        }

        protected IndexReader getContentReader() throws IOException {
            if (contentReader == null) {
                contentReader = IndexReader.open(contentIndexDirectory, readOnly);
            }
            return contentReader;
        }

        protected IndexWriter getPathsWriter() throws IOException {
            assert !readOnly;
            if (pathsWriter == null) {
                pathsWriter = new IndexWriter(pathsIndexDirectory, analyzer, overwrite, MaxFieldLength.UNLIMITED);
            }
            return pathsWriter;
        }

        protected IndexWriter getContentWriter() throws IOException {
            assert !readOnly;
            if (contentWriter == null) {
                contentWriter = new IndexWriter(contentIndexDirectory, analyzer, overwrite, MaxFieldLength.UNLIMITED);
            }
            return contentWriter;
        }

        protected IndexSearcher getPathsSearcher() throws IOException {
            if (pathsSearcher == null) {
                pathsSearcher = new IndexSearcher(getPathsReader());
            }
            return pathsSearcher;
        }

        protected IndexSearcher getContentSearcher() throws IOException {
            if (contentSearcher == null) {
                contentSearcher = new IndexSearcher(getContentReader());
            }
            return contentSearcher;
        }

        protected boolean hasWriters() {
            return pathsWriter != null || contentWriter != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#getContext()
         */
        public final ExecutionContext getContext() {
            return context;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#getSourceName()
         */
        public final String getSourceName() {
            return sourceName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#getWorkspaceName()
         */
        public String getWorkspaceName() {
            return workspaceName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#hasChanges()
         */
        public boolean hasChanges() {
            return changeCount > 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#index(org.jboss.dna.graph.Node)
         */
        public void index( Node node ) throws IOException {
            assert !readOnly;
            Location location = node.getLocation();
            UUID uuid = location.getUuid();
            if (uuid == null) uuid = UUID.randomUUID();
            Path path = location.getPath();
            String uuidStr = stringFactory.create(uuid);
            String pathStr = pathAsString(path, stringFactory);
            String nameStr = path.isRoot() ? "" : stringFactory.create(path.getLastSegment().getName());
            int sns = path.isRoot() ? 1 : path.getLastSegment().getIndex();

            Logger logger = Logger.getLogger(getClass());
            if (logger.isTraceEnabled()) {
                logger.trace("indexing {0}", pathStr);
            }

            // Create a separate document for the path, which makes it easier to handle moves since the path can
            // be changed without changing any other content fields ...
            Document doc = new Document();
            doc.add(new Field(PathIndex.PATH, pathStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(PathIndex.LOCAL_NAME, nameStr, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new NumericField(PathIndex.LOCAL_NAME, Field.Store.YES, true).setIntValue(sns));
            doc.add(new Field(PathIndex.UUID, uuidStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new NumericField(PathIndex.DEPTH, Field.Store.YES, true).setIntValue(path.size()));
            getPathsWriter().addDocument(doc);

            // Create the document for the content (properties) ...
            doc = new Document();
            doc.add(new Field(ContentIndex.UUID, uuidStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
            String stringValue = null;
            StringBuilder fullTextSearchValue = null;
            for (Property property : node.getProperties()) {
                Name name = property.getName();
                Rule rule = rules.getRule(name);
                if (rule.isSkipped()) continue;
                String nameString = stringFactory.create(name);
                if (rule.isDate()) {
                    for (Object value : property) {
                        if (value == null) continue;
                        DateTime dateValue = dateFactory.create(value);
                        // Add a separate field for each property value ...
                        doc.add(new NumericField(nameString, rule.getStoreOption(), true).setLongValue(dateValue.getMillisecondsInUtc()));
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
                    stringValue = stringFactory.create(value);
                    // Add a separate field for each property value ...
                    doc.add(new Field(nameString, stringValue, rule.getStoreOption(), rule.getIndexOption()));

                    if (rule.isFullText()) {
                        // Add this text to the full-text field ...
                        if (fullTextSearchValue == null) {
                            fullTextSearchValue = new StringBuilder();
                        } else {
                            fullTextSearchValue.append(' ');
                        }
                        fullTextSearchValue.append(stringValue);

                        // Also create a full-text-searchable field ...
                        String fullTextNameString = fullTextFieldName(nameString);
                        doc.add(new Field(fullTextNameString, stringValue, Store.NO, Index.ANALYZED));
                    }
                }
            }
            // Add the full-text-search field ...
            if (fullTextSearchValue != null) {
                doc.add(new Field(ContentIndex.FULL_TEXT, fullTextSearchValue.toString(), Field.Store.NO, Field.Index.ANALYZED));
            }
            getContentWriter().addDocument(doc);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#optimize()
         */
        public void optimize() throws IOException {
            getContentWriter().optimize();
            getPathsWriter().optimize();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#apply(java.lang.Iterable)
         */
        public int apply( Iterable<ChangeRequest> changes ) /*throws IOException*/{
            for (ChangeRequest change : changes) {
                if (change != null) continue;
            }
            return 0;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Because this strategy uses multiple indexes, and since there's no correlation between the documents in those indexes,
         * we need to perform the delete in multiple steps. First, we need to perform a query to find out which nodes exist below
         * a certain path. Then, we need to delete those nodes from the paths index. Finally, we need to delete the corresponding
         * documents in the content index that represent those same nodes.
         * </p>
         * <p>
         * Since we don't know how many documents there will be, we perform these steps in batches, where each batch limits the
         * number of results to a maximum number. We repeat batches as long as we find more results. This approach has the
         * advantage that we'll never bring in a large number of results, and it allows us to delete the documents from the
         * content node using a query.
         * </p>
         * 
         * @see org.jboss.dna.search.IndexSession#deleteBelow(org.jboss.dna.graph.property.Path)
         */
        public int deleteBelow( Path path ) throws IOException {
            assert !readOnly;
            // Perform a query using the reader to find those nodes at/below the path ...
            try {
                IndexReader pathReader = getPathsReader();
                IndexSearcher pathSearcher = new IndexSearcher(pathReader);
                String pathStr = stringFactory.create(path) + "/";
                PrefixQuery query = new PrefixQuery(new Term(PathIndex.PATH, pathStr));
                int numberDeleted = 0;
                while (true) {
                    // Execute the query and get the results ...
                    TopDocs results = pathSearcher.search(query, SIZE_OF_DELETE_BATCHES);
                    int numResultsInBatch = results.scoreDocs.length;
                    // Walk the results, delete the doc, and add to the query that we'll use against the content index ...
                    IndexReader contentReader = getContentReader();
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
                return numberDeleted;
            } catch (FileNotFoundException e) {
                // There are no index files yet, so nothing to delete ...
                return 0;
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#search(org.jboss.dna.graph.ExecutionContext, java.lang.String, int, int,
         *      java.util.List)
         */
        public void search( ExecutionContext context,
                            String fullTextString,
                            int maxResults,
                            int offset,
                            List<Location> results ) throws IOException, ParseException {
            assert fullTextString != null;
            assert fullTextString.length() > 0;
            assert offset >= 0;
            assert maxResults > 0;
            assert results != null;

            // Parse the full-text search and search against the 'fts' field ...
            QueryParser parser = new QueryParser(ContentIndex.FULL_TEXT, createAnalyzer());
            Query query = parser.parse(fullTextString);
            TopDocs docs = getContentSearcher().search(query, maxResults + offset);

            // Collect the results ...
            IndexReader contentReader = getContentReader();
            IndexReader pathReader = getPathsReader();
            IndexSearcher pathSearcher = getPathsSearcher();
            ScoreDoc[] scoreDocs = docs.scoreDocs;
            int numberOfResults = scoreDocs.length;
            if (numberOfResults > offset) {
                // There are enough results to satisfy the offset ...
                PathFactory pathFactory = context.getValueFactories().getPathFactory();
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
                    String pathString = pathDoc.get(PathIndex.PATH);
                    Path path = pathFactory.create(pathString);
                    // Now add the location ...
                    results.add(Location.create(path, UUID.fromString(uuid)));
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#query(org.jboss.dna.graph.query.QueryContext,
         *      org.jboss.dna.graph.query.model.QueryCommand)
         */
        public QueryResults query( QueryContext queryContext,
                                   QueryCommand query ) {
            return queryEngine.execute(queryContext, query);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#commit()
         */
        public void commit() throws IOException {
            IOException ioError = null;
            RuntimeException runtimeError = null;
            if (pathsReader != null) {
                try {
                    pathsReader.close();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    pathsReader = null;
                }
            }
            if (contentReader != null) {
                try {
                    contentReader.close();
                } catch (IOException e) {
                    if (ioError == null) ioError = e;
                } catch (RuntimeException e) {
                    if (runtimeError == null) runtimeError = e;
                } finally {
                    contentReader = null;
                }
            }
            if (pathsWriter != null) {
                try {
                    pathsWriter.commit();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    try {
                        pathsWriter.close();
                    } catch (IOException e) {
                        ioError = e;
                    } catch (RuntimeException e) {
                        runtimeError = e;
                    } finally {
                        pathsWriter = null;
                    }
                }
            }
            if (contentWriter != null) {
                try {
                    contentWriter.commit();
                } catch (IOException e) {
                    if (ioError == null) ioError = e;
                } catch (RuntimeException e) {
                    if (runtimeError == null) runtimeError = e;
                } finally {
                    try {
                        contentWriter.close();
                    } catch (IOException e) {
                        ioError = e;
                    } catch (RuntimeException e) {
                        runtimeError = e;
                    } finally {
                        contentWriter = null;
                    }
                }
            }
            if (ioError != null) throw ioError;
            if (runtimeError != null) throw runtimeError;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexSession#rollback()
         */
        public void rollback() throws IOException {
            IOException ioError = null;
            RuntimeException runtimeError = null;
            if (pathsReader != null) {
                try {
                    pathsReader.close();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    pathsReader = null;
                }
            }
            if (contentReader != null) {
                try {
                    contentReader.close();
                } catch (IOException e) {
                    if (ioError == null) ioError = e;
                } catch (RuntimeException e) {
                    if (runtimeError == null) runtimeError = e;
                } finally {
                    contentReader = null;
                }
            }
            if (pathsWriter != null) {
                try {
                    pathsWriter.rollback();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    try {
                        pathsWriter.close();
                    } catch (IOException e) {
                        ioError = e;
                    } catch (RuntimeException e) {
                        runtimeError = e;
                    } finally {
                        pathsWriter = null;
                    }
                }
            }
            if (contentWriter != null) {
                try {
                    contentWriter.rollback();
                } catch (IOException e) {
                    if (ioError == null) ioError = e;
                } catch (RuntimeException e) {
                    if (runtimeError == null) runtimeError = e;
                } finally {
                    try {
                        contentWriter.close();
                    } catch (IOException e) {
                        ioError = e;
                    } catch (RuntimeException e) {
                        runtimeError = e;
                    } finally {
                        contentWriter = null;
                    }
                }
            }
            if (ioError != null) throw ioError;
            if (runtimeError != null) throw runtimeError;
        }

        protected QueryEngine createQueryProcessor() {
            // Create the query engine ...
            Planner planner = new CanonicalPlanner();
            Optimizer optimizer = new RuleBasedOptimizer() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.query.optimize.RuleBasedOptimizer#populateRuleStack(java.util.LinkedList,
                 *      org.jboss.dna.graph.query.plan.PlanHints)
                 */
                @Override
                protected void populateRuleStack( LinkedList<OptimizerRule> ruleStack,
                                                  PlanHints hints ) {
                    super.populateRuleStack(ruleStack, hints);
                    // Add any custom rules here, either at the front of the stack or at the end
                }
            };
            QueryProcessor processor = new QueryProcessor() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.query.process.QueryProcessor#createAccessComponent(org.jboss.dna.graph.query.model.QueryCommand,
                 *      org.jboss.dna.graph.query.QueryContext, org.jboss.dna.graph.query.plan.PlanNode,
                 *      org.jboss.dna.graph.query.QueryResults.Columns,
                 *      org.jboss.dna.graph.query.process.SelectComponent.Analyzer)
                 */
                @Override
                protected ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                                     QueryContext context,
                                                                     PlanNode accessNode,
                                                                     Columns resultColumns,
                                                                     org.jboss.dna.graph.query.process.SelectComponent.Analyzer analyzer ) {
                    try {
                        return LuceneSession.this.createAccessComponent(originalQuery,
                                                                        context,
                                                                        accessNode,
                                                                        resultColumns,
                                                                        analyzer);
                    } catch (IOException e) {
                        I18n msg = SearchI18n.errorWhilePerformingQuery;
                        context.getProblems().addError(e,
                                                       msg,
                                                       Visitors.readable(originalQuery),
                                                       getWorkspaceName(),
                                                       getSourceName(),
                                                       e.getMessage());
                        return null;
                    }
                }
            };

            return new QueryEngine(planner, optimizer, processor);
        }

        protected abstract ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                                      QueryContext context,
                                                                      PlanNode accessNode,
                                                                      Columns resultColumns,
                                                                      org.jboss.dna.graph.query.process.SelectComponent.Analyzer analyzer )
            throws IOException;

        /**
         * Get the set of UUIDs for the children of the node at the given path.
         * 
         * @param parentPath the path to the parent node; may not be null
         * @return the UUIDs of the child nodes; never null but possibly empty
         * @throws IOException if there is an error accessing the indexes
         */
        protected Set<UUID> getUuidsForChildrenOf( Path parentPath ) throws IOException {
            // Find the path of the parent ...
            String stringifiedPath = pathAsString(parentPath, stringFactory);
            // Append a '/' to the parent path, so we'll only get decendants ...
            stringifiedPath = stringifiedPath + '/';

            // Create a query to find all the nodes below the parent path ...
            Query query = new PrefixQuery(new Term(PathIndex.PATH, stringifiedPath));
            // Include only the children ...
            int childrenDepth = parentPath.size() + 1;
            Query depthQuery = NumericRangeQuery.newIntRange(PathIndex.DEPTH, childrenDepth, childrenDepth, true, true);
            // And combine ...
            BooleanQuery combinedQuery = new BooleanQuery();
            combinedQuery.add(query, Occur.MUST);
            combinedQuery.add(depthQuery, Occur.MUST);
            query = combinedQuery;

            // Now execute and collect the UUIDs ...
            UuidCollector uuidCollector = new UuidCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, uuidCollector);
            return uuidCollector.getUuids();
        }

        /**
         * Get the set of UUIDs for the nodes that are descendants of the node at the given path.
         * 
         * @param parentPath the path to the parent node; may not be null and <i>may not be the root node</i>
         * @param includeParent true if the parent node should be included in the results, or false if only the descendants should
         *        be included
         * @return the UUIDs of the nodes; never null but possibly empty
         * @throws IOException if there is an error accessing the indexes
         */
        protected Set<UUID> getUuidsForDescendantsOf( Path parentPath,
                                                      boolean includeParent ) throws IOException {
            assert !parentPath.isRoot();

            // Find the path of the parent ...
            String stringifiedPath = pathAsString(parentPath, stringFactory);
            if (!includeParent) {
                // Append a '/' to the parent path, and we'll only get decendants ...
                stringifiedPath = stringifiedPath + '/';
            }

            // Create a prefix query ...
            Query query = new PrefixQuery(new Term(PathIndex.PATH, stringifiedPath));

            // Now execute and collect the UUIDs ...
            UuidCollector uuidCollector = new UuidCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, uuidCollector);
            return uuidCollector.getUuids();
        }

        /**
         * Get the set containing the single UUID for the node at the given path.
         * 
         * @param path the path to the node; may not be null
         * @return the UUID of the supplied node; or null if the node cannot be found
         * @throws IOException if there is an error accessing the indexes
         */
        protected UUID getUuidFor( Path path ) throws IOException {
            // Create a query to find all the nodes below the parent path ...
            IndexSearcher searcher = getPathsSearcher();
            String stringifiedPath = pathAsString(path, stringFactory);
            TermQuery query = new TermQuery(new Term(PathIndex.PATH, stringifiedPath));

            // Now execute and collect the UUIDs ...
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.totalHits == 0) return null;
            Document pathDoc = getPathsReader().document(topDocs.scoreDocs[0].doc);
            String uuidString = pathDoc.get(PathIndex.UUID);
            return UUID.fromString(uuidString);
        }

        /**
         * Utility method to create a query to find all of the documents representing nodes with the supplied UUIDs.
         * 
         * @param uuids the UUIDs of the nodes that are to be found; may not be null
         * @return the query; never null
         */
        protected Query findAllNodesWithUuids( Set<UUID> uuids ) {
            if (uuids.isEmpty()) {
                // There are no children, so return a null query ...
                return new MatchNoneQuery();
            }
            if (uuids.size() == 1) {
                UUID uuid = uuids.iterator().next();
                if (uuid == null) return new MatchNoneQuery();
                return new TermQuery(new Term(ContentIndex.UUID, uuid.toString()));
            }
            if (uuids.size() < 50) {
                // Create an OR boolean query for all the UUIDs, since this is probably more efficient ...
                BooleanQuery query = new BooleanQuery();
                for (UUID uuid : uuids) {
                    Query uuidQuery = new TermQuery(new Term(ContentIndex.UUID, uuid.toString()));
                    query.add(uuidQuery, Occur.SHOULD);
                }
                return query;
            }
            // Returna query that will always find all of the UUIDs ...
            return new UuidsQuery(ContentIndex.UUID, uuids, getContext().getValueFactories().getUuidFactory());
        }

        protected Query findAllNodesBelow( Path ancestorPath ) throws IOException {
            if (ancestorPath.isRoot()) {
                return new MatchAllDocsQuery();
            }
            Set<UUID> uuids = getUuidsForDescendantsOf(ancestorPath, false);
            return findAllNodesWithUuids(uuids);
        }

        /**
         * Return a query that can be used to find all of the documents that represent nodes that are children of the node at the
         * supplied path.
         * 
         * @param parentPath the path of the parent node.
         * @return the query; never null
         * @throws IOException if there is an error finding the UUIDs of the child nodes
         */
        protected Query findChildNodes( Path parentPath ) throws IOException {
            if (parentPath.isRoot()) {
                return new MatchAllDocsQuery();
            }
            Set<UUID> childUuids = getUuidsForChildrenOf(parentPath);
            return findAllNodesWithUuids(childUuids);
        }

        /**
         * Create a query that can be used to find the one document (or node) that exists at the exact path supplied. This method
         * first queries the {@link PathIndex path index} to find the UUID of the node at the supplied path, and then returns a
         * query that matches the UUID.
         * 
         * @param path the path of the node
         * @return the query; never null
         * @throws IOException if there is an error finding the UUID for the supplied path
         */
        protected Query findNodeAt( Path path ) throws IOException {
            UUID uuid = getUuidFor(path);
            if (uuid == null) return null;
            return new TermQuery(new Term(ContentIndex.UUID, uuid.toString()));
        }

        /**
         * Create a query that can be used to find documents (or nodes) that have a field value that satisfies the supplied LIKE
         * expression.
         * 
         * @param fieldName the name of the document field to search
         * @param likeExpression the JCR like expression
         * @return the query; never null
         */
        protected Query findNodesLike( String fieldName,
                                       String likeExpression ) {
            assert likeExpression != null;
            assert likeExpression.length() > 0;

            // '%' matches 0 or more characters
            // '_' matches any single character
            // '\x' matches 'x'
            // all other characters match themselves

            // Wildcard queries are a better match, but they can be slow and should not be used
            // if the first character of the expression is a '%' or '_' ...
            char firstChar = likeExpression.charAt(0);
            if (firstChar != '%' && firstChar != '_') {
                // Create a wildcard query ...
                String expression = toWildcardExpression(likeExpression);
                return new WildcardQuery(new Term(fieldName, expression));
            }
            // Create a regex query,
            String regex = toRegularExpression(likeExpression);
            RegexQuery query = new RegexQuery(new Term(fieldName, regex));
            query.setRegexImplementation(new JavaUtilRegexCapabilities());
            return query;
        }

        protected Query findNodesWith( PropertyValue propertyValue,
                                       Operator operator,
                                       Object value,
                                       boolean caseSensitive ) {
            String field = stringFactory.create(propertyValue.getPropertyName());
            PropertyType valueType = PropertyType.discoverType(value);
            ValueFactories factories = context.getValueFactories();
            switch (valueType) {
                case NAME:
                case PATH:
                case REFERENCE:
                case URI:
                case UUID:
                case STRING:
                    String stringValue = stringFactory.create(value);
                    if (valueType == PropertyType.PATH) {
                        stringValue = pathAsString(pathFactory.create(value), stringFactory);
                    }
                    if (!caseSensitive) stringValue = stringValue.toLowerCase();
                    switch (operator) {
                        case EQUAL_TO:
                            return new TermQuery(new Term(field, stringValue));
                        case NOT_EQUAL_TO:
                            Query query = new TermQuery(new Term(field, stringValue));
                            return new NotQuery(query);
                        case GREATER_THAN:
                            return CompareStringQuery.createQueryForNodesWithFieldGreaterThan(stringValue,
                                                                                              field,
                                                                                              factories,
                                                                                              caseSensitive);
                        case GREATER_THAN_OR_EQUAL_TO:
                            return CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(stringValue,
                                                                                                       field,
                                                                                                       factories,
                                                                                                       caseSensitive);
                        case LESS_THAN:
                            return CompareStringQuery.createQueryForNodesWithFieldLessThan(stringValue,
                                                                                           field,
                                                                                           factories,
                                                                                           caseSensitive);
                        case LESS_THAN_OR_EQUAL_TO:
                            return CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(stringValue,
                                                                                                    field,
                                                                                                    factories,
                                                                                                    caseSensitive);
                        case LIKE:
                            return findNodesLike(field, stringValue);
                    }
                    break;
                case DATE:
                    long date = factories.getLongFactory().create(value);
                    switch (operator) {
                        case EQUAL_TO:
                            return NumericRangeQuery.newLongRange(field, date, date, true, true);
                        case NOT_EQUAL_TO:
                            Query query = NumericRangeQuery.newLongRange(field, date, date, true, true);
                            return new NotQuery(query);
                        case GREATER_THAN:
                            return NumericRangeQuery.newLongRange(field, date, MAX_DATE, false, true);
                        case GREATER_THAN_OR_EQUAL_TO:
                            return NumericRangeQuery.newLongRange(field, date, MAX_DATE, true, true);
                        case LESS_THAN:
                            return NumericRangeQuery.newLongRange(field, MIN_DATE, date, true, false);
                        case LESS_THAN_OR_EQUAL_TO:
                            return NumericRangeQuery.newLongRange(field, MIN_DATE, date, true, true);
                        case LIKE:
                            // This is not allowed ...
                            assert false;
                            return null;
                    }
                    break;
                case LONG:
                    long longValue = factories.getLongFactory().create(value);
                    switch (operator) {
                        case EQUAL_TO:
                            return NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                        case NOT_EQUAL_TO:
                            Query query = NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                            return new NotQuery(query);
                        case GREATER_THAN:
                            return NumericRangeQuery.newLongRange(field, longValue, MAX_LONG, false, true);
                        case GREATER_THAN_OR_EQUAL_TO:
                            return NumericRangeQuery.newLongRange(field, longValue, MAX_LONG, true, true);
                        case LESS_THAN:
                            return NumericRangeQuery.newLongRange(field, MIN_LONG, longValue, true, false);
                        case LESS_THAN_OR_EQUAL_TO:
                            return NumericRangeQuery.newLongRange(field, MIN_LONG, longValue, true, true);
                        case LIKE:
                            // This is not allowed ...
                            assert false;
                            return null;
                    }
                    break;
                case DECIMAL:
                case DOUBLE:
                    double doubleValue = factories.getDoubleFactory().create(value);
                    switch (operator) {
                        case EQUAL_TO:
                            return NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                        case NOT_EQUAL_TO:
                            Query query = NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                            return new NotQuery(query);
                        case GREATER_THAN:
                            return NumericRangeQuery.newDoubleRange(field, doubleValue, MAX_DOUBLE, false, true);
                        case GREATER_THAN_OR_EQUAL_TO:
                            return NumericRangeQuery.newDoubleRange(field, doubleValue, MAX_DOUBLE, true, true);
                        case LESS_THAN:
                            return NumericRangeQuery.newDoubleRange(field, MIN_DOUBLE, doubleValue, true, false);
                        case LESS_THAN_OR_EQUAL_TO:
                            return NumericRangeQuery.newDoubleRange(field, MIN_DOUBLE, doubleValue, true, true);
                        case LIKE:
                            // This is not allowed ...
                            assert false;
                            return null;
                    }
                    break;
                case BOOLEAN:
                    boolean booleanValue = factories.getBooleanFactory().create(value);
                    stringValue = stringFactory.create(value);
                    switch (operator) {
                        case EQUAL_TO:
                            return new TermQuery(new Term(field, stringValue));
                        case NOT_EQUAL_TO:
                            return new TermQuery(new Term(field, stringFactory.create(!booleanValue)));
                        case GREATER_THAN:
                            if (!booleanValue) {
                                return new TermQuery(new Term(field, stringFactory.create(true)));
                            }
                            // Can't be greater than 'true', per JCR spec
                            return new MatchNoneQuery();
                        case GREATER_THAN_OR_EQUAL_TO:
                            return new TermQuery(new Term(field, stringFactory.create(true)));
                        case LESS_THAN:
                            if (booleanValue) {
                                return new TermQuery(new Term(field, stringFactory.create(false)));
                            }
                            // Can't be less than 'false', per JCR spec
                            return new MatchNoneQuery();
                        case LESS_THAN_OR_EQUAL_TO:
                            return new TermQuery(new Term(field, stringFactory.create(false)));
                        case LIKE:
                            // This is not allowed ...
                            assert false;
                            return null;
                    }
                    break;
                case OBJECT:
                case BINARY:
                    // This is not allowed ...
                    assert false;
                    return null;
            }
            return null;

        }

        protected Query findNodesWith( NodePath nodePath,
                                       Operator operator,
                                       Object value,
                                       boolean caseSensitive ) throws IOException {
            if (!caseSensitive) value = stringFactory.create(value).toLowerCase();
            Path pathValue = operator != Operator.LIKE ? pathFactory.create(value) : null;
            Query query = null;
            switch (operator) {
                case EQUAL_TO:
                    return findNodeAt(pathValue);
                case NOT_EQUAL_TO:
                    return new NotQuery(findNodeAt(pathValue));
                case LIKE:
                    String likeExpression = stringFactory.create(value);
                    return findNodesLike(PathIndex.PATH, likeExpression);
                case GREATER_THAN:
                    query = ComparePathQuery.createQueryForNodesWithPathGreaterThan(pathValue,
                                                                                    PathIndex.PATH,
                                                                                    context.getValueFactories(),
                                                                                    caseSensitive);
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    query = ComparePathQuery.createQueryForNodesWithPathGreaterThanOrEqualTo(pathValue,
                                                                                             PathIndex.PATH,
                                                                                             context.getValueFactories(),
                                                                                             caseSensitive);
                    break;
                case LESS_THAN:
                    query = ComparePathQuery.createQueryForNodesWithPathLessThan(pathValue,
                                                                                 PathIndex.PATH,
                                                                                 context.getValueFactories(),
                                                                                 caseSensitive);
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    query = ComparePathQuery.createQueryForNodesWithPathLessThanOrEqualTo(pathValue,
                                                                                          PathIndex.PATH,
                                                                                          context.getValueFactories(),
                                                                                          caseSensitive);
                    break;
            }
            // Now execute and collect the UUIDs ...
            UuidCollector uuidCollector = new UuidCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, uuidCollector);
            return findAllNodesWithUuids(uuidCollector.getUuids());
        }

        protected Query findNodesWith( NodeName nodeName,
                                       Operator operator,
                                       Object value,
                                       boolean caseSensitive ) throws IOException {
            String stringValue = stringFactory.create(value);
            if (!caseSensitive) stringValue = stringValue.toLowerCase();
            Path.Segment segment = operator != Operator.LIKE ? pathFactory.createSegment(stringValue) : null;
            int snsIndex = operator != Operator.LIKE ? segment.getIndex() : 0;
            Query query = null;
            switch (operator) {
                case EQUAL_TO:
                    BooleanQuery booleanQuery = new BooleanQuery();
                    booleanQuery.add(new TermQuery(new Term(PathIndex.LOCAL_NAME, stringValue)), Occur.MUST);
                    booleanQuery.add(NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, snsIndex, snsIndex, true, false),
                                     Occur.MUST);
                    return booleanQuery;
                case NOT_EQUAL_TO:
                    booleanQuery = new BooleanQuery();
                    booleanQuery.add(new TermQuery(new Term(PathIndex.LOCAL_NAME, stringValue)), Occur.MUST);
                    booleanQuery.add(NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, snsIndex, snsIndex, true, false),
                                     Occur.MUST);
                    return new NotQuery(booleanQuery);
                case GREATER_THAN:
                    query = CompareNameQuery.createQueryForNodesWithNameGreaterThan(segment,
                                                                                    PathIndex.LOCAL_NAME,
                                                                                    PathIndex.SNS_INDEX,
                                                                                    context.getValueFactories(),
                                                                                    caseSensitive);
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    query = CompareNameQuery.createQueryForNodesWithNameGreaterThanOrEqualTo(segment,
                                                                                             PathIndex.LOCAL_NAME,
                                                                                             PathIndex.SNS_INDEX,
                                                                                             context.getValueFactories(),
                                                                                             caseSensitive);
                    break;
                case LESS_THAN:
                    query = CompareNameQuery.createQueryForNodesWithNameLessThan(segment,
                                                                                 PathIndex.LOCAL_NAME,
                                                                                 PathIndex.SNS_INDEX,
                                                                                 context.getValueFactories(),
                                                                                 caseSensitive);
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    query = CompareNameQuery.createQueryForNodesWithNameLessThanOrEqualTo(segment,
                                                                                          PathIndex.LOCAL_NAME,
                                                                                          PathIndex.SNS_INDEX,
                                                                                          context.getValueFactories(),
                                                                                          caseSensitive);
                    break;
                case LIKE:
                    // See whether the like expression has brackets ...
                    String likeExpression = stringValue;
                    int openBracketIndex = likeExpression.indexOf('[');
                    if (openBracketIndex != -1) {
                        String localNameExpression = likeExpression.substring(0, openBracketIndex);
                        String snsIndexExpression = likeExpression.substring(openBracketIndex);
                        Query localNameQuery = createLocalNameQuery(localNameExpression);
                        Query snsQuery = createSnsIndexQuery(snsIndexExpression);
                        if (localNameQuery == null) {
                            if (snsQuery == null) {
                                query = new MatchNoneQuery();
                            } else {
                                // There is just an SNS part ...
                                query = snsQuery;
                            }
                        } else {
                            // There is a local name part ...
                            if (snsQuery == null) {
                                query = localNameQuery;
                            } else {
                                // There is both a local name part and a SNS part ...
                                booleanQuery = new BooleanQuery();
                                booleanQuery.add(localNameQuery, Occur.MUST);
                                booleanQuery.add(snsQuery, Occur.MUST);
                                query = booleanQuery;
                            }
                        }
                    } else {
                        // There is no SNS expression ...
                        query = createLocalNameQuery(likeExpression);
                    }
                    assert query != null;
                    break;
            }

            // Now execute and collect the UUIDs ...
            UuidCollector uuidCollector = new UuidCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, uuidCollector);
            return findAllNodesWithUuids(uuidCollector.getUuids());
        }

        protected Query findNodesWith( NodeLocalName nodeName,
                                       Operator operator,
                                       Object value,
                                       boolean caseSensitive ) throws IOException {
            String nameValue = stringFactory.create(value);
            Query query = null;
            switch (operator) {
                case LIKE:
                    String likeExpression = stringFactory.create(value);
                    return findNodesLike(PathIndex.LOCAL_NAME, likeExpression); // already is a query with UUIDs
                case EQUAL_TO:
                    query = new TermQuery(new Term(PathIndex.LOCAL_NAME, nameValue));
                    break;
                case NOT_EQUAL_TO:
                    query = new NotQuery(new TermQuery(new Term(PathIndex.LOCAL_NAME, nameValue)));
                    break;
                case GREATER_THAN:
                    query = CompareStringQuery.createQueryForNodesWithFieldGreaterThan(nameValue,
                                                                                       PathIndex.LOCAL_NAME,
                                                                                       context.getValueFactories(),
                                                                                       caseSensitive);
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    query = CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(nameValue,
                                                                                                PathIndex.LOCAL_NAME,
                                                                                                context.getValueFactories(),
                                                                                                caseSensitive);
                    break;
                case LESS_THAN:
                    query = CompareStringQuery.createQueryForNodesWithFieldLessThan(nameValue,
                                                                                    PathIndex.LOCAL_NAME,
                                                                                    context.getValueFactories(),
                                                                                    caseSensitive);
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    query = CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(nameValue,
                                                                                             PathIndex.LOCAL_NAME,
                                                                                             context.getValueFactories(),
                                                                                             caseSensitive);
                    break;
            }

            // Now execute and collect the UUIDs ...
            UuidCollector uuidCollector = new UuidCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, uuidCollector);
            return findAllNodesWithUuids(uuidCollector.getUuids());
        }

        protected Query findNodesWith( NodeDepth depthConstraint,
                                       Operator operator,
                                       Object value ) throws IOException {
            int depth = context.getValueFactories().getLongFactory().create(value).intValue();
            Query query = null;
            switch (operator) {
                case EQUAL_TO:
                    query = NumericRangeQuery.newIntRange(PathIndex.DEPTH, depth, depth, true, true);
                    break;
                case NOT_EQUAL_TO:
                    query = NumericRangeQuery.newIntRange(PathIndex.DEPTH, depth, depth, true, true);
                    query = new NotQuery(query);
                    break;
                case GREATER_THAN:
                    query = NumericRangeQuery.newIntRange(PathIndex.DEPTH, depth, MAX_DEPTH, false, true);
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    query = NumericRangeQuery.newIntRange(PathIndex.DEPTH, depth, MAX_DEPTH, true, true);
                    break;
                case LESS_THAN:
                    query = NumericRangeQuery.newIntRange(PathIndex.DEPTH, MIN_DEPTH, depth, true, false);
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    query = NumericRangeQuery.newIntRange(PathIndex.DEPTH, MIN_DEPTH, depth, true, true);
                    break;
                case LIKE:
                    // This is not allowed ...
                    return null;
            }

            // Now execute and collect the UUIDs ...
            UuidCollector uuidCollector = new UuidCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, uuidCollector);
            return findAllNodesWithUuids(uuidCollector.getUuids());
        }

        protected Query createLocalNameQuery( String likeExpression ) {
            if (likeExpression == null) return null;
            likeExpression = likeExpression.trim();
            if (likeExpression.length() == 0) return null;
            if (likeExpression.indexOf('?') != -1 || likeExpression.indexOf('*') != -1) {
                // The local name is a like ...
                return findNodesLike(PathIndex.LOCAL_NAME, likeExpression);
            }
            // The local name is an exact match ...
            return new TermQuery(new Term(PathIndex.LOCAL_NAME, likeExpression));
        }

        protected Query createSnsIndexQuery( String likeExpression ) {
            if (likeExpression == null) return null;
            likeExpression = likeExpression.trim();
            if (likeExpression.length() == 0) return null;

            // Remove the leading '[' ...
            assert likeExpression.charAt(0) == '[';
            likeExpression = likeExpression.substring(1);

            // Remove the trailing ']' if it exists ...
            int closeBracketIndex = likeExpression.indexOf(']');
            if (closeBracketIndex != -1) {
                likeExpression = likeExpression.substring(0, closeBracketIndex);
            }
            // If SNS expression contains '?' or '*' ...
            if (likeExpression.indexOf('?') != -1 || likeExpression.indexOf('*') != -1) {
                // There is a LIKE expression for the SNS ...
                return findNodesLike(PathIndex.SNS_INDEX, likeExpression);
            }
            // This is not a LIKE expression but an exact value specification and should be a number ...
            try {
                // This SNS is just a number ...
                int sns = Integer.parseInt(likeExpression);
                return NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, sns, sns, true, false);
            } catch (NumberFormatException e) {
                // It's not a number but it's in the SNS field, so there will be no results ...
                return new MatchNoneQuery();
            }
        }

    }

    /**
     * Convert the JCR like expression to a Lucene wildcard expression. The JCR like expression uses '%' to match 0 or more
     * characters, '_' to match any single character, '\x' to match the 'x' character, and all other characters to match
     * themselves.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    protected static String toWildcardExpression( String likeExpression ) {
        assert likeExpression != null;
        assert likeExpression.length() > 0;
        return likeExpression.replace('%', '*').replace('_', '?').replaceAll("\\\\(.)", "$1");
    }

    /**
     * Convert the JCR like expression to a regular expression. The JCR like expression uses '%' to match 0 or more characters,
     * '_' to match any single character, '\x' to match the 'x' character, and all other characters to match themselves. Note that
     * if any regex metacharacters appear in the like expression, they will be escaped within the resulting regular expression.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    protected static String toRegularExpression( String likeExpression ) {
        assert likeExpression != null;
        assert likeExpression.length() > 0;
        // Replace all '\x' with 'x' ...
        String result = likeExpression.replaceAll("\\\\(.)", "$1");
        // Escape characters used as metacharacters in regular expressions, including
        // '[', '^', '\', '$', '.', '|', '?', '*', '+', '(', and ')'
        result = result.replaceAll("([[^\\\\$.|?*+()])", "\\$1");
        // Replace '%'->'[.]+' and '_'->'[.]
        result = likeExpression.replace("%", "[.]+").replace("_", "[.]");
        return result;
    }

    protected static String pathAsString( Path path,
                                          ValueFactory<String> stringFactory ) {
        assert path != null;
        if (path.isRoot()) return "/";
        String pathStr = stringFactory.create(path);
        if (!pathStr.endsWith("]")) {
            pathStr = pathStr + '[' + Path.DEFAULT_INDEX + ']';
        }
        return pathStr;
    }

    /**
     * A {@link Collector} implementation that only captures the UUID of the documents returned by a query. Score information is
     * not recorded. This is often used when querying the {@link PathIndex} to collect the UUIDs of a set of nodes satisfying some
     * path constraint.
     * 
     * @see DualIndexLayout.LuceneSession#findChildNodes(Path)
     */
    protected static class UuidCollector extends Collector {
        private final Set<UUID> uuids = new HashSet<UUID>();
        private String[] uuidsByDocId;
        private int baseDocId;

        protected UuidCollector() {
        }

        /**
         * Get the UUIDs that have been collected.
         * 
         * @return the set of UUIDs; never null
         */
        public Set<UUID> getUuids() {
            return uuids;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#acceptsDocsOutOfOrder()
         */
        @Override
        public boolean acceptsDocsOutOfOrder() {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#setScorer(org.apache.lucene.search.Scorer)
         */
        @Override
        public void setScorer( Scorer scorer ) {
            // we don't care about scoring
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#collect(int)
         */
        @Override
        public void collect( int doc ) {
            int index = doc - baseDocId;
            assert index >= 0;
            String uuidString = uuidsByDocId[index];
            assert uuidString != null;
            uuids.add(UUID.fromString(uuidString));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#setNextReader(org.apache.lucene.index.IndexReader, int)
         */
        @Override
        public void setNextReader( IndexReader reader,
                                   int docBase ) throws IOException {
            this.uuidsByDocId = FieldCache.DEFAULT.getStrings(reader, UUID_FIELD);
            this.baseDocId = docBase;
        }
    }
}
