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
package org.modeshape.jdbc.metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * The MetaDataQueryResult is used to provide {@link NodeIterator} and the {@link RowIterator} in order to provide query results
 * when metadata is requested. This is done because there are no sql queries that can be executed to obtain certain types of
 * metadata in a return QueryResult from the JcrEngine.
 */
public class MetaDataQueryResult implements javax.jcr.query.QueryResult {

    private ResultSetMetaData rsmd;
    private String[] columnNames = null;
    private List<List<?>> tuplesArray = null;

    public static MetaDataQueryResult createResultSet( List<List<?>> records,
                                                       ResultSetMetaData resultSetMetaData ) throws SQLException {
        try {
            MetaDataQueryResult mdqr = new MetaDataQueryResult(records, resultSetMetaData);
            return mdqr;
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

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryResult#getColumnNames()
     */
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
        RowIterator ri = new QueryResultRowIterator(tuplesArray, columnNames);
        return ri;
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

/**
 */
class QueryResultNodeIterator implements NodeIterator {
    private final Node[] nodes;
    private final int size;
    private long position = 0L;

    protected QueryResultNodeIterator( Node[] nodes ) {
        this.nodes = nodes;
        this.size = nodes.length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NodeIterator#nextNode()
     */
    public Node nextNode() {
        Node node = nodes[(int)(position)];
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
        return (nodes.length > position);
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
            final List<?> tuple = tuples.next();
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

    /**
     * @param tuple
     * @return Row
     * @throws RepositoryException
     */
    private Row getNextRow( List<?> tuple ) throws RepositoryException {
        return new QueryResultRow(this, tuple, colNames);
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

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getNode()
     */
    @Override
    public Node getNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getNode( String selectorName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getPath()
     */
    @Override
    public String getPath() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getPath(java.lang.String)
     */
    @Override
    public String getPath( String selectorName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getScore()
     */
    @Override
    public double getScore() /* throws RepositoryException */{
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getScore(java.lang.String)
     */
    @Override
    public double getScore( String selectorName ) /* throws RepositoryException */{
        throw new UnsupportedOperationException();
    }

    /**
     * @throws ItemNotFoundException
     */
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

    /**
     * @throws RepositoryException
     */
    @Override
    public Value[] getValues() throws RepositoryException {
        Value[] values = new Value[tuple.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = createValue(tuple.get(i));

        }
        return values;
    }

    private Value createValue( final Object value ) {

        if (value == null) return null;

        Value rtnvalue = new Value() {
            final Object valueObject = value;

            @Override
            public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException {
                if (value instanceof Boolean) {
                    return ((Boolean)valueObject).booleanValue();
                }
                throw new ValueFormatException("Value not a Boolean");
            }

            @Override
            public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException {
                if (value instanceof Date) {
                    Calendar c = Calendar.getInstance();
                    c.setTime((Date)value);

                    return c;
                }
                throw new ValueFormatException("Value not instance of Date");
            }

            @Override
            public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException {
                if (value instanceof Double) {
                    return ((Double)valueObject).doubleValue();
                }

                throw new ValueFormatException("Value not a Double");
            }

            @Override
            public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException {
                if (value instanceof Long) {
                    return ((Long)valueObject).longValue();
                }
                throw new ValueFormatException("Value not a Long");
            }

            /**
             * {@inheritDoc}
             * 
             * @see javax.jcr.Value#getBinary()
             */
            @Override
            public Binary getBinary() throws RepositoryException {
                if (value instanceof Binary) {
                    return ((Binary)valueObject);
                }
                if (value instanceof byte[]) {
                    final byte[] bytes = (byte[])value;
                    return new Binary() {

                        @Override
                        public void dispose() {
                        }

                        @Override
                        public long getSize() {
                            return bytes.length;
                        }

                        @Override
                        public InputStream getStream() {
                            return new ByteArrayInputStream(bytes);
                        }

                        @Override
                        public int read( byte[] b,
                                         long position ) throws IOException {
                            if (getSize() <= position) return -1;
                            InputStream stream = null;
                            IOException error = null;
                            try {
                                stream = getStream();
                                // Read/skip the next 'position' bytes ...
                                long skip = position;
                                while (skip > 0) {
                                    long skipped = stream.skip(skip);
                                    if (skipped <= 0) return -1;
                                    skip -= skipped;
                                }
                                return stream.read(b);
                            } catch (IOException e) {
                                error = e;
                                throw e;
                            } finally {
                                if (stream != null) {
                                    try {
                                        stream.close();
                                    } catch (RuntimeException t) {
                                        // Only throw if we've not already
                                        // thrown an exception ...
                                        if (error == null) throw t;
                                    } catch (IOException t) {
                                        // Only throw if we've not already
                                        // thrown an exception ...
                                        if (error == null) throw t;
                                    }
                                }
                            }
                        }

                    };
                }
                throw new ValueFormatException("Value not a Binary");
            }

            /**
             * {@inheritDoc}
             * 
             * @see javax.jcr.Value#getDecimal()
             */
            @Override
            public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
                if (value instanceof BigDecimal) {
                    return ((BigDecimal)valueObject);
                }
                throw new ValueFormatException("Value not a Decimal");
            }

            @Override
            public InputStream getStream() throws IllegalStateException, RepositoryException {
                if (value instanceof Binary) {
                    return ((Binary)valueObject).getStream();
                }
                if (value instanceof InputStream) {
                    return ((InputStream)valueObject);
                }
                throw new ValueFormatException("Value not an InputStream");
            }

            @Override
            public String getString() throws IllegalStateException {
                if (value instanceof String) {
                    return (String)valueObject;
                }
                return valueObject.toString();
            }

            @Override
            public int getType() {
                return 1;
            }

        };

        return rtnvalue;

    }

}
