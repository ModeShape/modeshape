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

    private static final long serialVersionUID = 1L;

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
