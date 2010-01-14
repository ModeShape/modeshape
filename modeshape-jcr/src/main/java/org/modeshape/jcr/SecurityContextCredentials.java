package org.modeshape.jcr;

import javax.jcr.Credentials;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.SecurityContext;

/**
 * {@link Credentials} implementation that wraps a {@link SecurityContext ModeShape security context}.
 * <p>
 * This class provides a means of passing security information about an authenticated user into {@link JcrSession the ModeShape JCR
 * session implementation} without using JAAS. This class effectively bypasses ModeShape's internal authentication mechanisms, so it is
 * very important that this context be provided for <i>authenticated users only</i>.
 * </p>
 */
public final class SecurityContextCredentials implements Credentials {
    private static final long serialVersionUID = 1L;
    private final SecurityContext securityContext;

    /**
     * Initializes the class with an existing {@link SecurityContext security context}.
     * 
     * @param securityContext the security context; may not be null
     */
    public SecurityContextCredentials( SecurityContext securityContext ) {
        CheckArg.isNotNull(securityContext, "securityContext");

        this.securityContext = securityContext;
    }

    /**
     * Returns the {@link SecurityContext security context} for this instance.
     * 
     * @return the {@link SecurityContext security context} for this instance; never null
     */
    public final SecurityContext getSecurityContext() {
        return this.securityContext;
    }
}
