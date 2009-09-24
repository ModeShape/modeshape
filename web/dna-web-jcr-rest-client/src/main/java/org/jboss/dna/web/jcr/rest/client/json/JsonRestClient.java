/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client.json;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import javax.ws.rs.core.MediaType;
import org.jboss.dna.common.util.Base64;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.web.jcr.rest.client.IRestClient;
import org.jboss.dna.web.jcr.rest.client.RestClientI18n;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;
import org.jboss.dna.web.jcr.rest.client.json.IJsonConstants.RequestMethod;

/**
 * The <code>JsonRestClient</code> class is an implementation of <code>IRestClient</code> that works with the DNA REST server that
 * uses JSON as its interface protocol.
 */
public final class JsonRestClient implements IRestClient {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(JsonRestClient.class);

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * Creates a file node in the specified repository. Note: All parent folders are assumed to already exist.
     * 
     * @param workspace the workspace where the file node is being created
     * @param path the path in the workspace to the folder where the file node is being created
     * @param file the file whose contents will be contained in the file node being created
     * @throws Exception if there is a problem creating the file
     */
    private void createFileNode( Workspace workspace,
                                 String path,
                                 File file ) throws Exception {
        this.logger.trace("createFileNode: workspace={0}, path={1}, file={2}", workspace.getName(), path, file.getAbsolutePath()); //$NON-NLS-1$
        FileNode fileNode = new FileNode(workspace, path, file);
        HttpURLConnection connection = openConnection(workspace.getServer(), fileNode.getUrl(), RequestMethod.POST);

        try {
            this.logger.trace("createFileNode: create node={0}", fileNode); //$NON-NLS-1$
            connection.getOutputStream().write(fileNode.getContent());

            // make sure node was created
            if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                // node was not created
                this.logger.error(RestClientI18n.connectionErrorMsg, connection.getResponseCode(), "createFileNode"); //$NON-NLS-1$
                String msg = RestClientI18n.createFileFailedMsg.text(file.getName(),
                                                                     path,
                                                                     workspace.getName(),
                                                                     connection.getResponseCode());
                throw new RuntimeException(msg);
            }
        } finally {
            if (connection != null) {
                this.logger.trace("createFileNode: leaving"); //$NON-NLS-1$
                connection.disconnect();
            }
        }
    }

    /**
     * Creates a folder node in the specified workspace. Note: All parent folders are assumed to already exist.
     * 
     * @param workspace the workspace where the folder node is being created
     * @param path the folder path in the workspace
     * @throws Exception if there is a problem creating the folder
     */
    private void createFolderNode( Workspace workspace,
                                   String path ) throws Exception {
        this.logger.trace("createFolderNode: workspace={0}, path={1}", workspace.getName(), path); //$NON-NLS-1$
        FolderNode folderNode = new FolderNode(workspace, path);
        HttpURLConnection connection = openConnection(workspace.getServer(), folderNode.getUrl(), RequestMethod.POST);

        try {
            this.logger.trace("createFolderNode: create node={0}", folderNode); //$NON-NLS-1$
            connection.getOutputStream().write(folderNode.getContent());

            // make sure node was created
            if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                // node was not created
                this.logger.error(RestClientI18n.connectionErrorMsg, connection.getResponseCode(), "createFolderNode"); //$NON-NLS-1$
                String msg = RestClientI18n.createFolderFailedMsg.text(path, workspace.getName(), connection.getResponseCode());
                throw new RuntimeException(msg);
            }
        } finally {
            if (connection != null) {
                this.logger.trace("createFolderNode: leaving"); //$NON-NLS-1$
                connection.disconnect();
            }
        }
    }

    /**
     * Ensures the specified path exists in the specified workspace. The path must only contain folder names.
     * 
     * @param workspace the workspace being checked
     * @param folderPath the path being checked
     * @throws Exception if there is a problem ensuring the folder exists
     */
    private void ensureFolderExists( Workspace workspace,
                                     String folderPath ) throws Exception {
        this.logger.trace("ensureFolderExists: workspace={0}, path={1}", workspace.getName(), folderPath); //$NON-NLS-1$
        FolderNode folderNode = new FolderNode(workspace, folderPath);

        if (!pathExists(workspace.getServer(), folderNode.getUrl())) {
            StringBuilder path = new StringBuilder();

            for (char c : folderPath.toCharArray()) {
                if (c == '/') {
                    if (path.length() > 1) {
                        folderNode = new FolderNode(workspace, path.toString());

                        if (!pathExists(workspace.getServer(), folderNode.getUrl())) {
                            createFolderNode(workspace, folderNode.getPath());
                        }
                    }

                    path.append(c);
                } else {
                    path.append(c);

                    if (path.length() == folderPath.length()) {
                        folderNode = new FolderNode(workspace, path.toString());

                        if (!pathExists(workspace.getServer(), folderNode.getUrl())) {
                            createFolderNode(workspace, folderNode.getPath());
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#getRepositories(org.jboss.dna.web.jcr.rest.client.domain.Server)
     */
    public Collection<Repository> getRepositories( Server server ) throws Exception {
        CheckArg.isNotNull(server, "server"); //$NON-NLS-1$
        this.logger.trace("getRepositories: server={0}", server); //$NON-NLS-1$

        ServerNode serverNode = new ServerNode(server);
        HttpURLConnection connection = openConnection(server, serverNode.getFindRepositoriesUrl(), RequestMethod.GET);

        try {
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return serverNode.getRepositories(JsonUtils.readInputStream(connection));
            }

            // not a good response code
            this.logger.error(RestClientI18n.connectionErrorMsg, connection.getResponseCode(), "getRepositories"); //$NON-NLS-1$
            String msg = RestClientI18n.getRepositoriesFailedMsg.text(server.getName(), connection.getResponseCode());
            throw new RuntimeException(msg);
        } finally {
            if (connection != null) {
                this.logger.trace("getRepositories: leaving"); //$NON-NLS-1$
                connection.disconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#getUrl(java.io.File, java.lang.String,
     *      org.jboss.dna.web.jcr.rest.client.domain.Workspace)
     */
    public URL getUrl( File file,
                       String path,
                       Workspace workspace ) throws Exception {
        CheckArg.isNotNull(file, "file"); //$NON-NLS-1$
        CheckArg.isNotNull(path, "path"); //$NON-NLS-1$
        CheckArg.isNotNull(workspace, "workspace"); //$NON-NLS-1$

        // can't be a directory
        if (file.isDirectory()) {
            throw new IllegalArgumentException();
        }

        return new FileNode(workspace, path, file).getUrl();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#getWorkspaces(org.jboss.dna.web.jcr.rest.client.domain.Repository)
     */
    public Collection<Workspace> getWorkspaces( Repository repository ) throws Exception {
        CheckArg.isNotNull(repository, "repository"); //$NON-NLS-1$
        this.logger.trace("getWorkspaces: repository={0}", repository); //$NON-NLS-1$

        RepositoryNode repositoryNode = new RepositoryNode(repository);
        HttpURLConnection connection = openConnection(repository.getServer(), repositoryNode.getUrl(), RequestMethod.GET);

        try {
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return repositoryNode.getWorkspaces(JsonUtils.readInputStream(connection));
            }

            // not a good response code
            this.logger.error(RestClientI18n.connectionErrorMsg, connection.getResponseCode(), "getWorkspaces"); //$NON-NLS-1$
            String msg = RestClientI18n.getWorkspacesFailedMsg.text(repository.getName(),
                                                                    repository.getServer().getName(),
                                                                    connection.getResponseCode());
            throw new RuntimeException(msg);
        } finally {
            if (connection != null) {
                this.logger.trace("getWorkspaces: leaving"); //$NON-NLS-1$
                connection.disconnect();
            }
        }
    }

    /**
     * @param server the server where the connection will be established
     * @param url the URL where the connection will be established
     * @param method the request method
     * @return the open connection which <strong>MUST</strong> be disconnected
     * @throws Exception if there is a problem establishing the connection
     */
    private HttpURLConnection openConnection( Server server,
                                              URL url,
                                              RequestMethod method ) throws Exception {
        this.logger.trace("openConnection: url={0}, method={1}", url, method); //$NON-NLS-1$

        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoOutput(true);
            String encoding = Base64.encodeBytes((server.getUser() + ':' + server.getPassword()).getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoding); //$NON-NLS-1$ //$NON-NLS-2$
            connection.setRequestMethod(method.toString());
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON); //$NON-NLS-1$
            connection.setUseCaches(false);
            return connection;
        } catch (Exception e) {
            this.logger.trace("openConnection: exception:", e); //$NON-NLS-1$
            throw e;
        }
    }

    /**
     * Note: Currently used for testing only.
     * 
     * @param workspace the workspace where the file is published
     * @param path the path in the workspace where the file is published
     * @param file the file whose workspace contents are being requested
     * @return the base 64 encoded file contents or <code>null</code> if file is not found
     * @throws Exception if there is a problem obtaining the contents
     */
    String getFileContents( Workspace workspace,
                            String path,
                            File file ) throws Exception {
        FileNode fileNode = new FileNode(workspace, path, file);
        HttpURLConnection connection = openConnection(workspace.getServer(), fileNode.getFileContentsUrl(), RequestMethod.GET);
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String result = JsonUtils.readInputStream(connection);
            return fileNode.getFileContents(result);
        }

        return null;
    }

    /**
     * @param server the server where the URL will be used (never <code>null</code>)
     * @param url the path being checked (never <code>null</code>)
     * @return <code>true</code> if the path exists
     * @throws Exception if there is a problem checking the existence of the path
     */
    private boolean pathExists( Server server,
                                URL url ) throws Exception {
        this.logger.trace("pathExists: url={0}", url); //$NON-NLS-1$
        HttpURLConnection connection = openConnection(server, url, RequestMethod.GET);

        try {
            int responseCode = connection.getResponseCode();
            this.logger.trace("pathExists: responseCode={0}", responseCode); //$NON-NLS-1$
            return (responseCode == HttpURLConnection.HTTP_OK);
        } finally {
            if (connection != null) {
                this.logger.trace("pathExists: leaving"); //$NON-NLS-1$
                connection.disconnect();
            }
        }
    }

    /**
     * @param workspace the workspace being checked (never <code>null</code>)
     * @param path the path in workspace (never <code>null</code>)
     * @param file the file being checked (never <code>null</code>)
     * @return <code>true</code> if the file exists in the workspace at the specified path
     * @throws Exception if there is a problem checking the existence of the file
     */
    public boolean pathExists( Workspace workspace,
                               String path,
                               File file ) throws Exception {
        FileNode fileNode = new FileNode(workspace, path, file);
        return pathExists(workspace.getServer(), fileNode.getUrl());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#publish(org.jboss.dna.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     */
    public Status publish( Workspace workspace,
                           String path,
                           File file ) {
        CheckArg.isNotNull(workspace, "workspace"); //$NON-NLS-1$
        CheckArg.isNotNull(path, "path"); //$NON-NLS-1$
        CheckArg.isNotNull(file, "file"); //$NON-NLS-1$
        this.logger.trace("publish: workspace={0}, path={1}, file={2}", workspace.getName(), path, file.getAbsolutePath()); //$NON-NLS-1$

        try {
            // first delete if file exists at that path
            if (pathExists(workspace, path, file)) {
                unpublish(workspace, path, file);
            } else {
                // doesn't exist so make sure the parent path exists
                ensureFolderExists(workspace, path);
            }

            // publish file
            createFileNode(workspace, path, file);
        } catch (Exception e) {
            String msg = RestClientI18n.publishFailedMsg.text(file.getAbsolutePath(), path, workspace.getName());
            return new Status(Severity.ERROR, msg, e);
        }

        return Status.OK_STATUS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#unpublish(org.jboss.dna.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     */
    public Status unpublish( Workspace workspace,
                             String path,
                             File file ) {
        CheckArg.isNotNull(workspace, "workspace"); //$NON-NLS-1$
        CheckArg.isNotNull(path, "path"); //$NON-NLS-1$
        CheckArg.isNotNull(file, "file"); //$NON-NLS-1$
        this.logger.trace("unpublish: workspace={0}, path={1}, file={2}", workspace.getName(), path, file.getAbsolutePath()); //$NON-NLS-1$

        HttpURLConnection connection = null;

        try {
            FileNode fileNode = new FileNode(workspace, path, file);
            connection = openConnection(workspace.getServer(), fileNode.getUrl(), RequestMethod.DELETE);
            int responseCode = connection.getResponseCode();
            this.logger.trace("responseCode={0}", responseCode); //$NON-NLS-1$

            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                // check to see if the file was never published
                if (!pathExists(workspace.getServer(), fileNode.getUrl())) {
                    String msg = RestClientI18n.unpublishNeverPublishedMsg.text(file.getAbsolutePath(), workspace.getName(), path);
                    return new Status(Severity.INFO, msg, null);
                }

                // unexpected result
                this.logger.error(RestClientI18n.connectionErrorMsg, responseCode, "unpublish"); //$NON-NLS-1$
                String msg = RestClientI18n.unpublishFailedMsg.text(file.getName(), workspace.getName(), path);
                throw new RuntimeException(msg);
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            String msg = RestClientI18n.unpublishFailedMsg.text(file.getAbsolutePath(), workspace.getName(), path);
            return new Status(Severity.ERROR, msg, e);
        } finally {
            if (connection != null) {
                this.logger.trace("unpublish: leaving"); //$NON-NLS-1$
                connection.disconnect();
            }
        }
    }

}
