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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 
 */
public class JcrStatementTest {
   
    private JcrStatement stmt;
    
    @Mock
    private JcrConnection connection;
    @Mock
    private Session session;
    @Mock
    private QueryManager queryMgr;
    @Mock
    private QueryResult queryResult;
    @Mock
    private Query jcrQuery;
    @Mock
    private Workspace workspace;


    
    @After
    public void afterEach() {

	if (stmt != null) {
	    stmt.close();
	    stmt = null;
	}
    }

    @Before
    public void beforeEach() throws Exception  {
	MockitoAnnotations.initMocks(this);
	
        stmt =  new JcrStatement(connection, session);
        
        when(queryResult.getColumnNames()).thenReturn(TestResultSetMetaData.COLUMN_NAMES);

        
        when(session.getWorkspace()).thenReturn(workspace);
        
        when(workspace.getQueryManager()).thenReturn(queryMgr);
        
	when(queryMgr.createQuery(anyString(), anyString())).thenReturn(jcrQuery);

	when(jcrQuery.execute()).thenReturn(queryResult);
    }
    
    @Test
    public void shouldHaveStatement() {
        assertThat(stmt, is(notNullValue()));
    }
    
    @Test
    public void shouldBeAbleToClearWarnings() throws SQLException {
	stmt.clearWarnings();
    }
    
    @Test
    public void shouldHaveConnection() throws SQLException {
	assertThat(stmt.getConnection(), is(notNullValue()));
    }
    
    @Test
    public void shouldReturnDefaultForFetchDirection() throws SQLException {
	assertThat(stmt.getFetchDirection(), is(ResultSet.FETCH_FORWARD));
    }
    
    @Test
    public void shouldHaveFetchSize() throws SQLException {
	assertThat(stmt.getFetchSize(), is(0));
    }
    
    @Test
    public void shouldReturnDefaultForMaxRows() throws SQLException {
	assertThat(stmt.getMaxRows(), is(0));
    } 
    
    @Test
    public void shouldHaveMoreResults() throws SQLException {
	assertThat(stmt.getMoreResults(), is(false));
    }
    
    @Test
    public void shouldHaveMoreResultsAtPostion() throws SQLException {
	assertThat(stmt.getMoreResults(Statement.CLOSE_CURRENT_RESULT), is(false));
    }
    
    @Test
    public void shouldReturnDefaultForMaxFieldSize() throws SQLException {
	assertThat(stmt.getMaxFieldSize(), is(0));
    }
    
    @Test
    public void shouldReturnDefaultForQueryTimeout() throws SQLException {
	assertThat(stmt.getQueryTimeout(), is(0));
    }
    
    @Test
    public void shouldReturnDefaultForUpdateCount() throws SQLException {
	assertThat(stmt.getUpdateCount(), is(-1));
    }
      
    @Test
    /**
     * Because updates are not supported, this test should throw an
     * exception.
     */
    public void shouldExcute()  {
	try {
	    stmt.execute(TestResultSetMetaData.SQL_SELECT);
	    
	} catch (SQLException sqle) {
	    assertFalse(true);
	}
    }
    
    @Test
    /**
     * Because updates are not supported, this test should throw an
     * exception.
     */
    public void shouldExcuteQuery()  {
	try {
	    stmt.executeQuery(TestResultSetMetaData.SQL_SELECT);
	    
	} catch (SQLException sqle) {
	    assertFalse(true);
	}
    }
    
    @Test
    /**
     * Because updates are not supported, this test should throw an
     * exception.
     */
    public void shouldThrowExceptionForAddBatch()  {
	try {
	    stmt.addBatch("Update sql");
	    assertTrue(false);
	} catch (SQLException sqle) {
	    
	}
    }
    
    @Test
    /**
     * Because updates are not supported, this test should throw an
     * exception.
     */
    public void shouldThrowExceptionForExcuteBatch()  {
	try {
	    stmt.executeBatch();
	    assertTrue(false);
	} catch (SQLException sqle) {
	    
	}
    }
    
    
    @Test
    /**
     * Because updates are not supported, this test should throw an
     * exception.
     */
    public void shouldThrowExceptionForUpdate()  {
	try {
	    stmt.executeUpdate("Update sql");
	    assertTrue(false);
	} catch (SQLException sqle) {
	    
	}
    }
    
    @Test
    /**
     * Because updates are not supported, this test should throw an
     * exception.
     */
    public void shouldThrowExceptionForClearBatch()  {
	try {
	    stmt.clearBatch();
	    assertTrue(false);
	} catch (SQLException sqle) {
	    
	}
    }
    
    
    @Test
    public void shouldReturnResultSetConcurreny() throws SQLException {
	assertThat(stmt.getResultSetConcurrency(), is(ResultSet.CONCUR_READ_ONLY));
    }
    
    @Test
    public void shouldReturnResultSetHoldability() throws SQLException {
	assertThat(stmt.getResultSetHoldability(), is(ResultSet.CLOSE_CURSORS_AT_COMMIT));
    }
    
    @Test
    public void shouldReturnResultSetType() throws SQLException {
	assertThat(stmt.getResultSetType(), is(ResultSet.TYPE_SCROLL_INSENSITIVE));
    }    
    
    @Test
    public void shouldReturnDefaultForGeneratedKeys() throws SQLException {
	assertThat(stmt.getGeneratedKeys(), is(ResultSet.class));
    }
    
    @Test
    public void shouldReturnDefaultResultSet() throws SQLException {
	assertNull(stmt.getResultSet());
    }
    
    @Test
    public void shouldReturnDefaultForWarnings() throws SQLException {
	assertNull(stmt.getWarnings());
    }
     
    @Test
    public void shouldSupportCancel()  {
	try {
	    stmt.cancel();
	    
	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	    assertTrue(false);
	}
    }
    
    @Test
    public void shouldSupportEquals() {
	assertTrue(stmt.equals(stmt));
	
	JcrStatement stmt2 = null;
	try {
	    stmt2 =  new JcrStatement(connection, session);
	       	
	    assertFalse(stmt.equals(stmt2));

	} finally {
	    if (stmt2 != null) {
		stmt2.close();
	    }
	}
    }
       
    @Test
    public void shouldBeAbleToClose() {

	stmt.close();

    }
    
}
