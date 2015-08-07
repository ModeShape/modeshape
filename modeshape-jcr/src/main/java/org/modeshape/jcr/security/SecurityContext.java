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

import org.modeshape.common.annotation.NotThreadSafe;

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
     * @param roleName the name of the role to check. The name of the role will always come from ModeShape and will be one of 
     * ModeShape's built-in roles. 
     * @return true if the user has the role and is logged in; false otherwise
     * @see org.modeshape.jcr.ModeShapeRoles
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
