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

/**
 * {@link Credentials} implementation that wraps a {@link SecurityContext ModeShape JCR security context}.
 * <p>
 * This class provides a means of passing security information about an authenticated user into the ModeShape JCR session
 * implementation without using JAAS. This class effectively bypasses ModeShape's internal authentication mechanisms, so it is
 * very important that this context be provided for <i>authenticated users only</i>.
 * </p>
 * 
 * @deprecated Configure each repository to use a custom AuthenthicationProvider implementation
 */
@Deprecated
public class SecurityContextCredentials implements Credentials {
    private static final long serialVersionUID = 1L;
    private final SecurityContext jcrSecurityContext;

    /**
     * Initializes the class with an existing {@link SecurityContext JCR security context}.
     * 
     * @param jcrSecurityContext the security context; may not be null
     */
    public SecurityContextCredentials( final SecurityContext jcrSecurityContext ) {
        assert jcrSecurityContext != null;

        this.jcrSecurityContext = jcrSecurityContext;
    }

    /**
     * Returns the {@link SecurityContext JCR security context} for this instance.
     * 
     * @return the {@link SecurityContext JCR security context} for this instance; never null
     */
    public final SecurityContext getSecurityContext() {
        return this.jcrSecurityContext;
    }
}
