package org.modeshape.jcr.api;

import javax.jcr.Credentials;

/**
 * {@link Credentials} implementation that wraps a {@link SecurityContext ModeShape JCR security context}.
 * <p>
 * This class provides a means of passing security information about an authenticated user into the ModeShape JCR session
 * implementation without using JAAS. This class effectively bypasses ModeShape's internal authentication mechanisms, so it is
 * very important that this context be provided for <i>authenticated users only</i>.
 * </p>
 */
public final class SecurityContextCredentials implements Credentials {
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
