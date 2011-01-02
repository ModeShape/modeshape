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
package org.modeshape.jdbc;

import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.mockito.Mockito;

/**
 * This provides common result set metadata used by various tests
 */
public class TestUtil {

    public static final String STRING = PropertyType.nameFromValue(PropertyType.STRING);
    public static final String DOUBLE = PropertyType.nameFromValue(PropertyType.DOUBLE);
    public static final String LONG = PropertyType.nameFromValue(PropertyType.LONG);
    public static final String BOOLEAN = PropertyType.nameFromValue(PropertyType.BOOLEAN);
    public static final String DATE = PropertyType.nameFromValue(PropertyType.DATE);
    public static final String PATH = PropertyType.nameFromValue(PropertyType.PATH);
    public static final String BINARY = PropertyType.nameFromValue(PropertyType.BINARY);

    public static final String REFERENCE = PropertyType.nameFromValue(PropertyType.REFERENCE);

    public static String[] COLUMN_NAMES;
    
    public static String TIME_ZONE = "Europe/London";
    
    public static DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
    public static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    public static DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss a"); //$NON-NLS-1$

    public static String USE_TIMESTAMP_FOR_SOURCE = "2004-06-30 02:39:10.201";
    public static String USE_TIME_FOR_SOURCE = "02:39:10";
    public static String USE_DATE_FOR_SOURCE = "2004-06-30";
    
    // this is the expected date based on the test using "GMT-05:00" when testing rsultset methods that take a calendar
    public static String EXPECTED_TIMESTAMP_FOR_TARGET = "2004-06-29 20:39:10.201";
    public static String EXPECTED_TIME_FOR_TARGET = "20:39:10";
    public static String EXPECTED_DATE_FOR_TARGET = "2004-06-29";
    public static String EXPECTED_TIMEZONE = "America/Chicago";
    
    
    public static interface COLUMN_NAME_PROPERTIES {
        public static final String PROP_A = "propA";
        public static final String PROP_B = "propB";
        public static final String PROP_C = "propC";
        public static final String PROP_D = "propD";
        public static final String PROP_E = "propE";
        public static final String PROP_F = "propF";
        public static final String PROP_G = "propG";
        public static final String PROP_H = "propH";
        public static final String PROP_I = "propI";

    }

    public static String[] TABLE_NAMES;
    public static String[] TYPE_NAMES;
    public static String[] NODE_NAMES;

    public static List<Object[]> TUPLES;

    public static final String SQL_SELECT = "Select propA FROM typeA";

    public static final String DRIVER_VERSION = JdbcI18n.driverVersion.text();

    static {
    	
        Calendar cal_instance = createTestCalendar();
 
        // The column names must match the number of columns in #TUPLES
        COLUMN_NAMES = new String[] {COLUMN_NAME_PROPERTIES.PROP_A, COLUMN_NAME_PROPERTIES.PROP_B, COLUMN_NAME_PROPERTIES.PROP_C,
            COLUMN_NAME_PROPERTIES.PROP_D, COLUMN_NAME_PROPERTIES.PROP_E, COLUMN_NAME_PROPERTIES.PROP_F,
            COLUMN_NAME_PROPERTIES.PROP_G, COLUMN_NAME_PROPERTIES.PROP_H, COLUMN_NAME_PROPERTIES.PROP_I};
        TABLE_NAMES = new String[] {"typeA", "typeB", "typeA", "", "typeA"};
        // The TYPE_NAMES correspond to the column value types defined in #TUPLES
        TYPE_NAMES = new String[] {STRING, LONG, PATH, REFERENCE, DOUBLE, BOOLEAN, DATE, BINARY, LONG};

        NODE_NAMES = new String[] {"node1", "node2"};
        // Provides the resultset rows
        TUPLES = new ArrayList<Object[]>();

        /*
         *  the tuples data types for each column correspond to @see TYPE_NAMES
         */
         
        TUPLES.add(new Object[] {"r1c1", new Long(1), null, null, new Double(1), new Boolean(true),  cal_instance,
            new String("Heres my data at r1").getBytes(), null} );
        
        cal_instance = createTestCalendar();
        
        TUPLES.add(new Object[] {"r2c1", new Long(2), null, null, new Double(2), new Boolean(false), cal_instance,
            new String("Heres my data r2   ").getBytes(), null});
        
        cal_instance = createTestCalendar();
       
        TUPLES.add(new Object[] {"r3c1", new Long(3), null, null, new Double(3), new Boolean(true), cal_instance,
            new String("Heres my data at r3  ").getBytes(), null});
        
        cal_instance = createTestCalendar();
        
        TUPLES.add(new Object[] {"r4c1", 4L, null, null, 4D, new Boolean(true).booleanValue(), cal_instance,
            new String("Heres  my  data    r4  ").getBytes(), null});
 
    }
    
    private static Calendar createTestCalendar() {
        Calendar cal_instance = new GregorianCalendar();
        cal_instance.clear();
        cal_instance.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
    	
        cal_instance.set(Calendar.MONTH, Calendar.JUNE);
        cal_instance.set(Calendar.DAY_OF_MONTH, 30);
        cal_instance.set(Calendar.YEAR, 2004);        
        cal_instance.set(Calendar.HOUR_OF_DAY, 2);
        cal_instance.set(Calendar.MINUTE, 39);
        cal_instance.set(Calendar.SECOND, 10);
        cal_instance.set(Calendar.MILLISECOND, 201);
        cal_instance.set(Calendar.AM_PM, Calendar.AM);
        return cal_instance;

    }

