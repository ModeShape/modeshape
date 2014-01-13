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
package org.modeshape.web.jcr.rest.client.domain;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.web.jcr.rest.client.RestClientI18n;
import org.modeshape.web.jcr.rest.client.Utils;

/**
 * The <code>Server</code> class is the business object for a server that is hosting one or more ModeShape repositories.
 * <p>
 * The server requires a <i>url</i>, <i>user</i> name, and a <i>password</i> in order to connect. The {@link #url} that is used
 * has a format of <b>http://hostname:port/context root</b>. Where
 * <li>hostname is the name of the server</li>
 * <li>port is the port to connect to, generally its 8080</li>
 * <li>context root is the deployed war context</li>
 * <p>
 * The deployed war context root is based on what the deployed war file is called. If the ModeShape deployed war is called
 * resources.war (which is the default build name), then the context root would be <i>resources</i>.
 */
@Immutable
public class Server implements IModeShapeObject {

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

    /**
     * The original server URL, which may be the same as the {@link #url} (never <code>null</code>)
     */
    private final String originalUrl;

    /**
     * Determine whether
     */
    private final boolean validated;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs on new <code>Server</code>.
     * 
     * @param url the server URL, which must contain the deployed war file context (never <code>null</code>)
     * @param user the server user (may be <code>null</code>)
     * @param password the server password (may be <code>null</code>)
     * @throws IllegalArgumentException if the URL or user arguments are <code>null</code>
     */
    public Server( String url,
                   String user,
                   String password ) {
        assert url != null;
        assert user != null;

        this.url = url;
        this.user = user;
        this.password = password;
        this.validated = false;
        this.originalUrl = this.url;
        assert this.originalUrl != null;
    }

    /**
     * Constructs on new <code>Server</code>.
     * 
     * @param originalUrl the original, user-supplied server URL (may not be <code>null</code>)
     * @param validatedUrl the validated server URL to which the server can connect, which must contain the deployed war file
     *        context (never <code>null</code>)
     * @param user the server user (may be <code>null</code>)
     * @param password the server password (may be <code>null</code>)
     * @throws IllegalArgumentException if the URL or user arguments are <code>null</code>
     */
    protected Server( String originalUrl,
                      String validatedUrl,
                      String user,
                      String password ) {
        assert originalUrl != null;
        assert validatedUrl != null;
        assert user != null;

        this.url = validatedUrl;
        this.originalUrl = originalUrl;
        this.user = user;
        this.password = password;
        this.validated = true;
        assert this.originalUrl != null;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * Create a copy of this object that represents a validated URL to a server.
     * 
     * @param validatedUrl the validated URL; may not be null
     * @return the new, validated Server object; never null
     */
    public Server asValidated( String validatedUrl ) {
        assert validatedUrl != null;
        return new Server(this.originalUrl, validatedUrl, this.user, this.password);
    }

    /**
     * Get the original URL that the user supplied. If this object has been validated, then this URL may not be the actual URL
     * used to communicate with the REST service.
     * 
     * @return the original URL; never null
     */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /**
     * Determine whether this server has been {@link org.modeshape.web.jcr.rest.client.IRestClient#validate(Server) validated}.
     * 
     * @return true if validated, or false otherwise
     */
    public boolean isValidated() {
        return validated;
    }

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
        return Utils.equivalent(this.url, otherServer.url) && Utils.equivalent(this.originalUrl, otherServer.originalUrl)
               && Utils.equivalent(this.user, otherServer.user) && Utils.equivalent(this.password, otherServer.password);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getName()
     */
    @Override
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
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getShortDescription()
     */
    @Override
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
        return this.originalUrl.hashCode();
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
