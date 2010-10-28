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
package org.modeshape.jdbc.delegate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.modeshape.jdbc.JcrConnection;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.util.Logger;

/**
 * The AbstractRepositoryDelegate provides the common logic for the implementation of the {@link RepositoryDelegate}
 */
public abstract class AbstractRepositoryDelegate implements RepositoryDelegate {
    protected static final Logger LOGGER = Logger.getLogger("org.modeshape.jdbc.delegate");

    private Repository repository = null;
    private Set<String> repositoryNames = null;
    private ConnectionInfo connInfo = null;
    private String url;
    private Properties propertiesInfo;
  
    public AbstractRepositoryDelegate(String url, Properties info) {
	super();
		this.url = url;
		this.propertiesInfo = info;
    }
   
    /**
     * The implementor must return a @link ConnectionInfo that provides the information
     * that details 
     * r
     * @param url
     * @param info
     * @return ConnectionInfo
     */
    abstract ConnectionInfo createConnectionInfo(final String url, final Properties info);
    	   
	/**
	 * Implementor is responsible for creating the repository.
	 * @throws SQLException
	 */
    abstract void createRepository() throws SQLException;


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
    @Override
    public void rollback() throws RepositoryException {
    }

    
    @Override
	public  Connection createConnection() throws SQLException {
       	LOGGER.debug("Creating connection for RepositoryDelegte" );
       	if (this.repository == null) {
       		createRepository();
       	}

		return new JcrConnection(this);
    }  
       
    public synchronized Repository getRepository()  {
    	return this.repository;
    }
    
    protected void setRepository(Repository repository) {
    	this.repository = repository;
    }
    
    public String getRepositoryName()  {
    	return getConnectionInfo().getRepositoryName();
    }
    
    protected void setRepositoryName(String repositoryName) {
    	this.getConnectionInfo().setRepositoryName(repositoryName);
    }
       
    public Set<String> getRepositoryNames()  {
    	return this.repositoryNames;
    }
    
    protected void setRepositoryNames(Set<String> repositoryNames) {
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
    	       throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text(RepositoryDelegate.class.getSimpleName(),
                       iface.getName()));
    	}
    	
    	return iface.cast(this);
     }
   
}

