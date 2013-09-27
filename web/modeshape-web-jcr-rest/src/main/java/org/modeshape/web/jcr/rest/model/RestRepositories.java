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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     * @param url a {@code non-null} string, the absolute url to the repository
     * @return a {@link Repository} instance.
     */
    public Repository addRepository( String name,
                                     String url ) {
        Repository repository = new Repository(name, url);
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
        private final String url;
        private final Map<String, List<String>> metadata;
        private int activeSessionsCount;

        protected Repository( String name,
                            String url ) {
            this.name = name;
            this.url = url;
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
            object.put("workspaces", url);
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
