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
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>WorkspaceNode</code> class is responsible for knowing how to create a URL for a {@link Workspace workspace}. The URL
 * can be used to publish and unpublish resources.
 */
public class WorkspaceNode extends JsonNode {

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
}
