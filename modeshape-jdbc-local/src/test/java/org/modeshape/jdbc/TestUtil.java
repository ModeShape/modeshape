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
package org.modeshape.jdbc;

import static org.mockito.Mockito.when;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.mockito.Mockito;

/**
 * This provides common result set metadata used by various tests
 */
public class TestUtil {

    public static final String STRING = PropertyType.nameFromValue(PropertyType.STRING).toUpperCase();
    public static final String DOUBLE = PropertyType.nameFromValue(PropertyType.DOUBLE).toUpperCase();
    public static final String LONG = PropertyType.nameFromValue(PropertyType.LONG).toUpperCase();
    public static final String BOOLEAN = PropertyType.nameFromValue(PropertyType.BOOLEAN).toUpperCase();
    public static final String DATE = PropertyType.nameFromValue(PropertyType.DATE).toUpperCase();
    public static final String PATH = PropertyType.nameFromValue(PropertyType.STRING).toUpperCase();
    public static final String BINARY = PropertyType.nameFromValue(PropertyType.BINARY).toUpperCase();

    public static final String REFERENCE = PropertyType.nameFromValue(PropertyType.REFERENCE).toUpperCase();

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

        TUPLES.add(new Object[] {"r1c1", (long)1, null, null, (double)1, true, cal_instance, "Heres my data at r1".getBytes(),
            null});

        cal_instance = createTestCalendar();

        TUPLES.add(new Object[] {"r2c1", (long)2, null, null, (double)2, false, cal_instance, "Heres my data r2   ".getBytes(),
            null});

        cal_instance = createTestCalendar();

        TUPLES.add(new Object[] {"r3c1", (long)3, null, null, (double)3, true, cal_instance, "Heres my data at r3  ".getBytes(),
            null});

        cal_instance = createTestCalendar();

        TUPLES.add(new Object[] {"r4c1", 4L, null, null, 4D, Boolean.TRUE, cal_instance, "Heres  my  data    r4  ".getBytes(),
            null});
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

    public static int minorVersion( String versionString ) {
        String[] coords = versionString.split("[.-]");
        @SuppressWarnings( "unused" )
        final int major = Integer.parseInt(coords[0]);

        return Integer.parseInt(coords[1]);
    }

    public static int majorVersion( String versionString ) {
        String[] coords = versionString.split("[.-]");
        return Integer.parseInt(coords[0]);
    }

    public static QueryResult createQueryResult() {
        final Node[] nodes = createNodes();

        QueryResult qr = new org.modeshape.jcr.api.query.QueryResult() {

            @Override
            public String getPlan() {
                return null;
            }

            @Override
            public Collection<String> getWarnings() {
                return Collections.emptySet();
            }

            @Override
            public String[] getColumnNames() {
                String[] cns = new String[COLUMN_NAMES.length];
                System.arraycopy(COLUMN_NAMES, 0, cns, 0, COLUMN_NAMES.length);
                return cns;
            }

            @Override
            public boolean isEmpty() {
                return nodes.length == 0;
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

            @Override
            public void close() {
                // nothing to do
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
            final Object[] tuple = tuples.next();
            // Get the next row ...
            nextRow = getNextRow(tuple);
            if (nextRow != null) return true;
            --numRows;
        }
        return false;
    }

    private Row getNextRow( Object[] tuple ) {
        return new QueryResultRow(this, nodes, tuple);
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

class QueryResultRow implements javax.jcr.query.Row {
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

    @Override
    public String getPath() throws RepositoryException {
        if (nodes.length == 1) return nodes[0].getPath();
        throw new RepositoryException("More than one selector");
    }

    @Override
    public String getPath( String selectorName ) throws RepositoryException {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getName().equals(selectorName)) {
                return nodes[i].getPath();
            }
        }
        return null;
    }

    @Override
    public double getScore() /*throws RepositoryException*/{
        throw new UnsupportedOperationException();
    }

    @Override
    public double getScore( String selectorName ) /* throws RepositoryException */{
        throw new UnsupportedOperationException();
    }

    @Override
    public Value getValue( String arg0 ) throws ItemNotFoundException {
        for (int i = 0; i < TestUtil.COLUMN_NAMES.length; i++) {
            if (TestUtil.COLUMN_NAMES[i].equals(arg0)) {
                return JdbcJcrValueFactory.createValue(tuple[i]);
            }
        }

        throw new ItemNotFoundException("Item " + arg0 + " not found");
    }

    @Override
    public Value[] getValues() {
        Value[] values = new Value[tuple.length];
        for (int i = 0; i < tuple.length; i++) {
            values[i] = JdbcJcrValueFactory.createValue(tuple[i]);

        }
        return values;
    }
}
