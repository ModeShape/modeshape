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
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.Collections;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * The results of a query. This is not thread-safe because it relies upon JcrSession, which is not thread-safe. Also, although the
 * results of a query never change, the objects returned by the iterators may vary if the session information changes.
 * 
 * @see XPathQueryResult
 * @see JcrSqlQueryResult
 */
@NotThreadSafe
public class JcrQueryResult implements org.modeshape.jcr.api.query.QueryResult {
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

    @Override
    public String[] getColumnNames() /*throws RepositoryException*/{
        List<String> names = getColumnNameList();
        return names.toArray(new String[names.size()]); // make a defensive copy ...
    }

    @Override
    public String[] getColumnTypes() {
        List<String> types = getColumnTypeList();
        return types.toArray(new String[types.size()]); // make a defensive copy ...
    }

    @Override
    public String[] getSelectorNames() {
        if (columnTables == null) {
            // Discover the types ...
            Columns columns = results.getColumns();
            Set<SelectorName> selectorNames = new HashSet<SelectorName>();
            List<String> tables = new ArrayList<String>(columns.getColumnCount());
            for (Column column : columns) {
                SelectorName selectorName = column.selectorName();
                if (selectorNames.add(column.selectorName())) {
                    tables.add(selectorName.getString());
                }
            }
            columnTables = tables;
        }
        return columnTables.toArray(new String[columnTables.size()]);
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        if (getSelectorNames().length > 1) {
            throw new RepositoryException(JcrI18n.multipleSelectorsAppearInQueryUnableToCallMethod.text(queryStatement));
        }
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

    @Override
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

        @Override
        public Node nextNode() {
            Node node = nodes.next();
            ++position;
            return node;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public void skip( long skipNum ) {
            for (long i = 0L; i != skipNum; ++i)
                nextNode();
        }

        @Override
        public boolean hasNext() {
            return nodes.hasNext();
        }

        @Override
        public Object next() {
            return nextNode();
        }

        @Override
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
            List<String> columnSelectorNames = columns.getSelectorNames();
            locationIndexes = new int[columnSelectorNames.size()];
            for (String selectorName : columnSelectorNames) {
                locationIndexes[i++] = columns.getLocationIndex(selectorName);
            }
        }

        public boolean hasSelector( String selectorName ) {
            return this.selectorNames.contains(selectorName);
        }

        @Override
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

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public long getSize() {
            return numRows;
        }

        @Override
        public void skip( long skipNum ) {
            for (long i = 0L; i != skipNum; ++i) {
                tuples.next();
            }
            position += skipNum;
        }

        @Override
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
            boolean foundAtLeastOneNode = false;
            for (int locationIndex : locationIndexes) {
                Location location = (Location)tuple[locationIndex];
                Node node = context.getNode(location);
                if (node != null) {
                    foundAtLeastOneNode = true;
                }
                nodes[index++] = node;
            }
            if (!foundAtLeastOneNode) {
                // We found no nodes, so skip this row ...
                return null;
            }
            return new MultiSelectorQueryResultRow(this, nodes, locationIndexes, tuple);
        }

        protected String getPropertyNameForColumnName( String columnName ) {
            return columns.getPropertyNameForColumnName(columnName);
        }

        @Override
        public Object next() {
            return nextRow();
        }

        @Override
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

        protected Value jcrDouble( Double score ) {
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

    protected static class SingleSelectorQueryResultRow implements javax.jcr.query.Row {
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

        @Override
        public Node getNode( String selectorName ) throws RepositoryException {
            if (!iterator.hasSelector(selectorName)) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            return node;
        }

        @Override
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            // Get the property name for the column. Note that if the column is aliased, the property name will be different;
            // otherwise, the property name will be the same as the column name ...
            String propertyName = iterator.getPropertyNameForColumnName(columnName);
            if (PSEUDO_COLUMNS.contains(propertyName)) {
                if (JCR_PATH_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    return iterator.jcrPath(location.getPath());
                }
                if (JCR_NAME_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrName();
                    return iterator.jcrName(path.getLastSegment().getName());
                }
                if (MODE_LOCALNAME_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrString("");
                    return iterator.jcrString(path.getLastSegment().getName().getLocalName());
                }
                if (MODE_DEPTH_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[iterator.locationIndex];
                    Path path = location.getPath();
                    Long depth = new Long(path.size());
                    return iterator.jcrLong(depth);
                }
                if (JCR_SCORE_COLUMN_NAME.equals(propertyName)) {
                    Double score = iterator.scoreIndex == -1 ? 0.0d : (Double)tuple[iterator.scoreIndex];
                    return iterator.jcrDouble(score);
                }
            }
            // Get the property's value ...
            if (!node.hasProperty(propertyName)) return null;
            Property property = node.getProperty(propertyName);
            Value value = null;
            if (property.isMultiple()) {
                // Use only the first value of a multi-valued property ...
                value = property.getValues()[0];
            } else {
                value = property.getValue();
            }
            return value;
        }

        @Override
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

    protected static class MultiSelectorQueryResultRow implements javax.jcr.query.Row {
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

        @Override
        public Node getNode( String selectorName ) throws RepositoryException {
            int nodeIndex = iterator.columns.getSelectorNames().indexOf(selectorName);
            if (nodeIndex == -1) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            return nodes[nodeIndex];
        }

        @Override
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            String selectorName = iterator.columns.getSelectorNameForColumnName(columnName);
            int nodeIndex = iterator.columns.getSelectorNames().indexOf(selectorName);
            if (nodeIndex == -1) {
                throw new RepositoryException(JcrI18n.queryResultsDoNotIncludeColumn.text(columnName, iterator.query));
            }
            Node node = nodes[nodeIndex];
            if (node == null) return null;
            // Get the property name for the column. Note that if the column is aliased, the property name will be different;
            // otherwise, the property name will be the same as the column name ...
            String propertyName = iterator.getPropertyNameForColumnName(columnName);
            if (PSEUDO_COLUMNS.contains(propertyName)) {
                int locationIndex = iterator.columns.getLocationIndexForColumn(columnName);
                if (JCR_PATH_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[locationIndex];
                    return iterator.jcrPath(location.getPath());
                }
                if (JCR_NAME_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrName();
                    return iterator.jcrName(path.getLastSegment().getName());
                }
                if (MODE_LOCALNAME_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[locationIndex];
                    Path path = location.getPath();
                    if (path.isRoot()) return iterator.jcrString("");
                    return iterator.jcrString(path.getLastSegment().getName().getLocalName());
                }
                if (MODE_DEPTH_COLUMN_NAME.equals(propertyName)) {
                    Location location = (Location)tuple[locationIndex];
                    Path path = location.getPath();
                    Long depth = new Long(path.size());
                    return iterator.jcrLong(depth);
                }
                if (JCR_SCORE_COLUMN_NAME.equals(propertyName)) {
                    int scoreIndex = iterator.columns.getFullTextSearchScoreIndexFor(columnName);
                    Double score = scoreIndex == -1 ? 0.0d : (Double)tuple[scoreIndex];
                    return iterator.jcrDouble(score);
                }
            }
            // Get the property's value ...
            if (!node.hasProperty(propertyName)) return null;
            Property property = node.getProperty(propertyName);
            Value value = null;
            if (property.isMultiple()) {
                // Use only the first value of a multi-valued property ...
                value = property.getValues()[0];
            } else {
                value = property.getValue();
            }
            return value;
        }

        @Override
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
