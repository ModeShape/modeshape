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
package org.jboss.dna.search.lucene;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
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
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.QueryResults.Statistics;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.NodeDepth;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.NodePath;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.TupleCollector;
import org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession;
import org.jboss.dna.search.lucene.IndexRules.FieldType;
import org.jboss.dna.search.lucene.IndexRules.NumericRule;
import org.jboss.dna.search.lucene.IndexRules.Rule;
import org.jboss.dna.search.lucene.LuceneSearchWorkspace.ContentIndex;
import org.jboss.dna.search.lucene.LuceneSearchWorkspace.PathIndex;
import org.jboss.dna.search.lucene.query.CompareLengthQuery;
import org.jboss.dna.search.lucene.query.CompareNameQuery;
import org.jboss.dna.search.lucene.query.ComparePathQuery;
import org.jboss.dna.search.lucene.query.CompareStringQuery;
import org.jboss.dna.search.lucene.query.IdsQuery;
import org.jboss.dna.search.lucene.query.MatchNoneQuery;
import org.jboss.dna.search.lucene.query.NotQuery;

/**
 * The {@link WorkspaceSession} implementation for the {@link LuceneSearchEngine}.
 */
@NotThreadSafe
public class LuceneSearchSession implements WorkspaceSession {

    /**
     * An immutable {@link FieldSelector} instance that accesses the UUID field.
     */
    protected static final FieldSelector DOC_ID_FIELD_SELECTOR = new FieldSelector() {
        private static final long serialVersionUID = 1L;

        public FieldSelectorResult accept( String fieldName ) {
            return ContentIndex.ID.equals(fieldName) ? FieldSelectorResult.LOAD_AND_BREAK : FieldSelectorResult.NO_LOAD;
        }
    };

    /**
     * An immutable {@link FieldSelector} instance that accesses the UUID field.
     */
    protected static final FieldSelector LOCATION_FIELDS_SELECTOR = new FieldSelector() {
        private static final long serialVersionUID = 1L;

        public FieldSelectorResult accept( String fieldName ) {
            if (PathIndex.PATH.equals(fieldName) || PathIndex.LOCATION_ID_PROPERTIES.equals(fieldName)) {
                return FieldSelectorResult.LOAD;
            }
            return FieldSelectorResult.NO_LOAD;
        }
    };

    protected static final int MIN_DEPTH = 0;
    protected static final int MAX_DEPTH = 100;
    protected static final int MIN_SNS_INDEX = 1;
    protected static final int MAX_SNS_INDEX = 1000; // assume there won't be more than 1000 same-name-siblings

    private final LuceneSearchWorkspace workspace;
    protected final LuceneSearchProcessor processor;
    private final Directory pathsIndexDirectory;
    private final Directory contentIndexDirectory;
    private IndexReader pathsReader;
    private IndexWriter pathsWriter;
    private IndexSearcher pathsSearcher;
    private IndexReader contentReader;
    private IndexWriter contentWriter;
    private IndexSearcher contentSearcher;

