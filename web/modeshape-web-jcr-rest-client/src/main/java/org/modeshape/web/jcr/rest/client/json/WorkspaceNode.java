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
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>WorkspaceNode</code> class is responsible for knowing how to create a URL for a {@link Workspace workspace}. The URL
 * can be used to publish and unpublish resources.
 */
public class WorkspaceNode extends JsonNode {

    private static final long serialVersionUID = 1L;

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    private final Workspace workspace;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param workspace the repository workspace (never <code>null</code>)
     */
    public WorkspaceNode( Workspace workspace ) {
        super(workspace.getName());
        this.workspace = workspace;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.json.JsonNode#getUrl()
     */
    @Override
    public URL getUrl() throws Exception {
        RepositoryNode repositoryNode = new RepositoryNode(this.workspace.getRepository());
        StringBuilder url = new StringBuilder(repositoryNode.getUrl().toString());

        // add workspace path
        url.append('/').append(JsonUtils.encode(workspace.getName())).append(IJsonConstants.WORKSPACE_CONTEXT);
        return new URL(url.toString());
    }

    public URL getQueryUrl() throws Exception {
        RepositoryNode repositoryNode = new RepositoryNode(this.workspace.getRepository());
        StringBuilder url = new StringBuilder(repositoryNode.getUrl().toString());

        // add workspace path
        url.append('/').append(JsonUtils.encode(workspace.getName())).append(IJsonConstants.QUERY_CONTEXT);
        return new URL(url.toString());

    }

    public URL getQueryPlanUrl() throws Exception {
        RepositoryNode repositoryNode = new RepositoryNode(this.workspace.getRepository());
        StringBuilder url = new StringBuilder(repositoryNode.getUrl().toString());

        // add workspace path
        url.append('/').append(JsonUtils.encode(workspace.getName())).append(IJsonConstants.QUERY_PLAN_CONTEXT);
        return new URL(url.toString());

    }
}
