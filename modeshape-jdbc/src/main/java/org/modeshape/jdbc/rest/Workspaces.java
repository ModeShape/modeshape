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
package org.modeshape.jdbc.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * POJO which can unmarshal the {@link org.codehaus.jettison.json.JSONObject} representation of a list of workspaces coming
 * from a ModeShape REST Service.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class Workspaces implements Iterable<String> {

    private final List<String> workspaces;

    /**
     * Creates a new instance which wraps a JSON response.
     *
     * @param object a {@link org.codehaus.jettison.json.JSONObject}; never {@code null}
     */
    protected Workspaces( JSONObject object ) {
        try {
            if (!object.has("workspaces")) {
                throw new IllegalArgumentException("Invalid JSON object: " + object);
            }
            JSONArray workspacesJSON = object.getJSONArray("workspaces");
            this.workspaces = new ArrayList<>(workspacesJSON.length());
            for (int i = 0; i < workspacesJSON.length(); i++) {
                JSONObject workspaceJSON = workspacesJSON.getJSONObject(i);
                this.workspaces.add(workspaceJSON.getString("name"));
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return workspaces.iterator();
    }

    /**
     * Returns the list of workspaces.
     *
     * @return a list of workspace names; never {@code null}
     */
    public List<String> getWorkspaces() {
        return workspaces;
    }
}
