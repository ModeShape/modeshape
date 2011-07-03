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
package org.modeshape.jcr.api;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

/**
 * A {@link Credentials} implementation that assumes servlet-based authentication and provides authorization through the
 * {@link HttpServletRequest#isUserInRole(String) servlet role-checking mechanism}.
 * <p>
 * These credentials are really only valid for the life of the {@link HttpServletRequest servlet request}, and thus should be used
 * to obtain a Session for each request.
 * </p>
 * <p>
 * Note that this class can only be used if the {@link HttpServletRequest} class is on the classpath.
 * </p>
 */
public class ServletCredentials implements Credentials {

    private static final long serialVersionUID = 1L;

    private transient HttpServletRequest request;

    /**
     * Create a {@link Credentials} using the supplied {@link HttpServletRequest servlet information}.
     * 
     * @param request the servlet request
     */
    public ServletCredentials( HttpServletRequest request ) {
        this.request = request;
    }

    /**
     * Get the Servlet request that this credentials applies to.
     * 
     * @return the request, or null if this credentials is no longer valid
     */
    public HttpServletRequest getRequest() {
        return request;
    }
}
