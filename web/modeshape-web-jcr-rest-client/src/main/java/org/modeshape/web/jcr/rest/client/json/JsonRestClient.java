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
package org.modeshape.web.jcr.rest.client.json;

import java.io.File;
import java.io.FileFilter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.Base64;
import org.modeshape.common.util.Logger;
import org.modeshape.web.jcr.rest.client.IJcrConstants;
import org.modeshape.web.jcr.rest.client.IRestClient;
import org.modeshape.web.jcr.rest.client.RestClientI18n;
import org.modeshape.web.jcr.rest.client.Status;
import org.modeshape.web.jcr.rest.client.Status.Severity;
import org.modeshape.web.jcr.rest.client.domain.QueryRow;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.domain.Workspace;
import org.modeshape.web.jcr.rest.client.http.HttpClientConnection;
import org.modeshape.web.jcr.rest.client.json.IJsonConstants.RequestMethod;

/**
 * The <code>JsonRestClient</code> class is an implementation of <code>IRestClient</code> that works with the ModeShape REST
 * server that uses JSON as its interface protocol.
 */
public final class JsonRestClient implements IRestClient {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(JsonRestClient.class);

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @param server the server where the connection will be established
     * @param url the URL where the connection will be established
     * @param method the request method
     * @return the connection which <strong>MUST</strong> be disconnected
     * @throws Exception if there is a problem establishing the connection
     */
    private HttpClientConnection connect( Server server,
                                          URL url,
                                          RequestMethod method ) throws Exception {
        LOGGER.trace("connect: url={0}, method={1}", url, method);
        return new HttpClientConnection(server, url, method);
    }

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
        LOGGER.trace("createFileNode: workspace={0}, path={1}, file={2}", workspace.getName(), path, file.getAbsolutePath());
        FileNode fileNode = new FileNode(workspace, path, file);
        URL fileNodeUrl = fileNode.getUrl();
        URL fileNodeUrlWithTerseResponse = new URL(fileNodeUrl.toString() + "?mode:includeNode=false");
        HttpClientConnection connection = connect(workspace.getServer(), fileNodeUrlWithTerseResponse, RequestMethod.POST);

