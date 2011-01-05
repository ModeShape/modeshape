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
import static org.mockito.Mockito.when;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.jdbc.delegate.ConnectionInfo;
import org.modeshape.jdbc.delegate.RepositoryDelegate;

/**
 * 
 */
public class JcrStatementTest {

    private JcrStatement stmt;

    @Mock
    private JcrConnection connection;
    @Mock
    private QueryResult queryResult;

    @After
    public void afterEach() {
        if (stmt != null) {
            stmt.close();
            stmt = null;
        }
    }

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        stmt = new JcrStatement(connection);

        when(connection.getRepositoryDelegate()).thenReturn(new TestJcrCommRepositoryInterface());

        when(queryResult.getColumnNames()).thenReturn(TestUtil.COLUMN_NAMES);
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
    public void shouldExcute() throws SQLException {
        stmt.execute(TestUtil.SQL_SELECT);

    }

    public void shouldExcuteQuery() throws SQLException {
        stmt.executeQuery(TestUtil.SQL_SELECT);
    }

    /**
     * Because updates are not supported, this test should throw an exception.
     * 
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForAddBatch() throws SQLException {
        stmt.addBatch("Update sql");

    }

    /**
     * Because updates are not supported, this test should throw an exception.
     * 
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForExcuteBatch() throws SQLException {
        stmt.executeBatch();
    }

    /**
     * Because updates are not supported, this test should throw an exception.
     * 
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForUpdate() throws SQLException {
        stmt.executeUpdate("Update sql");
    }

    /**
     * Because updates are not supported, this test should throw an exception.
     * 
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionForClearBatch() throws SQLException {
        stmt.clearBatch();
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
    public void shouldReturnDefaultForGeneratedKeys() {
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

    /**
     * Because updates are not supported, this test should throw an exception.
     * 
     * @throws SQLException
     */
    @Test
    public void shouldSupportCancel() throws SQLException {
        stmt.cancel();
    }

    @Test
    public void shouldSupportEquals() {
        assertTrue(stmt.equals(stmt));

        JcrStatement stmt2 = null;
        try {
            stmt2 = new JcrStatement(connection);

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

    @Test
    public void shouldSetFetchSize() throws SQLException {
        stmt.setFetchSize(100);
    }

    /**
     * Because updates are not supported, this test should throw an exception.
     * 
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldSetCursorName() throws SQLException {
        stmt.setCursorName("CursorName");
    }

    @Test
    public void shouldSetEscapeProcessingTrue() throws SQLException {
        stmt.setEscapeProcessing(true);
    }

    @Test
    public void shouldSetEscapeProcessingFalse() throws SQLException {
        stmt.setEscapeProcessing(false);
    }

    @Test
    public void shouldSetFetchDirectionReverse() throws SQLException {
        stmt.setFetchDirection(ResultSet.FETCH_REVERSE);
    }

    @Test
    public void shouldSetFetchDirectionUnknown() throws SQLException {
        stmt.setFetchDirection(ResultSet.FETCH_UNKNOWN);
    }

    @Test
    public void shouldSetFetchDirectionForward() throws SQLException {
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
    }

    @Test
    public void shouldSetMaxFieldSize() throws SQLException {
        stmt.setMaxFieldSize(30);
    }

    @Test
    public void shouldSetMaxRows() throws SQLException {
        stmt.setMaxRows(200);
    }

    @Test
    public void shouldSetPoolableTrue() throws SQLException {
        stmt.setPoolable(true);
    }

    @Test
    public void shouldSetPoolableFalse() throws SQLException {
        stmt.setPoolable(false);
    }

    @Test
    public void shouldSetQueryTimeout() throws SQLException {
        stmt.setQueryTimeout(60);
    }

    public class TestJcrCommRepositoryInterface implements RepositoryDelegate {

        @Override
        public Connection createConnection() {
            return null;
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public QueryResult execute( String query,
                                    String language ) {
            return queryResult;
        }

        @Override
        public ConnectionInfo getConnectionInfo() {
            return null;
        }

        @Override
        public void close() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jdbc.delegate.RepositoryDelegate#closeStatement()
         */
        @Override
        public void closeStatement() {
        }

        @Override
        public void commit() {
        }

        @Override
        public boolean isValid( int timeout ) {
            return false;
        }

        @Override
        public NodeType nodeType( String name ) {
            return null;
        }

        @Override
        public List<NodeType> nodeTypes() {
            return null;
        }

        @Override
        public void rollback() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jdbc.delegate.RepositoryDelegate#isWrapperFor(java.lang.Class)
         */
        @Override
        public boolean isWrapperFor( Class<?> iface ) {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jdbc.delegate.RepositoryDelegate#unwrap(java.lang.Class)
         */
        @Override
        public <T> T unwrap( Class<T> iface ) {
            return null;
        }

        @Override
        public Set<String> getRepositoryNames() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jdbc.delegate.RepositoryDelegate#getDescriptor(java.lang.String)
         */
        @Override
        public String getDescriptor( String descriptorKey ) {
            return null;
        }

    }

}
