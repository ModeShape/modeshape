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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.modeshape.jcr.api.Repositories;
import org.modeshape.jdbc.util.UnmodifiableProperties;
import org.modeshape.jdbc.util.StringUtil;
import org.modeshape.jdbc.JcrConnection;
import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JcrMetaData;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.ModeShapeMetaData;
import org.modeshape.jdbc.JcrDriver.JcrContextFactory;
import org.modeshape.jdbc.util.TextDecoder;
import org.modeshape.jdbc.util.UrlEncoder;

/**
 * The LocalRepositoryDelegate provides a local Repository implementation to access the Jcr layer via JNDI Lookup.
 */
public class LocalRepositoryDelegate implements RepositoryDelegate {
    private static final String JNDI_EXAMPLE_URL = JcrDriver.JNDI_URL_PREFIX
	    + "{jndiName}";
    
    public static final TextDecoder URL_DECODER = new UrlEncoder();


     private JcrContextFactory jcrContext = null;
    
    private QueryResult jcrResults;
    private Query jcrQuery;
    private JNDIConnectionInfo connInfo = null;
    private Session session;
    private Repository repository = null;
    private Set<String> repositoryNames = null;

    public LocalRepositoryDelegate(String url, Properties info,
	    JcrContextFactory contextFactory) {
	super();

	if (contextFactory == null) {
	    jcrContext = new JcrContextFactory() {
		public Context createContext(Properties properties)
				throws NamingException {
		    		    
		    InitialContext initContext = ( (properties == null || properties.isEmpty()) ? new InitialContext()
		    : new InitialContext(properties));

		    return initContext;
		}
	    };
	} else {
	    this.jcrContext = contextFactory;
	}
	connInfo = new JNDIConnectionInfo(url, info);

    }
    
    protected Session session() throws RepositoryException {
        if (session == null) {
            Credentials credentials = connInfo.getCredentials();
            String workspaceName = connInfo.getWorkspaceName();
 
                if (workspaceName != null) {
                    this.session = credentials != null ? repository.login(credentials, workspaceName) : repository.login(workspaceName);
                } else {
                    this.session = credentials != null ? repository.login(credentials) : repository.login();
                }
             // this shouldn't happen, but in testing it did occur only because of the repository not being setup correctly
            assert session != null;
        }
        return session;
    }
    
    public NodeType nodeType( String name ) throws RepositoryException {
          return session().getWorkspace().getNodeTypeManager().getNodeType(name);
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

	if (connInfo.getJndiName() == null) {
	    String msg = JdbcI18n.urlMustContainJndiNameOfRepositoryOrRepositoriesObject
		    .text();
	    throw new SQLException(msg);
	}

	// Look up the object in JNDI and find the JCR Repository object ...
	String jndiName = connInfo.getJndiName();
	Context context = null;
	try {
	    context = this.jcrContext.createContext(connInfo.getProperties());
	} catch (NamingException e) {
	    throw new SQLException(JdbcI18n.unableToGetJndiContext.text(e
		    .getLocalizedMessage()));
	}
	if (context == null) {
	    throw new SQLException(JdbcI18n.unableToFindObjectInJndi
		    .text(jndiName));
	    
	}
	repository = null;
	try {
	           
	    Object target = context.lookup(jndiName);

	    if (target instanceof Repositories) {
		Repositories repositories = (Repositories) target;
		String repositoryName = connInfo.getRepositoryName();
		if (repositoryName == null) {
		    repositoryNames = repositories.getRepositoryNames();
		    if (repositoryNames == null || repositoryNames.isEmpty()) {
			throw new SQLException(JdbcI18n.noRepositoryNamesFound.text());
			
		    }
		    if(repositoryNames != null &&
			    repositoryNames.size() == 1) {
			repositoryName = repositoryNames.iterator().next();
		    } else {
			throw new SQLException(JdbcI18n.objectInJndiIsRepositories
				.text(jndiName));
		    }
		}
		try {
		    repository = repositories.getRepository(repositoryName);
		} catch (RepositoryException e) {
		    throw new SQLException(JdbcI18n.unableToFindNamedRepository
			    .text(jndiName, repositoryName));
		}
	    } else if (target instanceof Repository) {
		repository = (Repository) target;
		repositoryNames = new HashSet<String>(1);
		repositoryNames.add("DefaultRepository");
	    } else {
		throw new SQLException(
			JdbcI18n.objectInJndiMustBeRepositoryOrRepositories
				.text(jndiName));
	    }
	    assert repository != null;
	} catch (NamingException e) {
	    throw new SQLException(JdbcI18n.unableToFindObjectInJndi
		    .text(jndiName), e);
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

    protected class JNDIConnectionInfo extends ConnectionInfo {
	private String jndiName;

	/**
	 * @param url
	 * @param properties
	 */
	protected JNDIConnectionInfo(String url, Properties properties) {
	    super(url, properties);
	    init();

	}

	protected String getJndiName() {
	    return jndiName;
	}

	private void init() {
	    Properties props = getProperties() != null ? new Properties(
		    getProperties()) : new Properties();
	    jndiName = getUrl().substring(
		    JcrDriver.JNDI_URL_PREFIX.length());

	    // Find any URL parameters ...
	    int questionMarkIndex = jndiName.indexOf('?');
	    if (questionMarkIndex != -1) {
		if (jndiName.length() > questionMarkIndex + 1) {
		    String paramStr = jndiName.substring(questionMarkIndex + 1);
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
		jndiName = jndiName.substring(0, questionMarkIndex).trim();
	    }

	    Properties newprops = new UnmodifiableProperties(props);
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
	    StringBuilder url = new StringBuilder(JcrDriver.JNDI_URL_PREFIX);
	    url.append(this.jndiName);
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
	
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public DriverPropertyInfo[] getPropertyInfos() {
		JcrDriver.JcrContextFactory cf = jcrContext;
		List<DriverPropertyInfo> results = new ArrayList<DriverPropertyInfo>();
		if (getUrl() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.urlPropertyName.text(), null);
		    info.description = JdbcI18n.urlPropertyDescription
			    .text(JNDI_EXAMPLE_URL);
		    info.required = true;
		    info.choices = new String[] { JNDI_EXAMPLE_URL };
		    results.add(info);
		    cf = null;
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
		boolean nameRequired = false;
		if (getRepositoryName() == null) {
		    boolean found = false;
		    if (cf != null) {
			try {
			    Context context = cf
				    .createContext(getProperties());
			    Object obj = context.lookup(  ((JNDIConnectionInfo) getConnectionInfo()).getJndiName() );
			    if (obj instanceof Repositories) {
				nameRequired = true;
				found = true;
			    } else if (obj instanceof Repository) {
				found = true;
			    }
			} catch (NamingException e) {
			    // do nothing about it ...
			}
		    }
		    if (nameRequired || !found) {
			DriverPropertyInfo info = new DriverPropertyInfo(
				JdbcI18n.repositoryNamePropertyName.text(), null);
			info.description = JdbcI18n.repositoryNamePropertyDescription
				.text();
			info.required = nameRequired;
			info.choices = null;
			results.add(info);
		    }
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
