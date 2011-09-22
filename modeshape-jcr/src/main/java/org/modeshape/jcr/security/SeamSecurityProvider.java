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
import org.jboss.seam.security.Identity;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.SecurityContext;

/**
 * An implementation of {@link AuthenticationProvider} that uses Seam Security to perform all authentication and <i>role-based</i>
 * authorization.
 */
public class SeamSecurityProvider implements AuthenticationProvider {

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
        if (credentials == null) {
            // We don't care about credentials, as we'll always use the Seam Identity class ...
            Identity identity = null;
            try {
                identity = Identity.instance();
            } catch (Throwable e) {
                // There was no Identity instance
                return null;
            }
            if (identity != null && identity.isLoggedIn()) {
                SeamSecurityContext context = new SeamSecurityContext(identity);
                return repositoryContext.with(context);
            }
        }
        return null;
    }

    /**
     * A {@link SecurityContext security context} implementation that is based upon Seam Security and that provides authentication
     * and authorization through the Seam Security {@link Identity} instance.
     */
    @Immutable
    public static class SeamSecurityContext implements SecurityContext {

        private final Identity identity;

        /**
         * Create a {@link SeamSecurityContext} with the supplied {@link Identity} instance.
         * 
         * @param identity the Seam Security {@link Identity} instance; may not be null
         * @throws IllegalArgumentException if the <code>identity</code> is null
         */
        public SeamSecurityContext( Identity identity ) {
            CheckArg.isNotNull(identity, "identity");
            this.identity = identity;
        }

        /**
         * {@inheritDoc SecurityContext#getUserName()}
         * 
         * @see SecurityContext#getUserName()
         */
        public String getUserName() {
            return identity.getCredentials().getUsername();
        }

        /**
         * {@inheritDoc SecurityContext#hasRole(String)}
         * 
         * @see SecurityContext#hasRole(String)
         */
        public boolean hasRole( String roleName ) {
            return identity.hasRole(roleName);
        }

        /**
         * {@inheritDoc SecurityContext#logout()}
         * 
         * @see SecurityContext#logout()
         */
        public void logout() {
            // we'll let Seam Security handle logging out of the Identity ...
        }
    }

}
