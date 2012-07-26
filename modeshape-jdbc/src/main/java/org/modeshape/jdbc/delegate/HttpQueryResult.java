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

package org.modeshape.jdbc.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.jcr.api.query.QueryResult;
import org.modeshape.jdbc.JdbcJcrValueFactory;
import org.modeshape.web.jcr.rest.client.domain.QueryRow;

/**
 * A simple implementation of the {@link QueryResult} interface used to iterate over list of {@link QueryRow rows}
 * 
 * @author Horia Chiorean
 */
public final class HttpQueryResult implements QueryResult {

    protected final List<HttpRow> rows = new ArrayList<HttpRow>();

    /**
     * [columnName, columnType] mappings
     */
    protected final Map<String, String> columns = new LinkedHashMap<String, String>();

    HttpQueryResult( List<QueryRow> queryRows ) {
        assert queryRows != null;

        if (!queryRows.isEmpty()) {
            QueryRow firstQueryRow = queryRows.get(0);
            Collection<String> queryColumnNames = firstQueryRow.getColumnNames();
            for (String queryColumnName : queryColumnNames) {
                columns.put(queryColumnName, firstQueryRow.getColumnType(queryColumnName));
            }
        }

        for (QueryRow queryRow : queryRows) {
            rows.add(new HttpRow(queryRow));
        }
    }

    @Override
    public String[] getColumnNames() {
        return columns.keySet().toArray(new String[columns.size()]);
    }

    @Override
    public RowIterator getRows() {
        return new HttpRowIterator();
    }

    @Override
    public NodeIterator getNodes() {
        throw new UnsupportedOperationException("Method getNodes() not supported");
    }

    @Override
    public String[] getSelectorNames() {
        throw new UnsupportedOperationException("Method getSelectorNames() not supported");
    }

    @Override
    public String[] getColumnTypes() {
        return columns.values().toArray(new String[columns.size()]);
    }

    private class HttpRowIterator implements RowIterator {

        private static final int EMPTY_CURSOR = -1;
        private int cursor = rows.isEmpty() ? EMPTY_CURSOR : 0;

        protected HttpRowIterator() {
        }

        @Override
        public Row nextRow() {
            if (cursor == -1 || cursor >= rows.size()) {
                throw new NoSuchElementException("No more rows to iterate over");
            }
            return rows.get(cursor++);
        }

        @Override
        public void skip( long skipNum ) {
            if (skipNum < 0) {
                throw new IllegalArgumentException("skipNum must be a positive value");
            }
            int availableRowsCount = rows.size() - cursor;
            if (skipNum > availableRowsCount) {
                throw new NoSuchElementException("Skip would go past collection end");
            }
            cursor += skipNum;
        }

        @Override
        public long getSize() {
            return rows.size();
        }

        @Override
        public long getPosition() {
            return cursor;
        }

        @Override
        public boolean hasNext() {
            return cursor != -1 && cursor < rows.size();
        }

        @Override
        public Object next() {
            return nextRow();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Method remove() not supported by this iterator");
        }
    }

    private class HttpRow implements Row {
        private final Map<String, Value> valuesMap = new LinkedHashMap<String, Value>();

        protected HttpRow( QueryRow row ) {
            assert row != null;
            for (String columnName : columns.keySet()) {
                Object queryRowValue = row.getValue(columnName);
                valuesMap.put(columnName, JdbcJcrValueFactory.createValue(queryRowValue));
            }
        }

        @Override
        public Node getNode() {
            throw new UnsupportedOperationException("Method getNode() not supported");
        }

        @Override
        public Value[] getValues() {
            return valuesMap.values().toArray(new Value[valuesMap.size()]);
        }

        @Override
        public Value getValue( String columnName ) {
            return valuesMap.get(columnName);
        }

        @Override
        public Node getNode( String selectorName ) {
            throw new UnsupportedOperationException("Method getNode(selectorName) not supported");
        }

        @Override
        public String getPath() {
            throw new UnsupportedOperationException("Method getPath() not supported");
        }

        @Override
        public String getPath( String selectorName ) {
            throw new UnsupportedOperationException("Method getPath(selectorName) not supported");
        }

        @Override
        public double getScore() {
            throw new UnsupportedOperationException("Method getScore() not supported");
        }

        @Override
        public double getScore( String selectorName ) {
            throw new UnsupportedOperationException("Method getScore( String selectorName ) not supported");
        }
    }
}
