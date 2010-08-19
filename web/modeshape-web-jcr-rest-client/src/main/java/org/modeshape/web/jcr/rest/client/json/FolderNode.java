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
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.web.jcr.rest.client.IJcrConstants;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>FolderNode</code> class is responsible for knowing how to create a URL for a folder, create a JSON representation of
 * a folder, and create the appropriate JCR nodes for a folder.
 */
@Immutable
public final class FolderNode extends JsonNode {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The workspace where the file is being published.
     */
    private final Workspace workspace;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param workspace the workspace being used (never <code>null</code>)
     * @param fullPath the full path of the folder within the workspace (never <code>null</code>)
     * @throws Exception if there is a problem creating the folder node
     */
    public FolderNode( Workspace workspace,
                       String fullPath ) throws Exception {
        super(fullPath);
    	assert workspace != null;
    	assert fullPath != null;

        this.workspace = workspace;

        // add properties
        JSONObject properties = new JSONObject();
        properties.put(IJcrConstants.PRIMARY_TYPE_PROPERTY, IJcrConstants.FOLDER_NODE_TYPE);
        put(IJsonConstants.PROPERTIES_KEY, properties);
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @return the full path of folder within the workspace
     */
    public String getPath() {
        return getId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The URL will NOT end in '/'.
     * 
     * @see org.modeshape.web.jcr.rest.client.json.JsonNode#getUrl()
     */
    @Override
    public URL getUrl() throws Exception {
        WorkspaceNode workspaceNode = new WorkspaceNode(this.workspace);
        StringBuilder url = new StringBuilder(workspaceNode.getUrl().toString());

        // make sure path starts with a '/'
        String path = getPath();

        if (!path.startsWith("/")) {
            path = '/' + path;
        }

        // make sure path does NOT end with a '/'
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // path needs to be encoded
        url.append(JsonUtils.encode(path));

        return new URL(url.toString());
    }

}
