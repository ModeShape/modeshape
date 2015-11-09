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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;

/**
 * A simple client which can be used to make REST calls to a server.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@SuppressWarnings( "deprecation" )
public final class JSONRestClient {

    private static final UrlEncoder URL_ENCODER = new UrlEncoder().setSlashEncoded(false);
    protected final HttpClient httpClient;
    private final HttpClientContext httpContext;
    private final HttpHost host;
    private final String url;
    private final String baseUrl;

    protected JSONRestClient( String url,
                              String username,
                              String password ) {
        CheckArg.isNotNull(url, "url");
        try {
            this.url = url;
            URL connectionUrl = new URL(url);
            this.host = new HttpHost(connectionUrl.getHost(), connectionUrl.getPort(), connectionUrl.getProtocol());
            String urlPath = connectionUrl.getPath();
            if (urlPath.length() > 0) {
                String[] segments = urlPath.split("\\/");
                this.baseUrl = this.host.toURI() + "/" + (segments[0].length() > 0 ? segments[0] : segments[1]);
            } else {
                this.baseUrl = this.host.toURI();
            }
            this.httpClient = HttpClientBuilder.create().build();
            this.httpContext = HttpClientContext.create();
            if (!StringUtil.isBlank(username)) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(host()),
                                                   new UsernamePasswordCredentials(username, password));
                httpContext.setCredentialsProvider(credentialsProvider);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL string: " + url, e);
        }
    }

    protected String url() {
        return url;
    }

    protected HttpHost host() {
        return host;
    }

    protected Response doGet() {
        return new Response(newJSONRequest(HttpGet.class, null, null, null));
    }

    protected Response doGet( String url ) {
        return new Response(newJSONRequest(HttpGet.class, null, null, url));
    }

    protected Response postStream( InputStream is,
                                   String url,
                                   String requestContentType ) {
        HttpPost post = newJSONRequest(HttpPost.class, is, requestContentType, url);
        return new Response(post);
    }

    protected Response postStreamTextPlain( InputStream is,
                                            String url,
                                            String requestContentType ) {
        HttpPost post = newRequest(HttpPost.class, is, requestContentType, MediaType.TEXT_PLAIN, url);
        return new Response(post);
    }

    private <T extends HttpRequestBase> T newJSONRequest( Class<T> clazz,
                                                          InputStream inputStream,
                                                          String contentType,
                                                          String url ) {
        return newRequest(clazz, inputStream, contentType, MediaType.APPLICATION_JSON, url);
    }

    private <T extends HttpRequestBase> T newRequest( Class<T> clazz,
                                                      InputStream inputStream,
                                                      String contentType,
                                                      String accepts,
                                                      String url ) {
        assert accepts != null;
        try {
            if (url == null) {
                url = baseUrl;
            } else if (!url.startsWith(host.getSchemeName())) {
                url = appendToBaseURL(url);
            }
            URIBuilder uriBuilder;
            try {
                uriBuilder = new URIBuilder(url);
            } catch (URISyntaxException e) {
                uriBuilder = new URIBuilder(URL_ENCODER.encode(url));
            }

            T result = clazz.getConstructor(URI.class).newInstance(uriBuilder.build());
            result.setHeader("Accept", accepts);
            if (contentType != null) {
                result.setHeader("Content-Type", contentType);
            }
            if (inputStream != null) {
                assert result instanceof HttpEntityEnclosingRequestBase;
                InputStreamEntity inputStreamEntity = new InputStreamEntity(inputStream, inputStream.available());
                ((HttpEntityEnclosingRequestBase)result).setEntity(new BufferedHttpEntity(inputStreamEntity));
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static String append( String url,
                                    String... segments ) {
        for (String segment : segments) {
            if (url.endsWith(segment)) {
                continue;
            }
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            if (segment.startsWith("/")) {
                segment = segment.substring(1);
            }
            url += segment;
        }
        return url;
    }

    protected String appendToBaseURL( String... segments ) {
        return append(baseUrl, segments);
    }

    protected String appendToURL( String... segments ) {
        return append(url, segments);
    }

    protected interface MediaType {
        String APPLICATION_JSON = "application/json";
        String TEXT_PLAIN = "text/plain;";
    }

    protected class Response {

        private final HttpResponse response;
        private byte[] content;
        private String contentString;
        private JSONObject contentJSON;

        protected Response( HttpRequestBase request ) {
            try {
                response = httpClient.execute(host(), request, httpContext);
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

        public boolean isOK() {
            return hasCode(HttpURLConnection.HTTP_OK);
        }

        private int getStatusCode() {
            return response.getStatusLine().getStatusCode();
        }

        private boolean hasCode( int statusCode ) {
            return getStatusCode() == statusCode;
        }

        public JSONObject json() {
            try {
                if (contentJSON == null) {
                    contentJSON = new JSONObject(asString());
                }
                return contentJSON;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public String asString() {
            if (contentString == null) {
                contentString = new String(content);
            }
            return contentString;
        }

        @Override
        public String toString() {
            return asString();
        }
    }
}
