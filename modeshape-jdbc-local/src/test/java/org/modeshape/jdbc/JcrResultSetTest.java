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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.jcr.query.QueryResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jdbc.util.TimestampWithTimezone;

/**
 * 
 *
 */
public class JcrResultSetTest {

    @Mock
    private JcrStatement statement;

    private QueryResult result;

    private JcrResultSet resultSet;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        result = TestUtil.createQueryResult();
        resultSet = new JcrResultSet(statement, result, null);
        
        Calendar londonTime = new GregorianCalendar();
        londonTime.clear();
        londonTime.setTimeZone(TimeZone.getTimeZone(TestUtil.TIME_ZONE));
        
        TimestampWithTimezone.resetCalendar(TimeZone.getTimeZone(TestUtil.TIME_ZONE)); //$NON-NLS-1$ 
 
    }

    @After
    public void afterEach() throws Exception {
        result = null;
        resultSet = null;
    }

    @Test
    public void shouldHaveResultSet() {
        assertThat(resultSet, is(notNullValue()));
    }

    @Test
    public void shouldCallNext() throws SQLException {
        // iterate the full resultset
        for (int i = 1; i <= TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
        }
        // after all rows have been retrieved, the resultset
        // should return false
        assertThat(resultSet.next(), is(false));
    }

    @Test
    public void shouldCallGetRow() throws SQLException {
        // first call, before current row being set,
        // should return zero to indicate positioning pre-firsts row
        assertThat(resultSet.getRow(), is(0));
        // set i=1 because row position is 1 based
        for (int i = 1; i <= TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            assertThat(resultSet.getRow(), is(i));

        }
    }

    @Test
    public void shouldCallIsBeforeFirst() throws SQLException {
        assertThat(resultSet.isBeforeFirst(), is(true));
        // as the resultset is being processed
        // verify isBeforeFirst is only valid before processing the resultset
        for (int i = 1; i <= TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            assertThat(resultSet.isBeforeFirst(), is(false));

        }
        assertThat(resultSet.next(), is(false));
        assertThat(resultSet.isBeforeFirst(), is(false));
    }

    @Test
    public void shouldCallIsFirst() throws SQLException {
        assertThat(resultSet.isFirst(), is(false));
        // as the resultset is being processed
        // verify isFirst is only valid for the first row of the resultset
        for (int i = 1; i <= TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            if (i == 1) {
                assertThat(resultSet.isFirst(), is(true));
            } else {
                assertThat(resultSet.isFirst(), is(false));
            }
        }
        assertThat(resultSet.next(), is(false));
        assertThat(resultSet.isFirst(), is(false));

    }

    @Test
    public void shouldCallIsLast() throws SQLException {
        resultSet.isLast();
        assertThat(resultSet.isLast(), is(false));
        // as the resultset is being processed
        // verify isLast only valid for the last row of the resultset
        for (int i = 1; i <= TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            if (i == TestUtil.TUPLES.size()) {
                assertThat(resultSet.isLast(), is(true));
            } else {
                assertThat(resultSet.isLast(), is(false));
            }
        }
        assertThat(resultSet.next(), is(false));
        assertThat(resultSet.isLast(), is(false));
    }

    @Test
    public void shouldCallIsAfterLast() throws SQLException {
        assertThat(resultSet.isAfterLast(), is(false));
        // as the resultset is being processed
        // verify isAfterLast is only valid after processing all rows in the resultset
        for (int i = 1; i <= TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            assertThat(resultSet.isAfterLast(), is(false));
        }
        assertThat(resultSet.next(), is(false));
        assertThat(resultSet.isAfterLast(), is(true));
    }

    @Test
    public void shouldCallGetStringUsingColmnName() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.STRING);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            assertThat(resultSet.getString(TestUtil.COLUMN_NAMES[col]), is(tuple[col]));

        }
    }

    @Test
    public void shouldCallGetStringUsingColmnIndex() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.STRING);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            // need to increment because ResultSet is 1 based.
            assertThat(resultSet.getString(col + 1), is(tuple[col]));

        }
    }
    
    /**
     * MODE-1007
     * @throws SQLException
     */
    @Test
    public void shouldCallGetLongReturnZeroWhenNullUsingColumnName() throws SQLException {
        int col = 8;
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            assertThat(resultSet.getLong(TestUtil.COLUMN_NAMES[col]), is(  new Long(0).longValue()));

        }
    }    

    @Test
    public void shouldCallGetLongUsingColumnName() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.LONG);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            assertThat(resultSet.getLong(TestUtil.COLUMN_NAMES[col]), is(tuple[col]));

        }
    }

    @Test
    public void shouldCallGetLongUsingColmnIndex() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.LONG);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            // need to increment because ResultSet is 1 based.
            assertThat(resultSet.getLong(col + 1), is(tuple[col]));

        }
    }

    @Test
    public void shouldCallGetDoubleUsingColumnName() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DOUBLE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            assertThat(resultSet.getDouble(TestUtil.COLUMN_NAMES[col]), is(tuple[col]));

        }
    }

    @Test
    public void shouldCallGetDoubleUsingColmnIndex() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DOUBLE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            // need to increment because ResultSet is 1 based.
            assertThat(resultSet.getDouble(col + 1), is(tuple[col]));

        }
    }

    @Test
    public void shouldCallGetBooleanUsingColumnName() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.BOOLEAN);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            assertThat(resultSet.getBoolean(TestUtil.COLUMN_NAMES[col]), is(tuple[col]));

        }
    }

    @Test
    public void shouldCallGetBooleanUsingColmnIndex() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.BOOLEAN);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            // need to increment because ResultSet is 1 based.
            assertThat(resultSet.getBoolean(col + 1), is(tuple[col]));

        }
    }

    @Test
    public void shouldCallGetDateUsingColumnName() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            
            java.sql.Date r = resultSet.getDate(col + 1);

            // the result date should match the date coming from the souce, no change should occur 
            // somewhere based on calendar or timezone           

           assertThat(r.toString(), is(TestUtil.USE_DATE_FOR_SOURCE));
        }
    }

    @Test
    public void shouldCallGetDateUsingColumnNameAndCalendar() throws SQLException {
    	
    	Calendar localTime = new GregorianCalendar();
    	localTime.setTimeZone(TimeZone.getTimeZone(TestUtil.EXPECTED_TIMEZONE));

        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            
            java.sql.Date resultDate = resultSet.getDate(col + 1, localTime);
            
            assertThat(resultDate.toString(), is(TestUtil.EXPECTED_DATE_FOR_TARGET));
        }

    }

    @Test
    public void shouldCallGetDateUsingColmnIndex() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            
            java.sql.Date r = resultSet.getDate(col + 1);

          // the result date should match the date coming from the souce, no change should occur 
          // somewhere based on calendar or timezone           

            assertThat(r.toString(), is(TestUtil.USE_DATE_FOR_SOURCE));
        }
    }

    @Test
    public void shouldCallGetDateUsingColmnIndexAndCalendar() throws SQLException {  
    	    	
    	Calendar localTime = new GregorianCalendar();
    	localTime.setTimeZone(TimeZone.getTimeZone(TestUtil.EXPECTED_TIMEZONE));

        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));

            java.sql.Date resultDate = resultSet.getDate(col + 1, localTime);
            
            assertThat(resultDate.toString(), is(TestUtil.EXPECTED_DATE_FOR_TARGET));
        }
    }

    @Test
    public void shouldCallGetTimeUsingColumnName() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            
            java.sql.Time r = resultSet.getTime(col + 1);

            // the result date should match the date coming from the souce, no change should occur 
            // somewhere based on calendar or timezone           

           assertThat(r.toString(), is(TestUtil.USE_TIME_FOR_SOURCE));
        }
    }

    @Test
    public void shouldCallGetTimeUsingColumnNameAndCalendar() throws SQLException {
    	Calendar localTime = new GregorianCalendar();
    	localTime.setTimeZone(TimeZone.getTimeZone(TestUtil.EXPECTED_TIMEZONE));
    	
    	String EXPECTED_TIME_FOR_TARGET = "08:39:10";


    	int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            java.sql.Time resultTime = resultSet.getTime(TestUtil.COLUMN_NAMES[col], localTime);
            assertThat(resultTime.toString(), is(EXPECTED_TIME_FOR_TARGET));
  
        }
    }

    @Test
    public void shouldCallGetTimeUsingColmnIndex() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            
            java.sql.Time r = resultSet.getTime(col + 1);

            // the result date should match the date coming from the souce, no change should occur 
            // somewhere based on calendar or timezone           

           assertThat(r.toString(), is(TestUtil.USE_TIME_FOR_SOURCE));
 
        }
    }

    @Test
    public void shouldCallGetTimeUsingColmnIndexAndCalendar() throws SQLException {
    	Calendar localTime = new GregorianCalendar();
    	localTime.setTimeZone(TimeZone.getTimeZone(TestUtil.EXPECTED_TIMEZONE));
    	
    	String EXPECTED_TIME_FOR_TARGET = "08:39:10";


        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));

            java.sql.Time resultTime = resultSet.getTime(col + 1, localTime);
            
            assertThat(resultTime.toString(), is(EXPECTED_TIME_FOR_TARGET));
        }
    }

    @Test
    public void shouldCallGetTimeStampUsingColumnName() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            
            java.sql.Timestamp r = resultSet.getTimestamp(col + 1);

            // the result date should match the date coming from the souce, no change should occur 
            // somewhere based on calendar or timezone           

           assertThat(r.toString(), is(TestUtil.USE_TIMESTAMP_FOR_SOURCE));

        }
    }

    @Test
    public void shouldCallGetTimeStampUsingColumnNameAndCalendar() throws SQLException {
    	Calendar localTime = new GregorianCalendar();
    	localTime.clear();
    	localTime.setTimeZone(TimeZone.getTimeZone(TestUtil.EXPECTED_TIMEZONE));

    	int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            java.sql.Timestamp resultTimestamp = resultSet.getTimestamp(TestUtil.COLUMN_NAMES[col], localTime);
            
            assertThat(resultTimestamp.toString(), is( TestUtil.EXPECTED_TIMESTAMP_FOR_TARGET));
        }
    }

    @Test
    public void shouldCallGetTimeStampUsingColmnIndex() throws SQLException {
        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            
            java.sql.Timestamp r = resultSet.getTimestamp(col + 1);

            // the result date should match the date coming from the souce, no change should occur 
            // somewhere based on calendar or timezone           

           assertThat(r.toString(), is(TestUtil.USE_TIMESTAMP_FOR_SOURCE));

        }
    }

    @Test
    public void shouldCallGetTimeStampUsingColmnIndexAndCalendar() throws SQLException {
    	Calendar localTime = new GregorianCalendar();
    	localTime.clear();
    	localTime.setTimeZone(TimeZone.getTimeZone(TestUtil.EXPECTED_TIMEZONE));

        int col = getColumnTypeLoc(TestUtil.DATE);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));

            java.sql.Timestamp resultTime = resultSet.getTimestamp(col + 1, localTime);
            
            assertThat(resultTime.toString(), is(TestUtil.EXPECTED_TIMESTAMP_FOR_TARGET));
        }
    }

    @Test
    public void shouldCallGetBytesUsingColmnIndex() throws SQLException {
        int numCols = TestUtil.COLUMN_NAMES.length;
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            // need to start at 1 because ResultSet is 1 based.
            for (int x = 1; x <= numCols; x++) {
                assertThat(resultSet.getBytes(x), is((tuple[x - 1] != null ? (tuple[x - 1].toString()).getBytes() : null)));
            }
        }
    }

    @Test
    public void shouldCallGetBytesUsingColumnName() throws SQLException {
        int numCols = TestUtil.COLUMN_NAMES.length;
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);

            for (int x = 0; x < numCols; x++) {
                assertThat(resultSet.getBytes(TestUtil.COLUMN_NAMES[x]),
                           is((tuple[x] != null ? (tuple[x].toString()).getBytes() : null)));
            }
        }
    }

    @Test
    public void shouldCallGetBinaryUsingColmnIndex() throws SQLException, IOException {
        int col = getColumnTypeLoc(TestUtil.BINARY);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            // need to increment because ResultSet is 1 based.
            assertThat(IoUtil.readBytes(resultSet.getBinaryStream(col + 1)), is(tuple[col]));
        }
    }

    @Test
    public void shouldCallGetBinaryUsingColumnName() throws SQLException, IOException {
        int col = getColumnTypeLoc(TestUtil.BINARY);
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);
            assertThat(IoUtil.readBytes(resultSet.getBinaryStream(TestUtil.COLUMN_NAMES[col])), is(tuple[col]));
        }
    }

    @Test
    public void shouldCallGetObjectUsingColumnName() throws SQLException {
        int numCols = TestUtil.COLUMN_NAMES.length;
        for (int i = 0; i < TestUtil.TUPLES.size(); i++) {
            assertThat(resultSet.next(), is(true));
            Object[] tuple = TestUtil.TUPLES.get(i);

            for (int x = 0; x < numCols; x++) {
                Object o = resultSet.getObject(TestUtil.COLUMN_NAMES[x]);
                // doing .toString() to compare the Object value to the TestQueryResultMetaData
                // which has primitives
                assertThat((o != null ? o.toString() : null), is((tuple[x] != null ? tuple[x].toString() : null)));
            }
        }
    }

    private int getColumnTypeLoc( String type ) {
        for (int i = 0; i <= TestUtil.TYPE_NAMES.length; i++) {
            if (TestUtil.TYPE_NAMES[i].equals(type)) {
                return i;
            }
        }
        assertFalse("Did not find a type match: " + type, true);
        return -1;
    }

    @Test
    public void shouldReturnMetaData() throws SQLException {
        assertThat(resultSet.getMetaData(), is(notNullValue()));
    }

    @Test
    public void shouldReturnFetchDirection() throws SQLException {
        assertThat(resultSet.getFetchDirection(), is(ResultSet.FETCH_FORWARD));
    }

    @Test
    public void shouldReturnDefaultFetchSize() throws SQLException {
        assertThat(resultSet.getFetchSize(), is(0));
    }

    @Test
    public void shouldReturnHoldability() throws SQLException {
        assertThat(resultSet.getHoldability(), is(0));
    }

    @Test
    public void shouldBeAbleToClose() {
        resultSet.close();
    }

    @Test
    public void shouldReturnIsClosed() {
        assertThat(resultSet.isClosed(), is(false));
    }

    /**
     * Since no current row is set, it should return 0
     * 
     * @throws SQLException
     */
    @Test
    public void shouldReturnRow() throws SQLException {
        assertThat(resultSet.getRow(), is(0));
    }

    @Test
    public void shouldFindColumn() throws SQLException {
        assertThat(resultSet.findColumn(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A), is(1));
    }

    @Test
    public void shouldCallConcurrency() throws SQLException {
        assertThat(resultSet.getConcurrency(), is(0));
    }

    // *******************
    // There are 3 - wasNull - tests because each has its own path for setting current value.
    // *******************

    @Test
    public void shouldCallWasNull() throws SQLException {
        // wasNull should be null until a get method is called
        assertTrue(resultSet.wasNull());

        assertThat(resultSet.next(), is(true));
        assertTrue(resultSet.wasNull());

        assertThat(resultSet.getString(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A), is(notNullValue()));
        assertFalse(resultSet.wasNull());

    }

    @Test
    public void shouldCallWasNullCallingGetObject() throws SQLException {
        // wasNull should be null until a get method is called
        assertTrue(resultSet.wasNull());

        assertThat(resultSet.next(), is(true));
        assertTrue(resultSet.wasNull());

        assertThat(resultSet.getObject(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A), is(notNullValue()));
        assertFalse(resultSet.wasNull());

    }

    @Test
    public void shouldCallWasNullCallingGetBytes() throws SQLException {
        // wasNull should be null until a get method is called
        assertTrue(resultSet.wasNull());

        assertThat(resultSet.next(), is(true));
        assertTrue(resultSet.wasNull());

        assertThat(resultSet.getBytes(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A), is(notNullValue()));
        assertFalse(resultSet.wasNull());

    }

    // *******************
    // The following tests should throw an exception because fetch direction is {@link ResultSet#FETCH_FORWARD},
    // therefore, cursor movement related methods are not supported.
    // *******************

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForFetchDirectionNotForward() throws SQLException {
        resultSet.setFetchDirection(ResultSet.FETCH_REVERSE);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForBeforeFirst() throws SQLException {
        resultSet.beforeFirst();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForFirst() throws SQLException {
        assertThat(resultSet.first(), is(false));
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForLast() throws SQLException {
        assertThat(resultSet.last(), is(false));
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForAfterLast() throws SQLException {
        resultSet.afterLast();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForMoveToCurrentRow() throws SQLException {
        resultSet.moveToCurrentRow();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForAbsolute() throws SQLException {
        resultSet.absolute(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForRelative() throws SQLException {
        resultSet.relative(1);
    }

    // *******************
    // The following methods are not supported because updates are not supported
    //
    // NOTE: Not all the UPDATE related methods are tested here because
    // they all call the same 2 methods:
    // 1. notClosed();
    // 2. noUpdates();
    // Therefore, only 2 tests; insertRow() and deleteRow() are tested to verify
    // the accuracy of those 2 internal methods.
    // *******************

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForInsertRow() throws SQLException {
        resultSet.insertRow();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForDeleteRow() throws SQLException {
        resultSet.deleteRow();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForMoveToInsertRow() throws SQLException {
        resultSet.moveToInsertRow();
    }

    // *******************
    // The following tests initiate invalid test in order
    // to validate expected exceptions
    // *******************

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForInvalidColumnIndexPlus1() throws SQLException {
        assertThat(resultSet.next(), is(true));
        resultSet.getString(TestUtil.COLUMN_NAMES.length + 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForInvalidColumnIndexMinus1() throws SQLException {
        assertThat(resultSet.next(), is(true));
        resultSet.getString(-1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionResultSetIsForwardOnly() throws SQLException {
        resultSet.first();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionInvalidColumn() throws SQLException {
        assertThat(resultSet.next(), is(true));
        resultSet.getString("InvalidColumnName");
    }

    /**
     * Not all the update related methods are tested because they all perform the same check.
     * 
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowUpdatesNotSupported() throws SQLException {
        assertThat(resultSet.next(), is(true));
        resultSet.insertRow();
    }

    // *******************
    // These are negative tests and verify exception is thrown
    // when the resultset is closed
    // *******************

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionResultSetIsClosed() throws SQLException {
        resultSet.close();
        resultSet.next();
    }

    @Test( expected = SQLException.class )
    public void shouldThrowExceptionIsClosedWhenGettingValue() throws SQLException {
        resultSet.close();
        resultSet.getString(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A);
    }

    @Test( expected = SQLException.class )
    public void shouldThrowExceptionIsClosedWhenGettingObject() throws SQLException {
        resultSet.close();
        resultSet.getObject(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A);
    }

    @Test( expected = SQLException.class )
    public void shouldThrowExceptionIsClosedWhenGettingBytes() throws SQLException {
        resultSet.close();
        resultSet.getBytes(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A);
    }

    // *******************
    // There are negative tests the verify an exception will be thrown
    // when the row has not been set;
    // *******************

    @Test( expected = SQLException.class )
    public void shouldThrowExceptionIsRowSetWhenGettingValue() throws SQLException {
        resultSet.getString(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A);
    }

    @Test( expected = SQLException.class )
    public void shouldThrowExceptionIsRowSetWhenGettingObject() throws SQLException {
        resultSet.getObject(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A);
    }

    @Test( expected = SQLException.class )
    public void shouldThrowExceptionIsRowSetWhenGettingBytes() throws SQLException {
        resultSet.getBytes(TestUtil.COLUMN_NAME_PROPERTIES.PROP_A);
    }

    // *******************
    // The following are unsupported features
    // *******************

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetRowIDInt() throws SQLException {
        resultSet.getRowId(0);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetRowIDString() throws SQLException {
        resultSet.getRowId("columnname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetCursorName() throws SQLException {
        resultSet.getCursorName();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetArrayIdx() throws SQLException {
        resultSet.getArray(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetArrayColName() throws SQLException {
        resultSet.getArray("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetAsciiStreamIdx() throws SQLException {
        resultSet.getAsciiStream(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetAsciiStreamColName() throws SQLException {
        resultSet.getAsciiStream("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetBigDecimalIdx() throws SQLException {
        resultSet.getBigDecimal(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetBigDecimalColName() throws SQLException {
        resultSet.getBigDecimal("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetBigDecimalIdxScale() throws SQLException {
        resultSet.getBigDecimal(1, 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetBigDecimalColNameScale() throws SQLException {
        resultSet.getBigDecimal("colname", 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetBlobIdx() throws SQLException {
        resultSet.getBlob(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetBlobColName() throws SQLException {
        resultSet.getBlob("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetByteIdx() throws SQLException {
        resultSet.getByte(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetByteColName() throws SQLException {
        resultSet.getByte("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetCharacterStreamIdx() throws SQLException {
        resultSet.getCharacterStream(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetCharacterStreamColName() throws SQLException {
        resultSet.getCharacterStream("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetClobIdx() throws SQLException {
        resultSet.getClob(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetClobColName() throws SQLException {
        resultSet.getClob("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetFloatIdx() throws SQLException {
        resultSet.getFloat(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetFloatColNameCal() throws SQLException {
        resultSet.getFloat("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetNCharacterStreamIdx() throws SQLException {
        resultSet.getNCharacterStream(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetNCharacterStreamColName() throws SQLException {
        resultSet.getNCharacterStream("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetNClobIdx() throws SQLException {
        resultSet.getNClob(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetNClobColName() throws SQLException {
        resultSet.getNClob("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetNStringIdx() throws SQLException {
        resultSet.getNString(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetNStringColName() throws SQLException {
        resultSet.getNString("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetObjectIdxMap() throws SQLException {
        resultSet.getObject(1, null);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetObjectColNameMap() throws SQLException {
        resultSet.getObject("colname", null);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetRefIdx() throws SQLException {
        resultSet.getRef(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetRefColName() throws SQLException {
        resultSet.getRef("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetSQLXMLIdx() throws SQLException {
        resultSet.getSQLXML(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetSQLXMLColName() throws SQLException {
        resultSet.getSQLXML("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetShortIdx() throws SQLException {
        resultSet.getShort(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetShortColName() throws SQLException {
        resultSet.getShort("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetURLIdx() throws SQLException {
        resultSet.getURL(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetURLColName() throws SQLException {
        resultSet.getURL("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetUnicodeStreamIdx() throws SQLException {
        resultSet.getUnicodeStream(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetUnicodeStreamColName() throws SQLException {
        resultSet.getUnicodeStream("colname");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingRowDeleted() throws SQLException {
        resultSet.rowDeleted();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingRowInserted() throws SQLException {
        resultSet.rowInserted();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingRowUpdated() throws SQLException {
        resultSet.rowUpdated();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingSetFetchDirectionForward() throws SQLException {
        resultSet.setFetchDirection(ResultSet.FETCH_FORWARD);
    }

}
