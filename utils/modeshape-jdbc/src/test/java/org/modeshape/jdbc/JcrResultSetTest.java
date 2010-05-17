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

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.query.QueryResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author vanhalbert
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
        
        result = TestQueryResultMetaData.createQueryResult();
 	resultSet = new JcrResultSet(statement, result);        
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
    public void shouldCallNext() throws SQLException  {
	// iterate the full resultset
	for (int i=1; i<= TestQueryResultMetaData.TUPLES.size(); i++) {
	       assertThat(resultSet.next(), is(true));
	}
	// after all rows have been retrieved, the resultset
	// should return false
	assertThat(resultSet.next(), is(false));
    }
    
    @Test
    public void shouldCallGetRow() throws SQLException {
	//first call, before current row being set,
	// should return zero to indicate positioning pre-firsts row
	assertThat(resultSet.getRow(), is(0));
	// set i=1 because row position is 1 based
	for (int i=1; i<= TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    assertThat(resultSet.getRow(), is(i));

	}
    }
    
    @Test
    public void shouldCallIsBeforeFirst() throws SQLException {
        assertThat(resultSet.isBeforeFirst(), is(true));
	// as the resultset is being processed 
	// verify isBeforeFirst is only valid before processing the resultset
	for (int i=1; i<= TestQueryResultMetaData.TUPLES.size(); i++) {
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
	for (int i=1; i<= TestQueryResultMetaData.TUPLES.size(); i++) {
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
	for (int i=1; i<= TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    if (i == TestQueryResultMetaData.TUPLES.size()) {
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
	for (int i=1; i<= TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    assertThat(resultSet.isAfterLast(), is(false));
	}
	assertThat(resultSet.next(), is(false));
	assertThat(resultSet.isAfterLast(), is(true));
    }
 
    
    @Test
    public void shouldCallGetStringUsingColmnName() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.STRING);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    assertThat(resultSet.getString(TestQueryResultMetaData.COLUMN_NAMES[col]), is(tuple[col]));

	}
    }
    
    @Test
    public void shouldCallGetStringUsingColmnIndex() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.STRING);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    // need to increment because ResultSet is 1 based.
	    assertThat(resultSet.getString(col +1), is(tuple[col]));

	}
    }
    
    @Test
    public void shouldCallGetLongUsingColumnName() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.LONG);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    assertThat(resultSet.getLong(TestQueryResultMetaData.COLUMN_NAMES[col]), is(tuple[col]));

	}
    }
    
    @Test
    public void shouldCallGetLongUsingColmnIndex() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.LONG);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    // need to increment because ResultSet is 1 based.
	    assertThat(resultSet.getLong(col +1), is(tuple[col]));

	}
    }
    
    @Test
    public void shouldCallGetDoubleUsingColumnName() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.DOUBLE);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    assertThat(resultSet.getDouble(TestQueryResultMetaData.COLUMN_NAMES[col]), is(tuple[col]));

	}
    }
    
    @Test
    public void shouldCallGetDoubleUsingColmnIndex() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.DOUBLE);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    // need to increment because ResultSet is 1 based.
	    assertThat(resultSet.getDouble(col +1), is(tuple[col]));

	}
    }
    
    @Test
    public void shouldCallGetBooleanUsingColumnName() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.BOOLEAN);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    assertThat(resultSet.getBoolean(TestQueryResultMetaData.COLUMN_NAMES[col]), is(tuple[col]));

	}
    }
    
    @Test
    public void shouldCallGetBooleanUsingColmnIndex() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.BOOLEAN);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    // need to increment because ResultSet is 1 based.
	    assertThat(resultSet.getBoolean(col +1), is(tuple[col]));

	}
    }
    