    static Node[] createNodes() {
        Node[] nodes = new Node[NODE_NAMES.length];
        for (int i = 0; i < NODE_NAMES.length; i++) {

            // Create the new definition ...
            Node n = Mockito.mock(Node.class);
            try {
                when(n.getName()).thenReturn(NODE_NAMES[i]);
            } catch (RepositoryException e) {
            }
            nodes[i] = n;

        }
        return nodes;
    }

    public static int minorVersion() {

        String[] coords = DRIVER_VERSION.split("[.-]");
        @SuppressWarnings( "unused" )
        final int major = Integer.parseInt(coords[0]);
        final int minor = Integer.parseInt(coords[1]);

        return minor;

    }

    public static int majorVersion() {
        String[] coords = DRIVER_VERSION.split("[.-]");
        final int major = Integer.parseInt(coords[0]);

        return major;

    }

    public static QueryResult createQueryResult() {
        final Node[] nodes = createNodes();

        QueryResult qr = new org.modeshape.jcr.api.query.QueryResult() {

            @Override
            public String[] getColumnNames() {
                String[] cns = new String[COLUMN_NAMES.length];
                System.arraycopy(COLUMN_NAMES, 0, cns, 0, COLUMN_NAMES.length);
                return cns;
            }

            @Override
            public NodeIterator getNodes() {
                List<Node> nodeArray = new ArrayList<Node>();
                for (int i = 0; i < nodes.length; i++) {
                    nodeArray.add(nodes[i]);
                }
                return new QueryResultNodeIterator(nodeArray);
            }

            @Override
            public RowIterator getRows() {
                List<Object[]> tuplesArray = new ArrayList<Object[]>(TUPLES);
                RowIterator ri = new QueryResultRowIterator(nodes, SQL_SELECT, tuplesArray.iterator(), tuplesArray.size());
                return ri;
            }

            @Override
            public String[] getColumnTypes() {
                return null;
            }

            @Override
            public String[] getSelectorNames() {
                return null;
            }
        };
        return qr;
    }
}

/**
 */
class QueryResultNodeIterator implements NodeIterator {
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

class QueryResultRowIterator implements RowIterator {
    private final Iterator<Object[]> tuples;
    protected final String query;
    private long position = 0L;
    private long numRows;
    private Row nextRow;
    private Node[] nodes;

    protected QueryResultRowIterator( Node[] nodes,
                                      String query,
                                      Iterator<Object[]> tuples,
                                      long numRows ) {
        this.tuples = tuples;
        this.query = query;
        this.numRows = numRows;
        this.nodes = nodes;

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
            final Object[] tuple = tuples.next();
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
    private Row getNextRow( Object[] tuple ) throws RepositoryException {
        return new QueryResultRow(this, nodes, tuple);
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

class QueryResultRow implements Row, org.modeshape.jcr.api.query.Row {
    protected final QueryResultRowIterator iterator;
    private Node[] nodes;
    protected final Object[] tuple;

    protected QueryResultRow( QueryResultRowIterator iterator,
                              Node[] nodes,
                              Object[] tuple ) {
        this.iterator = iterator;
        this.tuple = tuple;
        this.nodes = nodes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getNode()
     */
    @Override
    public Node getNode() throws RepositoryException {
        if (nodes.length == 1) return nodes[0];
        throw new RepositoryException("More than one selector");
    }

    @Override
    public Node getNode( String selectorName ) throws RepositoryException {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getName().equals(selectorName)) {
                return nodes[i];
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getPath()
     */
    @Override
    public String getPath() throws RepositoryException {
        if (nodes.length == 1) return nodes[0].getPath();
        throw new RepositoryException("More than one selector");
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getPath(java.lang.String)
     */
    @Override
    public String getPath( String selectorName ) throws RepositoryException {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getName().equals(selectorName)) {
                return nodes[i].getPath();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Row#getScore()
     */
    @Override
    public double getScore() /*throws RepositoryException*/{
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
        for (int i = 0; i < TestUtil.COLUMN_NAMES.length; i++) {
            if (TestUtil.COLUMN_NAMES[i].equals(arg0)) {
                return createValue(tuple[i]);
            }
        }

        throw new ItemNotFoundException("Item " + arg0 + " not found");
    }

    /**
     * @throws RepositoryException
     */
    @Override
    public Value[] getValues() throws RepositoryException {
        Value[] values = new Value[tuple.length];
        for (int i = 0; i < tuple.length; i++) {
            values[i] = createValue(tuple[i]);

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
                	
                	Calendar t = new GregorianCalendar();
                	t.clear();
                	t.setTimeZone(TimeZone.getTimeZone(TestUtil.TIME_ZONE));
                	t.setTimeInMillis( ((Date)value).getTime());
                	return t;
 
                } else if (value instanceof Calendar) {

                	return (Calendar) value;
                	
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
                                        // Only throw if we've not already thrown an exception ...
                                        if (error == null) throw t;
                                    } catch (IOException t) {
                                        // Only throw if we've not already thrown an exception ...
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
