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

import org.junit.Ignore;

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
    @Ignore("Ignored because the deprecated parameter isn't supported anymore")
    public void shouldRetrieveRootNodeWhenDeprecatedDepthSet() throws Exception {
    }
}
