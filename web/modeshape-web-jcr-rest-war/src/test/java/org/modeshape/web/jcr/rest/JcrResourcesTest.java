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
package org.modeshape.web.jcr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.MediaType;
import junit.framework.AssertionFailedError;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.web.jcr.rest.handler.AbstractHandler;

/**
 * Test of the ModeShape JCR REST resource. Note that this test case uses a very low-level API to construct requests and
 * deconstruct the responses. Users are encouraged to use a higher-level library to communicate with the REST server (e.g., Apache
 * HTTP Commons).
 * 
 * @author ?
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JcrResourcesTest {

    private static final List<String> JSON_PROPERTIES_IGNORE_EQUALS = Arrays.asList("jcr:uuid", "jcr:score",
                                                                                    AbstractHandler.NODE_ID_CUSTOM_PROPERTY);

    /**
     * The name of the repository is dictated by the "repository-config.json" configuration file loaded by the
     * "org.modeshape.jcr.JCR_URL" parameter in the "web.xml" file.
     */
    protected static final String REPOSITORY_NAME = "repo";
    protected static final String TEST_NODE = "testNode";
    protected static final String CHILDREN_KEY = "children";
    protected static final String ID_KEY = "id";

    private static final HttpHost HOST = new HttpHost("localhost", 8090, "http");

    private static final String SERVER_CONTEXT = "/resources/v1";
    private static final String AUTH_USERNAME = "dnauser";
    private static final String AUTH_PASSWORD = "password";

    private static final UrlEncoder URL_ENCODER = new UrlEncoder().setSlashEncoded(false);

    protected DefaultHttpClient httpClient;

    @Before
    public void beforeEach() throws Exception {
        httpClient = new DefaultHttpClient();
        setAuthCredentials(AUTH_USERNAME, AUTH_PASSWORD);
    }

    private void setAuthCredentials( String authUsername,
                                     String authPassword ) {
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(getHost()),
                                                           new UsernamePasswordCredentials(authUsername, authPassword));
    }

    protected HttpHost getHost() {
        return HOST;
    }

    @After
    public void afterEach() throws Exception {
        doDelete(itemsUrl(TEST_NODE));
        httpClient.getConnectionManager().shutdown();
    }

    @Test
    public void shouldNotServeContentToUnauthorizedUser() throws Exception {
        setAuthCredentials("dnauser", "invalidpassword");
        doGet().isUnauthorized();
    }

    @Test
    public void shouldNotServeContentToUserWithoutConnectRole() throws Exception {
        setAuthCredentials("unauthorizeduser", "password");
        doGet().isUnauthorized();
    }

    @Test
    public void shouldServeContentAtRoot() throws Exception {
        // http://localhost:8090/resources/v1
        doGet().isOk().isJSONObjectLikeFile(contentRoot());
    }

    protected String contentRoot() {
        return "v1/get/root.json";
    }

    @Test
    public void shouldServeListOfWorkspacesForValidRepository() throws Exception {
        // http://localhost:8090/resources/v1/repo
        doGet(REPOSITORY_NAME).isOk().isJSONObjectLikeFile(workspaces());
    }

    protected String workspaces() {
        return "v1/get/workspaces.json";
    }

    @Test
    public void shouldReturnErrorForInvalidRepository() throws Exception {
        // http://localhost:8090/resources/v1/XX
        doGet("XXX").isNotFound().isJSON();
    }

    @Test
    public void shouldRetrieveRootNodeForValidRepository() throws Exception {
        // http://localhost:8090/resources/v1/repo/default
        doGet(itemsUrl()).isOk().isJSONObjectLikeFile(rootNode());
    }

    protected String rootNode() {
        return "v1/get/root_node.json";
    }

    @Test
    public void shouldRetrieveRootNodeWhenDepthSet() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items?depth=1
        doGet(itemsUrl() + "?depth=1").isOk().isJSONObjectLikeFile(rootNodeDepthOne());
    }

    @Test
    public void shouldRetrieveRootNodeWhenDeprecatedDepthSet() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items?mode:depth=1
        doGet(itemsUrl() + "?mode:depth=1").isOk().isJSONObjectLikeFile(rootNodeDepthOne());
    }

    protected String rootNodeDepthOne() {
        return "v1/get/root_node_depth1.json";
    }

    @Test
    public void shouldRetrieveSystemNodeWithDepthOne() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/jcr:system?depth=1
        doGet(itemsUrl("jcr:system") + "?depth=1").isOk().isJSONObjectLikeFile(systemNodeDepthOne());
    }

    protected String systemNodeDepthOne() {
        return "v1/get/system_node_depth1.json";
    }

    @Test
    public void shouldRetrieveNtBaseItems() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/jcr:system/jcr:nodeTypes/nt:base
        doGet(itemsUrl("jcr:system/jcr:nodeTypes/nt:base")).isOk().isJSONObjectLikeFile(ntBaseNodeType());
    }

    protected String ntBaseNodeType() {
        return "v1/get/nt_base.json";
    }

    @Test
    public void shouldRetrieveNtBaseDepthFour() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/jcr:system/jcr:nodeTypes/nt:base?depth=4
        doGet(itemsUrl("jcr:system/jcr:nodeTypes/nt:base") + "?depth=4").isOk().isJSONObjectLikeFile(ntBaseDepthFour());
    }

    protected String ntBaseDepthFour() {
        return "v1/get/nt_base_depth4.json";
    }

    @Test
    public void shouldNotRetrieveNonExistentNode() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/foo
        doGet(itemsUrl("foo")).isNotFound().isJSON();
    }

    @Test
    public void shouldNotRetrieveNonExistentProperty() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/jcr:system/foobar
        doGet(itemsUrl("jcr:system/foobar")).isNotFound().isJSON();
    }

    @Test
    public void shouldRetrieveProperty() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/jcr:system/jcr:primaryType
        doGet(itemsUrl("jcr:system/jcr:primaryType")).isOk().isJSONObjectLikeFile(systemPrimaryTypeProperty());
    }

    protected String systemPrimaryTypeProperty() throws Exception {
        return "v1/get/system_primaryType_property.json";
    }

    @Test
    public void shouldPostNodeToValidPathWithPrimaryType() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isCreated().isJSONObjectLikeFile(nodeWithPrimaryTypeResponse());
    }

    protected String nodeWithPrimaryTypeRequest() {
        return "v1/post/node_with_primaryType_request.json";
    }

    protected String nodeWithPrimaryTypeResponse() {
        return "v1/post/node_with_primaryType_response.json";
    }

    @Test
    public void shouldPostNodeToValidPathWithoutPrimaryType() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithoutPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isCreated()
                                                                    .isJSONObjectLikeFile(nodeWithoutPrimaryTypeResponse());
    }

    protected String nodeWithoutPrimaryTypeRequest() {
        return "v1/post/node_without_primaryType_request.json";
    }

    protected String nodeWithoutPrimaryTypeResponse() {
        return "v1/post/node_without_primaryType_response.json";
    }

    @Test
    @FixFor( "MODE-1950" )
    public void shouldConvertValueTypesFromJSONPrimitives() throws Exception {
        // note that
        doPost(differentPropertyTypesRequest(), itemsUrl(TEST_NODE)).isCreated()
                                                                    .isJSONObjectLikeFile(differentPropertyTypesResponse());
    }

    protected String differentPropertyTypesResponse() {
        return "v1/post/node_different_property_types_response.json";
    }

    protected String differentPropertyTypesRequest() {
        return "v1/post/node_different_property_types_request.json";
    }

    @Test
    public void shouldPostNodeToValidPathWithMixinTypes() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithMixinRequest(), itemsUrl(TEST_NODE)).isCreated().isJSONObjectLikeFile(nodeWithMixinResponse());
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(nodeWithMixinResponse());
    }

    protected String nodeWithMixinRequest() {
        return "v1/post/node_with_mixin_request.json";
    }

    protected String nodeWithMixinResponse() {
        return "v1/post/node_with_mixin_response.json";
    }

    @Test
    public void shouldNotPostNodeWithInvalidParentPath() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/foo/bar
        doPost(nodeWithoutPrimaryTypeRequest(), itemsUrl("foo/bar")).isNotFound();
    }

    @Test
    public void shouldNotPostNodeWithInvalidPrimaryType() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeInvalidPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isNotFound();
        doGet(itemsUrl(TEST_NODE)).isNotFound();
    }

    protected String nodeInvalidPrimaryTypeRequest() {
        return "v1/post/node_invalid_primaryType_request.json";
    }

    @Test
    public void shouldPostNodeHierarchy() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeHierarchyRequest(), itemsUrl(TEST_NODE)).isCreated();

        // Make sure that we can retrieve the node with a GET
        doGet(itemsUrl(TEST_NODE) + "?depth=1").isOk().isJSONObjectLikeFile(nodeHierarchyResponse());
    }

    protected String nodeHierarchyRequest() {
        return "v1/post/node_hierarchy_request.json";
    }

    protected String nodeHierarchyResponse() {
        return "v1/post/node_hierarchy_response.json";
    }

    @Test
    public void shouldFailWholeTransactionIfOneNodeIsBad() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeHierarchyInvalidTypeRequest(), itemsUrl(TEST_NODE)).isNotFound();
        doGet(itemsUrl(TEST_NODE) + "?depth=1").isNotFound();
    }

    protected String nodeHierarchyInvalidTypeRequest() {
        return "v1/post/node_hierarchy_invalidType_request.json";
    }

    @Test
    public void shouldNotDeleteNonExistentItem() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/invalidItemForDelete
        doDelete(itemsUrl("invalidItemForDelete")).isNotFound();
    }

    @Test
    public void shouldDeleteExistingNode() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithoutPrimaryTypeRequest(), itemsUrl(TEST_NODE)).isCreated();
        doGet(itemsUrl(TEST_NODE)).isOk();
        doDelete(itemsUrl(TEST_NODE)).isDeleted();
        doGet(itemsUrl(TEST_NODE)).isNotFound();
    }

    @Test
    public void shouldDeleteExistingProperty() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithProperty(), itemsUrl(TEST_NODE)).isCreated();
        doGet(itemsUrl(TEST_NODE, propertyName())).isOk();
        doDelete(itemsUrl(TEST_NODE, propertyName())).isDeleted();
        doGet(itemsUrl(TEST_NODE, propertyName())).isNotFound();
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(nodeWithoutProperty());
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
        // http://localhost:8090/resources/v1/repo/default/items/nonexistantNode
        doPut(propertyEdit(), itemsUrl("nonexistantNode")).isNotFound();
    }

    protected String propertyEdit() {
        return "v1/put/property_edit.json";
    }

    @Test
    public void shouldBeAbleToPutValueToProperty() throws Exception {
        doPost(nodeWithProperty(), itemsUrl(TEST_NODE)).isCreated();
        doPut(propertyEdit(), itemsUrl(TEST_NODE, propertyName())).isOk().isJSONObjectLikeFile(nodeWithPropertyAfterEdit());
    }

    protected String nodeWithPropertyAfterEdit() {
        return "v1/put/node_with_property_after_edit.json";
    }

    @Test
    public void shouldBeAbleToPutBinaryValueToProperty() throws Exception {
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();
        doPut(binaryPropertyEdit(), itemsUrl(TEST_NODE, binaryPropertyName())).isOk();
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(nodeBinaryPropertyAfterEdit());
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
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithProperties(), itemsUrl(TEST_NODE)).isCreated();
        doPut(propertiesEdit(), itemsUrl(TEST_NODE)).isOk();
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(nodeWithPropertiesAfterEdit());
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
        // http://localhost:8090/resources/v1/repo/default/items/testNode
        doPost(nodeWithProperties(), itemsUrl(TEST_NODE)).isCreated();
        doPut(addMixin(), itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(nodeWithMixin());
        doPut(removeMixins(), itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(nodeWithoutMixins());
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

    protected String nodeWithoutMixins() {
        return "v1/put/node_with_properties.json";
    }

    @Test
    @FixFor( "MODE-1950" )
    public void shouldBeAbleToRemoveMixinByRemovingProperties() throws Exception {
        doPost(publishArea(), itemsUrl(TEST_NODE)).isCreated();
        doPut(publishAreaInvalidUpdate(), itemsUrl(TEST_NODE)).isBadRequest();
        doPut(publishAreaValidUpdate(), itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(publishAreaResponse());
    }

    protected String publishAreaResponse() {
        return "v1/put/publish_area_response.json";
    }

    protected String publishAreaValidUpdate() {
        return "v1/put/publish_area_valid_update.json";
    }

    protected String publishAreaInvalidUpdate() {
        return "v1/put/publish_area_invalid_update.json";
    }

    protected String publishArea() {
        return "v1/put/publish_area.json";
    }

    @Test
    public void shouldRetrieveDataFromXPathQuery() throws Exception {
        doPost(queryNode(), itemsUrl(TEST_NODE)).isCreated();
        xpathQuery("//" + TEST_NODE, queryUrl()).isOk().isJSON().isJSONObjectLikeFile(singleNodeXPath());
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

        String query = "//element(child) order by @foo, @jcr:path";
        xpathQuery(query, queryUrl() + "?offset=1&limit=2").isOk().isJSON().isJSONObjectLikeFile(queryResultOffsetAndLimit());
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

        String query = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/" + TEST_NODE + "') ORDER BY [jcr:path]";
        jcrSQL2Query(query, queryUrl()).isOk().isJSON().isJSONObjectLikeFile(jcrSQL2Result());
    }

    protected String jcrSQL2Result() {
        return "v1/query/query_result_jcrSql2.json";
    }

    protected String itemsUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.ITEMS_METHOD_NAME, additionalPathSegments);
    }

    protected String nodesUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.NODES_METHOD_NAME, additionalPathSegments);
    }

    protected String queryUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.QUERY_METHOD_NAME, additionalPathSegments);
    }

    protected Response doGet() throws Exception {
        return new Response(newDefaultRequest(HttpGet.class, null, null));
    }

    protected Response doGet( String url ) throws Exception {
        return new Response(newDefaultRequest(HttpGet.class, null, null, url));
    }

    protected Response doPost( String payloadFile,
                               String url ) throws Exception {
        InputStream is = null;
        if (payloadFile != null) {
            is = fileStream(payloadFile);
            assertNotNull(is);
        }
        return postStream(is, url, MediaType.APPLICATION_JSON);
    }

    protected Response doPost( InputStream is,
                               String url ) throws Exception {
        return postStream(is, url, null);
    }

    protected Response doPost( JSONObject object,
                               String url ) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream);
        object.write(writer);
        writer.flush();
        writer.close();

        HttpPost post = newDefaultRequest(HttpPost.class, new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),
                                          MediaType.APPLICATION_JSON, url);
        return new Response(post);
    }

    protected InputStream fileStream( String file ) {
        return getClass().getClassLoader().getResourceAsStream(file);
    }

    protected JSONObject readJson( String file ) throws Exception {
        String fileContent = IoUtil.read(fileStream(file));
        return new JSONObject(fileContent);
    }

    protected Response xpathQuery( String query,
                                   String url ) throws Exception {
        return postStream(new ByteArrayInputStream(query.getBytes()), url, "application/jcr+xpath");
    }

    protected Response jcrSQL2Query( String query,
                                     String url ) throws Exception {
        return postStream(new ByteArrayInputStream(query.getBytes()), url, "application/jcr+sql2");
    }

    protected Response jcrSQL2QueryPlan( String query,
                                         String url ) throws Exception {
        return postStream(new ByteArrayInputStream(query.getBytes()), url, "application/jcr+sql2");
    }

    protected Response jcrSQL2QueryPlanAsText( String query,
                                               String url ) throws Exception {
        return postStreamForTextResponse(new ByteArrayInputStream(query.getBytes()), url, "application/jcr+sql2");
    }

    protected Response postStream( InputStream is,
                                   String url,
                                   String contentType ) throws Exception {
        HttpPost post = newDefaultRequest(HttpPost.class, is, contentType, url);
        return new Response(post);
    }

    protected Response postStreamForTextResponse( InputStream is,
                                                  String url,
                                                  String contentType ) throws Exception {
        HttpPost post = newRequest(HttpPost.class, is, contentType, MediaType.TEXT_PLAIN, url);
        return new Response(post);
    }

    protected Response doPostMultiPart( String filePath,
                                        String elementName,
                                        String url,
                                        String contentType ) {
        try {

            if (StringUtil.isBlank(contentType)) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }

            url = URL_ENCODER.encode(RestHelper.urlFrom(getServerContext(), url));

            HttpPost post = new HttpPost(url);
            post.setHeader("Accept", MediaType.APPLICATION_JSON);
            MultipartEntity reqEntity = new MultipartEntity();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IoUtil.write(fileStream(filePath), baos);
            reqEntity.addPart(elementName, new ByteArrayBody(baos.toByteArray(), "test_file"));
            post.setEntity(reqEntity);

            return new Response(post);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    protected Response doPut( String payloadFile,
                              String url ) throws Exception {
        HttpPut put = newDefaultRequest(HttpPut.class, fileStream(payloadFile), MediaType.APPLICATION_JSON, url);
        return new Response(put);
    }

    protected Response doPut( InputStream is,
                              String url ) throws Exception {
        HttpPut put = newDefaultRequest(HttpPut.class, is, MediaType.APPLICATION_JSON, url);
        return new Response(put);
    }

    protected Response doPut( JSONObject request,
                              String url ) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream);
        request.write(writer);
        writer.flush();
        writer.close();
        HttpPut put = newDefaultRequest(HttpPut.class, new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),
                                        MediaType.APPLICATION_JSON, url);
        return new Response(put);
    }

    protected Response doDelete( String url ) throws Exception {
        HttpDeleteWithBody delete = newDefaultRequest(HttpDeleteWithBody.class, null, null, url);
        return new Response(delete);
    }

    protected Response doDelete( String payloadFile,
                                 String url ) throws Exception {
        InputStream is = null;
        if (payloadFile != null) {
            is = fileStream(payloadFile);
        }
        HttpDeleteWithBody delete = newDefaultRequest(HttpDeleteWithBody.class, is, MediaType.APPLICATION_JSON, url);
        return new Response(delete);
    }

    private <T extends HttpRequestBase> T newDefaultRequest( Class<T> clazz,
                                                             InputStream inputStream,
                                                             String contentType,
                                                             String... pathSegments ) {
        return newRequest(clazz, inputStream, contentType, MediaType.APPLICATION_JSON, pathSegments);
    }

    private <T extends HttpRequestBase> T newRequest( Class<T> clazz,
                                                      InputStream inputStream,
                                                      String contentType,
                                                      String accepts,
                                                      String... pathSegments ) {
        String url = RestHelper.urlFrom(getServerContext(), pathSegments);

        try {
            URIBuilder uriBuilder;
            try {
                uriBuilder = new URIBuilder(url);
            } catch (URISyntaxException e) {
                uriBuilder = new URIBuilder(URL_ENCODER.encode(url));
            }

            T result = clazz.getConstructor(URI.class).newInstance(uriBuilder.build());
            result.setHeader("Accept", accepts);
            result.setHeader("Content-Type", contentType);
            HttpParams params = result.getParams();
            for (NameValuePair nameValuePair : uriBuilder.getQueryParams()) {
                params.setParameter(nameValuePair.getName(), nameValuePair.getValue());
            }
            if (inputStream != null) {
                assertTrue("Invalid request clazz (requires an entity)", result instanceof HttpEntityEnclosingRequestBase);
                InputStreamEntity inputStreamEntity = new InputStreamEntity(inputStream, inputStream.available());
                ((HttpEntityEnclosingRequestBase)result).setEntity(new BufferedHttpEntity(inputStreamEntity));
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    protected void assertJSON( Object expected,
                               Object actual ) throws JSONException {
        if (expected instanceof JSONObject) {
            assert (actual instanceof JSONObject);
            JSONObject expectedJSON = (JSONObject)expected;
            JSONObject actualJSON = (JSONObject)actual;

            for (Iterator<?> keyIterator = expectedJSON.keys(); keyIterator.hasNext();) {
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
            Assert.assertEquals("Arrays don't match. \nExpected:" + expectedArray.toString() + "\nActual  :" + actualArray,
                                expectedArray.length(), actualArray.length());
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

    protected String getServerContext() {
        return SERVER_CONTEXT;
    }

    protected static class HttpDeleteWithBody extends HttpPost {
        public HttpDeleteWithBody( URI uri ) {
            super(uri);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }

    protected class Response {

        private final HttpResponse response;
        private byte[] content;
        private String contentString;

        protected Response( HttpRequestBase request ) {
            try {
                response = httpClient.execute(getHost(), request);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    ByteArrayOutputStream baous = new ByteArrayOutputStream();
                    entity.writeTo(baous);
                    EntityUtils.consumeQuietly(entity);
                    content = baous.toByteArray();
                } else {
                    content = new byte[0];
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                request.releaseConnection();
            }
        }

        private Response hasCode( int responseCode ) throws Exception {
            assertEquals(responseCode, response.getStatusLine().getStatusCode());
            return this;
        }

        private Response hasHeader( String name,
                                    String value ) {
            assertEquals(value, response.getFirstHeader(name).getValue());
            return this;
        }

        protected String getContentTypeHeader() {
            return response.getFirstHeader("Content-Type").getValue();
        }

        protected Response hasMimeType( String mimeType ) {
            hasHeader("Content-Type", mimeType);
            return this;
        }

        protected Response hasContentDisposition( String contentDisposition ) {
            hasHeader("Content-Disposition", contentDisposition);
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
            assertTrue(getContentTypeHeader().toLowerCase().contains(MediaType.APPLICATION_JSON.toLowerCase()));
            return this;
        }

        protected Response isJSONObjectLikeFile( String pathToExpectedJSON ) throws Exception {
            isJSON();
            String expectedJSONString = IoUtil.read(fileStream(pathToExpectedJSON));
            JSONObject expectedObject = new JSONObject(expectedJSONString);

            JSONObject responseObject = new JSONObject(contentAsString());

            try {
                assertJSON(expectedObject, responseObject);
            } catch (AssertionFailedError e) {
                System.out.println("expected: " + expectedObject);
                System.out.println("response: " + responseObject);
                throw e;
            }

            return this;
        }

        protected Response isJSONObjectLike( Response otherResponse ) throws Exception {
            isJSON();
            JSONObject expectedObject = otherResponse.json();

            JSONObject responseObject = new JSONObject(contentAsString());
            assertJSON(expectedObject, responseObject);

            return this;
        }

        protected Response isJSONArrayLikeFile( String pathToExpectedJSON ) throws Exception {
            isJSON();
            String expectedJSONString = IoUtil.read(fileStream(pathToExpectedJSON));
            JSONArray expectedArray = new JSONArray(expectedJSONString);

            JSONArray responseObject = new JSONArray(contentAsString());
            assertJSON(expectedArray, responseObject);

            return this;
        }

        protected String hasNodeIdentifier() throws Exception {
            JSONObject responseObject = new JSONObject(contentAsString());
            String id = responseObject.getString("id");
            assertNotNull(id);
            assertTrue(id.trim().length() != 0);
            return id;
        }

        protected JSONObject json() throws Exception {
            return new JSONObject(contentAsString());
        }

        protected JSONObject children() throws Exception {
            return json().getJSONObject(CHILDREN_KEY);
        }

        protected String contentAsString() {
            if (contentString == null) {
                contentString = new String(content);
            }
            return contentString;
        }

        protected byte[] contentAsBytes() {
            return content;
        }

        @Override
        public String toString() {
            return contentAsString();
        }
    }
}