//    @Test
//    public void shouldCallGetDateUsingColumnName() throws SQLException {	
//	int col = getColumnTypeLoc(TestQueryResultMetaData.DATE);
//	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
//	    assertThat(resultSet.next(), is(true));
//	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
//	    assertThat(resultSet.getDate(TestQueryResultMetaData.COLUMN_NAMES[col]), is(tuple[col]));
//
//	}
//    }
//    
//    @Test
//    public void shouldCallGetDateUsingColmnIndex() throws SQLException {	
//	int col = getColumnTypeLoc(TestQueryResultMetaData.DATE);
//	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
//	    assertThat(resultSet.next(), is(true));
//	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
//	    // need to increment because ResultSet is 1 based.
//	    assertThat(resultSet.getDate(col +1), is(tuple[col]));
//
//	}
//    }
    
    @Test
    public void shouldCallGetBytesUsingColmnIndex() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.STRING);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    // need to increment because ResultSet is 1 based.
	    assertThat(resultSet.getBytes(col +1), 
		    is(  (tuple[col].toString()).getBytes()  ));
	}
    }
    
    @Test
    public void shouldCallGetBytesUsingColumnName() throws SQLException {	
	int col = getColumnTypeLoc(TestQueryResultMetaData.STRING);
	for (int i=0; i< TestQueryResultMetaData.TUPLES.size(); i++) {
	    assertThat(resultSet.next(), is(true));
	    Object[] tuple = TestQueryResultMetaData.TUPLES.get(i);  
	    assertThat(resultSet.getBytes(TestQueryResultMetaData.COLUMN_NAMES[col]), 
		    is(  (tuple[col].toString()).getBytes()  ));
	}
    }
    
    private int getColumnTypeLoc(String type) {
	for (int i=0; i<= TestQueryResultMetaData.TYPE_NAMES.length; i++) {
	    if (TestQueryResultMetaData.TYPE_NAMES[i].equals(type)) {
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
    public void shouldSetFetchDirectionForward() throws SQLException {
	resultSet.setFetchDirection(ResultSet.FETCH_FORWARD);
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
     * @throws SQLException
     */
    @Test
    public void shouldReturnRow() throws SQLException {
        assertThat(resultSet.getRow(), is(0));
    }
      
    @Test
    public void shouldFindColumn() throws SQLException {
        assertThat(resultSet.findColumn(TestQueryResultMetaData.COLUMN_NAME_PROPERTIES.PROP_A), is(1));
    }
    
    @Test
    public void shouldReturnFalseForRowDeleted() throws SQLException {
        assertThat(resultSet.rowDeleted(), is(false));
    }
    
    @Test
    public void shouldReturnFalseForRowInserted() throws SQLException {
        assertThat(resultSet.rowInserted(), is(false));
    }
    
    @Test
    public void shouldReturnFalseForRowUpdated() throws SQLException {
        assertThat(resultSet.rowUpdated(), is(false));
    }
    
    //*******************
    // Because fetch direction is {@link ResultSet#FETCH_FORWARD}, 
    // cursor movement related methods are not supported, therefore the 
    // following tests should throw an exception.	
    //*******************
    
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForFetchDirectionNotForward() throws SQLException {
	resultSet.setFetchDirection(ResultSet.FETCH_REVERSE);
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForBeforeFirst() throws SQLException {
        resultSet.beforeFirst();
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForFirst() throws SQLException {
	assertThat(resultSet.first(), is(false));
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForLast() throws SQLException {
	assertThat(resultSet.last(), is(false));
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForAfterLast() throws SQLException {
        resultSet.afterLast();
    }
    
   
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForMoveToCurrentRow() throws SQLException {
	resultSet.moveToCurrentRow();
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForAbsolute() throws SQLException  {
	resultSet.absolute(1);
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForRelative() throws SQLException {
	resultSet.relative(1);
    }
  
    //*******************
    //  The following methods are not supported because updates are not supported
    //
    //	NOTE:  Not all the UPDATE related methods are tested here because
    //		they all call the same 2 methods:
    //			1.	notClosed();
    //			2.	noUpdates();
    //	Therefore, only 2 tests; insertRow() and deleteRow() are tested to verify 
    //	the accuracy of	those 2 internal methods. 
    //*******************
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForInsertRow() throws SQLException  {
	resultSet.insertRow();
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForDeleteRow() throws SQLException  {
	resultSet.deleteRow();
    }
    
    /**
     * @throws SQLException 
      */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForMoveToInsertRow() throws SQLException  {
	resultSet.moveToInsertRow();
    }

    
    /**
     * @throws SQLException 
     */
    @Test
    public void shouldThrowExceptionForUpdate() throws SQLException   {
	resultSet.wasNull();

    }
    
    //*******************
    //  The following methods are not supported
    //*******************   
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForReturnRowIDInt() throws SQLException {
	resultSet.getRowId(0);
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForReturnRowIDString() throws SQLException {
	resultSet.getRowId("columnname");
    }
    
    
    //*******************
    //  The following tests are the negative tests to check for expected
    //  exceptions
    //*******************
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForInvalidColumnIndexPlus1() throws SQLException {
	assertThat(resultSet.next(), is(true));
	resultSet.getString(TestQueryResultMetaData.COLUMN_NAMES.length + 1);
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForInvalidColumnIndexMinus1() throws SQLException {
	assertThat(resultSet.next(), is(true));
	resultSet.getString(-1);
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionForCurrentRowNotSet() throws SQLException {
	resultSet.getString(1);
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionResultSetIsClosed() throws SQLException {
	resultSet.close();
	resultSet.next();
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionResultSetIsForwardOnly() throws SQLException {
	resultSet.first();
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowExceptionInvalidColumn() throws SQLException {
	assertThat(resultSet.next(), is(true));
	resultSet.getString("InvalidColumnName");
    }
    
    /**
     * @throws SQLException 
     */
    @Test(expected= SQLException.class)
    public void shouldThrowUpdatesNotSupported() throws SQLException {
	assertThat(resultSet.next(), is(true));
	resultSet.insertRow();
    }

}
