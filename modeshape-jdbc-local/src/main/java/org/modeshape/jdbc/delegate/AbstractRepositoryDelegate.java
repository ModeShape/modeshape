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
package org.modeshape.jdbc.delegate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.modeshape.common.logging.Logger;
import org.modeshape.jdbc.DriverInfo;
import org.modeshape.jdbc.JcrConnection;
import org.modeshape.jdbc.JdbcLocalI18n;

/**
 * The AbstractRepositoryDelegate provides the common logic for the implementation of the {@link RepositoryDelegate}
 */
public abstract class AbstractRepositoryDelegate implements RepositoryDelegate {

    protected final Logger logger = Logger.getLogger(getClass());

    private Repository repository = null;
    private Set<String> repositoryNames = null;
    private ConnectionInfo connInfo = null;
    private String url;
    private Properties propertiesInfo;

    public AbstractRepositoryDelegate( String url,
                                       Properties info ) {
        this.url = url;
        this.propertiesInfo = info;
    }

    /**
     * Returns a  {@link ConnectionInfo} object which represents the information of a specific connection, from a
     * given url format and some {@link Properties}
     *
     * @param url  a {@code non-null} string which represents a jdbc url
     * @param info a {@code non-null} {@link Properties} instance which may contain extra information needed by the connection
     *
     * @return {@link ConnectionInfo} instance, never {@code null}
     */
    abstract ConnectionInfo createConnectionInfo( final String url,
                                                  final Properties info );

    /**
     * Implementor is responsible for creating the repository.
     * 
     * @throws SQLException
     */
    abstract void retrieveRepository() throws SQLException;

    @Override
    public synchronized ConnectionInfo getConnectionInfo() {
        if (this.connInfo == null) {
            this.connInfo = createConnectionInfo(url, propertiesInfo);
            this.connInfo.init();
        }
        return connInfo;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jdbc.delegate.RepositoryDelegate#closeStatement()
     */
    @Override
    public void closeStatement() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#commit()
     */
    @SuppressWarnings( "unused" )
    @Override
    public void commit() throws RepositoryException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#rollback()
     */
    @SuppressWarnings( "unused" )
    @Override
    public void rollback() throws RepositoryException {
    }

    @Override
    public Connection createConnection( DriverInfo info ) throws SQLException {
        logger.debug("Creating connection for RepositoryDelegte");
        if (this.repository == null) {
            retrieveRepository();
        }
        return new JcrConnection(this, info);
    }

    public Repository getRepository() {
        return this.repository;
    }

    protected void setRepository( Repository repository ) {
        this.repository = repository;
    }

    public String getRepositoryName() {
        return getConnectionInfo().getRepositoryName();
    }

    protected void setRepositoryName( String repositoryName ) {
        this.getConnectionInfo().setRepositoryName(repositoryName);
    }

    @Override
    public Set<String> getRepositoryNames() {
        return this.repositoryNames;
    }

    protected void setRepositoryNames( Set<String> repositoryNames ) {
        this.repositoryNames = repositoryNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor( Class<?> iface ) {
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
            throw new SQLException(JdbcLocalI18n.classDoesNotImplementInterface.text(RepositoryDelegate.class.getSimpleName(),
                                                                                     iface.getName()));
        }

        return iface.cast(this);
    }

}
