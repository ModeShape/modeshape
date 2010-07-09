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
import java.sql.DatabaseMetaData;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryResult;

import org.modeshape.jdbc.JcrConnection;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.JcrDriver.JcrContextFactory;

/**
 * 
 * The HTTPRepositoryDelegate provides remote Repository implementation to access the Jcr layer via HTTP lookup.
 */
public class HttpRepositoryDelegate implements RepositoryDelegate {
    // private static final String HTTP_EXAMPLE_URL = HTTP_URL_PREFIX +
    // "{hostname}:{port}/{context root}";

    public HttpRepositoryDelegate(String url, Properties info,
	    JcrContextFactory contextFactory) {
	super();

    }

    @Override
    public QueryResult execute(String query, String language) throws RepositoryException {

	return null;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
	return null;
    }
    
    public NodeType nodeType( String name ) throws RepositoryException {
	return null;
    }

    @Override
    public Connection createConnection() throws SQLException {
	return null;
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
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws RepositoryException {
    }
    
    public void close() {
   
    }
    

    @Override
    public DatabaseMetaData createMetaData(JcrConnection connection) {
	return null;
    }

    @Override
    public boolean isValid(int timeout) throws RepositoryException {
	return true;
    }
    
    /**
     * 
     * @param iface 
     * @return boolean
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
   public boolean isWrapperFor( Class<?> iface ) {
       return iface.isInstance(this) || 
	iface.isInstance(Repository.class) || 
	iface.isInstance(Session.class) || 
	iface.isInstance(Workspace.class);
   }
    
    /**
     * 
     * @param iface 
     * @param <T> 
     * @return <T> T
     * @throws SQLException 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    public <T> T unwrap( Class<T> iface ) throws SQLException {
	       if (iface.isInstance(this)) {
	            return iface.cast(this);
	        }
	        throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text(Connection.class.getSimpleName(), iface.getName()));
    }


    public class HttpConnectionInfo extends ConnectionInfo {

	/**
	 * @param url
	 * @param properties
	 */
	protected HttpConnectionInfo(String url, Properties properties) {
	    super(url, properties);

	}

	@Override
	public String getEffectiveUrl() {
	    return "";
	}
	
	  /**
	  * Obtain the array of {@link DriverPropertyInfo} objects that describe the missing properties.
	  * 
	  * @return DriverPropertyInfo the property infos; never null but possibly empty
	  */
	@Override
	public DriverPropertyInfo[] getPropertyInfos( ) {
	    return null;
	}

    }

}
