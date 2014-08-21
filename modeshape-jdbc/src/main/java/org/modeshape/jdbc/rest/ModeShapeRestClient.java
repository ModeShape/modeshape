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

import java.io.ByteArrayInputStream;
import javax.jcr.query.Query;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jdbc.JdbcI18n;

/**
 * A simple java client which communicates via a {@link JSONRestClient} to an existing ModeShape REST service and unmarshals the
 * JSON objects into a custom POJO model.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class ModeShapeRestClient {

    private static final String NODE_TYPES_SEGMENT = "jcr:system/jcr:nodeTypes?depth=-1";
    private static final String ITEMS_METHOD = "items";
    private static final String QUERY_METHOD = "query";
    private static final String QUERY_PLAN_METHOD = "queryPlan";

    private final JSONRestClient jsonRestClient;

    /**
     * Creates a new REST client instance which will always use the given URL as the server connection
     *
     * @param repoUrl a {@code String} representing a connection to a ModeShape REST service in the format:
     *        [protocol]://[host]:[port]/[context]/[repository]/[workspace]. May not be {@code null}
     * @param username a {@code String} representing the name of the user to use when authenticating with the above server. May be
     *        {@code null}, in which case no authentication will be performed.
     * @param password a {@code String} the password of the above user, if used. May be {@code null}
     */
    public ModeShapeRestClient( String repoUrl,
                                String username,
                                String password ) {
        CheckArg.isNotNull(repoUrl, "repoUrl");
        this.jsonRestClient = new JSONRestClient(repoUrl, username, password);
    }

    /**
     * Returns the URL this client uses to connect to the server.
     * 
     * @return a {@code String}, never {@code null}
     */
    public String serverUrl() {
        return jsonRestClient.url();
    }

    /**
     * Returns a list with all the available repositories.
     *
     * @return a {@link Repositories} instance, never {@code null}
     */
    public Repositories getRepositories() {
        JSONRestClient.Response response = jsonRestClient.doGet();
        if (!response.isOK()) {
            throw new RuntimeException(JdbcI18n.invalidServerResponse.text(jsonRestClient.url(), response.asString()));
        }
        return new Repositories(response.json());
    }

    /**
     * Returns a repository which has the given name or {@code null}.
     *
     * @param name the name of a repository; may not be null
     * @return a {@link Repositories.Repository} instace or {@code null}
     */
    public Repositories.Repository getRepository( String name ) {
        JSONRestClient.Response response = jsonRestClient.doGet();
        if (!response.isOK()) {
            throw new RuntimeException(JdbcI18n.invalidServerResponse.text(jsonRestClient.url(), response.asString()));
        }
        return new Repositories(response.json()).getRepository(name);
    }

    /**
     * Returns all the workspaces for the named repository.
     *
     * @param repositoryName a {@code String} the name of a repository; may not be {@code null}
     * @return a {@link Workspaces} instance; never {@code null}
     */
    public Workspaces getWorkspaces( String repositoryName ) {
        String url = jsonRestClient.appendToBaseURL(repositoryName);
        JSONRestClient.Response response = jsonRestClient.doGet(url);
        if (!response.isOK()) {
            throw new RuntimeException(JdbcI18n.invalidServerResponse.text(url, response.asString()));
        }
        return new Workspaces(response.json());
    }

    /**
     * Returns all the node types that are available in the repository from {@code repoUrl}
     *
     * @return a {@link NodeTypes} instance; never {@code null}
     */
    public NodeTypes getNodeTypes() {
        String url = jsonRestClient.appendToURL(ITEMS_METHOD, NODE_TYPES_SEGMENT);
        JSONRestClient.Response response = jsonRestClient.doGet(url);
        if (!response.isOK()) {
            throw new RuntimeException(JdbcI18n.invalidServerResponse.text(url, response.asString()));
        }
        return new NodeTypes(response.json());
    }

    /**
     * Runs a query in the specified language against the repository from {@code repoUrl}.
     *
     * @param query a {@code String}, never {@code null}
     * @param queryLanguage the language of the query, never {@code null}
     * @return a {@link QueryResult} instance, never {@code null}
     * @see javax.jcr.query.Query
     */
    public QueryResult query( String query,
                              String queryLanguage ) {
        String url = jsonRestClient.appendToURL(QUERY_METHOD);
        String contentType = contentTypeForQueryLanguage(queryLanguage);
        JSONRestClient.Response response = jsonRestClient.postStream(new ByteArrayInputStream(query.getBytes()), url, contentType);
        if (!response.isOK()) {
            throw new RuntimeException(JdbcI18n.invalidServerResponse.text(url, response.asString()));
        }
        return new QueryResult(response.json());
    }

    /**
     * Returns a string representation of a query plan in a given language.
     * 
     * @param query a {@code String}, never {@code null}
     * @param queryLanguage the language of the query, never {@code null}
     * @return a {@code String} description of the plan, never {@code null}
     */
    public String queryPlan( String query,
                             String queryLanguage ) {
        String url = jsonRestClient.appendToURL(QUERY_PLAN_METHOD);
        String contentType = contentTypeForQueryLanguage(queryLanguage);
        JSONRestClient.Response response = jsonRestClient.postStreamTextPlain(new ByteArrayInputStream(query.getBytes()), url,
                                                                              contentType);
        if (!response.isOK()) {
            throw new RuntimeException(JdbcI18n.invalidServerResponse.text(url, response.asString()));
        }
        return response.asString();
    }

    @SuppressWarnings( "deprecation" )
    private String contentTypeForQueryLanguage( String queryLanguage ) {
        switch (queryLanguage) {
            case Query.XPATH: {
                return "application/jcr+xpath";
            }
            case Query.SQL: {
                return "application/jcr+sql";
            }
            case Query.JCR_SQL2: {
                return "application/jcr+sql2";
            }
            case Query.JCR_JQOM: {
                return "application/jcr+search";
            }
            default: {
                throw new IllegalArgumentException("Invalid query language: " + queryLanguage);
            }
        }
    }
}
