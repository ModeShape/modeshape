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
package org.modeshape.jcr.query.lucene.basic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.infinispan.schematic.document.NotThreadSafe;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.PathCache;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.RepositoryPathCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.lucene.LuceneQueryEngine.TupleCollector;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * The {@link BasicLuceneSchema} does not store any fields in the indexes, with the exception of the document identifier in which
 * we're storing the key of the node that the document represents. Thus, in order to provide for each document the column values
 * for each tuple, this collector looks up the {@link CachedNode} for each tuple using the {@link QueryContext}'s
 * {@link QueryContext#getRepositoryCache() RepositoryCache} and then finds the property value for each required column.
 * <p>
 * In all cases, only the persisted workspace content appears in the indexes, so every Lucene {@link Query} only operates against
 * this persisted content. However, the query results might reflect either only the persisted workspace content or the session's
 * view of the content (which includes both its transient state as well as the persisted workspace content), depending upon
 * whether the QueryContext's NodeCache is actually a {@link WorkspaceCache} or a {@link SessionCache}.
 * </p>
 * <p>
 * This class's constructor prepares the information that's ncessary to access the tuple values as fast as possible.
 * </p>
 */
@NotThreadSafe
public class BasicTupleCollector extends TupleCollector {

    private final QueryContext queryContext;
    private final RepositoryCache repositoryCache;
    private final Columns columns;
    private final LinkedList<Object[]> tuples = new LinkedList<Object[]>();
    private final RepositoryPathCache repositoryPathCache;
    private final Name[] columnNames;
    private final int numValues;
    private final FieldSelector fieldSelector;
    private final int locationIndex;
    private final PseudoColumnAssignment[] assignments;
    private Scorer scorer;
    private IndexReader currentReader;
    private int docOffset;
    private String lastWorkspaceName;
    private NodeCache lastWorkspaceCache;
    private PathCache lastWorkspacePathCache;

    public BasicTupleCollector( QueryContext queryContext,
                                Columns columns ) {
        this.queryContext = queryContext;
        this.repositoryCache = queryContext.getRepositoryCache();
        this.repositoryPathCache = new RepositoryPathCache();
        this.columns = columns;
        this.numValues = this.columns.getTupleSize();
        assert this.numValues >= 0;
        assert this.columns.getSelectorNames().size() == 1;
        final String selectorName = this.columns.getSelectorNames().get(0);
        this.locationIndex = this.columns.getLocationIndex(selectorName);

        // Use a score assignment if needed ...
        List<PseudoColumnAssignment> assignments = new ArrayList<PseudoColumnAssignment>();
        if (columns.hasFullTextSearchScores()) {
            int scoreIndex = this.columns.getFullTextSearchScoreIndexFor(selectorName);
            assignments.add(new ScoreColumnAssignment(scoreIndex));
        }

        // Get the names of the properties for each of the tuple fields ...
        this.columnNames = new Name[this.numValues];
        List<String> columnNames = this.columns.getColumnNames();
        NameFactory nameFactory = queryContext.getExecutionContext().getValueFactories().getNameFactory();
        for (String columnName : columnNames) {
            int index = this.columns.getColumnIndexForName(columnName);
            String propertyName = this.columns.getPropertyNameForColumn(index);
            if (JcrConstants.JCR_SCORE.equals(propertyName)) {
                continue; // already added
            } else if (JcrConstants.JCR_PATH.equals(propertyName)) {
                assignments.add(new PathColumnAssignment(index));
            } else if (JcrConstants.JCR_NAME.equals(propertyName)) {
                assignments.add(new NameColumnAssignment(index));
            } else if (JcrConstants.MODE_LOCAL_NAME.equals(propertyName)) {
                assignments.add(new LocalNameColumnAssignment(index));
            } else if (JcrConstants.MODE_DEPTH.equals(propertyName)) {
                assignments.add(new LocalNameColumnAssignment(index));
            } else {
                Name propName = nameFactory.create(propertyName);
                this.columnNames[index] = propName;
            }
        }

        // Create the array of assignments ...
        this.assignments = assignments.toArray(new PseudoColumnAssignment[assignments.size()]);

        // Create a FieldSelector that instructs Lucene to load only the ID field ...
        final Set<String> loadedFieldNames = new HashSet<String>();
        loadedFieldNames.add(NodeInfoIndex.FieldName.ID);
        loadedFieldNames.add(NodeInfoIndex.FieldName.WORKSPACE);
        this.fieldSelector = new FieldSelector() {
            private static final long serialVersionUID = 1L;

            @Override
            public FieldSelectorResult accept( String fieldName ) {
                // We only want to load the ID field; all other fields we want to get from the actual node ...
                return loadedFieldNames.contains(fieldName) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD;
            }
        };
    }

    @Override
    public void setScorer( Scorer scorer ) {
        this.scorer = scorer;
    }

    @Override
    public void setNextReader( IndexReader reader,
                               int docBase ) {
        this.currentReader = reader;
        this.docOffset = docBase;
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;
    }

    @Override
    public float doCollect( int doc ) throws IOException {
        // int docId = doc + docOffset;
        Object[] tuple = new Object[numValues];
        Document document = currentReader.document(doc, fieldSelector);
        // Read the id ...
        String id = document.get(NodeInfoIndex.FieldName.ID);
        String workspace = document.get(NodeInfoIndex.FieldName.WORKSPACE);
        float score = scorer.score();

        // And get the node ...
        NodeKey key = new NodeKey(id);
        if (!workspace.equals(lastWorkspaceName)) {
            lastWorkspaceName = workspace;
            lastWorkspaceCache = queryContext.getNodeCache(workspace);
            lastWorkspacePathCache = repositoryPathCache.getPathCache(workspace, lastWorkspaceCache);
        }
        CachedNode node = lastWorkspaceCache.getNode(key);

        // Every tuple has the location ...
        Path path = lastWorkspacePathCache.getPath(node);
        Location location = new Location(path, key);
        tuple[locationIndex] = location;

        // Set the column values ...
        for (int i = 0; i != numValues; ++i) {
            Name propName = columnNames[i];
            if (propName == null) {
                // This value in the tuple is a pseudo-column, which we'll set later ...
                continue;
            }
            // Find the node's named property for this tuple column ...
            Property property = node.getProperty(propName, lastWorkspaceCache);
            if (property == null) continue;
            if (property.isEmpty()) continue;
            if (property.isMultiple()) {
                Object[] values = property.getValuesAsArray();
                tuple[i] = values;
            } else {
                Object firstValue = property.getFirstValue();
                tuple[i] = firstValue;
            }
        }

        // Set the pseudo-columns using the assignments ...
        for (PseudoColumnAssignment assignment : assignments) {
            assignment.setValue(tuple, path, score);
        }

        tuples.add(tuple);
        return score;
    }

    @Override
    public List<Object[]> getTuples() {
        return tuples;
    }

    protected static abstract class PseudoColumnAssignment {
        private final int index;

        protected PseudoColumnAssignment( int index ) {
            this.index = index;
        }

        public final void setValue( Object[] tuple,
                                    Path path,
                                    float score ) {
            Object value = computeValue(path, score);
            tuple[index] = value;
        }

        protected abstract Object computeValue( Path path,
                                                float score );
    }

    protected static final class PathColumnAssignment extends PseudoColumnAssignment {
        protected PathColumnAssignment( int index ) {
            super(index);
        }

        @Override
        protected Object computeValue( Path path,
                                       float score ) {
            return path;
        }
    }

    protected static final class NameColumnAssignment extends PseudoColumnAssignment {
        protected NameColumnAssignment( int index ) {
            super(index);
        }

        @Override
        protected Object computeValue( Path path,
                                       float score ) {
            if (path.isRoot()) return Path.ROOT_NAME;
            return path.getLastSegment().getName();
        }
    }

    protected static final class LocalNameColumnAssignment extends PseudoColumnAssignment {
        protected LocalNameColumnAssignment( int index ) {
            super(index);
        }

        @Override
        protected Object computeValue( Path path,
                                       float score ) {
            if (path.isRoot()) return Path.ROOT_NAME.getLocalName();
            return path.getLastSegment().getName().getLocalName();
        }
    }

    protected static final class ScoreColumnAssignment extends PseudoColumnAssignment {
        protected ScoreColumnAssignment( int index ) {
            super(index);
        }

        @Override
        protected Object computeValue( Path path,
                                       float score ) {
            return new Float(score);
        }
    }

    protected static final class NodeDepthColumnAssignment extends PseudoColumnAssignment {
        protected NodeDepthColumnAssignment( int index ) {
            super(index);
        }

        @Override
        protected Object computeValue( Path path,
                                       float score ) {
            return new Integer(path.size());
        }
    }
}
