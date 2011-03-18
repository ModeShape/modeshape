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

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.jcr.api.SecurityContextCredentials;

/**
 * A {@link Credentials} implementation that assumes servlet-based authentication and provides authorization through the
 * {@link HttpServletRequest#isUserInRole(String) servlet role-checking mechanism}.
 * <p>
 * These credentials are really only valid for the life of the {@link HttpServletRequest servlet request}, and thus should be used
 * to obtain a Session for each request.
 * </p>
 */
public class ServletCredentials extends SecurityContextCredentials {

    private static final long serialVersionUID = 1L;

    /**
     * Create a {@link ServletSecurityContext} with the supplied {@link HttpServletRequest servlet information}.
     * 
     * @param request the servlet request; may not be null
     */
    public ServletCredentials( HttpServletRequest request ) {
        super(new ServletSecurityContext(request));
    }
}
