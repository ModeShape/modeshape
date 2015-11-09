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
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.web.jcr.rest.handler.AbstractHandler;
import junit.framework.AssertionFailedError;

/**
 * Base class used for testing the interaction with {@link org.modeshape.web.jcr.rest.ModeShapeRestService}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@SuppressWarnings( "deprecation" )
public abstract class AbstractRestTest {

    private static final List<String> JSON_PROPERTIES_IGNORE_EQUALS = Arrays.asList("jcr:uuid", "jcr:score",
                                                                                    AbstractHandler.NODE_ID_CUSTOM_PROPERTY);
    private static final UrlEncoder URL_ENCODER = new UrlEncoder().setSlashEncoded(false);

    protected CloseableHttpClient httpClient;
    protected HttpClientContext httpContext;
    protected abstract String getServerContext();
    protected abstract HttpHost getHost();

    @Before
    public void beforeEach() throws Exception {
        httpClient = HttpClientBuilder.create().build();
        httpContext = HttpClientContext.create();
    }

    @After
    public void afterEach() throws Exception {
        httpClient.getConnectionManager().shutdown();
    }

    protected void setAuthCredentials( String authUsername,
                                       String authPassword ) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(getHost()),
                                           new UsernamePasswordCredentials(authUsername, authPassword));
        httpContext.setCredentialsProvider(credentialsProvider);
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
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();  
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IoUtil.write(fileStream(filePath), baos);
            entityBuilder.addPart(elementName, new ByteArrayBody(baos.toByteArray(), "test_file"));
            post.setEntity(entityBuilder.build());

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
                assertTrue("Actual JSON object does not contain key '" + key + "': " + actualJSON, actualJSON.has(key));

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
        private JSONObject contentJSON;

        protected Response( HttpRequestBase request ) {
            try {
                response = httpClient.execute(getHost(), request, httpContext);
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
            if (contentJSON == null) {
                contentJSON = new JSONObject(contentAsString());
            }
            return contentJSON;
        }

        protected String contentAsString() {
            if (contentString == null) {
                contentString = new String(content);
            }
            return contentString;
        }

        protected Response hasPrimaryType(String primaryType) throws Exception {
            JSONObject json = json();
            assertTrue("jcr:primary type property not found", json.has(JcrConstants.JCR_PRIMARY_TYPE));
            assertEquals(primaryType, json().getString(JcrConstants.JCR_PRIMARY_TYPE));
            return this;
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
