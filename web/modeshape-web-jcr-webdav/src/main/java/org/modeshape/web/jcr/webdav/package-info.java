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
/**
 * This package contains the core components for the ModeShape WebDAV server implementation.
 * <p>
 * The key classes are:
 * <ul>
 * <li>{@link org.modeshape.web.jcr.webdav.ModeShapeWebdavServlet} - the servlet class that handles requests for WebDAV URIs</li>
 * <li>{@link org.modeshape.web.jcr.webdav.ModeShapeWebdavStore} - the implementation class that maps WebDAV operations to JCR operations</li>
 * <li>{@link org.modeshape.web.jcr.webdav.RequestResolver} - the contract for mapping an incoming URI to a repository, workspace, and path within the workspace</li>
 * <li>{@link org.modeshape.web.jcr.webdav.SingleRepositoryRequestResolver} - the {@link org.modeshape.web.jcr.webdav.RequestResolver} that maps URIs into a path within a single, hard-coded repository and workspace (defined by the configuration)</li>
 * <li>{@link org.modeshape.web.jcr.webdav.MultiRepositoryRequestResolver} - the {@link org.modeshape.web.jcr.webdav.RequestResolver} that maps URIs into a repository name, workspace name, and path</li>
 * <li>{@link org.modeshape.web.jcr.webdav.ResolvedRequest} - the representation of a repository name, workspace name, and path as output by the {@link org.modeshape.web.jcr.webdav.RequestResolver}</li>
 * </ul>
 * </p>
 */
package org.modeshape.web.jcr.webdav;

