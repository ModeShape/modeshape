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
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.modeshape.jcr.api.Repositories;
import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JcrDriver.JcrContextFactory;
import org.modeshape.jdbc.JdbcI18n;

/**
 * The LocalRepositoryDelegate provides a local Repository implementation to access the Jcr layer via JNDI Lookup.
 */
public class LocalRepositoryDelegate extends AbstractRepositoryDelegate {
    private static final String JNDI_EXAMPLE_URL = JcrDriver.JNDI_URL_PREFIX + "{jndiName}";
    
    protected static final Set<LocalSession> TRANSACTION_IDS = java.util.Collections.synchronizedSet(new HashSet<LocalSession>());

    private JcrContextFactory jcrContext = null;
    
    public LocalRepositoryDelegate( String url,
                                    Properties info,
                                    JcrContextFactory contextFactory ) {
        super(url, info);

        if (contextFactory == null) {
            jcrContext = new JcrContextFactory() {
                public Context createContext( Properties properties ) throws NamingException {
                    InitialContext initContext = ((properties == null || properties.isEmpty()) ? new InitialContext() : new InitialContext( properties));
                    return initContext;
                }
            };
        } else {
            this.jcrContext = contextFactory;
        }
    }

    @Override
    protected ConnectionInfo createConnectionInfo( String url,
                                                   Properties info ) {
        return new JNDIConnectionInfo(url, info);
    }

	private JcrContextFactory getJcrContext() {
        return jcrContext;
    }
    
    private LocalSession getLocalSession() throws LoginException, NoSuchWorkspaceException, RepositoryException {
    	return LocalSession.getLocalSessionInstance().getLocalSession(getRepository(), getConnectionInfo());
    }
    
