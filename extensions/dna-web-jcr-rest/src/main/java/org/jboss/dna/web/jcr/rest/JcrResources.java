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
package org.jboss.dna.web.jcr.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * RESTEasy handler to provide the JCR resources at the URIs below. Please note that these URIs assume a context of {@code
 * /resources} for the web application.
 * <table border="1">
 * <tr>
 * <th>URI Pattern</th>
 * <th>Description</th>
 * <th>Supported Methods</th>
 * </tr>
 * <tr>
 * <td>/resources</td>
 * <td>returns a list of accessible repositories</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/repositories</td>
 * <td>returns a list of accessible repositories</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}</td>
 * <td>returns a list of accessible workspaces within that repository</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/workspaces</td>
 * <td>returns a list of accessible workspaces within that repository</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}</td>
 * <td>returns a list of operations within the workspace</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/item/{path}</td>
 * <td>accesses the node at the path</td>
 * <td>ALL</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/item/{path}/@{propertyName}</td>
 * <td>accesses the named property at the path</td>
 * <td>ALL (except PUT)</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/item/{path}/@{propertyName}</td>
 * <td>adds the value from the body to the named property at the path</td>
 * <td>PUT</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/uuid/{uuid}</td>
 * <td>accesses the node with the given UUID</td>
 * <td>ALL</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/lock/{path}</td>
 * <td>locks the node at the path</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/lock/{path}</td>
 * <td>unlocks the node at the path</td>
 * <td>PUT</td>
 * </tr>
 * </table>
 */
@Path( "/" )
public class JcrResources {

    /**
     * Returns the list of JCR repositories available on this server
     * @return the list of JCR repositories available on this server
     */
    @GET
    @Path( "/repositories" )
    public String repositories() {
        return "Hello, DNA!";
    }

    /**
     * Returns the list of workspaces available to this user within the named repository.
     * @param repositoryName the name of the repository
     * @return the list of workspaces available to this user within the named repository.
     */
    @GET
    @Path( "/{repositoryName}/workspaces" )
    public String workspaces( @PathParam( "repositoryName" ) String repositoryName ) {
        return repositoryName;
    }

}
