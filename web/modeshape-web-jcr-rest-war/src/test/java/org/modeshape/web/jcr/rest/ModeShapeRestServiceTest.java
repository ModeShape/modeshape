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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.web.jcr.rest.handler.RestBinaryHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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

    @Override
    @Ignore( "Ignored because the deprecated parameter isn't supported anymore" )
    public void shouldRetrieveRootNodeWhenDeprecatedDepthSet() throws Exception {
    }

    @Test
    public void shouldRetrieveBinaryPropertyValue() throws Exception {
        doPost(nodeWithBinaryProperty(), itemsUrl(TEST_NODE)).isCreated();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doGet(binaryUrl(TEST_NODE, binaryPropertyName()))
                .isOk()
                .hasMimeType(MediaType.TEXT_PLAIN)
                .hasContentDisposition(RestBinaryHandler.DEFAULT_CONTENT_DISPOSITION_PREFIX + TEST_NODE)
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
        String urlParams = "?mimeType=" + RestHelper.URL_ENCODER.encode(mimeType) + "&contentDisposition=" + RestHelper
                .URL_ENCODER.encode(contentDisposition);
        doGet(binaryUrl(TEST_NODE, binaryPropertyName()) + urlParams)
                .isOk()
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
        doPost(fileStream("v2/post/binary.pdf"), binaryUrl(TEST_NODE, binaryPropertyName()))
                .isCreated()
                .isJSONObjectLikeFile(newBinaryProperty());
        ByteArrayOutputStream actualBinaryContent = new ByteArrayOutputStream();
        doGet(binaryUrl(TEST_NODE, binaryPropertyName()))
                .isOk()
                .copyInputStream(actualBinaryContent);

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
        doPostMultiPart(fileStream("v2/post/binary.pdf"), "file", binaryUrl(TEST_NODE, binaryPropertyName()),
                        MediaType.APPLICATION_OCTET_STREAM)
                .isCreated()
                .isJSONObjectLikeFile(newBinaryProperty());
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
        doPost(fileStream(nodeTypesCND()), nodeTypesUrl())
                .isOk()
                .isJSONArrayLikeFile(cndImportResponse());
    }

    private String cndImportResponse() {
        return "v2/post/cnd_import_response.json";
    }

    private String nodeTypesCND() {
        return "v2/post/node_types.cnd";
    }

    @Test
    public void shouldImportCNDFileViaMultiPartRequest() throws Exception {
        doPostMultiPart(fileStream(nodeTypesCND()), "file", nodeTypesUrl(), MediaType.TEXT_PLAIN)
                .isOk()
                .isJSONArrayLikeFile(cndImportResponse());
    }

    @Test
    public void importingCNDViaWrongHTMLElementNameShouldFail() throws Exception {
        doPostMultiPart(fileStream(nodeTypesCND()), "invalidHTML", nodeTypesUrl(), MediaType.TEXT_PLAIN)
                .isBadRequest();
    }

    @Test
    public void shouldCreateMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("v2/post/multiple_nodes_request.json", itemsUrl())
                .isOk()
                .isJSONArrayLikeFile("v2/post/multiple_nodes_response.json");
    }

    @Test
    public void shouldEditMultipleNodes() throws Exception {
        doPost((String)null, itemsUrl(TEST_NODE)).isCreated();
        doPost("v2/post/multiple_nodes_request.json", itemsUrl()).isOk();
        doPut("v2/put/multiple_nodes_edit_request.json", itemsUrl())
                .isOk()
                .isJSONArrayLikeFile("v2/put/multiple_nodes_edit_response.json");
    }

    @Test
    @Ignore("A limitation of HTTPUrlConnection prevents this test from running. Consider enabling it if/when switching to Http Client")
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
}
