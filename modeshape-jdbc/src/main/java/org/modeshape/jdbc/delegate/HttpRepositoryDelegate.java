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

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryResult;
import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JdbcLocalI18n;
import org.modeshape.jdbc.LocalJcrDriver.JcrContextFactory;
import org.modeshape.jdbc.rest.ModeShapeRestClient;
import org.modeshape.jdbc.rest.NodeTypes;
import org.modeshape.jdbc.rest.Repositories;

/**
 * The HTTPRepositoryDelegate provides remote Repository implementation to access the Jcr layer via HTTP lookup.
 */
public class HttpRepositoryDelegate extends AbstractRepositoryDelegate {

    protected static final int PROTOCOL_HTTP = 2;

    public static final RepositoryDelegateFactory FACTORY = new RepositoryDelegateFactory() {

        @Override
        protected int determineProtocol( String url ) {
            if (url.startsWith(JcrDriver.HTTP_URL_PREFIX) && url.length() > JcrDriver.HTTP_URL_PREFIX.length()) {
                // This fits the pattern so far ...
                return PROTOCOL_HTTP;
            }
            return super.determineProtocol(url);
        }

        @Override
        protected RepositoryDelegate create( int protocol,
                                             String url,
                                             Properties info,
                                             JcrContextFactory contextFactory ) {
            if (protocol == PROTOCOL_HTTP) {
                return new HttpRepositoryDelegate(url, info);
            }
            return super.create(protocol, url, info, contextFactory);
        }
    };

    private static final String HTTP_EXAMPLE_URL = JcrDriver.HTTP_URL_PREFIX + "{hostname}:{port}/{context root}";
    private AtomicReference<Map<String, NodeType>> nodeTypes = new AtomicReference<>();
    private AtomicReference<Repositories.Repository> repository = new AtomicReference<>();
    private ModeShapeRestClient restClient;

    protected HttpRepositoryDelegate( String url,
                                      Properties info ) {
        super(url, info);
    }

    @Override
    protected ConnectionInfo createConnectionInfo( String url,
                                                   Properties info ) {
        return new HttpConnectionInfo(url, info);
    }

    protected Repositories.Repository repository() {
        return this.repository.get();
    }

    @Override
    public QueryResult execute( String query,
                                String language ) throws RepositoryException {
        logger.trace("Executing query: {0}", query);
        try {
            org.modeshape.jdbc.rest.QueryResult result = this.restClient.query(query, language);
            return new HttpQueryResult(result);
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public String explain( String query,
                           String language ) throws RepositoryException {
        logger.trace("Explaining query: {0}", query);
        try {
            return this.restClient.queryPlan(query, language);
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public String getDescriptor( String descriptorKey ) {
        return repository() != null ? repository().getMetadata().get(descriptorKey).toString() : "";
    }

    @Override
    public NodeType nodeType( String name ) throws RepositoryException {
        if (nodeTypes.get() == null) {
            // load the node types
            nodeTypes();
        }

        NodeType nodetype = nodeTypes.get().get(name);
        if (nodetype == null) {
            throw new RepositoryException(JdbcLocalI18n.unableToGetNodeType.text(name));
        }

        return nodetype;
    }

    @Override
    public Collection<NodeType> nodeTypes() throws RepositoryException {
        Map<String, NodeType> nodeTypes = this.nodeTypes.get();
        if (nodeTypes == null) {
            NodeTypes restNodeTypes = this.restClient.getNodeTypes();
            if (restNodeTypes.isEmpty()) {
                throw new RepositoryException(JdbcLocalI18n.noNodeTypesReturned.text(restClient.serverUrl()));
            }
            nodeTypes = new HashMap<>();
            for (org.modeshape.jdbc.rest.NodeType nodeType : restNodeTypes) {
                nodeTypes.put(nodeType.getName(), nodeType);
            }
            this.nodeTypes.compareAndSet(null, nodeTypes);
        }
        return this.nodeTypes.get().values();
    }

    @Override
    protected void initRepository() throws SQLException {
        if (repository() != null) {
            return;
        }
        logger.debug("Creating repository for HttpRepositoryDelegate");

        ConnectionInfo info = getConnectionInfo();
        assert info != null;

        String path = info.getRepositoryPath();
        if (path == null) {
            throw new SQLException("Missing repo path from " + info.getUrl());
        }
        String username = info.getUsername();
        if (username == null) {
            throw new SQLException("Missing username from " + info.getUrl());
        }
        char[] password = info.getPassword();
        if (password == null) {
            throw new SQLException("Missing password path from " + info.getUrl());
        }

        String repositoryName = info.getRepositoryName();
        if (repositoryName == null) {
            throw new SQLException("Missing repository name from " + info.getUrl());
        }

        String serverUrl = "http://" + path + "/" + repositoryName;

        String workspaceName = info.getWorkspaceName();
        if (workspaceName == null) {
            // there is no WS info, so try to figure out a default one...
            ModeShapeRestClient client = new ModeShapeRestClient(serverUrl, username, String.valueOf(password));
            List<String> allWorkspaces = client.getWorkspaces(repositoryName).getWorkspaces();
            if (allWorkspaces.isEmpty()) {
                throw new SQLException("No workspaces found for the " + repositoryName + " repository");
            }
            // TODO author=Horia Chiorean date=19-Aug-14 description=There is no way to get the "default" ws so we'll choose one
            workspaceName = allWorkspaces.get(0);
        }

        serverUrl = serverUrl + "/" + workspaceName;
        logger.debug("Using server url: {0}", serverUrl);
        // this is only a connection test to confirm a connection can be made and results can be obtained.
        try {
            this.restClient = new ModeShapeRestClient(serverUrl, username, String.valueOf(password));
            Repositories repositories = this.restClient.getRepositories();
            this.setRepositoryNames(repositories.getRepositoryNames());
            Repositories.Repository repository = repositories.getRepository(repositoryName);
            if (repository == null) {
                throw new SQLException(JdbcLocalI18n.unableToFindNamedRepository.text(path, repositoryName));
            }
            this.repository.compareAndSet(null, repository);
        } catch (Exception e) {
            throw new SQLException(JdbcLocalI18n.noRepositoryNamesFound.text(), e);
        }
    }

    @Override
    public boolean isValid( final int timeout ) {
        try {
            this.restClient.getWorkspaces(getConnectionInfo().getRepositoryName());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public void close() {
        super.close();
        restClient = null;
        nodeTypes.set(null);
        repository.set(null);
    }

    private class HttpConnectionInfo extends ConnectionInfo {

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
            if (!repositoryPath.contains("/")) {
                setUrl(null);
            }
            super.addUrlPropertyInfo(results);
        }
    }
}
