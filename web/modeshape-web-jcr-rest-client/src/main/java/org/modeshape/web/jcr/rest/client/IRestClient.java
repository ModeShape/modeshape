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
package org.modeshape.web.jcr.rest.client;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.modeshape.web.jcr.rest.client.Status.Severity;
import org.modeshape.web.jcr.rest.client.domain.QueryRow;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>IRestClient</code> interface is the API for all REST clients used by the Eclipse ModeShape plugin.
 */
public interface IRestClient {

    /**
     * Obtains the ModeShape repositories defined within the specified server.
     * 
     * @param server the server whose repositories are being requested (never <code>null</code>)
     * @return the repositories within the specified server (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the repositories
     */
    Collection<Repository> getRepositories( Server server ) throws Exception;

    /**
     * Obtains the ModeShape node types defined within the specified workspace.
     * 
     * @param repository for whose node types are being requested (never <code>null</code>)
     * @return the node types defined within the specified workspace (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the node types
     */
    Map<String, javax.jcr.nodetype.NodeType> getNodeTypes( Repository repository ) throws Exception;

    /**
     * @param file the file whose URL is being requested (never <code>null</code>)
     * @param path the path in the ModeShape workspace where the file is/could be located (never <code>null</code>)
     * @param workspace the workspace where the file is/could be located (never <code>null</code>)
     * @return the workspace URL for the specified file (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the URL or if the file is a directory
     */
    URL getUrl( File file,
                String path,
                Workspace workspace ) throws Exception;

    /**
     * Obtains the workspaces defined within the specified ModeShape respository.
     * 
     * @param repository the repository whose workspaces are being requested (never <code>null</code>)
     * @return the workspaces within the specified repository (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the workspaces
     */
    Collection<Workspace> getWorkspaces( Repository repository ) throws Exception;

    /**
     * Publishes, or uploads, a local file to the workspace at the specified path.
     * 
     * @param workspace the workspace where the resource will be published (never <code>null</code>)
     * @param path the unencoded path to the folder where the file will be published (never <code>null</code>)
     * @param file the resource being published (never <code>null</code>)
     * @return a status of the publishing operation outcome (never <code>null</code>)
     */
    Status publish( Workspace workspace,
                    String path,
                    File file );

    /**
     * Unpublishes, or deletes, the resource at the specified path in the workspace. If a file being unpublished is not found in
     * the workspace an {@link Severity#INFO info status} is returned.
     * 
     * @param workspace the workspace where the resource will be unpublished (never <code>null</code>)
     * @param path the unencoded path to the folder where the file is published (never <code>null</code>)
     * @param file the file being unpublished (never <code>null</code>)
     * @return a status of the unpublishing operation outcome (never <code>null</code>)
     */
    Status unpublish( Workspace workspace,
                      String path,
                      File file );

    /**
     * Executes the given query in the workspace.
     * 
     * @param workspace the workspace where the resource will be unpublished (never <code>null</code>)
     * @param language the JCR query language to use (never <code>null</code>)
     * @param statement the query itself (never <code>null</code>)
     * @return the list of rows returned by the query (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the workspaces
     */
    List<QueryRow> query( Workspace workspace,
                          String language,
                          String statement ) throws Exception;

    /**
     * Executes the given query in the workspace.
     * 
     * @param workspace the workspace where the resource will be unpublished (never <code>null</code>)
     * @param language the JCR query language to use (never <code>null</code>)
     * @param statement the query itself (never <code>null</code>)
     * @param offset the first row to be returned; if this value is negative, rows are returned starting with the first row
     * @param limit the maximum number of rows to be returned; if this value is negative, all rows are returned
     * @return the list of rows returned by the query (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the workspaces
     */
    List<QueryRow> query( Workspace workspace,
                          String language,
                          String statement,
                          int offset,
                          int limit ) throws Exception;

    /**
     * Executes the given query in the workspace.
     * 
     * @param workspace the workspace where the resource will be unpublished (never <code>null</code>)
     * @param language the JCR query language to use (never <code>null</code>)
     * @param statement the query itself (never <code>null</code>)
     * @param offset the first row to be returned; if this value is negative, rows are returned starting with the first row
     * @param limit the maximum number of rows to be returned; if this value is negative, all rows are returned
     * @param variables the query variables; may be null
     * @return the list of rows returned by the query (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the workspaces
     */
    List<QueryRow> query( Workspace workspace,
                          String language,
                          String statement,
                          int offset,
                          int limit,
                          Map<String, String> variables ) throws Exception;
}
