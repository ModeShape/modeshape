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
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.PathCache;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.lucene.LuceneQueryEngine.TupleCollector;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * 
 */
public class BasicTupleCollector extends TupleCollector {

    private QueryContext queryContext;
    private NodeCache nodeCache;
    private final Columns columns;
    private final LinkedList<Object[]> tuples = new LinkedList<Object[]>();
    private final PathCache pathCache;
    private final Name[] columnNames;
    private final int numValues;
    private final FieldSelector fieldSelector;
    private final int locationIndex;
    private final PseudoColumnAssignment[] assignments;
    private Scorer scorer;
    private IndexReader currentReader;
    private int docOffset;

    public BasicTupleCollector( QueryContext queryContext,
                                Columns columns ) {
        this.queryContext = queryContext;
        this.nodeCache = queryContext.getNodeCache();
        this.pathCache = new PathCache(nodeCache);
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
        this.assignments = assignments.toArray(new PseudoColumnAssignment[assignments.size()]);

        // Get the names of the properties for each of the tuple fields ...
        this.columnNames = new Name[this.numValues];
        List<String> columnNames = this.columns.getColumnNames();
        NameFactory nameFactory = queryContext.getExecutionContext().getValueFactories().getNameFactory();
        for (String columnName : columnNames) {
            int index = this.columns.getColumnIndexForName(columnName);
            String propertyName = this.columns.getPropertyNameForColumn(index);
            if ("jcr:score".equals(propertyName)) {
                continue; // already added
            } else if ("jcr:path".equals(propertyName)) {
                assignments.add(new PathColumnAssignment(index));
            } else if ("jcr:name".equals(propertyName)) {
                assignments.add(new NameColumnAssignment(index));
            } else if ("mode:localName".equals(propertyName)) {
                assignments.add(new LocalNameColumnAssignment(index));
            } else if ("mode:depth".equals(propertyName)) {
                assignments.add(new LocalNameColumnAssignment(index));
            } else {
                Name propName = nameFactory.create(propertyName);
                this.columnNames[index] = propName;
            }
        }

        this.fieldSelector = new FieldSelector() {
            private static final long serialVersionUID = 1L;

            @Override
            public FieldSelectorResult accept( String fieldName ) {
                // We only want to load the ID field; all other fields we want to get from the actual node ...
                return NodeInfoIndex.FieldName.ID.equals(fieldName) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD;
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
        return true;
    }

    @Override
    public void collect( int doc ) throws IOException {
        int docId = doc + docOffset;
        Object[] tuple = new Object[numValues];
        Document document = currentReader.document(docId, fieldSelector);
        // Read the id ...
        String id = document.get(NodeInfoIndex.FieldName.ID);
        float score = scorer.score();

        // And get the node ...
        NodeKey key = new NodeKey(id);
        CachedNode node = nodeCache.getNode(key);
        Path path = pathCache.getPath(node);
        Location location = new Location(path);
        tuple[locationIndex] = location;

        // Set the column values ...
        for (int i = 0; i != numValues; ++i) {
            Name propName = columnNames[i];
            if (propName == null) continue;
            Property property = node.getProperty(propName, nodeCache);
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
