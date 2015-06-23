/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.NodeSequence.Restartable;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.engine.process.RestartableSequence;
import org.modeshape.jcr.query.engine.process.SecureSequence;

/**
 * The results of a query. This is not thread-safe because it relies upon JcrSession, which is not thread-safe. Also, although the
 * results of a query never change, the objects returned by the iterators may vary if the session information changes.
 *
 * @see XPathQueryResult
 * @see JcrSqlQueryResult
 */
@NotThreadSafe
public class JcrQueryResult implements org.modeshape.jcr.api.query.QueryResult {

    protected final JcrQueryContext context;
    protected final QueryResults results;
    protected final String queryStatement;
    private final NodeSequence sequence;
    private final boolean restartable;
    private List<String> warnings;
    private boolean accessed = false;

    protected JcrQueryResult( JcrQueryContext context,
                              String query,
                              QueryResults results,
                              boolean restartable,
                              int numRowsInMemory ) {
        this.context = context;
        this.results = results;
        this.queryStatement = query;
        this.restartable = restartable;
        NodeSequence rows = results.getRows();
        if (rows.isEmpty()) {
            this.sequence = rows;
        } else if (!restartable) {
            this.sequence = new SecureSequence(rows, context);
        } else {
            String workspace = context.getWorkspaceName();
            BufferManager bufferMgr = context.getBufferManager();
            CachedNodeSupplier nodeCache = results.getCachedNodes();
            NodeSequence secureSequence = new SecureSequence(rows, context);
            this.sequence = new RestartableSequence(workspace, secureSequence, bufferMgr, nodeCache, numRowsInMemory);
        }

        assert this.context != null;
        assert this.results != null;
        assert this.queryStatement != null;
    }

    protected List<String> getColumnNameList() {
        return results.getColumns().getColumnNames();
    }

    protected List<String> getColumnTypeList() {
        return results.getColumns().getColumnTypes();
    }

    protected final NodeSequence sequence() {
        return sequence;
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
        List<String> selectorNames = results.getColumns().getSelectorNames();
        return selectorNames.toArray(new String[selectorNames.size()]); // make a defensive copy ...
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        if (accessed) {
            if (restartable) {
                if (sequence instanceof Restartable) {
                    ((Restartable)sequence).restart();
                } else {
                    assert sequence.isEmpty();
                }
            } else if (!sequence.isEmpty()) {
                throw new RepositoryException(JcrI18n.multipleCallsToGetRowsOrNodesIsNotAllowed.text(queryStatement));
            }
        }
        if (getSelectorNames().length > 1) {
            throw new RepositoryException(JcrI18n.multipleSelectorsAppearInQueryUnableToCallMethod.text(queryStatement));
        }
        // Find all of the nodes in the results...
        accessed = true;
        int defaultSelectorIndex = computeDefaultSelectorIndex();
        return new QueryResultNodeIterator(context, sequence, defaultSelectorIndex);
    }

    protected int computeDefaultSelectorIndex() {
        Columns columns = results.getColumns();
        List<String> selectorNames = columns.getSelectorNames();
        int selectorIndex = 0;
        if (selectorNames.size() == 1) {
            selectorIndex = columns.getSelectorIndex(selectorNames.get(0));
        }
        if (selectorIndex >= sequence.width()) {
            // The columns were built on top of other columns that expose multiple selectors, but this sequenc only has
            // one selector ...
            selectorIndex = 0;
        }
        return selectorIndex;
    }

    @Override
    public RowIterator getRows() throws RepositoryException {
        if (accessed) {
            if (restartable) {
                if (sequence instanceof Restartable) {
                    ((Restartable)sequence).restart();
                } else {
                    assert sequence.isEmpty();
                }
            } else if (!sequence.isEmpty()) {
                throw new RepositoryException(JcrI18n.multipleCallsToGetRowsOrNodesIsNotAllowed.text(queryStatement));
            }
        }
        accessed = true;
        final Columns columns = results.getColumns();
        if (columns.getSelectorNames().size() == 1) {
            // Then we know that there is only one selector in the results ...
            return new SingleSelectorQueryResultRowIterator(context, queryStatement, sequence, columns);
        }
        // There may be 1 or more selectors in the columns, but the results definitely have more than one selector ...
        return new QueryResultRowIterator(context, queryStatement, sequence, results.getColumns());
    }

    @Override
    public String getPlan() {
        return results.getPlan();
    }

