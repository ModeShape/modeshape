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

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryResult;
import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.JcrDriver.JcrContextFactory;
import org.modeshape.web.jcr.rest.client.domain.QueryRow;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.domain.Workspace;
import org.modeshape.web.jcr.rest.client.json.JsonRestClient;

/**
 * The HTTPRepositoryDelegate provides remote Repository implementation to access the Jcr layer via HTTP lookup.
 */
public class HttpRepositoryDelegate extends AbstractRepositoryDelegate {
    private static final String HTTP_EXAMPLE_URL = JcrDriver.HTTP_URL_PREFIX + "{hostname}:{port}/{context root}";

    private JsonRestClient restClient;
    private Workspace workspace = null;
    private Map<String, NodeType> nodeTypes;
    private final Lock nodeTypeLock = new ReentrantLock();

    public HttpRepositoryDelegate( String url,
                                   Properties info,
                                   JcrContextFactory contextFactory ) {
        super(url, info);
    }

    @Override
    protected ConnectionInfo createConnectionInfo( String url,
                                                   Properties info ) {
        return new HttpConnectionInfo(url, info);
    }


    @Override
    public QueryResult execute( String query,
                                String language ) {
        LOGGER.trace("Executing query: {0}" + query);

        try {
            List<QueryRow> results = this.restClient.query(workspace, language, query);
            Iterator<QueryRow> resultsIt = results.iterator();
            while (resultsIt.hasNext()) {
                /*final QueryRow row =*/resultsIt.next();

            }

        } catch (Exception e) {
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jdbc.delegate.RepositoryDelegate#getDescriptor(java.lang.String)
     */
    @Override
    public String getDescriptor( String descriptorKey ) {
        return "";
    }

    @Override
    public NodeType nodeType( String name ) throws RepositoryException {
        if (nodeTypes == null) nodeTypes();

        NodeType nodetype = nodeTypes.get(name);
        if (nodetype == null) {
            throw new RepositoryException(JdbcI18n.unableToGetNodeType.text(name));
        }

        return nodetype;
    }

    @Override
    public List<NodeType> nodeTypes() throws RepositoryException {
        try {
            nodeTypeLock.lock();
            if (nodeTypes == null) {
                Map<String, NodeType> nodeTypesByName;
                try {
                    nodeTypesByName = this.restClient.getNodeTypes(workspace.getRepository());
                    if (nodeTypesByName == null || nodeTypesByName.isEmpty()) {
                        String msg = JdbcI18n.noNodeTypesReturned.text(this.workspace.getServer().getUrl() + "/"
                                                                       + this.workspace.getRepository().getName() + "/"
                                                                       + this.workspace.getName());
                        throw new RepositoryException(msg);
                    }
                    this.nodeTypes = nodeTypesByName;
                } catch (Exception e) {
                    throw new RepositoryException(JdbcI18n.unableToGetNodeTypes.text(this.workspace.getRepository().getName()), e);
                }
            }
            return new ArrayList<NodeType>(nodeTypes.values());
        } finally {
            nodeTypeLock.unlock();
        }
    }

    @Override
    protected void createRepository() throws SQLException {
        LOGGER.debug("Creating repository for HttpRepositoryDelegte");

        ConnectionInfo info = getConnectionInfo();
        assert info != null;

        String path = info.getRepositoryPath();
        if (path == null) {
            throw new SQLException("Missing repo path from " + info.getUrl());
        }
        if (info.getUsername() == null) {
            throw new SQLException("Missing username from " + info.getUrl());
        }
        if (info.getPassword() == null) {
            throw new SQLException("Missing password path from " + info.getUrl());
        }
        if (info.getRepositoryName() == null) {
            throw new SQLException("Missing repo name from " + info.getUrl());
        }
        Server server = new Server("http://" + path, info.getUsername(), new String(info.getPassword()));
        org.modeshape.web.jcr.rest.client.domain.Repository repo = new org.modeshape.web.jcr.rest.client.domain.Repository(
                                                                                                                           info.getRepositoryName(),
                                                                                                                           server);
        workspace = new Workspace(info.getWorkspaceName(), repo);

        restClient = new JsonRestClient();

        // this is only a connection test to confirm a connection can be made and results can be obtained.
        try {
            restClient.getRepositories(server);
        } catch (Exception e) {
            throw new SQLException(JdbcI18n.noRepositoryNamesFound.text(), e);
        }

        Set<String> repositoryNames = new HashSet<String>(1);
        repositoryNames.add(info.getRepositoryName());

        this.setRepositoryNames(repositoryNames);

    }
    

	/**
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid( final int timeout ) {
        try {
            this.restClient.getWorkspaces(workspace.getRepository());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() {
    	super.close();
        restClient = null;
        workspace = null;
        if (nodeTypes != null) nodeTypes.clear();
    }

    class HttpConnectionInfo extends ConnectionInfo {
        /**
         * @param url
         * @param properties
         */
        protected HttpConnectionInfo( String url,
                                      Properties properties ) {
            super(url, properties);

        }

        @Override
        protected void init() {
            // parsing 2 ways of specifying the repository and workspace
            // 1) defined using ?repositoryName
            // 2) defined in the path server:8080/modeshape-rest/respositoryName/workspaceName

            super.init();

            // if the workspace and/or repository name is not specified as a property on the url,
            // then parse the url to obtain the values from the path, the url must be in the format:
            // {hostname}:{port} / {context root} + / respositoryName / workspaceName

            StringBuilder url = new StringBuilder();
            String[] urlsections = repositoryPath.split("/");
            // if there are only 2 sections, then the url can have the workspace or repository name specified in the path
            if (urlsections.length < 3) {
                return;
            }

            // the assignment of url section is working back to front, this is so in cases where
            // the {context} is changed to be made up of multiple sections, instead of the default (modeshape-rest), the
            // workspace should be the last section (if exist) and the repository should be before the
            // workspace.
            int workspacePos = -1;
            int repositoryPos = -1;
            int repoPos = 1;
            if (this.getWorkspaceName() == null && urlsections.length > 3) {
                workspacePos = urlsections.length - 1;
                String workspaceName = urlsections[workspacePos];
                this.setWorkspaceName(workspaceName);
                // if workspace is found, then repository is assume in the prior section
                repoPos = 2;

            }
            if (this.getRepositoryName() == null && urlsections.length > 2) {
                repositoryPos = urlsections.length - repoPos;
                String repositoryName = urlsections[repositoryPos];
                this.setRepositoryName(repositoryName);
            }

            // rebuild the url without the repositoryName or WorkspaceName because
            // the createConnection() needs these separated.
            for (int i = 0; i < repositoryPos; i++) {
                url.append(urlsections[i]);
                if (i < repositoryPos - 1) {
                    url.append("/");
                }
            }

            this.repositoryPath = url.toString();

        }

        @Override
        public String getUrlExample() {
            return HTTP_EXAMPLE_URL;
        }

        @Override
        public String getUrlPrefix() {
            return JcrDriver.HTTP_URL_PREFIX;
        }

        @Override
        protected void addUrlPropertyInfo( List<DriverPropertyInfo> results ) {
            // if the repository path doesn't have at least the {context}
            // example: server:8080/modeshape-rest where modeshape-rest is the context,
            // then the URL is considered invalid.
            if (repositoryPath.indexOf("/") == -1) {
                setUrl(null);
            }
            super.addUrlPropertyInfo(results);
        }

    }

}
