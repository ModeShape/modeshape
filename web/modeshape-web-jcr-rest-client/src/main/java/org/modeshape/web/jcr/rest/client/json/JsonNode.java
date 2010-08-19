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
package org.modeshape.web.jcr.rest.client.json;

import java.net.URL;
import org.codehaus.jettison.json.JSONObject;

/**
 * The <code>JsonNode</code> class defines the API for interacing with JSON objects. Every <code>JsonNode</code> knows how to
 * create their URL and create their JCR content.
 */
public abstract class JsonNode extends JSONObject {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The node identifier.
     */
    private final String id;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param id the node identifier (never <code>null</code>)
     */
    protected JsonNode( String id ) {
    	assert id != null;
        this.id = id;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @return the content that gets published
     * @throws Exception if there is a problem obtaining the node content
     */
    public byte[] getContent() throws Exception {
        return super.toString().getBytes();
    }

    /**
     * @return a unique identifier for this node
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return an HTTP URL representing this node
     * @throws Exception if there is a problem constructing the URL
     */
    public abstract URL getUrl() throws Exception;

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.jettison.json.JSONObject#toString()
     */
    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder();
        txt.append("ID: ").append(getId());
        txt.append(", URL: ");

        try {
            txt.append(getUrl());
        } catch (Exception e) {
            txt.append("exception obtaining URL");
        }

        txt.append(", content: ").append(super.toString());
        return txt.toString();
    }

}
