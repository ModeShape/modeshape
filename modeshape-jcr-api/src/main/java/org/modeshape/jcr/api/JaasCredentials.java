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
package org.modeshape.jcr.api;

import javax.jcr.Credentials;
import javax.security.auth.login.LoginContext;

/**
 * A {@link Credentials} implementation that encapsulates an existing JAAS {@link LoginContext} instance. This can be used if
 * ModeShape is configured to use a different JAAS realm.
 * <p>
 * ModeShape will understand this Credentials implementation because it has a {@link #getLoginContext()} method that returns a
 * {@link LoginContext} object.
 * </p>
 */
public final class JaasCredentials implements Credentials {

    private static final long serialVersionUID = 1L;

    private final LoginContext loginContext;

    /**
     * Create a credentials that uses the supplied JAAS LoginContext for authentication and authorization.
     * 
     * @param loginContext the JAAS login context
     * @throws IllegalArgumentException if the provided argument is null
     */
    public JaasCredentials( LoginContext loginContext ) {
        if (loginContext == null) {
            throw new IllegalArgumentException("loginContext cannot be null");
        }
        this.loginContext = loginContext;
    }

    /**
     * Get the JAAS login context
     * 
     * @return loginContext the login context
     */
    public LoginContext getLoginContext() {
        return loginContext;
    }

}
