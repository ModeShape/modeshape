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
import javax.jcr.Session;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.AnonymousCredentials;
import org.modeshape.jcr.api.Repository;

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
