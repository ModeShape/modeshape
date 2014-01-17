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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import org.modeshape.jdbc.delegate.RepositoryDelegate;

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

    @Override
    public int getFetchDirection() throws SQLException {
        notClosed();
        return fetchDirection;
    }

    @Override
    public void setFetchDirection( int direction ) throws SQLException {
        notClosed();
        if (direction != ResultSet.FETCH_FORWARD && direction != ResultSet.FETCH_REVERSE && direction != ResultSet.FETCH_UNKNOWN) {
            throw new SQLException(JdbcLocalI18n.invalidArgument.text(direction, "" + ResultSet.FETCH_FORWARD + ", "
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

    @Override
    public void setMaxFieldSize( int max ) throws SQLException {
        notClosed();
        if (max < 0) {
            throw new SQLException(JdbcLocalI18n.argumentMayNotBeNegative.text("max", max));
        }
        // ignored otherwise
    }

    @Override
    public int getMaxRows() throws SQLException {
        notClosed();
        // need to map ModeShapes -1 rowLimit to 0
        // because the jdbc spec indicate maxRows must be >= 0
        // or an exception should be thrown.
        return (rowLimit == -1 ? 0 : rowLimit);
    }

    @Override
    public void setMaxRows( int max ) throws SQLException {
        notClosed();
        if (max < 0) {
            throw new SQLException(JdbcLocalI18n.argumentMayNotBeNegative.text("max", max));
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

    @Override
    public void setQueryTimeout( int seconds ) throws SQLException {
        notClosed();
        if (seconds < 0) {
            throw new SQLException(JdbcLocalI18n.argumentMayNotBeNegative.text("seconds", seconds));
        }
        // Otherwise ignore
    }

    @Override
    public boolean isPoolable() throws SQLException {
        notClosed();
        return poolable;
    }

    @Override
    public void setPoolable( boolean poolable ) throws SQLException {
        notClosed();
        this.poolable = poolable;
    }

    @Override
    public Connection getConnection() throws SQLException {
        notClosed();
        return connection;
    }

    @Override
    public void cancel() throws SQLException {
        notClosed();
        close();
        // Unable to cancel a JCR query ...
    }

    @Override
    public void clearWarnings() throws SQLException {
        notClosed();
        warning = null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        notClosed();
        return warning;
    }

    @Override
    public boolean isClosed() {
        return closed || connection.isClosed();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            connection.getRepositoryDelegate().closeStatement();
        }
    }

    protected final void notClosed() throws SQLException {
        if (isClosed()) throw new SQLException(JdbcLocalI18n.statementIsClosed.text());
    }

    protected final void noUpdates() throws SQLException {
        throw new SQLException(JdbcLocalI18n.updatesNotSupported.text());
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Updates
    // ----------------------------------------------------------------------------------------------------------------

    @Override
    public int executeUpdate( String sql ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    @Override
    public int executeUpdate( String sql,
                              int autoGeneratedKeys ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    @Override
    public int executeUpdate( String sql,
                              int[] columnIndexes ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    @Override
    public int executeUpdate( String sql,
                              String[] columnNames ) throws SQLException {
        notClosed();
        noUpdates();
        return 0;
    }

    @Override
    public void setCursorName( String name ) throws SQLException {
        notClosed();
        noUpdates();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        notClosed();
        return -1;
    }

    @Override
    public void addBatch( String sql ) throws SQLException {
        notClosed();
        noUpdates();
    }

    @Override
    public void clearBatch() throws SQLException {
        notClosed();
        noUpdates();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        notClosed();
        noUpdates();
        return null;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------------------------------------------------------

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

    @Override
    public boolean execute( String sql,
                            int autoGeneratedKeys ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean execute( String sql,
                            int[] columnIndexes ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean execute( String sql,
                            String[] columnNames ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSet executeQuery( String sql ) throws SQLException {
        execute(sql);
        return getResultSet();
    }

    @Override
    public ResultSet getGeneratedKeys() /*throws SQLException*/{
        // TODO: if and when ModeShape supports providing key information
        // then a result set containing the metadata will need to be created.
        return new JcrResultSet();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        notClosed();
        return moreResults > 0;
    }

    @Override
    public boolean getMoreResults( int current ) throws SQLException {
        notClosed();
        if (current != CLOSE_ALL_RESULTS && current != CLOSE_CURRENT_RESULT && current != KEEP_CURRENT_RESULT) {
            throw new SQLException(JdbcLocalI18n.invalidArgument.text(current, "" + CLOSE_ALL_RESULTS + ", "
                                                                               + CLOSE_CURRENT_RESULT + ", "
                                                                               + KEEP_CURRENT_RESULT));
        }
        if (KEEP_CURRENT_RESULT != current) {
            // Close (by nulling) the results ...
            // jcrResults = null;
            results = null;
        }
        if (moreResults > 0) --moreResults;
        return moreResults > 0;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        notClosed();
        return results; // may be null
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        notClosed();
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        notClosed();
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public int getResultSetType() throws SQLException {
        notClosed();
        return ResultSet.TYPE_SCROLL_INSENSITIVE;
    }

    @Override
    public void setEscapeProcessing( boolean enable ) throws SQLException {
        notClosed();
        // Ignore for now
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) /*throws SQLException*/{
        return iface.isInstance(this);
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (!isWrapperFor(iface)) {
            throw new SQLException(JdbcLocalI18n.classDoesNotImplementInterface.text(Statement.class.getSimpleName(),
                                                                                     iface.getName()));
        }
        return iface.cast(this);
    }

    @Override
    public void closeOnCompletion() {
    }

    @Override
    public boolean isCloseOnCompletion() {
        return true;
    }
}
