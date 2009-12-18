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
package org.jboss.dna.web.jcr.rest.client.domain;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.web.jcr.rest.client.RestClientI18n;
import org.jboss.dna.web.jcr.rest.client.Utils;

/**
 * The <code>Server</code> class is the business object for a server that is hosting one or more DNA repositories.
 */
@Immutable
public class Server implements IDnaObject {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The password to use when logging on to the server.
     */
    private final String password;

    /**
     * The server URL (never <code>null</code>).
     */
    private final String url;

    /**
     * The user name to use when logging on to the server (never <code>null</code>).
     */
    private final String user;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs on new <code>Server</code>.
     * 
     * @param url the server URL (never <code>null</code>)
     * @param user the server user (may be <code>null</code>)
     * @param password the server password (may be <code>null</code>)
     * @throws IllegalArgumentException if the URL or user arguments are <code>null</code>
     */
    public Server( String url,
                   String user,
                   String password ) {
        CheckArg.isNotNull(url, "url");
        CheckArg.isNotNull(user, "user");

        this.url = url;
        this.user = user;
        this.password = password;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if ((obj == null) || (getClass() != obj.getClass())) return false;

        Server otherServer = (Server)obj;
        return Utils.equivalent(this.url, otherServer.url) && Utils.equivalent(this.user, otherServer.user)
               && Utils.equivalent(this.password, otherServer.password);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.domain.IDnaObject#getName()
     */
    public String getName() {
        return getUrl();
    }

    /**
     * @return the server authentication password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.domain.IDnaObject#getShortDescription()
     */
    public String getShortDescription() {
        return RestClientI18n.serverShortDescription.text(this.url, this.user);
    }

    /**
     * @return the server URL (never <code>null</code>)
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * @return the server authentication user (never <code>null</code>)
     */
    public String getUser() {
        return this.user;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.url, this.user, this.password);
    }

    /**
     * A server has the same identifying properties if their URL and user matches.
     * 
     * @param otherServer the server whose key is being compared (never <code>null</code>)
     * @return <code>true</code> if the servers have the same key
     * @throws IllegalArgumentException if the argument is <code>null</code>
     */
    public boolean hasSameKey( Server otherServer ) {
        CheckArg.isNotNull(otherServer, "otherServer");
        return (Utils.equivalent(this.url, otherServer.url) && Utils.equivalent(this.user, otherServer.user));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getShortDescription();
    }

}
