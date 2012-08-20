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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.ClientInfoStatus;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import org.modeshape.jdbc.delegate.ConnectionInfo;
import org.modeshape.jdbc.delegate.RepositoryDelegate;

/**
 * This driver's implementation of JDBC {@link Connection}.
 */
public class JcrConnection implements Connection {

    public static final String JCR_SQL2 = Query.JCR_SQL2;
    @SuppressWarnings( "deprecation" )
    public static final String JCR_SQL = Query.SQL;

    private boolean closed;
    private boolean autoCommit = true;
    private SQLWarning warning;
    private Properties clientInfo = new Properties();
    private DatabaseMetaData metadata;
    private final RepositoryDelegate jcrDelegate;
    private final DriverInfo driverInfo;

    public JcrConnection( RepositoryDelegate jcrDelegate,
                          DriverInfo driverInfo ) {
        this.jcrDelegate = jcrDelegate;
        this.driverInfo = driverInfo;
        assert this.jcrDelegate != null;
        assert this.driverInfo != null;
    }

    protected ConnectionInfo info() {
        return this.jcrDelegate.getConnectionInfo();
    }

    protected DriverInfo driverInfo() {
        return this.driverInfo;
    }

    /**
     * Returns the interface used to communicate to the Jcr Repository.
     * 
     * @return RepositoryDelegate
     */
    public RepositoryDelegate getRepositoryDelegate() {
        return this.jcrDelegate;
    }

