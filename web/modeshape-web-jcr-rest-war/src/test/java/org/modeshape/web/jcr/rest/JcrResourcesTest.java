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
package org.modeshape.web.jcr.rest;

import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Test of the ModeShape JCR REST resource. Note that this test case uses a very low-level API to construct requests and
 * deconstruct the responses. Users are encouraged to use a higher-level library to communicate with the REST server (e.g., Apache
 * HTTP Commons).
 *
 * @author ?
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JcrResourcesTest {

    private static final List<String> JSON_PROPERTIES_IGNORE_EQUALS = Arrays.asList("jcr:uuid", "jcr:score");

    /**
     * The name of the repository is dictated by the "repository-config.json" configuration file loaded by the
     * "org.modeshape.jcr.JCR_URL" parameter in the "web.xml" file.
     */
    private static final String REPOSITORY_NAME = "repo";
    private static final String SERVER_CONTEXT = "/resources/v1";
    private static final String SERVER_URL = "http://localhost:8090";

    private static final String TEST_NODE = "testNode";

    private HttpURLConnection connection;

    @Before
    public void beforeEach() {
        setDefaultAuthenticator("dnauser", "password");
    }

    private void setDefaultAuthenticator( final String username,
                                          final String password ) {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });
    }

    @After
    public void afterEach() throws Exception {
        doDelete(itemsUrl(TEST_NODE)).submit();
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Test
    public void shouldNotServeContentToUnauthorizedUser() throws Exception {
        setDefaultAuthenticator("dnauser", "invalidpassword");
        doGet().isUnauthorized();
    }

    @Test
    public void shouldNotServeContentToUserWithoutConnectRole() throws Exception {
        setDefaultAuthenticator("unauthorizeduser", "password");
        doGet().isUnauthorized();
    }

    @Test
    public void shouldServeContentAtRoot() throws Exception {
        //http://localhost:8090/resources/v1
        doGet().isOk().hasBodyLikeFile(contentRoot());
    }

    protected String contentRoot() {
        return "v1/get/root.json";
    }

    @Test
    public void shouldServeListOfWorkspacesForValidRepository() throws Exception {
        //http://localhost:8090/resources/v1/repo
        doGet(REPOSITORY_NAME).isOk().hasBodyLikeFile(workspaces());
    }

    protected String workspaces() {
        return "v1/get/workspaces.json";
    }

    @Test
    public void shouldReturnErrorForInvalidRepository() throws Exception {
        //http://localhost:8090/resources/v1/XX
        doGet("XXX").isNotFound().isJSON();
    }

    @Test
    public void shouldRetrieveRootNodeForValidRepository() throws Exception {
        //http://localhost:8090/resources/v1/repo/default
        doGet(itemsUrl()).isOk().hasBodyLikeFile(rootNode());
    }

    protected String rootNode() {
        return "v1/get/root_node.json";
    }

    @Test
    public void shouldRetrieveRoottestNodendChildrenWhenDepthSet() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items?depth=1
        doGet(itemsUrl() + "?depth=1").isOk().hasBodyLikeFile(rootNodeDepthOne());
    }

    @Test
    public void shouldRetrieveRoottestNodendChildrenWhenDeprecatedDepthSet() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items?mode:depth=1
        doGet(itemsUrl() + "?mode:depth=1").isOk().hasBodyLikeFile(rootNodeDepthOne());
    }

    protected String rootNodeDepthOne() {
        return "v1/get/root_node_depth1.json";
    }

    @Test
    public void shouldRetrieveSystemtestNodendChildrenWithDepthOne() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/jcr:system?depth=1
        doGet(itemsUrl("jcr:system") + "?depth=1").isOk().hasBodyLikeFile(systemNodeDepthOne());
    }

    protected String systemNodeDepthOne() {
        return "v1/get/system_node_depth1.json";
    }

    @Test
    public void shouldRetrieveNtBaseNodeType() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/jcr:system/jcr:nodeTypes/nt:base
        doGet(itemsUrl("jcr:system/jcr:nodeTypes/nt:base")).isOk().hasBodyLikeFile(ntBaseNodeType());
    }

    protected String ntBaseNodeType() {
        return "v1/get/nt_base.json";
    }

    @Test
    public void shouldRetrieveNtBaseDepthFour() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/jcr:system/jcr:nodeTypes/nt:base?depth=4
        doGet(itemsUrl("jcr:system/jcr:nodeTypes/nt:base") + "?depth=4").isOk().hasBodyLikeFile(ntBaseDepthFour());
    }

    protected String ntBaseDepthFour() {
        return "v1/get/nt_base_depth4.json";
    }

    @Test
    public void shouldNotRetrieveNonExistentNode() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/foo
        doGet(itemsUrl("foo")).isNotFound().isJSON();
    }

    @Test
    public void shouldNotRetrieveNonExistentProperty() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/jcr:system/foobar
        doGet(itemsUrl("jcr:system/foobar")).isNotFound().isJSON();
    }

    @Test
    public void shouldRetrieveProperty() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/jcr:system/jcr:primaryType
        doGet(itemsUrl("jcr:system/jcr:primaryType")).isOk().hasBodyLikeFile(systemPrimaryTypeProperty());
    }

    protected String systemPrimaryTypeProperty() throws Exception {
        return "v1/get/system_primaryType_property.json";
    }

    @Test
    public void shouldPostNodeToValidPathWithPrimaryType() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isCreated().hasBodyLikeFile(nodeWithPrimaryTypeResponse());
    }

    protected String nodeWithPrimaryTypeRequest() {
        return "v1/post/node_with_primaryType_request.json";
    }

    protected String nodeWithPrimaryTypeResponse() {
        return "v1/post/node_with_primaryType_response.json";
    }

    @Test
    public void shouldPostNodeToValidPathWithoutPrimaryType() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithoutPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isCreated().hasBodyLikeFile(
                nodeWithoutPrimaryTypeResponse());
    }

    protected String nodeWithoutPrimaryTypeRequest() {
        return "v1/post/node_without_primaryType_request.json";
    }

    protected String nodeWithoutPrimaryTypeResponse() {
        return "v1/post/node_without_primaryType_response.json";
    }

    @Test
    public void shouldPostNodeToValidPathWithMixinTypes() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithMixinRequest(), itemsUrl(TEST_NODE)).isCreated().hasBodyLikeFile(nodeWithMixinResponse());
        doGet(itemsUrl(TEST_NODE)).isOk().hasBodyLikeFile(nodeWithMixinResponse());
    }

    protected String nodeWithMixinRequest() {
        return "v1/post/node_with_mixin_request.json";
    }

    protected String nodeWithMixinResponse() {
        return "v1/post/node_with_mixin_response.json";
    }

    @Test
    public void shouldNotPosttestNodetInvalidParentPath() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/foo/bar
        doPost(null, itemsUrl("foo/bar")).isBadRequest();
    }

    @Test
    public void shouldNotPostNodeWithInvalidPrimaryType() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeInvalidPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isBadRequest();
        doGet(itemsUrl(TEST_NODE)).isNotFound();
    }

    protected String nodeInvalidPrimaryTypeRequest() {
        return "v1/post/node_invalid_primaryType_request.json";
    }

    @Test
    public void shouldPostNodeHierarchy() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeHierarchyRequest(), itemsUrl(TEST_NODE)).isCreated();

        // Make sure that we can retrieve the node with a GET
        doGet(itemsUrl(TEST_NODE) + "?depth=1").isOk().hasBodyLikeFile(nodeHierarchyResponse());
    }

    protected String nodeHierarchyRequest() {
        return "v1/post/node_hierarchy_request.json";
    }

    protected String nodeHierarchyResponse() {
        return "v1/post/node_hierarchy_response.json";
    }

    @Test
    public void shouldFailWholeTransactionIfOneNodeIsBad() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeHierarchyInvalidTypeRequest(), itemsUrl(TEST_NODE)).isBadRequest();
        doGet(itemsUrl(TEST_NODE) + "?depth=1").isNotFound();
    }

    protected String nodeHierarchyInvalidTypeRequest() {
        return "v1/post/node_hierarchy_invalidType_request.json";
    }

    @Test
    public void shouldNotDeleteNonExistentItem() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/invalidItemForDelete
        doDelete(itemsUrl("invalidItemForDelete")).isNotFound();
    }

    @Test
    public void shouldDeleteExistingNode() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithoutPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isCreated();
        doGet(itemsUrl(TEST_NODE)).isOk();
        doDelete(itemsUrl(TEST_NODE)).isDeleted();
        doGet(itemsUrl(TEST_NODE)).isNotFound();
    }

    @Test
    public void shouldDeleteExistingProperty() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithProperty(), itemsUrl(TEST_NODE)).isCreated();
        doGet(itemsUrl(TEST_NODE, propertyName())).isOk();
        doDelete(itemsUrl(TEST_NODE, propertyName())).isDeleted();
        doGet(itemsUrl(TEST_NODE, propertyName())).isNotFound();
        doGet(itemsUrl(TEST_NODE)).isOk().hasBodyLikeFile(nodeWithoutProperty());
    }

    protected String propertyName() {
        return "testProperty";
    }

    protected String nodeWithProperty() {
        return "v1/put/node_with_property.json";
    }

    protected String nodeWithoutProperty() {
        return "v1/put/node_without_property.json";
    }

    @Test
    public void shouldNotBeAbleToPutAtInvalidPath() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/nonexistantNode
        doPut(propertyEdit(), itemsUrl("nonexistantNode")).isNotFound();
    }

    protected String propertyEdit() {
        return "v1/put/property_edit.json";
    }

    @Test
    public void shouldBeAbleToPutValueToProperty() throws Exception {
        doPost(nodeWithProperty(), itemsUrl(TEST_NODE)).isCreated();
        doPut(propertyEdit(), itemsUrl(TEST_NODE, propertyName())).isOk().hasBodyLikeFile(nodeWithPropertyAfterEdit());
    }

    protected String nodeWithPropertyAfterEdit() {
        return "v1/put/node_with_property_after_edit.json";
    }

    @Test
    public void shouldBeAbleToPutBinaryValueToProperty() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();
        doPut(binaryPropertyEdit(), itemsUrl(TEST_NODE, binaryPropertyName())).isOk();
        doGet(itemsUrl(TEST_NODE)).isOk().hasBodyLikeFile(nodeBinaryPropertyAfterEdit());
    }

    protected String nodeWithBinaryProperty() {
        return "v1/put/node_with_binary_property.json";
    }

    protected String binaryPropertyEdit() {
        return "v1/put/binary_property_edit.json";
    }

    protected String binaryPropertyName() {
        return "testProperty";
    }

    protected String nodeBinaryPropertyAfterEdit() {
        return "v1/put/node_with_binary_property_after_edit.json";
    }

    @Test
    public void shouldBeAbleToPutPropertiesToNode() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithProperties(), itemsUrl(TEST_NODE)).isCreated();
        doPut(propertiesEdit(), itemsUrl(TEST_NODE)).isOk();
        doGet(itemsUrl(TEST_NODE)).isOk().hasBodyLikeFile(nodeWithPropertiesAfterEdit());
    }

    protected String nodeWithProperties() {
        return "v1/put/node_with_properties.json";
    }

    protected String nodeWithPropertiesAfterEdit() {
        return "v1/put/node_with_properties_after_edit.json";
    }

    protected String propertiesEdit() {
        return "v1/put/properties_edit.json";
    }

    @Test
    public void shouldBeAbleToAddAndRemoveMixinTypes() throws Exception {
        //http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithProperties(), itemsUrl(TEST_NODE)).isCreated();
        doPut(addMixin(), itemsUrl(TEST_NODE)).isOk().hasBodyLikeFile(nodeWithMixin());
        doPut(removeMixins(), itemsUrl(TEST_NODE)).isOk().hasBodyLikeFile(nodeWithProperties());
    }

    protected String addMixin() {
        return "v1/put/add_mixin.json";
    }

    protected String removeMixins() {
        return "v1/put/remove_mixins.json";
    }

    protected String nodeWithMixin() {
        return "v1/put/node_with_mixin.json";
    }

    @Test
    public void shouldRetrieveDataFromXPathQuery() throws Exception {
        doPost(queryNode(), itemsUrl(TEST_NODE)).isCreated();
        xpathQuery("//" + TEST_NODE, queryUrl()).isOk().isJSON().hasBodyLikeFile(singleNodeXPath());
    }

    protected String queryNode() {
        return "v1/query/query_node.json";
    }

    protected String singleNodeXPath() {
        return "v1/query/single_node_xpath.json";
    }

    @Test
    public void shouldRespectQueryOffsetAndLimit() throws Exception {
        doPost(queryNode(), itemsUrl(TEST_NODE)).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();

        String query = "//element(child) order by @foo";
        xpathQuery(query, queryUrl() + "?offset=1&limit=2").isOk().isJSON().hasBodyLikeFile(queryResultOffsetAndLimit());
    }

    protected String queryResultOffsetAndLimit() {
        return "v1/query/query_result_offset_and_limit.json";
    }

    @Test
    public void shouldAllowJcrSql2Query() throws Exception {
        doPost(queryNode(), itemsUrl(TEST_NODE)).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNode(), itemsUrl(TEST_NODE, "child")).isCreated();

        String query = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/" + TEST_NODE + "')";
        jcrSQL2Query(query, queryUrl()).isOk().isJSON().hasBodyLikeFile(jcrSQL2Result());
    }

    protected String jcrSQL2Result() throws Exception {
        return "v1/query/query_result_jcrSql2.json";
    }

    protected String itemsUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/items", additionalPathSegments);
    }

    protected String queryUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/query", additionalPathSegments);
    }

    protected Response doGet() throws Exception {
        return new Response(newConnection("GET", null));
    }

    protected Response doGet( String url ) throws Exception {
        return new Response(newConnection("GET", null, url));
    }

    protected Response doPost( String payloadFile,
                               String url ) throws Exception {
        InputStream is = null;
        if (payloadFile != null) {
            is = getClass().getClassLoader().getResourceAsStream(payloadFile);
            assertNotNull(is);
        }
        return postStream(is, url, MediaType.APPLICATION_JSON);
    }

    protected Response xpathQuery( String query,
                                   String url ) throws Exception {
        return postStream(new ByteArrayInputStream(query.getBytes()), url, "application/jcr+xpath");
    }

    protected Response jcrSQL2Query( String query,
                                     String url ) throws Exception {
        return postStream(new ByteArrayInputStream(query.getBytes()), url, "application/jcr+sql2");
    }

    protected Response postStream( InputStream is,
                                   String url,
                                   String mediaType ) throws Exception {
        HttpURLConnection connection = newConnection("POST", mediaType, url);
        if (is != null) {
            connection.getOutputStream().write(IoUtil.readBytes(is));
        }
        return new Response(connection);
    }

    protected Response doPut( String payloadFile,
                              String url ) throws Exception {
        HttpURLConnection connection = newConnection("PUT", MediaType.APPLICATION_JSON, url);
        if (payloadFile != null) {
            String fileContent = IoUtil.read(getClass().getClassLoader().getResourceAsStream(payloadFile));
            connection.getOutputStream().write(fileContent.getBytes());
        }
        return new Response(connection);
    }

    protected Response doDelete( String url ) throws Exception {
       return new Response(newConnection("DELETE", null, url));
    }

    private HttpURLConnection newConnection( String method,
                                             String contentType,
                                             String... pathSegments ) throws IOException {
        if (connection != null) {
            connection.disconnect();
        }

        String serviceUrl = getServerUrl() + getServerContext();
        StringBuilder urlBuilder = new StringBuilder(serviceUrl);
        for (String pathSegment : pathSegments) {
            if (!pathSegment.startsWith("/")) {
                urlBuilder.append("/");
            }
            urlBuilder.append(pathSegment);
        }
        URL postUrl = new URL(urlBuilder.toString());
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        if (contentType != null) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        return connection;
    }

    private void assertJSON( Object expected,
                             Object actual ) throws JSONException {
        if (expected instanceof JSONObject) {
            assert (actual instanceof JSONObject);
            JSONObject expectedJSON = (JSONObject)expected;
            JSONObject actualJSON = (JSONObject)actual;

            for (Iterator<?> keyIterator = expectedJSON.keys(); keyIterator.hasNext(); ) {
                String key = keyIterator.next().toString();
                assertTrue("Actual JSON object does not contain key: " + key, actualJSON.has(key));

                Object expectedValueAtKey = expectedJSON.get(key);
                Object actualValueAtKey = actualJSON.get(key);

                if (shouldNotAssertEquality(key)) {
                    assertNotNull(actualValueAtKey);
                } else {
                    assertJSON(expectedValueAtKey, actualValueAtKey);
                }
            }
        } else if (expected instanceof JSONArray) {
            assert (actual instanceof JSONArray);
            JSONArray expectedArray = (JSONArray)expected;
            JSONArray actualArray = (JSONArray)actual;
            Assert.assertEquals("Arrays don't have the same length ", expectedArray.length(), actualArray.length());
            for (int i = 0; i < expectedArray.length(); i++) {
                assertJSON(expectedArray.get(i), actualArray.get(i));
            }
        } else {
            assertEquals("Values don't match", expected.toString(), actual.toString());
        }
    }

    private boolean shouldNotAssertEquality( String propertyName ) {
        for (String propertyToIgnore : JSON_PROPERTIES_IGNORE_EQUALS) {
            if (propertyName.toLowerCase().contains(propertyToIgnore.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String responseString( HttpURLConnection connection ) throws IOException {
        StringBuilder buff = new StringBuilder();
        InputStream stream = connection.getInputStream();
        int bytesRead;
        byte[] bytes = new byte[1024];
        while (-1 != (bytesRead = stream.read(bytes, 0, 1024))) {
            buff.append(new String(bytes, 0, bytesRead));
        }

        return buff.toString();
    }

    protected JSONObject responseObject( HttpURLConnection connection ) throws Exception {
        return new JSONObject(responseString(connection));
    }

    protected String getServerContext() {
        return SERVER_CONTEXT;
    }

    protected String getServerUrl() {
        return SERVER_URL;
    }

    protected final class Response {

        private final HttpURLConnection connection;

        private Response( HttpURLConnection connection ) {
            this.connection = connection;
        }

        private Response hasCode( int responseCode ) throws Exception {
            assertEquals(responseCode, connection.getResponseCode());
            return this;
        }

        private Response hasHeader( String name,
                                    String value ) {
            assertEquals(value, connection.getHeaderField(name));
            return this;
        }

        private Response submit() throws IOException {
            //just trigger the request, ignore the result
            connection.getResponseCode();
            return this;
        }

        protected Response isOk() throws Exception {
            return hasCode(HttpURLConnection.HTTP_OK);
        }

        protected Response isCreated() throws Exception {
            return hasCode(HttpURLConnection.HTTP_CREATED);
        }

        protected Response isDeleted() throws Exception {
            return hasCode(HttpURLConnection.HTTP_NO_CONTENT);
        }

        protected Response isNotFound() throws Exception {
            return hasCode(HttpURLConnection.HTTP_NOT_FOUND);
        }

        protected Response isUnauthorized() throws Exception {
            return hasCode(HttpURLConnection.HTTP_UNAUTHORIZED);
        }

        protected Response isBadRequest() throws Exception {
            return hasCode(HttpURLConnection.HTTP_BAD_REQUEST);
        }

        protected Response isJSON() throws Exception {
            return hasHeader("Content-Type", MediaType.APPLICATION_JSON);
        }

        protected Response hasBodyLikeFile( String pathToExpectedJSON ) throws Exception {
            isJSON();
            String expectedJSONString = IoUtil.read(getClass().getClassLoader().getResourceAsStream(pathToExpectedJSON));
            JSONObject expectedObject = new JSONObject(expectedJSONString);

            JSONObject responseObject = bodyJSON();
            assertJSON(expectedObject, responseObject);

            return this;
        }

        protected Response hasBodyLikeObject( JSONObject expectedObject ) throws Exception {
            isJSON();
            JSONObject body = bodyJSON();
            assertJSON(expectedObject, body);
            return this;
        }

        protected JSONObject bodyJSON() throws Exception {
            return responseObject(connection);
        }
    }
}
