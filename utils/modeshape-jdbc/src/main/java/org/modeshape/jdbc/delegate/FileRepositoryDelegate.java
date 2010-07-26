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

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.modeshape.graph.property.NamespaceException;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jdbc.JcrConnection;
import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JcrMetaData;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.ModeShapeMetaData;
import org.modeshape.jdbc.util.StringUtil;
import org.modeshape.jdbc.util.TextDecoder;
import org.modeshape.jdbc.util.UrlEncoder;

/**
 * The FileRepositoryDelegate provides a local Repository implementation so that ModeShape JcrEngine will
 * be run local to the JcrDriver.   This is also referred to as an embedded mode.
 */
public class FileRepositoryDelegate implements RepositoryDelegate {
    private static final String FILE_EXAMPLE_URL = JcrDriver.FILE_URL_PREFIX
	    + "//file.path";
    
    public static final TextDecoder URL_DECODER = new UrlEncoder();

    private QueryResult jcrResults;
    private Query jcrQuery;
    private FileConnectionInfo connInfo = null;
    private Session session;
    private Repository repository = null;
    private Set<String> repositoryNames = null;
 
    public FileRepositoryDelegate(String url, Properties info) {
	super();

		connInfo = new FileConnectionInfo(url, info);

    }
    
    protected Session session() throws RepositoryException {
        if (session == null) {
        	
        	this.session =  repository.login();
            // this shouldn't happen, but in testing it did occur only because of the repository not being setup correctly
            assert session != null;
        }
        return session;
    }
    
    public NodeType nodeType( String name ) throws RepositoryException {
          return session().getWorkspace().getNodeTypeManager().getNodeType(name);
     }
    
    public List<NodeType> nodeTypes( ) throws RepositoryException {
    	List<NodeType>types = new ArrayList<NodeType>();
        NodeTypeIterator its = session().getWorkspace().getNodeTypeManager().getAllNodeTypes();
        while(its.hasNext()) {
        	types.add((NodeType) its.next());       	
        }
        
        return types;
   }


    /**
     * This execute method is used for redirection so that the JNDI implementation can control calling execute. 
     * 
     * @see java.sql.Statement#execute(java.lang.String)
     */
    @Override
    public QueryResult execute(String query, String language) throws RepositoryException {
		jcrQuery = null;
		jcrResults = null;
	
		// Create the query ...
		jcrQuery = session().getWorkspace().getQueryManager().createQuery(query,
			language);
		jcrResults = jcrQuery.execute();
	
		return jcrResults;// always a ResultSet

    }

    @Override
    public Connection createConnection() throws SQLException {
    	
		try {
			
			String filename = connInfo.getFileName();
			if (filename == null) {
			    String msg = JdbcI18n.unableToGetJndiContext.text(connInfo.getEffectiveUrl());
			    throw new SQLException(msg);
			}
			
			File f = new File(filename);
			JcrConfiguration  jcrConfig = new JcrConfiguration().loadFrom(f);
		    JcrEngine eng = jcrConfig.build();
		    eng.start();
		    
		    String repositoryName = eng.getRepositoryNames().iterator().next();

		    repository = eng.getRepository(repositoryName);
		    
		    this.connInfo.setRepositoryName(repositoryName);
		    
			repositoryNames = new HashSet<String>(1);
			repositoryNames.add(repositoryName);
			
		    assert repository != null;
		} catch (Throwable e) {
			throw new SQLException(e.getMessage(), e);
		}

		return new JcrConnection(repository, connInfo, this);
    }

