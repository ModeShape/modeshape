package org.jboss.dna.web.jcr.rest;

import javax.servlet.http.HttpServletRequest;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.SecurityContext;

/**
 * Servlet-based {@link SecurityContext security context} that assumes servlet-based authentication and provides authorization
 * through the {@link HttpServletRequest#isUserInRole(String) servlet role-checking mechanism}.
 */
public class ServletSecurityContext implements SecurityContext {

    private final String userName;
    private final HttpServletRequest request;

    /**
     * Create a {@link ServletSecurityContext} with the supplied {@link HttpServletRequest servlet information}.
     * 
     * @param request the servlet request; may not be null
     */
    public ServletSecurityContext( HttpServletRequest request ) {
        CheckArg.isNotNull(request, "request");
        this.request = request;
        this.userName = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
    }

    /**
     * {@inheritDoc SecurityContext#getUserName()}
     * 
     * @see SecurityContext#getUserName()
     */
    public final String getUserName() {
        return userName;
    }

    /**
     * {@inheritDoc SecurityContext#hasRole(String)}
     * 
     * @see SecurityContext#hasRole(String)
     */
    public final boolean hasRole( String roleName ) {
        return request.isUserInRole(roleName);
    }

    /**
     * {@inheritDoc SecurityContext#logout()}
     * 
     * @see SecurityContext#logout()
     */
    public void logout() {
    }

}
