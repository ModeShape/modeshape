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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.IoUtil;
import org.modeshape.web.jcr.rest.handler.RestBinaryHandler;

/**
 * Unit test for the v2 version of the rest service: {@link ModeShapeRestService}
 * 
 * @author Horia Chiorean
 */
public class ModeShapeRestServiceTest extends JcrResourcesTest {

    @Override
    protected String getServerContext() {
        return "/resources";
    }

    @Override
    protected String contentRoot() {
        return "v2/get/root.json";
    }

    @Override
    protected String workspaces() {
        return "v2/get/workspaces.json";
    }

    @Override
    protected String rootNode() {
        return "v2/get/root_node.json";
    }

    @Override
    protected String rootNodeDepthOne() {
        return "v2/get/root_node_depth1.json";
    }

    @Override
    protected String systemNodeDepthOne() {
        return "v2/get/system_node_depth1.json";
    }

    @Override
    protected String ntBaseNodeType() {
        return "v2/get/nt_base.json";
    }

    @Override
    protected String ntBaseDepthFour() {
        return "v2/get/nt_base_depth4.json";
    }

    @Override
    protected String systemPrimaryTypeProperty() throws Exception {
        return "v2/get/system_primaryType_property.json";
    }

    @Override
    protected String nodeWithoutPrimaryTypeRequest() {
        return "v2/post/node_without_primaryType_request.json";
    }

    @Override
    protected String nodeWithoutPrimaryTypeResponse() {
        return "v2/post/node_without_primaryType_response.json";
    }

    @Override
    protected String differentPropertyTypesRequest() {
        return "v2/post/node_different_property_types_request.json";
    }

    @Override
    protected String differentPropertyTypesResponse() {
        return "v2/post/node_different_property_types_response.json";
    }

    @Override
    protected String nodeWithPrimaryTypeRequest() {
        return "v2/post/node_without_primaryType_request.json";
    }

    @Override
    protected String nodeWithPrimaryTypeResponse() {
        return "v2/post/node_without_primaryType_response.json";
    }

    @Override
    protected String nodeWithMixinRequest() {
        return "v2/post/node_with_mixin_request.json";
    }

    @Override
    protected String nodeWithMixinResponse() {
        return "v2/post/node_with_mixin_response.json";
    }

    @Override
    protected String nodeInvalidPrimaryTypeRequest() {
        return "v2/post/node_invalid_primaryType_request.json";
    }

    @Override
    protected String nodeHierarchyRequest() {
        return "v2/post/node_hierarchy_request.json";
    }

    @Override
    protected String nodeHierarchyResponse() {
        return "v2/post/node_hierarchy_response.json";
    }

    @Override
    protected String nodeHierarchyInvalidTypeRequest() {
        return "v2/post/node_hierarchy_invalidType_request.json";
    }

    @Override
    protected String addMixin() {
        return "v2/put/add_mixin.json";
    }

    @Override
    protected String removeMixins() {
        return "v2/put/remove_mixins.json";
    }

    @Override
    protected String nodeWithMixin() {
        return "v2/put/node_with_mixin.json";
    }

    @Override
    protected String nodeWithoutMixins() {
        return "v2/put/node_without_mixins.json";
    }

    @Override
    protected String binaryPropertyEdit() {
        return "v2/put/binary_property_edit.json";
    }

    @Override
    protected String nodeWithBinaryProperty() {
        return "v2/put/node_with_binary_property.json";
    }

    @Override
    protected String binaryPropertyName() {
        return "testProperty";
    }

    @Override
    protected String nodeBinaryPropertyAfterEdit() {
        return "v2/put/node_with_binary_property_after_edit.json";
    }

    @Override
    protected String nodeWithProperty() {
        return "v2/put/node_with_property.json";
    }

    @Override
    protected String nodeWithoutProperty() {
        return "v2/put/node_without_property.json";
    }

    @Override
    protected String propertyEdit() {
        return "v2/put/property_edit.json";
    }

    @Override
    protected String nodeWithPropertyAfterEdit() {
        return "v2/put/property_after_edit.json";
    }

    @Override
    protected String nodeWithProperties() {
        return "v2/put/node_with_properties.json";
    }

    @Override
    protected String nodeWithPropertiesAfterEdit() {
        return "v2/put/node_with_properties_after_edit.json";
    }

    @Override
    protected String propertiesEdit() {
        return "v2/put/properties_edit.json";
    }

    @Override
    protected String publishArea() {
        return "v2/put/publish_area.json";
    }

    @Override
    protected String publishAreaInvalidUpdate() {
        return "v2/put/publish_area_invalid_update.json";
    }

