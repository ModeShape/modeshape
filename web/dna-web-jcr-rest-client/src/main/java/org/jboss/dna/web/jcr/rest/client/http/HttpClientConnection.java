/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client.http;

import java.net.URL;
import javax.ws.rs.core.MediaType;
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
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.json.IJsonConstants.RequestMethod;

/**
 * <code>HttpClientConnection</code> is an <code>Apache HttpClient</code> implementation of <code>IHttpConnection</code>.
 */
public final class HttpClientConnection implements IHttpConnection {

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

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param server the server with the host and port information
     * @param url the URL that will be used in the request
     * @param method the HTTP request method
     * @throws Exception if there is a problem establishing the connection
     */
    public HttpClientConnection( Server server,
                                 URL url,
                                 RequestMethod method ) throws Exception {
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
            // TODO need exception message
            throw new RuntimeException();
        }

        // set request URI
        this.request.setURI(url.toURI());
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.http.IHttpConnection#disconnect()
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
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.http.IHttpConnection#getResponseCode()
     */
    public int getResponseCode() throws Exception {
        return getResponse().getStatusLine().getStatusCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.http.IHttpConnection#read()
     */
    public String read() throws Exception {
        return EntityUtils.toString(getResponse().getEntity());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.http.IHttpConnection#write(byte[])
     */
    public void write( byte[] bytes ) throws Exception {
        ByteArrayEntity entity = new ByteArrayEntity(bytes);
        entity.setContentType(MediaType.APPLICATION_JSON);

        if (this.request instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase)this.request).setEntity(entity);
        }
    }

}
