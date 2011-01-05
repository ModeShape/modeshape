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
package org.modeshape.jcr.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.collection.Collections;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.query.validate.Schemata.Table;
import org.modeshape.jcr.JcrI18n;

/**
 * The results of a query. This is not thread-safe because it relies upon JcrSession, which is not thread-safe. Also, although the
 * results of a query never change, the objects returned by the iterators may vary if the session information changes.
 * 
 * @see XPathQueryResult
 * @see JcrSqlQueryResult
 */
@NotThreadSafe
public class JcrQueryResult implements QueryResult, org.modeshape.jcr.api.query.QueryResult {
    public static final String JCR_SCORE_COLUMN_NAME = "jcr:score";
    public static final String JCR_PATH_COLUMN_NAME = "jcr:path";
    public static final String JCR_NAME_COLUMN_NAME = "jcr:name";
    public static final String MODE_LOCALNAME_COLUMN_NAME = "mode:localName";
    public static final String MODE_DEPTH_COLUMN_NAME = "mode:depth";
    protected static final Set<String> PSEUDO_COLUMNS = Collections.unmodifiableSet(JCR_SCORE_COLUMN_NAME,
                                                                                    JCR_PATH_COLUMN_NAME,
                                                                                    JCR_NAME_COLUMN_NAME,
                                                                                    MODE_LOCALNAME_COLUMN_NAME,
                                                                                    MODE_DEPTH_COLUMN_NAME);

    protected final JcrQueryContext context;
    protected final QueryResults results;
    protected final Schemata schemata;
    protected final String queryStatement;
    private List<String> columnTables;

    protected JcrQueryResult( JcrQueryContext context,
                              String query,
                              QueryResults graphResults,
                              Schemata schemata ) {
        this.context = context;
        this.results = graphResults;
        this.schemata = schemata;
        this.queryStatement = query;
        assert this.context != null;
        assert this.results != null;
        assert this.schemata != null;
        assert this.queryStatement != null;
    }

    protected QueryResults results() {
        return results;
    }

    public List<String> getColumnNameList() {
        return results.getColumns().getColumnNames();
    }

