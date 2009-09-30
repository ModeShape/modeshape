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

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ws.rs.core.MediaType;
import org.jboss.dna.common.util.Base64;
import org.jboss.dna.web.jcr.rest.client.ServerAuthenticator;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.json.JsonUtils;
import org.jboss.dna.web.jcr.rest.client.json.IJsonConstants.RequestMethod;

/**
 * <code>HttpUrlConnection</code> is a <code>java.net</code> implementation of <code>IHttpConnection</code>.
 */
public final class HttpUrlConnection implements IHttpConnection {

    // ===========================================================================================================================
    // Class Initializer
    // ===========================================================================================================================

    static {
        // must be set when using java.net framework but a problem occurs when other subsytems have their own Authenticator and
        // also call this method
        Authenticator.setDefault(new ServerAuthenticator());
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The HTTP connection that executes the request and obtains the response.
     */
    private final HttpURLConnection connection;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param server the server with the host and port information
     * @param url the URL that will be used in the request
     * @param method the HTTP request method
     * @throws Exception if there is a problem establishing the connection
     */
    public HttpUrlConnection( Server server,
                              URL url,
                              RequestMethod method ) throws Exception {
        this.connection = (HttpURLConnection)url.openConnection();
        this.connection.setDoOutput(true);

        String encoding = Base64.encodeBytes((server.getUser() + ':' + server.getPassword()).getBytes());
        this.connection.setRequestProperty("Authorization", "Basic " + encoding); //$NON-NLS-1$ //$NON-NLS-2$

        this.connection.setRequestMethod(method.toString());
        this.connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON); //$NON-NLS-1$
        this.connection.setUseCaches(false);
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
        this.connection.disconnect();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.http.IHttpConnection#getResponseCode()
     */
    public int getResponseCode() throws Exception {
        return this.connection.getResponseCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.http.IHttpConnection#read()
     */
    public String read() throws Exception {
        return JsonUtils.readInputStream(this.connection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.http.IHttpConnection#write(byte[])
     */
    public void write( byte[] bytes ) throws Exception {
        connection.getOutputStream().write(bytes);
    }

}
