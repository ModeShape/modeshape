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
package org.modeshape.jcr.security;

import java.util.Map;
import javax.jcr.Credentials;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.SecurityContext;
import org.modeshape.jcr.JcrSecurityContextCredentials;
import org.modeshape.jcr.api.SecurityContextCredentials;

/**
 * An implementation of and {@link AuthenticationProvider} that checks whether the supplied credentials are
 * {@link SecurityContextCredentials} and uses the contained {@link SecurityContext} for authentication and <i>role-based</i>
 * authorization. This provider should be used with care, as it delegates authentication and authorization to the Credentials
 * object supplied by the caller.
 * 
 * @deprecated Use custom AuthenticationProvider implementations instead
 */
@Deprecated
public class SecurityContextProvider implements AuthenticationProvider {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.security.AuthenticationProvider#authenticate(javax.jcr.Credentials, java.lang.String,
     *      java.lang.String, org.modeshape.graph.ExecutionContext, java.util.Map)
     */
    public ExecutionContext authenticate( Credentials credentials,
                                          String repositoryName,
                                          String workspaceName,
                                          ExecutionContext repositoryContext,
                                          Map<String, Object> sessionAttributes ) {
        if (credentials instanceof SecurityContextCredentials) {
            SecurityContextCredentials scc = (SecurityContextCredentials)credentials;
            return repositoryContext.with(contextFor(scc));
        }
        if (credentials instanceof JcrSecurityContextCredentials) {
            JcrSecurityContextCredentials jscc = (JcrSecurityContextCredentials)credentials;
            return repositoryContext.with(jscc.getSecurityContext());
        }
        return null;
    }

    /**
     * Adapts the modeshape-jcr-api {@link org.modeshape.jcr.api.SecurityContext} to the modeshape-graph {@link SecurityContext}
     * needed for repository login.
     * 
     * @param credentials the credentials containing the modeshape-jcr-api {@code SecurityContext}
     * @return an equivalent modeshape-graph {@code SecurityContext}
     */
    private SecurityContext contextFor( SecurityContextCredentials credentials ) {
        assert credentials != null;

        final org.modeshape.jcr.api.SecurityContext jcrSecurityContext = credentials.getSecurityContext();
        assert jcrSecurityContext != null;

        return new SecurityContext() {
            public String getUserName() {
                return jcrSecurityContext.getUserName();
            }

            public boolean hasRole( String roleName ) {
                return jcrSecurityContext.hasRole(roleName);
            }

            public void logout() {
                jcrSecurityContext.logout();
            }

        };
    }

}
