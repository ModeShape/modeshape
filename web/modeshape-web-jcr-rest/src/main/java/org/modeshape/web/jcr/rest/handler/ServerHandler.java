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

package org.modeshape.web.jcr.rest.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.web.jcr.RepositoryManager;
import org.modeshape.web.jcr.rest.RestHelper;

/**
 * Resource handler that implements REST methods for servers.
 * 
 * @deprecated since 3.0, use {@link RestServerHandler} instead
 */
@Immutable
@Deprecated
public class ServerHandler extends AbstractHandler {

    /**
     * Returns the list of JCR repositories available on this server
     * 
     * @param request the servlet request; may not be null
     * @return the JSON-encoded version of the item (and, if the item is a node, its subgraph, depending on the value of
     *         {@code depth})
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     * @deprecated since 3.0
     */
    @Deprecated
    public String getRepositories( HttpServletRequest request ) throws JSONException, RepositoryException {
        assert request != null;

        JSONObject jsonRepositories = new JSONObject();
        String uri = request.getRequestURI();

        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        Collection<String> repoNames = RepositoryManager.getJcrRepositoryNames();
        for (String repoName : repoNames) {
            if (repoName.trim().length() == 0) {
                repoName = EMPTY_REPOSITORY_NAME;
            }
            String name = RestHelper.URL_ENCODER.encode(repoName);
            JSONObject repository = new JSONObject();
            JSONObject resources = new JSONObject();
            resources.put("workspaces", uri + "/" + name);
            repository.put("name", name);
            repository.put("resources", resources);

            // Get a Session so we can get the descriptors ...
            try {
                String workspaceName = null;
                Session session = getSession(request, name, workspaceName);
                if (session != null) {
                    JSONObject metadata = getRepositoryMetadata(session);
                    if (metadata != null) {
                        repository.put("metadata", metadata);
                    }
                }
                JSONObject mapped = new JSONObject();
                mapped.put("repository", repository);
                jsonRepositories.put(name, mapped);
            } catch (RepositoryException e) {
                // Ignore, because we can't log in and thus cannot figure out any of the workspace names ...
                e.printStackTrace();
            }
        }
        return RestHelper.responseString(jsonRepositories, request);
    }

    protected JSONObject getRepositoryMetadata( Session session ) throws JSONException {
        JSONObject metadata = new JSONObject();
        Repository repository = session.getRepository();
        for (String key : repository.getDescriptorKeys()) {
            Value[] values = repository.getDescriptorValues(key);
            if (values == null) {
                continue;
            }
            if (values.length == 1) {
                Value value = values[0];
                if (value == null) {
                    continue;
                }
                metadata.put(key, RestHelper.jsonEncodedStringFor(value));
            } else {
                List<String> valueStrings = new ArrayList<String>();
                for (Value value : values) {
                    if (value == null) {
                        continue;
                    }
                    valueStrings.add(RestHelper.jsonEncodedStringFor(value));
                }
                if (valueStrings.isEmpty()) {
                    continue;
                }
                if (valueStrings.size() == 1) {
                    metadata.put(key, valueStrings.get(0));
                } else {
                    metadata.put(key, new JSONArray(valueStrings));
                }
            }
        }
        return metadata;
    }
}
