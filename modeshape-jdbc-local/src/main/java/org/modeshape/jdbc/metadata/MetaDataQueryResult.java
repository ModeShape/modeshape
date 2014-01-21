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
package org.modeshape.jdbc.metadata;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.jdbc.JdbcJcrValueFactory;

/**
 * The MetaDataQueryResult is used to provide {@link NodeIterator} and the {@link RowIterator} in order to provide query results
 * when metadata is requested. This is done because there are no sql queries that can be executed to obtain certain types of
 * metadata in a return {@link QueryResult} from the ModeShapeEngine.
 */
public class MetaDataQueryResult implements javax.jcr.query.QueryResult {

    private ResultSetMetaData rsmd;
    private String[] columnNames = null;
    private List<List<?>> tuplesArray = null;

    public static MetaDataQueryResult createResultSet( List<List<?>> records,
                                                       ResultSetMetaData resultSetMetaData ) throws SQLException {
        try {
            return new MetaDataQueryResult(records, resultSetMetaData);
        } catch (RepositoryException e) {
            if (e.getCause() instanceof SQLException) throw (SQLException)e.getCause();
            throw new SQLException(e);
        }
    }

    MetaDataQueryResult( List<List<?>> tuples,
                         ResultSetMetaData rsmd ) throws RepositoryException {
        this.rsmd = rsmd;
        this.tuplesArray = tuples;
        getColumnNames();
    }

    @Override
    public String[] getColumnNames() throws RepositoryException {
        if (columnNames != null) {
            return columnNames;
        }

        try {
            columnNames = new String[rsmd.getColumnCount()];
            for (int col = 0; col < columnNames.length; col++) {
                columnNames[col] = rsmd.getColumnName(col + 1);
            }

            return columnNames;
        } catch (SQLException sqle) {
            throw new RepositoryException(sqle);
        }
    }

    @Override
    public NodeIterator getNodes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RowIterator getRows() {
        return new QueryResultRowIterator(tuplesArray, columnNames);
    }

    public String[] getColumnTypes() {
        String[] columnTypes = new String[columnNames.length];
        for (int i = 0; i <= columnNames.length; i++) {
            try {
                columnTypes[i] = rsmd.getColumnTypeName(i + 1);
            } catch (SQLException e) {
                columnTypes[i] = "NotFound";
            }
        }
        return columnTypes;
    }

    @Override
    public String[] getSelectorNames() {
        throw new UnsupportedOperationException();
    }

}

class QueryResultRowIterator implements RowIterator {
    private final Iterator<List<?>> tuples;
    private long position = 0L;
    private long numRows;
    private Row nextRow;
    private String[] colNames;

    protected QueryResultRowIterator( List<List<?>> tuplesArray,
                                      String[] columnNames ) {
        this.tuples = tuplesArray.iterator();
        this.numRows = tuplesArray.size();
        this.colNames = columnNames;
    }

    public boolean hasSelector( String selectorName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.RowIterator#nextRow()
     */
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
        position++;
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
            final List<?> tuple = tuples.next();
            // Get the next row ...
            nextRow = getNextRow(tuple);
            if (nextRow != null) return true;
            --numRows;
        }
        return false;
    }

    private Row getNextRow( List<?> tuple ) {
        return new QueryResultRow(this, tuple, colNames);
    }

    @Override
    public Object next() {
        return nextRow();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

class QueryResultRow implements Row {
    protected final QueryResultRowIterator iterator;
    protected final List<?> tuple;
    private String[] columnNames = null;

    protected QueryResultRow( QueryResultRowIterator iterator,
                              List<?> tuple,
                              String[] colNames ) {
        this.iterator = iterator;
        this.tuple = tuple;
        this.columnNames = colNames;
    }

    @Override
    public Node getNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getNode( String selectorName ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPath( String selectorName ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getScore() /* throws RepositoryException */{
        throw new UnsupportedOperationException();
    }

    @Override
    public double getScore( String selectorName ) /* throws RepositoryException */{
        throw new UnsupportedOperationException();
    }

    @Override
    public Value getValue( String arg0 ) throws ItemNotFoundException {
        int pos = getColumnPosition(arg0);
        if (pos >= 0) {
            return createValue(tuple.get(pos));
        }

        throw new ItemNotFoundException("Item " + arg0 + " not found");
    }

    private int getColumnPosition( String colName ) {
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].equals(colName)) return i;
        }
        return -1;

    }

    @SuppressWarnings( "unused" )
    @Override
    public Value[] getValues() throws RepositoryException {
        Value[] values = new Value[tuple.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = createValue(tuple.get(i));

        }
        return values;
    }

    private Value createValue( final Object value ) {
        return JdbcJcrValueFactory.createValue(value);
    }

}