    private LocalSession getCurrentLocalSession() {
    	return LocalSession.getLocalSessionInstance().getLocalSession();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jdbc.delegate.RepositoryDelegate#getDescriptor(java.lang.String)
     */
    @Override
    public String getDescriptor( String descriptorKey ) {
        return getRepository().getDescriptor(descriptorKey);
    }

    @Override
    public NodeType nodeType( String name ) throws RepositoryException {
    	LocalSession localSession = getLocalSession();
		return localSession.getSession().getWorkspace().getNodeTypeManager().getNodeType(name);
    }

    @Override
    public List<NodeType> nodeTypes() throws RepositoryException {
        
    	LocalSession localSession = getLocalSession();
        List<NodeType> types = new ArrayList<NodeType>();
        NodeTypeIterator its = localSession.getSession().getWorkspace().getNodeTypeManager().getAllNodeTypes();
        while (its.hasNext()) {
            types.add((NodeType)its.next());
        }
        return types;

    }

    /**
     * This execute method is used for redirection so that the JNDI implementation can control calling execute.
     * 
     * @see java.sql.Statement#execute(java.lang.String)
     */
    @Override
    public QueryResult execute( String query,
                                String language ) throws RepositoryException {
        LOGGER.trace("Executing query: {0}" + query);

        // Create the query ...
        
        final Query jcrQuery = getLocalSession().getSession().getWorkspace().getQueryManager().createQuery(query, language);
        return  jcrQuery.execute();
    }

    @Override
    protected void createRepository() throws SQLException {
        LOGGER.debug("Creating repository for LocalRepositoryDelegte");

        Repository repository = null;
        Set<String> repositoryNames = null;
        ConnectionInfo connInfo = this.getConnectionInfo();
        assert connInfo != null;

        // Look up the object in JNDI and find the JCR Repository object ...
        String jndiName = connInfo.getRepositoryPath();
        if (jndiName == null) {
            String msg = JdbcI18n.urlMustContainJndiNameOfRepositoryOrRepositoriesObject.text();
            throw new SQLException(msg);
        }

        Context context = null;
        try {
            context = this.jcrContext.createContext(connInfo.getProperties());
        } catch (NamingException e) {
            throw new SQLException(JdbcI18n.unableToGetJndiContext.text(e.getLocalizedMessage()));
        }
        if (context == null) {
            throw new SQLException(JdbcI18n.unableToFindObjectInJndi.text(jndiName));

        }
        String repositoryName = "NotAssigned";
        try {

            Object target = context.lookup(jndiName);
            repositoryName = connInfo.getRepositoryName();

            if (target instanceof Repositories) {
                LOGGER.trace("JNDI Lookup found Repositories ");
                Repositories repositories = (Repositories)target;

                if (repositoryName == null) {
                    repositoryNames = repositories.getRepositoryNames();
                    if (repositoryNames == null || repositoryNames.isEmpty()) {
                        throw new SQLException(JdbcI18n.noRepositoryNamesFound.text());
                    }
                    if (repositoryNames.size() == 1) {
                        repositoryName = repositoryNames.iterator().next();
                        connInfo.setRepositoryName(repositoryName);
                        LOGGER.trace("Setting Repository {0} as default", repositoryName);

                    } else {
                        throw new SQLException(JdbcI18n.objectInJndiIsRepositories.text(jndiName));
                    }
                }
                try {
                    repository = repositories.getRepository(repositoryName);
                } catch (RepositoryException e) {
                    throw new SQLException(JdbcI18n.unableToFindNamedRepository.text(jndiName, repositoryName));
                }
            } else if (target instanceof Repository) {
                LOGGER.trace("JNDI Lookup found a Repository");
                repository = (Repository)target;
                repositoryNames = new HashSet<String>(1);

                if (repositoryName == null) {
                    repositoryName = ("DefaultRepository");
                    connInfo.setRepositoryName(repositoryName);
                }

                repositoryNames.add(repositoryName);
            } else {
                throw new SQLException(JdbcI18n.objectInJndiMustBeRepositoryOrRepositories.text(jndiName));
            }
            assert repository != null;
        } catch (NamingException e) {
            throw new SQLException(JdbcI18n.unableToFindObjectInJndi.text(jndiName), e);
        }
        this.setRepository(repository);
        this.setRepositoryName(repositoryName);
        this.setRepositoryNames(repositoryNames);

    }

    /**
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid( final int timeout ) throws RepositoryException {
        
    	LocalSession ls = getLocalSession();
    	if (! ls.getSession().isLive()) {
    		ls.remove();
    		return false;
    	}
    	
        return true;
    }
    
    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.jdbc.delegate.RepositoryDelegate#closeStatement()
     */
    @Override
    public void closeStatement() {
    	LocalSession session = getCurrentLocalSession();
		try {
			if (session != null) {
				session.remove();
			}
		} catch (Exception e) {
			// do nothing
		} 
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() {
    	for (Iterator<LocalSession> it= TRANSACTION_IDS.iterator(); it.hasNext();) {
    		LocalSession id = it.next();
    		id.remove();
    	}
    }   



    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws RepositoryException {
    	closeStatement();
    }

    /**
     * @param iface
     * @param <T>
     * @return <T> T
     * @throws SQLException
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {

        try {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }

            if (iface.isInstance(Workspace.class)) {
                Workspace workspace = getLocalSession().getSession().getWorkspace();
                return iface.cast(workspace);
            }
        } catch (RepositoryException re) {
            throw new SQLException(re.getLocalizedMessage());
        }

        throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text(Connection.class.getSimpleName(), iface.getName()));
    }

    class JNDIConnectionInfo extends ConnectionInfo {

        /**
         * @param url
         * @param properties
         */
        protected JNDIConnectionInfo( String url,
                                      Properties properties ) {
            super(url, properties);
        }

        @Override
        public String getUrlExample() {
            return JNDI_EXAMPLE_URL;
        }

        @Override
        public String getUrlPrefix() {
            return JcrDriver.JNDI_URL_PREFIX;
        }

        @Override
        protected void addRepositoryNamePropertyInfo( List<DriverPropertyInfo> results ) {
            boolean no_errors = results.size() == 0;
            boolean nameRequired = false;
            if (getRepositoryName() == null) {
                boolean found = false;
                if (no_errors) {
                    try {
                        Context context = getJcrContext().createContext(getProperties());
                        Object obj = context.lookup(getRepositoryPath());
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
                    DriverPropertyInfo info = new DriverPropertyInfo(JdbcI18n.repositoryNamePropertyName.text(), null);
                    info.description = JdbcI18n.repositoryNamePropertyDescription.text();
                    info.required = nameRequired;
                    info.choices = null;
                    results.add(info);
                }
            }
        }
    }
  
}
