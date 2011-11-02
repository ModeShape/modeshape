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
import javax.jcr.Session;
import org.modeshape.jcr.api.AnonymousCredentials;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.core.SecurityContext;

/**
 * An interface used by a ModeShape {@link Repository} for authenticating users when they create new {@link Session sessions}
 * using {@link Repository#login(javax.jcr.Credentials, String)} and related methods.
 */
public interface AuthenticationProvider {

    /**
     * Authenticate the user that is using the supplied credentials. If the supplied credentials are authenticated, this method
     * should construct an {@link ExecutionContext} that reflects the authenticated environment, including the context's valid
     * {@link SecurityContext security context} that will be used for authorization throughout.
     * <p>
     * Note that each provider is handed a map into which it can place name-value pairs that will be used in the
     * {@link Session#getAttributeNames() Session attributes} of the Session that results from this authentication attempt.
     * ModeShape will ignore any attributes if this provider does not authenticate the credentials.
     * </p>
     * 
     * @param credentials the user's JCR credentials, which may be an {@link AnonymousCredentials} if authenticating as an
     *        anonymous user
     * @param repositoryName the name of the JCR repository; never null
     * @param workspaceName the name of the JCR workspace; never null
     * @param repositoryContext the execution context of the repository, which may be wrapped by this method
     * @param sessionAttributes the map of name-value pairs that will be placed into the {@link Session#getAttributeNames()
     *        Session attributes}; never null
     * @return the execution context for the authenticated user, or null if this provider could not authenticate the user
     */
    ExecutionContext authenticate( Credentials credentials,
                                   String repositoryName,
                                   String workspaceName,
                                   ExecutionContext repositoryContext,
                                   Map<String, Object> sessionAttributes );

}