    public List<String> getColumnTypeList() {
        return results.getColumns().getColumnTypes();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryResult#getColumnNames()
     */
    public String[] getColumnNames() /*throws RepositoryException*/{
        List<String> names = getColumnNameList();
        return names.toArray(new String[names.size()]); // make a defensive copy ...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.QueryResult#getColumnTypes()
     */
    @Override
    public String[] getColumnTypes() {
        List<String> types = getColumnTypeList();
        return types.toArray(new String[types.size()]); // make a defensive copy ...
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.QueryResult#getSelectorNames()
     */
    @Override
    public String[] getSelectorNames() {
        if (columnTables == null) {
            // Discover the types ...
            Columns columns = results.getColumns();
            List<String> tables = new ArrayList<String>(columns.getColumnCount());
            for (Column column : columns) {
                String tableName = "";
                Table table = schemata.getTable(column.selectorName());
                if (table != null) tableName = table.getName().name();
                tables.add(tableName);
            }
            columnTables = tables;
        }
        return columnTables.toArray(new String[columnTables.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryResult#getNodes()
     */
    public NodeIterator getNodes() throws RepositoryException {
        // Find all of the nodes in the results. We have to do this pre-emptively, since this
        // is the only method to throw RepositoryException ...
        final int numRows = results.getRowCount();
        if (numRows == 0) return context.emptyNodeIterator();

        final List<Node> nodes = new ArrayList<Node>(numRows);
        final String selectorName = results.getColumns().getSelectorNames().get(0);
        final int locationIndex = results.getColumns().getLocationIndex(selectorName);
        for (Object[] tuple : results.getTuples()) {
            Location location = (Location)tuple[locationIndex];
            Node node = context.getNode(location);
            if (node != null) {
                nodes.add(node);
            }
        }
        return new QueryResultNodeIterator(nodes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryResult#getRows()
     */
    public RowIterator getRows() /*throws RepositoryException*/{
        // We can actually delay the loading of the nodes until the rows are accessed ...
        final int numRows = results.getRowCount();
        final List<Object[]> tuples = results.getTuples();
        if (results.getColumns().getLocationCount() == 1) {
            return new SingleSelectorQueryResultRowIterator(context, queryStatement, results, tuples.iterator(), numRows);
        }
        return new QueryResultRowIterator(context, queryStatement, results, tuples.iterator(), numRows);
    }

    /**
     * Get a description of the query plan, if requested.
     * 
     * @return the query plan, or null if the plan was not requested
     */
    public String getPlan() {
        return results.getPlan();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return results.toString();
    }

    /**
     * The {@link NodeIterator} implementation returned by the {@link JcrQueryResult}.
     * 
     * @see JcrQueryResult#getNodes()
     */
    @NotThreadSafe
    protected static class QueryResultNodeIterator implements NodeIterator {
        private final Iterator<? extends Node> nodes;
        private final int size;
        private long position = 0L;

        protected QueryResultNodeIterator( List<? extends Node> nodes ) {
            this.nodes = nodes.iterator();
            this.size = nodes.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.NodeIterator#nextNode()
         */
        public Node nextNode() {
            Node node = nodes.next();
            ++position;
            return node;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getPosition()
         */
        public long getPosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getSize()
         */
        public long getSize() {
            return size;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#skip(long)
         */
        public void skip( long skipNum ) {
            for (long i = 0L; i != skipNum; ++i)
                nextNode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return nodes.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            return nextNode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The {@link RowIterator} implementation returned by the {@link JcrQueryResult}.
     * 
     * @see JcrQueryResult#getRows()
     */
    @NotThreadSafe
    protected static class QueryResultRowIterator implements RowIterator {
        protected final List<String> columnNames;
        private final Iterator<Object[]> tuples;
        private final Set<String> selectorNames;
        protected final JcrQueryContext context;
        protected final Columns columns;
        protected final String query;
        private int[] locationIndexes;
        private long position = 0L;
        private long numRows;
        private Row nextRow;

        protected QueryResultRowIterator( JcrQueryContext context,
                                          String query,
                                          QueryResults results,
                                          Iterator<Object[]> tuples,
                                          long numRows ) {
            this.tuples = tuples;
            this.query = query;
            this.columns = results.getColumns();
            this.columnNames = this.columns.getColumnNames();
            this.context = context;
            this.numRows = numRows;
            this.selectorNames = new HashSet<String>(columns.getSelectorNames());
            int i = 0;
            locationIndexes = new int[selectorNames.size()];
            for (String selectorName : selectorNames) {
                locationIndexes[i++] = columns.getLocationIndex(selectorName);
            }
        }

        public boolean hasSelector( String selectorName ) {
            return this.selectorNames.contains(selectorName);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.RowIterator#nextRow()
         */
        public Row nextRow() {
            if (nextRow == null) {
                // Didn't call 'hasNext()' ...
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
            }
            assert nextRow != null;
            Row result = nextRow;
            nextRow = null;
            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getPosition()
         */
        public long getPosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getSize()
         */
        public long getSize() {
            return numRows;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#skip(long)
         */
        public void skip( long skipNum ) {
            for (long i = 0L; i != skipNum; ++i) {
                tuples.next();
            }
            position += skipNum;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            if (nextRow != null) {
                return true;
            }
            while (tuples.hasNext()) {
                final Object[] tuple = tuples.next();
                ++position;
                try {
                    // Get the next row ...
                    nextRow = getNextRow(tuple);
                    if (nextRow != null) return true;
                } catch (RepositoryException e) {
                    // The node could not be found in this session, so skip it ...
                }
                --numRows;
            }
            return false;
        }

        protected Row getNextRow( Object[] tuple ) throws RepositoryException {
            // Make sure that each node referenced by the tuple exists and is accessible ...
            Node[] nodes = new Node[locationIndexes.length];
            int index = 0;
            for (int locationIndex : locationIndexes) {
                Location location = (Location)tuple[locationIndex];
                Node node = context.getNode(location);
                if (node == null) {
                    // Skip this record because one of the nodes no longer exists ...
                    return null;
                }
                nodes[index++] = node;
            }
            return new MultiSelectorQueryResultRow(this, nodes, locationIndexes, tuple);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            return nextRow();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        protected Value jcrPath( Path path ) {
            return context.createValue(PropertyType.PATH, path);
        }

        protected Value jcrName( Name name ) {
            return context.createValue(PropertyType.NAME, name);
        }

        protected Value jcrName() {
            return context.createValue(PropertyType.NAME, "");
        }

        protected Value jcrString( String name ) {
            return context.createValue(PropertyType.STRING, name);
        }

        protected Value jcrLong( Long value ) {
            return context.createValue(PropertyType.LONG, value);
        }

        protected Value jcrDouble( Float score ) {
            return context.createValue(PropertyType.DOUBLE, score);
        }
    }

    /**
     * The {@link RowIterator} implementation returned by the {@link JcrQueryResult}.
     * 
     * @see JcrQueryResult#getRows()
     */
    @NotThreadSafe
    protected static class SingleSelectorQueryResultRowIterator extends QueryResultRowIterator {
        protected final int locationIndex;
        protected final int scoreIndex;

        protected SingleSelectorQueryResultRowIterator( JcrQueryContext context,
                                                        String query,
                                                        QueryResults results,
                                                        Iterator<Object[]> tuples,
                                                        long numRows ) {
            super(context, query, results, tuples, numRows);
            String selectorName = columns.getSelectorNames().get(0);
            locationIndex = columns.getLocationIndex(selectorName);
            scoreIndex = columns.getFullTextSearchScoreIndexFor(selectorName);
        }

        /**
         * {@inheritDoc}
         * 
         * @see QueryResultRowIterator#getNextRow(java.lang.Object[])
         */
        @Override
        protected Row getNextRow( Object[] tuple ) throws RepositoryException {
            Location location = (Location)tuple[locationIndex];
            Node node = context.getNode(location);
            return node != null ? createRow(node, tuple) : null;
        }

        protected Row createRow( Node node,
                                 Object[] tuple ) {
            return new SingleSelectorQueryResultRow(this, node, tuple);
        }
    }

    protected static class SingleSelectorQueryResultRow implements Row, org.modeshape.jcr.api.query.Row {
        protected final SingleSelectorQueryResultRowIterator iterator;
        protected final Node node;
        protected final Object[] tuple;
        private Value[] values = null;

        protected SingleSelectorQueryResultRow( SingleSelectorQueryResultRowIterator iterator,
                                                Node node,
                                                Object[] tuple ) {
            this.iterator = iterator;
            this.node = node;
            this.tuple = tuple;
            assert this.iterator != null;
            assert this.node != null;
            assert this.tuple != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.api.query.Row#getNode(java.lang.String)
         */
        @Override
        public Node getNode( String selectorName ) throws RepositoryException {
            if (!iterator.hasSelector(selectorName)) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            return node;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValue(java.lang.String)
         */
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            if (PSEUDO_COLUMNS.contains(columnName)) {
                if (JCR_PATH_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    return iterator.jcrPath(location.getPath());
                }
                if (JCR_NAME_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrName();
                    return iterator.jcrName(path.getLastSegment().getName());
                }
                if (MODE_LOCALNAME_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrString("");
                    return iterator.jcrString(path.getLastSegment().getName().getLocalName());
                }
                if (MODE_DEPTH_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    Path path = location.getPath();
                    Long depth = new Long(path.size());
                    return iterator.jcrLong(depth);
                }
                if (JCR_SCORE_COLUMN_NAME.equals(columnName)) {
                    Float score = iterator.scoreIndex == -1 ? 0.0f : (Float)tuple[iterator.scoreIndex];
                    return iterator.jcrDouble(score);
                }
            }
            return node.hasProperty(columnName) ? node.getProperty(columnName).getValue() : null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValues()
         */
        public Value[] getValues() throws RepositoryException {
            if (values == null) {
                int i = 0;
                values = new Value[iterator.columnNames.size()];
                for (String columnName : iterator.columnNames) {
                    values[i++] = getValue(columnName);
                }
            }
            return values;
        }

        @Override
        public Node getNode() {
            return node;
        }

        @Override
        public String getPath() throws RepositoryException {
            return node.getPath();
        }

        @Override
        public String getPath( String selectorName ) throws RepositoryException {
            if (!iterator.hasSelector(selectorName)) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            return node.getPath();
        }

        @Override
        public double getScore() throws RepositoryException {
            int index = iterator.scoreIndex;
            if (index == -1) {
                throw new RepositoryException(JcrI18n.queryResultsDoNotIncludeScore.text(iterator.query));
            }
            Object score = tuple[index];
            return score instanceof Float ? ((Float)score).doubleValue() : (Double)score;
        }

        @Override
        public double getScore( String selectorName ) throws RepositoryException {
            if (!iterator.hasSelector(selectorName)) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            return getScore();
        }
    }

    protected static class MultiSelectorQueryResultRow implements Row, org.modeshape.jcr.api.query.Row {
        protected final QueryResultRowIterator iterator;
        protected final Object[] tuple;
        private Value[] values = null;
        private Node[] nodes;

        protected MultiSelectorQueryResultRow( QueryResultRowIterator iterator,
                                               Node[] nodes,
                                               int[] locationIndexes,
                                               Object[] tuple ) {
            this.iterator = iterator;
            this.tuple = tuple;
            this.nodes = nodes;
            assert this.iterator != null;
            assert this.tuple != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.api.query.Row#getNode(java.lang.String)
         */
        @Override
        public Node getNode( String selectorName ) throws RepositoryException {
            int locationIndex = iterator.columns.getLocationIndex(selectorName);
            if (locationIndex == -1) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            return nodes[locationIndex];
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValue(java.lang.String)
         */
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            int locationIndex = iterator.columns.getLocationIndexForColumn(columnName);
            if (locationIndex == -1) {
                throw new RepositoryException(JcrI18n.queryResultsDoNotIncludeColumn.text(columnName, iterator.query));
            }
            Node node = nodes[locationIndex];
            if (node == null) return null;
            if (PSEUDO_COLUMNS.contains(columnName)) {
                if (JCR_PATH_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[locationIndex];
                    return iterator.jcrPath(location.getPath());
                }
                if (JCR_NAME_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrName();
                    return iterator.jcrName(path.getLastSegment().getName());
                }
                if (MODE_LOCALNAME_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrString("");
                    return iterator.jcrString(path.getLastSegment().getName().getLocalName());
                }
                if (MODE_DEPTH_COLUMN_NAME.equals(columnName)) {
                    Location location = (Location)tuple[locationIndex];
                    Path path = location.getPath();
                    Long depth = new Long(path.size());
                    return iterator.jcrLong(depth);
                }
                if (JCR_SCORE_COLUMN_NAME.equals(columnName)) {
                    int scoreIndex = iterator.columns.getFullTextSearchScoreIndexFor(columnName);
                    Float score = scoreIndex == -1 ? 0.0f : (Float)tuple[scoreIndex];
                    return iterator.jcrDouble(score);
                }
            }
            return node.hasProperty(columnName) ? node.getProperty(columnName).getValue() : null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValues()
         */
        public Value[] getValues() throws RepositoryException {
            if (values == null) {
                int i = 0;
                values = new Value[iterator.columnNames.size()];
                for (String columnName : iterator.columnNames) {
                    values[i++] = getValue(columnName);
                }
            }
            return values;
        }

        @Override
        public Node getNode() throws RepositoryException {
            throw new RepositoryException(
                                          JcrI18n.multipleSelectorsAppearInQueryRequireSpecifyingSelectorName.text(iterator.query));
        }

        @Override
        public String getPath() throws RepositoryException {
            throw new RepositoryException(
                                          JcrI18n.multipleSelectorsAppearInQueryRequireSpecifyingSelectorName.text(iterator.query));
        }

        @Override
        public double getScore() throws RepositoryException {
            throw new RepositoryException(
                                          JcrI18n.multipleSelectorsAppearInQueryRequireSpecifyingSelectorName.text(iterator.query));
        }

        @Override
        public String getPath( String selectorName ) throws RepositoryException {
            return getNode(selectorName).getPath();
        }

        @Override
        public double getScore( String selectorName ) throws RepositoryException {
            if (!iterator.hasSelector(selectorName)) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            int scoreIndex = iterator.columns.getFullTextSearchScoreIndexFor(selectorName);
            if (scoreIndex == -1) {
                throw new RepositoryException(JcrI18n.queryResultsDoNotIncludeScore.text(iterator.query));
            }
            Object score = tuple[scoreIndex];
            return score instanceof Float ? ((Float)score).doubleValue() : (Double)score;
        }

    }
}
