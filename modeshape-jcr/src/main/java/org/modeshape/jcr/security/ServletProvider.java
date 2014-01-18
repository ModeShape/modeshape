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
package org.modeshape.jcr.security;

import java.util.Map;
import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.ServletCredentials;

/**
 * An implementation of {@link AuthenticationProvider} that delegates to the {@link HttpServletRequest} referenced by the supplied
 * {@link ServletCredentials} instances for all authentication and <i>role-based</i> authorization.
 * <p>
 * Note that this class can only be used if the {@link HttpServletRequest} class is on the classpath.
 * </p>
 */
public class ServletProvider implements AuthenticationProvider {

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

        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public final String getUserName() {
            return username;
        }

        @Override
        public final boolean hasRole( String roleName ) {
            return request != null && request.isUserInRole(roleName);
        }

        @Override
        public void logout() {
            request = null;
        }

    }
}
