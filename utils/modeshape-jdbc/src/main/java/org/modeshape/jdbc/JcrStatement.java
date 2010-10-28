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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import org.modeshape.jdbc.delegate.RepositoryDelegate;

/**
 * 
 */
class JcrStatement implements Statement {

    private final JcrConnection connection;
    private ResultSet results;
    private boolean closed;
    private SQLWarning warning;
    private int rowLimit = -1;
    private int fetchDirection = ResultSet.FETCH_FORWARD;
    private boolean poolable;
    private int moreResults = 0;

    private String sqlLanguage = JcrConnection.JCR_SQL2;

    JcrStatement( JcrConnection connection ) {
        this.connection = connection;
        assert this.connection != null;
    }

    JcrConnection connection() {
        return this.connection;
    }

    public void setJcrSqlLanguage( String jcrSQL ) {
        this.sqlLanguage = (jcrSQL != null ? jcrSQL : JcrConnection.JCR_SQL2);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver doesn't have a way to set the fetch size, so this method always returns 0.
     * </p>
     * 
     * @see java.sql.Statement#getFetchSize()
     */
    @Override
    public int getFetchSize() throws SQLException {
        notClosed();
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver doesn't have a way to set the fetch size, so this method is ignored and does nothing.
     * </p>
     * 
     * @see java.sql.Statement#setFetchSize(int)
     */
    @Override
    public void setFetchSize( int rows ) throws SQLException {
        notClosed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getFetchDirection()
     */
    @Override
    public int getFetchDirection() throws SQLException {
        notClosed();
        return fetchDirection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#setFetchDirection(int)
     */
    @Override
    public void setFetchDirection( int direction ) throws SQLException {
        notClosed();
        if (direction != ResultSet.FETCH_FORWARD && direction != ResultSet.FETCH_REVERSE && direction != ResultSet.FETCH_UNKNOWN) {
            throw new SQLException(JdbcI18n.invalidArgument.text(direction, "" + ResultSet.FETCH_FORWARD + ", "
                                                                            + ResultSet.FETCH_REVERSE + ", "
                                                                            + ResultSet.FETCH_UNKNOWN));
        }
        fetchDirection = direction;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver does not support limiting the field size, and always returns 0.
     * </p>
     * 
     * @see java.sql.Statement#getMaxFieldSize()
     */
    @Override
    public int getMaxFieldSize() throws SQLException {
        notClosed();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#setMaxFieldSize(int)
     */
    @Override
    public void setMaxFieldSize( int max ) throws SQLException {
        notClosed();
        if (max < 0) {
            throw new SQLException(JdbcI18n.argumentMayNotBeNegative.text("max", max));
        }
        // ignored otherwise
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getMaxRows()
     */
    @Override
    public int getMaxRows() throws SQLException {
        notClosed();
        // need to map ModeShapes -1 rowLimit to 0
        // because the jdbc spec indicate maxRows must be >= 0
        // or an exception should be thrown.
        return (rowLimit == -1 ? 0 : rowLimit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#setMaxRows(int)
     */
    @Override
    public void setMaxRows( int max ) throws SQLException {
        notClosed();
        if (max < 0) {
            throw new SQLException(JdbcI18n.argumentMayNotBeNegative.text("max", max));
        }
        rowLimit = max;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns 0 since there is currently no timeout with JCR 1.0 or JCR 2.0.
     * </p>
     * 
     * @see java.sql.Statement#getQueryTimeout()
     */
    @Override
    public int getQueryTimeout() throws SQLException {
        notClosed();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#setQueryTimeout(int)
     */
    @Override
    public void setQueryTimeout( int seconds ) throws SQLException {
        notClosed();
        if (seconds < 0) {
            throw new SQLException(JdbcI18n.argumentMayNotBeNegative.text("seconds", seconds));
        }
        // Otherwise ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#isPoolable()
     */
    @Override
    public boolean isPoolable() throws SQLException {
        notClosed();
        return poolable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#setPoolable(boolean)
     */
    @Override
    public void setPoolable( boolean poolable ) throws SQLException {
        notClosed();
        this.poolable = poolable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException {
        notClosed();
        return connection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#cancel()
     */
    @Override
    public void cancel() throws SQLException {
        notClosed();
        close();
        // Unable to cancel a JCR query ...
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        notClosed();
        warning = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        notClosed();
        return warning;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#isClosed()
     */
    @Override
    public boolean isClosed() {
        return closed || connection.isClosed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#close()
     */
    @Override
    public void close() {
        if (!closed) {
        	closed = true;
        	connection.getRepositoryDelegate().closeStatement();            
        }
    }

    protected final void notClosed() throws SQLException {
        if (isClosed()) throw new SQLException(JdbcI18n.statementIsClosed.text());
    }

    protected final void noUpdates() throws SQLException {
        throw new SQLException(JdbcI18n.updatesNotSupported.text());
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Updates
    // ----------------------------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#executeUpdate(java.lang.String)
     */
    @Override
    public int executeUpdate( String sql ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#executeUpdate(java.lang.String, int)
     */
    @Override
    public int executeUpdate( String sql,
                              int autoGeneratedKeys ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
     */
    @Override
    public int executeUpdate( String sql,
                              int[] columnIndexes ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
     */
    @Override
    public int executeUpdate( String sql,
                              String[] columnNames ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#setCursorName(java.lang.String)
     */
    @Override
    public void setCursorName( String name ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getUpdateCount()
     */
    @Override
    public int getUpdateCount() throws SQLException {
        notClosed();
        return -1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#addBatch(java.lang.String)
     */
    @Override
    public void addBatch( String sql ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#clearBatch()
     */
    @Override
    public void clearBatch() throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#executeBatch()
     */
    @Override
    public int[] executeBatch() throws SQLException {
        notClosed();
        noUpdates();
        return null;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#execute(java.lang.String)
     */
    @Override
    public boolean execute( String sql ) throws SQLException {
        notClosed();
        warning = null;
        moreResults = 0;
        try {
            // Convert the supplied SQL into JCR-SQL2 ...
            String jcrSql2 = connection.nativeSQL(sql);
            // Create the query ...
            final QueryResult jcrResults = getJcrRepositoryDelegate().execute(jcrSql2, this.sqlLanguage);
            results = new JcrResultSet(this, jcrResults, null);
            moreResults = 1;
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
        return true; // always a ResultSet
    }

    protected RepositoryDelegate getJcrRepositoryDelegate() {
        return this.connection.getRepositoryDelegate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#execute(java.lang.String, int)
     */
    @Override
    public boolean execute( String sql,
                            int autoGeneratedKeys ) throws SQLException {
    	throw new SQLFeatureNotSupportedException();    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#execute(java.lang.String, int[])
     */
    @Override
    public boolean execute( String sql,
                            int[] columnIndexes ) throws SQLException {
    	throw new SQLFeatureNotSupportedException();    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
     */
    @Override
    public boolean execute( String sql,
                            String[] columnNames ) throws SQLException {
    	throw new SQLFeatureNotSupportedException();    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#executeQuery(java.lang.String)
     */
    @Override
    public ResultSet executeQuery( String sql ) throws SQLException {
        execute(sql);
        return getResultSet();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getGeneratedKeys()
     */
    @Override
    public ResultSet getGeneratedKeys() /*throws SQLException*/{
        // TODO: if and when ModeShape supports providing key information
        // then a result set containing the metadata will need to be created.
        return new JcrResultSet();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getMoreResults()
     */
    @Override
    public boolean getMoreResults() throws SQLException {
        notClosed();
        return moreResults > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getMoreResults(int)
     */
    @Override
    public boolean getMoreResults( int current ) throws SQLException {
        notClosed();
        if (current != CLOSE_ALL_RESULTS && current != CLOSE_CURRENT_RESULT && current != KEEP_CURRENT_RESULT) {
            throw new SQLException(JdbcI18n.invalidArgument.text(current, "" + CLOSE_ALL_RESULTS + ", " + CLOSE_CURRENT_RESULT
                                                                          + ", " + KEEP_CURRENT_RESULT));
        }
        if (KEEP_CURRENT_RESULT != current) {
            // Close (by nulling) the results ...
//            jcrResults = null;
            results = null;
        }
        if (moreResults > 0) --moreResults;
        return moreResults > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getResultSet()
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        notClosed();
        return results; // may be null
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getResultSetConcurrency()
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        notClosed();
        return ResultSet.CONCUR_READ_ONLY;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getResultSetHoldability()
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        notClosed();
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#getResultSetType()
     */
    @Override
    public int getResultSetType() throws SQLException {
        notClosed();
        return ResultSet.TYPE_SCROLL_INSENSITIVE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Statement#setEscapeProcessing(boolean)
     */
    @Override
    public void setEscapeProcessing( boolean enable ) throws SQLException {
        notClosed();
        // Ignore for now
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor( Class<?> iface ) /*throws SQLException*/{
        return iface.isInstance(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (!isWrapperFor(iface)) {
            throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text(Statement.class.getSimpleName(), iface.getName()));
        }

        return iface.cast(this);
    }

}
