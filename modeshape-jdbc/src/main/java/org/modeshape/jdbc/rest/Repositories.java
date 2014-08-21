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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * POJO which can unmarshal the {@link org.codehaus.jettison.json.JSONObject} representation of a list of repositories coming
 * from a ModeShape REST Service.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class Repositories implements Iterable<Repositories.Repository> {

    private static final String KEY_REPOSITORIES = "repositories";
    private static final String KEY_NAME = "name";

    private final Map<String, Repository> repositories;

    /**
     * Creates a new instance wrapping the JSON data.
     *
     * @param json a {@link org.codehaus.jettison.json.JSONObject} coming from the REST server; never {@code null}
     */
    protected Repositories( JSONObject json ) {
        try {
            if (!json.has(KEY_REPOSITORIES)) {
                throw new IllegalArgumentException("Invalid JSON object: " + json);
            }
            JSONArray repositoriesJSON = json.getJSONArray(KEY_REPOSITORIES);
            int length = repositoriesJSON.length();
            this.repositories = new HashMap<>(length);
            for (int i = 0; i < length; i++) {
                JSONObject repositoryJSON = repositoriesJSON.getJSONObject(i);
                Repository repository = new Repository(repositoryJSON);
                repositories.put(repository.getName(), repository);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<Repository> iterator() {
        return repositories.values().iterator();
    }

    /**
     * Returns the list of repositories this container holds.
     *
     * @return a {@code List(Repository)}, never {@code null}
     */
    public Set<Repository> getRepositories() {
        return new HashSet<>(repositories.values());
    }

    /**
     * Returns a list of all the repository names.
     *
     * @return a {@code Set} of names, never {@code null}
     */
    public Set<String> getRepositoryNames() {
        return new HashSet<>(repositories.keySet());
    }

    /**
     * Returns a repository with the given name from the list of contained repositories.
     *
     * @param name a {@code String} the name of the repository to look for; never {@code null}
     * @return either a {@link Repositories.Repository} instance or {@code null} if
     * there is no such repository.
     */
    public Repository getRepository(String name) {
        return repositories.get(name);
    }

    /**
     * POJO representation of a {@link javax.jcr.Repository}
     */
    public static final class Repository {
        private final String name;
        private final Map<String, Object> metadata;
        private final int activeSessionsCount;

        @SuppressWarnings( "unchecked" )
        protected Repository( JSONObject object ) {
            try {
                this.name = object.getString(KEY_NAME);
                this.activeSessionsCount = object.has("activeSessionsCount") ? object.getInt("activeSessionsCount") : 0;
                this.metadata = new LinkedHashMap<>();
                if (object.has("metadata")) {
                    JSONObject metadataObject = object.getJSONObject("metadata");
                    for (Iterator<String> keysIterator = metadataObject.keys(); keysIterator.hasNext(); ) {
                        String key = keysIterator.next();
                        Object value = metadataObject.get(key);
                        if (value instanceof JSONArray) {
                            JSONArray array = (JSONArray)value;
                            String[] strings = new String[array.length()];
                            for (int i = 0; i < array.length(); i++) {
                                strings[i] = array.get(i).toString();
                            }
                            this.metadata.put(key, strings);
                        } else {
                            this.metadata.put(key, value.toString());
                        }
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns the name of the repository.
         *
         * @return a {@code String}, never {@code null}
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the repository metadata, if any.
         *
         * @return a {@code Map(metadataKey, metadataValue)}, never {@code null}
         */
        public Map<String, Object> getMetadata() {
            return metadata;
        }

        /**
         * Returns the number of active sessions on this repository.
         *
         * @return an {@code int}, never negative.
         */
        public int getActiveSessionsCount() {
            return activeSessionsCount;
        }
    }
}
