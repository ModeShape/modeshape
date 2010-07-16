package org.modeshape.jcr;

import javax.jcr.Credentials;
import org.modeshape.graph.SecurityContext;

public class JcrSecurityContextCredentials implements Credentials {
    private static final long serialVersionUID = 1L;
    private final SecurityContext securityContext;

    /**
     * Initializes the class with an existing {@link SecurityContext security context}.
     * 
     * @param securityContext the security context; may not be null
     */
    public JcrSecurityContextCredentials( final SecurityContext securityContext ) {
        assert securityContext != null;

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
