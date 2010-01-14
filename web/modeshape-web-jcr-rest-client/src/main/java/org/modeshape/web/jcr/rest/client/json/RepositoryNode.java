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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.CheckArg;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>RepositoryNode</code> class is responsible for knowing how to create a URL for a repository and to parse a JSON
 * response into {@link Workspace workspace} objects.
 */
public final class RepositoryNode extends JsonNode {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The ModeShape repository.
     */
    private final Repository repository;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param repository the ModeShape repository (never <code>null</code>)
     */
    public RepositoryNode( Repository repository ) {
        super(repository.getName());
        this.repository = repository;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * <p>
     * This URL can be used to obtain the workspaces contained in this repository. The URL will NOT end in '/'.
     * 
     * @see org.modeshape.web.jcr.rest.client.json.JsonNode#getUrl()
     */
    @Override
    public URL getUrl() throws Exception {
        ServerNode serverNode = new ServerNode(this.repository.getServer());
        StringBuilder url = new StringBuilder(serverNode.getUrl().toString());

        // add repository path
        url.append('/').append(JsonUtils.encode(repository.getName()));
        return new URL(url.toString());
    }

    /**
     * @param jsonResponse the HTTP connection JSON response (never <code>null</code>) containing the workspaces
     * @return the workspaces for this repository (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the workspaces
     */
    @SuppressWarnings( "unchecked" )
    public Collection<Workspace> getWorkspaces( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");
        Collection<Workspace> workspaces = new ArrayList<Workspace>();
        JSONObject jsonObj = new JSONObject(jsonResponse);

        // keys are the repository names
        for (Iterator<String> itr = jsonObj.keys(); itr.hasNext();) {
            String name = JsonUtils.decode(itr.next());
            Workspace workspace = new Workspace(name, this.repository);
            workspaces.add(workspace);
        }

        return workspaces;
    }

}
