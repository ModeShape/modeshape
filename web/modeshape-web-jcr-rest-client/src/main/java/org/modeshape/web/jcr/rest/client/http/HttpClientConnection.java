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
package org.modeshape.web.jcr.rest.client.http;

import static org.modeshape.web.jcr.rest.client.RestClientI18n.unknownHttpRequestMethodMsg;
import java.net.URL;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.json.IJsonConstants.RequestMethod;

/**
 * <code>HttpClientConnection</code> uses the <code>Apache HttpClient</code>.
 */
public final class HttpClientConnection {

    private static final String APPLICATION_JSON = "application/json";

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The HTTP client that opens the connection, executes the request, and closes the connection.
     */
    private final AbstractHttpClient httpClient;

    /**
     * The HTTP request.
     */
    private final HttpRequestBase request;

    /**
     * The HTTP response.
     */
    private HttpResponse response;

    /**
     * The content type
     */
    private String contentType;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param server the server with the host and port information (never <code>null</code>)
     * @param url the URL that will be used in the request (never <code>null</code>)
     * @param method the HTTP request method (never <code>null</code>)
     * @throws Exception if there is a problem establishing the connection
     */
    public HttpClientConnection( Server server,
                                 URL url,
                                 RequestMethod method ) throws Exception {
        assert server != null;
        assert url != null;
        assert method != null;

        this.httpClient = new DefaultHttpClient();
        this.httpClient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort()),
                                                                new UsernamePasswordCredentials(server.getUser(),
                                                                                                server.getPassword()));
        // determine the request type
        if (RequestMethod.GET == method) {
            this.request = new HttpGet();
        } else if (RequestMethod.DELETE == method) {
            this.request = new HttpDelete();
        } else if (RequestMethod.POST == method) {
            this.request = new HttpPost();
        } else if (RequestMethod.PUT == method) {
            this.request = new HttpPut();
        } else {
            throw new RuntimeException(unknownHttpRequestMethodMsg.text(method));
        }

        // set the accepts header to application/json
        this.request.setHeader("Accept", APPLICATION_JSON);

        // set request URI
        this.request.setURI(url.toURI());
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * Disconnects this connection.
     */
    public void disconnect() {
        this.httpClient.getConnectionManager().shutdown();
    }

    /**
     * @return the HTTP response
     * @throws Exception if there is a problem obtaining the response
     */
    private HttpResponse getResponse() throws Exception {
        if (this.response == null) {
            this.response = this.httpClient.execute(request);
        }

        return this.response;
    }

    /**
     * @return the HTTP response code
     * @throws Exception if there is a problem getting the response code
     */
    public int getResponseCode() throws Exception {
        return getResponse().getStatusLine().getStatusCode();
    }

    /**
     * @return the HTTP response body as a string
     * @throws Exception if there is a problem reading from the connection
     */
    public String read() throws Exception {
        return EntityUtils.toString(getResponse().getEntity());
    }

    /**
     * @param bytes the bytes being posted to the HTTP connection (never <code>null</code>)
     * @throws Exception if there is a problem writing to the connection
     */
    public void write( byte[] bytes ) throws Exception {
        assert bytes != null;

        ByteArrayEntity entity = new ByteArrayEntity(bytes);
        if (contentType == null) {
            entity.setContentType(APPLICATION_JSON);
        }

        if (this.request instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase)this.request).setEntity(entity);
        }
    }

    /**
     * Sets the content type for the request
     * 
     * @param contentType the content type to use
     */
    public void setContentType( String contentType ) {
        this.contentType = contentType;
        this.request.setHeader("Content-Type", contentType);
    }
}