        try {
            LOGGER.trace("createFileNode: create node={0}", fileNode);
            connection.write(fileNode.getContent());

            // make sure node was created
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                // node was not created
                LOGGER.error(RestClientI18n.connectionErrorMsg, responseCode, "createFileNode");
                String msg = RestClientI18n.createFileFailedMsg.text(file.getName(), path, workspace.getName(), responseCode);
                throw new RuntimeException(msg);
            }
        } finally {
            if (connection != null) {
                LOGGER.trace("createFileNode: leaving");
                connection.disconnect();
            }
        }
    }

    /**
     * Creates a file node in the specified repository. Note: All parent folders are assumed to already exist.
     * 
     * @param workspace the workspace where the file node is being created
     * @param path the path in the workspace to the folder where the file node is being created
     * @param file the file whose contents will be contained in the file node being created
     * @throws Exception if there is a problem creating the file
     */
    private void updateFileNode( Workspace workspace,
                                 String path,
                                 File file ) throws Exception {
        LOGGER.trace("updateFileNode: workspace={0}, path={1}, file={2}", workspace.getName(), path, file.getAbsolutePath());
        FileNode fileNode = new FileNode(workspace, path, file);
        URL fileNodeUrl = fileNode.getUrl();
        URL fileNodeUrlWithTerseResponse = new URL(fileNodeUrl.toString() + "?mode:includeNode=false");
        HttpClientConnection connection = connect(workspace.getServer(), fileNodeUrlWithTerseResponse, RequestMethod.PUT);

        try {
            LOGGER.trace("updateFileNode: create node={0}", fileNode);
            connection.write(fileNode.getContent());

            // make sure node was created
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                // node was not updated
                LOGGER.error(RestClientI18n.connectionErrorMsg, responseCode, "updateFileNode");
                String msg = RestClientI18n.updateFileFailedMsg.text(file.getName(), path, workspace.getName(), responseCode);
                throw new RuntimeException(msg);
            }
        } finally {
            if (connection != null) {
                LOGGER.trace("updateFileNode: leaving");
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
        LOGGER.trace("createFolderNode: workspace={0}, path={1}", workspace.getName(), path);
        FolderNode folderNode = new FolderNode(workspace, path);

        HttpClientConnection connection = connect(workspace.getServer(), folderNode.getUrl(), RequestMethod.POST);

        try {
            LOGGER.trace("createFolderNode: create node={0}", folderNode);
            connection.write(folderNode.getContent());

            // make sure node was created
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                // node was not created
                LOGGER.error(RestClientI18n.connectionErrorMsg, responseCode, "createFolderNode");
                String msg = RestClientI18n.createFolderFailedMsg.text(path, workspace.getName(), responseCode);
                throw new RuntimeException(msg);
            }
        } finally {
            if (connection != null) {
                LOGGER.trace("createFolderNode: leaving");
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
        LOGGER.trace("ensureFolderExists: workspace={0}, path={1}", workspace.getName(), folderPath);
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
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getRepositories(org.modeshape.web.jcr.rest.client.domain.Server)
     */
    public Collection<Repository> getRepositories( Server server ) throws Exception {
        assert server != null;
        LOGGER.trace("getRepositories: server={0}", server);

        ServerNode serverNode = new ServerNode(server);
        HttpClientConnection connection = connect(server, serverNode.getFindRepositoriesUrl(), RequestMethod.GET);

        try {
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return serverNode.getRepositories(connection.read());
            }

            // not a good response code
            LOGGER.error(RestClientI18n.connectionErrorMsg, responseCode, "getRepositories");
            String msg = RestClientI18n.getRepositoriesFailedMsg.text(server.getName(), responseCode);
            throw new RuntimeException(msg);
        } finally {
            if (connection != null) {
                LOGGER.trace("getRepositories: leaving");
                connection.disconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getNodeTypes(org.modeshape.web.jcr.rest.client.domain.Repository)
     */
    @Override
    public Map<String, javax.jcr.nodetype.NodeType> getNodeTypes( Repository repository ) throws Exception {
        assert repository != null;
        LOGGER.trace("getNodeTypes: workspace={0}", repository);

        // because the http://<url> needs the workspace when it appends the depth option
        // this logic must be used to obtain one.
        Collection<Workspace> workspaces = getWorkspaces(repository);
        Workspace workspace = null;
        Workspace systemWs = null;
        for (Workspace wspace : workspaces) {
            if (wspace.getName().equalsIgnoreCase("default")) {
                workspace = wspace;
                break;
            }
            if (workspace == null && !wspace.getName().equalsIgnoreCase("system")) {
                workspace = wspace;
            }

            if (wspace.getName().equalsIgnoreCase("system")) {
                systemWs = wspace;
            }
        }
        if (workspace == null) {
            workspace = systemWs;
        }

        NodeTypeNode nodetypeNode = new NodeTypeNode(workspace);
        HttpClientConnection connection = connect(workspace.getServer(), nodetypeNode.getUrl(), RequestMethod.GET);

        try {
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return nodetypeNode.getNodeTypes(connection.read());
            }

            // not a good response code
            LOGGER.error(RestClientI18n.connectionErrorMsg, responseCode, "getNodeTypes");
            String msg = RestClientI18n.getNodeTypesFailedMsg.text(nodetypeNode.getUrl(), responseCode);
            throw new RuntimeException(msg);
        } finally {
            if (connection != null) {
                LOGGER.trace("getNodeTypes: leaving");
                connection.disconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getUrl(java.io.File, java.lang.String,
     *      org.modeshape.web.jcr.rest.client.domain.Workspace)
     */
    public URL getUrl( File file,
                       String path,
                       Workspace workspace ) throws Exception {
        assert file != null;
        assert path != null;
        assert workspace != null;

        // can't be a directory
        if (file.isDirectory()) {
            throw new IllegalArgumentException();
        }

        return new FileNode(workspace, path, file).getUrl();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getWorkspaces(org.modeshape.web.jcr.rest.client.domain.Repository)
     */
    public Collection<Workspace> getWorkspaces( Repository repository ) throws Exception {
        assert repository != null;
        LOGGER.trace("getWorkspaces: repository={0}", repository);

        RepositoryNode repositoryNode = new RepositoryNode(repository);
        HttpClientConnection connection = connect(repository.getServer(), repositoryNode.getUrl(), RequestMethod.GET);

        try {
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return repositoryNode.getWorkspaces(connection.read());
            }

            // not a good response code
            LOGGER.error(RestClientI18n.connectionErrorMsg, responseCode, "getWorkspaces");
            String msg = RestClientI18n.getWorkspacesFailedMsg.text(repository.getName(),
                                                                    repository.getServer().getName(),
                                                                    responseCode);
            throw new RuntimeException(msg);
        } finally {
            if (connection != null) {
                LOGGER.trace("getWorkspaces: leaving");
                connection.disconnect();
            }
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
        HttpClientConnection connection = connect(workspace.getServer(), fileNode.getFileContentsUrl(), RequestMethod.GET);
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String result = connection.read();
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
        LOGGER.trace("pathExists: url={0}", url);
        HttpClientConnection connection = connect(server, url, RequestMethod.GET);

        try {
            int responseCode = connection.getResponseCode();
            LOGGER.trace("pathExists: responseCode={0}", responseCode);
            return (responseCode == HttpURLConnection.HTTP_OK);
        } finally {
            if (connection != null) {
                LOGGER.trace("pathExists: leaving");
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
     * @see org.modeshape.web.jcr.rest.client.IRestClient#publish(org.modeshape.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     */
    public Status publish( Workspace workspace,
                           String path,
                           File file ) {
        assert workspace != null;
        assert path != null;
        assert file != null;
        LOGGER.trace("publish: workspace={0}, path={1}, file={2}", workspace.getName(), path, file.getAbsolutePath());

        try {
            if (pathExists(workspace, path, file)) {
                // Update it ...
                updateFileNode(workspace, path, file);
            } else {
                // doesn't exist so make sure the parent path exists
                ensureFolderExists(workspace, path);
                // publish file
                createFileNode(workspace, path, file);
            }

        } catch (Exception e) {
            String msg = RestClientI18n.publishFailedMsg.text(file.getAbsolutePath(), path, workspace.getName());
            return new Status(Severity.ERROR, msg, e);
        }

        return Status.OK_STATUS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#unpublish(org.modeshape.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     */
    public Status unpublish( Workspace workspace,
                             String path,
                             File file ) {
        assert workspace != null;
        assert path != null;
        assert file != null;
        LOGGER.trace("unpublish: workspace={0}, path={1}, file={2}", workspace.getName(), path, file.getAbsolutePath());

        HttpClientConnection connection = null;

        try {
            FileNode fileNode = new FileNode(workspace, path, file);
            connection = connect(workspace.getServer(), fileNode.getUrl(), RequestMethod.DELETE);
            int responseCode = connection.getResponseCode();
            LOGGER.trace("responseCode={0}", responseCode);

            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                // check to see if the file was never published
                if (!pathExists(workspace.getServer(), fileNode.getUrl())) {
                    String msg = RestClientI18n.unpublishNeverPublishedMsg.text(file.getAbsolutePath(), workspace.getName(), path);
                    return new Status(Severity.INFO, msg, null);
                }

                // unexpected result
                LOGGER.error(RestClientI18n.connectionErrorMsg, responseCode, "unpublish");
                String msg = RestClientI18n.unpublishFailedMsg.text(file.getName(), workspace.getName(), path);
                throw new RuntimeException(msg);
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            String msg = RestClientI18n.unpublishFailedMsg.text(file.getAbsolutePath(), workspace.getName(), path);
            return new Status(Severity.ERROR, msg, e);
        } finally {
            if (connection != null) {
                LOGGER.trace("unpublish: leaving");
                connection.disconnect();
            }
        }
    }

    @Override
    public List<QueryRow> query( Workspace workspace,
                                 String language,
                                 String statement ) throws Exception {
        return query(workspace, language, statement, 0, -1, null);
    }

    @Override
    public List<QueryRow> query( Workspace workspace,
                                 String language,
                                 String statement,
                                 int offset,
                                 int limit ) throws Exception {
        return query(workspace, language, statement, 0, -1, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#query(org.modeshape.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.lang.String, int, int, java.util.Map)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public List<QueryRow> query( Workspace workspace,
                                 String language,
                                 String statement,
                                 int offset,
                                 int limit,
                                 Map<String, String> variables ) throws Exception {
        assert workspace != null;
        assert language != null;
        assert statement != null;

        LOGGER.trace("query: workspace={0}, language={1}, file={2}, offset={3}, limit={4}",
                     workspace.getName(),
                     language,
                     statement,
                     offset,
                     limit);

        HttpClientConnection connection = null;

        try {
            WorkspaceNode workspaceNode = new WorkspaceNode(workspace);
            StringBuilder url = new StringBuilder(workspaceNode.getQueryUrl().toString());
            // url.append("/query");

            boolean hasOffset = offset > 0;
            boolean firstQueryParam = true;
            if (hasOffset) {
                url.append("?offset=").append(offset);
                firstQueryParam = false;
            }

            if (limit >= 0) {
                if (hasOffset) {
                    url.append("&");
                } else {
                    url.append("?");
                }

                url.append("limit=").append(limit);
                firstQueryParam = false;
            }

            if (variables != null && !variables.isEmpty()) {
                for (Map.Entry<String, String> varEntry : variables.entrySet()) {
                    String varName = varEntry.getKey();
                    String varValue = varEntry.getValue();
                    if (varName == null || varName.trim().length() == 0) continue;
                    if (varValue == null || varValue.trim().length() == 0) continue;
                    if (firstQueryParam) {
                        firstQueryParam = false;
                        url.append("?");
                    } else {
                        url.append("&");
                    }
                    url.append(varName);
                    url.append('=');
                    url.append(varValue);
                }
            }

            connection = connect(workspace.getServer(), new URL(url.toString()), RequestMethod.POST);
            connection.setContentType(contentTypeFor(language));
            connection.write(statement.getBytes());

            // A query only succeeds if the response is 200 ...
            int responseCode = connection.getResponseCode();
            LOGGER.trace("responseCode={0}", responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // Something other than 200, so fail ...
                String response = connection.read();
                String msg = "Error while executiong {0} query \"{1}\" with offset {2} and limit {3}: {4}";
                LOGGER.debug(msg, language, statement, offset, limit, response);
                throw new RuntimeException(RestClientI18n.invalidQueryMsg.text(response));
            }

            String response = connection.read();
            JSONObject result = new JSONObject(response);
            Map<String, String> columnTypes = new HashMap<String, String>();
            if (result.has("types")) {
                JSONObject types = (JSONObject)result.get("types");

                for (Iterator<String> iter = types.keys(); iter.hasNext();) {
                    String columnName = iter.next();
                    columnTypes.put(columnName, types.getString(columnName));
                }
            }

            Map<String, String> types = Collections.unmodifiableMap(columnTypes);

            JSONArray rows = (JSONArray)result.get("rows");
            List<QueryRow> queryRows = new LinkedList<QueryRow>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = (JSONObject)rows.get(i);
                Map<String, Object> values = new HashMap<String, Object>();

                for (Iterator<String> valueIter = row.keys(); valueIter.hasNext();) {
                    String valueName = valueIter.next();
                    if (valueName.endsWith(IJsonConstants.BASE64_SUFFIX)) {
                        byte[] data = Base64.decode(row.getString(valueName));
                        valueName = valueName.substring(0, valueName.length() - IJsonConstants.BASE64_SUFFIX.length());
                        values.put(valueName, data);
                    } else {
                        values.put(valueName, row.getString(valueName));
                    }
                }

                queryRows.add(new QueryRow(types, values));
            }

            return queryRows;
        } finally {
            if (connection != null) {
                LOGGER.trace("query: leaving");
                connection.disconnect();
            }
        }
    }

    private String contentTypeFor( String language ) throws Exception {
        if (IJcrConstants.XPATH.equalsIgnoreCase(language)) {
            return "application/jcr+xpath";
        }
        if (IJcrConstants.JCR_SQL.equalsIgnoreCase(language)) {
            return "application/jcr+sql";
        }
        if (IJcrConstants.JCR_SQL2.equalsIgnoreCase(language)) {
            return "application/jcr+sql2";
        }
        if (IJcrConstants.JCR_SEARCH.equalsIgnoreCase(language)) {
            return "application/jcr+search";
        }

        throw new IllegalStateException(
                                        RestClientI18n.invalidQueryLanguageMsg.text(language, IJcrConstants.VALID_QUERY_LANGUAGES));
    }

    private static final String SERVER_PARM = "--server";
    private static final String REPO_PARM = "--repo";
    private static final String WORKSPACENAME_PARM = "--workspacename";
    private static final String WORKSPACEPATH_PARM = "--workspacepath";
    private static final String FILE_PARM = "--file";
    private static final String DIR_PARM = "--dir";
    private static final String USERNAME_PARM = "--username";
    private static final String PWD_PARM = "--pwd";
    private static final String UNPUBLISH = "--unpublish";
    private static final String HELP_PARM = "--help";

    /*
     *  The main method enables the scripting of publishing artifacts.
     */
    @SuppressWarnings( "null" )
    public static void main( String[] args ) {

        if (args == null || args.length == 0 || args[0].equals(HELP_PARM)) {
            System.out.println("Running the ModeShape Rest Client");
            System.out.println("	required arguments are:");
            System.out.println("  	 	" + SERVER_PARM);
            System.out.println("  	 	" + FILE_PARM + " or " + DIR_PARM);
            System.out.println("  		" + WORKSPACEPATH_PARM);
            System.out.println("  		" + REPO_PARM);
            System.out.println("	optional arguments are:");
            System.out.println("  	 	" + WORKSPACENAME_PARM + " (default=default)");
            System.out.println(" 		" + USERNAME_PARM + "(default=admin");
            System.out.println("  	 	" + PWD_PARM + " (default=admin");
            System.out.println("  	 	" + UNPUBLISH + " with no parameter, will remove file(s)");

            System.exit(0);
        }

        String server_name = null;
        String workspace_name = "default";
        String workspace_path = null;
        String file_name = null;
        String dir_loc = null;
        String user = "admin";
        String pwd = "admin";
        String repo_name = null;

        boolean publish = true;

        int pos = 0;
        for (String arg : args) {
            arg = arg.trim();
            if (arg.equals(SERVER_PARM)) {
                server_name = args[pos + 1];
            } else if (arg.equals(REPO_PARM)) {
                repo_name = args[pos + 1];
            } else if (arg.equals(WORKSPACENAME_PARM)) {
                workspace_name = args[pos + 1];
            } else if (arg.equals(WORKSPACEPATH_PARM)) {
                workspace_path = args[pos + 1];
            } else if (arg.equals(FILE_PARM)) {
                file_name = args[pos + 1];
            } else if (arg.equals(DIR_PARM)) {
                dir_loc = args[pos + 1];
            } else if (arg.equals(USERNAME_PARM)) {
                user = args[pos + 1];
            } else if (arg.equals(PWD_PARM)) {
                pwd = args[pos + 1];
            } else if (arg.equals(UNPUBLISH)) {
                publish = false;
            }

            ++pos;
        }

        String errparm = null;
        if (server_name == null) {
            errparm = SERVER_PARM;
        } else if (repo_name == null) {
            errparm = REPO_PARM;
        } else if (file_name == null && dir_loc == null) {
            errparm = "[" + FILE_PARM + " | " + DIR_PARM + "]";
        } else if (user == null) {
            errparm = USERNAME_PARM;
        } else if (pwd == null) {
            errparm = PWD_PARM;
        } else if (workspace_path == null) {
            errparm = WORKSPACEPATH_PARM;
        }

        if (errparm != null) {
            LOGGER.error(RestClientI18n.nullArgumentMsg, errparm);
            System.exit(-1);
        }

        Server server = new Server(server_name, user, pwd);
        Repository repository = new Repository(repo_name, server);
        Workspace workspace = new Workspace(workspace_name, repository);

        JsonRestClient client = new JsonRestClient();

        try {
            client.getRepositories(server);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (publish) {

            if (file_name != null) {
                Status status = client.publish(workspace, workspace_path, new File(file_name));
                if (checkStatus(status)) {
                    LOGGER.info(RestClientI18n.publishSucceededMsg, file_name, workspace_path, workspace_name);
                }

            } else {
                File[] files = findAllFilesInDirectory(dir_loc);
                for (File f : files) {
                    Status status = client.publish(workspace, workspace_path, f);
                    if (checkStatus(status)) {
                        LOGGER.info(RestClientI18n.publishSucceededMsg, f.getName(), workspace_path, workspace_name);
                    }
                }
            }
        } else {
            if (file_name != null) {
                Status status = client.unpublish(workspace, workspace_path, new File(file_name));
                if (checkStatus(status)) {
                    LOGGER.info(RestClientI18n.unpublishSucceededMsg, file_name, workspace_path, workspace_name);
                }

            } else {
                File[] files = findAllFilesInDirectory(dir_loc);
                for (File f : files) {
                    Status status = client.unpublish(workspace, workspace_path, f);
                    if (checkStatus(status)) {
                        LOGGER.info(RestClientI18n.unpublishSucceededMsg, f.getName(), workspace_path, workspace_name);
                    }
                }
            }
        }

    }

    private static boolean checkStatus( Status status ) {
        if (status.isError()) {
            System.err.println(status.getMessage());
            status.getException().printStackTrace(System.err);
            return false;
        }
        return true;
    }

    /**
     * Returns a <code>File</code> array that will contain all the files that exist in the directory
     * 
     * @param dir the path that contains the files to be published
     * @return File[] of files in the directory
     */
    private static File[] findAllFilesInDirectory( String dir ) {

        // Find all files in the specified directory
        File modelsDirFile = new File(dir);
        FileFilter fileFilter = new FileFilter() {

            public boolean accept( File file ) {
                if (file.isDirectory()) {
                    return false;
                }

                String fileName = file.getName();

                if (fileName == null || fileName.length() == 0) {
                    return false;
                }

                return true;

            }
        };

        File[] modelFiles = modelsDirFile.listFiles(fileFilter);

        return modelFiles;

    }

}
