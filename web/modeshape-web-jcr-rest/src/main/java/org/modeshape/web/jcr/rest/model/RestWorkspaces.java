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

package org.modeshape.web.jcr.rest.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.web.jcr.rest.RestHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Horia Chiorean
 */
public final class RestWorkspaces implements JSONAble {

    private final List<Workspace> workspaces;

    public RestWorkspaces() {
        this.workspaces = new ArrayList<Workspace>();
    }

    public Workspace addWorkspace(String name, String repositoryUrl) {
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