    protected LuceneSearchSession( LuceneSearchWorkspace workspace,
                                   LuceneSearchProcessor processor ) {
        assert workspace != null;
        assert processor != null;
        this.workspace = workspace;
        this.pathsIndexDirectory = workspace.pathDirectory;
        this.contentIndexDirectory = workspace.contentDirectory;
        this.processor = processor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#getWorkspaceName()
     */
    public String getWorkspaceName() {
        return workspace.getWorkspaceName();
    }

    /**
     * @return workspace
     */
    public LuceneSearchWorkspace getWorkspace() {
        return workspace;
    }

    protected IndexReader getPathsReader() throws IOException {
        if (pathsReader == null) {
            pathsReader = IndexReader.open(pathsIndexDirectory, processor.readOnly);
        }
        return pathsReader;
    }

    protected IndexReader getContentReader() throws IOException {
        if (contentReader == null) {
            contentReader = IndexReader.open(contentIndexDirectory, processor.readOnly);
        }
        return contentReader;
    }

    protected IndexWriter getPathsWriter() throws IOException {
        assert !processor.readOnly;
        if (pathsWriter == null) {
            // Don't overwrite, but create if missing ...
            pathsWriter = new IndexWriter(pathsIndexDirectory, workspace.analyzer, MaxFieldLength.UNLIMITED);
        }
        return pathsWriter;
    }

    protected IndexWriter getContentWriter() throws IOException {
        assert !processor.readOnly;
        if (contentWriter == null) {
            // Don't overwrite, but create if missing ...
            contentWriter = new IndexWriter(contentIndexDirectory, workspace.analyzer, MaxFieldLength.UNLIMITED);
        }
        return contentWriter;
    }

    protected IndexSearcher getPathsSearcher() throws IOException {
        if (pathsSearcher == null) {
            pathsSearcher = new IndexSearcher(getPathsReader());
        }
        return pathsSearcher;
    }

    public IndexSearcher getContentSearcher() throws IOException {
        if (contentSearcher == null) {
            contentSearcher = new IndexSearcher(getContentReader());
        }
        return contentSearcher;
    }

    public boolean hasWriters() {
        return pathsWriter != null || contentWriter != null;
    }

    public boolean optimize() throws IOException {
        getContentWriter().optimize();
        getPathsWriter().optimize();
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#commit()
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
            String msg = LuceneI18n.errorWhileCommittingIndexChanges.text(workspace.getWorkspaceName(),
                                                                          processor.getSourceName(),
                                                                          ioError.getMessage());
            throw new LuceneException(msg, ioError);
        }
        if (runtimeError != null) throw runtimeError;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#rollback()
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
            String msg = LuceneI18n.errorWhileRollingBackIndexChanges.text(workspace.getWorkspaceName(),
                                                                           processor.getSourceName(),
                                                                           ioError.getMessage());
            throw new LuceneException(msg, ioError);
        }
        if (runtimeError != null) throw runtimeError;
    }

    protected Statistics search( String fullTextSearchExpression,
                                 List<Object[]> results,
                                 int maxRows,
                                 int offset ) throws ParseException, IOException {
        // Parse the full-text search and search against the 'fts' field ...
        long planningNanos = System.nanoTime();
        QueryParser parser = new QueryParser(Version.LUCENE_29, ContentIndex.FULL_TEXT, workspace.analyzer);
        Query query = parser.parse(fullTextSearchExpression);
        planningNanos = System.nanoTime() - planningNanos;
        TopDocs docs = getContentSearcher().search(query, maxRows + offset);

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
                Document doc = contentReader.document(docId, DOC_ID_FIELD_SELECTOR);
                String id = doc.get(ContentIndex.ID);
                Document pathDoc = getPathDocument(id, pathReader, pathSearcher, LOCATION_FIELDS_SELECTOR);
                Location location = readLocation(pathDoc);
                if (location == null) {
                    // No path record found ...
                    continue;
                }
                // Now add the location ...
                results.add(new Object[] {location, result.score});
            }
        }
        long executionNanos = System.nanoTime() - planningNanos;
        return new Statistics(planningNanos, 0L, 0L, executionNanos);
    }

    protected Location readLocation( Document doc ) {
        // Read the path ...
        String pathString = doc.get(PathIndex.PATH);
        Path path = processor.pathFactory.create(pathString);
        // Look for the Location's ID properties ...
        String[] idProps = doc.getValues(PathIndex.LOCATION_ID_PROPERTIES);
        if (idProps.length == 0) {
            return Location.create(path);
        }
        if (idProps.length == 1) {
            Property idProp = processor.deserializeProperty(idProps[0]);
            if (idProp == null) return Location.create(path);
            if (idProp.isSingle() && (idProp.getName().equals(JcrLexicon.UUID) || idProp.getName().equals(DnaLexicon.UUID))) {
                return Location.create(path, (UUID)idProp.getFirstValue()); // know that deserialize returns UUID value
            }
            return Location.create(path, idProp);
        }
        List<Property> properties = new LinkedList<Property>();
        for (String idProp : idProps) {
            Property prop = processor.deserializeProperty(idProp);
            if (prop != null) properties.add(prop);
        }
        return properties.isEmpty() ? Location.create(path) : Location.create(path, properties);
    }

    protected void setOrReplaceProperties( String idString,
                                           Iterable<Property> properties ) throws IOException {
        // Create the document for the content (properties) ...
        Document doc = new Document();
        doc.add(new Field(ContentIndex.ID, idString, Field.Store.YES, Field.Index.NOT_ANALYZED));
        String stringValue = null;
        StringBuilder fullTextSearchValue = null;
        for (Property property : properties) {
            Name name = property.getName();
            Rule rule = workspace.rules.getRule(name);
            if (rule.isSkipped()) continue;
            String nameString = processor.stringFactory.create(name);
            FieldType type = rule.getType();
            if (type == FieldType.DATE) {
                boolean index = rule.getIndexOption() != Field.Index.NO;
                for (Object value : property) {
                    if (value == null) continue;
                    // Add a separate field for each property value ...
                    DateTime dateValue = processor.dateFactory.create(value);
                    long longValue = dateValue.getMillisecondsInUtc();
                    doc.add(new NumericField(nameString, rule.getStoreOption(), index).setLongValue(longValue));
                }
                continue;
            }
            if (type == FieldType.INT) {
                ValueFactory<Long> longFactory = processor.valueFactories.getLongFactory();
                boolean index = rule.getIndexOption() != Field.Index.NO;
                for (Object value : property) {
                    if (value == null) continue;
                    // Add a separate field for each property value ...
                    int intValue = longFactory.create(value).intValue();
                    doc.add(new NumericField(nameString, rule.getStoreOption(), index).setIntValue(intValue));
                }
                continue;
            }
            if (type == FieldType.DOUBLE) {
                ValueFactory<Double> doubleFactory = processor.valueFactories.getDoubleFactory();
                boolean index = rule.getIndexOption() != Field.Index.NO;
                for (Object value : property) {
                    if (value == null) continue;
                    // Add a separate field for each property value ...
                    double dValue = doubleFactory.create(value);
                    doc.add(new NumericField(nameString, rule.getStoreOption(), index).setDoubleValue(dValue));
                }
                continue;
            }
            if (type == FieldType.FLOAT) {
                ValueFactory<Double> doubleFactory = processor.valueFactories.getDoubleFactory();
                boolean index = rule.getIndexOption() != Field.Index.NO;
                for (Object value : property) {
                    if (value == null) continue;
                    // Add a separate field for each property value ...
                    float fValue = doubleFactory.create(value).floatValue();
                    doc.add(new NumericField(nameString, rule.getStoreOption(), index).setFloatValue(fValue));
                }
                continue;
            }
            if (type == FieldType.BINARY) {
                // TODO : add to full-text search ...
                continue;
            }
            assert type == FieldType.STRING;
            for (Object value : property) {
                if (value == null) continue;
                stringValue = processor.stringFactory.create(value);
                // Add a separate field for each property value ...
                doc.add(new Field(nameString, stringValue, rule.getStoreOption(), rule.getIndexOption()));

                if (rule.getIndexOption() != Field.Index.NO) {
                    // This field is to be full-text searchable ...
                    if (fullTextSearchValue == null) {
                        fullTextSearchValue = new StringBuilder();
                    } else {
                        fullTextSearchValue.append(' ');
                    }
                    fullTextSearchValue.append(stringValue);

                    // Also create a full-text-searchable field ...
                    String fullTextNameString = processor.fullTextFieldName(nameString);
                    doc.add(new Field(fullTextNameString, stringValue, Store.NO, Index.ANALYZED));
                }
            }
        }
        // Add the full-text-search field ...
        if (fullTextSearchValue != null && fullTextSearchValue.length() != 0) {
            doc.add(new Field(ContentIndex.FULL_TEXT, fullTextSearchValue.toString(), Field.Store.NO, Field.Index.ANALYZED));
        }
        getContentWriter().addDocument(doc);
    }

    protected Document getPathDocument( String id,
                                        IndexReader pathReader,
                                        IndexSearcher pathSearcher,
                                        FieldSelector selector ) throws IOException {
        // Find the path for this node (is there a better way to do this than one search per ID?) ...
        TopDocs pathDocs = pathSearcher.search(new TermQuery(new Term(PathIndex.ID, id)), 1);
        if (pathDocs.scoreDocs.length < 1) {
            // No path record found ...
            return null;
        }
        return pathReader.document(pathDocs.scoreDocs[0].doc, selector);
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
        String stringifiedPath = processor.pathAsString(parentPath);
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
     * @param includeParent true if the parent node should be included in the results, or false if only the descendants should be
     *        included
     * @return the IDs of the nodes; never null but possibly empty
     * @throws IOException if there is an error accessing the indexes
     */
    protected Set<String> getIdsForDescendantsOf( Path parentPath,
                                                  boolean includeParent ) throws IOException {
        assert !parentPath.isRoot();

        // Find the path of the parent ...
        String stringifiedPath = processor.pathAsString(parentPath);
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
        String stringifiedPath = processor.pathAsString(path);
        TermQuery query = new TermQuery(new Term(PathIndex.PATH, stringifiedPath));

        // Now execute and collect the UUIDs ...
        TopDocs topDocs = searcher.search(query, 1);
        if (topDocs.totalHits == 0) return null;
        Document pathDoc = getPathsReader().document(topDocs.scoreDocs[0].doc);
        String idString = pathDoc.get(PathIndex.ID);
        assert idString != null;
        return idString;
    }

    protected Location getLocationFor( Path path ) throws IOException {
        // Create a query to find all the nodes below the parent path ...
        IndexSearcher searcher = getPathsSearcher();
        String stringifiedPath = processor.pathAsString(path);
        TermQuery query = new TermQuery(new Term(PathIndex.PATH, stringifiedPath));

        // Now execute and collect the UUIDs ...
        TopDocs topDocs = searcher.search(query, 1);
        if (topDocs.totalHits == 0) return null;
        Document pathDoc = getPathsReader().document(topDocs.scoreDocs[0].doc);
        return readLocation(pathDoc);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#createTupleCollector(org.jboss.dna.graph.query.QueryResults.Columns)
     */
    public TupleCollector createTupleCollector( Columns columns ) {
        return new DualIndexTupleCollector(this, columns);
    }

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
    public Query findChildNodes( Path parentPath ) throws IOException {
        if (parentPath.isRoot()) {
            return new MatchAllDocsQuery();
        }
        Set<String> childIds = getIdsForChildrenOf(parentPath);
        return findAllNodesWithIds(childIds);
    }

    /**
     * Create a query that can be used to find the one document (or node) that exists at the exact path supplied. This method
     * first queries the {@link PathIndex path index} to find the ID of the node at the supplied path, and then returns a query
     * that matches the ID.
     * 
     * @param path the path of the node
     * @return the query; never null
     * @throws IOException if there is an error finding the ID for the supplied path
     */
    public Query findNodeAt( Path path ) throws IOException {
        String id = getIdFor(path);
        if (id == null) return null;
        return new TermQuery(new Term(ContentIndex.ID, id));
    }

    public Query findNodesLike( String fieldName,
                                String likeExpression,
                                boolean caseSensitive ) {
        ValueFactories factories = processor.valueFactories;
        return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression, fieldName, factories, caseSensitive);
    }

    public Query findNodesWith( Length propertyLength,
                                Operator operator,
                                Object value ) {
        assert propertyLength != null;
        assert value != null;
        PropertyValue propertyValue = propertyLength.getPropertyValue();
        String field = processor.stringFactory.create(propertyValue.getPropertyName());
        ValueFactories factories = processor.valueFactories;
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

    @SuppressWarnings( "unchecked" )
    public Query findNodesWith( PropertyValue propertyValue,
                                Operator operator,
                                Object value,
                                boolean caseSensitive ) {
        ValueFactory<String> stringFactory = processor.stringFactory;
        String field = stringFactory.create(propertyValue.getPropertyName());
        Name fieldName = processor.nameFactory.create(propertyValue.getPropertyName());
        ValueFactories factories = processor.valueFactories;
        IndexRules.Rule rule = workspace.rules.getRule(fieldName);
        if (rule == null || rule.isSkipped()) return new MatchNoneQuery();
        FieldType type = rule.getType();
        switch (type) {
            case STRING:
                String stringValue = stringFactory.create(value);
                if (value instanceof Path) {
                    stringValue = processor.pathAsString((Path)value);
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
                NumericRule<Long> longRule = (NumericRule<Long>)rule;
                long date = factories.getLongFactory().create(value);
                switch (operator) {
                    case EQUAL_TO:
                        return NumericRangeQuery.newLongRange(field, date, date, true, true);
                    case NOT_EQUAL_TO:
                        Query query = NumericRangeQuery.newLongRange(field, date, date, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        return NumericRangeQuery.newLongRange(field, date, longRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newLongRange(field, date, longRule.getMaximum(), true, true);
                    case LESS_THAN:
                        return NumericRangeQuery.newLongRange(field, longRule.getMinimum(), date, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newLongRange(field, longRule.getMinimum(), date, true, true);
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case LONG:
                longRule = (NumericRule<Long>)rule;
                long longValue = factories.getLongFactory().create(value);
                switch (operator) {
                    case EQUAL_TO:
                        return NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                    case NOT_EQUAL_TO:
                        Query query = NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        return NumericRangeQuery.newLongRange(field, longValue, longRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newLongRange(field, longValue, longRule.getMaximum(), true, true);
                    case LESS_THAN:
                        return NumericRangeQuery.newLongRange(field, longRule.getMinimum(), longValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newLongRange(field, longRule.getMinimum(), longValue, true, true);
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case INT:
                NumericRule<Integer> intRule = (NumericRule<Integer>)rule;
                int intValue = factories.getLongFactory().create(value).intValue();
                switch (operator) {
                    case EQUAL_TO:
                        return NumericRangeQuery.newIntRange(field, intValue, intValue, true, true);
                    case NOT_EQUAL_TO:
                        Query query = NumericRangeQuery.newIntRange(field, intValue, intValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        return NumericRangeQuery.newIntRange(field, intValue, intRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newIntRange(field, intValue, intRule.getMaximum(), true, true);
                    case LESS_THAN:
                        return NumericRangeQuery.newIntRange(field, intRule.getMinimum(), intValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newIntRange(field, intRule.getMinimum(), intValue, true, true);
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case DOUBLE:
                NumericRule<Double> dRule = (NumericRule<Double>)rule;
                double doubleValue = factories.getDoubleFactory().create(value);
                switch (operator) {
                    case EQUAL_TO:
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                    case NOT_EQUAL_TO:
                        Query query = NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, dRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, dRule.getMaximum(), true, true);
                    case LESS_THAN:
                        return NumericRangeQuery.newDoubleRange(field, dRule.getMinimum(), doubleValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newDoubleRange(field, dRule.getMinimum(), doubleValue, true, true);
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case FLOAT:
                NumericRule<Float> fRule = (NumericRule<Float>)rule;
                float floatValue = factories.getDoubleFactory().create(value).floatValue();
                switch (operator) {
                    case EQUAL_TO:
                        return NumericRangeQuery.newFloatRange(field, floatValue, floatValue, true, true);
                    case NOT_EQUAL_TO:
                        Query query = NumericRangeQuery.newFloatRange(field, floatValue, floatValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        return NumericRangeQuery.newFloatRange(field, floatValue, fRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newFloatRange(field, floatValue, fRule.getMaximum(), true, true);
                    case LESS_THAN:
                        return NumericRangeQuery.newFloatRange(field, fRule.getMinimum(), floatValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newFloatRange(field, fRule.getMinimum(), floatValue, true, true);
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
            case BINARY:
                // This is not allowed ...
                assert false;
                return null;
        }
        return null;
    }

    public Query findNodesWithNumericRange( PropertyValue propertyValue,
                                            Object lowerValue,
                                            Object upperValue,
                                            boolean includesLower,
                                            boolean includesUpper ) {
        String field = processor.stringFactory.create(propertyValue.getPropertyName());
        return findNodesWithNumericRange(field, lowerValue, upperValue, includesLower, includesUpper);
    }

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
        Name fieldName = processor.nameFactory.create(field);
        IndexRules.Rule rule = workspace.rules.getRule(fieldName);
        if (rule == null || rule.isSkipped()) return new MatchNoneQuery();
        FieldType type = rule.getType();
        ValueFactories factories = processor.valueFactories;
        switch (type) {
            case DATE:
                long lowerDate = factories.getLongFactory().create(lowerValue);
                long upperDate = factories.getLongFactory().create(upperValue);
                return NumericRangeQuery.newLongRange(field, lowerDate, upperDate, includesLower, includesUpper);
            case LONG:
                long lowerLong = factories.getLongFactory().create(lowerValue);
                long upperLong = factories.getLongFactory().create(upperValue);
                return NumericRangeQuery.newLongRange(field, lowerLong, upperLong, includesLower, includesUpper);
            case DOUBLE:
                double lowerDouble = factories.getDoubleFactory().create(lowerValue);
                double upperDouble = factories.getDoubleFactory().create(upperValue);
                return NumericRangeQuery.newDoubleRange(field, lowerDouble, upperDouble, includesLower, includesUpper);
            case FLOAT:
                float lowerFloat = factories.getDoubleFactory().create(lowerValue).floatValue();
                float upperFloat = factories.getDoubleFactory().create(upperValue).floatValue();
                return NumericRangeQuery.newFloatRange(field, lowerFloat, upperFloat, includesLower, includesUpper);
            case INT:
                int lowerInt = factories.getLongFactory().create(lowerValue).intValue();
                int upperInt = factories.getLongFactory().create(upperValue).intValue();
                return NumericRangeQuery.newIntRange(field, lowerInt, upperInt, includesLower, includesUpper);
            case BOOLEAN:
                lowerInt = factories.getBooleanFactory().create(lowerValue).booleanValue() ? 1 : 0;
                upperInt = factories.getBooleanFactory().create(upperValue).booleanValue() ? 1 : 0;
                return NumericRangeQuery.newIntRange(field, lowerInt, upperInt, includesLower, includesUpper);
            case STRING:
            case BINARY:
                assert false;
        }
        return new MatchNoneQuery();
    }

    public Query findNodesWith( NodePath nodePath,
                                Operator operator,
                                Object value,
                                boolean caseSensitive ) throws IOException {
        if (!caseSensitive) value = processor.stringFactory.create(value).toLowerCase();
        Path pathValue = operator != Operator.LIKE ? processor.pathFactory.create(value) : null;
        Query query = null;
        switch (operator) {
            case EQUAL_TO:
                return findNodeAt(pathValue);
            case NOT_EQUAL_TO:
                return new NotQuery(findNodeAt(pathValue));
            case LIKE:
                String likeExpression = processor.stringFactory.create(value);
                query = findNodesLike(PathIndex.PATH, likeExpression, caseSensitive);
                break;
            case GREATER_THAN:
                query = ComparePathQuery.createQueryForNodesWithPathGreaterThan(pathValue,
                                                                                PathIndex.PATH,
                                                                                processor.valueFactories,
                                                                                caseSensitive);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = ComparePathQuery.createQueryForNodesWithPathGreaterThanOrEqualTo(pathValue,
                                                                                         PathIndex.PATH,
                                                                                         processor.valueFactories,
                                                                                         caseSensitive);
                break;
            case LESS_THAN:
                query = ComparePathQuery.createQueryForNodesWithPathLessThan(pathValue,
                                                                             PathIndex.PATH,
                                                                             processor.valueFactories,
                                                                             caseSensitive);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = ComparePathQuery.createQueryForNodesWithPathLessThanOrEqualTo(pathValue,
                                                                                      PathIndex.PATH,
                                                                                      processor.valueFactories,
                                                                                      caseSensitive);
                break;
        }
        // Now execute and collect the IDs ...
        IdCollector idCollector = new IdCollector();
        IndexSearcher searcher = getPathsSearcher();
        searcher.search(query, idCollector);
        return findAllNodesWithIds(idCollector.getIds());
    }

    public Query findNodesWith( NodeName nodeName,
                                Operator operator,
                                Object value,
                                boolean caseSensitive ) throws IOException {
        ValueFactories factories = processor.valueFactories;
        String stringValue = processor.stringFactory.create(value);
        if (!caseSensitive) stringValue = stringValue.toLowerCase();
        Path.Segment segment = operator != Operator.LIKE ? processor.pathFactory.createSegment(stringValue) : null;
        int snsIndex = operator != Operator.LIKE ? segment.getIndex() : 0;
        Query query = null;
        switch (operator) {
            case EQUAL_TO:
                BooleanQuery booleanQuery = new BooleanQuery();
                booleanQuery.add(new TermQuery(new Term(PathIndex.NODE_NAME, stringValue)), Occur.MUST);
                booleanQuery.add(NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, snsIndex, snsIndex, true, false), Occur.MUST);
                return booleanQuery;
            case NOT_EQUAL_TO:
                booleanQuery = new BooleanQuery();
                booleanQuery.add(new TermQuery(new Term(PathIndex.NODE_NAME, stringValue)), Occur.MUST);
                booleanQuery.add(NumericRangeQuery.newIntRange(PathIndex.SNS_INDEX, snsIndex, snsIndex, true, false), Occur.MUST);
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

    public Query findNodesWith( NodeLocalName nodeName,
                                Operator operator,
                                Object value,
                                boolean caseSensitive ) throws IOException {
        String nameValue = processor.stringFactory.create(value);
        Query query = null;
        switch (operator) {
            case LIKE:
                String likeExpression = processor.stringFactory.create(value);
                query = findNodesLike(PathIndex.LOCAL_NAME, likeExpression, caseSensitive);
                break;
            case EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                               PathIndex.LOCAL_NAME,
                                                                               processor.valueFactories,
                                                                               caseSensitive);
                break;
            case NOT_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                               PathIndex.LOCAL_NAME,
                                                                               processor.valueFactories,
                                                                               caseSensitive);
                query = new NotQuery(query);
                break;
            case GREATER_THAN:
                query = CompareStringQuery.createQueryForNodesWithFieldGreaterThan(nameValue,
                                                                                   PathIndex.LOCAL_NAME,
                                                                                   processor.valueFactories,
                                                                                   caseSensitive);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(nameValue,
                                                                                            PathIndex.LOCAL_NAME,
                                                                                            processor.valueFactories,
                                                                                            caseSensitive);
                break;
            case LESS_THAN:
                query = CompareStringQuery.createQueryForNodesWithFieldLessThan(nameValue,
                                                                                PathIndex.LOCAL_NAME,
                                                                                processor.valueFactories,
                                                                                caseSensitive);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(nameValue,
                                                                                         PathIndex.LOCAL_NAME,
                                                                                         processor.valueFactories,
                                                                                         caseSensitive);
                break;
        }

        // Now execute and collect the IDs ...
        IdCollector idCollector = new IdCollector();
        IndexSearcher searcher = getPathsSearcher();
        searcher.search(query, idCollector);
        return findAllNodesWithIds(idCollector.getIds());
    }

    public Query findNodesWith( NodeDepth depthConstraint,
                                Operator operator,
                                Object value ) throws IOException {
        int depth = processor.valueFactories.getLongFactory().create(value).intValue();
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
        return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression,
                                                                   PathIndex.LOCAL_NAME,
                                                                   processor.valueFactories,
                                                                   caseSensitive);
    }

    /**
     * Utility method to generate a query against the SNS indexes. This method attempts to generate a query that works most
     * efficiently, depending upon the supplied expression. For example, if the supplied expression is just "[3]", then a range
     * query is used to find all values matching '3'. However, if "[3_]" is used (where '_' matches any single-character, or digit
     * in this case), then a range query is used to find all values between '30' and '39'. Similarly, if "[3%]" is used, then a
     * regular expression query is used.
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

    /**
     * A {@link Collector} implementation that only captures the UUID of the documents returned by a query. Score information is
     * not recorded. This is often used when querying the {@link PathIndex} to collect the UUIDs of a set of nodes satisfying some
     * path constraint.
     * 
     * @see LuceneSearchSession#findChildNodes(Path)
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
    protected static class DualIndexTupleCollector extends TupleCollector {
        private final LuceneSearchSession session;
        private final LuceneSearchProcessor processor;
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

        protected DualIndexTupleCollector( LuceneSearchSession session,
                                           Columns columns ) {
            this.session = session;
            this.processor = session.processor;
            this.columns = columns;
            assert this.processor != null;
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
                    Location location = getLocationForDocument(id, pathReader, pathSearcher);
                    if (location == null) continue;
                    tuple[locationIndex] = location;
                }
                resolvedLocations = true;
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
            return session.readLocation(pathDoc);
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
