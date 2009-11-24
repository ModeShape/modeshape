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
import java.util.Iterator;
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
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.text.SecureHashTextEncoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.SecureHash.Algorithm;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryEngine;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.NodeDepth;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.NodePath;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.QueryCommand;
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
import org.jboss.dna.graph.search.SearchException;
import org.jboss.dna.graph.search.SearchProvider;
import org.jboss.dna.search.IndexRules.Rule;
import org.jboss.dna.search.LuceneSession.TupleCollector;
import org.jboss.dna.search.query.CompareLengthQuery;
import org.jboss.dna.search.query.CompareNameQuery;
import org.jboss.dna.search.query.ComparePathQuery;
import org.jboss.dna.search.query.CompareStringQuery;
import org.jboss.dna.search.query.IdsQuery;
import org.jboss.dna.search.query.MatchNoneQuery;
import org.jboss.dna.search.query.NotQuery;

/**
 * A simple {@link SearchProvider} implementation that relies upon two separate indexes: one for the node content and a second one
 * for paths and UUIDs.
 */
@ThreadSafe
public class DualIndexSearchProvider implements SearchProvider {

    /**
     * The default set of {@link IndexRules} used by {@link DualIndexSearchProvider} instances when no rules are provided. These
     * rules default to index and analyze all properties, and to index the {@link DnaLexicon#UUID dna:uuid} and
     * {@link JcrLexicon#UUID jcr:uuid} properties to be indexed and stored only (not analyzed and not included in full-text
     * search. The rules also treat {@link JcrLexicon#CREATED jcr:created} and {@link JcrLexicon#LAST_MODIFIED jcr:lastModified}
     * properties as dates.
     */
    public static final IndexRules DEFAULT_RULES;

    static {
        IndexRules.Builder builder = IndexRules.createBuilder();
        // Configure the default behavior ...
        builder.defaultTo(IndexRules.INDEX | IndexRules.FULL_TEXT | IndexRules.STORE);
        // Configure the UUID properties to be just indexed and stored (not analyzed, not included in full-text) ...
        builder.store(JcrLexicon.UUID, DnaLexicon.UUID);
        // Configure the properties that we'll treat as dates ...
        builder.treatAsDates(JcrLexicon.CREATED, JcrLexicon.LAST_MODIFIED);
        DEFAULT_RULES = builder.build();
    }

    protected static final long MIN_DATE = 0;
    protected static final long MAX_DATE = Long.MAX_VALUE;
    protected static final long MIN_LONG = Long.MIN_VALUE;
    protected static final long MAX_LONG = Long.MAX_VALUE;
    protected static final double MIN_DOUBLE = Double.MIN_VALUE;
    protected static final double MAX_DOUBLE = Double.MAX_VALUE;
    protected static final int MIN_DEPTH = 0;
    protected static final int MAX_DEPTH = 100;
    protected static final int MIN_SNS_INDEX = 1;
    protected static final int MAX_SNS_INDEX = 1000; // assume there won't be more than 1000 same-name-siblings

    protected static final String PATHS_INDEX_NAME = "paths";
    protected static final String CONTENT_INDEX_NAME = "content";

    /**
     * Given the name of a property field of the form "&lt;namespace>:&lt;local>" (where &lt;namespace> can be zero-length), this
     * provider also stores the value(s) for free-text searching in a field named ":ft:&lt;namespace>:&lt;local>". Thus, even if
     * the namespace is zero-length, the free-text search field will be named ":ft::&lt;local>" and will not clash with any other
     * property name.
     */
    protected static final String FULL_TEXT_PREFIX = ":ft:";

    /**
     * This index stores only these fields, so we can use the most obvious names and not worry about clashes.
     */
    static class PathIndex {
        public static final String PATH = "pth";
        public static final String NODE_NAME = "nam";
        public static final String LOCAL_NAME = "loc";
        public static final String SNS_INDEX = "sns";
        public static final String LOCATION_ID_PROPERTIES = "idp";
        public static final String ID = ContentIndex.ID;
        public static final String DEPTH = "dep";
    }

