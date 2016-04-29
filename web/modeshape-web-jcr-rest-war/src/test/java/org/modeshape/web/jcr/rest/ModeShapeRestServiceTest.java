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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpHost;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.web.jcr.rest.form.FileUploadForm;
import org.modeshape.web.jcr.rest.handler.RestBinaryHandler;

/**
 * Unit test for the v2 version of the rest service: {@link ModeShapeRestService}
 *
 * @author Horia Chiorean
 */
public class ModeShapeRestServiceTest extends AbstractRestTest {

    private static final HttpHost HOST = new HttpHost("localhost", 8090, "http");
    private static final String SERVER_CONTEXT = "/resources";
    private static final String USERNAME = "dnauser";
    private static final String PASSWORD = "password";

    /**
     * The name of the repository is dictated by the "repository-config.json" configuration file loaded by the
     * "org.modeshape.jcr.JCR_URL" parameter in the "web.xml" file.
     */
    private static final String REPOSITORY_NAME = "repo";
    private static final String TEST_NODE = "testNode";
    private static final String CHILDREN_KEY = "children";
    private static final String ID_KEY = "id";

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        setAuthCredentials(USERNAME, PASSWORD);
    }

    @Override
    @After
    public void afterEach() throws Exception {
        doDelete(itemsUrl(TEST_NODE));
        super.afterEach();
    }

    @Override
    protected HttpHost getHost() {
        return HOST;
    }

    @Override
    protected String getServerContext() {
        return SERVER_CONTEXT;
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

    protected String uploadUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.UPLOAD_METHOD_NAME, additionalPathSegments);
    }

    protected String backupUrl() {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/" + RestHelper.BACKUP_METHOD_NAME);
    }

    protected String restoreUrl() {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/" + RestHelper.RESTORE_METHOD_NAME);
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
        doGet().isOk().isJSONObjectLikeFile("get/root.json");
    }

    @Test
    public void shouldServeListOfWorkspacesForValidRepository() throws Exception {
        doGet(REPOSITORY_NAME).isOk().isJSONObjectLikeFile("get/workspaces.json");
    }

    @Test
    public void shouldReturnErrorForInvalidRepository() throws Exception {
        doGet("XXX").isNotFound().isJSON();
    }

    @Test
    public void shouldRetrieveRootNodeForValidRepository() throws Exception {
        doGet(itemsUrl()).isOk().isJSONObjectLikeFile("get/root_node.json");
    }

    @Test
    public void shouldRetrieveRootNodeWhenDepthSet() throws Exception {
        doGet(itemsUrl() + "?depth=1").isOk().isJSONObjectLikeFile("get/root_node_depth1.json");
    }

    @Test
    public void shouldRetrieveSystemNodeWithDepthOne() throws Exception {
        doGet(itemsUrl("jcr:system") + "?depth=1").isOk().isJSONObjectLikeFile("get/system_node_depth1.json");
    }

    @Test
    public void shouldRetrieveNtBaseItems() throws Exception {
        doGet(itemsUrl("jcr:system/jcr:nodeTypes/nt:base")).isOk().isJSONObjectLikeFile("get/nt_base.json");
    }

    @Test
    public void shouldRetrieveNtBaseDepthFour() throws Exception {
        doGet(itemsUrl("jcr:system/jcr:nodeTypes/nt:base") + "?depth=4").isOk().isJSONObjectLikeFile("get/nt_base_depth4.json");
    }

    @Test
    public void shouldNotRetrieveNonExistentNode() throws Exception {
        doGet(itemsUrl("foo")).isNotFound().isJSON();
    }

    @Test
    public void shouldNotRetrieveNonExistentProperty() throws Exception {
        doGet(itemsUrl("jcr:system/foobar")).isNotFound().isJSON();
    }

    @Test
    public void shouldRetrieveProperty() throws Exception {
        doGet(itemsUrl("jcr:system/jcr:primaryType")).isOk().isJSONObjectLikeFile("get/system_primaryType_property.json");
    }

    @Test
    public void shouldPostNodeToValidPathWithPrimaryType() throws Exception {
        doPost("post/node_without_primaryType_request.json", itemsUrl(TEST_NODE)).isCreated()
                                                                                 .isJSONObjectLikeFile("post/node_without_primaryType_response.json");
    }

    @Test
    public void shouldPostNodeToValidPathWithoutPrimaryType() throws Exception {
        doPost("post/node_without_primaryType_request.json", itemsUrl(TEST_NODE)).isCreated()
                                                                                 .isJSONObjectLikeFile("post/node_without_primaryType_response.json");
    }

    @Test
    @FixFor( "MODE-1950" )
    public void shouldConvertValueTypesFromJSONPrimitives() throws Exception {
        doPost("post/node_different_property_types_request.json", itemsUrl(TEST_NODE)).isCreated()
                                                                                      .isJSONObjectLikeFile("post/node_different_property_types_response.json");
    }

    @Test
    public void shouldPostNodeToValidPathWithMixinTypes() throws Exception {
        doPost("post/node_with_mixin_request.json", itemsUrl(TEST_NODE)).isCreated()
                                                                        .isJSONObjectLikeFile("post/node_with_mixin_response.json");
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile("post/node_with_mixin_response.json");
    }

    @Test
    public void shouldNotPostNodeWithInvalidParentPath() throws Exception {
        doPost("post/node_without_primaryType_request.json", itemsUrl("foo/bar")).isNotFound();
    }

    @Test
    public void shouldNotPostNodeWithInvalidPrimaryType() throws Exception {
        doPost("post/node_invalid_primaryType_request.json", itemsUrl(TEST_NODE)).isNotFound();
        doGet(itemsUrl(TEST_NODE)).isNotFound();
    }

    @Test
    public void shouldPostNodeHierarchy() throws Exception {
        doPost("post/node_hierarchy_request.json", itemsUrl(TEST_NODE)).isCreated();

        // Make sure that we can retrieve the node with a GET
        doGet(itemsUrl(TEST_NODE) + "?depth=1").isOk().isJSONObjectLikeFile("post/node_hierarchy_response.json");
    }

    @Test
    public void shouldFailWholeTransactionIfOneNodeIsBad() throws Exception {
        doPost("post/node_hierarchy_invalidType_request.json", itemsUrl(TEST_NODE)).isNotFound();
        doGet(itemsUrl(TEST_NODE) + "?depth=1").isNotFound();
    }

    @Test
    public void shouldNotDeleteNonExistentItem() throws Exception {
        doDelete(itemsUrl("invalidItemForDelete")).isNotFound();
    }

    @Test
    public void shouldDeleteExistingNode() throws Exception {
        doPost("post/node_without_primaryType_request.json", itemsUrl(TEST_NODE)).isCreated();
        doGet(itemsUrl(TEST_NODE)).isOk();
        doDelete(itemsUrl(TEST_NODE)).isDeleted();
        doGet(itemsUrl(TEST_NODE)).isNotFound();
    }

    @Test
    public void shouldDeleteExistingProperty() throws Exception {
        doPost("put/node_with_property.json", itemsUrl(TEST_NODE)).isCreated();
        doGet(itemsUrl(TEST_NODE, "testProperty")).isOk();
        doDelete(itemsUrl(TEST_NODE, "testProperty")).isDeleted();
        doGet(itemsUrl(TEST_NODE, "testProperty")).isNotFound();
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile("put/node_without_property.json");
    }

    @Test
    public void shouldNotBeAbleToPutAtInvalidPath() throws Exception {
        doPut("put/property_edit.json", itemsUrl("nonexistantNode")).isNotFound();
    }

    @Test
    public void shouldBeAbleToPutValueToProperty() throws Exception {
        doPost("put/node_with_property.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/property_edit.json", itemsUrl(TEST_NODE, "testProperty")).isOk()
                                                                            .isJSONObjectLikeFile("put/property_after_edit.json");
    }

    @Test
    public void shouldBeAbleToPutBinaryValueToProperty() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/binary_property_edit.json", itemsUrl(TEST_NODE, "testProperty")).isOk();
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile("put/node_with_binary_property_after_edit.json");
    }

    @Test
    public void shouldBeAbleToPutPropertiesToNode() throws Exception {
        doPost("put/node_with_properties.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/properties_edit.json", itemsUrl(TEST_NODE)).isOk();
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile("put/node_with_properties_after_edit.json");
    }

    @Test
    public void shouldBeAbleToAddAndRemoveMixinTypes() throws Exception {
        doPost("put/node_with_properties.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/add_mixin.json", itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile("put/node_with_mixin.json");
        doPut("put/remove_mixins.json", itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile("put/node_without_mixins.json");
    }

    @Test
    @FixFor( "MODE-1950" )
    public void shouldBeAbleToRemoveMixinByRemovingProperties() throws Exception {
        doPost("put/publish_area.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/publish_area_invalid_update.json", itemsUrl(TEST_NODE)).isBadRequest();
        doPut("put/publish_area_valid_update.json", itemsUrl(TEST_NODE)).isOk()
                                                                        .isJSONObjectLikeFile("put/publish_area_response.json");
    }

    @Test
    public void shouldRetrieveDataFromXPathQuery() throws Exception {
        doPost("query/query_node.json", itemsUrl(TEST_NODE)).isCreated();
        xpathQuery("//" + TEST_NODE, queryUrl()).isOk().isJSON().isJSONObjectLikeFile("query/single_node_xpath.json");
    }

    @Test
    public void shouldRespectQueryOffsetAndLimit() throws Exception {
        String queryNodeFile = "query/query_node.json";

        doPost(queryNodeFile, itemsUrl(TEST_NODE)).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();

        String query = "//element(child) order by @foo, @jcr:path";
        xpathQuery(query, queryUrl() + "?offset=1&limit=2").isOk().isJSON()
                                                           .isJSONObjectLikeFile("query/query_result_offset_and_limit.json");
    }

    @Test
    public void shouldAllowJcrSql2Query() throws Exception {
        String queryNodeFile = "query/query_node.json";

        doPost(queryNodeFile, itemsUrl(TEST_NODE)).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();
        doPost(queryNodeFile, itemsUrl(TEST_NODE, "child")).isCreated();

        String query = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/" + TEST_NODE + "') ORDER BY [jcr:path]";
        jcrSQL2Query(query, queryUrl()).isOk().isJSON().isJSONObjectLikeFile("query/query_result_jcrSql2.json");
    }

    @Test
    public void shouldRetrieveBinaryPropertyValue() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();
        Response response = doGet(binaryUrl(TEST_NODE, "testProperty")).isOk()
                                                                       .hasMimeType(MediaType.TEXT_PLAIN)
                                                                       .hasContentDisposition(RestBinaryHandler.DEFAULT_CONTENT_DISPOSITION_PREFIX
                                                                                              + TEST_NODE);
        assertEquals("testValue", response.contentAsString());
    }

    @Test
    public void shouldRetrieveBinaryPropertyValueWithMimeTypeAndContentDisposition() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();
        String mimeType = MediaType.TEXT_XML;
        String contentDisposition = "inline;filename=TestFile.txt";
        String urlParams = "?mimeType=" + RestHelper.URL_ENCODER.encode(mimeType) + "&contentDisposition="
                           + RestHelper.URL_ENCODER.encode(contentDisposition);
        doGet(binaryUrl(TEST_NODE, "testProperty") + urlParams).isOk().hasMimeType(mimeType)
                                                               .hasContentDisposition(contentDisposition);
    }

    @Test
    public void shouldReturnNotFoundForInvalidBinaryProperty() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();
        doGet(binaryUrl(TEST_NODE, "invalid")).isNotFound();
    }

    @Test
    public void shouldReturnNotFoundForNonBinaryProperty() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();
        doGet(binaryUrl(TEST_NODE, "jcr:primaryType")).isNotFound();
    }

    @Test
    public void shouldCreateBinaryProperty() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost(fileStream("post/binary.pdf"), binaryUrl(TEST_NODE, "testProperty")).isCreated()
                                                                                   .isJSONObjectLikeFile("post/new_binary_property_response.json");
        Response response = doGet(binaryUrl(TEST_NODE, "testProperty")).isOk();

        byte[] expectedBinaryContent = IoUtil.readBytes(fileStream("post/binary.pdf"));
        assertArrayEquals(expectedBinaryContent, response.contentAsBytes());
    }

    @Test
    public void shouldUpdateBinaryPropertyViaPost() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();

        String otherBinaryValue = String.valueOf(System.currentTimeMillis());
        doPost(new ByteArrayInputStream(otherBinaryValue.getBytes()), binaryUrl(TEST_NODE, "testProperty")).isOk();

        Response response = doGet(binaryUrl(TEST_NODE, "testProperty")).isOk();
        assertEquals(otherBinaryValue, response.contentAsString());
    }

    @Test
    public void shouldUpdateBinaryPropertyViaPut() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();

        String otherBinaryValue = String.valueOf(System.currentTimeMillis());
        doPut(new ByteArrayInputStream(otherBinaryValue.getBytes()), binaryUrl(TEST_NODE, "testProperty")).isOk();

        Response response = doGet(binaryUrl(TEST_NODE, "testProperty")).isOk();
        assertEquals(otherBinaryValue, response.contentAsString());
    }

    @Test
    public void shouldNotUpdateNonExistingBinaryPropertyViaPut() throws Exception {
        doPost("put/node_with_binary_property.json", itemsUrl(TEST_NODE)).isCreated();

        String otherBinaryValue = String.valueOf(System.currentTimeMillis());
        doPut(new ByteArrayInputStream(otherBinaryValue.getBytes()), binaryUrl(TEST_NODE, "invalidProp")).isNotFound();
    }

    @Test
    public void shouldCreateBinaryValueViaMultiPartRequest() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPostMultiPart("post/binary.pdf", FileUploadForm.PARAM_NAME, binaryUrl(TEST_NODE, "testProperty"),
                        MediaType.APPLICATION_OCTET_STREAM).isCreated()
                                                           .isJSONObjectLikeFile("post/new_binary_property_response.json");
    }

    @Test
    public void shouldRetrieveNtBaseNodeType() throws Exception {
        JSONObject object = doGet(nodeTypesUrl("nt:base")).isOk().json();
        assertTrue(object.has("nt:base"));
    }

    @Test
    public void shouldReturnNotFoundIfNodeTypeMissing() throws Exception {
        doGet(nodeTypesUrl("invalidNodeType")).isNotFound();
    }

    @Test
    public void shouldImportCNDFile() throws Exception {
        String response = doPost(fileStream("post/node_types.cnd"), nodeTypesUrl()).isOk().contentAsString();
        assertCNDImport(response);
    }

    @Test
    public void shouldImportCNDFileViaMultiPartRequest() throws Exception {
        String response = doPostMultiPart("post/node_types.cnd", FileUploadForm.PARAM_NAME, nodeTypesUrl(), MediaType.TEXT_PLAIN)
                .isOk().contentAsString();
        assertNotNull(response);
        assertCNDImport(response);
    }

    private void assertCNDImport( String response ) throws JSONException {
        JSONArray array = new JSONArray(response);
        assertEquals(3, array.length());
        assertEquals("nt:base", array.getJSONObject(0).keys().next().toString());
        assertEquals("nt:unstructured", array.getJSONObject(1).keys().next().toString());
        assertEquals("mix:created", array.getJSONObject(2).keys().next().toString());
    }

    @Test
    public void importingCNDViaWrongHTMLElementNameShouldFail() throws Exception {
        doPostMultiPart("post/node_types.cnd", "invalidHTML", nodeTypesUrl(), MediaType.TEXT_PLAIN).isBadRequest();
    }

    @Test
    public void shouldCreateMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("post/multiple_nodes_request.json", itemsUrl()).isOk().isJSONArrayLikeFile("post/multiple_nodes_response.json");
        doGet(itemsUrl(TEST_NODE + "/child[2]")).isJSONObjectLikeFile("get/node_with_sns_children.json");
    }

    @Test
    public void shouldEditMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("post/multiple_nodes_request.json", itemsUrl()).isOk();
        doPut("put/multiple_nodes_edit_request.json", itemsUrl()).isOk()
                                                                 .isJSONArrayLikeFile("put/multiple_nodes_edit_response.json");
        // System.out.println("*****  GET: \n" + doGet(itemsUrl(TEST_NODE)));
    }

    @Test
    public void shouldDeleteMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("post/multiple_nodes_request.json", itemsUrl()).isOk();
        doDelete("delete/multiple_nodes_delete.json", itemsUrl()).isOk();
    }

    private String binaryUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.BINARY_METHOD_NAME, additionalPathSegments);
    }

    private String nodeTypesUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.NODE_TYPES_METHOD_NAME, additionalPathSegments);
    }

    @Test
    public void shouldGetNodeByIdentifier() throws Exception {
        // Create the node using the path (there is no ID-based option to do this) ...
        Response response = doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        String id = response.hasNodeIdentifier();
        // Get by ID ...
        response = doGet(nodesUrl(id)).isJSONObjectLike(response);
        // System.out.println("**** GET-BY-ID: \n" + response);

        // Update by ID ...
        response = doPut("put/node_with_binary_property.json", nodesUrl(id)).isOk()
                                                                            .isJSONObjectLikeFile("put/node_with_binary_property_after_edit.json");
        // System.out.println("**** GET-BY-ID: \n" + response);

        // Delete by ID ...
        response = doDelete(nodesUrl(id)).isDeleted();
        // System.out.println("**** GET-BY-ID: \n" + response);
    }

    @Test
    @FixFor( "MODE-1816" )
    public void shouldAllowPostingNodeWithMixinAndPrimaryTypesPropertiesAfterProperties() throws Exception {
        doPost("post/node_with_mixin_after_props_request.json", itemsUrl(TEST_NODE)).isCreated()
                                                                                    .isJSONObjectLikeFile("post/node_with_mixin_after_props_response.json");
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile("post/node_with_mixin_after_props_response.json");
    }

    @Test
    @FixFor( "MODE-1901" )
    public void shouldComputePlainTextPlanForJcrSql2Query() throws Exception {
        // No need to create any data, since we are not executing the query ...
        String query = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/" + TEST_NODE + "')";
        Response response = jcrSQL2QueryPlanAsText(query, queryPlanUrl()).isOk();
        assertThat(response.getContentTypeHeader().toLowerCase().startsWith("text/plain;"), is(true));
        String plan = response.contentAsString();
        assertThat(plan, is(notNullValue()));
        // System.out.println("**** PLAN: \n" + plan);
    }

    @Test
    @FixFor( "MODE-1901" )
    public void shouldComputeJsonPlanForJcrSql2Query() throws Exception {
        // No need to create any data, since we are not executing the query ...
        String query = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/" + TEST_NODE + "')";
        Response response = jcrSQL2QueryPlan(query, queryPlanUrl()).isOk();
        assertThat(response.getContentTypeHeader().startsWith("application/json"), is(true));
        String plan = response.contentAsString();
        assertThat(plan, is(notNullValue()));
        // System.out.println("**** PLAN: \n" + plan);
    }

    protected String queryPlanUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.QUERY_PLAN_METHOD_NAME, additionalPathSegments);
    }

    @Test
    @FixFor( "MODE-2048" )
    public void shouldAllowChildrenUpdateViaID() throws Exception {
        /**
         * testNode - child1 [prop="child1"] - child2 [prop="child2"] - child3 [prop="child3"]
         */
        JSONObject nodeWithHierarchyRequest = readJson("post/node_multiple_children_request.json");

        // replace in the original request node paths with IDs
        JSONObject children = doPost(nodeWithHierarchyRequest, itemsUrl(TEST_NODE)).isCreated().json()
                                                                                   .getJSONObject(CHILDREN_KEY);
        String child1Id = children.getJSONObject("child1").getString(ID_KEY);
        String child2Id = children.getJSONObject("child2").getString(ID_KEY);
        String child3Id = children.getJSONObject("child3").getString(ID_KEY);

        children = nodeWithHierarchyRequest.getJSONObject(CHILDREN_KEY);
        JSONObject child1 = children.getJSONObject("child1");
        child1.put("update", true);
        children.put(child1Id, child1);
        children.remove("child1");

        JSONObject child2 = children.getJSONObject("child2");
        child2.put("update", true);
        children.put(child2Id, child2);
        children.remove("child2");

        JSONObject child3 = children.getJSONObject("child3");
        child3.put("update", true);
        children.put(child3Id, child3);
        children.remove("child3");

        doPut(nodeWithHierarchyRequest, itemsUrl(TEST_NODE)).isOk();
        assertTrue(doGet(itemsUrl(TEST_NODE, "child1")).json().has("update"));
        assertTrue(doGet(itemsUrl(TEST_NODE, "child2")).json().has("update"));
        assertTrue(doGet(itemsUrl(TEST_NODE, "child3")).json().has("update"));
    }

    @Test
    @FixFor( "MODE-2048" )
    public void shouldPerformChildReordering() throws Exception {
        /**
         * testNode - child1 - child2 - child3
         */
        doPost("post/node_multiple_children_request.json", itemsUrl(TEST_NODE)).isCreated();

        /**
         * testNode - child3 - child2 - child1
         */
        JSONObject children = doPut("put/node_multiple_children_reorder1.json", itemsUrl(TEST_NODE)).isOk().json()
                                                                                                    .getJSONObject(CHILDREN_KEY);

        List<String> actualOrder = new ArrayList<String>();
        for (Iterator<?> iterator = children.keys(); iterator.hasNext();) {
            actualOrder.add(iterator.next().toString());
        }
        assertEquals("Invalid child order", Arrays.asList("child3", "child2", "child1"), actualOrder);

        /**
         * testNode - child2 - child3 - child1
         */
        children = doPut("put/node_multiple_children_reorder2.json", itemsUrl(TEST_NODE)).isOk().json()
                                                                                         .getJSONObject(CHILDREN_KEY);
        actualOrder = new ArrayList<String>();
        for (Iterator<?> iterator = children.keys(); iterator.hasNext();) {
            actualOrder.add(iterator.next().toString());
        }
        assertEquals("Invalid child order", Arrays.asList("child2", "child3", "child1"), actualOrder);
    }

    @Test
    @FixFor( "MODE-2048" )
    public void shouldMoveNode() throws Exception {
        /**
         * node1 - child1 - child2 - child3
         */
        try {
            JSONObject children = doPost("post/node_multiple_children_request.json", itemsUrl("node1")).isCreated()
                                                                                                       .json()
                                                                                                       .getJSONObject(CHILDREN_KEY);
            String child2Id = children.getJSONObject("child2").getString(ID_KEY);

            /**
             * node2 - childNode
             */
            JSONObject request = readJson("post/node_hierarchy_request.json");
            doPost(request, itemsUrl("node2")).isCreated();

            JSONObject requestChildren = request.getJSONObject(CHILDREN_KEY);
            request.remove("childNode");
            requestChildren.put(child2Id, Collections.emptyMap());

            // move node1/child2 to node2
            doPut(request, itemsUrl("node2")).isOk();
            assertTrue(doGet(itemsUrl("node2")).json().getJSONObject(CHILDREN_KEY).has("child2"));
            assertFalse(doGet(itemsUrl("node1")).json().getJSONObject(CHILDREN_KEY).has("child2"));
        } finally {
            doDelete(itemsUrl("node1")).isDeleted();
            doDelete(itemsUrl("node2")).isDeleted();
        }
    }

    @Test
    @FixFor( "MODE-2056" )
    public void shouldCloseActiveSessions() throws Exception {
        // execute 3 requests
        doPost("post/node_multiple_children_request.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("post/node_multiple_children_request.json", itemsUrl(TEST_NODE)).isOk();
        doGet(itemsUrl(TEST_NODE));

        JSONObject repositories = doGet("/").isOk().json();
        JSONObject repository = repositories.getJSONArray("repositories").getJSONObject(0);
        int activeSessionsCount = repository.getInt("activeSessionsCount");

        assertEquals("There are active sessions in the repository", 0, activeSessionsCount);
    }

    @Test
    @FixFor( "MODE-2170" )
    public void shouldAllowUpdatingMultivaluedProperty() throws Exception {
        doPost("post/node_multivalue_prop_request.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/node_multivalue_prop_request.json", itemsUrl(TEST_NODE)).isOk()
                                                                           .isJSONObjectLikeFile("put/node_multivalue_prop_response.json");

    }

    @Test
    @FixFor( "MODE-2181" )
    public void shouldAllowCreatingSNS() throws Exception {
        doPost("post/node_with_sns_request.json", itemsUrl(TEST_NODE)).isCreated();
        doGet(itemsUrl(TEST_NODE, "foo[1]")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]/name")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]/name")).isOk();
    }

    @Test
    @FixFor( "MODE-2181" )
    public void shouldAllowUpdatingSNSViaArray() throws Exception {
        doPost("post/node_with_sns_request.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/node_with_sns_edit_request.json", itemsUrl(TEST_NODE)).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]/name")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]/editedName")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]/name")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]/editedName")).isOk();
    }

    @Test
    @FixFor( "MODE-2181" )
    public void shouldAllowUpdatingSNSViaObject() throws Exception {
        doPost("post/node_with_sns_request.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("put/node_with_sns_edit_alt_request.json", itemsUrl(TEST_NODE)).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]/name")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]/editedName")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]/name")).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[2]/editedName")).isOk();
    }

    @Test
    @FixFor( "MODE-2181" )
    public void shouldAllowDeletingSNS() throws Exception {
        doPost("post/node_with_sns_request.json", itemsUrl(TEST_NODE)).isCreated();
        doDelete("delete/sns_nodes_delete.json", itemsUrl()).isOk();
        doGet(itemsUrl(TEST_NODE, "foo[1]")).isNotFound();
        doGet(itemsUrl(TEST_NODE, "foo[2]")).isNotFound();
    }

    @Test
    @FixFor( "MODE-2182" )
    public void shouldUploadFileUnderRootUsingStream() throws Exception {
        try {
            assertUpload("testFile1", true, false);
        } catch (Exception e) {
            doDelete(itemsUrl("testFile1")).isDeleted();
        }
    }

    @Test
    @FixFor( "MODE-2182" )
    public void shouldUploadFileUnderRootUsingForm() throws Exception {
        try {
            assertUpload("testFile2", true, true);
        } finally {
            doDelete(itemsUrl("testFile2")).isDeleted();
        }
    }

    @Test
    @FixFor( "MODE-2182" )
    public void shouldUploadFileAndCreateHierarchy() throws Exception {
        try {
            assertUpload("node1/node2/file", true, false);
            doGet(itemsUrl("node1")).isOk().hasPrimaryType("nt:folder");
            doGet(itemsUrl("node1/node2")).isOk().hasPrimaryType("nt:folder");
            doGet(itemsUrl("node1/node2/file")).isOk().hasPrimaryType("nt:file");
        } finally {
            doDelete(itemsUrl("node1"));
        }
    }

    @Test
    @FixFor( "MODE-2182" )
    public void shouldUploadFileAndCreatePartialHierarchy() throws Exception {
        try {
            doPost("post/folder.json", itemsUrl("node1")).isCreated().hasPrimaryType("nt:folder");
            doPost("post/folder.json", itemsUrl("node1/node2")).isCreated().hasPrimaryType("nt:folder");
            assertUpload("node1/node2/file", true, false);
            doGet(itemsUrl("node1/node2/file")).isOk().hasPrimaryType("nt:file");
        } finally {
            doDelete(itemsUrl("node1"));
        }
    }

    @Test
    @FixFor( "MODE-2182" )
    public void shouldModifyExistingFileViaUpload() throws Exception {
        try {
            doPost("post/folder.json", itemsUrl("node1")).isCreated().hasPrimaryType("nt:folder");
            doPost("post/folder.json", itemsUrl("node1/node2")).isCreated().hasPrimaryType("nt:folder");
            doPost("post/file.json", itemsUrl("node1/node2/file")).isCreated().hasPrimaryType("nt:file");
            assertUpload("node1/node2/file", false, false);
        } finally {
            doDelete(itemsUrl("node1"));
        }
    }

    @Test
    @FixFor( "MODE-2182" )
    public void shouldNotAllowUploadIfTypeInvalid() throws Exception {
        try {
            doPost("post/folder.json", itemsUrl("node1")).isCreated().hasPrimaryType("nt:folder");
            doPost(fileStream("post/binary.pdf"), uploadUrl("node1")).isBadRequest();
        } finally {
            doDelete(itemsUrl("node1"));
        }
    }

    @Test
    @FixFor( "MODE-2250" )
    public void shouldReturnAllSNSForNegativeDepth() throws Exception {
        doPost("post/node_with_nested_sns_request.json", itemsUrl(TEST_NODE)).isCreated();
        JSONObject children = doGet(itemsUrl(TEST_NODE) + "?depth=-1").isOk().json().getJSONObject(CHILDREN_KEY);
        JSONObject foo1 = children.getJSONObject("foo");
        assertNotNull(foo1);
        JSONObject foo1Children = foo1.getJSONObject("children");
        assertNotNull(foo1Children);
        assertTrue(foo1Children.has("bar"));
        assertTrue(foo1Children.has("bar[2]"));

        JSONObject foo2 = children.getJSONObject("foo[2]");
        assertNotNull(foo2);
        JSONObject foo2Children = foo1.getJSONObject("children");
        assertNotNull(foo2Children);
        assertTrue(foo2Children.has("bar"));
        assertTrue(foo2Children.has("bar[2]"));
    }

    @Test
    @FixFor( "MODE-2261" )
    public void shouldRunQueryWithMultipleSelectors() throws Exception {
        doPost("post/node_with_nested_sns_request.json", itemsUrl(TEST_NODE)).isCreated();
        String query = "SELECT parent.[jcr:path], child.* FROM [nt:unstructured] as parent INNER JOIN [nt:unstructured] as child "
                       + "ON ISCHILDNODE(parent, child) WHERE parent.[jcr:path] LIKE '/" + TEST_NODE + "/%'";
        jcrSQL2Query(query, queryUrl()).isOk();
    }


    @Test
    @FixFor( "MODE-2452")
    public void shouldPerformRepositoryBackup() throws Exception {
        // create a node with a binary property
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPostMultiPart("v2/post/binary.pdf",
                        "file",
                        binaryUrl(TEST_NODE, "testProperty"),
                        MediaType.APPLICATION_OCTET_STREAM).isCreated();

        // now backup with default options
        JSONObject response = doPost((String)null, backupUrl()).isCreated().json();
        assertNotNull(response.getString("name"));
        String backupURL = response.getString("url");
        assertNotNull(backupURL);

        File backupFolderDefault  = new File(new URI(backupURL));
        assertTrue(backupFolderDefault.exists() && backupFolderDefault.isDirectory());
        String[] backupContentDefault = backupFolderDefault.list();
        assertTrue(backupContentDefault.length > 0);

        // now backup with custom options
        response = doPost((String)null, backupUrl() + "?includeBinaries=false&compress=false&documentsPerFile=12&batchSize=100").isCreated().json();
        assertNotNull(response.getString("name"));
        backupURL = response.getString("url");
        assertNotNull(backupURL);
        File backupFolderCustom  = new File(new URI(backupURL));
        assertTrue(backupFolderCustom.exists() && backupFolderCustom.isDirectory());
        String[] backupContentCustom = backupFolderCustom.list();
        assertTrue(backupFolderCustom.list().length > 0);
        assertTrue(backupContentDefault.length != backupContentCustom.length);

        FileUtil.delete(backupFolderDefault);
        FileUtil.delete(backupFolderCustom);
    }

    @Test
    @FixFor( "MODE-2452")
    public void shouldPerformRepositoryRestore() throws Exception {
        // create a node with a binary property
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPostMultiPart("v2/post/binary.pdf",
                        "file",
                        binaryUrl(TEST_NODE, "testProperty"),
                        MediaType.APPLICATION_OCTET_STREAM).isCreated();

        // now backup with default options
        JSONObject response = doPost((String)null, backupUrl()).isCreated().json();
        String backupName = response.getString("name");
        String backupURL = response.getString("url");
        // create a new node
        doPost((String)null, itemsUrl(TEST_NODE, "child")).isCreated();

        // restore with an invalid name
        doPost((String)null, restoreUrl() + "?name=invalid").isBadRequest();

        // now restore with default options
        doPost((String)null, restoreUrl() + "?name=" + backupName).isOk();

        // now restore with custom options
        doPost((String)null, restoreUrl() + "?name=" + backupName + "&includeBinaries=false&reindexContent=false&batchSize=10").isOk();

        FileUtil.delete(new File(new URI(backupURL)));
    }


    @Test
    @FixFor( "MODE-2594" )
    public void shouldReturnAllValuesWhenQueryingMultiValuedProperty() throws Exception {
        doPost("post/node_multivalue_prop_request.json", itemsUrl(TEST_NODE)).isCreated();
        String query = "SELECT property FROM [nt:unstructured] WHERE [jcr:path] = '/" + TEST_NODE + "'";
        JSONObject result = jcrSQL2Query(query, queryUrl()).isOk().json();
        JSONArray rows = result.getJSONArray("rows");
        assertEquals(1, rows.length());
        JSONObject row = rows.getJSONObject(0);
        JSONArray values = row.getJSONArray("property");
        assertEquals("[\"value1\",\"value2\",\"value3\"]", values.toString());
    }


    private void assertUpload( String url,
                               boolean expectCreated,
                               boolean useMultiPart ) throws Exception {
        Response response = useMultiPart ? doPostMultiPart("post/binary.pdf", FileUploadForm.PARAM_NAME, uploadUrl(url),
                                                           MediaType.APPLICATION_OCTET_STREAM) : doPost(fileStream("post/binary.pdf"),
                                                                                                        uploadUrl(url));
        if (expectCreated) {
            response.isCreated();
        } else {
            response.isOk();
        }
        String binaryPropertyRequest = response.json().getString("jcr:data");
        binaryPropertyRequest = binaryPropertyRequest.substring(binaryPropertyRequest.indexOf(getServerContext())
                                                                + getServerContext().length());
        byte[] uploaded = doGet(binaryPropertyRequest).isOk().contentAsBytes();
        assertArrayEquals(IoUtil.readBytes(fileStream("post/binary.pdf")), uploaded);
    }
}
