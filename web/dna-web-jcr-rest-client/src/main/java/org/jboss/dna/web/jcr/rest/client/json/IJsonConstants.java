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
package org.jboss.dna.web.jcr.rest.client.json;

/**
 * The <code>IJsonConstants</code> interface provides JSON-specific constants used when JSON as a transport mechanism between the
 * REST client and the REST server.
 */
public interface IJsonConstants {

    /**
     * The HTTP method to use when creating a connection with the REST server.
     */
    enum RequestMethod {
        /**
         * The HTTP DELETE request method.
         */
        DELETE,

        /**
         * The HTTP GET request method.
         */
        GET,

        /**
         * The HTTP POST request method.
         */
        POST,

        /**
         * The HTTP PUT request method.
         */
        PUT
    }

    /**
     * The key in the <code>JSONObject</code> whose value is the collection of node children.
     */
    String CHILDREN_KEY = "children"; //$NON-NLS-1$

    /**
     * The key in the <code>JSONObject</code> whose value is the collection of node properties.
     */
    String PROPERTIES_KEY = "properties"; //$NON-NLS-1$

    /**
     * The server context added to URLs.
     */
    String SERVER_CONTEXT = "/resources"; //$NON-NLS-1$

    /**
     * The workspace context added to the URLs.
     */
    String WORKSPACE_CONTEXT = "/items"; //$NON-NLS-1$

}
