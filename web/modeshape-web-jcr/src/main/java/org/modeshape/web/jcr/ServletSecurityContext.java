/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.web.jcr;

import javax.servlet.http.HttpServletRequest;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.SecurityContext;

/**
 * Servlet-based {@link SecurityContext security context} that assumes servlet-based authentication and provides authorization
 * through the {@link HttpServletRequest#isUserInRole(String) servlet role-checking mechanism}.
 * <p>
 * This security context is really only valid for the life of the {@link HttpServletRequest servlet request} and should
 * only be used to support longer-lasting session scopes with great care. * 
 * </p>
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
