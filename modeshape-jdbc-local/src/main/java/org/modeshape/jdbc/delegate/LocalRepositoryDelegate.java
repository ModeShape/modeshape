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
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.modeshape.jcr.api.Repositories;
import org.modeshape.jdbc.JdbcLocalI18n;
import org.modeshape.jdbc.LocalJcrDriver;
import org.modeshape.jdbc.LocalJcrDriver.JcrContextFactory;

/**
 * The LocalRepositoryDelegate provides a local Repository implementation to access the Jcr layer via JNDI Lookup.
 */
public class LocalRepositoryDelegate extends AbstractRepositoryDelegate {
    public static final RepositoryDelegateFactory FACTORY = new RepositoryDelegateFactory() {};

    private static final String JNDI_EXAMPLE_URL = LocalJcrDriver.JNDI_URL_PREFIX + "{jndiName}";

    protected static final Set<LocalSession> TRANSACTION_IDS = java.util.Collections.synchronizedSet(new HashSet<LocalSession>());

    private JcrContextFactory jcrContext = null;
    private Repository repository = null;

    public LocalRepositoryDelegate( String url,
                                    Properties info,
                                    JcrContextFactory contextFactory ) {
        super(url, info);

        if (contextFactory == null) {
            jcrContext = new JcrContextFactory() {
                @Override
                public Context createContext( Properties properties ) throws NamingException {
                    return ((properties == null || properties.isEmpty()) ? new InitialContext() : new InitialContext(properties));
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

    protected JcrContextFactory getJcrContext() {
        return jcrContext;
    }

    private LocalSession getLocalSession() throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return LocalSession.getLocalSessionInstance().getLocalSession(repository, getConnectionInfo());
    }

    private LocalSession getCurrentLocalSession() {
        return LocalSession.getLocalSessionInstance().getLocalSession();
    }

    @Override
    public String getDescriptor( String descriptorKey ) {
        return repository.getDescriptor(descriptorKey);
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
        logger.trace("Executing query: {0}", query);

        // Create the query ...

        final Query jcrQuery = getLocalSession().getSession().getWorkspace().getQueryManager().createQuery(query, language);
        return jcrQuery.execute();
    }

    @Override
    public String explain( String query,
                           String language ) throws RepositoryException {
        logger.trace("Explaining query: {0}", query);

        // Create the query ...

        final org.modeshape.jcr.api.query.Query jcrQuery = (org.modeshape.jcr.api.query.Query)getLocalSession().getSession().getWorkspace().getQueryManager().createQuery(query,
                                                                                                                                                                          language);
        return jcrQuery.explain().getPlan();
    }

    @Override
    protected void initRepository() throws SQLException {
        if (repository != null) {
            return;
        }
        logger.debug("Creating repository for LocalRepositoryDelegate");

        Set<String> repositoryNames = null;
        ConnectionInfo connInfo = this.getConnectionInfo();
        assert connInfo != null;

        // Look up the object in JNDI and find the JCR Repository object ...
        String jndiName = connInfo.getRepositoryPath();
        if (jndiName == null) {
            String msg = JdbcLocalI18n.urlMustContainJndiNameOfRepositoryOrRepositoriesObject.text();
            throw new SQLException(msg);
        }

        Context context = null;
        try {
            context = this.jcrContext.createContext(connInfo.getProperties());
        } catch (NamingException e) {
            throw new SQLException(JdbcLocalI18n.unableToGetJndiContext.text(e.getLocalizedMessage()));
        }
        if (context == null) {
            throw new SQLException(JdbcLocalI18n.unableToFindObjectInJndi.text(jndiName));

        }
        String repositoryName = "NotAssigned";
        try {

            Object target = context.lookup(jndiName);
            repositoryName = connInfo.getRepositoryName();

            if (target instanceof Repositories) {
                logger.trace("JNDI Lookup found Repositories ");
                Repositories repositories = (Repositories)target;

                if (repositoryName == null) {
                    repositoryNames = repositories.getRepositoryNames();
                    if (repositoryNames == null || repositoryNames.isEmpty()) {
                        throw new SQLException(JdbcLocalI18n.noRepositoryNamesFound.text());
                    }
                    if (repositoryNames.size() == 1) {
                        repositoryName = repositoryNames.iterator().next();
                        connInfo.setRepositoryName(repositoryName);
                        logger.trace("Setting Repository {0} as default", repositoryName);

                    } else {
                        throw new SQLException(JdbcLocalI18n.objectInJndiIsRepositories.text(jndiName));
                    }
                }
                try {
                    repository = repositories.getRepository(repositoryName);
                } catch (RepositoryException e) {
                    throw new SQLException(JdbcLocalI18n.unableToFindNamedRepository.text(jndiName, repositoryName));
                }
            } else if (target instanceof Repository) {
                logger.trace("JNDI Lookup found a Repository");
                repository = (Repository)target;
                repositoryNames = new HashSet<String>(1);

                if (repositoryName == null) {
                    repositoryName = ("DefaultRepository");
                    connInfo.setRepositoryName(repositoryName);
                }

                repositoryNames.add(repositoryName);
            } else {
                throw new SQLException(JdbcLocalI18n.objectInJndiMustBeRepositoryOrRepositories.text(jndiName));
            }
            assert repository != null;
        } catch (NamingException e) {
            throw new SQLException(JdbcLocalI18n.unableToFindObjectInJndi.text(jndiName), e);
        }
        this.setRepositoryName(repositoryName);
        this.setRepositoryNames(repositoryNames);
    }

    @Override
    public boolean isValid( final int timeout ) throws RepositoryException {

        LocalSession ls = getLocalSession();
        if (!ls.getSession().isLive()) {
            ls.remove();
            return false;
        }

        return true;
    }

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

    @Override
    public void close() {
        for (LocalSession id : TRANSACTION_IDS) {
            id.remove();
        }
        this.repository = null;
    }

    @SuppressWarnings( "unused" )
    @Override
    public void rollback() throws RepositoryException {
        closeStatement();
    }

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

        throw new SQLException(JdbcLocalI18n.classDoesNotImplementInterface.text(Connection.class.getSimpleName(),
                                                                                 iface.getName()));
    }

    class JNDIConnectionInfo extends ConnectionInfo {

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
            return LocalJcrDriver.JNDI_URL_PREFIX;
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
                    DriverPropertyInfo info = new DriverPropertyInfo(JdbcLocalI18n.repositoryNamePropertyName.text(), null);
                    info.description = JdbcLocalI18n.repositoryNamePropertyDescription.text();
                    info.required = nameRequired;
                    info.choices = null;
                    results.add(info);
                }
            }
        }
    }

}
