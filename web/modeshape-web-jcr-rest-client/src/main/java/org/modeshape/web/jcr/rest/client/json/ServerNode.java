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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Server;

/**
 * The <code>ServerNode</code> class is responsible for knowing how to create a URL for a server, create a URL to obtain a
 * server's repositories, and parse a JSON response into {@link Repository repository} objects.
 */
public final class ServerNode extends JsonNode {

    private static final long serialVersionUID = 1L;

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(ServerNode.class);

    /**
     * The server containing ModeShape repositories.
     */
    private final Server server;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param server the server containing the repositories (never <code>null</code>)
     */
    public ServerNode( Server server ) {
        super(server.getName());
        this.server = server;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * <p>
     * The URL will NOT end in '/'.
     * 
     * @see org.modeshape.web.jcr.rest.client.json.JsonNode#getUrl()
     */
    @Override
    public URL getUrl() throws Exception {
        StringBuilder url = new StringBuilder(this.server.getUrl());

        // strip off last '/' if necessary
        if (url.lastIndexOf("/") == (url.length() - 1)) {
            url.delete((url.length() - 1), (url.length() - 1));
        }
        return new URL(url.toString());
    }

    /**
     * @return the URL to use when requesting the repositories (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the repositories
     */
    public URL getFindRepositoriesUrl() throws Exception {
        return new URL(getUrl().toString() + '/');
    }

    /**
     * @param jsonResponse the HTTP connection JSON response (never <code>null</code>) containing the repositories
     * @return the repositories found in the JSON response (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the repositories
     */
    @SuppressWarnings( "unchecked" )
    public Collection<Repository> getRepositories( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");
        Collection<Repository> repositories = new ArrayList<Repository>();
        LOGGER.trace("getRepositories:jsonResponse={0}", jsonResponse);
        JSONObject jsonObj = new JSONObject(jsonResponse);

        // keys are the repository names
        for (Iterator<String> itr = jsonObj.keys(); itr.hasNext();) {
            String encodedName = itr.next();
            String name = JsonUtils.decode(encodedName);

            // Get the metadata, if there ...
            Map<String, Object> meta = new HashMap<String, Object>();
            JSONObject named = (JSONObject)jsonObj.get(encodedName);
            if (named.has("repository")) {
                JSONObject repo = (JSONObject)named.get("repository");
                if (repo.has("metadata")) {
                    JSONObject metadata = (JSONObject)repo.get("metadata");
                    for (Iterator<String> keyIter = metadata.keys(); keyIter.hasNext();) {
                        String key = keyIter.next();
                        Object values = metadata.get(key);
                        if (values instanceof JSONArray) {
                            // Extract the string values ...
                            JSONArray vals = (JSONArray)values;
                            String[] stringValues = new String[vals.length()];
                            for (int i = 0; i != vals.length(); ++i) {
                                stringValues[i] = vals.getString(i);
                            }
                            values = stringValues;
                        }
                        meta.put(key, values);
                    }
                    LOGGER.trace("getRepositories: found metadata {0}", meta);
                }
            }
            Repository repository = new Repository(name, this.server, meta);
            repositories.add(repository);
            LOGGER.trace("getRepositories: adding repository={0}", repository);
        }

        return repositories;
    }

}