    /**
     * This index stores these two fields <i>plus</i> all properties. Therefore, we have to worry about name clashes, which is why
     * these field names are prefixed with '::', which is something that does appear in property names as they are serialized.
     */
    static class ContentIndex {
        public static final String ID = "::id";
        public static final String FULL_TEXT = "::fts";
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
            return PathIndex.ID.equals(fieldName) ? FieldSelectorResult.LOAD_AND_BREAK : FieldSelectorResult.NO_LOAD;
        }
    };

    private final IndexRules rules;
    private final LuceneConfiguration directoryConfiguration;
    private final TextEncoder namespaceEncoder;

    public DualIndexSearchProvider( LuceneConfiguration directoryConfiguration,
                                    IndexRules rules ) {
        assert directoryConfiguration != null;
        assert rules != null;
        this.rules = rules;
        this.directoryConfiguration = directoryConfiguration;
        this.namespaceEncoder = new SecureHashTextEncoder(Algorithm.SHA_1, 10);
    }

    public DualIndexSearchProvider( LuceneConfiguration directoryConfiguration ) {
        this(directoryConfiguration, DEFAULT_RULES);
    }

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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchProvider#createSession(org.jboss.dna.graph.ExecutionContext, java.lang.String,
     *      java.lang.String, boolean, boolean)
     */
    public SearchProvider.Session createSession( ExecutionContext context,
                                                 String sourceName,
                                                 String workspaceName,
                                                 boolean overwrite,
                                                 boolean readOnly ) {
        Directory pathIndexDirectory = directoryConfiguration.getDirectory(workspaceName, PATHS_INDEX_NAME);
        Directory contentIndexDirectory = directoryConfiguration.getDirectory(workspaceName, CONTENT_INDEX_NAME);
        assert pathIndexDirectory != null;
        assert contentIndexDirectory != null;
        Analyzer analyzer = createAnalyzer();
        assert analyzer != null;
        NamespaceRegistry encodingRegistry = new EncodingNamespaceRegistry(context.getNamespaceRegistry(), namespaceEncoder);
        ExecutionContext encodingContext = context.with(encodingRegistry);
        return new DualIndexSession(encodingContext, sourceName, workspaceName, rules, pathIndexDirectory, contentIndexDirectory,
                                    analyzer, overwrite, readOnly);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchProvider#destroyIndexes(org.jboss.dna.graph.ExecutionContext, java.lang.String,
     *      java.lang.String)
     */
    public boolean destroyIndexes( ExecutionContext context,
                                   String sourceName,
                                   String workspaceName ) {
        directoryConfiguration.destroyDirectory(workspaceName, PATHS_INDEX_NAME);
        directoryConfiguration.destroyDirectory(workspaceName, CONTENT_INDEX_NAME);
        return true;
    }

    protected class DualIndexSession extends LuceneSession {
        private final Directory pathsIndexDirectory;
        private final Directory contentIndexDirectory;
        private IndexReader pathsReader;
        private IndexWriter pathsWriter;
        private IndexSearcher pathsSearcher;
        private IndexReader contentReader;
        private IndexWriter contentWriter;
        private IndexSearcher contentSearcher;

        protected DualIndexSession( ExecutionContext context,
                                    String sourceName,
                                    String workspaceName,
                                    IndexRules rules,
                                    Directory pathsIndexDirectory,
                                    Directory contentIndexDirectory,
                                    Analyzer analyzer,
                                    boolean overwrite,
                                    boolean readOnly ) {
            super(context, sourceName, workspaceName, rules, analyzer, overwrite, readOnly);
            this.pathsIndexDirectory = pathsIndexDirectory;
            this.contentIndexDirectory = contentIndexDirectory;
            assert this.pathsIndexDirectory != null;
            assert this.contentIndexDirectory != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.LuceneSession#fullTextFieldName(java.lang.String)
         */
        @Override
        protected String fullTextFieldName( String propertyName ) {
            return FULL_TEXT_PREFIX + propertyName;
        }

        protected void addIdProperties( Location location,
                                        Document doc ) {
            if (!location.hasIdProperties()) return;
            for (Property idProp : location.getIdProperties()) {
                String fieldValue = serializeProperty(idProp);
                doc.add(new Field(PathIndex.LOCATION_ID_PROPERTIES, fieldValue, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
        }

        protected Location readLocation( Document doc ) {
            // Read the path ...
            String pathString = doc.get(PathIndex.PATH);
            Path path = pathFactory.create(pathString);
            // Look for the Location's ID properties ...
            String[] idProps = doc.getValues(PathIndex.LOCATION_ID_PROPERTIES);
            if (idProps.length == 0) {
                return Location.create(path);
            }
            if (idProps.length == 1) {
                Property idProp = deserializeProperty(idProps[0]);
                if (idProp == null) return Location.create(path);
                if (idProp.isSingle() && (idProp.getName().equals(JcrLexicon.UUID) || idProp.getName().equals(DnaLexicon.UUID))) {
                    return Location.create(path, (UUID)idProp.getFirstValue()); // know that deserialize returns UUID value
                }
                return Location.create(path, idProp);
            }
            List<Property> properties = new LinkedList<Property>();
            for (String idProp : doc.getValues(PathIndex.LOCATION_ID_PROPERTIES)) {
                Property prop = deserializeProperty(idProp);
                if (prop != null) properties.add(prop);
            }
            return properties.isEmpty() ? Location.create(path) : Location.create(path, properties);

        }

        protected final String serializeProperty( Property property ) {
            StringBuilder sb = new StringBuilder();
            sb.append(stringFactory.create(property.getName()));
            sb.append('=');
            Iterator<?> iter = property.getValues();
            if (iter.hasNext()) {
                sb.append(stringFactory.create(iter.next()));
            }
            while (iter.hasNext()) {
                sb.append('\n');
                sb.append(stringFactory.create(iter.next()));
            }
            return sb.toString();
        }

        protected final Property deserializeProperty( String propertyString ) {
            int index = propertyString.indexOf('=');
            assert index > -1;
            if (index == propertyString.length() - 1) return null;
            Name propName = nameFactory.create(propertyString.substring(0, index));
            String valueString = propertyString.substring(index + 1);
            // Break into multiple values if multiple lines ...
            String[] values = valueString.split("\\n");
            if (values.length == 0) return null;
            if (values.length == 1) {
                Object value = values[0];
                if (DnaLexicon.UUID.equals(propName) || JcrLexicon.UUID.equals(propName)) {
                    value = uuidFactory.create(value);
                }
                return context.getPropertyFactory().create(propName, value);
            }
            List<String> propValues = new LinkedList<String>();
            for (String value : values) {
                propValues.add(value);
            }
            return context.getPropertyFactory().create(propName, propValues);
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
                if (overwrite) {
                    // Always overwrite it ...
                    pathsWriter = new IndexWriter(pathsIndexDirectory, analyzer, overwrite, MaxFieldLength.UNLIMITED);
                } else {
                    // Don't overwrite, but create if missing ...
                    pathsWriter = new IndexWriter(pathsIndexDirectory, analyzer, MaxFieldLength.UNLIMITED);
                }
            }
            return pathsWriter;
        }

        protected IndexWriter getContentWriter() throws IOException {
            assert !readOnly;
            if (contentWriter == null) {
                if (overwrite) {
                    // Always overwrite it ...
                    contentWriter = new IndexWriter(contentIndexDirectory, analyzer, overwrite, MaxFieldLength.UNLIMITED);
                } else {
                    // Don't overwrite, but create if missing ...
                    contentWriter = new IndexWriter(contentIndexDirectory, analyzer, MaxFieldLength.UNLIMITED);
                }
            }
            return contentWriter;
        }

        protected IndexSearcher getPathsSearcher() throws IOException {
            if (pathsSearcher == null) {
                pathsSearcher = new IndexSearcher(getPathsReader());
            }
            return pathsSearcher;
        }

        @Override
        public IndexSearcher getContentSearcher() throws IOException {
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
         * @see org.jboss.dna.graph.search.SearchProvider.Session#index(org.jboss.dna.graph.Node)
         */
        public void index( Node node ) {
            assert !readOnly;
            Location location = node.getLocation();
            UUID uuid = location.getUuid();
            if (uuid == null) uuid = UUID.randomUUID();
            Path path = location.getPath();
            String idStr = stringFactory.create(uuid);
            String pathStr = pathAsString(path, stringFactory);
            String nameStr = path.isRoot() ? "" : stringFactory.create(path.getLastSegment().getName());
            String localNameStr = path.isRoot() ? "" : path.getLastSegment().getName().getLocalName();
            int sns = path.isRoot() ? 1 : path.getLastSegment().getIndex();

            Logger logger = Logger.getLogger(getClass());
            if (logger.isTraceEnabled()) {
                logger.trace("indexing {0}", pathStr);
            }

            try {

                // Create a separate document for the path, which makes it easier to handle moves since the path can
                // be changed without changing any other content fields ...
                Document doc = new Document();
                doc.add(new Field(PathIndex.PATH, pathStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(PathIndex.NODE_NAME, nameStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(PathIndex.LOCAL_NAME, localNameStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new NumericField(PathIndex.SNS_INDEX, Field.Store.YES, true).setIntValue(sns));
                doc.add(new Field(PathIndex.ID, idStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new NumericField(PathIndex.DEPTH, Field.Store.YES, true).setIntValue(path.size()));
                addIdProperties(location, doc);
                getPathsWriter().addDocument(doc);

                // Create the document for the content (properties) ...
                doc = new Document();
                doc.add(new Field(ContentIndex.ID, idStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
                String stringValue = null;
                StringBuilder fullTextSearchValue = null;
                for (Property property : node.getProperties()) {
                    Name name = property.getName();
                    Rule rule = rules.getRule(name);
                    if (!rule.isIncluded()) continue;
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
                    doc.add(new Field(ContentIndex.FULL_TEXT, fullTextSearchValue.toString(), Field.Store.NO,
                                      Field.Index.ANALYZED));
                }
                getContentWriter().addDocument(doc);
            } catch (IOException e) {
                throw new LuceneException(e);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.search.SearchProvider.Session#optimize()
         */
        public void optimize() {
            try {
                getContentWriter().optimize();
                getPathsWriter().optimize();
            } catch (IOException e) {
                throw new LuceneException(e);

            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.search.SearchProvider.Session#apply(java.lang.Iterable)
         */
        public int apply( Iterable<ChangeRequest> changes ) {
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
         * @see org.jboss.dna.graph.search.SearchProvider.Session#deleteBelow(org.jboss.dna.graph.property.Path)
         */
        public int deleteBelow( Path path ) {
            assert !readOnly;
            try {
                // Create a query to find all the nodes at or below the specified path ...
                Set<String> ids = getIdsForDescendantsOf(path, true);
                Query uuidQuery = findAllNodesWithIds(ids);
                // Now delete the documents from each index using this query, which we can reuse ...
                getPathsWriter().deleteDocuments(uuidQuery);
                getContentWriter().deleteDocuments(uuidQuery);
                return ids.size();
            } catch (FileNotFoundException e) {
                // There are no index files yet, so nothing to delete ...
                return 0;
            } catch (IOException e) {
                throw new LuceneException(e);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.search.SearchProvider.Session#search(org.jboss.dna.graph.ExecutionContext, java.lang.String,
         *      int, int, java.util.List)
         */
        public void search( ExecutionContext context,
                            String fullTextString,
                            int maxResults,
                            int offset,
                            List<Location> results ) {
            assert fullTextString != null;
            assert fullTextString.length() > 0;
            assert offset >= 0;
            assert maxResults > 0;
            assert results != null;

            try {
                // Parse the full-text search and search against the 'fts' field ...
                QueryParser parser = new QueryParser(Version.LUCENE_29, ContentIndex.FULL_TEXT, createAnalyzer());
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
                    for (int i = offset, num = scoreDocs.length; i != num; ++i) {
                        ScoreDoc result = scoreDocs[i];
                        int docId = result.doc;
                        // Find the UUID of the node (this UUID might be artificial, so we have to find the path) ...
                        Document doc = contentReader.document(docId, UUID_FIELD_SELECTOR);
                        String id = doc.get(ContentIndex.ID);
                        Location location = getLocationForDocument(id, pathReader, pathSearcher);
                        if (location == null) {
                            // No path record found ...
                            continue;
                        }
                        // Now add the location ...
                        results.add(location);
                    }
                }
            } catch (ParseException e) {
                String msg = SearchI18n.errorWhilePerformingSearch.text(workspaceName, sourceName, fullTextString, e.getMessage());
                throw new SearchException(fullTextString, msg, e);
            } catch (IOException e) {
                throw new LuceneException(e);
            }
        }

        protected Location getLocationForDocument( String id,
                                                   IndexReader pathReader,
                                                   IndexSearcher pathSearcher ) throws IOException {
            // Find the path for this node (is there a better way to do this than one search per ID?) ...
            TopDocs pathDocs = pathSearcher.search(new TermQuery(new Term(PathIndex.ID, id)), 1);
            if (pathDocs.scoreDocs.length < 1) {
                // No path record found ...
                return null;
            }
            Document pathDoc = pathReader.document(pathDocs.scoreDocs[0].doc);
            return readLocation(pathDoc);
        }

        protected UUID getUuid( Document document,
                                Name name ) {
            String nameString = stringFactory.create(name);
            String value = document.get(nameString);
            return value != null ? uuidFactory.create(value) : null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.search.SearchProvider.Session#query(org.jboss.dna.graph.query.QueryContext,
         *      org.jboss.dna.graph.query.model.QueryCommand)
         */
        public QueryResults query( QueryContext queryContext,
                                   QueryCommand query ) {
            return queryEngine().execute(queryContext, query);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.search.SearchProvider.Session#commit()
         */
        public void commit() {
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
                // try {
                // pathsWriter.commit();
                // } catch (IOException e) {
                // if (ioError == null) ioError = e;
                // } catch (RuntimeException e) {
                // if (runtimeError == null) runtimeError = e;
                // } finally {
                try {
                    pathsWriter.close();
                } catch (IOException e) {
                    if (ioError == null) ioError = e;
                } catch (RuntimeException e) {
                    if (runtimeError == null) runtimeError = e;
                } finally {
                    pathsWriter = null;
                }
                // }
            }
            if (contentWriter != null) {
                // try {
                // contentWriter.commit();
                // } catch (IOException e) {
                // if (ioError == null) ioError = e;
                // } catch (RuntimeException e) {
                // if (runtimeError == null) runtimeError = e;
                // } finally {
                try {
                    contentWriter.close();
                } catch (IOException e) {
                    if (ioError == null) ioError = e;
                } catch (RuntimeException e) {
                    if (runtimeError == null) runtimeError = e;
                } finally {
                    contentWriter = null;
                }
                // }
            }
            if (ioError != null) {
                String msg = SearchI18n.errorWhileCommittingIndexChanges.text(workspaceName, sourceName, ioError.getMessage());
                throw new LuceneException(msg, ioError);
            }
            if (runtimeError != null) throw runtimeError;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.search.SearchProvider.Session#rollback()
         */
        public void rollback() {
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
            if (ioError != null) {
                String msg = SearchI18n.errorWhileRollingBackIndexChanges.text(workspaceName, sourceName, ioError.getMessage());
                throw new LuceneException(msg, ioError);
            }
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
                    return DualIndexSession.this.createAccessComponent(originalQuery,
                                                                       context,
                                                                       accessNode,
                                                                       resultColumns,
                                                                       analyzer);
                }
            };

            return new QueryEngine(planner, optimizer, processor);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.LuceneSession#createAccessComponent(org.jboss.dna.graph.query.model.QueryCommand,
         *      org.jboss.dna.graph.query.QueryContext, org.jboss.dna.graph.query.plan.PlanNode,
         *      org.jboss.dna.graph.query.QueryResults.Columns, org.jboss.dna.graph.query.process.SelectComponent.Analyzer)
         */
        @Override
        protected ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                             QueryContext context,
                                                             PlanNode accessNode,
                                                             Columns resultColumns,
                                                             org.jboss.dna.graph.query.process.SelectComponent.Analyzer analyzer ) {
            // Create a processing component for this access query ...
            return new LuceneQueryComponent(this, originalQuery, context, resultColumns, accessNode, analyzer, sourceName,
                                            workspaceName);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.LuceneSession#createTupleCollector(Columns)
         */
        @Override
        public TupleCollector createTupleCollector( Columns columns ) {
            return new DualIndexTupleCollector(this, columns);
        }

        /**
         * Get the set of IDs for the children of the node at the given path.
         * 
         * @param parentPath the path to the parent node; may not be null
         * @return the doc IDs of the child nodes; never null but possibly empty
         * @throws IOException if there is an error accessing the indexes
         */
        protected Set<String> getIdsForChildrenOf( Path parentPath ) throws IOException {
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

            // Now execute and collect the IDs ...
            IdCollector idCollector = new IdCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, idCollector);
            return idCollector.getIds();
        }

        /**
         * Get the set of IDs for the nodes that are descendants of the node at the given path.
         * 
         * @param parentPath the path to the parent node; may not be null and <i>may not be the root node</i>
         * @param includeParent true if the parent node should be included in the results, or false if only the descendants should
         *        be included
         * @return the IDs of the nodes; never null but possibly empty
         * @throws IOException if there is an error accessing the indexes
         */
        protected Set<String> getIdsForDescendantsOf( Path parentPath,
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

            // Now execute and collect the IDs ...
            IdCollector idCollector = new IdCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, idCollector);
            return idCollector.getIds();
        }

        /**
         * Get the set containing the single ID for the node at the given path.
         * 
         * @param path the path to the node; may not be null
         * @return the ID of the supplied node; or null if the node cannot be found
         * @throws IOException if there is an error accessing the indexes
         */
        protected String getIdFor( Path path ) throws IOException {
            // Create a query to find all the nodes below the parent path ...
            IndexSearcher searcher = getPathsSearcher();
            String stringifiedPath = pathAsString(path, stringFactory);
            TermQuery query = new TermQuery(new Term(PathIndex.PATH, stringifiedPath));

            // Now execute and collect the UUIDs ...
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.totalHits == 0) return null;
            Document pathDoc = getPathsReader().document(topDocs.scoreDocs[0].doc);
            String idString = pathDoc.get(PathIndex.ID);
            assert idString != null;
            return idString;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.LuceneSession#findAllNodesWithIds(java.util.Set)
         */
        @Override
        public Query findAllNodesWithIds( Set<String> ids ) {
            if (ids.isEmpty()) {
                // There are no children, so return a null query ...
                return new MatchNoneQuery();
            }
            if (ids.size() == 1) {
                String id = ids.iterator().next();
                if (id == null) return new MatchNoneQuery();
                return new TermQuery(new Term(ContentIndex.ID, id));
            }
            if (ids.size() < 50) {
                // Create an OR boolean query for all the UUIDs, since this is probably more efficient ...
                BooleanQuery query = new BooleanQuery();
                for (String id : ids) {
                    Query uuidQuery = new TermQuery(new Term(ContentIndex.ID, id));
                    query.add(uuidQuery, Occur.SHOULD);
                }
                return query;
            }
            // Return a query that will always find all of the UUIDs ...
            return new IdsQuery(ContentIndex.ID, ids);
        }

        @Override
        public Query findAllNodesBelow( Path ancestorPath ) throws IOException {
            if (ancestorPath.isRoot()) {
                return new MatchAllDocsQuery();
            }
            Set<String> ids = getIdsForDescendantsOf(ancestorPath, false);
            return findAllNodesWithIds(ids);
        }

        /**
         * Return a query that can be used to find all of the documents that represent nodes that are children of the node at the
         * supplied path.
         * 
         * @param parentPath the path of the parent node.
         * @return the query; never null
         * @throws IOException if there is an error finding the UUIDs of the child nodes
         */
        @Override
        public Query findChildNodes( Path parentPath ) throws IOException {
            if (parentPath.isRoot()) {
                return new MatchAllDocsQuery();
            }
            Set<String> childIds = getIdsForChildrenOf(parentPath);
            return findAllNodesWithIds(childIds);
        }

        /**
         * Create a query that can be used to find the one document (or node) that exists at the exact path supplied. This method
         * first queries the {@link PathIndex path index} to find the ID of the node at the supplied path, and then returns a
         * query that matches the ID.
         * 
         * @param path the path of the node
         * @return the query; never null
         * @throws IOException if there is an error finding the ID for the supplied path
         */
        @Override
        public Query findNodeAt( Path path ) throws IOException {
            String id = getIdFor(path);
            if (id == null) return null;
            return new TermQuery(new Term(ContentIndex.ID, id));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.LuceneSession#findNodesLike(java.lang.String, java.lang.String, boolean)
         */
        @Override
        public Query findNodesLike( String fieldName,
                                    String likeExpression,
                                    boolean caseSensitive ) {
            ValueFactories factories = context.getValueFactories();
            return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression, fieldName, factories, caseSensitive);
        }

        @Override
        public Query findNodesWith( Length propertyLength,
                                    Operator operator,
                                    Object value ) {
            assert propertyLength != null;
            assert value != null;
            PropertyValue propertyValue = propertyLength.getPropertyValue();
            String field = stringFactory.create(propertyValue.getPropertyName());
            ValueFactories factories = context.getValueFactories();
            int length = factories.getLongFactory().create(value).intValue();
            switch (operator) {
                case EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldEqualTo(length, field, factories);
                case NOT_EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldNotEqualTo(length, field, factories);
                case GREATER_THAN:
                    return CompareLengthQuery.createQueryForNodesWithFieldGreaterThan(length, field, factories);
                case GREATER_THAN_OR_EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(length, field, factories);
                case LESS_THAN:
                    return CompareLengthQuery.createQueryForNodesWithFieldLessThan(length, field, factories);
                case LESS_THAN_OR_EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldLessThanOrEqualTo(length, field, factories);
                case LIKE:
                    // This is not allowed ...
                    assert false;
                    break;
            }
            return null;
        }

        @Override
        public Query findNodesWith( PropertyValue propertyValue,
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
                            return CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue,
                                                                                          field,
                                                                                          factories,
                                                                                          caseSensitive);
                        case NOT_EQUAL_TO:
                            Query query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue,
                                                                                                 field,
                                                                                                 factories,
                                                                                                 caseSensitive);
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
                            return findNodesLike(field, stringValue, caseSensitive);
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

        @Override
        public Query findNodesWithNumericRange( PropertyValue propertyValue,
                                                Object lowerValue,
                                                Object upperValue,
                                                boolean includesLower,
                                                boolean includesUpper ) {
            String field = stringFactory.create(propertyValue.getPropertyName());
            return findNodesWithNumericRange(field, lowerValue, upperValue, includesLower, includesUpper);
        }

        @Override
        public Query findNodesWithNumericRange( NodeDepth depth,
                                                Object lowerValue,
                                                Object upperValue,
                                                boolean includesLower,
                                                boolean includesUpper ) {
            return findNodesWithNumericRange(PathIndex.DEPTH, lowerValue, upperValue, includesLower, includesUpper);
        }

        protected Query findNodesWithNumericRange( String field,
                                                   Object lowerValue,
                                                   Object upperValue,
                                                   boolean includesLower,
                                                   boolean includesUpper ) {
            PropertyType type = PropertyType.discoverType(lowerValue);
            assert type == PropertyType.discoverType(upperValue);
            ValueFactories factories = context.getValueFactories();
            switch (type) {
                case DATE:
                    long lowerDate = factories.getLongFactory().create(lowerValue);
                    long upperDate = factories.getLongFactory().create(upperValue);
                    return NumericRangeQuery.newLongRange(field, lowerDate, upperDate, includesLower, includesUpper);
                case LONG:
                    long lowerLong = factories.getLongFactory().create(lowerValue);
                    long upperLong = factories.getLongFactory().create(upperValue);
                    return NumericRangeQuery.newLongRange(field, lowerLong, upperLong, includesLower, includesUpper);
                case DECIMAL:
                case DOUBLE:
                    double lowerDouble = factories.getDoubleFactory().create(lowerValue);
                    double upperDouble = factories.getDoubleFactory().create(upperValue);
                    return NumericRangeQuery.newDoubleRange(field, lowerDouble, upperDouble, includesLower, includesUpper);
                default:
                    // This is not allowed ...
                    assert false;
                    return null;
            }
        }

        @Override
        public Query findNodesWith( NodePath nodePath,
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
                    query = findNodesLike(PathIndex.PATH, likeExpression, caseSensitive);
                    break;
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
            // Now execute and collect the IDs ...
            IdCollector idCollector = new IdCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, idCollector);
            return findAllNodesWithIds(idCollector.getIds());
        }

        @Override
        public Query findNodesWith( NodeName nodeName,
                                    Operator operator,
                                    Object value,
                                    boolean caseSensitive ) throws IOException {
            ValueFactories factories = getContext().getValueFactories();
            String stringValue = stringFactory.create(value);
            if (!caseSensitive) stringValue = stringValue.toLowerCase();
            Path.Segment segment = operator != Operator.LIKE ? pathFactory.createSegment(stringValue) : null;
            int snsIndex = operator != Operator.LIKE ? segment.getIndex() : 0;
            Query query = null;
            switch (operator) {
                case EQUAL_TO:
                    BooleanQuery booleanQuery = new BooleanQuery();
                    booleanQuery.add(new TermQuery(new Term(PathIndex.NODE_NAME, stringValue)), Occur.MUST);
                    booleanQuery.add(NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, snsIndex, snsIndex, true, false),
                                     Occur.MUST);
                    return booleanQuery;
                case NOT_EQUAL_TO:
                    booleanQuery = new BooleanQuery();
                    booleanQuery.add(new TermQuery(new Term(PathIndex.NODE_NAME, stringValue)), Occur.MUST);
                    booleanQuery.add(NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, snsIndex, snsIndex, true, false),
                                     Occur.MUST);
                    return new NotQuery(booleanQuery);
                case GREATER_THAN:
                    query = CompareNameQuery.createQueryForNodesWithNameGreaterThan(segment,
                                                                                    PathIndex.NODE_NAME,
                                                                                    PathIndex.SNS_INDEX,
                                                                                    factories,
                                                                                    caseSensitive);
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    query = CompareNameQuery.createQueryForNodesWithNameGreaterThanOrEqualTo(segment,
                                                                                             PathIndex.NODE_NAME,
                                                                                             PathIndex.SNS_INDEX,
                                                                                             factories,
                                                                                             caseSensitive);
                    break;
                case LESS_THAN:
                    query = CompareNameQuery.createQueryForNodesWithNameLessThan(segment,
                                                                                 PathIndex.NODE_NAME,
                                                                                 PathIndex.SNS_INDEX,
                                                                                 factories,
                                                                                 caseSensitive);
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    query = CompareNameQuery.createQueryForNodesWithNameLessThanOrEqualTo(segment,
                                                                                          PathIndex.NODE_NAME,
                                                                                          PathIndex.SNS_INDEX,
                                                                                          factories,
                                                                                          caseSensitive);
                    break;
                case LIKE:
                    // See whether the like expression has brackets ...
                    String likeExpression = stringValue;
                    int openBracketIndex = likeExpression.indexOf('[');
                    if (openBracketIndex != -1) {
                        String localNameExpression = likeExpression.substring(0, openBracketIndex);
                        String snsIndexExpression = likeExpression.substring(openBracketIndex);
                        Query localNameQuery = CompareStringQuery.createQueryForNodesWithFieldLike(localNameExpression,
                                                                                                   PathIndex.NODE_NAME,
                                                                                                   factories,
                                                                                                   caseSensitive);
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
                        query = CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression,
                                                                                    PathIndex.NODE_NAME,
                                                                                    factories,
                                                                                    caseSensitive);
                    }
                    assert query != null;
                    break;
            }

            // Now execute and collect the IDs ...
            IdCollector idCollector = new IdCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, idCollector);
            return findAllNodesWithIds(idCollector.getIds());
        }

        @Override
        public Query findNodesWith( NodeLocalName nodeName,
                                    Operator operator,
                                    Object value,
                                    boolean caseSensitive ) throws IOException {
            String nameValue = stringFactory.create(value);
            Query query = null;
            switch (operator) {
                case LIKE:
                    String likeExpression = stringFactory.create(value);
                    query = findNodesLike(PathIndex.LOCAL_NAME, likeExpression, caseSensitive);
                    break;
                case EQUAL_TO:
                    query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                                   PathIndex.LOCAL_NAME,
                                                                                   context.getValueFactories(),
                                                                                   caseSensitive);
                    break;
                case NOT_EQUAL_TO:
                    query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                                   PathIndex.LOCAL_NAME,
                                                                                   context.getValueFactories(),
                                                                                   caseSensitive);
                    query = new NotQuery(query);
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

            // Now execute and collect the IDs ...
            IdCollector idCollector = new IdCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, idCollector);
            return findAllNodesWithIds(idCollector.getIds());
        }

        @Override
        public Query findNodesWith( NodeDepth depthConstraint,
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

            // Now execute and collect the IDs ...
            IdCollector idCollector = new IdCollector();
            IndexSearcher searcher = getPathsSearcher();
            searcher.search(query, idCollector);
            return findAllNodesWithIds(idCollector.getIds());
        }

        protected Query createLocalNameQuery( String likeExpression,
                                              boolean caseSensitive ) {
            if (likeExpression == null) return null;
            ValueFactories factories = getContext().getValueFactories();
            return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression,
                                                                       PathIndex.LOCAL_NAME,
                                                                       factories,
                                                                       caseSensitive);
        }

        /**
         * Utility method to generate a query against the SNS indexes. This method attempts to generate a query that works most
         * efficiently, depending upon the supplied expression. For example, if the supplied expression is just "[3]", then a
         * range query is used to find all values matching '3'. However, if "[3_]" is used (where '_' matches any
         * single-character, or digit in this case), then a range query is used to find all values between '30' and '39'.
         * Similarly, if "[3%]" is used, then a regular expression query is used.
         * 
         * @param likeExpression the expression that uses the JCR 2.0 LIKE representation, and which includes the leading '[' and
         *        trailing ']' characters
         * @return the query, or null if the expression cannot be represented as a query
         */
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
            if (likeExpression.equals("_")) {
                // The SNS expression can only be one digit ...
                return NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, MIN_SNS_INDEX, 9, true, true);
            }
            if (likeExpression.equals("%")) {
                // The SNS expression can be any digits ...
                return NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, MIN_SNS_INDEX, MAX_SNS_INDEX, true, true);
            }
            if (likeExpression.indexOf('_') != -1) {
                if (likeExpression.indexOf('%') != -1) {
                    // Contains both ...
                    return findNodesLike(PathIndex.SNS_INDEX, likeExpression, true);
                }
                // It presumably contains some numbers and at least one '_' character ...
                int firstWildcardChar = likeExpression.indexOf('_');
                if (firstWildcardChar + 1 < likeExpression.length()) {
                    // There's at least some characters after the first '_' ...
                    int secondWildcardChar = likeExpression.indexOf('_', firstWildcardChar + 1);
                    if (secondWildcardChar != -1) {
                        // There are multiple '_' characters ...
                        return findNodesLike(PathIndex.SNS_INDEX, likeExpression, true);
                    }
                }
                // There's only one '_', so parse the lowermost value and uppermost value ...
                String lowerExpression = likeExpression.replace('_', '0');
                String upperExpression = likeExpression.replace('_', '9');
                try {
                    // This SNS is just a number ...
                    int lowerSns = Integer.parseInt(lowerExpression);
                    int upperSns = Integer.parseInt(upperExpression);
                    return NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, lowerSns, upperSns, true, true);
                } catch (NumberFormatException e) {
                    // It's not a number but it's in the SNS field, so there will be no results ...
                    return new MatchNoneQuery();
                }
            }
            if (likeExpression.indexOf('%') != -1) {
                // It presumably contains some numbers and at least one '%' character ...
                return findNodesLike(PathIndex.SNS_INDEX, likeExpression, true);
            }
            // This is not a LIKE expression but an exact value specification and should be a number ...
            try {
                // This SNS is just a number ...
                int sns = Integer.parseInt(likeExpression);
                return NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, sns, sns, true, true);
            } catch (NumberFormatException e) {
                // It's not a number but it's in the SNS field, so there will be no results ...
                return new MatchNoneQuery();
            }
        }

    }

    /**
     * A {@link Collector} implementation that only captures the UUID of the documents returned by a query. Score information is
     * not recorded. This is often used when querying the {@link PathIndex} to collect the UUIDs of a set of nodes satisfying some
     * path constraint.
     * 
     * @see DualIndexSearchProvider.DualIndexSession#findChildNodes(Path)
     */
    protected static class IdCollector extends Collector {
        private final Set<String> ids = new HashSet<String>();
        private String[] idsByDocId;

        // private int baseDocId;

        protected IdCollector() {
        }

        /**
         * Get the UUIDs that have been collected.
         * 
         * @return the set of UUIDs; never null
         */
        public Set<String> getIds() {
            return ids;
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
        public void collect( int docId ) {
            assert docId >= 0;
            String idString = idsByDocId[docId];
            assert idString != null;
            ids.add(idString);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#setNextReader(org.apache.lucene.index.IndexReader, int)
         */
        @Override
        public void setNextReader( IndexReader reader,
                                   int docBase ) throws IOException {
            this.idsByDocId = FieldCache.DEFAULT.getStrings(reader, ContentIndex.ID); // same value as PathIndex.ID
            // this.baseDocId = docBase;
        }
    }

    /**
     * This collector is responsible for loading the value for each of the columns into each tuple array.
     */
    protected class DualIndexTupleCollector extends TupleCollector {
        private final DualIndexSession session;
        private final LinkedList<Object[]> tuples = new LinkedList<Object[]>();
        private final Columns columns;
        private final int numValues;
        private final boolean recordScore;
        private final int scoreIndex;
        private final FieldSelector fieldSelector;
        private final int locationIndex;
        private Scorer scorer;
        private IndexReader currentReader;
        private int docOffset;
        private boolean resolvedLocations = false;

        protected DualIndexTupleCollector( DualIndexSession session,
                                           Columns columns ) {
            this.session = session;
            this.columns = columns;
            assert this.session != null;
            assert this.columns != null;
            this.numValues = this.columns.getTupleSize();
            assert this.numValues >= 0;
            assert this.columns.getSelectorNames().size() == 1;
            final String selectorName = this.columns.getSelectorNames().get(0);
            this.locationIndex = this.columns.getLocationIndex(selectorName);
            this.recordScore = this.columns.hasFullTextSearchScores();
            this.scoreIndex = this.recordScore ? this.columns.getFullTextSearchScoreIndexFor(selectorName) : -1;
            final Set<String> columnNames = new HashSet<String>(this.columns.getColumnNames());
            columnNames.add(ContentIndex.ID); // add the UUID, which we'll put into the Location ...
            this.fieldSelector = new FieldSelector() {
                private static final long serialVersionUID = 1L;

                public FieldSelectorResult accept( String fieldName ) {
                    return columnNames.contains(fieldName) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD;
                }
            };
        }

        /**
         * @return tuples
         */
        @Override
        public LinkedList<Object[]> getTuples() {
            resolveLocations();
            return tuples;
        }

        protected void resolveLocations() {
            if (resolvedLocations) return;
            try {
                // The Location field in the tuples all contain the ID of the document, so we need to replace these
                // with the appropriate Location objects, using the content from the PathIndex ...
                IndexReader pathReader = session.getPathsReader();
                IndexSearcher pathSearcher = session.getPathsSearcher();
                for (Object[] tuple : tuples) {
                    String id = (String)tuple[locationIndex];
                    assert id != null;
                    Location location = session.getLocationForDocument(id, pathReader, pathSearcher);
                    assert location != null;
                    tuple[locationIndex] = location;
                }
                resolvedLocations = true;
            } catch (IOException e) {
                throw new LuceneException(e);
            }
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
         * Get the location index.
         * 
         * @return locationIndex
         */
        public int getLocationIndex() {
            return locationIndex;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#setNextReader(org.apache.lucene.index.IndexReader, int)
         */
        @Override
        public void setNextReader( IndexReader reader,
                                   int docBase ) {
            this.currentReader = reader;
            this.docOffset = docBase;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#setScorer(org.apache.lucene.search.Scorer)
         */
        @Override
        public void setScorer( Scorer scorer ) {
            this.scorer = scorer;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#collect(int)
         */
        @Override
        public void collect( int doc ) throws IOException {
            int docId = doc + docOffset;
            Object[] tuple = new Object[numValues];
            Document document = currentReader.document(docId, fieldSelector);
            for (String columnName : columns.getColumnNames()) {
                int index = columns.getColumnIndexForName(columnName);
                // We just need to retrieve the first value if there is more than one ...
                tuple[index] = document.get(columnName);
            }

            // Set the score column if required ...
            if (recordScore) {
                assert scorer != null;
                tuple[scoreIndex] = scorer.score();
            }

            // Load the document ID (which is a stringified UUID) into the Location slot,
            // which will be replaced later with a real Location ...
            tuple[locationIndex] = document.get(ContentIndex.ID);
            tuples.add(tuple);
        }
    }
}