    public ConnectionInfo getConnectionInfo() {
	return connInfo;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#commit()
     */
    @Override
    public void commit() throws RepositoryException {
        if (session != null) {
                 session.save();
         }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws RepositoryException {
        if (session != null) {
                 session.refresh(false);
         }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() {
	try {
	    if (session != null)
		session.logout();
	} finally {
	    session = null;
	}
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

	        if (iface.isInstance(Session.class)) {
	            return iface.cast(session);
	        }
	        if (iface.isInstance(repository)) {
	            return iface.cast(repository);
	        }
	        
	        if (iface.isInstance(Workspace.class)) {
	            Workspace workspace = session.getWorkspace();
	            return iface.cast(workspace);
	        }
	        throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text(Connection.class.getSimpleName(), iface.getName()));
    }
    
    /**
     * 
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid( final int timeout ) throws RepositoryException {

	session().getRootNode();
        return true;
    }
    
    public Set<String> getRepositoryNames()  {
	return this.repositoryNames;
    }
    
    public DatabaseMetaData createMetaData(final JcrConnection connection ) throws RepositoryException {
	Session localSession = session();
	
	if (localSession.getRepository().getDescriptor(Repository.REP_NAME_DESC) != null) {
	    if (localSession.getRepository().getDescriptor(Repository.REP_NAME_DESC).toLowerCase().contains("modeshape")) {
		return new ModeShapeMetaData(connection, localSession);
	    }
	}
        return new JcrMetaData(connection, localSession);
    }

    protected class FileConnectionInfo extends ConnectionInfo {
	private String fileName;

	/**
	 * @param url
	 * @param properties
	 */
	protected FileConnectionInfo(String url, Properties properties) {
	    super(url, properties);
	    init();

	}

	
	protected String getFileName() {
	    return fileName;
	}

	private void init() {
	    Properties props = getProperties() != null ? (Properties) getProperties().clone() : new Properties();
	    fileName = getUrl().substring(
		    JcrDriver.FILE_URL_PREFIX.length());

	    // Find any URL parameters ...
	    int questionMarkIndex = fileName.indexOf('?');
	    if (questionMarkIndex != -1) {
			if (fileName.length() > questionMarkIndex + 1) {
			    String paramStr = fileName.substring(questionMarkIndex + 1);
			    for (String param : paramStr.split("&")) {
				String[] pair = param.split("=");
				if (pair.length > 1) {
				    String key = URL_DECODER
					    .decode(pair[0] != null ? pair[0].trim()
						    : null);
				    String value = URL_DECODER
					    .decode(pair[1] != null ? pair[1].trim()
						    : null);
				    if (!props.containsKey(key)) {
					props.put(key, value);
				    }
				}
			    }
			}
			fileName = fileName.substring(0, questionMarkIndex).trim();
	    }

	    Properties newprops = new Properties(props);
	    this.setProperties(newprops);
	    String url = getUrl();
	    this.setUrl(url != null ? url.trim() : null);
	}

	/**
	 * Get the effective URL of this connection, which places all properties
	 * on the URL (with a '*' for each character in the password property)
	 * 
	 * @return the effective URL; never null
	 */
	@Override
	public String getEffectiveUrl() {
	    StringBuilder url = new StringBuilder(JcrDriver.FILE_URL_PREFIX);
	    url.append(this.fileName);
	    char propertyDelim = '?';
	    for (String propertyName : getProperties().stringPropertyNames()) {
		String value = getProperties().getProperty(propertyName);
		if (value == null)
		    continue;
		if (JcrDriver.PASSWORD_PROPERTY_NAME.equals(propertyName)) {
		    value = StringUtil.createString('*', value.length());
		}
		url.append(propertyDelim).append(propertyName).append('=')
			.append(value);
		propertyDelim = '&';
	    }
	    return url.toString();
	}
	
	    @Override
	    public DriverPropertyInfo[] getPropertyInfos() {
		List<DriverPropertyInfo> results = new ArrayList<DriverPropertyInfo>();
		if (getUrl() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.urlPropertyName.text(), null);
		    info.description = JdbcI18n.urlPropertyDescription
			    .text(FILE_EXAMPLE_URL);
		    info.required = true;
		    info.choices = new String[] { FILE_EXAMPLE_URL };
		    results.add(info);
		}
		if (getUsername() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.usernamePropertyName.text(), null);
		    info.description = JdbcI18n.usernamePropertyDescription.text();
		    info.required = false;
		    info.choices = null;
		    results.add(info);
		}
		if (getPassword() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.passwordPropertyName.text(), null);
		    info.description = JdbcI18n.passwordPropertyDescription.text();
		    info.required = false;
		    info.choices = null;
		    results.add(info);
		}

		if (getWorkspaceName() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.workspaceNamePropertyName.text(), null);
		    info.description = JdbcI18n.workspaceNamePropertyDescription.text();
		    info.required = false;
		    info.choices = null;
		    results.add(info);
		}
		return results.toArray(new DriverPropertyInfo[results.size()]);

	    }

    }

}
