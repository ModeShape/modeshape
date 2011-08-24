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
import javax.servlet.http.HttpServletRequest;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.core.SecurityContext;

/**
 * An implementation of {@link AuthenticationProvider} that delegates to the {@link HttpServletRequest} referenced by the supplied
 * {@link ServletCredentials} instances for all authentication and <i>role-based</i> authorization.
 * <p>
 * Note that this class can only be used if the {@link HttpServletRequest} class is on the classpath.
 * </p>
 */
public class ServletProvider implements AuthenticationProvider {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.security.AuthenticationProvider#authenticate(javax.jcr.Credentials, java.lang.String,
     *      java.lang.String, org.modeshape.jcr.core.ExecutionContext, java.util.Map)
     */
    @Override
    public ExecutionContext authenticate( Credentials credentials,
                                          String repositoryName,
                                          String workspaceName,
                                          ExecutionContext repositoryContext,
                                          Map<String, Object> sessionAttributes ) {
        if (credentials instanceof ServletCredentials) {
            ServletCredentials creds = (ServletCredentials)credentials;
            HttpServletRequest request = creds.getRequest();
            if (request != null) {
                return repositoryContext.with(new ServletSecurityContext(request));
            }
        }
        return null;
    }

    protected static class ServletSecurityContext implements SecurityContext {
        private HttpServletRequest request;
        private final String username;

        protected ServletSecurityContext( HttpServletRequest request ) {
            this.request = request;
            this.username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.core.SecurityContext#isAnonymous()
         */
        @Override
        public boolean isAnonymous() {
            return false;
        }

        /**
         * {@inheritDoc SecurityContext#getUserName()}
         * 
         * @see SecurityContext#getUserName()
         */
        @Override
        public final String getUserName() {
            return username;
        }

        /**
         * {@inheritDoc SecurityContext#hasRole(String)}
         * 
         * @see SecurityContext#hasRole(String)
         */
        @Override
        public final boolean hasRole( String roleName ) {
            return request != null && request.isUserInRole(roleName);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.core.SecurityContext#logout()
         */
        @Override
        public void logout() {
            request = null;
        }
    }
}
