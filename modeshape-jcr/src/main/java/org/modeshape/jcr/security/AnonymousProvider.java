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

import java.security.Principal;
import java.util.ArrayList;
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
