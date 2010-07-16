package org.modeshape.jcr.api;

/**
 * This interface is easily adaptable to, but not dependent on, the graph layer {@code SecurityContext} interface. This removes
 * the dependency between implementing classes and the modeshape-graph module.
 * <p>
 * This class is not thread-safe.
 * </p>
 */
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
