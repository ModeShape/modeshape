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
import java.util.Set;
import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.AnonymousCredentials;

/**
 * An implementation of {@link AuthenticationProvider} that allows for anonymous authentication and <i>role-based</i>
 * authorization of a supplied set of allowed roles.
 */
public class AnonymousProvider implements AuthenticationProvider {

    private final SecurityContext anonymousContext;

    /**
     * Creates a new anonymous provider.
     * 
     * @param anonymousUsername the anonymous user name
     * @param userRoles the set of roles for the anonymous user
     */
    public AnonymousProvider( final String anonymousUsername,
                              final Set<String> userRoles ) {
        CheckArg.isNotEmpty(anonymousUsername, "anonymousUsername");
        CheckArg.isNotNull(userRoles, "userRoles");
        this.anonymousContext = new AnonymousSecurityContext(userRoles, anonymousUsername);
    }

    @Override
    public ExecutionContext authenticate( Credentials credentials,
                                          String repositoryName,
                                          String workspaceName,
                                          ExecutionContext repositoryContext,
                                          Map<String, Object> sessionAttributes ) {

        if (credentials == null) {
            return repositoryContext.with(anonymousContext);
        }
        if (credentials instanceof AnonymousCredentials) {
            AnonymousCredentials creds = (AnonymousCredentials)credentials;
            sessionAttributes.putAll(creds.getAttributes());
            return repositoryContext.with(anonymousContext);
        }
        if (credentials instanceof GuestCredentials) {
            return repositoryContext.with(anonymousContext);
        }
        return null;
    }

    protected final class AnonymousSecurityContext implements SecurityContext {
        private final Set<String> userRoles;
        private final String anonymousUsername;

        protected AnonymousSecurityContext( Set<String> userRoles,
                                            String anonymousUsername ) {
            this.userRoles = userRoles;
            this.anonymousUsername = anonymousUsername;
        }

        @Override
        public boolean isAnonymous() {
            return true;
        }

        @Override
        public String getUserName() {
            return anonymousUsername;
        }

        @Override
        public boolean hasRole( String roleName ) {
            return userRoles.contains(roleName);
        }

        @Override
        public void logout() {
            // do nothing
        }

    }
}
