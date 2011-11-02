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
/**
 * ModeShape JCR repositories have a pluggable authentication and authorization framework. Out of the box, each repository
 * is configured to support authenticating and authorizing using JAAS, HTTP servlet (if the servlet library is on the classpath), and (if configured) anonymous logins.
 * In addition, Each repository can also be configured with customzied authenticators.
 * <p>
 * Creating a custom authenticator is a matter of properly implementing {@link org.modeshape.jcr.security.AuthenticationProvider}
 * and configuring the repository to use that class. Each authenticator is responsible for authenticating the supplied
 * {@link javax.jcr.Credentials} and returning an ExecutionContext that will represent the user, including
 * its embedded {@link org.modeshape.jcr.core.SecurityContext} (for simple role-based authorization) or {@link org.modeshape.jcr.security.AuthorizationProvider} (for a combination of path- and role-based authorization).
 * </p>
 */

package org.modeshape.jcr.security;

