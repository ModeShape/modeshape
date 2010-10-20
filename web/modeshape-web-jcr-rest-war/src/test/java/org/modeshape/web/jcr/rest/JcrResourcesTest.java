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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.Base64;

/**
 * Test of the ModeShape JCR REST resource. Note that this test case uses a very low-level API to construct requests and
 * deconstruct the responses. Users are encouraged to use a higher-level library to communicate with the REST server (e.g., Apache
 * HTTP Commons).
 */
public class JcrResourcesTest {

    private static final String SERVER_CONTEXT = "/resources";
    private static final String SERVER_URL = "http://localhost:8090" + SERVER_CONTEXT;

    @Before
    public void beforeEach() {

        // Configured in pom
        final String login = "dnauser";
        final String password = "password";

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(login, password.toCharArray());
            }
        });
    }

    private String getResponseFor( HttpURLConnection connection ) throws IOException {
        StringBuffer buff = new StringBuffer();

        InputStream stream = connection.getInputStream();
        int bytesRead;
        byte[] bytes = new byte[1024];
        while (-1 != (bytesRead = stream.read(bytes, 0, 1024))) {
            buff.append(new String(bytes, 0, bytesRead));
        }

        return buff.toString();
    }

    @Test
    public void shouldNotServeContentToUnauthorizedUser() throws Exception {

        final String login = "dnauser";
        final String password = "invalidpassword";

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(login, password.toCharArray());
            }
        });

        URL postUrl = new URL(SERVER_URL + "/");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_UNAUTHORIZED));
        connection.disconnect();

    }

    @Test
    public void shouldNotServeContentToUserWithoutConnectRole() throws Exception {

        // Configured in pom
        final String login = "unauthorizeduser";
        final String password = "password";

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(login, password.toCharArray());
            }
        });

        URL postUrl = new URL(SERVER_URL + "/");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_UNAUTHORIZED));
        connection.disconnect();

    }

    @Test
    public void shouldServeContentAtRoot() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        String body = getResponseFor(connection);

        JSONObject objFromResponse = new JSONObject(body);
        JSONObject expected = new JSONObject(
                                             "{\"mode%3arepository\":{\"repository\":{\"name\":\"mode%3arepository\",\"resources\":{\"workspaces\":\"/resources/mode%3arepository\"}}}}");

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        assertThat(objFromResponse.toString(), is(expected.toString()));
        connection.disconnect();
    }

    @Test
    public void shouldServeListOfWorkspacesForValidRepository() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        String body = getResponseFor(connection);

        JSONObject objFromResponse = new JSONObject(body);
        JSONObject expected = new JSONObject(
                                             "{\"default\":{\"workspace\":{\"name\":\"default\",\"resources\":{\"query\":\"/resources/mode%3arepository/default/query\",\"items\":\"/resources/mode%3arepository/default/items\"}}}}");

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        assertThat(objFromResponse.toString(), is(expected.toString()));
        connection.disconnect();
    }

    @Test
    public void shouldReturnErrorForInvalidWorkspace() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/XXX");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();
    }

    @Test
    public void shouldRetrieveRootNodeForValidRepository() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(2));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        assertThat(properties.getString("jcr:primaryType"), is("mode:root"));
        assertThat(properties.get("jcr:uuid"), is(notNullValue()));

        JSONArray children = body.getJSONArray("children");
        assertThat(children.length(), is(1));
        assertThat(children.getString(0), is("jcr:system"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();
    }

    @Test
    public void shouldRetrieveRootNodeAndChildrenWhenDepthSet() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items?mode:depth=1");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(2));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        assertThat(properties.getString("jcr:primaryType"), is("mode:root"));
        assertThat(properties.get("jcr:uuid"), is(notNullValue()));

        JSONObject children = body.getJSONObject("children");
        assertThat(children.length(), is(1));

        JSONObject system = children.getJSONObject("jcr:system");
        assertThat(system.length(), is(2));

        properties = system.getJSONObject("properties");
        assertThat(properties.length(), is(1));
        assertThat(properties.getString("jcr:primaryType"), is("mode:system"));

        JSONArray namespaces = system.getJSONArray("children");
        assertThat(namespaces.length(), is(4));
        assertThat(namespaces.getString(0), is("jcr:versionStorage"));
        assertThat(namespaces.getString(1), is("mode:namespaces"));
        assertThat(namespaces.getString(2), is("jcr:nodeTypes"));
        assertThat(namespaces.getString(3), is("mode:locks"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();
    }

    @Test
    public void shouldRetrieveNodeAndChildrenWhenDepthSet() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/jcr:system?mode:depth=1");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(2));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(1));
        assertThat(properties.getString("jcr:primaryType"), is("mode:system"));

        JSONObject children = body.getJSONObject("children");
        assertThat(children.length(), is(4));

        JSONObject namespaces = children.getJSONObject("mode:namespaces");
        assertThat(namespaces.length(), is(2));
        JSONObject locks = children.getJSONObject("mode:locks");
        assertThat(locks.length(), is(1));
        JSONObject versionStorage = children.getJSONObject("jcr:versionStorage");
        assertThat(versionStorage.length(), is(1));

        properties = namespaces.getJSONObject("properties");
        assertThat(properties.length(), is(1));
        assertThat(properties.getString("jcr:primaryType"), is("mode:namespaces"));

        JSONArray namespace = namespaces.getJSONArray("children");
        assertThat(namespace.length(), is(10));
        Set<String> prefixes = new HashSet<String>(namespace.length());

        for (int i = 0; i < namespace.length(); i++) {
            prefixes.add(namespace.getString(i));
        }

        String[] expectedNamespaces = new String[] {"mode", "jcr", "nt", "mix", "sv", "xml", "modeint", "xmlns", "xsi", "xs"};
        for (int i = 0; i < expectedNamespaces.length; i++) {
            assertTrue(prefixes.contains(expectedNamespaces[i]));
        }

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();
    }

    @Test
    public void shouldRetrieveNodeTypeFromJcrSystemBranchIncludingSameNameSiblingChildren() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/jcr:system/jcr:nodeTypes/nt:base");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject(getResponseFor(connection));
        connection.disconnect();

        JSONArray children = body.getJSONArray("children");
        assertThat(children.length(), is(2));
        assertThat(children.getString(0), is("jcr:propertyDefinition"));
        assertThat(children.getString(1), is("jcr:propertyDefinition[2]"));
    }

    @Test
    public void shouldRetrieveNodeTypeSubgraphFromJcrSystemBranchIncludingSameNameSiblingChildren() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/jcr:system/jcr:nodeTypes/nt:base?mode:depth=4");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject(getResponseFor(connection));
        connection.disconnect();

        JSONObject children = body.getJSONObject("children");
        JSONObject propDefn1 = children.getJSONObject("jcr:propertyDefinition");
        JSONObject propDefn2 = children.getJSONObject("jcr:propertyDefinition[2]");
        assertThat(propDefn1, is(notNullValue()));
        assertThat(propDefn2, is(notNullValue()));
    }

    @Test
    public void shouldNotRetrieveNonExistentNode() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/foo");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();
    }

    @Test
    public void shouldNotRetrieveNonExistentProperty() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/jcr:system/foobar");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();
    }

    @Test
    public void shouldRetrieveProperty() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/jcr:system/jcr:primaryType");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String body = getResponseFor(connection);
        assertThat(body, is("{\"jcr:primaryType\":\"mode:system\"}"));
        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();
    }

    @Test
    public void shouldPostNodeToValidPathWithPrimaryType() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeA");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"testProperty\": \"testValue\", \"multiValuedProperty\": [\"value1\", \"value2\"]}}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is("testValue"));
        assertThat(properties.get("multiValuedProperty"), instanceOf(JSONArray.class));

        JSONArray values = properties.getJSONArray("multiValuedProperty");
        assertThat(values, is(notNullValue()));
        assertThat(values.length(), is(2));
        assertThat(values.getString(0), is("value1"));
        assertThat(values.getString(1), is("value2"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();
    }

    @Test
    public void shouldPostNodeToValidPathWithoutPrimaryType() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/noPrimaryType");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(1));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();
    }

    @Test
    public void shouldPostNodeToValidPathWithMixinTypes() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/withMixinType");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:mixinTypes\": \"mix:referenceable\"}}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("jcr:uuid"), is(notNullValue()));

        JSONArray values = properties.getJSONArray("jcr:mixinTypes");
        assertThat(values, is(notNullValue()));
        assertThat(values.length(), is(1));
        assertThat(values.getString(0), is("mix:referenceable"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/withMixinType");
        connection = (HttpURLConnection)postUrl.openConnection();

        // Make sure that we can retrieve the node with a GET
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        body = new JSONObject(getResponseFor(connection));

        assertThat(body.length(), is(1));

        properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("jcr:uuid"), is(notNullValue()));

        values = properties.getJSONArray("jcr:mixinTypes");
        assertThat(values, is(notNullValue()));
        assertThat(values.length(), is(1));
        assertThat(values.getString(0), is("mix:referenceable"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

    }

    @Test
    public void shouldNotPostNodeAtInvalidParentPath() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/foo/bar");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();

    }

    @Test
    public void shouldNotPostNodeWithInvalidPrimaryType() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/invalidPrimaryType");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"invalidType\", \"testProperty\": \"testValue\", \"multiValuedProperty\": [\"value1\", \"value2\"]}}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_BAD_REQUEST));
        connection.disconnect();

        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/invalidPrimaryType");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();

    }

    @Test
    public void shouldPostNodeHierarchy() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nestedPost");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"testProperty\": \"testValue\", \"multiValuedProperty\": [\"value1\", \"value2\"]},"
                         + " \"children\": { \"childNode\" : { \"properties\": {\"nestedProperty\": \"nestedValue\"}}}}";

        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nestedPost?mode:depth=1");
        connection = (HttpURLConnection)postUrl.openConnection();

        // Make sure that we can retrieve the node with a GET
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject(getResponseFor(connection));

        assertThat(body.length(), is(2));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is("testValue"));
        assertThat(properties.get("multiValuedProperty"), instanceOf(JSONArray.class));

        JSONArray values = properties.getJSONArray("multiValuedProperty");
        assertThat(values, is(notNullValue()));
        assertThat(values.length(), is(2));
        assertThat(values.getString(0), is("value1"));
        assertThat(values.getString(1), is("value2"));

        JSONObject children = body.getJSONObject("children");
        assertThat(children, is(notNullValue()));
        assertThat(children.length(), is(1));

        JSONObject child = children.getJSONObject("childNode");
        assertThat(child, is(notNullValue()));
        assertThat(child.length(), is(1));

        properties = child.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        // Parent primary type is nt:unstructured, so this should default to nt:unstructured primary type
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("nestedProperty"), is("nestedValue"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

    }

    @Test
    public void shouldFailWholeTransactionIfOneNodeIsBad() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/invalidNestedPost");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"testProperty\": \"testValue\", \"multiValuedProperty\": [\"value1\", \"value2\"]},"
                         + " \"children\": { \"childNode\" : { \"properties\": {\"jcr:primaryType\": \"invalidType\"}}}}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_BAD_REQUEST));
        connection.disconnect();

        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/invalidNestedPost?mode:depth=1");
        connection = (HttpURLConnection)postUrl.openConnection();

        // Make sure that we can retrieve the node with a GET
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();

    }

    @Test
    public void shouldNotDeleteNonExistentItem() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/invalidItemForDelete");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();
    }

    @Test
    public void shouldDeleteExtantNode() throws Exception {

        // Create the node
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForDeletion");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"testProperty\": \"testValue\", \"multiValuedProperty\": [\"value1\", \"value2\"]}}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is("testValue"));
        assertThat(properties.get("multiValuedProperty"), instanceOf(JSONArray.class));

        JSONArray values = properties.getJSONArray("multiValuedProperty");
        assertThat(values, is(notNullValue()));
        assertThat(values.length(), is(2));
        assertThat(values.getString(0), is("value1"));
        assertThat(values.getString(1), is("value2"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        // Confirm that it exists
        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForDeletion");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

        // Delete the node
        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForDeletion");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NO_CONTENT));
        connection.disconnect();

        // Confirm that it no longer exists
        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForDeletion");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();
    }

    @Test
    public void shouldDeleteExtantProperty() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/propertyForDeletion");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"testProperty\": \"testValue\", \"multiValuedProperty\": [\"value1\", \"value2\"]}}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        // Confirm that it exists
        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/propertyForDeletion");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is("testValue"));
        assertThat(properties.get("multiValuedProperty"), instanceOf(JSONArray.class));

        JSONArray values = properties.getJSONArray("multiValuedProperty");
        assertThat(values, is(notNullValue()));
        assertThat(values.length(), is(2));
        assertThat(values.getString(0), is("value1"));
        assertThat(values.getString(1), is("value2"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

        // Delete the property
        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/propertyForDeletion/multiValuedProperty");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NO_CONTENT));
        connection.disconnect();

        // Confirm that it no longer exists
        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/propertyForDeletion");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is("testValue"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

    }

    @Test
    public void shouldNotBeAbleToPutAtInvalidPath() throws Exception {

        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nonexistantNode");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"firstProperty\": \"someValue\" }";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
        connection.disconnect();
    }

    @Test
    public void shouldBeAbleToPutValueToProperty() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForPutProperty");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"testProperty\": \"testValue\" }}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForPutProperty/testProperty");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        payload = "{\"testProperty\":\"someOtherValue\"}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is("someOtherValue"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

    }

    @Test
    public void shouldBeAbleToPutBinaryValueToProperty() throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForPutBinaryProperty");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        // Base64-encode a value ...
        String encodedValue = Base64.encodeBytes("propertyValue".getBytes("UTF-8"));

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"testProperty/base64/\": \""
                         + encodedValue + "\" }}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        URL putUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForPutBinaryProperty/testProperty");
        connection = (HttpURLConnection)putUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String otherValue = "someOtherValue";
        payload = "{\"testProperty/base64/\":\"" + Base64.encodeBytes(otherValue.getBytes("UTF-8")) + "\"}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        String responseEncodedValue = properties.getString("testProperty/base64/");
        String decodedValue = new String(Base64.decode(responseEncodedValue), "UTF-8");
        assertThat(decodedValue, is(otherValue));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

        // Try putting a non-binary value ...
        connection = (HttpURLConnection)putUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String anotherValue = "yetAnotherValue";
        payload = "{\"testProperty\":\"" + anotherValue + "\"}";
        connection.getOutputStream().write(payload.getBytes());

        body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is(anotherValue));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();
    }

    @Test
    public void shouldBeAbleToPutPropertiesToNode() throws Exception {

        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForPutProperties");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\" }}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeForPutProperties");
        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        payload = "{\"testProperty\": \"testValue\", \"multiValuedProperty\": [\"value1\", \"value2\"]}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        assertThat(properties.getString("testProperty"), is("testValue"));
        assertThat(properties.get("multiValuedProperty"), instanceOf(JSONArray.class));

        JSONArray values = properties.getJSONArray("multiValuedProperty");
        assertThat(values, is(notNullValue()));
        assertThat(values.length(), is(2));
        assertThat(values.getString(0), is("value1"));
        assertThat(values.getString(1), is("value2"));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

    }

    @Test
    public void shouldBeAbleToAddAndRemoveMixinTypes() throws Exception {

        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items/nodeWithNoMixins");
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\" }}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        payload = "{\"jcr:mixinTypes\": \"mix:referenceable\"}";
        connection.getOutputStream().write(payload.getBytes());

        JSONObject body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        JSONObject properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));

        assertThat(properties.length(), is(3));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));
        JSONArray mixinTypes = properties.getJSONArray("jcr:mixinTypes");
        assertThat(mixinTypes, is(notNullValue()));
        assertThat(mixinTypes.length(), is(1));
        assertThat(mixinTypes.getString(0), is("mix:referenceable"));
        assertThat(properties.getString("jcr:uuid"), is(notNullValue()));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

        connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        payload = "{\"jcr:mixinTypes\": []}";
        connection.getOutputStream().write(payload.getBytes());

        body = new JSONObject(getResponseFor(connection));
        assertThat(body.length(), is(1));

        properties = body.getJSONObject("properties");
        assertThat(properties, is(notNullValue()));
        assertThat(properties.length(), is(2));
        assertThat(properties.getString("jcr:primaryType"), is("nt:unstructured"));

        // removeMixin doesn't currently null out this value
        mixinTypes = properties.getJSONArray("jcr:mixinTypes");
        assertThat(mixinTypes, is(notNullValue()));
        assertThat(mixinTypes.length(), is(0));

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        connection.disconnect();

    }

    @Test
    public void shouldRetrieveDataFromXPathQuery() throws Exception {
        final String NODE_PATH = "/nodeForQuery";
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items" + NODE_PATH);
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\" }}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));
        connection.disconnect();

        URL queryUrl = new URL(SERVER_URL + "/mode%3arepository/default/query");
        connection = (HttpURLConnection)queryUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/jcr+xpath");

        payload = "//nodeForQuery";
        connection.getOutputStream().write(payload.getBytes());
        JSONObject queryResult = new JSONObject(getResponseFor(connection));
        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(1));

        JSONObject result = (JSONObject)results.get(0);
        assertThat(result, is(notNullValue()));
        assertThat((String)result.get("jcr:path"), is(NODE_PATH));
        assertThat((String)result.get("jcr:primaryType"), is("nt:unstructured"));

    }

    private void createNode( String path,
                             int index ) throws Exception {
        URL postUrl = new URL(SERVER_URL + "/mode%3arepository/default/items" + path);
        HttpURLConnection connection = (HttpURLConnection)postUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);

        String payload = "{ \"properties\": {\"jcr:primaryType\": \"nt:unstructured\", \"foo\": \"" + index + "\" }}";
        connection.getOutputStream().write(payload.getBytes());

        assertThat(connection.getResponseCode(), is(HttpURLConnection.HTTP_CREATED));

        connection.disconnect();
    }

    @Test
    public void shouldRespectQueryOffsetAndLimit() throws Exception {
        final String NODE_PATH = "nodeForOffsetAndLimitTest";

        createNode("/" + NODE_PATH, 1);
        createNode("/" + NODE_PATH, 2);
        createNode("/" + NODE_PATH, 3);
        createNode("/" + NODE_PATH, 4);

        URL queryUrl = new URL(SERVER_URL + "/mode%3arepository/default/query?offset=1&limit=2");
        HttpURLConnection connection = (HttpURLConnection)queryUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/jcr+xpath");

        String payload = "//element(" + NODE_PATH + ") order by @foo";

        connection.getOutputStream().write(payload.getBytes());
        JSONObject queryResult = new JSONObject(getResponseFor(connection));
        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(2));

        JSONObject result = (JSONObject)results.get(0);
        assertThat(result, is(notNullValue()));
        assertThat((String)result.get("jcr:path"), is("/" + NODE_PATH + "[2]"));

        result = (JSONObject)results.get(1);
        assertThat(result, is(notNullValue()));
        assertThat((String)result.get("jcr:path"), is("/" + NODE_PATH + "[3]"));
    }

    // @FixFor( "MODE-886" )
    @Test
    public void shouldAllowJcrSql2Query() throws Exception {
        final String NODE_PATH = "nodeForJcrSql2QueryTest";

        createNode("/" + NODE_PATH, 1);

        createNode("/" + NODE_PATH + "/child", 1);
        createNode("/" + NODE_PATH + "/child", 2);
        createNode("/" + NODE_PATH + "/child", 3);
        createNode("/" + NODE_PATH + "/child", 4);

        URL queryUrl = new URL(SERVER_URL + "/mode%3arepository/default/query");
        HttpURLConnection connection = (HttpURLConnection)queryUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/jcr+sql2");

        String payload = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/" + NODE_PATH + "')";

        connection.getOutputStream().write(payload.getBytes());
        JSONObject queryResult = new JSONObject(getResponseFor(connection));
        JSONArray results = (JSONArray)queryResult.get("rows");

        assertThat(results.length(), is(4));
    }

}
