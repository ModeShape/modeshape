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
import java.sql.ResultSetMetaData;
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
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.jdbc.JcrDriver.ConnectionInfo;

/**
 * This driver's implementation of JDBC {@link Connection}.
 */
@NotThreadSafe
public class JcrConnection implements Connection {

    private final ConnectionInfo info;
    private final Repository repository;
    private Session session;
    private boolean closed;
    private boolean autoCommit = true;
    private int isolation = Connection.TRANSACTION_READ_COMMITTED;
    private SQLWarning warning;
    private Properties clientInfo = new Properties();
    private JcrMetaData metadata;
    private Map<String, Class<?>> typeMap = new HashMap<String, Class<?>>();
    private Map<String, JcrType> builtInTypeMap;

    protected JcrConnection( Repository repository,
                             ConnectionInfo info ) {
        this(repository, info, null);
    }

    protected JcrConnection( Repository repository,
                             ConnectionInfo info,
                             Map<String, JcrType> builtInTypeMap ) {
        this.info = info;
        this.repository = repository;
        this.builtInTypeMap = builtInTypeMap != null ? builtInTypeMap : JcrType.builtInTypeMap();
        assert this.info != null;
        assert this.repository != null;
    }

    protected ConnectionInfo info() {
        return info;
    }

    protected Session session() throws SQLException {
        if (session == null) {
            Credentials credentials = info.getCredentials();
            String workspaceName = info.getWorkspaceName();
            try {
                if (workspaceName != null) {
                    this.session = credentials != null ? repository.login(credentials, workspaceName) : repository.login(workspaceName);
                } else {
                    this.session = credentials != null ? repository.login(credentials) : repository.login();
                }
            } catch (RepositoryException e) {
                throw new SQLException(e.getLocalizedMessage());
            }
        }
        return session;
    }

    protected NodeType nodeType( String name ) throws SQLException {
        try {
            return session().getWorkspace().getNodeTypeManager().getNodeType(name);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    /**
     * Get the information about the type with the supplied name.
     * 
     * @param typeName the name of the type
     * @return the type information for the type name
     */
    protected JcrType typeInfo( String typeName ) {
        return builtInTypeMap.get(typeName);
    }

    /**
     * Get the Java class that is used to represent values with the supplied JCR type name.
     * 
     * @param typeName the name of the type
     * @return the class; never null
     * @see ResultSetMetaData#getColumnClassName(int)
     */
    protected Class<?> typeClass( String typeName ) {
        Class<?> type = typeMap.get(typeName);
        if (type == null) {
            JcrType typeInfo = builtInTypeMap.get(typeName);
            if (typeInfo != null) {
                type = typeInfo.getRepresentationClass();
            } else {
                type = String.class;
            }
        }
        return type;
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
            session().getRootNode();
            return true;
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
                if (session != null) session.logout();
            } finally {
                metadata = null;
                session = null;
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
        if (session != null) {
            try {
                session.save();
            } catch (RepositoryException e) {
                throw new SQLException(e.getLocalizedMessage());
            }
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
        if (session != null) {
            try {
                session.refresh(false);
            } catch (RepositoryException e) {
                throw new SQLException(e.getLocalizedMessage());
            }
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
        return info.getRepositoryName();
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
            metadata = createMetaData(session());
        }
        return metadata;
    }

    protected JcrMetaData createMetaData( Session session ) {
        if (session.getRepository().getDescriptor(Repository.REP_NAME_DESC).toLowerCase().contains("modeshape")) {
            return new ModeShapeMetaData(this, session);
        }
        return new JcrMetaData(this, session);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#getTypeMap()
     */
    @Override
    public Map<String, Class<?>> getTypeMap() {
        return typeMap;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#setTypeMap(java.util.Map)
     */
    @Override
    public void setTypeMap( Map<String, Class<?>> map ) {
        this.typeMap = map != null ? map : new HashMap<String, Class<?>>();
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
    public String nativeSQL( String sql ) throws SQLException {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createStatement()
     */
    @Override
    public Statement createStatement() throws SQLException {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#createStatement(int, int)
     */
    @Override
    public Statement createStatement( int resultSetType,
                                      int resultSetConcurrency ) throws SQLException {
        return null;
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
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    @Override
    public PreparedStatement prepareStatement( String sql ) throws SQLException {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int autoGeneratedKeys ) throws SQLException {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int[] columnIndexes ) throws SQLException {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    @Override
    public PreparedStatement prepareStatement( String sql,
                                               String[] columnNames ) throws SQLException {
        return null;
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
        return null;
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
        return null;
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
        notClosed();
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
        return iface.isInstance(this) || iface.isInstance(repository) || iface.isInstance(session)
               || iface.isInstance(session.getWorkspace());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        if (iface.isInstance(session)) {
            return iface.cast(session);
        }
        if (iface.isInstance(repository)) {
            return iface.cast(repository);
        }
        Workspace workspace = session.getWorkspace();
        if (iface.isInstance(workspace)) {
            return iface.cast(workspace);
        }
        throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text(Connection.class.getSimpleName(), iface.getName()));
    }
}
