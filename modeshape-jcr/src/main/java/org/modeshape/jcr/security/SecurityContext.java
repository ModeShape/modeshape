package org.modeshape.jcr.security;

import java.security.Principal;
import java.util.List;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.JcrSession;

/**
 * A security context provides a pluggable means to support disparate authentication and authorization mechanisms that specify the
 * user name and roles.
 * <p>
 * A security context should only be associated with the execution context <b>after</b> authentication has occurred.
 * </p>
 */
@NotThreadSafe
public interface SecurityContext {

    /**
     * Return whether this security context is an anonymous context.
     * 
     * @return true if this context represents an anonymous user, or false otherwise
     */
    boolean isAnonymous();

    /**
     * Returns the authenticated user's name
     * 
     * @return the authenticated user's name
     */
    String getUserName();

    /**
     * Returns whether the authenticated user has the given role.
     * 
     * @param roleName the name of the role to check
     * @return true if the user has the role and is logged in; false otherwise
     */
    boolean hasRole( String roleName );

    /**
     * Logs the user out of the authentication mechanism.
     * <p>
     * For some authentication mechanisms, this will be implemented as a no-op.
     * </p>
     */
    void logout();

    /**
     * Discovers principals known to authentication provider.
     * 
     * TCK related to the access control manager prohibits creation of access list
     * with unknown principals. So security context must exposes known principals.
     * 
     * From the other hand this method allows to keep principals transparent 
     * without explicit definition of principal's type: user or group.
     * 
     * @return list of known principals. 
     */
    public List<Principal> getPrincipals();    
    
    /**
     * Associate given session with this context.
     * 
     * This method allows to utilize content for security purposes.
     * 
     * @param session 
     */
    public void with(JcrSession session);
}
