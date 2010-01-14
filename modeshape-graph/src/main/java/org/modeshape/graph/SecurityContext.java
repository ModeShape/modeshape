package org.modeshape.graph;

import net.jcip.annotations.NotThreadSafe;

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

}
