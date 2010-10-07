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
package org.modeshape.search.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
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
import org.apache.lucene.search.regex.JavaUtilRegexCapabilities;
import org.apache.lucene.search.regex.RegexQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeIntLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.ModeShapeIntLexicon.Namespace;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.basic.BasicName;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.search.lucene.AbstractLuceneSearchEngine.TupleCollector;
import org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession;
import org.modeshape.search.lucene.IndexRules.FieldType;
import org.modeshape.search.lucene.IndexRules.NumericRule;
import org.modeshape.search.lucene.IndexRules.Rule;
import org.modeshape.search.lucene.LuceneSearchWorkspace.ContentIndex;
import org.modeshape.search.lucene.query.CompareLengthQuery;
import org.modeshape.search.lucene.query.CompareNameQuery;
import org.modeshape.search.lucene.query.ComparePathQuery;
import org.modeshape.search.lucene.query.CompareStringQuery;
import org.modeshape.search.lucene.query.MatchNoneQuery;
import org.modeshape.search.lucene.query.NotQuery;

/**
 * The {@link WorkspaceSession} implementation for the {@link LuceneSearchEngine}.
 */
@NotThreadSafe
public class LuceneSearchSession implements WorkspaceSession {

    protected static final Set<Name> NON_SEARCHABLE_NAMES = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                          Arrays.asList(JcrLexicon.UUID,
                                                                                                                        ModeShapeLexicon.UUID,
                                                                                                                        JcrLexicon.PRIMARY_TYPE,
                                                                                                                        JcrLexicon.MIXIN_TYPES,
                                                                                                                        ModeShapeIntLexicon.NODE_DEFINITON,
                                                                                                                        new BasicName(
                                                                                                                                      Namespace.URI,
                                                                                                                                      "multiValuedProperties"))));

    /**
     * An immutable {@link FieldSelector} instance that accesses the UUID field.
     */
    protected static final FieldSelector LOCATION_FIELDS_SELECTOR = new FieldSelector() {
        private static final long serialVersionUID = 1L;

        public FieldSelectorResult accept( String fieldName ) {
            if (ContentIndex.PATH.equals(fieldName) || ContentIndex.LOCATION_ID_PROPERTIES.equals(fieldName)) {
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
    private final Directory contentIndexDirectory;
    private IndexReader contentReader;
    private IndexWriter contentWriter;
    private IndexSearcher contentSearcher;
    private int numChanges;
    private final Logger logger = Logger.getLogger(getClass());

    protected LuceneSearchSession( LuceneSearchWorkspace workspace,
                                   LuceneSearchProcessor processor ) {
        assert workspace != null;
        assert processor != null;
        this.workspace = workspace;
        this.contentIndexDirectory = workspace.contentDirectory;
        this.processor = processor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#getWorkspaceName()
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

    protected IndexReader getContentReader() throws IOException {
        if (contentReader == null) {
            try {
                contentReader = IndexReader.open(contentIndexDirectory, processor.readOnly);
            } catch (IOException e) {
                // try creating the workspace ...
                IndexWriter writer = new IndexWriter(contentIndexDirectory, workspace.analyzer, MaxFieldLength.UNLIMITED);
                writer.close();
                // And try reading again ...
                contentReader = IndexReader.open(contentIndexDirectory, processor.readOnly);
            }
        }
        return contentReader;
    }

    protected IndexWriter getContentWriter() throws IOException {
        assert !processor.readOnly;
        if (contentWriter == null) {
            // Don't overwrite, but create if missing ...
            contentWriter = new IndexWriter(contentIndexDirectory, workspace.analyzer, MaxFieldLength.UNLIMITED);
        }
        return contentWriter;
    }

    public IndexSearcher getContentSearcher() throws IOException {
        if (contentSearcher == null) {
            contentSearcher = new IndexSearcher(getContentReader());
        }
        return contentSearcher;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#getAnalyzer()
     */
    public Analyzer getAnalyzer() {
        return workspace.analyzer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#getVersion()
     */
    @Override
    public Version getVersion() {
        return workspace.getVersion();
    }

    public boolean hasWriters() {
        return contentWriter != null;
    }

    protected final void recordChange() {
        ++numChanges;
    }

    protected final void recordChanges( int numberOfChanges ) {
        assert numberOfChanges >= 0;
        numChanges += numberOfChanges;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#getChangeCount()
     */
    public final int getChangeCount() {
        return numChanges;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#commit()
     */
    public void commit() {
        if (logger.isTraceEnabled() && numChanges > 0) {
            logger.trace("index for \"{0}\" workspace: COMMIT", workspace.getWorkspaceName());
        }

        // Is optimization required ...
        final boolean optimize = workspace.isOptimizationRequired(numChanges);
        numChanges = 0;

        IOException ioError = null;
        RuntimeException runtimeError = null;
        if (contentReader != null) {
            try {
                contentReader.close();
            } catch (IOException e) {
                ioError = e;
            } catch (RuntimeException e) {
                runtimeError = e;
            } finally {
                contentReader = null;
            }
        }
        if (contentWriter != null) {
            try {
                if (optimize) contentWriter.optimize();
            } catch (IOException e) {
                if (ioError == null) ioError = e;
            } catch (RuntimeException e) {
                if (runtimeError == null) runtimeError = e;
            } finally {
                try {
                    contentWriter.close();
                } catch (IOException e) {
                    if (ioError == null) ioError = e;
                } catch (RuntimeException e) {
                    if (runtimeError == null) runtimeError = e;
                } finally {
                    contentWriter = null;
                }
            }
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
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#rollback()
     */
    public void rollback() {
        if (logger.isTraceEnabled() && numChanges > 0) {
            logger.trace("index for \"{0}\" workspace: ROLLBACK", workspace.getWorkspaceName());
        }
        numChanges = 0;
        IOException ioError = null;
        RuntimeException runtimeError = null;
        if (contentReader != null) {
            try {
                contentReader.close();
            } catch (IOException e) {
                ioError = e;
            } catch (RuntimeException e) {
                runtimeError = e;
            } finally {
                contentReader = null;
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
        QueryParser parser = new QueryParser(workspace.getVersion(), ContentIndex.FULL_TEXT, workspace.analyzer);
        Query query = parser.parse(fullTextSearchExpression);
        planningNanos = System.nanoTime() - planningNanos;
        if (logger.isTraceEnabled()) {
            logger.trace("search \"{0}\" workspace using {1}", workspace.getWorkspaceName(), query);
        }

        // Execute the search and place the results into the supplied list ...
        TopDocs docs = getContentSearcher().search(query, maxRows + offset);
        IndexReader contentReader = getContentReader();
        ScoreDoc[] scoreDocs = docs.scoreDocs;
        int numberOfResults = scoreDocs.length;
        if (numberOfResults > offset) {
            // There are enough results to satisfy the offset ...
            for (int i = offset, num = scoreDocs.length; i != num; ++i) {
                ScoreDoc result = scoreDocs[i];
                int docId = result.doc;
                // Find the UUID of the node (this UUID might be artificial, so we have to find the path) ...
                Document doc = contentReader.document(docId, LOCATION_FIELDS_SELECTOR);
                Location location = readLocation(doc);
                // Now add the location ...
                results.add(new Object[] {location, result.score});
            }
        }
        long executionNanos = System.nanoTime() - planningNanos;
        return new Statistics(planningNanos, 0L, 0L, executionNanos);
    }

    protected Location readLocation( Document doc ) {
        // Read the path ...
        String pathString = doc.get(ContentIndex.PATH);
        Path path = processor.pathFactory.create(pathString);
        // Look for the Location's ID properties ...
        String[] idProps = doc.getValues(ContentIndex.LOCATION_ID_PROPERTIES);
        if (idProps.length == 0) {
            return Location.create(path);
        }
        if (idProps.length == 1) {
            Property idProp = processor.deserializeProperty(idProps[0]);
            if (idProp == null) return Location.create(path);
            if (idProp.isSingle() && (idProp.getName().equals(JcrLexicon.UUID) || idProp.getName().equals(ModeShapeLexicon.UUID))) {
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

    protected void setOrReplaceProperties( Location location,
                                           Iterable<Property> properties ) throws IOException {
        // Create the document for the content (properties) ...
        Document doc = new Document();

        // Add the information every node has ...
        Path path = location.getPath();
        String pathStr = processor.pathAsString(path);
        String nameStr = path.isRoot() ? "" : processor.stringFactory.create(path.getLastSegment().getName());
        String localNameStr = path.isRoot() ? "" : path.getLastSegment().getName().getLocalName();
        int sns = path.isRoot() ? 1 : path.getLastSegment().getIndex();

        // Create a separate document for the path, which makes it easier to handle moves since the path can
        // be changed without changing any other content fields ...
        doc.add(new Field(ContentIndex.PATH, pathStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(ContentIndex.NODE_NAME, nameStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(ContentIndex.LOCAL_NAME, localNameStr, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new NumericField(ContentIndex.SNS_INDEX, Field.Store.YES, true).setIntValue(sns));
        doc.add(new NumericField(ContentIndex.DEPTH, Field.Store.YES, true).setIntValue(path.size()));
        if (location.hasIdProperties()) {
            for (Property idProp : location.getIdProperties()) {
                String fieldValue = processor.serializeProperty(idProp);
                doc.add(new Field(ContentIndex.LOCATION_ID_PROPERTIES, fieldValue, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
        }

        // Always include the local name in the full-text search field ...
        StringBuilder fullTextSearchValue = new StringBuilder();
        fullTextSearchValue.append(localNameStr);

        // Index the properties
        String stringValue = null;
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
            if (type == FieldType.DECIMAL) {
                ValueFactory<BigDecimal> decimalFactory = processor.valueFactories.getDecimalFactory();
                for (Object value : property) {
                    if (value == null) continue;
                    BigDecimal decimal = decimalFactory.create(value);
                    // Convert to a string that is lexicographically sortable ...
                    value = FieldUtil.decimalToString(decimal);
                    doc.add(new Field(nameString, stringValue, rule.getStoreOption(), Field.Index.NOT_ANALYZED));
                }
                continue;
            }
            if (type == FieldType.BINARY) {
                // TODO : add to full-text search ...
                continue;
            }
            if (type == FieldType.WEAK_REFERENCE) {
                ValueFactory<Path> pathFactory = processor.valueFactories.getPathFactory();
                for (Object value : property) {
                    if (value == null) continue;
                    // Add a separate field for each property value ...
                    String valueStr = processor.stringFactory.create(pathFactory.create(value));
                    doc.add(new Field(nameString, valueStr, rule.getStoreOption(), Field.Index.NOT_ANALYZED));
                    // Add a value to the common reference value ...
                    doc.add(new Field(ContentIndex.REFERENCES, stringValue, Field.Store.NO, Field.Index.NOT_ANALYZED));
                }
                continue;
            }
            if (type == FieldType.REFERENCE) {
                for (Object value : property) {
                    if (value == null) continue;
                    // Obtain the string value of the reference (i.e., should be the string of the UUID) ...
                    stringValue = processor.stringFactory.create(value);
                    // Add a separate field for each property value in exact form (not analyzed) ...
                    doc.add(new Field(nameString, stringValue, rule.getStoreOption(), Field.Index.NOT_ANALYZED));
                    // Add a value to the common reference value ...
                    doc.add(new Field(ContentIndex.REFERENCES, stringValue, Field.Store.NO, Field.Index.NOT_ANALYZED));
                    // Add a value to the strong reference value ...
                    doc.add(new Field(ContentIndex.STRONG_REFERENCES, stringValue, Field.Store.NO, Field.Index.NOT_ANALYZED));
                }
                continue;
            }
            if (type == FieldType.BOOLEAN) {
                ValueFactory<Boolean> booleanFactory = processor.valueFactories.getBooleanFactory();
                boolean index = rule.getIndexOption() != Field.Index.NO;
                for (Object value : property) {
                    if (value == null) continue;
                    // Add a separate field for each property value ...
                    int intValue = booleanFactory.create(value).booleanValue() ? 1 : 0;
                    doc.add(new NumericField(nameString, rule.getStoreOption(), index).setIntValue(intValue));
                }
                continue;
            }
            assert type == FieldType.STRING;
            for (Object value : property) {
                if (value == null) continue;
                stringValue = processor.stringFactory.create(value);
                // Add a separate field for each property value in exact form (not analyzed) ...
                doc.add(new Field(nameString, stringValue, rule.getStoreOption(), Field.Index.NOT_ANALYZED));

                boolean treatedAsReference = false;
                if (value instanceof Reference) {
                    Reference ref = (Reference)value;
                    doc.add(new Field(ContentIndex.REFERENCES, stringValue, Field.Store.YES, Field.Index.NOT_ANALYZED));
                    if (!ref.isWeak()) {
                        doc.add(new Field(ContentIndex.STRONG_REFERENCES, stringValue, Field.Store.YES, Field.Index.NOT_ANALYZED));
                    }
                    treatedAsReference = true;
                } else if (rule.canBeReference()) {
                    if (stringValue.length() == 36 && stringValue.charAt(8) == '-') {
                        // The value looks like a string representation of a UUID ...
                        try {
                            UUID.fromString(stringValue);
                            // Add a value to the common reference value ...
                            treatedAsReference = true;
                            doc.add(new Field(ContentIndex.REFERENCES, stringValue, Field.Store.YES, Field.Index.NOT_ANALYZED));
                        } catch (IllegalArgumentException e) {
                            // Must not conform to the UUID format
                        }
                    }
                }
                if (!treatedAsReference && rule.getIndexOption() != Field.Index.NO && rule.isFullTextSearchable()
                    && !NON_SEARCHABLE_NAMES.contains(name)) {
                    // This field is to be full-text searchable ...
                    fullTextSearchValue.append(' ').append(stringValue);

                    // Also create a full-text-searchable field ...
                    String fullTextNameString = processor.fullTextFieldName(nameString);
                    doc.add(new Field(fullTextNameString, stringValue, Store.NO, Index.ANALYZED));
                }
            }
        }
        // Add the full-text-search field ...
        if (fullTextSearchValue.length() != 0) {
            doc.add(new Field(ContentIndex.FULL_TEXT, fullTextSearchValue.toString(), Field.Store.NO, Field.Index.ANALYZED));
        }
        if (logger.isTraceEnabled()) {
            logger.trace("index for \"{0}\" workspace: ADD {1} {2}", workspace.getWorkspaceName(), pathStr, doc);
            if (fullTextSearchValue.length() != 0) {

                // Run the expression through the Lucene analyzer to extract the terms ...
                String fullTextContent = fullTextSearchValue.toString();
                TokenStream stream = getAnalyzer().tokenStream(ContentIndex.FULL_TEXT, new StringReader(fullTextContent));
                TermAttribute term = stream.addAttribute(TermAttribute.class);
                // PositionIncrementAttribute positionIncrement = stream.addAttribute(PositionIncrementAttribute.class);
                // OffsetAttribute offset = stream.addAttribute(OffsetAttribute.class);
                // TypeAttribute type = stream.addAttribute(TypeAttribute.class);
                // int position = 0;
                StringBuilder output = new StringBuilder();
                while (stream.incrementToken()) {
                    output.append(term.term()).append(' ');
                    // // The term attribute object has been modified to contain the next term ...
                    // int incr = positionIncrement.getPositionIncrement();
                    // if (incr > 0) {
                    // position = position + incr;
                    // output.append(' ').append(position).append(':');
                    // }
                    // output.append('[')
                    // .append(term.term())
                    // .append(':')
                    // .append(offset.startOffset())
                    // .append("->")
                    // .append(offset.endOffset())
                    // .append(':')
                    // .append(type.type())
                    // .append(']');
                }
                logger.trace("index for \"{0}\" workspace:     {1} fts terms: {2}", workspace.getWorkspaceName(), pathStr, output);
            }
        }
        getContentWriter().updateDocument(new Term(ContentIndex.PATH, pathStr), doc);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#createTupleCollector(org.modeshape.graph.query.QueryResults.Columns)
     */
    public TupleCollector createTupleCollector( Columns columns ) {
        return new DualIndexTupleCollector(this, columns);
    }

    public Location getLocationForRoot() throws IOException {
        // Look for the root node ...
        Query query = NumericRangeQuery.newIntRange(ContentIndex.DEPTH, 0, 0, true, true);

        // Execute the search and place the results into the supplied list ...
        List<Object[]> tuples = new ArrayList<Object[]>(1);
        FullTextSearchTupleCollector collector = new FullTextSearchTupleCollector(this, tuples);
        getContentSearcher().search(query, collector);

        // Extract the location from the results ...
        return tuples.isEmpty() ? Location.create(processor.pathFactory.createRootPath()) : (Location)tuples.get(0)[0];
    }

    public Query findAllNodesBelow( Path parentPath ) {
        // Find the path of the parent ...
        String stringifiedPath = processor.pathAsString(parentPath);
        // Append a '/' to the parent path, and we'll only get decendants ...
        stringifiedPath = stringifiedPath + '/';

        // Create a prefix query ...
        return new PrefixQuery(new Term(ContentIndex.PATH, stringifiedPath));
    }

    public Query findAllNodesAtOrBelow( Path parentPath ) {
        if (parentPath.isRoot()) {
            return new MatchAllDocsQuery();
        }
        // Find the path of the parent ...
        String stringifiedPath = processor.pathAsString(parentPath);

        // Create a prefix query ...
        return new PrefixQuery(new Term(ContentIndex.PATH, stringifiedPath));
    }

    /**
     * Return a query that can be used to find all of the documents that represent nodes that are children of the node at the
     * supplied path.
     * 
     * @param parentPath the path of the parent node.
     * @return the query; never null
     */
    public Query findChildNodes( Path parentPath ) {
        // Find the path of the parent ...
        String stringifiedPath = processor.pathAsString(parentPath);
        // Append a '/' to the parent path, so we'll only get decendants ...
        stringifiedPath = stringifiedPath + '/';

        // Create a query to find all the nodes below the parent path ...
        Query query = new PrefixQuery(new Term(ContentIndex.PATH, stringifiedPath));
        // Include only the children ...
        int childrenDepth = parentPath.size() + 1;
        Query depthQuery = NumericRangeQuery.newIntRange(ContentIndex.DEPTH, childrenDepth, childrenDepth, true, true);
        // And combine ...
        BooleanQuery combinedQuery = new BooleanQuery();
        combinedQuery.add(query, Occur.MUST);
        combinedQuery.add(depthQuery, Occur.MUST);
        return combinedQuery;
    }

    /**
     * Create a query that can be used to find the one document (or node) that exists at the exact path supplied.
     * 
     * @param path the path of the node
     * @return the query; never null
     */
    public Query findNodeAt( Path path ) {
        if (path.isRoot()) {
            // Look for the root node ...
            return NumericRangeQuery.newIntRange(ContentIndex.DEPTH, 0, 0, true, true);
        }
        String stringifiedPath = processor.pathAsString(path);
        return new TermQuery(new Term(ContentIndex.PATH, stringifiedPath));
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
        PropertyValue propertyValue = propertyLength.propertyValue();
        String field = processor.stringFactory.create(propertyValue.propertyName());
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
        String field = stringFactory.create(propertyValue.propertyName());
        Name fieldName = processor.nameFactory.create(field);
        ValueFactories factories = processor.valueFactories;
        IndexRules.Rule rule = workspace.rules.getRule(fieldName);
        if (rule == null || rule.isSkipped()) return new MatchNoneQuery();
        FieldType type = rule.getType();
        switch (type) {
            case REFERENCE:
            case WEAK_REFERENCE:
            case DECIMAL: // stored in lexicographically-ordered form
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
                        if (date < longRule.getMinimum() || date > longRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, date, date, true, true);
                    case NOT_EQUAL_TO:
                        if (date < longRule.getMinimum() || date > longRule.getMaximum()) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newLongRange(field, date, date, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        if (date > longRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, date, longRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (date > longRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, date, longRule.getMaximum(), true, true);
                    case LESS_THAN:
                        if (date < longRule.getMinimum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longRule.getMinimum(), date, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (date < longRule.getMinimum()) return new MatchNoneQuery();
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
                        if (longValue < longRule.getMinimum() || longValue > longRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                    case NOT_EQUAL_TO:
                        if (longValue < longRule.getMinimum() || longValue > longRule.getMaximum()) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        if (longValue > longRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longValue, longRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (longValue > longRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longValue, longRule.getMaximum(), true, true);
                    case LESS_THAN:
                        if (longValue < longRule.getMinimum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longRule.getMinimum(), longValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (longValue < longRule.getMinimum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longRule.getMinimum(), longValue, true, true);
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
            case INT:
                NumericRule<Integer> intRule = (NumericRule<Integer>)rule;
                int intValue = factories.getLongFactory().create(value).intValue();
                switch (operator) {
                    case EQUAL_TO:
                        if (intValue < intRule.getMinimum() || intValue > intRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newIntRange(field, intValue, intValue, true, true);
                    case NOT_EQUAL_TO:
                        if (intValue < intRule.getMinimum() || intValue > intRule.getMaximum()) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newIntRange(field, intValue, intValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        if (intValue > intRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newIntRange(field, intValue, intRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (intValue > intRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newIntRange(field, intValue, intRule.getMaximum(), true, true);
                    case LESS_THAN:
                        if (intValue < intRule.getMinimum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newIntRange(field, intRule.getMinimum(), intValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (intValue < intRule.getMinimum()) return new MatchNoneQuery();
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
                        if (doubleValue < dRule.getMinimum() || doubleValue > dRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                    case NOT_EQUAL_TO:
                        if (doubleValue < dRule.getMinimum() || doubleValue > dRule.getMaximum()) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        if (doubleValue > dRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, dRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (doubleValue > dRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, dRule.getMaximum(), true, true);
                    case LESS_THAN:
                        if (doubleValue < dRule.getMinimum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, dRule.getMinimum(), doubleValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (doubleValue < dRule.getMinimum()) return new MatchNoneQuery();
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
                        if (floatValue < fRule.getMinimum() || floatValue > fRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newFloatRange(field, floatValue, floatValue, true, true);
                    case NOT_EQUAL_TO:
                        if (floatValue < fRule.getMinimum() || floatValue > fRule.getMaximum()) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newFloatRange(field, floatValue, floatValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        if (floatValue > fRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newFloatRange(field, floatValue, fRule.getMaximum(), false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (floatValue > fRule.getMaximum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newFloatRange(field, floatValue, fRule.getMaximum(), true, true);
                    case LESS_THAN:
                        if (floatValue < fRule.getMinimum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newFloatRange(field, fRule.getMinimum(), floatValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (floatValue < fRule.getMinimum()) return new MatchNoneQuery();
                        return NumericRangeQuery.newFloatRange(field, fRule.getMinimum(), floatValue, true, true);
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession#findNodesWith(org.modeshape.graph.query.model.ReferenceValue,
     *      org.modeshape.graph.query.model.Operator, java.lang.Object)
     */
    @Override
    public Query findNodesWith( ReferenceValue referenceValue,
                                Operator operator,
                                Object value ) {
        String field = referenceValue.propertyName();
        if (field == null) {
            if (referenceValue.includesWeakReferences()) {
                field = LuceneSearchWorkspace.ContentIndex.REFERENCES;
            } else {
                field = LuceneSearchWorkspace.ContentIndex.STRONG_REFERENCES;
            }
        }
        ValueFactories factories = processor.valueFactories;
        String stringValue = processor.stringFactory.create(value);
        switch (operator) {
            case EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue, field, factories, true);
            case NOT_EQUAL_TO:
                return new NotQuery(CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue, field, factories, true));
            case GREATER_THAN:
                return CompareStringQuery.createQueryForNodesWithFieldGreaterThan(stringValue, field, factories, true);
            case GREATER_THAN_OR_EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(stringValue, field, factories, true);
            case LESS_THAN:
                return CompareStringQuery.createQueryForNodesWithFieldLessThan(stringValue, field, factories, true);
            case LESS_THAN_OR_EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(stringValue, field, factories, true);
            case LIKE:
                return findNodesLike(field, stringValue, false);
        }
        return null;
    }

    public Query findNodesWithNumericRange( PropertyValue propertyValue,
                                            Object lowerValue,
                                            Object upperValue,
                                            boolean includesLower,
                                            boolean includesUpper ) {
        String field = processor.stringFactory.create(propertyValue.propertyName());
        return findNodesWithNumericRange(field, lowerValue, upperValue, includesLower, includesUpper);
    }

    public Query findNodesWithNumericRange( NodeDepth depth,
                                            Object lowerValue,
                                            Object upperValue,
                                            boolean includesLower,
                                            boolean includesUpper ) {
        return findNodesWithNumericRange(ContentIndex.DEPTH, lowerValue, upperValue, includesLower, includesUpper);
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
            case DECIMAL:
                BigDecimal lowerDecimal = factories.getDecimalFactory().create(lowerValue);
                BigDecimal upperDecimal = factories.getDecimalFactory().create(upperValue);
                String lsv = FieldUtil.decimalToString(lowerDecimal);
                String usv = FieldUtil.decimalToString(upperDecimal);
                Query lower = null;
                if (includesLower) {
                    lower = CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(lsv, field, factories, false);
                } else {
                    lower = CompareStringQuery.createQueryForNodesWithFieldGreaterThan(lsv, field, factories, false);
                }
                Query upper = null;
                if (includesUpper) {
                    upper = CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(usv, field, factories, false);
                } else {
                    upper = CompareStringQuery.createQueryForNodesWithFieldLessThan(usv, field, factories, false);
                }
                BooleanQuery query = new BooleanQuery();
                query.add(lower, Occur.MUST);
                query.add(upper, Occur.MUST);
                return query;
            case STRING:
            case REFERENCE:
            case WEAK_REFERENCE:
            case BINARY:
                assert false;
        }
        return new MatchNoneQuery();
    }

    protected String likeExpresionForWildcardPath( String path ) {
        if (path.equals("/") || path.equals("%")) return path;
        StringBuilder sb = new StringBuilder();
        path = path.replaceAll("%+", "%");
        if (path.startsWith("%/")) {
            sb.append("%");
            if (path.length() == 2) return sb.toString();
            path = path.substring(2);
        }
        for (String segment : path.split("/")) {
            if (segment.length() == 0) continue;
            sb.append("/");
            sb.append(segment);
            if (segment.equals("%") || segment.equals("_")) continue;
            if (!segment.endsWith("]") && !segment.endsWith("]%") && !segment.endsWith("]_")) {
                sb.append("[1]");
            }
        }
        if (path.endsWith("/")) sb.append("/");
        return sb.toString();
    }

    public Query findNodesWith( NodePath nodePath,
                                Operator operator,
                                Object value,
                                boolean caseSensitive ) {
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
                likeExpression = likeExpresionForWildcardPath(likeExpression);
                if (likeExpression.indexOf("[%]") != -1) {
                    // We can't use '[%]' because we only want to match digits,
                    // so handle this using a regex ...
                    String regex = likeExpression;
                    regex = regex.replace("[%]", "[\\d+]");
                    regex = regex.replace("[", "\\[");
                    regex = regex.replace("*", ".*").replace("?", ".");
                    regex = regex.replace("%", ".*").replace("_", ".");
                    // Now create a regex query ...
                    RegexQuery regexQuery = new RegexQuery(new Term(ContentIndex.PATH, regex));
                    int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                    regexQuery.setRegexImplementation(new JavaUtilRegexCapabilities(flags));
                    query = regexQuery;
                } else {
                    query = findNodesLike(ContentIndex.PATH, likeExpression, caseSensitive);
                }
                break;
            case GREATER_THAN:
                query = ComparePathQuery.createQueryForNodesWithPathGreaterThan(pathValue,
                                                                                ContentIndex.PATH,
                                                                                processor.valueFactories,
                                                                                caseSensitive);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = ComparePathQuery.createQueryForNodesWithPathGreaterThanOrEqualTo(pathValue,
                                                                                         ContentIndex.PATH,
                                                                                         processor.valueFactories,
                                                                                         caseSensitive);
                break;
            case LESS_THAN:
                query = ComparePathQuery.createQueryForNodesWithPathLessThan(pathValue,
                                                                             ContentIndex.PATH,
                                                                             processor.valueFactories,
                                                                             caseSensitive);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = ComparePathQuery.createQueryForNodesWithPathLessThanOrEqualTo(pathValue,
                                                                                      ContentIndex.PATH,
                                                                                      processor.valueFactories,
                                                                                      caseSensitive);
                break;
        }
        return query;
    }

    public Query findNodesWith( NodeName nodeName,
                                Operator operator,
                                Object value,
                                boolean caseSensitive ) {
        ValueFactories factories = processor.valueFactories;
        String stringValue = processor.stringFactory.create(value);
        if (!caseSensitive) stringValue = stringValue.toLowerCase();
        Path.Segment segment = operator != Operator.LIKE ? processor.pathFactory.createSegment(stringValue) : null;
        // Determine if the string value contained a SNS index ...
        boolean includeSns = stringValue.indexOf('[') != -1;
        int snsIndex = operator != Operator.LIKE ? segment.getIndex() : 0;
        Query query = null;
        switch (operator) {
            case EQUAL_TO:
                if (!includeSns) {
                    return new TermQuery(new Term(ContentIndex.NODE_NAME, stringValue));
                }
                BooleanQuery booleanQuery = new BooleanQuery();
                booleanQuery.add(new TermQuery(new Term(ContentIndex.NODE_NAME, stringValue)), Occur.MUST);
                booleanQuery.add(NumericRangeQuery.newIntRange(ContentIndex.SNS_INDEX, snsIndex, snsIndex, true, true),
                                 Occur.MUST);
                return booleanQuery;
            case NOT_EQUAL_TO:
                if (!includeSns) {
                    return new NotQuery(new TermQuery(new Term(ContentIndex.NODE_NAME, stringValue)));
                }
                booleanQuery = new BooleanQuery();
                booleanQuery.add(new TermQuery(new Term(ContentIndex.NODE_NAME, stringValue)), Occur.MUST);
                booleanQuery.add(NumericRangeQuery.newIntRange(ContentIndex.SNS_INDEX, snsIndex, snsIndex, true, true),
                                 Occur.MUST);
                return new NotQuery(booleanQuery);
            case GREATER_THAN:
                query = CompareNameQuery.createQueryForNodesWithNameGreaterThan(segment,
                                                                                ContentIndex.NODE_NAME,
                                                                                ContentIndex.SNS_INDEX,
                                                                                factories,
                                                                                caseSensitive,
                                                                                includeSns);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = CompareNameQuery.createQueryForNodesWithNameGreaterThanOrEqualTo(segment,
                                                                                         ContentIndex.NODE_NAME,
                                                                                         ContentIndex.SNS_INDEX,
                                                                                         factories,
                                                                                         caseSensitive,
                                                                                         includeSns);
                break;
            case LESS_THAN:
                query = CompareNameQuery.createQueryForNodesWithNameLessThan(segment,
                                                                             ContentIndex.NODE_NAME,
                                                                             ContentIndex.SNS_INDEX,
                                                                             factories,
                                                                             caseSensitive,
                                                                             includeSns);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = CompareNameQuery.createQueryForNodesWithNameLessThanOrEqualTo(segment,
                                                                                      ContentIndex.NODE_NAME,
                                                                                      ContentIndex.SNS_INDEX,
                                                                                      factories,
                                                                                      caseSensitive,
                                                                                      includeSns);
                break;
            case LIKE:
                // See whether the like expression has brackets ...
                String likeExpression = stringValue;
                int openBracketIndex = likeExpression.indexOf('[');
                if (openBracketIndex != -1) {
                    String localNameExpression = likeExpression.substring(0, openBracketIndex);
                    String snsIndexExpression = likeExpression.substring(openBracketIndex);
                    Query localNameQuery = CompareStringQuery.createQueryForNodesWithFieldLike(localNameExpression,
                                                                                               ContentIndex.NODE_NAME,
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
                                                                                ContentIndex.NODE_NAME,
                                                                                factories,
                                                                                caseSensitive);
                }
                assert query != null;
                break;
        }
        return query;
    }

    public Query findNodesWith( NodeLocalName nodeName,
                                Operator operator,
                                Object value,
                                boolean caseSensitive ) {
        String nameValue = processor.stringFactory.create(value);
        Query query = null;
        switch (operator) {
            case LIKE:
                String likeExpression = processor.stringFactory.create(value);
                query = findNodesLike(ContentIndex.LOCAL_NAME, likeExpression, caseSensitive);
                break;
            case EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                               ContentIndex.LOCAL_NAME,
                                                                               processor.valueFactories,
                                                                               caseSensitive);
                break;
            case NOT_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                               ContentIndex.LOCAL_NAME,
                                                                               processor.valueFactories,
                                                                               caseSensitive);
                query = new NotQuery(query);
                break;
            case GREATER_THAN:
                query = CompareStringQuery.createQueryForNodesWithFieldGreaterThan(nameValue,
                                                                                   ContentIndex.LOCAL_NAME,
                                                                                   processor.valueFactories,
                                                                                   caseSensitive);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(nameValue,
                                                                                            ContentIndex.LOCAL_NAME,
                                                                                            processor.valueFactories,
                                                                                            caseSensitive);
                break;
            case LESS_THAN:
                query = CompareStringQuery.createQueryForNodesWithFieldLessThan(nameValue,
                                                                                ContentIndex.LOCAL_NAME,
                                                                                processor.valueFactories,
                                                                                caseSensitive);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(nameValue,
                                                                                         ContentIndex.LOCAL_NAME,
                                                                                         processor.valueFactories,
                                                                                         caseSensitive);
                break;
        }
        return query;
    }

    public Query findNodesWith( NodeDepth depthConstraint,
                                Operator operator,
                                Object value ) {
        int depth = processor.valueFactories.getLongFactory().create(value).intValue();
        switch (operator) {
            case EQUAL_TO:
                return NumericRangeQuery.newIntRange(ContentIndex.DEPTH, depth, depth, true, true);
            case NOT_EQUAL_TO:
                Query query = NumericRangeQuery.newIntRange(ContentIndex.DEPTH, depth, depth, true, true);
                return new NotQuery(query);
            case GREATER_THAN:
                return NumericRangeQuery.newIntRange(ContentIndex.DEPTH, depth, MAX_DEPTH, false, true);
            case GREATER_THAN_OR_EQUAL_TO:
                return NumericRangeQuery.newIntRange(ContentIndex.DEPTH, depth, MAX_DEPTH, true, true);
            case LESS_THAN:
                return NumericRangeQuery.newIntRange(ContentIndex.DEPTH, MIN_DEPTH, depth, true, false);
            case LESS_THAN_OR_EQUAL_TO:
                return NumericRangeQuery.newIntRange(ContentIndex.DEPTH, MIN_DEPTH, depth, true, true);
            case LIKE:
                // This is not allowed ...
                return null;
        }
        return null;
    }

    protected Query createLocalNameQuery( String likeExpression,
                                          boolean caseSensitive ) {
        if (likeExpression == null) return null;
        return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression,
                                                                   ContentIndex.LOCAL_NAME,
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
            return NumericRangeQuery.newIntRange(ContentIndex.SNS_INDEX, MIN_SNS_INDEX, 9, true, true);
        }
        if (likeExpression.equals("%")) {
            // The SNS expression can be any digits ...
            return NumericRangeQuery.newIntRange(ContentIndex.SNS_INDEX, MIN_SNS_INDEX, MAX_SNS_INDEX, true, true);
        }
        if (likeExpression.indexOf('_') != -1) {
            if (likeExpression.indexOf('%') != -1) {
                // Contains both ...
                return findNodesLike(ContentIndex.SNS_INDEX, likeExpression, true);
            }
            // It presumably contains some numbers and at least one '_' character ...
            int firstWildcardChar = likeExpression.indexOf('_');
            if (firstWildcardChar + 1 < likeExpression.length()) {
                // There's at least some characters after the first '_' ...
                int secondWildcardChar = likeExpression.indexOf('_', firstWildcardChar + 1);
                if (secondWildcardChar != -1) {
                    // There are multiple '_' characters ...
                    return findNodesLike(ContentIndex.SNS_INDEX, likeExpression, true);
                }
            }
            // There's only one '_', so parse the lowermost value and uppermost value ...
            String lowerExpression = likeExpression.replace('_', '0');
            String upperExpression = likeExpression.replace('_', '9');
            try {
                // This SNS is just a number ...
                int lowerSns = Integer.parseInt(lowerExpression);
                int upperSns = Integer.parseInt(upperExpression);
                return NumericRangeQuery.newIntRange(ContentIndex.SNS_INDEX, lowerSns, upperSns, true, true);
            } catch (NumberFormatException e) {
                // It's not a number but it's in the SNS field, so there will be no results ...
                return new MatchNoneQuery();
            }
        }
        if (likeExpression.indexOf('%') != -1) {
            // It presumably contains some numbers and at least one '%' character ...
            return findNodesLike(ContentIndex.SNS_INDEX, likeExpression, true);
        }
        // This is not a LIKE expression but an exact value specification and should be a number ...
        try {
            // This SNS is just a number ...
            int sns = Integer.parseInt(likeExpression);
            return NumericRangeQuery.newIntRange(ContentIndex.SNS_INDEX, sns, sns, true, true);
        } catch (NumberFormatException e) {
            // It's not a number but it's in the SNS field, so there will be no results ...
            return new MatchNoneQuery();
        }
    }

    /**
     * This collector is responsible for loading the value for each of the columns into each tuple array.
     */
    protected static class DualIndexTupleCollector extends TupleCollector {
        private final LuceneSearchSession session;
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

        protected DualIndexTupleCollector( LuceneSearchSession session,
                                           Columns columns ) {
            this.session = session;
            this.columns = columns;
            assert this.columns != null;
            this.numValues = this.columns.getTupleSize();
            assert this.numValues >= 0;
            assert this.columns.getSelectorNames().size() == 1;
            final String selectorName = this.columns.getSelectorNames().get(0);
            this.locationIndex = this.columns.getLocationIndex(selectorName);
            this.recordScore = this.columns.hasFullTextSearchScores();
            this.scoreIndex = this.recordScore ? this.columns.getFullTextSearchScoreIndexFor(selectorName) : -1;

            // Create the set of field names that we need to load from the document ...
            final Set<String> fieldNames = new HashSet<String>(this.columns.getColumnNames());
            fieldNames.add(ContentIndex.LOCATION_ID_PROPERTIES); // add the UUID, which we'll put into the Location ...
            fieldNames.add(ContentIndex.PATH); // add the UUID, which we'll put into the Location ...
            this.fieldSelector = new FieldSelector() {
                private static final long serialVersionUID = 1L;

                public FieldSelectorResult accept( String fieldName ) {
                    return fieldNames.contains(fieldName) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD;
                }
            };
        }

        /**
         * @return tuples
         */
        @Override
        public LinkedList<Object[]> getTuples() {
            return tuples;
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
            // Read the location ...
            Location location = session.readLocation(document);
            tuple[locationIndex] = location;

            // Set the score column if required ...
            Float score = null;
            if (recordScore) {
                assert scorer != null;
                score = new Float(scorer.score());
                tuple[scoreIndex] = score;
            }

            // Read the column values ...
            for (String columnName : columns.getColumnNames()) {
                int index = columns.getColumnIndexForName(columnName);
                // We just need to retrieve the first value if there is more than one ...
                Object value = document.get(columnName);
                if (value == null) {
                    if (columnName.equals("jcr:path")) {
                        value = session.processor.stringFactory.create(location.getPath());
                    } else if (columnName.equals("mode:depth")) {
                        value = new Long(location.getPath().size());
                    } else if (columnName.equals("jcr:score")) {
                        value = score == null ? new Double(scorer.score()) : new Double(score.doubleValue());
                    } else if (columnName.equals("jcr:name")) {
                        Path path = location.getPath();
                        if (path.isRoot()) {
                            value = "";
                        } else {
                            value = session.processor.stringFactory.create(path.getLastSegment().getName());
                        }
                    } else if (columnName.equals("mode:localName")) {
                        Path path = location.getPath();
                        if (path.isRoot()) {
                            value = "";
                        } else {
                            value = path.getLastSegment().getName().getLocalName();
                        }
                    }
                }
                tuple[index] = value;
            }

            tuples.add(tuple);
        }
    }

    /**
     * This collector is responsible for loading the value for each of the columns into each tuple array.
     */
    protected static class FullTextSearchTupleCollector extends TupleCollector {
        private final List<Object[]> tuples;
        private final FieldSelector fieldSelector;
        private final LuceneSearchSession session;
        private Scorer scorer;
        private IndexReader currentReader;
        private int docOffset;

        protected FullTextSearchTupleCollector( LuceneSearchSession session,
                                                List<Object[]> tuples ) {
            assert session != null;
            assert tuples != null;
            this.session = session;
            this.tuples = tuples;
            this.fieldSelector = LOCATION_FIELDS_SELECTOR;
        }

        /**
         * @return tuples
         */
        @Override
        public List<Object[]> getTuples() {
            return tuples;
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
            Object[] tuple = new Object[2];
            Document document = currentReader.document(docId, fieldSelector);
            // Read the Location ...
            tuple[0] = session.readLocation(document);
            // And read the score ...
            tuple[1] = scorer.score();
            // And add the tuple ...
            tuples.add(tuple);
        }
    }
}
