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
    @SuppressWarnings("deprecation")
    public static final String JCR_SQL = Query.SQL;

    private boolean closed;
    private boolean autoCommit = true;
    private int isolation = Connection.TRANSACTION_READ_COMMITTED;
    private SQLWarning warning;
    private Properties clientInfo = new Properties();
    private DatabaseMetaData metadata;
    private RepositoryDelegate jcrDelegate;

    public JcrConnection(RepositoryDelegate jcrDelegate ) {
        this.jcrDelegate = jcrDelegate;
        assert this.jcrDelegate != null;

    }

    public ConnectionInfo info() {
        return this.jcrDelegate.getConnectionInfo();
    }
    
    /**
     * Returns the interface used to communicate to the Jcr Repository. 
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#isReadOnly()
     */
    @Override
    public boolean isReadOnly() throws SQLException {
        notClosed();
        return true; // always read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setReadOnly(boolean)
     */
    @Override
    public void setReadOnly( boolean readOnly ) throws SQLException {
        notClosed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid( int timeout ) throws SQLException {
        if (closed) return false;
        if (timeout < 0) throw new SQLException(JdbcI18n.timeoutMayNotBeNegative.text());
        try {
            return this.getRepositoryDelegate().isValid(timeout);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#close()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#isClosed()
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    protected final void notClosed() throws SQLException {
        if (isClosed()) throw new SQLException(JdbcI18n.connectionIsClosed.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#commit()
     */
    @Override
    public void commit() throws SQLException {
        notClosed();      
        try {
    	 this.getRepositoryDelegate().commit();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws SQLException {
        notClosed();
        try {
    	 this.getRepositoryDelegate().rollback();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    @Override
    public void rollback( Savepoint savepoint ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        notClosed();
        warning = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        notClosed();
        return warning;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getAutoCommit()
     */
    @Override
    public boolean getAutoCommit() throws SQLException {
        notClosed();
        return autoCommit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    @Override
    public void setAutoCommit( boolean autoCommit ) throws SQLException {
        notClosed();
        this.autoCommit = autoCommit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getTransactionIsolation()
     */
    @Override
    public int getTransactionIsolation() throws SQLException {
        notClosed();
        return isolation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    @Override
    public void setTransactionIsolation( int level ) throws SQLException {
        notClosed();
        // silently ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setSavepoint()
     */
    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setSavepoint(java.lang.String)
     */
    @Override
    public Savepoint setSavepoint( String name ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
     */
    @Override
    public void releaseSavepoint( Savepoint savepoint ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getCatalog()
     */
    @Override
    public String getCatalog() {
        return this.info().getRepositoryName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setCatalog(java.lang.String)
     */
    @Override
    public void setCatalog( String catalog ) {
        // silently ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getClientInfo()
     */
    @Override
    public Properties getClientInfo() /*throws SQLException*/{
        return clientInfo;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getClientInfo(java.lang.String)
     */
    @Override
    public String getClientInfo( String name ) /*throws SQLException*/{
        return clientInfo.getProperty(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setClientInfo(java.util.Properties)
     */
    @Override
    public void setClientInfo( Properties properties ) throws SQLClientInfoException {
        Map<String, ClientInfoStatus> status = new HashMap<String, ClientInfoStatus>();
        Properties validProperties = new Properties();
        for (String name : properties.stringPropertyNames()) {
            // Don't override the built-in properties ...
            if (name == null || JcrDriver.ALL_PROPERTY_NAMES.contains(name)) {
                status.put(name, ClientInfoStatus.REASON_VALUE_INVALID);
            } else {
                String value = properties.getProperty(name);
                validProperties.put(name, value);
            }
        }
        if (validProperties.isEmpty()) {
            if (!status.isEmpty()) {
                String reason = JdbcI18n.invalidClientInfo.text();
                throw new SQLClientInfoException(reason, status);
            }
        } else {
            clientInfo.putAll(validProperties);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
     */
    @Override
    public void setClientInfo( String name,
                               String value ) throws SQLClientInfoException {
        Properties properties = new Properties();
        properties.put(name, value);
        setClientInfo(properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getHoldability()
     */
    @Override
    public int getHoldability() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setHoldability(int)
     */
    @Override
    public void setHoldability( int holdability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getMetaData()
     */
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

    /**
     * Retrieves the type map associated with this Connection object. The type map
     * contains entries for undefined types. This method always returns an empty
     * map since it is not possible to add entries to this type map
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getTypeMap()
     */
    @Override
    public Map<String, Class<?>> getTypeMap() {
        return new HashMap<String, Class<?>>(1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setTypeMap(java.util.Map)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createStatement()
     */
    @Override
    public Statement createStatement() throws SQLException {
        return new JcrStatement(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createStatement(int, int)
     */
    @Override
    public Statement createStatement( int resultSetType,
                                      int resultSetConcurrency ) throws SQLException {
    	throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    @Override
    public Statement createStatement( int resultSetType,
                                      int resultSetConcurrency,
                                      int resultSetHoldability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    @Override
    public PreparedStatement prepareStatement( String sql ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int autoGeneratedKeys )  throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int[] columnIndexes ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               String[] columnNames ) throws SQLException {
	throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int resultSetType,
                                               int resultSetConcurrency ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int resultSetType,
                                               int resultSetConcurrency,
                                               int resultSetHoldability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    @Override
    public CallableStatement prepareCall( String sql ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    @Override
    public CallableStatement prepareCall( String sql,
                                          int resultSetType,
                                          int resultSetConcurrency ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    @Override
    public CallableStatement prepareCall( String sql,
                                          int resultSetType,
                                          int resultSetConcurrency,
                                          int resultSetHoldability ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createArrayOf(java.lang.String, java.lang.Object[])
     */
    @Override
    public Array createArrayOf( String typeName,
                                Object[] elements ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createBlob()
     */
    @Override
    public Blob createBlob() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createClob()
     */
    @Override
    public Clob createClob() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createNClob()
     */
    @Override
    public NClob createNClob() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createSQLXML()
     */
    @Override
    public SQLXML createSQLXML() throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
     */
    @Override
    public Struct createStruct( String typeName,
                                Object[] attributes ) throws SQLException {
        notClosed();
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor( Class<?> iface ) /*throws SQLException*/{
		if ( iface.isInstance(this) ) {
		    return true;
		}
		
		return this.getRepositoryDelegate().isWrapperFor(iface); 
	    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
    	if (isWrapperFor(iface)) {
		    return iface.cast(this);
		}
		return getRepositoryDelegate().unwrap(iface);
    }

}
