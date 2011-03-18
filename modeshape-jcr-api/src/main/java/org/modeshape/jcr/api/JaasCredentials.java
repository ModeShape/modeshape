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
import javax.security.auth.login.LoginContext;

/**
 * A {@link Credentials} implementation that encapsulates an existing JAAS {@link LoginContext} instance. This can be used if
 * ModeShape is configured to use a different JAAS realm.
 * <p>
 * ModeShape will understand this Credentials implementation because it has a {@link #getLoginContext()} method that returns a
 * {@link LoginContext} object.
 * </p>
 */
public final class JaasCredentials implements Credentials {

    private static final long serialVersionUID = 1L;

    private final LoginContext loginContext;

    /**
     * Create a credentials that uses the supplied JAAS LoginContext for authentication and authorization.
     * 
     * @param loginContext the JAAS login context
     */
    public JaasCredentials( LoginContext loginContext ) {
        assert loginContext != null;
        this.loginContext = loginContext;
    }

    /**
     * Get the JAAS login context
     * 
     * @return loginContext the login context
     */
    public LoginContext getLoginContext() {
        return loginContext;
    }

}
