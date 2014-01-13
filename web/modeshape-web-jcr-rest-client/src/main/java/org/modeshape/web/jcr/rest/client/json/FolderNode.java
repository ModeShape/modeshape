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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.web.jcr.rest.client.IJcrConstants;
import org.modeshape.web.jcr.rest.client.domain.Workspace;
import static org.modeshape.web.jcr.rest.client.IJcrConstants.MIXIN_TYPES_PROPERTY;
import static org.modeshape.web.jcr.rest.client.IJcrConstants.PUBLISH_AREA_DESCRIPTION;
import static org.modeshape.web.jcr.rest.client.IJcrConstants.PUBLISH_AREA_TITLE;
import static org.modeshape.web.jcr.rest.client.IJcrConstants.PUBLISH_AREA_TYPE;

/**
 * The <code>FolderNode</code> class is responsible for knowing how to create a URL for a folder, create a JSON representation of
 * a folder, and create the appropriate JCR nodes for a folder.
 */
@Immutable
public final class FolderNode extends JsonNode {

    private static final long serialVersionUID = 1L;

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
        withPrimaryType(IJcrConstants.FOLDER_NODE_TYPE);
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

    void markAsPublishArea(String title, String description) throws Exception {
        withMixin(PUBLISH_AREA_TYPE).withProperty(PUBLISH_AREA_TITLE, title).withProperty(PUBLISH_AREA_DESCRIPTION, description);
    }

    void unmarkAsPublishArea() throws Exception {
        withProperty(MIXIN_TYPES_PROPERTY, new JSONArray()).withProperty(PUBLISH_AREA_TITLE, JSONObject.NULL).withProperty(
                PUBLISH_AREA_DESCRIPTION, JSONObject.NULL);
    }
}