    @Override
    public Collection<String> getWarnings() {
        if (warnings == null) {
            // Obtain the warnings ...
            if (!results.hasWarnings()) {
                warnings = java.util.Collections.emptyList();
            } else {
                List<String> messages = new LinkedList<String>();
                for (Problem problem : results.getProblems()) {
                    if (problem.getStatus() == Status.WARNING) {
                        String msg = problem.getMessageString();
                        if (!messages.contains(msg)) messages.add(msg);
                    }
                }
                warnings = java.util.Collections.unmodifiableList(messages);
            }
        }
        return warnings;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String toString() {
        return results.toString();
    }

    @Override
    public void close() {
        sequence.close();
    }

    /**
     * The {@link NodeIterator} implementation returned by the {@link JcrQueryResult}.
     * 
     * @see JcrQueryResult#getNodes()
     */
    @NotThreadSafe
    protected static abstract class QueryResultIterator implements RangeIterator {
        protected final JcrQueryContext context;
        private NodeSequence sequence;
        private long position = 0L;
        private Batch currentBatch;

        protected QueryResultIterator( JcrQueryContext context,
                                       NodeSequence sequence ) {
            this.context = context;
            this.sequence = sequence;
        }

        @Override
        public boolean hasNext() {
            while (findNextBatch() != null) {
                if (currentBatch.hasNext()) return true;
                currentBatch = null;
            }
            return false;
        }

        protected final Batch moveToNextRow() {
            if (findNextBatch() == null || !currentBatch.hasNext()) throw new NoSuchElementException();
            currentBatch.nextRow();
            ++position;
            return currentBatch;
        }

        protected Batch findNextBatch() {
            if (currentBatch == null || !currentBatch.hasNext()) {
                currentBatch = sequence.nextBatch();
            }
            return currentBatch;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public void skip( long skipNum ) {
            for (long i = 0L; i != skipNum; ++i)
                moveToNextRow();
        }

        @Override
        public final long getSize() {
            return sequence.getRowCount();
        }
    }

    /**
     * The {@link NodeIterator} implementation returned by the {@link JcrQueryResult}.
     * 
     * @see JcrQueryResult#getNodes()
     */
    @NotThreadSafe
    protected static class QueryResultNodeIterator extends QueryResultIterator implements NodeIterator {

        private final int defaultSelectorIndex;

        protected QueryResultNodeIterator( JcrQueryContext context,
                                           NodeSequence sequence,
                                           int defaultSelectorIndex ) {
            super(context, sequence);
            this.defaultSelectorIndex = defaultSelectorIndex;
        }

        @Override
        public Node nextNode() {
            CachedNode cachedNode = moveToNextRow().getNode(defaultSelectorIndex);
            return context.getNode(cachedNode);
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
    protected static class QueryResultRowIterator extends QueryResultIterator implements RowIterator {
        protected final List<String> columnNames;
        private final Set<String> selectorNames;
        protected final Columns columns;
        protected final String query;

        protected QueryResultRowIterator( JcrQueryContext context,
                                          String query,
                                          NodeSequence sequence,
                                          Columns columns ) {
            super(context, sequence);
            this.query = query;
            this.columns = columns;
            this.columnNames = this.columns.getColumnNames();
            this.selectorNames = new HashSet<String>(columns.getSelectorNames());
        }

        public boolean hasSelector( String selectorName ) {
            return this.selectorNames.contains(selectorName);
        }

        @Override
        public Row nextRow() {
            return createRow(moveToNextRow());
        }

        @Override
        public Object next() {
            return nextRow();
        }

        protected Row createRow( Batch batch ) {
            return new MultiSelectorQueryResultRow(this, batch);
        }

        protected String getPropertyNameForColumnName( String columnName ) {
            return columns.getPropertyNameForColumnName(columnName);
        }

        protected int nodeIndexForSelector( String selector ) {
            return columns.getSelectorIndex(selector);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        protected Value jcrPath( CachedNode node ) {
            assert node != null;
            // Every node has a path ...
            return context.createValue(PropertyType.PATH, context.getPath(node));
        }

        protected Value jcrUuid( CachedNode node ) {
            assert node != null;
            // only referenceable nodes have UUIDs ...
            String uuid = context.getUuid(node);
            return uuid == null ? null : context.createValue(PropertyType.STRING, uuid);
        }

        protected Value jcrName( CachedNode node ) {
            assert node != null;
            // Every node has a name; even the root has a zero-length name ...
            return context.createValue(PropertyType.NAME, context.getName(node));
        }

        protected Value jcrLocalName( CachedNode node ) {
            assert node != null;
            // Every node has a name; even the root has a zero-length name ...
            return context.createValue(PropertyType.NAME, context.getName(node).getLocalName());
        }

        protected Value jcrDepth( CachedNode node ) {
            assert node != null;
            // Every node has a depth ...
            return context.createValue(PropertyType.LONG, context.getDepth(node));
        }

        protected Value jcrChildCount( CachedNode node ) {
            assert node != null;
            // Every node has a child count ...
            return context.createValue(PropertyType.LONG, context.getChildCount(node));
        }

        protected Value jcrId( CachedNode node ) {
            assert node != null;
            // Every node has an identifier, but we need to figure out the correct version that's exposed
            return context.createValue(PropertyType.STRING, context.getIdentifier(node));
        }

        protected Value jcrPath( String path ) {
            return context.createValue(PropertyType.PATH, path);
        }

        protected Value jcrName( String name ) {
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

        private final int selectorIndex;

        protected SingleSelectorQueryResultRowIterator( JcrQueryContext context,
                                                        String query,
                                                        NodeSequence sequence,
                                                        Columns columns ) {
            super(context, query, sequence, columns);
            int selectorIndex = columns.getSelectorIndex(columns.getSelectorNames().get(0));
            if (selectorIndex >= sequence.width()) {
                // The columns were built on top of other columns that expose multiple selectors, but this sequence only has
                // one selector ...
                selectorIndex = 0;
            }
            this.selectorIndex = selectorIndex;
        }

        @Override
        protected Row createRow( Batch batch ) {
            return new SingleSelectorQueryResultRow(this, batch, selectorIndex);
        }
    }

    protected static abstract class AbstractRow implements javax.jcr.query.Row {
        protected final QueryResultRowIterator iterator;
        protected final Batch batchAtRow;
        private Value[] values = null;

        protected AbstractRow( QueryResultRowIterator iterator,
                               Batch batchAtRow ) {
            this.iterator = iterator;
            this.batchAtRow = batchAtRow;
            assert this.iterator != null;
            assert this.batchAtRow != null;
        }

        @Override
        public Node getNode( String selectorName ) throws RepositoryException {
            int nodeIndex = iterator.nodeIndexForSelector(selectorName);
            if (nodeIndex < 0) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            CachedNode cachedNode = batchAtRow.getNode(nodeIndex);
            return cachedNode == null ? null : iterator.context.getNode(cachedNode);
        }

        protected Value getValue( String columnName,
                                  CachedNode cachedNode,
                                  int nodeIndex ) throws ItemNotFoundException, RepositoryException {
            if (cachedNode == null) return null;
            // Get the property name for the column. Note that if the column is aliased, the property name will be different;
            // otherwise, the property name will be the same as the column name ...
            String propertyName = iterator.getPropertyNameForColumnName(columnName);
            if (propertyName == null) return null;

            if (PseudoColumns.contains(propertyName, true)) {
                if (PseudoColumns.isPath(propertyName)) {
                    return iterator.jcrPath(cachedNode);
                }
                if (PseudoColumns.isName(propertyName)) {
                    return iterator.jcrName(cachedNode);
                }
                if (PseudoColumns.isLocalName(propertyName)) {
                    return iterator.jcrLocalName(cachedNode);
                }
                if (PseudoColumns.isDepth(propertyName)) {
                    return iterator.jcrDepth(cachedNode);
                }
                if (PseudoColumns.isId(propertyName)) {
                    return iterator.jcrId(cachedNode);
                }
                if (PseudoColumns.isScore(propertyName)) {
                    float score = batchAtRow.getScore(nodeIndex);
                    return iterator.jcrDouble(score);
                }
                if (PseudoColumns.isUuid(propertyName)) {
                    return iterator.jcrUuid(cachedNode);
                }
            }
            // Get the property's value ...
            Node node = iterator.context.getNode(cachedNode);
            if (node == null || !node.hasProperty(propertyName)) return null;
            Property property = node.getProperty(propertyName);
            Value value = null;
            if (property.isMultiple()) {
                Value[] values = property.getValues();
                // The array of values might be empty ...
                if (values.length > 0) {
                    // Use only the first value of a multi-valued property ...
                    value = property.getValues()[0];
                }
                // Otherwise the value will be null
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
            Node node = getNode(selectorName);
            return node == null ? null : node.getPath();
        }

        @Override
        public double getScore( String selectorName ) throws RepositoryException {
            if (!iterator.hasSelector(selectorName)) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            int nodeIndex = iterator.columns.getSelectorNames().indexOf(selectorName);
            return batchAtRow.getScore(nodeIndex);
        }

    }

    protected static class SingleSelectorQueryResultRow extends AbstractRow {
        protected final CachedNode cachedNode;
        protected final Node node;
        protected final int selectorIndex;

        protected SingleSelectorQueryResultRow( QueryResultRowIterator iterator,
                                                Batch batchAtRow,
                                                int selectorIndex ) {
            super(iterator, batchAtRow);
            this.selectorIndex = selectorIndex;
            this.cachedNode = batchAtRow.getNode(selectorIndex);
            this.node = iterator.context.getNode(cachedNode);
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
            return getValue(columnName, cachedNode, selectorIndex);
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
        public double getScore() {
            return batchAtRow.getScore(selectorIndex);
        }

        @Override
        public double getScore( String selectorName ) throws RepositoryException {
            if (!iterator.hasSelector(selectorName)) {
                throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
            }
            return getScore();
        }
    }

    protected static class MultiSelectorQueryResultRow extends AbstractRow {

        protected MultiSelectorQueryResultRow( QueryResultRowIterator iterator,
                                               Batch batchAtRow ) {
            super(iterator, batchAtRow);
        }

        @Override
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            String selectorName = iterator.columns.getSelectorNameForColumnName(columnName);
            int nodeIndex = iterator.columns.getSelectorIndex(selectorName);
            if (nodeIndex == -1) {
                throw new RepositoryException(JcrI18n.queryResultsDoNotIncludeColumn.text(columnName, iterator.query));
            }
            CachedNode cachedNode = batchAtRow.getNode(nodeIndex);
            return getValue(columnName, cachedNode, nodeIndex);
        }
    }
}
