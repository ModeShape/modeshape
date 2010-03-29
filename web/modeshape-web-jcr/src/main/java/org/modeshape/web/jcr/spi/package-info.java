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
 * Service provider interface (SPI) for the JCR implementation that backs the ModeShape web libraries.
 * <p>
 * Service providers must provide a thread-safe implementation of the {@link RepositoryProvider} interface
 * which is then bundled in the server WAR.  The web library can be configured to use the provider by specifying
 * the fully-qualified name (FQN) of the custom repository provider class in the {@code org.modeshape.web.jcr.REPOSITORY_PROVIDER} context parameter
 * in the web configuration file (web.xml).   
 * </p>
 * <p>
 * Custom repository providers for JCR implementations that do not support hosting multiple repositories in the same server
 * can context can ignore the {@code repositoryName} parameter for {@link RepositoryProvider#getSession(javax.servlet.http.HttpServletRequest, String, String)},
 * but must always return a non-empty, non-null set containing some default repository name from {@link RepositoryProvider#getJcrRepositoryNames()}.
 * </p>
 * 
 * @see org.modeshape.web.jcr.RepositoryFactory
 * @see org.modeshape.web.jcr.spi.RepositoryProvider
 */
package org.modeshape.web.jcr.spi;