    @Override
    protected String publishAreaResponse() {
        return "v2/put/publish_area_response.json";
    }

    @Override
    protected String publishAreaValidUpdate() {
        return "v2/put/publish_area_valid_update.json";
    }

    @Override
    protected String queryNode() {
        return "v2/query/query_node.json";
    }

    @Override
    protected String singleNodeXPath() {
        return "v2/query/single_node_xpath.json";
    }

    @Override
    protected String queryResultOffsetAndLimit() {
        return "v2/query/query_result_offset_and_limit.json";
    }

    @Override
    protected String jcrSQL2Result() {
        return "v2/query/query_result_jcrSql2.json";
    }

    protected String nodeWithMixinAfterPropsRequest() {
        return "v2/post/node_with_mixin_after_props_request.json";
    }

    protected String nodeWithMixinAfterPropsResponse() {
        return "v2/post/node_with_mixin_after_props_response.json";
    }

    @Override
    @Ignore( "Ignored because the deprecated parameter isn't supported anymore" )
    public void shouldRetrieveRootNodeWhenDeprecatedDepthSet() throws Exception {
    }

    @Test
    public void shouldRetrieveBinaryPropertyValue() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doGet(binaryUrl(TEST_NODE, binaryPropertyName())).isOk()
                                                         .hasMimeType(MediaType.TEXT_PLAIN)
                                                         .hasContentDisposition(RestBinaryHandler.DEFAULT_CONTENT_DISPOSITION_PREFIX
                                                                                + TEST_NODE)
                                                         .copyInputStream(bos);
        assertTrue(bos.size() > 0);
        assertEquals("testValue", new String(bos.toByteArray()));
    }

    private String newBinaryProperty() {
        return "v2/post/new_binary_property_response.json";
    }

    @Test
    public void shouldRetrieveBinaryPropertyValueWithMimeTypeAndContentDisposition() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();
        String mimeType = MediaType.TEXT_XML;
        String contentDisposition = "inline;filename=TestFile.txt";
        String urlParams = "?mimeType=" + RestHelper.URL_ENCODER.encode(mimeType) + "&contentDisposition="
                           + RestHelper.URL_ENCODER.encode(contentDisposition);
        doGet(binaryUrl(TEST_NODE, binaryPropertyName()) + urlParams).isOk()
                                                                     .hasMimeType(mimeType)
                                                                     .hasContentDisposition(contentDisposition);
    }

    @Test
    public void shouldReturnNotFoundForInvalidBinaryProperty() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();
        doGet(binaryUrl(TEST_NODE, "invalid")).isNotFound();
    }

    @Test
    public void shouldReturnNotFoundForNonBinaryProperty() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();
        doGet(binaryUrl(TEST_NODE, "jcr:primaryType")).isNotFound();
    }

    @Test
    public void shouldCreateBinaryProperty() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost(fileStream("v2/post/binary.pdf"), binaryUrl(TEST_NODE, binaryPropertyName())).isCreated()
                                                                                            .isJSONObjectLikeFile(newBinaryProperty());
        ByteArrayOutputStream actualBinaryContent = new ByteArrayOutputStream();
        doGet(binaryUrl(TEST_NODE, binaryPropertyName())).isOk().copyInputStream(actualBinaryContent);

        byte[] expectedBinaryContent = IoUtil.readBytes(fileStream("v2/post/binary.pdf"));
        assertArrayEquals(expectedBinaryContent, actualBinaryContent.toByteArray());
    }

    @Test
    public void shouldUpdateBinaryPropertyViaPost() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();

        String otherBinaryValue = String.valueOf(System.currentTimeMillis());
        doPost(new ByteArrayInputStream(otherBinaryValue.getBytes()), binaryUrl(TEST_NODE, binaryPropertyName())).isOk();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doGet(binaryUrl(TEST_NODE, binaryPropertyName())).isOk().copyInputStream(bos);
        assertEquals(otherBinaryValue, new String(bos.toByteArray()));
    }

    @Test
    public void shouldUpdateBinaryPropertyViaPut() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();

        String otherBinaryValue = String.valueOf(System.currentTimeMillis());
        doPut(new ByteArrayInputStream(otherBinaryValue.getBytes()), binaryUrl(TEST_NODE, binaryPropertyName())).isOk();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doGet(binaryUrl(TEST_NODE, binaryPropertyName())).isOk().copyInputStream(bos);
        assertEquals(otherBinaryValue, new String(bos.toByteArray()));
    }

    @Test
    public void shouldNotUpdateNonExistingBinaryPropertyViaPut() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();

        String otherBinaryValue = String.valueOf(System.currentTimeMillis());
        doPut(new ByteArrayInputStream(otherBinaryValue.getBytes()), binaryUrl(TEST_NODE, "invalidProp")).isNotFound();
    }

    @Test
    public void shouldCreateBinaryValueViaMultiPartRequest() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPostMultiPart(fileStream("v2/post/binary.pdf"),
                        "file",
                        binaryUrl(TEST_NODE, binaryPropertyName()),
                        MediaType.APPLICATION_OCTET_STREAM).isCreated().isJSONObjectLikeFile(newBinaryProperty());
    }

    @Test
    public void shouldRetrieveNtBaseNodeType() throws Exception {
        doGet(nodeTypesUrl("nt:base")).isOk().isJSONObjectLikeFile("v2/get/nt_base_nodeType_response.json");
    }

    @Test
    public void shouldReturnNotFoundIfNodeTypeMissing() throws Exception {
        doGet(nodeTypesUrl("invalidNodeType")).isNotFound();
    }

    @Test
    public void shouldImportCNDFile() throws Exception {
        doPost(fileStream(nodeTypesCND()), nodeTypesUrl()).isOk().isJSONArrayLikeFile(cndImportResponse());
    }

    private String cndImportResponse() {
        return "v2/post/cnd_import_response.json";
    }

    private String nodeTypesCND() {
        return "v2/post/node_types.cnd";
    }

    @Test
    public void shouldImportCNDFileViaMultiPartRequest() throws Exception {
        doPostMultiPart(fileStream(nodeTypesCND()), "file", nodeTypesUrl(), MediaType.TEXT_PLAIN).isOk()
                                                                                                 .isJSONArrayLikeFile(cndImportResponse());
    }

    @Test
    public void importingCNDViaWrongHTMLElementNameShouldFail() throws Exception {
        doPostMultiPart(fileStream(nodeTypesCND()), "invalidHTML", nodeTypesUrl(), MediaType.TEXT_PLAIN).isBadRequest();
    }

    @Test
    public void shouldCreateMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("v2/post/multiple_nodes_request.json", itemsUrl()).isOk()
                                                                 .isJSONArrayLikeFile("v2/post/multiple_nodes_response.json");
        doGet(itemsUrl(TEST_NODE + "/child[2]")).isJSONObjectLikeFile("v2/get/node_with_sns_children.json");
    }

    @Test
    public void shouldEditMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("v2/post/multiple_nodes_request.json", itemsUrl()).isOk();
        doPut("v2/put/multiple_nodes_edit_request.json", itemsUrl()).isOk()
                                                                    .isJSONArrayLikeFile("v2/put/multiple_nodes_edit_response.json");
        // System.out.println("*****  GET: \n" + doGet(itemsUrl(TEST_NODE)));
    }

    @Test
    @Ignore( "A limitation of HTTPUrlConnection prevents this test from running. Consider enabling it if/when switching to Http Client" )
    public void shouldDeleteMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("v2/post/multiple_nodes_request.json", itemsUrl()).isOk();
        doDelete("v2/delete/multiple_nodes_delete.json", itemsUrl()).isOk();
        doGet(itemsUrl(TEST_NODE)).isJSONObjectLikeFile(nodeWithoutPrimaryTypeRequest());
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
        response = doPut(nodeWithBinaryProperty(), nodesUrl(id)).isOk().isJSONObjectLikeFile(nodeBinaryPropertyAfterEdit());
        // System.out.println("**** GET-BY-ID: \n" + response);

        // Delete by ID ...
        response = doDelete(nodesUrl(id)).isDeleted();
        // System.out.println("**** GET-BY-ID: \n" + response);
    }

    @Test
    @FixFor( "MODE-1816" )
    public void shouldAllowPostingNodeWithMixinAndPrimaryTypesPropertiesAfterProperties() throws Exception {
        doPost(nodeWithMixinAfterPropsRequest(), itemsUrl(TEST_NODE)).isCreated()
                                                                     .isJSONObjectLikeFile(nodeWithMixinAfterPropsResponse());
        doGet(itemsUrl(TEST_NODE)).isOk().isJSONObjectLikeFile(nodeWithMixinAfterPropsResponse());
    }

    @Test
    @FixFor( "MODE-1901" )
    public void shouldComputePlainTextPlanForJcrSql2Query() throws Exception {
        // No need to create any data, since we are not executing the query ...
        String query = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/" + TEST_NODE + "')";
        Response response = jcrSQL2QueryPlanAsText(query, queryPlanUrl()).isOk();
        assertThat(response.getContentTypeHeader().toLowerCase().startsWith("text/plain;"), is(true));
        String plan = response.responseString();
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
        String plan = response.responseString();
        assertThat(plan, is(notNullValue()));
        // System.out.println("**** PLAN: \n" + plan);
    }

    protected String queryPlanUrl( String... additionalPathSegments ) {
        return RestHelper.urlFrom(REPOSITORY_NAME + "/default/" + RestHelper.QUERY_PLAN_METHOD_NAME, additionalPathSegments);
    }

    @Test
    @FixFor( "MODE-2048")
    public void shouldAllowChildrenUpdateViaID() throws Exception {
        /**
         * testNode
         *   - child1 [prop="child1"]
         *   - child2 [prop="child2"]
         *   - child3 [prop="child3"]
         */
        JSONObject nodeWithHierarchyRequest = readJson("v2/post/node_multiple_children_request.json");

        //replace in the original request node paths with IDs
        JSONObject children = doPost(nodeWithHierarchyRequest, itemsUrl(TEST_NODE)).isCreated().children();
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
    @FixFor( "MODE-2048")
    public void shouldPerformChildReordering() throws Exception {
        /**
         * testNode
         *   - child1
         *   - child2
         *   - child3
         */
        doPost("v2/post/node_multiple_children_request.json", itemsUrl(TEST_NODE)).isCreated();

        /**
         * testNode
         *   - child3
         *   - child2
         *   - child1
         */
        JSONObject children = doPut("v2/put/node_multiple_children_reorder1.json", itemsUrl(TEST_NODE)).isOk().children();

        List<String> actualOrder = new ArrayList<String>();
        for (Iterator<?> iterator = children.keys(); iterator.hasNext();) {
            actualOrder.add(iterator.next().toString());
        }
        assertEquals("Invalid child order", Arrays.asList("child3", "child2", "child1"), actualOrder);

        /**
         * testNode
         *   - child2
         *   - child3
         *   - child1
         */
        children = doPut("v2/put/node_multiple_children_reorder2.json", itemsUrl(TEST_NODE)).isOk().children();
        actualOrder = new ArrayList<String>();
        for (Iterator<?> iterator = children.keys(); iterator.hasNext();) {
            actualOrder.add(iterator.next().toString());
        }
        assertEquals("Invalid child order", Arrays.asList("child2", "child3", "child1"), actualOrder);
    }

    @Test
    @FixFor( "MODE-2048")
    public void shouldMoveNode() throws Exception {
        /**
         * node1
         *   - child1
         *   - child2
         *   - child3
         */
        JSONObject children = doPost("v2/post/node_multiple_children_request.json", itemsUrl("node1")).isCreated().children();
        String child2Id = children.getJSONObject("child2").getString(ID_KEY);

        /**
         * node2
         *   - childNode
         */
        JSONObject request = readJson("v2/post/node_hierarchy_request.json");
        doPost(request, itemsUrl("node2")).isCreated();

        JSONObject requestChildren = request.getJSONObject(CHILDREN_KEY);
        request.remove("childNode");
        requestChildren.put(child2Id, Collections.emptyMap());

        //move node1/child2 to node2
        doPut(request, itemsUrl("node2")).isOk();
        assertTrue(doGet(itemsUrl("node2")).children().has("child2"));
        assertFalse(doGet(itemsUrl("node1")).children().has("child2"));
    }

    @Test
    @FixFor( "MODE-2056" )
    public void shouldCloseActiveSessions() throws Exception {
        //execute 3 requests
        doPost("v2/post/node_multiple_children_request.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("v2/post/node_multiple_children_request.json", itemsUrl(TEST_NODE)).isOk();
        doGet(itemsUrl(TEST_NODE));

        JSONObject repositories = doGet("/").isOk().json();
        JSONObject repository = repositories.getJSONArray("repositories").getJSONObject(0);
        int activeSessionsCount = repository.getInt("activeSessionsCount");

        assertEquals("There are active sessions in the repository", 0, activeSessionsCount);
    }

    @Test
    @FixFor( "MODE-2170" )
    public void shouldAllowUpdatingMultivaluedProperty() throws Exception {
        doPost("v2/post/node_multivalue_prop_request.json", itemsUrl(TEST_NODE)).isCreated();
        doPut("v2/put/node_multivalue_prop_request.json", itemsUrl(TEST_NODE))
                .isOk()
                .isJSONObjectLikeFile("v2/put/node_multivalue_prop_response.json");

    }
}
