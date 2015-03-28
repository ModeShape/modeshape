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
import java.util.Map;
import java.util.TreeMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.web.jcr.rest.RestHelper;

/**
 * A REST representation of a collection of {@link Repository repositories}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestRepositories implements JSONAble {

    private final List<Repository> repositories;

    /**
     * Creates an empty instance.
     */
    public RestRepositories() {
        repositories = new ArrayList<Repository>();
    }

    /**
     * Adds a repository to the list.
     *
     * @param name a {@code non-null} string, the name of the repository.
     * @param request a {@link HttpServletRequest} instance
     * @return a {@link Repository} instance.
     */
    public Repository addRepository( String name,
                                     HttpServletRequest request ) {
        Repository repository = new Repository(name, request);
        repositories.add(repository);
        return repository;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray repositories = new JSONArray();
        for (Repository repository : this.repositories) {
            repositories.put(repository.toJSON());
        }
        result.put("repositories", repositories);
        return result;
    }

    public final class Repository implements JSONAble {
        private final String name;
        private final String workspacesUrl;
        private final Map<String, List<String>> metadata;
        private final String backupUrl;
        private final String restoreUrl;
        private int activeSessionsCount;

        protected Repository( String name, 
                              HttpServletRequest request) {
            this.name = name;
            this.workspacesUrl = RestHelper.urlFrom(request, name);
            this.backupUrl = RestHelper.urlFrom(request, name, RestHelper.BACKUP_METHOD_NAME);
            this.restoreUrl = RestHelper.urlFrom(request, name, RestHelper.RESTORE_METHOD_NAME);
            this.metadata = new TreeMap<String, List<String>>();
        }

        /**
         * Adds metadata to this repository.
         *
         * @param key a a {@code non-null} string, the key/title of the metadata.
         * @param value a list of values for the above key.
         */
        public void addMetadata( String key,
                                 List<String> value ) {
            if (key != null && value != null && !value.isEmpty()) {
                metadata.put(key, value);
            }
        }

        /**
         * Sets the number of active sessions for this repository.
         *
         * @param activeSessionsCount the number of active sessions
         */
        public void setActiveSessionsCount(int activeSessionsCount) {
            this.activeSessionsCount = activeSessionsCount;
        }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("name", name);
            object.put("workspaces", workspacesUrl);
            object.put("backup", backupUrl);
            object.put("restore", restoreUrl);
            object.put("activeSessionsCount", activeSessionsCount);
            JSONObject metadata = new JSONObject();
            for (String metadataKey : this.metadata.keySet()) {
                List<String> values = this.metadata.get(metadataKey);
                if (values.size() == 1) {
                    metadata.put(metadataKey, values.get(0));
                } else {
                    metadata.put(metadataKey, values);
                }
            }
            object.put("metadata", metadata);
            return object;
        }
    }
}
