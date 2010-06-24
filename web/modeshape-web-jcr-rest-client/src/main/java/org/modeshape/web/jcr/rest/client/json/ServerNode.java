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

import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * The <code>ServerNode</code> class is responsible for knowing how to create a URL for a server, create a URL to obtain a
 * server's repositories, and parse a JSON response into {@link Repository repository} objects.
 */
public final class ServerNode extends JsonNode {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(ServerNode.class);

    /**
     * The server containing ModeShape repositories.
     */
    private final Server server;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param server the server containing the repositories (never <code>null</code>)
     */
    public ServerNode( Server server ) {
        super(server.getName());
        this.server = server;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * <p>
     * The URL will NOT end in '/'.
     * 
     * @see org.modeshape.web.jcr.rest.client.json.JsonNode#getUrl()
     */
    @Override
    public URL getUrl() throws Exception {
        StringBuilder url = new StringBuilder(this.server.getUrl());

        // strip off last '/' if necessary
        if (url.lastIndexOf("/") == (url.length() - 1)) {
            url.delete((url.length() - 1), (url.length() - 1));
        }
        return new URL(url.toString());
    }

    /**
     * @return the URL to use when requesting the repositories (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the repositories
     */
    public URL getFindRepositoriesUrl() throws Exception {
        return new URL(getUrl().toString() + '/');
    }

    /**
     * @param jsonResponse the HTTP connection JSON response (never <code>null</code>) containing the repositories
     * @return the repositories found in the JSON response (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the repositories
     */
    @SuppressWarnings( "unchecked" )
    public Collection<Repository> getRepositories( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");
        Collection<Repository> repositories = new ArrayList<Repository>();
        LOGGER.trace("getRepositories:jsonResponse={0}", jsonResponse);
        JSONObject jsonObj = new JSONObject(jsonResponse);

        // keys are the repository names
        for (Iterator<String> itr = jsonObj.keys(); itr.hasNext();) {
            String name = JsonUtils.decode(itr.next());
            Repository repository = new Repository(name, this.server);
            repositories.add(repository);
            LOGGER.trace("getRepositories: adding repository={0}", repository);
        }

        return repositories;
    }

}
