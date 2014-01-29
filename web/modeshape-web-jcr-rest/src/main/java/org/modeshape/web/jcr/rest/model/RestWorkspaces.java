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

package org.modeshape.web.jcr.rest.model;

import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.web.jcr.rest.RestHelper;

/**
 * A REST representation of a collection of {@link Workspace workspaces}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestWorkspaces implements JSONAble {

    private final List<Workspace> workspaces;

    /**
     * Creates an empty instance.
     */
    public RestWorkspaces() {
        this.workspaces = new ArrayList<Workspace>();
    }

    /**
     * Adds a new workspace to the list of workspaces.
     *
     * @param name a {@code non-null} string, the name of a workspace.
     * @param repositoryUrl a {@code non-null} string, the absolute url to the repository to which the workspace belongs.
     * @return a {@link Workspace} instance
     */
    public Workspace addWorkspace( String name,
                                   String repositoryUrl ) {
        Workspace workspace = new Workspace(name, repositoryUrl);
        workspaces.add(workspace);
        return workspace;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray workspaces = new JSONArray();
        for (Workspace workspace : this.workspaces) {
            workspaces.put(workspace.toJSON());
        }
        result.put("workspaces", workspaces);
        return result;
    }

    private class Workspace implements JSONAble {
        private final String name;
        private final String repositoryUrl;
        private final String queryUrl;
        private final String itemsUrl;
        private final String binaryUrl;
        private final String nodeTypesUrl;

        public Workspace( String name,
                          String repositoryUrl ) {
            this.name = name;
            this.repositoryUrl = repositoryUrl;
            this.queryUrl = RestHelper.urlFrom(repositoryUrl, name, RestHelper.QUERY_METHOD_NAME);
            this.itemsUrl = RestHelper.urlFrom(repositoryUrl, name, RestHelper.ITEMS_METHOD_NAME);
            this.binaryUrl = RestHelper.urlFrom(repositoryUrl, name, RestHelper.BINARY_METHOD_NAME);
            this.nodeTypesUrl = RestHelper.urlFrom(repositoryUrl, name, RestHelper.NODE_TYPES_METHOD_NAME);
        }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("name", name);
            result.put("repository", repositoryUrl);
            result.put("items", itemsUrl);
            result.put("query", queryUrl);
            result.put("binary", binaryUrl);
            result.put("nodeTypes", nodeTypesUrl);
            return result;
        }
    }
}