    protected NodeType nodeType( String name ) throws SQLException {
        try {
            return getRepositoryDelegate().nodeType(name);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        notClosed();
        return true; // always read-only
    }

    @Override
    public void setReadOnly( boolean readOnly ) throws SQLException {
        notClosed();
    }

    @Override
    public boolean isValid( int timeout ) throws SQLException {
        if (closed) return false;
        if (timeout < 0) throw new SQLException(JdbcLocalI18n.timeoutMayNotBeNegative.text());
        try {
            return this.getRepositoryDelegate().isValid(timeout);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    @Override
    public void close() {
        if (!closed) {
            try {
                this.getRepositoryDelegate().close();
            } finally {
                metadata = null;
                closed = true;
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    protected final void notClosed() throws SQLException {
        if (isClosed()) throw new SQLException(JdbcLocalI18n.connectionIsClosed.text());
    }

    @Override
    public void commit() throws SQLException {
        notClosed();
        try {
            this.getRepositoryDelegate().commit();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    @Override
    public void rollback() throws SQLException {
        notClosed();
        try {
            this.getRepositoryDelegate().rollback();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    @Override
    public void rollback( Savepoint savepoint ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
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
    public boolean getAutoCommit() throws SQLException {
        notClosed();
        return autoCommit;
    }

    @Override
    public void setAutoCommit( boolean autoCommit ) throws SQLException {
        notClosed();
        this.autoCommit = autoCommit;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        notClosed();
        return Connection.TRANSACTION_READ_COMMITTED;
    }

    @Override
    public void setTransactionIsolation( int level ) throws SQLException {
        notClosed();
        // silently ignore
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Savepoint setSavepoint( String name ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void releaseSavepoint( Savepoint savepoint ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String getCatalog() {
        return this.info().getRepositoryName();
    }

    @Override
    public void setCatalog( String catalog ) {
        // silently ignore
    }

    @Override
    public Properties getClientInfo() /*throws SQLException*/{
        return clientInfo;
    }

    @Override
    public String getClientInfo( String name ) /*throws SQLException*/{
        return clientInfo.getProperty(name);
    }

    @Override
    public void setClientInfo( Properties properties ) throws SQLClientInfoException {
        Map<String, ClientInfoStatus> status = new HashMap<String, ClientInfoStatus>();
        Properties validProperties = new Properties();
        for (String name : properties.stringPropertyNames()) {
            // Don't override the built-in properties ...
            if (name == null || LocalJcrDriver.ALL_PROPERTY_NAMES.contains(name)) {
                status.put(name, ClientInfoStatus.REASON_VALUE_INVALID);
            } else {
                String value = properties.getProperty(name);
                validProperties.put(name, value);
            }
        }
        if (validProperties.isEmpty()) {
            if (!status.isEmpty()) {
                String reason = JdbcLocalI18n.invalidClientInfo.text();
                throw new SQLClientInfoException(reason, status);
            }
        } else {
            clientInfo.putAll(validProperties);
        }
    }

    @Override
    public void setClientInfo( String name,
                               String value ) throws SQLClientInfoException {
        Properties properties = new Properties();
        properties.put(name, value);
        setClientInfo(properties);
    }

    @Override
    public int getHoldability() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setHoldability( int holdability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        notClosed();
        if (metadata == null) {
            String descriptor = this.getRepositoryDelegate().getDescriptor(Repository.REP_NAME_DESC);
            if (descriptor != null && descriptor.toLowerCase().contains("modeshape")) {
                return new ModeShapeMetaData(this);
            }
            return new JcrMetaData(this);
        }
        return metadata;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() {
        return new HashMap<String, Class<?>>(1);
    }

    @Override
    public void setTypeMap( Map<String, Class<?>> map ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method pre-processes the supplied SQL-compatible query and returns the corresponding JCR-SQL2.
     * </p>
     * 
     * @see java.sql.Connection#nativeSQL(java.lang.String)
     */
    @Override
    public String nativeSQL( String sql ) {
        return sql;
    }

    @SuppressWarnings( "unused" )
    @Override
    public Statement createStatement() throws SQLException {
        return new JcrStatement(this);
    }

    @Override
    public Statement createStatement( int resultSetType,
                                      int resultSetConcurrency ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Statement createStatement( int resultSetType,
                                      int resultSetConcurrency,
                                      int resultSetHoldability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement( String sql ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int autoGeneratedKeys ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int[] columnIndexes ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement( String sql,
                                               String[] columnNames ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int resultSetType,
                                               int resultSetConcurrency ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int resultSetType,
                                               int resultSetConcurrency,
                                               int resultSetHoldability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public CallableStatement prepareCall( String sql ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public CallableStatement prepareCall( String sql,
                                          int resultSetType,
                                          int resultSetConcurrency ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public CallableStatement prepareCall( String sql,
                                          int resultSetType,
                                          int resultSetConcurrency,
                                          int resultSetHoldability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Array createArrayOf( String typeName,
                                Object[] elements ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Blob createBlob() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Clob createClob() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public NClob createNClob() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Struct createStruct( String typeName,
                                Object[] attributes ) throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) /*throws SQLException*/{
        return iface.isInstance(this) || this.getRepositoryDelegate().isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (isWrapperFor(iface)) {
            return iface.cast(this);
        }
        return getRepositoryDelegate().unwrap(iface);
    }

    /**
     * Sets the given schema name to access. If the driver does not support schemas, it will silently ignore this request. Calling
     * setSchema has no effect on previously created or prepared Statement objects. It is implementation defined whether a DBMS
     * prepare operation takes place immediately when the Connection method prepareStatement or prepareCall is invoked. For
     * maximum portability, setSchema should be called before a Statement is created or prepared.
     * 
     * @param schema - the name of a schema in which to work
     * @throws SQLException - if a database access error occurs or this method is called on a closed connection
     */
    // TODO: JDK 1.7 - add @Override and remove JavaDoc
    public void setSchema( String schema ) throws SQLException {
    }

    /**
     * Retrieves this Connection object's current schema name.
     * 
     * @return the current schema name or null if there is none
     * @throws SQLException - if a database access error occurs or this method is called on a closed connection
     */
    // TODO: JDK 1.7 - add @Override
    public String getSchema() throws SQLException {
        return null;
    }

    /**
     * Terminates an open connection.
     * <p>
     * <em>Note:</em> This method is part of the JDBC API in JDK 1.7.
     * </p>
     * 
     * @param executor The {@link Executor} implementation which will be used by <code>abort</code>.
     */
    // TODO: JDK 1.7 - add @Override
    public void abort( Executor executor ) /*throws SQLException*/{
    }

    /**
     * This method is not implemented and always throws {@link SQLFeatureNotSupportedException}.
     * <p>
     * <em>Note:</em> This method is part of the JDBC API in JDK 1.7.
     * </p>
     * 
     * @param executor The <code>Executor</code> implementation which will be used by <code>setNetworkTimeout</code>.
     * @param milliseconds The time in milliseconds to wait for the database operation to complete. If the JDBC driver does not
     *        support milliseconds, the JDBC driver will round the value up to the nearest second. If the timeout period expires
     *        before the operation completes, a SQLException will be thrown. A value of 0 indicates that there is not timeout for
     *        database operations.
     * @throws SQLException
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     */
    // TODO: JDK 1.7 - add @Override
    public void setNetworkTimeout( Executor executor,
                                   int milliseconds ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * This method is not implemented and always throws {@link SQLFeatureNotSupportedException}.
     * <p>
     * <em>Note:</em> This method is part of the JDBC API in JDK 1.7.
     * </p>
     * 
     * @return the timeout
     * @throws SQLException
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support this method
     */
    // TODO: JDK 1.7 - add @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }
}
